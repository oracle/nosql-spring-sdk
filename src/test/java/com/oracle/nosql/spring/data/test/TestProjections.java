/*-
 * Copyright (c) 2020, 2026 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */
package com.oracle.nosql.spring.data.test;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Point;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@ExtendWith(SpringExtension.class)
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


    // Without any id fields
    @Test
    public void testCustomerProjectionView() {
        List<CustomerView> smiths = nosqlRepo.findAllByLastName("Smith");

        assertEquals(3, smiths.size());
        assertTrue(smiths.stream()
            .allMatch( cv -> "Smith".equals(cv.getLastName())));
    }

    @Test
    public void testCustomerProjection() {
        List<CustomerProjection> smiths = nosqlRepo.getAllByLastName("Smith");

        assertEquals(3, smiths.size());
        assertTrue(smiths.stream()
            .allMatch( cv -> "Smith".equals(cv.getLastName())));
    }

    @Test
    public void testDistinctProjectionView() {
        List<CustomerView> smiths =
            nosqlRepo.findAllDistinctByLastName("Smith");

        assertEquals(1, smiths.size());
        assertTrue(smiths.stream()
            .allMatch( cv -> "Smith".equals(cv.getLastName())));
    }

    @Test
    public void testDistinctProjection() {
        List<CustomerProjection> smiths =
            nosqlRepo.getAllDistinctByLastName("Smith");

        assertEquals(2, smiths.size());
        assertTrue(smiths.stream()
            .allMatch( cv -> "Smith".equals(cv.getLastName())));
    }

    // with id field
    @Test
    public void testCustomerProjectionViewId() {
        List<CustomerViewWithId> smiths = nosqlRepo.queryByLastName("Smith");

        assertEquals(3, smiths.size());
        assertTrue(smiths.stream()
            .allMatch( cv -> "Smith".equals(cv.getLastName())));
    }

    @Test
    public void testCustomerProjectionId() {
        List<CustomerProjectionWithId> smiths = nosqlRepo.readByLastName("Smith");

        assertEquals(3, smiths.size());
        assertTrue(smiths.stream()
            .allMatch( cv -> "Smith".equals(cv.getLastName())));
    }

    @Test
    public void testDistinctProjectionViewId() {
        List<CustomerViewWithId> smiths =
            nosqlRepo.getDistinctByLastName("Smith");

        assertEquals(3, smiths.size());
        assertTrue(smiths.stream()
            .allMatch( cv -> "Smith".equals(cv.getLastName())));
    }

    @Test
    public void testDistinctProjectionId() {
        List<CustomerProjectionWithId> smiths =
            nosqlRepo.queryDistinctByLastName("Smith");

        assertEquals(3, smiths.size());
        assertTrue(smiths.stream()
            .allMatch( cv -> "Smith".equals(cv.getLastName())));
    }
}
