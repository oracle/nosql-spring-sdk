/*-
 * Copyright (c) 2020, 2021 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */
package com.oracle.nosql.spring.data.repository;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.data.annotation.QueryAnnotation;

/**
 * Annotation to declare native queries directly on repository methods.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.ANNOTATION_TYPE })
@Documented
@QueryAnnotation
public @interface Query {
    /**
     * Takes an Oracle NoSQL DB SQL query string to define the actual query to
     * be executed. This one will take precedence over the
     * method name then.
     */
    String value() default "";

    /**
     * Returns whether the query defined should be executed as count projection.
     */
    boolean count() default false;

    /**
     * Returns whether the query defined should be executed as exists projection.
     */
    boolean exists() default false;

    /**
     * Returns whether the query defined should delete matching rows.
     */
    boolean delete() default false;
}
