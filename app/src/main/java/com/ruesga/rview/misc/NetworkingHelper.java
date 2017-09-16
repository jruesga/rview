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
package com.ruesga.rview.misc;

import android.util.Log;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

public class NetworkingHelper {

    private static final String TAG = "NetworkingHelper";

    public static OkHttpClient createNetworkClient() {
        return new OkHttpClient.Builder()
            .readTimeout(20000, java.util.concurrent.TimeUnit.MILLISECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .addInterceptor(createLoggingInterceptor())
            .build();
    }

    private static HttpLoggingInterceptor createLoggingInterceptor() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor(s -> Log.d(TAG, s));
        logging.setLevel(HttpLoggingInterceptor.Level.BASIC);
        return logging;
    }
}
