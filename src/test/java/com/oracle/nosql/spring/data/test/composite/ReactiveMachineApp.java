/*-
 * Copyright (c) 2020, 2025 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */
package com.oracle.nosql.spring.data.test.composite;

import com.oracle.nosql.spring.data.core.NosqlTemplate;
import com.oracle.nosql.spring.data.test.app.AppConfig;
import com.oracle.nosql.spring.data.test.reactive.ReactiveAppConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ReactiveAppConfig.class)

public class ReactiveMachineApp {
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private ReactiveMachineRepository repo;
    private Map<MachineId, Machine> machineCache;

    private static NosqlTemplate template;

    @BeforeAll
    public static void staticSetup() throws ClassNotFoundException {
        template = NosqlTemplate.create(AppConfig.nosqlDBConfig);
        template.dropTableIfExists(Machine.class.getSimpleName());
        template.createTableIfNotExists(template.
            getNosqlEntityInformation(Machine.class));
    }

    @BeforeEach
    public void setup() {
        repo.clearPreparedStatementsCache();
        repo.deleteAll();
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
                Mono<Machine> mono = repo.save(machine);
                StepVerifier.create(mono).expectNext(machine).verifyComplete();
                machineCache.put(machineId, machine);
            }
        }
        StepVerifier.create(repo.count()).expectNext(Long.valueOf(16)).verifyComplete();
    }

    @AfterAll
    public static void staticTeardown() {
        template.dropTableIfExists(Machine.class.getSimpleName());
    }

    @Test
    public void testCRUD() {
        //get machines
        machineCache.forEach(((machineId, machine) -> {
            StepVerifier.create(repo.findById(machineId)).expectNext(machine).verifyComplete();
        }));

        //update a machine
        Machine updateMachine = machineCache.get(new MachineId("version1",
                "name1"));
        updateMachine.setLocation("mumbai");
        repo.save(updateMachine);
        Mono<Machine> mono = repo.save(updateMachine);
        StepVerifier.create(mono).expectNext(updateMachine);

        //find by machineId
        MachineId machineId = new MachineId("version1", "name1");
        Machine machine = repo.findById(machineId).block();
        assertNotNull(machine);
        assertEquals(machineId, machine.getMachineId());

        //delete a row
        repo.deleteById(machineId).subscribe();
        StepVerifier.create(repo.existsById(machineId)).
                expectNext(Boolean.FALSE).verifyComplete();

        StepVerifier.create(repo.count()).expectNext(Long.valueOf(15)).verifyComplete();
    }

    @Test
    public void testCompositeKeyGet() {
        //find all machines with machineId.version=1
        List<Machine> machines =
                repo.findByMachineIdVersion("version1").collectList().block();
        assertEquals(4, machines.size());
        machines.forEach(m -> assertEquals(machineCache.get(m.getMachineId()), m));

        //find all machines with machineID.name=name3
        machines = repo.findByMachineIdName("name3").collectList().block();
        assertEquals(4, machines.size());
        machines.forEach(m -> assertEquals(machineCache.get(m.getMachineId()), m));

        //find all rows located in london
        machines = repo.findByLocation("london").collectList().block();
        assertEquals(8, machines.size());
        machines.forEach(m -> assertEquals(machineCache.get(m.getMachineId()), m));
    }

    @Test
    public void testCompositeKeyLogical() {
        //find all machines name=name1 and version=1
        List<Machine> machines = repo.findByMachineIdNameAndMachineIdVersion(
                "name1",
                "version1").collectList().block();
        assertEquals(1, machines.size());
        machines.forEach(m -> assertEquals(machineCache.get(m.getMachineId())
                , m));

        //find all machines name=name1 or version=1
        machines = repo.findByMachineIdNameOrMachineIdVersion(
                "name1",
                "version1").collectList().block();
        assertEquals(7, machines.size());
        machines.forEach(m -> assertEquals(machineCache.get(m.getMachineId())
                , m));

        //find all machines name=name1 and location=london
        machines = repo.findByMachineIdNameAndLocation(
                "name1",
                "newyork").collectList().block();
        assertEquals(4, machines.size());
        machines.forEach(m -> assertEquals(machineCache.get(m.getMachineId())
                , m));
    }

    @Test
    public void testCompositeSortingAndPaging() {
        //find all machines with machineId.version=1 with sort by name
        List<Machine> machines = repo.
                findByMachineIdVersionOrderByMachineIdNameAsc("version1").
                collectList().block();
        assertEquals(4, machines.size());
        machines.forEach(m -> assertEquals(machineCache.get(m.getMachineId()), m));
        //check sort by name is correct
        String prev = "";
        for (Machine m : machines) {
            String cur = m.getMachineId().getName();
            assertTrue(cur.compareTo(prev) >= 0);
            prev = cur;
        }
        machines.forEach(m -> assertEquals(machineCache.get(m.getMachineId()), m));


        machines = repo.findAll(Sort.by("machineId.name", "machineId" +
                        ".version"))
                .collectList().block();
        List<String> expectedNames = Arrays.asList(
                "name1", "name1", "name1", "name1",
                "name2", "name2", "name2", "name2",
                "name3", "name3", "name3", "name3",
                "name4", "name4", "name4", "name4");

        List<String> actualNames = new ArrayList<>();
        machines.forEach(m -> actualNames.add(m.getMachineId().getName()));
        assertEquals(expectedNames, actualNames);
    }

    @Test
    public void testIgnoreCase() {
        //ignore case
        List<Machine> machines = repo.
                findByMachineIdNameIgnoreCase("NaMe1").collectList().block();
        assertEquals(4, machines.size());
        machines.forEach(m -> assertEquals(machineCache.get(m.getMachineId())
                , m));
    }

    @Test
    public void testNative() {
        List<Machine> machines = repo.
                findAllByLocationNative().collectList().block();
        assertEquals(8, machines.size());
        machines.forEach(m -> {
            assertNotNull(m.getMachineId());
            assertNotNull(m.getMachineId().getName());
            assertNotNull(m.getMachineId().getVersion());
        });

        machines = repo.findByMachineIdNameNative("name3").collectList().block();
        assertEquals(4, machines.size());
        machines.forEach(m -> assertEquals(machineCache.get(m.getMachineId()), m));
    }
}
