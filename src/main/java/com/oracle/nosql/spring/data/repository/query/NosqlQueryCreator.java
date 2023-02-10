/*-
 * Copyright (c) 2020, 2022 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */
package com.oracle.nosql.spring.data.repository.query;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.oracle.nosql.spring.data.core.mapping.NosqlPersistentProperty;
import com.oracle.nosql.spring.data.core.query.Criteria;
import com.oracle.nosql.spring.data.core.query.CriteriaQuery;
import com.oracle.nosql.spring.data.core.query.CriteriaType;
import com.oracle.nosql.spring.data.core.query.NosqlQuery;

import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.repository.query.ReturnedType;
import org.springframework.data.repository.query.parser.AbstractQueryCreator;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.lang.Nullable;


public class NosqlQueryCreator  extends
    AbstractQueryCreator<NosqlQuery, Criteria> {

    private final MappingContext<?, NosqlPersistentProperty> mappingContext;
    private final PartTree tree;
    private final ReturnedType returnedType;


    public NosqlQueryCreator(PartTree tree, NosqlParameterAccessor accessor,
        MappingContext<?, NosqlPersistentProperty> mappingContext,
        ReturnedType returnedType) {

        super(tree, accessor);

        this.mappingContext = mappingContext;
        this.tree = tree;
        this.returnedType = returnedType;
    }

    private String getSubject(Part part) {
        if (part == null) {
            return null;
        }

        String subject = mappingContext.
            getPersistentPropertyPath(part.getProperty()).toDotPath();

        //todo support composite keys
//        final Class<?> domainClass = part.getProperty().getOwningType().getType();

//        @SuppressWarnings("unchecked") final NosqlEntityInformation information =
//            new NosqlEntityInformation(domainClass);

//        if (information.getIdField().getName().equals(subject)) {
//            subject = Constants.ID_PROPERTY_NAME;
//        }

        return subject;
    }

    @Override // Note: side effect here, this method will change the iterator status of parameters.
    protected Criteria create(Part part, Iterator<Object> parameters) {
        if (part == null) {
            return null;
        }

        final Part.Type type = part.getType();
        final String subject = getSubject(part);
        final List<Object> values = new ArrayList<>();

        if (CriteriaType.isPartTypeUnSupported(type)) {
            throw new UnsupportedOperationException("Unsupported keyword: " + type);
        }

        //todo use param/property type instead of value type
//        PersistentPropertyPath<NosqlPersistentProperty> path =
//            context.getPersistentPropertyPath(part.getProperty());
//        NosqlPersistentProperty property = path.getLeafProperty();

        for (int i = 0; i < part.getNumberOfArguments(); i++) {
            if (!parameters.hasNext()) {
                throw new IllegalArgumentException("Expected parameter for " +
                    "part: " + part.getProperty() + "[" + i + "]");
            }
            values.add(parameters.next());
        }

        boolean shouldIgnoreCase =
            part.shouldIgnoreCase() == Part.IgnoreCaseType.ALWAYS ||
                part.shouldIgnoreCase() == Part.IgnoreCaseType.WHEN_POSSIBLE;

        return Criteria.getInstance(part, shouldIgnoreCase, subject, values);
    }

    @Override
    protected Criteria and(Part part, Criteria base,
        Iterator<Object> parameters) {
        final Criteria right = this.create(part, parameters);

        return Criteria.getInstance(part, CriteriaType.AND, base, right);
    }

    @Override
    protected Criteria or(Criteria base, Criteria criteria) {
        if (base == null) {
            return null;
        }

        return Criteria.getInstance(base.getPart(), CriteriaType.OR, base,
            criteria);
    }

    @Override
    protected NosqlQuery complete(@Nullable Criteria criteria, Sort sort) {

        return new CriteriaQuery(criteria, mappingContext)
            .with(sort)
            .setCount(tree.isCountProjection())
            .setDistinct(tree.isDistinct())
            .limit(tree.getMaxResults())
            .project(returnedType);
    }
}
