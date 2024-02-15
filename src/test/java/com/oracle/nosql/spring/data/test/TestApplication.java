/*-
 * Copyright (c) 2020, 2024 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */
package com.oracle.nosql.spring.data.test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.oracle.nosql.spring.data.NosqlDbFactory;
import com.oracle.nosql.spring.data.core.NosqlOperations;
import com.oracle.nosql.spring.data.core.NosqlTemplate;
import com.oracle.nosql.spring.data.core.mapping.NosqlCapacityMode;
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

    @Test
    public void testDefaultValues() {
        NosqlDbFactory factory = NosqlDbFactory.createCloudSimFactory("foo");
        Assert.assertEquals(NosqlCapacityMode.PROVISIONED,
            factory.getDefaultCapacityMode());
        Assert.assertEquals(25, factory.getDefaultStorageGB());
        Assert.assertEquals(50, factory.getDefaultReadUnits());
        Assert.assertEquals(50, factory.getDefaultWriteUnits());


        // Test AppConfig file settings
        Assert.assertEquals(NosqlCapacityMode.PROVISIONED,
            AppConfig.nosqlDBConfig.getDefaultCapacityMode());
        Assert.assertEquals(2,
            AppConfig.nosqlDBConfig.getDefaultStorageGB());
        Assert.assertEquals(3,
            AppConfig.nosqlDBConfig.getDefaultReadUnits());
        Assert.assertEquals(4,
            AppConfig.nosqlDBConfig.getDefaultWriteUnits());
    }

    @Test
    public void testConsistencyDurability() {
        Assert.assertEquals("EVENTUAL", repo.getConsistency());
        repo.setConsistency("ABSOLUTE");
        Assert.assertEquals("ABSOLUTE", repo.getConsistency());

        Assert.assertEquals("COMMIT_NO_SYNC", repo.getDurability());
        repo.setDurability("COMMIT_WRITE_NO_SYNC");
        Assert.assertEquals("COMMIT_WRITE_NO_SYNC",
            repo.getDurability());

        //null durability
        repo.setDurability(null);
        Assert.assertEquals("COMMIT_NO_SYNC", repo.getDurability());

        //invalid durability
        repo.setDurability("INVALID");
        Assert.assertEquals("COMMIT_NO_SYNC", repo.getDurability());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testMaps() {
        Customer c1 = new Customer();
        c1.kids = 2;
        c1.firstName = "maps are null";
        repo.save(c1);
        long c1Id = c1.customerId;

        Customer c2 = new Customer();
        c2.firstName = "maps with entries";
        c2.mapField = new HashMap<>();
        c2.mapField.put("f1", null);
        c2.mapField.put("f2", "f2 value");

        c2.enumMap = new HashMap<>();
        c2.enumMap.put(Customer.Priority.LOW, null);
        c2.enumMap.put(Customer.Priority.MEDIUM, "medium priority");
        //c2.enumMap.put(Customer.Priority.HIGH, "high priority");
        //c2.enumMap.put(Customer.Priority.LOW, new HashMap<>());
        //c2.enumMap.put(Customer.Priority.MEDIUM, new Object[]{});
        c2.enumMap.put(Customer.Priority.HIGH, new ArrayList<>());

        c2.classicMap = new HashMap<>();
        c2.classicMap.put("entry1", "entry value 1");
        c2.classicMap.put(Customer.Priority.HIGH, "value for HIGH");

        Address pojo = new Address.USAddress("Oracle Way", "Austin", "TX", 78777);
        c2.classicMap.put("pojo", pojo);

        List<Object> list = new ArrayList<>();
        list.add(null);
        list.add(1);
        list.add(2L);
        list.add(3.0);
        list.add(4.0d);
        list.add(false);
        list.add("hello");
        Address pojo2 = new Address.UKAddress("Oracle Circle", "London", "UK77");
        list.add(pojo2);
        c2.classicMap.put("list", list);

        c2.arraysMap = new HashMap<>();
        c2.arraysMap.put("null", null);
        c2.arraysMap.put("empty", new Object[0]);
        c2.arraysMap.put("123", new Object[]{null, 1, 2L, 3.0, 4.0d, true, "six"});

        c2.navigableMap = new TreeMap<>();
        c2.navigableMap.put("navigable", "map");
        c2.sortedMap = new TreeMap<>();
        c2.sortedMap.put("sorted", "map");

        c2.hashMap = new HashMap<>();
        c2.hashMap.put("null", null);
        c2.hashMap.put("hash", "map");
        c2.linkedHashMap = new LinkedHashMap<>();
        c2.linkedHashMap.put("null", null);
        c2.linkedHashMap.put("linked", "hash");
        c2.treeMap = new TreeMap<>();
        c2.treeMap.put("tree", "map");
        c2.hashtable = new Hashtable<>();
        c2.hashtable.put("empty", new ArrayList<>());

        ArrayList<Object> al = new ArrayList<>();
        al.add(null);
        al.add("");
        al.add(1);
        al.add(2L);
        al.add(3.0f);
        al.add(true);
        ArrayList<Object> al2 = new ArrayList<>();
        al2.add(null);
        al2.add("");
        al2.add(1);
        al2.add(2L);
        al2.add(3.0f);
        al2.add(true);
        Map<String, Object> innerMap = new HashMap<>();
        innerMap.put("null", null);
        innerMap.put("empty", "");
        innerMap.put("1", 1);
        innerMap.put("2L", 2L);
        innerMap.put("3.0f", 3.0f);
        innerMap.put("4.0d", 4.0d);
        innerMap.put("true", true);
        al2.add(innerMap);
        al.add(al2);
        c2.hashtable.put("nested lists", al);

        repo.save(c2);
        long c2Id = c2.customerId;

        Optional<Customer> optC1Db = repo.findById(c1Id);
        Assert.assertTrue(optC1Db.isPresent());
        Customer c1Db = optC1Db.get();
        Assert.assertEquals(c1.customerId, c1Db.customerId);
        Assert.assertEquals(c1.firstName, c1Db.firstName);
        Assert.assertEquals(c1.mapField, c1Db.mapField);
        Assert.assertEquals(c1.enumMap, c1Db.enumMap);
        Assert.assertEquals(c1.classicMap, c1Db.classicMap);
        Assert.assertEquals(c1.arraysMap, c1Db.arraysMap);
        Assert.assertEquals(c1.navigableMap, c1Db.navigableMap);
        Assert.assertEquals(c1.sortedMap, c1Db.sortedMap);
        Assert.assertEquals(c1.hashMap, c1Db.hashMap);
        Assert.assertEquals(c1.linkedHashMap, c1Db.linkedHashMap);
        Assert.assertEquals(c1.treeMap, c1Db.treeMap);
        Assert.assertEquals(c1.hashtable, c1Db.hashtable);

        Optional<Customer> optC2Db = repo.findById(c2Id);
        Assert.assertTrue(optC2Db.isPresent());
        Customer c2Db = optC2Db.get();
        Assert.assertEquals(c2.customerId, c2Db.customerId);
        Assert.assertEquals(c2.firstName, c2Db.firstName);
        Assert.assertEquals(c2.mapField, c2Db.mapField);
        Assert.assertTrue("enumMaps should be equal - exp: " + c2.enumMap +
            "   - act: " + c2Db.enumMap,
            Customer.mapEquals(c2.enumMap, c2Db.enumMap));
        Assert.assertTrue("classicMap should be equal equal exp: " +
            c2.classicMap + " act: " + c2Db.classicMap,
            Customer.mapEquals(c2.classicMap, c2Db.classicMap));
        Assert.assertArrayEquals("arraysMap[123] should be equal",
            c2.arraysMap.get("123"), c2Db.arraysMap.get("123"));
        Assert.assertTrue("arraysMap should be equal equal exp: " +
                c2.arraysMap + " act: " + c2Db.arraysMap,
            Customer.mapEquals(c2.arraysMap, c2Db.arraysMap));

        Assert.assertEquals("navigableMap NOT equal",
            c2.navigableMap, c2Db.navigableMap);
        Assert.assertEquals("sortedMap NOT equal",
            c2.sortedMap, c2Db.sortedMap);
        Assert.assertEquals("hashMap NOT equal",
            "" + c2.hashMap, "" + c2Db.hashMap);
        Assert.assertEquals("linkedHashMap NOT equal",
            "" + c2.linkedHashMap, "" + c2Db.linkedHashMap);
        Assert.assertEquals("treeMap NOT equal",
            "" + c2.treeMap, "" + c2Db.treeMap);
        Assert.assertEquals("hashtable NOT equal",
            "" + c2.hashtable, "" + c2Db.hashtable);

//        printWithTypes("enumMap", c2Db.enumMap);
//        printWithTypes("classicMap", c2Db.classicMap);
//        printWithTypes("array", c2Db.arraysMap);
//        printWithTypes("navigable", c2Db.navigableMap);
//        printWithTypes("sorted", c2Db.sortedMap);
//        printWithTypes("hash", c2Db.hashMap);
//        printWithTypes("linked", c2Db.linkedHashMap);
//        printWithTypes("tree", c2Db.treeMap);
//        printWithTypes("hashtable", c2Db.hashtable);

        Assert.assertTrue("\nExp: " + c2 +
            "\nAct: " + c2Db, c2.equals(c2Db));

        // Check errors when keys are null or other types
        c2.classicMap.put(null, "value for null key");
        checkError(c2, "Unsupported null map key: public java.util.Map " +
            "com.oracle.nosql.spring.data.test.app.Customer.classicMap");
        c2.classicMap.remove(null);

        c2.classicMap.put(123, "value for 123 key");
        checkError(c2, "Unsupported map key type: class java.lang.Integer");
        c2.classicMap.remove(123);

        Address pojoKey = new Address("Oracle Rue", "Paris");
        c2.mapField.put(pojoKey, "pojo key");
        checkError(c2, "Unsupported map key type: class " +
            "com.oracle.nosql.spring.data.test.app.Address");
        c2.mapField.remove(pojo);
    }

    private void checkError(Customer c2, String msg) {
        try {
            repo.save(c2);
            Assert.fail("repo.save didn't throw expected error: " + msg);
        } catch (IllegalArgumentException e) {
            Assert.assertEquals(msg, e.getMessage());
        }
    }

    private void printWithTypes(String name, Map<?, ?> map) {
        if (map == null) {
            System.out.println("map '" + name + "' is null");
            return;
        }

        System.out.println("map: " + name + ":  " + map.getClass());
        for (Map.Entry entry : map.entrySet()) {
            System.out.println("  - k: " +
                (entry.getKey() == null ? "null" : entry.getKey().getClass()) +
                " = " + entry.getKey());
            System.out.println("    v: " +
                (entry.getValue() == null ? "null" : entry.getValue()
                    .getClass()) + " = " + entry.getValue());
            if (entry.getValue() != null && entry.getValue().getClass()
                .isArray() ) {
                Object[] arr = (Object[]) entry.getValue();
                System.out.println("      " + Arrays.toString(arr));
            }
        }
    }
}
