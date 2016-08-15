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
 * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-projects.html#config-info"
 */
public class ConfigInfo {
    @SerializedName("description") public String description;
    @SerializedName("use_contributor_agreements") public InheritBooleanInfo useContributorAgreements;
    @SerializedName("use_content_merge") public InheritBooleanInfo useContentMerge;
    @SerializedName("use_signed_off_by") public InheritBooleanInfo useSignedOffBy;
    @SerializedName("create_new_change_for_all_not_in_target") public InheritBooleanInfo createNewChangeForAllNotInTarget;
    @SerializedName("require_change_id") public InheritBooleanInfo requireChangeId;
    @SerializedName("enable_signed_push") public InheritBooleanInfo enableSignedPush;
    @SerializedName("require_signed_push") public InheritBooleanInfo requireSignedPush;
    @SerializedName("max_object_size_limit") public MaxObjectSizeLimitInfo maxObjectSizeLimit;
    @SerializedName("submit_type") public SubmitType submitType;
    @SerializedName("commentlinks") public Map<String, CommentLinkInfo> commentLinks;
    @SerializedName("theme") public ThemeInfo theme;
    @SerializedName("plugin_config") public Map<String, ConfigParameterInfo> pluginConfig;
    @SerializedName("actions") public Map<String, ActionInfo> actions;
}

