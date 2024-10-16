/*-
 * Copyright (c) 2020, 2024 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */
package com.oracle.nosql.spring.data.core.mapping;

/**
 * Cloud only, selector for how capacity is set when creating new tables.
 *
 * @see oracle.nosql.driver.ops.TableLimits.CapacityMode
 */
public enum NosqlCapacityMode {
    PROVISIONED,
    ON_DEMAND
}
