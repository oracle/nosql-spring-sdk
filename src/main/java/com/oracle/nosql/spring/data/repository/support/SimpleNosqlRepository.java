/*-
 * Copyright (c) 2020, 2025 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */
package com.oracle.nosql.spring.data.repository.support;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import oracle.nosql.driver.Durability;

import com.oracle.nosql.spring.data.core.NosqlOperations;
import com.oracle.nosql.spring.data.repository.NosqlRepository;

import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

public class SimpleNosqlRepository <T, ID extends Serializable>
    implements NosqlRepository<T, ID> {

    private final NosqlOperations operation;
    private final NosqlEntityInformation<T, ID> entityInformation;

    public SimpleNosqlRepository(NosqlEntityInformation<T, ID> entityInformation,
        ApplicationContext applicationContext) {
        Assert.notNull(applicationContext, "ApplicationContext must not be " +
            "null.");
        this.operation = applicationContext.getBean(NosqlOperations.class);
        this.entityInformation = entityInformation;

        if (this.entityInformation.isAutoCreateTable()) {
            createTableIfNotExists();
        }
    }

    public SimpleNosqlRepository(NosqlEntityInformation<T, ID> metadata,
        NosqlOperations dbOperations) {
        Assert.notNull(dbOperations, "NosqlOperations must not be null.");
        this.operation = dbOperations;
        this.entityInformation = metadata;

        if (this.entityInformation.isAutoCreateTable()) {
            createTableIfNotExists();
        }
    }

    private void createTableIfNotExists() {
        this.operation.createTableIfNotExists(this.entityInformation);
    }

    /**
     * Save entity.
     */
    @SuppressWarnings("unchecked")
    @Override
    public <S extends T> S save(S entity) {
        Assert.notNull(entity, "Entity must not be null.");

        if (entityInformation.isAutoGeneratedId()) {
            // Must use update if !new
            boolean isNew = entityInformation.isNew(entity);
            if (isNew) {
                return (S) operation.insert(entityInformation, entity);
            } else {
                operation.update(entityInformation, entity);
            }
        } else {
            // do a put (set or insert) if !isAutoGeneratedId
            return (S) operation.insert(entityInformation, entity);
        }

        return entity;
    }

    /**
     * Batch save entities.
     */
    @Override
    public <S extends T> Iterable<S> saveAll(Iterable<S> entities) {
        Assert.notNull(entities, "Iterable entities should not be null.");
//        long time = System.currentTimeMillis();
        entities.forEach(this::save);
//        time = System.currentTimeMillis() - time;
//        System.out.println("  saveAll took: " + time + " ms");
        return entities;
    }

    /**
     * Find all entities from the table.
     */
    @Override
    public Iterable<T> findAll() {
        return operation.findAll(entityInformation);
    }

    /**
     * Find entities based on id list.
     */
    @Override
    public Iterable<T> findAllById(Iterable<ID> ids) {
        Assert.notNull(ids, "Iterable ids should not be null");

        return operation.findAllById(entityInformation, ids);
    }

    /**
     * Find entity by its id.
     */
    @Override
    public Optional<T> findById(ID id) {
        Assert.notNull(id, "Id must not be null.");

        if (id instanceof String && !StringUtils.hasText((String) id)) {
            return Optional.empty();
        }

        return Optional.ofNullable(
            operation.findById(entityInformation, id));
    }

    /**
     * Return count of rows in the table.
     */
    @Override
    public long count() {
        return operation.count(entityInformation);
    }

    /**
     * Delete one row for id.
     */
    @Override
    public void deleteById(ID id) {
        Assert.notNull(id, "Id to be deleted should not be null.");

        operation.deleteById(entityInformation, id);
    }

    /**
     * Delete one row for entity.
     */
    @Override
    public void delete(T entity) {
        Assert.notNull(entity, "Entity to be deleted should not be null.");

        operation.deleteById(entityInformation,
            entityInformation.getId(entity));
    }

    @Override
    public void deleteAllById(Iterable<? extends ID> ids) {
        ids.forEach(this::deleteById);
    }

    /**
     * Delete all rows from table.
     */
    @Override
    public void deleteAll() {
        operation.deleteAll(entityInformation);
    }

    /**
     * Delete list of entities from table.
     */
    @Override
    public void deleteAll(Iterable<? extends T> entities) {
        Assert.notNull(entities, "Iterable entities should not be null.");

        List<ID> ids = StreamSupport.stream(entities.spliterator(), true)
            .map( e -> {
                Assert.notNull(e, "Entity can not be null.");
                return entityInformation.getId(e);
            })
            .collect(Collectors.toList());
        operation.deleteAll(entityInformation, ids);
    }

    /**
     * Check if an entity exists per id.
     */
    @Override
    public boolean existsById(ID primaryKey) {
        Assert.notNull(primaryKey, "PrimaryKey should not be null.");

        return findById(primaryKey).isPresent();
    }

    /**
     * Returns all entities sorted by the given options.
     */
    @Override
    public Iterable<T> findAll(Sort sort) {
        Assert.notNull(sort, "Sort of findAll should not be null.");

        return operation.findAll(entityInformation, sort);
    }

    /**
     * Returns a Page of entities meeting the paging restriction provided in the Pageable object.
     */
    @Override
    public Page<T> findAll(Pageable pageable) {
        Assert.notNull(pageable, "Pageable should not be null.");

        return operation.findAll(entityInformation, pageable);
    }

    /**
     * @see NosqlRepository#getTimeout()
     */
    @Override
    public int getTimeout() {
        return entityInformation.getTimeout();
    }

    /**
     * @see NosqlRepository#setTimeout(int)
     */
    @Override
    public void setTimeout(int milliseconds) {
        entityInformation.setTimeout(milliseconds);
    }

    /**
     * @see NosqlRepository#getConsistency()
     */
    @Override
    public String getConsistency() {
        return entityInformation.getConsistency().getType().name();
    }

    /**
     * @see NosqlRepository#setConsistency(String)
     */
    @Override
    public void setConsistency(String consistency) {
        entityInformation.setConsistency(consistency);
    }

    /**
     * @see NosqlRepository#getDurability()
     */
    @Override
    public String getDurability() {
        return convertDurability(entityInformation.getDurability());
    }

    static String convertDurability(Durability durability) {
        if (durability == Durability.COMMIT_NO_SYNC) {
            return "COMMIT_NO_SYNC";
        }
        if (durability == Durability.COMMIT_SYNC) {
            return "COMMIT_SYNC";
        }
        if (durability == Durability.COMMIT_WRITE_NO_SYNC) {
            return "COMMIT_WRITE_NO_SYNC";
        }

        return "COMMIT_NO_SYNC";
    }

    /**
     * @see NosqlRepository#setDurability(String)
     */
    @Override
    public void setDurability(String durability) {
        entityInformation.setDurability(durability);
    }


    /**
     * @see NosqlRepository#clearPreparedStatementsCache()
     */
    public void clearPreparedStatementsCache() {
        operation.clearPreparedStatementsCache();
    }
}
