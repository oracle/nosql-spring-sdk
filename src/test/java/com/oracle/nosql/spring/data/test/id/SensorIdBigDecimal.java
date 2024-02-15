/*-
 * Copyright (c) 2020, 2024 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */
package com.oracle.nosql.spring.data.test.id;

import java.math.BigDecimal;

import org.springframework.data.annotation.Id;

public class SensorIdBigDecimal {
    String name;

    @Id
    BigDecimal temp;

    long time;
}
