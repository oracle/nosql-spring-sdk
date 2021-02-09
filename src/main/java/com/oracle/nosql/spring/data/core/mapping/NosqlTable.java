/*-
 * Copyright (c) 2020, 2021 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */
package com.oracle.nosql.spring.data.core.mapping;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import oracle.nosql.driver.Consistency;
import oracle.nosql.driver.ops.TableLimits;

import com.oracle.nosql.spring.data.Constants;
import com.oracle.nosql.spring.data.repository.NosqlRepository;

import org.springframework.data.annotation.Persistent;

/**
 * Annotation used to set options regarding the table used for entity.
 */
@Persistent
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface NosqlTable {

    /**
     * Sets the name of the table to be used for this entity. If not set or
     * set to "", the {@link Class#getSimpleName()} is used.
     */
    String tableName() default Constants.DEFAULT_TABLE_NAME;

    /**
     * Flag that indicates if automatic creation of table is enabled. By
     * default this is set to {@link Constants#DEFAULT_AUTO_CREATE_TABLE}.
     */
    boolean autoCreateTable() default Constants.DEFAULT_AUTO_CREATE_TABLE;

    /**
     * Sets the read units when table is created. Valid values are only
     * values greater than 0. This applies only in cloud or cloud
     * sim scenarios.
     * If not set the default value is -1, which means that there will
     * be no table limit set. All three: readUnits, writeUnits and storageGB
     * must be greater than 0 to set a valid {@link TableLimits}.
     */
    int readUnits() default Constants.NOTSET_TABLE_READ_UNITS;

    /**
     * Sets the write units when table is created. Valid values are only
     * values greater than 0. This applies only in cloud or cloud sim scenarios.
     * If not set the default value is -1, which means that there will
     * be no table limit set. All three: readUnits, writeUnits and storageGB
     * must be greater than 0 to set a valid {@link TableLimits}.
     */
    int writeUnits() default Constants.NOTSET_TABLE_WRITE_UNITS;

    /**
     * Sets the storage units when table is created. Valid values are only
     * values greater than 0. This applies only in cloud or cloud sim scenarios.
     * If not set the default value is -1, which means that there will
     * be no table limit set. All three: readUnits, writeUnits and storageGB
     * must be greater than 0 to set a valid {@link TableLimits}.
     */
    int storageGB() default Constants.NOTSET_TABLE_STORAGE_GB;

    /**
     * Sets the default consistency for all read operations applied to this
     * table. Valid values for this are defined in {@link Consistency}.
     * If not set the default value for this is EVENTUAL.
     */
    String consistency() default Constants.DEFAULT_TABLE_CONSISTENCY;

    /**
     * Sets the default timeout in milliseconds for all operations applied on
     * this table.
     * If not set the default value is 0 which means it is ignored.
     * Note: {@link NosqlRepository#setTimeout(int)} take precedence over
     * the set here.
     */
    int timeout() default Constants.NOTSET_TABLE_TIMEOUT_MS;
}
