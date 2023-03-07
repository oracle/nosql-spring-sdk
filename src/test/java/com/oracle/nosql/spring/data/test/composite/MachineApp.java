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

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
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
        Machine machine = new Machine(machineId, "phoenix");
        repo.save(machine);

        Machine retMachine = repo.findById(machineId).orElse(null);
        assertEquals(machine, retMachine);

        //update the row
        machine.setLocation("london");
        repo.save(machine);
        retMachine = repo.findById(machineId).orElse(null);
        assertEquals(machine, retMachine);

        //delete row
        repo.deleteById(machineId);
        assertNull(repo.findById(machineId).orElse(null));
    }
}
