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

import java.util.Map;

/**
 * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-projects.html#config-input"
 */
public class ConfigInput {
    @Nullable @SerializedName("description") public String description;
    @Nullable @SerializedName("use_contributor_agreements") public UseStatus useContributorAgreements;
    @Nullable @SerializedName("use_content_merge") public UseStatus useContentMerge;
    @Nullable @SerializedName("use_signed_off_by") public UseStatus useSignedOffBy;
    @Nullable @SerializedName("create_new_change_for_all_not_in_target") public UseStatus createNewChangeForAllNotInTarget;
    @Nullable @SerializedName("require_change_id") public UseStatus requireChangeId;
    @Nullable @SerializedName("max_object_size_limit") public MaxObjectSizeLimitInfo maxObjectSizeLimit;
    @Nullable @SerializedName("submit_type") public SubmitStatus submitType;
    @Nullable @SerializedName("state") public ProjectStatus state;
    @Nullable @SerializedName("plugin_config_values") public Map<String, Map<String, String>> pluginConfigValues;
}

