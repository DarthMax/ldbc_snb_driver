package com.ldbc.driver.runtime.metrics;

import com.ldbc.driver.OperationResultReport;
import com.ldbc.driver.runtime.ConcurrentErrorReporter;
import com.ldbc.driver.runtime.QueueEventFetcher;
import com.ldbc.driver.runtime.scheduling.ExecutionDelayPolicy;
import com.ldbc.driver.util.csv.SimpleCsvFileWriter;

import java.util.Queue;

public class ThreadedQueuedConcurrentMetricsServiceThread extends Thread {
    private final MetricsManager metricsManager;
    private final ConcurrentErrorReporter errorReporter;
    private final QueueEventFetcher<MetricsCollectionEvent> queueEventFetcher;
    private final ExecutionDelayPolicy executionDelayPolicy;
    private final boolean shouldRecordStartTimeDelayLatencies;
    private final SimpleCsvFileWriter csvResultsLogWriter;
    private Long processedEventCount = 0l;
    private Long expectedEventCount = null;

    public ThreadedQueuedConcurrentMetricsServiceThread(ConcurrentErrorReporter errorReporter,
                                                        Queue<MetricsCollectionEvent> metricsEventsQueue,
                                                        MetricsManager metricsManager,
                                                        boolean shouldRecordStartTimeDelayLatencies,
                                                        ExecutionDelayPolicy executionDelayPolicy,
                                                        SimpleCsvFileWriter csvResultsLogWriter) {
        this(errorReporter,
                QueueEventFetcher.queueEventFetcherFor(metricsEventsQueue),
                metricsManager,
                shouldRecordStartTimeDelayLatencies,
                executionDelayPolicy,
                csvResultsLogWriter);
    }

    private ThreadedQueuedConcurrentMetricsServiceThread(ConcurrentErrorReporter errorReporter,
                                                         QueueEventFetcher<MetricsCollectionEvent> queueEventFetcher,
                                                         MetricsManager metricsManager,
                                                         boolean shouldRecordStartTimeDelayLatencies,
                                                         ExecutionDelayPolicy executionDelayPolicy,
                                                         SimpleCsvFileWriter csvResultsLogWriter) {
        super(ThreadedQueuedConcurrentMetricsServiceThread.class.getSimpleName() + "-" + System.currentTimeMillis());
        this.errorReporter = errorReporter;
        this.metricsManager = metricsManager;
        this.queueEventFetcher = queueEventFetcher;
        this.shouldRecordStartTimeDelayLatencies = shouldRecordStartTimeDelayLatencies;
        this.executionDelayPolicy = executionDelayPolicy;
        this.csvResultsLogWriter = csvResultsLogWriter;
    }

    @Override
    public void run() {
        long toleratedDelayAsNano = executionDelayPolicy.toleratedDelay().asNano();
        while (null == expectedEventCount || processedEventCount < expectedEventCount) {
            try {
                MetricsCollectionEvent event = queueEventFetcher.fetchNextEvent();
                switch (event.type()) {
                    case SUBMIT_RESULT:
                        OperationResultReport result = ((MetricsCollectionEvent.SubmitResultEvent) event).result();

                        if (null != csvResultsLogWriter) {
                            csvResultsLogWriter.writeRow(
                                    result.operation().getClass().getSimpleName(),
                                    Long.toString(result.operation().scheduledStartTime().asMilli()),
                                    Long.toString(result.actualStartTime().asMilli()),
                                    Long.toString(result.runDuration().asMilli()));
                        }

                        boolean shouldRecordResultMetricsForThisOperation = true;
                        if (shouldRecordStartTimeDelayLatencies) {
                            // TODO if operation is blocked in spinner because something like GCT_CHECK never returns there needs to be a way to detect and terminate
                            // TODO this may not be triggered by maximum runtime check, as execution does not begin until spinner returns
                            // TODO perhaps it can be done in the same/similar way though, by somehow getting metrics service to occasionally check for "progress"? look into further
                            // TOO EARLY = <---(now)--(scheduled)[<---delay--->]------> <=(Time Line)
                            // GOOD      = <-----(scheduled)[<-(now)--delay--->]------> <=(Time Line)
                            // TOO LATE  = <-----(scheduled)[<---delay--->]--(now)----> <=(Time Line)
                            if (result.operation().scheduledStartTime().asNano() + toleratedDelayAsNano < result.actualStartTime().asNano()) {
                                shouldRecordResultMetricsForThisOperation = executionDelayPolicy.handleExcessiveDelay(result.operation());
                            }
                        }

                        if (shouldRecordResultMetricsForThisOperation) {
                            try {
                                metricsManager.measure(result);
                            } catch (MetricsCollectionException e) {
                                errorReporter.reportError(
                                        this,
                                        String.format("Encountered error while collecting metrics for result: %s\n%s",
                                                result.toString(),
                                                ConcurrentErrorReporter.stackTraceToString(e)));
                            }
                        }
                        processedEventCount++;
                        break;
                    case WORKLOAD_STATUS:
                        ThreadedQueuedConcurrentMetricsService.MetricsStatusFuture statusFuture = ((MetricsCollectionEvent.StatusEvent) event).future();
                        statusFuture.set(metricsManager.status());
                        break;
                    case WORKLOAD_RESULT:
                        ThreadedQueuedConcurrentMetricsService.MetricsWorkloadResultFuture workloadResultFuture = ((MetricsCollectionEvent.WorkloadResultEvent) event).future();
                        WorkloadResultsSnapshot resultsSnapshot = metricsManager.snapshot();
                        workloadResultFuture.set(resultsSnapshot);
                        break;
                    case TERMINATE_SERVICE:
                        if (expectedEventCount == null) {
                            expectedEventCount = ((MetricsCollectionEvent.TerminationEvent) event).expectedEventCount();
                        } else {
                            // this is not the first termination event that the thread has received
                            errorReporter.reportError(
                                    this,
                                    String.format("Encountered multiple %s events. First expectedEventCount[%s]. Second expectedEventCount[%s]",
                                            MetricsCollectionEvent.MetricsEventType.TERMINATE_SERVICE.name(),
                                            expectedEventCount,
                                            ((MetricsCollectionEvent.TerminationEvent) event).expectedEventCount()));
                        }
                        break;
                    default:
                        errorReporter.reportError(
                                this,
                                String.format("Encountered unexpected event type: %s", event.type().name()));
                        return;
                }
            } catch (Throwable e) {
                errorReporter.reportError(
                        this,
                        String.format("Encountered unexpected exception\n%s", ConcurrentErrorReporter.stackTraceToString(e)));
                return;
            }
        }
    }
}
