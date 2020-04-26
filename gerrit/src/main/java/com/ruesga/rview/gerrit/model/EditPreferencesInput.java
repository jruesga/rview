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

import androidx.annotation.Nullable;

/**
 * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#edit-preferences-info"
 */
public class EditPreferencesInput {
    @Deprecated @Nullable @SerializedName("theme") public String theme;
    @Deprecated @Nullable @SerializedName("key_map_type") public KeyMapType keyMapType;
    @Nullable @SerializedName("tab_size") public Integer tabSize;
    @Nullable @SerializedName("line_length") public Integer lineLength;
    @Nullable @SerializedName("indent_unit") public Integer indentUnit;
    @Nullable @SerializedName("cursor_blink_rate") public Integer cursorBlinkRate;
    @Nullable @SerializedName("hide_top_menu") public Boolean hideTopMenu;
    @Nullable @SerializedName("show_tabs") public Boolean showTabs;
    @Nullable @SerializedName("show_whitespace_errors") public Boolean showWhitespaceErrors;
    @Nullable @SerializedName("syntax_highlighting") public Boolean syntaxHighlighting;
    @Nullable @SerializedName("hide_line_numbers") public Boolean hideLineNumbers;
    @Nullable @SerializedName("match_brackets") public Boolean matchBrackets;
    @Nullable @SerializedName("auto_close_brackets") public Boolean autoCloseBrackets;
}

