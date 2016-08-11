/*
 * Copyright (C) 2016 Jorge Ruesga
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ruesga.rview.gerrit.filter;

public abstract class ComplexQuery<T extends ComplexQuery> extends Query {

    public T and(T query) {
        if (queries().size() == 0) {
            throw new IllegalArgumentException("Can't use operator here");
        }
        if (query.queries().size() == 0) {
            throw new IllegalArgumentException("Empty query");
        }
        add("AND (" + query + ")");
        //noinspection unchecked
        return (T)this;
    }

    public T or(T query) {
        if (queries().size() == 0) {
            throw new IllegalArgumentException("Can't use operator here");
        }
        if (query.queries().size() == 0) {
            throw new IllegalArgumentException("Empty query");
        }
        add("OR (" + query + ")");
        //noinspection unchecked
        return (T)this;
    }

    public T negate(T query) {
        if (query.queries().size() == 0) {
            throw new IllegalArgumentException("Empty query");
        }
        add("-(" + query + ")");
        //noinspection unchecked
        return (T)this;
    }

    @Override
    public final String toString() {
        StringBuilder sb = new StringBuilder();
        int count = queries().size();
        for (int i = 0; i < count; i++) {
            sb.append(" ");
            sb.append(queries().get(i));
        }
        return sb.toString().trim();
    }
}
