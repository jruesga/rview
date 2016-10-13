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
import com.ruesga.rview.preferences.Constants;

import java.lang.reflect.Method;
import java.util.ArrayList;

public class TabFragmentActivity extends ChangeListBaseActivity {

    private final String EXTRA_SELECTED_ITEM = "selected_item";

    private int mSelectedChangeId = INVALID_ITEM;

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

        String fragment = getIntent().getStringExtra(Constants.EXTRA_FRAGMENT);
        if (fragment == null) {
            finish();
            return;
        }
        ArrayList<String> args = getIntent().getStringArrayListExtra(Constants.EXTRA_FRAGMENT_ARGS);
        if (args == null) {
            finish();
            return;
        }

        String title = getIntent().getStringExtra(Constants.EXTRA_TITLE);
        if (title == null) {
            finish();
            return;
        }
        String subtitle = getIntent().getStringExtra(Constants.EXTRA_SUBTITLE);

        // Setup the title
        setupActivity();
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
            getSupportActionBar().setSubtitle(subtitle);
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
                openFragment(fragment, args);
            }
        } else {
            openFragment(fragment, args);
        }
    }

    @SuppressWarnings("ConfusingArgumentToVarargsMethod")
    private void openFragment(String fragmentName, ArrayList<String> args)
            throws IllegalArgumentException {
        Fragment fragment;
        try {
            Class<?> cls = Class.forName(fragmentName);
            Method m = cls.getDeclaredMethod("newFragment", ArrayList.class);
            fragment = (Fragment) m.invoke(null, args);
        } catch (Exception cause) {
            throw new IllegalArgumentException(cause);
        }

        FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
        tx.replace(R.id.content, fragment, FRAGMENT_TAG_LIST);
        tx.commit();
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
}
