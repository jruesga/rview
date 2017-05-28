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
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.ruesga.rview.gerrit.GerritApi;
import com.ruesga.rview.gerrit.model.Features;
import com.ruesga.rview.misc.ExceptionHelper;
import com.ruesga.rview.misc.ModelHelper;
import com.ruesga.rview.model.Account;
import com.ruesga.rview.preferences.Preferences;

import java.util.List;

public class AccountStatusFetcherService extends IntentService {

    private static final String TAG = "AccountStatusFetcher";

    public static final String ACCOUNT_STATUS_FETCHER_ACTION =
            "com.ruesga.rview.actions.ACCOUNT_STATUS_FETCHER";
    public static final String EXTRA_ACCOUNT = "account";

    public AccountStatusFetcherService() {
        super(TAG);
    }

    @Override
    @SuppressWarnings("Convert2streamapi")
    protected void onHandleIntent(Intent intent) {
        if (intent != null && intent.getAction().equals(ACCOUNT_STATUS_FETCHER_ACTION)) {
            String account = intent.getStringExtra(EXTRA_ACCOUNT);
            List<Account> accounts = Preferences.getAccounts(this);
            for (Account acct : accounts) {
                if (acct.getAccountHash().equals(account)) {
                    // Perform registration in background
                    Thread t = new Thread(() -> {
                        final Context ctx = AccountStatusFetcherService.this.getApplicationContext();
                        performFetchAccountStatus(ctx, acct);
                    });
                    t.start();
                }
            }
        }
    }

    private void performFetchAccountStatus(Context ctx, Account account) {
        try {
            GerritApi api = ModelHelper.getGerritApi(ctx, account);
            account.mServerVersion = api.getServerVersion().blockingFirst();
            if (account.hasAuthenticatedAccessMode() && api.supportsFeature(Features.ACCOUNT_STATUS)) {
                account.mAccount.status =
                        api.getAccountStatus(GerritApi.SELF_ACCOUNT).blockingFirst();
            } else {
                account.mAccount.status = null;
            }
            Preferences.setAccount(ctx, account);
            notifyAccountStatusChanged(ctx, account);

        } catch (Exception ex) {
            // Check if feature is supported
            if (ExceptionHelper.isResourceNotFoundException(ex)) {
                account.mAccount.status = null;
                account.mServerVersion = null;
                Preferences.setAccount(ctx, account);
                notifyAccountStatusChanged(ctx, account);
                return;
            }
            Log.e(TAG, "Failed to fetch account status", ex);
        }
    }

    private void notifyAccountStatusChanged(Context ctx, Account account) {
        Intent i = new Intent(ACCOUNT_STATUS_FETCHER_ACTION);
        i.putExtra(EXTRA_ACCOUNT, account.getAccountHash());
        LocalBroadcastManager.getInstance(ctx).sendBroadcast(i);
    }
}
