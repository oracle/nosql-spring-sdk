/*-
 * Copyright (c) 2020, 2021 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */
package com.oracle.nosql.spring.data.repository;

import oracle.nosql.driver.Consistency;

import com.oracle.nosql.spring.data.core.mapping.NosqlTable;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.reactive.ReactiveSortingRepository;

@NoRepositoryBean
public interface ReactiveNosqlRepository <T, K>
    extends ReactiveSortingRepository<T, K> {

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
}
