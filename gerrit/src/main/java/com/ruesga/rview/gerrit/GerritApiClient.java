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
import android.text.format.DateUtils;

import com.burgstaller.okhttp.AuthenticationCacheInterceptor;
import com.burgstaller.okhttp.CachingAuthenticatorDecorator;
import com.burgstaller.okhttp.DispatchingAuthenticator;
import com.burgstaller.okhttp.basic.BasicAuthenticator;
import com.burgstaller.okhttp.digest.CachingAuthenticator;
import com.burgstaller.okhttp.digest.Credentials;
import com.burgstaller.okhttp.digest.DigestAuthenticator;
import com.google.gson.annotations.Since;
import com.ruesga.rview.gerrit.annotations.Until;
import com.ruesga.rview.gerrit.filter.AccountQuery;
import com.ruesga.rview.gerrit.filter.ChangeQuery;
import com.ruesga.rview.gerrit.filter.GroupQuery;
import com.ruesga.rview.gerrit.filter.Option;
import com.ruesga.rview.gerrit.filter.ProjectQuery;
import com.ruesga.rview.gerrit.model.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.reactivex.Observable;
import me.tatarka.rxloader2.safe.SafeObservable;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

class GerritApiClient implements GerritApi {

    private final String mEndPoint;
    private final GerritRestApi mService;
    private final CookieManager mCookieManager;
    private boolean mWasAuthorizedPreviously = false;
    private final PlatformAbstractionLayer mAbstractionLayer;
    private long mLastServerVersionCheck = 0;
    ServerVersion mServerVersion;
    private List<Features> mSupportedFeatures = new ArrayList<>();

    private static final String AUTHENTICATED_PATH = "/a/";
    private static final String LOGIN_PATH = "login/";
    private static final Pattern xAUTH_PATTERN = Pattern.compile(".*?xGerritAuth=\"(.+?)\"");

    private static class CookieManager implements CookieJar {
        private static final String GERRIT_ACCOUNT_COOKIE = "GerritAccount";
        private static final String XSRF_TOKEN_COOKIE = "XSRF_TOKEN";

        private static final List<String> ACCEPTED_COOKIES =
                Arrays.asList(GERRIT_ACCOUNT_COOKIE, XSRF_TOKEN_COOKIE);
        private final Map<String, Cookie> mCookieStore = new LinkedHashMap<>();
        private final String mEntryPoint;

        CookieManager(String entryPoint) {
            mEntryPoint = entryPoint;
        }

        @Override
        public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
            if (isGerritEntryPointUrl(url)) {
                for (Cookie cookie : cookies) {
                    if (ACCEPTED_COOKIES.contains(cookie.name())){
                        mCookieStore.put(cookie.name(), cookie);
                    }
                }
            }
        }

        @Override
        public List<Cookie> loadForRequest(HttpUrl url) {
            List<Cookie> cookies = new ArrayList<>();
            if (isGerritEntryPointUrl(url)) {
                cookies.addAll(mCookieStore.values());
            }
            return cookies;
        }

        private String value(String name) {
            if (mCookieStore.containsKey(name)) {
                return mCookieStore.get(name).value();
            }
            return null;
        }

        private boolean expired(String name) {
            if (mCookieStore.containsKey(name)) {
                long expiresAt = mCookieStore.get(name).expiresAt();
                return expiresAt > 0 && expiresAt < System.currentTimeMillis();
            }
            return false;
        }

        private void add(String name, String value) {
            Cookie cookie = new Cookie.Builder()
                    .name(name)
                    .value(value)
                    .path("/")
                    .expiresAt(-1)
                    .build();
            mCookieStore.put(name, cookie);
        }

        private void clear() {
            mCookieStore.clear();
        }

        private boolean isGerritEntryPointUrl(HttpUrl url) {
            return url.toString().startsWith(mEntryPoint);
        }
    }


    GerritApiClient(String endpoint, Authorization authorization,
            PlatformAbstractionLayer abstractionLayer) {
        mAbstractionLayer = abstractionLayer;
        mEndPoint = endpoint;
        mCookieManager = new CookieManager(toUnauthenticatedEndpoint(mEndPoint));

        Authorization auth = authorization;
        if (auth == null) {
            auth = new Authorization();
        }

        DispatchingAuthenticator authenticator = null;
        if (!auth.isAnonymousUser()) {
            final Credentials credentials = new Credentials(auth.mUsername, auth.mPassword);
            final BasicAuthenticator basicAuthenticator = new BasicAuthenticator(credentials);
            final DigestAuthenticator digestAuthenticator = new DigestAuthenticator(credentials);
            authenticator = new DispatchingAuthenticator.Builder()
                    .with("digest", digestAuthenticator)
                    .with("basic", basicAuthenticator)
                    .requireAuthScheme(false)
                    .build();
        }

        // OkHttp client
        OkHttpClient.Builder clientBuilder = auth.mTrustAllCertificates
                ? OkHttpHelper.getUnsafeClientBuilder() : OkHttpHelper.getSafeClientBuilder();
        clientBuilder
                .readTimeout(20000, java.util.concurrent.TimeUnit.MILLISECONDS)
                .cookieJar(mCookieManager)
                .followRedirects(true)
                .followSslRedirects(true)
                .addInterceptor(createConnectivityCheckInterceptor())
                .addInterceptor(createLoggingInterceptor())
                .addInterceptor(createHeadersInterceptor(auth));
        if (!auth.isAnonymousUser()) {
            final Map<String, CachingAuthenticator> authCache = new ConcurrentHashMap<>();
            clientBuilder
                    .authenticator(new CachingAuthenticatorDecorator(authenticator, authCache))
                    .addInterceptor(new AuthenticationCacheInterceptor(authCache));
        }
        OkHttpClient client = clientBuilder.build();

        // Gson adapter
        GsonConverterFactory gsonFactory = GsonConverterFactory.create(
                GsonHelper.createGerritGsonBuilder(true, mAbstractionLayer).create());

        // RxJava adapter
        RxJava2CallAdapterFactory rxAdapter = RxJava2CallAdapterFactory.create();

        // Retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(endpoint)
                .client(client)
                .addConverterFactory(gsonFactory)
                .addCallAdapterFactory(rxAdapter)
                .build();

        // Build the api
        mService = retrofit.create(GerritRestApi.class);
    }

    private HttpLoggingInterceptor createLoggingInterceptor() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor(mAbstractionLayer::log);
        logging.setLevel(mAbstractionLayer.isDebugBuild()
                ? HttpLoggingInterceptor.Level.BODY : HttpLoggingInterceptor.Level.BASIC);
        return logging;
    }

    private Interceptor createHeadersInterceptor(Authorization auth) {
        return chain -> {
            Request original = chain.request();
            Request.Builder requestBuilder = original.newBuilder();
            if (!mAbstractionLayer.isDebugBuild()) {
                requestBuilder.header("Accept",
                        "application/octet-stream, text/plain, application/json");
            }

            // If the call doesn't support authentication, then we don't need to worry about
            // anything more. Just can an assume the result of the response
            if (auth.isAnonymousUser()) {
                Request request = requestBuilder.build();
                mWasAuthorizedPreviously = false;
                return chain.proceed(request);
            }

            // If the X-Gerrit-Unauthenticated header is present, the api entry point is
            // unauthenticated regardless of whether the client was configured with authentication
            // (for example for the Documentation entry points)
            if (original.header("X-Gerrit-Unauthenticated") != null) {
                changeToUnauthenticatedEntryPoint(original, requestBuilder);
                Request request = requestBuilder.build();
                return chain.proceed(request);
            }

            // Sent cookie authorization?
            String token = mCookieManager.value(CookieManager.XSRF_TOKEN_COOKIE);
            if (token != null && mCookieManager.expired(CookieManager.GERRIT_ACCOUNT_COOKIE)) {
                // Expired? Login again
                if (login(chain, auth)) {
                    token = mCookieManager.value(CookieManager.XSRF_TOKEN_COOKIE);
                }
            }
            if (token != null) {
                requestBuilder.header("X-Gerrit-Auth", token);
                changeToUnauthenticatedEntryPoint(original, requestBuilder);
            }

            // Proceed with the request
            Request request = requestBuilder.build();
            Response response = chain.proceed(request);
            if (response.code() == 401 || (response.code() == 403 && !mWasAuthorizedPreviously)) {
                // Unauthorized or Forbidden without previous being authorized.
                // Try to login in order to obtain a XSRF token
                if (login(chain, auth)) {
                    // Make request again, now with all the necessary tokens
                    changeToUnauthenticatedEntryPoint(original, requestBuilder);
                    request = requestBuilder.build();
                    response = markAsAuthorized(chain.proceed(request));
                }
            }
            return markAsAuthorized(response);
        };
    }

    private Response markAsAuthorized(Response response) {
        if (!mWasAuthorizedPreviously) {
            mWasAuthorizedPreviously = response.isSuccessful();
        }
        return response;
    }

    private boolean login(Interceptor.Chain chain, Authorization auth) {
        // Clear all cookies before try to login
        mCookieManager.clear();
        return basicAuthLogin(chain) || formAuthLogin(chain, auth);
    }

    private boolean basicAuthLogin(Interceptor.Chain chain) {
        try {
            Request original = chain.request();
            Request.Builder requestBuilder =
                    original.newBuilder()
                            .removeHeader("Accept")
                            .url(toUnauthenticatedEndpoint(mEndPoint) + LOGIN_PATH);
            Request request = requestBuilder.build();
            Response response = chain.proceed(request);
            if (!response.isSuccessful()) {
                return false;
            }

            // Extract cookie from body response (in Gerrit <2.12 xAuth cookie is inside html
            // response, rather than as a cookie)
            return extractXAuthCookieFromHtmlBody(response);
        } catch (IOException ex) {
            // Ignore
        }
        return false;
    }

    private boolean formAuthLogin(Interceptor.Chain chain, Authorization auth) {
        try {
            RequestBody requestBody = new FormBody.Builder()
                    .add("username", auth.mUsername)
                    .add("password", auth.mPassword)
                    .build();
            Request original = chain.request();
            Request.Builder requestBuilder =
                    original.newBuilder()
                            .removeHeader("Accept")
                            .url(toUnauthenticatedEndpoint(mEndPoint) + LOGIN_PATH)
                            .post(requestBody);
            Request request = requestBuilder.build();
            Response response = chain.proceed(request);
            if (!response.isSuccessful()) {
                return false;
            }

            // Extract cookie from body response (in Gerrit <2.12 xAuth cookie is inside html
            // response, rather than as a cookie)
            return extractXAuthCookieFromHtmlBody(response);
        } catch (IOException ex) {
            // Ignore
        }
        return false;
    }

    private boolean extractXAuthCookieFromHtmlBody(Response response) throws IOException {
        // If we have the cookie, ignore this call
        if (mCookieManager.value(CookieManager.XSRF_TOKEN_COOKIE) != null) {
            return true;
        }

        if (mCookieManager.value(CookieManager.GERRIT_ACCOUNT_COOKIE) != null) {
            ResponseBody body = response.body();
            if (body != null) {
                try {
                    Matcher matcher = xAUTH_PATTERN.matcher(body.string());
                    if (matcher.find()) {
                        mCookieManager.add(CookieManager.XSRF_TOKEN_COOKIE, matcher.group(1));
                        return true;
                    }
                } finally {
                    body.close();
                }
            }
        }
        return false;
    }

    private void changeToUnauthenticatedEntryPoint(
            Request original, Request.Builder requestBuilder) {
        HttpUrl url = original.url();
        if (url.encodedPath().startsWith(AUTHENTICATED_PATH)) {
            requestBuilder.url(original.url().toString()
                    .replaceFirst(AUTHENTICATED_PATH, "/"));
        }
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
        return SafeObservable.fromNullCallable(() -> {
            long now = System.currentTimeMillis();
            if (mServerVersion == null ||
                    (now - mLastServerVersionCheck > DateUtils.DAY_IN_MILLIS)) {
                mServerVersion = getServerVersion().blockingFirst();
                mSupportedFeatures = filterByVersion(Arrays.asList(Features.values()));
                mLastServerVersionCheck = now;
            }
            return observable.blockingFirst();
        });
    }

    private <T> Observable<T> withEmptyObservable(final Observable<T> observable) {
        return SafeObservable.fromNullCallable(observable::blockingFirst);
    }

    private Observable<ServerVersion> andCacheVersion(final Observable<ServerVersion> observable) {
        return SafeObservable.fromNullCallable(() -> {
            mServerVersion = observable.blockingFirst();
            return mServerVersion;
        });
    }

    private <T> List<T> filterByVersion(List<T> o) {
        if (mServerVersion == null) {
            mServerVersion = getServerVersion().blockingFirst();
        }
        return filterByVersion(o, mServerVersion);
    }

    private <T> List<T> filterByVersion(List<T> o, ServerVersion serverVersion) {
        if (o == null) {
            return null;
        }

        ArrayList<T> filter = new ArrayList<>(o.size());
        for (T t : o) {
            boolean isSupported = true;
            try {
                Since a = t.getClass().getDeclaredField(t.toString()).getAnnotation(Since.class);
                if (a != null && a.value() > serverVersion.getVersion()) {
                    isSupported = false;
                }
            } catch (Exception e) {
                // Ignore
            }
            try {
                Until a = t.getClass().getDeclaredField(t.toString()).getAnnotation(Until.class);
                if (a != null && a.value() <= serverVersion.getVersion()) {
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
    // Mediator methods
    // ===============================

    private IgnoreWhitespaceType resolveIgnoreWhiteSpaceType(WhitespaceType type) {
        if (type == null) {
            return null;
        }
        if (mServerVersion.getVersion() >= 2.13) {
            return null;
        }
        return IgnoreWhitespaceType.values()[type.ordinal()];
    }

    private DraftActionType resolveDraftActionType(DraftActionType type) {
        if (type == null) {
            return null;
        }
        if (mServerVersion.getVersion() <= 2.11
                && type.equals(DraftActionType.PUBLISH_ALL_REVISIONS)) {
            return DraftActionType.PUBLISH;
        }
        return type;
    }

    // From 2.14.3+ api changed the start parameter from s to S to mimic the rest of
    // the api methods.
    private Integer[] resolveStartFor21413(Integer start) {
        Integer s1 = null;
        Integer s2 = null;
        if (start != null) {
            if (mServerVersion.getVersion() < 2.14d) {
                s2 = start;
            } else if (mServerVersion.getVersion() > 2.14d) {
                s1 = start;
            } else {
                // 2.14: Decided based on build (only 2.14, 2.14-rc*,, 2.14.1, 2.14.2). All other
                // combinations are 2.14.3+
                if (mServerVersion.build != null && (mServerVersion.build.isEmpty() ||
                        mServerVersion.build.equals("-rc") ||
                        mServerVersion.build.equals("1") ||
                        mServerVersion.build.equals("2"))) {
                    s2 = start;
                } else {
                    s1 = start;
                }
            }
        }
        return new Integer[]{s1, s2};
    }

    private <T> T resolve(T o, double version) {
        if (mServerVersion.getVersion() < version) {
            return null;
        }
        return o;
    }


    // ===============================
    // Non-Api operations
    // ===============================

    private String toUnauthenticatedEndpoint(String endPoint) {
        return endPoint.endsWith(AUTHENTICATED_PATH)
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
                changeId, revisionId, format.toString().toLowerCase(Locale.US)));
    }

    @Override
    public Uri getPatchFileRevisionUri(
            @NonNull String changeId, @NonNull String revisionId, @NonNull PatchFileFormat format) {
        return Uri.parse(String.format(Locale.US, "%schanges/%s/revisions/%s/patch?%s",
                toUnauthenticatedEndpoint(mEndPoint),
                changeId, revisionId, format.mOption.toLowerCase(Locale.US)));
    }

    @Override
    public Uri getAvatarUri(String accountId, int size) {
        return Uri.parse(String.format(Locale.US, "%saccounts/%s/avatar?s=%d",
                toUnauthenticatedEndpoint(mEndPoint), accountId, size));
    }

    @Override
    public Uri getDocumentationUri(@NonNull String docPath) {
        return Uri.parse(String.format(Locale.US, "%s%s",
                toUnauthenticatedEndpoint(mEndPoint), docPath));
    }

    @Override
    public boolean supportsFeature(Features feature) {
        return mSupportedFeatures.contains(feature);
    }

    @Override
    public boolean supportsFeature(Features feature, ServerVersion version) {
        if (version == null) {
            return false;
        }
        List<Features> features = filterByVersion(Arrays.asList(Features.values()), version);
        return features.contains(feature);
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
            @NonNull String query, @Nullable Integer count, @Nullable Option suggest) {
        return withVersionRequestCheck(mService.getAccountsSuggestions(query, count, suggest));
    }

    @Override
    public Observable<List<AccountInfo>> getAccounts(
            @NonNull AccountQuery query, @Nullable Integer count,
            @Nullable Integer start, @Nullable List<AccountOptions> options) {
        return withVersionRequestCheck(SafeObservable.fromNullCallable(
                () -> mService.getAccounts(query, count, start, filterByVersion(options))
                        .blockingFirst()));
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
        return withVersionRequestCheck(
                withEmptyObservable(
                        mService.deleteAccountName(accountId)));
    }

    @Override
    public Observable<String> getAccountStatus(@NonNull String accountId) {
        return withVersionRequestCheck(mService.getAccountStatus(accountId));
    }

    @Override
    public Observable<String> setAccountStatus(
            @NonNull String accountId, @NonNull AccountStatusInput input) {
        return withVersionRequestCheck(mService.setAccountStatus(accountId, input));
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
    public Observable<String> setAccountDisplayName(
            @NonNull String accountId, @NonNull DisplayNameInput input) {
        return withVersionRequestCheck(mService.setAccountDisplayName(accountId, input));
    }

    @Override
    public Observable<String> isAccountActive(@NonNull String accountId) {
        return withVersionRequestCheck(
                withEmptyObservable(
                        mService.isAccountActive(accountId)));
    }

    @Override
    public Observable<Void> setAccountAsActive(@NonNull String accountId) {
        return withVersionRequestCheck(
                withEmptyObservable(
                        mService.setAccountAsActive(accountId)));
    }

    @Override
    public Observable<Void> setAccountAsInactive(@NonNull String accountId) {
        return withVersionRequestCheck(
                withEmptyObservable(
                        mService.setAccountAsInactive(accountId)));
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
        return withVersionRequestCheck(
                withEmptyObservable(
                        mService.deleteHttpPassword(accountId)));
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
        return withVersionRequestCheck(
                withEmptyObservable(
                        mService.deleteAccountEmail(accountId, emailId)));
    }

    @Override
    public Observable<Void> setAccountPreferredEmail(
            @NonNull String accountId, @NonNull String emailId) {
        return withVersionRequestCheck(
                withEmptyObservable(
                        mService.setAccountPreferredEmail(accountId, emailId)));
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
        return withVersionRequestCheck(
                withEmptyObservable(
                        mService.deleteAccountSshKey(accountId, sshKeyId)));
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
        return withVersionRequestCheck(SafeObservable.fromNullCallable(
                () -> mService.getAccountCapabilities(accountId, filterByVersion(filter))
                        .blockingFirst()));
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
        return withVersionRequestCheck(
                withEmptyObservable(
                        mService.deleteAccountWatchedProjects(accountId, input)));
    }

    @Override
    public Observable<List<AccountExternalIdInfo>> getAccountExternalIds(@NonNull String accountId) {
        return withVersionRequestCheck(mService.getAccountExternalIds(accountId));
    }

    @Override
    public Observable<Void> deleteAccountExternalIds(
            @NonNull String accountId, @NonNull List<String> externalIds) {
        return withVersionRequestCheck(
                withEmptyObservable(
                        mService.deleteAccountExternalIds(accountId, externalIds)));
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

    @Override
    public Observable<Void> indexAccount(@NonNull String accountId) {
        return withVersionRequestCheck(withEmptyObservable(mService.indexAccount(accountId)));
    }

    @Override
    public Observable<List<DeletedDraftCommentInfo>> deleteAccountDraftComments(
            @NonNull String accountId, @NonNull DeleteDraftCommentsInput input) {
        return withVersionRequestCheck(mService.deleteAccountDraftComments(accountId, input));
    }

    @Override
    public Observable<List<ChangeInfo>> getDefaultStarredChanges(@NonNull String accountId) {
        return withVersionRequestCheck(mService.getDefaultStarredChanges(accountId));
    }

    @Override
    public Observable<Void> putDefaultStarOnChange(
            @NonNull String accountId, @NonNull String changeId) {
        return withVersionRequestCheck(
                withEmptyObservable(
                        mService.putDefaultStarOnChange(accountId, changeId)));
    }

    @Override
    public Observable<Void> deleteDefaultStarFromChange(
            @NonNull String accountId, @NonNull String changeId) {
        return withVersionRequestCheck(
                withEmptyObservable(
                        mService.deleteDefaultStarFromChange(accountId, changeId)));
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
        return withVersionRequestCheck(SafeObservable.fromNullCallable(
                () -> mService.getChanges(query, count, start, filterByVersion(options))
                        .blockingFirst()));
    }

    @Override
    public Observable<ChangeInfo> getChange(
            @NonNull String changeId, @Nullable List<ChangeOptions> options) {
        return withVersionRequestCheck(SafeObservable.fromNullCallable(
                () -> mService.getChange(changeId, filterByVersion(options))
                        .blockingFirst()));
    }

    @Override
    public Observable<ChangeInfo> createMergePathSetForChange(
            @NonNull String changeId, @NonNull MergePatchSetInput input) {
        return withVersionRequestCheck(mService.createMergePathSetForChange(changeId, input));
    }

    @Override
    public Observable<ChangeInfo> setChangeCommitMessage(
            @NonNull String changeId, @NonNull CommitMessageInput input) {
        return withVersionRequestCheck(mService.setChangeCommitMessage(changeId, input));
    }

    @Override
    public Observable<ChangeInfo> getChangeDetail(
            @NonNull String changeId, @Nullable List<ChangeOptions> options) {
        return withVersionRequestCheck(SafeObservable.fromNullCallable(
                () -> mService.getChangeDetail(changeId, filterByVersion(options))
                        .blockingFirst()));
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
        return withVersionRequestCheck(
                withEmptyObservable(
                        mService.deleteChangeTopic(changeId)));
    }

    @Override
    public Observable<AccountInfo> getChangeAssignee(@NonNull String changeId) {
        return withVersionRequestCheck(mService.getChangeAssignee(changeId));
    }

    @Override
    public Observable<List<AccountInfo>> getChangePastAssignees(@NonNull String changeId) {
        return withVersionRequestCheck(mService.getChangePastAssignees(changeId));
    }

    @Override
    public Observable<AccountInfo> setChangeAssignee(
            @NonNull String changeId, @NonNull AssigneeInput input) {
        return withVersionRequestCheck(mService.setChangeAssignee(changeId, input));
    }

    @Override
    public Observable<AccountInfo> deleteChangeAssignee(@NonNull String changeId) {
        return withVersionRequestCheck(mService.deleteChangeAssignee(changeId));
    }

    @Override
    public Observable<PureRevertInfo> getChangePureRevert(
            @NonNull String changeId, @Nullable String commit, @Nullable String revertOf) {
        return withVersionRequestCheck(mService.getChangePureRevert(changeId, commit, revertOf));
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
    @Deprecated
    @SuppressWarnings("deprecation")
    public Observable<Void> publishDraftChange(@NonNull String changeId) {
        return withVersionRequestCheck(
                withEmptyObservable(
                        mService.publishDraftChange(changeId)));
    }

    @Override
    public Observable<Void> deleteChange(@NonNull String changeId) {
        return withVersionRequestCheck(
                withEmptyObservable(
                        mService.deleteChange(changeId)));
    }

    @Override
    public Observable<IncludedInInfo> getChangeIncludedIn(@NonNull String changeId) {
        return withVersionRequestCheck(mService.getChangeIncludedIn(changeId));
    }

    @Override
    public Observable<Void> indexChange(@NonNull String changeId) {
        return withVersionRequestCheck(
                withEmptyObservable(
                        mService.indexChange(changeId)));
    }

    @Override
    public Observable<Map<String, List<CommentInfo>>> getChangeComments(@NonNull String changeId) {
        return withVersionRequestCheck(mService.getChangeComments(changeId));
    }

    @Override
    public Observable<Map<String, List<RobotCommentInfo>>> getChangeRobotComments(
            @NonNull String changeId) {
        return withVersionRequestCheck(mService.getChangeRobotComments(changeId));
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
    public Observable<Void> setChangeWorkInProgress(
            @NonNull String changeId, @NonNull WorkInProgressInput input) {
        return withVersionRequestCheck(
                withEmptyObservable(
                        mService.setChangeWorkInProgress(changeId, input)));
    }

    @Override
    public Observable<Void> setChangeReadyForReview(
            @NonNull String changeId, @NonNull WorkInProgressInput input) {
        return withVersionRequestCheck(
                withEmptyObservable(
                        mService.setChangeReadyForReview(changeId, input)));
    }

    @Override
    public Observable<Void> markChangeAsPrivate(
            @NonNull String changeId, @NonNull PrivateInput input) {
        return withVersionRequestCheck(
                withEmptyObservable(
                        mService.markChangeAsPrivate(changeId, input)));
    }

    @Override
    public Observable<Void> unmarkChangeAsPrivate(
            @NonNull String changeId, @NonNull PrivateInput input) {
        return withVersionRequestCheck(
                withEmptyObservable(
                        mService.unmarkChangeAsPrivate(changeId, input)));
    }

    @Override
    public Observable<Void> ignoreChange(@NonNull String changeId) {
        return withVersionRequestCheck(
                withEmptyObservable(
                        mService.ignoreChange(changeId)));
    }

    @Override
    public Observable<Void> unignoreChange(@NonNull String changeId) {
        return withVersionRequestCheck(
                withEmptyObservable(
                        mService.unignoreChange(changeId)));
    }

    @Override
    public Observable<Void> markChangeAsReviewed(@NonNull String changeId) {
        return withVersionRequestCheck(
                withEmptyObservable(
                        mService.markChangeAsReviewed(changeId)));
    }

    @Override
    public Observable<Void> markChangeAsUnreviewed(@NonNull String changeId) {
        return withVersionRequestCheck(
                withEmptyObservable(
                        mService.markChangeAsUnreviewed(changeId)));
    }

    @Override
    public Observable<String[]> getChangeHashtags(@NonNull String changeId) {
        return withVersionRequestCheck(mService.getChangeHashtags(changeId));
    }

    @Override
    public Observable<String[]> setChangeHashtags(
            @NonNull String changeId, @NonNull HashtagsInput input) {
        return withVersionRequestCheck(mService.setChangeHashtags(changeId, input));
    }

    @Override
    public Observable<List<ChangeMessageInfo>> getChangeMessages(@NonNull String changeId) {
        return withVersionRequestCheck(mService.getChangeMessages(changeId));
    }

    @Override
    public Observable<ChangeMessageInfo> getChangeMessage(
            @NonNull String changeId, @NonNull String messageId) {
        return withVersionRequestCheck(mService.getChangeMessage(changeId, messageId));
    }

    @Override
    public Observable<ChangeMessageInfo> deleteChangeMessage(
            @NonNull String changeId, @NonNull String messageId,
            @NonNull DeleteChangeMessageInput input) {
        return withVersionRequestCheck(mService.deleteChangeMessage(changeId, messageId, input));
    }

    @Override
    public Observable<EditInfo> getChangeEdit(@NonNull String changeId,
            @Nullable Option list, @Nullable String base, @Nullable Option downloadCommands) {
        return withVersionRequestCheck(mService.getChangeEdit(
                changeId, list, base, downloadCommands));
    }

    @Override
    public Observable<Void> setChangeEditFile(
            @NonNull String changeId, @NonNull String fileId, @NonNull RequestBody data) {
        return withVersionRequestCheck(
                withEmptyObservable(
                        mService.setChangeEditFile(changeId, fileId, data)));
    }

    @Override
    public Observable<Void> restoreChangeEditFile(
            @NonNull String changeId, @NonNull RestoreChangeEditInput input) {
        return withVersionRequestCheck(
                withEmptyObservable(
                        mService.restoreChangeEditFile(changeId, input)));
    }

    @Override
    public Observable<Void> renameChangeEditFile(
            @NonNull String changeId, @NonNull RenameChangeEditInput input) {
        return withVersionRequestCheck(
                withEmptyObservable(
                        mService.renameChangeEditFile(changeId, input)));
    }

    @Override
    public Observable<Void> newChangeEditFile(
            @NonNull String changeId, @NonNull NewChangeEditInput input) {
        return withVersionRequestCheck(
                withEmptyObservable(
                        mService.newChangeEditFile(changeId, input)));
    }

    @Override
    public Observable<Void> deleteChangeEditFile(@NonNull String changeId, @NonNull String fileId) {
        return withVersionRequestCheck(
                withEmptyObservable(
                        mService.deleteChangeEditFile(changeId, fileId)));
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
        return withVersionRequestCheck(
                withEmptyObservable(
                        mService.setChangeEditMessage(changeId, input)));
    }

    @Override
    public Observable<Void> publishChangeEdit(@NonNull String changeId) {
        return withVersionRequestCheck(
                withEmptyObservable(
                        mService.publishChangeEdit(changeId)));
    }

    @Override
    public Observable<Void> rebaseChangeEdit(@NonNull String changeId) {
        return withVersionRequestCheck(
                withEmptyObservable(
                        mService.rebaseChangeEdit(changeId)));
    }

    @Override
    public Observable<Void> deleteChangeEdit(@NonNull String changeId) {
        return withVersionRequestCheck(
                withEmptyObservable(
                        mService.deleteChangeEdit(changeId)));
    }

    @Override
    public Observable<List<ReviewerInfo>> getChangeReviewers(@NonNull String changeId) {
        return withVersionRequestCheck(mService.getChangeReviewers(changeId));
    }

    @Override
    @SuppressWarnings("deprecation")
    public Observable<List<SuggestedReviewerInfo>> getChangeSuggestedReviewers(
            @NonNull String changeId, @NonNull String query, @Nullable Integer count,
            @Nullable Option excludeGroups, @Nullable SuggestedReviewersState reviewersState) {
        return withVersionRequestCheck(
            mServerVersion.getVersion() >= 3.1d
                    ? mService.getChangeSuggestedReviewers(
                            changeId, query, count, excludeGroups, reviewersState)
                    : mService.getChangeSuggestedReviewers(
                            changeId, query, count, resolve(
                                    excludeGroups != null
                                            ? ExcludeGroupsFromSuggestedReviewers.INSTANCE
                                            : null, 2.15d)));
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
        return withVersionRequestCheck(
                withEmptyObservable(
                        mService.deleteChangeReviewer(changeId, accountId)));
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
                withEmptyObservable(
                    mService.deleteChangeReviewerVote(changeId, accountId, labelId, input)));
    }

    @Override
    public Observable<CommitInfo> getChangeRevisionCommit(
            @NonNull String changeId, @NonNull String revisionId, @Nullable Option links) {
        return withVersionRequestCheck(
                mService.getChangeRevisionCommit(changeId, revisionId, links));
    }

    @Override
    public Observable<String> getChangeRevisionDescription(
            @NonNull String changeId, @NonNull String revisionId) {
        return withVersionRequestCheck(mService.getChangeRevisionDescription(changeId, revisionId));
    }

    @Override
    public Observable<String> setChangeRevisionDescription(
            @NonNull String changeId, @NonNull String revisionId, @NonNull DescriptionInput input) {
        return withVersionRequestCheck(
                mService.setChangeRevisionDescription(changeId, revisionId, input));
    }

    @Override
    public Observable<List<CommentInfo>> getChangeRevisionMergeList(
            @NonNull String changeId, @NonNull String revisionId) {
        return withVersionRequestCheck(mService.getChangeRevisionMergeList(changeId, revisionId));
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
    public Observable<ReviewResultInfo> setChangeRevisionReview(@NonNull String changeId,
            @NonNull String revisionId, @NonNull ReviewInput input) {
        return withVersionRequestCheck(SafeObservable.fromNullCallable(() -> {
            if (mServerVersion.getVersion() >= 2.15) {
                // Since 2.15, strictLabels was remove from ReviewInput
                input.strictLabels = null;
            }
            input.drafts = resolveDraftActionType(input.drafts);
            return mService.setChangeRevisionReview(
                    changeId, revisionId, input).blockingFirst();
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
    @Deprecated
    @SuppressWarnings("deprecation")
    public Observable<Void> publishChangeDraftRevision(
            @NonNull String changeId, @NonNull String revisionId) {
        return withVersionRequestCheck(
                withEmptyObservable(
                        mService.publishChangeDraftRevision(changeId, revisionId)));
    }

    @Override
    @Deprecated
    @SuppressWarnings("deprecation")
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
    public Observable<ResponseBody> getChangeRevisionSubmitPreview(@NonNull String changeId, @NonNull String revisionId) {
        return withVersionRequestCheck(mService.getChangeRevisionSubmitPreview(changeId, revisionId));
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
                withEmptyObservable(
                    mService.deleteChangeRevisionDraft(changeId, revisionId, draftId)));
    }

    @Override
    public Observable<Map<String, List<CommentInfo>>> getChangeRevisionComments(
            @NonNull String changeId, @NonNull String revisionId) {
        return withVersionRequestCheck(mService.getChangeRevisionComments(changeId, revisionId));
    }

    @Override
    public Observable<Map<String, List<RobotCommentInfo>>> getChangeRevisionRobotComments(
            @NonNull String changeId, @NonNull String revisionId) {
        return withVersionRequestCheck(
                mService.getChangeRevisionRobotComments(changeId, revisionId));
    }

    @Override
    public Observable<RobotCommentInfo> getChangeRevisionRobotComment(
            @NonNull String changeId, @NonNull String revisionId, @NonNull String commentId) {
        return withVersionRequestCheck(
                mService.getChangeRevisionRobotComment(changeId, revisionId, commentId));
    }

    @Override
    public Observable<EditInfo> applyChangeRevisionFix(
            @NonNull String changeId, @NonNull String revisionId, @NonNull String fixId) {
        return withVersionRequestCheck(
                mService.applyChangeRevisionFix(changeId, revisionId, fixId));
    }

    @Override
    public Observable<CommentInfo> getChangeRevisionComment(@NonNull String changeId,
            @NonNull String revisionId, @NonNull String commentId) {
        return withVersionRequestCheck(
                mService.getChangeRevisionComment(changeId, revisionId, commentId));
    }

    @Override
    public Observable<CommentInfo> deleteChangeRevisionComment(
            @NonNull String changeId, @NonNull String revisionId,
            @NonNull String commentId, @NonNull DeleteCommentInput input) {
        return withVersionRequestCheck(
                mService.deleteChangeRevisionComment(changeId, revisionId, commentId, input));
    }

    @Override
    public Observable<Map<String, FileInfo>> getChangeRevisionFiles(
            @NonNull String changeId, @NonNull String revisionId, @Nullable String base,
            @Nullable Option reviewed) {
        return withVersionRequestCheck(mService.getChangeRevisionFiles(
                changeId, revisionId, base, reviewed));
    }

    @Override
    public Observable<List<String>> getChangeRevisionFilesSuggestion(
            @NonNull String changeId, @NonNull String revisionId, @Nullable String base,
            @Nullable Option reviewed, String filter) {
        return withVersionRequestCheck(mService.getChangeRevisionFilesSuggestion(
                changeId, revisionId, base, reviewed, filter));
    }

    @Override
    public Observable<ResponseBody> getChangeRevisionFileContent(@NonNull String changeId,
            @NonNull String revisionId, @NonNull String fileId, Integer parent) {
        return withVersionRequestCheck(SafeObservable.fromCallable(() -> {
            Integer p = resolve(parent, 2.15d);
            return mService.getChangeRevisionFileContent(
                    changeId, revisionId, fileId, p).blockingFirst();
        }));
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
            @Nullable WhitespaceType whitespace, @Nullable ContextType context) {
        return withVersionRequestCheck(mService.getChangeRevisionFileDiff(changeId, revisionId,
                        fileId, base, intraline, weblinksOnly,
                        resolve(whitespace, 2.13d),
                        resolveIgnoreWhiteSpaceType(whitespace),
                        context));
    }

    @Override
    public Observable<List<BlameInfo>> getChangeRevisionFileBlames(@NonNull String changeId,
            @NonNull String revisionId, @NonNull String fileId, @Nullable BlameBaseType base) {
        return withVersionRequestCheck(
                mService.getChangeRevisionFileBlames(changeId, revisionId, fileId, base));
    }

    @Override
    public Observable<Void> setChangeRevisionFileAsReviewed(
            @NonNull String changeId, @NonNull String revisionId, @NonNull String fileId) {
        return withVersionRequestCheck(
                withEmptyObservable(
                    mService.setChangeRevisionFileAsReviewed(changeId, revisionId, fileId)));
    }

    @Override
    public Observable<Void> setChangeRevisionFileAsNotReviewed(
            @NonNull String changeId, @NonNull String revisionId, @NonNull String fileId) {
        return withVersionRequestCheck(
                withEmptyObservable(
                    mService.setChangeRevisionFileAsNotReviewed(changeId, revisionId, fileId)));
    }

    @Override
    public Observable<CherryPickChangeInfo> cherryPickChangeRevision(@NonNull String changeId,
            @NonNull String revisionId, @NonNull CherryPickInput input) {
        return withVersionRequestCheck(
                mService.cherryPickChangeRevision(changeId, revisionId, input));
    }

    @Override
    public Observable<List<ReviewerInfo>> getChangeRevisionReviewers(
            @NonNull String changeId, @NonNull String revisionId) {
        return withVersionRequestCheck(mService.getChangeRevisionReviewers(changeId, revisionId));
    }

    @Override
    public Observable<List<ReviewerInfo>> getChangeRevisionReviewersVotes(
            @NonNull String changeId, @NonNull String revisionId, @NonNull String accountId) {
        return withVersionRequestCheck(mService.getChangeRevisionReviewersVotes(
                changeId, revisionId, accountId));
    }

    @Override
    public Observable<Void> deleteChangeRevisionReviewerVote(
            @NonNull String changeId, @NonNull String revisionId, @NonNull String accountId,
            @NonNull String labelId, @NonNull DeleteVoteInput input) {
        return withVersionRequestCheck(
                withEmptyObservable(
                        mService.deleteChangeRevisionReviewerVote(
                                changeId, revisionId, accountId, labelId, input)));
    }



    // ===============================
    // Gerrit configuration endpoints
    // @link "https://gerrit-review.googlesource.com/Documentation/rest-api-config.html"
    // ===============================

    @Override
    public Observable<ServerVersion> getServerVersion() {
        return andCacheVersion(SafeObservable.fromNullCallable(() -> {
            ServerVersion version = mService.getServerVersion().blockingFirst();
            if (version != null && version.isDevelopmentVersion()) {
                return version.createDevelopmentVersion();
            }
            return version;
        }));
    }

    @Override
    public Observable<ServerInfo> getServerInfo() {
        return withVersionRequestCheck(mService.getServerInfo());
    }

    @Override
    public Observable<ConsistencyCheckInfo> checkConsistency(@NonNull ConsistencyCheckInput input) {
        return withVersionRequestCheck(mService.checkConsistency(input));
    }

    @Override
    public Observable<ConfigUpdateInfo> reloadServerConfig() {
        return withVersionRequestCheck(mService.reloadServerConfig());
    }

    @Override
    public Observable<Void> confirmEmail(@NonNull EmailConfirmationInput input) {
        return withVersionRequestCheck(
                withEmptyObservable(
                        mService.confirmEmail(input)));
    }

    @Override
    public Observable<Map<String, CacheInfo>> getServerCaches() {
        return withVersionRequestCheck(mService.getServerCaches());
    }

    @Override
    public Observable<Void> executeServerCachesOperations(CacheOperationInput input) {
        return withVersionRequestCheck(
                withEmptyObservable(
                        mService.executeServerCachesOperations(input)));
    }

    @Override
    public Observable<CacheInfo> getServerCache(@NonNull String cacheId) {
        return withVersionRequestCheck(mService.getServerCache(cacheId));
    }

    @Override
    public Observable<Void> flushServerCache(@NonNull String cacheId) {
        return withVersionRequestCheck(
                withEmptyObservable(
                        mService.flushServerCache(cacheId)));
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
        return withVersionRequestCheck(
                withEmptyObservable(
                        mService.deleteServerTask(taskId)));
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

    @Override
    public Observable<EditPreferencesInfo> getServerDefaultEditPreferences() {
        return withVersionRequestCheck(mService.getServerDefaultEditPreferences());
    }

    @Override
    public Observable<EditPreferencesInfo> setServerDefaultEditPreferences(
            @NonNull EditPreferencesInput input) {
        return withVersionRequestCheck(mService.setServerDefaultEditPreferences(input));
    }

    @Override
    public Observable<ResponseBody> indexChanges(
            @NonNull IndexChangesInput input) {
        return withVersionRequestCheck(mService.indexChanges(input));
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
            @Nullable Integer count, @Nullable Integer start,
            @Nullable String project, @Nullable String user, @Nullable Option owned,
            @Nullable Option visibleToAll, @Nullable Option verbose,
            @Nullable List<GroupOptions> options, @Nullable String suggest,
            @Nullable String regexp, @Nullable String match) {
        return withVersionRequestCheck(SafeObservable.fromNullCallable(
                () -> mService.getGroups(count, start, project, user, owned,
                        visibleToAll, verbose, filterByVersion(options),suggest,
                        resolve(regexp, 2.15d), resolve(match, 2.15d))
                        .blockingFirst()));
    }

    @Override
    public Observable<List<GroupInfo>> getGroups(
            @NonNull GroupQuery query, @Nullable Integer count, @Nullable Integer start,
            @Nullable String ownedBy, @Nullable List<GroupOptions> options) {
        return withVersionRequestCheck(SafeObservable.fromNullCallable(
                () -> mService.getGroups(
                            query, count, start, resolve(ownedBy, 2.16d), filterByVersion(options))
                        .blockingFirst()));
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
        return withVersionRequestCheck(
                withEmptyObservable(
                        mService.deleteGroupDescription(groupId)));
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
    public Observable<Void> indexGroup(@NonNull String groupId) {
        return withVersionRequestCheck(withEmptyObservable(mService.indexGroup(groupId)));
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
        return withVersionRequestCheck(
                withEmptyObservable(
                        mService.deleteGroupMember(groupId, accountId)));
    }

    @Override
    public Observable<Void> deleteGroupMembers(
            @NonNull String groupId, @NonNull MemberInput input) {
        return withVersionRequestCheck(
                withEmptyObservable(
                        mService.deleteGroupMembers(groupId, input)));
    }

    @Override
    public Observable<List<GroupInfo>> getGroupSubgroups(@NonNull String groupId) {
        return withVersionRequestCheck(mService.getGroupSubgroups(groupId));
    }

    @Override
    public Observable<GroupInfo> getGroupSubgroup(
            @NonNull String groupId, @NonNull String subgroupId) {
        return withVersionRequestCheck(mService.getGroupSubgroup(groupId, subgroupId));
    }

    @Override
    public Observable<GroupInfo> addGroupSubgroup(
            @NonNull String groupId, @NonNull String subgroupId) {
        return withVersionRequestCheck(mService.addGroupSubgroup(groupId, subgroupId));
    }

    @Override
    public Observable<GroupInfo> addGroupSubgroups(
            @NonNull String groupId, @NonNull SubgroupInput input) {
        return withVersionRequestCheck(mService.addGroupSubgroups(groupId, input));
    }

    @Override
    public Observable<Void> deleteGroupSubgroup(
            @NonNull String groupId, @NonNull String subgroupId) {
        return withVersionRequestCheck(
                withEmptyObservable(
                        mService.deleteGroupSubgroup(groupId, subgroupId)));
    }

    @Override
    public Observable<Void> deleteGroupSubgroups(
            @NonNull String groupId, @NonNull SubgroupInput input) {
        return withVersionRequestCheck(
                withEmptyObservable(
                        mService.deleteGroupSubgroups(groupId, input)));
    }



    // ===============================
    // Gerrit plugins endpoints
    // @link "https://gerrit-review.googlesource.com/Documentation/rest-api-plugins.html"
    // ===============================

    @Override
    public Observable<Map<String, PluginInfo>> getPlugins(
            @Nullable Option all, @Nullable Integer count, @Nullable Integer skip,
            @Nullable String prefix, @Nullable String regexp, @Nullable String match) {
        return withVersionRequestCheck(mService.getPlugins(
                all,
                resolve(count, 2.15d),
                resolve(skip, 2.15d),
                resolve(prefix, 2.15d),
                resolve(regexp, 2.15d),
                resolve(match, 2.15d)));
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
    public Observable<Map<String, ProjectInfo>> getProjects(@Nullable Integer count,
        @Nullable Integer start, @Nullable String prefix, @Nullable String regexp,
        @Nullable String match, @Nullable Option showDescription, @Nullable Option showTree,
        @Nullable String branch, @Nullable ProjectType type, @Nullable String group,
        @Nullable ProjectStatus state) {
        return withVersionRequestCheck(
                mService.getProjects(count, start, prefix, regexp, match,
                        showDescription, showTree, branch, type, group, state));
    }

    @Override
    public Observable<List<ProjectInfo>> queryProjects(
        @NonNull ProjectQuery query, @Nullable Integer count, @Nullable Integer start,
        @Nullable String prefix, @Nullable String regexp, @Nullable String match,
        @Nullable Option showDescription, @Nullable Option showTree,
        @Nullable String branch, @Nullable ProjectType type, @Nullable String group,
        @Nullable ProjectStatus state) {
        return withVersionRequestCheck(
                mService.queryProjects(query, count, start, prefix, regexp, match,
                        showDescription, showTree, branch, type, group, state));
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
        return withVersionRequestCheck(
                withEmptyObservable(
                        mService.deleteProjectDescription(projectName)));
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
    public Observable<ChangeInfo> createProjectAccessRightsChange(
            @NonNull String projectName, @NonNull ProjectAccessInput input) {
        return withVersionRequestCheck(mService.createProjectAccessRightsChange(projectName, input));
    }

    @Override
    @SuppressWarnings("deprecation")
    public Observable<AccessCheckInfo> checkProjectAccessRights(
            @NonNull String projectName, @NonNull AccessCheckInput input) {
        return withVersionRequestCheck(
                mServerVersion.getVersion() >= 3.0d
                    ? mService.getCheckProjectAccessRights(projectName, input.account, input.ref)
                    : mService.postCheckProjectAccessRights(projectName, input));
    }

    @Override
    public Observable<Void> indexProject(
            @NonNull String projectName, @NonNull IndexProjectInput input) {
        return withVersionRequestCheck(
                withEmptyObservable(mService.indexProject(projectName, input)));
    }

    @Override
    public Observable<Void> indexProjectChanges(@NonNull String projectName) {
        return withVersionRequestCheck(
                withEmptyObservable(mService.indexProjectChanges(projectName)));
    }

    @Override
    public Observable<CheckProjectResultInfo> checkProjectConsistency(
            @NonNull String projectName, @NonNull CheckProjectInput input) {
        return withVersionRequestCheck(mService.checkProjectConsistency(projectName, input));
    }

    @Override
    public Observable<List<BranchInfo>> getProjectBranches(@NonNull String projectName,
            @Nullable Integer count, @Nullable Integer start, @Nullable String match,
            @Nullable String regexp) {
        return withVersionRequestCheck(SafeObservable.fromNullCallable(() -> {
            Integer[] s = resolveStartFor21413(start);
            return mService.getProjectBranches(
                    projectName, count, s[0], s[1], match, regexp).blockingFirst();
        }));
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
        return withVersionRequestCheck(
                withEmptyObservable(
                        mService.deleteProjectBranch(projectName, branchId)));
    }

    @Override
    public Observable<Void> deleteProjectBranches(
            @NonNull String projectName, @NonNull DeleteBranchesInput input) {
        return withVersionRequestCheck(
                withEmptyObservable(
                        mService.deleteProjectBranches(projectName, input)));
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
    public Observable<List<TagInfo>> getProjectTags(@NonNull String projectName,
            @Nullable Integer count, @Nullable Integer start, @Nullable String match,
            @Nullable String regexp) {
        return withVersionRequestCheck(SafeObservable.fromNullCallable(() -> {
            Integer[] s = resolveStartFor21413(start);
            return mService.getProjectTags(
                    projectName, count, s[0], s[1], match, regexp).blockingFirst();
        }));
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
    public Observable<Void> deleteProjectTag(@NonNull String projectName, @NonNull String tagId) {
        return withVersionRequestCheck(
                withEmptyObservable(mService.deleteProjectTag(projectName, tagId)));
    }

    @Override
    public Observable<Void> deleteProjectTags(
            @NonNull String projectName, @NonNull DeleteTagsInput input) {
        return withVersionRequestCheck(
                withEmptyObservable(mService.deleteProjectTags(projectName, input)));
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
    public Observable<CherryPickChangeInfo> cherryPickProjectCommit(
            @NonNull String projectName, @NonNull String commitId, @NonNull CherryPickInput input) {
        return withVersionRequestCheck(
                mService.cherryPickProjectCommit(projectName, commitId, input));
    }

    @Override
    public Observable<Map<String, FileInfo>> listProjectCommitFiles(
            @NonNull String projectName, @NonNull String commitId) {
        return withVersionRequestCheck(
                mService.listProjectCommitFiles(projectName, commitId));
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
    public Observable<DashboardInfo> createOrUpdateProjectDashboard(@NonNull String projectName,
            @NonNull String dashboardId, @NonNull DashboardInput input) {
        return withVersionRequestCheck(
                mService.createOrUpdateProjectDashboard(projectName, dashboardId, input));
    }

    @Override
    public Observable<Void> deleteProjectDashboard(
            @NonNull String projectName, @NonNull String dashboardId) {
        return withVersionRequestCheck(
                withEmptyObservable(
                        mService.deleteProjectDashboard(projectName, dashboardId)));
    }


    // ===============================
    // Gerrit documentation endpoints
    // @link "https://gerrit-review.googlesource.com/Documentation/rest-api-documentation.html"
    // ===============================

    @Override
    public Observable<List<DocResult>> findDocumentation(@NonNull String keyword) {
        return withVersionRequestCheck(mService.findDocumentation(keyword));
    }


    // ===============================
    // Other endpoints
    // ===============================

    // -- Cloud Notifications Plugin --
    // https://github.com/jruesga/gerrit-cloud-notifications-plugin

    @Override
    public Observable<CloudNotificationsConfigInfo> getCloudNotificationsConfig() {
        return withVersionRequestCheck(mService.getCloudNotificationsConfig());
    }

    @Override
    public Observable<List<CloudNotificationInfo>> listCloudNotifications(
            @NonNull String accountId, @NonNull String deviceId) {
        return withVersionRequestCheck(mService.listCloudNotifications(accountId, deviceId));
    }

    @Override
    public Observable<CloudNotificationInfo> getCloudNotification(
            @NonNull String accountId, @NonNull String deviceId, @NonNull String token) {
        return withVersionRequestCheck(mService.getCloudNotification(accountId, deviceId, token));
    }

    @Override
    public Observable<CloudNotificationInfo> registerCloudNotification(
            @NonNull String accountId, @NonNull String deviceId,
            @NonNull CloudNotificationInput input) {
        return withVersionRequestCheck(mService.registerCloudNotification(
                accountId, deviceId, input));
    }

    @Override
    public Observable<Void> unregisterCloudNotification(
            @NonNull String accountId, @NonNull String deviceId, @NonNull String token) {
        return withVersionRequestCheck(
                withEmptyObservable(
                    mService.unregisterCloudNotification(accountId, deviceId, token)));
    }
}
