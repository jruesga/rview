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

/**
 * @link "https://gerrit-review.googlesource.com/Documentation/user-search-groups.html#_search_operators"
 */
public class GroupQuery extends SimpleQuery {

    public GroupQuery any(String q) {
        add(sanitizeValue(q));
        return this;
    }

    public GroupQuery description(String description) {
        add("description:" + sanitizeValue(description));
        return this;
    }

    public GroupQuery name(String name) {
        add("name:" + sanitizeValue(name));
        return this;
    }

    public GroupQuery inname(String inname) {
        add("inname:" + sanitizeValue(inname));
        return this;
    }

    public GroupQuery owner(String owner) {
        add("owner:" + sanitizeValue(owner));
        return this;
    }

    public GroupQuery uuid(String uuid) {
        add("uuid:" + sanitizeValue(uuid));
        return this;
    }

    public GroupQuery member(String member) {
        add("member:" + sanitizeValue(member));
        return this;
    }

    public GroupQuery subgroup(String subgroup) {
        add("subgroup:" + sanitizeValue(subgroup));
        return this;
    }

    public GroupQuery visibletoall(boolean status) {
        if (status) {
            add("is:visibletoall");
        } else {
            remove("is:visibletoall");
        }
        return this;
    }

    public GroupQuery visible(boolean status) {
        if (status) {
            add("is:visible");
        } else {
            remove("is:visible");
        }
        return this;
    }
}

