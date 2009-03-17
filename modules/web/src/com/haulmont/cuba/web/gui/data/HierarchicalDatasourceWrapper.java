/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Konstantin Krivopustov
 * Created: 29.01.2009 13:26:42
 *
 * $Id$
 */
package com.haulmont.cuba.web.gui.data;

import com.haulmont.chile.core.model.Instance;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.gui.data.HierarchicalDatasource;
import com.itmill.toolkit.data.Container;

import java.util.Collection;

public class HierarchicalDatasourceWrapper
    extends
        CollectionDatasourceWrapper
    implements
        Container.Hierarchical
{
    private String parentPropertyName;

    public HierarchicalDatasourceWrapper(HierarchicalDatasource datasource)
    {
        super(datasource);
        this.parentPropertyName = datasource.getHierarchyPropertyName();
    }

    public Collection getChildren(Object itemId) {
        return ((HierarchicalDatasource<Entity, Object>) datasource).getChildren(itemId);
    }

    public Object getParent(Object itemId) {
        return ((HierarchicalDatasource<Entity, Object>) datasource).getParent(itemId);
    }

    public Collection rootItemIds() {
        return ((HierarchicalDatasource<Entity, Object>) datasource).getRootItemIds();
    }

    public boolean setParent(Object itemId, Object newParentId) throws UnsupportedOperationException {
        Instance item = (Instance) datasource.getItem(itemId);
        if (item != null) {
            item.setValue(parentPropertyName, datasource.getItem(newParentId));
            return true;
        }
        return false;
    }

    public boolean areChildrenAllowed(Object itemId) {
        return true;
    }

    public boolean setChildrenAllowed(Object itemId, boolean areChildrenAllowed) throws UnsupportedOperationException {
        return true;
    }

    public boolean isRoot(Object itemId) {
        return ((HierarchicalDatasource<Entity, Object>) datasource).isRoot(itemId);
    }

    public boolean hasChildren(Object itemId) {
        return ((HierarchicalDatasource<Entity, Object>) datasource).hasChildren(itemId);
    }
}
