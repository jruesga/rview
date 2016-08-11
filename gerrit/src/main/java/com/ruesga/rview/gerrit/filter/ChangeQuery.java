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

import java.util.Date;
import java.util.Locale;

/**
 * @link "https://gerrit-review.googlesource.com/Documentation/user-search.html#_search_operators"
 */
public class ChangeQuery extends ComplexQuery<ChangeQuery> {

    public enum TimeUnit {
        SECONDS("s"), MINUTES("m"), HOURS("h"), DAYS("d"), WEEKS("w"), MONTHS("mon"), YEARS("y");

        private String mUnit;
        TimeUnit(String unit) {
            mUnit = unit;
        }

        private String toQuery(int value) {
            return String.format(Locale.US, "%d%s", value, mUnit);
        }
    }

    public ChangeQuery age(TimeUnit unit, int value) {
        add("age:" + unit.toQuery(value));
        return this;
    }

    public ChangeQuery before(Date date) {
        add("before:" + getTimeFormatter().format(date));
        return this;
    }

    public ChangeQuery after(Date date) {
        add("after:" + getTimeFormatter().format(date));
        return this;
    }

    public ChangeQuery until(Date date) {
        add("until:" + getTimeFormatter().format(date));
        return this;
    }

    public ChangeQuery since(Date date) {
        add("since:" + getTimeFormatter().format(date));
        return this;
    }

    public ChangeQuery change(String changeId) {
        add("change:" + sanitizeValue(changeId));
        return this;
    }

    public ChangeQuery conflicts(String changeId) {
        add("conflicts:" + sanitizeValue(changeId));
        return this;
    }

    public ChangeQuery destination(String destination) {
        add("destination:" + sanitizeValue(destination));
        return this;
    }

    public ChangeQuery owner(String user) {
        add("owner:" + sanitizeValue(user));
        return this;
    }

    public ChangeQuery ownerSelf() {
        add("owner:self");
        return this;
    }

    public ChangeQuery ownerIn(String group) {
        add("ownerin:" + sanitizeValue(group));
        return this;
    }

    public ChangeQuery query(String name) {
        add("query:" + sanitizeValue(name));
        return this;
    }

    public ChangeQuery reviewer(String user) {
        add("reviewer:" + sanitizeValue(user));
        return this;
    }

    public ChangeQuery reviewerSelf() {
        add("reviewer:self");
        return this;
    }

    public ChangeQuery reviewerIn(String group) {
        add("reviewerin:" + sanitizeValue(group));
        return this;
    }

    public ChangeQuery commit(String sha1) {
        add("commit:" + sanitizeValue(sha1));
        return this;
    }

    public ChangeQuery project(String project) {
        add("project:" + sanitizeValue(project));
        return this;
    }

    public ChangeQuery projects(String prefix) {
        add("projects:" + sanitizeValue(prefix));
        return this;
    }

    public ChangeQuery parentProject(String project) {
        add("parentproject:" + sanitizeValue(project));
        return this;
    }

    public ChangeQuery branch(String branch) {
        add("branch:" + sanitizeValue(branch));
        return this;
    }

    public ChangeQuery inTopic(String topic) {
        add("intopic:" + sanitizeValue(topic));
        return this;
    }

    public ChangeQuery topic(String topic) {
        add("topic:" + sanitizeValue(topic));
        return this;
    }

    public ChangeQuery ref(String ref) {
        add("ref:" + sanitizeValue(ref));
        return this;
    }

    public ChangeQuery track(String id) {
        add("tr:" + sanitizeValue(id));
        return this;
    }

    public ChangeQuery bug(String id) {
        add("bug:" + sanitizeValue(id));
        return this;
    }

    public ChangeQuery label(String label, int score) {
        add("label:" + sanitizeValue(label + "=" + score));
        return this;
    }

    public ChangeQuery message(String message) {
        add("message:" + sanitizeValue(message));
        return this;
    }

    public ChangeQuery comment(String text) {
        add("comment:" + sanitizeValue(text));
        return this;
    }

    public ChangeQuery path(String path) {
        add("path:" + sanitizeValue(path));
        return this;
    }

    public ChangeQuery file(String name) {
        add("file:" + sanitizeValue(name));
        return this;
    }

    public ChangeQuery star(String label) {
        add("star:" + sanitizeValue(label));
        return this;
    }

    public ChangeQuery has(HasType type) {
        add("has:" + type.toString().toLowerCase(Locale.US));
        return this;
    }

    public ChangeQuery is(IsType type) {
        add("is:" + type.toString().toLowerCase(Locale.US));
        return this;
    }

    public ChangeQuery status(StatusType type) {
        add("is:" + type.toString().toLowerCase(Locale.US));
        return this;
    }

    public ChangeQuery added(Relation relation, int count) {
        add("added:\"" + relation.getRelation() + count + "\"");
        return this;
    }

    public ChangeQuery deleted(Relation relation, int count) {
        add("deleted:\"" + relation.getRelation() + count + "\"");
        return this;
    }

    public ChangeQuery delta(Relation relation, int count) {
        add("delta:\"" + relation.getRelation() + count + "\"");
        return this;
    }

    public ChangeQuery size(Relation relation, int count) {
        add("size:\"" + relation.getRelation() + count + "\"");
        return this;
    }

    public ChangeQuery commentBy(String user) {
        add("commentby:" + sanitizeValue(user));
        return this;
    }

    public ChangeQuery from(String user) {
        add("from:" + sanitizeValue(user));
        return this;
    }

    public ChangeQuery reviewedBy(String user) {
        add("reviewedby:" + sanitizeValue(user));
        return this;
    }

    public ChangeQuery author(String author) {
        add("author:" + sanitizeValue(author));
        return this;
    }

    public ChangeQuery committer(String committer) {
        add("committer:" + sanitizeValue(committer));
        return this;
    }

    public ChangeQuery visibleTo(String userOrGroup) {
        add("visibleto:" + sanitizeValue(userOrGroup));
        return this;
    }

    public ChangeQuery starredBy(String user) {
        add("starredby:" + sanitizeValue(user));
        return this;
    }

    public ChangeQuery starredBySelf() {
        add("starredby:self");
        return this;
    }

    public ChangeQuery watchedBy(String user) {
        add("watchedby:" + sanitizeValue(user));
        return this;
    }

    public ChangeQuery watchedBySelf() {
        add("watchedby:self");
        return this;
    }

    public ChangeQuery draftBy(String user) {
        add("draftby:" + sanitizeValue(user));
        return this;
    }

    public ChangeQuery draftBySelf() {
        add("draftby:self");
        return this;
    }
}

