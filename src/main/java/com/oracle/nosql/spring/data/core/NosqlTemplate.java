/*-
 * Copyright (c) 2020, 2025 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */
package com.oracle.nosql.spring.data.core;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.oracle.nosql.spring.data.core.mapping.NosqlKey;
import com.oracle.nosql.spring.data.core.mapping.NosqlPersistentProperty;
import oracle.nosql.driver.NoSQLException;
import oracle.nosql.driver.NoSQLHandle;
import oracle.nosql.driver.ops.DeleteRequest;
import oracle.nosql.driver.ops.GetResult;
import oracle.nosql.driver.ops.PutResult;
import oracle.nosql.driver.ops.TableRequest;
import oracle.nosql.driver.ops.TableResult;
import oracle.nosql.driver.ops.WriteMultipleRequest;
import oracle.nosql.driver.values.FieldValue;
import oracle.nosql.driver.values.LongValue;
import oracle.nosql.driver.values.MapValue;

import com.oracle.nosql.spring.data.NosqlDbFactory;
import com.oracle.nosql.spring.data.config.AbstractNosqlConfiguration;
import com.oracle.nosql.spring.data.config.NosqlDbConfig;
import com.oracle.nosql.spring.data.core.convert.MappingNosqlConverter;
import com.oracle.nosql.spring.data.core.mapping.NosqlPersistentEntity;
import com.oracle.nosql.spring.data.core.query.NosqlQuery;
import com.oracle.nosql.spring.data.repository.support.NosqlEntityInformation;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

public class NosqlTemplate
    extends  NosqlTemplateBase
    implements NosqlOperations {

    private final SpelAwareProxyProjectionFactory projectionFactory;


    public static NosqlTemplate create(NosqlDbConfig nosqlDBConfig)
        throws ClassNotFoundException {
        Assert.notNull(nosqlDBConfig, "NosqlDbConfig should not be null.");
        return create(new NosqlDbFactory(nosqlDBConfig));
    }

    public static NosqlTemplate create(NosqlDbFactory nosqlDbFactory)
        throws ClassNotFoundException {
        Assert.notNull(nosqlDbFactory, "NosqlDbFactory should not be null.");
        AbstractNosqlConfiguration configuration =
            new AbstractNosqlConfiguration();
        return new NosqlTemplate(nosqlDbFactory,
            configuration.mappingNosqlConverter());
    }

    public NosqlTemplate(NosqlDbFactory nosqlDbFactory,
        MappingNosqlConverter mappingNosqlConverter) {

        super(nosqlDbFactory, mappingNosqlConverter);
        this.projectionFactory = new SpelAwareProxyProjectionFactory();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext)
        throws BeansException {
    }

    @Override
    public String getTableName(Class<?> domainClass) {
        Assert.notNull(domainClass, "domainClass should not be null");

        return getNosqlEntityInformation(domainClass).getTableName();
    }

    @Override
    public boolean createTableIfNotExists(
        NosqlEntityInformation<?, ?> entityInformation) {
        boolean isTableExist = doCheckExistingTable(entityInformation);
        // if table does not exist create
        return (!isTableExist) ? doCreateTable(entityInformation) : true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T insert(@NonNull T entity) {
        Assert.notNull(entity, "entity should not be null");

        return insert(getNosqlEntityInformation(
            (Class<T>) entity.getClass()),
            entity);
    }

    /**
     * If entity doesn't have autogen field objectToSave is wrote using put.
     * If entity has autogen field objectToSave should not have field set.
     */
    @Override
    public <T, ID> T insert(NosqlEntityInformation<T, ID> entityInformation,
        @NonNull T entity) {

        Assert.notNull(entityInformation, "Entity information " +
            "should not be null.");

        Assert.notNull(entity, "entity should not be null");

        final MapValue row = mappingNosqlConverter.convertObjToRow(
            entity, entityInformation.isAutoGeneratedId());

        PutResult putRes = doPut(entityInformation, row, false);

        FieldValue id;
        if (entityInformation.isAutoGeneratedId()) {
            id = putRes.getGeneratedValue();
            if (id == null) {
                throw new IllegalStateException("Expected generated value is " +
                    "null.");
            }
            // for the case when id is autogenerated, the generated value is in
            // the result
            // id is set to the same object and returned
            entity = populateIdIfNecessary(entity, id);
        }

        return entity;
    }

    private <T> T populateIdIfNecessary(T objectToSave, FieldValue id) {
        return mappingNosqlConverter.setId(objectToSave, id);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> void update(T entity) {
        Assert.notNull(entity, "entity should not be null");

        update(getNosqlEntityInformation((Class<T>) entity.getClass()),
            entity);
    }

    @Override
    public <T, ID> void update(NosqlEntityInformation<T, ID> entityInformation,
        T entity) {

        Assert.notNull(entityInformation, "Entity information " +
            "should not be null.");
        Assert.notNull(entity, "entity should not be null");

        LOG.debug("execute update in table {}", entityInformation.getTableName());
        final MapValue row = mappingNosqlConverter
            .convertObjToRow(entity, false);

        doUpdate(entityInformation, row);
    }

    @Override
    public void deleteAll(NosqlEntityInformation<?, ?> entityInformation) {

        Assert.notNull(entityInformation, "Entity information " +
            "should not be null.");

        String sql = String.format(TEMPLATE_DELETE_ALL,
            entityInformation.getTableName() );

        // Since this returns an Iterable the query isn't run until first
        // result is read. Must read at least one result.
        runQuery(entityInformation, sql).iterator().next();
//        log.debug("deleteAll(" + tableName + "): " + res);
    }

    @Override
    public <T, ID> void deleteAll(
        NosqlEntityInformation<T, ID> entityInformation,
        Iterable<? extends ID> ids) {

        Assert.notNull(entityInformation, "Entity information " +
            "should not be null.");
        Assert.notNull(ids, "ids should not be null");

        // Cannot use nosqlClient.multiDelete or writeMultiple because this
        // can't guarantee all ids are in the same shard. For this see
        // deleteInShard() method.
        //todo supporting limiting parallelism (1000+ fails) requires
        // external libraries
        StreamSupport.stream(ids.spliterator(), false)
            .forEach(id -> deleteById(entityInformation, id));
    }

    /**
     * Deletes ids from one shard. Note: All ids must be in the same shard
     * otherwise it's an error. It uses
     * {@link NoSQLHandle#writeMultiple(WriteMultipleRequest)}.
     */
    public <T, ID> void deleteInShard(String tableName, Class<T> entityClass,
        Iterable<? extends ID> ids)
    {
        Assert.hasText(tableName, "Table name should not be null, " +
            "empty or only whitespaces");
        Assert.notNull(ids, "ids should not be null");
        Assert.notNull(entityClass, "entityClass should not be null");

        NosqlEntityInformation<?, ?> entityInformation =
            getNosqlEntityInformation(entityClass);

        WriteMultipleRequest wmReq = new WriteMultipleRequest();
        if (entityInformation.getTimeout() > 0) {
            wmReq.setTimeout(entityInformation.getTimeout());
        }

        wmReq.setDurability(entityInformation.getDurability());

        String idColumnName = getIdColumnName(entityClass);

        StreamSupport.stream(ids.spliterator(), true)
            .map(id -> mappingNosqlConverter.convertIdToPrimaryKey(idColumnName, id))
            .map(pk -> new DeleteRequest().setKey(pk).setTableName(tableName))
            .forEach(dr -> wmReq.add(dr, false));

        try {
            nosqlClient.writeMultiple(wmReq);
        } catch (NoSQLException nse) {
            LOG.error("WriteMultiple: table: {}", wmReq.getTableName());
            LOG.error(nse.getMessage());
            throw MappingNosqlConverter.convert(nse);
        }
    }

    /**
     * Drops table and returns true if result indicates table state changed to
     * DROPPED or DROPPING.
     * Uses {@link NosqlDbFactory#getTableReqTimeout()} and
     * {@link NosqlDbFactory#getTableReqPollInterval()} to check the result.
     */
    @Override
    public boolean dropTableIfExists(String tableName) {
        Assert.hasText(tableName, "tableName should not be null, " +
            "empty or only whitespaces");

        String sql = String.format(TEMPLATE_DROP_TABLE, tableName );

        TableRequest tableReq = new TableRequest().setStatement(sql);

        TableResult tableRes = doTableRequest(null, tableReq);
        psCache.clear();

        return tableRes.getTableState() == TableResult.State.DROPPED ||
            tableRes.getTableState() == TableResult.State.DROPPING;
    }

    @Override
    public MappingNosqlConverter getConverter() {
        return mappingNosqlConverter;
    }

    public <T> NosqlEntityInformation<T, ?> getNosqlEntityInformation(
        Class<T> domainClass) {
        return new NosqlEntityInformation<>(applicationContext, domainClass);
    }

    @Override
    public <T, ID> T findById(ID id, Class<T> javaType) {
        return findById(getNosqlEntityInformation(javaType), id);
    }

    @Override
    public <T, ID> T findById(NosqlEntityInformation<T, ID> entityInformation,
        ID id) {

        Assert.notNull(entityInformation, "Entity information " +
            "should not be null.");
        Assert.notNull(id, "id should not be null");

        LOG.debug("execute findById in table {}",
            entityInformation.getTableName());

        final String idColumnName = mappingNosqlConverter
            .getIdProperty(entityInformation.getJavaType()).getName();

        final MapValue row = mappingNosqlConverter
            .convertIdToPrimaryKey(idColumnName, id);

        GetResult getRes = doGet(entityInformation, row);

        return mappingNosqlConverter.read(entityInformation.getJavaType(),
            getRes.getValue());
    }

    @Override
    public <T, ID> Iterable<T> findAllById(
        NosqlEntityInformation<T, ID> entityInformation, Iterable<ID> ids) {

        Assert.notNull(entityInformation, "Entity information " +
            "should not be null.");
        Assert.notNull(ids, "Id list should not be null");

        //todo usage of limited parallel streams (10000+ fails) requires
        // external libs
        return IterableUtil.getIterableFromStream(
            StreamSupport.stream(ids.spliterator(), false)
                .map(x -> findById(entityInformation, x)));
    }

    @Override
    public <T, ID> void deleteById(
        NosqlEntityInformation<T, ID> entityInformation,
        ID id) {

        Assert.notNull(entityInformation, "Entity information " +
            "should not be null.");        Assert.notNull(id, "id should not be null");

        LOG.debug("execute deleteById in table {}",
            entityInformation.getTableName());

        final String idColumnName = getIdColumnName(
            entityInformation.getJavaType());

        final MapValue row = mappingNosqlConverter.convertIdToPrimaryKey(
            idColumnName, id);

        doDelete(entityInformation, row);
    }

    private <T> String getIdColumnName(@NonNull Class<T> entityClass) {
        final NosqlPersistentEntity<?> entity =
            mappingNosqlConverter.getMappingContext().getPersistentEntity(entityClass);
        Assert.notNull(entity, "entity should not be null");
        Assert.notNull(entity.getIdProperty(), "entity.getIdProperty() should" +
            " not be null");
        return entity.getIdProperty().getName();
    }

    @Override
    public <T> Iterable<T> findAll(Class<T> entityClass) {
        Assert.notNull(entityClass, "entityClass should not be null");

        return findAll(getNosqlEntityInformation(entityClass));
    }

    @Override
    public <T> Iterable<T> findAll(
        NosqlEntityInformation<T, ?> entityInformation) {
        Assert.notNull(entityInformation, "Entity information " +
            "should not be null.");

        String sql = String.format(TEMPLATE_SELECT_ALL,
            entityInformation.getTableName());

        Iterable<MapValue> items = runQuery(entityInformation, sql);
        Stream<T> result = IterableUtil.getStreamFromIterable(items)
                .map(d -> getConverter()
                    .read(entityInformation.getJavaType(), d));
        return IterableUtil.getIterableFromStream(result);
    }

    @Override
    public long count(NosqlEntityInformation<?, ?> entityInformation) {

        Assert.notNull(entityInformation, "Entity information " +
            "should not be null.");

        String sql = String.format(TEMPLATE_COUNT,
            entityInformation.getTableName());

        Iterable<MapValue> res = runQuery(entityInformation, sql);

        Assert.isTrue(res != null,
            "Result of a count query should not be null and should have a non" +
                " null iterator.");
        Iterator<MapValue> iterator = res.iterator();
        Assert.isTrue(iterator.hasNext(),
            "Result of count query iterator should have 1 result.");
        Collection<FieldValue> values = iterator.next().values();
//        Assert.isTrue(values.size() == 1, "Results of a count query " +
//            "collection should have 1 result.");
        FieldValue countField = values.iterator().next();
        Assert.isTrue(countField != null && countField.getType() ==
            FieldValue.Type.LONG,
            "Result of a count query should be of type LONG.");
        return countField.asLong().getValue();
    }

    @Override
    public <T> Iterable<T> findAll(
        NosqlEntityInformation<T, ?> entityInformation,
        Sort sort) {

        Assert.notNull(entityInformation, "Entity information " +
            "should not be null.");
        String sql = String.format(TEMPLATE_SELECT_ALL,
            entityInformation.getTableName());

        sql += " " + orderBySql(entityInformation, sort);
//        log.debug("findAll(" + tableName + ", " + sort + "): SQL: " + sql);

        Iterable<MapValue> items = runQuery(entityInformation, sql);

        return IterableUtil.getIterableFromStream(
            IterableUtil.getStreamFromIterable(items)
            .map(d -> getConverter().read(entityInformation.getJavaType(), d)));
    }

    @Override
    public <T> Page<T> findAll(NosqlEntityInformation<T, ?> entityInformation,
        Pageable pageable) {

        Assert.notNull(entityInformation, "Entity information " +
            "should not be null.");

        Map<String, FieldValue> params = new HashMap<>();

        String sql = limitOffsetSql(entityInformation, pageable, params);
//        log.debug("findAll(" + tableName + ", " + pageable + "): SQL: " + sql);

        Iterable<MapValue> items = runQueryNosqlParams(entityInformation, sql,
            params);

        List<T> result = IterableUtil.getStreamFromIterable(items)
            .map(d -> getConverter().read(entityInformation.getJavaType(), d))
            .collect(Collectors.toList());

        return new PageImpl<>(result, pageable, count(entityInformation));
    }

    private <T> String limitOffsetSql(
        NosqlEntityInformation<T, ?> entityInformation, Pageable pageable,
        @NonNull Map<String, FieldValue> params) {

        String sql = String.format(TEMPLATE_SELECT_ALL,
            entityInformation.getTableName());

        if (pageable == null || pageable.isUnpaged()) {
            return sql;
        }

        sql = sql + orderBySql(entityInformation, pageable.getSort());

        sql += " LIMIT $kv_limit_ OFFSET $kv_offset_";

        params.put("$kv_limit_", new LongValue(pageable.getPageSize()));
        params.put("$kv_offset_", new LongValue(pageable.getOffset()));

        sql = "DECLARE $kv_limit_ LONG; $kv_offset_ LONG; " + sql;

        return sql;
    }

    private <T> String orderBySql(
        NosqlEntityInformation<T, ?> entityInformation, Sort sort) {

        if (sort == null || sort.isUnsorted()) {
            return "";
        }

        String sql = sort.stream()
            .map(f -> ( "t." +
                convertProperty(entityInformation, f.getProperty()) + " " +
                (f.isAscending() ? "ASC" : "DESC")))
            .collect(Collectors.joining(",", "ORDER BY ", ""));
        return " " + sql;
    }

    private <T> String convertProperty(
        NosqlEntityInformation<T, ?> entityInformation,
        @NonNull String property) {

        Field field = FieldUtils.getField(entityInformation.getJavaType(),
            property);

        // if field == null can mean it's not accessible so it can still be a
        // valid non-id field
        if (field != null && field.equals(entityInformation.getIdField())) {
            return property;
        }

        //field can be composite key
        NosqlPersistentProperty pp = mappingNosqlConverter.getMappingContext().
                getPersistentPropertyPath(property,
                        entityInformation.getJavaType()).getLeafProperty();

        NosqlPersistentProperty parentPp =
                mappingNosqlConverter.getMappingContext().
                        getPersistentPropertyPath(property,
                                entityInformation.getJavaType()).getBaseProperty();
        if (pp != null) {
            if (pp.isAnnotationPresent(NosqlKey.class)) {
                return pp.getName();
            }
            if (parentPp != null && parentPp.isIdProperty()) {
                return pp.getName();
            }
        }

        return JSON_COLUMN + "." + property;
    }

    public void runTableRequest(String statement) {
        TableRequest tableRequest = new TableRequest();
        tableRequest.setStatement(statement);

        doTableRequest(null, tableRequest);
    }

    public Iterable<MapValue> runQuery(
        NosqlEntityInformation<?, ?> entityInformation,
        String query) {
        return runQueryNosqlParams(entityInformation, query, null);
    }

    /**
     *  javaParams is a Map of param_name to Java objects
     */
    public Iterable<MapValue> runQueryJavaParams(
        NosqlEntityInformation<?, ?> entityInformation,
        String query,
        Map<String, Object> javaParams) {
        Map<String, FieldValue> nosqlParams = null;

        if (javaParams != null) {
            nosqlParams = new HashMap<>(javaParams.size());

            for (Map.Entry<String, Object> e : javaParams.entrySet()) {
                FieldValue fieldValue =
                    mappingNosqlConverter.convertObjToFieldValue(e.getValue(),
                        null, false);
                nosqlParams.put(e.getKey(), fieldValue);
            }
        }

        return runQueryNosqlParams(entityInformation, query, nosqlParams);
    }

    /**
     * nosqlParams is a Map of param_name to FieldValue
     */
    public Iterable<MapValue> runQueryNosqlParams(
        NosqlEntityInformation<?, ?> entityInformation,
        String query,
        Map<String, FieldValue> nosqlParams) {

        return doRunQueryNosqlParams(entityInformation, query, nosqlParams);
    }

    /* Query execution for dynamic queries */
    @Override
    public <T> Iterable<MapValue> count(
        NosqlEntityInformation<T, ?> entityInformation, NosqlQuery query) {

        return doExecuteMapValueQuery(query, entityInformation);
    }

    @Override
    public <T, ID> Iterable<T> delete(
        NosqlEntityInformation<T, ID> entityInformation, NosqlQuery query) {

        return IterableUtil.getIterableFromStream(
            IterableUtil.getStreamFromIterable(
                find(entityInformation, entityInformation.getJavaType(), query))
            .map(e -> {
                deleteById(entityInformation, entityInformation.getId(e));
                return e;
            }));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <S, T> Iterable<T> find(
        NosqlEntityInformation<S, ?> entityInformation,
        Class<T> targetType,
        NosqlQuery query) {

        Class<?> entityType = entityInformation.getJavaType();
        Class<?> typeToRead = targetType.isInterface() ||
            targetType.isAssignableFrom(entityType)
            ? entityType
            : targetType;

        Iterable<MapValue> results = doExecuteMapValueQuery(query,
            entityInformation);

        Stream<T> resStream = IterableUtil.getStreamFromIterable(results)
            .map(d -> {
                Object source = getConverter().read(typeToRead, d);
                return targetType.isInterface()
                    ? projectionFactory.createProjection(targetType, source)
                    : (T) source;
            });

        return IterableUtil.getIterableFromStream(resStream);
    }

    public NoSQLHandle getNosqlClient() {
        return nosqlClient;
    }
}
