/*-
 * Copyright (c) 2020, 2023 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */

package com.oracle.nosql.spring.data.core.mapping;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static com.oracle.nosql.spring.data.Constants.NOTSET_PRIMARY_KEY_ORDER;
import static com.oracle.nosql.spring.data.Constants.NOTSET_TABLE_SHARD_KEY;

/**
 * Identifies the annotated field as a primary key column of the composite
 * primary key.
 */

@Documented
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = {ElementType.ANNOTATION_TYPE, ElementType.FIELD,
        ElementType.METHOD})
public @interface NosqlKey {
    /**
     * Specifies whether the field is shard key or not. Default value is
     * {@link com.oracle.nosql.spring.data.Constants#NOTSET_TABLE_SHARD_KEY}.
     *
     * @return
     */
    boolean shardKey() default NOTSET_TABLE_SHARD_KEY;

    /**
     * Specifies the order of this primary key related to other primary keys
     * of composite key class.
     * This ordering is used in table creation DDL to specify PRIMARY KEY and
     * SHARD KEY ordering. Fields are first ordered by shardKey and then by
     * order and then by field name during table creation. Default value is
     * {@link com.oracle.nosql.spring.data.Constants#NOTSET_PRIMARY_KEY_ORDER}
     *
     * @return
     */

    int order() default NOTSET_PRIMARY_KEY_ORDER;

}
