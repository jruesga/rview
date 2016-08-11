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
 * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-config.html#auth-info"
 */
public class AuthInfo {
    @SerializedName("auth_type") public String authType;
    @SerializedName("use_contributor_agreements") public boolean useContributorAgreements;
    @SerializedName("editable_account_fields") public String[] editableAccountFields;
    @SerializedName("login_url") public String loginUrl;
    @SerializedName("login_text") public String loginText;
    @SerializedName("switch_account_url") public String switchAccountUrl;
    @SerializedName("register_url") public String registerUrl;
    @SerializedName("register_text") public String registerText;
    @SerializedName("edit_full_name_url") public String editFullNameUrl;
    @SerializedName("http_password_url") public String httpPasswordUrl;
    @SerializedName("is_git_basic_auth") public boolean isGitBasicAuth;
}

