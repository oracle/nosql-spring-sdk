/*-
 * Copyright (c) 2020, 2023 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */

package com.oracle.nosql.spring.data.test.composite;

import com.oracle.nosql.spring.data.core.NosqlTemplate;
import com.oracle.nosql.spring.data.core.mapping.NosqlId;
import com.oracle.nosql.spring.data.core.mapping.NosqlTable;
import com.oracle.nosql.spring.data.repository.NosqlRepository;
import com.oracle.nosql.spring.data.repository.Query;
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

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNull;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = AppConfig.class)
public class MachineAppWithoutAnnotation {
    @Autowired
    private MachineRepositoryWithoutAnnotation repo;

    private static NosqlTemplate template;

    @BeforeClass
    public static void staticSetup() throws ClassNotFoundException {
        template = NosqlTemplate.create(AppConfig.nosqlDBConfig);
    }

    @Before
    public void setup() {
        template.dropTableIfExists(MachineWithoutAnnotation.class.getSimpleName());
    }

    @After
    public void teardown() {
        template.dropTableIfExists(MachineWithoutAnnotation.class.getSimpleName());
    }

    @Test
    public void testCRUD() {
        Map<MachineIdWithoutAnnotation, MachineWithoutAnnotation> map =
                new HashMap<>();

        //create machines
        for (int i = 1; i <= 4; i++) {
            for (int j = 1; j <= 4; j++) {
                MachineIdWithoutAnnotation machineId =
                        new MachineIdWithoutAnnotation();
                machineId.setName("name" + i);
                machineId.setVersion("version" + j);
                MachineWithoutAnnotation machine =
                        new MachineWithoutAnnotation();
                machine.setMachineId(machineId);
                machine.setLocation((i % 2 == 0) ? "london" : "newyork");
                repo.save(machine);
                map.put(machineId, machine);
            }
        }

        //get total count of records
        assertEquals(16, repo.count());

        //get machines
        map.forEach((machineId, machine) -> assertEquals(machine,
                repo.findById(machineId).orElse(null)));


        //update a machine
        MachineWithoutAnnotation updateMachine =
                map.get(new MachineIdWithoutAnnotation(
                "version1",
                "name1"));
        updateMachine.setLocation("mumbai");
        repo.save(updateMachine);
        assertEquals(updateMachine,
                repo.findById(new MachineIdWithoutAnnotation("version1",
                        "name1")).orElse(null));


        //find all machines with machineId.version=1
        List<MachineWithoutAnnotation> res =
                repo.findByMachineIdVersionOrderByMachineIdNameAsc("version1");
        assertEquals(4, res.size());
        res.forEach(m -> assertEquals(map.get(m.getMachineId()), m));

        //find all machines with machineId.version=2
        res = repo.findByMachineIdVersionOrderByMachineIdNameAsc("version2");
        assertEquals(4, res.size());
        res.forEach(m -> assertEquals(map.get(m.getMachineId()), m));


        //find all machines with machineID.name=name3
        res = repo.findByMachineIdName("name3");
        assertEquals(4, res.size());
        res.forEach(m -> assertEquals(map.get(m.getMachineId()), m));


        //find all rows located in mumbai
        res = repo.findByLocation("mumbai");
        assertEquals(1, res.size());
        res.forEach(m -> assertEquals(map.get(m.getMachineId()), m));

        //find all machines name=name1 and version=1
        res = repo.findByMachineIdNameAndMachineIdVersion(
                "name1",
                "version1");
        assertEquals(1, res.size());
        res.forEach(m -> assertEquals(map.get(m.getMachineId()), m));

        //native query
        res = repo.findByVersionNative();
        assertEquals(4, res.size());
        res.forEach(m -> assertEquals(map.get(m.getMachineId()), m));

        //ignore case
        res = repo.findByMachineIdNameIgnoreCase("NaMe1");
        assertEquals(4, res.size());
        res.forEach(m -> assertEquals(map.get(m.getMachineId()), m));

        Sort sort = Sort.by(Sort.Direction.DESC, "machineId.version");
        Pageable pageable = PageRequest.of(0, 2, sort);
        Page<MachineWithoutAnnotation> pageByNameQuery =
                repo.findByMachineIdName(
                "name1",
                pageable);
        for (int page = 1; !pageByNameQuery.isEmpty(); page++) {
            for (MachineWithoutAnnotation m : pageByNameQuery) {
                assertEquals(map.get(m.getMachineId()), m);
            }
            pageable = PageRequest.of(page, 2, sort);
            pageByNameQuery = repo.findByMachineIdName("linux", pageable);
        }

        //delete some rows
        repo.deleteById(new MachineIdWithoutAnnotation("version1", "name1"));
        assertNull(repo.findById(new MachineIdWithoutAnnotation("version1",
                "name1")).orElse(null));

        repo.deleteById(new MachineIdWithoutAnnotation("version2", "name2"));
        assertNull(repo.findById(new MachineIdWithoutAnnotation("version2",
                "name2")).orElse(null));

        assertEquals(14, repo.count());
    }

    @NosqlTable(autoCreateTable = true, readUnits = 100, writeUnits = 100,
            storageGB = 1)
    public static class MachineWithoutAnnotation {
        @NosqlId
        MachineIdWithoutAnnotation machineId;
        private String location;

        public MachineWithoutAnnotation() {
        }

        public MachineIdWithoutAnnotation getMachineId() {
            return machineId;
        }

        public void setMachineId(MachineIdWithoutAnnotation machineId) {
            this.machineId = machineId;
        }

        public String getLocation() {
            return location;
        }

        public void setLocation(String location) {
            this.location = location;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof MachineWithoutAnnotation)) {
                return false;
            }
            MachineWithoutAnnotation that = (MachineWithoutAnnotation) o;
            return Objects.equals(machineId, that.machineId) &&
                    Objects.equals(location, that.location);
        }

        @Override
        public int hashCode() {
            return Objects.hash(machineId, location);
        }
    }

    public static class MachineIdWithoutAnnotation implements Serializable {
        private String version;
        private String name;

        public MachineIdWithoutAnnotation() {
        }

        public MachineIdWithoutAnnotation(String version, String name) {
            this.version = version;
            this.name = name;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof MachineIdWithoutAnnotation)) {
                return false;
            }
            MachineIdWithoutAnnotation that = (MachineIdWithoutAnnotation) o;
            return Objects.equals(version, that.version) &&
                    Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(version, name);
        }
    }

    public static interface MachineRepositoryWithoutAnnotation extends
            NosqlRepository<MachineWithoutAnnotation, MachineIdWithoutAnnotation> {
        List<MachineWithoutAnnotation> findByLocation(String location);

        List<MachineWithoutAnnotation> findByMachineIdVersionOrderByMachineIdNameAsc(String version);

        List<MachineWithoutAnnotation> findByMachineIdName(String name);

        Page<MachineWithoutAnnotation> findByMachineIdName(String name,
                                                           Pageable pageable);

        List<MachineWithoutAnnotation> findByMachineIdNameAndMachineIdVersion(String name,
                                                                              String version);

        @Query("SELECT * FROM MachineWithoutAnnotation m WHERE m" +
                ".VERSION='version1'")
        List<MachineWithoutAnnotation> findByVersionNative();

        List<MachineWithoutAnnotation> findByMachineIdNameIgnoreCase(String name);

    }
}
