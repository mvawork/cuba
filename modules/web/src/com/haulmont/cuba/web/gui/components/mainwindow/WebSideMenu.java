/*
 * Copyright (c) 2008-2017 Haulmont.
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

package com.haulmont.cuba.web.gui.components.mainwindow;

import com.haulmont.cuba.gui.components.mainwindow.SideMenu;
import com.haulmont.cuba.web.gui.components.WebAbstractComponent;
import com.haulmont.cuba.web.gui.components.WebComponentsHelper;
import com.haulmont.cuba.web.sys.SideMenuBuilder;
import com.haulmont.cuba.web.toolkit.ui.CubaSideMenu;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.haulmont.bali.util.Preconditions.checkNotNullArgument;

public class WebSideMenu extends WebAbstractComponent<CubaSideMenu> implements SideMenu {

    protected Map<String, MenuItem> allItemsIds = new HashMap<>();

    public WebSideMenu() {
        component = new CubaSideMenu();
    }

    @Override
    public void loadMenuConfig() {
        SideMenuBuilder menuBuilder = new SideMenuBuilder(this);
        menuBuilder.build();
    }

    @Override
    public boolean isSelectOnClick() {
        return component.isSelectOnClick();
    }

    @Override
    public void setSelectOnClick(boolean selectOnClick) {
        component.setSelectOnClick(selectOnClick);
    }

    @Override
    public MenuItem getSelectedItem() {
        return ((MenuItemWrapper) component.getSelectedItem()).getMenuItem();
    }

    @Override
    public void setSelectedItem(MenuItem selectedItem) {
        component.setSelectedItem(((MenuItemImpl) selectedItem).getDelegateItem());
    }

    protected void checkItemIdDuplicate(String id) {
        if (allItemsIds.containsKey(id)) {
            throw new IllegalArgumentException("MenuItem with this id already exists");
        }
    }

    protected void checkItemOwner(MenuItem item) {
        if (item.getMenu() != this) {
            throw new IllegalArgumentException("MenuItem is not created by this menu");
        }
    }

    @Override
    public MenuItem createMenuItem(String id) {
        return createMenuItem(id, null, null, null);
    }

    @Override
    public MenuItem createMenuItem(String id, String caption) {
        return createMenuItem(id, caption, null, null);
    }

    @Override
    public MenuItem createMenuItem(String id, String caption,
                                   @Nullable String icon, @Nullable Consumer<MenuItem> command) {
        checkNotNullArgument(id);
        checkItemIdDuplicate(id);

        MenuItemWrapper delegateItem = new MenuItemWrapper();

        MenuItem menuItem = new MenuItemImpl(this, id, delegateItem);
        menuItem.setCaption(caption);
        menuItem.setIcon(icon);
        menuItem.setCommand(command);

        delegateItem.setMenuItem(menuItem);

        return menuItem;
    }

    @Override
    public void addMenuItem(MenuItem menuItem) {
        checkNotNullArgument(menuItem);
        checkItemIdDuplicate(menuItem.getId());
        checkItemOwner(menuItem);

        component.addMenuItem(((MenuItemImpl) menuItem).getDelegateItem());
        registerMenuItem(menuItem);
    }

    protected void registerMenuItem(MenuItem menuItem) {
        allItemsIds.put(menuItem.getId(), menuItem);
    }

    protected void unregisterItem(MenuItem menuItem) {
        allItemsIds.remove(menuItem.getId());
    }

    @Override
    public void addMenuItem(MenuItem menuItem, int index) {
        checkNotNullArgument(menuItem);
        checkItemIdDuplicate(menuItem.getId());
        checkItemOwner(menuItem);

        component.addMenuItem(((MenuItemImpl) menuItem).getDelegateItem(), index);
        registerMenuItem(menuItem);
    }

    @Override
    public void removeMenuItem(MenuItem menuItem) {
        checkNotNullArgument(menuItem);
        checkItemOwner(menuItem);

        component.removeMenuItem(((MenuItemImpl) menuItem).getDelegateItem());
        unregisterItem(menuItem);
    }

    @Override
    public void removeMenuItem(int index) {
        CubaSideMenu.MenuItem delegateItem = component.getMenuItems().get(index);
        component.removeMenuItem(index);
        unregisterItem(((MenuItemWrapper) delegateItem).getMenuItem());
    }

    @Override
    public MenuItem getMenuItem(String id) {
        return allItemsIds.get(id);
    }

    @Override
    public MenuItem getMenuItemNN(String id) {
        MenuItem menuItem = allItemsIds.get(id);
        if (menuItem == null) {
            throw new IllegalArgumentException("Unable to find menu item with id: " + id);
        }
        return menuItem;
    }

    @Override
    public List<MenuItem> getMenuItems() {
        return component.getMenuItems().stream()
                .map(delegateItem -> ((MenuItemWrapper) delegateItem).getMenuItem())
                .collect(Collectors.toList());
    }

    @Override
    public boolean hasMenuItems() {
        return component.hasMenuItems();
    }

    protected static class MenuItemWrapper extends CubaSideMenu.MenuItem {
        protected MenuItem menuItem;

        public MenuItemWrapper() {
        }

        public MenuItem getMenuItem() {
            return menuItem;
        }

        public void setMenuItem(MenuItem menuItem) {
            this.menuItem = menuItem;
        }
    }

    protected static class MenuItemImpl implements MenuItem {
        protected WebSideMenu menu;
        protected String id;
        protected CubaSideMenu.MenuItem delegateItem;
        protected Consumer<MenuItem> command;

        protected String icon;

        public MenuItemImpl(WebSideMenu menu, String id, CubaSideMenu.MenuItem delegateItem) {
            this.menu = menu;
            this.id = id;
            this.delegateItem = delegateItem;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public SideMenu getMenu() {
            return menu;
        }

        public CubaSideMenu.MenuItem getDelegateItem() {
            return delegateItem;
        }

        @Override
        public String getCaption() {
            return delegateItem.getCaption();
        }

        @Override
        public void setCaption(String caption) {
            delegateItem.setCaption(caption);
        }

        @Override
        public String getDescription() {
            return delegateItem.getDescription();
        }

        @Override
        public void setDescription(String description) {
            delegateItem.setDescription(description);
        }

        @Override
        public String getIcon() {
            return icon;
        }

        @Override
        public void setIcon(String icon) {
            this.icon = icon;

            delegateItem.setIcon(WebComponentsHelper.getIcon(icon));
        }

        @Override
        public boolean isCaptionAsHtml() {
            return delegateItem.isCaptionAsHtml();
        }

        @Override
        public void setCaptionAsHtml(boolean captionAsHtml) {
            delegateItem.setCaptionAsHtml(captionAsHtml);
        }

        @Override
        public boolean isVisible() {
            return delegateItem.isVisible();
        }

        @Override
        public void setVisible(boolean visible) {
            delegateItem.setVisible(visible);
        }

        @Override
        public boolean isExpanded() {
            return delegateItem.isExpanded();
        }

        @Override
        public void setExpanded(boolean expanded) {
            delegateItem.setExpanded(expanded);
        }

        @Override
        public String getStyleName() {
            return delegateItem.getStyleName();
        }

        @Override
        public void setStyleName(String styleName) {
            delegateItem.setStyleName(styleName);
        }

        @Override
        public void addStyleName(String styleName) {
            delegateItem.addStyleName(styleName);
        }

        @Override
        public void removeStyleName(String styleName) {
            delegateItem.removeStyleName(styleName);
        }

        @Override
        public String getBadgeText() {
            return delegateItem.getBadgeText();
        }

        @Override
        public void setBadgeText(String badgeText) {
            delegateItem.setBadgeText(badgeText);
        }

        @Override
        public String getTestId() {
            return delegateItem.getTestId();
        }

        @Override
        public void setTestId(String testId) {
            delegateItem.setTestId(testId);
        }

        @Override
        public Consumer<MenuItem> getCommand() {
            return command;
        }

        @Override
        public void setCommand(Consumer<MenuItem> command) {
            this.command = command;

            if (command != null) {
                delegateItem.setCommand(event -> this.command.accept(this));
            } else {
                delegateItem.setCommand(null);
            }
        }

        @Override
        public void addChildItem(MenuItem menuItem) {
            checkNotNullArgument(menuItem);
            menu.checkItemOwner(menuItem);

            delegateItem.addChildItem(((MenuItemImpl) menuItem).getDelegateItem());

            menu.registerMenuItem(menuItem);
        }

        @Override
        public void addChildItem(MenuItem menuItem, int index) {
            checkNotNullArgument(menuItem);
            menu.checkItemOwner(menuItem);

            delegateItem.addChildItem(((MenuItemImpl) menuItem).getDelegateItem(), index);

            menu.registerMenuItem(menuItem);
        }

        @Override
        public void removeChildItem(MenuItem menuItem) {
            checkNotNullArgument(menuItem);
            menu.checkItemOwner(menuItem);

            delegateItem.removeChildItem(((MenuItemImpl) menuItem).getDelegateItem());

            menu.unregisterItem(menuItem);
        }

        @Override
        public void removeChildItem(int index) {
            CubaSideMenu.MenuItem menuItem = delegateItem.getChildren().get(index);
            delegateItem.removeChildItem(index);

            menu.unregisterItem(((MenuItemWrapper) menuItem).getMenuItem());
        }

        @Override
        public List<MenuItem> getChildren() {
            return delegateItem.getChildren().stream()
                    .map(delegateItem -> ((MenuItemWrapper) delegateItem).getMenuItem())
                    .collect(Collectors.toList());
        }

        @Override
        public boolean hasChildren() {
            return delegateItem.hasChildren();
        }
    }
}