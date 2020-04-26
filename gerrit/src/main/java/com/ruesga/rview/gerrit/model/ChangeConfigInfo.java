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
 * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-config.html#change-config-info"
 */
public class ChangeConfigInfo {
    @SerializedName("allow_blame") public boolean allowBlame;
    @SerializedName("allow_drafts") public boolean allowDrafts;
    @SerializedName("private_by_default") public boolean privateByDefault;
    @SerializedName("large_change") public int largeChange;
    @SerializedName("reply_label") public String replyLabel;
    @SerializedName("reply_tooltip") public String replyTooltip;
    @SerializedName("update_delay") public int updateDelay;
    @SerializedName("submit_whole_topic") public boolean submit_whole_topic;
    @SerializedName("exclude_mergeable_in_change_info") public boolean excludeMergeableInChangeInfo;
}
