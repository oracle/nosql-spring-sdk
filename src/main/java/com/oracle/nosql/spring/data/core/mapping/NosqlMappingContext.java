/*-
 * Copyright (c) 2020, 2024 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */
package com.oracle.nosql.spring.data.core.mapping;

import org.springframework.context.ApplicationContext;
import org.springframework.data.mapping.context.AbstractMappingContext;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.util.TypeInformation;

public class NosqlMappingContext
    extends AbstractMappingContext<BasicNosqlPersistentEntity<?>,
                                   NosqlPersistentProperty> {

     private ApplicationContext context;

    @Override
    protected <T> BasicNosqlPersistentEntity<T> createPersistentEntity(
        TypeInformation<T> typeInformation) {
        final BasicNosqlPersistentEntity<T> entity =
            new BasicNosqlPersistentEntity<>(typeInformation);

        if (context != null) {
            entity.setApplicationContext(context);
        }
        return entity;
    }

    @Override
    public NosqlPersistentProperty createPersistentProperty(Property property,
        BasicNosqlPersistentEntity<?> owner,
        SimpleTypeHolder simpleTypeHolder) {
        return
            new BasicNosqlPersistentProperty(property, owner, simpleTypeHolder);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.context = applicationContext;
    }

    @Override
    protected boolean shouldCreatePersistentEntityFor(TypeInformation<?> type) {

        NosqlPersistentProperty.TypeCode typeCode =
            BasicNosqlPersistentProperty
                .getCodeForSerialization(type.getType());

        boolean r;

        if (typeCode.isAtomic()) {
            r = false;
        } else {
            r = super.shouldCreatePersistentEntityFor(type);
        }

        return r;
    }
}
