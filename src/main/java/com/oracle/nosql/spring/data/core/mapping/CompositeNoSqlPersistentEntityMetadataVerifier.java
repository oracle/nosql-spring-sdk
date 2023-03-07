/*-
 * Copyright (c) 2020, 2023 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */

package com.oracle.nosql.spring.data.core.mapping;

import org.springframework.data.mapping.MappingException;
import org.springframework.util.Assert;

import java.util.Arrays;
import java.util.Collection;

public class CompositeNoSqlPersistentEntityMetadataVerifier implements NoSqlPersistentEntityMetadataVerifier {
    private Collection<NoSqlPersistentEntityMetadataVerifier> verifiers;

    public CompositeNoSqlPersistentEntityMetadataVerifier() {
        this(Arrays.asList(new NoSqlKeyClassEntityMetadataVerifier(),
                new BasicNoSqlPersistentEntityMetadataVerifier()));
    }

    public CompositeNoSqlPersistentEntityMetadataVerifier(Collection<NoSqlPersistentEntityMetadataVerifier> verifiers) {
        Assert.notNull(verifiers, "Verifiers must not be null");
        this.verifiers = verifiers;
    }


    @Override
    public void verify(NosqlPersistentEntity<?> entity) throws MappingException {
        verifiers.forEach(verifier -> verifier.verify(entity));
    }
}
