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
 * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#label-info"
 */
public class LabelInfo {
    // Common fields
    @SerializedName("optional") public boolean optional;
    @SerializedName("default_value") public int defaultValue;

    // LABELS
    @SerializedName("approved") public AccountInfo approved;
    @SerializedName("rejected") public AccountInfo rejected;
    @SerializedName("recommended") public AccountInfo recommended;
    @SerializedName("disliked") public AccountInfo disliked;
    @SerializedName("blocking") public boolean blocking;
    @SerializedName("value") public int value;

    // DETAILED_LABELS
    @SerializedName("all") public ApprovalInfo[] all;
    @SerializedName("values") public Map<Integer, String> values;
}

