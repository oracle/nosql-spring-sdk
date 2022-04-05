/*-
 * Copyright (c) 2020, 2022 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */
package com.oracle.nosql.spring.data.core;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import oracle.nosql.driver.NoSQLException;
import oracle.nosql.driver.NoSQLHandle;
import oracle.nosql.driver.TableNotFoundException;
import oracle.nosql.driver.ops.DeleteRequest;
import oracle.nosql.driver.ops.DeleteResult;
import oracle.nosql.driver.ops.GetRequest;
import oracle.nosql.driver.ops.GetResult;
import oracle.nosql.driver.ops.PrepareRequest;
import oracle.nosql.driver.ops.PrepareResult;
import oracle.nosql.driver.ops.PreparedStatement;
import oracle.nosql.driver.ops.PutRequest;
import oracle.nosql.driver.ops.PutResult;
import oracle.nosql.driver.ops.QueryRequest;
import oracle.nosql.driver.ops.TableRequest;
import oracle.nosql.driver.ops.TableResult;
import oracle.nosql.driver.util.LruCache;
import oracle.nosql.driver.values.FieldValue;
import oracle.nosql.driver.values.MapValue;

import com.oracle.nosql.spring.data.NosqlDbFactory;
import com.oracle.nosql.spring.data.config.AbstractNosqlConfiguration;
import com.oracle.nosql.spring.data.config.NosqlDbConfig;
import com.oracle.nosql.spring.data.core.convert.MappingNosqlConverter;
import com.oracle.nosql.spring.data.core.query.NosqlQuery;
import com.oracle.nosql.spring.data.repository.support.NosqlEntityInformation;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.Assert;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class ReactiveNosqlTemplate
    implements ReactiveNosqlOperations, ApplicationContextAware {

    private static final Logger log =
        LoggerFactory.getLogger(ReactiveNosqlTemplate.class);

    private final NosqlDbFactory nosqlDbFactory;
    private final NoSQLHandle nosqlClient;
    private final MappingNosqlConverter mappingNosqlConverter;
    private LruCache<String, PreparedStatement> psCache;

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
        Assert.notNull(nosqlDbFactory, "NosqlDbFactory should not be null.");
        this.nosqlDbFactory = nosqlDbFactory;
        nosqlClient = nosqlDbFactory.getNosqlClient();
        this.mappingNosqlConverter = mappingNosqlConverter;
        psCache = new LruCache<>(nosqlDbFactory.getQueryCacheCapacity(),
            nosqlDbFactory.getQueryCacheLifetime());
    }
    
    @Override
    public void setApplicationContext(ApplicationContext applicationContext)
        throws BeansException {
    }

    @Override
    public String getTableName(Class<?> domainClass) {
        Assert.notNull(domainClass, "domainClass should not be null");

        return new NosqlEntityInformation<>(domainClass).getTableName();
    }

    /**
     * Creates a table for the given entity type if it doesn't exist.
     */
    @Override
    public Mono<Boolean> createTableIfNotExists(
        NosqlEntityInformation<?, ?> information) {
        Assert.notNull(information, "Entity information should not be null");

        return Mono.just(createTable(information));
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

        String sql = String.format(NosqlTemplate.TEMPLATE_DROP_TABLE,
            tableName );

        TableRequest tableReq = new TableRequest().setStatement(sql);

        TableResult tableRes = doTableRequest(null, tableReq);

        return Mono.just(tableRes.getTableState() == TableResult.State.DROPPED ||
            tableRes.getTableState() == TableResult.State.DROPPING);
    }

    @Override
    public <T, ID> Flux<T> findAll(
        NosqlEntityInformation<T, ID> entityInformation)
    {
        Assert.notNull(entityInformation, "EntityInformation should " +
            "not be null");
        String tableName = entityInformation.getTableName();
        Assert.hasText(tableName, "Table name should not be null, " +
            "empty or only whitespaces");

        String sql = String.format(NosqlTemplate.TEMPLATE_SELECT_ALL,
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
        ID id)
    {
        Assert.notNull(entityInformation, "EntityInformation should " +
            "not be null");
        String tableName = entityInformation.getTableName();
        Assert.hasText(tableName, "tableName should not be null, " +
            "empty or only whitespaces");
        Assert.notNull(id, "id should not be null");

        log.debug("execute findById in table {}", tableName);

        Class<T> entityClass = entityInformation.getJavaType();
        final String idColumName = mappingNosqlConverter
            .getIdProperty(entityClass).getName();

        final MapValue row = mappingNosqlConverter
            .convertIdToPrimaryKey(idColumName, id);

        GetResult getRes = doGet(entityInformation, row);

        T res = mappingNosqlConverter.read(entityClass, getRes.getValue());
        return Mono.justOrEmpty(res);
    }

    private GetResult doGet(NosqlEntityInformation<?, ?> entityInformation,
        MapValue primaryKey) {

        GetRequest getReq = new GetRequest()
            .setTableName(entityInformation.getTableName())
            .setKey(primaryKey);

        if (entityInformation.getTimeout() > 0) {
            getReq.setTimeout(entityInformation.getTimeout());
        }

        getReq.setConsistency(entityInformation.getConsistency());

        GetResult getRes;

        try {
            getRes = nosqlClient.get(getReq);
        } catch (NoSQLException nse) {
            log.error("Get: table: {} key: {}", getReq.getTableName(),
                primaryKey);
            log.error(nse.getMessage());
            throw MappingNosqlConverter.convert(nse);
        }

        assert getRes != null;
        return getRes;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Mono<T> insert(T objectToSave) {
        Assert.notNull(objectToSave, "entityClass should not be null");

        return insert(getNosqlEntityInformation(
            (Class<T>) (objectToSave.getClass())),
            objectToSave);
    }

    @Override
    public <S, ID> Mono<S> insert(
        NosqlEntityInformation<?, ID> entityInformation,
        S objectToSave)
    {
        Assert.notNull(entityInformation, "EntityInformation should " +
            "not be null");
        String tableName = entityInformation.getTableName();
        Assert.hasText(tableName, "tableName should not be null, " +
            "empty or only whitespaces");
        Assert.notNull(objectToSave, "objectToSave should not be null");

        log.debug("execute insert in table {}", tableName);

        final MapValue row = mappingNosqlConverter.convertObjToRow(
            objectToSave, entityInformation.isAutoGeneratedId());

        PutResult putRes = doPut(row, entityInformation, false);

        FieldValue id;
        if (entityInformation.isAutoGeneratedId()) {
            id = putRes.getGeneratedValue();
            // for the case when id is autogenerated, the generated value is in
            // the result, id is set to the same object and returned
            objectToSave = mappingNosqlConverter.setId(objectToSave, id);
        }

        return Mono.just(objectToSave);
    }

    private <T, ID> NosqlEntityInformation<T, ID> getNosqlEntityInformation(
        Class<T> domainClass) {
        return new NosqlEntityInformation<>(domainClass);
    }

    private PutResult doPut(MapValue row,
        NosqlEntityInformation<?, ?> entityInformation, boolean ifPresent)
    {
        PutRequest putReq = new PutRequest()
            .setTableName(entityInformation.getTableName())
            .setValue(row);

        if (ifPresent) {
            putReq.setOption(PutRequest.Option.IfPresent);
        }

        if (entityInformation.isAutoGeneratedId()) {
            putReq.setReturnRow(true);
        }

        if (entityInformation.getTimeout() > 0) {
            putReq.setTimeout(entityInformation.getTimeout());
        }

        //todo add durability when putReq API adds support

        PutResult putRes;
        try {
            try {
                putRes = nosqlClient.put(putReq);
            } catch (TableNotFoundException tnfe) {
                if (entityInformation.isAutoCreateTable()) {
                    createTable(entityInformation);
                    putRes = nosqlClient.put(putReq);
                } else {
                    throw tnfe;
                }
            }
        } catch (NoSQLException nse) {
            log.error("Put: table: {} key: {}", putReq.getTableName(),
                row.get(entityInformation.getIdColumnName()));
            log.error(nse.getMessage());
            throw MappingNosqlConverter.convert(nse);
        }

        assert putRes != null;
        return putRes;
    }

    private boolean createTable(NosqlEntityInformation<?, ?> entityInformation)
    {
        String idColName = entityInformation.getIdField().getName();

        String idColType = entityInformation.getIdNosqlType().toString();
        if (entityInformation.getIdNosqlType() == FieldValue.Type.TIMESTAMP) {
            // For example: CREATE TABLE IF NOT EXISTS SensorIdTimestamp
            //     (time TIMESTAMP(3) , kv_json_ JSON, PRIMARY KEY( time ))
            idColType += "(" + nosqlDbFactory.getTimestampPrecision() + ")";
        }

        String tableName = entityInformation.getTableName();

        String sql = String.format(NosqlTemplate.TEMPLATE_CREATE_TABLE,
            tableName,
            idColName, idColType,
            (entityInformation.isAutoGeneratedId() ?
                NosqlTemplate.TEMPLATE_GENERATED_ALWAYS : ""), idColName);

        TableRequest tableReq = new TableRequest().setStatement(sql)
            .setTableLimits(entityInformation.getTableLimits());

        TableResult tableRes = doTableRequest(entityInformation, tableReq);

        TableResult.State tableState = tableRes.getTableState();
//        if (tableState != TableResult.State.ACTIVE) {
//            throw new RuntimeException("Table '" + tableName + "' not ACTIVE " +
//                "after waitingForCompletion. Current state: " +
//                tableState.name());
//        }
        return tableState == TableResult.State.ACTIVE;
    }

    private TableResult doTableRequest(NosqlEntityInformation<?, ?> entityInformation,
        TableRequest tableReq) {

        if (entityInformation != null &&
            entityInformation.getTimeout() > 0) {
            tableReq.setTimeout(entityInformation.getTimeout());
        }

        TableResult tableRes;
        try {
            log.debug("DDL: {}", tableReq.getStatement());
            tableRes = nosqlClient.doTableRequest(tableReq,
                nosqlDbFactory.getTableReqTimeout(),
                nosqlDbFactory.getTableReqPollInterval());
        } catch (NoSQLException nse) {
            log.error("DDL: {}", tableReq.getStatement());
            log.error(nse.getMessage());
            throw MappingNosqlConverter.convert(nse);
        }
        return tableRes;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Mono<T> update(T object) {
        Assert.notNull(object, "entity should not be null");

        return update(getNosqlEntityInformation(
            (Class<T>) (object.getClass())),
            object);
    }

    @Override
    public <S, ID> Mono<S> update(
        NosqlEntityInformation<?, ID> entityInformation,
        S object)
    {
        Assert.notNull(entityInformation, "EntityInformation should " +
            "not be null");
        String tableName = entityInformation.getTableName();
        Assert.hasText(tableName, "Table name should not be null, " +
            "empty or only whitespaces");
        Assert.notNull(object, "object should not be null");

        log.debug("execute update in table {}", tableName);
        final MapValue row = mappingNosqlConverter
            .convertObjToRow(object, false);

        doUpdate(tableName, row, entityInformation);
        return Mono.just(object);
    }

    private <T, ID> void doUpdate(String tableName, MapValue row,
        NosqlEntityInformation<T, ID> entityInformation)
    {
        // When id is autogenerated, it's required to do a SQL update query
        if (entityInformation.isAutoGeneratedId()) {
            final String idColumnName = entityInformation.getIdColumnName();
            String sql = String.format(NosqlTemplate.TEMPLATE_UPDATE,
                entityInformation.getIdNosqlType().name(), tableName,
                idColumnName);

            Map<String, FieldValue> params = new HashMap<>();
            //todo implement composite keys
            params.put("$id", row.get(idColumnName));
            params.put("$json", row.get(NosqlTemplate.JSON_COLUMN));

            // Must read at least one result to execute query!!!
            runQueryNosqlParams(entityInformation, sql, params).iterator().next();
        } else {
            // otherwise do a regular put, which is faster, use less resources
            doPut(row, entityInformation, true);
        }
    }


    @Override
    public <ID> Mono<Void> deleteById(
        NosqlEntityInformation<?, ID> entityInformation, ID id)
    {
        Assert.notNull(entityInformation, "EntityInformation should " +
            "not be null");
        String tableName = entityInformation.getTableName();
        Assert.hasText(tableName, "tableName should not be null, " +
            "empty or only whitespaces");
        Assert.notNull(id, "id should not be null");

        log.debug("execute deleteById in table {}", tableName);

        final String idColumName = mappingNosqlConverter
            .getIdProperty(entityInformation.getJavaType()).getName();

        final MapValue row = mappingNosqlConverter
            .convertIdToPrimaryKey(idColumName, id);

        doDelete(entityInformation, row);

        return Mono.empty();
    }

    private DeleteResult doDelete(NosqlEntityInformation<?, ?> entityInformation,
        MapValue primaryKey) {
        DeleteRequest delReq = new DeleteRequest()
            .setTableName(entityInformation.getTableName())
            .setKey(primaryKey);

        if (entityInformation.getTimeout() > 0) {
            delReq.setTimeout(entityInformation.getTimeout());
        }

        //todo add durability when API adds support

        DeleteResult delRes;

        try {
            delRes = nosqlClient.delete(delReq);
        } catch (NoSQLException nse) {
            log.error("Delete: table: {} key: {}", delReq.getTableName(),
                primaryKey);
            log.error(nse.getMessage());
            throw MappingNosqlConverter.convert(nse);
        }

        assert delRes != null;
        return delRes;
    }

    @Override
    public Mono<Void> deleteAll(NosqlEntityInformation<?, ?> entityInformation) {
        Assert.notNull(entityInformation, "EntityInformation should " +
            "not be null");

        String sql = String.format(NosqlTemplate.TEMPLATE_DELETE_ALL,
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
        ID id)
    {
        Assert.notNull(entityInformation, "EntityInformation should " +
            "not be null");
        String tableName = entityInformation.getTableName();

        Assert.hasText(tableName, "tableName should not be null, " +
            "empty or only whitespaces");
        Assert.notNull(id, "id should not be null");

        log.debug("execute existsById in table {}", tableName);

        final String idColumName = mappingNosqlConverter
            .getIdProperty(entityInformation.getJavaType()).getName();

        final MapValue row = mappingNosqlConverter
            .convertIdToPrimaryKey(idColumName, id);

        GetResult getRes = doGet(entityInformation, row);

        return Mono.just(getRes.getValue() != null);
    }

    @Override
    public Mono<Long> count(NosqlEntityInformation<?, ?> entityInformation)
    {
        Assert.notNull(entityInformation, "EntityInformation should " +
            "not be null");
        String tableName = entityInformation.getTableName();
        Assert.hasText(tableName, "Table name should not be null, empty or " +
            "only whitespaces");

        String sql = String.format(NosqlTemplate.TEMPLATE_COUNT, tableName);
        log.debug("count(" + tableName + "): SQL: " + sql);

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
        NosqlEntityInformation<?, ?> entityInformation)
    {
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
                Assert.isTrue(countField != null && countField.getType() ==
                        FieldValue.Type.LONG,
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
    private Iterable<MapValue> runQueryNosqlParams(
        NosqlEntityInformation<?, ?> entityInformation, String query,
        Map<String, FieldValue> nosqlParams) {

        PreparedStatement preparedStatement =
            getPreparedStatement(entityInformation, query);

        if (nosqlParams != null) {
            for (Map.Entry<String, FieldValue> e : nosqlParams.entrySet()) {
                preparedStatement.setVariable(e.getKey(), e.getValue());
            }
        }

        QueryRequest qReq = new QueryRequest()
            .setPreparedStatement(preparedStatement);

        if (entityInformation.getTimeout() > 0) {
            qReq.setTimeout(entityInformation.getTimeout());
        }

        qReq.setConsistency(entityInformation.getConsistency());

        log.debug("Q: {}", query);
        Iterable<MapValue> results = doQuery(qReq);

        return results;
    }

    /* Query execution */
    public <T> Flux<T> executeQuery(NosqlQuery query,
        NosqlEntityInformation<T, ?> entityInformation)
    {
        Assert.notNull(entityInformation, "EntityInformation should " +
            "not be null");
        Class<T> entityClass = entityInformation.getJavaType();

        return executeMapValueQuery(query, entityInformation)
            .map(d -> getConverter().read(entityClass, d));
    }

    public <T> Flux<MapValue> executeMapValueQuery(NosqlQuery query,
        NosqlEntityInformation<T, ?> entityInformation)
    {
        Assert.notNull(entityInformation, "EntityInformation should " +
            "not be null");
        String tableName = entityInformation.getTableName();
        Assert.hasText(tableName, "Table name should not be null, empty or " +
            "only whitespaces.");
        Assert.notNull(query, "Query should not be null.");

        Class<T> entityClass = entityInformation.getJavaType();
        String idPropertyName = ( entityClass == null ||
            mappingNosqlConverter.getIdProperty(entityClass) == null ? null :
            mappingNosqlConverter.getIdProperty(entityClass).getName());

        final Map<String, Object> params = new LinkedHashMap<>();
        String sql = query.generateSql(tableName, params, idPropertyName);

        PreparedStatement pStmt = getPreparedStatement(entityInformation, sql);

        for (Map.Entry<String, Object> param : params.entrySet()) {
            pStmt.setVariable(param.getKey(),
                mappingNosqlConverter.convertObjToFieldValue(param.getValue(),
                    null, false));
        }

        QueryRequest qReq = new QueryRequest().setPreparedStatement(pStmt);

        if (entityInformation.getTimeout() > 0) {
            qReq.setTimeout(entityInformation.getTimeout());
        }

        qReq.setConsistency(entityInformation.getConsistency());

        log.debug("Q: {}", sql);
        Iterable<MapValue> results = doQuery(qReq);

        return Flux.fromIterable(results);
    }

    private PreparedStatement getPreparedStatement(
        NosqlEntityInformation<?, ?> entityInformation, String query) {
        PreparedStatement preparedStatement;

        preparedStatement = psCache.get(query);
        if (preparedStatement == null) {
            PrepareRequest pReq = new PrepareRequest()
                .setStatement(query);

            if (entityInformation.getTimeout() > 0) {
                pReq.setTimeout(entityInformation.getTimeout());
            }

            try {
                log.debug("Prepare: {}", pReq.getStatement());
                PrepareResult pRes = nosqlClient.prepare(pReq);
                preparedStatement = pRes.getPreparedStatement();
                psCache.put(query, preparedStatement);
            } catch (NoSQLException nse) {
                log.error("Prepare: {}", pReq.getStatement());
                log.error(nse.getMessage());
                throw MappingNosqlConverter.convert(nse);
            }
        }
        return preparedStatement.copyStatement();
    }

    private Iterable<MapValue> doQuery(QueryRequest qReq) {
        return new IterableUtil.IterableImpl(nosqlClient, qReq);
    }
}
