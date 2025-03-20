/*-
 * Copyright (c) 2020, 2025 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */

package com.oracle.nosql.spring.data.repository.query;

import com.oracle.nosql.spring.data.core.ReactiveNosqlOperations;
import com.oracle.nosql.spring.data.core.query.NosqlQuery;
import com.oracle.nosql.spring.data.core.query.StringQuery;
import com.oracle.nosql.spring.data.repository.Query;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;

import static com.oracle.nosql.spring.data.repository.query.StringBasedNosqlQuery.hasAmbiguousProjectionFlags;

public class ReactiveStringBasedNosqlQuery extends AbstractReactiveNosqlQuery {
    private final String query;

    private final boolean isCountQuery;
    private final boolean isExistsQuery;
    private final boolean isDeleteQuery;

    public ReactiveStringBasedNosqlQuery(NosqlQueryMethod method,
         ReactiveNosqlOperations operations,
         QueryMethodEvaluationContextProvider evaluationContextProvider) {
        this(method.getAnnotatedQuery(), method, operations,
                evaluationContextProvider);
    }

    public ReactiveStringBasedNosqlQuery(String query,
        NosqlQueryMethod method,
        ReactiveNosqlOperations operations,
        QueryMethodEvaluationContextProvider evaluationContextProvider) {
        super(method, operations);
        this.query = query;
        if (method.hasAnnotatedQuery()) {
            Query queryAnnotation = method.getQueryAnnotation();
            isCountQuery = queryAnnotation.count();
            isExistsQuery = queryAnnotation.exists();
            isDeleteQuery = queryAnnotation.delete();
            if (hasAmbiguousProjectionFlags(isCountQuery, isExistsQuery,
                    isDeleteQuery)) {
                throw new IllegalArgumentException(
                    String.format("Manually defined query for %s cannot be a " +
                        "count and exists or delete query at the same time!",
                        method));
            }
        } else {
            isCountQuery = false;
            isExistsQuery = false;
            isDeleteQuery = false;
        }
    }

    @Override
    protected NosqlQuery createQuery(NosqlParameterAccessor accessor) {
        return new StringQuery(getQueryMethod(), query, accessor);
    }

    @Override
    protected boolean isDeleteQuery() {
        return isDeleteQuery;
    }

    @Override
    protected boolean isExistsQuery() {
        return isExistsQuery;
    }

    @Override
    protected boolean isCountQuery() {
        return isCountQuery;
    }
}
