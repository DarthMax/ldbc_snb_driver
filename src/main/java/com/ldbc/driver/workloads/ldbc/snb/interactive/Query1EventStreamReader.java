package com.ldbc.driver.workloads.ldbc.snb.interactive;


import com.google.common.collect.Lists;
import com.ldbc.driver.Operation;
import com.ldbc.driver.generator.CsvEventStreamReader;
import com.ldbc.driver.generator.GeneratorException;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.util.Iterator;

public class Query1EventStreamReader implements Iterator<Operation<?>> {
    public static final String PERSON_ID = "PersonID";
    public static final String PERSON_URI = "PersonURI";
    public static final String FIRST_NAME = "Name";

    private final CsvEventStreamReader<Operation<?>> csvEventStreamReader;
    private final CsvEventStreamReader.EventDecoder<Operation<?>> decoder;

    public Query1EventStreamReader(Iterator<String[]> csvRowIterator) {
        this(csvRowIterator, CsvEventStreamReader.EventReturnPolicy.AT_LEAST_ONE_MATCH);
    }

    public Query1EventStreamReader(Iterator<String[]> csvRowIterator, CsvEventStreamReader.EventReturnPolicy eventReturnPolicy) {
        this.decoder = new EventDecoderQuery1(new ObjectMapper());
        Iterable<CsvEventStreamReader.EventDecoder<Operation<?>>> decoders = Lists.newArrayList(this.decoder);
        CsvEventStreamReader.EventDescriptions<Operation<?>> eventDescriptions = new CsvEventStreamReader.EventDescriptions<>(decoders, eventReturnPolicy);
        this.csvEventStreamReader = new CsvEventStreamReader<>(csvRowIterator, eventDescriptions);
    }

    @Override
    public boolean hasNext() {
        return csvEventStreamReader.hasNext();
    }

    @Override
    public Operation<?> next() {
        return csvEventStreamReader.next();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException(String.format("%s does not support remove()", getClass().getSimpleName()));
    }

    public static class EventDecoderQuery1 implements CsvEventStreamReader.EventDecoder<Operation<?>> {
        private final ObjectMapper objectMapper;

        public EventDecoderQuery1(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        public boolean eventMatchesDecoder(String[] csvRow) {
            return true;
        }

        @Override
        public Operation<?> decodeEvent(String[] csvRow) {
            String eventParamsAsJsonString = csvRow[0];
            JsonNode params;
            try {
                params = objectMapper.readTree(eventParamsAsJsonString);
            } catch (IOException e) {
                throw new GeneratorException(String.format("Error parsing JSON event params\n%s", eventParamsAsJsonString), e);
            }
            long personId = params.get(PERSON_ID).asLong();
            String personUri = params.get(PERSON_URI).asText();
            String personFirstName = params.get(FIRST_NAME).asText();
            return new LdbcQuery1(personId, personUri, personFirstName, LdbcQuery1.DEFAULT_LIMIT);
        }
    }

    ;
}
