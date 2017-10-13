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

/**
 * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#preferences-info"
 */
public class PreferencesInfo {
    @SerializedName("changes_per_page") public int changesPerPage;
    @SerializedName("show_site_header") public boolean showSiteHeader;
    @SerializedName("use_flash_clipboard") public boolean useFlashClipboard;
    @SerializedName("download_scheme") public String downloadScheme;
    @SerializedName("download_command") public String downloadCommand;
    @SerializedName("copy_self_on_email") public Boolean copySelfOnEmail;
    @SerializedName("date_format") public DateFormat dateFormat;
    @SerializedName("time_format") public TimeFormat timeFormat;
    @SerializedName("relative_date_in_change_table") public boolean relativeDateInChangeTable;
    @SerializedName("size_bar_in_change_table") public boolean sizeBarInChangeTable;
    @SerializedName("legacycid_in_change_table") public boolean legacycidInChangeTable;
    @SerializedName("mute_common_path_prefixes") public boolean muteCommonPathPrefixes;
    @SerializedName("signed_off_by") public boolean signedOffBy;
    @SerializedName("review_category_strategy") public CategoryStrategy reviewCategoryStrategy;
    @SerializedName("my") public TopMenuItemInfo[] my;
    @SerializedName("diff_view") public DiffViewType diffView;
    @SerializedName("url_aliases") public Map<String, String> urlAliases;
    @SerializedName("email_strategy") public EmailStrategy emailStrategy;
    @SerializedName("default_base_for_merges") public DefaultBaseForMergesStrategy defaultBaseForMerges;
    @SerializedName("publish_comments_on_push") public Boolean publishCommentsOnPush;
}
