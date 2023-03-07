/*-
 * Copyright (c) 2020, 2023 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */

package com.oracle.nosql.spring.data.test.composite;

import com.oracle.nosql.spring.data.repository.NosqlRepository;

import java.util.List;

public interface MachineRepository extends NosqlRepository<Machine, MachineId> {
    List<Machine> findByLocation(String location);
}