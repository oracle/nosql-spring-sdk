/*-
 * Copyright (c) 2020, 2026 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */
package com.oracle.nosql.spring.data.test.id;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.oracle.nosql.spring.data.core.NosqlTemplate;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = AppConfig.class)
public class TestIdTypes {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private RepoSensorIdStr idStrRepo;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private RepoSensorIdStrGenerated idStrGeneratedRepo;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private RepoSensorIdInt idIntRepo;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private RepoSensorIdLong idLongRepo;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private RepoSensorIdDouble idDoubleRepo;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private RepoSensorIdFloat idFloatRepo;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private RepoSensorIdBigInteger idBIRepo;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private RepoSensorIdBigDecimal idBDRepo;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private RepoSensorIdTimestamp idTimestampRepo;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private RepoSensorIdDate idDateRepo;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private RepoSensorIdInstant idInstantRepo;

    @AfterAll
    public static void after()
        throws ClassNotFoundException {
        NosqlTemplate template = NosqlTemplate.create(AppConfig.nosqlDBConfig);
        template.dropTableIfExists("SensorIdStr");
        template.dropTableIfExists("SensorIdStrGenerated");
        template.dropTableIfExists("SensorIdInt");
        template.dropTableIfExists("SensorIdLong");
        template.dropTableIfExists("SensorIdDouble");
        template.dropTableIfExists("SensorIdBigInteger");
        template.dropTableIfExists("SensorIdBigDecimal");
        template.dropTableIfExists("SensorIdDate");
        template.dropTableIfExists("SensorIdTimestamp");
        template.dropTableIfExists("SensorIdInstant");
    }

    @Test
    public void testIdStr() {
        SensorIdStr e = new SensorIdStr();
        e.name = "key1";
        e.temp = 100;
        e.time = 1;

        SensorIdStr r = idStrRepo.save(e);
        assertEquals(1, idStrRepo.count());

        assertEquals("key1", r.name);
        assertEquals(100, r.temp);
        assertEquals(1, r.time);
    }

    @Test
    public void testIdStrGenerated() {
        SensorIdStrGenerated e = new SensorIdStrGenerated();
        e.temp = 100;
        e.time = 1;

        SensorIdStrGenerated r = idStrGeneratedRepo.save(e);
        assertEquals(1, idStrGeneratedRepo.count());

        assertNotNull(r);
        assertNotNull(r.id);
        assertEquals(100, r.temp);
        assertEquals(1, r.time);

        assertEquals(36, e.id.length());
    }

    @Test
    public void testIdInt() {
        SensorIdInt e = new SensorIdInt();
        e.name = "key1";
        e.temp = 100;
        e.time = 1;

        SensorIdInt r = idIntRepo.save(e);
        assertEquals(1, idIntRepo.count());

        assertEquals("key1", r.name);
        assertEquals(100, r.temp);
        assertEquals(1, r.time);
    }

    @Test
    public void testIdLong() {
        SensorIdLong e = new SensorIdLong();
        e.name = "key1";
        e.temp = 100;
        e.time = 1;

        SensorIdLong r = idLongRepo.save(e);
        assertEquals(1, idLongRepo.count());

        assertEquals("key1", r.name);
        assertEquals(100, r.temp);
        assertEquals(1, r.time);
    }

    @Test
    public void testIdDouble() {
        SensorIdDouble e = new SensorIdDouble();
        e.name = "event1";
        e.temp = 100.1;
        e.time = 1;

        SensorIdDouble r = idDoubleRepo.save(e);
        assertEquals(1, idDoubleRepo.count());

        assertEquals("event1", r.name);
        assertEquals(100.1, r.temp, 0.01);
        assertEquals(1, r.time);
    }

    @Test
    public void testIdFloat() {
        SensorIdFloat e = new SensorIdFloat();
        e.name = "event1";
        e.temp = 202.22f;
        e.time = 2;

        SensorIdFloat r = idFloatRepo.save(e);
        assertEquals(1, idFloatRepo.count());

        assertEquals("event1", r.name);
        assertEquals(202.22f, r.temp, 0.001);
        assertEquals(2, r.time);
    }

    @Test
    public void testIdBI() {
        SensorIdBigInteger e = new SensorIdBigInteger();
        e.name = "key1";
        e.temp = BigInteger.valueOf(100);
        e.time = 1;

        SensorIdBigInteger r = idBIRepo.save(e);
        assertEquals(1, idBIRepo.count());

        assertEquals("key1", r.name);
        assertEquals(BigInteger.valueOf(100), r.temp);
        assertEquals(1, r.time);
    }

    @Test
    public void testIdBD() {
        SensorIdBigDecimal e = new SensorIdBigDecimal();
        e.name = "key1";
        e.temp = BigDecimal.valueOf(100.001);
        e.time = 1;

        SensorIdBigDecimal r = idBDRepo.save(e);
        assertEquals(1, idBDRepo.count());

        assertEquals("key1", r.name);
        assertEquals(BigDecimal.valueOf(100.001), r.temp);
        assertEquals(1, r.time);
    }

    @Test
    public void testIdDate() {
        SensorIdDate e = new SensorIdDate();
        e.name = "key1";
        e.temp = 100;
        e.time = Date.from(Instant.parse("2020-05-11T15:31:30.01Z"));

        SensorIdDate r = idDateRepo.save(e);
        assertEquals(1, idDateRepo.count());

        assertEquals("key1", r.name);
        assertEquals(100, r.temp);
        assertEquals(Date.from(Instant.parse("2020-05-11T15:31:30.01Z")), r.time);

        SensorIdDate read = idDateRepo.findById(e.time).get();
        assertEquals(e.name, read.name);
        assertEquals(e.temp, read.temp);
        assertEquals(e.time, read.time);
    }

    @Test
    public void testBetweenDates() {
        SensorIdDate e1 = new SensorIdDate();
        e1.name = "event1";
        e1.temp = 101;
        e1.time = Date.from(Instant.parse("2020-08-11T11:11:11Z"));

        SensorIdDate e2 = new SensorIdDate();
        e2.name = "event2";
        e2.temp = 102;
        e2.time = Date.from(Instant.parse("2020-08-12T12:12:12.2Z"));

        SensorIdDate e3 = new SensorIdDate();
        e3.name = "event3";
        e3.temp = 103;
        e3.time = Date.from(Instant.parse("2020-08-13T13:13:13.012Z"));

        SensorIdDate e4 = new SensorIdDate();
        e4.name = "event4";
        e4.temp = 104;
        e4.time = Date.from(Instant.parse("2020-08-14T14:14:14.0123Z"));

        idDateRepo.saveAll(Arrays.asList(e1, e2, e3, e4));

        List<SensorIdDate> list = idDateRepo.findByTimeBetween(
            Date.from(Instant.parse("2020-08-12T00:00:00Z")),
            Date.from(Instant.parse("2020-08-13T23:59:59.9Z"))
        );

        assertEquals(2, list.size());
        assertTrue(list.containsAll(Arrays.asList(e2, e3)));

        idDateRepo.deleteAll(Arrays.asList(e1, e2, e3, e4));
    }

    @Test
    public void testIdTimestamp() {
        SensorIdTimestamp e = new SensorIdTimestamp();
        e.name = "key1";
        e.temp = 100;
        e.time = Timestamp.valueOf("2020-08-11 15:31:30");

        SensorIdTimestamp r = idTimestampRepo.save(e);
        assertEquals(1, idTimestampRepo.count());

        assertEquals("key1", r.name);
        assertEquals(100, r.temp);
        assertEquals(Timestamp.valueOf("2020-08-11 15:31:30"), r.time);

        SensorIdTimestamp read = idTimestampRepo.findById(e.time).get();
        assertEquals(e.name, read.name);
        assertEquals(e.temp, read.temp);
        assertEquals(e.time, read.time);
    }

    @Test
    public void testIdInstant() {
        SensorIdInstant e = new SensorIdInstant();
        e.name = "key1";
        e.temp = 100;
        e.time = Instant.parse("2020-08-11T15:31:30.01Z");

        SensorIdInstant r = idInstantRepo.save(e);
        assertEquals(1, idInstantRepo.count());

        assertEquals("key1", r.name);
        assertEquals(100, r.temp);
        assertEquals(Instant.parse("2020-08-11T15:31:30.01Z"), r.time);

        SensorIdInstant read = idInstantRepo.findById(e.time).get();
        assertEquals(e.name, read.name);
        assertEquals(e.temp, read.temp);
        assertEquals(e.time, read.time);
    }

    /** Tests save(entity) with and without id on an entity without generated id */
    @Test
    public void testUpdate() {
        SensorIdInt s = new SensorIdInt();
        try {
            assertEquals(0, s.temp);
            SensorIdInt saved = idIntRepo.save(s);
            // Since SensorIdInt entity doesn't have an autogenerated field
            // it will be saved.
            assertEquals(0, saved.temp);
        } catch (IllegalArgumentException iae) {
            fail("Failed to throw IllegalArgumentException");
        }

        s.temp = 100;
        s.name = "Boiling Sensor";
        s.time = 1;

        SensorIdInt s2 = idIntRepo.save(s);

        assertEquals(s.temp, s2.temp);
        assertEquals(s.name, s2.name);
        assertEquals(s.time, s2.time);

        assertEquals(s.temp, 100);
        assertEquals(s.name, "Boiling Sensor");
        assertEquals(s.time, 1);

        s.name = "Temperature Sensor";
        s2 = idIntRepo.save(s);

        assertEquals(s.temp, s2.temp);
        assertEquals(s.name, s2.name);
        assertEquals(s.time, s2.time);

        assertEquals(s.temp, 100);
        assertEquals(s.name, "Temperature Sensor");
        assertEquals(s.time, 1);

        s2 = idIntRepo.findById(100).get();

        assertEquals(s2.temp, 100);
        assertEquals(s2.name, "Temperature Sensor");
        assertEquals(s2.time, 1);
    }
}
