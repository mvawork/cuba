/*
 * Copyright (c) 2008-2016 Haulmont.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.haulmont.restapi.common;

import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.chile.core.model.MetaProperty;
import com.haulmont.cuba.core.entity.BaseEntityInternalAccess;
import com.haulmont.cuba.core.entity.BaseGenericIdEntity;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.EntityAttributeVisitor;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.core.global.MetadataTools;
import com.haulmont.cuba.core.global.Security;
import com.haulmont.restapi.exception.RestAPIException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Arrays;

/**
 */
@Component("cuba_RestControllerUtils")
public class RestControllerUtils {

    @Inject
    protected Metadata metadata;

    @Inject
    protected Security security;

    @Inject
    protected MetadataTools metadataTools;

    /**
     * Finds metaClass by entityName. Throws a RestAPIException if metaClass not found
     */
    public MetaClass getMetaClass(String entityName) {
        MetaClass metaClass = metadata.getClass(entityName);
        if (metaClass == null) {
            throw new RestAPIException("MetaClass not found",
                    String.format("MetaClass %s not found", entityName),
                    HttpStatus.NOT_FOUND);
        }

        return metaClass;
    }

    /**
     * By default entity is loaded from the middleware with the attributes that are not allowed for reading according
     * to roles permissions. This methods removes attributes not allowed for the user.
     * @param entity the entity. After the method is executed forbidden attributes will be cleaned.
     */
    public void applyAttributesSecurity(Entity entity) {
        metadataTools.traverseAttributes(entity, new FillingInaccessibleAttributesVisitor());
    }

    private class FillingInaccessibleAttributesVisitor implements EntityAttributeVisitor {

        @Override
        public boolean skip(MetaProperty property) {
            return metadataTools.isTransient(property);
        }

        @Override
        public void visit(Entity entity, MetaProperty property) {
            MetaClass metaClass = metadata.getClassNN(entity.getClass());
            if (!security.isEntityAttrReadPermitted(metaClass, property.getName())) {
                addInaccessibleAttribute((BaseGenericIdEntity) entity, property.getName());
                if (!metadataTools.isSystem(property) && !property.isReadOnly()) {
                    // Using reflective access to field because the attribute can be unfetched if loading not partial entities,
                    // which is the case when in-memory constraints exist
                    BaseEntityInternalAccess.setValue((BaseGenericIdEntity) entity, property.getName(), null);
                }
            }
        }
    }

    private void addInaccessibleAttribute(BaseGenericIdEntity entity, String property) {
        String[] attributes = BaseEntityInternalAccess.getInaccessibleAttributes(entity);
        attributes = attributes == null ? new String[1] : Arrays.copyOf(attributes, attributes.length + 1);
        attributes[attributes.length - 1] = property;
        BaseEntityInternalAccess.setInaccessibleAttributes(entity, attributes);
    }
}
