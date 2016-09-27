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
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.SparseArray;
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

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
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

public class DiffViewerFragment extends Fragment {

    private static final String TAG = "DiffViewerFragment";

    public class FileDiffFragmentAdapter extends FragmentPagerAdapter {
        private final SparseArray<WeakReference<FileDiffViewerFragment>> mFragments
                = new SparseArray<>();

        private DiffResponse mResponse;

        public FileDiffFragmentAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            FileDiffViewerFragment fragment =
                    (FileDiffViewerFragment) super.instantiateItem(container, position);
            mFragments.put(position, new WeakReference<>(fragment));
            return fragment;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            super.destroyItem(container, position, object);
            mFragments.remove(position);
        }

        @Override
        public Fragment getItem(int position) {
            final String fileId = getFileAtPosition(position);
            return FileDiffViewerFragment.newInstance(mLegacyChangeId, mRevisionId, fileId, 0);
        }

        @Override
        public int getCount() {
            if (mResponse == null)  {
                return 0;
            }
            return mResponse.mFiles.size();
        }

        private String getFileAtPosition(int position) {
            if (mResponse == null) {
                return null;
            }
            return (String) mResponse.mFiles.keySet().toArray()[position];
        }
    }

    private static class DiffResponse {
        private Map<String, FileInfo> mFiles = new LinkedHashMap<>();
        private Map<String, List<CommentInfo>> mCommentsA = new LinkedHashMap<>();
        private Map<String, List<CommentInfo>> mCommentsB = new LinkedHashMap<>();
    }

    private final RxLoaderObserver<DiffResponse> mObserver = new RxLoaderObserver<DiffResponse>() {
        @Override
        public void onNext(DiffResponse response) {
            mAdapter.mResponse = response;
            mAdapter.notifyDataSetChanged();
            mBinding.viewPager.setCurrentItem(getFilePosition(response), false);
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
    };

    private DiffViewerFragmentBinding mBinding;
    private FileDiffFragmentAdapter mAdapter;

    private int mLegacyChangeId;
    private String mRevisionId;
    private String mFileId;
    private ArrayList<String> mRevisions;
    private int mBaseA = 0;
    private int mBaseB = 1;

    private RxLoader<DiffResponse> mLoader;
    
    public static DiffViewerFragment newInstance(
            int legacyChangeId, String revisionId, String fileId, ArrayList<String> revisions) {
        DiffViewerFragment fragment = new DiffViewerFragment();
        Bundle arguments = new Bundle();
        arguments.putInt(Constants.EXTRA_LEGACY_CHANGE_ID, legacyChangeId);
        arguments.putString(Constants.EXTRA_REVISION_ID, revisionId);
        arguments.putString(Constants.EXTRA_FILE_ID, fileId);
        arguments.putStringArrayList(Constants.EXTRA_REVISIONS, revisions);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLegacyChangeId = getArguments().getInt(
                Constants.EXTRA_LEGACY_CHANGE_ID, Constants.INVALID_CHANGE_ID);
        mRevisionId = getArguments().getString(Constants.EXTRA_REVISION_ID);
        mFileId = getArguments().getString(Constants.EXTRA_FILE_ID);
        mRevisions = getArguments().getStringArrayList(Constants.EXTRA_REVISIONS);
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

    private void startLoadersWithValidContext() {
        if (getActivity() == null) {
            return;
        }

        if (mLoader == null) {
            mAdapter = new FileDiffFragmentAdapter(getChildFragmentManager());
            mBinding.viewPager.setSwipeable(true);
            mBinding.viewPager.setOffscreenPageLimit(3);
            mBinding.viewPager.setAdapter(mAdapter);
            mBinding.viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                @Override
                public void onPageScrolled(int position, float offset, int offsetPixels) {
                }

                @Override
                public void onPageScrollStateChanged(int state) {
                }

                @Override
                public void onPageSelected(int position) {
                    updateStatusBar(mAdapter.getFileAtPosition(position));
                }
            });

            // Fetch or join current loader
            RxLoaderManager loaderManager = RxLoaderManagerCompat.get(this);
            mLoader = loaderManager.create("diff-" + mLegacyChangeId, fetchFilesAndComments(),
                    mObserver).start();
        }
    }

    @Override
    public final void onDestroyView() {
        super.onDestroyView();
        mBinding.unbind();
    }


    @SuppressWarnings("ConstantConditions")
    private Observable<DiffResponse> fetchFilesAndComments() {
        final Context ctx = getActivity();
        final GerritApi api = ModelHelper.getGerritApi(ctx);
        return Observable.zip(
                    api.getChangeRevisionFiles(String.valueOf(mLegacyChangeId), mRevisionId),
                    Observable.fromCallable(() -> {
                        if (mBaseA != 0) {
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
        if (commentsA != null) {
            response.mCommentsB.putAll(commentsB);
        }
        return response;
    }

    private void showProgress(boolean show) {
        BaseActivity activity = (BaseActivity) getActivity();
        if (show) {
            activity.onRefreshStart();
        } else {
            activity.onRefreshEnd(null);
        }
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

    private void updateStatusBar(String fileId) {
        AppCompatActivity activity = ((AppCompatActivity)getActivity());
        if (activity.getSupportActionBar() != null) {
            activity.getSupportActionBar().setTitle(new File(fileId).getName());
            activity.getSupportActionBar().setSubtitle(
                    getString(R.string.change_details_title, mLegacyChangeId));
        }
    }
}
