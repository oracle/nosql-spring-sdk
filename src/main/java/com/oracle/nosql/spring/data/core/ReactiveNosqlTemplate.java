/*-
 * Copyright (c) 2020, 2023 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */
package com.oracle.nosql.spring.data.core;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import oracle.nosql.driver.ops.GetResult;
import oracle.nosql.driver.ops.PutResult;
import oracle.nosql.driver.ops.TableRequest;
import oracle.nosql.driver.ops.TableResult;
import oracle.nosql.driver.values.FieldValue;
import oracle.nosql.driver.values.MapValue;

import com.oracle.nosql.spring.data.NosqlDbFactory;
import com.oracle.nosql.spring.data.config.AbstractNosqlConfiguration;
import com.oracle.nosql.spring.data.config.NosqlDbConfig;
import com.oracle.nosql.spring.data.core.convert.MappingNosqlConverter;
import com.oracle.nosql.spring.data.core.query.NosqlQuery;
import com.oracle.nosql.spring.data.repository.support.NosqlEntityInformation;

import org.reactivestreams.Publisher;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.Assert;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class ReactiveNosqlTemplate
    extends  NosqlTemplateBase
    implements ReactiveNosqlOperations, ApplicationContextAware {

    public static ReactiveNosqlTemplate create(NosqlDbConfig nosqlDBConfig)
        throws ClassNotFoundException {
        Assert.notNull(nosqlDBConfig, "NosqlDbConfig should not be null.");
        return create(new NosqlDbFactory(nosqlDBConfig));
    }

    public static ReactiveNosqlTemplate create(NosqlDbFactory nosqlDbFactory)
        throws ClassNotFoundException {
        Assert.notNull(nosqlDbFactory, "NosqlDbFactory should not be null.");
        AbstractNosqlConfiguration configuration =
            new AbstractNosqlConfiguration() {
            };
        return new ReactiveNosqlTemplate(nosqlDbFactory,
            configuration.mappingNosqlConverter());
    }

    public ReactiveNosqlTemplate(NosqlDbFactory nosqlDbFactory,
        MappingNosqlConverter mappingNosqlConverter) {

        super(nosqlDbFactory, mappingNosqlConverter);
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

    /**
     * Creates a table for the given entity type if it doesn't exist.
     */
    @Override
    public Mono<Boolean> createTableIfNotExists(
        NosqlEntityInformation<?, ?> entityInformation) {
        Assert.notNull(entityInformation, "Entity information should not be null");

        String ddl = getCreateTableDDL(entityInformation);
        try {
            doCheckExistingTable(entityInformation);
        } catch (IllegalArgumentException iae) {
            String msg = String.format("Error executing DDL '%s': Table %s " +
                            "exists but definitions do not match : %s" , ddl,
                    entityInformation.getTableName(), iae.getMessage());
            throw new IllegalArgumentException(msg, iae);
        }
        return Mono.just(doCreateTable(entityInformation, ddl));
    }

    /**
     * Drops table and returns true if result indicates table state changed to
     * DROPPED or DROPPING.
     * Uses {@link NosqlDbFactory#getTableReqTimeout()} and
     * {@link NosqlDbFactory#getTableReqPollInterval()} to check the result.
     */
    @Override
    public Mono<Boolean> dropTableIfExists(String tableName) {
        Assert.hasText(tableName, "Table name should not be null, " +
            "empty or only whitespaces");

        String sql = String.format(NosqlTemplateBase.TEMPLATE_DROP_TABLE,
            tableName );

        TableRequest tableReq = new TableRequest().setStatement(sql);

        TableResult tableRes = doTableRequest(null, tableReq);

        return Mono.just(tableRes.getTableState() == TableResult.State.DROPPED ||
            tableRes.getTableState() == TableResult.State.DROPPING);
    }

    @Override
    public <T, ID> Flux<T> findAll(
        NosqlEntityInformation<T, ID> entityInformation) {
        Assert.notNull(entityInformation, "EntityInformation should " +
            "not be null");
        String tableName = entityInformation.getTableName();
        Assert.hasText(tableName, "Table name should not be null, " +
            "empty or only whitespaces");

        String sql = String.format(NosqlTemplateBase.TEMPLATE_SELECT_ALL,
            tableName);

        return Flux
            .fromIterable(runQuery(sql, entityInformation))
            .map(mv -> getConverter().read(entityInformation.getJavaType(), mv));
    }

    @Override
    public <T> Flux<T> findAll(Class<T> entityClass) {
        return findAll(getNosqlEntityInformation(entityClass));
    }

    @Override
    public <T> Mono<T> findById(Object id, Class<T> entityClass) {
        return findById(getNosqlEntityInformation(entityClass), id);
    }

    @Override
    public <T, ID> Mono<T> findById(
        NosqlEntityInformation<T, ID> entityInformation,
        ID id) {
        Assert.notNull(entityInformation, "EntityInformation should " +
            "not be null");
        String tableName = entityInformation.getTableName();
        Assert.hasText(tableName, "tableName should not be null, " +
            "empty or only whitespaces");
        Assert.notNull(id, "id should not be null");

        LOG.debug("execute findById in table {}", tableName);

        Class<T> entityClass = entityInformation.getJavaType();
        final String idColumnName = mappingNosqlConverter
            .getIdProperty(entityClass).getName();

        final MapValue row = mappingNosqlConverter
            .convertIdToPrimaryKey(idColumnName, id);

        GetResult getRes = doGet(entityInformation, row);

        T res = mappingNosqlConverter.read(entityClass, getRes.getValue());
        return Mono.justOrEmpty(res);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Mono<T> insert(T entity) {
        Assert.notNull(entity, "entity should not be null");

        return insert(getNosqlEntityInformation(
            (Class<T>) entity.getClass()),
            entity);
    }

    @Override
    public <T, ID> Mono<T> insert(
        NosqlEntityInformation<?, ID> entityInformation,
        T entity) {
        Assert.notNull(entityInformation, "EntityInformation should " +
            "not be null");
        String tableName = entityInformation.getTableName();
        Assert.hasText(tableName, "tableName should not be null, " +
            "empty or only whitespaces");
        Assert.notNull(entity, "entity should not be null");

        LOG.debug("execute insert in table {}", tableName);

        final MapValue row = mappingNosqlConverter.convertObjToRow(
            entity, entityInformation.isAutoGeneratedId());

        PutResult putRes = doPut(entityInformation, row, false);

        FieldValue id;
        if (entityInformation.isAutoGeneratedId()) {
            id = putRes.getGeneratedValue();
            // for the case when id is autogenerated, the generated value is in
            // the result, id is set to the same object and returned
            entity = mappingNosqlConverter.setId(entity, id);
        }

        return Mono.just(entity);
    }

    private <T, ID> NosqlEntityInformation<T, ID> getNosqlEntityInformation(
        Class<T> domainClass) {
        return new NosqlEntityInformation<>(applicationContext, domainClass);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Mono<T> update(T entity) {
        Assert.notNull(entity, "entity should not be null");

        return update(getNosqlEntityInformation(
            (Class<T>) entity.getClass()),
            entity);
    }

    @Override
    public <T, ID> Mono<T> update(
        NosqlEntityInformation<?, ID> entityInformation,
        T entity) {
        Assert.notNull(entityInformation, "EntityInformation should " +
            "not be null");
        String tableName = entityInformation.getTableName();
        Assert.hasText(tableName, "Table name should not be null, " +
            "empty or only whitespaces");
        Assert.notNull(entity, "entity should not be null");

        LOG.debug("execute update in table {}", tableName);
        final MapValue row = mappingNosqlConverter
            .convertObjToRow(entity, false);

        doUpdate(entityInformation, row);
        return Mono.just(entity);
    }

    @Override
    public <ID> Mono<Void> deleteById(
        NosqlEntityInformation<?, ID> entityInformation, ID id) {
        Assert.notNull(entityInformation, "EntityInformation should " +
            "not be null");
        String tableName = entityInformation.getTableName();
        Assert.hasText(tableName, "tableName should not be null, " +
            "empty or only whitespaces");
        Assert.notNull(id, "id should not be null");

        LOG.debug("execute deleteById in table {}", tableName);

        final String idColumnName = mappingNosqlConverter
            .getIdProperty(entityInformation.getJavaType()).getName();

        final MapValue row = mappingNosqlConverter
            .convertIdToPrimaryKey(idColumnName, id);

        doDelete(entityInformation, row);

        return Mono.empty();
    }

    @Override
    public Mono<Void> deleteAll(NosqlEntityInformation<?, ?> entityInformation) {
        Assert.notNull(entityInformation, "EntityInformation should " +
            "not be null");

        String sql = String.format(NosqlTemplateBase.TEMPLATE_DELETE_ALL,
            entityInformation.getTableName());

        // Since this returns an Iterable the query isn't run until first
        // result is read. Must read at least one result.
        runQuery(sql, entityInformation).iterator().next();
        return Mono.empty();
    }

    @Override
    public <T, ID> Flux<T> delete(NosqlQuery query,
        NosqlEntityInformation<T, ID> entityInformation) {

        Assert.notNull(entityInformation, "EntityInformation should " +
            "not be null");

        return executeQuery(query, entityInformation)
            .map(e -> {
                deleteById(entityInformation, entityInformation.getId(e));
                return e;
            });
    }

    @Override
    public <T> Flux<T> find(NosqlQuery query,
        NosqlEntityInformation<T, ?> entityInformation) {
        return executeQuery(query, entityInformation);
    }

    @Override
    public Mono<Boolean> exists(NosqlQuery query,
        NosqlEntityInformation<?, ?> entityInformation) {

        Assert.notNull(entityInformation, "EntityInformation should " +
            "not be null");

        return executeQuery(query, entityInformation).hasElements();
    }

    @Override
    public <ID> Mono<Boolean> existsById(
        NosqlEntityInformation<?, ID> entityInformation,
        ID id) {
        Assert.notNull(entityInformation, "EntityInformation should " +
            "not be null");
        String tableName = entityInformation.getTableName();

        Assert.hasText(tableName, "tableName should not be null, " +
            "empty or only whitespaces");
        Assert.notNull(id, "id should not be null");

        LOG.debug("execute existsById in table {}", tableName);

        final String idColumnName = mappingNosqlConverter
            .getIdProperty(entityInformation.getJavaType()).getName();

        final MapValue row = mappingNosqlConverter
            .convertIdToPrimaryKey(idColumnName, id);

        GetResult getRes = doGet(entityInformation, row);

        return Mono.just(getRes.getValue() != null);
    }

    @Override
    public Mono<Long> count(NosqlEntityInformation<?, ?> entityInformation) {
        Assert.notNull(entityInformation, "EntityInformation should " +
            "not be null");
        String tableName = entityInformation.getTableName();
        Assert.hasText(tableName, "Table name should not be null, empty or " +
            "only whitespaces");

        String sql = String.format(NosqlTemplateBase.TEMPLATE_COUNT, tableName);
        LOG.debug("count(" + tableName + "): SQL: " + sql);

        Iterable<MapValue> res = runQuery(sql, entityInformation);

        Assert.isTrue(res != null && res.iterator() != null,
            "Result of a count query should not be null and should have a non" +
                " null iterator.");
        Iterator<MapValue> iterator = res.iterator();
        Assert.isTrue(iterator.hasNext(),
            "Result of count query iterator should have 1 result.");
        Collection<FieldValue> values = iterator.next().values();
        Assert.isTrue(values.size() == 1, "Results of a count query " +
            "collection should have 1 result.");
        FieldValue countField = values.iterator().next();
        Assert.isTrue(countField != null && countField.getType() ==
                FieldValue.Type.LONG,
            "Result of a count query should be of type LONG.");
        return Mono.just(countField.asLong().getValue());
    }

    @Override
    public Mono<Long> count(NosqlQuery query,
        NosqlEntityInformation<?, ?> entityInformation) {
        Assert.notNull(entityInformation, "EntityInformation should " +
            "not be null");
        String tableName = entityInformation.getTableName();
        Assert.hasText(tableName, "Table name should not be null, " +
            "empty or only whitespaces");
        Assert.isTrue(query.isCount(), "Query must be a count projection " +
            "query.");

        return executeMapValueQuery(query, entityInformation)
            .elementAt(0)
            .map(mv -> {
                Assert.isTrue(mv != null && mv.values() != null,
                    "Count query must return at least one value.");
                FieldValue countField = mv.values().iterator().next();
                Assert.notNull(countField,
                    "Results of a count query " +
                        "collection should have 1 result.");
                Assert.isTrue(countField.getType() == FieldValue.Type.LONG,
                    "Result of a count query should be of type LONG.");
                return countField.asLong().getValue();
            });
    }

    @Override
    public MappingNosqlConverter getConverter() {
        return mappingNosqlConverter;
    }

    @Override
    public <T, ID> Flux<T> findAllById(
        NosqlEntityInformation<T, ID> entityInformation,
        Publisher<ID> idStream) {

        return Flux.from(idStream).flatMap(
            id -> findById(entityInformation, id));
    }

    private Iterable<MapValue> runQuery(String query,
        NosqlEntityInformation<?, ?> entityInformation) {

        return runQueryNosqlParams(entityInformation, query, null);
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

    /* Query execution */
    public <T> Flux<T> executeQuery(NosqlQuery query,
        NosqlEntityInformation<T, ?> entityInformation) {
        Assert.notNull(entityInformation, "EntityInformation should " +
            "not be null");
        Class<T> entityClass = entityInformation.getJavaType();

        return executeMapValueQuery(query, entityInformation)
            .map(d -> getConverter().read(entityClass, d));
    }

    public <T> Flux<MapValue> executeMapValueQuery(NosqlQuery query,
        NosqlEntityInformation<T, ?> entityInformation) {
        Iterable<MapValue> results = doExecuteMapValueQuery(query,
            entityInformation);

        return Flux.fromIterable(results);
    }
}
