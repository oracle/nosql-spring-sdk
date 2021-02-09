/*-
 * Copyright (c) 2020, 2021 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */
package com.oracle.nosql.spring.data.core.query;

import java.util.Map;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

public abstract class NosqlQuery {
    protected Sort sort = Sort.unsorted();
    protected Pageable pageable = Pageable.unpaged();
    protected Integer limit;
    protected boolean isCount;

    public NosqlQuery with(Sort sort) {
        if (sort.isSorted()) {
            this.sort = sort.and(this.sort);
        }

        return this;
    }

    public NosqlQuery with(@NonNull Pageable pageable) {
        Assert.notNull(pageable, "pageable should not be null");

        this.pageable = pageable;
        return this;
    }

    /**
     * Limit the number of returned rows to {@code limit}. Null if no limit.
     */
    public NosqlQuery limit(Integer limit) {
        this.limit = limit;
        return this;
    }

    public Integer getLimit() {
        return limit != null && limit > 0 ?
            limit :
            ( pageable.equals(Pageable.unpaged()) ?
                null :
                pageable.getPageSize());
    }

    public Sort getSort() {
        return sort;
    }

    public abstract String generateSql(String tableName,
        final Map<String, Object> params, String idPropertyName);

    public NosqlQuery setCount(boolean isCount) {
        this.isCount = isCount;
        return this;
    }

    public boolean isCount() {
        return isCount;
    }
}
