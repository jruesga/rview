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

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.app.JobIntentService;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.ruesga.rview.gerrit.GerritApi;
import com.ruesga.rview.gerrit.model.CloudNotificationInput;
import com.ruesga.rview.gerrit.model.CloudNotificationResponseMode;
import com.ruesga.rview.misc.ModelHelper;
import com.ruesga.rview.model.Account;
import com.ruesga.rview.preferences.Preferences;

import java.io.IOException;
import java.util.List;

import me.tatarka.rxloader2.safe.Empty;
import me.tatarka.rxloader2.safe.SafeObservable;

public class DeviceRegistrationService extends JobIntentService {

    private static final String TAG = "DeviceRegisterService";

    public static final String REGISTER_DEVICE_ACTION = "com.ruesga.rview.actions.REGISTER_DEVICE";
    public static final String EXTRA_ACCOUNT = "account";

    private static final String TOKEN_SCOPE = "FCM";

    private static final int JOB_ID = 1001;

    public DeviceRegistrationService() {
        super();
    }

    public static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, DeviceRegistrationService.class, JOB_ID, work);
    }

    @Override
    @SuppressWarnings("Convert2streamapi")
    protected void onHandleWork(@NonNull Intent intent) {
        if (REGISTER_DEVICE_ACTION.equals(intent.getAction())) {
            String account = intent.getStringExtra(EXTRA_ACCOUNT);
            List<Account> accounts = Preferences.getAccounts(this);
            for (Account acct : accounts) {
                // Only those accounts that can and wants notifications
                if (acct.hasNotificationsSupport() &&
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
        String deviceId = getToken(account);
        if (deviceId == null) {
            return;
        }

        CloudNotificationInput input = new CloudNotificationInput();
        input.token = account.getAccountHash();
        input.events = Preferences.getAccountNotificationsEvents(ctx, account);
        input.responseMode = CloudNotificationResponseMode.DATA;

        GerritApi api = ModelHelper.getGerritApi(ctx, account);
        try {
            SafeObservable.fromNullCallable(() -> {
                        api.registerCloudNotification(GerritApi.SELF_ACCOUNT, deviceId, input)
                                .blockingFirst();
                        return Empty.NULL;
                    }).blockingFirst();

        } catch (Exception ex) {
            Log.e(TAG, "Failed to register device: " + deviceId + "/" + input.token, ex);
        }
    }

    private void performDeviceUnregistration(Context ctx, Account account) {
        String deviceId = getToken(account);
        if (deviceId == null) {
            return;
        }

        final String accountToken = account.getAccountHash();
        GerritApi api = ModelHelper.getGerritApi(ctx, account);
        try {
            SafeObservable.fromNullCallable(() -> {
                        api.unregisterCloudNotification(
                                GerritApi.SELF_ACCOUNT, deviceId, accountToken).blockingFirst();
                        return Empty.NULL;
                    }).blockingFirst();
        } catch (Exception ex) {
            Log.e(TAG, "Failed to unregister device: " + deviceId + "/" + accountToken, ex);
        }
    }

    private String getToken(Account account) {
        String deviceId = FirebaseInstanceId.getInstance().getToken();
        if (deviceId == null) {
            return null;
        }

        try {
            return FirebaseInstanceId.getInstance().getToken(
                    account.mNotificationsSenderId, TOKEN_SCOPE);
        } catch (IOException ex) {
            // ignore
        }

        return null;
    }
}
