package com.ldbc.driver.generator;


import com.ldbc.driver.util.Function1;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;


public class CsvEventStreamReader_NEW<BASE_EVENT_TYPE, DECODER_KEY_TYPE> implements Iterator<BASE_EVENT_TYPE> {
    private final Map<DECODER_KEY_TYPE, EventDecoder<BASE_EVENT_TYPE>> decoders;
    private final Iterator<String[]> csvRowIterator;
    private final Function1<String[], DECODER_KEY_TYPE> decoderKeyExtractor;

    public CsvEventStreamReader_NEW(Iterator<String[]> csvRowIterator,
                                    Map<DECODER_KEY_TYPE, EventDecoder<BASE_EVENT_TYPE>> decoders,
                                    Function1<String[], DECODER_KEY_TYPE> decoderKeyExtractor) {
        this.csvRowIterator = csvRowIterator;
        this.decoders = decoders;
        this.decoderKeyExtractor = decoderKeyExtractor;
    }

    @Override
    public boolean hasNext() {
        return csvRowIterator.hasNext();
    }

    @Override
    public BASE_EVENT_TYPE next() {
        String[] csvRow = csvRowIterator.next();
        DECODER_KEY_TYPE decoderKey = decoderKeyExtractor.apply(csvRow);
        EventDecoder<BASE_EVENT_TYPE> decoder = decoders.get(decoderKey);
        if (null == decoder)
            throw new NoSuchElementException(String.format(
                    "No decoder found that matches this column\nROW: %s\nDECODER KEY: %s",
                    Arrays.toString(csvRow),
                    decoderKey
            ));
        return decoder.decodeEvent(csvRow);
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException(String.format("%s does not support remove()", getClass().getSimpleName()));
    }


    public static interface EventDecoder<BASE_EVENT_TYPE> {
        BASE_EVENT_TYPE decodeEvent(String[] csvRow);
    }
}
