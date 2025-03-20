/*-
 * Copyright (c) 2020, 2025 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */
package com.oracle.nosql.spring.data.core.mapping;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeSet;

import oracle.nosql.driver.values.FieldValue;

import com.oracle.nosql.spring.data.Constants;
import com.oracle.nosql.spring.data.repository.support.NosqlEntityInformation;

import org.springframework.data.geo.Point;
import org.springframework.data.geo.Polygon;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.model.AnnotationBasedPersistentProperty;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.SimpleTypeHolder;

public class BasicNosqlPersistentProperty
    extends AnnotationBasedPersistentProperty<NosqlPersistentProperty>
    implements NosqlPersistentProperty {

    private TypeCode typeCode = null;

    BasicNosqlPersistentProperty(Property property,
        NosqlPersistentEntity<?> owner,
        SimpleTypeHolder simpleTypeHolder) {
        super(property, owner, simpleTypeHolder);
    }

    @Override
    protected Association<NosqlPersistentProperty> createAssociation() {
        return new Association<>(this, null);
    }

    @Override
    public boolean isIdProperty() {
        // returns true for isAnnotationPresent(Id.class)
        if (super.isIdProperty()) {
            return true;
        }

        // avoid the same check
        //if (isAnnotationPresent(Id.class)) {
        //    return true;
        //}

        if (isAnnotationPresent(NosqlId.class)) {
            return true;
        }

        return Constants.ID_PROPERTY_NAME.equals(getName());
    }

    /**
     * The code of the type of the property
     *
     * @return The property type code
     */
    @Override
    public TypeCode getTypeCode() {
        if (typeCode == null) {
            typeCode = getCodeForDeserialization(getType());
        }
        return typeCode;
    }

    public static TypeCode getCodeForDeserialization(Class<?> cls) {
        if (String.class.equals(cls)) {
            return TypeCode.STRING;
        } else if (int.class.equals(cls) || Integer.class.equals(cls)) {
            return TypeCode.INT;
        } else if (long.class.equals(cls) || Long.class.equals(cls)) {
            return TypeCode.LONG;
        } else if (float.class.equals(cls) || Float.class.equals(cls)) {
            return TypeCode.FLOAT;
        } else if (double.class.equals(cls) || Double.class.equals(cls)) {
            return TypeCode.DOUBLE;
        } else if (BigInteger.class.equals(cls)) {
            return TypeCode.BIGINTEGER;
        } else if (BigDecimal.class.equals(cls)) {
            return TypeCode.BIGDECIMAL;
        } else if (boolean.class.equals(cls) || Boolean.class.equals(cls)) {
            return TypeCode.BOOLEAN;
        } else if (byte[].class.equals(cls)) {
            return TypeCode.BYTEARRAY;
        } else if (Timestamp.class.isAssignableFrom(cls)) {
            // this must be before check for Date since Timestamp extends Date
            return TypeCode.TIMESTAMP;
        } else if (Date.class.isAssignableFrom(cls)) {
            return TypeCode.DATE;
        } else if (Instant.class.isAssignableFrom(cls)) {
            return TypeCode.INSTANT;
        } else if (Point.class.isAssignableFrom(cls)) {
            return TypeCode.GEO_JSON_POINT;
        } else if (Polygon.class.isAssignableFrom(cls)) {
            return TypeCode.GEO_JSON_POLYGON;
        } else if (cls.isArray()) {
            return TypeCode.ARRAY;
        } else if (cls.isAssignableFrom(ArrayList.class) ||
            cls.isAssignableFrom(HashSet.class) ||
            cls.isAssignableFrom(TreeSet.class)) {
            return TypeCode.COLLECTION;
        } else if (Object.class.equals(cls)) {
            return TypeCode.OBJECT;
        } else if (FieldValue.class.isAssignableFrom(cls)) {
            return TypeCode.FIELD_VALUE;
        } else if (Enum.class.isAssignableFrom(cls)) {
            return TypeCode.ENUM;
        } else {
            return TypeCode.POJO;
        }
    }

    public static TypeCode getCodeForSerialization(Class<?> cls) {
        if (String.class.equals(cls)) {
            return TypeCode.STRING;
        } else if (int.class.equals(cls) || Integer.class.equals(cls)) {
            return TypeCode.INT;
        } else if (long.class.equals(cls) || Long.class.equals(cls)) {
            return TypeCode.LONG;
        } else if (float.class.equals(cls) || Float.class.equals(cls)) {
            return TypeCode.FLOAT;
        } else if (double.class.equals(cls) || Double.class.equals(cls)) {
            return TypeCode.DOUBLE;
        } else if (BigInteger.class.equals(cls)) {
            return TypeCode.BIGINTEGER;
        } else if (BigDecimal.class.equals(cls)) {
            return TypeCode.BIGDECIMAL;
        } else if (boolean.class.equals(cls) || Boolean.class.equals(cls)) {
            return TypeCode.BOOLEAN;
        } else if (byte[].class.equals(cls)) {
            return TypeCode.BYTEARRAY;
        } else if (Timestamp.class.isAssignableFrom(cls)) {
            // this must be before check for Date since Timestamp extends Date
            return TypeCode.TIMESTAMP;
        } else if (Date.class.isAssignableFrom(cls)) {
            return TypeCode.DATE;
        } else if (Instant.class.isAssignableFrom(cls)) {
            return TypeCode.INSTANT;
        } else if (Point.class.isAssignableFrom(cls)) {
            return TypeCode.GEO_JSON_POINT;
        } else if (Polygon.class.isAssignableFrom(cls)) {
            return TypeCode.GEO_JSON_POLYGON;
        } else if (cls.isArray()) {
            return TypeCode.ARRAY;
        } else if (Collection.class.isAssignableFrom(cls)) {
            return TypeCode.COLLECTION;
        } else if (Map.class.isAssignableFrom(cls)) {
            return TypeCode.MAP;
        } else if (Object.class.equals(cls)) {
            return TypeCode.OBJECT;
        } else if (FieldValue.class.isAssignableFrom(cls)) {
            return TypeCode.FIELD_VALUE;
        } else if (Enum.class.isAssignableFrom(cls)) {
            return TypeCode.ENUM;
        } else {
            return TypeCode.POJO;
        }
    }

    @Override
    public boolean isCompositeKey() {
        return isIdProperty() &&
                NosqlEntityInformation.isCompositeKeyType(getType());
    }

    @Override
    public boolean isNosqlKey() {
        return isAnnotationPresent(NosqlKey.class);
    }
}