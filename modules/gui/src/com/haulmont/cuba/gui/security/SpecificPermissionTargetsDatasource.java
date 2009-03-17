/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Dmitry Abramov
 * Created: 16.03.2009 11:52:34
 * $Id$
 */

package com.haulmont.cuba.gui.security;

import com.haulmont.bali.datastruct.Tree;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.cuba.gui.data.DataService;
import com.haulmont.cuba.gui.data.DsContext;
import com.haulmont.cuba.gui.data.TreeDatasourceWrapper;
import com.haulmont.cuba.gui.ApplicationProperties;
import com.haulmont.cuba.gui.config.PermissionConfig;

public class SpecificPermissionTargetsDatasource extends TreeDatasourceWrapper {
    public SpecificPermissionTargetsDatasource(
            DsContext context, DataService dataservice,
                String id, MetaClass metaClass, String viewName)
    {
        super(context, dataservice, id, metaClass, viewName);
    }

    protected Tree<PermissionConfig.Target> loadTree() {
        return ApplicationProperties.getInstance().getPermissionConfig().getSpecific();
    }
}