/*-
 * Copyright (c) 2020, 2025 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */

package com.oracle.nosql.spring.data.test.composite;

import com.oracle.nosql.spring.data.core.mapping.NosqlId;

import java.util.Objects;

public class MachineProjectionDTO {
    @NosqlId
    private MachineId machineId;
    private String location;

    public MachineProjectionDTO(MachineId machineId, String location) {
        this.machineId = machineId;
        this.location = location;
    }

    public MachineProjectionDTO() {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MachineProjectionDTO that = (MachineProjectionDTO) o;
        return Objects.equals(machineId, that.machineId) && Objects.equals(location, that.location);
    }

    @Override
    public int hashCode() {
        return Objects.hash(machineId, location);
    }

    @Override
    public String toString() {
        return "MachineProjectionDTO{" +
                "machineId=" + machineId +
                ", location='" + location + '\'' +
                '}';
    }
}