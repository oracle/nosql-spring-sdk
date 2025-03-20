/*-
 * Copyright (c) 2020, 2025 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */
package com.oracle.nosql.spring.data.test.app;

import java.util.Objects;

import org.springframework.data.geo.Point;

public class Address {
    public String street;
    public String city;
    public Point geoJsonPoint;

    public Address(String street, String city) {
        this.street = street;
        this.city = city;
    }

    public Address() {
    }

    @Override
    public String toString() {
        return "Address{" +
            "street='" + street + '\'' +
            ", city='" + city + '\'' +
            ", geoJsonPoint='" + geoJsonPoint + '\'' +
            '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof Address)) {
            return false;
        }
        Address a = (Address) obj;
        return (street == null && a.street == null ||
            street != null && street.equals(a.street)) &&
            (city == null && a.city == null ||
                city != null && city.equals(a.city)) &&
            (geoJsonPoint == null && a.geoJsonPoint == null ||
                geoJsonPoint != null && geoJsonPoint.equals(a.geoJsonPoint));
    }

    @Override
    public int hashCode() {
        return Objects.hash(street, city, geoJsonPoint);
    }

    public static class USAddress extends Address {
        String state;
        int zip;

        public USAddress() {
            super();
        }

        public USAddress(String street, String city, String state, int zip) {
            super(street, city);
            this.state = state;
            this.zip = zip;
        }

        @Override
        public String toString() {
            return "USAddress{" +
                super.toString() +
                ", state='" + state + '\'' +
                ", zip=" + zip +
                "}";
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null || !(obj instanceof USAddress)) {
                return false;
            }
            USAddress a = (USAddress) obj;
            return super.equals(a) &&
                (state == null && a.state == null ||
                    state != null && state.equals(a.state)) &&
                zip == a.zip;
        }

        @Override
        public int hashCode() {
            return Objects.hash(state, zip);
        }
    }

    public static class UKAddress extends Address {
        String code;

        public UKAddress() {
            super();
        }

        public UKAddress(String street, String city, String code) {
            super(street, city);
            this.code = code;
        }

        @Override
        public String toString() {
            return "UKAddress{" +
                super.toString() +
                ", code='" + code + '\'' +
                "} ";
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null || !(obj instanceof UKAddress)) {
                return false;
            }
            UKAddress a = (UKAddress) obj;
            return super.equals(a) &&
                (code == null && a.code == null ||
                    code != null && code.equals(a.code));
        }

        @Override
        public int hashCode() {
            return Objects.hash(code);
        }
    }
}