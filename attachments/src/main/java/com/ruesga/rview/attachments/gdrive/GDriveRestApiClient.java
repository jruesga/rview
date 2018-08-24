/*
 * Copyright (C) 2017 Jorge Ruesga
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
package com.ruesga.rview.attachments.gdrive;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.GsonBuilder;
import com.ruesga.rview.attachments.AuthenticationException;
import com.ruesga.rview.attachments.AuthenticationInfo;
import com.ruesga.rview.attachments.NoConnectivityException;
import com.ruesga.rview.attachments.Provider;
import com.ruesga.rview.attachments.R;
import com.ruesga.rview.attachments.gdrive.model.FileMetadata;
import com.ruesga.rview.attachments.gdrive.model.FileMetadataInput;
import com.ruesga.rview.attachments.gdrive.model.FileMetadataPageInfo;
import com.ruesga.rview.attachments.gdrive.model.PermissionMetadata;
import com.ruesga.rview.attachments.gdrive.model.PermissionMetadataInput;
import com.ruesga.rview.attachments.gdrive.model.PermissionMetadataPageInfo;
import com.ruesga.rview.attachments.gdrive.oauth.AccessToken;
import com.ruesga.rview.attachments.misc.ExceptionHelper;
import com.ruesga.rview.attachments.misc.OkHttpHelper;
import com.ruesga.rview.attachments.preferences.Config;
import com.ruesga.rview.attachments.preferences.Preferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.reactivex.Observable;
import me.tatarka.rxloader2.safe.SafeObservable;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class GDriveRestApiClient implements GDriveRestApi {

    private static final String TAG = "GDriveRestApiClient";

    private static final String ENTRY_POINT = "https://www.googleapis.com/";

    @SuppressLint("StaticFieldLeak")
    private static GDriveRestApiClient sInstance;
    private GDriveRestApi mApi;
    private Context mContext;

    public static synchronized GDriveRestApiClient newClientInstance(Context context) {
        if (sInstance == null) {
            sInstance = new GDriveRestApiClient(context);
        }
        return sInstance;
    }

    private GDriveRestApiClient(Context context) {
        mContext = context.getApplicationContext();

        // OkHttp client
        OkHttpClient.Builder clientBuilder = OkHttpHelper.getSafeClientBuilder();
        clientBuilder
                .readTimeout(20000, java.util.concurrent.TimeUnit.MILLISECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .addInterceptor(createConnectivityCheckInterceptor())
                .addInterceptor(createLoggingInterceptor())
                .addInterceptor(createHeadersInterceptor());
        OkHttpClient client = clientBuilder.build();

        // Gson adapter
        GsonConverterFactory gsonFactory =
                GsonConverterFactory.create(new GsonBuilder().create());

        // RxJava adapter
        RxJava2CallAdapterFactory rxAdapter = RxJava2CallAdapterFactory.create();

        // Retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(ENTRY_POINT)
                .client(client)
                .addConverterFactory(gsonFactory)
                .addCallAdapterFactory(rxAdapter)
                .build();

        // Build the api
        mApi = retrofit.create(GDriveRestApi.class);
    }

    private HttpLoggingInterceptor createLoggingInterceptor() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor(s -> Log.i(TAG, s));
        logging.setLevel(Config.isApkDebugSigned(mContext) ?
                HttpLoggingInterceptor.Level.BODY : HttpLoggingInterceptor.Level.BASIC);
        return logging;
    }

    private Interceptor createHeadersInterceptor() {
        return chain -> {
            Request original = chain.request();
            Request.Builder requestBuilder = original.newBuilder();
            boolean isOAuthEntryPoint = original.url().encodedPath().equals("/oauth2/v4/token");
            if (!isOAuthEntryPoint) {
                final AuthenticationInfo auth = Preferences.getAuthenticationInfo(
                        mContext, Provider.GDRIVE);
                if (auth != null && !TextUtils.isEmpty(auth.accessToken)
                        && !TextUtils.isEmpty(auth.tokenType)) {
                    requestBuilder.header("Authorization", auth.tokenType + " " + auth.accessToken);
                }
            }
            Request request = requestBuilder.build();
            return chain.proceed(request);
        };
    }

    private Interceptor createConnectivityCheckInterceptor() {
        return chain -> {
            if (!hasConnectivity()) {
                throw new NoConnectivityException();
            }
            return chain.proceed(chain.request());
        };
    }

    @SuppressLint("Deprecated")
    private boolean hasConnectivity() {
        ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(
                Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        }
        return false;
    }

    private <T> Observable<T> withAccessTokenCheck(final Observable<T> observable) {
        return SafeObservable.fromNullCallable(() -> {
            try {
                return observable.blockingFirst();
            } catch (Throwable cause) {
                if (ExceptionHelper.isAuthenticationException(cause)) {
                    Log.w(TAG, "Got authentication exception for GDrive access token."
                            + " Disable account", cause);
                    Preferences.setAuthenticationInfo(mContext, Provider.GDRIVE, null);
                    Preferences.setProvider(mContext, Provider.NONE);
                    throw new AuthenticationException(Provider.GDRIVE.name());
                }
                throw cause;
            }
        });
    }

    private <T> Observable<T> withTokenRefreshCheck(final Observable<T> observable) {
        return SafeObservable.fromNullCallable(() -> {
            final AuthenticationInfo auth = Preferences.getAuthenticationInfo(
                    mContext, Provider.GDRIVE);
            if (auth != null && auth.expiresIn > 0
                    && auth.expiresIn < System.currentTimeMillis()) {
                // Token expired. Refresh it
                try {
                    final String clientId = mContext.getString(R.string.gdrive_client_id);
                    final String secret = mContext.getString(R.string.gdrive_client_secret);
                    AccessToken token = refreshAccessToken(auth.refreshToken, clientId, secret,
                            GDriveRestApi.OAUTH_GRANT_TYPE_REFRESH_TOKEN)
                                    .blockingFirst();
                    auth.accessToken = token.accessToken;
                    auth.tokenType = token.tokenType;
                    auth.expiresIn = System.currentTimeMillis() + (token.expiresIn * 1000L);
                    Preferences.setAuthenticationInfo(mContext, Provider.GDRIVE, auth);
                    Log.i(TAG, "GDrive access token refreshed. Will expired in " + auth.expiresIn);

                } catch (Throwable cause) {
                    if (ExceptionHelper.isAuthenticationException(cause)) {
                        Log.w(TAG, "Got authentication exception refreshing GDrive access token."
                                + " Disable account", cause);
                        Preferences.setAuthenticationInfo(mContext, Provider.GDRIVE, null);
                        Preferences.setProvider(mContext, Provider.NONE);
                        throw new AuthenticationException(Provider.GDRIVE.name());
                    }

                    Log.w(TAG, "Can't refresh GDrive access token", cause);
                }
            }

            return observable.blockingFirst();
        });
    }



    @Override
    public Observable<AccessToken> requestAccessToken(
            @NonNull String code, @NonNull String clientId, @NonNull String clientSecret,
            @NonNull String redirectUri, @NonNull String grantType) {
        return withAccessTokenCheck(
                mApi.requestAccessToken(code, clientId, clientSecret, redirectUri, grantType));
    }

    @Override
    public Observable<AccessToken> refreshAccessToken(
            @NonNull String refreshToken, @NonNull String clientId, @NonNull String clientSecret,
            @NonNull String grantType) {
        return withAccessTokenCheck(
                mApi.refreshAccessToken(refreshToken, clientId, clientSecret, grantType));
    }

    @Override
    public Observable<AccessToken> requestTokenInfo(@NonNull String accessToken) {
        return withAccessTokenCheck(
                mApi.requestTokenInfo(accessToken));
    }

    @Override
    public Observable<FileMetadataPageInfo> listFiles(
            @Nullable Integer pageSize, @Nullable String pageToken, @Nullable String query) {
        return withAccessTokenCheck(
                withTokenRefreshCheck(
                        mApi.listFiles(pageSize, pageToken, query)));
    }

    @Override
    public Observable<FileMetadata> createFileMetadata(@NonNull FileMetadataInput input) {
        return withAccessTokenCheck(
                withTokenRefreshCheck(
                        mApi.createFileMetadata(input)));
    }

    @Override
    public Observable<FileMetadata> uploadFileContent(
            @NonNull  String fileId, @NonNull RequestBody input) {
        return withAccessTokenCheck(
                withTokenRefreshCheck(
                        mApi.uploadFileContent(fileId, input)));
    }

    @Override
    public Observable<PermissionMetadataPageInfo> listFilePermissions(
            @NonNull String fileId, @Nullable Integer pageSize, @Nullable String pageToken) {
        return withAccessTokenCheck(
                withTokenRefreshCheck(
                        mApi.listFilePermissions(fileId, pageSize, pageToken)));
    }

    @Override
    public Observable<PermissionMetadata> createFilePermission(
            @NonNull String fileId, @NonNull PermissionMetadataInput input) {
        return withAccessTokenCheck(
                withTokenRefreshCheck(
                        mApi.createFilePermission(fileId, input)));
    }
}
