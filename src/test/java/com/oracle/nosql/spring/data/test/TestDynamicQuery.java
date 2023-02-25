/*-
 * Copyright (c) 2020, 2023 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */
package com.oracle.nosql.spring.data.test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.oracle.nosql.spring.data.core.NosqlTemplate;
import com.oracle.nosql.spring.data.test.app.Address;
import com.oracle.nosql.spring.data.test.app.AppConfig;
import com.oracle.nosql.spring.data.test.app.Customer;
import com.oracle.nosql.spring.data.test.app.CustomerRepository;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Point;
import org.springframework.data.geo.Polygon;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = AppConfig.class)
public class TestDynamicQuery {
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private CustomerRepository nosqlRepo;

    public static Customer c1, c2, c3, c4, c5, c6, c7;
    private static Customer[] c;


    static {
        c1 = new Customer("Alice", "Smith", null);
        c1.kids = 1;
        c1.vanilla = true;
        c1.length = 10L;

        c2 = new Customer("Bob", "Smith", null);
        c2.kids = 2;
        c2.vanilla = true;
        c2.weight = 2.2f;

        c3 = new Customer("John", "Smith", null);
        c3.kids = 3;
        c3.length = 303;
        c3.coins = 3.33d;

        c4 = new Customer("John", "Doe", null);
        c4.kids = 4;
        c4.weight = 4.4f;
        c4.biField = BigInteger.valueOf(4);

        c5 = new Customer("Clark", "Kent", null);
        c5.kids = 5;
        c5.coins = 5.55f;
        c5.bdField = BigDecimal.valueOf(5.05d);
        c5.birthDay = Date.from(Instant.parse("1977-04-18T05:00:00Z"));
        c5.address = new Address.USAddress("344 Clinton Street",
            "Metropolis", "NY", 21000);
        c5.address.geoJsonPoint = new Point(40.700960, -74.014504);

        c6 = new Customer("Berry", "Allan", null);
        c6.kids = 6;
        c6.biField = BigInteger.valueOf(6);
        c6.birthDay = Date.from(Instant.parse("1989-03-14T06:00:00Z"));
        c6.address = new Address.USAddress("Some St",
            "Central City", "NY", 21001);
        c6.address.geoJsonPoint = new Point(40.698472, -74.038335);

        c7 = new Customer("Diana", "Prince", null);
        c7.kids = 7;
        c7.bdField = BigDecimal.valueOf(7.07d);
        c7.birthDay = Date.from(Instant.parse("1979-06-06T07:00:00Z"));
        c7.address = new Address("Main St", "Paradise Island");

        c = new Customer[]{c1, c2, c3, c4, c5, c6, c7};
    }


    @Before
    public void before() {
        nosqlRepo.deleteAll();
        c1.customerId = c2.customerId = c3.customerId =
            c4.customerId = c5.customerId = c6.customerId = c7.customerId = 0;
        nosqlRepo.saveAll(Arrays.asList(c));
    }

    @After
    public void after() {
        nosqlRepo.deleteAll();
    }

    @Test
    public void testSimple() {
        List<Customer> johns = nosqlRepo.findByFirstName("John");

        Assert.assertEquals(2, johns.size());
        Assert.assertTrue(johns.contains(c3) && johns.contains(c4));

        List<Customer> smiths = nosqlRepo.findByLastName("Smith");

        Assert.assertEquals(3, smiths.size());
        Assert.assertTrue(smiths.containsAll(Arrays.asList(c1, c2, c3)));
    }

    @Test
    public void testExpressions() {
        List<Customer> jAndS = nosqlRepo.findByFirstNameAndLastName("John",
            "Smith");

        Assert.assertEquals(1, jAndS.size());
        Assert.assertTrue(jAndS.contains(c3));

        List<Customer> jOrS = nosqlRepo.findByFirstNameOrLastName("John",
            "Smith");

        Assert.assertEquals(4, jOrS.size());
        Assert.assertTrue(jOrS.containsAll(Arrays.asList(c1, c2, c3, c4)));
    }

    @Test
    public void testTypes() {
        List<Customer> list;
        list = nosqlRepo.findByKidsGreaterThanAndKidsLessThanEqual(4, 6);

        Assert.assertEquals(2, list.size());
        Assert.assertTrue(list.containsAll(Arrays.asList(c5, c6)));


        list = nosqlRepo.findByLengthIsGreaterThanEqualAndLengthIsLessThan(202,
            404);

        Assert.assertEquals(1, list.size());
        Assert.assertTrue(list.contains(c3));


        list = nosqlRepo.findByVanilla(true);

        Assert.assertEquals(2, list.size());
        Assert.assertTrue(list.containsAll(Arrays.asList(c1, c2)));


        list = nosqlRepo.findByVanillaIsTrue();

        Assert.assertEquals(2, list.size());
        Assert.assertTrue(list.containsAll(Arrays.asList(c1, c2)));


        list = nosqlRepo.findByWeight(2.2f);

        Assert.assertEquals(1, list.size());
        Assert.assertTrue(list.containsAll(Arrays.asList(c2)));


        list = nosqlRepo.findByCoins(3.33d);

        Assert.assertEquals(1, list.size());
        Assert.assertTrue(list.containsAll(Arrays.asList(c3)));


        list = nosqlRepo.findByBiField(BigInteger.valueOf(6));

        Assert.assertEquals(1, list.size());
        Assert.assertTrue(list.contains(c6));


        list = nosqlRepo.findByBdField(BigDecimal.valueOf(7.07d));

        Assert.assertEquals(1, list.size());
        Assert.assertTrue(list.contains(c7));

        //todo add tests for Timestamp, Date and Instant
    }

    @Test
    public void testOrderBy() {
        List<Customer> list;

        list = nosqlRepo.queryByLastNameOrderByFirstNameDesc("Smith");

        Assert.assertEquals(3, list.size());
        Assert.assertArrayEquals(new Customer[]{c3, c2, c1}, list.toArray());

        list = nosqlRepo.getByLastNameOrderByFirstNameAsc("Smith");

        Assert.assertEquals(3, list.size());
        Assert.assertArrayEquals(new Customer[]{c1, c2, c3}, list.toArray());
    }

    @Test
    public void testIgnoreCase() {
        List<Customer> list;

        list = nosqlRepo.findByLastNameIgnoreCase("smIth");
        Assert.assertEquals(3, list.size());
        Assert.assertTrue(list.containsAll(Arrays.asList(c1, c2, c3)));

        // ignore case applies to all fields
        list = nosqlRepo.findByLastNameAndFirstNameAllIgnoreCase("smIth", "alIce");
        Assert.assertEquals(1, list.size());
        Assert.assertTrue(list.containsAll(Arrays.asList(c1)));

        // ignore case applies only to firstName field
        list = nosqlRepo.findByLastNameAndFirstNameIgnoreCase("Smith", "alIce");
        Assert.assertEquals(1, list.size());
        Assert.assertTrue(list.containsAll(Arrays.asList(c1)));

        // ignore case for IN or NOT_IN expressions
        list = nosqlRepo.findByAddressCityIsInAllIgnoreCase(
            Arrays.asList("metropolis", "ParaDise isLand", "cenTral cITy"));
        Assert.assertEquals(3, list.size());
        Assert.assertTrue(list.containsAll(Arrays.asList(c5, c6, c7)));
    }

    @Test
    public void testTraverseProperty() {
        List<Customer> list;

        list = nosqlRepo.findByAddressCity("Metropolis");

        Assert.assertEquals(1, list.size());
        Assert.assertTrue(list.containsAll(Arrays.asList(c5)));
    }

    @Test
    public void testTraverseWithIn() {
        List<Customer> list;

        list = nosqlRepo.findByAddressCityIn(Arrays.asList("Metropolis",
            "Paradise Island", "Central City", 3));

        Assert.assertEquals(3, list.size());
        Assert.assertTrue(list.containsAll(Arrays.asList(c5, c6, c7)));


        list = nosqlRepo.findByAddressCityNotIn(Arrays.asList("Metropolis",
            "Paradise Island"));

        Assert.assertEquals(5, list.size());
        Assert.assertTrue(list.containsAll(Arrays.asList(c1, c2, c3, c4, c6)));


        list = nosqlRepo.findByAddressCityIsIn(new ArrayList<>());
        Assert.assertEquals(0, list.size());


        try {
            list = nosqlRepo.findByAddressCityIn("Central City");
            Assert.fail("Prev line should throw IllegalArgEx!");
        } catch (IllegalArgumentException ile) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void testStringFunctions() {
        List<Customer> list;

        list = nosqlRepo.findByFirstNameStartsWith("Di");

        Assert.assertEquals(1, list.size());
        Assert.assertTrue(list.contains(c7));


        list = nosqlRepo.findByFirstNameEndsWith("ana");

        Assert.assertEquals(1, list.size());
        Assert.assertTrue(list.contains(c7));


        list = nosqlRepo.findByFirstNameContains("rr");

        Assert.assertEquals(1, list.size());
        Assert.assertTrue(list.contains(c6));


        list = nosqlRepo.findByFirstNameIsNotContaining("o");

        Assert.assertEquals(4, list.size());
        Assert.assertTrue(list.containsAll(Arrays.asList(c1, c5, c6, c7)));
    }

    @Test
    public void testBetween() {
        List<Customer> list;

        list = nosqlRepo.findByKidsBetween(3, 6);

        Assert.assertEquals(4, list.size());
        Assert.assertTrue(list.containsAll(Arrays.asList(c3, c4, c5, c6)));


        list =
            nosqlRepo.findByBirthDayNotNullAndBirthDayBetween(
                Date.from(Instant.parse("1970-01-01T00:00:00Z")),
                Date.from(Instant.parse("1980-01-01T00:00:00Z")));

        Assert.assertEquals(2, list.size());
        Assert.assertTrue(list.containsAll(Arrays.asList(c5, c7)));
    }

    @Test
    public void testRegexLike() {
        List<Customer> list;

        list = nosqlRepo.findByFirstNameRegex("J.*");

        Assert.assertEquals(2, list.size());
        Assert.assertTrue(list.containsAll(Arrays.asList(c3, c4)));


        list = nosqlRepo.findByFirstNameLike("J.*");

        Assert.assertEquals(2, list.size());
        Assert.assertTrue(list.containsAll(Arrays.asList(c3, c4)));


        list = nosqlRepo.findByFirstNameIsNotLike("J.*");

        Assert.assertEquals(5, list.size());
        Assert.assertTrue(list.containsAll(Arrays.asList(c1, c2, c5, c6, c7)));
    }

    @Test
    public void testOtherPrefixes() {
        List<Customer> list;

        list = nosqlRepo.readByFirstName("John");

        Assert.assertEquals(2, list.size());
        Assert.assertTrue(list.containsAll(Arrays.asList(c3, c4)));


        list = nosqlRepo.queryByFirstName("John");

        Assert.assertEquals(2, list.size());
        Assert.assertTrue(list.containsAll(Arrays.asList(c3, c4)));


        long count = nosqlRepo.countByFirstName("John");
        Assert.assertEquals(2 /* c3 and c4*/, count);


        Iterable<Customer> iterable = nosqlRepo.getByFirstName("John");
        list = new ArrayList<>();
        iterable.iterator().forEachRemaining(list::add);
        Assert.assertEquals(2, list.size());
        Assert.assertTrue(list.containsAll(Arrays.asList(c3, c4)));
    }

    @Test
    public void testDistinct() {
        try {
            List<Customer> list =
                nosqlRepo.readDistinctByFirstNameOrderByCustomerId("John");
            Assert.fail();
        } catch (IllegalArgumentException iae) {
            // must throw java.lang.IllegalArgumentException: Distinct not
            // supported on full * projection.
        }

        long count = nosqlRepo.countDistinctByFirstName("John");
        Assert.assertEquals(2, count);
    }

    @Test
    public void testPageSlice() {
        //return Page
        Page<Customer> page = nosqlRepo.findByLastName("Smith",
            PageRequest.of(0, 1, Sort.by("kids")));

        List<Customer> list = new ArrayList<>();
        for (Customer c : page) {
            list.add(c);
        }
        Assert.assertEquals(1, list.size());
        Assert.assertTrue(list.contains(c1));


        // return Slice
        list.clear();
        Slice<Customer> slice = nosqlRepo.findByFirstName("John",
            PageRequest.of(0, 1,
                Sort.by(Sort.Direction.DESC, "kids")));

        for (Customer c : slice) {
            list.add(c);
        }
        Assert.assertEquals(1, list.size());
        Assert.assertTrue(list.contains(c4));


        // use Sort and return List
        list = nosqlRepo.findByLastName("Smith",
            Sort.by(Sort.Direction.DESC, "kids"));

        Assert.assertEquals(3, list.size());
        Assert.assertTrue(list.equals(Arrays.asList(c3, c2, c1)));


        // use Pageable return List
        list = nosqlRepo.findByAddressCity("Central City",
            PageRequest.of(0, 1, Sort.Direction.ASC, "kids"));

        Assert.assertEquals(1, list.size());
        Assert.assertTrue(list.contains(c6));
    }

    @Test
    public void testExists() {
        // starting with existsBy
        boolean exists = nosqlRepo.existsByLastName("Smith");
        Assert.assertTrue(exists);

        // exists expression on field
        List<Customer> list;
        list = nosqlRepo.findByAddressCityExists();

        Assert.assertEquals(3, list.size());
        Assert.assertTrue(list.containsAll(Arrays.asList(c5, c6, c7)));
    }

    @Test
    public void testCount() {
        long count = nosqlRepo.countByLastName("Smith");
        Assert.assertEquals(3, count);
    }

    @Test
    public void testFilterByPrimaryKey() {
        List<Customer> customers = Arrays.asList(c);

        List<Customer> expected = customers
            .stream()
            .map( c -> c.customerId)
            .filter( id -> id < c4.customerId ? true : false)
            .sorted()
            .map( id -> customers.stream()
                .filter(c -> c.customerId == id).findAny().get() )
            .collect(Collectors.toList());

        List<Customer> list;
        list = nosqlRepo.findAllByCustomerIdLessThan(c4.customerId);

        Assert.assertEquals(expected.size(), list.size());
        Assert.assertTrue(list.containsAll(expected));
    }

    @Test
    public void testGeoJson()
        throws ClassNotFoundException {

        // create index on points to improve performance
        NosqlTemplate template = NosqlTemplate.create(AppConfig.nosqlDBConfig);
        template.runTableRequest(
            "create index if not exists idx_Customer_points on Customer" +
            "(kv_json_.address.geoJsonPoint as point)");

        // using Near keyword
        // This is translated to SQL geo_near(entityShape, point, radius)
        List<Customer> list = nosqlRepo
            .findByAddressGeoJsonPointNear(
                new Circle( new Point(40.710376, -74.012735), 10));
        Assert.assertTrue(list.isEmpty());

        list = nosqlRepo
            .findByAddressGeoJsonPointNear(
                new Circle( new Point(40.710376, -74.012735), 1000));
        Assert.assertEquals(1, list.size());
        Assert.assertTrue(list.contains(c5));

        list = nosqlRepo
            .findByAddressGeoJsonPointNear(
                new Circle( new Point(40.710376, -74.012735), 5000));
        Assert.assertEquals(2, list.size());
        Assert.assertTrue(list.containsAll(Arrays.asList(c5, c6)));


        // using Within keyword
        List<Point> coord = new ArrayList<>();
        coord.add(new Point( 40.736739, -74.024951));
        coord.add(new Point( 40.679054, -74.038901));
        coord.add(new Point( 40.729716, -73.961308));
        coord.add(new Point( 40.736739, -74.024951));

        // This gets translated to SQL geo_inside(entityShape, polygon)
        list = nosqlRepo.findByAddressGeoJsonPointWithin(new Polygon(coord));

        Assert.assertEquals(1, list.size());
        Assert.assertTrue(list.contains(c5));


        coord.clear();
        coord.add(new Point( 40.736739, -74.024951));
        coord.add(new Point( 40.689174, -74.054158));
        coord.add(new Point( 40.679054, -74.038901));
        coord.add(new Point( 40.729716, -73.961308));
        coord.add(new Point( 40.736739, -74.024951));

        list = nosqlRepo.findByAddressGeoJsonPointWithin(new Polygon(coord));

        Assert.assertEquals(2, list.size());
        Assert.assertTrue(list.containsAll(Arrays.asList(c5, c6)));

        //todo add support for LineString, MultiPoint, MultiLineString,
        // MultiPolygon and GeometryCollection
        // see https://docs.oracle.com/en/database/other-databases/nosql-database/20.2/sqlfornosql/geojson-data-definitions.html
        // https://docs.oracle.com/en/database/other-databases/nosql-database/20.2/sqlreferencefornosql/functions-geojson-data.html
    }

    @Test
    public void testDeleteBy() {
        List<Customer> list = nosqlRepo
            .deleteByFirstName("John");
        Assert.assertTrue(list.size() == 2 &&
            list.containsAll(Arrays.asList(c3, c4)));

        list = nosqlRepo.findByFirstName("Smith");
        Assert.assertTrue(list.isEmpty());

        list = nosqlRepo.removeByLastName("Smith");
        Assert.assertTrue(list.size() == 2 &&
            list.containsAll(Arrays.asList(c1, c2)));

        list = nosqlRepo.findByLastName("Smith");
        Assert.assertTrue(list.isEmpty());
    }

    @Test
    public void testFindByNot() {
        List<Customer> list = nosqlRepo.findByFirstNameNot("John");
        Assert.assertEquals(5, list.size());
        Assert.assertTrue(list.containsAll(Arrays.asList(c1, c2, c5, c6, c7)));
    }

    @Test
    public void testFindByIsNull() {
        List<Customer> list = nosqlRepo.findByAddressIsNull();
        Assert.assertEquals(4, list.size());
        Assert.assertTrue(list.containsAll(Arrays.asList(c1, c2, c3, c4)));

        list = nosqlRepo.findByAddressNotNull();
        Assert.assertEquals(3, list.size());
        Assert.assertTrue(list.containsAll(Arrays.asList(c5, c6, c7)));
    }

    @Test
    public void testFirstTop() {
        List<Customer> list = nosqlRepo.findFirstByOrderByLastNameAsc();
        Assert.assertEquals(1, list.size());
        Assert.assertTrue(list.containsAll(Arrays.asList(c6)));


        list = nosqlRepo.findTopByOrderByLastNameDesc();
        Assert.assertEquals(1, list.size());
        // Result is any one of the 3 Smith customers
        Assert.assertTrue(Arrays.asList(c1, c2, c3).containsAll(list));

        list = nosqlRepo.findTop2ByOrderByLastNameAsc();
        Assert.assertEquals(2, list.size());
        Assert.assertTrue(list.containsAll(Arrays.asList(c4, c6)));
    }

    @Test
    public void testAndOr() {
        // AND takes precedence over OR
        List<Customer> list = nosqlRepo.findByFirstNameAndLastNameOrKids(
            "John" , "Doe", 2);
        Assert.assertEquals(2, list.size());
        Assert.assertTrue(list.containsAll(Arrays.asList(c2, c4)));


        list = nosqlRepo.findByFirstNameOrLastNameAndKids(
            "John" , "Doe", 2);
        Assert.assertEquals(2, list.size());
        Assert.assertTrue(list.containsAll(Arrays.asList(c3, c4)));
    }
}