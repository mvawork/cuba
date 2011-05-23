/*
 * Copyright (c) 2011 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.
 */

package com.haulmont.cuba.desktop.gui.data;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.haulmont.chile.core.model.Instance;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.MessageProvider;
import com.haulmont.cuba.gui.AppConfig;
import com.haulmont.cuba.gui.components.CaptionMode;
import com.haulmont.cuba.gui.data.CollectionDatasource;
import com.haulmont.cuba.gui.data.HierarchicalDatasource;
import com.haulmont.cuba.gui.data.impl.CollectionDsListenerAdapter;

import javax.annotation.Nullable;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * <p>$Id$</p>
 *
 * @author krivopustov
 */
public class TreeModelAdapter implements TreeModel {

    private HierarchicalDatasource<Entity<Object>, Object> datasource;

    private Object rootNode = MessageProvider.getMessage(AppConfig.getMessagesPack(), "TreeModelAdapter.rootNode");

    private List<TreeModelListener> listeners = new ArrayList<TreeModelListener>();

    private CaptionMode captionMode;
    private String captionProperty;

    public TreeModelAdapter(HierarchicalDatasource datasource, CaptionMode captionMode, String captionProperty) {
        this.datasource = datasource;
        this.captionMode = captionMode;
        this.captionProperty = captionProperty;

        datasource.addListener(
                new CollectionDsListenerAdapter() {
                    @Override
                    public void collectionChanged(CollectionDatasource ds, Operation operation) {
                        for (TreeModelListener listener : listeners) {
                            TreeModelEvent ev = new TreeModelEvent(this, new Object[]{getRoot()});
                            listener.treeStructureChanged(ev);
                        }
                    }
                }
        );
    }

    @Override
    public Object getRoot() {
        Collection rootItemIds = datasource.getRootItemIds();
        if (rootItemIds.isEmpty())
            return null;
        else if (rootItemIds.size() == 1) {
            Object itemId = rootItemIds.iterator().next();
            return new Node(datasource.getItem(itemId));
        } else {
            return rootNode;
        }
    }

    @Override
    public Object getChild(Object parent, int index) {
        Collection<Object> childrenIds;
        if (parent == rootNode) {
            childrenIds = datasource.getRootItemIds();
        } else {
            childrenIds = datasource.getChildren(((Node) parent).getEntity().getId());
        }
        Object id = Iterables.get(childrenIds, index);
        return new Node(datasource.getItem(id));
    }

    @Override
    public int getChildCount(Object parent) {
        if (parent == rootNode) {
            return datasource.getRootItemIds().size();
        } else {
            Entity entity = ((Node) parent).getEntity();
            Collection<Object> childrenIds = datasource.getChildren(entity.getId());
            return childrenIds.size();
        }
    }

    @Override
    public boolean isLeaf(Object node) {
        if (node == rootNode) {
            return false;
        } else {
            Entity entity = ((Node) node).getEntity();
            Collection<Object> childrenIds = datasource.getChildren(entity.getId());
            return childrenIds.size() == 0;
        }
    }

    @Override
    public void valueForPathChanged(TreePath path, Object newValue) {
    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        if (parent == null || child == null)
            return -1;

        Collection<Object> childrenIds;
        if (parent == rootNode) {
            childrenIds = datasource.getRootItemIds();
        } else {
            Entity entity = ((Node) parent).getEntity();
            childrenIds = datasource.getChildren(entity.getId());
        }
        final Entity childEntity = ((Node) child).getEntity();
        return Iterables.indexOf(childrenIds, new Predicate<Object>() {
            public boolean apply(@Nullable Object id) {
                return childEntity.getId().equals(id);
            }
        });
    }

    @Override
    public void addTreeModelListener(TreeModelListener l) {
        if (!listeners.contains(l))
            listeners.add(l);
    }

    @Override
    public void removeTreeModelListener(TreeModelListener l) {
        listeners.remove(l);
    }

    public void setCaptionMode(CaptionMode captionMode) {
        this.captionMode = captionMode;
    }

    public void setCaptionProperty(String captionProperty) {
        this.captionProperty = captionProperty;
    }

    public Node createNode(Entity entity) {
        return new Node(entity);
    }

    public class Node {
        private Entity entity;

        public Node(Entity entity) {
            if (entity == null)
                throw new IllegalArgumentException("item must not be null");
            this.entity = entity;
        }

        public Entity getEntity() {
            return entity;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Node node = (Node) o;

            return entity.equals(node.entity);

        }

        @Override
        public int hashCode() {
            return entity.hashCode();
        }

        @Override
        public String toString() {
            Object value;
            if (captionMode.equals(CaptionMode.ITEM) || !(entity instanceof Instance)) {
                value = entity;
            } else {
                value = ((Instance) entity).getValue(captionProperty);
            }
            return value == null ? "" : value.toString();
        }
    }
}
