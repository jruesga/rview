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
public class AccountInfo {
    @SerializedName("_account_id") public int accountId;
    @SerializedName("name") public String name;
    @SerializedName("username") public String username;
    @SerializedName("email") public String email;
    @SerializedName("secondary_emails") public String[] secondaryEmails;
    @SerializedName("avatars") public AvatarInfo[] avatars;
    @SerializedName("status") public String status;
    @SerializedName("inactive") public boolean inactive;
}

