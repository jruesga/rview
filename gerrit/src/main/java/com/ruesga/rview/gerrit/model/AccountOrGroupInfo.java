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
 * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#account-info"
 */
public class AccountOrGroupInfo {
    // Shared properties
    @SerializedName("name") public String name;

    // Account
    @SerializedName("_account_id") public int accountId;
    @SerializedName("username") public String username;
    @SerializedName("email") public String email;
    @SerializedName("secondary_emails") public String[] secondaryEmails;
    @SerializedName("avatars") public AvatarInfo[] avatars;

    // Group
    @SerializedName("id") public String id;
    @SerializedName("url") public String url;
    @SerializedName("options") public GroupOptionsInfo options;
    @SerializedName("description") public String description;
    @SerializedName("group_id") public int groupId;
    @SerializedName("owner") public String owner;
    @SerializedName("owner_id") public int ownerId;
    @SerializedName("members") public AccountInfo[] members;
    @SerializedName("includes") public GroupInfo[] includes;
}

