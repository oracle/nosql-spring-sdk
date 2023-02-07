/*-
 * Copyright (c) 2020, 2022 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */
package com.oracle.nosql.spring.data.repository.support;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Optional;

import com.oracle.nosql.spring.data.core.NosqlOperations;
import com.oracle.nosql.spring.data.repository.query.NosqlQueryMethod;
import com.oracle.nosql.spring.data.repository.query.PartTreeNosqlQuery;
import com.oracle.nosql.spring.data.repository.query.StringBasedNosqlQuery;

import org.springframework.context.ApplicationContext;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.util.Assert;

public class NosqlRepositoryFactory extends RepositoryFactorySupport {

    private final ApplicationContext applicationContext;
    private final NosqlOperations nosqlOperations;

    public NosqlRepositoryFactory(NosqlOperations nosqlOperations,
        ApplicationContext applicationContext)
    {
        this.nosqlOperations = nosqlOperations;
        this.applicationContext = applicationContext;
    }

    @Override
    protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {
        return SimpleNosqlRepository.class;
    }

    @Override
    protected Object getTargetRepository(RepositoryInformation information) {
        final EntityInformation<?, Serializable> entityInformation =
            getEntityInformation(information.getDomainType());
        return getTargetRepositoryViaReflection(information, entityInformation,
            applicationContext);
    }

    @Override
    public <T, ID> EntityInformation<T, ID> getEntityInformation(
        Class<T> domainClass) {
        return new NosqlEntityInformation<>(applicationContext, domainClass);
    }

    @Override
    protected Optional<QueryLookupStrategy> getQueryLookupStrategy(
        QueryLookupStrategy.Key key,
        QueryMethodEvaluationContextProvider evaluationContextProvider) {
        return Optional.of(
            new NosqlDbQueryLookupStrategy(applicationContext, nosqlOperations,
                evaluationContextProvider));
    }

    private static class NosqlDbQueryLookupStrategy
        implements QueryLookupStrategy {
        private final ApplicationContext applicationContext;
        private final NosqlOperations dbOperations;
        private final QueryMethodEvaluationContextProvider
            evaluationContextProvider;

        public NosqlDbQueryLookupStrategy(
            ApplicationContext applicationContext,
            NosqlOperations operations,
            QueryMethodEvaluationContextProvider provider) {
            this.applicationContext = applicationContext;
            this.dbOperations = operations;
            this.evaluationContextProvider = provider;
        }

        @Override
        public RepositoryQuery resolveQuery(Method method,
            RepositoryMetadata metadata,
            ProjectionFactory factory, NamedQueries namedQueries) {

            final NosqlQueryMethod queryMethod =
                new NosqlQueryMethod(applicationContext, method, metadata, factory);
            String namedQueryName = queryMethod.getNamedQueryName();

            Assert.notNull(queryMethod, "queryMethod must not be null!");
            Assert.notNull(dbOperations, "dbOperations must not be null!");

            if (namedQueries.hasQuery(namedQueryName)) {
                String namedQuery = namedQueries.getQuery(namedQueryName);
                return new StringBasedNosqlQuery(namedQuery, queryMethod,
                    dbOperations, evaluationContextProvider);
            } else if (queryMethod.hasAnnotatedQuery()) {
                return new StringBasedNosqlQuery(queryMethod, dbOperations,
                    evaluationContextProvider);
            } else {
                return new PartTreeNosqlQuery(queryMethod, dbOperations);
            }
        }
    }
}
