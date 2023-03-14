/*-
 * Copyright (c) 2020, 2023 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */

package com.oracle.nosql.spring.data.test.composite;

import com.oracle.nosql.spring.data.repository.NosqlRepository;
import com.oracle.nosql.spring.data.repository.Query;

import java.util.List;

public interface MachineRepository extends NosqlRepository<Machine, MachineId> {
    List<Machine> findByLocation(String location);
    List<Machine> findByMachineIdVersion(String version);
    List<Machine> findByMachineIdName(String name);
    List<Machine> findByMachineIdNameAndMachineIdVersion(String name,
                                                         String version);
    @Query("SELECT * FROM Machine m WHERE m.VERSION='1'")
    List<Machine> findByVersionNative();
}
