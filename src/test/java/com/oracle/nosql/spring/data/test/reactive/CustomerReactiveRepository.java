/*-
 * Copyright (c) 2020, 2023 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */
package com.oracle.nosql.spring.data.test.reactive;

import com.oracle.nosql.spring.data.repository.ReactiveNosqlRepository;
import com.oracle.nosql.spring.data.test.app.Customer;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CustomerReactiveRepository
    extends ReactiveNosqlRepository<Customer, Long> {

    Flux<Customer> findByFirstName(String first);

    Flux<Customer> findByLastNameOrderByFirstName(String last);

    Mono<Boolean> existsByLastName(String last);

    Mono<Long> countByLastName(String last);

    Flux<Customer> deleteByLastName(String last);
}
