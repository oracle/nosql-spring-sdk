/*-
 * Copyright (c) 2020, 2025 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */
package com.oracle.nosql.spring.data.core.mapping;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.data.annotation.Id;

/**
 * Identifies the primary key field of the entity. Primary key can be of a
 * basic type or of a type that represents a composite primary key. This
 * field corresponds to the {@code PRIMARY KEY} of the corresponding Nosql
 * table. Only one field of the entity can be annotated with this annotation.
 *
 * If using a composite primary key, the fields comprising the composite key
 * must have distinct names when viewed in lower case, otherwise an error is
 * raised.
 */
@Id
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.ANNOTATION_TYPE })
public @interface NosqlId {
    /**
     * Specifies if values for the field are automatically generated. Valid only
     * for int, Integer, long, Long, BigInteger, BigDecimal,
     */
    boolean generated() default false;
}
