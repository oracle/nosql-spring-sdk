/*-
 * Copyright (c) 2020, 2025 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */

package com.oracle.nosql.spring.data.test.composite;

import com.oracle.nosql.spring.data.repository.Query;
import com.oracle.nosql.spring.data.repository.ReactiveNosqlRepository;
import org.springframework.data.repository.query.Param;
import reactor.core.publisher.Flux;

public interface ReactiveMachineRepository extends ReactiveNosqlRepository<Machine, MachineId> {
    Flux<Machine> findByMachineIdVersion(String version);

    Flux<Machine> findByMachineIdName(String name);

    Flux<Machine> findByLocation(String location);

    //and
    Flux<Machine> findByMachineIdNameAndMachineIdVersion(String name,
                                                         String version);

    Flux<Machine> findByMachineIdNameAndLocation(String name, String location);

    //or
    Flux<Machine> findByMachineIdNameOrMachineIdVersion(String name,
                                                        String version);

    //sorting and paging
    Flux<Machine> findByMachineIdVersionOrderByMachineIdNameAsc(String version);
    
    //Ignore case
    Flux<Machine> findByMachineIdNameIgnoreCase(String name);

    //native
    @Query("SELECT * FROM Machine m WHERE m" +
            ".kv_json_.location='newyork'")
    Flux<Machine> findAllByLocationNative();

    @Query(value = "DECLARE $name STRING; SELECT * FROM Machine AS m " +
            "WHERE m.name = $name")
    Flux<Machine> findByMachineIdNameNative(@Param("$name") String name);
}
