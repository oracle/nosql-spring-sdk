/*-
 * Copyright (c) 2020, 2024 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */

package com.oracle.nosql.spring.data.test;

import com.oracle.nosql.spring.data.core.NosqlTemplate;
import com.oracle.nosql.spring.data.core.mapping.NosqlId;
import com.oracle.nosql.spring.data.core.mapping.NosqlTable;
import com.oracle.nosql.spring.data.repository.NosqlRepository;
import com.oracle.nosql.spring.data.repository.support.NosqlEntityInformation;
import com.oracle.nosql.spring.data.test.app.AppConfig;
import oracle.nosql.driver.TimeToLive;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.fail;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = AppConfig.class)
public class TestTTL {
    @Autowired
    private EntityWith10DaysTTLRepo with10DaysTTLRepo;
    @Autowired
    private EntityWithDefaultTTLRepo defaultTTLRepo;

    private static NosqlTemplate template;

    @BeforeClass
    public static void staticSetup() throws ClassNotFoundException {
        template = NosqlTemplate.create(AppConfig.nosqlDBConfig);
    }

    @Before
    public void setup() {
        template.dropTableIfExists(EntityWith10DaysTTL.class.getSimpleName());
        template.dropTableIfExists(EntityWithDefaultTTL.class.getSimpleName());
        template.dropTableIfExists(EntityWithNegativeTTL.class.getSimpleName());

        template.createTableIfNotExists(template.
                getNosqlEntityInformation(EntityWith10DaysTTL.class));
        template.createTableIfNotExists(template.
                getNosqlEntityInformation(EntityWithDefaultTTL.class));
    }

    @After
    public void teardown() {
        template.dropTableIfExists(EntityWith10DaysTTL.class.getSimpleName());
        template.dropTableIfExists(EntityWithDefaultTTL.class.getSimpleName());
        template.dropTableIfExists(EntityWithNegativeTTL.class.getSimpleName());
    }

    @Test
    public void test10DaysTTL() {
        EntityWith10DaysTTL entity = new EntityWith10DaysTTL(
                "John", 20);
        //Save entity to database
        with10DaysTTLRepo.save(entity);

        //Get entity information and check TTL is correct
        NosqlEntityInformation<EntityWith10DaysTTL, ?> entityInformation =
                template.getNosqlEntityInformation(EntityWith10DaysTTL.class);
        assertEquals(TimeToLive.ofDays(10).toString(),
                entityInformation.getTtl().toString());

        //Get entity from database
        assertNotNull(with10DaysTTLRepo.findById(entity.getId()));

    }

    @Test
    public void testDefaultTTL() {
        EntityWithDefaultTTL entity = new EntityWithDefaultTTL("John", 20);

        //Save entity to database
        defaultTTLRepo.save(entity);

        //Get entity information and check TTL is correct
        NosqlEntityInformation<EntityWithDefaultTTL, ?> entityInformation =
                template.getNosqlEntityInformation(EntityWithDefaultTTL.class);
        assertEquals(TimeToLive.DO_NOT_EXPIRE.toString(),
                entityInformation.getTtl().toString());

        //Get entity from database
        assertNotNull(defaultTTLRepo.findById(entity.getId()));
    }

    @Test
    public void testInvalidTtl() {
        try {
            template.getNosqlEntityInformation(EntityWithNegativeTTL.class);
            fail("Expecting IllegalArgumentException but didn't get");

        } catch (IllegalArgumentException iae) {
            //expected
        }
    }

    @NosqlTable(readUnits = 100, writeUnits = 100, storageGB = 1, ttl = 10,
            ttlUnit = NosqlTable.TtlUnit.DAYS)
    public static class EntityWith10DaysTTL {
        @NosqlId(generated = true)
        private long id;
        private final String firstName;
        private final int age;

        EntityWith10DaysTTL(String firstName, int age) {
            this.firstName = firstName;
            this.age = age;
        }

        public long getId() {
            return id;
        }
    }

    interface EntityWith10DaysTTLRepo extends
            NosqlRepository<EntityWith10DaysTTL, Long> {
    }

    @NosqlTable(readUnits = 100, writeUnits = 100, storageGB = 1)
    public static class EntityWithDefaultTTL {
        @NosqlId(generated = true)
        private long id;
        private final String firstName;
        private final int age;

        EntityWithDefaultTTL(String firstName, int age) {
            this.firstName = firstName;
            this.age = age;
        }

        public long getId() {
            return id;
        }
    }

    interface EntityWithDefaultTTLRepo extends
            NosqlRepository<EntityWithDefaultTTL, Long> {
    }

    @NosqlTable(readUnits = 100, writeUnits = 100, storageGB = 1, ttl = -1)
    public static class EntityWithNegativeTTL {
        @NosqlId(generated = true)
        private long id;
        private final String firstName;
        private final int age;

        public EntityWithNegativeTTL(String firstName, int age) {
            this.firstName = firstName;
            this.age = age;
        }

        public long getId() {
            return id;
        }

        public String getFirstName() {
            return firstName;
        }

        public int getAge() {
            return age;
        }
    }
}
