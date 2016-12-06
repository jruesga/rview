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
package com.ruesga.rview.services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.ruesga.rview.gerrit.GerritApi;
import com.ruesga.rview.gerrit.model.CloudNotificationInfo;
import com.ruesga.rview.gerrit.model.CloudNotificationInput;
import com.ruesga.rview.gerrit.model.CloudNotificationResponseMode;
import com.ruesga.rview.misc.ModelHelper;
import com.ruesga.rview.model.Account;
import com.ruesga.rview.preferences.Preferences;
import com.ruesga.rview.providers.NotificationEntity;

import java.util.List;
import java.util.concurrent.Callable;

import me.tatarka.rxloader2.safe.SafeObservable;

public class DeviceRegistrationService extends IntentService {

    private static final String TAG = "DeviceRegisterService";

    public static final String REGISTER_DEVICE_ACTION = "com.ruesga.rview.actions.REGISTER_DEVICE";
    public static final String EXTRA_ACCOUNT = "account";

    public DeviceRegistrationService() {
        super(TAG);
    }

    @Override
    @SuppressWarnings("Convert2streamapi")
    protected void onHandleIntent(Intent intent) {
        if (intent.getAction().equals(REGISTER_DEVICE_ACTION)) {
            String account = intent.getStringExtra(EXTRA_ACCOUNT);
            List<Account> accounts = Preferences.getAccounts(this);
            for (Account acct : accounts) {
                // Only those accounts that can and wants notifications
                if (acct.mSupportNotifications &&
                        acct.hasAuthenticatedAccessMode() &&
                        account == null || acct.getAccountHash().equals(account)) {
                    // Perform registration in background
                    Thread t = new Thread(() -> {
                        // Register/Unregister device
                        final Context ctx = DeviceRegistrationService.this.getApplicationContext();
                        if (Preferences.isAccountNotificationsEnabled(ctx, acct)) {
                            performDeviceRegistration(ctx, acct);
                        } else {
                            performDeviceUnregistration(ctx, acct);
                        }
                    });
                    t.start();
                }
            }
        }
    }

    private void performDeviceRegistration(Context ctx, Account account) {
        String deviceId = FirebaseInstanceId.getInstance().getToken();
        if (deviceId == null) {
            return;
        }

        CloudNotificationInput input = new CloudNotificationInput();
        input.token = account.getAccountHash();
        input.events = Preferences.getAccountNotificationsEvents(ctx, account);
        input.responseMode = CloudNotificationResponseMode.DATA;

        GerritApi api = ModelHelper.getGerritApi(ctx, account);
        try {
            SafeObservable.fromCallable(() ->
                    api.registerCloudNotification(GerritApi.SELF_ACCOUNT, deviceId, input)
                            .blockingFirst()
                    ).blockingFirst();

        } catch (Exception ex) {
            Log.e(TAG, "Failed to register device: " + deviceId + "/" + input.token, ex);
        }
    }

    private void performDeviceUnregistration(Context ctx, Account account) {
        String deviceId = FirebaseInstanceId.getInstance().getToken();
        if (deviceId == null) {
            return;
        }

        final String accountToken = account.getAccountHash();
        GerritApi api = ModelHelper.getGerritApi(ctx, account);
        try {
            SafeObservable.fromCallable(() ->
                    api.unregisterCloudNotification(GerritApi.SELF_ACCOUNT, deviceId, accountToken)
                            .blockingFirst()
                    ).blockingFirst();
        } catch (Exception ex) {
            Log.e(TAG, "Failed to unregister device: " + deviceId + "/" + accountToken, ex);
        }
    }
}
