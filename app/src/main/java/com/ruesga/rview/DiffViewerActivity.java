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
import android.text.TextUtils;
import android.util.Log;

import com.ruesga.rview.databinding.ContentBinding;
import com.ruesga.rview.fragments.DiffViewerFragment;
import com.ruesga.rview.gerrit.model.ChangeInfo;
import com.ruesga.rview.misc.CacheHelper;
import com.ruesga.rview.misc.SerializationManager;
import com.ruesga.rview.preferences.Constants;

import java.io.IOException;

public class DiffViewerActivity extends BaseActivity {

    private static final String TAG = "DiffViewerActivity";

    private static final String FRAGMENT_TAG = "diff";

    private ContentBinding mBinding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBinding = DataBindingUtil.setContentView(this, R.layout.content);

        // Check we have valid arguments
        if (getIntent() == null) {
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        String file = getIntent().getStringExtra(Constants.EXTRA_FILE);
        if (TextUtils.isEmpty(file)) {
            setResult(RESULT_CANCELED);
            finish();
            return;
        }
        String revisionId = getIntent().getStringExtra(Constants.EXTRA_REVISION_ID);
        if (TextUtils.isEmpty(file)) {
            setResult(RESULT_CANCELED);
            finish();
            return;
        }
        String data = getIntent().getStringExtra(Constants.EXTRA_DATA);
        if (TextUtils.isEmpty(data)) {
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        // Deserialize the change and cache the information
        ChangeInfo change;
        try {
            change = SerializationManager.getInstance().fromJson(data, ChangeInfo.class);
            CacheHelper.writeAccountDiffCacheDir(this,
                    CacheHelper.CACHE_CHANGE_JSON, data.getBytes());
        } catch (IOException ex) {
            Log.e(TAG, "Failed to load change cached data", ex);
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        // Setup the title
        setupActivity();
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(
                    getString(R.string.change_details_title, change.legacyChangeId));
        }

        setUseTwoPanel(false);

        if (savedInstanceState != null) {
            Fragment fragment = getSupportFragmentManager().getFragment(
                    savedInstanceState, FRAGMENT_TAG);
            if (fragment != null) {
                FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
                tx.replace(R.id.content, fragment, FRAGMENT_TAG);
                tx.commit();
            }
        } else {
            createDiffViewFragment(revisionId, file);
        }

        // All configured ok
        setResult(RESULT_OK);
    }

    private void createDiffViewFragment(String revisionId, String file) {
        FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
        Fragment fragment = DiffViewerFragment.newInstance(revisionId, file);
        tx.replace(R.id.content, fragment, FRAGMENT_TAG);
        tx.commit();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        //Save the fragment's instance
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG);
        if (fragment != null) {
            getSupportFragmentManager().putFragment(outState, FRAGMENT_TAG_LIST, fragment);
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
}
