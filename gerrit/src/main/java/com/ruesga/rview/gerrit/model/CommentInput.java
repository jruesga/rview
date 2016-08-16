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

import java.util.Date;

/**
 * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#comment-input"
 */
public class CommentInput {
    @Nullable @SerializedName("id") public String id;
    @Nullable @SerializedName("path") public String path;
    @Nullable @SerializedName("side") public SideType side;
    @Nullable @SerializedName("line") public Integer line;
    @Nullable @SerializedName("range") public CommentRange range;
    @Nullable @SerializedName("in_reply_to") public String inReplyTo;
    @Nullable @SerializedName("updated") public Date updated;
    @Nullable @SerializedName("message") public String message;
    @Nullable @SerializedName("tag") public String tag;
}

