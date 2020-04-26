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
package com.ruesga.rview.gerrit.model;

import com.google.gson.annotations.SerializedName;

/**
 * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#diff-preferences-info"
 */
public class DiffPreferencesInfo {
    @SerializedName("context") public int context;
    @Deprecated @SerializedName("theme") public int theme;
    @SerializedName("expand_all_comments") public boolean expandAllComments;
    @SerializedName("ignore_whitespace") public IgnoreWhiteSpaceStrategy ignoreWhitespace;
    @SerializedName("intraline_difference") public boolean intralineDifference;
    @SerializedName("line_length") public int lineLength;
    @SerializedName("cursor_blink_rate") public int cursorBlinkRate;
    @SerializedName("retain_header") public boolean retainHeader;
    @SerializedName("show_line_endings") public boolean showLineEndings;
    @SerializedName("show_tabs") public boolean showTabs;
    @SerializedName("show_whitespace_errors") public boolean showWhitespaceErrors;
    @SerializedName("skip_deleted") public boolean skipDeleted;
    @SerializedName("skipUncommented") public boolean skipUncommented;
    @SerializedName("syntax_highlighting") public boolean syntaxHighlighting;
    @SerializedName("hide_top_menu") public boolean hideTopMenu;
    @SerializedName("auto_hide_diff_table_header") public boolean autoHideDiffTableHeader;
    @SerializedName("hide_line_numbers") public boolean hideLineNumbers;
    @SerializedName("tab_size") public int tabSize;
    @SerializedName("hide_empty_pane") public boolean hideEmptyPane;
    @SerializedName("match_brackets") public boolean matchBrackets;
    @SerializedName("line_wrapping") public boolean lineWrapping;
}
