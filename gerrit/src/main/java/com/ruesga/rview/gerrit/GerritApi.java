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
package com.ruesga.rview.gerrit;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.ruesga.rview.gerrit.filter.AccountQuery;
import com.ruesga.rview.gerrit.filter.ChangeQuery;
import com.ruesga.rview.gerrit.model.AccountDetailInfo;
import com.ruesga.rview.gerrit.model.AccountInfo;
import com.ruesga.rview.gerrit.model.AccountInput;
import com.ruesga.rview.gerrit.model.AccountNameInput;
import com.ruesga.rview.gerrit.model.AccountOptions;
import com.ruesga.rview.gerrit.model.AddGpgKeyInput;
import com.ruesga.rview.gerrit.model.Capability;
import com.ruesga.rview.gerrit.model.CapabilityInfo;
import com.ruesga.rview.gerrit.model.ChangeInfo;
import com.ruesga.rview.gerrit.model.ChangeOptions;
import com.ruesga.rview.gerrit.model.ConfigInfo;
import com.ruesga.rview.gerrit.model.ConfigInput;
import com.ruesga.rview.gerrit.model.ContributorAgreementInfo;
import com.ruesga.rview.gerrit.model.ContributorAgreementInput;
import com.ruesga.rview.gerrit.model.DeleteGpgKeyInput;
import com.ruesga.rview.gerrit.model.DeleteProjectWatchInput;
import com.ruesga.rview.gerrit.model.DiffPreferencesInfo;
import com.ruesga.rview.gerrit.model.DiffPreferencesInput;
import com.ruesga.rview.gerrit.model.EditPreferencesInfo;
import com.ruesga.rview.gerrit.model.EditPreferencesInput;
import com.ruesga.rview.gerrit.model.EmailInfo;
import com.ruesga.rview.gerrit.model.EmailInput;
import com.ruesga.rview.gerrit.model.GcInput;
import com.ruesga.rview.gerrit.model.GpgKeyInfo;
import com.ruesga.rview.gerrit.model.GroupInfo;
import com.ruesga.rview.gerrit.model.HeadInput;
import com.ruesga.rview.gerrit.model.HttpPasswordInput;
import com.ruesga.rview.gerrit.model.OAuthTokenInfo;
import com.ruesga.rview.gerrit.model.PreferencesInfo;
import com.ruesga.rview.gerrit.model.PreferencesInput;
import com.ruesga.rview.gerrit.model.ProjectAccessInfo;
import com.ruesga.rview.gerrit.model.ProjectDescriptionInput;
import com.ruesga.rview.gerrit.model.ProjectInfo;
import com.ruesga.rview.gerrit.model.ProjectInput;
import com.ruesga.rview.gerrit.model.ProjectParentInput;
import com.ruesga.rview.gerrit.model.ProjectType;
import com.ruesga.rview.gerrit.model.ProjectWatchInfo;
import com.ruesga.rview.gerrit.model.ProjectWatchInput;
import com.ruesga.rview.gerrit.model.RepositoryStatisticsInfo;
import com.ruesga.rview.gerrit.model.ServerInfo;
import com.ruesga.rview.gerrit.model.ServerVersion;
import com.ruesga.rview.gerrit.model.SshKeyInfo;
import com.ruesga.rview.gerrit.model.StarInput;
import com.ruesga.rview.gerrit.model.UsernameInput;

import java.util.List;
import java.util.Map;

import okhttp3.Response;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;
import rx.Observable;

/**
 * Gerrit REST api
 */
public interface GerritApi {

    /**
     * The current supported Gerrit api version
     */
    double VERSION = 2.12;

    // ===============================
    // Gerrit access endpoints
    // @link "https://gerrit-review.googlesource.com/Documentation/rest-api-access.html"
    // ===============================

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-access.html#list-access"
     */
    @GET("access/")
    Observable<Map<String, ProjectAccessInfo>> getAccessRights(
            @NonNull @Query("project") String[] names);



    // ===============================
    // Gerrit accounts endpoints
    // @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html"
    // ===============================

    /**
     * The own account id (requires and authenticated account)
     */
    String SELF_ACCOUNT = "self";

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#query-account"
     */
    @GET("accounts/")
    Observable<List<AccountInfo>> getAccountsSuggestions(
            @NonNull @Query("q") String query,
            @Nullable @Query("n") Integer count);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#query-account"
     */
    @GET("accounts/")
    Observable<List<AccountInfo>> getAccounts(
            @NonNull @Query("q") AccountQuery query,
            @Nullable @Query("n") Integer count,
            @Nullable @Query("S") Integer start,
            @Nullable @Query("o") AccountOptions[] options);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#get-account"
     */
    @GET("accounts/{account-id}")
    Observable<AccountInfo> getAccount(@NonNull @Path("account-id") String accountId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#create-account"
     */
    @Headers({"Content-Type: application/json; charset=UTF-8"})
    @PUT("accounts/{username}")
    Observable<AccountInfo> createAccount(
            @NonNull @Path("username") String username,
            @NonNull @Body AccountInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#get-detail"
     */
    @GET("accounts/{account-id}/detail")
    Observable<AccountDetailInfo> getAccountDetails(@NonNull @Path("account-id") String accountId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#get-account-name"
     */
    @GET("accounts/{account-id}/name")
    Observable<String> getAccountName(@NonNull @Path("account-id") String accountId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#set-account-name"
     */
    @Headers({"Content-Type: application/json; charset=UTF-8"})
    @PUT("accounts/{account-id}/name")
    Observable<String> setAccountName(
            @NonNull @Path("account-id") String accountId,
            @NonNull @Body AccountNameInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#delete-account-name"
     */
    @DELETE("accounts/{account-id}/name")
    Observable<Void> deleteAccountName(@NonNull @Path("account-id") String accountId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#get-username"
     */
    @GET("accounts/{account-id}/username")
    Observable<String> getAccountUsername(@NonNull @Path("account-id") String accountId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#set-username"
     */
    @Headers({"Content-Type: application/json; charset=UTF-8"})
    @PUT("accounts/{account-id}/username")
    Observable<String> setAccountUsername(
            @NonNull @Path("account-id") String accountId,
            @NonNull @Body UsernameInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#get-active"
     */
    @GET("accounts/{account-id}/active")
    Observable<String> isAccountActive(@NonNull @Path("account-id") String accountId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#set-active"
     */
    @PUT("accounts/{account-id}/active")
    Observable<Void> setAccountAsActive(@NonNull @Path("account-id") String accountId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#delete-active"
     */
    @DELETE("accounts/{account-id}/active")
    Observable<Void> setAccountAsInactive(@NonNull @Path("account-id") String accountId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#get-http-password"
     */
    @GET("accounts/{account-id}/password.http")
    Observable<String> getHttpPassword(@NonNull @Path("account-id") String accountId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#set-http-password"
     */
    @Headers({"Content-Type: application/json; charset=UTF-8"})
    @PUT("accounts/{account-id}/password.http")
    Observable<String> setHttpPassword(
            @NonNull @Path("account-id") String accountId,
            @NonNull @Body HttpPasswordInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#delete-http-password"
     */
    @DELETE("accounts/{account-id}/password.http")
    Observable<Void> deleteHttpPassword(@NonNull @Path("account-id") String accountId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#get-oauth-token"
     */
    @GET("accounts/{account-id}/oauthtoken")
    Observable<OAuthTokenInfo> getOAuthToken(@NonNull @Path("account-id") String accountId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#list-account-emails"
     */
    @GET("accounts/{account-id}/emails")
    Observable<List<EmailInfo>> getAccountEmails(@NonNull @Path("account-id") String accountId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#list-account-email"
     */
    @GET("accounts/{account-id}/emails/{email-id}")
    Observable<EmailInfo> getAccountEmail(
            @NonNull @Path("account-id") String accountId,
            @NonNull @Path("email-id") String emailId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#create-account-email"
     */
    @Headers({"Content-Type: application/json; charset=UTF-8"})
    @PUT("accounts/{account-id}/emails/{email-id}")
    Observable<EmailInfo> createAccountEmail(
            @NonNull @Path("account-id") String accountId,
            @NonNull @Path("email-id") String emailId,
            @NonNull @Body EmailInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#delete-account-email"
     */
    @DELETE("accounts/{account-id}/emails/{email-id}")
    Observable<Void> deleteAccountEmail(
            @NonNull @Path("account-id") String accountId,
            @NonNull @Path("email-id") String emailId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#set-preferred-email"
     */
    @PUT("accounts/{account-id}/emails/{email-id}/preferred")
    Observable<Void> setAccountPreferredEmail(
            @NonNull @Path("account-id") String accountId,
            @NonNull @Path("email-id") String emailId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#list-ssh-keys"
     */
    @GET("accounts/{account-id}/sshkeys")
    Observable<List<SshKeyInfo>> getAccountSshKeys(@NonNull @Path("account-id") String accountId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#get-ssh-key"
     */
    @GET("accounts/{account-id}/sshkeys/{ssh-key-id}")
    Observable<SshKeyInfo> getAccountSshKey(
            @NonNull @Path("account-id") String accountId,
            @Path("ssh-key-id") int sshKeyId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#add-ssh-key"
     */
    @Headers({"Content-Type: plain/text"})
    @POST("accounts/{account-id}/sshkeys")
    Observable<SshKeyInfo> addAccountSshKey(
            @NonNull @Path("account-id") String accountId,
            @NonNull @Body String encodedKey);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#delete-ssh-key"
     */
    @DELETE("accounts/{account-id}/sshkeys/{ssh-key-id}")
    Observable<Void> deleteAccountSshKey(
            @NonNull @Path("account-id") String accountId,
            @Path("ssh-key-id") int sshKeyId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#list-gpg-keys"
     */
    @GET("accounts/{account-id}/gpgkeys")
    Observable<List<GpgKeyInfo>> getAccountGpgKeys(@NonNull @Path("account-id") String accountId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#get-gpg-key"
     */
    @GET("accounts/{account-id}/gpgkeys/{gpg-key-id}")
    Observable<GpgKeyInfo> getAccountGpgKey(
            @NonNull @Path("account-id") String accountId,
            @NonNull @Path("gpg-key-id") String gpgKeyId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#add-delete-gpg-keys"
     */
    @Headers({"Content-Type: application/json; charset=UTF-8"})
    @POST("accounts/{account-id}/gpgkeys")
    Observable<Map<String, GpgKeyInfo>> addAccountGpgKeys(
            @NonNull @Path("account-id") String accountId,
            @NonNull @Body AddGpgKeyInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#add-delete-gpg-keys"
     */
    @Headers({"Content-Type: application/json; charset=UTF-8"})
    @POST("accounts/{account-id}/gpgkeys")
    Observable<Map<String, GpgKeyInfo>> deleteAccountGpgKeys(
            @NonNull @Path("account-id") String accountId,
            @NonNull @Body DeleteGpgKeyInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#list-account-capabilities"
     */
    @GET("accounts/{account-id}/capabilities")
    Observable<CapabilityInfo> getAccountCapabilities(
            @NonNull @Path("account-id") String accountId,
            @Nullable @Query("q") Capability[] filter);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#check-account-capabilities"
     */
    @GET("accounts/{account-id}/capabilities/{capability-id}")
    Observable<String> hasAccountCapability(
            @NonNull @Path("account-id") String accountId,
            @NonNull @Path("capability-id") Capability capabilityId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#list-groups"
     */
    @GET("accounts/{account-id}/groups")
    Observable<List<GroupInfo>> getAccountGroups(@NonNull @Path("account-id") String accountId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#get-avatar"
     */
    @GET("accounts/{account-id}/avatar")
    Observable<Response> getAccountAvatar(@NonNull @Path("account-id") String accountId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#get-avatar-change-url"
     */
    @GET("accounts/{account-id}/avatar.change.url")
    Observable<String> getAccountAvatarChangeUrl(@NonNull @Path("account-id") String accountId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#get-user-preferences"
     */
    @GET("accounts/{account-id}/preferences")
    Observable<PreferencesInfo> getAccountPreferences(
            @NonNull @Path("account-id") String accountId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#set-user-preferences"
     */
    @Headers({"Content-Type: application/json; charset=UTF-8"})
    @PUT("accounts/{account-id}/preferences")
    Observable<PreferencesInfo> setAccountPreferences(
            @NonNull @Path("account-id") String accountId,
            @NonNull @Body PreferencesInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#get-diff-preferences"
     */
    @GET("accounts/{account-id}/preferences.diff")
    Observable<DiffPreferencesInfo> getAccountDiffPreferences(
            @NonNull @Path("account-id") String accountId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#set-diff-preferences"
     */
    @Headers({"Content-Type: application/json; charset=UTF-8"})
    @PUT("accounts/{account-id}/preferences.diff")
    Observable<DiffPreferencesInfo> setAccountDiffPreferences(
            @NonNull @Path("account-id") String accountId,
            @NonNull @Body DiffPreferencesInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#get-edit-preferences"
     */
    @GET("accounts/{account-id}/preferences.edit")
    Observable<EditPreferencesInfo> getAccountEditPreferences(
            @NonNull @Path("account-id") String accountId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#set-edit-preferences"
     */
    @Headers({"Content-Type: application/json; charset=UTF-8"})
    @PUT("accounts/{account-id}/preferences.edit")
    Observable<EditPreferencesInfo> setAccountEditPreferences(
            @NonNull @Path("account-id") String accountId,
            @NonNull @Body EditPreferencesInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#get-watched-projects"
     */
    @GET("accounts/{account-id}/watched.projects")
    Observable<List<ProjectWatchInfo>> getAccountWatchedProjects(
            @NonNull @Path("account-id") String accountId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#set-watched-projects"
     */
    @Headers({"Content-Type: application/json; charset=UTF-8"})
    @POST("accounts/{account-id}/watched.projects")
    Observable<List<ProjectWatchInfo>> addOrUpdateAccountWatchedProjects(
            @NonNull @Path("account-id") String accountId,
            @NonNull @Body ProjectWatchInput[] input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#delete-watched-projects"
     */
    @Headers({"Content-Type: application/json; charset=UTF-8"})
    @POST("accounts/{account-id}/watched.projects")
    Observable<Void> deleteAccountWatchedProjects(
            @NonNull @Path("account-id") String accountId,
            @NonNull @Body DeleteProjectWatchInput[] input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#get-changes-with-default-star"
     */
    @GET("accounts/{account-id}/starred.changes")
    Observable<List<ChangeInfo>> getDefaultStarredChanges(
            @NonNull @Path("account-id") String accountId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#star-change"
     */
    @PUT("accounts/{account-id}/starred.changes/{change-id}")
    Observable<Void> putDefaultStarOnChange(
            @NonNull @Path("account-id") String accountId,
            @NonNull @Path("change-id") String changeId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#unstar-change"
     */
    @PUT("accounts/{account-id}/starred.changes/{change-id}")
    Observable<Void> removeDefaultStarFromChange(
            @NonNull @Path("account-id") String accountId,
            @NonNull @Path("change-id") String changeId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#get-starred-changes"
     */
    @GET("accounts/{account-id}/stars.changes")
    Observable<List<ChangeInfo>> getStarredChanges(
            @NonNull @Path("account-id") String accountId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#get-stars"
     */
    @GET("accounts/{account-id}/stars.changes/{change-id}")
    Observable<List<String>> getStarLabelsFromChange(
            @NonNull @Path("account-id") String accountId,
            @NonNull @Path("change-id") String changeId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#set-stars"
     */
    @Headers({"Content-Type: application/json; charset=UTF-8"})
    @POST("accounts/{account-id}/stars.changes/{change-id}")
    Observable<List<String>> updateStarLabelsFromChange(
            @NonNull @Path("account-id") String accountId,
            @NonNull @Path("change-id") String changeId,
            @NonNull @Body StarInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#list-contributor-agreements"
     */
    @GET("accounts/{account-id}/agreements")
    Observable<List<ContributorAgreementInfo>> getContributorAgreements(
            @NonNull @Path("account-id") String accountId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#list-contributor-agreements"
     */
    @Headers({"Content-Type: application/json; charset=UTF-8"})
    @PUT("accounts/{account-id}/agreements")
    Observable<String> signContributorAgreement(
            @NonNull @Path("account-id") String accountId,
            @NonNull @Body ContributorAgreementInput input);



    // ===============================
    // Gerrit changes endpoints
    // @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html"
    // ===============================

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#list-changes"
     */
    @GET("changes/")
    Observable<List<ChangeInfo>> getChanges(
            @NonNull @Query("q") ChangeQuery query,
            @Nullable @Query("n") Integer count,
            @Nullable @Query("S") Integer start,
            @Nullable @Query("o") ChangeOptions[] options);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#get-change"
     */
    @GET("changes/{change-id}")
    Observable<ChangeInfo> getChange(
            @NonNull @Path("change-id") String changeId,
            @Nullable @Query("o") ChangeOptions[] options);


    // ===============================
    // Gerrit configuration endpoints
    // @link "https://gerrit-review.googlesource.com/Documentation/rest-api-config.html"
    // ===============================

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-config.html#get-version"
     */
    @GET("config/server/version")
    Observable<ServerVersion> getServerVersion();

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-config.html#get-info"
     */
    @GET("config/server/info")
    Observable<ServerInfo> getServerInfo();

    // ===============================
    // Gerrit groups endpoints
    // @link "https://gerrit-review.googlesource.com/Documentation/rest-api-groups.html"
    // ===============================

    // ===============================
    // Gerrit plugins endpoints
    // @link "https://gerrit-review.googlesource.com/Documentation/rest-api-plugins.html"
    // ===============================

    // ===============================
    // Gerrit projects endpoints
    // @link "https://gerrit-review.googlesource.com/Documentation/rest-api-projects.html"
    // ===============================

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-projects.html#list-projects"
     */
    @GET("projects/")
    Observable<Map<String, ProjectInfo>> getProjects(
            @Nullable @Query("d") Boolean showDescription,
            @Nullable @Query("t") Boolean showTree,
            @Nullable @Query("b") String branch,
            @Nullable @Query("type") ProjectType type,
            @Nullable @Query("has-acl-for") String group);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-projects.html#get-project"
     */
    @GET("projects/{project-name}")
    Observable<ProjectInfo> getProject(@NonNull @Path("project-name") String name);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-projects.html#create-project"
     */
    @Headers({"Content-Type: application/json; charset=UTF-8"})
    @PUT("projects/{project-name}")
    Observable<ProjectInfo> createProject(
            @NonNull @Path("project-name") String name,
            @NonNull @Body ProjectInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-projects.html#get-project-description"
     */
    @GET("projects/{project-name}/description")
    Observable<String> getProjectDescription(@NonNull @Path("project-name") String name);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-projects.html#set-project-description"
     */
    @Headers({"Content-Type: application/json; charset=UTF-8"})
    @PUT("projects/{project-name}/description")
    Observable<String> setProjectDescription(
            @NonNull @Path("project-name") String name,
            @NonNull @Body ProjectDescriptionInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-projects.html#delete-project-description"
     */
    @DELETE("projects/{project-name}/description")
    Observable<Void> deleteProjectDescription(@NonNull @Path("project-name") String name);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-projects.html#get-project-parent"
     */
    @GET("projects/{project-name}/parent")
    Observable<String> getProjectParent(@NonNull @Path("project-name") String name);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-projects.html#set-project-parent"
     */
    @Headers({"Content-Type: application/json; charset=UTF-8"})
    @PUT("projects/{project-name}/parent")
    Observable<String> setProjectParent(
            @NonNull @Path("project-name") String name,
            @NonNull @Body ProjectParentInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-projects.html#get-head"
     */
    @GET("projects/{project-name}/HEAD")
    Observable<String> getProjectHead(@NonNull @Path("project-name") String name);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-projects.html#set-head"
     */
    @Headers({"Content-Type: application/json; charset=UTF-8"})
    @PUT("projects/{project-name}/HEAD")
    Observable<String> setProjectHead(
            @NonNull @Path("project-name") String name,
            @NonNull @Body HeadInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-projects.html#get-repository-statistics"
     */
    @GET("projects/{project-name}/statistics.git")
    Observable<RepositoryStatisticsInfo> getProjectStatistics(
            @NonNull @Path("project-name") String name);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-projects.html#get-config"
     */
    @GET("projects/{project-name}/config")
    Observable<ConfigInfo> getProjectConfig(@NonNull @Path("project-name") String name);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-projects.html#set-config"
     */
    @Headers({"Content-Type: application/json; charset=UTF-8"})
    @PUT("projects/{project-name}/config")
    Observable<ConfigInfo> setProjectConfig(
            @NonNull @Path("project-name") String name,
            @NonNull @Body ConfigInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-projects.html#run-gc"
     */
    @Headers({"Content-Type: application/json; charset=UTF-8"})
    @POST("projects/{project-name}/gc")
    Observable<Response> runProjectGc(
            @NonNull @Path("project-name") String name,
            @NonNull @Body GcInput input);
}
