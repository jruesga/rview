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
package com.ruesga.rview.attachments.gdrive.oauth;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.Scope;
import com.ruesga.rview.attachments.AuthenticationException;
import com.ruesga.rview.attachments.AuthenticationInfo;
import com.ruesga.rview.attachments.Provider;
import com.ruesga.rview.attachments.R;
import com.ruesga.rview.attachments.gdrive.GDriveRestApi;
import com.ruesga.rview.attachments.gdrive.GDriveRestApiClient;
import com.ruesga.rview.attachments.preferences.Constants;
import com.ruesga.rview.attachments.preferences.Preferences;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import me.tatarka.rxloader2.RxLoader;
import me.tatarka.rxloader2.RxLoaderManager;
import me.tatarka.rxloader2.RxLoaderManagerCompat;
import me.tatarka.rxloader2.RxLoaderObserver;
import me.tatarka.rxloader2.safe.SafeObservable;

@SuppressWarnings("deprecation")
public class OAuthProxyFragment extends Fragment implements OnConnectionFailedListener {

    public static final String TAG = "OAuthProxyFragment";

    private static final int REQUEST_CODE_OAUTH = 101;

    private final RxLoaderObserver<AuthenticationInfo> mOAuthFlowObserver
            = new RxLoaderObserver<AuthenticationInfo>() {
        @Override
        public void onNext(AuthenticationInfo auth) {
            mOAuthFlowLoader.clear();
            if (mDialog != null) {
                mDialog.dismiss();
            }
            mDialog = null;

            Preferences.setAuthenticationInfo(getContext(), Provider.GDRIVE, auth);
            Intent filter = new Intent(Constants.ATTACHMENT_PROVIDER_CHANGED_ACTION);
            LocalBroadcastManager.getInstance(getContext()).sendBroadcast(filter);
        }

        @Override
        public void onError(Throwable error) {
            mOAuthFlowLoader.clear();
            if (mDialog != null) {
                mDialog.dismiss();
            }
            mDialog = null;

            handleError();
        }

        @Override
        public void onStarted() {
            if (mDialog != null) {
                mDialog.dismiss();
            }
            mDialog = new ProgressDialog(getActivity());
            mDialog.setMessage(getString(R.string.attachment_negotiate_keys));
            mDialog.setCancelable(false);
            mDialog.show();
        }
    };


    private ProgressDialog mDialog;
    private RxLoader<AuthenticationInfo> mOAuthFlowLoader;
    private GoogleApiClient mGoogleApiClient;


    public static OAuthProxyFragment newInstance() {
        return new OAuthProxyFragment();
    }

    public OAuthProxyFragment() {
    }

    public void show(FragmentManager fragmentManager) {
        FragmentTransaction ft = fragmentManager.beginTransaction();
        ft.add(this, TAG);
        ft.commit();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = new View(getContext());
        v.setBackgroundColor(Color.RED);
        if (container != null) {
            container.addView(v);
        }
        return v;
    }

    @Override
    public final void onDestroyView() {
        super.onDestroyView();
        if (mDialog != null) {
            mDialog.dismiss();
        }
        mDialog = null;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        createLoadersWithValidContext();

        final String clientId = getContext().getString(R.string.gdrive_client_id);
        GoogleSignInOptions gso = new Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestServerAuthCode(clientId, true)
                .requestScopes(new Scope(Scopes.DRIVE_FILE))
                .build();

        AuthenticationInfo auth = Preferences.getAuthenticationInfo(getContext(), Provider.GDRIVE);
        if (auth == null || TextUtils.isEmpty(auth.serverAuthCode)) {
            // Request the user to login into its Google account and request permissions
            mGoogleApiClient = new GoogleApiClient.Builder(getContext())
                    .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                        @Override
                        public void onConnected(@Nullable Bundle bundle) {
                            Auth.GoogleSignInApi.signOut(mGoogleApiClient);

                            Intent signInIntent =
                                    Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
                            startActivityForResult(signInIntent, REQUEST_CODE_OAUTH);
                        }

                        @Override
                        public void onConnectionSuspended(int cause) {
                        }
                    })
                    .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                    .build();
            mGoogleApiClient.connect();
            return;
        }

        // Has a valid server token. Just retrieve an accessToken
        requestAccessToken();
    }

    private void createLoadersWithValidContext() {
        RxLoaderManager loaderManager = RxLoaderManagerCompat.get(this);
        mOAuthFlowLoader = loaderManager.create(
                "auth_flow", performOAuthFlow(), mOAuthFlowObserver);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_OAUTH) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                // Extract the server auth code and user email
                GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
                if (result.isSuccess() && result.getSignInAccount() != null) {
                    AuthenticationInfo auth = new AuthenticationInfo();
                    auth.serverAuthCode = result.getSignInAccount().getServerAuthCode();
                    auth.accountEmail = result.getSignInAccount().getEmail();
                    Preferences.setAuthenticationInfo(getContext(), Provider.GDRIVE, auth);

                    requestAccessToken();
                    return;
                }
            }

            handleError();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        handleError();
    }

    private void handleError() {
        Toast.makeText(getContext(), R.string.attachment_failed_to_login, Toast.LENGTH_SHORT).show();
        if (mDialog != null) {
            mDialog.dismiss();
        }
        mDialog = null;
    }

    private void requestAccessToken() {
        mOAuthFlowLoader.clear();
        mOAuthFlowLoader.restart();
    }

    private Observable<AuthenticationInfo> performOAuthFlow() {
        final GDriveRestApiClient client = GDriveRestApiClient.newClientInstance(getContext());
        return SafeObservable.fromNullCallable(() -> {
                    final AuthenticationInfo auth = Preferences.getAuthenticationInfo(
                            getContext(), Provider.GDRIVE);
                    if (auth == null) {
                        throw new AuthenticationException();
                    }
                    final String clientId = getContext().getString(R.string.gdrive_client_id);
                    final String secret = getContext().getString(R.string.gdrive_client_secret);
                    AccessToken token = client.requestAccessToken(
                            auth.serverAuthCode, clientId, secret, GDriveRestApi.OAUTH_REDIRECT_URL,
                                    GDriveRestApi.OAUTH_GRANT_TYPE_AUTH_TOKEN)
                                            .blockingFirst();
                    auth.accessToken = token.accessToken;
                    auth.refreshToken = token.refreshToken;
                    auth.expiresIn = System.currentTimeMillis() + (token.expiresIn * 1000L);
                    auth.tokenType = token.tokenType;
                    Log.i(TAG, "Got GDrive access token. Will expired in " + auth.expiresIn);
                    return auth;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }
}
