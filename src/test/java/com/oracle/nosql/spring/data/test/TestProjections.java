/*-
 * Copyright (c) 2020, 2025 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */
package com.oracle.nosql.spring.data.test;


import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.oracle.nosql.spring.data.test.app.Address;
import com.oracle.nosql.spring.data.test.app.AppConfig;
import com.oracle.nosql.spring.data.test.app.Customer;
import com.oracle.nosql.spring.data.test.app.CustomerProjection;
import com.oracle.nosql.spring.data.test.app.CustomerProjectionWithId;
import com.oracle.nosql.spring.data.test.app.CustomerRepository;
import com.oracle.nosql.spring.data.test.app.CustomerView;
import com.oracle.nosql.spring.data.test.app.CustomerViewWithId;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Point;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = AppConfig.class)
public class TestProjections {
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private CustomerRepository nosqlRepo;

    public static Customer c1, c2, c3, c4, c5, c6, c7;
    private static Customer[] c;


    static {
        c1 = new Customer("Alice", "Smith", null);
        c1.kids = 1;

        c2 = new Customer("Bob", "Smith", null);
        c2.address = new Address("Main St", "Seattle");

        c3 = new Customer("Bob", "Smith", null);
        c3.address = new Address("Colorado St", "Boulder");

        c4 = new Customer("John", "Doe", null);
        c4.kids = 4;
        c4.weight = 4.4f;
        c4.biField = BigInteger.valueOf(4);

        c5 = new Customer("Clark", "Kent", null);
        c5.kids = 5;
        c5.coins = 5.55f;
        c5.bdField = BigDecimal.valueOf(5.05d);
        c5.birthDay = Date.from(Instant.parse("1977-04-18T05:00:00Z"));
        c5.address = new Address.USAddress("344 Clinton Street",
            "Metropolis", "NY", 21000);
        c5.address.geoJsonPoint = new Point(40.700960, -74.014504);

        c6 = new Customer("Berry", "Allan", null);
        c6.kids = 6;
        c6.biField = BigInteger.valueOf(6);
        c6.birthDay = Date.from(Instant.parse("1989-03-14T06:00:00Z"));
        c6.address = new Address.USAddress("Some St",
            "Central City", "NY", 21001);
        c6.address.geoJsonPoint = new Point(40.698472, -74.038335);

        c7 = new Customer("Diana", "Prince", null);
        c7.kids = 7;
        c7.bdField = BigDecimal.valueOf(7.07d);
        c7.birthDay = Date.from(Instant.parse("1979-06-06T07:00:00Z"));
        c7.address = new Address("Main St", "Paradise Island");

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


    // Without any id fields
    @Test
    public void testCustomerProjectionView() {
        List<CustomerView> smiths = nosqlRepo.findAllByLastName("Smith");

        Assert.assertEquals(3, smiths.size());
        Assert.assertTrue(smiths.stream()
            .allMatch( cv -> "Smith".equals(cv.getLastName())));
    }

    @Test
    public void testCustomerProjection() {
        List<CustomerProjection> smiths = nosqlRepo.getAllByLastName("Smith");

        Assert.assertEquals(3, smiths.size());
        Assert.assertTrue(smiths.stream()
            .allMatch( cv -> "Smith".equals(cv.getLastName())));
    }

    @Test
    public void testDistinctProjectionView() {
        List<CustomerView> smiths =
            nosqlRepo.findAllDistinctByLastName("Smith");

        Assert.assertEquals(1, smiths.size());
        Assert.assertTrue(smiths.stream()
            .allMatch( cv -> "Smith".equals(cv.getLastName())));
    }

    @Test
    public void testDistinctProjection() {
        List<CustomerProjection> smiths =
            nosqlRepo.getAllDistinctByLastName("Smith");

        Assert.assertEquals(2, smiths.size());
        Assert.assertTrue(smiths.stream()
            .allMatch( cv -> "Smith".equals(cv.getLastName())));
    }

    // with id field
    @Test
    public void testCustomerProjectionViewId() {
        List<CustomerViewWithId> smiths = nosqlRepo.queryByLastName("Smith");

        Assert.assertEquals(3, smiths.size());
        Assert.assertTrue(smiths.stream()
            .allMatch( cv -> "Smith".equals(cv.getLastName())));
    }

    @Test
    public void testCustomerProjectionId() {
        List<CustomerProjectionWithId> smiths = nosqlRepo.readByLastName("Smith");

        Assert.assertEquals(3, smiths.size());
        Assert.assertTrue(smiths.stream()
            .allMatch( cv -> "Smith".equals(cv.getLastName())));
    }

    @Test
    public void testDistinctProjectionViewId() {
        List<CustomerViewWithId> smiths =
            nosqlRepo.getDistinctByLastName("Smith");

        Assert.assertEquals(3, smiths.size());
        Assert.assertTrue(smiths.stream()
            .allMatch( cv -> "Smith".equals(cv.getLastName())));
    }

    @Test
    public void testDistinctProjectionId() {
        List<CustomerProjectionWithId> smiths =
            nosqlRepo.queryDistinctByLastName("Smith");

        Assert.assertEquals(3, smiths.size());
        Assert.assertTrue(smiths.stream()
            .allMatch( cv -> "Smith".equals(cv.getLastName())));
    }
}
