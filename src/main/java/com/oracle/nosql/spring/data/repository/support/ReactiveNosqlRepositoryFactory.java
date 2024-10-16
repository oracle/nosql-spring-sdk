/*-
 * Copyright (c) 2020, 2024 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */
package com.oracle.nosql.spring.data.repository.support;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Optional;

import com.oracle.nosql.spring.data.core.ReactiveNosqlOperations;
import com.oracle.nosql.spring.data.repository.query.NosqlQueryMethod;
import com.oracle.nosql.spring.data.repository.query.PartTreeReactiveNosqlQuery;

import com.oracle.nosql.spring.data.repository.query.ReactiveStringBasedNosqlQuery;
import org.springframework.context.ApplicationContext;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.ReactiveRepositoryFactorySupport;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.util.Assert;

public class ReactiveNosqlRepositoryFactory  extends
    ReactiveRepositoryFactorySupport {

    private final ApplicationContext applicationContext;
    private final ReactiveNosqlOperations reactiveNosqlOperations;

    public ReactiveNosqlRepositoryFactory(ReactiveNosqlOperations nosqlOperations,
        ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        this.reactiveNosqlOperations = nosqlOperations;
    }

    @Override
    public <T, ID> EntityInformation<T, ID> getEntityInformation(Class<T> domainClass) {
        return new NosqlEntityInformation<>(applicationContext, domainClass);
    }

    @Override
    protected Object getTargetRepository(RepositoryInformation information) {
        final EntityInformation<?, Serializable> entityInformation =
            getEntityInformation(information.getDomainType());
        return getTargetRepositoryViaReflection(information, entityInformation,
            this.applicationContext);
    }

    @Override
    protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {
        return SimpleReactiveNosqlRepository.class;
    }

    @Override
    protected Optional<QueryLookupStrategy> getQueryLookupStrategy(
        QueryLookupStrategy.Key key,
        QueryMethodEvaluationContextProvider evaluationContextProvider) {
        return Optional.of(new ReactiveNosqlQueryLookupStrategy(
            applicationContext,
            reactiveNosqlOperations,
            evaluationContextProvider));
    }

    private static class ReactiveNosqlQueryLookupStrategy implements QueryLookupStrategy {
        private final ApplicationContext applicationContext;
        private final ReactiveNosqlOperations nosqlOperations;
        private final QueryMethodEvaluationContextProvider
                evaluationContextProvider;

        public ReactiveNosqlQueryLookupStrategy(
            ApplicationContext applicationContext,
            ReactiveNosqlOperations operations,
            QueryMethodEvaluationContextProvider provider) {
            this.applicationContext = applicationContext;
            this.nosqlOperations = operations;
            this.evaluationContextProvider = provider;
        }

        @Override
        public RepositoryQuery resolveQuery(Method method, RepositoryMetadata metadata,
            ProjectionFactory factory, NamedQueries namedQueries) {

            final NosqlQueryMethod queryMethod =
                new NosqlQueryMethod(applicationContext, method, metadata, factory);

            Assert.notNull(queryMethod, "queryMethod must not be null!");
            Assert.notNull(nosqlOperations, "dbOperations must not be null!");

            String namedQueryName = queryMethod.getNamedQueryName();
            if (namedQueries.hasQuery(namedQueryName)) {
                String namedQuery = namedQueries.getQuery(namedQueryName);
                return new ReactiveStringBasedNosqlQuery(namedQuery, queryMethod,
                        nosqlOperations, evaluationContextProvider);
            } else if (queryMethod.hasAnnotatedQuery()) {
                return new ReactiveStringBasedNosqlQuery(queryMethod, nosqlOperations,
                        evaluationContextProvider);
            } else {
                return new PartTreeReactiveNosqlQuery(queryMethod, nosqlOperations);
            }
        }
    }
}
