/*-
 * Copyright (c) 2020, 2023 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */
package com.oracle.nosql.spring.data.test.reactive;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.oracle.nosql.spring.data.test.app.Customer;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = ReactiveAppConfig.class)
public class TestReactiveApp {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private CustomerReactiveRepository repo;

    @Test
    public void testRepo() {
        // get/setTimeout()
        repo.setTimeout(20000);
        Assert.assertEquals(20000, repo.getTimeout());

        // deleteAll
        repo.deleteAll();
        Flux<Customer> flux = repo.findAll();
        StepVerifier.create(flux).verifyComplete();

        // save
        Customer c1 = new Customer("Nosql Alice", "Smith", null);
        c1.kids = 1;
        c1.length = 10;
        c1.weight = 100.1f;
        c1.coins = 101.1d;
        c1.biField = BigInteger.ONE;
        c1.bdField = BigDecimal.ONE;
        c1.vanilla = true;
        c1.code = new byte[] {1, 1, 0, 0, 1};

        Mono<Customer> mono = repo.save(c1);
        StepVerifier.create(mono).expectNext(c1).verifyComplete();

        Customer c2 = new Customer("Nosql Bob", "Smith", null);
        c2.kids = 2;
        c2.length = 20;
        c2.weight = 200.2f;
        c2.coins = 202.2d;
        c2.biField = BigInteger.valueOf(2);
        c2.bdField = BigDecimal.TEN;
        c2.vanilla = false;
        c2.code = new byte[] {2, 2, 0, 0, 2};

        mono = repo.save(c2);
        StepVerifier.create(mono).expectNext(c2).verifyComplete();

        // findAll
        flux = repo.findAll();

        List<Customer> list = flux.collectList().block();
        Assert.assertEquals(2, list.size());
        Assert.assertTrue(list.containsAll(Arrays.asList(c1, c2)));

        Customer c3 = new Customer("John", "Smith", null);
        c3.kids = 3;
        Customer c4 = new Customer("John", "Doe", null);
        c4.kids = 4;

        List<Customer> customerList = new ArrayList<>();
        customerList.add(c3);
        customerList.add(c4);

        // saveAll
        flux = repo.saveAll(customerList);
        StepVerifier.create(flux).expectNext(c3, c4).verifyComplete();


        List<Customer> expected = Arrays.asList(c1, c2, c3, c4)
            .stream()
            .sorted(Comparator.comparingLong(cst -> cst.customerId))
            .collect(Collectors.toList());

        // findAll(Sort)
        flux = repo.findAll(Sort.by("customerId").ascending());
        StepVerifier.create(flux).expectNextSequence(expected).verifyComplete();

        expected = Arrays.asList(c1, c2, c3, c4)
            .stream()
            .sorted((cst1, cst2) ->
                Long.compare(cst2.customerId, cst1.customerId))
            .collect(Collectors.toList());

        flux = repo.findAll(Sort.by("customerId").descending());
        StepVerifier.create(flux).expectNextSequence(expected).verifyComplete();

        flux = repo.findAll(Sort.by("kids").ascending());
        StepVerifier.create(flux).expectNext(c1, c2, c3, c4).verifyComplete();

        flux = repo.findAll(Sort.by("kids").descending());
        StepVerifier.create(flux).expectNext(c4, c3, c2, c1).verifyComplete();


        Customer c5 = new Customer("Clark", "Kent", null);
        c5.kids = 5;

        Customer c6 = new Customer("Berry", "Allan", null);
        c6.kids = 6;

        Customer c7 = new Customer("Diana", "Prince", null);
        c7.kids = 7;

        // saveAll(Publisher)
        flux = repo.saveAll(Flux.just(c5, c6, c7));
        StepVerifier.create(flux).expectNext(c5, c6, c7).verifyComplete();

        // findById
        mono = repo.findById(c1.customerId);
        StepVerifier.create(mono).expectNext(c1).verifyComplete();

        mono = repo.findById(c1.customerId - 100);
        StepVerifier.create(mono).expectComplete().verify();

        mono = repo.findById(Flux.just(c2.customerId, c3.customerId));
        StepVerifier.create(mono).expectNext(c2).verifyComplete();

        // existsById
        Mono<Boolean> exists = repo.existsById(c1.customerId);
        StepVerifier.create(exists).expectNext(Boolean.TRUE).verifyComplete();

        exists = repo.existsById(c1.customerId - 100);
        StepVerifier.create(exists).expectNext(Boolean.FALSE).verifyComplete();

        exists = repo.existsById(Flux.just(c2.customerId, c3.customerId));
        StepVerifier.create(exists).expectNext(Boolean.TRUE).verifyComplete();

        exists = repo.existsById(Mono.just(c2.customerId - 90));
        StepVerifier.create(exists).expectNext(Boolean.FALSE).verifyComplete();

        // findAllById
        flux = repo.findAllById(
            Flux.just(c1.customerId, c2.customerId, c3.customerId).toIterable());
        StepVerifier.create(flux).expectNext(c1, c2, c3).verifyComplete();

        flux = repo.findAllById(
            Flux.just(c4.customerId, c5.customerId, c6.customerId));
        StepVerifier.create(flux).expectNext(c4, c5, c6).verifyComplete();

        // count
        Mono<Long> count = repo.count();
        StepVerifier.create(count).expectNext(7L).verifyComplete();

        //native queries
        List<Customer> johns =
                repo.findCustomersByFirstNameJohn().collectList().block();
        Assert.assertTrue(johns.contains(c3) && johns.contains(c4));

        johns = repo.findCustomersByFirstName("John").collectList().block();
        Assert.assertTrue(johns.size() == 2 &&
                johns.contains(c3) && johns.contains(c4));

        johns = repo.findCustomersWithLastAndFirstNames("Doe", "John").
                        collectList().block();
        Assert.assertTrue(johns.size() == 1 && johns.contains(c4));

        // deleteById
        repo.deleteById(c7.customerId).subscribe();
        exists = repo.existsById(c7.customerId);
        StepVerifier.create(exists).expectNext(Boolean.FALSE).verifyComplete();

        repo.deleteById(c7.customerId).subscribe();
        exists = repo.existsById(c7.customerId);
        StepVerifier.create(exists).expectNext(Boolean.FALSE).verifyComplete();


        repo.deleteById(Flux.just(c6.customerId, c5.customerId)).subscribe();

        exists = repo.existsById(c6.customerId);
        StepVerifier.create(exists).expectNext(Boolean.FALSE).verifyComplete();
        exists = repo.existsById(c5.customerId);
        StepVerifier.create(exists).expectNext(Boolean.TRUE).verifyComplete();

        count = repo.count();
        StepVerifier.create(count).expectNext(5L).verifyComplete();

        // delete
        repo.delete(c5).subscribe();
        exists = repo.existsById(c5.customerId);
        StepVerifier.create(exists).expectNext(Boolean.FALSE).verifyComplete();

        count = repo.count();
        StepVerifier.create(count).expectNext(4L).verifyComplete();

        // deleteAll
        repo.deleteAll(Arrays.asList(c3, c4)).subscribe();

        exists = repo.existsById(c3.customerId);
        StepVerifier.create(exists).expectNext(Boolean.FALSE).verifyComplete();
        exists = repo.existsById(c4.customerId);
        StepVerifier.create(exists).expectNext(Boolean.FALSE).verifyComplete();

        count = repo.count();
        StepVerifier.create(count).expectNext(2L).verifyComplete();


        repo.deleteAll(Flux.just(c1, c2)).subscribe();

        exists = repo.existsById(c1.customerId);
        StepVerifier.create(exists).expectNext(Boolean.FALSE).verifyComplete();
        exists = repo.existsById(c2.customerId);
        StepVerifier.create(exists).expectNext(Boolean.FALSE).verifyComplete();

        count = repo.count();
        StepVerifier.create(count).expectNext(0L).verifyComplete();
    }

    @Test
    public void testDynamic() {
        repo.deleteAll().subscribe();

        Customer c1 = new Customer("Nosql Alice", "Smith", null);
        Customer c2 = new Customer("Nosql Bob", "Smith", null);
        Customer c3 = new Customer("John", "Smith", null);
        Customer c4 = new Customer("John", "Doe", null);
        Customer c5 = new Customer("Clark", "Kent", null);
        Customer c6 = new Customer("Berry", "Allan", null);
        Customer c7 = new Customer("Diana", "Prince", null);
        repo.saveAll(Flux.just(c1, c2, c3, c4, c5, c6, c7)).subscribe();

        // bindBy
        Flux<Customer> flux = repo.findByFirstName("John");
        StepVerifier.create(flux.sort(Comparator.comparing(o -> o.lastName)))
            .expectNext(c4)
            .expectNext(c3)
            .verifyComplete();

        flux = repo.findByLastNameOrderByFirstName("Smith");
        StepVerifier.create(flux)
            .expectNext(c3)
            .expectNext(c1)
            .expectNext(c2)
            .verifyComplete();

        // existsBy
        Mono<Boolean> exists = repo.existsByLastName("Doe");
        StepVerifier.create(exists)
            .expectNext(Boolean.TRUE)
            .verifyComplete();

        exists = repo.existsByLastName("Foe");
        StepVerifier.create(exists)
            .expectNext(Boolean.FALSE)
            .verifyComplete();

        // countBy
        Mono<Long> count = repo.countByLastName("Smith");
        StepVerifier.create(count)
            .expectNext(3L)
            .verifyComplete();

        count = repo.countByLastName("Kent");
        StepVerifier.create(count)
            .expectNext(1L)
            .verifyComplete();

        count = repo.countByLastName("Anderson");
        StepVerifier.create(count)
            .expectNext(0L)
            .verifyComplete();

        // deleteBy
        flux = repo.deleteByLastName("Allan");
        StepVerifier.create(flux)
            .expectNext(c6)
            .verifyComplete();

        count = repo.countByLastName("Allan");
        StepVerifier.create(count)
            .expectNext(0L)
            .verifyComplete();

        flux = repo.deleteByLastName("Smith");
        StepVerifier.create(flux.sort(Comparator.comparing(cu -> cu.firstName)))
            .expectNext(c3)
            .expectNext(c1)
            .expectNext(c2)
            .verifyComplete();

        count = repo.count();
        StepVerifier.create(count)
            .expectNext(3L)
            .verifyComplete();

        repo.deleteAll().subscribe();
    }
}