/*-
 * Copyright (c) 2020, 2023 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */
package com.oracle.nosql.spring.data.config;

import java.io.IOException;

import oracle.nosql.driver.AuthorizationProvider;
import oracle.nosql.driver.NoSQLHandleConfig;
import oracle.nosql.driver.Region;
import oracle.nosql.driver.iam.SignatureProvider;
import oracle.nosql.driver.kv.StoreAccessTokenProvider;
import oracle.nosql.driver.ops.TableLimits;

import com.oracle.nosql.spring.data.Constants;
import com.oracle.nosql.spring.data.NosqlDbFactory;
import com.oracle.nosql.spring.data.core.mapping.NosqlCapacityMode;
import com.oracle.nosql.spring.data.core.mapping.NosqlTable;


public class NosqlDbConfig {

    private NoSQLHandleConfig nosqlHandleConfig;
    private int queryCacheCapacity = Constants.DEFAULT_QUERY_CACHE_CAPACITY;
    private int queryCacheLifetime = Constants.DEFAULT_QUERY_CACHE_LIFETIME_MS;
    private int tableReqTimeout = Constants.DEFAULT_TABLE_REQ_TIMEOUT_MS;
    private int tableReqPollInterval = Constants.DEFAULT_TABLE_REQ_POLL_INTEVEL_MS;
    private int timestampPrecision = Constants.DEFAULT_TIMESTAMP_PRECISION;
    private int defaultStorageGB = 25;
    private NosqlCapacityMode defaultCapacityMode = NosqlCapacityMode.PROVISIONED;
    private int defaultReadUnits = 50;
    private int defaultWriteUnits = 50;

    private String defaultNamespace = null;

    public NosqlDbConfig(String endpoint,
        AuthorizationProvider authorizationProvider) {
        this(new NoSQLHandleConfig(endpoint).setAuthorizationProvider(authorizationProvider));
    }

    public NosqlDbConfig(NoSQLHandleConfig nosqlHandleConfig) {
        this.nosqlHandleConfig = nosqlHandleConfig;
    }

    /**
     * Creates a NosqlDbConfig object based on endpoint and config file path.
     * Note: Will use the DEFAULT profile from config file.
     * Note: For use with NoSQL DB Cloud Service only.
     *
     * @param endpoint Endpoint of service.
     *                 Example: {@link Region#endpoint()}
     * @param configFile Path to config file. Example: "~/.oci/config"
     * @return returns the NosqlDbConfig object.
     * @throws IOException if config file cannot be accessed.
     */
    public static NosqlDbConfig createCloudConfig(String endpoint,
        String configFile)
        throws IOException
    {
        return createCloudConfig(endpoint, configFile, "DEFAULT");
    }

    /**
     * Creates a NosqlDbConfig object based on endpoint, config file path and
     * profile name.
     * Note: For use with NoSQL DB Cloud Service only.
     *
     * @param endpoint Endpoint of service.
     *                 Example: {@link Region#endpoint()}
     * @param configFile Path to config file. Example: "~/.oci/config"
     * @param profileName The name of the profile defined in config file.
     *                    Example: "DEFAULT".
     * @return returns the NosqlDbConfig object.
     * @throws IOException if config file cannot be accessed.
     */
    public static NosqlDbConfig createCloudConfig(String endpoint,
        String configFile, String profileName)
        throws IOException
    {
        NosqlDbConfig config = new NosqlDbConfig(endpoint,
            new SignatureProvider(configFile, profileName));
        config.getNosqlHandleConfig().setRateLimitingEnabled(true);
        return config;
    }

    /**
     * Creates a NosqlDbConfig object for a CloudSim server.
     *
     * @param endpoint Endpoint of service. Example: "http://localhost:8080"
     * @return returns the NosqlDbConfig object.
     */
    public static NosqlDbConfig createCloudSimConfig(String endpoint) {
        NosqlDbConfig config = new NosqlDbConfig(endpoint,
            NosqlDbFactory.CloudSimProvider.getProvider());
        config.getNosqlHandleConfig().setRateLimitingEnabled(true);
        return config;
    }

    /**
     * Creates a NosqlDbConfig object for an on-prem server using an
     * httproxy server.
     *
     * @param endpoint Endpoint of service. Example: "http://localhost:8080"
     * @return returns the NosqlDbConfig object.
     */
    public static NosqlDbConfig createProxyConfig(String endpoint) {
        return new NosqlDbConfig(endpoint, new StoreAccessTokenProvider());
    }

    /**
     * Creates a NosqlDbConfig object for an on-prem server using a
     * secured httproxy server.
     *
     * @param endpoint Endpoint of service. Example: "https://localhost:8080"
     * @param username User name
     * @param password User password
     * @return returns the NosqlDbConfig object.
     */
    public static NosqlDbConfig createProxyConfig(String endpoint,
        String username, char[] password) {
        return new NosqlDbConfig(endpoint,
                new StoreAccessTokenProvider(username, password));
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
     * {@link Constants#DEFAULT_TIMESTAMP_PRECISION}.<p>
     *
     * Timestamp values have a precision (0 - 9) which represents the fractional
     * seconds to be held by the timestamp. A value of 0 means that no
     * fractional seconds are stored, 3 means that the timestamp stores
     * milliseconds, and 9 means a precision of nanoseconds.<p>
     *
     * In the context of a CREATE TABLE statement, a precision must be
     * explicitly specified. This restriction is to prevent users from
     * inadvertently creating TIMESTAMP values with precision 9 (which takes
     * more space) when in reality they don't need that high precision.<p>
     *
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

    /**
     * Sets the default storageGB when table is created if
     * {@link NosqlTable} annotation is not present or it doesn't specify a
     * storageGB value. Valid values are only values greater than 0. This
     * applies only in cloud or cloud sim scenarios.<p>
     * If not set the default value is 25GB.<p>
     *
     * For {@link TableLimits} to be set one of the following two
     * conditions must be met:<ul><li>
     *   capacityMode is set to PROVISIONED and all three: readUnits,
     *     writeUnits and storageGB must be greater than 0,</li><li>
     *   or capacityMode is set to ON_DEMAND and storageGB must be greater than
     *     0.</li></ul><p>
     *
     * Note: StorageBG, capacity mode and read/write units can later be
     * configured from the OCI console or using the API.
     */
    public NosqlDbConfig setDefaultStorageGB(int defaultStorageGB) {
        this.defaultStorageGB = defaultStorageGB;
        return this;
    }

    /**
     * Returns the value that was set for default storageGB or 25 otherwise.
     */
    public int getDefaultStorageGB() {
        return defaultStorageGB;
    }

    /**
     * Sets the default capacity mode when table is created if
     * {@link NosqlTable} annotation is not present. This
     * applies only in cloud or cloud sim scenarios.<p>
     * If not set the default value is
     * {@link TableLimits.CapacityMode#PROVISIONED}.<p>
     *
     * Note: StorageBG, capacity mode and read/write units can later be
     * configured from the OCI console or using the API.
     */
    public NosqlDbConfig setDefaultCapacityMode(NosqlCapacityMode defaultCapacityMode) {
        this.defaultCapacityMode = defaultCapacityMode;
        return this;
    }

    /**
     * Returns the default capacity mode set.
     * If not set the returned value is
     * {@link TableLimits.CapacityMode#PROVISIONED}.
     */
    public NosqlCapacityMode getDefaultCapacityMode() {
        return defaultCapacityMode;
    }

    /**
     * Sets the default read units when table is created if
     * {@link NosqlTable} annotation is not present or it doesn't specify a
     * readUnits value. Valid values are only values greater than 0. This
     * applies only in cloud or cloud sim scenarios.
     * When in PROVISIONED mode all three: storageGB, readUnits and writeUnits
     * must be greater than 0 to be valid.
     * {@link oracle.nosql.driver.ops.TableLimits}.<p>
     * If not set the default value is 50.<p>
     *
     * Note: StorageBG, capacity mode and read/write units can later be
     * configured from the OCI console or using the API.
     */
    public NosqlDbConfig setDefaultReadUnits(int defaultReadUnits) {
        this.defaultReadUnits = defaultReadUnits;
        return this;
    }

    /**
     * Returns the value that was set for default read units or 50 otherwise.
     */
    public int getDefaultReadUnits() {
        return defaultReadUnits;
    }

    /**
     * Sets the default write units when table is created if
     * {@link NosqlTable} annotation is not present, or it doesn't specify a
     * writeUnits value. Valid values are only values greater than 0. This
     * applies only in cloud or cloud sim scenarios.
     * When in PROVISIONED mode all three: storageGB, readUnits and writeUnits
     * must be greater than 0 to be valid.
     * {@link oracle.nosql.driver.ops.TableLimits}.<p>
     * If not set the default value is 50.<p>
     *
     * Note: StorageBG, capacity mode and read/write units can later be
     * configured from the OCI console or using the API.
     */
    public NosqlDbConfig setDefaultWriteUnits(int defaultWriteUnits) {
        this.defaultWriteUnits = defaultWriteUnits;
        return this;
    }

    /**
     * Returns the value that was set for default write units or 50 otherwise.
     */
    public int getDefaultWriteUnits() {
        return defaultWriteUnits;
    }

    /**
     * Sets the default write units when table is created if
     * {@link NosqlTable} annotation is not present, or it doesn't specify a
     * writeUnits value. Valid values are only values greater than 0. This
     * applies only in cloud or cloud sim scenarios.
     * When in PROVISIONED mode all three: storageGB, readUnits and writeUnits
     * must be greater than 0 to be valid.
     * {@link oracle.nosql.driver.ops.TableLimits}.<p>
     * If not set the default value is 50.<p>
     *
     * Note: StorageBG, capacity mode and read/write units can later be
     * configured from the OCI console or using the API.
     */
    public NosqlDbConfig setDefaultNamespace(String defaultNamespace) {
        this.defaultNamespace = defaultNamespace;
        return this;
    }

    /**
     * Returns the value that was set for default write units or 50 otherwise.
     */
    public String getDefaultNamespace() {
        return defaultNamespace;
    }
}