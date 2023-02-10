/*-
 * Copyright (c) 2020, 2022 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */
package com.oracle.nosql.spring.data.core.query;


import java.util.ArrayList;
import java.util.List;

import org.springframework.data.repository.query.parser.Part;
import org.springframework.lang.NonNull;

public class Criteria {

    private Part part;
    private final CriteriaType type;
    private final List<Criteria> subCriteria;
    private boolean ignoreCase;
    private String subject;
    private List<Object> subjectValues;

    private Criteria(CriteriaType type) {
        this.type = type;
        this.subCriteria = new ArrayList<>();
    }

    public static Criteria getInstance(Part part, boolean isIgnoreCase,
        @NonNull String subject, @NonNull List<Object> values) {
        final Criteria criteria =
            new Criteria(CriteriaType.toCriteriaType(part.getType()));

        criteria.part = part;
        criteria.ignoreCase = isIgnoreCase;
        criteria.subject = subject;
        criteria.subjectValues = values;

        return criteria;
    }

    public static Criteria getInstance(Part part, CriteriaType type,
        @NonNull Criteria left, @NonNull Criteria right) {
        final Criteria criteria = new Criteria(type);

        criteria.subCriteria.add(left);
        criteria.subCriteria.add(right);
        criteria.part = part;

        return criteria;
    }

    public static Criteria getInstance(CriteriaType type) {
        return new Criteria(type);
    }

    public String getSubject() {
        return subject;
    }

    public List<Object> getSubjectValues() {
        return subjectValues;
    }

    public CriteriaType getType() {
        return type;
    }

    public List<Criteria> getSubCriteria() {
        return subCriteria;
    }

    public boolean isIgnoreCase() {
        return ignoreCase;
    }

    public Part getPart() {
        return part;
    }
}

