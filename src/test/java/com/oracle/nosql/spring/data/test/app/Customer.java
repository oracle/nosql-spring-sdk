/*-
 * Copyright (c) 2020, 2022 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */
package com.oracle.nosql.spring.data.test.app;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

import oracle.nosql.driver.values.BinaryValue;

import com.oracle.nosql.spring.data.core.mapping.NosqlId;
import com.oracle.nosql.spring.data.core.mapping.NosqlTable;

@NosqlTable(readUnits = 100, writeUnits = 50, storageGB = 1)
public class Customer {

    @NosqlId(generated = true)
    public long customerId;

    public String firstName;
    public String lastName;
    public int kids;
    public long length;
    public float weight;
    public double coins;
    public BigInteger biField;
    public BigDecimal bdField;
    public boolean vanilla;
    public byte[] code;
    public Date birthDay;

    public Address address;
    public List<Address> addList;
    public Address[] addArray;
    public List<Object> list;
    public Object[] arr;

    // Map fields
    public Map<Object, Object> mapField;                   // LinkedHashMap impl
    public Map<Priority, Object> enumMap;                  // LinkedHashMap impl
    @SuppressWarnings("unchecked")
    public Map classicMap;                                 // LinkedHashMap impl
    public Map<String, Object[]> arraysMap;                // LinkedHashMap impl

    public NavigableMap<String, Object> navigableMap;      // TreeMap impl
    public SortedMap<String, Object> sortedMap;            // TreeMap impl
    public HashMap<String, Object> hashMap;                // HashMap
    public LinkedHashMap<String, Object> linkedHashMap;    // LinkedHashMap
    public Hashtable<String, ArrayList<Object>> hashtable; // Hashtable
    public TreeMap<String, Object> treeMap;                // TreeMap

    public enum Priority {
        HIGH,
        MEDIUM,
        LOW
    }

    public Priority priority;

    public Customer() {}

    public Customer(String firstName, String lastName, List<Address> addresses) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.addList = addresses;
    }

    @Override
    public String toString() {
        return String.format(
            "Customer{id=%s, firstName='%s', lastName='%s', " +
                "kids=%s, length=%s, weight=%s, coins=%s, " +
                "biField=%s, bdField=%s, vanilla=%s, code=%s, birthDay='%s'" +
                "address=%s, addList='%s', addArray=%s, list=%s, " +
                "arr=%s, priority=%s, mapField=%s, enumMap=%s, " +
                "classicMap=%s, arraysMaps=%s}, navigableMap=%s, " +
                "sortedMap=%s, hashMap=%s, linkedHashMap=%s, hashtable=%s, " +
                "treeMap=%s",
            customerId, firstName, lastName, kids, length, weight,
            coins, biField, bdField, vanilla,
            Arrays.toString(code),
            (birthDay != null ? birthDay.toInstant() : ""),
            address, addList,
            Arrays.toString(addArray), list, Arrays.toString(arr), priority,
            mapField, enumMap, classicMap, arraysMap, navigableMap, sortedMap,
            hashMap, linkedHashMap, hashtable, treeMap);
    }

    @Override
    public int hashCode() {
        int result = Objects
            .hash(customerId, firstName, lastName, kids, length, weight, coins,
                biField, bdField, vanilla, birthDay, address, addList, list,
                priority, mapField, enumMap, classicMap, arraysMap,
                navigableMap, sortedMap, hashMap, linkedHashMap, hashtable,
                treeMap);
        result = 31 * result + Arrays.hashCode(code);
        result = 31 * result + Arrays.hashCode(addArray);
        result = 31 * result + Arrays.hashCode(arr);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Customer)) {
            return false;
        }

        Customer c = (Customer) obj;
        if ((firstName == null && c.firstName != null) ||
            (firstName != null && c.firstName == null) ||
            (lastName == null && c.lastName != null) ||
            (lastName != null && c.lastName == null) ||
            (biField == null && c.biField != null) ||
            (biField != null && c.biField == null) ||
            (bdField == null && c.bdField != null) ||
            (bdField != null && c.bdField == null) ||
            (code == null && c.code != null) ||
            (code != null && c.code == null) ||
            (birthDay == null && c.birthDay != null) ||
            (birthDay != null && c.birthDay == null) ||
            (address == null && c.address != null) ||
            (address != null && c.address == null) ||
            (addList == null && c.addList != null) ||
            (addList != null && c.addList == null) ||
            (addArray == null && c.addArray != null) ||
            (addArray != null && c.addArray == null) ||
            (list == null && c.list != null) ||
            (list != null && c.list == null) ||
            (arr == null && c.arr != null) ||
            (arr != null && c.arr == null) ||
            (priority == null && c.priority != null) ||
            (priority != null && c.priority == null) ||
            (mapField == null && c.mapField != null) ||
            (mapField != null && c.mapField == null) ||
            (enumMap == null && c.enumMap != null) ||
            (enumMap != null && c.enumMap == null) ||
            (classicMap == null && c.classicMap != null) ||
            (classicMap != null && c.classicMap == null) ||
            (arraysMap == null && c.arraysMap != null) ||
            (arraysMap != null && c.arraysMap == null) ||
            (navigableMap == null && c.navigableMap != null) ||
            (navigableMap != null && c.navigableMap == null) ||
            (sortedMap == null && c.sortedMap != null) ||
            (sortedMap != null && c.sortedMap == null) ||
            (hashMap == null && c.hashMap != null) ||
            (hashMap != null && c.hashMap == null) ||
            (linkedHashMap == null && c.linkedHashMap != null) ||
            (linkedHashMap != null && c.linkedHashMap == null) ||
            (hashtable == null && c.hashtable != null) ||
            (hashtable != null && c.hashtable == null) ||
            (treeMap == null && c.treeMap != null) ||
            (treeMap != null && c.treeMap == null)
        ) {
            return false;
        }


        return customerId == c.customerId &&
            (firstName == null && c.firstName == null ||
                firstName != null && firstName.equals(c.firstName)) &&
            (lastName == null && c.lastName == null ||
                lastName != null && lastName.equals(c.lastName)) &&
            kids == c.kids &&
            length == c.length &&
            weight == c.weight &&
            coins == c.coins &&
            (biField == null && c.biField == null ||
                biField != null && biField.compareTo(c.biField) == 0) &&
            (bdField == null && c.bdField == null ||
                bdField != null && bdField.compareTo(c.bdField) == 0) &&
            vanilla == c.vanilla &&
            Arrays.equals(code, c.code) &&
            (birthDay == null && c.birthDay == null ||
                birthDay != null && birthDay.equals(c.birthDay)) &&
            (address == null && c.address == null ||
                address != null && address.equals(c.address)) &&
            listEquals(addList, addList) &&
            Arrays.equals(addArray, c.addArray) &&
            listEquals(list, c.list) &&
            Arrays.equals(arr, c.arr) &&
            priority == c.priority &&
            mapEquals(mapField, c.mapField) &&
            mapEquals(enumMap, c.enumMap) &&
            mapEquals(classicMap, c.classicMap) &&
            mapEquals(arraysMap, c.arraysMap) &&
            mapEquals(navigableMap, c.navigableMap) &&
            mapEquals(sortedMap, c.sortedMap) &&
            mapEquals(hashMap, c.hashMap) &&
            mapEquals(linkedHashMap, c.linkedHashMap) &&
            mapEquals(hashtable, c.hashtable) &&
            mapEquals(treeMap, c.treeMap)
            ;
    }

    /** Check if the two values are matching. */
    private static boolean objEquals(Object o1, Object o2) {
        if (o1 == null && o2 != null || o1 != null && o2 == null) {
            return false;
        }

        if (o1 == null && o2 == null) {
            return true;
        }

        if (o1.getClass() == byte[].class &&
            o2.getClass() == String.class) {
            return Arrays.equals((byte[]) o1,
                BinaryValue.decodeBase64((String) o2));
        }
        if (o1.getClass() == String.class &&
            o2.getClass() == byte[].class) {
            return Arrays.equals((byte[]) o2,
                BinaryValue.decodeBase64((String) o1));
        }

        if (o1 instanceof Collection &&
            o2 instanceof Object[]) {
            return listEquals((Collection<?>) o1, Arrays.asList((Object[]) o2));
        }
        if (o2 instanceof Collection &&
            o1 instanceof Object[]) {
            return listEquals((Collection<?>) o2, Arrays.asList((Object[]) o1));
        }

        if (o1 instanceof Collection &&
            o2 instanceof Collection) {
            return listEquals((Collection<?>) o1, (Collection<?>) o2);
        }
        if (o1 instanceof Object[] &&
            o2 instanceof Object[]) {
            return listEquals(Arrays.asList((Object[]) o1),
                Arrays.asList((Object[]) o2));
        }

        if (o1 instanceof Map &&
            o2 instanceof Map) {
            return mapEquals((Map<?, ?>) o1, (Map<?, ?>) o2);
        }

        if (o1 instanceof Float &&
            o2 instanceof Double) {
            return Math.abs((double) o2 - (float) o1) < 0.0001d;
        }
        if (o1 instanceof Double &&
            o2 instanceof Float) {
            return Math.abs((double) o1 - (float) o2) < 0.0001d;
        }

        return o1.equals(o2);
    }

    /** Check if the two lists have matching values. */
    private static boolean listEquals(Collection<?> c1, Collection<?> c2) {
        if (c1 == null && c2 != null || c1 != null && c2 == null) {
            return false;
        }

        if (c1 == null && c2 == null) {
            return true;
        }

        if (c1.size() != c2.size()) {
            return false;
        }

        Iterator<?> it1 = c1.iterator();
        Iterator<?> it2 = c2.iterator();

        for (int i = 0; i < c1.size(); i++) {
            Object i1 = it1.next();
            Object i2 = it2.next();

            if (!objEquals(i1, i2)) {
                return false;
            }
        }
        return true;
    }

    /** Check if values inside the two maps match. Note: only m1 can contain
     *  enum objects as map keys that correspond to String keys in m2.
     *  Values that are byte[] are equal to their base64 encoding.
     */
    public static boolean mapEquals(Map<?, ?> m1, Map<?, ?> m2) {
        if (m1 == null && m2 != null || m1 != null && m2 == null) {
            return false;
        }

        if (m1 == null && m2 == null) {
            return true;
        }

        if (m1.size() != m2.size()) {
            return false;
        }

        for (Map.Entry<?, ?> e1 : m1.entrySet()) {
            Object k1 = e1.getKey();
            Object v1 = e1.getValue();
            Object v2 = m2.get(k1);

            if (v1 == null && v2 == null) {
                continue;
            }

            // Check if enum got saved as String
            if (v2 == null && k1 instanceof Enum) {
                v2 = m2.get(k1.toString());
            }

            if (!objEquals(v1, v2)) {
                return false;
            }
        }
        return true;
    }
}
