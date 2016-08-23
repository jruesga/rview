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

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

public class GerritServiceFactory {

    private static class AndroidPlatformAbstractionLayer implements PlatformAbstractionLayer {
        private static final String TAG = "Gerrit";
        private Context mApplicationContext;

        public AndroidPlatformAbstractionLayer(Context applicationContext) {
            mApplicationContext = applicationContext;
        }

        @Override
        public boolean isDebugBuild() {
            return BuildConfig.DEBUG;
        }

        @Override
        public void log(String message) {
            Log.i(TAG ,message);
        }

        @Override
        public byte[] encodeBase64(byte[] data) {
            return Base64.encode(data, Base64.NO_WRAP);
        }

        @Override
        public byte[] decodeBase64(byte[] data) {
            return Base64.decode(data, Base64.NO_WRAP);
        }

        @Override
        public boolean hasConnectivity() {
            ConnectivityManager cm = (ConnectivityManager) mApplicationContext.getSystemService(
                    Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        }
    }

    private final static Map<String, GerritApiClient> sInstances = new HashMap<>();
    private static AndroidPlatformAbstractionLayer sAbstractionLayer;

    public static GerritApiClient getInstance(Context applicationContext, String endpoint) {
        return getInstance(applicationContext, endpoint, null);
    }

    public static GerritApiClient getInstance(Context applicationContext,
                String endpoint, Authorization authorization) {
        if (sAbstractionLayer == null) {
            sAbstractionLayer = new AndroidPlatformAbstractionLayer(applicationContext);
        }

        // Ensure we have a correct endpoint to invoke gerrit
        endpoint = sanitizeEndpoint(endpoint);

        // Change to authentication endpoint if needed
        if (authorization != null && !authorization.isAnonymousUser()) {
            endpoint += "a/";
        }

        // Create a hash from the endpoint + authorization
        String credentials = "";
        if (authorization != null && authorization.isAnonymousUser()) {
            credentials += authorization.mUsername + ":"
                    + (TextUtils.isEmpty(authorization.mPassword) ? "" : authorization.mPassword);
        }
        final String endpointHash = Base64.encodeToString(
                (endpoint + ":" + credentials).getBytes(), Base64.NO_WRAP);

        // Have a cached instance?
        if (!sInstances.containsKey(endpointHash)) {
            sInstances.put(endpointHash,
                    new GerritApiClient(endpoint, authorization, sAbstractionLayer));
        }
        return sInstances.get(endpointHash);
    }

    private static String sanitizeEndpoint(String endpoint) {
        // Sanitize endpoint
        if (!endpoint.toLowerCase().startsWith("http://")
                && !endpoint.toLowerCase().startsWith("https://")) {
            endpoint = "http://" + endpoint;
        }
        if (!endpoint.endsWith("/")) {
            endpoint += "/";
        }
        return endpoint;
    }
}
