/*-
 * Copyright (c) 2020, 2022 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */
package com.oracle.nosql.spring.data.core;

import oracle.nosql.driver.values.MapValue;

import com.oracle.nosql.spring.data.NosqlDbFactory;
import com.oracle.nosql.spring.data.core.convert.MappingNosqlConverter;
import com.oracle.nosql.spring.data.core.query.NosqlQuery;
import com.oracle.nosql.spring.data.repository.support.NosqlEntityInformation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public interface NosqlOperations {

    /**
     * Creates a table for the given entity type if it doesn't exist.
     */
    boolean createTableIfNotExists(NosqlEntityInformation<?, ?> information);

    /**
     * Drops the given table, information about all saved entities is
     * removed.
     * Returns true if result indicates table state changed to DROPPED or
     * DROPPING.
     * Uses {@link NosqlDbFactory#getTableReqTimeout()} and
     * {@link NosqlDbFactory#getTableReqPollInterval()} to check the result.
     */
    boolean dropTableIfExists(String tableName);

    /**
     * Returns the assigned mapping converter.
     */
    MappingNosqlConverter getConverter();

    /**
     * Returns the tableName for the given entity class.
     */
    String getTableName(Class<?> entityClass);

    /**
     * Inserts the entity into the table, if id generated is used the id
     * field must be null or 0.
     */
    <T> T insert(T objectToSave);

    /**
     * Inserts the entity into the given table, if id generated is used the id
     * field must be null or 0.
     */
    <T, ID> T  insert(NosqlEntityInformation<T, ID> entityInformation,
        T objectToSave);

    /**
     * Updates the entity into the table. Entity must contain a valid id
     * value.
     */
    <T> void update(T object);

    /**
     * Updates the entity into the given table. Entity must contain a valid id
     * value.
     */
    <T, ID> void update(NosqlEntityInformation<T, ID> entityInformation,
        T object);

    /**
     * Returns a result of all the entities in the table. Not recommended,
     * unless table is known to contain a small amount of rows. Instead use
     * the method with Pageable parameter.
     */
    <T> Iterable<T> findAll(Class<T> entityClass);

    /**
     * Returns a result of all the entities in the given table. Not
     * recommended, unless table is known to contain a small amount of rows.
     * Instead use the method with Pageable parameter.
     */
    <T> Iterable<T> findAll(NosqlEntityInformation<T, ?> entityInformation);

    /**
     * Returns all the entities in the table for the given ids.
     */
    <T, ID> Iterable<T> findAllById(
        NosqlEntityInformation<T, ID> entityInformation,
        Iterable<ID> ids);

    /**
     * Returns the entity for the given id in the given table.
     */
    <T, ID> T findById(NosqlEntityInformation<T, ID> entityInformation, ID id);

    /**
     * Returns the entity for the given table.
     */
    <T, ID> T findById(ID id, Class<T> entityClass);

    /**
     * Deletes the entity with the id from the given table.
     */
    <T, ID> void deleteById(NosqlEntityInformation<T, ID> entityInformation,
        ID id);

    /**
     * Deletes all entries from the given table.
     */
    void deleteAll(NosqlEntityInformation<?, ?> entityInformation);

    /**
     * Deletes all the entries with the ids from the given table.
     */
    <T, ID> void deleteAll(NosqlEntityInformation<T, ID> entityInformation,
        Iterable<? extends ID> ids);

    /**
     * Returns a count of all the entries in the given table.
     */
    long count(NosqlEntityInformation<?, ?> entityInformation);

    /**
     *  Returns all entities in the given table sorted accordingly.
     */
    <T> Iterable<T> findAll(NosqlEntityInformation<T, ?> entityInformation,
        Sort sort);

    /**
     *  Returns all entities in the given table sorted accordingly grouped in
     *  pages. */
    <T> Page<T> findAll(NosqlEntityInformation<T, ?> entityInformation,
        Pageable pageable);

    /**
     * Executes a NosqlQuery (this can be a CriteriaQuery for queries
     * derived from repository method names or a StringQuery for native
     * queries).
     */
    <S, T> Iterable<T> find(NosqlEntityInformation<S, ?> entityInformation,
            Class<T> targetType, NosqlQuery query);

    <T> Iterable<MapValue> count(
        NosqlEntityInformation<T, ?> entityInformation, NosqlQuery query);

    <T, ID> Iterable<T> delete(NosqlEntityInformation<T, ID> entityInformation,
        NosqlQuery query);
}