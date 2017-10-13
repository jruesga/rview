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

import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#preferences-input"
 */
public class DiffPreferencesInput {
    @Nullable @SerializedName("context") public Integer context;
    @Nullable @SerializedName("theme") public Integer theme;
    @Nullable @SerializedName("expand_all_comments") public Boolean expandAllComments;
    @Nullable @SerializedName("ignore_whitespace") public IgnoreWhiteSpaceStrategy ignoreWhitespace;
    @Nullable @SerializedName("intraline_difference") public Boolean intralineDifference;
    @Nullable @SerializedName("line_length") public Integer lineLength;
    @Nullable @SerializedName("cursor_blink_rate") public Integer cursorBlinkRate;
    @Nullable @SerializedName("retain_header") public Boolean retainHeader;
    @Nullable @SerializedName("show_line_endings") public Boolean showLineEndings;
    @Nullable @SerializedName("show_tabs") public Boolean showTabs;
    @Nullable @SerializedName("show_whitespace_errors") public Boolean showWhitespaceErrors;
    @Nullable @SerializedName("skip_deleted") public Boolean skipDeleted;
    @Nullable @SerializedName("skipUncommented") public Boolean skipUncommented;
    @Nullable @SerializedName("syntax_highlighting") public Boolean syntaxHighlighting;
    @Nullable @SerializedName("hide_top_menu") public Boolean hideTopMenu;
    @Nullable @SerializedName("auto_hide_diff_table_header") public Boolean autoHideDiffTableHeader;
    @Nullable @SerializedName("hide_line_numbers") public Boolean hideLineNumbers;
    @Nullable @SerializedName("tab_size") public Integer tabSize;
    @Nullable @SerializedName("hide_empty_pane") public Boolean hideEmptyPane;
    @Nullable @SerializedName("match_brackets") public Boolean matchBrackets;
    @Nullable @SerializedName("line_wrapping") public Boolean lineWrapping;
    @Nullable @SerializedName("indent_with_tabs") public Boolean indentWithTabs;
}

