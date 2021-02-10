/*-
 * Copyright (c) 2020, 2021 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */
package com.oracle.nosql.spring.data.config;

import java.io.IOException;

import oracle.nosql.driver.AuthorizationProvider;
import oracle.nosql.driver.NoSQLHandleConfig;
import oracle.nosql.driver.iam.SignatureProvider;
import oracle.nosql.driver.kv.StoreAccessTokenProvider;

import com.oracle.nosql.spring.data.Constants;
import com.oracle.nosql.spring.data.NosqlDbFactory;

public class NosqlDbConfig {

    private NoSQLHandleConfig nosqlHandleConfig;
    private int queryCacheCapacity = Constants.DEFAULT_QUERY_CACHE_CAPACITY;
    private int queryCacheLifetime = Constants.DEFAULT_QUERY_CACHE_LIFETIME_MS;
    private int tableReqTimeout = Constants.DEFAULT_TABLE_REQ_TIMEOUT_MS;
    private int tableReqPollInterval = Constants.DEFAULT_TABLE_REQ_POLL_INTEVEL_MS;
    private int timestampPrecision = Constants.DEFAULT_TIMESTAMP_PRECISION;


    public NosqlDbConfig(String endpoint,
        AuthorizationProvider authorizationProvider) {
        this(new NoSQLHandleConfig(endpoint).setAuthorizationProvider(authorizationProvider));
    }

    public NosqlDbConfig(NoSQLHandleConfig nosqlHandleConfig) {
        this.nosqlHandleConfig = nosqlHandleConfig;
    }

    public static NosqlDbConfig createCloudConfig(String endpoint,
        String configFile)
        throws IOException
    {
        NosqlDbConfig config = new NosqlDbConfig(endpoint,
            new SignatureProvider(configFile, "DEFAULT"));
        config.getNosqlHandleConfig().setRateLimitingEnabled(true);
        return config;
    }

    public static NosqlDbConfig createCloudSimConfig(String endpoint) {
        NosqlDbConfig config = new NosqlDbConfig(endpoint,
            NosqlDbFactory.CloudSimProvider.getProvider());
        config.getNosqlHandleConfig().setRateLimitingEnabled(true);
        return config;
    }

    public static NosqlDbConfig createProxyConfig(String endpoint) {
        return new NosqlDbConfig(endpoint, new StoreAccessTokenProvider());
    }

    public static NosqlDbConfig createProxyConfig(String endpoint,
        String user, char[] password) {
        return new NosqlDbConfig(endpoint,
                new StoreAccessTokenProvider(user, password));
    }

    public NoSQLHandleConfig getNosqlHandleConfig() {
        return nosqlHandleConfig;
    }

    /**
     * Returns the capacity of the prepared query cache. By default this is set
     * to {@link Constants#DEFAULT_QUERY_CACHE_CAPACITY}.
     */
    public int getQueryCacheCapacity() {
        return queryCacheCapacity;
    }

    /**
     * Sets the capacity of the prepared query cache. By default this is set
     * to {@link Constants#DEFAULT_QUERY_CACHE_CAPACITY}. The prepared query
     * cache is controlled by both {@link #setQueryCacheCapacity(int)} and
     * {@link #setQueryCacheLifetime(int)}. If capacity is 0 then entries are
     * only ever removed because they expire. If lifetime is 0 then entries are
     * only ever removed because the cache has reached capacity.
     */
    NosqlDbConfig setQueryCacheCapacity(int capacity) {
        queryCacheCapacity = capacity;
        return this;
    }

    /**
     * Returns the lifetime of the prepared query cache in milliseconds. By
     * default this is set
     * to {@link Constants#DEFAULT_QUERY_CACHE_LIFETIME_MS}.
     */
    public int getQueryCacheLifetime() {
        return queryCacheLifetime;
    }

    /**
     * Sets the lifetime of the prepared query cache in milliseconds. By
     * default this is set to {@link Constants#DEFAULT_QUERY_CACHE_LIFETIME_MS}.
     * The prepared query cache is controlled by both
     * {@link #setQueryCacheCapacity(int)} and
     * {@link #setQueryCacheLifetime(int)}. If capacity is 0 then entries are
     * only ever removed because they expire. If lifetime is 0 then entries are
     * only ever removed because the cache has reached capacity.
     */
    NosqlDbConfig setQueryCacheLifetime(int lifetime) {
        queryCacheCapacity = lifetime;
        return this;
    }

    /** Returns the table request timeout in milliseconds. By default this is
     * set to {@link Constants#DEFAULT_TABLE_REQ_TIMEOUT_MS}
     */
    public int getTableReqTimeout() {
        return tableReqTimeout;
    }

    /** Sets the table request timeout in milliseconds. By default this is
     * set to {@link Constants#DEFAULT_TABLE_REQ_TIMEOUT_MS}
     */
    public NosqlDbConfig setTableReqTimeout(int tableReqTimeout) {
        this.tableReqTimeout = tableReqTimeout;
        return this;
    }

    /** Returns the table request poll interval in milliseconds. By default this
     *  is set to {@link Constants#DEFAULT_TABLE_REQ_POLL_INTEVEL_MS}
     */
    public int getTableReqPollInterval() {
        return tableReqPollInterval;
    }

    /** Sets the table request poll interval in milliseconds. By default
     * this is set to {@link Constants#DEFAULT_TABLE_REQ_POLL_INTEVEL_MS}
     */
    public NosqlDbConfig setTableReqPollInterval(int pollInterval) {
        this.tableReqPollInterval = pollInterval;
        return this;
    }

    /**
     * Returns the precision of the Timestamp NoSQL DB type when creating a
     * new table. By default this is set to
     * {@link Constants#DEFAULT_TIMESTAMP_PRECISION}.
     * <br>
     * Timestamp values have a precision (0 - 9) which represents the fractional
     * seconds to be held by the timestamp. A value of 0 means that no
     * fractional seconds are stored, 3 means that the timestamp stores
     * milliseconds, and 9 means a precision of nanoseconds.
     * <br>
     * In the context of a CREATE TABLE statement, a precision must be
     * explicitly specified. This restriction is to prevent users from
     * inadvertently creating TIMESTAMP values with precision 9 (which takes
     * more space) when in reality they don't need that high precision.
     * <br>
     * See <a href="https://docs.oracle.com/en/database/other-databases/nosql-database/20.2/sqlreferencefornosql/data-type-definitions.html">Timestamp documentation</a> for more details.
     */
    public int getTimestampPrecision() {
        return timestampPrecision;
    }

    /**
     * Sets the precision of the Timestamp NoSQL DB type when creating a
     * new table. By default this is set to
     * {@link Constants#DEFAULT_TIMESTAMP_PRECISION}.
     * <br>
     * Timestamp values have a precision (0 - 9) which represents the fractional
     * seconds to be held by the timestamp. A value of 0 means that no
     * fractional seconds are stored, 3 means that the timestamp stores
     * milliseconds, and 9 means a precision of nanoseconds.
     * <br>
     * In the context of a CREATE TABLE statement, a precision must be
     * explicitly specified. This restriction is to prevent users from
     * inadvertently creating TIMESTAMP values with precision 9 (which takes
     * more space) when in reality they don't need that high precision.
     * <br>
     * See <a href="https://docs.oracle.com/en/database/other-databases/nosql-database/20.2/sqlreferencefornosql/data-type-definitions.html">Timestamp documentation</a> for more details.
     */
    public NosqlDbConfig setTimestampPrecision(int precision) {
        timestampPrecision = precision;
        return this;
    }
}
