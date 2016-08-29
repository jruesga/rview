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

import com.ruesga.rview.databinding.ContentBinding;
import com.ruesga.rview.fragments.ChangeDetailsFragment;
import com.ruesga.rview.fragments.ChangeListFragment;

public class ChangeDetailsActivity extends BaseActivity {
    private ContentBinding mContentBinding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        mContentBinding = DataBindingUtil.setContentView(this, R.layout.content);
        super.onCreate(savedInstanceState);

        // Check we have valid arguments
        if (getIntent() == null) {
            finish();
            return;
        }
// TODO Uncomment
int legacyChangeId = 157838;
String changeId = "Ie69289c3c125ebe3d36a8448be95b16c4240348c";
//        int legacyChangeId = getIntent().getIntExtra(ChangeDetailsFragment.EXTRA_LEGACY_CHANGE_ID, -1);
//        if (legacyChangeId == -1) {
//            finish();
//            return;
//        }
//        String changeId = getIntent().getStringExtra(ChangeDetailsFragment.EXTRA_CHANGE_ID);
//        if (changeId == null) {
//            finish();
//            return;
//        }

        // Setup the title
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getString(R.string.change_details_title, legacyChangeId));
            getSupportActionBar().setSubtitle(changeId);
        }

        FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
        Fragment newFragment = ChangeDetailsFragment.newInstance(changeId);
        tx.replace(R.id.content, newFragment, "details").commit();
    }

    @Override
    public ContentBinding getContentBinding() {
        return mContentBinding;
    }

    @Override
    public DrawerLayout getDrawerLayout() {
        return null;
    }
}
