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

/**
 * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#comment-info"
 */
public class CommentInfo {
    @SerializedName("patch_set") public int patchSet;
    @SerializedName("id") public String id;
    @SerializedName("path") public String path;
    @SerializedName("side") public SideType side;
    @SerializedName("parent") public String parent;
    @SerializedName("line") public int line;
    @SerializedName("range") public CommentRange range;
    @SerializedName("inReplyTo") public String inReplyTo;
    @SerializedName("message") public String message;
    @SerializedName("updated") public Date updated;
    @SerializedName("author") public AccountInfo author;
    @SerializedName("tag") public String tag;
}
