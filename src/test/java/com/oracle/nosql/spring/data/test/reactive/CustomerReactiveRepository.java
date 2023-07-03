/*-
 * Copyright (c) 2020, 2023 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */
package com.oracle.nosql.spring.data.test.reactive;

import com.oracle.nosql.spring.data.repository.Query;
import com.oracle.nosql.spring.data.repository.ReactiveNosqlRepository;
import com.oracle.nosql.spring.data.test.app.Customer;

import org.springframework.data.repository.query.Param;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CustomerReactiveRepository
    extends ReactiveNosqlRepository<Customer, Long> {

    Flux<Customer> findByFirstName(String first);

    Flux<Customer> findByLastNameOrderByFirstName(String last);

    Mono<Boolean> existsByLastName(String last);

    Mono<Long> countByLastName(String last);

    Flux<Customer> deleteByLastName(String last);

    @Query("SELECT * FROM Customer AS c WHERE c.kv_json_.firstName = 'John'")
    Flux<Customer> findCustomersByFirstNameJohn();

    @Query(value = "DECLARE $firstName STRING; SELECT * FROM Customer AS c " +
            "WHERE c.kv_json_.firstName = $firstName")
    Flux<Customer> findCustomersByFirstName(@Param("$firstName") String firstName);

    @Query("DECLARE $firstName STRING; $last STRING; " +
            "SELECT * FROM Customer AS c " +
            "WHERE c.kv_json_.firstName = $firstName AND " +
            "c.kv_json_.lastName = $last")
    Flux<Customer> findCustomersWithLastAndFirstNames(
            @Param("$last") String paramLast,
            @Param("$firstName") String firstName
    );
}
