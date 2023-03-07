/*-
 * Copyright (c) 2020, 2023 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */

package com.oracle.nosql.spring.data.core.mapping;

import com.oracle.nosql.spring.data.repository.support.NosqlEntityInformation;
import org.springframework.data.mapping.MappingException;

public class NoSqlKeyClassEntityMetadataVerifier implements NoSqlPersistentEntityMetadataVerifier {
    @Override
    public void verify(NosqlPersistentEntity<?> entity) throws MappingException {
        if (entity.getType().isInterface() || !entity.isCompositeKey()) {
            return;
        }
        // Ensure entity is not both a @NoSqlTable and a @NoSqlKeyClass
        if (entity.isAnnotationPresent(NosqlTable.class)) {
            throw new MappingException(String.format("Entity cannot be of " +
                            "type @%s and @%s",
                    NosqlTable.class.getSimpleName(),
                    NoSqlKeyClass.class.getSimpleName()));
        }
        //List<NosqlPersistentProperty> noSqlKeys = new ArrayList<>();
        entity.forEach(property -> {
            if (property.isWritable() &&
                    !NosqlEntityInformation.isSimpleType(property.getType())) {
                throw new MappingException(String.format(
                        "field '%s' must be one of type java.lang.String," +
                                " int, java.lang.Integer, long, java.lang" +
                                ".Long," +
                                " java.math.BigInteger, java.math.BigDecimal," +
                                " java.sql.Timestamp, java.util.Date or" +
                                " java.time.Instant in %s",
                        property.getName(),
                        entity.getName())
                );
            }
           /* if (property.isNoSqlKey()) {
                noSqlKeys.add(property);
            }*/
        });
        /*if (noSqlKeys.isEmpty()) {
            throw new MappingException(String.format("Composite key class %s " +
                            "must have at least one field with @%s annotation",
                    entity.getName(), NoSqlKey.class.getSimpleName()));
        }*/
    }

}
