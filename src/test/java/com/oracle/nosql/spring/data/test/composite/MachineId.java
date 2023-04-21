/*-
 * Copyright (c) 2020, 2023 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */

package com.oracle.nosql.spring.data.test.composite;

import com.oracle.nosql.spring.data.core.mapping.NosqlKey;
import org.springframework.data.annotation.Transient;

import java.io.Serializable;
import java.util.Objects;

public class MachineId implements Serializable {
    private static final long serialVersionUID = 1L;
    @NosqlKey(shardKey = true, order = 0)
    private String version;
    @NosqlKey(shardKey = false, order = 1)
    private String name;
    @Transient
    private final String temp = "temp";

    public MachineId() {
    }

    public MachineId(String version, String name) {
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
        if (!(o instanceof MachineId)) {
            return false;
        }
        MachineId machineId = (MachineId) o;
        return Objects.equals(version, machineId.version) &&
                Objects.equals(name, machineId.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(version, name);
    }

    @Override
    public String toString() {
        return "MachineId{" +
                "version='" + version + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
