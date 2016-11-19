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

import com.ruesga.rview.gerrit.model.AccountInfo;
import com.ruesga.rview.misc.SerializationManager;
import com.ruesga.rview.model.Account;
import com.ruesga.rview.model.Repository;
import com.ruesga.rview.preferences.Preferences;
import com.ruesga.rview.wizard.WizardActivity;

public class SetupAccountActivity extends WizardActivity {

    public static final String EXTRA_ACCOUNT = "account";

    @Override
    @SuppressWarnings("unchecked")
    public void setupPages() {
        final boolean isFirstRun = Preferences.isFirstRun(this);
        if (isFirstRun) {
            addPage(WelcomePageFragment.class);
        } else {
            addPage(AccountSetupPageFragment.class);
        }
        addPage(RepositoryPageFragment.class);
        addPage(AccountPageFragment.class);
        if (isFirstRun) {
            addPage(ConfirmationPageFragment.class);
        } else {
            addPage(AccountReadyPageFragment.class);
        }
    }

    @Override
    public Intent onWizardFinished(Bundle savedState) {
        // Extra all the needed extras
        String repoName = savedState.getString(RepositoryPageFragment.STATE_REPO_NAME);
        String repoUrl = savedState.getString(RepositoryPageFragment.STATE_REPO_URL);
        boolean repoTrustAllCertificates =
                savedState.getBoolean(RepositoryPageFragment.STATE_REPO_TRUST_ALL_CERTIFICATES);
        String accountUsername;
        String accountPassword = null;
        boolean authenticatedMode = savedState.getBoolean(
                AccountPageFragment.STATE_ACCOUNT_ACCESS_MODE);
        AccountInfo accountInfo = SerializationManager.getInstance().fromJson(
                savedState.getString(AccountPageFragment.STATE_ACCOUNT_INFO), AccountInfo.class);
        if (authenticatedMode) {
            accountUsername = savedState.getString(AccountPageFragment.STATE_ACCOUNT_USERNAME);
            accountPassword = savedState.getString(AccountPageFragment.STATE_ACCOUNT_PASSWORD);
            if (accountInfo.username == null) {
                accountInfo.username = accountUsername;
            }
        }

        // Create the account
        Account account = new Account();
        account.mRepository = new Repository(repoName, repoUrl, repoTrustAllCertificates);
        account.mAccount = accountInfo;
        if (authenticatedMode) {
            account.mToken = accountPassword;
        }

        // We have an account, so assume that we ended the "first run" experience
        Preferences.setFirstRun(this);

        // Send the information of the new account
        Intent intent = new Intent();
        intent.putExtra(EXTRA_ACCOUNT, account);
        return intent;
    }
}
