/*-
 * Copyright (c) 2020, 2021 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */
package com.oracle.nosql.spring.data.repository.query;

import org.springframework.data.repository.query.ParameterAccessor;

public interface NosqlParameterAccessor extends ParameterAccessor {
    Object[] getValues();
}

