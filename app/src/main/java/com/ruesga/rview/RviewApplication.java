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
package com.ruesga.rview;

import android.app.Application;
import android.content.Intent;
import android.os.StrictMode;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.core.CrashlyticsCore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.ruesga.rview.misc.Formatter;
import com.ruesga.rview.misc.ModelHelper;
import com.ruesga.rview.misc.NotificationsHelper;
import com.ruesga.rview.model.Account;
import com.ruesga.rview.preferences.Preferences;
import com.ruesga.rview.providers.NotificationEntity;
import com.ruesga.rview.services.DeviceRegistrationService;

import java.util.List;

import io.fabric.sdk.android.Fabric;
import io.fabric.sdk.android.Kit;

public class RviewApplication extends Application {

    private static final String TAG = "RviewApplication";

    @Override
    public void onCreate() {
        super.onCreate();

        // Enable StrictMode  in debug builds
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build());
        }

        // Install a hook to Crashlytics and Answers (only in production releases)
        if (!BuildConfig.DEBUG) {
            try {
                CrashlyticsCore core = new CrashlyticsCore.Builder().build();
                Crashlytics crashlytics = new Crashlytics.Builder().core(core).build();
                Answers answers = new Answers();
                final Fabric fabric = new Fabric.Builder(this)
                        .kits(new Kit[]{crashlytics, answers})
                        .build();
                Fabric.with(fabric);
            } catch (Throwable ex) {
                // Ignore any fabric exception by miss-configuration
                Log.e(TAG, "Cannot configure Fabric", ex);
            }
        }

        // Configure Firebase (just an empty stub to set empty sender ids). Senders
        // ids will be fetched from the Gerrit server instances.
        FirebaseOptions options = new FirebaseOptions.Builder()
                .setApplicationId(getString(R.string.fcm_app_id))
                .setApiKey(getString(R.string.fcm_api_key))
                .setGcmSenderId(null)
                .build();
        FirebaseApp.initializeApp(getApplicationContext(), options);


        // Initialize application resources
        Formatter.refreshCachedPreferences(getApplicationContext());

        // Recreate notifications
        NotificationEntity.truncateNotifications(getApplicationContext());
        NotificationsHelper.recreateNotifications(getApplicationContext());

        // Register devices for push notifications
        Intent intent = new Intent(this, DeviceRegistrationService.class);
        intent.setAction(DeviceRegistrationService.REGISTER_DEVICE_ACTION);
        startService(intent);

        // Enable Url Handlers
        enableExternalUrlHandlers();
    }

    private void enableExternalUrlHandlers() {
        List<Account> accounts =  Preferences.getAccounts(getApplicationContext());
        for (Account account : accounts) {
            if (Preferences.isAccountHandleLinks(getApplicationContext(), account)) {
                ModelHelper.setAccountUrlHandlingStatus(getApplicationContext(), account, true);
            } else {
                ModelHelper.setAccountUrlHandlingStatus(getApplicationContext(), account, false);
            }
        }
    }
}
