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

import com.ruesga.rview.gerrit.GerritApi;
import com.ruesga.rview.gerrit.filter.antlr.QueryLexer;
import com.ruesga.rview.gerrit.filter.antlr.QueryParseException;
import com.ruesga.rview.gerrit.filter.antlr.QueryParser;
import com.ruesga.rview.gerrit.model.AccountInfo;
import com.ruesga.rview.gerrit.model.ProjectInfo;
import com.ruesga.rview.gerrit.model.SubmitRecordStatusType;

import org.antlr.runtime.tree.Tree;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * @link "https://gerrit-review.googlesource.com/Documentation/user-search.html#_search_operators"
 */
public class ChangeQuery extends ComplexQuery<ChangeQuery> {

    private static class Label {
    }

    public static final String[] FIELDS_NAMES = {
            "age", "before", "after", "until", "since", "change", "conflicts", "destination",
            "owner", "ownerin", "query", "reviewer", "reviewerin", "commit", "project",
            "projects", "parentproject", "branch", "intopic", "topic", "ref", "tr",
            "bug", "label", "message", "comment", "path", "file", "star", "has",
            "is", "status", "added", "deleted", "delta", "size", "commentby", "from",
            "reviewedby", "author", "committer", "visibleto", "starredby", "watchedby",
            "draftby", "assignee", "cc", "unresolved", "submittable", "revertof",
            "hashtag", "extension", "onlyextensions", "directory", "footer", "attention"
    };

    public static final Class[] FIELDS_TYPES = {
            TimeUnit.class, Date.class, Date.class, Date.class, Date.class, String.class, String.class, String.class,
            String.class, String.class, String.class, String.class, String.class, String.class, String.class,
            String.class, String.class, String.class, String.class, String.class, String.class, String.class,
            String.class, Label.class, String.class, String.class, String.class, String.class, String.class, HasType.class,
            IsType.class, StatusType.class, Relation.class, Relation.class, Relation.class, Relation.class, String.class, String.class,
            String.class, String.class, String.class, String.class, String.class, String.class,
            String.class, String.class, String.class, Relation.class, SubmitRecordStatusType.class, Integer.class,
            String.class, String.class, String.class, String.class, String.class, String.class
    };

    public static final Class[] SUGGEST_TYPES = {
            null, null, null, null, null, null, null, null,
            AccountInfo.class, null, null, AccountInfo.class, null, null, ProjectInfo.class,
            null, ProjectInfo.class, null, null, null, null, null,
            null, null, null, null, null, null, null, HasType.class,
            IsType.class, StatusType.class, null, null, null, null, AccountInfo.class, AccountInfo.class,
            AccountInfo.class, AccountInfo.class, AccountInfo.class, AccountInfo.class, AccountInfo.class, AccountInfo.class,
            AccountInfo.class, AccountInfo.class, AccountInfo.class, Relation.class, SubmitRecordStatusType.class, null,
            null, null, null, null, null, null
    };

    public static final Double[] SUPPORTED_FROM_VERSION = {
            null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null,
            null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null,
            null, null, null, null, null, null,
            null, 2.14d, 2.14d, 2.14d, 2.14d, 2.15d,
            2.15d, 3.0d, 3.0d, 3.0d, 3.0d, 3.7d
    };

    public static final Double[] UNSUPPORTED_FROM_VERSION = {
            null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null,
            null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null,
            null, null, null, null, null, null,
            2.15d, null, null, null, null, null,
            null, null, null, null, null, null
    };

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

    public ChangeQuery cc(String user) {
        add("cc:" + sanitizeValue(user));
        return this;
    }

    public ChangeQuery ccSelf() {
        add("cc:self");
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
        add("label:" + sanitizeValue(label + "=" + toScore(score)));
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
        add("status:" + type.toString().toLowerCase(Locale.US));
        return this;
    }

    public ChangeQuery added(Relation relation, int count) {
        add("added:\"" + relation.toQuery(count) + "\"");
        return this;
    }

    public ChangeQuery deleted(Relation relation, int count) {
        add("deleted:\"" + relation.toQuery(count) + "\"");
        return this;
    }

    public ChangeQuery delta(Relation relation, int count) {
        add("delta:\"" + relation.toQuery(count) + "\"");
        return this;
    }

    public ChangeQuery size(Relation relation, int count) {
        add("size:\"" + relation.toQuery(count) + "\"");
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

    public ChangeQuery assignee(String user) {
        add("assignee:" + sanitizeValue(user));
        return this;
    }

    public ChangeQuery assigneeBySelf() {
        add("assignee:self");
        return this;
    }

    public ChangeQuery unresolved(Relation relation, int count) {
        add("unresolved:\"" + relation.toQuery(count) + "\"");
        return this;
    }

    public ChangeQuery submittable(SubmitRecordStatusType type) {
        add("submittable:" + type.toString().toLowerCase(Locale.US));
        return this;
    }

    public ChangeQuery revertOf(int changeId) {
        add("revertof:" + changeId);
        return this;
    }

    public ChangeQuery hashtag(String hashtag) {
        add("hashtag:" + sanitizeValue(hashtag));
        return this;
    }

    public ChangeQuery simple(String query) {
        add(sanitizeValue(query));
        return this;
    }

    private static String toScore(int n) {
        return (n > 0 ? "+" : n < 0 ? "-" : "") +  n;
    }

    public static ChangeQuery parse(String query) throws QueryParseException {
        // Clear the current query
        ChangeQueryTokenizer tokenizer = new ChangeQueryTokenizer();
        return tokenizer.parse(query);
    }

    private static class ChangeQueryTokenizer {
        public ChangeQuery parse(String expression) throws QueryParseException {
            final Tree tree = QueryParser.parse(expression);
            return toChangeQuery(tree);
        }

        private ChangeQuery toChangeQuery(Tree tree) throws QueryParseException {
            ChangeQuery query = new ChangeQuery();

            switch (tree.getType()) {
                case QueryLexer.FIELD_NAME:
                    addField(tree, query);
                    break;

                case QueryLexer.AND:
                case QueryLexer.OR:
                    int childType = tree.getChild(0).getType();
                    switch (childType) {
                        case QueryLexer.AND:
                        case QueryLexer.OR:
                            ChangeQuery q = toChangeQuery(tree.getChild(0));
                            if (query.queries().isEmpty()) {
                                query.wrap(q);
                            } else if (childType == QueryLexer.AND) {
                                query.and(q);
                            } else {
                                query.or(q);
                            }
                            break;
                        case QueryLexer.DEFAULT_FIELD:
                            query.add(getDefaultFieldText(tree.getChild(0)));
                            break;
                        case QueryLexer.NOT:
                            query.negate(toChangeQuery(tree.getChild(0).getChild(0)));
                            break;
                        default:
                            addField(tree.getChild(0), query);
                            break;
                    }
                    for (int i = 1; i < tree.getChildCount(); i++) {
                        childType = tree.getChild(i).getType();
                        if (childType == QueryLexer.FIELD_NAME
                                || childType == QueryLexer.DEFAULT_FIELD) {
                            if (tree.getType() == QueryLexer.AND) {
                                query.and(toChangeQuery(tree.getChild(i)));
                            } else {
                                query.or(toChangeQuery(tree.getChild(i)));
                            }
                        } else if (childType == QueryLexer.AND || childType == QueryLexer.OR
                                || childType == QueryLexer.NOT) {
                            ChangeQuery childQuery = toChangeQuery(tree.getChild(i));
                            if (tree.getType() == QueryLexer.AND) {
                                query.and(childQuery);
                            } else {
                                query.or(childQuery);
                            }
                        }
                    }
                    break;
                case QueryLexer.NOT:
                    query.negate(toChangeQuery(tree.getChild(0)));
                    break;
                case QueryLexer.DEFAULT_FIELD:
                    query.add(getDefaultFieldText(tree));
                    break;
                default:
                    throw new QueryParseException("Invalid query at " +
                            tree.getCharPositionInLine() + ": " + tree.getText());
            }

            return query;
        }

        private String getDefaultFieldText(Tree tree) throws QueryParseException {
            String val = tree.getChild(0).toString();
            if (val.startsWith("\"") && val.endsWith("\"") && val.length() >= 2) {
                val = val.substring(1, val.length() - 1);
            }
            if (!isValidExpression(val)) {
                throw new QueryParseException("Invalid query at " +
                        tree.getCharPositionInLine() + ": " + tree.getText());
            }
            return Query.sanitizeValue(val);
        }

        @SuppressWarnings("unchecked")
        private void addField(Tree tree, ChangeQuery query) throws QueryParseException {
            String fieldName = tree.getText();
            String text = getFieldText(tree);
            List<String> names = Arrays.asList(FIELDS_NAMES);
            int index = names.indexOf(fieldName);
            if (index >= 0) {
                Class type = FIELDS_TYPES[index];
                if (type.equals(String.class)) {
                    if (!text.equalsIgnoreCase(GerritApi.SELF_ACCOUNT)) {
                        text = "\"" + text + "\"";
                    } else {
                        text = GerritApi.SELF_ACCOUNT;
                    }
                } else if (type.equals(Integer.class)) {
                    // ignore

                } else if (type.equals(TimeUnit.class)) {
                    int value = 0;
                    TimeUnit unit = null;
                    for (TimeUnit u : TimeUnit.values()) {
                        String s = findTimeUnit(text, u);
                        if (s != null) {
                            try {
                                value = Integer.valueOf(text.substring(
                                        0, text.length() - s.length()));
                                unit = u;
                                break;
                            } catch (Exception ex) {
                                // Ignore
                            }
                        }
                    }
                    if (unit == null) {
                        throw new QueryParseException("Illegal value for field " + fieldName
                                +  " at " + tree.getCharPositionInLine() + ": " + text);
                    }
                    text = unit.toQuery(value);

                } else if (type.equals(Date.class)) {
                    Date date;
                    try {
                        date = query.getTimeFormatter().parse("\"" + text + "\"");
                    } catch (ParseException e) {
                        throw new QueryParseException("Illegal value for field " + fieldName
                                +  " at " + tree.getCharPositionInLine() + ": " + text);
                    }
                    text = query.getTimeFormatter().format(date);

                } else if (type.equals(Label.class)) {
                    String[] v = text.split("=");
                    if (v.length != 2) {
                        throw new QueryParseException("Illegal value for field " + fieldName
                                +  " at " + tree.getCharPositionInLine() + ": " + text);
                    }
                    String score;
                    try {
                        score = toScore(Integer.parseInt(v[1].trim()));
                    } catch (NumberFormatException ex) {
                        throw new QueryParseException("Illegal value for field " + fieldName
                                +  " at " + tree.getCharPositionInLine() + ": " + text);
                    }
                    text = "\"" + v[0].trim() + "=" + score + "\"";

                } else if (type.equals(Relation.class)) {
                    int value = 0;
                    Relation relation = null;
                    for (Relation r : Relation.values()) {
                        if (text.toLowerCase(Locale.US).startsWith(r.mRelation)) {
                            try {
                                value = Integer.valueOf(text.substring(r.mRelation.length()));
                                relation = r;
                                break;
                            } catch (Exception ex) {
                                // Ignore
                            }
                        }
                    }
                    if (relation == null) {
                        throw new QueryParseException("Illegal value for field " + fieldName
                                +  " at " + tree.getCharPositionInLine() + ": " + text);
                    }
                    text = "\"" + relation.toQuery(value) + "\"";

                } else if (type.isEnum()) {
                    try {
                        Enum e = Enum.valueOf(type, text.toUpperCase(Locale.US));
                        text = String.valueOf(e).toLowerCase(Locale.US);
                    } catch (IllegalArgumentException ex) {
                        throw new QueryParseException("Illegal value for field " + fieldName
                                +  " at " + tree.getCharPositionInLine() + ": " + text);
                    }

                } else {
                    throw new QueryParseException("Illegal field value at "
                            + tree.getText() + ": " + fieldName);
                }

                query.add(fieldName + ":" + text);
                return;
            }

            throw new QueryParseException("Illegal field at " + tree.getCharPositionInLine()
                    + ": " + fieldName);
        }

        private String getFieldText(Tree tree) {
            return tree.getChild(0).getText();
        }

        private String findTimeUnit(String text, TimeUnit unit) {
            for (String s : unit.mUnits) {
                if (text.toLowerCase(Locale.US).endsWith(s)) {
                    return s;
                }
            }
            return null;
        }
    }
}

