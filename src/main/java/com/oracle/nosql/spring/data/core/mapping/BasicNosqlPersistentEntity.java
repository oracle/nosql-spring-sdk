/*-
 * Copyright (c) 2020, 2023 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */
package com.oracle.nosql.spring.data.core.mapping;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.expression.BeanFactoryAccessor;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.data.annotation.Id;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.model.BasicPersistentEntity;
import org.springframework.data.util.TypeInformation;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.lang.Nullable;

public class BasicNosqlPersistentEntity<T>
    extends BasicPersistentEntity<T, NosqlPersistentProperty>
    implements NosqlPersistentEntity<T>, ApplicationContextAware {

    private static final NoSqlPersistentEntityMetadataVerifier DEFAULT_VERIFIER =
            new CompositeNoSqlPersistentEntityMetadataVerifier();

    private final StandardEvaluationContext context;
    private NoSqlPersistentEntityMetadataVerifier verifier;


    public BasicNosqlPersistentEntity(TypeInformation<T> typeInformation) {
        this(typeInformation, DEFAULT_VERIFIER);
    }

    public BasicNosqlPersistentEntity(TypeInformation<T> typeInformation,
        NoSqlPersistentEntityMetadataVerifier verifier) {
        super(typeInformation);
        this.context = new StandardEvaluationContext();
        this.verifier = verifier;
    }
    public void setApplicationContext(ApplicationContext applicationContext)
        throws BeansException {
        context.addPropertyAccessor(new BeanFactoryAccessor());
        context.setBeanResolver(new BeanFactoryResolver(applicationContext));
        context.setRootObject(applicationContext);
    }

    @Nullable
    protected NosqlPersistentProperty
        returnPropertyIfBetterIdPropertyCandidateOrNull(
            NosqlPersistentProperty property) {
        if (!property.isIdProperty()) {
            return null;
        }

        if (!hasIdProperty()) {
            if (property.isIdProperty()) {
                return property;
            } else {
                return null;
            }
        }

        // hasIdProperty() && property.isIdProperty()
        NosqlPersistentProperty currentIdProp = getIdProperty();
        if (!currentIdProp.isAnnotationPresent(NosqlId.class) &&
            !currentIdProp.isAnnotationPresent(Id.class) &&
            (property.isAnnotationPresent(NosqlId.class) ||
                property.isAnnotationPresent(Id.class)) ) {
            return property;
        }

        throw new MappingException(String.format("Attempt to add id property " +
            "%s but " +
            "already have property %s registered as id. Check your mapping " +
            "configuration!", property.getField(), currentIdProp.getField()));
    }

    @Override
    public boolean isCompositeKey() {
        return isAnnotationPresent(NoSqlKeyClass.class);
    }

    @Override
    public void verify() throws MappingException {
        super.verify();
        this.verifier.verify(this);
    }
}
