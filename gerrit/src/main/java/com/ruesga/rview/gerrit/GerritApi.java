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

import android.net.Uri;

import com.ruesga.rview.gerrit.filter.AccountQuery;
import com.ruesga.rview.gerrit.filter.ChangeQuery;
import com.ruesga.rview.gerrit.filter.GroupQuery;
import com.ruesga.rview.gerrit.filter.Option;
import com.ruesga.rview.gerrit.model.*;

import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.reactivex.Observable;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;

/**
 * Gerrit REST api
 */
public interface GerritApi {

    /**
     * The current supported Gerrit api version.
     */
    double API_VERSION = 2.15;

    /**
     * The current minimum supported Gerrit api version.
     */
    double MIN_API_VERSION = 2.11;

    /**
     * The uri of the page where to obtain the http password.
     */
    String HTTP_PASSWORD_URI = "#/settings/http-password";

    /**
     * The uri of the page where to login into the repository.
     */
    String LOGIN_URI = "login";



    // ===============================
    // Non-Api operations
    // ===============================

    /**
     * Return the uri of a change
     */
    Uri getChangeUri(@NonNull String changeId);

    /**
     * Return the uri of a change's revision
     */
    Uri getRevisionUri(@NonNull String changeId, @NonNull String revisionNumber);

    /**
     * Return the uri of an user avatar
     */
    Uri getAvatarUri(String accountId, int size);

    /**
     * Return the uri of a documentation url
     */
    Uri getDocumentationUri(@NonNull String docPath);

    /**
     * Return the uri for download a complete change's revision
     */
    Uri getDownloadRevisionUri(
            @NonNull String changeId,
            @NonNull String revisionId,
            @NonNull DownloadFormat format);

    /**
     * Return the uri for download a patch file of a change's revision
     */
    Uri getPatchFileRevisionUri(
            @NonNull String changeId,
            @NonNull String revisionId,
            @NonNull PatchFileFormat format);

    /**
     * Return an implementation of ApiVersionMediator
     */
    boolean supportsFeature(Features feature);

    /**
     * Return an implementation of ApiVersionMediator
     */
    boolean supportsFeature(Features feature, ServerVersion version);




    // ===============================
    // Gerrit access endpoints
    // @link "https://gerrit-review.googlesource.com/Documentation/rest-api-access.html"
    // ===============================

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-access.html#list-access"
     */
    Observable<Map<String, ProjectAccessInfo>> getAccessRights(
            @NonNull String[] names);



    // ===============================
    // Gerrit accounts endpoints
    // @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html"
    // ===============================

    /**
     * The own account id (requires and authenticated account)
     */
    String SELF_ACCOUNT = "self";

    /**
     * The default project's dashboard
     */
    String DEFAULT_DASHBOARD = "default";

    /**
     * The current revision
     */
    String CURRENT_REVISION = "current";

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#query-account"
     */
    Observable<List<AccountInfo>> getAccountsSuggestions(
            @NonNull String query,
            @Nullable Integer count,
            @Nullable Option suggest);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#query-account"
     */
    Observable<List<AccountInfo>> getAccounts(
            @NonNull AccountQuery query,
            @Nullable Integer count,
            @Nullable Integer start,
            @Nullable List<AccountOptions> options);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#get-account"
     */
    Observable<AccountInfo> getAccount(@NonNull String accountId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#create-account"
     */
    Observable<AccountInfo> createAccount(
            @NonNull String username,
            @NonNull AccountInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#get-detail"
     */
    Observable<AccountDetailInfo> getAccountDetails(@NonNull String accountId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#get-account-name"
     */
    Observable<String> getAccountName(@NonNull String accountId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#set-account-name"
     */
    Observable<String> setAccountName(
            @NonNull String accountId,
            @NonNull AccountNameInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#delete-account-name"
     */
    Observable<Void> deleteAccountName(@NonNull String accountId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#get-account-status"
     */
    Observable<String> getAccountStatus(@NonNull String accountId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#set-account-status"
     */
    Observable<String> setAccountStatus(
            @NonNull String accountId,
            @NonNull AccountStatusInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#get-username"
     */
    Observable<String> getAccountUsername(@NonNull String accountId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#set-username"
     */
    Observable<String> setAccountUsername(
            @NonNull String accountId,
            @NonNull UsernameInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#get-active"
     */
    Observable<String> isAccountActive(@NonNull String accountId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#set-active"
     */
    Observable<Void> setAccountAsActive(@NonNull String accountId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#delete-active"
     */
    Observable<Void> setAccountAsInactive(@NonNull String accountId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#get-http-password"
     */
    Observable<String> getHttpPassword(@NonNull String accountId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#set-http-password"
     */
    Observable<String> setHttpPassword(
            @NonNull String accountId,
            @NonNull HttpPasswordInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#delete-http-password"
     */
    Observable<Void> deleteHttpPassword(@NonNull String accountId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#get-oauth-token"
     */
    Observable<OAuthTokenInfo> getOAuthToken(@NonNull String accountId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#list-account-emails"
     */
    Observable<List<EmailInfo>> getAccountEmails(@NonNull String accountId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#list-account-email"
     */
    Observable<EmailInfo> getAccountEmail(
            @NonNull String accountId,
            @NonNull String emailId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#create-account-email"
     */
    Observable<EmailInfo> createAccountEmail(
            @NonNull String accountId,
            @NonNull String emailId,
            @NonNull EmailInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#delete-account-email"
     */
    Observable<Void> deleteAccountEmail(
            @NonNull String accountId,
            @NonNull String emailId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#set-preferred-email"
     */
    Observable<Void> setAccountPreferredEmail(
            @NonNull String accountId,
            @NonNull String emailId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#list-ssh-keys"
     */
    Observable<List<SshKeyInfo>> getAccountSshKeys(@NonNull String accountId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#get-ssh-key"
     */
    Observable<SshKeyInfo> getAccountSshKey(
            @NonNull String accountId,
            int sshKeyId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#add-ssh-key"
     */
    Observable<SshKeyInfo> addAccountSshKey(
            @NonNull String accountId,
            @NonNull String encodedKey);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#delete-ssh-key"
     */
    Observable<Void> deleteAccountSshKey(
            @NonNull String accountId,
            int sshKeyId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#list-gpg-keys"
     */
    Observable<List<GpgKeyInfo>> getAccountGpgKeys(@NonNull String accountId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#get-gpg-key"
     */
    Observable<GpgKeyInfo> getAccountGpgKey(
            @NonNull String accountId,
            @NonNull String gpgKeyId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#add-delete-gpg-keys"
     */
    Observable<Map<String, GpgKeyInfo>> addAccountGpgKeys(
            @NonNull String accountId,
            @NonNull AddGpgKeyInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#add-delete-gpg-keys"
     */
    Observable<Map<String, GpgKeyInfo>> deleteAccountGpgKeys(
            @NonNull String accountId,
            @NonNull DeleteGpgKeyInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#list-account-capabilities"
     */
    Observable<AccountCapabilityInfo> getAccountCapabilities(
            @NonNull String accountId,
            @Nullable List<Capability> filter);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#check-account-capabilities"
     */
    Observable<String> hasAccountCapability(
            @NonNull String accountId,
            @NonNull Capability capabilityId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#list-groups"
     */
    Observable<List<GroupInfo>> getAccountGroups(@NonNull String accountId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#get-avatar"
     */
    Observable<ResponseBody> getAccountAvatar(
            @NonNull String accountId,
            @Nullable Integer size);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#get-avatar-change-url"
     */
    Observable<String> getAccountAvatarChangeUrl(@NonNull String accountId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#get-user-preferences"
     */
    Observable<PreferencesInfo> getAccountPreferences(
            @NonNull String accountId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#set-user-preferences"
     */
    Observable<PreferencesInfo> setAccountPreferences(
            @NonNull String accountId,
            @NonNull PreferencesInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#get-diff-preferences"
     */
    Observable<DiffPreferencesInfo> getAccountDiffPreferences(
            @NonNull String accountId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#set-diff-preferences"
     */
    Observable<DiffPreferencesInfo> setAccountDiffPreferences(
            @NonNull String accountId,
            @NonNull DiffPreferencesInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#get-edit-preferences"
     */
    Observable<EditPreferencesInfo> getAccountEditPreferences(
            @NonNull String accountId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#set-edit-preferences"
     */
    Observable<EditPreferencesInfo> setAccountEditPreferences(
            @NonNull String accountId,
            @NonNull EditPreferencesInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#get-watched-projects"
     */
    Observable<List<ProjectWatchInfo>> getAccountWatchedProjects(
            @NonNull String accountId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#set-watched-projects"
     */
    Observable<List<ProjectWatchInfo>> addOrUpdateAccountWatchedProjects(
            @NonNull String accountId,
            @NonNull List<ProjectWatchInput> input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#delete-watched-projects"
     */
    Observable<Void> deleteAccountWatchedProjects(
            @NonNull String accountId,
            @NonNull List<DeleteProjectWatchInput> input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#get-account-external-ids"
     */
    Observable<List<AccountExternalIdInfo>> getAccountExternalIds(
            @NonNull String accountId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#get-account-external-ids"
     */
    Observable<Void> deleteAccountExternalIds(
            @NonNull String accountId,
            @NonNull List<String> externalIds);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#get-changes-with-default-star"
     */
    Observable<List<ChangeInfo>> getDefaultStarredChanges(
            @NonNull String accountId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#star-change"
     */
    Observable<Void> putDefaultStarOnChange(
            @NonNull String accountId,
            @NonNull String changeId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#unstar-change"
     */
    Observable<Void> deleteDefaultStarFromChange(
            @NonNull String accountId,
            @NonNull String changeId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#get-starred-changes"
     */
    Observable<List<ChangeInfo>> getStarredChanges(
            @NonNull String accountId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#get-stars"
     */
    Observable<List<String>> getStarLabelsFromChange(
            @NonNull String accountId,
            @NonNull String changeId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#set-stars"
     */
    Observable<List<String>> updateStarLabelsFromChange(
            @NonNull String accountId,
            @NonNull String changeId,
            @NonNull StarInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#list-contributor-agreements"
     */
    Observable<List<ContributorAgreementInfo>> getContributorAgreements(
            @NonNull String accountId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#list-contributor-agreements"
     */
    Observable<String> signContributorAgreement(
            @NonNull String accountId,
            @NonNull ContributorAgreementInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html#index-account"
     */
    Observable<Void> indexAccount(
            @NonNull String accountId);



    // ===============================
    // Gerrit changes endpoints
    // @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html"
    // ===============================

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#create-change"
     */
    Observable<ChangeInfo> createChange(@NonNull ChangeInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#list-changes"
     */
    Observable<List<ChangeInfo>> getChanges(
            @NonNull ChangeQuery query,
            @Nullable Integer count,
            @Nullable Integer start,
            @Nullable List<ChangeOptions> options);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#get-change"
     */
    Observable<ChangeInfo> getChange(
            @NonNull String changeId,
            @Nullable List<ChangeOptions> options);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#create-merge-patch-set-for-change"
     */
    Observable<ChangeInfo> createMergePathSetForChange(
            @NonNull String changeId,
            @NonNull MergePatchSetInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#set-message"
     */
    Observable<ChangeInfo> setChangeCommitMessage(
            @NonNull String changeId,
            @NonNull CommitMessageInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#get-change-detail"
     */
    Observable<ChangeInfo> getChangeDetail(
            @NonNull String changeId,
            @Nullable List<ChangeOptions> options);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#get-topic"
     */
    Observable<String> getChangeTopic(@NonNull String changeId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#set-topic"
     */
    Observable<String> setChangeTopic(
            @NonNull String changeId,
            @NonNull TopicInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#delete-topic"
     */
    Observable<Void> deleteChangeTopic(@NonNull String changeId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#get-assignee"
     */
    Observable<AccountInfo> getChangeAssignee(@NonNull String changeId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#get-past-assignees"
     */
    Observable<List<AccountInfo>> getChangePastAssignees(@NonNull String changeId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#set-assignee"
     */
    Observable<AccountInfo> setChangeAssignee(
            @NonNull String changeId,
            @NonNull AssigneeInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#delete-assignee"
     */
    Observable<AccountInfo> deleteChangeAssignee(@NonNull String changeId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#get-pure-revert"
     */
    Observable<PureRevertInfo> getChangePureRevert(
            @NonNull String changeId,
            @Nullable String commit,
            @Nullable String revertOf);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#abandon-change"
     */
    Observable<ChangeInfo> abandonChange(
            @NonNull String changeId,
            @NonNull AbandonInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#restore-change"
     */
    Observable<ChangeInfo> restoreChange(
            @NonNull String changeId,
            @NonNull RestoreInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#rebase-change"
     */
    Observable<ChangeInfo> rebaseChange(
            @NonNull String changeId,
            @NonNull RebaseInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#move-change"
     */
    Observable<ChangeInfo> moveChange(
            @NonNull String changeId,
            @NonNull MoveInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#revert-change"
     */
    Observable<ChangeInfo> revertChange(
            @NonNull String changeId,
            @NonNull RevertInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#submit-change"
     */
    Observable<ChangeInfo> submitChange(
            @NonNull String changeId,
            @NonNull SubmitInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#submitted-together"
     */
    Observable<List<ChangeInfo>> getChangesSubmittedTogether(
            @NonNull String changeId,
            @Nullable List<SubmittedTogetherOptions> options);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#publish-draft-change"
     * @deprecated since 2.15
     */
    @Deprecated
    Observable<Void> publishDraftChange(@NonNull String changeId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#delete-change"
     */
    Observable<Void> deleteChange(@NonNull String changeId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#get-included-in"
     */
    Observable<IncludedInInfo> getChangeIncludedIn(@NonNull String changeId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#index-change"
     */
    Observable<Void> indexChange(@NonNull String changeId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#list-change-comments"
     */
    Observable<Map<String, List<CommentInfo>>> getChangeComments(
            @NonNull String changeId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#list-change-robot-comments"
     */
    Observable<Map<String, List<RobotCommentInfo>>> getChangeRobotComments(
            @NonNull String changeId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#list-change-drafts"
     */
    Observable<Map<String, List<CommentInfo>>> getChangeDraftComments(
            @NonNull String changeId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#check-change"
     */
    Observable<ChangeInfo> checkChange(@NonNull String changeId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#fix-change"
     */
    Observable<ChangeInfo> fixChange(
            @NonNull String changeId,
            @NonNull FixInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#fix-change"
     */
    Observable<Void> setChangeWorkInProgress(
            @NonNull String changeId,
            @NonNull WorkInProgressInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#set-ready-for-review"
     */
    Observable<Void> setChangeReadyForReview(
            @NonNull String changeId,
            @NonNull WorkInProgressInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#mark-private"
     */
    Observable<Void> markChangeAsPrivate(
            @NonNull String changeId,
            @NonNull PrivateInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#unmark-private"
     */
    Observable<Void> unmarkChangeAsPrivate(
            @NonNull String changeId,
            @NonNull PrivateInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#ignore"
     */
    Observable<Void> ignoreChange(@NonNull String changeId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#unignore"
     */
    Observable<Void> unignoreChange(@NonNull String changeId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#reviewed"
     */
    Observable<Void> markChangeAsReviewed(@NonNull String changeId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#unreviewed"
     */
    Observable<Void> markChangeAsUnreviewed(@NonNull String changeId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#get-hashtags"
     */
    Observable<String[]> getChangeHashtags(@NonNull String changeId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#set-hashtags"
     */
    Observable<String[]> setChangeHashtags(
            @NonNull String changeId,
            @NonNull HashtagsInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#get-edit-detail"
     */
    Observable<EditInfo> getChangeEdit(
            @NonNull String changeId,
            @Nullable Option list,
            @Nullable String base,
            @Nullable Option downloadCommands);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#put-edit-file"
     */
    Observable<Void> setChangeEditFile(
            @NonNull String changeId,
            @NonNull String fileId,
            @NonNull RequestBody data);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#post-edit"
     */
    Observable<Void> restoreChangeEditFile(
            @NonNull String changeId,
            @NonNull RestoreChangeEditInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#post-edit"
     */
    Observable<Void> renameChangeEditFile(
            @NonNull String changeId,
            @NonNull RenameChangeEditInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#post-edit"
     */
    Observable<Void> newChangeEditFile(
            @NonNull String changeId,
            @NonNull NewChangeEditInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#delete-edit-file"
     */
    Observable<Void> deleteChangeEditFile(
            @NonNull String changeId,
            @NonNull String fileId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#get-edit-file"
     */
    Observable<Base64Data> getChangeEditFileContent(
            @NonNull String changeId,
            @NonNull String fileId,
            @Nullable String base);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#get-edit-file"
     */
    Observable<EditFileInfo> getChangeEditFileMetadata(
            @NonNull String changeId,
            @NonNull String fileId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#get-edit-message"
     */
    Observable<String> getChangeEditMessage(@NonNull String changeId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#put-change-edit-message"
     */
    Observable<Void> setChangeEditMessage(
            @NonNull String changeId,
            @NonNull ChangeEditMessageInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#publish-edit"
     */
    Observable<Void> publishChangeEdit(@NonNull String changeId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#rebase-edit"
     */
    Observable<Void> rebaseChangeEdit(@NonNull String changeId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#delete-edit"
     */
    Observable<Void> deleteChangeEdit(@NonNull String changeId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#list-reviewers"
     */
    Observable<List<ReviewerInfo>> getChangeReviewers(@NonNull String changeId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#suggest-reviewers"
     */
    Observable<List<SuggestedReviewerInfo>> getChangeSuggestedReviewers(
            @NonNull String changeId,
            @NonNull String query,
            @Nullable Integer count,
            @Nullable ExcludeGroupsFromSuggestedReviewers excludeGroups);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#get-reviewer"
     */
    Observable<List<ReviewerInfo>> getChangeReviewer(
            @NonNull String changeId,
            @NonNull String accountId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#add-reviewer"
     */
    Observable<AddReviewerResultInfo> addChangeReviewer(
            @NonNull String changeId,
            @NonNull ReviewerInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#delete-reviewer"
     */
    Observable<Void> deleteChangeReviewer(
            @NonNull String changeId,
            @NonNull String accountId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#list-votes"
     */
    Observable<Map<String, Integer>> getChangeReviewerVotes(
            @NonNull String changeId,
            @NonNull String accountId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#delete-vote"
     */
    Observable<Void> deleteChangeReviewerVote(
            @NonNull String changeId,
            @NonNull String accountId,
            @NonNull String labelId,
            @NonNull DeleteVoteInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#get-commit"
     */
    Observable<CommitInfo> getChangeRevisionCommit(
            @NonNull String changeId,
            @NonNull String revisionId,
            @Nullable Option links);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#get-description"
     */
    Observable<String> getChangeRevisionDescription(
            @NonNull String changeId,
            @NonNull String revisionId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#set-description"
     */
    Observable<String> setChangeRevisionDescription(
            @NonNull String changeId,
            @NonNull String revisionId,
            @NonNull DescriptionInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#get-merge-list"
     */
    Observable<List<CommentInfo>> getChangeRevisionMergeList(
            @NonNull String changeId,
            @NonNull String revisionId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#get-revision-actions"
     */
    Observable<Map<String, ActionInfo>> getChangeRevisionActions(
            @NonNull String changeId,
            @NonNull String revisionId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#get-review"
     */
    Observable<ChangeInfo> getChangeRevisionReview(
            @NonNull String changeId,
            @NonNull String revisionId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#set-review"
     */
    Observable<ReviewResultInfo> setChangeRevisionReview(
            @NonNull String changeId,
            @NonNull String revisionId,
            @NonNull ReviewInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#get-related-changes"
     */
    Observable<RelatedChangesInfo> getChangeRevisionRelatedChanges(
            @NonNull String changeId,
            @NonNull String revisionId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#rebase-revision"
     */
    Observable<ChangeInfo> rebaseChangeRevision(
            @NonNull String changeId,
            @NonNull String revisionId,
            @NonNull RebaseInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#submit-revision"
     */
    Observable<SubmitInfo> submitChangeRevision(
            @NonNull String changeId,
            @NonNull String revisionId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#publish-draft-revision"
     * @deprecated since 2.15
     */
    @Deprecated
    Observable<Void> publishChangeDraftRevision(
            @NonNull String changeId,
            @NonNull String revisionId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#delete-draft-revision"
     * @deprecated since 2.15
     */
    @Deprecated
    Observable<SubmitInfo> deleteChangeDraftRevision(
            @NonNull String changeId,
            @NonNull String revisionId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#get-patch"
     */
    Observable<Base64Data> getChangeRevisionPatch(
            @NonNull String changeId,
            @NonNull String revisionId,
            @Nullable Option zip,
            @Nullable Option download);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#submit-preview"
     */
    Observable<ResponseBody> getChangeRevisionSubmitPreview(
            @NonNull String changeId,
            @NonNull String revisionId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#get-mergeable"
     */
    Observable<MergeableInfo> getChangeRevisionMergeableStatus(
            @NonNull String changeId,
            @NonNull String revisionId,
            @Nullable Option otherBranches);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#get-submit-type"
     */
    Observable<SubmitType> getChangeRevisionSubmitType(
            @NonNull String changeId,
            @NonNull String revisionId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#test-submit-type"
     */
    Observable<SubmitType> testChangeRevisionSubmitType(
            @NonNull String changeId,
            @NonNull String revisionId,
            @NonNull RuleInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#test-submit-rule"
     */
    Observable<List<SubmitRecordInfo>> testChangeRevisionSubmitRule(
            @NonNull String changeId,
            @NonNull String revisionId,
            @NonNull RuleInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#list-drafts"
     */
    Observable<Map<String, List<CommentInfo>>> getChangeRevisionDrafts(
            @NonNull String changeId,
            @NonNull String revisionId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#create-draft"
     */
    Observable<CommentInfo> createChangeRevisionDraft(
            @NonNull String changeId,
            @NonNull String revisionId,
            @NonNull CommentInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#get-draft"
     */
    Observable<CommentInfo> getChangeRevisionDraft(
            @NonNull String changeId,
            @NonNull String revisionId,
            @NonNull String draftId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#update-draft"
     */
    Observable<CommentInfo> updateChangeRevisionDraft(
            @NonNull String changeId,
            @NonNull String revisionId,
            @NonNull String draftId,
            @NonNull CommentInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#delete-draft"
     */
    Observable<Void> deleteChangeRevisionDraft(
            @NonNull String changeId,
            @NonNull String revisionId,
            @NonNull String draftId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#list-comments"
     */
    Observable<Map<String, List<CommentInfo>>> getChangeRevisionComments(
            @NonNull String changeId,
            @NonNull String revisionId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#get-comment"
     */
    Observable<CommentInfo> getChangeRevisionComment(
            @NonNull String changeId,
            @NonNull String revisionId,
            @NonNull String commentId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#delete-comment"
     */
    Observable<CommentInfo> deleteChangeRevisionComment(
            @NonNull String changeId,
            @NonNull String revisionId,
            @NonNull String commentId,
            @NonNull DeleteCommentInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#list-robot-comments"
     */
    Observable<List<RobotCommentInfo>> getChangeRevisionRobotComments(
            @NonNull String changeId,
            @NonNull String revisionId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#get-robot-comment"
     */
    Observable<RobotCommentInfo> getChangeRevisionRobotComment(
            @NonNull String changeId,
            @NonNull String revisionId,
            @NonNull String commentId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#apply-fix"
     */
    Observable<EditInfo> applyChangeRevisionFix(
            @NonNull String changeId,
            @NonNull String revisionId,
            @NonNull String fixId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#list-files"
     */
    Observable<Map<String, FileInfo>> getChangeRevisionFiles(
            @NonNull String changeId,
            @NonNull String revisionId,
            @Nullable String base,
            @Nullable Option reviewed);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#list-files"
     */
    Observable<List<String>> getChangeRevisionFilesSuggestion(
            @NonNull String changeId,
            @NonNull String revisionId,
            @Nullable String base,
            @Nullable Option reviewed,
            @Nullable String filter);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#get-content"
     */
    Observable<ResponseBody> getChangeRevisionFileContent(
            @NonNull String changeId,
            @NonNull String revisionId,
            @NonNull String fileId,
            @Nullable Integer parent);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#get-safe-content"
     */
    Observable<ResponseBody> getChangeRevisionFileDownload(
            @NonNull String changeId,
            @NonNull String revisionId,
            @NonNull String fileId,
            @Nullable SuffixMode suffixMode,
            @Nullable Integer parent);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#get-diff"
     */
    Observable<DiffInfo> getChangeRevisionFileDiff(
            @NonNull String changeId,
            @NonNull String revisionId,
            @NonNull String fileId,
            @Nullable Integer base,
            @Nullable Option intraline,
            @Nullable Option weblinksOnly,
            @Nullable WhitespaceType whitespace,
            @Nullable ContextType context);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#get-blame"
     */
    Observable<List<BlameInfo>> getChangeRevisionFileBlames(
            @NonNull String changeId,
            @NonNull String revisionId,
            @NonNull String fileId,
            @Nullable BlameBaseType base);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#set-reviewed"
     */
    Observable<Void> setChangeRevisionFileAsReviewed(
            @NonNull String changeId,
            @NonNull String revisionId,
            @NonNull String fileId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#delete-reviewed"
     */
    Observable<Void> setChangeRevisionFileAsNotReviewed(
            @NonNull String changeId,
            @NonNull String revisionId,
            @NonNull String fileId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#cherry-pick"
     */
    Observable<ChangeInfo> cherryPickChangeRevision(
            @NonNull String changeId,
            @NonNull String revisionId,
            @NonNull CherryPickInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#list-revision-reviewers"
     */
    Observable<List<ReviewerInfo>> getChangeRevisionReviewers(
            @NonNull String changeId,
            @NonNull String revisionId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#list-revision-votes"
     */
    Observable<List<ReviewerInfo>> getChangeRevisionReviewersVotes(
            @NonNull String changeId,
            @NonNull String revisionId,
            @NonNull String accountId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html#delete-revision-vote"
     */
    Observable<Void> deleteChangeRevisionReviewerVote(
            @NonNull String changeId,
            @NonNull String revisionId,
            @NonNull String accountId,
            @NonNull String labelId,
            @NonNull DeleteVoteInput input);



    // ===============================
    // Gerrit configuration endpoints
    // @link "https://gerrit-review.googlesource.com/Documentation/rest-api-config.html"
    // ===============================

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-config.html#get-version"
     */
    Observable<ServerVersion> getServerVersion();

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-config.html#get-info"
     */
    Observable<ServerInfo> getServerInfo();

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-config.html#check-consistency"
     */
    Observable<ConsistencyCheckInfo> checkConsistency(@NonNull ConsistencyCheckInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-config.html#email-confirmation-input"
     */
    Observable<Void> confirmEmail(@NonNull EmailConfirmationInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-config.html#list-caches"
     */
    Observable<Map<String, CacheInfo>> getServerCaches();

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-config.html#cache-operations"
     */
    Observable<Void> executeServerCachesOperations(CacheOperationInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-config.html#get-cache"
     */
    Observable<CacheInfo> getServerCache(@NonNull String cacheId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-config.html#flush-cache"
     */
    Observable<Void> flushServerCache(@NonNull String cacheId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-config.html#get-summary"
     */
    Observable<SummaryInfo> getServerSummary(
            @Nullable Option jvm,
            @Nullable Option gc);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-config.html#list-capabilities"
     */
    Observable<Map<Capability, ServerCapabilityInfo>> getServerCapabilities();

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-config.html#list-tasks"
     */
    Observable<List<TaskInfo>> getServerTasks();

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-config.html#get-task"
     */
    Observable<TaskInfo> getServerTask(@NonNull String taskId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-config.html#delete-task"
     */
    Observable<Void> deleteServerTask(@NonNull String taskId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-config.html#get-top-menus"
     */
    Observable<List<TopMenuEntryInfo>> getServerTopMenus();

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-config.html#get-user-preferences"
     */
    Observable<PreferencesInfo> getServerDefaultPreferences();

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-config.html#set-user-preferences"
     */
    Observable<PreferencesInfo> setServerDefaultPreferences(@NonNull PreferencesInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-config.html#get-diff-preferences"
     */
    Observable<DiffPreferencesInfo> getServerDefaultDiffPreferences();

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-config.html#set-diff-preferences"
     */
    Observable<DiffPreferencesInfo> setServerDefaultDiffPreferences(
            @NonNull DiffPreferencesInput input);



    // ===============================
    // Gerrit groups endpoints
    // @link "https://gerrit-review.googlesource.com/Documentation/rest-api-groups.html"
    // ===============================

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-groups.html#list-groups"
     */
    Observable<List<GroupInfo>> getGroupSuggestions(
            @NonNull String query,
            @Nullable Integer count);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-groups.html#list-groups"
     */
    Observable<List<GroupInfo>> getGroups(
            @Nullable Integer count,
            @Nullable Integer start,
            @Nullable String project,
            @Nullable String user,
            @Nullable Option owned,
            @Nullable Option visibleToAll,
            @Nullable Option verbose,
            @Nullable List<GroupOptions> options,
            @Nullable String suggest,
            @Nullable String regexp,
            @Nullable String match);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-groups.html#query-groups"
     */
    Observable<List<GroupInfo>> getGroups(
            @NonNull GroupQuery query,
            @Nullable Integer count,
            @Nullable Integer start,
            @Nullable List<GroupOptions> options);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-groups.html#get-group"
     */
    Observable<GroupInfo> getGroup(@NonNull String groupId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-groups.html#create-group"
     */
    Observable<GroupInfo> createGroup(
            @NonNull String groupName,
            @NonNull GroupInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-groups.html#get-group-detail"
     */
    Observable<GroupInfo> getGroupDetail(@NonNull String groupId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-groups.html#get-group-name"
     */
    Observable<String> getGroupName(@NonNull String groupId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-groups.html#rename-group"
     */
    Observable<String> setGroupName(
            @NonNull String groupId,
            @NonNull GroupNameInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-groups.html#get-group-description"
     */
    Observable<String> getGroupDescription(@NonNull String groupId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-groups.html#set-group-description"
     */
    Observable<String> setGroupDescription(
            @NonNull String groupId,
            @NonNull GroupDescriptionInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-groups.html#delete-group-description"
     */
    Observable<Void> deleteGroupDescription(@NonNull String groupId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-groups.html#get-group-options"
     */
    Observable<GroupOptionsInfo> getGroupOptions(@NonNull String groupId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-groups.html#set-group-options"
     */
    Observable<GroupOptionsInfo> setGroupOptions(
            @NonNull String groupId,
            @NonNull GroupOptionsInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-groups.html#get-group-owner"
     */
    Observable<GroupInfo> getGroupOwner(@NonNull String groupId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-groups.html#set-group-owner"
     */
    Observable<GroupInfo> setGroupOwner(
            @NonNull String groupId,
            @NonNull GroupOwnerInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-groups.html#get-audit-log"
     */
    Observable<List<GroupAuditEventInfo>> getGroupAuditLog(@NonNull String groupId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-groups.html#index-group"
     */
    Observable<Void> indexGroup(@NonNull String groupId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-groups.html#group-members"
     */
    Observable<List<AccountInfo>> getGroupMembers(
            @NonNull String groupId,
            @Nullable Option recursive);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-groups.html#get-group-member"
     */
    Observable<AccountInfo> getGroupMember(
            @NonNull String groupId,
            @NonNull String accountId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-groups.html#add-group-member"
     */
    Observable<AccountInfo> addGroupMember(
            @NonNull String groupId,
            @NonNull String accountId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-groups.html#_add_group_members"
     */
    Observable<List<AccountInfo>> addGroupMembers(
            @NonNull String groupId,
            @NonNull MemberInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-groups.html#remove-group-member"
     */
    Observable<Void> deleteGroupMember(
            @NonNull String groupId,
            @NonNull String accountId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-groups.html#remove-group-members"
     */
    Observable<Void> deleteGroupMembers(
            @NonNull String groupId,
            @NonNull MemberInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-groups.html#list-subgroups"
     */
    Observable<List<GroupInfo>> getGroupSubgroups(@NonNull String groupId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-groups.html#get-subgroup"
     */
    Observable<GroupInfo> getGroupSubgroup(
            @NonNull String groupId,
            @NonNull String subgroupId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-groups.html#add-subgroup"
     */
    Observable<GroupInfo> addGroupSubgroup(
            @NonNull String groupId,
            @NonNull String subgroupId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-groups.html#add-subgroups"
     */
    Observable<GroupInfo> addGroupSubgroups(
            @NonNull String groupId,
            @NonNull SubgroupInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-groups.html#remove-subgroup"
     */
    Observable<Void> deleteGroupSubgroup(
            @NonNull String groupId,
            @NonNull String subgroupId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-groups.html#remove-subgroups"
     */
    Observable<Void> deleteGroupSubgroups(
            @NonNull String groupId,
            @NonNull SubgroupInput input);



    // ===============================
    // Gerrit plugins endpoints
    // @link "https://gerrit-review.googlesource.com/Documentation/rest-api-plugins.html"
    // ===============================

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-plugins.html#list-plugins"
     */
    Observable<Map<String, PluginInfo>> getPlugins(
            @Nullable Option all,
            @Nullable Integer count,
            @Nullable Integer skip,
            @Nullable String prefix,
            @Nullable String regexp,
            @Nullable String match);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-plugins.html#install-plugin"
     */
    Observable<PluginInfo> installPlugin(
            @NonNull String pluginId,
            @NonNull PluginInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-plugins.html#get-plugin-status"
     */
    Observable<PluginInfo> getPluginStatus(@NonNull String pluginId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-plugins.html#enable-plugin"
     */
    Observable<PluginInfo> enablePlugin(@NonNull String pluginId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-plugins.html#disable-plugin"
     */
    Observable<PluginInfo> disablePlugin(@NonNull String pluginId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-plugins.html#reload-plugin"
     */
    Observable<PluginInfo> reloadPlugin(@NonNull String pluginId);



    // ===============================
    // Gerrit projects endpoints
    // @link "https://gerrit-review.googlesource.com/Documentation/rest-api-projects.html"
    // ===============================

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-projects.html#list-projects"
     */
    Observable<Map<String, ProjectInfo>> getProjects(
            @Nullable Integer count,
            @Nullable Integer start,
            @Nullable String prefix,
            @Nullable String regexp,
            @Nullable String match,
            @Nullable Option showDescription,
            @Nullable Option showTree,
            @Nullable String branch,
            @Nullable ProjectType type,
            @Nullable String group);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-projects.html#get-project"
     */
    Observable<ProjectInfo> getProject(@NonNull String projectName);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-projects.html#create-project"
     */
    Observable<ProjectInfo> createProject(
            @NonNull String name,
            @NonNull ProjectInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-projects.html#get-project-description"
     */
    Observable<String> getProjectDescription(@NonNull String projectName);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-projects.html#set-project-description"
     */
    Observable<String> setProjectDescription(
            @NonNull String name,
            @NonNull ProjectDescriptionInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-projects.html#delete-project-description"
     */
    Observable<Void> deleteProjectDescription(@NonNull String projectName);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-projects.html#get-project-parent"
     */
    Observable<String> getProjectParent(@NonNull String projectName);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-projects.html#set-project-parent"
     */
    Observable<String> setProjectParent(
            @NonNull String projectName,
            @NonNull ProjectParentInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-projects.html#get-head"
     */
    Observable<String> getProjectHead(@NonNull String projectName);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-projects.html#set-head"
     */
    Observable<String> setProjectHead(
            @NonNull String projectName,
            @NonNull HeadInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-projects.html#get-repository-statistics"
     */
    Observable<RepositoryStatisticsInfo> getProjectStatistics(
            @NonNull String projectName);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-projects.html#get-config"
     */
    Observable<ConfigInfo> getProjectConfig(@NonNull String projectName);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-projects.html#set-config"
     */
    Observable<ConfigInfo> setProjectConfig(
            @NonNull String projectName,
            @NonNull ConfigInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-projects.html#run-gc"
     */
    Observable<ResponseBody> runProjectGc(
            @NonNull String projectName,
            @NonNull GcInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-projects.html#ban-commit"
     */
    Observable<BanResultInfo> banProject(
            @NonNull String projectName,
            @NonNull BanInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-projects.html#get-access"
     */
    Observable<ProjectAccessInfo> getProjectAccessRights(
            @NonNull String projectName);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-projects.html#set-access"
     */
    Observable<ProjectAccessInfo> setProjectAccessRights(
            @NonNull String projectName,
            @NonNull ProjectAccessInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-projects.html#create-access-change"
     */
    Observable<ChangeInfo> createProjectAccessRightsChange(
            @NonNull String projectName,
            @NonNull ProjectAccessInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-projects.html#check-access"
     */
    Observable<AccessCheckInfo> checkProjectAccessRights(
            @NonNull String projectName,
            @NonNull AccessCheckInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-projects.html#list-branches"
     */
    Observable<List<BranchInfo>> getProjectBranches(
            @NonNull String projectName,
            @Nullable Integer count,
            @Nullable Integer start,
            @Nullable String match,
            @Nullable String regexp);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-projects.html#get-branch"
     */
    Observable<BranchInfo> getProjectBranch(
            @NonNull String projectName,
            @NonNull String branchId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-projects.html#create-branch"
     */
    Observable<BranchInfo> createProjectBranch(
            @NonNull String projectName,
            @NonNull String branchId,
            @NonNull BranchInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-projects.html#delete-branch"
     */
    Observable<Void> deleteProjectBranch(
            @NonNull String projectName,
            @NonNull String branchId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-projects.html#delete-branches"
     */
    Observable<Void> deleteProjectBranches(
            @NonNull String projectName,
            @NonNull DeleteBranchesInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-projects.html#get-content"
     */
    Observable<Base64Data> getProjectBranchFileContent(
            @NonNull String projectName,
            @NonNull String branchId,
            @NonNull String fileId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-projects.html#get-mergeable-info"
     */
    Observable<MergeableInfo> getProjectBranchMergeableStatus(
            @NonNull String projectName,
            @NonNull String branchId,
            @NonNull String sourceBranchId,
            @Nullable MergeStrategy strategy);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-projects.html#get-reflog"
     */
    Observable<List<ReflogEntryInfo>> getProjectBranchReflog(
            @NonNull String projectName,
            @NonNull String branchId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-projects.html#list-child-projects"
     */
    Observable<List<ProjectInfo>> getProjectChildProjects(
            @NonNull String projectName);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-projects.html#get-child-project"
     */
    Observable<ProjectInfo> getProjectChildProject(
            @NonNull String projectName,
            @NonNull String childProjectName);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-projects.html#list-tags"
     */
    Observable<List<TagInfo>> getProjectTags(
            @NonNull String projectName,
            @Nullable Integer count,
            @Nullable Integer start,
            @Nullable String match,
            @Nullable String regexp);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-projects.html#get-tag"
     */
    Observable<TagInfo> getProjectTag(
            @NonNull String projectName,
            @NonNull String tagId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-projects.html#create-tag"
     */
    Observable<TagInfo> createProjectTag(
            @NonNull String projectName,
            @NonNull String tagId,
            @NonNull TagInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-projects.html#delete-tag"
     */
    Observable<Void> deleteProjectTag(
            @NonNull String projectName,
            @NonNull String tagId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-projects.html#delete-tags"
     */
    Observable<Void> deleteProjectTags(
            @NonNull String projectName,
            @NonNull DeleteTagsInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-projects.html#get-commit"
     */
    Observable<CommitInfo> getProjectCommit(
            @NonNull String projectName,
            @NonNull String commitId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-projects.html#get-content-from-commit"
     */
    Observable<Base64Data> getProjectCommitFileContent(
            @NonNull String projectName,
            @NonNull String commitId,
            @NonNull String fileId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-projects.html#cherry-pick-commit"
     */
    Observable<ChangeInfo> cherryPickProjectCommit(
            @NonNull String projectName,
            @NonNull String commitId,
            @NonNull CherryPickInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-projects.html#list-dashboards"
     */
    Observable<List<DashboardInfo>> getProjectDashboards(
            @NonNull String projectName);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-projects.html#get-dashboard"
     */
    Observable<DashboardInfo> getProjectDashboard(
            @NonNull String projectName,
            @NonNull String dashboardId);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-projects.html#set-dashboard"
     */
    Observable<DashboardInfo> setProjectDashboard(
            @NonNull String projectName,
            @NonNull String dashboardId,
            @NonNull DashboardInput input);

    /**
     * @link "https://gerrit-review.googlesource.com/Documentation/rest-api-projects.html#delete-dashboard"
     */
    Observable<Void> deleteProjectDashboard(
            @NonNull String projectName,
            @NonNull String dashboardId);


    // ===============================
    // Gerrit documentation endpoints
    // @link "https://gerrit-review.googlesource.com/Documentation/rest-api-documentation.html"
    // ===============================

    /**
     * @link "https://review.lineageos.org/Documentation/rest-api-documentation.html#search-documentation"
     */
    Observable<List<DocResult>> findDocumentation(@NonNull String keyword);



    // ===============================
    // Other endpoints
    // ===============================

    // -- Cloud Notifications Plugin --
    // https://gerrit.googlesource.com/plugins/cloud-notifications/

    /**
     * @link "https://gerrit.googlesource.com/plugins/cloud-notifications/+/master/src/main/resources/Documentation/api.md#get-cloud-notifications-config"
     */
    Observable<CloudNotificationsConfigInfo> getCloudNotificationsConfig();

    /**
     * @link "https://gerrit.googlesource.com/plugins/cloud-notifications/+/master/src/main/resources/Documentation/api.md#list-cloud-notifications"
     */
    Observable<List<CloudNotificationInfo>> listCloudNotifications(
            @NonNull String accountId,
            @NonNull String deviceId);

    /**
     * @link "https://gerrit.googlesource.com/plugins/cloud-notifications/+/master/src/main/resources/Documentation/api.md#get-cloud-notification"
     */
    Observable<CloudNotificationInfo> getCloudNotification(
            @NonNull String accountId,
            @NonNull String deviceId,
            @NonNull String token);

    /**
     * @link "https://gerrit.googlesource.com/plugins/cloud-notifications/+/master/src/main/resources/Documentation/api.md#register-cloud-notification"
     */
    Observable<CloudNotificationInfo> registerCloudNotification(
            @NonNull String accountId,
            @NonNull String deviceId,
            @NonNull CloudNotificationInput input);

    /**
     * @link "https://gerrit.googlesource.com/plugins/cloud-notifications/+/master/src/main/resources/Documentation/api.md#unregister-cloud-notification"
     */
    Observable<Void> unregisterCloudNotification(
            @NonNull String accountId,
            @NonNull String deviceId,
            @NonNull String token);
}
