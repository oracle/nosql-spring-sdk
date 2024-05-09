/*-
 * Copyright (c) 2020, 2024 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */
package com.oracle.nosql.spring.data.core;

import java.util.Iterator;
import java.util.stream.Stream;

import oracle.nosql.driver.NoSQLException;
import oracle.nosql.driver.NoSQLHandle;
import oracle.nosql.driver.ops.PreparedStatement;
import oracle.nosql.driver.ops.QueryRequest;
import oracle.nosql.driver.ops.QueryResult;
import oracle.nosql.driver.util.LruCache;
import oracle.nosql.driver.values.MapValue;

import com.oracle.nosql.spring.data.core.convert.MappingNosqlConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.StreamUtils;
import org.springframework.lang.NonNull;

/**
 * Set of helping classes and methods to get Iterable, Iterator, Stream based
 * on the results of a QueryRequest.
 */
public class IterableUtil {

    public static class IterableImpl implements Iterable<MapValue> {
        final IteratorImpl iter;

        IterableImpl(NoSQLHandle nosqlClient,
            LruCache<String, PreparedStatement> psCache,
            QueryRequest queryRequest) {
            this.iter = new IteratorImpl(nosqlClient, psCache, queryRequest);
        }

        @Override
        public Iterator<MapValue> iterator() {
            return iter;
        }
    }


    public static class IteratorImpl implements Iterator<MapValue> {
        private static final Logger log =
            LoggerFactory.getLogger(IteratorImpl.class);

        final NoSQLHandle nosqlClient;
        final LruCache<String, PreparedStatement> psCache;
        final QueryRequest queryRequest;
        QueryResult queryResult;
        Iterator<MapValue> iterator;

        IteratorImpl(NoSQLHandle nosqlClient,
            LruCache<String, PreparedStatement> psCache,
            QueryRequest queryRequest) {
            this.nosqlClient = nosqlClient;
            this.psCache = psCache;
            this.queryRequest = queryRequest;
        }

        @Override
        public boolean hasNext() {
            ensureIterator();
            return iterator.hasNext();
        }

        @Override
        public MapValue next() {
            ensureIterator();
            return iterator.next();
        }

        private boolean ensureIterator() {
            if (iterator != null && iterator.hasNext()) {
                // iterator still has results
                return true;
            }

            if (iterator != null &&
                !iterator.hasNext() && queryRequest.isDone()) {
                return false;
            }

            // get more results from server
            try {
                do {
                    queryResult = nosqlClient.query(queryRequest);
                    iterator = queryResult.getResults().iterator();
                } // if results is empty try again if not done
                while (!iterator.hasNext() && !queryRequest.isDone());
                // there may be more results

                if (!iterator.hasNext() && queryRequest.isDone()) {
                    // no more results left
                    return false;
                }

                return true;
            } catch (NoSQLException nse) {
                String sql = queryRequest.getPreparedStatement() != null ?
                    queryRequest.getPreparedStatement().getSQLText() :
                    queryRequest.getStatement();
                log.error("Query: {}", sql);
                log.error(nse.getMessage());

                psCache.remove(sql);
                log.info("Removed from prepared statements cache: '{}'", sql);
                throw MappingNosqlConverter.convert(nse);
            }
        }
    }

    public static <T> Iterable<T> getIterableFromStream(
        Stream<T> stream) {
        return new StreamIterable<>(stream);
    }

    public static <T> Stream<T> getStreamFromIterable(
        @NonNull Iterable<T> iterable) {
        Iterator<T> iterator = iterable.iterator();
        return getStreamFromIterator(iterator);
    }

    public static <T> Stream<T> getStreamFromIterator(
        @NonNull Iterator<T> iterator) {
        return StreamUtils.createStreamFromIterator(iterator);
    }

    static class StreamIterable<T> implements Iterable<T> {
        private final Stream<T> stream;

        StreamIterable(Stream<T> stream) {
            this.stream = stream;
        }

        @Override
        public Iterator<T> iterator() {
            return stream.iterator();
        }
    }
}
