/*-
 * Copyright (c) 2020, 2023 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */
package com.oracle.nosql.spring.data.core.mapping;

import org.springframework.data.mapping.PersistentProperty;

public interface NosqlPersistentProperty
    extends PersistentProperty<NosqlPersistentProperty> {

    /** To optimize class info for different properties make use of the
     * typecode.
     */
    enum TypeCode {
        OBJECT(1),              // java.lang.Object - used only during deserialization
        // atomic types 1 .. 99
        STRING(2),              // java.lang.String
        INT(3),                 // int, java.lang.Integer
        LONG(4),                // long, java.lang.Long
        FLOAT(5),               // float, java.lang.Float
        DOUBLE(6),              // double, java.lang.Double
        BIGINTEGER(7),          // java.math.BigInteger
        BIGDECIMAL(8),          // java.math.BigDecimal
        BOOLEAN(9),             // boolean, java.lang.Boolean
        BYTEARRAY(10),          // byte[]
        DATE(20),               // java.util.Date
        TIMESTAMP(21),          // java.sql.Timestamp
        INSTANT(22),            // java.time.Instant
        GEO_JSON_POINT(23),     // org.springframework.data.geo.Point
        GEO_JSON_POLYGON(24),   // org.springframework.data.geo.Polygon
        FIELD_VALUE(25),        // any of oracle.nosql.driver.values.FieldValue
        ENUM(26),               // any enum class
        MAP(27),                // java.util.Map

        // array like  100 .. 199
        ARRAY(100),      // <?>[]
        COLLECTION(101), // java.util.Collection<?>

        // POJO like 1000 .. 1999
        POJO(1000);      // ?

        private int code;
        TypeCode(int code) {
            this.code = code;
        }

        public boolean isAtomic() {
            return code > 1 && code < 100;
        }

//        public boolean isArrayLike() {
//            return code >= 100 && code < 200;
//        }
    }

    /**
     * The code of the type of the property
     *
     * @return The property type code
     */
    TypeCode getTypeCode();
}
