/*-
 * Copyright (c) 2020, 2023 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */

package com.oracle.nosql.spring.data.test.composite;

import com.oracle.nosql.spring.data.core.NosqlTemplate;
import com.oracle.nosql.spring.data.test.app.AppConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = AppConfig.class)
public class MachineApp {
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private MachineRepository repo;
    private Map<MachineId, Machine> machineCache;


    private static NosqlTemplate template;

    @BeforeClass
    public static void staticSetup() throws ClassNotFoundException {
        template = NosqlTemplate.create(AppConfig.nosqlDBConfig);
    }

    @Before
    public void setup() {
        template.dropTableIfExists(Machine.class.getSimpleName());

        machineCache = new HashMap<>();
        List<IpAddress> routeAddress = new ArrayList<>();
        routeAddress.add(new IpAddress("127.0.0.1"));
        routeAddress.add(new IpAddress("host1"));
        routeAddress.add(new IpAddress("host2"));

        //create machines
        for (int i = 1; i <= 4; i++) {
            for (int j = 1; j <= 4; j++) {
                MachineId machineId = new MachineId();
                machineId.setName("name" + i);
                machineId.setVersion("version" + j);
                Machine machine = new Machine(machineId, (i % 2 == 0) ?
                        "london" : "newyork", routeAddress.get(0),
                        routeAddress);
                repo.save(machine);
                machineCache.put(machineId, machine);
            }
        }

        //get total count of records
        assertEquals(16, repo.count());

    }

    @After
    public void teardown() {
        template.dropTableIfExists(Machine.class.getSimpleName());
    }

    @Test
    public void testCRUD() {
        //get machines
        machineCache.forEach((machineId, machine) -> assertEquals(machine,
                repo.findById(machineId).orElse(null)));

        //update a machine
        Machine updateMachine = machineCache.get(new MachineId("version1",
                "name1"));
        updateMachine.setLocation("mumbai");
        repo.save(updateMachine);
        assertEquals(updateMachine,
                repo.findById(new MachineId("version1", "name1")).orElse(null));

        //find bases on machineId
        MachineId machineId = new MachineId("version1", "name1");
        Optional<Machine> row = repo.findById(machineId);
        assertTrue(row.isPresent());
        assertEquals(machineCache.get(machineId), row.get());

        //delete some rows
        repo.deleteById(new MachineId("version1", "name1"));
        assertNull(repo.findById(new MachineId("version1", "name1")).orElse(null));

        repo.deleteById(new MachineId("version2", "name2"));
        assertNull(repo.findById(new MachineId("version2", "name2")).orElse(null));

        assertEquals(14, repo.count());
    }

    @Test
    public void testCompositeKeyGet() {
        Map<MachineId, Machine> map = new HashMap<>();
        List<IpAddress> routeAddress = new ArrayList<>();
        routeAddress.add(new IpAddress("127.0.0.1"));
        routeAddress.add(new IpAddress("host1"));
        routeAddress.add(new IpAddress("host2"));

        //create machines
        for (int i = 1; i <= 4; i++) {
            for (int j = 1; j <= 4; j++) {
                MachineId machineId = new MachineId();
                machineId.setName("name" + i);
                machineId.setVersion("version" + j);
                Machine machine = new Machine(machineId, (i % 2 == 0) ?
                        "london" : "newyork", routeAddress.get(0),
                        routeAddress);
                repo.save(machine);
                map.put(machineId, machine);
            }
        }

        //get total count of records
        assertEquals(16, repo.count());

        //find all machines with machineId.version=1
        List<Machine> machines = repo.findByMachineIdVersion("version1");
        assertEquals(4, machines.size());
        machines.forEach(m -> assertEquals(map.get(m.getMachineId()), m));


        //find all machines with machineID.name=name3
        machines = repo.findByMachineIdName("name3");
        assertEquals(4, machines.size());
        machines.forEach(m -> assertEquals(map.get(m.getMachineId()), m));

        //find all rows located in london
        machines = repo.findByLocation("london");
        assertEquals(8, machines.size());
        machines.forEach(m -> assertEquals(map.get(m.getMachineId()), m));
    }

    @Test
    public void testCompositeKeyLogical() {
        //find all machines name=name1 and version=1
        List<Machine> machines = repo.findByMachineIdNameAndMachineIdVersion(
                "name1",
                "version1");
        assertEquals(1, machines.size());
        machines.forEach(m -> assertEquals(machineCache.get(m.getMachineId())
                , m));

        //find all machines name=name1 or version=1
        machines = repo.findByMachineIdNameOrMachineIdVersion(
                "name1",
                "version1");
        assertEquals(7, machines.size());
        machines.forEach(m -> assertEquals(machineCache.get(m.getMachineId())
                , m));

        //find all machines name=name1 and location=london
        machines = repo.findByMachineIdNameAndLocation(
                "name1",
                "newyork");
        assertEquals(4, machines.size());
        machines.forEach(m -> assertEquals(machineCache.get(m.getMachineId())
                , m));
    }

    @Test
    public void testCompositeSortingAndPaging() {
        //find all machines with machineId.version=1 with sort by name
        List<Machine> res =
                repo.findByMachineIdVersionOrderByMachineIdNameAsc("version1");
        assertEquals(4, res.size());
        res.forEach(m -> assertEquals(machineCache.get(m.getMachineId()), m));
        //check sort by name is correct
        String prev = "";
        for (Machine m : res) {
            String cur = m.getMachineId().getName();
            assertTrue(cur.compareTo(prev) >= 0);
            prev = cur;
        }
        res.forEach(m -> assertEquals(machineCache.get(m.getMachineId()), m));


        Sort sort = Sort.by(Sort.Direction.DESC, "machineId.version");
        Pageable pageable = PageRequest.of(0, 2, sort);
        Page<Machine> pageByNameQuery = repo.findByMachineIdName("name1",
                pageable);
        for (int page = 1; !pageByNameQuery.isEmpty(); page++) {
            for (Machine m : pageByNameQuery) {
                assertEquals(machineCache.get(m.getMachineId()), m);
            }
            pageable = PageRequest.of(page, 2, sort);
            pageByNameQuery = repo.findByMachineIdName("linux", pageable);
        }
    }

    @Test
    public void testFindAllSort() {
        Iterable<Machine> machines = repo.findAll(
                Sort.by("machineId.name", "machineId.version"));
        List<Machine> machineList = new ArrayList<>();
        machines.forEach(machineList::add);
        machineList.forEach(m -> assertEquals(machineCache.get(m.getMachineId()), m));

        List<String> expectedNames = Arrays.asList(
                "name1", "name1", "name1", "name1",
                "name2", "name2", "name2", "name2",
                "name3", "name3", "name3", "name3",
                "name4", "name4", "name4", "name4");
        List<String> actualNames = new ArrayList<>();
        machineList.forEach(m -> actualNames.add(m.getMachineId().getName()));
        assertEquals(expectedNames, actualNames);
    }

    @Test
    public void testNative() {
        //native query
        List<Machine> machines = repo.findByVersionNative();
        assertEquals(4, machines.size());
        machines.forEach(m -> assertEquals(machineCache.get(m.getMachineId())
                , m));
    }

    @Test
    public void testIgnoreCase() {
        //ignore case
        List<Machine> machines = repo.findByMachineIdNameIgnoreCase("NaMe1");
        assertEquals(4, machines.size());
        machines.forEach(m -> assertEquals(machineCache.get(m.getMachineId())
                , m));
    }

    @Test
    public void testInterfaceProjection() {
        List<MachineProjection> projections = repo.findAllByLocation("london");
        projections.forEach(m -> {
            assertNotNull(m.getMachineId().getName());
            assertNotNull(m.getMachineId().getVersion());
            assertEquals("london", m.getLocation());
        });
    }

    @Test
    public void testDTOProjection() {
        List<MachineProjectionDTO> projections = repo.findAllByMachineIdName(
                "name1");
        projections.forEach(m -> {
            assertEquals("name1", m.getMachineId().getName());
            assertNotNull(m.getMachineId().getVersion());
            assertNotNull(m.getLocation());
        });
    }
}
