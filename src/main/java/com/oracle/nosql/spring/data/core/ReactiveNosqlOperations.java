/*-
 * Copyright (c) 2020, 2025 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */
package com.oracle.nosql.spring.data.core;

import com.oracle.nosql.spring.data.NosqlDbFactory;
import com.oracle.nosql.spring.data.core.convert.MappingNosqlConverter;
import com.oracle.nosql.spring.data.core.query.NosqlQuery;
import com.oracle.nosql.spring.data.repository.support.NosqlEntityInformation;

import org.reactivestreams.Publisher;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ReactiveNosqlOperations {

    /**
     * Returns the table name associated to the domainClass
     * @param domainClass the domain class
     * @return the table name
     */
    String getTableName(Class<?> domainClass);

    /**
     * Creates table for entity information.
     * Uses {@link NosqlDbFactory#getTableReqTimeout()} and
     * {@link NosqlDbFactory#getTableReqPollInterval()} to check the result.
     * Throws @{@link RuntimeException} if result indicates table state
     * different than ACTIVE.
     */
    Mono<Boolean> createTableIfNotExists(
        NosqlEntityInformation<?, ?> entityInformation);

    /**
     * Drops table and returns true if result indicates table state changed to
     * DROPPED or DROPPING.
     * Uses {@link NosqlDbFactory#getTableReqTimeout()} and
     * {@link NosqlDbFactory#getTableReqPollInterval()} to check the result.
     */
    Mono<Boolean> dropTableIfExists(String tableName);

    /**
     * Clears the cache of prepared statements.
     */
    void clearPreparedStatementsCache();


    <T, ID> Flux<T> findAll(NosqlEntityInformation<T, ID> entityInformation);

    <T> Flux<T> findAll(Class<T> entityClass);

    <T> Mono<T> findById(Object id, Class<T> entityClass);

    <T, ID> Mono<T> findById(NosqlEntityInformation<T, ID> entityInformation,
        ID id);

    <T, ID> Flux<T> findAllById(NosqlEntityInformation<T, ID> entityInformation,
        Publisher<ID> idStream);

    <T> Mono<T> insert(T entity);

    <T, ID> Mono<T> insert(NosqlEntityInformation<?, ID> entityInformation,
        T entity);

    <T> Mono<T> update(T entity);

    <T, ID> Mono<T> update(NosqlEntityInformation<?, ID> entityInformation,
        T entity);

    <ID> Mono<Void> deleteById(NosqlEntityInformation<?, ID> entityInformation,
        ID id);

    Mono<Void> deleteAll(NosqlEntityInformation<?, ?> entityInformation);

    <T, ID> Flux<T> delete(NosqlQuery query,
        NosqlEntityInformation<T, ID> entityInformation);

    <T> Flux<T> find(NosqlQuery query,
        NosqlEntityInformation<T, ?> entityInformation);

    Mono<Boolean> exists(NosqlQuery query,
        NosqlEntityInformation<?, ?> entityInformation);

    <ID> Mono<Boolean> existsById(
        NosqlEntityInformation<?, ID> entityInformation, ID id);

    Mono<Long> count(NosqlEntityInformation<?, ?> entityInformation);

    Mono<Long> count(NosqlQuery query,
        NosqlEntityInformation<?, ?> entityInformation);

    MappingNosqlConverter getConverter();
}
