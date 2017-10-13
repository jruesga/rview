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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

/**
 * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#change-input"
 */
public class ChangeInput {
    @NonNull @SerializedName("project") public String project = "";
    @NonNull @SerializedName("branch") public String branch = "";
    @NonNull @SerializedName("subject") public String subject = "";
    @Nullable @SerializedName("topic") public String topic;
    @Nullable @SerializedName("status") public InitialChangeStatus status;
    @Nullable @SerializedName("is_private") public Boolean isPrivate;
    @Nullable @SerializedName("work_in_progress") public Boolean workInProgress;
    @Nullable @SerializedName("base_change") public String baseChange;
    @Nullable @SerializedName("new_branch") public Boolean newBranch;
    @Nullable @SerializedName("merge") public MergeInput merge;
    @Nullable @SerializedName("notify") public NotifyType notify;
    @Nullable @SerializedName("notify_details") public NotifyInfo notifyDetails;
}

