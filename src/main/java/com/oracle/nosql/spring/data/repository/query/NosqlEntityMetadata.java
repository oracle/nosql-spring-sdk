/*-
 * Copyright (c) 2020, 2021 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */
package com.oracle.nosql.spring.data.repository.query;

import com.oracle.nosql.spring.data.repository.support.NosqlEntityInformation;

import org.springframework.data.repository.core.EntityMetadata;

public interface NosqlEntityMetadata<T> extends EntityMetadata<T> {

    String getTableName();

    NosqlEntityInformation<T, ?> getNosqlEntityInformation();
}