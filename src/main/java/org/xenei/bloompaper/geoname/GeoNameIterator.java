package org.xenei.bloompaper.geoname;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.apache.commons.collections4.bloomfilter.Shape;

public class GeoNameIterator implements Iterator<GeoName>, AutoCloseable {

    public final static URL DEFAULT_INPUT = GeoNameIterator.class.getResource("/allCountries.txt");

    private final Shape shape;
    private final BufferedReader bufferedReader;
    private GeoName next;
    private int count = 0;

    public GeoNameIterator(URL inputFile, Shape shape) throws IOException {
        this(inputFile.openStream(), shape);
    }

    public GeoNameIterator(InputStream stream, Shape shape) {
        this(new InputStreamReader(stream), shape);
    }

    public GeoNameIterator(Reader reader, Shape shape) {
        if (reader instanceof BufferedReader) {
            bufferedReader = (BufferedReader) reader;
        } else {
            bufferedReader = new BufferedReader(reader);
        }
        this.shape = shape;

        next = null;
    }

    @Override
    public void close() throws IOException {
        bufferedReader.close();
    }

    @Override
    public boolean hasNext() {
        if (next == null) {
            String s;
            try {
                s = bufferedReader.readLine();
            } catch (IOException e) {
                return false;
            }
            if (s == null) {
                return false;
            }
            next = GeoName.Serde.deserialize(s,shape);
        }
        return true;
    }

    @Override
    public GeoName next() {
        if (hasNext()) {
            try {
                count++;
                if ((count % 1000) == 0) {
                    System.out.println(String.format("read : %8d", count));
                }
                return next;
            } finally {
                next = null;
            }
        } else {
            throw new NoSuchElementException();
        }
    }
}
