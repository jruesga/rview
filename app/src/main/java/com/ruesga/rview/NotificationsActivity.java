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

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.ruesga.rview.databinding.ActivityBaseBinding;
import com.ruesga.rview.fragments.NotificationsFragment;
import com.ruesga.rview.misc.ActivityHelper;
import com.ruesga.rview.misc.ModelHelper;
import com.ruesga.rview.model.Account;
import com.ruesga.rview.preferences.Constants;
import com.ruesga.rview.providers.NotificationEntity;

public class NotificationsActivity extends AppCompatDelegateActivity {

    private static final String FRAGMENT_TAG = "notifications";

    private ActivityBaseBinding mBinding;
    private Account mAccount;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retrieve the account
        if (getIntent() != null) {
            String accountId = getIntent().getStringExtra(Constants.EXTRA_ACCOUNT_HASH);
            if (accountId != null) {
                mAccount = ModelHelper.getAccountFromHash(this, accountId);
            }
        }

        // Check we have a valid account
        if (mAccount == null) {
            // Not sure how can get here
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_base);
        setResult(RESULT_OK);

        // Setup the title
        setupToolbar();
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getString(R.string.account_notifications_title));
            getSupportActionBar().setSubtitle(getString(R.string.account_notifications_subtitle,
                    mAccount.getRepositoryDisplayName(), mAccount.getAccountDisplayName()));
        }

        FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
        Fragment fragment;
        if (savedInstanceState != null) {
            fragment = getSupportFragmentManager().getFragment(savedInstanceState, FRAGMENT_TAG);
        } else {
            fragment = NotificationsFragment.newInstance(mAccount);
        }
        tx.replace(R.id.content, fragment, FRAGMENT_TAG).commit();
    }

    protected void setupToolbar() {
        setSupportActionBar(mBinding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        //Save the fragment's instance
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG);
        if (fragment != null) {
            getSupportFragmentManager().putFragment(outState, FRAGMENT_TAG, fragment);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.notification_options, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                return ActivityHelper.performFinishActivity(this, false);
            case R.id.menu_mark_as_read:
                performMarkAsReadAccountNotifications();
                return true;
            case R.id.menu_delete_all:
                performDeleteAccountNotifications();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void performMarkAsReadAccountNotifications() {
        NotificationEntity.markAccountNotificationsAsRead(this, mAccount.getAccountHash());
        invalidateOptionsMenu();
    }

    private void performDeleteAccountNotifications() {
        NotificationEntity.deleteAccountNotifications(this, mAccount.getAccountHash());
        invalidateOptionsMenu();
    }
}
