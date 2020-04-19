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
import android.os.StrictMode;

import com.ruesga.rview.analytics.AnalyticsFlavor;
import com.ruesga.rview.attachments.AttachmentsProviderFactory;
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

        // Setup analytics based on product flavor
        AnalyticsFlavor.setup(this);

        // Initialize application resources
        Formatter.refreshCachedPreferences(getApplicationContext());
        AttachmentsProviderFactory.initialize(getApplicationContext());

        // Recreate notifications
        NotificationsHelper.createNotificationChannels(getApplicationContext());
        NotificationEntity.truncateNotifications(getApplicationContext());
        NotificationsHelper.recreateNotifications(getApplicationContext());

        // Register devices for push notifications
        DeviceRegistrationService.register(this, null);

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
