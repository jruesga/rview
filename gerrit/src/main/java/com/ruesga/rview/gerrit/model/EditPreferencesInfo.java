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
 * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#edit-preferences-info"
 */
public class EditPreferencesInfo {
    @Deprecated @SerializedName("theme") public String theme;
    @Deprecated @SerializedName("key_map_type") public KeyMapType keyMapType;
    @SerializedName("tab_size") public int tabSize;
    @SerializedName("line_length") public int lineLength;
    @SerializedName("indent_unit") public int indentUnit;
    @SerializedName("cursor_blink_rate") public int cursorBlinkRate;
    @SerializedName("hide_top_menu") public boolean hideTopMenu;
    @SerializedName("show_tabs") public boolean showTabs;
    @SerializedName("show_whitespace_errors") public boolean showWhitespaceErrors;
    @SerializedName("syntax_highlighting") public boolean syntaxHighlighting;
    @SerializedName("hide_line_numbers") public boolean hideLineNumbers;
    @SerializedName("match_brackets") public boolean matchBrackets;
    @SerializedName("auto_close_brackets") public boolean autoCloseBrackets;
}
