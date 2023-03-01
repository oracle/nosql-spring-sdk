/*-
 * Copyright (c) 2020, 2023 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */
package com.oracle.nosql.spring.data.test.id;

import java.time.Instant;

import org.springframework.data.annotation.Id;

public class SensorIdInstant {
    String name;
    int temp;

    @Id
    Instant time;
}