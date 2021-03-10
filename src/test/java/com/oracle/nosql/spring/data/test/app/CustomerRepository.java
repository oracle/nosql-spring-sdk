/*-
 * Copyright (c) 2020, 2021 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */
package com.oracle.nosql.spring.data.test.app;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;

import oracle.nosql.driver.values.StringValue;

import com.oracle.nosql.spring.data.repository.NosqlRepository;
import com.oracle.nosql.spring.data.repository.Query;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Polygon;
import org.springframework.data.repository.query.Param;

public interface CustomerRepository
    extends NosqlRepository<Customer, Long> {
    // Simple by field
    List<Customer> findByFirstName(String firstName);
    List<Customer> findByLastName(String lastName);
    // AND expresion
    List<Customer> findByFirstNameAndLastName(String firstName,
        String lastName);
    // OR expression
    List<Customer> findByFirstNameOrLastName(String firstName, String lastName);
    // AND + OR
    List<Customer> findByFirstNameOrLastNameAndKids(String firstName,
        String lastName, int kids);
    List<Customer> findByFirstNameAndLastNameOrKids(String firstName,
        String lastName, int kids);
    // NOT expression
    List<Customer> findByFirstNameNot(String firstName);


    // different value types
    // int with > and <= expressions
    List<Customer> findByKidsGreaterThanAndKidsLessThanEqual(int anInt1,
        int anInt2);
    // long with >= and < expressions
    List<Customer> findByLengthIsGreaterThanEqualAndLengthIsLessThan(long l1,
        long l2);
    // boolean
    List<Customer> findByVanilla(boolean b);
    // boolean is true
    List<Customer> findByVanillaIsTrue();
    // float
    List<Customer> findByWeight(float w);
    // double
    List<Customer> findByCoins(double c);
    // BigInteger
    List<Customer> findByBiField(BigInteger bi);
    // BigDecimal
    List<Customer> findByBdField(BigDecimal bd);


    // Enabling static ORDER BY for a query
    List<Customer> queryByLastNameOrderByFirstNameDesc(String lastname);
    List<Customer> getByLastNameOrderByFirstNameAsc(String lastname);

    // Enabling ignoring case for an individual property
    List<Customer> findByLastNameIgnoreCase(String lastname);
    // Enabling ignoring case for all suitable properties
    List<Customer> findByLastNameAndFirstNameAllIgnoreCase(String lastname,
        String firstname);
    // Enable ignore case only for firstName field
    List<Customer> findByLastNameAndFirstNameIgnoreCase(String lastname,
        String firstname);
    List<Customer> findByAddressCityIsInAllIgnoreCase(List<String> cities);
    List<Customer> findByLastNameBetweenIgnoreCase(String from, String to);


    // traversing nested properties
    List<Customer> findByAddressCity(String city);
    // not working since State is not in Address but only in USAddress
    //List<Customer> findByAddressState(String city);

    // isNull, isNotNull
    List<Customer> findByAddressIsNull();
    List<Customer> findByAddressNotNull();


    List<Customer> findByAddressCityIn(List<Object> cities);
    List<Customer> findByAddressCityIsIn(List<String> cities);
    List<Customer> findByAddressCityNotIn(List<String> cities);

    // This fails as expected because IN operator expects Collection type
    List<Customer> findByAddressCityIn(String city);

    // String functions
    List<Customer> findByFirstNameStartsWith(String start);
    List<Customer> findByFirstNameEndsWith(String start);
    List<Customer> findByFirstNameContains(String start);
    List<Customer> findByFirstNameIsNotContaining(String start);

    // Between
    List<Customer> findByKidsBetween(int startKids, int endKids);
    List<Customer> findByBirthDayNotNullAndBirthDayBetween(Date start,
        Date end);

    // Regex, Like
    List<Customer> findByFirstNameRegex(String regex);
    List<Customer> findByFirstNameLike(String s);
    List<Customer> findByFirstNameIsNotLike(String s);

    // Other prefixes
    List<Customer> readByFirstName(String firstName);
    List<Customer> queryByFirstName(String firstName);
    Iterable<Customer> getByFirstName(String firstName);
    long countByFirstName(String firstName);

    // delete and remove
    List<Customer> deleteByFirstName(String firstName);
    List<Customer> removeByLastName(String lastName);

    List<Customer> readDistinctByFirstNameOrderByCustomerId(String first);
    long countDistinctByFirstName(String first);

    // Pageable, Sort, Slice
    Page<Customer> findByLastName(String lastname, Pageable pageable);
    Slice<Customer> findByFirstName(String lastname, Pageable pageable);
    List<Customer> findByLastName(String lastname, Sort sort);
    List<Customer> findByAddressCity(String city, Pageable pageable);

    // exists
    boolean existsByLastName(String lastname);
    List<Customer> findByAddressCityExists();

    // count
    long countByLastName(String name);

    // filter by primary key
    List<Customer> findAllByCustomerIdLessThan(long pk);

    // GeoJson near and within
    List<Customer> findByAddressGeoJsonPointNear(Circle circle);
    List<Customer> findByAddressGeoJsonPointWithin(Polygon point);

    // Top, First, Bottom, Last
    List<Customer> findFirstByOrderByLastNameAsc();
    List<Customer> findTopByOrderByLastNameDesc();
    List<Customer> findTop2ByOrderByLastNameAsc();


    // Native Queries
    @Query("SELECT * FROM Customer AS c WHERE c.kv_json_.firstName = 'John'")
    List<Customer> findCustomersByFirstNameJohn();

    @Query(value = "DECLARE $firstName STRING; SELECT * FROM Customer AS c " +
        "WHERE c.kv_json_.firstName = $firstName")
    List<Customer> findCustomersByFirstName(@Param("$firstName") String firstName);

    @Query("DECLARE $firstName STRING; $last STRING; " +
        "SELECT * FROM Customer AS c " +
        "WHERE c.kv_json_.firstName = $firstName AND " +
        "c.kv_json_.lastName = $last")
    List<Customer> findCustomersWithLastAndFirstNames(
        @Param("$last") String paramLast,
        @Param("$firstName") String firstName
        );

// todo Enable when positional bind params are supported
//    @Query(
//        "SELECT * FROM Customer AS c " +
//        "WHERE c.kv_json_.firstName = ? AND " +
//        "c.kv_json_.lastName = ?")
//    List<Customer> findCustomersWithFirstLast(
//        String first,
//        String last
//    );

    @Query("DECLARE $firstName STRING; $last STRING; " +
        "SELECT * FROM Customer AS c " +
        "WHERE c.kv_json_.firstName = $firstName AND " +
        "c.kv_json_.lastName = $last")
    List<Customer> findCustomersWithLastAndFirstNosqlValues(
        @Param("$last") StringValue paramLast,
        @Param("$firstName") StringValue firstName
    );

    // Projections
    List<CustomerView> findAllByLastName(String lastName);
    List<CustomerProjection> getAllByLastName(String lastName);

    List<CustomerView> findAllDistinctByLastName(String lastName);
    List<CustomerProjection> getAllDistinctByLastName(String lastName);

    List<CustomerViewWithId> queryByLastName(String lastName);
    List<CustomerProjectionWithId> readByLastName(String lastName);

    List<CustomerViewWithId> getDistinctByLastName(String lastName);
    List<CustomerProjectionWithId> queryDistinctByLastName(String lastName);
}