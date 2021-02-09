/*-
 * Copyright (c) 2020, 2021 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */
package com.oracle.nosql.spring.data.test.app;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

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
                "arr=%s}",
            customerId, firstName, lastName, kids, length, weight,
            coins, biField, bdField, vanilla,
            Arrays.toString(code),
            (birthDay != null ? birthDay.toInstant() : ""),
            address, addList,
            Arrays.toString(addArray), list, Arrays.toString(arr));
    }

    @Override
    public int hashCode() {
        int result = Objects
            .hash(customerId, firstName, lastName, kids, length, weight, coins,
                biField, bdField, vanilla, birthDay, address, addList, list);
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
            (arr != null && c.arr == null)
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
            Arrays.equals(arr, c.arr);
    }

    private boolean listEquals(Collection<?> c1, Collection<?> c2) {
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

            if (i1 == null && i2 != null || i1 != null && i2 == null) {
                return false;
            }
            if (i1 == null && i2 == null) {
                continue;
            }

            if (i1.getClass() == byte[].class &&
                i2.getClass() == String.class) {
                if (Arrays.equals((byte[]) i1,
                    BinaryValue.decodeBase64((String) i2))) {
                    continue;
                } else {
                    return false;
                }
            }
            if (i1.getClass() == String.class &&
                i2.getClass() == byte[].class) {
                if (Arrays.equals((byte[]) i2,
                    BinaryValue.decodeBase64((String) i1))) {
                    continue;
                } else {
                    return false;
                }
            }

            if (i1 instanceof Collection &&
                i2 instanceof Object[]) {
                if (listEquals((Collection) i1, Arrays.asList((Object[]) i2))) {
                    continue;
                } else {
                    return false;
                }
            }
            if (i2 instanceof Collection &&
                i1 instanceof Object[]) {
                if (listEquals((Collection) i2, Arrays.asList((Object[]) i1))) {
                    continue;
                } else {
                    return false;
                }
            }

            if (i1 instanceof Collection &&
                i2 instanceof Collection) {
                if (listEquals((Collection) i1, (Collection<?>) i2)) {
                    continue;
                } else {
                    return false;
                }
            }
            if (i1 instanceof Object[] &&
                i2 instanceof Object[]) {
                if (listEquals(Arrays.asList((Object[]) i1),
                    Arrays.asList((Object[]) i2))) {
                    continue;
                } else {
                    return false;
                }
            }

            if (!i1.equals(i2)) {
                return false;
            }
        }
        return true;
    }
}
