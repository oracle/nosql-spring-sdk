/*-
 * Copyright (c) 2020, 2023 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */
package com.oracle.nosql.spring.data.repository;

import java.io.Serializable;
import java.util.Optional;

import oracle.nosql.driver.Consistency;
import oracle.nosql.driver.Durability;

import com.oracle.nosql.spring.data.core.mapping.NosqlTable;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.PagingAndSortingRepository;

@NoRepositoryBean
public interface NosqlRepository<T, ID extends Serializable> extends
    PagingAndSortingRepository<T, ID>,
    CrudRepository<T, ID> {

    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.CrudRepository#findAll()
     */
    @Override
    Iterable<T> findAll();


    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.CrudRepository#save()
     */
    @Override
    <S extends T> S save(S entity);

    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.CrudRepository#saveAll()
     */
    @Override
    <S extends T> Iterable<S> saveAll(Iterable<S> entities);

    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.CrudRepository#findById()
     */
    @Override
    Optional<T> findById(ID id);

    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.CrudRepository#existsById()
     */
    @Override
    boolean existsById(ID id);

    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.CrudRepository#findAllById()
     */
    @Override
    Iterable<T> findAllById(Iterable<ID> ids);

    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.CrudRepository#count()
     */
    @Override
    long count();

    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.CrudRepository#deleteById()
     */
    @Override
    void deleteById(ID id);

    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.CrudRepository#delete()
     */
    @Override
    void delete(T entity);

    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.CrudRepository#deleteAll()
     */
    @Override
    void deleteAll(Iterable<? extends T> entities);

    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository.CrudRepository#deleteAll()
     */
    @Override
    void deleteAll();

    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository
     * .PagingAndSortingRepository#findAll(Sort sort)
     */
    @Override
    Iterable<T> findAll(Sort sort);

    /*
     * (non-Javadoc)
     * @see org.springframework.data.repository
     * .PagingAndSortingRepository#findAll(Pageable pageable)
     */
    @Override
    Page<T> findAll(Pageable pageable);

    /**
     * Returns the configured request timeout value, in milliseconds, or 0 if
     * it has not been set.
     */
    int getTimeout();

    /**
     * Sets the request timeout value, in milliseconds. This overrides any
     * default value set in NoSQLHandleConfig. The value must be positive.
     * This set takes precedence over the one set when using
     * {@link NosqlTable#timeout()}.
     */
    void setTimeout(int milliseconds);

    /**
     * Returns the configured read request consistency value.
     */
    String getConsistency();

    /**
     * Sets the read request consistency value. The value must be one of
     * {@link Consistency} values.
     * This set takes precedence over the one set when using
     * {@link NosqlTable#consistency()}.
     */
    void setConsistency(String consistency);

    /**
     * Returns the configured request durability value.
     */
    String getDurability();

    /**
     * Sets the request durability value. The value must be one of the defined
     * Durability values: {@link Durability#COMMIT_NO_SYNC},
     * {@link Durability#COMMIT_WRITE_NO_SYNC} or
     * {@link Durability#COMMIT_SYNC}.<p>
     * This set takes precedence over the one set when using
     * {@link NosqlTable#durability()}.<p>
     * If null or invalid value is provided durability will be set to
     * {@link Durability#COMMIT_NO_SYNC}<p>
     * Note: This applies to On-Prem installations only.
     */
    void setDurability(String durability);
}