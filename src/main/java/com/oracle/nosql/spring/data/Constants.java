/*-
 * Copyright (c) 2020, 2024 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */
package com.oracle.nosql.spring.data;

public class Constants {
    public static final String DEFAULT_TABLE_NAME = "";
    public static final boolean DEFAULT_AUTO_CREATE_TABLE = true;
    public static final String ID_PROPERTY_NAME = "id";
    public static final String NOSQLDB_MODULE_NAME = "oracle-nosql";
    public static final String NOSQLDB_MODULE_PREFIX = "oracle-nosql";
    public static final String NOSQL_MAPPING_CONTEXT = "nosqlMappingContext";
    public static final String DEFAULT_REPOSITORY_IMPLEMENT_POSTFIX = "Impl";
    public static final String NOSQL_XML_TEMPLATE_REF = "nosql-template-ref";
    public static final int DEFAULT_TABLE_REQ_TIMEOUT_MS = 60000;
    public static final int DEFAULT_TABLE_RED_POLL_INTERVAL_MS = 500;
    public static final int DEFAULT_QUERY_CACHE_CAPACITY = 1000;
    public static final int DEFAULT_QUERY_CACHE_LIFETIME_MS = 1000 * 60 * 10; // 10min
    public static final int DEFAULT_TIMESTAMP_PRECISION = 3;
    public static final String DEFAULT_TABLE_CONSISTENCY = "EVENTUAL";
    public static final String DEFAULT_TABLE_DURABILITY = "COMMIT_NO_SYNC";

    public static final int NOTSET_TABLE_READ_UNITS = -1;
    public static final int NOTSET_TABLE_WRITE_UNITS = -1;
    public static final int NOTSET_TABLE_STORAGE_GB = -1;
    public static final int NOTSET_TABLE_TIMEOUT_MS = 0;
    public static final int NOTSET_TABLE_TTL = 0;

    public static final boolean NOTSET_SHARD_KEY = true;
    public static final int NOTSET_PRIMARY_KEY_ORDER = -1;

    public static final String USER_AGENT = "NoSQL-SpringSDK";

    private Constants() {}
}
