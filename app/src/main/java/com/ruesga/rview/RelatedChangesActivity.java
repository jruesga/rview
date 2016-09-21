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
import android.support.v4.widget.DrawerLayout;
import android.view.MenuItem;

import com.ruesga.rview.databinding.ContentBinding;
import com.ruesga.rview.fragments.RelatedChangesFragment;
import com.ruesga.rview.preferences.Constants;

public class RelatedChangesActivity extends ChangeListBaseActivity {

    private int mSelectedChangeId = INVALID_ITEM;

    private final String EXTRA_SELECTED_ITEM = "selected_item";

    private ContentBinding mBinding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBinding = DataBindingUtil.setContentView(this, R.layout.content);

        // Check we have valid arguments
        if (getIntent() == null) {
            finish();
            return;
        }
        int legacyChangeId = getIntent().getIntExtra(Constants.EXTRA_LEGACY_CHANGE_ID, -1);
        if (legacyChangeId == -1) {
            finish();
            return;
        }
        String changeId = getIntent().getStringExtra(Constants.EXTRA_CHANGE_ID);
        if (changeId == null) {
            finish();
            return;
        }
        String projectId = getIntent().getStringExtra(Constants.EXTRA_PROJECT_ID);
        if (projectId == null) {
            finish();
            return;
        }
        String revisionId = getIntent().getStringExtra(Constants.EXTRA_REVISION_ID);
        if (revisionId == null) {
            finish();
            return;
        }
        String topic = getIntent().getStringExtra(Constants.EXTRA_TOPIC);

        // Setup the title
        setupToolbar();
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getString(R.string.change_details_title, legacyChangeId));
            getSupportActionBar().setSubtitle(changeId);
        }

        if (savedInstanceState != null) {
            mSelectedChangeId = savedInstanceState.getInt(EXTRA_SELECTED_ITEM, INVALID_ITEM);

            Fragment detailsFragment = getSupportFragmentManager().getFragment(
                    savedInstanceState, FRAGMENT_TAG_DETAILS);
            Fragment listFragment = getSupportFragmentManager().getFragment(
                    savedInstanceState, FRAGMENT_TAG_LIST);
            if (listFragment != null) {
                FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
                tx.replace(R.id.content, listFragment, FRAGMENT_TAG_LIST);
                if (detailsFragment != null) {
                    tx.replace(R.id.details, detailsFragment, FRAGMENT_TAG_DETAILS);
                }
                tx.commit();
            } else {
                openRelatedChangesFragment(legacyChangeId, changeId, projectId, revisionId, topic);
            }
        } else {
            openRelatedChangesFragment(legacyChangeId, changeId, projectId, revisionId, topic);
        }
    }

    private void openRelatedChangesFragment(int legacyChangeId, String changeId,
            String projectId, String revisionId, String topic) {
        FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
        Fragment fragment = RelatedChangesFragment.newInstance(
                legacyChangeId, changeId, projectId, revisionId, topic);
        tx.replace(R.id.content, fragment, FRAGMENT_TAG_LIST).commit();
    }

    @Override
    public int getSelectedChangeId() {
        return mSelectedChangeId;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(EXTRA_SELECTED_ITEM, mSelectedChangeId);

        //Save the fragment's instance
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG_LIST);
        if (fragment != null) {
            getSupportFragmentManager().putFragment(outState, FRAGMENT_TAG_LIST, fragment);
        }
        fragment = getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG_DETAILS);
        if (fragment != null) {
            getSupportFragmentManager().putFragment(outState, FRAGMENT_TAG_DETAILS, fragment);
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
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
