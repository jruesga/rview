/*
 * Copyright (C) 2018 Jorge Ruesga
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
package com.ruesga.rview.widget;

import com.ruesga.rview.model.Repository;
import com.ruesga.rview.preferences.Constants;
import com.ruesga.rview.widget.RegExLinkifyTextView.RegExLink;

import org.junit.Test;

import java.util.List;
import java.util.regex.Matcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class RegExLinkifyTextViewTest {
    @Test
    public void testReplaceLink() {
        assertEquals("http://test.com/a",
                replaceLink(RegExLinkifyTextView.WEB_LINK_REGEX,
                        "text http://test.com/a test"));
        assertEquals("http://test.com/a/",
                replaceLink(RegExLinkifyTextView.WEB_LINK_REGEX,
                        "text http://test.com/a/ test"));
        assertEquals("http://test.com/a/",
                replaceLink(RegExLinkifyTextView.WEB_LINK_REGEX,
                        "text http://test.com/a/. test"));
        assertEquals("https://r8-review.googlesource.com/c/r8/+/32540",
                replaceLink(RegExLinkifyTextView.WEB_LINK_REGEX,
                        "After https://r8-review.googlesource.com/c/r8/+/32540, D8/R8 will not"));


        Repository repository = new Repository("test.com", "http://test.com/", false);
        List<RegExLink> links = RegExLinkifyTextView.createRepositoryRegExpLinks(repository);
        assertEquals(4, links.size());
        assertNull(replaceLink(links.get(0),
                "aaa https://test.com/#/c/project/abc_cba-123/+/129166/ aa"));
        assertEquals("com.ruesga.rview://" + Constants.CUSTOM_URI_CHANGE_ID + "/129166",
                replaceLink(links.get(1),
                        "aaa https://test.com/#/c/project/abc_cba-123/+/129166/ aa"));
        assertEquals("com.ruesga.rview://" + Constants.CUSTOM_URI_CHANGE_ID + "/32540",
                replaceLink(links.get(1),
                        "aaa https://test.com/c/r8/+/32540, aa"));
        assertNull(replaceLink(links.get(2),
                "aaa https://test.com/#/c/project/abc_cba-123/+/129166/ aa"));
        assertEquals("https://test.com/dashboard/?title=aa&mine=status:open",
                replaceLink(links.get(3),
                        "aaa https://test.com/dashboard/?title=aa&mine=status:open aa"));

        assertNull(replaceLink(links.get(0),
                "aaa\n\nhttps://test.com/#/c/project_aa_aa/abc/+/129166/\n\nhttps://test.com/#/c/project/abc/+/129167/\n\naa"));
        assertEquals("com.ruesga.rview://" + Constants.CUSTOM_URI_CHANGE_ID + "/129166",
                replaceLink(links.get(1),
                        "aaa\n\nhttps://test.com/#/c/project_aa_aa/abc/+/129166/\n\nhttps://test.com/#/c/project/abc/+/129167/\n\naa"));
        assertNull(replaceLink(links.get(2),
                "aaa\n\nhttps://test.com/#/c/project_aa_aa/abc/+/129166/\n\nhttps://test.com/#/c/project/abc/+/129167/\n\naa"));
        assertEquals("https://test.com/dashboard/?title=aa&mine=status:open",
                replaceLink(links.get(3),
                        "aaa\n\nhttps://test.com/dashboard/?title=aa&mine=status:open\n\naa"));



        assertEquals("mailto:test@test.com",
                replaceLink(RegExLinkifyTextView.EMAIL_REGEX,
                        "text test@test.com test"));


        assertEquals("com.ruesga.rview://" + Constants.CUSTOM_URI_CHANGE
                        + "/I528af53639e27b3b8297079b0bd18dc123d5b168",
                replaceLink(RegExLinkifyTextView.GERRIT_CHANGE_ID_REGEX,
                        "text I528af53639e27b3b8297079b0bd18dc123d5b168 test"));
        assertEquals("com.ruesga.rview://" + Constants.CUSTOM_URI_COMMIT
                        + "/054822cf95da677c4768c2aec70ddeb8c66b0381",
                replaceLink(RegExLinkifyTextView.GERRIT_COMMIT_REGEX,
                        "text 054822cf95da677c4768c2aec70ddeb8c66b0381 test"));

        assertEquals("#/q/I3742dd68dd1ed8f7b8d76791dceb8c7c4cf631f7",
                replaceLink(createRegExLink("(I[0-9a-f]{8,40})", "#/q/$1"),
                        "text I3742dd68dd1ed8f7b8d76791dceb8c7c4cf631f7 test"));

        assertEquals("http://bugs.example.com/show_bug.cgi?id=432",
                replaceLink(createRegExLink("(bug\\s+#?)(\\d+)",
                            "http://bugs.example.com/show_bug.cgi?id=$2"),
                        "text bug 432 test"));

        assertEquals("http://bugs.example.com/show_bug.cgi?id=432",
                replaceLink(createRegExLink("(bug\\s+#?)(\\d+)",
                        "http://bugs.example.com/show_bug.cgi?id=$2"),
                        "text bug #432 test"));

        assertEquals("http://bugs.example.com/12/show_bug.cgi?id=433",
                replaceLink(createRegExLink("bug\\s+(\\d+)-(\\d+)",
                        "http://bugs.example.com/$1/show_bug.cgi?id=$2"),
                        "text bug 12-433 test"));

        assertEquals("https://jira.atlassian.net/browse/JIRA-894",
                replaceLink(createRegExLink("(#)?([A-Z][A-Z0-9_]{1,25}-\\d+)",
                        "https://jira.atlassian.net/browse/$2"),
                        "JIRA-894"));

        assertEquals("https://jira.atlassian.net/browse/JIRA-894",
                replaceLink(createRegExLink("(#)?([A-Z][A-Z0-9_]{1,25}-\\d+)",
                        "https://jira.atlassian.net/browse/$2"),
                        "#JIRA-894"));
    }

    private String replaceLink(RegExLink regEx, String test) {
        Matcher matcher = regEx.mPattern.matcher(test);
        if (!matcher.find()) {
           return null;
        }
        return RegExLinkifyTextView.replaceLink(regEx, matcher);
    }

    private static RegExLink createRegExLink(String regex, String link) {
        return new RegExLink(
                "web", regex, link, true);
    }
}
