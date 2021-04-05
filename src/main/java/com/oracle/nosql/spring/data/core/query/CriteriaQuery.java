/*-
 * Copyright (c) 2020, 2021 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */
package com.oracle.nosql.spring.data.core.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import oracle.nosql.driver.values.LongValue;

import com.oracle.nosql.spring.data.core.NosqlTemplate;
import com.oracle.nosql.spring.data.core.convert.MappingNosqlConverter;
import com.oracle.nosql.spring.data.core.mapping.NosqlPersistentEntity;
import com.oracle.nosql.spring.data.core.mapping.NosqlPersistentProperty;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.geo.Circle;
import org.springframework.data.mapping.PersistentPropertyPath;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.repository.query.ReturnedType;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

public class CriteriaQuery extends NosqlQuery {

    private final Criteria criteria;
    private final MappingContext<?, NosqlPersistentProperty> mappingContext;
    private boolean isDistinct;
    // Used for projection
    private ReturnedType returnedType;

    public CriteriaQuery(@Nullable Criteria criteria,
        MappingContext<?, NosqlPersistentProperty> mappingContext) {
        this.criteria = criteria;
        this.mappingContext = mappingContext;
    }


    public CriteriaQuery with(Sort sort) {
        super.with(sort);
        return this;
    }

    public CriteriaQuery with(@NonNull Pageable pageable) {
        super.with(pageable);
        return this;
    }

    /**
     * Limit the number of returned rows to {@code limit}.
     */
    public CriteriaQuery limit(Integer limit) {
        super.limit(limit);
        return this;
    }

    public CriteriaQuery setCount(boolean isCount) {
        super.setCount(isCount);
        return this;
    }


    public Criteria getCriteria() {
        return criteria;
    }

    public Pageable getPageable() {
        return pageable;
    }

    private boolean hasKeywordOr() {
        // If there is OR keyword in DocumentQuery, the top node of Criteria
        // must be OR type.
        return criteria != null && criteria.getType() == CriteriaType.OR;
    }

    public Optional<Criteria> getCriteriaByType(
        @NonNull CriteriaType criteriaType) {
        return getCriteriaByType(criteriaType, criteria);
    }

    private Optional<Criteria> getCriteriaByType(
        @NonNull CriteriaType criteriaType,
        @NonNull Criteria criteria) {
        if (criteria.getType().equals(criteriaType)) {
            return Optional.of(criteria);
        }

        for (final Criteria subCriteria: criteria.getSubCriteria()) {
            if (getCriteriaByType(criteriaType, subCriteria).isPresent()) {
                return Optional.of(subCriteria);
            }
        }

        return Optional.empty();
    }

    private Optional<Criteria> getSubjectCriteria(@NonNull Criteria criteria,
        @NonNull String keyName) {

        if (keyName.equals(criteria.getSubject())) {
            return Optional.of(criteria);
        }

        final List<Criteria> subCriteriaList = criteria.getSubCriteria();

        for (final Criteria c : subCriteriaList) {
            final Optional<Criteria> subjectCriteria =
                getSubjectCriteria(c, keyName);

            if (subjectCriteria.isPresent()) {
                return subjectCriteria;
            }
        }

        return Optional.empty();
    }

    public String generateSql(String tableName,
        final Map<String, Object> params, String idPropertyName) {

        String sql = "select " +
            (isDistinct ? "distinct " : "") +
            (isCount() ? "count(*)" : generateProjection(idPropertyName) ) +
            " from " + tableName + " as t";

        String whereClause = generateSql(criteria, params);

        if (StringUtils.hasText(whereClause)) {
            sql += " where " + whereClause;
        }

        if (getSort().isSorted()) {
            sql += " ORDER BY ";
            sql += getSort().stream().map(order -> (
                      getSqlField(order.getProperty(),
                          order.getProperty().equals(idPropertyName)) +
                          (order.isAscending() ? " ASC" : " DESC")))
                .collect(Collectors.joining(","));
        }

        Integer limit = getLimit();
        if (limit != null) {
            sql += " LIMIT $kv_limit_";

            params.put("$kv_limit_", new LongValue(getLimit()));
        }

        if (getPageable().isPaged()) {
            sql += " OFFSET $kv_offset_";

            params.put("$kv_offset_", new LongValue(getPageable().getOffset()));
        }

        if (!params.isEmpty()) {
            String paramDecl = params.keySet().stream()
                .map(key -> key + " " +
                    MappingNosqlConverter.toNosqlSqlType(params.get(key)))
                .collect(Collectors.joining("; ", "declare ", "; "));

            sql = paramDecl + sql;
        }

        return sql;
    }

    private String generateSql(@Nullable Criteria crt,
        @NonNull Map<String, Object> parameters) {

        if (crt == null) {
            return "";
        }

        switch (crt.getType()) {
        /* unary */
        case ALL:
            return "";
        case TRUE:
            if (StringUtils.hasText(crt.getSubject())) {
                return getSqlField(crt) + " = true";
            }
            return "true";
        case FALSE:
            if (StringUtils.hasText(crt.getSubject())) {
                return getSqlField(crt) + " = false";
            }
            return "false";
        case IS_NULL:
            if (StringUtils.hasText(crt.getSubject())) {
                return getSqlField(crt) + " = null";
            }
            return " is null";
        case IS_NOT_NULL:
            if (StringUtils.hasText(crt.getSubject())) {
                return getSqlField(crt) + " != null";
            }
            return " is not null";
        case EXISTS:
            if (StringUtils.hasText(crt.getSubject())) {
                return  "EXISTS " + getSqlField(crt);
            }
            return " EXISTS ";


        /* binary */
        case OR:
        case AND:
            Assert.isTrue(crt.getSubCriteria().size() == 2,
                "AND, OR criteria should have two children.");
            String op1 = generateSql(crt.getSubCriteria().get(0), parameters);
            String op2 = generateSql(crt.getSubCriteria().get(1), parameters);

            return " (" + op1 + " " + crt.getType().getSqlKeyword() + " " +
                op2 + ") ";


        case IS_EQUAL:
        case NOT:
        case LESS_THAN:
        case GREATER_THAN:
        case LESS_THAN_EQUAL:
        case GREATER_THAN_EQUAL:
        case AFTER:
        case BEFORE:
        case STARTS_WITH:
        case ENDS_WITH:
        case CONTAINING:
        case NOT_CONTAINING:
        case REGEX:
        case LIKE:
        case NOT_LIKE:
            Assert.isTrue(crt.getSubjectValues().size() == 1, "Binary " +
                "criteria should have only one subject value");
            Assert.isTrue(CriteriaType.isBinary(crt.getType()), "Criteria " +
                "type should be binary operation");

            String subject = getSqlFieldWithCast(crt);
            final Object subjectValue = crt.getSubjectValues().get(0);
            String parameter = generateQueryParameter(crt.getSubject(),
                parameters);

            parameters.put(parameter, subjectValue);

            if (crt.isIgnoreCase()) {
                subject = "lower(" + subject + ")";
                parameter = "lower(" + parameter + ")";
            }

            if (CriteriaType.isFunction(crt.getType())) {
                return String.format("%s(%s, %s)",
                    crt.getType().getSqlKeyword(), subject, parameter);
            } else {
                return String.format("%s %s %s", subject,
                    crt.getType().getSqlKeyword(), parameter);
            }

        /* functions */
        case IN:
        case NOT_IN:
            Assert.isTrue(crt.getSubjectValues().size() == 1,
                "Criteria should have only one subject value");
            if (!(crt.getSubjectValues().get(0) instanceof Collection)) {
                throw new IllegalArgumentException("IN keyword requires " +
                    "Collection type in parameters");
            }

            String inParameter = generateQueryParameter(crt.getSubject(),
                parameters);
            final Object inSubjectValue = crt.getSubjectValues().get(0);
            parameters.put(inParameter, inSubjectValue);
            inParameter = inParameter + "[]";

            String sqlFieldWithCast = getSqlFieldWithCast(crt);

            if (crt.isIgnoreCase()) {
                sqlFieldWithCast = "lower(" + sqlFieldWithCast + ")";
                inParameter = "seq_transform(" + inParameter + ", lower($))";
            }

            String result = String.format("%s %s (%s)",
                sqlFieldWithCast,
                crt.getType().getSqlKeyword(),
                inParameter);

            if (crt.getType() == CriteriaType.NOT_IN) {
                result = "NOT (" + result + ")";
            }
            return result;

        case BETWEEN:
            Assert.isTrue(crt.getSubjectValues().size() == 2,
                "Between criteria should have two subject values");

            final Object bwSubjectValue1 = crt.getSubjectValues().get(0);
            final Object bwSubjectValue2 = crt.getSubjectValues().get(1);
            String bwParameter1 = generateQueryParameter(crt.getSubject(),
                parameters);
            parameters.put(bwParameter1, bwSubjectValue1);
            String bwParameter2 = generateQueryParameter(crt.getSubject(),
                parameters);
            parameters.put(bwParameter2, bwSubjectValue2);
            String bwField = getSqlFieldWithCast(crt);

            if (crt.isIgnoreCase()) {
                bwField = "lower(" + bwField + ")";
                bwParameter1 = "lower(" + bwParameter1 + ")";
                bwParameter2 = "lower(" + bwParameter2 + ")";
            }

            return String.format("(%s >= %s AND %s <= %s)",
                bwField, bwParameter1, bwField, bwParameter2);

        case NEAR:
            Assert.isTrue(crt.getSubjectValues().size() == 1,
                "Near criteria should have 1 subject values.");

            final Object nSubjectValue1 = crt.getSubjectValues().get(0);
            if (nSubjectValue1 instanceof Circle) {
                Circle circle = (Circle) nSubjectValue1;
                final String nGeoShape =
                    generateQueryParameter(crt.getSubject() + "_shape",
                    parameters);
                parameters.put(nGeoShape, circle.getCenter());
                final String nGeoDist =
                    generateQueryParameter(crt.getSubject() + "_dist",
                    parameters);
                parameters.put(nGeoDist, circle.getRadius().getNormalizedValue());
                final String nField = getSqlFieldWithCast(crt);

                return String.format("%s(%s, %s, %s)",
                    crt.getType().getSqlKeyword(),
                    nField,
                    nGeoShape,
                    nGeoDist);
            } else {
                // can't add support for polygon because near with distance 0
                throw new IllegalArgumentException("Unsupported type for Near" +
                    " criteria: " + nSubjectValue1.getClass().getName() + ". " +
                    "Use org.springframework.data.geo.Circle instead.");
            }

        case WITHIN:
            Assert.isTrue(crt.getSubjectValues().size() == 1,
                "Within criteria should have one subject values");

            final Object wSubjectValue1 = crt.getSubjectValues().get(0);
            final String wGeoShape = generateQueryParameter(crt.getSubject(),
                parameters);
            parameters.put(wGeoShape, wSubjectValue1);
            final String wField = getSqlFieldWithCast(crt);

            return String.format("%s(%s, %s)",
                crt.getType().getSqlKeyword(),
                wField,
                wGeoShape);

        default:
            throw new IllegalArgumentException("Unsupported Criteria type: " +
                crt.getType());
        }
    }

    private String getSqlFieldWithCast(@NonNull Criteria crt) {
        PersistentPropertyPath<NosqlPersistentProperty> path =
            mappingContext.getPersistentPropertyPath(crt.getPart().getProperty());
        NosqlPersistentProperty property = path.getLeafProperty();

        return getSqlFieldWithCast(crt.getSubject(), property);
    }

    private String getSqlFieldWithCast(@NonNull String field,
        NosqlPersistentProperty property) {
        String result =  getSqlField(field, property.isIdProperty());

        if (requiresTimestampCast(property)) {
            result = "cast(" + result + " as " +
                "Timestamp)";
        }
        return result;
    }

    private String getSqlField(@NonNull Criteria crt) {
        PersistentPropertyPath<NosqlPersistentProperty> path =
            mappingContext.getPersistentPropertyPath(crt.getPart().getProperty());
        NosqlPersistentProperty property = path.getLeafProperty();

        return getSqlField(crt.getSubject(), property);
    }

    private String getSqlField(@NonNull String field,
        NosqlPersistentProperty property) {
        return getSqlField(field, property.isIdProperty());
    }

    private String getSqlField(@NonNull String field, boolean isKey) {
        if (isKey) {
            return "t." + field;
        }

        return "t." + NosqlTemplate.JSON_COLUMN + "." + field;
    }


    /* Date, Instant and Timestamp fields are mapped to Nosql Timestamp which
     is represented in a JSON column as string. This requires casting to
     Timestamp when comparing to a Timestamp value.
     */
    private boolean requiresTimestampCast(NosqlPersistentProperty property) {
        NosqlPersistentProperty.TypeCode typeCode = property.getTypeCode();
        return !property.isIdProperty() &&
            ( typeCode == NosqlPersistentProperty.TypeCode.DATE ||
              typeCode == NosqlPersistentProperty.TypeCode.INSTANT ||
              typeCode == NosqlPersistentProperty.TypeCode.TIMESTAMP);
    }

    private String generateQueryParameter(@NonNull String field,
        @NonNull Map<String, Object> parameters) {

        String rootName = "$p_" + field.replace(".", "_");
        String potentialName = rootName;
        int i = 0;
        while (parameters.containsKey(potentialName)) {
            if (i > 1000000) {
                throw new IllegalStateException("Too many tries to find a " +
                    "valid sql parameter name.");
            }
            potentialName = rootName + ++i;
        }

        return potentialName;
    }

    public CriteriaQuery setDistinct(boolean isDistinct) {
        this.isDistinct = isDistinct;
        return this;
    }

    public CriteriaQuery project(ReturnedType returnedType) {
        this.returnedType = returnedType;

        return this;
    }

    private String generateProjection(String idPropertyName) {
        if (returnedType == null || !returnedType.isProjecting()) {
            return "*";
        }

        List<String> inputProperties = new ArrayList<>();

        final NosqlPersistentEntity<?> entity = (NosqlPersistentEntity<?>)
            mappingContext.getPersistentEntity(returnedType.getReturnedType());
        entity.doWithProperties(
            (PropertyHandler<NosqlPersistentProperty>) prop -> {
                if (prop.isWritable()) {
                    inputProperties.add(prop.getName());
                }
            });

        if (inputProperties.isEmpty()) {
            throw new IllegalArgumentException("There are no accessible " +
                "fields in returned type: " +
                returnedType.getReturnedType().getName());
        }

        List<String> keyFields = new ArrayList<>();
        List<String> nonKeyFields = new ArrayList<>();

        inputProperties
            .stream()
            .forEach( prop -> {
                NosqlPersistentProperty pp = mappingContext
                    .getPersistentPropertyPath(prop,
                        returnedType.getReturnedType()).getBaseProperty();

                String field = getSqlField(prop, pp);
                if (pp.getName().equals(idPropertyName)) {
                    keyFields.add(getSqlField(pp.getName(), true));
                } else {
                    nonKeyFields.add(field);
                }
            });

        String nonKeysProj = nonKeyFields.stream()
            .map(f -> "'" + f.substring(f.lastIndexOf('.') + 1) +
                "': " + f)
            .collect(Collectors.joining(", ", "{",
                "} as " + NosqlTemplate.JSON_COLUMN));

        return Stream.concat(keyFields.stream(),
            nonKeyFields.isEmpty() ? Stream.empty() : Stream.of(nonKeysProj))
            .collect(Collectors.joining(", "));
    }
}
