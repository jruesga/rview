/*
 * Copyright (C) 2020 Jorge Ruesga
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
package com.ruesga.rview.analytics;

import android.content.Context;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.ruesga.rview.R;

public class AnalyticsFlavor {
    private static final String TAG = "AnalyticsFlavor [GMS]";

    public static void setup(Context context) {
        Log.d(TAG, "GMS flavor. Configure Firebase Analytics and Firebase Crashlytics");

        // Configure Firebase (just an empty stub to set empty sender ids). Senders
        // ids will be fetched from the Gerrit server instances.
        try {
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setProjectId(context.getString(R.string.fcm_project_id))
                    .setApplicationId(context.getString(R.string.fcm_app_id))
                    .setGcmSenderId(context.getString(R.string.fcm_sender_id))
                    .setApiKey(context.getString(R.string.fcm_api_key))
                    .build();
            FirebaseApp.initializeApp(context.getApplicationContext(), options);
            AnalyticsManagerImpl.instance().appStarted(context.getApplicationContext());

            // Configure Firebase Crashlytics
            boolean enableCrashlytics = context.getResources().getBoolean(
                    R.bool.fcm_enable_crashlytics);
            FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();
            crashlytics.setCrashlyticsCollectionEnabled(enableCrashlytics);
            if (!enableCrashlytics) {
                crashlytics.deleteUnsentReports();
            }
        } catch (Throwable ex) {
            // Ignore any firebase exception by miss-configuration
            Log.e(TAG, "Cannot configure Firebase", ex);
        }
    }
}
