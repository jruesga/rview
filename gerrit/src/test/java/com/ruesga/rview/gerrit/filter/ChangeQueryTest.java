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

import com.ruesga.rview.gerrit.filter.antlr.QueryParseException;

import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ChangeQueryTest {

    @Test
    public void testParseQuery() {
        Date now = new Date();
        String nowFormatted = new ChangeQuery().getTimeFormatter().format(now);

        testInvalidParseQuery("AND");
        testInvalidParseQuery("AND status:open");
        testInvalidParseQuery("status:open2");

        testParseQuery("status:open",
                new ChangeQuery().status(StatusType.OPEN));
        testParseQuery("status:open AND owner:username",
                new ChangeQuery()
                        .status(StatusType.OPEN)
                        .and(new ChangeQuery().owner("username")));
        testParseQuery("status:open OR owner:\"username\"",
                new ChangeQuery()
                        .status(StatusType.OPEN)
                        .or(new ChangeQuery().owner("username")));
        testParseQuery("-(status:open owner:username)",
                new ChangeQuery()
                        .negate(new ChangeQuery().status(StatusType.OPEN)
                                .and(new ChangeQuery().owner("username"))));
        testParseQuery("label:\"Code-Review=+1\"",
                new ChangeQuery().label("Code-Review", 1));
        testParseQuery("after:" + nowFormatted + "",
                new ChangeQuery().after(now));
        testParseQuery("age:2d",
                new ChangeQuery().age(TimeUnit.DAYS, 2));
        testParseQuery("added:2",
                new ChangeQuery().added(Relation.EQUALS_THAN, 2));
        testParseQuery("added:\"=2\"",
                new ChangeQuery().added(Relation.EQUALS_THAN, 2));
        testParseQuery("added:>=2",
                new ChangeQuery().added(Relation.GREATER_OR_EQUALS_THAN, 2));
        testParseQuery("is:open AND owner:self",
                new ChangeQuery().is(IsType.OPEN).and(new ChangeQuery().ownerSelf()));
        testParseQuery("is:open AND (reviewer:self AND (-(owner:self)))",
                new ChangeQuery().is(IsType.OPEN).and(
                        new ChangeQuery().reviewerSelf().and(
                                new ChangeQuery().negate(new ChangeQuery().ownerSelf()))));
        testParseQuery("is:closed AND (owner:self OR (reviewer:self)) AND (-(age:4w)))",
                new ChangeQuery().is(IsType.CLOSED)
                        .and(new ChangeQuery().ownerSelf()
                                .or(new ChangeQuery().reviewerSelf()))
                        .and(new ChangeQuery().negate(new ChangeQuery().age(TimeUnit.WEEKS, 4))));

        testParseQuery("status:open AND -(topic:\"translations\")",
                new ChangeQuery().status(StatusType.OPEN).and(
                        new ChangeQuery().negate(
                            new ChangeQuery().topic("translations"))));
        testParseQuery("status:open AND -(topic:\"translations\" AND owner:\"aa\")",
                new ChangeQuery().status(StatusType.OPEN).and(
                        new ChangeQuery().negate(
                                new ChangeQuery().topic("translations").and(
                                        new ChangeQuery().owner("aa")))));
        testParseQuery("aaa",
                new ChangeQuery().simple("aaa"));
        testParseQuery("status:open aaa",
                new ChangeQuery().status(StatusType.OPEN)
                        .and(new ChangeQuery().simple("aaa")));
        testParseQuery("aaa status:open",
                new ChangeQuery().simple("aaa")
                        .and(new ChangeQuery().status(StatusType.OPEN)));
        testParseQuery("owner:\"Test <test@test.com>\" (aaa branch:test2)",
                new ChangeQuery().owner("Test <test@test.com>")
                        .and(new ChangeQuery().simple("aaa")
                                .and(new ChangeQuery().branch("test2"))));


        testParseQuery("(is:open label:Verified=+1 -age:1month) AND NOT (is:open reviewerin:Tuleap-Integrators -age:1month label:Verified=+1 -is:draft)",
                new ChangeQuery().is(IsType.OPEN)
                        .and(new ChangeQuery().label("Verified", 1))
                        .and(new ChangeQuery().negate(new ChangeQuery().age(TimeUnit.MONTHS, 1)))
                        .and(new ChangeQuery().negate(
                                new ChangeQuery().is(IsType.OPEN)
                                        .and(new ChangeQuery().reviewerIn("Tuleap-Integrators"))
                                        .and(new ChangeQuery().negate(new ChangeQuery().age(TimeUnit.MONTHS, 1)))
                                        .and(new ChangeQuery().label("Verified", 1))
                                        .and(new ChangeQuery().negate(new ChangeQuery().is(IsType.DRAFT)))
                        )));
    }

    private void testParseQuery(String expression, ChangeQuery expectedResult) {
        String result = null;
        try {
            result = ChangeQuery.parse(expression).toString();
        } catch (QueryParseException ex) {
            // Ignore
        }
        System.out.println("Testing '" + expression + "' (" + String.valueOf(result) + "): '" +
                String.valueOf(expectedResult) + "'");
        assertEquals(expression, String.valueOf(expectedResult), result);
    }

    private void testInvalidParseQuery(String expression) {
        try {
            ChangeQuery.parse(expression);
            fail();
        } catch (QueryParseException ex) {
            // Ignore
        }
        System.out.println("Failure of '" + expression + "'");
    }

}
