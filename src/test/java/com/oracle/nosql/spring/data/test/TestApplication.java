/*-
 * Copyright (c) 2020, 2022 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */
package com.oracle.nosql.spring.data.test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.oracle.nosql.spring.data.core.NosqlOperations;
import com.oracle.nosql.spring.data.core.NosqlTemplate;
import com.oracle.nosql.spring.data.test.app.Address;
import com.oracle.nosql.spring.data.test.app.AppConfig;
import com.oracle.nosql.spring.data.test.app.Customer;
import com.oracle.nosql.spring.data.test.app.CustomerRepository;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = AppConfig.class)
public class TestApplication {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private CustomerRepository repo;

    @Test
    public <T> void testAtomicTypes() {
        repo.setTimeout(20000);
        repo.deleteAll();


        Customer c1 = new Customer("Nosql Alice", "Smith", null);
        c1.kids = 1;
        c1.length = 10;
        c1.weight = 100.1f;
        c1.coins = 101.1d;
        c1.biField = BigInteger.ONE;
        c1.bdField = BigDecimal.ONE;
        c1.vanilla = true;
        c1.code = new byte[] {1, 1, 0, 0, 1};
        c1.priority = Customer.Priority.HIGH;

        repo.save(c1);


        Customer c2 = new Customer("Nosql Bob", "Smith", null);
        c2.kids = 2;
        c2.length = 20;
        c2.weight = 200.2f;
        c2.coins = 202.2d;
        c2.biField = BigInteger.valueOf(2);
        c2.bdField = BigDecimal.TEN;
        c2.vanilla = false;
        c2.code = new byte[] {2, 2, 0, 0, 2};
        c2.priority = Customer.Priority.LOW;

        repo.save(c2);

        // fetch all customers
        for (Customer customer : repo.findAll()) {
            if (customer.customerId == c1.customerId) {
                Assert.assertEquals(c1, customer);
            } else if (customer.customerId == c2.customerId){
                Assert.assertEquals(c2, customer);
            }
        }
    }

    @Test
    public void testPojo() {
        repo.setTimeout(20000);
        repo.deleteAll();

        Customer c1 = new Customer("Nosql Alice", "Smith", null);
        c1.address = new Address("Paris", "Champs-Élysées");
        repo.save(c1);

        Customer c2 = new Customer("Nosql Bob", "Smith", null);
        c2.address = new Address("ViaSofia", "Sofia");
        repo.save(c2);

        // fetch all customers
        for (Customer customer : repo.findAll()) {
            if (customer.customerId == c1.customerId) {
                Assert.assertEquals(c1, customer);
            } else if (customer.customerId == c2.customerId){
                Assert.assertEquals(c2, customer);
            }
        }
    }

    @Test
    public void testPojoPolymorphism() {
        repo.setTimeout(20000);
        repo.deleteAll();

        Customer c1 = new Customer("Nosql Alice", "Smith", null);
        c1.address = new Address.USAddress("Wall St", "New York", "NY",
            20001);
        repo.save(c1);

        Customer c2 = new Customer("Nosql Bob", "Smith", null);
        c2.address = new Address.UKAddress("Big Ban", "London", "3000");
        repo.save(c2);

        // fetch all customers
        for (Customer customer : repo.findAll()) {
            if (customer.customerId == c1.customerId) {
                Assert.assertEquals(c1, customer);
            } else if (customer.customerId == c2.customerId) {
                Assert.assertEquals(c2, customer);
            }
        }
    }

    @Test
    public void testListArrayInheritance() {
        repo.setTimeout(20000);
        repo.deleteAll();

        List<Address> addresses = new ArrayList<>();
        addresses.add(new Address( "Via Cuomo", "Rome"));
        addresses.add(new Address.USAddress("Broadway", "New York", "NY",
            20000));
        addresses.add(new Address.UKAddress("Trafalgar Sq", "London", "10000"));
        addresses.add(null);

        Customer c1 = new Customer("Nosql Alice", "Smith", addresses);
        c1.addArray = new Address[] {new Address("Gran Vía", "Madrid"), null};
        c1.list = new ArrayList<>();
        c1.list.add(null);
        c1.list.add(1);
        c1.list.add(true);
        c1.list.add(new byte[] {1, 2, 3});
        List<Object> ll = new ArrayList<>();
        c1.list.add(ll);
        ll.add(null);
        ll.add("Bla bla bla!");
        ll.add(new Address("Santa", "Florence"));
        ll.add(new Address.USAddress("Memorial", "Houston", "TX", 77024));
        Object[] larr = new Object[] {null, 1, "boo", new Address.UKAddress(
            "MC", "Mancester", "20101")};
        ll.add(larr);
        repo.save(c1);


        addresses = new ArrayList<>();
        addresses.add(new Address("Alexanderplatz", "Berlin"));
        addresses.add(null);
        Customer c2 = new Customer("Nosql Bob", "Smith", addresses);
        c2.addArray = new Address[] {new Address("Magheru", "Bucharest"), null};
        repo.save(c2);

        // fetch all customers
        for (Customer customer : repo.findAll()) {
            if (customer.customerId == c1.customerId) {
                Assert.assertEquals(c1, customer);
            } else if (customer.customerId == c2.customerId){
                Assert.assertEquals(c2, customer);
            }
        }
    }

    @Test
    public void testRepoInterface() {
        repo.setTimeout(20000);
        repo.deleteAll();

        List<Address> addresses = new ArrayList<>();
        addresses.add(new Address("Via Cuomo", "Rome"));
        addresses.add(new Address.USAddress("Broadway", "New York", "NY",
            20000));
        addresses.add(new Address.UKAddress("Trafalgar Sq", "London", "10000"));
        addresses.add(null);
        Customer c1 = new Customer("Nosql Alice", "Smith", addresses);
        c1.kids = 1; c1.length = 10; c1.weight = 100.1f; c1.coins = 101.1d;
        c1.biField = BigInteger.ONE; c1.bdField = BigDecimal.ONE;
        c1.vanilla = true;
        c1.code = new byte[] {1, 1, 0, 0, 1};
        c1.address = new Address("Paris", "Champs-Élysées");
        c1.addArray = new Address[] {new Address("Gran Vía", "Madrid"), null};
        c1.list = new ArrayList<>();
        c1.list.add(null);
        c1.list.add(1);
        c1.list.add(true);
        c1.list.add(new byte[] {1, 2, 3});
        List<Object> ll = new ArrayList<>();
        c1.list.add(ll);
        ll.add(null);
        ll.add("Bla bla bla!");
        ll.add(new Address("Santa", "Florence"));
        ll.add(new Address.USAddress("Memorial", "Houston", "TX", 77024));
        Object[] larr = new Object[] {null, 1, "boo", new Address.UKAddress(
            "MC", "Mancester", "20101")};
        ll.add(larr);
        repo.save(c1);


        addresses = new ArrayList<>();
        addresses.add(new Address("Alexanderplatz", "Berlin"));
        addresses.add(null);
        Customer c2 = new Customer("Nosql Bob", "Smith", addresses);
        c2.kids = 2; c2.length = 20; c2.weight = 200.2f; c2.coins = 202.2d;
        c2.biField = BigInteger.valueOf(2); c2.bdField = BigDecimal.valueOf(22);
        c2.vanilla = false;
        c2.code = new byte[] {2, 2, 0, 0, 2};
        c2.address = new Address("ViaSofia", "Sofia");
        c2.addArray = new Address[] {new Address("Magheru", "Bucharest"), null};
        repo.save(c2);

        // fetch all customers
        for (Customer customer : repo.findAll()) {
            if (customer.customerId == c1.customerId) {
                Assert.assertEquals(c1, customer);
            } else if (customer.customerId == c2.customerId){
                Assert.assertEquals(c2, customer);
            }
        }

        Assert.assertTrue(repo.findById(c1.customerId).isPresent() &&
            c1.customerId == repo.findById(c1.customerId).get().customerId);
        Assert.assertTrue(repo.findById(c2.customerId).isPresent() &&
            c2.customerId == repo.findById(c2.customerId).get().customerId);

        repo.deleteById(c1.customerId);

        Assert.assertFalse(repo.existsById(c1.customerId));
        Assert.assertTrue(repo.existsById(c2.customerId));

        repo.delete(c2);

        Assert.assertFalse(repo.existsById(c1.customerId));
        Assert.assertFalse(repo.existsById(c2.customerId));

        List<Customer> clist = new ArrayList<>();
        int max = 10;
        Random random = new Random();
        for (int i = 0; i < max; i++) {
            clist.add(new Customer("F" + random.nextInt(max),
                "L" + i, null));
        }
        repo.saveAll(clist);

        long count = StreamSupport.stream(repo.findAll().spliterator(),
            false).count();
        Assert.assertEquals(max, count);


        // Pageable
        Pageable firstPageWithTwoElements = PageRequest.of(0, 2, Sort.by(
            "customerId"));
        Page<Customer> p1 = repo.findAll(firstPageWithTwoElements);

        Assert.assertEquals(max / 2, p1.getTotalPages());
        Assert.assertEquals(max, p1.getTotalElements());

        Pageable secondPageWithFiveElements = PageRequest.of(1, 5,
            Sort.Direction.DESC, "customerId");
        Page<Customer> p2 = repo.findAll(secondPageWithFiveElements);

        Assert.assertEquals(max / 5, p2.getTotalPages());
        Assert.assertEquals(max, p2.getTotalElements());


        // Sort
        Iterable<Customer> sorted = repo.findAll(Sort.by("customerId"));
        int i = 0;
        for ( Customer ignored : sorted) {
            i++;
        }
        Assert.assertEquals(max, i);


        List<Customer> toFind = clist.stream()
            .filter(x -> x.customerId % 2 == 0)
            .collect(Collectors.toList());

        List<Long> toFindCustIds = toFind.stream()
            .map(x -> x.customerId)
            .collect(Collectors.toList());

        count = StreamSupport.stream(
            repo.findAllById(toFindCustIds).spliterator(),
            false)
            .map( cust -> {
                Assert.assertTrue(toFindCustIds.contains(cust.customerId));
                return cust;
            })
            .count();

        Assert.assertEquals(toFindCustIds.size(), count);

        // check null throws IllegalArgumentException
        Assert.assertThrows(IllegalArgumentException.class,
            () -> repo.findById(null));

        Assert.assertThrows(IllegalArgumentException.class,
            () -> repo.findAllById(null));

        List<Long> toDelIds = Arrays.asList(c1.customerId, null, c2.customerId);
        Assert.assertThrows(IllegalArgumentException.class,
            () -> {
                // must read to get the exception
                for (Customer ignored : repo.findAllById(toDelIds)) {
                }
            });

        // deleteAll
        repo.deleteAll(toFind);

        repo.setTimeout(30000);
        count = repo.count();
        Assert.assertEquals(max - toFind.size(), count);

        repo.deleteAll();

        // count()
        count = repo.count();
        Assert.assertEquals(0, count);
    }

    @Test
    public void testNosqlOperations()
        throws ClassNotFoundException {

        NosqlOperations nosqlOps =
            NosqlTemplate.create(AppConfig.nosqlDBConfig);

        Customer c = new Customer("Alice", "Smith", null);
        nosqlOps.insert(c);
        long idAlice = c.customerId;

        c = new Customer("Bob", "Smith", null);
        nosqlOps.insert(c);
        long idBob = c.customerId;
        c = nosqlOps.findById(idAlice, Customer.class);
        Assert.assertEquals("Alice", c.firstName);

        c = nosqlOps.findById(idBob, Customer.class);
        Assert.assertEquals("Bob", c.firstName);
    }

    /** Tests save(entity) with and without id on an entity with generated id */
    @Test
    public void testUpdate() {
        Customer c1 = new Customer("Nosql Alice", "Smith", null);

        Assert.assertEquals(0, c1.customerId);
        repo.save(c1);
        long id = c1.customerId;

        Assert.assertNotEquals(0, c1.customerId);

        c1.firstName = "Updated Alice";
        c1.lastName = "Cooper";

        // tests save of
        repo.save(c1);

        Assert.assertEquals(id, c1.customerId);
        Assert.assertEquals("Updated Alice", c1.firstName);
        Assert.assertEquals("Cooper", c1.lastName);

        Customer alice = repo.findById(id).get();
        Assert.assertEquals("Updated Alice", alice.firstName);
        Assert.assertEquals("Cooper", alice.lastName);
    }

    @Test
    public void testDeleteAll() {
        repo.deleteAll();

        Assert.assertEquals(0, repo.count());

        Customer c1 = new Customer("Nosql Alice", "Smith", null);
        repo.save(c1);


        Customer c2 = new Customer("Nosql Bob", "Smith", null);
        repo.save(c2);

        Assert.assertEquals(2, repo.count());

        List<Customer> customers = new ArrayList<>();
        customers.add(c1);
        customers.add(c1);

        repo.deleteAll(customers);
        Assert.assertEquals(1, repo.count());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDeleteAllNull() {
        List<Customer> customers = new ArrayList<>();
        customers.add(null);
        repo.deleteAll(customers);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFindAllNullItem() {
        List<Long> ids = new ArrayList<>();
        ids.add(null);
        repo.findAllById(ids).iterator().next();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFindAllNull() {
        repo.findAllById(null);
    }
}