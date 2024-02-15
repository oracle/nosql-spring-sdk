/*-
 * Copyright (c) 2020, 2024 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */

package com.oracle.nosql.spring.data.test.composite;

import com.oracle.nosql.spring.data.core.mapping.NosqlId;

import java.util.Objects;

public class MachineProjectionDTOOnlyId {
    @NosqlId
    private MachineId machineId;

    public MachineProjectionDTOOnlyId() {
    }

    public MachineProjectionDTOOnlyId(MachineId machineId) {
        this.machineId = machineId;
    }

    public MachineId getMachineId() {
        return machineId;
    }

    public void setMachineId(MachineId machineId) {
        this.machineId = machineId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MachineProjectionDTOOnlyId that = (MachineProjectionDTOOnlyId) o;
        return Objects.equals(machineId, that.machineId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(machineId);
    }
}
