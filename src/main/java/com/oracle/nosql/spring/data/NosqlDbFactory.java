/*-
 * Copyright (c) 2020, 2022 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */
package com.oracle.nosql.spring.data;

import java.io.IOException;

import oracle.nosql.driver.AuthorizationProvider;
import oracle.nosql.driver.NoSQLHandle;
import oracle.nosql.driver.NoSQLHandleConfig;
import oracle.nosql.driver.NoSQLHandleFactory;
import oracle.nosql.driver.ops.Request;

import com.oracle.nosql.spring.data.config.NosqlDbConfig;

import org.springframework.util.Assert;

/**
 * Factory class for connecting to an Oracle NoSQL Database on premise
 * database or cloud service.
 */
public class NosqlDbFactory {

    private static String libraryVersion = findVersion();

    private final NosqlDbConfig config;
    private NoSQLHandle handle;

    public NosqlDbFactory(NosqlDbConfig config) {
        Assert.notNull(config, "NosqlDbConfig should not be null.");
        validateConfig(config);

        this.config = config;
    }

    public NoSQLHandle getNosqlClient() {
        if ( handle == null ) {
            synchronized (this) {
                if (handle == null) {
                    handle = NoSQLHandleFactory
                        .createNoSQLHandle(config.getNosqlHandleConfig());
                }
            }
        }
        return handle;
    }

    private void validateConfig(NosqlDbConfig config) {
        Assert.notNull(config, "NosqlDbConfig should " +
            "not be null.");
        Assert.notNull(config.getNosqlHandleConfig(), "NosqlDbConfig should " +
            "have a non-null NoSQLHandleConfig.");
        Assert.notNull(config.getNosqlHandleConfig().getServiceURL(),
            "NosqlDbConfig should " +
            "have a non-null endpoint.");
        Assert.notNull(config.getNosqlHandleConfig().getAuthorizationProvider(),
            "NosqlDbConfig should " +
            "have a non-null authorization provider.");
    }

    public static NosqlDbFactory createCloudFactory(String endpoint,
        String configFile)
        throws IOException
    {
        return new NosqlDbFactory(NosqlDbConfig.createCloudConfig(endpoint,
            configFile));
    }

    public static NosqlDbFactory createCloudSimFactory(String endpoint) {
        return new NosqlDbFactory(NosqlDbConfig.createCloudSimConfig(endpoint));
    }

    public static NosqlDbFactory createProxyFactory(String endpoint) {
        return new NosqlDbFactory(NosqlDbConfig.createProxyConfig(endpoint));
    }

    public static NosqlDbFactory createProxyFactory(String endpoint,
        String user, char[] password) {
        return new NosqlDbFactory(NosqlDbConfig.createProxyConfig(endpoint,
            user, password));
    }

    /**
     * Returns the capacity of the prepared query cache. By default this is set
     * to {@link Constants#DEFAULT_QUERY_CACHE_CAPACITY}.
     */
    public int getQueryCacheCapacity() {
        return config.getQueryCacheCapacity();
    }

    /**
     * Returns the lifetime of the prepared query cache in milliseconds. By
     * default this is set to
     * {@link Constants#DEFAULT_QUERY_CACHE_LIFETIME_MS}.
     */
    public int getQueryCacheLifetime() {
        return config.getQueryCacheLifetime();
    }

    /**
     * Returns the table request timeout in milliseconds. By default this is
     * set to {@link Constants#DEFAULT_TABLE_REQ_TIMEOUT_MS}
     */
    public int getTableReqTimeout() {
        return config.getTableReqTimeout();
    }

    /** Returns the table request poll interval in milliseconds. By default this
     * is  set to {@link Constants#DEFAULT_TABLE_REQ_POLL_INTEVEL_MS}
     */
    public int getTableReqPollInterval() {
        return config.getTableReqPollInterval();
    }

    /**
     * Returns the precision of the Timestamp NoSQL DB type when creating a
     * new table. By default this is set to
     * {@link Constants#DEFAULT_TIMESTAMP_PRECISION}.<p>
     *
     * In the context of a CREATE TABLE statement, a precision must be
     * explicitly specified. This restriction is to prevent users from
     * inadvertently creating TIMESTAMP values with precision 9 (which takes
     * more space) when in reality they don't need that high precision.<p>
     *
     * See <a href="https://docs.oracle.com/en/database/other-databases/nosql-database/20.2/sqlreferencefornosql/data-type-definitions.html">
     * Timestamp documentation</a> for more details.
     */
    public int getTimestampPrecision() {
        return config.getTimestampPrecision();
    }


    /**
     * Pulls the version string from the manifest. The version is added
     * by maven.
     */
    private static String findVersion() {
        return  NoSQLHandleConfig.class.getPackage().getImplementationVersion();
    }

    /**
     * Returns the current version of the NoSQL DB Sprint Data SDK, as a
     * string in a x.y.z format.
     */
    public static String getLibraryVersion() {
        return libraryVersion;
    }

    /**
     * A simple provider that uses a manufactured id for use by the
     * Cloud Simulator. It is used as a namespace for tables and not
     * for any actual authorization or authentication.
     */
    public static class CloudSimProvider implements AuthorizationProvider {

        private static final String id = "Bearer exampleId";
        private static AuthorizationProvider provider =
            new CloudSimProvider();

        public static AuthorizationProvider getProvider() {
            return provider;
        }

        /**
         * Disallow external construction. This is a singleton.
         */
        private CloudSimProvider() {}

        @Override
        public String getAuthorizationString(Request request) {
            return id;
        }

        @Override
        public void close() {}
    }
}
