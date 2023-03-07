/*-
 * Copyright (c) 2020, 2023 Oracle and/or its affiliates.  All rights reserved.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 *  https://oss.oracle.com/licenses/upl/
 */

package com.oracle.nosql.spring.data.core.mapping;

import com.oracle.nosql.spring.data.Constants;
import com.oracle.nosql.spring.data.core.NosqlTemplateBase;
import com.oracle.nosql.spring.data.repository.support.NosqlEntityInformation;
import org.springframework.data.annotation.Id;
import org.springframework.data.mapping.MappingException;

import java.util.ArrayList;
import java.util.List;

public class BasicNoSqlPersistentEntityMetadataVerifier implements NoSqlPersistentEntityMetadataVerifier {
    @Override
    public void verify(NosqlPersistentEntity<?> entity) throws MappingException {
        List<MappingException> exceptions = new ArrayList<>();

        if (entity.getType().isInterface() || !entity.isAnnotationPresent(NosqlTable.class)) {
            return;
        }

        // Ensure entity is not both a @NoSqlTable and a @NoSqlKeyClass
        if (entity.isCompositeKey()) {
            throw new MappingException(String.format(
                    "Entity %s cannot be of type @%s and @%s",
                    entity.getName(),
                    NosqlTable.class.getSimpleName(),
                    NoSqlKeyClass.class.getSimpleName()));
        }

        List<NosqlPersistentProperty> idProperties = new ArrayList<>();
        List<NosqlPersistentProperty> noSqlIdProperties = new ArrayList<>();
        boolean hasIdField = false;


        // Parse entity properties
        for (NosqlPersistentProperty property : entity) {
            if (property.isAnnotationPresent(Id.class)) {
                idProperties.add(property);
            }
            if (property.isAnnotationPresent(NosqlId.class)) {
                noSqlIdProperties.add(property);
            }
            if (property.getName().equals(Constants.ID_PROPERTY_NAME)) {
                hasIdField = true;
            }
        }

        if (idProperties.isEmpty() && noSqlIdProperties.isEmpty() && !hasIdField) {
            throw new MappingException(String.format(
                    "Entity should contain @Id or @NoSqlId annotated field " +
                            "or field named id: %s", entity.getName()));
        }

        if (idProperties.size() > 1) {
            throw new MappingException(String.format(
                    "Only one field can be with @Id annotation in entity %s",
                    entity.getName()));
        }

        if (noSqlIdProperties.size() > 1) {
            throw new MappingException(String.format(
                    "Only one field can be with @NoSqlId annotation in entity" +
                            " %s", entity.getName()));
        }

        if (!idProperties.isEmpty() && !noSqlIdProperties.isEmpty()) {
            throw new MappingException(String.format(
                    "Only one of the @Id or @NoSqlId annotation can be used " +
                            "on entity %s", entity.getName()));
        }

        NosqlPersistentProperty idProperty = entity.getIdProperty();

        if (!NosqlEntityInformation.isSimpleType(idProperty.getType()) &&
                !idProperty.isCompositeKey()) {
            throw new MappingException(String.format("Composite key type %s " +
                            "must be annotated with @%s of entity %s",
                    idProperty.getName(), NoSqlKeyClass.class.getSimpleName()
                    , entity.getName()));
        }

        if (NosqlTemplateBase.JSON_COLUMN.equals(entity.getIdProperty().getName())) {
            throw new MappingException("Id field cannot be named '" +
                    NosqlTemplateBase.JSON_COLUMN + "' in " + entity.getName());
        }

    }
}
