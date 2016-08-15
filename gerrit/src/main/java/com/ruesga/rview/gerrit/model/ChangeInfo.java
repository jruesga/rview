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

import java.util.Date;
import java.util.Map;

/**
 * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#change-info"
 */
public class ChangeInfo {
    @SerializedName("id") public String id;
    @SerializedName("project") public String project;
    @SerializedName("branch") public String branch;
    @SerializedName("topic") public String topic;
    @SerializedName("change_id") public String changeId;
    @SerializedName("subject") public String subject;
    @SerializedName("status") public ChangeStatus status;
    @SerializedName("created") public Date created;
    @SerializedName("updated") public Date updated;
    @SerializedName("starred") public boolean starred;
    @SerializedName("stars") public String[] stars;
    @SerializedName("reviewed") public boolean reviewed;
    @SerializedName("submit_type") public SubmitType submitType;
    @SerializedName("mergeable") public boolean mergeable;
    @SerializedName("insertions") public int insertions;
    @SerializedName("deletions") public int deletions;
    @SerializedName("_number") public int legacyChangeId;
    @SerializedName("owner") public AccountInfo owner;
    @SerializedName("actions") public Map<String, ActionInfo> actions;
    @SerializedName("labels") public Map<String, LabelInfo> labels;
    @SerializedName("permitted_labels") public Map<String, Integer[]> permittedLabels;
    @SerializedName("removable_reviewers") public AccountInfo[] removableReviewers;
    @SerializedName("reviewers") public Map<ReviewerStatus, AccountInfo[]> reviewers;
    @SerializedName("reviewer_updates") public ReviewerUpdateInfo[] reviewerUpdates;
    @SerializedName("messages") public ChangeMessageInfo[] messages;
    @SerializedName("current_revision") public String currentRevision;
    @SerializedName("revisions") public Map<String, RevisionInfo> revisions;
    @SerializedName("problems") public ProblemInfo[] problems;
}

