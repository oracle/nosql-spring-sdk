/*-
 * Copyright (c) 2020, 2024 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */
package com.oracle.nosql.spring.data.test.app;

import java.io.IOException;

import com.oracle.nosql.spring.data.config.NosqlDbConfig;
import com.oracle.nosql.spring.data.core.mapping.NosqlCapacityMode;
import com.oracle.nosql.spring.data.repository.config.EnableNosqlRepositories;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
/* Looks for configuration properties in this file. */
@PropertySource(value = "classpath:application.properties")
@EnableNosqlRepositories(basePackageClasses =
        {com.oracle.nosql.spring.data.test.TestTTL.class},
        considerNestedRepositories = true)
public class AppConfig extends AppConfigBase {

    /* Get values specified in application.properties file or use
    specified default otherwise. */
    @Value("${test.config.defaultStorageGB:25}")
    private int defaultStorageGB;
    @Value("${test.config.defaultCapacityMode:PROVISIONED}")
    private String defaultCapacityMode;
    @Value("${test.config.defaultReadUnits:50}")
    private int defaultReadUnits;
    @Value("${test.config.defaultWriteUnits:50}")
    private int defaultWriteUnits;

    public static NosqlDbConfig nosqlDBConfig;
    static {

//// On-prem configuration:
//        nosqlDBConfig =
//            new NosqlDbConfig("localhost:8080",
//                new StoreAccessTokenProvider());


//// Configuration for cloud service:
//// Note: Requires account to access Oracle NoSQL Cloud Service.
//// Use these pages to configure your setup:
////   - blog entry: 15 minutes to Hello World:
////       https://blogs.oracle.com/nosql/15-minutes-to-hello-world
////   - instructions to generate private/public keys and key fingerprint:
////       https://docs.cloud.oracle.com/en-us/iaas/Content/API/Concepts/apisigningkey.htm#How
////   - SDK and CLI Configuration File:
////       https://docs.cloud.oracle.com/en-us/iaas/Content/API/Concepts/sdkconfig.htm
//// Note: Requires ~/.oci/config with above info.
//        try {
//            nosqlDBConfig =
//                new NosqlDbConfig(new NoSQLHandleConfig(
//                    "endpoint",
//                    new SignatureProvider(
//                        "~/.oci/config", "DEFAULT")
//                ));
//        }
//        catch (IOException e) {
//            e.printStackTrace();
//        }

        // Setup for automatic testing see test profiles in pom.xml
        staticSetup();

        if (onprem) {
            if (secure) {
                nosqlDBConfig = NosqlDbConfig.createProxyConfig(endpoint,
                    user, password.toCharArray());
            } else {
                nosqlDBConfig = NosqlDbConfig.createProxyConfig(endpoint);
            }
        } else {
            if (secure) {
                try {
                    /* cloud */
                    nosqlDBConfig = NosqlDbConfig.createCloudConfig(endpoint,
                        cloudConfig);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                /* cloud simulator */
                nosqlDBConfig = NosqlDbConfig.createCloudSimConfig(endpoint);
            }
        }

        nosqlDBConfig.setTableReqTimeout(DEFAULT_REQ_TIMEOUT);
        nosqlDBConfig.setTableReqPollInterval(DEFAULT_REQ_POLL_INTERVAL);
    }


    @Bean
    public NosqlDbConfig nosqlDbConfig() {

        nosqlDBConfig.setDefaultCapacityMode(
            "ON_DEMAND".equals(this.defaultCapacityMode) ?
                NosqlCapacityMode.ON_DEMAND : NosqlCapacityMode.PROVISIONED);
        nosqlDBConfig.setDefaultStorageGB(this.defaultStorageGB);
        nosqlDBConfig.setDefaultReadUnits(this.defaultReadUnits);
        nosqlDBConfig.setDefaultWriteUnits(this.defaultWriteUnits);

        return nosqlDBConfig;
    }
}
