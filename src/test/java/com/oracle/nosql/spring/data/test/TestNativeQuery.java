/*-
 * Copyright (c) 2020, 2025 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */
package com.oracle.nosql.spring.data.test;

import java.util.Arrays;
import java.util.List;

import oracle.nosql.driver.values.StringValue;
import com.oracle.nosql.spring.data.test.app.AppConfig;
import com.oracle.nosql.spring.data.test.app.Customer;
import com.oracle.nosql.spring.data.test.app.CustomerRepository;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
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


    @Before
    public void before() {
        nosqlRepo.deleteAll();
        c1.customerId = c2.customerId = c3.customerId =
            c4.customerId = c5.customerId = c6.customerId = c7.customerId = 0;
        nosqlRepo.saveAll(Arrays.asList(c));
    }


    @After
    public void after() {
        nosqlRepo.deleteAll();
    }

    @Test
    public void testSimple() {
        final List<Customer> johns = nosqlRepo.findCustomersByFirstNameJohn();

        Assert.assertTrue(johns.contains(c3) && johns.contains(c4));
    }

    @Test
    public void testWithOneParam() {
        final List<Customer> johns = nosqlRepo.findCustomersByFirstName("John");

        Assert.assertTrue(johns.size() == 2 &&
            johns.contains(c3) && johns.contains(c4));
    }

    @Test
    public void testWithTwoParams() {
        final List<Customer> johns =
            nosqlRepo.findCustomersWithLastAndFirstNames(
            "Doe", "John");

        Assert.assertTrue(johns.size() == 1 && johns.contains(c4));
    }

// todo Enable when positional bind params are supported
//    @Test
//    public void testWithTwoParamsQm() {
//
//        List<Customer> johns = nosqlRepo.findCustomersWithFirstLast(
//            "John", "Doe");
//
//        Assert.assertTrue(johns.size() == 1 && johns.contains(c4));
//    }

    @Test
    public void testWithTwoNosqlValueParams() {
        final List<Customer> johns =
            nosqlRepo.findCustomersWithLastAndFirstNosqlValues(
            new StringValue("Smith"), new StringValue("John"));

        Assert.assertTrue(johns.size() == 1 && johns.contains(c3));
    }
}
