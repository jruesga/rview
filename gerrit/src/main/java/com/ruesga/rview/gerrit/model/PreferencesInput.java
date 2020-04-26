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

import java.util.Map;

import androidx.annotation.Nullable;

/**
 * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#preferences-input"
 */
public class PreferencesInput {
    @Nullable @SerializedName("changes_per_page") public Integer changesPerPage;
    @Deprecated @Nullable @SerializedName("show_site_header") public Boolean showSiteHeader;
    @Deprecated @Nullable @SerializedName("use_flash_clipboard") public Boolean useFlashClipboard;
    @Nullable @SerializedName("download_scheme") public String downloadScheme;
    @Deprecated @Nullable @SerializedName("download_command") public String downloadCommand;
    @Nullable @SerializedName("copy_self_on_email") public Boolean copySelfOnEmail;
    @Nullable @SerializedName("date_format") public DateFormat dateFormat;
    @Nullable @SerializedName("time_format") public TimeFormat timeFormat;
    @Nullable @SerializedName("relative_date_in_change_table") public Boolean relativeDateInChangeTable;
    @Nullable @SerializedName("size_bar_in_change_table") public Boolean sizeBarInChangeTable;
    @Nullable @SerializedName("legacycid_in_change_table") public Boolean legacycidInChangeTable;
    @Nullable @SerializedName("mute_common_path_prefixes") public Boolean muteCommonPathPrefixes;
    @Nullable @SerializedName("signed_off_by") public Boolean signedOffBy;
    @Deprecated @Nullable @SerializedName("review_category_strategy") public CategoryStrategy reviewCategoryStrategy;
    @Nullable @SerializedName("my") public TopMenuItemInfo[] my;
    @Nullable @SerializedName("diff_view") public DiffViewType diffView;
    @Deprecated @Nullable @SerializedName("url_aliases") public Map<String, String> urlAliases;
    @Nullable @SerializedName("email_strategy") public EmailStrategy emailStrategy;
}

