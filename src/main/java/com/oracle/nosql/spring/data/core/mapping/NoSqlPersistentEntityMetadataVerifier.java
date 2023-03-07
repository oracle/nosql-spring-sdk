/*-
 * Copyright (c) 2020, 2023 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */

package com.oracle.nosql.spring.data.core.mapping;

import org.springframework.data.mapping.MappingException;

public interface NoSqlPersistentEntityMetadataVerifier {
    /**
     * Performs verification on the Persistent Entity to ensure all markers and marker combinations are valid.
     *
     * @param entity the entity to verify, must not be {@literal null}.
     */
    void verify(NosqlPersistentEntity<?> entity) throws MappingException;
}
