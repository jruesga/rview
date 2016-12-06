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
package com.ruesga.rview.model;

import com.google.gson.annotations.SerializedName;

public class Notification {
    @SerializedName("name") public long when;
    @SerializedName("token") public String token;
    @SerializedName("event") public int event;
    @SerializedName("change") public String change;
    @SerializedName("legacyChangeId") public int legacyChangeId;
    @SerializedName("revision") public String revision;
    @SerializedName("project") public String project;
    @SerializedName("branch") public String branch;
    @SerializedName("topic") public String topic;
    @SerializedName("author") public String author;
    @SerializedName("subject") public String subject;
    @SerializedName("extra") public String extra;
}
