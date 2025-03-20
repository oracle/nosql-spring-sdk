/*-
 * Copyright (c) 2020, 2025 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */
package com.oracle.nosql.spring.data.repository.query;

import com.oracle.nosql.spring.data.core.NosqlOperations;
import com.oracle.nosql.spring.data.core.query.NosqlQuery;
import com.oracle.nosql.spring.data.repository.support.NosqlEntityInformation;

import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.ResultProcessor;
import org.springframework.lang.Nullable;

public abstract class AbstractNosqlQuery implements RepositoryQuery {

    private final NosqlQueryMethod method;
    private final NosqlOperations operations;

    public AbstractNosqlQuery(NosqlQueryMethod method,
        NosqlOperations operations) {
        this.method = method;
        this.operations = operations;
    }

    @SuppressWarnings("unchecked")
    @Override
    @Nullable
    public Object execute(Object[] parameters) {
        final NosqlParameterAccessor accessor =
            new NosqlParameterParameterAccessor(method, parameters);
        final NosqlQuery query = createQuery(accessor);

        final NosqlQueryExecution execution = getExecution(accessor);

        Object queryResult = execution.execute(query);

        final ResultProcessor processor = method.getResultProcessor().
            withDynamicProjection(accessor);

        if (processor.getReturnedType().isProjecting()) {
            return queryResult;
        }
        return processor.processResult(queryResult);
    }

    @SuppressWarnings("unchecked")
    private <T> NosqlQueryExecution getExecution(
        NosqlParameterAccessor accessor) {

        NosqlEntityInformation<T, ?> entityInformation =
            ((NosqlEntityMetadata<T>) method.getEntityInformation())
            .getNosqlEntityInformation();

        if (isDeleteQuery()) {
            return new NosqlQueryExecution
                .DeleteExecution<>(operations, entityInformation, method);
        } else if (method.isSliceQuery()) {
            return new NosqlQueryExecution.SlicedExecution<>(operations,
                entityInformation, method, accessor.getPageable());
        //todo
//        } else if (method.isStreamQuery()) {
//            return q -> operations.matching(q).stream();
        } else if (isCountQuery()) {
            return new NosqlQueryExecution
                .CountExecution<>(operations, entityInformation, method);
        } else if (method.isCollectionQuery()) {
            return new NosqlQueryExecution
                .CollectionExecution<>(operations, entityInformation, method);
        } else if (method.isPageQuery()) {
            return new NosqlQueryExecution
                .PagedExecution<>(operations, entityInformation, method,
                accessor.getPageable());
        } else if (isExistsQuery()) {
            return new NosqlQueryExecution
                .ExistsExecution<>(operations, entityInformation, method);
        } else {
            throw new IllegalStateException("NYI");
        }
    }

    @Override
    public NosqlQueryMethod getQueryMethod() {
        return method;
    }

    protected abstract NosqlQuery createQuery(NosqlParameterAccessor accessor);

    protected abstract boolean isDeleteQuery();

    protected abstract boolean isExistsQuery();

    protected abstract boolean isCountQuery();
}

