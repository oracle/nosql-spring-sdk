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
import static com.oracle.nosql.spring.data.Constants.NOTSET_SHARD_KEY;

/**
 * Identifies the annotated field as a primary key of the composite
 * primary key.
 * It is recommended to use both shard and order elements when providing this
 * annotation.
 * <pre>
 *     Example:
 *     class CompositeKey {
 *          &#64;NosqlKey(shardKey=true, order = 1)
 *          private String city;
 *
 *          &#64;NosqlKey(shardKey = false, order = 2)
 *          private String area;
 *     }
 * </pre>
 *   @since 1.6.0
 */

@Documented
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = {ElementType.ANNOTATION_TYPE, ElementType.FIELD,
        ElementType.METHOD})
public @interface NosqlKey {
    /**
     * Specifies whether the field is shard key or not. Default value is
     * {@link com.oracle.nosql.spring.data.Constants#NOTSET_SHARD_KEY}.
     *
     * @since 1.6.0
     */
    boolean shardKey() default NOTSET_SHARD_KEY;

    /**
     * Specifies the order of this primary key related to other primary keys
     * of composite key class.
     * This ordering is used in table creation DDL to specify PRIMARY KEY and
     * SHARD KEY ordering.
     * Ordering is done based on below rules.
     * <ul>
     *  <li>SHARD KEYS are placed before non SHARD KEYS.</li>
     *  <li>For each SHARD and non SHARD group</li>
     *  <li>
     *  <ul>
     *     <li>
     *         If order is specified it must be specified on all the fields
     *     otherwise it is an error.
     *     </li>
     *     <li>
     *          If order is specified it must be unique otherwise it is an
     *          error.
     *     </li>
     *     <li>
     *         If no order is specified then fields are sorted by lower case
     *         alphabetical order of the field names
     *     </li>
     *   </ul>
     *   </li>
     * </ul>
     * Default value is
     * {@link com.oracle.nosql.spring.data.Constants#NOTSET_PRIMARY_KEY_ORDER}
     *
     * @since 1.6.0
     */

    int order() default NOTSET_PRIMARY_KEY_ORDER;

}
