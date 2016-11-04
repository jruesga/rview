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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.format.DateUtils;

import com.burgstaller.okhttp.AuthenticationCacheInterceptor;
import com.burgstaller.okhttp.CachingAuthenticatorDecorator;
import com.burgstaller.okhttp.DispatchingAuthenticator;
import com.burgstaller.okhttp.basic.BasicAuthenticator;
import com.burgstaller.okhttp.digest.CachingAuthenticator;
import com.burgstaller.okhttp.digest.Credentials;
import com.burgstaller.okhttp.digest.DigestAuthenticator;
import com.google.gson.annotations.Since;
import com.ruesga.rview.gerrit.filter.AccountQuery;
import com.ruesga.rview.gerrit.filter.ChangeQuery;
import com.ruesga.rview.gerrit.filter.GroupQuery;
import com.ruesga.rview.gerrit.filter.Option;
import com.ruesga.rview.gerrit.model.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import rx.Observable;

public class GerritApiClient implements GerritApi {
    private static final Map<String, CachingAuthenticator> sAuthCache = new ConcurrentHashMap<>();

    private final String mEndPoint;
    private final GerritApi mService;
    private final PlatformAbstractionLayer mAbstractionLayer;
    private long mLastServerVersionCheck = 0;
    ServerVersion mServerVersion;
    private List<Features> mSupportedFeatures = new ArrayList<>();

    private final ApiVersionMediator mMediator = new ApiVersionMediator() {
        @Override
        public WhitespaceType resolveWhiteSpaceType(WhitespaceType type) {
            if (type == null) {
                return null;
            }
            if (mServerVersion.getVersion() < 2.13) {
                return null;
            }
            return type;
        }

        @Override
        public IgnoreWhitespaceType resolveIgnoreWhiteSpaceType(IgnoreWhitespaceType type) {
            if (type == null) {
                return null;
            }
            if (mServerVersion.getVersion() >= 2.13) {
                return null;
            }
            return type;
        }

        @Override
        public DraftActionType resolveDraftActionType(DraftActionType type) {
            if (type == null) {
                return null;
            }
            if (mServerVersion.getVersion() <= 2.11
                    && type.equals(DraftActionType.PUBLISH_ALL_REVISIONS)) {
                return DraftActionType.PUBLISH;
            }
            return type;
        }
    };

    public GerritApiClient(String endpoint, Authorization authorization,
            PlatformAbstractionLayer abstractionLayer) {
        mAbstractionLayer = abstractionLayer;
        mEndPoint = endpoint;

        DispatchingAuthenticator authenticator = null;
        if (authorization != null && !authorization.isAnonymousUser()) {
            final Credentials credentials = new Credentials(
                    authorization.mUsername, authorization.mPassword);
            final BasicAuthenticator basicAuthenticator = new BasicAuthenticator(credentials);
            final DigestAuthenticator digestAuthenticator = new DigestAuthenticator(credentials);
            authenticator = new DispatchingAuthenticator.Builder()
                    .with("digest", digestAuthenticator)
                    .with("basic", basicAuthenticator)
                    .build();
        }

        // OkHttp client
        OkHttpClient.Builder clientBuilder = OkHttpHelper.getSafeClientBuilder();
        clientBuilder.followRedirects(true)
                .readTimeout(60000, java.util.concurrent.TimeUnit.MILLISECONDS)
                .followSslRedirects(true)
                .addInterceptor(createConnectivityCheckInterceptor())
                .addInterceptor(createLoggingInterceptor())
                .addInterceptor(createHeadersInterceptor());
        if (authorization != null && !authorization.isAnonymousUser()) {
            clientBuilder
                    .authenticator(new CachingAuthenticatorDecorator(authenticator, sAuthCache))
                    .addInterceptor(new AuthenticationCacheInterceptor(sAuthCache));
        }
        OkHttpClient client = clientBuilder.build();

        // Gson adapter
        GsonConverterFactory gsonFactory = GsonConverterFactory.create(
                GsonHelper.createGerritGsonBuilder(true, mAbstractionLayer).create());

        // RxJava adapter
        RxJavaCallAdapterFactory rxAdapter = RxJavaCallAdapterFactory.create();

        // Retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(endpoint)
                .client(client)
                .addConverterFactory(gsonFactory)
                .addCallAdapterFactory(rxAdapter)
                .build();

        // Build the api
        mService = retrofit.create(GerritApi.class);
    }

    private HttpLoggingInterceptor createLoggingInterceptor() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor(mAbstractionLayer::log);
        logging.setLevel(mAbstractionLayer.isDebugBuild()
                ? HttpLoggingInterceptor.Level.BODY : HttpLoggingInterceptor.Level.BASIC);
        return logging;
    }

    private Interceptor createHeadersInterceptor() {
        return chain -> {
            Request original = chain.request();
            Request.Builder requestBuilder = original.newBuilder();
            if (!mAbstractionLayer.isDebugBuild()) {
                requestBuilder.header("Accept", "application/json");
            }
            Request request = requestBuilder.build();
            return chain.proceed(request);
        };
    }

    private Interceptor createConnectivityCheckInterceptor() {
        return chain -> {
            if (!mAbstractionLayer.hasConnectivity()) {
                throw new NoConnectivityException();
            }
            return chain.proceed(chain.request());
        };
    }

    private <T> Observable<T> withVersionRequestCheck(final Observable<T> observable) {
        return Observable.fromCallable(() -> {
            long now = System.currentTimeMillis();
            if (mServerVersion == null ||
                    (now - mLastServerVersionCheck > DateUtils.DAY_IN_MILLIS)) {
                mServerVersion = getServerVersion().toBlocking().first();
                mSupportedFeatures = filterByVersion(Arrays.asList(Features.values()));
                mLastServerVersionCheck = now;
            }
            return observable.toBlocking().first();
        });
    }

    private Observable<ServerVersion> andCacheVersion(final Observable<ServerVersion> observable) {
        return Observable.fromCallable(() -> {
            mServerVersion = observable.toBlocking().first();
            return mServerVersion;
        });
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> filterByVersion(List<T> o) {
        if (o == null) {
            return null;
        }
        if (mServerVersion == null) {
            mServerVersion = getServerVersion().toBlocking().first();
        }
        ArrayList<T> filter = new ArrayList<>(o.size());
        for (T t : o) {
            boolean isSupported = true;
            try {
                Since a = t.getClass().getDeclaredField(t.toString()).getAnnotation(Since.class);
                if (a != null && a.value() > mServerVersion.getVersion()) {
                    isSupported = false;
                }
            } catch (Exception e) {
                // Ignore
            }
            if (isSupported) {
                filter.add(t);
            }
        }
        return filter;
    }

    // ===============================
    // Non-Api operations
    // ===============================

    private String toUnauthenticatedEndpoint(String endPoint) {
        return endPoint.endsWith("/a/")
                ? endPoint.substring(0, endPoint.length() - 2)
                : endPoint;
    }

    @Override
    public Uri getChangeUri(@NonNull String changeId) {
        return Uri.parse(String.format(Locale.US, "%s#/c/%s",
                toUnauthenticatedEndpoint(mEndPoint), changeId));
    }

    @Override
    public Uri getRevisionUri(@NonNull String changeId, @NonNull String revisionNumber) {
        return Uri.parse(String.format(Locale.US, "%s#/c/%s/%s",
                toUnauthenticatedEndpoint(mEndPoint), changeId, revisionNumber));
    }

    @Override
    public Uri getDownloadRevisionUri(
            @NonNull String changeId, @NonNull String revisionId, @NonNull DownloadFormat format) {
        return Uri.parse(String.format(Locale.US, "%schanges/%s/revisions/%s/archive?format=%s",
                toUnauthenticatedEndpoint(mEndPoint),
                changeId, revisionId, format.toString().toLowerCase()));
    }

    @Override
    public Uri getAvatarUri(String accountId, int size) {
        return Uri.parse(String.format(Locale.US, "%saccounts/%s/avatar?s=%d",
                toUnauthenticatedEndpoint(mEndPoint), accountId, size));
    }

    @Override
    public ApiVersionMediator getApiVersionMediator() {
        return mMediator;
    }

    @Override
    public boolean supportsFeature(Features feature) {
        return mSupportedFeatures.contains(feature);
    }


    // ===============================
    // Gerrit access endpoints
    // @link "https://gerrit-review.googlesource.com/Documentation/rest-api-access.html"
    // ===============================

    @Override
    public Observable<Map<String, ProjectAccessInfo>> getAccessRights(@NonNull String[] names) {
        return withVersionRequestCheck(mService.getAccessRights(names));
    }


    // ===============================
    // Gerrit accounts endpoints
    // @link "https://gerrit-review.googlesource.com/Documentation/rest-api-accounts.html"
    // ===============================

    @Override
    public Observable<List<AccountInfo>> getAccountsSuggestions(
            @NonNull String query, @Nullable Integer count) {
        return withVersionRequestCheck(mService.getAccountsSuggestions(query, count));
    }

    @Override
    public Observable<List<AccountInfo>> getAccounts(
            @NonNull AccountQuery query, @Nullable Integer count,
            @Nullable Integer start, @Nullable List<AccountOptions> options) {
        return withVersionRequestCheck(Observable.fromCallable(
                () -> mService.getAccounts(query, count, start, filterByVersion(options))
                        .toBlocking().first()));
    }

    @Override
    public Observable<AccountInfo> getAccount(@NonNull String accountId) {
        return withVersionRequestCheck(mService.getAccount(accountId));
    }

    @Override
    public Observable<AccountInfo> createAccount(
            @NonNull String username, @NonNull AccountInput input) {
        return withVersionRequestCheck(mService.createAccount(username, input));
    }

    @Override
    public Observable<AccountDetailInfo> getAccountDetails(@NonNull String accountId) {
        return withVersionRequestCheck(mService.getAccountDetails(accountId));
    }

    @Override
    public Observable<String> getAccountName(@NonNull String accountId) {
        return withVersionRequestCheck(mService.getAccountName(accountId));
    }

    @Override
    public Observable<String> setAccountName(
            @NonNull String accountId, @NonNull AccountNameInput input) {
        return withVersionRequestCheck(mService.setAccountName(accountId, input));
    }

    @Override
    public Observable<Void> deleteAccountName(@NonNull String accountId) {
        return withVersionRequestCheck(mService.deleteAccountName(accountId));
    }

    @Override
    public Observable<String> getAccountUsername(@NonNull String accountId) {
        return withVersionRequestCheck(mService.getAccountUsername(accountId));
    }

    @Override
    public Observable<String> setAccountUsername(
            @NonNull String accountId, @NonNull UsernameInput input) {
        return withVersionRequestCheck(mService.setAccountUsername(accountId, input));
    }

    @Override
    public Observable<String> isAccountActive(@NonNull String accountId) {
        return withVersionRequestCheck(mService.isAccountActive(accountId));
    }

    @Override
    public Observable<Void> setAccountAsActive(@NonNull String accountId) {
        return withVersionRequestCheck(mService.setAccountAsActive(accountId));
    }

    @Override
    public Observable<Void> setAccountAsInactive(@NonNull String accountId) {
        return withVersionRequestCheck(mService.setAccountAsInactive(accountId));
    }

    @Override
    public Observable<String> getHttpPassword(@NonNull String accountId) {
        return withVersionRequestCheck(mService.getHttpPassword(accountId));
    }

    @Override
    public Observable<String> setHttpPassword(
            @NonNull String accountId, @NonNull HttpPasswordInput input) {
        return withVersionRequestCheck(mService.setHttpPassword(accountId, input));
    }

    @Override
    public Observable<Void> deleteHttpPassword(@NonNull String accountId) {
        return withVersionRequestCheck(mService.deleteHttpPassword(accountId));
    }

    @Override
    public Observable<OAuthTokenInfo> getOAuthToken(@NonNull String accountId) {
        return withVersionRequestCheck(mService.getOAuthToken(accountId));
    }

    @Override
    public Observable<List<EmailInfo>> getAccountEmails(@NonNull String accountId) {
        return withVersionRequestCheck(mService.getAccountEmails(accountId));
    }

    @Override
    public Observable<EmailInfo> getAccountEmail(
            @NonNull String accountId, @NonNull String emailId) {
        return withVersionRequestCheck(mService.getAccountEmail(accountId, emailId));
    }

    @Override
    public Observable<EmailInfo> createAccountEmail(@NonNull String accountId,
            @NonNull String emailId, @NonNull EmailInput input) {
        return withVersionRequestCheck(mService.createAccountEmail(accountId, emailId, input));
    }

    @Override
    public Observable<Void> deleteAccountEmail(@NonNull String accountId, @NonNull String emailId) {
        return withVersionRequestCheck(mService.deleteAccountEmail(accountId, emailId));
    }

    @Override
    public Observable<Void> setAccountPreferredEmail(
            @NonNull String accountId, @NonNull String emailId) {
        return withVersionRequestCheck(mService.setAccountPreferredEmail(accountId, emailId));
    }

    @Override
    public Observable<List<SshKeyInfo>> getAccountSshKeys(@NonNull String accountId) {
        return withVersionRequestCheck(mService.getAccountSshKeys(accountId));
    }

    @Override
    public Observable<SshKeyInfo> getAccountSshKey(@NonNull String accountId, int sshKeyId) {
        return withVersionRequestCheck(mService.getAccountSshKey(accountId, sshKeyId));
    }

    @Override
    public Observable<SshKeyInfo> addAccountSshKey(
            @NonNull String accountId, @NonNull String encodedKey) {
        return withVersionRequestCheck(mService.addAccountSshKey(accountId, encodedKey));
    }

    @Override
    public Observable<Void> deleteAccountSshKey(@NonNull String accountId, int sshKeyId) {
        return withVersionRequestCheck(mService.deleteAccountSshKey(accountId, sshKeyId));
    }

    @Override
    public Observable<List<GpgKeyInfo>> getAccountGpgKeys(@NonNull String accountId) {
        return withVersionRequestCheck(mService.getAccountGpgKeys(accountId));
    }

    @Override
    public Observable<GpgKeyInfo> getAccountGpgKey(
            @NonNull String accountId, @NonNull String gpgKeyId) {
        return withVersionRequestCheck(mService.getAccountGpgKey(accountId, gpgKeyId));
    }

    @Override
    public Observable<Map<String, GpgKeyInfo>> addAccountGpgKeys(
            @NonNull String accountId, @NonNull AddGpgKeyInput input) {
        return withVersionRequestCheck(mService.addAccountGpgKeys(accountId, input));
    }

    @Override
    public Observable<Map<String, GpgKeyInfo>> deleteAccountGpgKeys(
            @NonNull String accountId, @NonNull DeleteGpgKeyInput input) {
        return withVersionRequestCheck(mService.deleteAccountGpgKeys(accountId, input));
    }

    @Override
    public Observable<AccountCapabilityInfo> getAccountCapabilities(
            @NonNull String accountId, @Nullable List<Capability> filter) {
        return withVersionRequestCheck(Observable.fromCallable(
                () -> mService.getAccountCapabilities(accountId, filterByVersion(filter))
                        .toBlocking().first()));
    }

    @Override
    public Observable<String> hasAccountCapability(
            @NonNull String accountId, @NonNull Capability capabilityId) {
        return withVersionRequestCheck(mService.hasAccountCapability(accountId, capabilityId));
    }

    @Override
    public Observable<List<GroupInfo>> getAccountGroups(@NonNull String accountId) {
        return withVersionRequestCheck(mService.getAccountGroups(accountId));
    }

    @Override
    public Observable<ResponseBody> getAccountAvatar(
            @NonNull String accountId, @Nullable Integer size) {
        return withVersionRequestCheck(mService.getAccountAvatar(accountId, size));
    }

    @Override
    public Observable<String> getAccountAvatarChangeUrl(@NonNull String accountId) {
        return withVersionRequestCheck(mService.getAccountAvatarChangeUrl(accountId));
    }

    @Override
    public Observable<PreferencesInfo> getAccountPreferences(@NonNull String accountId) {
        return withVersionRequestCheck(mService.getAccountPreferences(accountId));
    }

    @Override
    public Observable<PreferencesInfo> setAccountPreferences(
            @NonNull String accountId,
            @NonNull PreferencesInput input) {
        return withVersionRequestCheck(mService.setAccountPreferences(accountId, input));
    }

    @Override
    public Observable<DiffPreferencesInfo> getAccountDiffPreferences(@NonNull String accountId) {
        return withVersionRequestCheck(mService.getAccountDiffPreferences(accountId));
    }

    @Override
    public Observable<DiffPreferencesInfo> setAccountDiffPreferences(
            @NonNull String accountId,
            @NonNull DiffPreferencesInput input) {
        return withVersionRequestCheck(mService.setAccountDiffPreferences(accountId, input));
    }

    @Override
    public Observable<EditPreferencesInfo> getAccountEditPreferences(@NonNull String accountId) {
        return withVersionRequestCheck(mService.getAccountEditPreferences(accountId));
    }

    @Override
    public Observable<EditPreferencesInfo> setAccountEditPreferences(
            @NonNull String accountId,
            @NonNull EditPreferencesInput input) {
        return withVersionRequestCheck(mService.setAccountEditPreferences(accountId, input));
    }

    @Override
    public Observable<List<ProjectWatchInfo>> getAccountWatchedProjects(@NonNull String accountId) {
        return withVersionRequestCheck(mService.getAccountWatchedProjects(accountId));
    }

    @Override
    public Observable<List<ProjectWatchInfo>> addOrUpdateAccountWatchedProjects(
            @NonNull String accountId, @NonNull List<ProjectWatchInput> input) {
        return withVersionRequestCheck(mService.addOrUpdateAccountWatchedProjects(accountId, input));
    }

    @Override
    public Observable<Void> deleteAccountWatchedProjects(
            @NonNull String accountId, @NonNull List<DeleteProjectWatchInput> input) {
        return withVersionRequestCheck(mService.deleteAccountWatchedProjects(accountId, input));
    }

    @Override
    public Observable<List<ChangeInfo>> getDefaultStarredChanges(@NonNull String accountId) {
        return withVersionRequestCheck(mService.getDefaultStarredChanges(accountId));
    }

    @Override
    public Observable<Void> putDefaultStarOnChange(
            @NonNull String accountId, @NonNull String changeId) {
        return withVersionRequestCheck(mService.putDefaultStarOnChange(accountId, changeId));
    }

    @Override
    public Observable<Void> deleteDefaultStarFromChange(
            @NonNull String accountId, @NonNull String changeId) {
        return withVersionRequestCheck(mService.deleteDefaultStarFromChange(accountId, changeId));
    }

    @Override
    public Observable<List<ChangeInfo>> getStarredChanges(@NonNull String accountId) {
        return withVersionRequestCheck(mService.getStarredChanges(accountId));
    }

    @Override
    public Observable<List<String>> getStarLabelsFromChange(
            @NonNull String accountId, @NonNull String changeId) {
        return withVersionRequestCheck(mService.getStarLabelsFromChange(accountId, changeId));
    }

    @Override
    public Observable<List<String>> updateStarLabelsFromChange(@NonNull String accountId,
            @NonNull String changeId, @NonNull StarInput input) {
        return withVersionRequestCheck(mService.updateStarLabelsFromChange(accountId, changeId, input));
    }

    @Override
    public Observable<List<ContributorAgreementInfo>> getContributorAgreements(
            @NonNull String accountId) {
        return withVersionRequestCheck(mService.getContributorAgreements(accountId));
    }

    @Override
    public Observable<String> signContributorAgreement(
            @NonNull String accountId, @NonNull ContributorAgreementInput input) {
        return withVersionRequestCheck(mService.signContributorAgreement(accountId, input));
    }



    // ===============================
    // Gerrit changes endpoints
    // @link "https://gerrit-review.googlesource.com/Documentation/rest-api-changes.html"
    // ===============================

    @Override
    public Observable<ChangeInfo> createChange(@NonNull ChangeInput input) {
        return withVersionRequestCheck(mService.createChange(input));
    }

    @Override
    public Observable<List<ChangeInfo>> getChanges(
            @NonNull ChangeQuery query, @Nullable Integer count,
            @Nullable Integer start, @Nullable List<ChangeOptions> options) {
        return withVersionRequestCheck(Observable.fromCallable(
                () -> mService.getChanges(query, count, start, filterByVersion(options))
                        .toBlocking().first()));
    }

    @Override
    public Observable<ChangeInfo> getChange(
            @NonNull String changeId, @Nullable List<ChangeOptions> options) {
        return withVersionRequestCheck(Observable.fromCallable(
                () -> mService.getChange(changeId, filterByVersion(options))
                        .toBlocking().first()));
    }

    @Override
    public Observable<ChangeInfo> getChangeDetail(
            @NonNull String changeId, @Nullable List<ChangeOptions> options) {
        return withVersionRequestCheck(Observable.fromCallable(
                () -> mService.getChangeDetail(changeId, filterByVersion(options))
                        .toBlocking().first()));
    }

    @Override
    public Observable<String> getChangeTopic(@NonNull String changeId) {
        return withVersionRequestCheck(mService.getChangeTopic(changeId));
    }

    @Override
    public Observable<String> setChangeTopic(@NonNull String changeId, @NonNull TopicInput input) {
        return withVersionRequestCheck(mService.setChangeTopic(changeId, input));
    }

    @Override
    public Observable<Void> deleteChangeTopic(@NonNull String changeId) {
        return withVersionRequestCheck(mService.deleteChangeTopic(changeId));
    }

    @Override
    public Observable<ChangeInfo> abandonChange(
            @NonNull String changeId, @NonNull AbandonInput input) {
        return withVersionRequestCheck(mService.abandonChange(changeId, input));
    }

    @Override
    public Observable<ChangeInfo> restoreChange(
            @NonNull String changeId, @NonNull RestoreInput input) {
        return withVersionRequestCheck(mService.restoreChange(changeId, input));
    }

    @Override
    public Observable<ChangeInfo> rebaseChange(
            @NonNull String changeId, @NonNull RebaseInput input) {
        return withVersionRequestCheck(mService.rebaseChange(changeId, input));
    }

    @Override
    public Observable<ChangeInfo> moveChange(
            @NonNull String changeId, @NonNull MoveInput input) {
        return withVersionRequestCheck(mService.moveChange(changeId, input));
    }

    @Override
    public Observable<ChangeInfo> revertChange(
            @NonNull String changeId, @NonNull RevertInput input) {
        return withVersionRequestCheck(mService.revertChange(changeId, input));
    }

    @Override
    public Observable<ChangeInfo> submitChange(
            @NonNull String changeId, @NonNull SubmitInput input) {
        return withVersionRequestCheck(mService.submitChange(changeId, input));
    }

    @Override
    public Observable<List<ChangeInfo>> getChangesSubmittedTogether(
            @NonNull String changeId, @Nullable List<SubmittedTogetherOptions> options) {
        return withVersionRequestCheck(mService.getChangesSubmittedTogether(changeId, options));
    }

    @Override
    public Observable<Void> publishDraftChange(@NonNull String changeId) {
        return withVersionRequestCheck(mService.publishDraftChange(changeId));
    }

    @Override
    public Observable<Void> deleteDraftChange(@NonNull String changeId) {
        return withVersionRequestCheck(mService.deleteDraftChange(changeId));
    }

    @Override
    public Observable<IncludeInInfo> getChangeIncludedIn(@NonNull String changeId) {
        return withVersionRequestCheck(mService.getChangeIncludedIn(changeId));
    }

    @Override
    public Observable<Void> indexChange(@NonNull String changeId) {
        return withVersionRequestCheck(mService.indexChange(changeId));
    }

    @Override
    public Observable<Map<String, List<CommentInfo>>> getChangeComments(@NonNull String changeId) {
        return withVersionRequestCheck(mService.getChangeComments(changeId));
    }

    @Override
    public Observable<Map<String, List<CommentInfo>>> getChangeDraftComments(
            @NonNull String changeId) {
        return withVersionRequestCheck(mService.getChangeDraftComments(changeId));
    }

    @Override
    public Observable<ChangeInfo> checkChange(@NonNull String changeId) {
        return withVersionRequestCheck(mService.checkChange(changeId));
    }

    @Override
    public Observable<ChangeInfo> fixChange(@NonNull String changeId, @NonNull FixInput input) {
        return withVersionRequestCheck(mService.fixChange(changeId, input));
    }

    @Override
    public Observable<EditInfo> getChangeEdit(@NonNull String changeId,
            @Nullable Option list, @Nullable String base, @Nullable Option downloadCommands) {
        return withVersionRequestCheck(mService.getChangeEdit(
                changeId, list, base, downloadCommands));
    }

    @Override
    public Observable<Void> setChangeEdit(
            @NonNull String changeId, @NonNull String fileId, @NonNull RequestBody data) {
        return withVersionRequestCheck(mService.setChangeEdit(changeId, fileId, data));
    }

    @Override
    public Observable<Void> restoreChangeEdit(
            @NonNull String changeId, @NonNull RestoreChangeEditInput input) {
        return withVersionRequestCheck(mService.restoreChangeEdit(changeId, input));
    }

    @Override
    public Observable<Void> renameChangeEdit(
            @NonNull String changeId, @NonNull RenameChangeEditInput input) {
        return withVersionRequestCheck(mService.renameChangeEdit(changeId, input));
    }

    @Override
    public Observable<Void> newChangeEdit(
            @NonNull String changeId, @NonNull NewChangeEditInput input) {
        return withVersionRequestCheck(mService.newChangeEdit(changeId, input));
    }

    @Override
    public Observable<Void> deleteChangeEditFile(@NonNull String changeId, @NonNull String fileId) {
        return withVersionRequestCheck(mService.deleteChangeEditFile(changeId, fileId));
    }

    @Override
    public Observable<Base64Data> getChangeEditFileContent(
            @NonNull String changeId, @NonNull String fileId, @Nullable String base) {
        return withVersionRequestCheck(mService.getChangeEditFileContent(changeId, fileId, base));
    }

    @Override
    public Observable<EditFileInfo> getChangeEditFileMetadata(
            @NonNull String changeId, @NonNull String fileId) {
        return withVersionRequestCheck(mService.getChangeEditFileMetadata(changeId, fileId));
    }

    @Override
    public Observable<String> getChangeEditMessage(@NonNull String changeId) {
        return withVersionRequestCheck(mService.getChangeEditMessage(changeId));
    }

    @Override
    public Observable<Void> setChangeEditMessage(
            @NonNull String changeId, @NonNull ChangeEditMessageInput input) {
        return withVersionRequestCheck(mService.setChangeEditMessage(changeId, input));
    }

    @Override
    public Observable<Void> publishChangeEdit(@NonNull String changeId) {
        return withVersionRequestCheck(mService.publishChangeEdit(changeId));
    }

    @Override
    public Observable<Void> rebaseChangeEdit(@NonNull String changeId) {
        return withVersionRequestCheck(mService.rebaseChangeEdit(changeId));
    }

    @Override
    public Observable<Void> deleteChangeEdit(@NonNull String changeId) {
        return withVersionRequestCheck(mService.deleteChangeEdit(changeId));
    }

    @Override
    public Observable<List<ReviewerInfo>> getChangeReviewers(@NonNull String changeId) {
        return withVersionRequestCheck(mService.getChangeReviewers(changeId));
    }

    @Override
    public Observable<List<SuggestedReviewerInfo>> getChangeSuggestedReviewers(
            @NonNull String changeId, @NonNull String query, @Nullable Integer count) {
        return withVersionRequestCheck(
                mService.getChangeSuggestedReviewers(changeId, query, count));
    }

    @Override
    public Observable<List<ReviewerInfo>> getChangeReviewer(
            @NonNull String changeId, @NonNull String accountId) {
        return withVersionRequestCheck(mService.getChangeReviewer(changeId, accountId));
    }

    @Override
    public Observable<AddReviewerResultInfo> addChangeReviewer(
            @NonNull String changeId, @NonNull ReviewerInput input) {
        return withVersionRequestCheck(mService.addChangeReviewer(changeId, input));
    }

    @Override
    public Observable<Void> deleteChangeReviewer(
            @NonNull String changeId, @NonNull String accountId) {
        return withVersionRequestCheck(mService.deleteChangeReviewer(changeId, accountId));
    }

    @Override
    public Observable<Map<String, Integer>> getChangeReviewerVotes(
            @NonNull String changeId, @NonNull String accountId) {
        return withVersionRequestCheck(mService.getChangeReviewerVotes(changeId, accountId));
    }

    @Override
    public Observable<Void> deleteChangeReviewerVote(@NonNull String changeId,
            @NonNull String accountId, @NonNull String labelId, @NonNull DeleteVoteInput input) {
        return withVersionRequestCheck(
                mService.deleteChangeReviewerVote(changeId, accountId, labelId, input));
    }

    @Override
    public Observable<CommitInfo> getChangeRevisionCommit(
            @NonNull String changeId, @NonNull String revisionId, @Nullable Option links) {
        return withVersionRequestCheck(
                mService.getChangeRevisionCommit(changeId, revisionId, links));
    }

    @Override
    public Observable<Map<String, ActionInfo>> getChangeRevisionActions(
            @NonNull String changeId, @NonNull String revisionId) {
        return withVersionRequestCheck(mService.getChangeRevisionActions(changeId, revisionId));
    }

    @Override
    public Observable<ChangeInfo> getChangeRevisionReview(
            @NonNull String changeId, @NonNull String revisionId) {
        return withVersionRequestCheck(mService.getChangeRevisionReview(changeId, revisionId));
    }

    @Override
    public Observable<ReviewInfo> setChangeRevisionReview(@NonNull String changeId,
            @NonNull String revisionId, @NonNull ReviewInput input) {
        return withVersionRequestCheck(Observable.fromCallable(() -> {
            input.drafts = getApiVersionMediator().resolveDraftActionType(input.drafts);
            return mService.setChangeRevisionReview(
                    changeId, revisionId, input).toBlocking().first();
        }));
    }

    @Override
    public Observable<RelatedChangesInfo> getChangeRevisionRelatedChanges(
            @NonNull String changeId, @NonNull String revisionId) {
        return withVersionRequestCheck(
                mService.getChangeRevisionRelatedChanges(changeId, revisionId));
    }

    @Override
    public Observable<ChangeInfo> rebaseChangeRevision(
            @NonNull String changeId, @NonNull String revisionId, @NonNull RebaseInput input) {
        return withVersionRequestCheck(mService.rebaseChangeRevision(changeId, revisionId, input));
    }

    @Override
    public Observable<SubmitInfo> submitChangeRevision(
            @NonNull String changeId, @NonNull String revisionId) {
        return withVersionRequestCheck(mService.submitChangeRevision(changeId, revisionId));
    }

    @Override
    public Observable<SubmitInfo> publishChangeDraftRevision(
            @NonNull String changeId, @NonNull String revisionId) {
        return withVersionRequestCheck(mService.publishChangeDraftRevision(changeId, revisionId));
    }

    @Override
    public Observable<SubmitInfo> deleteChangeDraftRevision(
            @NonNull String changeId, @NonNull String revisionId) {
        return withVersionRequestCheck(mService.deleteChangeDraftRevision(changeId, revisionId));
    }

    @Override
    public Observable<Base64Data> getChangeRevisionPatch(@NonNull String changeId,
            @NonNull String revisionId, @Nullable Option zip, @Nullable Option download) {
        return withVersionRequestCheck(
                mService.getChangeRevisionPatch(changeId, revisionId, zip, download));
    }

    @Override
    public Observable<MergeableInfo> getChangeRevisionMergeableStatus(@NonNull String changeId,
            @NonNull String revisionId, @Nullable Option otherBranches) {
        return withVersionRequestCheck(
                mService.getChangeRevisionMergeableStatus(changeId, revisionId, otherBranches));
    }

    @Override
    public Observable<SubmitType> getChangeRevisionSubmitType(
            @NonNull String changeId, @NonNull String revisionId) {
        return withVersionRequestCheck(mService.getChangeRevisionSubmitType(changeId, revisionId));
    }

    @Override
    public Observable<SubmitType> testChangeRevisionSubmitType(@NonNull String changeId,
            @NonNull String revisionId, @NonNull RuleInput input) {
        return withVersionRequestCheck(
                mService.testChangeRevisionSubmitType(changeId, revisionId, input));
    }

    @Override
    public Observable<List<SubmitRecordInfo>> testChangeRevisionSubmitRule(
            @NonNull String changeId, @NonNull String revisionId, @NonNull RuleInput input) {
        return withVersionRequestCheck(
                mService.testChangeRevisionSubmitRule(changeId, revisionId, input));
    }

    @Override
    public Observable<Map<String, List<CommentInfo>>> getChangeRevisionDrafts(
            @NonNull String changeId, @NonNull String revisionId) {
        return withVersionRequestCheck(
                mService.getChangeRevisionDrafts(changeId, revisionId));
    }

    @Override
    public Observable<CommentInfo> createChangeRevisionDraft(
            @NonNull String changeId, @NonNull String revisionId, @NonNull CommentInput input) {
        return withVersionRequestCheck(
                mService.createChangeRevisionDraft(changeId, revisionId, input));
    }

    @Override
    public Observable<CommentInfo> getChangeRevisionDraft(
            @NonNull String changeId, @NonNull String revisionId, @NonNull String draftId) {
        return withVersionRequestCheck(
                mService.getChangeRevisionDraft(changeId, revisionId, draftId));
    }

    @Override
    public Observable<CommentInfo> updateChangeRevisionDraft(@NonNull String changeId,
            @NonNull String revisionId, @NonNull String draftId, @NonNull CommentInput input) {
        return withVersionRequestCheck(
                mService.updateChangeRevisionDraft(changeId, revisionId, draftId, input));
    }

    @Override
    public Observable<Void> deleteChangeRevisionDraft(
            @NonNull String changeId, @NonNull String revisionId, @NonNull String draftId) {
        return withVersionRequestCheck(
                mService.deleteChangeRevisionDraft(changeId, revisionId, draftId));
    }

    @Override
    public Observable<Map<String, List<CommentInfo>>> getChangeRevisionComments(
            @NonNull String changeId, @NonNull String revisionId) {
        return withVersionRequestCheck(mService.getChangeRevisionComments(changeId, revisionId));
    }

    @Override
    public Observable<CommentInfo> getChangeRevisionComment(@NonNull String changeId,
            @NonNull String revisionId, @NonNull String commentId) {
        return withVersionRequestCheck(
                mService.getChangeRevisionComment(changeId, revisionId, commentId));
    }

    @Override
    public Observable<Map<String, FileInfo>> getChangeRevisionFiles(
            @NonNull String changeId, @NonNull String revisionId) {
        return withVersionRequestCheck(mService.getChangeRevisionFiles(changeId, revisionId));
    }

    @Override
    public Observable<ResponseBody> getChangeRevisionFileContent(@NonNull String changeId,
            @NonNull String revisionId, @NonNull String fileId) {
        return withVersionRequestCheck(
                mService.getChangeRevisionFileContent(changeId, revisionId, fileId));
    }

    @Override
    public Observable<ResponseBody> getChangeRevisionFileDownload(@NonNull String changeId,
            @NonNull String revisionId, @NonNull String fileId,
            @Nullable SuffixMode suffixMode, @Nullable Integer parent) {
        return withVersionRequestCheck(mService.getChangeRevisionFileDownload(
                changeId, revisionId, fileId, suffixMode, parent));
    }

    @Override
    public Observable<DiffInfo> getChangeRevisionFileDiff(@NonNull String changeId,
            @NonNull String revisionId, @NonNull String fileId, @Nullable Integer base,
            @Nullable Option intraline, @Nullable Option weblinksOnly,
            @Nullable WhitespaceType whitespace, @Nullable IgnoreWhitespaceType ignoreWhitespace,
            @Nullable ContextType context) {
        return withVersionRequestCheck(mService.getChangeRevisionFileDiff(changeId, revisionId,
                        fileId, base, intraline, weblinksOnly,
                        getApiVersionMediator().resolveWhiteSpaceType(whitespace),
                        getApiVersionMediator().resolveIgnoreWhiteSpaceType(ignoreWhitespace),
                        context));
    }

    @Override
    public Observable<BlameInfo> getChangeRevisionFileBlame(@NonNull String changeId,
            @NonNull String revisionId, @NonNull String fileId, @Nullable String base) {
        return withVersionRequestCheck(
                mService.getChangeRevisionFileBlame(changeId, revisionId, fileId, base));
    }

    @Override
    public Observable<Void> setChangeRevisionFileAsReviewed(
            @NonNull String changeId, @NonNull String revisionId, @NonNull String fileId) {
        return withVersionRequestCheck(
                mService.setChangeRevisionFileAsReviewed(changeId, revisionId, fileId));
    }

    @Override
    public Observable<Void> setChangeRevisionFileAsNotReviewed(
            @NonNull String changeId, @NonNull String revisionId, @NonNull String fileId) {
        return withVersionRequestCheck(
                mService.setChangeRevisionFileAsNotReviewed(changeId, revisionId, fileId));
    }

    @Override
    public Observable<ChangeInfo> cherryPickChangeRevision(@NonNull String changeId,
            @NonNull String revisionId, @NonNull CherryPickInput input) {
        return withVersionRequestCheck(
                mService.cherryPickChangeRevision(changeId, revisionId, input));
    }




    // ===============================
    // Gerrit configuration endpoints
    // @link "https://gerrit-review.googlesource.com/Documentation/rest-api-config.html"
    // ===============================

    @Override
    public Observable<ServerVersion> getServerVersion() {
        return andCacheVersion(mService.getServerVersion());
    }

    @Override
    public Observable<ServerInfo> getServerInfo() {
        return withVersionRequestCheck(mService.getServerInfo());
    }

    @Override
    public Observable<Void> confirmEmail(@NonNull EmailConfirmationInput input) {
        return withVersionRequestCheck(mService.confirmEmail(input));
    }

    @Override
    public Observable<Map<String, CacheInfo>> getServerCaches() {
        return withVersionRequestCheck(mService.getServerCaches());
    }

    @Override
    public Observable<Void> executeServerCachesOperations(CacheOperationInput input) {
        return withVersionRequestCheck(mService.executeServerCachesOperations(input));
    }

    @Override
    public Observable<CacheInfo> getServerCache(@NonNull String cacheId) {
        return withVersionRequestCheck(mService.getServerCache(cacheId));
    }

    @Override
    public Observable<Void> flushServerCache(@NonNull String cacheId) {
        return withVersionRequestCheck(mService.flushServerCache(cacheId));
    }

    @Override
    public Observable<SummaryInfo> getServerSummary(@Nullable Option jvm, @Nullable Option gc) {
        return withVersionRequestCheck(mService.getServerSummary(jvm, gc));
    }

    @Override
    public Observable<Map<Capability, ServerCapabilityInfo>> getServerCapabilities() {
        return withVersionRequestCheck(mService.getServerCapabilities());
    }

    @Override
    public Observable<List<TaskInfo>> getServerTasks() {
        return withVersionRequestCheck(mService.getServerTasks());
    }

    @Override
    public Observable<TaskInfo> getServerTask(@NonNull String taskId) {
        return withVersionRequestCheck(mService.getServerTask(taskId));
    }

    @Override
    public Observable<Void> deleteServerTask(@NonNull String taskId) {
        return withVersionRequestCheck(mService.deleteServerTask(taskId));
    }

    @Override
    public Observable<List<TopMenuEntryInfo>> getServerTopMenus() {
        return withVersionRequestCheck(mService.getServerTopMenus());
    }

    @Override
    public Observable<PreferencesInfo> getServerDefaultPreferences() {
        return withVersionRequestCheck(mService.getServerDefaultPreferences());
    }

    @Override
    public Observable<PreferencesInfo> setServerDefaultPreferences(
            @NonNull PreferencesInput input) {
        return withVersionRequestCheck(mService.setServerDefaultPreferences(input));
    }

    @Override
    public Observable<DiffPreferencesInfo> getServerDefaultDiffPreferences() {
        return withVersionRequestCheck(mService.getServerDefaultDiffPreferences());
    }

    @Override
    public Observable<DiffPreferencesInfo> setServerDefaultDiffPreferences(
            @NonNull DiffPreferencesInput input) {
        return withVersionRequestCheck(mService.setServerDefaultDiffPreferences(input));
    }



    // ===============================
    // Gerrit groups endpoints
    // @link "https://gerrit-review.googlesource.com/Documentation/rest-api-groups.html"
    // ===============================

    @Override
    public Observable<List<GroupInfo>> getGroupSuggestions(
            @NonNull String query, @Nullable Integer count) {
        return withVersionRequestCheck(mService.getGroupSuggestions(query, count));
    }

    @Override
    public Observable<List<GroupInfo>> getGroups(
            @Nullable GroupQuery query, @Nullable Integer count, @Nullable Integer start,
            @Nullable String project, @Nullable String user, @Nullable Option owned,
            @Nullable Option visibleToAll, @Nullable Option verbose,
            @Nullable List<GroupOptions> options) {
        return withVersionRequestCheck(Observable.fromCallable(
                () -> mService.getGroups(query, count, start, project, user, owned,
                        visibleToAll, verbose, filterByVersion(options))
                        .toBlocking().first()));
    }

    @Override
    public Observable<GroupInfo> getGroup(@NonNull String groupId) {
        return withVersionRequestCheck(mService.getGroup(groupId));
    }

    @Override
    public Observable<GroupInfo> createGroup(@NonNull String groupName, @NonNull GroupInput input) {
        return withVersionRequestCheck(mService.createGroup(groupName, input));
    }

    @Override
    public Observable<GroupInfo> getGroupDetail(@NonNull String groupId) {
        return withVersionRequestCheck(mService.getGroupDetail(groupId));
    }

    @Override
    public Observable<String> getGroupName(@NonNull String groupId) {
        return withVersionRequestCheck(mService.getGroupName(groupId));
    }

    @Override
    public Observable<String> setGroupName(@NonNull String groupId, @NonNull GroupNameInput input) {
        return withVersionRequestCheck(mService.setGroupName(groupId, input));
    }

    @Override
    public Observable<String> getGroupDescription(@NonNull String groupId) {
        return withVersionRequestCheck(mService.getGroupDescription(groupId));
    }

    @Override
    public Observable<String> setGroupDescription(
            @NonNull String groupId, @NonNull GroupDescriptionInput input) {
        return withVersionRequestCheck(mService.setGroupDescription(groupId, input));
    }

    @Override
    public Observable<Void> deleteGroupDescription(@NonNull String groupId) {
        return withVersionRequestCheck(mService.deleteGroupDescription(groupId));
    }

    @Override
    public Observable<GroupOptionsInfo> getGroupOptions(@NonNull String groupId) {
        return withVersionRequestCheck(mService.getGroupOptions(groupId));
    }

    @Override
    public Observable<GroupOptionsInfo> setGroupOptions(
            @NonNull String groupId, @NonNull GroupOptionsInput input) {
        return withVersionRequestCheck(mService.setGroupOptions(groupId, input));
    }

    @Override
    public Observable<GroupInfo> getGroupOwner(@NonNull String groupId) {
        return withVersionRequestCheck(mService.getGroupOwner(groupId));
    }

    @Override
    public Observable<GroupInfo> setGroupOwner(
            @NonNull String groupId, @NonNull GroupOwnerInput input) {
        return withVersionRequestCheck(mService.setGroupOwner(groupId, input));
    }

    @Override
    public Observable<List<GroupAuditEventInfo>> getGroupAuditLog(@NonNull String groupId) {
        return withVersionRequestCheck(mService.getGroupAuditLog(groupId));
    }

    @Override
    public Observable<List<AccountInfo>> getGroupMembers(
            @NonNull String groupId, @Nullable Option recursive) {
        return withVersionRequestCheck(mService.getGroupMembers(groupId, recursive));
    }

    @Override
    public Observable<AccountInfo> getGroupMember(
            @NonNull String groupId, @NonNull String accountId) {
        return withVersionRequestCheck(mService.getGroupMember(groupId, accountId));
    }

    @Override
    public Observable<AccountInfo> addGroupMember(
            @NonNull String groupId, @NonNull String accountId) {
        return withVersionRequestCheck(mService.addGroupMember(groupId, accountId));
    }

    @Override
    public Observable<List<AccountInfo>> addGroupMembers(
            @NonNull String groupId, @NonNull MemberInput input) {
        return withVersionRequestCheck(mService.addGroupMembers(groupId, input));
    }

    @Override
    public Observable<Void> deleteGroupMember(@NonNull String groupId, @NonNull String accountId) {
        return withVersionRequestCheck(mService.deleteGroupMember(groupId, accountId));
    }

    @Override
    public Observable<Void> deleteGroupMembers(
            @NonNull String groupId, @NonNull MemberInput input) {
        return withVersionRequestCheck(mService.deleteGroupMembers(groupId, input));
    }

    @Override
    public Observable<List<GroupInfo>> getGroupIncludedGroups(@NonNull String groupId) {
        return withVersionRequestCheck(mService.getGroupIncludedGroups(groupId));
    }

    @Override
    public Observable<GroupInfo> getGroupIncludedGroup(
            @NonNull String groupId, @NonNull String includedGroupId) {
        return withVersionRequestCheck(mService.getGroupIncludedGroup(groupId, includedGroupId));
    }

    @Override
    public Observable<GroupInfo> addGroupIncludeGroup(
            @NonNull String groupId, @NonNull String includedGroupId) {
        return withVersionRequestCheck(mService.addGroupIncludeGroup(groupId, includedGroupId));
    }

    @Override
    public Observable<GroupInfo> addGroupIncludeGroups(
            @NonNull String groupId, @NonNull IncludeGroupInput input) {
        return withVersionRequestCheck(mService.addGroupIncludeGroups(groupId, input));
    }

    @Override
    public Observable<Void> deleteGroupIncludeGroup(
            @NonNull String groupId, @NonNull String includedGroupId) {
        return withVersionRequestCheck(mService.deleteGroupIncludeGroup(groupId, includedGroupId));
    }

    @Override
    public Observable<Void> deleteGroupIncludeGroup(
            @NonNull String groupId, @NonNull IncludeGroupInput input) {
        return withVersionRequestCheck(mService.deleteGroupIncludeGroup(groupId, input));
    }



    // ===============================
    // Gerrit plugins endpoints
    // @link "https://gerrit-review.googlesource.com/Documentation/rest-api-plugins.html"
    // ===============================

    @Override
    public Observable<Map<String, PluginInfo>> getPlugins() {
        return withVersionRequestCheck(mService.getPlugins());
    }

    @Override
    public Observable<PluginInfo> installPlugin(
            @NonNull String pluginId, @NonNull PluginInput input) {
        return withVersionRequestCheck(mService.installPlugin(pluginId, input));
    }

    @Override
    public Observable<PluginInfo> getPluginStatus(@NonNull String pluginId) {
        return withVersionRequestCheck(mService.getPluginStatus(pluginId));
    }

    @Override
    public Observable<PluginInfo> enablePlugin(@NonNull String pluginId) {
        return withVersionRequestCheck(mService.enablePlugin(pluginId));
    }

    @Override
    public Observable<PluginInfo> disablePlugin(@NonNull String pluginId) {
        return withVersionRequestCheck(mService.disablePlugin(pluginId));
    }

    @Override
    public Observable<PluginInfo> reloadPlugin(@NonNull String pluginId) {
        return withVersionRequestCheck(mService.reloadPlugin(pluginId));
    }



    // ===============================
    // Gerrit projects endpoints
    // @link "https://gerrit-review.googlesource.com/Documentation/rest-api-projects.html"
    // ===============================

    @Override
    public Observable<Map<String, ProjectInfo>> getProjects(@Nullable Option showDescription,
            @Nullable Option showTree, @Nullable String branch,
            @Nullable ProjectType type, @Nullable String group) {
        return withVersionRequestCheck(
                mService.getProjects(showDescription, showTree, branch, type, group));
    }

    @Override
    public Observable<ProjectInfo> getProject(@NonNull String projectName) {
        return withVersionRequestCheck(mService.getProject(projectName));
    }

    @Override
    public Observable<ProjectInfo> createProject(
            @NonNull String projectName, @NonNull ProjectInput input) {
        return withVersionRequestCheck(mService.createProject(projectName, input));
    }

    @Override
    public Observable<String> getProjectDescription(@NonNull String projectName) {
        return withVersionRequestCheck(mService.getProjectDescription(projectName));
    }

    @Override
    public Observable<String> setProjectDescription(
            @NonNull String projectName, @NonNull ProjectDescriptionInput input) {
        return withVersionRequestCheck(mService.setProjectDescription(projectName, input));
    }

    @Override
    public Observable<Void> deleteProjectDescription(@NonNull String projectName) {
        return withVersionRequestCheck(mService.deleteProjectDescription(projectName));
    }

    @Override
    public Observable<String> getProjectParent(@NonNull String projectName) {
        return withVersionRequestCheck(mService.getProjectParent(projectName));
    }

    @Override
    public Observable<String> setProjectParent(
            @NonNull String projectName, @NonNull ProjectParentInput input) {
        return withVersionRequestCheck(mService.setProjectParent(projectName, input));
    }

    @Override
    public Observable<String> getProjectHead(@NonNull String projectName) {
        return withVersionRequestCheck(mService.getProjectHead(projectName));
    }

    @Override
    public Observable<String> setProjectHead(
            @NonNull String projectName, @NonNull HeadInput input) {
        return withVersionRequestCheck(mService.setProjectHead(projectName, input));
    }

    @Override
    public Observable<RepositoryStatisticsInfo> getProjectStatistics(@NonNull String projectName) {
        return withVersionRequestCheck(mService.getProjectStatistics(projectName));
    }

    @Override
    public Observable<ConfigInfo> getProjectConfig(@NonNull String projectName) {
        return withVersionRequestCheck(mService.getProjectConfig(projectName));
    }

    @Override
    public Observable<ConfigInfo> setProjectConfig(
            @NonNull String projectName, @NonNull ConfigInput input) {
        return withVersionRequestCheck(mService.setProjectConfig(projectName, input));
    }

    @Override
    public Observable<ResponseBody> runProjectGc(@NonNull String projectName, @NonNull GcInput input) {
        return withVersionRequestCheck(mService.runProjectGc(projectName, input));
    }

    @Override
    public Observable<BanResultInfo> banProject(
            @NonNull String projectName, @NonNull BanInput input) {
        return withVersionRequestCheck(mService.banProject(projectName, input));
    }

    @Override
    public Observable<ProjectAccessInfo> getProjectAccessRights(@NonNull String projectName) {
        return withVersionRequestCheck(mService.getProjectAccessRights(projectName));
    }

    @Override
    public Observable<ProjectAccessInfo> setProjectAccessRights(
            @NonNull String projectName, @NonNull ProjectAccessInput input) {
        return withVersionRequestCheck(mService.setProjectAccessRights(projectName, input));
    }

    @Override
    public Observable<List<BranchInfo>> getProjectBranches(@NonNull String projectName,
            @Nullable Integer count, @Nullable Integer start, @Nullable String substring,
            @Nullable String regexp) {
        return withVersionRequestCheck(
                mService.getProjectBranches(projectName, count, start, substring, regexp));
    }

    @Override
    public Observable<BranchInfo> getProjectBranch(
            @NonNull String projectName, @NonNull String branchId) {
        return withVersionRequestCheck(mService.getProjectBranch(projectName, branchId));
    }

    @Override
    public Observable<BranchInfo> createProjectBranch(
            @NonNull String projectName, @NonNull String branchId, @NonNull BranchInput input) {
        return withVersionRequestCheck(mService.createProjectBranch(projectName, branchId, input));
    }

    @Override
    public Observable<Void> deleteProjectBranch(
            @NonNull String projectName, @NonNull String branchId) {
        return withVersionRequestCheck(mService.deleteProjectBranch(projectName, branchId));
    }

    @Override
    public Observable<Void> deleteProjectBranches(
            @NonNull String projectName, @NonNull DeleteBranchesInput input) {
        return withVersionRequestCheck(mService.deleteProjectBranches(projectName, input));
    }

    @Override
    public Observable<Base64Data> getProjectBranchFileContent(
            @NonNull String projectName, @NonNull String branchId, @NonNull String fileId) {
        return withVersionRequestCheck(
                mService.getProjectBranchFileContent(projectName, branchId, fileId));
    }

    @Override
    public Observable<MergeableInfo> getProjectBranchMergeableStatus(@NonNull String projectName,
            @NonNull String branchId, @NonNull String sourceBranchId,
            @Nullable MergeStrategy strategy) {
        return withVersionRequestCheck(mService.getProjectBranchMergeableStatus(
                projectName, branchId, sourceBranchId, strategy));
    }

    @Override
    public Observable<List<ReflogEntryInfo>> getProjectBranchReflog(
            @NonNull String projectName, @NonNull String branchId) {
        return withVersionRequestCheck(mService.getProjectBranchReflog(projectName, branchId));
    }

    @Override
    public Observable<List<ProjectInfo>> getProjectChildProjects(@NonNull String projectName) {
        return withVersionRequestCheck(mService.getProjectChildProjects(projectName));
    }

    @Override
    public Observable<ProjectInfo> getProjectChildProject(
            @NonNull String projectName, @NonNull String childProjectName) {
        return withVersionRequestCheck(
                mService.getProjectChildProject(projectName, childProjectName));
    }

    @Override
    public Observable<List<TagInfo>> getProjectTags(@NonNull String projectName) {
        return withVersionRequestCheck(mService.getProjectTags(projectName));
    }

    @Override
    public Observable<TagInfo> getProjectTag(@NonNull String projectName, @NonNull String tagId) {
        return withVersionRequestCheck(mService.getProjectTag(projectName, tagId));
    }

    @Override
    public Observable<TagInfo> createProjectTag(
            @NonNull String projectName, @NonNull String tagId, @NonNull TagInput input) {
        return withVersionRequestCheck(mService.createProjectTag(projectName, tagId, input));
    }

    @Override
    public Observable<CommitInfo> getProjectCommit(
            @NonNull String projectName, @NonNull String commitId) {
        return withVersionRequestCheck(mService.getProjectCommit(projectName, commitId));
    }

    @Override
    public Observable<Base64Data> getProjectCommitFileContent(
            @NonNull String projectName, @NonNull String commitId, @NonNull String fileId) {
        return withVersionRequestCheck(
                mService.getProjectCommitFileContent(projectName, commitId, fileId));
    }

    @Override
    public Observable<List<DashboardInfo>> getProjectDashboards(@NonNull String projectName) {
        return withVersionRequestCheck(mService.getProjectDashboards(projectName));
    }

    @Override
    public Observable<DashboardInfo> getProjectDashboard(
            @NonNull String projectName, @NonNull String dashboardId) {
        return withVersionRequestCheck(mService.getProjectDashboard(projectName, dashboardId));
    }

    @Override
    public Observable<DashboardInfo> setProjectDashboard(@NonNull String projectName,
            @NonNull String dashboardId, @NonNull DashboardInput input) {
        return withVersionRequestCheck(
                mService.setProjectDashboard(projectName, dashboardId, input));
    }

    @Override
    public Observable<Void> deleteProjectDashboard(
            @NonNull String projectName, @NonNull String dashboardId) {
        return withVersionRequestCheck(mService.deleteProjectDashboard(projectName, dashboardId));
    }
}
