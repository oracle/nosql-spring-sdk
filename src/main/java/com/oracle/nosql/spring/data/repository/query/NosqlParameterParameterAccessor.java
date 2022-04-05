/*-
 * Copyright (c) 2020, 2022 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */
package com.oracle.nosql.spring.data.repository.query;

import java.util.Arrays;
import java.util.List;

import org.springframework.data.repository.query.ParametersParameterAccessor;

public class NosqlParameterParameterAccessor
    extends ParametersParameterAccessor
    implements NosqlParameterAccessor {

    private final List<Object> values;

    public NosqlParameterParameterAccessor(
        NosqlQueryMethod method,
        Object[] values) {

        super(method.getParameters(), values);

        this.values = Arrays.asList(values);
    }

    @Override
    public Object[] getValues() {
        return values.toArray();
    }
}