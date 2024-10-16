/*-
 * Copyright (c) 2020, 2024 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */

package com.oracle.nosql.spring.data.test.composite;

import com.oracle.nosql.spring.data.core.mapping.NosqlId;
import com.oracle.nosql.spring.data.core.mapping.NosqlTable;
import org.springframework.data.annotation.Transient;

import java.util.Date;
import java.util.List;
import java.util.Objects;

@NosqlTable(autoCreateTable = true, readUnits = 100, writeUnits = 100,
        storageGB = 1)
public class Machine {
    @NosqlId
    MachineId machineId;
    private String location;
    private Date creationDate = new Date();
    private IpAddress hostAddress;
    private List<IpAddress> routeAddress;
    private int version = -1; //version as both top level property and
    // property in MachineId class
    @Transient
    private final String transientString = "temp";

    public Machine(MachineId machineId, String location,
                   IpAddress hostAddress, List<IpAddress> routeAddress) {
        this.machineId = machineId;
        this.location = location;
        this.hostAddress = hostAddress;
        this.routeAddress = routeAddress;
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

    public IpAddress getHostAddress() {
        return hostAddress;
    }

    public List<IpAddress> getRouteAddress() {
        return routeAddress;
    }

    public void setHostAddress(IpAddress hostAddress) {
        this.hostAddress = hostAddress;
    }

    public void setRouteAddress(List<IpAddress> routeAddress) {
        this.routeAddress = routeAddress;
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
                Objects.equals(creationDate, machine.creationDate) &&
                Objects.equals(hostAddress, machine.hostAddress) &&
                Objects.equals(routeAddress, machine.routeAddress) &&
                Objects.equals(version, machine.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(machineId, location, creationDate, hostAddress, routeAddress, version);
    }
}
