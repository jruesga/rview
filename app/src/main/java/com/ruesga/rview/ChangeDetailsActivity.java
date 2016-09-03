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
import com.ruesga.rview.fragments.ChangeDetailsFragment;

import static com.ruesga.rview.fragments.ChangeDetailsFragment.EXTRA_CHANGE_ID;
import static com.ruesga.rview.fragments.ChangeDetailsFragment.EXTRA_LEGACY_CHANGE_ID;

public class ChangeDetailsActivity extends BaseActivity {

    private ContentBinding mBinding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBinding = DataBindingUtil.setContentView(this, R.layout.content);

        boolean isTwoPanel = getResources().getBoolean(R.bool.config_is_two_pane);
        if (isTwoPanel) {
            // Tablets have a two panel layout in landscape, so finish the current activity
            // to show the change in the proper activity
            finish();
            return;
        }

        // Check we have valid arguments
        if (getIntent() == null) {
            finish();
            return;
        }
        int legacyChangeId = getIntent().getIntExtra(EXTRA_LEGACY_CHANGE_ID, -1);
        if (legacyChangeId == -1) {
            legacyChangeId = 158921;
            //finish();
            //return;
        }
        String changeId = getIntent().getStringExtra(EXTRA_CHANGE_ID);
        if (changeId == null) {
            changeId = "Ibe984d862df0e1a5910ea80a66c44d599c299f10";
            //finish();
            //return;
        }

        // Setup the title
        setupToolbar();
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getString(R.string.change_details_title, legacyChangeId));
            getSupportActionBar().setSubtitle(changeId);
        }

        FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
        Fragment fragment;
        if (savedInstanceState != null) {
            fragment = getSupportFragmentManager().getFragment(savedInstanceState, "details");
        } else {
            fragment = ChangeDetailsFragment.newInstance(legacyChangeId);
        }
        tx.replace(R.id.content, fragment, "details").commit();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        //Save the fragment's instance
        Fragment fragment = getSupportFragmentManager().findFragmentByTag("details");
        if (fragment != null) {
            getSupportFragmentManager().putFragment(outState, "details", fragment);
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
