/*-
 * Copyright (c) 2020, 2023 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */
package com.oracle.nosql.spring.data.repository.config;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;

import com.oracle.nosql.spring.data.Constants;
import com.oracle.nosql.spring.data.core.mapping.NosqlMappingContext;
import com.oracle.nosql.spring.data.core.mapping.NosqlTable;
import com.oracle.nosql.spring.data.repository.NosqlRepository;
import com.oracle.nosql.spring.data.repository.support.NosqlRepositoryFactoryBean;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.data.config.ParsingUtils;
import org.springframework.data.repository.config.AnnotationRepositoryConfigurationSource;
import org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport;
import org.springframework.data.repository.config.RepositoryConfigurationSource;
import org.springframework.data.repository.config.XmlRepositoryConfigurationSource;
import org.springframework.data.repository.core.RepositoryMetadata;

public class NosqlRepositoryConfigurationExtension extends
    RepositoryConfigurationExtensionSupport {

    @Override
    public String getModuleName() {
        return Constants.NOSQLDB_MODULE_NAME;
    }

    @Override
    public String getModulePrefix() {
        return Constants.NOSQLDB_MODULE_PREFIX;
    }

    public String getRepositoryFactoryBeanClassName() {
        return NosqlRepositoryFactoryBean.class.getName();
    }

    @Override
    protected Collection<Class<?>> getIdentifyingTypes() {
        return Collections.<Class<?>>singleton(NosqlRepository.class);
    }

    @Override
    protected Collection<Class<? extends Annotation>> getIdentifyingAnnotations() {
        return Collections.singleton(NosqlTable.class);
    }

    @Override
    public void registerBeansForRoot(BeanDefinitionRegistry registry, RepositoryConfigurationSource config) {
        super.registerBeansForRoot(registry, config);

        if (!registry.containsBeanDefinition(Constants.NOSQL_MAPPING_CONTEXT)) {
            final RootBeanDefinition definition = new RootBeanDefinition(
                NosqlMappingContext.class);
            definition.setRole(AbstractBeanDefinition.ROLE_INFRASTRUCTURE);
            definition.setSource(config.getSource());

            registry.registerBeanDefinition(Constants.NOSQL_MAPPING_CONTEXT, definition);
        }
    }

    @Override
    public void postProcess(BeanDefinitionBuilder builder, RepositoryConfigurationSource source) {
        super.postProcess(builder, source);
    }

    @Override
    public void postProcess(BeanDefinitionBuilder builder, XmlRepositoryConfigurationSource config) {
        ParsingUtils.setPropertyReference(builder, config.getElement(),
            Constants.NOSQL_XML_TEMPLATE_REF, "nosqlOperations");
    }

    @Override
    public void postProcess(BeanDefinitionBuilder builder, AnnotationRepositoryConfigurationSource config) {

        AnnotationAttributes attributes = config.getAttributes();

        builder.addPropertyReference("nosqlOperations", attributes.getString(
            "nosqlTemplateRef"));
    }

    @Override
    protected boolean useRepositoryConfiguration(RepositoryMetadata metadata) {
        //  NosqlRepository is the sync repository, and hence returning !isReactiveRepository.
        //  ReactiveNosqlRepository is reactive repository.
        return !metadata.isReactiveRepository();
    }
}
