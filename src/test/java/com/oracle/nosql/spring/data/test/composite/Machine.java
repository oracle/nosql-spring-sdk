/*-
 * Copyright (c) 2020, 2023 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */

package com.oracle.nosql.spring.data.test.composite;

import com.oracle.nosql.spring.data.core.mapping.NoSqlKeyClass;
import com.oracle.nosql.spring.data.core.mapping.NosqlId;
import com.oracle.nosql.spring.data.core.mapping.NosqlTable;

import java.util.Date;
import java.util.Objects;

@NosqlTable(autoCreateTable = true, readUnits = 100, writeUnits = 100,
        storageGB = 1)
public class Machine {
    @NosqlId
    MachineId machineId;
    private String location;
    private Date creationDate = new Date();

    public Machine(MachineId machineId, String location) {
        this.machineId = machineId;
        this.location = location;
    }

    public MachineId getMachineId() {
        return machineId;
    }

    public void setMachineId(MachineId machineId) {
        this.machineId = machineId;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Machine)) {
            return false;
        }
        Machine machine = (Machine) o;
        return Objects.equals(machineId, machine.machineId) &&
                Objects.equals(location, machine.location) &&
                Objects.equals(creationDate, machine.creationDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(machineId, location, creationDate);
    }
}
