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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.ruesga.rview.BuildConfig;
import com.ruesga.rview.gerrit.GerritApi;
import com.ruesga.rview.gerrit.model.CloudNotificationInput;
import com.ruesga.rview.gerrit.model.CloudNotificationResponseMode;
import com.ruesga.rview.misc.ExceptionHelper;
import com.ruesga.rview.misc.ModelHelper;
import com.ruesga.rview.model.Account;
import com.ruesga.rview.preferences.Preferences;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;
import me.tatarka.rxloader2.safe.Empty;
import me.tatarka.rxloader2.safe.SafeObservable;

public class DeviceRegistrationService extends JobIntentService {

    private static final String TAG = "DeviceRegisterService";

    private static final String REGISTER_DEVICE_ACTION = BuildConfig.APPLICATION_ID + ".actions.REGISTER_DEVICE";
    private static final String EXTRA_ACCOUNT = "account";

    private static final String TOKEN_SCOPE = "FCM";

    private static final int JOB_ID = 1001;

    public DeviceRegistrationService() {
        super();
    }

    public static void register(Context context, Account account) {
        Intent work = new Intent();
        work.setAction(REGISTER_DEVICE_ACTION);
        if (account != null) {
            work.putExtra(DeviceRegistrationService.EXTRA_ACCOUNT, account.getAccountHash());
        }
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

    @SuppressLint("CheckResult")
    @SuppressWarnings("ResultOfMethodCallIgnored")
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

    @SuppressLint("CheckResult")
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void performDeviceUnregistration(Context ctx, Account account) {
        String deviceId = getToken(account);
        if (deviceId == null) {
            return;
        }

        final String accountToken = account.getAccountHash();
        GerritApi api = ModelHelper.getGerritApi(ctx, account);
        boolean unregistered = false;
        try {
            SafeObservable.fromNullCallable(() -> {
                        api.unregisterCloudNotification(
                                GerritApi.SELF_ACCOUNT, deviceId, accountToken).blockingFirst();
                        return Empty.NULL;
                    }).blockingFirst();
            unregistered = true;
        } catch (Exception ex) {
            if (ExceptionHelper.isResourceNotFoundException(ex)) {
                unregistered = true;
            } else {
                Log.e(TAG, "Failed to unregister device: " + deviceId + "/" + accountToken, ex);
            }
        }

        if (unregistered) {
            Log.i(TAG, "Device unregistered: " + deviceId + "/" + accountToken);
            account.mNotificationsSenderId = null;
            Preferences.addOrUpdateAccount(this, account);
            Account acct = Preferences.getAccount(this);
            if (acct != null && account.getAccountHash().equals(acct.getAccountHash())) {
                Preferences.setAccount(this, account);
            }
        }
    }

    @SuppressLint("MissingFirebaseInstanceTokenRefresh")
    private String getToken(Account account) {
        try {
            return FirebaseInstanceId.getInstance().getToken(
                    account.mNotificationsSenderId, TOKEN_SCOPE);
        } catch (Exception ex) {
            Log.w(TAG, "Failed to obtain a Firebase token", ex);
        }
        return null;
    }
}
