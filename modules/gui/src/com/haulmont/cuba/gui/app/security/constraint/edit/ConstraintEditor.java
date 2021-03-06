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
 *
 */

package com.haulmont.cuba.gui.app.security.constraint.edit;

import com.haulmont.bali.util.Dom4j;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.core.global.filter.GroovyGenerator;
import com.haulmont.cuba.core.global.filter.SecurityJpqlGenerator;
import com.haulmont.cuba.core.sys.jpql.ErrorRec;
import com.haulmont.cuba.core.sys.jpql.JpqlSyntaxException;
import com.haulmont.cuba.gui.WindowManager;
import com.haulmont.cuba.gui.WindowManagerProvider;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.autocomplete.JpqlSuggestionFactory;
import com.haulmont.cuba.gui.components.autocomplete.Suggestion;
import com.haulmont.cuba.gui.components.filter.ConditionsTree;
import com.haulmont.cuba.gui.components.filter.FakeFilterSupport;
import com.haulmont.cuba.gui.components.filter.FilterParser;
import com.haulmont.cuba.gui.components.filter.Param;
import com.haulmont.cuba.gui.components.filter.edit.FilterEditor;
import com.haulmont.cuba.gui.config.WindowConfig;
import com.haulmont.cuba.gui.config.WindowInfo;
import com.haulmont.cuba.gui.data.CollectionDatasource;
import com.haulmont.cuba.gui.data.Datasource;
import com.haulmont.cuba.gui.data.DsBuilder;
import com.haulmont.cuba.security.app.UserManagementService;
import com.haulmont.cuba.security.entity.Constraint;
import com.haulmont.cuba.security.entity.ConstraintCheckType;
import com.haulmont.cuba.security.entity.ConstraintOperationType;
import com.haulmont.cuba.security.entity.FilterEntity;
import org.apache.commons.lang.StringUtils;
import org.dom4j.Element;

import javax.inject.Inject;
import java.util.*;

import static java.util.Arrays.asList;

public class ConstraintEditor extends AbstractEditor<Constraint> {

    @Inject
    protected LookupField entityName;

    @Inject
    protected SourceCodeEditor joinClause;

    @Inject
    protected SourceCodeEditor whereClause;

    @Inject
    protected SourceCodeEditor groovyScript;

    @Inject
    protected Label groovyScriptLabel;

    @Inject
    protected Label joinClauseLabel;

    @Inject
    protected Label whereClauseLabel;

    @Inject
    protected LinkButton whereClauseHelp;

    @Inject
    protected LinkButton joinClauseHelp;

    @Inject
    protected LinkButton groovyScriptHelp;

    @Inject
    protected TextField code;

    @Inject
    protected Label codeLabel;

    @Inject
    protected LookupField operationType;

    @Inject
    protected Datasource<Constraint> constraint;

    @Inject
    protected Metadata metadata;

    @Inject
    protected ExtendedEntities extendedEntities;

    @Inject
    protected WindowManagerProvider windowManagerProvider;

    @Inject
    protected Button testConstraint;

    @Inject
    protected WindowConfig windowConfig;

    @Inject
    protected LookupField type;

    @Inject
    protected UserManagementService userManagementService;

    protected Map<Object, String> entities;

    protected static final String SESSION_PREFIX = "session$";

    @Override
    public void postInit() {
        Map<String, Object> options = new TreeMap<>();
        MessageTools messageTools = AppBeans.get(MessageTools.NAME);
        entities = new HashMap<>();
        for (MetaClass metaClass : metadata.getTools().getAllPersistentMetaClasses()) {
            if (extendedEntities.getExtendedClass(metaClass) == null) {
                MetaClass mainMetaClass = extendedEntities.getOriginalOrThisMetaClass(metaClass);
                String originalName = mainMetaClass.getName();
                options.put(messageTools.getEntityCaption(metaClass) + " (" + metaClass.getName() + ")", originalName);
                entities.put(originalName, metaClass.getName());
            }
        }
        entityName.setOptionsMap(options);

        joinClause.setSuggester((source, text, cursorPosition) -> requestHint(joinClause, text, cursorPosition));
        whereClause.setSuggester((source, text, cursorPosition) -> requestHint(whereClause, text, cursorPosition));

        setupVisibility();
        constraint.addItemPropertyChangeListener(e -> {
            if ("checkType".equals(e.getProperty()) || "operationType".equals(e.getProperty())) {
                setupVisibility();
            }
        });
    }

    protected void setupVisibility() {
        Constraint item = (Constraint) getItem();
        asList(groovyScript, groovyScriptLabel, groovyScriptHelp)
                .forEach(component -> component.setVisible(item.getCheckType().memory()));
        asList(joinClause, joinClauseLabel, joinClauseHelp, whereClause, whereClauseLabel, whereClauseHelp)
                .forEach(component -> component.setVisible(item.getCheckType().database() &&
                        item.getOperationType() != ConstraintOperationType.CREATE &&
                        item.getOperationType() != ConstraintOperationType.DELETE &&
                        item.getOperationType() != ConstraintOperationType.UPDATE));
        asList(code, codeLabel)
                .forEach(component -> component.setVisible(item.getOperationType() == ConstraintOperationType.CUSTOM));

        if (item.getOperationType() != ConstraintOperationType.ALL &&
                item.getOperationType() != ConstraintOperationType.CUSTOM &&
                item.getOperationType() != ConstraintOperationType.READ) {
            item.setCheckType(ConstraintCheckType.MEMORY);
            type.setEnabled(false);
        } else {
            type.setEnabled(true);
        }

        if (item.getCheckType() == ConstraintCheckType.DATABASE_AND_MEMORY
                || item.getCheckType() == ConstraintCheckType.DATABASE) {
            testConstraint.setVisible(true);
        } else {
            testConstraint.setVisible(false);
        }
    }

    protected List<Suggestion> requestHint(SourceCodeEditor sender, String text, int cursorPosition) {
        if (entityName.getValue() == null) {
            return Collections.emptyList();
        }

        String joinStr = joinClause.getValue();
        String whereStr = whereClause.getValue();

        // the magic entity name!  The length is three character to match "{E}" length in query
        String entityNameAlias = "a39";

        int position = 0;

        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("select ");
        queryBuilder.append(entityNameAlias);
        queryBuilder.append(" from ");
        queryBuilder.append(entities.get(entityName.getValue()));
        queryBuilder.append(" ");
        queryBuilder.append(entityNameAlias);
        queryBuilder.append(" ");
        if (joinStr != null && !joinStr.equals("")) {
            if (sender == joinClause) {
                position = queryBuilder.length() + cursorPosition - 1;
            }
            queryBuilder.append(joinStr);
        }

        if (whereStr != null && !whereStr.equals("")) {
            if (sender == whereClause) {
                position = queryBuilder.length() + " WHERE ".length() + cursorPosition - 1;
            }
            queryBuilder.append(" WHERE ").append(whereStr);
        }

        String query = queryBuilder.toString();
        query = query.replace("{E}", entityNameAlias);

        List<Suggestion> suggestions
                = JpqlSuggestionFactory.requestHint(query, position, sender.getAutoCompleteSupport(), cursorPosition);
        addSpecificSuggestions(sender, text, cursorPosition, suggestions);
        return suggestions;
    }

    protected void addSpecificSuggestions(SourceCodeEditor sender, String text, int cursorPosition, List<Suggestion> suggestions) {
        if (cursorPosition <= 0)
            return;
        int colonIdx = text.substring(0, cursorPosition).lastIndexOf(":");
        if (colonIdx < 0)
            return;

        List<String> strings = new ArrayList<>();
        strings.add(SESSION_PREFIX + "userGroupId");
        strings.add(SESSION_PREFIX + "userId");
        strings.add(SESSION_PREFIX + "userLogin");
        if (PersistenceHelper.isLoaded(getItem(), "group") && getItem().getGroup() != null) {
            List<String> attributeNames = userManagementService.getSessionAttributeNames(getItem().getGroup().getId());
            for (String name : attributeNames) {
                strings.add(SESSION_PREFIX + name);
            }
        }
        Collections.sort(strings);

        String entered = text.substring(colonIdx + 1, cursorPosition);
        for (String string : strings) {
            if (string.startsWith(entered)) {
                suggestions.add(new Suggestion(sender.getAutoCompleteSupport(), string, string.substring(entered.length()), "", cursorPosition, cursorPosition));
            }
        }
    }

    public void getJoinClauseHelp() {
        getDialogOptions()
                .setModal(false)
                .setWidth(600);
        showMessageDialog(getMessage("joinClause"), getMessage("joinClauseHelp"), MessageType.CONFIRMATION_HTML);
    }

    public void getWhereClauseHelp() {
        getDialogOptions()
                .setModal(false)
                .setWidth(600);
        showMessageDialog(getMessage("whereClause"), getMessage("whereClauseHelp"), MessageType.CONFIRMATION_HTML);
    }

    public void getGroovyScriptHelp() {
        getDialogOptions()
                .setModal(false)
                .setWidth(600);
        showMessageDialog(getMessage("groovyScript"), getMessage("groovyScriptHelp"), MessageType.CONFIRMATION_HTML);
    }

    public void openWizard() {
        String entityNameValue = entityName.getValue();
        if (StringUtils.isBlank(entityNameValue)) {
            showNotification(getMessage("notification.entityIsEmpty"), NotificationType.HUMANIZED);
            entityName.requestFocus();
            return;
        }

        FakeFilterSupport fakeFilterSupport = new FakeFilterSupport(this, metadata.getSession().getClass(entityNameValue));

        WindowInfo windowInfo = windowConfig.getWindowInfo("filterEditor");

        Map<String, Object> params = new HashMap<>();
        Constraint constraint = (Constraint) getItem();
        final Filter fakeFilter = fakeFilterSupport.createFakeFilter();
        final FilterEntity filterEntity = fakeFilterSupport.createFakeFilterEntity(constraint.getFilterXml());
        final ConditionsTree conditionsTree = fakeFilterSupport.createFakeConditionsTree(fakeFilter, filterEntity);

        params.put("filter", fakeFilter);
        params.put("filterEntity", filterEntity);
        params.put("conditions", conditionsTree);
        params.put("useShortConditionForm", true);

        FilterEditor filterEditor = (FilterEditor) windowManagerProvider.get().openWindow(windowInfo, WindowManager.OpenType.DIALOG, params);
        filterEditor.addCloseListener(actionId -> {
            if (!COMMIT_ACTION_ID.equals(actionId)) return;
            FilterParser filterParser1 = AppBeans.get(FilterParser.class);
            //todo eude rename com.haulmont.cuba.gui.components.filter.FilterParser
            filterEntity.setXml(filterParser1.getXml(filterEditor.getConditions(), Param.ValueProperty.DEFAULT_VALUE));
            if (filterEntity.getXml() != null) {
                Element element = Dom4j.readDocument(filterEntity.getXml()).getRootElement();
                com.haulmont.cuba.core.global.filter.FilterParser filterParser = new com.haulmont.cuba.core.global.filter.FilterParser(element);

                Constraint item = (Constraint) getItem();
                if (item.getCheckType().database()) {
                    String jpql = new SecurityJpqlGenerator().generateJpql(filterParser.getRoot());
                    constraint.setWhereClause(jpql);
                }

                if (item.getCheckType().memory()) {
                    String groovy = new GroovyGenerator().generateGroovy(filterParser.getRoot());
                    constraint.setGroovyScript(groovy);
                }
                constraint.setFilterXml(filterEntity.getXml());
            }
        });
    }

    public void testConstraint() {
        Constraint constraint = (Constraint) getItem();
        String entityName = constraint.getEntityName();
        if (validateAll()) {
            String baseQueryString = "select e from " + entityName + " e";
            String resultQueryStr = null;

            try {
                QueryTransformer transformer = QueryTransformerFactory.createTransformer(baseQueryString);
                if (StringUtils.isNotBlank(constraint.getJoinClause())) {
                    transformer.addJoinAndWhere(constraint.getJoinClause(), constraint.getWhereClause());
                } else {
                    transformer.addWhere(constraint.getWhereClause());
                }
                CollectionDatasource datasource = DsBuilder.create()
                        .setMetaClass(metadata.getSession().getClassNN(entityName))
                        .setMaxResults(0)
                        .buildCollectionDatasource();
                datasource.setQuery(transformer.getResult());
                datasource.refresh();

                showNotification(getMessage("notification.success"), NotificationType.HUMANIZED);
            } catch (JpqlSyntaxException e) {
                StringBuilder stringBuilder = new StringBuilder();
                for (ErrorRec rec : e.getErrorRecs()) {
                    stringBuilder.append(rec.toString()).append("<br>");
                }
                showMessageDialog(getMessage("notification.error"),
                        formatMessage("notification.syntaxErrors", stringBuilder), MessageType.WARNING_HTML);
            } catch (Exception e) {
                showMessageDialog(getMessage("notification.error"),
                        formatMessage("notification.runtimeError", resultQueryStr, e.getMessage()), MessageType.WARNING_HTML);
            }
        }
    }
}