/*-
 * Copyright (c) 2020, 2022 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */
package com.oracle.nosql.spring.data.core.query;

import java.util.Map;

import com.oracle.nosql.spring.data.repository.query.NosqlParameterAccessor;
import com.oracle.nosql.spring.data.repository.query.NosqlQueryMethod;

import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.Parameters;

public class StringQuery extends NosqlQuery {
    final NosqlQueryMethod method;
    final String query;
    final NosqlParameterAccessor accessor;

    public StringQuery(NosqlQueryMethod method, String query,
        NosqlParameterAccessor accessor) {
        this.method = method;
        this.query = query;
        this.accessor = accessor;
    }

    @Override
    public String generateSql(String tableName,
        Map<String, Object> params, String idPropertyName) {

        Parameters<?, ?> methodParams = method.getParameters();
        int i = 0;

        for (Parameter p : methodParams.getBindableParameters()) {
            if (p.isNamedParameter()) {
                if (p.getName().isPresent()) {
                    params.put(p.getName().get(), accessor.getBindableValue(i));
                    i++;
                } else {
                    throw new IllegalArgumentException("Parameter must have a" +
                        " non null name.");
                }
            } else {
                //todo support index based params when implemented downstream
                throw new IllegalArgumentException("Not explicitly named " +
                    "parameters are not supported with native queries.");
            }
        }

        return query;
    }
}
