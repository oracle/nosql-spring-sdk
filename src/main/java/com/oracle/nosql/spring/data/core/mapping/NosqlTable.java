/*-
 * Copyright (c) 2020, 2024 Oracle and/or its affiliates.  All rights reserved.
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
import oracle.nosql.driver.Durability;
import oracle.nosql.driver.ops.TableLimits;

import com.oracle.nosql.spring.data.Constants;
import com.oracle.nosql.spring.data.config.NosqlDbConfig;
import com.oracle.nosql.spring.data.repository.NosqlRepository;

import org.springframework.data.annotation.Persistent;

/**
 * Optional annotation used to set options regarding the table used for entity.
 * <p>
 * If annotation is not explicitly used the {@link TableLimits} are determined
 * by {@link NosqlDbConfig#getDefaultCapacityMode()},
 * {@link NosqlDbConfig#getDefaultStorageGB()},
 * {@link NosqlDbConfig#getDefaultReadUnits()}, and
 * {@link NosqlDbConfig#getDefaultWriteUnits()}.
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
     * Sets the capacity mode when table is created. This applies only in cloud
     * or cloud sim scenarios.<p>
     *
     * For {@link TableLimits} to be set one of the following two
     * conditions must be met:<ul><li>
     *   capacityMode is set to PROVISIONED and all three: readUnits,
     *     writeUnits and storageGB must be greater than 0,</li><li>
     *   or capacityMode is set to ON_DEMAND and storageGB must be greater than
     * 0.</li></ul><p>
     *
     * If not set the default value is {@link NosqlCapacityMode#PROVISIONED}.
     * @since 1.3.0
     */
    NosqlCapacityMode capacityMode() default NosqlCapacityMode.PROVISIONED;

    /**
     * Sets the read units when table is created. Valid values are only
     * values greater than 0. This property applies only in cloud or cloud
     * sim scenarios and capacity mode is {@link NosqlCapacityMode#PROVISIONED}.
     * If not set the value {@link NosqlDbConfig#getDefaultReadUnits()} is used.
     * All three: readUnits, writeUnits and storageGB must be greater than 0 to
     * set a valid {@link TableLimits}.
     */
    int readUnits() default Constants.NOTSET_TABLE_READ_UNITS;

    /**
     * Sets the write units when table is created. Valid values are only
     * values greater than 0. This applies only in cloud or cloud sim scenarios
     * and capacity mode is {@link NosqlCapacityMode#PROVISIONED}.
     * If not set the value {@link NosqlDbConfig#getDefaultWriteUnits()} is
     * used. All three: readUnits, writeUnits and storageGB must be greater than
     * 0 to set a valid {@link TableLimits}.
     */
    int writeUnits() default Constants.NOTSET_TABLE_WRITE_UNITS;

    /**
     * Sets the storageGB when table is created. This applies only in cloud or
     * cloud sim scenarios.<p>
     *
     * If not set, the value of {@link NosqlDbConfig#getDefaultStorageGB()} is
     * used.<p>
     *
     * A 0 or less than -1 value will force no table limits, but they are
     * required in cloud and cloudsim instalations.<p>
     *
     * For {@link TableLimits} to be set one of the following two
     * conditions must be met:<ul><li>
     *   capacityMode is set to PROVISIONED and all three: readUnits,
     *      writeUnits and storageGB must be greater than 0,</li><li>
     *   or capacityMode is set to ON_DEMAND and storageGB must be greater than
     *      0.</li></ul>
     */
    int storageGB() default Constants.NOTSET_TABLE_STORAGE_GB;

    /**
     * Sets the default consistency for all read operations applied to this
     * table. Valid values for this are defined in {@link Consistency}.
     * If not set the default value for this is EVENTUAL.
     */
    String consistency() default Constants.DEFAULT_TABLE_CONSISTENCY;

    /**
     * Sets the default durability for all write operations
     * applied to this table. Valid values for this are defined in
     * {@link Durability}. If not set the default value for this is
     * COMMIT_NO_SYNC.<p>
     *
     * Note: This applies to On-Prem installations only.
     */
    String durability() default Constants.DEFAULT_TABLE_DURABILITY;

    /**
     * Sets the default timeout in milliseconds for all operations applied on
     * this table. If not set the default value is 0 which means it is ignored.
     * Note: {@link NosqlRepository#setTimeout(int)} take precedence over
     * the set here.
     */
    int timeout() default Constants.NOTSET_TABLE_TIMEOUT_MS;

    /**
     * Sets the default table level Time to Live (TTL) when table is created.
     * TTL allows automatic expiration of table rows when specified duration of
     * time is elapsed. If not set the value
     * {@link com.oracle.nosql.spring.data.Constants#NOTSET_TABLE_TTL} is used,
     * which means no table level TTL. This is applicable only when
     * {@link #autoCreateTable} is set to true.
     *
     * @since 1.5.0
     * @see oracle.nosql.driver.TimeToLive
     * @see <a href="https://docs.oracle.com/en/database/other-databases/nosql-database/22.3/java-driver-table/using-time-live.html">Using TTL</a>
     */
    int ttl() default Constants.NOTSET_TABLE_TTL;

    enum TtlUnit {
        DAYS,
        HOURS
    }

    /**
     * Sets the unit of TTL value. If not set the default value is days. This
     * is applicable only when {@link #autoCreateTable} is set to true.
     *
     * @since 1.5.0
     * @see oracle.nosql.driver.TimeToLive
     * @see <a href="https://docs.oracle.com/en/database/other-databases/nosql-database/22.3/java-driver-table/using-time-live.html#GUID-A768A8F9-309A-4018-8CC3-D2D6B8793C59">Using TTL</a>
     */
    TtlUnit ttlUnit() default TtlUnit.DAYS;

}
