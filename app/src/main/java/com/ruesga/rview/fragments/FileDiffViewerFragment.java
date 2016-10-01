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
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.util.Pair;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ruesga.rview.BaseActivity;
import com.ruesga.rview.R;
import com.ruesga.rview.annotations.ProguardIgnored;
import com.ruesga.rview.databinding.FileDiffViewerFragmentBinding;
import com.ruesga.rview.gerrit.GerritApi;
import com.ruesga.rview.gerrit.filter.Option;
import com.ruesga.rview.gerrit.model.ChangeInfo;
import com.ruesga.rview.gerrit.model.CommentInfo;
import com.ruesga.rview.gerrit.model.DiffInfo;
import com.ruesga.rview.misc.CacheHelper;
import com.ruesga.rview.misc.ModelHelper;
import com.ruesga.rview.misc.SerializationManager;
import com.ruesga.rview.preferences.Constants;

import java.io.IOException;
import java.util.List;

import me.tatarka.rxloader.RxLoader;
import me.tatarka.rxloader.RxLoaderManager;
import me.tatarka.rxloader.RxLoaderManagerCompat;
import me.tatarka.rxloader.RxLoaderObserver;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class FileDiffViewerFragment extends Fragment {

    private static final String TAG = "FileDiffViewerFragment";

    @ProguardIgnored
    @SuppressWarnings({"UnusedParameters", "unused"})
    public static class EventHandlers {
        private final FileDiffViewerFragment mFragment;

        public EventHandlers(FileDiffViewerFragment fragment) {
            mFragment = fragment;
        }
    }

    public static class FileDiffResponse {
        DiffInfo diff;
        Pair<List<CommentInfo>, List<CommentInfo>> comments;
        Pair<List<CommentInfo>, List<CommentInfo>> draftComments;
    }

    private final RxLoaderObserver<FileDiffResponse> mObserver
            = new RxLoaderObserver<FileDiffResponse>() {
        @Override
        public void onNext(FileDiffResponse response) {
            mResponse = response;
            update();
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

    private FileDiffResponse mResponse;
    private FileDiffViewerFragmentBinding mBinding;
    private EventHandlers mEventHandlers;
    private Handler mHandler;

    private RxLoader<FileDiffResponse> mLoader;

    private ChangeInfo mChange;

    private String mRevisionId;
    private String mFile;
    private Integer mBase;

    private int mDiffMode;
    private boolean mWrap;

    public static FileDiffViewerFragment newInstance(String revisionId, int base, String file) {
        FileDiffViewerFragment fragment = new FileDiffViewerFragment();
        Bundle arguments = new Bundle();
        arguments.putString(Constants.EXTRA_REVISION_ID, revisionId);
        arguments.putString(Constants.EXTRA_FILE, file);
        arguments.putInt(Constants.EXTRA_BASE, base);
        fragment.setArguments(arguments);
        return fragment;
    }

    public FileDiffViewerFragment mode(int mode) {
        mDiffMode = mode;
        return this;
    }

    public FileDiffViewerFragment wrap(boolean wrap) {
        mWrap = wrap;
        return this;
    }

    public void update() {
        if (mResponse != null) {
            mHandler.postDelayed(() ->
                mBinding.diff
                    .from(mResponse.diff.content)
                    .with(mResponse.comments)
                    .mode(mDiffMode)
                    .wrap(mWrap)
                    .update(),
            250L);
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new Handler();

        mRevisionId = getArguments().getString(Constants.EXTRA_REVISION_ID);
        mFile = getArguments().getString(Constants.EXTRA_FILE);
        int base = getArguments().getInt(Constants.EXTRA_BASE);
        mBase = base == 0 ? null : base;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(
                inflater, R.layout.file_diff_viewer_fragment, container, false);
        setupLoaderWithValidContext();
        return mBinding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setupLoaderWithValidContext();
    }

    private void setupLoaderWithValidContext() {
        if (getActivity() == null) {
            return;
        }

        if (mLoader == null) {
            mEventHandlers = new EventHandlers(this);

            try {
                // Deserialize the change
                mChange = SerializationManager.getInstance().fromJson(
                        new String(CacheHelper.readAccountDiffCacheDir(
                                getContext(), CacheHelper.CACHE_CHANGE_JSON)), ChangeInfo.class);

            } catch (IOException ex) {
                Log.e(TAG, "Failed to load change cached data", ex);
                getActivity().finish();
            }

            // Fetch or join current loader
            RxLoaderManager loaderManager = RxLoaderManagerCompat.get(this);
            mLoader = loaderManager.create(
                    "file-diff-" + hashCode(), fetchDiffs(), mObserver).start();
        }
    }

    @Override
    public final void onDestroyView() {
        super.onDestroyView();
        mBinding.unbind();
    }

    @SuppressWarnings("ConstantConditions")
    private Observable<FileDiffResponse> fetchDiffs() {
        final Context ctx = getActivity();
        final GerritApi api = ModelHelper.getGerritApi(ctx);
        return Observable.zip(
                api.getChangeRevisionFileDiff(
                        String.valueOf(mChange.legacyChangeId),
                        mRevisionId,
                        mFile,
                        mBase,
                        Option.INSTANCE,
                        null),
                Observable.just(null),
                Observable.just(null),
                this::combine
            )
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread());
    }

    private FileDiffResponse combine(DiffInfo diff,
                                     List<CommentInfo> commentsA, List<CommentInfo> commentsB) {
        FileDiffResponse response = new FileDiffResponse();
        response.diff = diff;
        response.comments = new Pair<>(commentsA, commentsB);
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
}
