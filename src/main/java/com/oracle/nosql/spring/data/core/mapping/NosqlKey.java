/*-
 * Copyright (c) 2020, 2025 Oracle and/or its affiliates.  All rights reserved.
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
 * Identifies the annotated field as a component of the composite
 * primary key.<p>
 *
 * Order of the fields affects index selection on queries, and
 * data distribution. Query performance is improved when field 1
 * through field N appears as query predicates where N is less than
 * or equal to the total fields in the composite key.<p>
 *
 * It is recommended to provide both {@code shardKey} and {@code order} options
 * with this annotation.
 * <pre>
 *     Example creating the composite key (country, city, street):
 *     class CompositeKey {
 *          &#64;NosqlKey(shardKey = true, order = 1)
 *          private String country;
 *
 *          &#64;NosqlKey(shardKey = false, order = 2)
 *          private String city;
 *
 *          &#64;NosqlKey(shardKey = false, order = 3)
 *          private String street;
 *     }
 * </pre>
 * Queries using country, or country and city, or country and city and street
 * as filtering predicates are faster than queries specifying only street.
 *
 * @since 1.6.0
 */
@Documented
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = {ElementType.ANNOTATION_TYPE, ElementType.FIELD,
        ElementType.METHOD})
public @interface NosqlKey {
    /**
     * Specifies whether the field is part of the shard key or not. Default value
     * is {@link com.oracle.nosql.spring.data.Constants#NOTSET_SHARD_KEY}.
     * Shard keys affect distribution of rows across shards and atomicity of
     * operations on a single shard.
     * @since 1.6.0
     */
    boolean shardKey() default NOTSET_SHARD_KEY;

    /**
     * Specifies the order of the field related to other fields listed in the
     * composite key class.<p>
     * This ordering is used in table creation DDL to specify PRIMARY KEY and
     * SHARD KEY ordering.<p>
     * Ordering is done based on below rules:<ul><li>
     *     Shard keys are placed at the beginning.</li><li>
     *     When using the <code>order</code> option:<ul><li>
     *         It must be specified on all the fields otherwise it is an error.
     *           </li><li>
     *         It must be unique otherwise it is an error.
     *           </li><li>
     *         Order of the shard keys must be less than the order of non
     *         shard keys otherwise it is an error.
     *         </li>
     *         </ul></li><li>
     *     If {@code order} is not specified then fields are sorted
     *     alphabetically using lower case field names for fields grouped by the
     *     same shardKey value.</li></ul><p>
     * Default value is
     * {@link com.oracle.nosql.spring.data.Constants#NOTSET_PRIMARY_KEY_ORDER}
     *
     * @since 1.6.0
     */
    int order() default NOTSET_PRIMARY_KEY_ORDER;
}
