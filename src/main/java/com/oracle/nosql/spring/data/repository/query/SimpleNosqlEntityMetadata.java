/*-
 * Copyright (c) 2020, 2024 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */
package com.oracle.nosql.spring.data.repository.query;

import com.oracle.nosql.spring.data.repository.support.NosqlEntityInformation;

import org.springframework.util.Assert;

public class SimpleNosqlEntityMetadata<T>
    implements NosqlEntityMetadata<T> {

    private final Class<T> type;
    private final NosqlEntityInformation<T, ?> entityInformation;

    public SimpleNosqlEntityMetadata(Class<T> type,
        NosqlEntityInformation<T, ?> entityInformation) {

        Assert.notNull(type, "type must not be null!");
        Assert.notNull(entityInformation, "entityInformation must not be null!");

        this.type = type;
        this.entityInformation = entityInformation;
    }

    @Override
    public Class<T> getJavaType() {
        return type;
    }

    @Override
    public String getTableName() {
        return entityInformation.getTableName();
    }

    @Override
    public NosqlEntityInformation<T, ?> getNosqlEntityInformation() {
        return entityInformation;
    }
}
