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
 * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-projects.html#project-input"
 */
public class ProjectInput {
    @Nullable @SerializedName("name") public String name;
    @Nullable @SerializedName("parent") public String parent;
    @Nullable @SerializedName("description") public String description;
    @Nullable @SerializedName("permissions_only") public Boolean permissionsOnly;
    @Nullable @SerializedName("create_empty_commit") public Boolean createEmptyCommit;
    @Nullable @SerializedName("submit_type") public SubmitType submitType;
    @Nullable @SerializedName("branches") public String[] branches;
    @Nullable @SerializedName("owners") public String[] owners;
    @Nullable @SerializedName("use_contributor_agreements") public UseStatus useContributorAgreements;
    @Nullable @SerializedName("use_signed_off_by") public UseStatus useSignedOffBy;
    @Nullable @SerializedName("create_new_change_for_all_not_in_target") public UseStatus createNewChangeForAllNotInTarget;
    @Nullable @SerializedName("use_content_merge") public UseStatus useContentMerge;
    @Nullable @SerializedName("require_change_id") public UseStatus requireChangeId;
    @Nullable @SerializedName("enable_signed_push") public UseStatus enableSignedPush;
    @Nullable @SerializedName("require_signed_push") public UseStatus requireSignedPush;
    @Nullable @SerializedName("max_object_size_limit") public SizeLimitInfo maxObjectSizeLimit;
    @Nullable @SerializedName("plugin_config_values") public Map<String, Map<String, String>> pluginConfigValues;
    @Nullable @SerializedName("reject_empty_commit") public UseStatus rejectEmptyCommit;
}

