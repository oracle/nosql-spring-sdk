/*-
 * Copyright (c) 2020, 2024 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */
package com.oracle.nosql.spring.data.test.id;

import com.oracle.nosql.spring.data.config.AbstractNosqlConfiguration;
import com.oracle.nosql.spring.data.config.NosqlDbConfig;
import com.oracle.nosql.spring.data.repository.config.EnableNosqlRepositories;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableNosqlRepositories
public class AppConfig
    extends AbstractNosqlConfiguration {

    public static NosqlDbConfig nosqlDBConfig =
        com.oracle.nosql.spring.data.test.app.AppConfig.nosqlDBConfig;

    @Bean
    public NosqlDbConfig nosqlDbConfig() {
        return nosqlDBConfig;
    }
}
