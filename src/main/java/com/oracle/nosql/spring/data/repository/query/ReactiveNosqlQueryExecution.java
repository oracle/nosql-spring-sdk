/*-
 * Copyright (c) 2020, 2022 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */
package com.oracle.nosql.spring.data.repository.query;

import com.oracle.nosql.spring.data.core.ReactiveNosqlOperations;
import com.oracle.nosql.spring.data.core.query.NosqlQuery;
import com.oracle.nosql.spring.data.repository.support.NosqlEntityInformation;

public interface ReactiveNosqlQueryExecution  {

    Object execute(NosqlQuery query, Class<?> type, String collection);


    final class MultiEntityExecution implements ReactiveNosqlQueryExecution {

        private final ReactiveNosqlOperations operations;
        private final NosqlEntityInformation<?, ?> entityInformation;

        public MultiEntityExecution(ReactiveNosqlOperations operations,
            NosqlEntityInformation<?, ?> entityInformation) {
            this.operations = operations;
            this.entityInformation = entityInformation;
        }

        @Override
        public Object execute(NosqlQuery query, Class<?> type,
            String tableName) {
            return operations.find(query, entityInformation);
        }
    }

    final class ExistsExecution implements ReactiveNosqlQueryExecution {

        private final ReactiveNosqlOperations operations;
        private final NosqlEntityInformation<?, ?> entityInformation;

        public ExistsExecution(ReactiveNosqlOperations operations,
            NosqlEntityInformation<?, ?> entityInformation) {
            this.operations = operations;
            this.entityInformation = entityInformation;
        }

        @Override
        public Object execute(NosqlQuery query, Class<?> type,
            String tableName) {
            return operations.exists(query, entityInformation);
        }
    }

    final class CountExecution implements ReactiveNosqlQueryExecution {

        private final ReactiveNosqlOperations operations;
        private final NosqlEntityInformation<?, ?> entityInformation;

        public CountExecution(ReactiveNosqlOperations operations,
            NosqlEntityInformation<?, ?> entityInformation) {
            this.operations = operations;
            this.entityInformation = entityInformation;
        }

        @Override
        public Object execute(NosqlQuery query, Class<?> type,
            String tableName) {
            return operations.count(query, entityInformation);
        }
    }

    final class DeleteExecution implements ReactiveNosqlQueryExecution {

        private final ReactiveNosqlOperations operations;
        private final NosqlEntityInformation<?, ?> entityInformation;

        public DeleteExecution(ReactiveNosqlOperations operations,
            NosqlEntityInformation<?, ?> entityInformation) {
            this.operations = operations;
            this.entityInformation = entityInformation;
        }

        @Override
        public Object execute(NosqlQuery query, Class<?> type,
            String collection) {
            return operations.delete(query, entityInformation);
        }
    }
}
