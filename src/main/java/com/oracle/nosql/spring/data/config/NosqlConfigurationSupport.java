/*-
 * Copyright (c) 2020, 2021 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */
package com.oracle.nosql.spring.data.config;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.oracle.nosql.spring.data.core.mapping.NosqlMappingContext;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.data.annotation.Persistent;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

public abstract class NosqlConfigurationSupport {

    @Bean
    @NonNull
    public NosqlMappingContext nosqlMappingContext()
        throws ClassNotFoundException {
        final NosqlMappingContext mappingContext = new NosqlMappingContext();
        mappingContext.setInitialEntitySet(getInitialEntitySet());

        return mappingContext;
    }

    protected Collection<String> getMappingBasePackages() {
        final Package mappingBasePackage = getClass().getPackage();
        return Collections.singleton(
            mappingBasePackage == null ? null : mappingBasePackage.getName());
    }

    protected Set<Class<?>> getInitialEntitySet()
        throws ClassNotFoundException {
        final Set<Class<?>> initialEntitySet = new HashSet<>();

        for (final String basePackage : getMappingBasePackages()) {
            initialEntitySet.addAll(scanForEntities(basePackage));
        }

        return initialEntitySet;
    }

    protected Set<Class<?>> scanForEntities(String basePackage)
        throws ClassNotFoundException {
        if (!StringUtils.hasText(basePackage)) {
            return Collections.emptySet();
        }

        final Set<Class<?>> initialEntitySet = new HashSet<>();

        if (StringUtils.hasText(basePackage)) {
            final ClassPathScanningCandidateComponentProvider componentProvider
                = new ClassPathScanningCandidateComponentProvider(false);

            componentProvider.addIncludeFilter(new AnnotationTypeFilter(
                Persistent.class));

            for (final BeanDefinition candidate :
                componentProvider.findCandidateComponents(basePackage)) {
                final String className = candidate.getBeanClassName();
                Assert.notNull(className, "Bean class name is null.");

                initialEntitySet
                    .add(ClassUtils
                        .forName(className,
                            NosqlConfigurationSupport.class.getClassLoader()));
            }
        }

        return initialEntitySet;
    }
}