/*-
 * Copyright (c) 2020, 2025 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */
package com.oracle.nosql.spring.data.core.convert;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import oracle.nosql.driver.IndexExistsException;
import oracle.nosql.driver.IndexNotFoundException;
import oracle.nosql.driver.InvalidAuthorizationException;
import oracle.nosql.driver.JsonParseException;
import oracle.nosql.driver.NoSQLException;
import oracle.nosql.driver.OperationNotSupportedException;
import oracle.nosql.driver.RequestTimeoutException;
import oracle.nosql.driver.ResourceExistsException;
import oracle.nosql.driver.ResourceLimitException;
import oracle.nosql.driver.RetryableException;
import oracle.nosql.driver.TableExistsException;
import oracle.nosql.driver.TableNotFoundException;
import oracle.nosql.driver.TableSizeException;
import oracle.nosql.driver.UnauthorizedException;
import oracle.nosql.driver.values.ArrayValue;
import oracle.nosql.driver.values.BinaryValue;
import oracle.nosql.driver.values.BooleanValue;
import oracle.nosql.driver.values.DoubleValue;
import oracle.nosql.driver.values.FieldValue;
import oracle.nosql.driver.values.IntegerValue;
import oracle.nosql.driver.values.JsonNullValue;
import oracle.nosql.driver.values.LongValue;
import oracle.nosql.driver.values.MapValue;
import oracle.nosql.driver.values.NullValue;
import oracle.nosql.driver.values.NumberValue;
import oracle.nosql.driver.values.StringValue;
import oracle.nosql.driver.values.TimestampValue;

import com.oracle.nosql.spring.data.core.NosqlTemplateBase;
import com.oracle.nosql.spring.data.core.mapping.BasicNosqlPersistentProperty;
import com.oracle.nosql.spring.data.core.mapping.NosqlPersistentEntity;
import com.oracle.nosql.spring.data.core.mapping.NosqlPersistentProperty;
import com.oracle.nosql.spring.data.repository.support.NosqlEntityInformation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.dao.PermissionDeniedDataAccessException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.data.convert.EntityConverter;
import org.springframework.data.geo.Point;
import org.springframework.data.geo.Polygon;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.Parameter;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.model.ConvertingPropertyAccessor;
import org.springframework.data.mapping.model.EntityInstantiator;
import org.springframework.data.mapping.model.EntityInstantiators;
import org.springframework.data.mapping.model.ParameterValueProvider;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

public class MappingNosqlConverter
    implements EntityConverter<NosqlPersistentEntity<?>,
                               NosqlPersistentProperty,
                               Object,
                               FieldValue>,
        ApplicationContextAware {

    private static final String CLASS_FIELD_NAME = "#class";

    private static final Logger log =
        LoggerFactory.getLogger(MappingNosqlConverter.class);
    private final MappingContext<? extends NosqlPersistentEntity<?>,
        NosqlPersistentProperty>
        mappingContext;
    private final GenericConversionService conversionService;
    private ApplicationContext applicationContext;
    private final EntityInstantiators instantiators = new EntityInstantiators();

    public MappingNosqlConverter(
        @NonNull MappingContext<? extends NosqlPersistentEntity<?>,
            NosqlPersistentProperty> mappingContext) {
        this.mappingContext = mappingContext;
        this.conversionService = new GenericConversionService();
    }

    public <E> NosqlPersistentProperty getIdProperty(
        @NonNull Class<E> entityClass) {
        final NosqlPersistentEntity<?> persistentEntity =
            mappingContext.getPersistentEntity(entityClass);

        if (persistentEntity == null) {
            throw new MappingException("No mapping metadata for entity type: " +
                entityClass.getName());
        }

        return persistentEntity.getIdProperty();
    }

    @Nullable
    @Override
    public <R> R read(@NonNull Class<R> type,
        @NonNull FieldValue nosqlRowValue) {
        return convertFieldValueToObj(type, nosqlRowValue, true, null);
    }

    @Override
    @Deprecated
    public void write(@Nullable Object sourceEntity,
        @Nullable FieldValue document) {
        throw new UnsupportedOperationException(
            "The feature is not implemented yet");
    }

    public ApplicationContext getApplicationContext() {
        return this.applicationContext;
    }

    @Override
    public void setApplicationContext(
        @NonNull ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    @NonNull
    public ConversionService getConversionService() {
        return conversionService;
    }

    @NonNull
    @Override
    public  MappingContext<? extends NosqlPersistentEntity<?>,
        NosqlPersistentProperty> getMappingContext() {
        return mappingContext;
    }


    @NonNull
    private <T> ConvertingPropertyAccessor<T> getPropertyAccessor(
        T entity) {
        final NosqlPersistentEntity<?> entityInformation =
            mappingContext.getPersistentEntity(entity.getClass());

        Assert.notNull(entityInformation,
            "EntityInformation should not be null.");

        final PersistentPropertyAccessor<T> accessor =
            entityInformation.getPropertyAccessor(entity);
        return new ConvertingPropertyAccessor<>(accessor, conversionService);
    }


    /*
     *  Serialization from Java Object to FieldValue
     */

    /**
     * Converts an entity to the value stored in NosqlDB.
     *
     * @param objectToSave entity to be converted
     * @param skipSetId for inserts id is not set, it will be generated by
     *                  Nosql driver
     * @return the Nosql row representation to be saved into table
     */
    public <T> MapValue convertObjToRow(T objectToSave, boolean skipSetId) {

        if (objectToSave == null) {
            return null;
        }

        final NosqlPersistentEntity<?> persistentEntity =
            mappingContext.getPersistentEntity(objectToSave.getClass());

        if (persistentEntity == null) {
            throw new MappingException("no mapping metadata for entity type: " +
                objectToSave.getClass().getName());
        }

        final ConvertingPropertyAccessor<T> accessor =
            getPropertyAccessor(objectToSave);
        final NosqlPersistentProperty idProperty =
            persistentEntity.getIdProperty();
        MapValue row = new MapValue();
        MapValue valueMap = new MapValue();
        row.put(NosqlTemplateBase.JSON_COLUMN, valueMap);

        if (!skipSetId && idProperty != null) {
            if (idProperty.isCompositeKey()) {
                MapValue ids = convertObjToFieldValue(
                        accessor.getProperty(idProperty),
                        idProperty,
                        false).asMap();
                ids.getMap().forEach(row::put);
            } else {
                row.put(idProperty.getName(),
                    convertObjToFieldValue(accessor.getProperty(idProperty),
                        idProperty, false));
            }
        }

        for (NosqlPersistentProperty prop : persistentEntity) {
            if (prop.equals(idProperty) || !prop.isWritable()) {
                continue;
            }

            Object value = accessor.getProperty(prop);

            convertObjToFieldValue(value, valueMap, prop);
        }

        //System.out.println("  Convert obj: " + objectToSave + " -> row: " +
        // row);
        return row;
    }

    /*
     * isItemInCollection param indicates javaObj is an item in a Collection
     * or Array and the prop represents the property that holds the
     * Collection/Array.
     */
    private void convertObjToFieldValue(Object javaObj, MapValue valueMap,
        NosqlPersistentProperty prop) {

        FieldValue convertedValue = convertObjToFieldValue(javaObj, prop,
            false);
        valueMap.put(prop.getName(), convertedValue);
    }

    /*
     * isItemInCollection param indicates javaObj is an item in a Collection
     * or Array and the prop represents the property that holds the
     * Collection/Array.
     */
    @Nullable
    public FieldValue convertObjToFieldValue(Object javaObj,
        @Nullable NosqlPersistentProperty prop, boolean isItemInCollection) {

        if ( javaObj == null ) {
            return NullValue.getInstance();
        }

        NosqlPersistentProperty.TypeCode typeCode = BasicNosqlPersistentProperty
            .getCodeForSerialization(javaObj.getClass());

        FieldValue convertedValue;

        switch (typeCode) {
        case STRING:
            convertedValue = new StringValue((String) javaObj);
            break;
        case INT:
            convertedValue = new IntegerValue((Integer) javaObj);
            break;
        case LONG:
            convertedValue = new LongValue((Long) javaObj);
            break;
        case FLOAT:
            convertedValue = new DoubleValue((float) javaObj);
            break;
        case DOUBLE:
            convertedValue = new DoubleValue((double) javaObj);
            break;
        case BIGINTEGER:
            convertedValue = new NumberValue(
                new BigDecimal((BigInteger) javaObj));
            break;
        case BIGDECIMAL:
            convertedValue = new NumberValue((BigDecimal) javaObj);
            break;
        case BOOLEAN:
            convertedValue = BooleanValue.getInstance((boolean) javaObj);
            break;
        case BYTEARRAY:
            convertedValue = new BinaryValue((byte[]) javaObj);
            break;
        case DATE:
            convertedValue = new TimestampValue(((Date) javaObj).getTime());
            break;
        case TIMESTAMP:
            convertedValue = new TimestampValue((Timestamp) javaObj);
            break;
        case INSTANT:
            convertedValue = new TimestampValue(
                ((Instant) javaObj).toEpochMilli());
            break;
        case ENUM:
            convertedValue = new StringValue(((Enum) javaObj).name());
            break;
        case GEO_JSON_POINT:
            Point point = (Point) javaObj;
            MapValue geoPoint = new MapValue();
            geoPoint.put(CLASS_FIELD_NAME, Point.class.getName());
            geoPoint.put("type", "point");
            geoPoint.put("coordinates",
                new ArrayValue(2).add(point.getX()).add(point.getY()));
            convertedValue = geoPoint;
            break;
        case GEO_JSON_POLYGON:
            Polygon polygon = (Polygon) javaObj;
            MapValue geoPolygon = new MapValue();
            geoPolygon.put("type", "polygon");
            ArrayValue coordinates = new ArrayValue();
            polygon.forEach(p ->
                coordinates.add(
                    new ArrayValue(2).add(p.getX()).add(p.getY())));
            geoPolygon.put("coordinates",
                new ArrayValue(1).add(coordinates));
            //geoPolygon.put(CLASS_FIELD_NAME, Polygon.class.getName());
            convertedValue = geoPolygon;
            break;
        case MAP:
            if (!(javaObj instanceof Map)) {
                throw new IllegalStateException("Expected Map<?,?> " +
                    "actual: " + javaObj.getClass().getName());
            }
            convertedValue = convertMapObjToFieldValue((Map<?, ?>) javaObj, prop);
            break;
        case ARRAY:
            if (!(javaObj instanceof Object[])) {
                throw new IllegalStateException("Expected Object[] " +
                    "actual: " + javaObj.getClass().getName());
            }
            convertedValue = convertCollectionObjToFieldValue(
                Arrays.asList((Object[]) javaObj), prop);
            break;
        case COLLECTION:
            if (!(javaObj instanceof Collection<?>)) {
                throw new IllegalStateException("Expected Collection<?> " +
                    "actual: " + javaObj.getClass().getName());
            }
            convertedValue =
                convertCollectionObjToFieldValue((Collection<?>) javaObj, prop);
            break;
        case FIELD_VALUE:
            convertedValue = (FieldValue) javaObj;
            break;
        case POJO:
            Class<?> expectedCls = null;
            if (prop != null) {
                expectedCls = isItemInCollection ? prop.getActualType() :
                    prop.getType();
            }
            convertedValue = convertPojoObjToFieldValue(javaObj, expectedCls);
            break;
        case OBJECT:
        default:
            throw new RuntimeException("Simple type: " + typeCode + " " +
                "not supported.");
        }

        //System.out.println("      " + convertedValue.getType() + "  " +
        // convertedValue);
        return convertedValue;
    }

    private <K, V> FieldValue convertMapObjToFieldValue(Map<K, V> javaObj,
        NosqlPersistentProperty prop) {

        if (javaObj == null) {
            return null;
        }

        MapValue res = new MapValue();
        for (Map.Entry<K, V> entry : javaObj.entrySet()) {
            if (entry.getKey() == null) {
                throw new IllegalArgumentException("Unsupported null map key: " +
                    prop);
            }
            boolean isEnum = entry.getKey().getClass().isEnum();
            String key;
            if (isEnum) {
                key = ((Enum) entry.getKey()).name();
            } else if (entry.getKey().getClass() == String.class) {
                key = (String) entry.getKey();
            } else  {
                throw new IllegalArgumentException("Unsupported map key type: " +
                    entry.getKey().getClass());
            }
            res.put( key,
                convertObjToFieldValue(entry.getValue(), null, false));
        }
        return res;
    }

    private <T> FieldValue convertPojoObjToFieldValue(@NonNull T javaObj,
        Class<?> expectedCls) {
        final NosqlPersistentEntity<?> persistentEntity =
            mappingContext.getPersistentEntity(javaObj.getClass());

        if (persistentEntity == null) {
            throw new MappingException("no mapping metadata for entity type: " +
                javaObj.getClass().getName());
        }

        final ConvertingPropertyAccessor<T> accessor =
            getPropertyAccessor(javaObj);
        MapValue valueMap = new MapValue();

        if (javaObj.getClass() != expectedCls) {
            valueMap.put(CLASS_FIELD_NAME,
                javaObj.getClass().getName());
        }

        for (NosqlPersistentProperty prop : persistentEntity) {
            if (!prop.isWritable()) {
                continue;
            }

            Object propValue = accessor.getProperty(prop);

            convertObjToFieldValue(propValue, valueMap, prop);
        }
        return valueMap;
    }

    private FieldValue convertCollectionObjToFieldValue(
        Collection<?> javaCollection,
        @Nullable NosqlPersistentProperty prop) {

        if (prop != null) {
            Assert.isTrue(prop.isCollectionLike(),
                "PersistedProperty required to" +
                    " be list like.");
        }

        ArrayValue arrayValue = new ArrayValue(javaCollection.size());
        for (Object element : javaCollection) {
            arrayValue.add(convertObjToFieldValue(element, prop, true));
        }
        return arrayValue;
    }

    public <E, ID> E setId(E objectToSave, FieldValue id) {
        final NosqlPersistentEntity<?> persistentEntity =
            mappingContext.getPersistentEntity(objectToSave.getClass());

        if (persistentEntity == null) {
            throw new MappingException("no mapping metadata for entity type: " +
                objectToSave.getClass().getName());
        }

        final ConvertingPropertyAccessor<E> accessor =
            getPropertyAccessor(objectToSave);
        final NosqlPersistentProperty idProperty =
            persistentEntity.getIdProperty();

        if (idProperty != null) {
            ID idValue = convertFieldValueToObject(id, idProperty);
            accessor.setProperty(idProperty, idValue);
        }
        return objectToSave;
    }




    /*
     *  Deserialization from FieldValue to Java Object
     */

    /**
     * If isRoot is true, it means nosqlValue represents the top level row,
     * otherwise it represents a POJO inside the json tree.
     */
    @SuppressWarnings("unchecked")
    private <E> E convertFieldValueToObj(Class<?> type,
        final FieldValue nosqlValue, boolean isRoot,
        @Nullable TypeInformation<E> typeInfo) {

        if (type != null && nosqlValue != null &&
            FieldValue.class.isAssignableFrom(type) &&
            type.isAssignableFrom(nosqlValue.getClass())) {
            return (E) nosqlValue;
        }

        if (nosqlValue == null || nosqlValue.isNull() ||
            nosqlValue.isJsonNull() || nosqlValue.isEMPTY()) {
            return null;
        }

        FieldValue.Type nosqlType = nosqlValue.getType();
        switch (nosqlType) {
        case INTEGER:
            return (E) (Integer) nosqlValue.asInteger().getValue();

        case LONG:
            return (E) (Long) nosqlValue.asLong().getValue();

        case DOUBLE:
            return (E) (Double) nosqlValue.asDouble().getValue();

        case NUMBER:
            return (E) nosqlValue.asNumber().getValue();

        case STRING:
            return (E) nosqlValue.asString().getValue();

        case TIMESTAMP:
            return (E) nosqlValue.asTimestamp().getString();

        case BOOLEAN:
            return (E) (Boolean) nosqlValue.asBoolean().getValue();

        case BINARY:
            return convertFieldValueToByteArray(nosqlValue, type);

        case EMPTY:
        case NULL:
        case JSON_NULL:
            return null;

        case ARRAY:
            NosqlPersistentProperty.TypeCode entityCode =
                BasicNosqlPersistentProperty.getCodeForDeserialization(type);
            switch(entityCode) {
            case BYTEARRAY:
                return convertFieldValueToByteArray(nosqlValue, type);

            case COLLECTION:
            case OBJECT:
                return (E) convertArrayValueToCollection(nosqlValue, typeInfo);

            case ARRAY:
                List<Object> list = convertArrayValueToCollection(nosqlValue,
                    typeInfo);
                return (E) list.toArray();

            default:
                throw new IllegalArgumentException("Conversion unknown from: " +
                    nosqlValue.getType().name() + " to " + type.getName() +
                    ".");
            }

        case MAP:
            E entityObj = null;

            final NosqlPersistentEntity<E> entity = (NosqlPersistentEntity<E>)
                mappingContext.getPersistentEntity(
                    type != null ? type : Object.class);

            if (isRoot) {
                if (type == MapValue.class) {
                    // this must be a special query
                    return (E) nosqlValue.asMap();
                }

                // if toplevel row get id from id column and rest of the
                // properties from JSON column value
                FieldValue idFieldValue = null;

                if (entity.getIdProperty() != null) {
                    NosqlPersistentProperty idProperty = entity.getIdProperty();

                    if (idProperty.isCompositeKey()) {
                        idFieldValue = new MapValue();
                        NosqlPersistentEntity<?> idEntity =
                                mappingContext.getPersistentEntity(idProperty.getType());
                        for (NosqlPersistentProperty p : idEntity) {
                            idFieldValue.asMap().put(p.getName(),
                                    nosqlValue.asMap().get(p.getName()));
                        }
                    } else {
                        idFieldValue = nosqlValue.asMap()
                                .get(entity.getIdProperty().getName());
                    }
                }

                MapValue jsonValue = null;
                if (nosqlValue.asMap().get(NosqlTemplateBase.JSON_COLUMN) !=
                    null) {
                    jsonValue = nosqlValue.asMap().
                            get(NosqlTemplateBase.JSON_COLUMN).asMap();
                }
                NosqlPersistentEntity<E> clsEntity =
                    updateEntity(entity, getInstanceClass(jsonValue));
                entityObj = getNewInstance(clsEntity, nosqlValue.asMap(),
                    jsonValue);

                if (idFieldValue != null) {
                    setId(entityObj, idFieldValue);
                }
                setPojoProperties(clsEntity, entityObj, jsonValue);

            } else {
                MapValue mapValue = nosqlValue.asMap();
                String instClsStr = getInstanceClass(mapValue);

                if (Point.class.getName().equals(instClsStr)) {
                    if (mapValue.get("coordinates") != null &&
                        mapValue.get("coordinates").getType() ==
                            FieldValue.Type.ARRAY &&
                        mapValue.get("coordinates").asArray().size() >= 2 &&
                        mapValue.get("coordinates").asArray().get(0) != null &&
                        mapValue.get("coordinates").asArray().get(1) != null &&
                        mapValue.get("coordinates").asArray().get(0).isNumeric() &&
                        mapValue.get("coordinates").asArray().get(1).isNumeric()
                    ) {
                        ArrayValue coord = mapValue.get("coordinates").asArray();
                        return (E) new Point(coord.get(0).getDouble(),
                            coord.get(1).getDouble());
                    } else {
                        throw new IllegalArgumentException("Unexpected " +
                            "GeoJson " +
                            "point representation: " + mapValue);
                    }

                } else if (Polygon.class.getName().equals(instClsStr)) {
                    if (mapValue.get("coordinates") != null &&
                        mapValue.get("coordinates").getType() ==
                            FieldValue.Type.ARRAY &&
                        mapValue.get("coordinates").asArray().get(0) != null &&
                        mapValue.get("coordinates").asArray().get(0).getType()
                            == FieldValue.Type.ARRAY &&
                        mapValue.get("coordinates").asArray().get(0).asArray()
                            != null &&
                        mapValue.get("coordinates").asArray().get(0).asArray()
                            .get(0) != null &&
                        mapValue.get("coordinates").asArray().get(0).asArray()
                            .get(0).getType() == FieldValue.Type.ARRAY
                    ) {
                        ArrayValue coord =
                            mapValue.get("coordinates")
                                .asArray()
                                .get(0)
                                .asArray()
                                .get(0)
                                .asArray();

                        List<Point> points = new ArrayList<>(coord.size());
                        coord.forEach(fv ->
                        {
                            if (fv != null &&
                                fv.getType() == FieldValue.Type.ARRAY &&
                                fv.asArray().size() >= 2 &&
                                fv.asArray().get(0) != null &&
                                fv.asArray().get(1) != null &&
                                fv.asArray().get(0).isNumeric() &&
                                fv.asArray().get(1).isNumeric()
                            ) {
                                points.add(
                                    new Point(
                                        fv.asArray()
                                            .get(0)
                                            .getDouble(),
                                        fv.asArray()
                                            .get(1)
                                            .getDouble()));
                            } else {
                                throw new IllegalArgumentException(
                                    "Unexpected GeoJson " +
                                    "point representation: " + fv);
                            }
                        });

                        return (E) new Polygon(points);
                    } else {
                        throw new IllegalArgumentException("Unexpected " +
                            "GeoJson polygon representation: " + mapValue);
                    }
                } else if ((type != null && Map.class.isAssignableFrom(type))
                    || (type == Object.class && instClsStr == null)) {
                    entityObj = (E) convertMapValueToMap(nosqlValue.asMap(),
                        typeInfo);
                } else if (entity != null || instClsStr != null) {
                    // decode to POJO
                    NosqlPersistentEntity<E> clsEntity =
                        updateEntity(entity, instClsStr);
                    entityObj = getNewInstance(clsEntity, null, mapValue);

                    setPojoProperties(clsEntity, entityObj, mapValue);
                } else {
                    // not enough info to deserialize go for a Map
                    entityObj = (E) convertMapValueToMap(nosqlValue.asMap(),
                        typeInfo);
                }
            }
            return entityObj;

        default:
            throw new IllegalStateException("Unknown FieldValue.Type: " +
                nosqlType.name());
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <K, V, E, C> Map<K, V> convertMapValueToMap(MapValue mapValue,
        @Nullable TypeInformation<E> typeInfo) {

        TypeInformation<V> valueType = typeInfo == null ? null :
            (TypeInformation<V>) typeInfo.getMapValueType();
        Class<?> valueTypeClass = valueType != null ? valueType.getType()
            : Object.class;

        Map<K, V> res;
        if (typeInfo != null && HashMap.class == typeInfo.getType()) {
            res = new HashMap<>();
        } else if (typeInfo == null ||
            typeInfo.getType().isAssignableFrom(LinkedHashMap.class)) {
            res = new LinkedHashMap<>();
        } else if (typeInfo.getType().isAssignableFrom(Hashtable.class)) {
            res = new Hashtable<>();
        } else if (typeInfo.getType().isAssignableFrom(TreeMap.class)) {
            res = new TreeMap<>();
        } else {
            throw new IllegalArgumentException("Unsupported Map type: " +
                typeInfo.getType());
        }

        TypeInformation<K> componentType = (typeInfo == null) ? null :
            (TypeInformation<K>) typeInfo.getComponentType();
        for (Map.Entry<String, FieldValue> entry : mapValue.getMap().entrySet())
        {
            K key;
            if (typeInfo != null && typeInfo.isMap() &&
                componentType != null &&
                componentType.getType().isEnum()) {
                key = (K) Enum.valueOf(
                    (Class<? extends Enum>) componentType.getType(),
                    entry.getKey());
            } else {
                key = (K) entry.getKey();
            }
            res.put( key,
                convertFieldValueToObj(valueTypeClass, entry.getValue(), false,
                    valueType));
        }
        return res;
    }

    private static <T extends Enum<T>> T of(Class<T> clazz, String key) {
        T value = Enum.valueOf(clazz, key);
        return value;
    }

    @SuppressWarnings("unchecked")
    private <E> E convertFieldValueToByteArray(FieldValue nosqlValue,
        Class<?> type) {
        switch (nosqlValue.getType()) {
        case BINARY:
            return (E) nosqlValue.asBinary().getValue();
        case STRING:
            return (E) BinaryValue.decodeBase64(
                nosqlValue.asString().getValue());
        default:
            throw new IllegalArgumentException("Conversion unknown from: " +
                nosqlValue.getType().name() + " to " + type.getName() +
                ".");
        }
    }

    /* In case an instance class was specified in the serialized (using
     * #class field), than that will be used if possible.
     */
    @SuppressWarnings("unchecked")
    private <E> NosqlPersistentEntity<E> updateEntity(
        NosqlPersistentEntity<E> entity,
        String instanceClsName) {
        if (instanceClsName != null && (entity == null ||
            !entity.getType().getName().equals(instanceClsName))) {
            try {
                Class<?> instanceCls = Class.forName(instanceClsName);
                if (instanceCls != null && (entity == null ||
                    entity.getType().isAssignableFrom(instanceCls))) {
                        entity = (NosqlPersistentEntity<E>)
                            mappingContext.getPersistentEntity(instanceCls);
                }
            } catch (Exception cnf) {
                // if instanceClass is not found ignore it, try using the
                // expected one
                if (entity == null) {
                    throw new IllegalArgumentException("Class '" +
                        instanceClsName + "' couldn't be found and no entity " +
                        "hint available.");
                }
            }
        }
        return entity;
    }

    private String getInstanceClass(MapValue mapValue) {
        if (mapValue != null) {
            FieldValue clsField = mapValue.get(CLASS_FIELD_NAME);
            if (clsField != null &&
                clsField.getType() == FieldValue.Type.STRING ){
                return clsField.asString().getValue();
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private <E> List<Object> convertArrayValueToCollection(FieldValue nosqlValue,
           @Nullable TypeInformation<E> typeInfo) {
        List<Object> list = new ArrayList<>();
        TypeInformation<E> componentType = (typeInfo == null) ? null :
            (TypeInformation<E>) typeInfo.getComponentType();
        for (FieldValue item : nosqlValue.asArray()) {
            list.add(convertFieldValueToObj(Object.class,
                item, false,
                componentType));
        }
        return list;
    }

    private <R> R getNewInstance(NosqlPersistentEntity<R> entity,
        MapValue rootFieldValue,
        @Nullable MapValue jsonValue) {

        EntityInstantiator instantiator =
            instantiators.getInstantiatorFor(entity);

        ParameterValueProvider<NosqlPersistentProperty> paramProvider =
            new ParameterValueProvider<NosqlPersistentProperty>() {
                @Override
                public <T> T getParameterValue(
                    @NonNull Parameter<T, NosqlPersistentProperty> parameter) {

                    String paramName = parameter.getName();
                    // Sometimes isIdProperty is not set correctly hence the
                    // dance below
                    NosqlPersistentProperty prop =
                        entity.getPersistentProperty(paramName);

                    FieldValue value = null;
                    if (rootFieldValue == null && jsonValue != null) {
                        value = jsonValue.get(paramName);
                    } else {
                        if (prop.isIdProperty()) {
                            value = rootFieldValue.get(paramName);
                        } else {
                            if (jsonValue != null) {
                                value = jsonValue.get(paramName);
                            }
                            if (value == null) {
                                // if field is not marked id and it's not in
                                // kv_json_ it may be an unmarked id field
                                value = rootFieldValue.get(paramName);
                            }
                        }
                    }

                    return convertFieldValueToObject( value, prop);
                }
            };
        try {
            return instantiator.createInstance(entity, paramProvider);
        } catch (RuntimeException e) {
            throw new RuntimeException("Failed to instantiate entity type: " +
                entity.getType() + ". Check available public constructors.", e);
        }
    }

    private <E> void setPojoProperties(NosqlPersistentEntity<E> entity,
        E entityObj, MapValue jsonValue) {
        if (jsonValue == null) {
            return;
        }

        final ConvertingPropertyAccessor<E> accessor =
            getPropertyAccessor(entityObj);

        for (Map.Entry<String, FieldValue> entry :  jsonValue.entrySet() ) {
            NosqlPersistentProperty prop =
                entity.getPersistentProperty(entry.getKey());
            if (prop != null && prop.isWritable()) {
                Object value =
                    convertFieldValueToObject(entry.getValue(), prop);
                accessor.setProperty(prop, value);
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private <T> T convertFieldValueToObject(FieldValue fieldValue,
        @Nullable NosqlPersistentProperty prop) {

        if (fieldValue == null) {
            return null;
        }

        T objValue;
        NosqlPersistentProperty.TypeCode objClsTypeCode =
            (prop == null ?
                NosqlPersistentProperty.TypeCode.OBJECT :
                prop.getTypeCode());

        switch(fieldValue.getType()) {
        case NULL:
        case JSON_NULL:
            objValue = null;
            break;
        case STRING:
            switch(objClsTypeCode) {
            case STRING:
                objValue = (T) fieldValue.asString().getValue();
                break;
            case BYTEARRAY:
                objValue = (T)
                    BinaryValue.decodeBase64(fieldValue.asString().getValue());
                break;
            case DATE:
                objValue = (T) new Date(
                    fieldValue.asTimestamp().getValue().getTime());
                break;
            case TIMESTAMP:
                objValue = (T) fieldValue.asTimestamp().getValue();
                break;
            case INSTANT:
                objValue = (T) fieldValue.asTimestamp().getValue().toInstant();
                break;
            case ENUM:
                String strValue = fieldValue.asString().getValue();

                objValue = (T) Enum.valueOf((Class) prop.getType(),
                    strValue.trim().toUpperCase());
                break;
            default:
                throw new IllegalArgumentException("Conversion unknown from: " +
                    fieldValue.getType().name() + " to " + objClsTypeCode + ".");
            }
            break;
        case INTEGER:
            int i = fieldValue.asInteger().getValue();
            switch (objClsTypeCode) {
            case INT:
                objValue = (T) (Integer) i;
                break;
            case STRING:
                objValue = (T) String.valueOf(i);
                break;
            default:
                throw new IllegalArgumentException("Conversion unknown from: " +
                    fieldValue.getType().name() + " to " + objClsTypeCode + ".");
            }
            break;
        case LONG:
            long l = fieldValue.asLong().getValue();
            switch (objClsTypeCode) {
            case LONG:
                objValue = (T) (Long) l;
                break;
            case STRING:
                objValue = (T) String.valueOf(l);
                break;
            default:
                throw new IllegalArgumentException("Conversion unknown from: " +
                    fieldValue.getType().name() + " to " +
                    objClsTypeCode + ".");
            }
            break;
        case DOUBLE:
            switch (objClsTypeCode) {
            case DOUBLE:
                objValue = (T) (Double) fieldValue.asDouble().getValue();
                break;
            case FLOAT:
                objValue =
                    (T) (Float) (float) fieldValue.asDouble().getValue();
                break;
            default:
                throw new IllegalArgumentException("Conversion unknown from: " +
                    fieldValue.getType().name() + " to " + objClsTypeCode + ".");
            }
            break;
        case NUMBER:
            BigDecimal decimal = fieldValue.asNumber().getNumber();
            switch (objClsTypeCode) {
            case BIGDECIMAL:
                objValue = (T) decimal;
                break;
            case BIGINTEGER:
                objValue = (T) decimal.toBigInteger();
                break;
            case STRING:
                objValue = (T) decimal.toString();
                break;
            default:
                throw new IllegalArgumentException("Conversion unknown from: " +
                    fieldValue.getType().name() + " to " + objClsTypeCode + ".");
            }
            break;
        case BOOLEAN:
            boolean boolVal = fieldValue.asBoolean().getValue();
            switch (objClsTypeCode) {
            case BOOLEAN:
                objValue = (T) (Boolean) boolVal;
                break;
            case STRING:
                objValue = (T) Boolean.valueOf(boolVal).toString();
                break;
            default:
                throw new IllegalArgumentException("Conversion unknown from: " +
                    fieldValue.getType().name() + " to " + objClsTypeCode + ".");
            }
            break;
        case TIMESTAMP:
            Timestamp timestamp = fieldValue.asTimestamp().getValue();
            switch (objClsTypeCode) {
            case DATE:
                objValue = (T) new Date(timestamp.getTime());
                break;
            case TIMESTAMP:
                objValue = (T) new Timestamp(timestamp.getTime());
                break;
            case INSTANT:
                objValue = (T) Instant.ofEpochMilli(timestamp.getTime());
                break;
            case STRING:
                objValue = (T) timestamp.toString();
                break;
            default:
                throw new IllegalArgumentException("Conversion unknown from: " +
                    fieldValue.getType().name() + " to " + objClsTypeCode + ".");
            }
            break;
        case BINARY:
            // there is no BINARY coming back from JSON, it comes back as STRING
            byte[] baVal = fieldValue.asBinary().getValue();
            switch (objClsTypeCode) {
            case BYTEARRAY:
                objValue = (T) baVal;
                break;
            case STRING:
                objValue = (T) Arrays.toString(baVal);
                break;
            default:
                throw new IllegalArgumentException("Conversion unknown from: " +
                    fieldValue.getType().name() + " to " + objClsTypeCode + ".");
            }
            break;
        case ARRAY:
            List<Object> list = new ArrayList<>();
            Class<?> actualType = ( prop == null ? Object.class :
                prop.getActualType());
            for (FieldValue item : fieldValue.asArray()) {
                list.add(convertFieldValueToObj(actualType ,
                    item, false, prop.getTypeInformation()));
            }

            switch(objClsTypeCode) {
            case COLLECTION:
                Class<?> propType = prop.getRawType();
                if (propType.isAssignableFrom(ArrayList.class)) {
                    objValue = (T) list;
                } else if (propType.isAssignableFrom(HashSet.class)) {
                    objValue = (T) new HashSet<>(list);
                } else if (propType.isAssignableFrom(TreeSet.class)) {
                    objValue = (T) new TreeSet<>(list);
                } else {
                    objValue = (T) list;
                }
                break;
            case ARRAY:
                objValue = (T) listToArray(list, prop.getActualType());
                break;
            default:
                throw new IllegalArgumentException("Conversion unknown from: " +
                    fieldValue.getType().name() + " to " + objClsTypeCode + ".");
            }
            break;
        case MAP:
            Class<?> cls = ( prop == null ? Object.class : prop.getType());
            objValue = convertFieldValueToObj(cls, fieldValue, false,
                (TypeInformation<T>) prop.getTypeInformation());
            break;
        case EMPTY:
            throw new IllegalStateException("Invalid type: " +
                fieldValue.getType().name());
        default:
            throw new IllegalStateException(
                "Unexpected value: " + fieldValue.getType());
        }
        return objValue;
    }

    @SuppressWarnings("unchecked")
    private static <E> E[] listToArray(List<Object> list, Class<E> cls)
    {
        int s;
        if (list == null || (s = list.size()) < 1) {
            return null;
        }

        E[] temp = (E[]) Array.newInstance(cls, s);

        for (int i = 0; i < list.size(); i++) {
            Object item = list.get(i);
            if (item != null) {
                if (cls.isAssignableFrom(item.getClass())) {
                    Array.set(temp, i, item);
                } else {
                    throw new IllegalArgumentException(
                        "Cannot assign item of " +
                            "class " + item.getClass().getName() +
                            " to array of type: " + cls.getName());
                }
            }
        }
        return temp;
    }


    /**
     * Converts the ID id to a MapValue representing the primaryKey.
     */
    public <ID> MapValue convertIdToPrimaryKey(String idColumnName, ID id) {
        if (id == null) {
            return null;
        }

        MapValue row = new MapValue();
        if (NosqlEntityInformation.isCompositeKeyType(id.getClass())) {
            /*composite key. Here convertObjToFieldValue adds #class that is
              why convertObjToRow is used*/
            MapValue compositeKey = convertObjToRow(id, false);
            compositeKey.get(NosqlTemplateBase.JSON_COLUMN).asMap().
                    getMap().forEach(row::put);
        } else {
            row.put(idColumnName, convertObjToFieldValue(id, null, false));
        }
        return row;
    }

    /* Used when generating create table or bind variable types */
    public static String toNosqlSqlType(Object fromPropertyValue) {
        if (fromPropertyValue == null ||
            fromPropertyValue instanceof JsonNullValue ||
            fromPropertyValue instanceof NullValue) {
            throw new IllegalArgumentException("Param value can not be a null" +
                " value.");
        }

        if (fromPropertyValue instanceof String ||
                fromPropertyValue instanceof StringValue) {
            return "String";
        } else if (fromPropertyValue instanceof Integer ||
                fromPropertyValue instanceof IntegerValue) {
            return "Integer";
        } else if (fromPropertyValue instanceof Long ||
                fromPropertyValue instanceof LongValue) {
            return "Long";
        } else if (fromPropertyValue instanceof BigDecimal ||
                fromPropertyValue instanceof BigInteger ||
                fromPropertyValue instanceof NumberValue) {
            return "Number";
        } else if (fromPropertyValue instanceof Float ||
                fromPropertyValue instanceof Double ||
                fromPropertyValue instanceof DoubleValue) {
            return "Double";
        } else if (fromPropertyValue instanceof Boolean ||
                fromPropertyValue instanceof BooleanValue) {
            return "Boolean";
        } else if (fromPropertyValue instanceof byte[] ||
                fromPropertyValue instanceof BinaryValue) {
            return "Binary";
        } else if (
                // Timestamp extends Date
                //fromPropertyValue instanceof Timestamp ||
                fromPropertyValue instanceof Date ||
                fromPropertyValue instanceof Instant ||
                fromPropertyValue instanceof TimestampValue) {
            return "Timestamp";
        } else if (fromPropertyValue instanceof List<?>) {
            List<?> list = (List<?>) fromPropertyValue;
            if (list.size() > 0) {
                String nosqlType = toNosqlSqlType(list.get(0));

                for (int i = 1; i < list.size(); i++) {
                    if (!nosqlType.equals(toNosqlSqlType(list.get(i)))) {
                        log.debug("Not all entries in the array map to the " +
                            "same type. Will use ARRAY(ANY).");
                        return "ARRAY(ANY)";
                    }
                }

                return "ARRAY(" + nosqlType + ")";
            } else {
                return "ARRAY(ANY)";
            }
        } else if (fromPropertyValue instanceof Object[]) {
            Object[] arr = (Object[]) fromPropertyValue;
            if (arr != null || arr.length > 0) {
                String nosqlType = toNosqlSqlType(Array.get(arr, 0));

                for (int i = 1; i < arr.length; i++) {
                    if (!nosqlType.equals(toNosqlSqlType(Array.get(arr, i)))) {
                        log.debug("Not all entries in the array map to the " +
                            "same type. Will use ARRAY(ANY).");
                        return "ARRAY(ANY)";
                    }
                }

                return "ARRAY(" + nosqlType + ")";
            } else {
                return "ARRAY(ANY)";
            }
        } else if (fromPropertyValue instanceof Point ||
                   fromPropertyValue instanceof Polygon) {
            //todo maybe add support for our own MultiPoint and MultiPolygon
            return "JSON";
        } else if (fromPropertyValue instanceof ArrayValue) {
            return "ARRAY(ANY)";
        } else if (fromPropertyValue instanceof MapValue) {
            return "MAP(ANY)";
        } else {
            throw new IllegalArgumentException("Unsupported type: " +
                fromPropertyValue.getClass());
        }
    }

    public static RuntimeException convert(NoSQLException nse) {
        if (nse instanceof RequestTimeoutException) {
            return new QueryTimeoutException(nse.getMessage(), nse.getCause());
        } else if (nse instanceof RetryableException) {
            return new TransientDataAccessResourceException(nse.getMessage(),
                nse.getCause());
        } else if (nse instanceof InvalidAuthorizationException ||
                   nse instanceof UnauthorizedException) {
            return new PermissionDeniedDataAccessException(nse.getMessage(),
                nse.getCause());
        } else if (nse instanceof IndexExistsException ||
                    nse instanceof IndexNotFoundException ||
                    nse instanceof JsonParseException ||
                    nse instanceof OperationNotSupportedException ||
                    nse instanceof ResourceExistsException ||
                    nse instanceof TableExistsException ||
                    nse instanceof TableNotFoundException) {
            return new InvalidDataAccessApiUsageException(nse.getMessage(),
                nse.getCause());
        } else if (nse instanceof ResourceLimitException ||
                    nse instanceof TableSizeException) {
                return new InvalidDataAccessResourceUsageException(
                    nse.getMessage(), nse.getCause());
        } else {
            // if unknown log and pass it over
            log.debug("Unknown exception to convert: " + nse.getClass().getName());
            return nse;
        }
    }
}
