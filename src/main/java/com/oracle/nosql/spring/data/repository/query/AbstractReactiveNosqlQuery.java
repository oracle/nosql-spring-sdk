/*-
 * Copyright (c) 2020, 2025 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */
package com.oracle.nosql.spring.data.repository.query;

import com.oracle.nosql.spring.data.core.ReactiveNosqlOperations;
import com.oracle.nosql.spring.data.core.query.NosqlQuery;
import com.oracle.nosql.spring.data.repository.support.NosqlEntityInformation;

import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.ResultProcessor;

public abstract class AbstractReactiveNosqlQuery implements RepositoryQuery {

    private final NosqlQueryMethod method;
    private final ReactiveNosqlOperations operations;

    public AbstractReactiveNosqlQuery(NosqlQueryMethod method,
        ReactiveNosqlOperations operations) {
        this.method = method;
        this.operations = operations;
    }

    @Override
    public Object execute(Object[] parameters) {
        final NosqlParameterAccessor accessor =
            new NosqlParameterParameterAccessor(method, parameters);
        final NosqlQuery query = createQuery(accessor);

        final ResultProcessor processor =
            method.getResultProcessor().withDynamicProjection(accessor);
        final String tableName =
            ((NosqlEntityMetadata) method.getEntityInformation()).getTableName();

        final ReactiveNosqlQueryExecution execution = getExecution(accessor);
        return execution.execute(query,
            processor.getReturnedType().getDomainType(),
            tableName);
    }


    private ReactiveNosqlQueryExecution getExecution(
        NosqlParameterAccessor accessor) {
        NosqlEntityInformation<?, ?> entityInformation =
            ((NosqlEntityMetadata) method.getEntityInformation())
                .getNosqlEntityInformation();
        if (isDeleteQuery()) {
            return new ReactiveNosqlQueryExecution.DeleteExecution(operations,
                entityInformation);
        } else if (isExistsQuery()) {
            return new ReactiveNosqlQueryExecution.ExistsExecution(operations,
                entityInformation);
        } else if (isCountQuery()) {
            return new ReactiveNosqlQueryExecution.CountExecution(operations,
                entityInformation);
        } else {
            return new ReactiveNosqlQueryExecution.MultiEntityExecution(
                operations, entityInformation);
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
