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
import android.view.KeyEvent;

import com.ruesga.rview.databinding.ContentBinding;
import com.ruesga.rview.fragments.EditorFragment;
import com.ruesga.rview.fragments.KeyEventBindable;
import com.ruesga.rview.preferences.Constants;

public class EditorActivity extends BaseActivity {

    private static final String FRAGMENT_TAG = "details";

    private ContentBinding mBinding;

    private int mLegacyChangeId;
    private String mChangeId;

    private String mFileName;
    private String mContentFile;

    @SuppressWarnings("Convert2streamapi")
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

        mLegacyChangeId = getIntent().getIntExtra(Constants.EXTRA_LEGACY_CHANGE_ID, -1);
        if (mLegacyChangeId == -1) {
            finish();
            return;
        }
        mChangeId = getIntent().getStringExtra(Constants.EXTRA_CHANGE_ID);
        if (TextUtils.isEmpty(mChangeId)) {
            finish();
            return;
        }
        mFileName = getIntent().getStringExtra(Constants.EXTRA_FILE);
        mContentFile = getIntent().getStringExtra(Constants.EXTRA_CONTENT_FILE);
        boolean mReadOnly = !TextUtils.isEmpty(mContentFile);

        // Setup the title
        setUseTwoPanel(false);
        setForceSinglePanel(true);
        setupActivity();
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(
                    getString(mReadOnly ? R.string.change_view_title : R.string.change_edit_title,
                            mLegacyChangeId));
        }

        if (savedInstanceState != null) {
            Fragment fragment = getSupportFragmentManager().getFragment(
                    savedInstanceState, FRAGMENT_TAG);
            if (fragment != null) {
                FragmentTransaction tx = getSupportFragmentManager().beginTransaction()
                        .setAllowOptimization(false);
                tx.replace(R.id.content, fragment, FRAGMENT_TAG);
                tx.commit();
            }
        } else {
            createEditorFragment();
        }

        // All configured ok, but the RESULT_OK should be done on publishing the edit
        setResult(RESULT_CANCELED);
    }

    private void createEditorFragment() {
        FragmentTransaction tx = getSupportFragmentManager().beginTransaction()
                .setAllowOptimization(false);
        Fragment fragment = EditorFragment.newInstance(
                mLegacyChangeId, mChangeId, mFileName, mContentFile);
        tx.replace(R.id.content, fragment, FRAGMENT_TAG);
        tx.commit();
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
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        //Save the fragment's instance
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG);
        if (fragment != null) {
            getSupportFragmentManager().putFragment(outState, FRAGMENT_TAG, fragment);
        }
    }

    @Override
    public boolean onKeyDown(int keycode, KeyEvent e) {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG);
        if (fragment != null && fragment instanceof KeyEventBindable) {
            if (((KeyEventBindable) fragment).onKeyDown(keycode, e)) {
                return true;
            }
        }
        return super.onKeyDown(keycode, e);
    }

    @Override
    public void onBackPressed() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG);
        if (fragment != null && fragment instanceof KeyEventBindable) {
            if (((KeyEventBindable) fragment).onKeyDown(KeyEvent.KEYCODE_BACK, null)) {
                return;
            }
        }
        super.onBackPressed();
    }
}
