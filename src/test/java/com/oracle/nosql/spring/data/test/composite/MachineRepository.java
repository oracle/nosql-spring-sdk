/*-
 * Copyright (c) 2020, 2023 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */

package com.oracle.nosql.spring.data.test.composite;

import com.oracle.nosql.spring.data.repository.NosqlRepository;
import com.oracle.nosql.spring.data.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface MachineRepository extends NosqlRepository<Machine, MachineId> {
    List<Machine> findByLocation(String location);
    List<Machine> findByMachineIdVersionOrderByMachineIdNameAsc(String version);
    List<Machine> findByMachineIdName(String name);
    Page<Machine> findByMachineIdName(String name, Pageable pageable);
    List<Machine> findByMachineIdNameAndMachineIdVersion(String name,
                                                         String version);
    @Query("SELECT * FROM Machine m WHERE m.VERSION='version1'")
    List<Machine> findByVersionNative();

    List<Machine> findByMachineIdNameIgnoreCase(String name);
}
