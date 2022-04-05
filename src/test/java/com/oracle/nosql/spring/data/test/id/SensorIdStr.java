/*-
 * Copyright (c) 2020, 2022 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */
package com.oracle.nosql.spring.data.test.id;

import com.oracle.nosql.spring.data.core.mapping.NosqlId;

import org.springframework.data.annotation.Persistent;

@Persistent
public class SensorIdStr {
    @NosqlId
    String name;

    int temp;
    long time;
}
