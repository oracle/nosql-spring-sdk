/*-
 * Copyright (c) 2020, 2021 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */
package com.oracle.nosql.spring.data.test.id;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Persistent;

@Persistent
public class SensorIdInt {
    String name;

    @Id
    int temp;

    long time;
}
