/*-
 * Copyright (c) 2020, 2021 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */
package com.oracle.nosql.spring.data.test.id;

import com.oracle.nosql.spring.data.core.mapping.NosqlId;

public class SensorIdStrGenerated {
    @NosqlId(generated = true)
    String id;

    int temp;
    long time;
}
