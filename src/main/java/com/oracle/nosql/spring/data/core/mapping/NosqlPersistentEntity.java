/*-
 * Copyright (c) 2020, 2021 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */
package com.oracle.nosql.spring.data.core.mapping;

import org.springframework.data.mapping.PersistentEntity;

public interface NosqlPersistentEntity<T>
    extends PersistentEntity<T, NosqlPersistentProperty> {

}
