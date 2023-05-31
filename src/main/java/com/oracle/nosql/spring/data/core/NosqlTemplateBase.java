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
import java.util.stream.Collectors;

import oracle.nosql.driver.NoSQLException;
import oracle.nosql.driver.NoSQLHandle;
import oracle.nosql.driver.TableNotFoundException;
import oracle.nosql.driver.TimeToLive;
import oracle.nosql.driver.ops.DeleteRequest;
import oracle.nosql.driver.ops.DeleteResult;
import oracle.nosql.driver.ops.GetRequest;
import oracle.nosql.driver.ops.GetResult;
import oracle.nosql.driver.ops.GetTableRequest;
import oracle.nosql.driver.ops.PrepareRequest;
import oracle.nosql.driver.ops.PrepareResult;
import oracle.nosql.driver.ops.PreparedStatement;
import oracle.nosql.driver.ops.PutRequest;
import oracle.nosql.driver.ops.PutResult;
import oracle.nosql.driver.ops.QueryRequest;
import oracle.nosql.driver.ops.TableRequest;
import oracle.nosql.driver.ops.TableResult;
import oracle.nosql.driver.util.LruCache;
import oracle.nosql.driver.values.ArrayValue;
import oracle.nosql.driver.values.FieldValue;
import oracle.nosql.driver.values.JsonOptions;
import oracle.nosql.driver.values.JsonUtils;
import oracle.nosql.driver.values.MapValue;

import com.oracle.nosql.spring.data.NosqlDbFactory;
import com.oracle.nosql.spring.data.core.convert.MappingNosqlConverter;
import com.oracle.nosql.spring.data.core.query.NosqlQuery;
import com.oracle.nosql.spring.data.repository.support.NosqlEntityInformation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.Assert;

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

    protected String getCreateTableDDL(NosqlEntityInformation<?, ?> entityInformation) {
        String tableName = entityInformation.getTableName();
        String sql;

        Map<String, FieldValue.Type> shardKeys =
                entityInformation.getShardKeys();

        Map<String, FieldValue.Type> nonShardKeys =
                entityInformation.getNonShardKeys();

        StringBuilder tableBuilder = new StringBuilder();
        tableBuilder.append("CREATE TABLE IF NOT EXISTS ");
        tableBuilder.append(tableName).append("("); //create open (

        shardKeys.forEach((key, type) -> {
            String keyType = type.name();
            if (keyType.equals(FieldValue.Type.TIMESTAMP.toString())) {
                keyType += "(" + nosqlDbFactory.getTimestampPrecision() + ")";
            }
            String autogen = getAutoGenType(entityInformation);
            tableBuilder.append(key).append(" ").append(keyType)
                    .append(" ").append(autogen).append(",");
        });

        nonShardKeys.forEach((key, type) -> {
            String keyType = type.name();
            if (keyType.equals(FieldValue.Type.TIMESTAMP.toString())) {
                keyType += "(" + nosqlDbFactory.getTimestampPrecision() + ")";
            }
            String autogen = getAutoGenType(entityInformation);
            tableBuilder.append(key).append(" ").append(keyType)
                    .append(" ").append(autogen).append(",");
        });
        tableBuilder.append(JSON_COLUMN).append(" ").append("JSON").append(",");

        tableBuilder.append("PRIMARY KEY").append("("); //primary key open (
        tableBuilder.append("SHARD").append("(");
        tableBuilder.append(String.join(",", shardKeys.keySet()));
        tableBuilder.append(")");

        if (!nonShardKeys.isEmpty()) {
            tableBuilder.append(",");
            tableBuilder.append(String.join(",", nonShardKeys.keySet()));
        }
        tableBuilder.append(")"); //primary key close )
        tableBuilder.append(")"); //create close )

        //ttl
        if (entityInformation.getTtl() != null &&
                entityInformation.getTtl().getValue() != 0) {
            tableBuilder.append(String.format(TEMPLATE_TTL_CREATE,
                    entityInformation.getTtl().toString()));
        }
        sql = tableBuilder.toString();
        return sql;
    }

    protected boolean doCreateTable(NosqlEntityInformation<?, ?> entityInformation) {
        String ddl = getCreateTableDDL(entityInformation);
        TableRequest tableReq = new TableRequest().setStatement(ddl)
            .setTableLimits(entityInformation.getTableLimits(nosqlDbFactory));

        TableResult tableRes = doTableRequest(entityInformation, tableReq);

        TableResult.State tableState = tableRes.getTableState();
        return tableState == TableResult.State.ACTIVE;
    }

    protected boolean doCheckExistingTable(NosqlEntityInformation<?, ?> entityInformation) {
        final String colField = "fields";
        final String colNameField = "name";
        final String colTypeField = "type";
        final String shardField = "shardKey";
        final String primaryField = "primaryKey";
        final String ttlField = "ttl";
        final String identityField = "identity";

        List<String> errors = new ArrayList<>();
        try {
            TableResult tableResult = doGetTable(entityInformation);

            // table does not exist return false
            if (tableResult == null) {
                return false;
            }

            /* If table already exist in the database compare and throw error if
               mismatch*/
            MapValue tableSchema = JsonUtils.createValueFromJson(
                            tableResult.getSchema(),
                            new JsonOptions().setMaintainInsertionOrder(true)).
                    asMap();

            ArrayValue tableColumns = tableSchema.get(colField).asArray();
            ArrayValue tableShardKeys =
                    tableSchema.get(shardField).asArray();
            ArrayValue tablePrimaryKeys =
                    tableSchema.get(primaryField).asArray();

            Map<String, String> tableShardMap = new LinkedHashMap<>();
            Map<String, String> tableNonShardMap = new LinkedHashMap<>();
            Map<String, String> tableOthersMap = new LinkedHashMap<>();

            // extract table details into maps
            for (int i = 0; i < tableColumns.size(); i++) {
                MapValue column = tableColumns.get(i).asMap();
                String colName =
                        column.getString(colNameField).toLowerCase();
                String columnType =
                        column.getString(colTypeField).toLowerCase();

                if (i < tableShardKeys.size()) {
                    tableShardMap.put(colName, columnType);
                } else if (i < tablePrimaryKeys.size()) {
                    tableNonShardMap.put(colName, columnType);
                } else {
                    tableOthersMap.put(colName, columnType);
                }
            }

            // extract entity details into maps
            Map<String, FieldValue.Type> shardKeys = entityInformation.
                    getShardKeys();
            Map<String, FieldValue.Type> nonShardKeys = entityInformation.
                    getNonShardKeys();

            Map<String, String> entityShardMap = new LinkedHashMap<>();
            shardKeys.forEach((k, v) -> entityShardMap.put(
                    k.toLowerCase(), v.name().toLowerCase()));

            Map<String, String> entityNonShardMap = new LinkedHashMap<>();
            nonShardKeys.forEach((k, v) -> entityNonShardMap.put(
                    k.toLowerCase(), v.name().toLowerCase()));

            Map<String, String> entityOthersMap = new LinkedHashMap<>();
            entityOthersMap.put(JSON_COLUMN.toLowerCase(), "json");

            // convert maps to String
            String tableShards = "{" + tableShardMap.entrySet().stream()
                    .map(e -> e.getKey() + " " + e.getValue()).
                    collect(Collectors.joining(",")) + "}";
            String tableNonShards = "{" + tableNonShardMap.entrySet()
                    .stream().map(e -> e.getKey() + " " + e.getValue()).
                    collect(Collectors.joining(",")) + "}";
            String tableOthers = "{" + tableOthersMap.entrySet().stream()
                    .map(e -> e.getKey() + " " + e.getValue()).
                    collect(Collectors.joining(",")) + "}";

            String entityShards = "{" + entityShardMap.entrySet().stream()
                    .map(e -> e.getKey() + " " + e.getValue()).
                    collect(Collectors.joining(",")) + "}";
            String entityNonShards = "{" + entityNonShardMap.entrySet()
                    .stream().map(e -> e.getKey() + " " + e.getValue()).
                    collect(Collectors.joining(",")) + "}";
            String entityOthers = "{" + entityOthersMap.entrySet().stream()
                    .map(e -> e.getKey() + " " + e.getValue()).
                    collect(Collectors.joining(",")) + "}";

            String msg;
            // check shard keys and types match
            if (!tableShards.equals(entityShards)) {
                msg = String.format("Shard primary keys mismatch: " +
                        "table=%s, entity=%s.", tableShards, entityShards);
                errors.add(msg);
            }

            // check non-shard keys and types match
            if (!tableNonShards.equals(entityNonShards)) {
                msg = String.format("Non-shard primary keys mismatch: " +
                                "table=%s, entity=%s.", tableNonShards,
                        entityNonShards);
                errors.add(msg);
            }

            // check non-primary keys and types match
            if (!tableOthers.equals(entityOthers)) {
                msg = String.format("Non-primary key columns mismatch:" +
                                "table=%s, entity=%s.", tableOthers,
                        entityOthers);
                errors.add(msg);
            }

            // check identity same
            FieldValue identity = tableSchema.get(identityField);
            if (identity != null && !entityInformation.isAutoGeneratedId()) {
                errors.add("Identity information mismatch.");

            } else if (identity == null && entityInformation.isAutoGeneratedId() &&
                    entityInformation.getIdNosqlType() != FieldValue.Type.STRING) {
                errors.add("Identity information mismatch.");
            }

            // TTL warning
            FieldValue ttlValue = tableSchema.get(ttlField);
            TimeToLive ttl = entityInformation.getTtl();
            // TTL is present in database but not in the entity
            if (ttlValue != null && ttl != null &&
                    !ttl.toString().equalsIgnoreCase(ttlValue.getString())) {
                LOG.warn("TTL of the table in database is different from " +
                        "the TTL of the entity " +
                        entityInformation.getJavaType().getName());
            } else if (ttlValue == null && ttl != null && ttl.getValue() != 0) {
                // TTL is present in entity but not in the database
                LOG.warn("TTL of the table in database is different from " +
                        "the TTL of the entity " +
                        entityInformation.getJavaType().getName());
            }
        } catch (NullPointerException npe) {
            LOG.warn("Error while checking DDLs of table and entity " + npe.getMessage());
            if (LOG.isDebugEnabled()) {
                npe.printStackTrace();
            }
        }

        if (!errors.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("The following mismatch have been found between the " +
                    "entity class ");
            sb.append(entityInformation.getJavaType().getName());
            sb.append(" definition and the existing table ");
            sb.append(entityInformation.getTableName());
            sb.append(" in the database:\n");
            errors.forEach(err -> sb.append(err).append("\n"));
            sb.append("To fix this errors, either make the entity class to" +
                    " match the table definition or use NosqlTable.name" +
                    " annotation to use a different table.");
            throw new IllegalArgumentException(sb.toString());
        }
        // no mismatch between table and entity return true
        return true;
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
            putRes = nosqlClient.put(putReq);
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
            idPropertyName, mappingNosqlConverter.
                        getMappingContext().getPersistentEntity(entityClass));

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

    protected TableResult doGetTable(NosqlEntityInformation<?, ?> entityInformation) {
        try {
            GetTableRequest request = new GetTableRequest();
            request.setTableName(entityInformation.getTableName());
            return nosqlClient.getTable(request);
        } catch (TableNotFoundException tne) {
            return null;
        } catch (NoSQLException nse) {
            throw MappingNosqlConverter.convert(nse);
        }
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

    private String getAutoGenType(NosqlEntityInformation<?, ?> entityInformation) {
        if (entityInformation.isAutoGeneratedId()) {
            return (entityInformation.getIdNosqlType() == FieldValue.Type.STRING) ?
                    TEMPLATE_GENERATED_UUID : TEMPLATE_GENERATED_ALWAYS;
        }
        return "";
    }
}
