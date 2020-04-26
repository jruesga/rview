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
 * @link "https://gerrit-review.googlesource.com/Documentation/user-search-accounts.html#_search_operators"
 */
public class AccountQuery extends SimpleQuery {

    public AccountQuery any(String q) {
        add(sanitizeValue(q));
        return this;
    }

    public AccountQuery cansee(String change) {
        add("cansee:" + sanitizeValue(change));
        return this;
    }

    public AccountQuery email(String email) {
        add("email:" + sanitizeValue(email));
        return this;
    }

    public AccountQuery name(String name) {
        add("name:" + sanitizeValue(name));
        return this;
    }

    public AccountQuery username(String username) {
        add("username:" + sanitizeValue(username));
        return this;
    }

    public AccountQuery active(boolean status) {
        if (status) {
            remove("is:inactive");
            add("is:active");
        } else {
            remove("is:active");
            add("is:inactive");
        }
        return this;
    }

    public AccountQuery visible(boolean status) {
        if (status) {
            add("is:visible");
        } else {
            remove("is:visible");
        }
        return this;
    }
}

