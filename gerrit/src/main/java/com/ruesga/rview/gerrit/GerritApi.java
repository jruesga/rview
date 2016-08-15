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
import com.ruesga.rview.gerrit.model.AbandonInput;
import com.ruesga.rview.gerrit.model.AccountDetailInfo;
import com.ruesga.rview.gerrit.model.AccountInfo;
import com.ruesga.rview.gerrit.model.AccountInput;
import com.ruesga.rview.gerrit.model.AccountNameInput;
import com.ruesga.rview.gerrit.model.AccountOptions;
import com.ruesga.rview.gerrit.model.AddGpgKeyInput;
import com.ruesga.rview.gerrit.model.AddReviewerResultInfo;
import com.ruesga.rview.gerrit.model.Capability;
import com.ruesga.rview.gerrit.model.CapabilityInfo;
import com.ruesga.rview.gerrit.model.ChangeInfo;
import com.ruesga.rview.gerrit.model.ChangeInput;
import com.ruesga.rview.gerrit.model.ChangeOptions;
import com.ruesga.rview.gerrit.model.CommentInfo;
import com.ruesga.rview.gerrit.model.ConfigInfo;
import com.ruesga.rview.gerrit.model.ConfigInput;
import com.ruesga.rview.gerrit.model.ContributorAgreementInfo;
import com.ruesga.rview.gerrit.model.ContributorAgreementInput;
import com.ruesga.rview.gerrit.model.DeleteGpgKeyInput;
import com.ruesga.rview.gerrit.model.DeleteProjectWatchInput;
import com.ruesga.rview.gerrit.model.DeleteVoteInput;
import com.ruesga.rview.gerrit.model.DiffPreferencesInfo;
import com.ruesga.rview.gerrit.model.DiffPreferencesInput;
import com.ruesga.rview.gerrit.model.EditPreferencesInfo;
import com.ruesga.rview.gerrit.model.EditPreferencesInput;
import com.ruesga.rview.gerrit.model.EmailInfo;
import com.ruesga.rview.gerrit.model.EmailInput;
import com.ruesga.rview.gerrit.model.FixInput;
import com.ruesga.rview.gerrit.model.GcInput;
import com.ruesga.rview.gerrit.model.GpgKeyInfo;
import com.ruesga.rview.gerrit.model.GroupInfo;
import com.ruesga.rview.gerrit.model.HeadInput;
import com.ruesga.rview.gerrit.model.HttpPasswordInput;
import com.ruesga.rview.gerrit.model.IncludeInInfo;
import com.ruesga.rview.gerrit.model.MoveInput;
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
import com.ruesga.rview.gerrit.model.RebaseInput;
import com.ruesga.rview.gerrit.model.RepositoryStatisticsInfo;
import com.ruesga.rview.gerrit.model.RestoreInput;
import com.ruesga.rview.gerrit.model.RevertInput;
import com.ruesga.rview.gerrit.model.ReviewerInfo;
import com.ruesga.rview.gerrit.model.ReviewerInput;
import com.ruesga.rview.gerrit.model.ServerInfo;
import com.ruesga.rview.gerrit.model.ServerVersion;
import com.ruesga.rview.gerrit.model.SshKeyInfo;
import com.ruesga.rview.gerrit.model.StarInput;
import com.ruesga.rview.gerrit.model.SubmitInput;
import com.ruesga.rview.gerrit.model.SubmittedTogetherInfo;
import com.ruesga.rview.gerrit.model.SubmittedTogetherOptions;
import com.ruesga.rview.gerrit.model.SuggestedReviewerInfo;
import com.ruesga.rview.gerrit.model.TopicInput;
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
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#create-change"
     */
    @Headers({"Content-Type: application/json; charset=UTF-8"})
    @POST("changes/")
    Observable<ChangeInfo> createChange(@NonNull @Body ChangeInput input);

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

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#get-change-detail"
     */
    @GET("changes/{change-id}/detail")
    Observable<ChangeInfo> getChangeDetail(
            @NonNull @Path("change-id") String changeId,
            @Nullable @Query("o") ChangeOptions[] options);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#get-topic"
     */
    @GET("changes/{change-id}/topic")
    Observable<String> getChangeTopic(@NonNull @Path("change-id") String changeId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#set-topic"
     */
    @Headers({"Content-Type: application/json; charset=UTF-8"})
    @PUT("changes/{change-id}/topic")
    Observable<String> setChangeTopic(
            @NonNull @Path("change-id") String changeId,
            @NonNull @Body TopicInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#delete-topic"
     */
    @DELETE("changes/{change-id}/topic")
    Observable<Void> deleteChangeTopic(@NonNull @Path("change-id") String changeId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#abandon-change"
     */
    @Headers({"Content-Type: application/json; charset=UTF-8"})
    @POST("changes/{change-id}/abandon")
    Observable<ChangeInfo> abandonChange(
            @NonNull @Path("change-id") String changeId,
            @NonNull @Body AbandonInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#restore-change"
     */
    @Headers({"Content-Type: application/json; charset=UTF-8"})
    @POST("changes/{change-id}/restore")
    Observable<ChangeInfo> restoreChange(
            @NonNull @Path("change-id") String changeId,
            @NonNull @Body RestoreInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#rebase-change"
     */
    @Headers({"Content-Type: application/json; charset=UTF-8"})
    @POST("changes/{change-id}/rebase")
    Observable<ChangeInfo> rebaseChange(
            @NonNull @Path("change-id") String changeId,
            @NonNull @Body RebaseInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#move-change"
     */
    @Headers({"Content-Type: application/json; charset=UTF-8"})
    @POST("changes/{change-id}/move")
    Observable<ChangeInfo> moveChange(
            @NonNull @Path("change-id") String changeId,
            @NonNull @Body MoveInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#revert-change"
     */
    @Headers({"Content-Type: application/json; charset=UTF-8"})
    @POST("changes/{change-id}/revert")
    Observable<ChangeInfo> revertChange(
            @NonNull @Path("change-id") String changeId,
            @NonNull @Body RevertInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#submit-change"
     */
    @Headers({"Content-Type: application/json; charset=UTF-8"})
    @POST("changes/{change-id}/submit")
    Observable<ChangeInfo> submitChange(
            @NonNull @Path("change-id") String changeId,
            @NonNull @Body SubmitInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#submitted-together"
     */
    @GET("changes/{change-id}/submitted_together")
    Observable<SubmittedTogetherInfo> getChangesSubmittedTogether(
            @NonNull @Path("change-id") String changeId,
            @Nullable @Query("o") SubmittedTogetherOptions[] options);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#publish-draft-change"
     */
    @POST("changes/{change-id}/publish")
    Observable<Void> publishDraftChange(@NonNull @Path("change-id") String changeId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#delete-draft-change"
     */
    @DELETE("changes/{change-id}")
    Observable<Void> deleteDraftChange(@NonNull @Path("change-id") String changeId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#get-included-in"
     */
    @GET("changes/{change-id}/in")
    Observable<IncludeInInfo> getChangeIncludedIn(@NonNull @Path("change-id") String changeId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#index-change"
     */
    @POST("changes/{change-id}/index")
    Observable<Void> indexChange(@NonNull @Path("change-id") String changeId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#list-change-comments"
     */
    @GET("changes/{change-id}/comments")
    Observable<Map<String, List<CommentInfo>>> getChangeComments(
            @NonNull @Path("change-id") String changeId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#list-change-drafts"
     */
    @GET("changes/{change-id}/drafts")
    Observable<Map<String, List<CommentInfo>>> getChangeDraftComments(
            @NonNull @Path("change-id") String changeId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#check-change"
     */
    @GET("changes/{change-id}/check")
    Observable<ChangeInfo> checkChange(@NonNull @Path("change-id") String changeId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#fix-change"
     */
    @Headers({"Content-Type: application/json; charset=UTF-8"})
    @POST("changes/{change-id}/check")
    Observable<ChangeInfo> fixChange(
            @NonNull @Path("change-id") String changeId,
            @NonNull @Body FixInput input);

    // TODO Edit endpoints

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#list-reviewers"
     */
    @GET("changes/{change-id}/reviewers")
    Observable<List<ReviewerInfo>> getChangeReviewers(@NonNull @Path("change-id") String changeId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#suggest-reviewers"
     */
    @GET("changes/{change-id}/suggest_reviewers")
    Observable<List<SuggestedReviewerInfo>> getChangeSuggestedReviewers(
            @NonNull @Path("change-id") String changeId,
            @NonNull @Query("q") String query,
            @Nullable @Query("n") Integer count);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#get-reviewer"
     */
    @GET("changes/{change-id}/reviewers/{account-id}")
    Observable<List<ReviewerInfo>> getChangeReviewer(
            @NonNull @Path("change-id") String changeId,
            @NonNull @Path("account-id") String accountId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#add-reviewer"
     */
    @Headers({"Content-Type: application/json; charset=UTF-8"})
    @POST("changes/{change-id}/reviewers")
    Observable<AddReviewerResultInfo> addChangeReviewer(
            @NonNull @Path("change-id") String changeId,
            @NonNull @Body ReviewerInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#delete-reviewer"
     */
    @DELETE("changes/{change-id}/reviewers/{account-id}")
    Observable<Void> deleteChangeReviewer(
            @NonNull @Path("change-id") String changeId,
            @NonNull @Path("account-id") String accountId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#list-votes"
     */
    @GET("changes/{change-id}/reviewers/{account-id}/votes/")
    Observable<Map<String, Integer>> getChangeReviewerVotes(
            @NonNull @Path("change-id") String changeId,
            @NonNull @Path("account-id") String accountId);

    // TODO Where are the Add and Update vote actions?

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#delete-vote"
     */
    @Headers({"Content-Type: application/json; charset=UTF-8"})
    @POST("changes/{change-id}/reviewers/{account-id}/votes/{label-id}/delete")
    Observable<Void> deleteChangeReviewerVote(
            @NonNull @Path("change-id") String changeId,
            @NonNull @Path("account-id") String accountId,
            @NonNull @Path("label-id") String labelId,
            @NonNull @Body DeleteVoteInput input);



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
