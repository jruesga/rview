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

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ruesga.rview.BaseActivity;
import com.ruesga.rview.R;
import com.ruesga.rview.databinding.DiffViewerFragmentBinding;
import com.ruesga.rview.gerrit.GerritApi;
import com.ruesga.rview.gerrit.model.CommentInfo;
import com.ruesga.rview.gerrit.model.FileInfo;
import com.ruesga.rview.misc.ModelHelper;
import com.ruesga.rview.preferences.Constants;
import com.ruesga.rview.widget.SwipeableViewPager;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import me.tatarka.rxloader.RxLoader;
import me.tatarka.rxloader.RxLoaderManager;
import me.tatarka.rxloader.RxLoaderManagerCompat;
import me.tatarka.rxloader.RxLoaderObserver;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class DiffViewerFragment extends PageableFragment {

    private static final String TAG = "DiffViewerFragment";

    private static class DiffResponse {
        private Map<String, FileInfo> mFiles = new LinkedHashMap<>();
        private Map<String, List<CommentInfo>> mCommentsA = new LinkedHashMap<>();
        private Map<String, List<CommentInfo>> mCommentsB = new LinkedHashMap<>();
    }

    private final RxLoaderObserver<DiffResponse> mObserver = new RxLoaderObserver<DiffResponse>() {
        @Override
        public void onNext(DiffResponse response) {
            mResponse = response;
            mFiles = response.mFiles.keySet().toArray(new String[response.mFiles.size()]);
            invalidateAdapter();
            navigateToItem(getFilePosition(response), false);
            showProgress(false);
        }

        @Override
        public void onError(Throwable error) {
            ((BaseActivity) getActivity()).handleException(TAG, error);
            showProgress(false);
        }

        @Override
        public void onStarted() {
            showProgress(true);
        }

        private int getFilePosition(DiffResponse response) {
            int i = 0;
            for (String fileId : response.mFiles.keySet()) {
                if (fileId.equals(mFileId)) {
                    return i;
                }
                i++;
            }
            return 0;
        }
    };

    private DiffViewerFragmentBinding mBinding;

    private DiffResponse mResponse;
    private String[] mFiles = new String[0];

    private int mLegacyChangeId;
    private String mRevisionId;
    private String mFileId;
    private int mBase;

    private RxLoader<DiffResponse> mLoader;

    public static DiffViewerFragment newInstance(int changeId, String revisionId, String fileId) {
        DiffViewerFragment fragment = new DiffViewerFragment();
        Bundle arguments = new Bundle();
        arguments.putInt(Constants.EXTRA_LEGACY_CHANGE_ID, changeId);
        arguments.putString(Constants.EXTRA_REVISION_ID, revisionId);
        arguments.putString(Constants.EXTRA_FILE_ID, fileId);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle state = (savedInstanceState != null) ? savedInstanceState : getArguments();
        mLegacyChangeId = state.getInt(Constants.EXTRA_LEGACY_CHANGE_ID, Constants.INVALID_CHANGE_ID);
        mRevisionId = state.getString(Constants.EXTRA_REVISION_ID);
        mFileId = state.getString(Constants.EXTRA_FILE_ID);
        mBase = state.getInt(Constants.EXTRA_BASE, 0);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(
                inflater, R.layout.diff_viewer_fragment, container, false);
        startLoadersWithValidContext();
        return mBinding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        startLoadersWithValidContext();
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

        outState.putInt(Constants.EXTRA_LEGACY_CHANGE_ID, mLegacyChangeId);
        outState.putString(Constants.EXTRA_REVISION_ID, mRevisionId);
        outState.putString(Constants.EXTRA_FILE_ID, mFileId);
        outState.putInt("baseA", mBase);
    }

    private void startLoadersWithValidContext() {
        if (getActivity() == null) {
            return;
        }

        if (mLoader == null) {
            // Fetch or join current loader
            RxLoaderManager loaderManager = RxLoaderManagerCompat.get(this);
            mLoader = loaderManager.create("diff-" + mLegacyChangeId, fetchData(),
                    mObserver).start();
        }
    }


    @SuppressWarnings("ConstantConditions")
    private Observable<DiffResponse> fetchData() {
        final Context ctx = getActivity();
        final GerritApi api = ModelHelper.getGerritApi(ctx);
        return Observable.zip(
                    api.getChangeRevisionFiles(String.valueOf(mLegacyChangeId), mRevisionId),
                    Observable.fromCallable(() -> {
                        if (mBase != 0) {
                            return api.getChangeRevisionComments(
                                    String.valueOf(mLegacyChangeId),
                                    mRevisionId).toBlocking().first();
                        }
                        return null;
                    }),
                    api.getChangeRevisionComments(String.valueOf(mLegacyChangeId), mRevisionId),
                    this::combine
                )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    private DiffResponse combine(Map<String, FileInfo> files, Map<String,
            List<CommentInfo>> commentsA, Map<String, List<CommentInfo>> commentsB) {
        DiffResponse response = new DiffResponse();
        response.mFiles.clear();
        response.mCommentsA.clear();
        response.mCommentsB.clear();

        response.mFiles.putAll(files);
        if (commentsA != null) {
            response.mCommentsA.putAll(commentsA);
        }
        if (commentsB != null) {
            response.mCommentsB.putAll(commentsB);
        }
        return response;
    }

    @Override
    public String[] getPages() {
        return mFiles;
    }

    @Override
    public Fragment getFragment(int position) {
        final String fileId = mFiles[position];
        final int base = mResponse == null ? 0 : mBase;
        return FileDiffViewerFragment.newInstance(mLegacyChangeId, mRevisionId, fileId,
                base, getCommentsA(fileId), getCommentsB(fileId));
    }

    @Override
    public boolean isSwipeable() {
        return false;
    }

    @Override
    public SwipeableViewPager getViewPager() {
        return mBinding.viewPager;
    }

    @Override
    public int getMode() {
        return MODE_NAVIGATION;
    }

    @Override
    public int getOffscreenPageLimit() {
        return 3;
    }

    @Override
    public CharSequence getPage(int position) {
        if (mFiles == null) {
            return null;
        }
        return new File(mFiles[position]).getName();
    }

    private List<CommentInfo> getCommentsA(String fileId) {
        if (mResponse == null) {
            return null;
        }
        return mResponse.mCommentsA.get(fileId);
    }

    private List<CommentInfo> getCommentsB(String fileId) {
        if (mResponse == null) {
            return null;
        }
        return mResponse.mCommentsB.get(fileId);
    }

    private void showProgress(boolean show) {
        BaseActivity activity = (BaseActivity) getActivity();
        if (show) {
            activity.onRefreshStart();
        } else {
            activity.onRefreshEnd(null);
        }
    }
}
