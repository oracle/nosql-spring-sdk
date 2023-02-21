/*-
 * Copyright (c) 2020, 2023 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */
package com.oracle.nosql.spring.data.test.reactive;

import com.oracle.nosql.spring.data.config.AbstractNosqlConfiguration;
import com.oracle.nosql.spring.data.config.NosqlDbConfig;
import com.oracle.nosql.spring.data.repository.config.EnableReactiveNosqlRepositories;
import com.oracle.nosql.spring.data.test.app.AppConfig;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableReactiveNosqlRepositories
public class ReactiveAppConfig extends AbstractNosqlConfiguration {
    @Bean
    public NosqlDbConfig nosqlDbConfig() {
        return AppConfig.nosqlDBConfig;
    }
}