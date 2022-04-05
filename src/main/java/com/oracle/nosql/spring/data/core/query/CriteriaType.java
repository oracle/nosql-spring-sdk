/*-
 * Copyright (c) 2020, 2022 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */
package com.oracle.nosql.spring.data.core.query;


import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.data.repository.query.parser.Part;
import org.springframework.lang.NonNull;

public enum CriteriaType {

    ALL(""),
    IS_EQUAL("="),
    OR("OR"),
    AND("AND"),
    NOT("!="),
    BEFORE("<"),
    AFTER(">"),
    IN("IN"),
    NOT_IN("IN"),               // NOT(...) applied outside
    IS_NULL("IS NULL"),
    IS_NOT_NULL("IS NOT NULL"),
    LESS_THAN("<"),
    LESS_THAN_EQUAL("<="),
    GREATER_THAN(">"),
    GREATER_THAN_EQUAL(">="),
    CONTAINING("contains"),
    NOT_CONTAINING("NOT contains"),
    ENDS_WITH("ends_with"),
    STARTS_WITH("starts_with"),
    TRUE("= true"),
    FALSE("= false"),
    BETWEEN("between"),
    REGEX("regex_like"),
    LIKE("regex_like"),
    NOT_LIKE("NOT regex_like"),
    EXISTS("exists"),
    NEAR("geo_near"),
    WITHIN("geo_inside");


    private String sqlKeyword;

    private static final Map<Part.Type, CriteriaType> PART_TREE_TYPE_TO_CRITERIA;

    static {
        final Map<Part.Type, CriteriaType> map = new HashMap<>();

        map.put(Part.Type.NEGATING_SIMPLE_PROPERTY, CriteriaType.NOT);
        map.put(Part.Type.IS_NULL, CriteriaType.IS_NULL);
        map.put(Part.Type.IS_NOT_NULL, CriteriaType.IS_NOT_NULL);
        map.put(Part.Type.SIMPLE_PROPERTY, CriteriaType.IS_EQUAL);
        map.put(Part.Type.BEFORE, CriteriaType.BEFORE);
        map.put(Part.Type.AFTER, CriteriaType.AFTER);
        map.put(Part.Type.IN, CriteriaType.IN);
        map.put(Part.Type.NOT_IN, CriteriaType.NOT_IN);
        map.put(Part.Type.GREATER_THAN, CriteriaType.GREATER_THAN);
        map.put(Part.Type.CONTAINING, CriteriaType.CONTAINING);
        map.put(Part.Type.NOT_CONTAINING, CriteriaType.NOT_CONTAINING);
        map.put(Part.Type.ENDING_WITH, CriteriaType.ENDS_WITH);
        map.put(Part.Type.STARTING_WITH, CriteriaType.STARTS_WITH);
        map.put(Part.Type.GREATER_THAN_EQUAL, CriteriaType.GREATER_THAN_EQUAL);
        map.put(Part.Type.LESS_THAN, CriteriaType.LESS_THAN);
        map.put(Part.Type.LESS_THAN_EQUAL, CriteriaType.LESS_THAN_EQUAL);
        map.put(Part.Type.TRUE, CriteriaType.TRUE);
        map.put(Part.Type.FALSE, CriteriaType.FALSE);
        map.put(Part.Type.BETWEEN, CriteriaType.BETWEEN);
        map.put(Part.Type.REGEX, CriteriaType.REGEX);
        map.put(Part.Type.LIKE, CriteriaType.LIKE);
        map.put(Part.Type.NOT_LIKE, CriteriaType.NOT_LIKE);
        map.put(Part.Type.EXISTS, CriteriaType.EXISTS);
        map.put(Part.Type.NEAR, CriteriaType.NEAR);
        map.put(Part.Type.WITHIN, CriteriaType.WITHIN);

        PART_TREE_TYPE_TO_CRITERIA = Collections.unmodifiableMap(map);

        //todo spring data mongo driver also supports:
        // IgnoreCase, First3, Top10
    }

    CriteriaType(String sqlKeyword) {
        this.sqlKeyword = sqlKeyword;
    }

    public String getSqlKeyword() {
        return sqlKeyword;
    }

    /**
     * Check if PartType is NOT supported.
     *
     * @return True if unsupported, or false.
     */
    public static boolean isPartTypeUnSupported(@NonNull
        Part.Type partType) {
        return !isPartTypeSupported(partType);
    }

    /**
     * Check if PartType is supported.
     *
     * @return True if supported, or false.
     */
    public static boolean isPartTypeSupported(@NonNull Part.Type partType) {
        return PART_TREE_TYPE_TO_CRITERIA.containsKey(partType);
    }

    public static CriteriaType toCriteriaType(@NonNull Part.Type partType) {
        final CriteriaType criteriaType = PART_TREE_TYPE_TO_CRITERIA.get(partType);

        if (criteriaType == null) {
            throw new UnsupportedOperationException("Unsupported part type: " + partType);
        }

        return criteriaType;
    }

    /**
     * Check if CriteriaType operation is closure, with format of (A ops A -&gt; A).
     * Example: AND, OR.
     *
     * @return True if match, or false.
     */
    public static boolean isClosed(CriteriaType type) {
        switch (type) {
        case AND:
        case OR:
            return true;
        default:
            return false;
        }
    }

    /**
     * Check if CriteriaType operation is binary, with format of (A ops A -&gt; B).
     * Example: IS_EQUAL, AFTER.
     *
     * @return True if match, or false.
     */
    public static boolean isBinary(CriteriaType type) {
        switch (type) {
        case IN:
        case AND:
        case OR:
        case NOT:
        case IS_EQUAL:
        case BEFORE:
        case AFTER:
        case LESS_THAN:
        case LESS_THAN_EQUAL:
        case GREATER_THAN:
        case GREATER_THAN_EQUAL:
        case CONTAINING:
        case NOT_CONTAINING:
        case ENDS_WITH:
        case STARTS_WITH:
        case REGEX:
        case LIKE:
        case NOT_LIKE:
            return true;
        default:
            return false;
        }
    }

    /**
     * Check if CriteriaType operation is a function.
     *
     * @return True if match, or false.
     */
    public static boolean isFunction(CriteriaType type) {
        switch (type) {
        case CONTAINING:
        case NOT_CONTAINING:
        case ENDS_WITH:
        case STARTS_WITH:
        case IS_NULL:
        case IS_NOT_NULL:
        case REGEX:
        case LIKE:
        case NOT_LIKE:
            return true;
        default:
            return false;
        }
    }

    /**
     * Check if CriteriaType operation is unary, with format of (ops A -&gt; B).
     *
     * @return True if match, or false.
     */
    public static boolean isUnary(CriteriaType type) {
        switch (type) {
        case NOT_IN:
        case IS_NULL:
        case IS_NOT_NULL:
        case TRUE:
        case FALSE:
        case EXISTS:
        case NEAR:
        case WITHIN:
            return true;
        default:
            return false;
        }
    }
}
