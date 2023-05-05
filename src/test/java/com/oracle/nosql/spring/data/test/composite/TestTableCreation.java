/*-
 * Copyright (c) 2020, 2023 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */

package com.oracle.nosql.spring.data.test.composite;

import com.oracle.nosql.spring.data.core.NosqlTemplate;
import com.oracle.nosql.spring.data.core.mapping.NosqlId;
import com.oracle.nosql.spring.data.core.mapping.NosqlKey;
import com.oracle.nosql.spring.data.core.mapping.NosqlTable;
import com.oracle.nosql.spring.data.repository.support.NosqlEntityInformation;
import com.oracle.nosql.spring.data.test.app.AppConfig;
import oracle.nosql.driver.ops.GetTableRequest;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.fail;

/**
 * Tests for composite key table creation DDL
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = AppConfig.class)
public class TestTableCreation {
    private static final String CLOUD_SIM_NAMESPACE = "in.valid.iac.name" +
            ".space:";
    private static NosqlTemplate template;

    @BeforeClass
    public static void staticSetup() throws ClassNotFoundException {
        template = NosqlTemplate.create(AppConfig.nosqlDBConfig);
    }

    @Test
    public void testCompositeEntityWithNoKeys() {
        Class<?> domainClass = CompositeEntityWithNoKeys.class;
        NosqlEntityInformation<?, ?> entityInformation =
                template.getNosqlEntityInformation(domainClass);

        template.dropTableIfExists(domainClass.getSimpleName());
        template.createTableIfNotExists(entityInformation);

        String tableDDL = template.getNosqlClient().
                getTable(new GetTableRequest().
                        setTableName(domainClass.getSimpleName())).getDdl();
        if (tableDDL != null) {
            tableDDL = tableDDL.replaceAll(CLOUD_SIM_NAMESPACE, "");
            assertEquals(CompositeEntityWithNoKeys.DDL, tableDDL);
        }
        template.dropTableIfExists(domainClass.getSimpleName());
    }

    @Test
    public void testCompositeEntityWithAllKeys() {
        Class<?> domainClass = CompositeEntityWithAllKeys.class;
        NosqlEntityInformation<?, ?> entityInformation =
                template.getNosqlEntityInformation(domainClass);

        template.dropTableIfExists(domainClass.getSimpleName());
        template.createTableIfNotExists(entityInformation);

        String tableDDL = template.getNosqlClient().
                getTable(new GetTableRequest().
                        setTableName(domainClass.getSimpleName())).getDdl();
        if (tableDDL != null) {
            tableDDL = tableDDL.replaceAll(CLOUD_SIM_NAMESPACE, "");
            assertEquals(CompositeEntityWithAllKeys.DDL, tableDDL);
        }
        template.dropTableIfExists(domainClass.getSimpleName());
    }

    @Test
    public void testCompositeEntityWithShardKey() {
        Class<?> domainClass = CompositeEntityWithShardKey.class;
        NosqlEntityInformation<?, ?> entityInformation =
                template.getNosqlEntityInformation(domainClass);

        template.dropTableIfExists(domainClass.getSimpleName());
        template.createTableIfNotExists(entityInformation);

        String tableDDL = template.getNosqlClient().
                getTable(new GetTableRequest().
                        setTableName(domainClass.getSimpleName())).getDdl();
        if (tableDDL != null) {
            tableDDL = tableDDL.replaceAll(CLOUD_SIM_NAMESPACE, "");
            assertEquals(CompositeEntityWithShardKey.DDL, tableDDL);
        }
        template.dropTableIfExists(domainClass.getSimpleName());
    }

    @Test
    public void testCompositeEntityWithNoShardKey() {
        Class<?> domainClass = CompositeEntityWithNoShardKey.class;
        try {
            NosqlEntityInformation<?, ?> entityInformation =
                    template.getNosqlEntityInformation(domainClass);
            template.createTableIfNotExists(entityInformation);
            fail("Expecting IllegalArgumentException but didn't get");
        } catch (IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testCompositeEntityWithOrder() {
        Class<?> domainClass = CompositeEntityWithOrder.class;
        NosqlEntityInformation<?, ?> entityInformation =
                template.getNosqlEntityInformation(domainClass);

        template.dropTableIfExists(domainClass.getSimpleName());
        template.createTableIfNotExists(entityInformation);

        String tableDDL = template.getNosqlClient().
                getTable(new GetTableRequest().
                        setTableName(domainClass.getSimpleName())).getDdl();
        if (tableDDL != null) {
            tableDDL = tableDDL.replaceAll(CLOUD_SIM_NAMESPACE, "");
            assertEquals(CompositeEntityWithOrder.DDL, tableDDL);
        }
        template.dropTableIfExists(domainClass.getSimpleName());
    }

    @Test
    public void testCompositeEntityWithMultipleKeys() {
        Class<?> domainClass = CompositeEntityWithMultipleKeys.class;
        try {
            NosqlEntityInformation<?, ?> entityInformation =
                    template.getNosqlEntityInformation(domainClass);
            fail("Expecting IllegalArgumentException but didn't get");
        } catch (IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testCompositeEntityWithRepeatingOrder() {
        Class<?> domainClass = CompositeEntityWithRepeatingOrder.class;
        try {
            NosqlEntityInformation<?, ?> entityInformation =
                    template.getNosqlEntityInformation(domainClass);
            template.createTableIfNotExists(entityInformation);
            fail("Expecting IllegalArgumentException but didn't get");
        } catch (IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testCompositeEntityWithMissingOrder() {
        Class<?> domainClass = CompositeEntityWithMissingOrder.class;
        try {
            NosqlEntityInformation<?, ?> entityInformation =
                    template.getNosqlEntityInformation(domainClass);
            template.createTableIfNotExists(entityInformation);
            fail("Expecting IllegalArgumentException but didn't get");
        } catch (IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testCompositeEntityWithMissingNonShardOrder() {
        Class<?> domainClass = CompositeEntityWithMissingNonShardOrder.class;
        try {
            NosqlEntityInformation<?, ?> entityInformation =
                    template.getNosqlEntityInformation(domainClass);
            template.createTableIfNotExists(entityInformation);
            fail("Expecting IllegalArgumentException but didn't get");
        } catch (IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testCompositeEntityRecommended() {
        Class<?> domainClass = CompositeEntityRecommended.class;
        NosqlEntityInformation<?, ?> entityInformation =
                template.getNosqlEntityInformation(domainClass);

        template.dropTableIfExists(domainClass.getSimpleName());
        template.createTableIfNotExists(entityInformation);

        String tableDDL = template.getNosqlClient().
                getTable(new GetTableRequest().
                        setTableName(domainClass.getSimpleName())).getDdl();
        if (tableDDL != null) {
            tableDDL = tableDDL.replaceAll(CLOUD_SIM_NAMESPACE, "");
            assertEquals(CompositeEntityRecommended.DDL, tableDDL);
        }
        template.dropTableIfExists(domainClass.getSimpleName());
    }

    @Test
    public void testCompositeEntityCaseInsensitive() {
        Class<?> domainClass = CompositeEntityCaseInsensitive.class;
        NosqlEntityInformation<?, ?> entityInformation =
                template.getNosqlEntityInformation(domainClass);

        template.dropTableIfExists(domainClass.getSimpleName());
        template.createTableIfNotExists(entityInformation);

        String tableDDL = template.getNosqlClient().
                getTable(new GetTableRequest().
                        setTableName(domainClass.getSimpleName())).getDdl();
        if (tableDDL != null) {
            tableDDL = tableDDL.replaceAll(CLOUD_SIM_NAMESPACE, "");
            assertEquals(CompositeEntityCaseInsensitive.DDL, tableDDL);
        }
        template.dropTableIfExists(domainClass.getSimpleName());
    }

    @Test
    public void testCompositeKeyCollision() {
        //shard and non shard key collision
        Class<?> domainClass = CompositeEntityFieldCollision.class;
        try {
            template.getNosqlEntityInformation(domainClass);
            fail("Expecting IllegalArgumentException but didn't get");
        } catch (IllegalArgumentException ignored) {

        }
    }

    @Test
    public void testCompositeEntityKvJsonField() {
        //shard and non shard key collision
        Class<?> domainClass = CompositeEntityKvJsonField.class;
        try {
            template.getNosqlEntityInformation(domainClass);
            fail("Expecting IllegalArgumentException but didn't get");
        } catch (IllegalArgumentException ignored) {
        }
    }

    @NosqlTable
    public static class CompositeEntityWithNoKeys {
        @NosqlId
        private CompositeKeyNoKeys id;
        String value;
        private static final String DDL = String.format(
                "CREATE TABLE IF NOT EXISTS %s (id1 STRING, id2 STRING, " +
                        "kv_json_ JSON, PRIMARY KEY(SHARD(id1, id2)))",
                CompositeEntityWithNoKeys.class.getSimpleName());
    }

    public static class CompositeKeyNoKeys {
        String id2;
        String id1;
    }

    @NosqlTable
    public static class CompositeEntityWithAllKeys {
        @NosqlId
        private CompositeKeyAllKeys id;
        String value;
        private static final String DDL = String.format(
                "CREATE TABLE IF NOT EXISTS %s (id1 STRING, id2 STRING, " +
                        "kv_json_ JSON, PRIMARY KEY(SHARD(id1, id2)))",
                CompositeEntityWithAllKeys.class.getSimpleName());
    }

    public static class CompositeKeyAllKeys {
        @NosqlKey
        String id1;
        @NosqlKey
        String id2;
    }

    @NosqlTable
    public static class CompositeEntityWithShardKey {
        @NosqlId
        private CompositeKeyShardKeys id;
        String value;
        private static final String DDL = String.format(
                "CREATE TABLE IF NOT EXISTS %s (id1 STRING, id2 STRING, " +
                        "kv_json_ JSON, PRIMARY KEY(SHARD(id1, id2)))",
                CompositeEntityWithShardKey.class.getSimpleName());
    }

    public static class CompositeKeyShardKeys {
        @NosqlKey(shardKey = true)
        String id1;
        @NosqlKey
        String id2;
    }

    @NosqlTable
    public static class CompositeEntityWithNoShardKey {
        @NosqlId
        private CompositeKeyNoShardKeys id;
        String value;
        private static final String DDL = String.format(
                "CREATE TABLE IF NOT EXISTS %s (id1 STRING, id2 STRING, " +
                        "kv_json_ JSON, PRIMARY KEY(id1, id2))",
                CompositeEntityWithNoShardKey.class.getSimpleName());
    }

    public static class CompositeKeyNoShardKeys {
        @NosqlKey(shardKey = false)
        String id1;
        @NosqlKey(shardKey = false)
        String id2;
    }

    @NosqlTable
    public static class CompositeEntityWithOrder {
        @NosqlId
        private CompositeKeyWithOrder id;
        String value;
        private static final String DDL = String.format(
                "CREATE TABLE IF NOT EXISTS %s (id2 STRING, id1 STRING, " +
                        "kv_json_ JSON, PRIMARY KEY(SHARD(id2, id1)))",
                CompositeEntityWithOrder.class.getSimpleName());
    }

    public static class CompositeKeyWithOrder {
        @NosqlKey(shardKey = true, order = 0)
        String id2;
        @NosqlKey(shardKey = true, order = 1)
        String id1;
    }

    @NosqlTable
    public static class CompositeEntityWithMultipleKeys {
        @NosqlId
        private CompositeKeyMulti id;
        String value;

        private static final String DDL = String.format(
                "CREATE TABLE IF NOT EXISTS %s (id2 STRING, id1 STRING, id3 " +
                        "STRING, id5 STRING, id4 STRING, " +
                        "kv_json_ JSON, PRIMARY KEY(SHARD(id2, id1, id3), " +
                        "id5, id4))",
                CompositeEntityWithMultipleKeys.class.getSimpleName());
    }

    public static class CompositeKeyMulti {
        @NosqlKey(shardKey = true, order = 0)
        String id2;
        @NosqlKey(shardKey = true, order = 1)
        String id1;
        @NosqlKey(shardKey = true, order = 2)
        String id3;
        @NosqlKey(shardKey = false, order = 0)
        String id5;
        @NosqlKey(shardKey = false, order = 1)
        String id4;
    }

    @NosqlTable
    public static class CompositeEntityWithRepeatingOrder {
        @NosqlId
        private CompositeKeyWithRepeatingOrder id;
        String value;
    }

    public static class CompositeKeyWithRepeatingOrder {
        @NosqlKey(shardKey = true, order = 0)
        String id2;
        @NosqlKey(shardKey = true, order = 1)
        String id1;
        @NosqlKey(shardKey = true, order = 1)
        String id3;
        @NosqlKey(shardKey = false, order = 1)
        String id5;
        @NosqlKey(shardKey = false, order = 1)
        String id4;
    }

    @NosqlTable
    public static class CompositeEntityWithMissingOrder {
        @NosqlId
        private CompositeKeyWithMissingOrder id;
        String value;
    }

    public static class CompositeKeyWithMissingOrder {
        @NosqlKey(shardKey = true, order = 1)
        String id1;

        @NosqlKey(shardKey = true)
        String id2;
    }

    @NosqlTable
    public static class CompositeEntityWithMissingNonShardOrder {
        @NosqlId
        private CompositeKeyWithMissingNonShardOrder id;
        String value;
    }

    public static class CompositeKeyWithMissingNonShardOrder {
        @NosqlKey(shardKey = true)
        String id1;

        @NosqlKey(shardKey = false, order = 1)
        String id2;

        @NosqlKey(shardKey = false)
        String id3;
    }

    @NosqlTable
    public static class CompositeEntityRecommended {
        @NosqlId
        private CompositeKeyRecommended id;
        String value;

        private static final String DDL = String.format(
                "CREATE TABLE IF NOT EXISTS %s (id2 STRING, id1 STRING, id4 " +
                        "STRING, id3 STRING, " +
                        "kv_json_ JSON, PRIMARY KEY(SHARD(id2, id1), " +
                        "id4, id3))",
                CompositeEntityRecommended.class.getSimpleName());
    }
    public static class CompositeKeyRecommended {
        @NosqlKey(shardKey = true, order = 1)
        String id2;

        @NosqlKey(shardKey = true, order = 2)
        String id1;

        @NosqlKey(shardKey = false, order = 3)
        String id4;

        @NosqlKey(shardKey = false, order = 4)
        String id3;

    }

    @NosqlTable
    public static class CompositeEntityCaseInsensitive {
        @NosqlId
        private CompositeKeyCaseInsensitive id;
        String value;

        private static final String DDL = String.format(
                "CREATE TABLE IF NOT EXISTS %s (abcd STRING, id1 STRING, ID2 " +
                        "STRING, kv_json_ JSON, PRIMARY KEY(SHARD(abcd, id1, " +
                        "ID2)))",
                CompositeEntityCaseInsensitive.class.getSimpleName()
        );
    }

    public static class CompositeKeyCaseInsensitive {
        @NosqlKey
        String ID2;

        @NosqlKey
        String id1;

        @NosqlKey
        String abcd;
    }

    @NosqlTable
    public static class CompositeEntityFieldCollision {
        @NosqlId
        private CompositeKeyFieldCollision id;
    }

    public static class CompositeKeyFieldCollision {
        @NosqlKey
        String id1;

        @NosqlKey
        String ID1;
    }

    @NosqlTable
    public static class CompositeEntityKvJsonField {
        @NosqlId
        private CompositeKeyKvJsonField id;
    }

    public static class CompositeKeyKvJsonField {
        @NosqlKey(shardKey = true, order = 1)
        private String kv_json_;
    }
}
