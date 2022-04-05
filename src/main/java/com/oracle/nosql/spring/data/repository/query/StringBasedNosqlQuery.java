/*-
 * Copyright (c) 2020, 2022 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */
package com.oracle.nosql.spring.data.repository.query;

import com.oracle.nosql.spring.data.core.NosqlOperations;
import com.oracle.nosql.spring.data.core.query.NosqlQuery;
import com.oracle.nosql.spring.data.core.query.StringQuery;
import com.oracle.nosql.spring.data.repository.Query;

import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;

/**
 * Class implements native queries that come from {@link Query} annotation.
 */
public class StringBasedNosqlQuery extends AbstractNosqlQuery {
    private final String query;

    private final boolean isCountQuery;
    private final boolean isExistsQuery;
    private final boolean isDeleteQuery;

    public StringBasedNosqlQuery(NosqlQueryMethod method,
        NosqlOperations dbOperations,
        QueryMethodEvaluationContextProvider evaluationContextProvider) {
        this(method.getAnnotatedQuery(), method, dbOperations,
            evaluationContextProvider);
    }

    public StringBasedNosqlQuery(String query, NosqlQueryMethod method,
        NosqlOperations dbOperations,
        QueryMethodEvaluationContextProvider evaluationContextProvider) {
        super(method, dbOperations);

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

    private static boolean hasAmbiguousProjectionFlags(boolean isCountQuery, boolean isExistsQuery,
        boolean isDeleteQuery) {
        return countBooleanTrueValues(isCountQuery, isExistsQuery, isDeleteQuery) > 1;
    }

    static int countBooleanTrueValues(boolean... values) {

        int count = 0;

        for (boolean value : values) {
            if (value) {
                count++;
            }
        }

        return count;
    }
}
