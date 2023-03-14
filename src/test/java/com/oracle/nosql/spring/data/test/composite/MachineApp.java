/*-
 * Copyright (c) 2020, 2023 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */

package com.oracle.nosql.spring.data.test.composite;

import com.oracle.nosql.spring.data.test.app.AppConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNull;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = AppConfig.class)
public class MachineApp {
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private MachineRepository repo;

    @Test
    public void testCRUD() {
        repo.deleteAll();

        //create a row
        MachineId machineId = new MachineId("1",  "linux");
        IpAddress hostAddress = new IpAddress("127.0.0.1");
        List<IpAddress> routeAddress = new ArrayList<>();
        routeAddress.add(new IpAddress("host1"));
        routeAddress.add(new IpAddress("host2"));

        Machine machine = new Machine(machineId, "phoenix",
                hostAddress, routeAddress);
        repo.save(machine);

        Machine retMachine = repo.findById(machineId).orElse(null);
        assertEquals(machine, retMachine);

        //update the row
        machine.setLocation("london");
        machine.setHostAddress(new IpAddress("localhost"));
        repo.save(machine);
        retMachine = repo.findById(machineId).orElse(null);
        assertEquals(machine, retMachine);

        //insert 6 more rows
        repo.save(new Machine(
                new MachineId("2", "linux"),
                "london",
                new IpAddress("192.168.56.1"),
                null)
        );

        repo.save(new Machine(
                new MachineId("3", "linux"),
                "london",
                new IpAddress("192.168.56.2"),
                null)
        );

        repo.save(new Machine(
                new MachineId("1", "windows"),
                "mumbai",
                new IpAddress("92.68.56.10"),
                null)
        );

        repo.save(new Machine(
                new MachineId("2", "windows"),
                "mumbai",
                new IpAddress("92.68.56.100"),
                null)
        );

        repo.save(new Machine(
                new MachineId("3", "windows"),
                "mumbai",
                new IpAddress("92.168.56.10"),
                new ArrayList<IpAddress>())
        );

        repo.save(new Machine(
                new MachineId("4", "mac"),
                "newyork",
                new IpAddress("92.68.56.10"),
                null)
        );

        //get total count of records
        assertEquals(7, repo.count());

        //find all machines with machineId.version=1
        assertEquals(2, repo.findByMachineIdVersion("1").size());

        //find all machines with machineId.version=2
        assertEquals(2, repo.findByMachineIdVersion("2").size());

        //find all machines with machineId.version=4
        assertEquals(1, repo.findByMachineIdVersion("4").size());

        //find all machines with machineID.name=linux
        assertEquals(3, repo.findByMachineIdName("linux").size());

        //find all rows located in london
        assertEquals(3, repo.findByLocation("london").size());

        //find all machines name=linux and version=1
        assertEquals(1, repo.findByMachineIdNameAndMachineIdVersion("linux",
                "1").size());

        //native query
        assertEquals(2, repo.findByVersionNative().size());

        //delete row
        repo.deleteById(machineId);
        assertNull(repo.findById(machineId).orElse(null));

        repo.deleteById(new MachineId("1", "windows"));
        assertNull(repo.findById(machineId).orElse(null));
    }
}
