/*
 * Copyright (C) 2017 Jorge Ruesga
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

import com.ruesga.rview.gerrit.model.ProjectStatus;

/**
 * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-projects.html#query-projects"
 */
public class ProjectQuery extends SimpleQuery {

    public ProjectQuery description(String description) {
        add("description:" + sanitizeValue(description));
        return this;
    }

    public ProjectQuery name(String name) {
        add("name:" + sanitizeValue(name));
        return this;
    }

    public ProjectQuery inname(String inname) {
        add("inname:" + sanitizeValue(inname));
        return this;
    }

    public ProjectQuery state(ProjectStatus state) {
        add("state:" + String.valueOf(state).toLowerCase());
        return this;
    }

    public ProjectQuery visible(boolean status) {
        if (status) {
            add("is:visible");
        } else {
            remove("is:visible");
        }
        return this;
    }
}

