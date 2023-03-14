/*-
 * Copyright (c) 2020, 2023 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */
package com.oracle.nosql.spring.data.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

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
import com.oracle.nosql.spring.data.core.convert.MappingNosqlConverter;
import com.oracle.nosql.spring.data.core.mapping.NosqlKey;
import com.oracle.nosql.spring.data.core.mapping.NosqlPersistentEntity;
import com.oracle.nosql.spring.data.core.mapping.NosqlPersistentProperty;
import com.oracle.nosql.spring.data.core.query.NosqlQuery;
import com.oracle.nosql.spring.data.repository.support.NosqlEntityInformation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.Assert;

import static com.oracle.nosql.spring.data.Constants.NOTSET_SHARD_KEY;


public abstract class NosqlTemplateBase
    implements ApplicationContextAware {

    public static final String JSON_COLUMN = "kv_json_";

    protected static final Logger LOG =
        LoggerFactory.getLogger(NosqlTemplateBase.class);
    static final String TEMPLATE_CREATE_TABLE =
        "CREATE TABLE IF NOT EXISTS %s (%s %s %s, " +
            JSON_COLUMN + " JSON, PRIMARY KEY( %s )) %s";
    static final String TEMPLATE_GENERATED_ALWAYS =
        "GENERATED ALWAYS as IDENTITY (NO CYCLE)";
    static final String TEMPLATE_GENERATED_UUID =
        " AS UUID GENERATED BY DEFAULT";
    static final String TEMPLATE_DROP_TABLE =
        "DROP TABLE IF EXISTS %s ";
    static final String TEMPLATE_DELETE_ALL =
        "DELETE FROM %s ";
    static final String TEMPLATE_SELECT_ALL =
        "SELECT * FROM %s t";
    static final String TEMPLATE_COUNT =
        "SELECT count(*) FROM %s ";
    static final String TEMPLATE_UPDATE =
        "DECLARE $id %s; $json JSON; " +
        "UPDATE %s t SET t." + JSON_COLUMN + " = $json WHERE t.%s = $id";
    static final String TEMPLATE_TTL_CREATE = "USING TTL %s";

    protected final NosqlDbFactory nosqlDbFactory;
    protected final NoSQLHandle nosqlClient;
    protected final MappingNosqlConverter mappingNosqlConverter;
    protected LruCache<String, PreparedStatement> psCache;
    protected ApplicationContext applicationContext;

    protected NosqlTemplateBase(NosqlDbFactory nosqlDbFactory,
        MappingNosqlConverter mappingNosqlConverter) {

        Assert.notNull(nosqlDbFactory, "NosqlDbFactory should not be null.");
        this.nosqlDbFactory = nosqlDbFactory;
        nosqlClient = nosqlDbFactory.getNosqlClient();
        this.mappingNosqlConverter = mappingNosqlConverter;
        psCache = new LruCache<>(nosqlDbFactory.getQueryCacheCapacity(),
            nosqlDbFactory.getQueryCacheLifetime());
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    protected TableResult doTableRequest(NosqlEntityInformation<?, ?> entityInformation,
        TableRequest tableReq) {

        if (entityInformation != null &&
            entityInformation.getTimeout() > 0) {
            tableReq.setTimeout(entityInformation.getTimeout());
        }

        TableResult tableRes;
        try {
            LOG.debug("DDL: {}", tableReq.getStatement());
            tableRes = nosqlClient.doTableRequest(tableReq,
                nosqlDbFactory.getTableReqTimeout(),
                nosqlDbFactory.getTableReqPollInterval());
        } catch (NoSQLException nse) {
            LOG.error("DDL: {}", tableReq.getStatement());
            LOG.error(nse.getMessage());
            throw MappingNosqlConverter.convert(nse);
        }
        return tableRes;
    }

    protected boolean doCreateTableIfNotExists(
        NosqlEntityInformation<?, ?> entityInformation) {

        String tableName = entityInformation.getTableName();
        String sql;

        /*If composite key, sort composite key class to create table.
         sorting is based on following rules.
         Sort by shard key=true, then by order then by name
         */
        if (NosqlEntityInformation.isCompositeKeyType(entityInformation.getIdField().getType())) {
            StringBuilder tableBuilder = new StringBuilder();
            Map<Integer, SortedSet<String>> shardKeys = new TreeMap<>();
            Map<Integer, SortedSet<String>> otherKeys = new TreeMap<>();

            tableBuilder.append("CREATE TABLE IF NOT EXISTS ");
            tableBuilder.append(tableName).append("("); //create open (

            NosqlPersistentEntity<?> compositeKeyEntity =
                    mappingNosqlConverter.getMappingContext().getPersistentEntity(entityInformation.getIdType());
            for (NosqlPersistentProperty idProperty : compositeKeyEntity) {
                if (idProperty.isWritable()) {
                    String keyName = idProperty.getName();
                    String ketType =
                            NosqlEntityInformation.findIdNosqlType(idProperty.getType()).toString();
                    tableBuilder.append(keyName).append(" ").append(ketType).append(",");

                    if (idProperty.isAnnotationPresent(NosqlKey.class)) {
                        NosqlKey noSqlKey =
                                idProperty.findAnnotation(NosqlKey.class);
                        int order = noSqlKey.order();
                        if (!noSqlKey.shardKey()) {
                            SortedSet<String> ss = otherKeys.getOrDefault(order,
                                    new TreeSet<>());
                            ss.add(idProperty.getName());
                            otherKeys.put(order, ss);

                        } else {
                            SortedSet<String> ss = shardKeys.getOrDefault(order,
                                    new TreeSet<>());
                            ss.add(idProperty.getName());
                            shardKeys.put(order, ss);
                        }
                    } else {
                        SortedSet<String> ss =
                                shardKeys.getOrDefault(NOTSET_SHARD_KEY,
                                new TreeSet<>());
                        ss.add(idProperty.getName());
                        shardKeys.put(-1, ss);
                    }
                }
            }
            tableBuilder.append(JSON_COLUMN).append(" ").append("JSON").append(",");

            List<String> sortedShardKeys = new ArrayList<>();
            List<String> sortedOtherKeys = new ArrayList<>();

            shardKeys.forEach((order, keys) -> {
                sortedShardKeys.addAll(keys);
            });

            otherKeys.forEach((order, keys) -> {
                sortedOtherKeys.addAll(keys);
            });

            tableBuilder.append("PRIMARY KEY").append("("); //primary key open (

            if (shardKeys.isEmpty()) {
                tableBuilder.append(String.join(",", sortedOtherKeys));
            } else {
                tableBuilder.append("SHARD").append("(");
                tableBuilder.append(String.join(",", sortedShardKeys));
                tableBuilder.append(")");
                if (!otherKeys.isEmpty()) {
                    tableBuilder.append(",");
                    tableBuilder.append(String.join(",", sortedOtherKeys));
                }
            }
            tableBuilder.append(")"); //primary key close )
            tableBuilder.append(")"); //create close )

            if (entityInformation.getTtl() != null &&
                    entityInformation.getTtl().getValue() != 0) {
                tableBuilder.append(String.format(TEMPLATE_TTL_CREATE,
                        entityInformation.getTtl().toString()));
            }
            sql = tableBuilder.toString();
        } else {
            String idColName = entityInformation.getIdField().getName();

            String idColType = entityInformation.getIdNosqlType().toString();
            if (entityInformation.getIdNosqlType() == FieldValue.Type.TIMESTAMP) {
                // For example: CREATE TABLE IF NOT EXISTS SensorIdTimestamp
                //   (time TIMESTAMP(3) , kv_json_ JSON, PRIMARY KEY( time ))
                idColType += "(" + nosqlDbFactory.getTimestampPrecision() + ")";
            }

            String autogen = "";
            if (entityInformation.isAutoGeneratedId()) {
                if (entityInformation.getIdNosqlType() == FieldValue.Type.STRING) {
                    autogen = TEMPLATE_GENERATED_UUID;
                } else {
                    autogen = TEMPLATE_GENERATED_ALWAYS;
                }
            }

            String ttl = "";
            if (entityInformation.getTtl() != null &&
                    entityInformation.getTtl().getValue() != 0) {
                ttl = String.format(TEMPLATE_TTL_CREATE,
                        entityInformation.getTtl().toString());
            }

            sql = String.format(TEMPLATE_CREATE_TABLE,
                    tableName,
                    idColName, idColType, autogen, idColName, ttl);
        }
        TableRequest tableReq = new TableRequest().setStatement(sql)
            .setTableLimits(entityInformation.getTableLimits(nosqlDbFactory));

        TableResult tableRes = doTableRequest(entityInformation, tableReq);

        TableResult.State tableState = tableRes.getTableState();
        return tableState == TableResult.State.ACTIVE;
    }

    protected DeleteResult doDelete(
        NosqlEntityInformation<?, ?> entityInformation,
        MapValue primaryKey) {

        DeleteRequest delReq = new DeleteRequest()
            .setTableName(entityInformation.getTableName())
            .setKey(primaryKey);

        if (entityInformation.getTimeout() > 0) {
            delReq.setTimeout(entityInformation.getTimeout());
        }

        delReq.setDurability(entityInformation.getDurability());

        DeleteResult delRes;

        try {
            delRes = nosqlClient.delete(delReq);
        } catch (NoSQLException nse) {
            LOG.error("Delete: table: {} key: {}", delReq.getTableName(),
                primaryKey);
            LOG.error(nse.getMessage());
            throw MappingNosqlConverter.convert(nse);
        }

        assert delRes != null;
        return delRes;
    }

    protected PutResult doPut(NosqlEntityInformation<?, ?> entityInformation,
        MapValue row, boolean ifPresent) {
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

        putReq.setDurability(entityInformation.getDurability());

        PutResult putRes;
        try {
            try {
                putRes = nosqlClient.put(putReq);
            } catch (TableNotFoundException tnfe) {
                if (entityInformation.isAutoCreateTable()) {
                    doCreateTableIfNotExists(entityInformation);
                    putRes = nosqlClient.put(putReq);
                } else {
                    throw tnfe;
                }
            }
        } catch (NoSQLException nse) {
            LOG.error("Put: table: {} key: {}", putReq.getTableName(),
                row.get(entityInformation.getIdColumnName()));
            LOG.error(nse.getMessage());
            throw MappingNosqlConverter.convert(nse);
        }

        assert putRes != null;
        return putRes;
    }

    protected GetResult doGet(NosqlEntityInformation<?, ?> entityInformation,
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
            LOG.error("Get: table: {} key: {}", getReq.getTableName(),
                primaryKey);
            LOG.error(nse.getMessage());
            throw MappingNosqlConverter.convert(nse);
        }

        assert getRes != null;
        return getRes;
    }

    protected void doUpdate(NosqlEntityInformation<?, ?> entityInformation,
        MapValue row) {
        // When id is autogenerated, it's required to do a SQL update query
        if (entityInformation.isAutoGeneratedId()) {
            final String idColumnName = entityInformation.getIdColumnName();
            String sql = String.format(TEMPLATE_UPDATE,
                entityInformation.getIdNosqlType().name(),
                entityInformation.getTableName(),
                idColumnName);

            Map<String, FieldValue> params = new HashMap<>();
            //todo implement composite keys
            params.put("$id", row.get(idColumnName));
            params.put("$json", row.get(JSON_COLUMN));

            // Must read at least one result to execute query!!!
            doRunQueryNosqlParams(entityInformation, sql, params)
                .iterator()
                .next();
        } else {
            // otherwise do a regular put, which is faster, use less resources
            doPut(entityInformation, row, true);
        }
    }

    /**
     * nosqlParams is a Map of param_name to FieldValue
     */
    protected Iterable<MapValue> doRunQueryNosqlParams(
        NosqlEntityInformation<?, ?> entityInformation,
        String query,
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

        if (entityInformation != null) {
            if (entityInformation.getTimeout() > 0) {
                qReq.setTimeout(entityInformation.getTimeout());
            }

            qReq.setConsistency(entityInformation.getConsistency());
        }

        LOG.debug("Q: {}", query);
        
        return doQuery(qReq);
    }

    protected <T> Iterable<MapValue> doExecuteMapValueQuery(NosqlQuery query,
        NosqlEntityInformation<T, ?> entityInformation) {

        Assert.notNull(entityInformation, "Entity information " +
            "should not be null.");
        Assert.notNull(query, "Query should not be null.");

        Class<T> entityClass = entityInformation.getJavaType();

        String idPropertyName = ( entityClass == null ||
            mappingNosqlConverter.getIdProperty(entityClass) == null ? null :
            mappingNosqlConverter.getIdProperty(entityClass).getName());

        final Map<String, Object> params = new LinkedHashMap<>();
        String sql = query.generateSql(entityInformation.getTableName(), params,
            idPropertyName);

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

        if (query.isCount()) {
            qReq.setLimit(1);
        }

        LOG.debug("Q: {}", sql);
//        System.out.println("Q: " + sql);
        return doQuery(qReq);
    }

    private PreparedStatement getPreparedStatement(
        NosqlEntityInformation<?, ?> entityInformation, String query) {
        PreparedStatement preparedStatement;

        preparedStatement = psCache.get(query);
        if (preparedStatement == null) {
            PrepareRequest pReq = new PrepareRequest()
                .setStatement(query);

            if (entityInformation != null) {
                if (entityInformation.getTimeout() > 0) {
                    pReq.setTimeout(entityInformation.getTimeout());
                }
            }

            try {
                LOG.debug("Prepare: {}", pReq.getStatement());
                PrepareResult pRes = nosqlClient.prepare(pReq);
                preparedStatement = pRes.getPreparedStatement();
                psCache.put(query, preparedStatement);
            } catch (NoSQLException nse) {
                LOG.error("Prepare: {}", pReq.getStatement());
                LOG.error(nse.getMessage());
                throw MappingNosqlConverter.convert(nse);
            }
        }
        return preparedStatement.copyStatement();
    }

    private Iterable<MapValue> doQuery(QueryRequest qReq) {
        return new IterableUtil.IterableImpl(nosqlClient, qReq);
    }
}
