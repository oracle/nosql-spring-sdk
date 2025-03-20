/*-
 * Copyright (c) 2020, 2025 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */
package com.oracle.nosql.spring.data.config;

import com.oracle.nosql.spring.data.NosqlDbFactory;
import com.oracle.nosql.spring.data.core.NosqlTemplate;
import com.oracle.nosql.spring.data.core.ReactiveNosqlTemplate;
import com.oracle.nosql.spring.data.core.convert.MappingNosqlConverter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;

@Configuration
public class AbstractNosqlConfiguration extends
    NosqlConfigurationSupport {

    @Bean
    public NosqlDbFactory nosqlDbFactory(NosqlDbConfig nosqlDBConfig) {
        return new NosqlDbFactory(nosqlDBConfig);
    }

    @Bean
    public NosqlTemplate nosqlTemplate(NosqlDbFactory nosqlDbFactory)
        throws ClassNotFoundException {
        return new NosqlTemplate(nosqlDbFactory, mappingNosqlConverter());
    }

    @Bean
    public ReactiveNosqlTemplate reactiveNosqlTemplate(
        NosqlDbFactory nosqlDbFactory)
        throws ClassNotFoundException
    {
        return new ReactiveNosqlTemplate(nosqlDbFactory,
            mappingNosqlConverter());
    }

    @Bean
    @NonNull
    public  MappingNosqlConverter mappingNosqlConverter()
        throws ClassNotFoundException
    {
        return new MappingNosqlConverter(this.nosqlMappingContext());
    }
}
