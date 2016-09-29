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
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ruesga.rview.BaseActivity;
import com.ruesga.rview.R;
import com.ruesga.rview.databinding.DiffViewerFragmentBinding;
import com.ruesga.rview.gerrit.model.ChangeInfo;
import com.ruesga.rview.misc.CacheHelper;
import com.ruesga.rview.misc.SerializationManager;
import com.ruesga.rview.preferences.Constants;
import com.ruesga.rview.widget.PagerControllerLayout.PagerControllerAdapter;

import java.io.File;
import java.io.IOException;
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
            return FileDiffViewerFragment.newInstance(
                    mChange.legacyChangeId, mRevisionId, mFile, mBase, null, null);
        }

        @Override
        public int getTarget() {
            return R.id.diff_content;
        }
    };

    private DiffViewerFragmentBinding mBinding;

    private ChangeInfo mChange;
    private final List<String> mFiles = new ArrayList<>();
    private String mRevisionId;
    private String mFile;
    private int mBase;

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
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        try {
            // Deserialize the change
            mChange = SerializationManager.getInstance().fromJson(
                    new String(CacheHelper.readAccountDiffCacheDir(
                            getContext(), EXTRA_CHANGE_JSON)), ChangeInfo.class);
            loadFiles();

            // Configure the pages adapter
            BaseActivity activity = ((BaseActivity) getActivity());
            activity.configurePages(mAdapter, position -> {
                mFile = mFiles.get(position);
                mCurrentFile = position;
            });
            activity.getContentBinding().pagerController.currentPage(mCurrentFile);

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
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(Constants.EXTRA_REVISION_ID, mRevisionId);
        outState.putString(Constants.EXTRA_FILE, mFile);
        outState.putInt(Constants.EXTRA_BASE, mBase);
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

}
