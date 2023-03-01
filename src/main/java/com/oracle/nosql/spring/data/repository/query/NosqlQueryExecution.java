/*-
 * Copyright (c) 2020, 2023 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */
package com.oracle.nosql.spring.data.repository.query;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import oracle.nosql.driver.values.MapValue;

import com.oracle.nosql.spring.data.core.IterableUtil;
import com.oracle.nosql.spring.data.core.NosqlOperations;
import com.oracle.nosql.spring.data.core.query.NosqlQuery;
import com.oracle.nosql.spring.data.repository.support.NosqlEntityInformation;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.lang.NonNull;

public interface NosqlQueryExecution {

    Object execute(NosqlQuery query);


    abstract class AbstractExecution<T> implements NosqlQueryExecution {
        protected final NosqlOperations operations;
        protected final NosqlEntityInformation<T, ?> entityInformation;
        protected final NosqlQueryMethod queryMethod;

        AbstractExecution(NosqlOperations operations,
            NosqlEntityInformation<T, ?> entityInformation,
            NosqlQueryMethod queryMethod) {
            this.operations = operations;
            this.entityInformation = entityInformation;
            this.queryMethod = queryMethod;
        }

        @Override
        public abstract Object execute(NosqlQuery query);
    }

    final class CollectionExecution<T> extends AbstractExecution<T> {
        CollectionExecution(NosqlOperations operations,
            NosqlEntityInformation<T, ?> entityInformation,
            NosqlQueryMethod queryMethod) {
            super(operations, entityInformation, queryMethod);
        }

        @SuppressWarnings("unchecked")
        @Override
        public Iterable<T> execute(NosqlQuery query) {
            return operations.find(entityInformation,
                (Class<T>) queryMethod.getReturnedObjectType(),
                query);
        }
    }

    final class ExistsExecution<T> extends AbstractExecution<T> {
        ExistsExecution(NosqlOperations operations,
            NosqlEntityInformation<T, ?> entityInformation,
            NosqlQueryMethod queryMethod) {
            super(operations, entityInformation, queryMethod);
        }

        @Override
        public Object execute(NosqlQuery query)
        {
            Iterable<T> res = operations.find(entityInformation,
                entityInformation.getJavaType(),
                query);

            if (res == null) {
                throw new IllegalStateException("Exists result not available.");
            }

            return res.iterator().hasNext();
        }
    }

    final class DeleteExecution<T> extends AbstractExecution<T> {
        DeleteExecution(NosqlOperations operations,
            NosqlEntityInformation<T, ?> entityInformation,
            NosqlQueryMethod queryMethod) {
            super(operations, entityInformation, queryMethod);
        }

        @Override
        public Iterable<T> execute(NosqlQuery query) {
            return operations.delete(entityInformation, query);
        }
    }

    final class PagedExecution<T> extends AbstractExecution<T> {
        private final Pageable pageable;

        PagedExecution(NosqlOperations operations,
            NosqlEntityInformation<T, ?> entityInformation,
            NosqlQueryMethod queryMethod,
            Pageable pageable) {
            super(operations, entityInformation, queryMethod);
            this.pageable = pageable;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Object execute(NosqlQuery query) {
            query.with(pageable);
            int overallLimit = query.getLimit();

            List<T> result =
                IterableUtil.getStreamFromIterable(
                    operations.find(entityInformation,
                        (Class<T>) queryMethod.getReturnedObjectType(), query))
                    .collect(Collectors.toList());

            return PageableExecutionUtils.getPage(result, pageable, () -> {
                long count = result.size();
                return overallLimit != 0 ? Math.min(count, overallLimit) : count;
            });
        }
    }

    final class SlicedExecution<T> extends AbstractExecution<T> {
        @NonNull
        private final Pageable pageable;

        SlicedExecution(NosqlOperations operations,
            NosqlEntityInformation<T, ?> entityInformation,
            NosqlQueryMethod queryMethod,
            @NonNull Pageable pageable) {
            super(operations, entityInformation, queryMethod);
            this.pageable = pageable;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Object execute(NosqlQuery query) {
            int pageSize = pageable.getPageSize();
            // Apply Pageable but tweak limit to peek into next page
            NosqlQuery modifiedQuery = query.with(pageable).limit(pageSize + 1);
            List<T> result =
                IterableUtil.getStreamFromIterable(
                    operations.find(entityInformation,
                        (Class<T>) queryMethod.getReturnedObjectType(),
                        modifiedQuery))
                    .collect(Collectors.toList());

            boolean hasNext = result.size() > pageSize;

            return new SliceImpl<>(hasNext ?
                result.subList(0, pageSize) :
                result, pageable, hasNext);
        }
    }

    final class CountExecution<T> extends AbstractExecution<T> {
        CountExecution(NosqlOperations operations,
            NosqlEntityInformation<T, ?> entityInformation,
            NosqlQueryMethod queryMethod) {
            super(operations, entityInformation, queryMethod);
        }

        @Override
        public Object execute(NosqlQuery query) {

            Iterable<MapValue> res =
                operations.count(entityInformation, query);

            if (res == null) {
                throw new IllegalStateException("Count result not available.");
            }

            Iterator<MapValue> iterator = res.iterator();

            MapValue mapValue = iterator.next();
            if (mapValue.size() != 1) {
                throw new IllegalStateException("Unexpected count query " +
                    "result.");
            }
            return mapValue.values().iterator().next().asLong().getValue();
        }
    }
}
