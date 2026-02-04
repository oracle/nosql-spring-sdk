/*-
 * Copyright (c) 2020, 2026 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */
package com.oracle.nosql.spring.data.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import oracle.nosql.driver.values.StringValue;
import com.oracle.nosql.spring.data.test.app.AppConfig;
import com.oracle.nosql.spring.data.test.app.Customer;
import com.oracle.nosql.spring.data.test.app.CustomerRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = AppConfig.class)
public class TestNativeQuery {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private CustomerRepository nosqlRepo;

    private static Customer c1, c2, c3, c4, c5, c6, c7;
    private static Customer[] c;


    static {
        c1 = TestDynamicQuery.c1;

        c2 = TestDynamicQuery.c2;

        c3 = TestDynamicQuery.c3;

        c4 = TestDynamicQuery.c4;

        c5 = TestDynamicQuery.c5;

        c6 = TestDynamicQuery.c6;

        c7 = TestDynamicQuery.c7;

        c = new Customer[]{c1, c2, c3, c4, c5, c6, c7};
    }


    @BeforeEach
    public void before() {
        nosqlRepo.deleteAll();
        c1.customerId = c2.customerId = c3.customerId =
            c4.customerId = c5.customerId = c6.customerId = c7.customerId = 0;
        nosqlRepo.saveAll(Arrays.asList(c));
    }


    @AfterEach
    public void after() {
        nosqlRepo.deleteAll();
    }

    @Test
    public void testSimple() {
        final List<Customer> johns = nosqlRepo.findCustomersByFirstNameJohn();

        assertTrue(johns.contains(c3) && johns.contains(c4));
    }

    @Test
    public void testWithOneParam() {
        final List<Customer> johns = nosqlRepo.findCustomersByFirstName("John");

        assertTrue(johns.size() == 2 &&
            johns.contains(c3) && johns.contains(c4));
    }

    @Test
    public void testWithTwoParams() {
        final List<Customer> johns =
            nosqlRepo.findCustomersWithLastAndFirstNames(
            "Doe", "John");

        assertTrue(johns.size() == 1 && johns.contains(c4));
    }

// todo Enable when positional bind params are supported
//    @Test
//    public void testWithTwoParamsQm() {
//
//        List<Customer> johns = nosqlRepo.findCustomersWithFirstLast(
//            "John", "Doe");
//
//        assertTrue(johns.size() == 1 && johns.contains(c4));
//    }

    @Test
    public void testWithTwoNosqlValueParams() {
        final List<Customer> johns =
            nosqlRepo.findCustomersWithLastAndFirstNosqlValues(
            new StringValue("Smith"), new StringValue("John"));

        assertTrue(johns.size() == 1 && johns.contains(c3));
    }
}
