/*-
 * Copyright (c) 2020, 2023 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */
package com.oracle.nosql.spring.data.test.id;

import java.sql.Timestamp;

import org.springframework.data.annotation.Id;

public class SensorIdTimestamp {
    String name;

    int temp;

    @Id
    Timestamp time;
}
