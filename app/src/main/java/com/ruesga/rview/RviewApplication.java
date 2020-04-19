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

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.ruesga.rview.attachments.AttachmentsProviderFactory;
import com.ruesga.rview.misc.AnalyticsHelper;
import com.ruesga.rview.misc.Formatter;
import com.ruesga.rview.misc.ModelHelper;
import com.ruesga.rview.misc.NotificationsHelper;
import com.ruesga.rview.model.Account;
import com.ruesga.rview.preferences.Preferences;
import com.ruesga.rview.providers.NotificationEntity;
import com.ruesga.rview.receivers.CacheCleanerReceiver;
import com.ruesga.rview.services.DeviceRegistrationService;

import java.util.List;

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

        // Configure Firebase (just an empty stub to set empty sender ids). Senders
        // ids will be fetched from the Gerrit server instances.
        try {
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setProjectId(getString(R.string.fcm_project_id))
                    .setApplicationId(getString(R.string.fcm_app_id))
                    .setGcmSenderId(getString(R.string.fcm_sender_id))
                    .setApiKey(getString(R.string.fcm_api_key))
                    .build();
            FirebaseApp.initializeApp(getApplicationContext(), options);
            AnalyticsHelper.appStarted(getApplicationContext());

            // Configure Firebase Crashlytics
            boolean enableCrashlytics = getResources().getBoolean(R.bool.fcm_enable_crashlytics);
            FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();
            crashlytics.setCrashlyticsCollectionEnabled(enableCrashlytics);
            if (!enableCrashlytics) {
                crashlytics.deleteUnsentReports();
            }
        } catch (Throwable ex) {
            // Ignore any firebase exception by miss-configuration
            Log.e(TAG, "Cannot configure Firebase", ex);
        }

        // Initialize application resources
        Formatter.refreshCachedPreferences(getApplicationContext());
        AttachmentsProviderFactory.initialize(getApplicationContext());

        // Recreate notifications
        NotificationsHelper.createNotificationChannels(getApplicationContext());
        NotificationEntity.truncateNotifications(getApplicationContext());
        NotificationsHelper.recreateNotifications(getApplicationContext());

        // Register devices for push notifications
        Intent intent = new Intent();
        intent.setAction(DeviceRegistrationService.REGISTER_DEVICE_ACTION);
        DeviceRegistrationService.enqueueWork(this, intent);

        // Schedule a cache clean
        CacheCleanerReceiver.cleanCache(getApplicationContext(), true);

        // Enable Url Handlers
        enableExternalUrlHandlers();
    }

    @SuppressWarnings("Convert2streamapi")
    private void enableExternalUrlHandlers() {
        List<Account> accounts =  Preferences.getAccounts(getApplicationContext());
        for (Account account : accounts) {
            if (ModelHelper.canAccountHandleUrls(getApplicationContext(), account)) {
                ModelHelper.setAccountUrlHandlingStatus(getApplicationContext(), account,
                        Preferences.isAccountHandleLinks(getApplicationContext(), account));
            }
        }
    }
}
