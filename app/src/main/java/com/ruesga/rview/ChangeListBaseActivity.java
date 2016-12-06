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

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;

import com.ruesga.rview.fragments.ChangeDetailsFragment;
import com.ruesga.rview.fragments.ChangeListFragment;
import com.ruesga.rview.gerrit.model.ChangeInfo;
import com.ruesga.rview.misc.ActivityHelper;

public abstract class ChangeListBaseActivity extends BaseActivity implements OnChangeItemListener {

    public static final int INVALID_ITEM = -1;

    private boolean mIsTwoPane;

    public abstract int getSelectedChangeId();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mIsTwoPane = getResources().getBoolean(R.bool.config_is_two_pane);
    }

    @Override
    public void onChangeItemPressed(ChangeInfo change) {
        if (!mIsTwoPane) {
            // Open activity
            ActivityHelper.openChangeDetails(this, change, true, false);
        } else {
            // Open the filter fragment
            loadChangeDetailsFragment(change.legacyChangeId);
        }
    }

    @Override
    public void onChangeItemRestored(int changeId) {
        if (mIsTwoPane && changeId != getSelectedChangeId()) {
            loadChangeDetailsFragment(changeId);
        }
    }

    @Override
    public void onChangeItemSelected(int changeId) {
        // This event is only interesting for the two pane layout
        if (mIsTwoPane) {
            if (changeId == ChangeListFragment.NO_SELECTION) {
                // Remove the details fragment
                FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
                Fragment oldFragment = getSupportFragmentManager().findFragmentByTag(
                        FRAGMENT_TAG_DETAILS);
                if (oldFragment != null) {
                    tx.remove(oldFragment);
                }
                tx.commit();
            } else {
                // Load the details of the change
                loadChangeDetailsFragment(changeId);
            }
        }
    }

    private void loadChangeDetailsFragment(int changeId) {
        FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
        Fragment oldFragment = getSupportFragmentManager().findFragmentByTag(
                FRAGMENT_TAG_DETAILS);
        if (oldFragment != null) {
            tx.remove(oldFragment);
        }
        Fragment newFragment = ChangeDetailsFragment.newInstance(changeId);
        tx.replace(mIsTwoPane ? R.id.details : R.id.content, newFragment,
                FRAGMENT_TAG_DETAILS).commit();
    }
}
