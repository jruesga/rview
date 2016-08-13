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
 * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#capability-info"
 */
public class CapabilityInfo {
    @SerializedName("accessDatabase") public boolean accessDatabase;
    @SerializedName("administrateServer") public boolean administrateServer;
    @SerializedName("createAccount") public boolean createAccount;
    @SerializedName("createGroup") public boolean createGroup;
    @SerializedName("createProject") public boolean createProject;
    @SerializedName("emailReviewers") public boolean emailReviewers;
    @SerializedName("flushCaches") public boolean flushCaches;
    @SerializedName("killTask") public boolean killTask;
    @SerializedName("maintainServer") public boolean maintainServer;
    @SerializedName("priority") public boolean priority;
    @SerializedName("queryLimit") public QueryLimitInfo queryLimit;
    @SerializedName("runAs") public boolean runAs;
    @SerializedName("runGC") public boolean runGC;
    @SerializedName("streamEvents") public boolean streamEvents;
    @SerializedName("viewAllAccounts") public boolean viewAllAccounts;
    @SerializedName("viewCaches") public boolean viewCaches;
    @SerializedName("viewConnections") public boolean viewConnections;
    @SerializedName("viewPlugins") public boolean viewPlugins;
    @SerializedName("viewQueue") public boolean viewQueue;
}
