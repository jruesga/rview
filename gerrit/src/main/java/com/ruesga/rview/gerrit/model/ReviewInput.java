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

import java.util.List;
import java.util.Map;

/**
 * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#reviewer-input"
 */
public class ReviewInput {
    @Nullable @SerializedName("message") public String message;
    @Nullable @SerializedName("tag") public String tag;
    @Nullable @SerializedName("labels") public Map<String, Map<String, Integer>> labels;
    @Nullable @SerializedName("comments") public Map<String, List<CommentInput>> comments;
    @Nullable @SerializedName("strict_labels") public Boolean strictLabels;
    @Nullable @SerializedName("drafts") public DraftActionType drafts;
    @Nullable @SerializedName("notify") public NotifyType notify;
    @Nullable @SerializedName("omit_duplicate_comments") public Boolean omitDuplicateComments;
    @Nullable @SerializedName("on_behalf_of") public Boolean onBehalfOf;
}

