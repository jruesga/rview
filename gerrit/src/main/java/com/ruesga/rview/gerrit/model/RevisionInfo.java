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
 * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#revision-info"
 */
public class RevisionInfo {
    @SerializedName("draft") public boolean draft;
    @SerializedName("kind") public ChangeKind kind;
    @SerializedName("_number") public int number;
    @SerializedName("created") public Date created;
    @SerializedName("uploader") public AccountInfo uploader;
    @SerializedName("ref") public String ref;
    @SerializedName("fetch") public Map<String, FetchInfo> fetch;
    @SerializedName("commit") public CommitInfo commit;
    @SerializedName("files") public Map<String, FileInfo> files;
    @SerializedName("actions") public Map<String, ActionInfo> actions;
    @SerializedName("reviewed") public boolean reviewed;
    @SerializedName("messageWithFooter") private String messageWithFooter;
    @SerializedName("commitWithFooters") private String commitWithFooters;
    @SerializedName("push_certificate") public PushCertificateInfo pushCertificate;
    @SerializedName("description") public String description;

    public String commitWithFooters() {
        return commitWithFooters != null ? commitWithFooters : messageWithFooter;
    }
}

