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

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.widget.DrawerLayout;
import android.view.MenuItem;

import com.ruesga.rview.databinding.ContentBinding;
import com.ruesga.rview.fragments.AccountSettingsFragment;
import com.ruesga.rview.model.Account;
import com.ruesga.rview.preferences.Preferences;

public class AccountSettingsActivity extends BaseActivity {

    private static final String FRAGMENT_TAG = "settings";

    private ContentBinding mBinding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check we have a valid account
        Account account = Preferences.getAccount(this);
        if (account == null) {
            // Not sure how can get here
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        mBinding = DataBindingUtil.setContentView(this, R.layout.content);
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
    public DrawerLayout getDrawerLayout() {
        return null;
    }

    @Override
    public ContentBinding getContentBinding() {
        return mBinding;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                Intent upIntent = NavUtils.getParentActivityIntent(this);
                if (NavUtils.shouldUpRecreateTask(this, upIntent)) {
                    TaskStackBuilder.create(this)
                            .addNextIntentWithParentStack(upIntent)
                            .startActivities();
                } else {
                    NavUtils.navigateUpTo(this, upIntent);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
