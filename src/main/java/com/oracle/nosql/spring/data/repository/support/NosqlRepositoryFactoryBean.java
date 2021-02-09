/*-
 * Copyright (c) 2020, 2021 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */
package com.oracle.nosql.spring.data.repository.support;

import java.io.Serializable;

import com.oracle.nosql.spring.data.core.NosqlOperations;
import com.oracle.nosql.spring.data.core.mapping.NosqlMappingContext;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;

public class NosqlRepositoryFactoryBean
        <T extends Repository<S, ID>, S, ID extends Serializable>
    extends RepositoryFactoryBeanSupport<T, S, ID>
    implements ApplicationContextAware {

    private ApplicationContext applicationContext;
    private NosqlOperations operations;
    private boolean mappingContextConfigured = false;


    public NosqlRepositoryFactoryBean(Class<? extends T> repositoryInterface) {
        super(repositoryInterface);
    }

    @Autowired
    public void setNosqlOperations(NosqlOperations nosqlOperations) {
        this.operations = nosqlOperations;
    }

    @Override
    protected final RepositoryFactorySupport createRepositoryFactory() {
        return getFactoryInstance(applicationContext);
    }

    protected RepositoryFactorySupport getFactoryInstance(
        ApplicationContext applicationContext) {
        return new NosqlRepositoryFactory(operations, applicationContext);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext)
        throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    protected void setMappingContext(MappingContext<?, ?> mappingContext) {
        super.setMappingContext(mappingContext);
        this.mappingContextConfigured = true;
    }

    @Override
    public void afterPropertiesSet() {
        super.afterPropertiesSet();

        if (!this.mappingContextConfigured) {
            if (operations != null && operations.getConverter() != null) {
                setMappingContext(
                    operations.getConverter().getMappingContext());
            } else {
                setMappingContext(new NosqlMappingContext());
            }
        }
    }
}
