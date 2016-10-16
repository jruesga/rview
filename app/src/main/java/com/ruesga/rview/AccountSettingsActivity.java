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
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.view.MenuItem;

import com.ruesga.rview.databinding.SettingsBinding;
import com.ruesga.rview.fragments.AccountSettingsFragment;
import com.ruesga.rview.misc.ActivityHelper;
import com.ruesga.rview.model.Account;
import com.ruesga.rview.preferences.Preferences;

public class AccountSettingsActivity extends AppCompatActivity {

    private static final String FRAGMENT_TAG = "settings";

    private SettingsBinding mBinding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);

        // Check we have a valid account
        Account account = Preferences.getAccount(this);
        if (account == null) {
            // Not sure how can get here
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        mBinding = DataBindingUtil.setContentView(this, R.layout.settings);
        setResult(RESULT_OK);

        // Setup the title
        setupToolbar();
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getString(R.string.account_settings_title));
            getSupportActionBar().setSubtitle(getString(R.string.account_settings_subtitle,
                    account.getRepositoryDisplayName(), account.getAccountDisplayName()));
        }

        FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
        Fragment fragment;
        if (savedInstanceState != null) {
            fragment = getSupportFragmentManager().getFragment(savedInstanceState, FRAGMENT_TAG);
        } else {
            fragment = AccountSettingsFragment.newInstance();
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
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                return ActivityHelper.performFinishActivity(this, true);
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
