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
package com.ruesga.rview.wizards;

import android.content.Intent;
import android.os.Bundle;

import com.ruesga.rview.MainActivity;
import com.ruesga.rview.gerrit.model.AccountInfo;
import com.ruesga.rview.misc.ExceptionHelper;
import com.ruesga.rview.misc.Formatter;
import com.ruesga.rview.misc.SerializationManager;
import com.ruesga.rview.model.Account;
import com.ruesga.rview.preferences.Preferences;
import com.ruesga.rview.wizard.WizardActivity;

public class AuthorizationAccountSetupActivity extends WizardActivity {

    @Override
    @SuppressWarnings("unchecked")
    public void setupPages() {
        Account account = Preferences.getAccount(this);
        if (account == null) {
            return;
        }

        addPage(AccountPageFragment.class);

        Bundle savedInstance = new Bundle();
        savedInstance.putString(AccountPageFragment.STATE_REPO_NAME,
                account.mRepository.mName);
        savedInstance.putString(AccountPageFragment.STATE_REPO_URL,
                account.mRepository.mUrl);
        savedInstance.putBoolean(AccountPageFragment.STATE_REPO_TRUST_ALL_CERTIFICATES,
                account.mRepository.mTrustAllCertificates);
        savedInstance.putString(AccountPageFragment.STATE_ACCOUNT_USERNAME,
                account.mAccount.username);
        savedInstance.putBoolean(AccountPageFragment.STATE_ACCOUNT_ACCESS_MODE,
                account.hasAuthenticatedAccessMode());
        savedInstance.putBoolean(AccountPageFragment.STATE_AUTHENTICATION_FAILURE,
                getIntent().getBooleanExtra(ExceptionHelper.EXTRA_AUTHENTICATION_FAILURE, false));
        savedInstance.putBoolean(AccountPageFragment.STATE_SINGLE_PAGE, true);
        restoreInstance(savedInstance);
    }

    @Override
    public Intent onWizardFinished(Bundle savedState) {
        super.onWizardFinished(savedState);

        // Update the authentication account variables
        Account account = Preferences.getAccount(this);
        if (account != null) {
            Account oldAccount = new Account();
            oldAccount.mAccount = account.mAccount;
            oldAccount.mRepository = account.mRepository;

            boolean authenticatedMode = savedState.getBoolean(
                    AccountPageFragment.STATE_ACCOUNT_ACCESS_MODE);
            account.mAccount = SerializationManager.getInstance().fromJson(
                    savedState.getString(AccountPageFragment.STATE_ACCOUNT_INFO),
                            AccountInfo.class);
            if (authenticatedMode) {
                account.mToken = savedState.getString(AccountPageFragment.STATE_ACCOUNT_PASSWORD);
            }

            Preferences.addOrUpdateAccount(this, oldAccount, account);
            Preferences.setAccount(this, account);
            Formatter.refreshCachedPreferences(this);
        }

        // Reload the main activity
        Intent i = new Intent(this, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);

        return null;
    }
}
