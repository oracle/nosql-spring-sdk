/*-
 * Copyright (c) 2020, 2023 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */
package com.oracle.nosql.spring.data.repository.query;

import com.oracle.nosql.spring.data.core.NosqlOperations;
import com.oracle.nosql.spring.data.core.mapping.NosqlPersistentProperty;
import com.oracle.nosql.spring.data.core.query.NosqlQuery;

import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.repository.query.ResultProcessor;
import org.springframework.data.repository.query.ReturnedType;
import org.springframework.data.repository.query.parser.PartTree;

public class PartTreeNosqlQuery extends AbstractNosqlQuery {

    private final PartTree tree;
    private final MappingContext<?, NosqlPersistentProperty> mappingContext;
    private final ResultProcessor processor;

    public PartTreeNosqlQuery(NosqlQueryMethod method,
        NosqlOperations operations) {

        super(method, operations);

        this.processor = method.getResultProcessor();
        this.tree = new PartTree(method.getName(),
            processor.getReturnedType().getDomainType());
        this.mappingContext = operations.getConverter().getMappingContext();
    }

    @Override
    protected NosqlQuery createQuery(NosqlParameterAccessor accessor) {
        ReturnedType returnedType = processor.withDynamicProjection(accessor)
            .getReturnedType();

        final NosqlQueryCreator creator =
            new NosqlQueryCreator(tree, accessor, mappingContext, returnedType);

        return creator.createQuery();
    }

    @Override
    protected boolean isDeleteQuery() {
        return tree.isDelete();
    }

    @Override
    protected boolean isExistsQuery() {
        return tree.isExistsProjection();
    }

    @Override
    protected boolean isCountQuery() {
        return tree.isCountProjection();
    }
}
