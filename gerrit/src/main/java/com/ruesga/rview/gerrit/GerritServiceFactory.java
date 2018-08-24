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

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.security.auth.x500.X500Principal;

import androidx.annotation.NonNull;

public class GerritServiceFactory {

    private static class AndroidPlatformAbstractionLayer implements PlatformAbstractionLayer {
        private static final String TAG = "Gerrit";

        private static final X500Principal DEBUG_DN =
                new X500Principal("CN=Android Debug,O=Android,C=US");

        private Context mApplicationContext;
        private final boolean mDebuggable;

        AndroidPlatformAbstractionLayer(Context applicationContext) {
            mApplicationContext = applicationContext;
            mDebuggable = isApkDebugSigned(mApplicationContext);
        }

        @TargetApi(Build.VERSION_CODES.P)
        @SuppressLint({"PackageManagerGetSignatures", "Deprecated"})
        private boolean isApkDebugSigned(Context ctx) {
            try {
                final Signature[] signatures;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    PackageInfo packageInfo = ctx.getPackageManager().getPackageInfo(
                            ctx.getPackageName(), PackageManager.GET_SIGNING_CERTIFICATES);
                    signatures = packageInfo.signingInfo.getApkContentsSigners();
                } else {
                    PackageInfo packageInfo = ctx.getPackageManager().getPackageInfo(
                            ctx.getPackageName(), PackageManager.GET_SIGNATURES);
                    signatures = packageInfo.signatures;
                }
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                for (Signature signature : signatures) {
                    ByteArrayInputStream stream =
                            new ByteArrayInputStream(signature.toByteArray());
                    X509Certificate cert = (X509Certificate) cf.generateCertificate(stream);
                    boolean debuggable = cert.getSubjectX500Principal().equals(DEBUG_DN);
                    if (debuggable) {
                        return true;
                    }
                }
            } catch (Exception e) {
                // Ignore
            }
            return false;
        }

        @Override
        public boolean isDebugBuild() {
            return mDebuggable;
        }

        @Override
        public void log(String message) {
            Log.i(TAG ,message);
        }

        @Override
        public byte[] encodeBase64(byte[] data) {
            if (data == null) {
                return null;
            }
            return Base64.encode(data, Base64.NO_WRAP);
        }

        @Override
        public byte[] decodeBase64(byte[] data) {
            if (data == null) {
                return null;
            }
            return Base64.decode(data, Base64.NO_WRAP);
        }

        @Override
        @SuppressLint("Deprecated")
        public boolean hasConnectivity() {
            ConnectivityManager cm = (ConnectivityManager) mApplicationContext.getSystemService(
                    Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
            }
            return false;
        }
    }

    private final static Map<String, GerritApiClient> sInstances = new HashMap<>();

    public static GerritApiClient getInstance(@NonNull Context applicationContext,
            @NonNull String endpoint, boolean trustAllServerCertificates) {
        return getInstance(applicationContext, endpoint, new Authorization(
                null, null, trustAllServerCertificates));
    }

    public static GerritApiClient getInstance(@NonNull Context applicationContext,
            @NonNull String endpoint, @NonNull Authorization authorization) {

        // Ensure we have a correct endpoint to invoke gerrit
        endpoint = sanitizeEndpoint(endpoint);

        // Change to authentication endpoint if needed
        if (!authorization.isAnonymousUser()) {
            endpoint += "a/";
        }

        // Create a hash from the endpoint + authorization
        String credentials = "";
        if (!authorization.isAnonymousUser()) {
            credentials += authorization.mUsername + ":"
                    + (TextUtils.isEmpty(authorization.mPassword) ? "" : authorization.mPassword);
        }
        final String endpointHash = Base64.encodeToString(
                (endpoint + ":" + authorization.mTrustAllCertificates
                        + ":" + credentials).getBytes(), Base64.NO_WRAP);

        // Have a cached instance?
        if (!sInstances.containsKey(endpointHash)) {
            sInstances.put(endpointHash,
                    new GerritApiClient(endpoint, authorization,
                            new AndroidPlatformAbstractionLayer(applicationContext)));
        }
        return sInstances.get(endpointHash);
    }

    private static String sanitizeEndpoint(String endpoint) {
        // Sanitize endpoint
        String endpointLower = endpoint.toLowerCase(Locale.US);
        if (!endpointLower.startsWith("http://") && !endpointLower.startsWith("https://")) {
            endpoint = "http://" + endpoint;
        }

        if (!endpoint.endsWith("/")) {
            endpoint += "/";
        }

        return endpoint;
    }
}
