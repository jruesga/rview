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
 * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-config.html#task-info"
 */
public class TaskInfo {
    @SerializedName("id") public String id;
    @SerializedName("state") public TaskStatus state;
    @SerializedName("start_time") public Date startTime;
    @SerializedName("delay") public long delay;
    @SerializedName("command") public String command;
    @SerializedName("remote_name") public String remoteName;
    @SerializedName("project") public String project;
}

