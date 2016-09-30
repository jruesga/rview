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
package com.ruesga.rview.fragments;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView.OnNavigationItemSelectedListener;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.ruesga.rview.BaseActivity;
import com.ruesga.rview.R;
import com.ruesga.rview.databinding.DiffViewerFragmentBinding;
import com.ruesga.rview.gerrit.model.ChangeInfo;
import com.ruesga.rview.misc.CacheHelper;
import com.ruesga.rview.misc.SerializationManager;
import com.ruesga.rview.model.Account;
import com.ruesga.rview.preferences.Constants;
import com.ruesga.rview.preferences.Preferences;
import com.ruesga.rview.widget.DiffView;
import com.ruesga.rview.widget.PagerControllerLayout.PagerControllerAdapter;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class DiffViewerFragment extends Fragment {

    private static final String TAG = "DiffViewerFragment";

    public static final String EXTRA_CHANGE_JSON = "change.json";

    private PagerControllerAdapter<String> mAdapter = new PagerControllerAdapter<String>() {

        @Override
        public FragmentManager getFragmentManager() {
            return getChildFragmentManager();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            if (position < 0 || position >= getCount()) {
                return null;
            }
            return new File(mFiles.get(position)).getName();
        }

        @Override
        public String getItem(int position) {
            if (position < 0 || position >= getCount()) {
                return null;
            }
            return mFiles.get(position);
        }

        @Override
        public int getCount() {
            return mFiles.size();
        }

        @Override
        public Fragment getFragment(int position) {
            // FIXME
            mFragment = new WeakReference<>(
                    FileDiffViewerFragment.newInstance(
                            mChange.legacyChangeId, mRevisionId, mFile, mBase, null, null));
            mFragment.get().wrap(mWrap).mode(mDiffMode);
            return mFragment.get();
        }

        @Override
        public int getTarget() {
            return R.id.diff_content;
        }
    };

    private OnNavigationItemSelectedListener mOptionsItemListener
            = new OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            if (mFragment != null && mFragment.get() != null) {
                switch (item.getItemId()) {
                    case R.id.diff_mode_unified:
                        mFragment.get().mode(DiffView.UNIFIED_MODE).update();
                        mDiffMode = DiffView.UNIFIED_MODE;
                        break;
                    case R.id.diff_mode_side_by_side:
                        mFragment.get().mode(DiffView.SIDE_BY_SIDE_MODE).update();
                        mDiffMode = DiffView.SIDE_BY_SIDE_MODE;
                        break;
                    case R.id.wrap_mode_on:
                        mFragment.get().wrap(true).update();
                        mWrap = true;
                        break;
                    case R.id.wrap_mode_off:
                        mFragment.get().wrap(false).update();
                        mWrap = false;
                        break;
                }
            }
            ((BaseActivity) getActivity()).closeOptionsDrawer();
            return true;
        }
    };

    private DiffViewerFragmentBinding mBinding;
    private WeakReference<FileDiffViewerFragment> mFragment;

    private ChangeInfo mChange;
    private final List<String> mFiles = new ArrayList<>();
    private String mRevisionId;
    private String mFile;
    private int mBase;

    private int mDiffMode;
    private boolean mWrap;

    private int mCurrentFile;

    public static DiffViewerFragment newInstance(String revisionId, String file) {
        DiffViewerFragment fragment = new DiffViewerFragment();
        Bundle arguments = new Bundle();
        arguments.putString(Constants.EXTRA_REVISION_ID, revisionId);
        arguments.putString(Constants.EXTRA_FILE, file);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle state = (savedInstanceState != null) ? savedInstanceState : getArguments();
        mRevisionId = state.getString(Constants.EXTRA_REVISION_ID);
        mFile = state.getString(Constants.EXTRA_FILE_ID);
        mBase = state.getInt(Constants.EXTRA_BASE, 0);

        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(
                inflater, R.layout.diff_viewer_fragment, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle state) {
        super.onActivityCreated(state);

        try {
            // Deserialize the change
            mChange = SerializationManager.getInstance().fromJson(
                    new String(CacheHelper.readAccountDiffCacheDir(
                            getContext(), EXTRA_CHANGE_JSON)), ChangeInfo.class);
            loadFiles();

            // Get diff user preferences
            if (state != null) {
                mDiffMode = state.getInt(Constants.PREF_ACCOUNT_WRAP_MODE, DiffView.UNIFIED_MODE);
                mWrap = state.getBoolean(Constants.PREF_ACCOUNT_WRAP_MODE, true);
            } else {
                Account account = Preferences.getAccount(getContext());
                String diffMode = Preferences.getAccountDiffMode(getContext(), account);
                mDiffMode = diffMode.equals(Constants.DIFF_MODE_SIDE_BY_SIDE)
                        ? DiffView.SIDE_BY_SIDE_MODE : DiffView.UNIFIED_MODE;
                mWrap = Preferences.getAccountWrapMode(getContext(), account);
            }

            // Configure the pages adapter
            BaseActivity activity = ((BaseActivity) getActivity());
            activity.configurePages(mAdapter, position -> {
                mFile = mFiles.get(position);
                mCurrentFile = position;
            });
            activity.getContentBinding().pagerController.currentPage(mCurrentFile);
            activity.configureOptionsMenu(R.menu.diff_options, mOptionsItemListener);

        } catch (IOException ex) {
            Log.e(TAG, "Failed to load change cached data", ex);
            getActivity().finish();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mBinding != null) {
            mBinding.unbind();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.more, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_more:
                openOptionsMenu();
                break;
        }
        return false;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(Constants.EXTRA_REVISION_ID, mRevisionId);
        outState.putString(Constants.EXTRA_FILE, mFile);
        outState.putInt(Constants.EXTRA_BASE, mBase);
        outState.putInt("diff_mode", mDiffMode);
        outState.putBoolean("wrap_mode", mWrap);
    }

    private void loadFiles() {
        mFiles.clear();
        // Revisions doesn't include the COMMIT_MSG
        mFiles.add(Constants.COMMIT_MESSAGE);
        mCurrentFile = 0;
        int i = 1;
        for (String file : mChange.revisions.get(mRevisionId).files.keySet()) {
            if (file.equals(mFile)) {
                mCurrentFile = i;
            }
            mFiles.add(file);
            i++;
        }
    }

    private void openOptionsMenu() {
        // Update options
        BaseActivity activity =  ((BaseActivity) getActivity());
        Menu menu = activity.getOptionsMenu();
        menu.findItem(R.id.diff_mode_side_by_side).setChecked(
                mDiffMode == DiffView.SIDE_BY_SIDE_MODE);
        menu.findItem(R.id.diff_mode_unified).setChecked(mDiffMode == DiffView.UNIFIED_MODE);
        menu.findItem(R.id.wrap_mode_on).setChecked(mWrap);
        menu.findItem(R.id.wrap_mode_off).setChecked(!mWrap);

        // Open drawer
        activity.openOptionsDrawer();
    }

}
