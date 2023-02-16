/*-
 * Copyright (c) 2020, 2022 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */
package com.oracle.nosql.spring.data.repository.query;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;

import com.oracle.nosql.spring.data.repository.Query;
import com.oracle.nosql.spring.data.repository.support.NosqlEntityInformation;

import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.EntityMetadata;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.lang.Nullable;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.StringUtils;

public class NosqlQueryMethod extends QueryMethod {

    private final Method method;
    private ApplicationContext applicationContext;
    private final Map<Class<? extends Annotation>, Optional<Annotation>>
        annotationCache;
    private NosqlEntityMetadata<?> metadata;

    public NosqlQueryMethod(ApplicationContext applicationContext,
        Method method, RepositoryMetadata metadata, ProjectionFactory factory) {

        super(method, metadata, factory);

        this.method = method;
        this.applicationContext = applicationContext;
        this.annotationCache = new ConcurrentReferenceHashMap<>();
    }

    @Override
    @SuppressWarnings("unchecked")
    public EntityMetadata<?> getEntityInformation() {
        final Class<Object> domainClass = (Class<Object>) getDomainClass();
        final NosqlEntityInformation<Object, String> entityInformation =
            new NosqlEntityInformation<>(applicationContext, domainClass);

        this.metadata = new SimpleNosqlEntityMetadata<>(domainClass, entityInformation);
        return this.metadata;
    }

    /**
     * Returns whether the method has an annotated query.
     */
    public boolean hasAnnotatedQuery() {
        return findAnnotatedQuery().isPresent();
    }

    /**
     * Returns the query string declared in a {@link Query} annotation or
     * {@literal null} if neither the annotation found nor the attribute was
     * specified.
     */
    @Nullable
    String getAnnotatedQuery() {
        return findAnnotatedQuery().orElse(null);
    }

    private Optional<String> findAnnotatedQuery() {

        return lookupQueryAnnotation() //
            .map(Query::value) //
            .filter(StringUtils::hasText);
    }

    /**
     * Returns the {@link Query} annotation that is applied to the method or
     * {@code null} if none available.
     */
    @Nullable
    Query getQueryAnnotation() {
        return lookupQueryAnnotation().orElse(null);
    }

    Optional<Query> lookupQueryAnnotation() {
        return doFindAnnotation(Query.class);
    }

    @SuppressWarnings("unchecked")
    private <A extends Annotation> Optional<A> doFindAnnotation(
        Class<A> annotationType) {

        return (Optional<A>) this.annotationCache.computeIfAbsent(annotationType,
            it -> Optional.ofNullable(
                AnnotatedElementUtils.findMergedAnnotation(method, it)));
    }
}
