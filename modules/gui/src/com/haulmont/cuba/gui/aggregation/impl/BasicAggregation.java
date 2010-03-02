/*
 * Copyright (c) 2008 Haulmont Technology Ltd. All Rights Reserved.
 * Haulmont Technology proprietary and confidential.
 * Use is subject to license terms.

 * Author: Nikolay Gorodnov
 * Created: 25.02.2010 22:40:05
 *
 * $Id$
 */
package com.haulmont.cuba.gui.aggregation.impl;

import com.haulmont.cuba.gui.aggregation.Aggregation;

import java.util.Collection;

public class BasicAggregation<T> implements Aggregation<T> {

    private Class<T> clazz;

    public BasicAggregation(Class<T> clazz) {
        this.clazz = clazz;
    }

    public T sum(Collection<T> items) {
        throw new UnsupportedOperationException();
    }

    public boolean allowSum() {
        return false;
    }

    public T avg(Collection<T> items) {
        throw new UnsupportedOperationException();
    }

    public boolean allowAvg() {
        return false;
    }

    public T min(Collection<T> items) {
        throw new UnsupportedOperationException();
    }

    public boolean allowMin() {
        return false;
    }

    public T max(Collection<T> items) {
        throw new UnsupportedOperationException();
    }

    public boolean allowMax() {
        return false;
    }

    public int count(Collection<T> items) {
        return items.size();
    }

    public boolean allowCount() {
        return true;
    }

    public Class<T> getJavaClass() {
        return clazz;
    }
}
