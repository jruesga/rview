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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.gson.reflect.TypeToken;
import com.ruesga.rview.BaseActivity;
import com.ruesga.rview.R;
import com.ruesga.rview.annotations.ProguardIgnored;
import com.ruesga.rview.databinding.FileDiffViewerFragmentBinding;
import com.ruesga.rview.gerrit.GerritApi;
import com.ruesga.rview.gerrit.filter.Option;
import com.ruesga.rview.gerrit.model.CommentInfo;
import com.ruesga.rview.gerrit.model.DiffInfo;
import com.ruesga.rview.misc.ModelHelper;
import com.ruesga.rview.misc.SerializationManager;
import com.ruesga.rview.preferences.Constants;
import com.ruesga.rview.widget.DiffView;

import java.lang.reflect.Type;
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
    }

    private final RxLoaderObserver<FileDiffResponse> mObserver = new RxLoaderObserver<FileDiffResponse>() {
        @Override
        public void onNext(FileDiffResponse response) {
            mHandler.postDelayed(() ->
                    mBinding.diff
                        .from(response.diff.content)
                        .with(response.comments)
                        .mode(DiffView.UNIFIED_MODE)
                        .wrap(true)
                        .update(),
                    250L);
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

    private FileDiffViewerFragmentBinding mBinding;
    private EventHandlers mEventHandlers;
    private Handler mHandler;

    private RxLoader<FileDiffResponse> mLoader;

    private int mLegacyChangeId;
    private String mRevisionId;
    private String mFile;
    private Integer mBase;
    private List<CommentInfo> mCommentsA;
    private List<CommentInfo> mCommentsB;

    public static FileDiffViewerFragment newInstance(
            int legacyChangeId, String revisionId, String file, int base,
            List<CommentInfo> commentsA, List<CommentInfo> commentsB) {
        FileDiffViewerFragment fragment = new FileDiffViewerFragment();
        Bundle arguments = new Bundle();
        arguments.putInt(Constants.EXTRA_LEGACY_CHANGE_ID, legacyChangeId);
        arguments.putString(Constants.EXTRA_REVISION_ID, revisionId);
        arguments.putString(Constants.EXTRA_FILE, file);
        arguments.putInt(Constants.EXTRA_BASE, base);
        if (commentsA != null) {
            arguments.putString("comments_a", SerializationManager.getInstance().toJson(commentsA));
        }
        if (commentsB != null) {
            arguments.putString("comments_b", SerializationManager.getInstance().toJson(commentsB));
        }
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new Handler();

        mLegacyChangeId = getArguments().getInt(
                Constants.EXTRA_LEGACY_CHANGE_ID, Constants.INVALID_CHANGE_ID);
        mRevisionId = getArguments().getString(Constants.EXTRA_REVISION_ID);
        mFile = getArguments().getString(Constants.EXTRA_FILE);

        String commentsA = getArguments().getString("comments_a");
        String commentsB = getArguments().getString("comments_b");
        Type type = new TypeToken<List<CommentInfo>>(){}.getType();
        if (commentsA != null) {
            mCommentsA = SerializationManager.getInstance().fromJson(commentsA, type);
        }
        if (commentsB != null) {
            mCommentsB = SerializationManager.getInstance().fromJson(commentsB, type);
        }

        int base = getArguments().getInt(Constants.EXTRA_BASE, 0);
        mBase = base == 0 ? null : base;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(
                inflater, R.layout.file_diff_viewer_fragment, container, false);
        setupLoader();
        return mBinding.getRoot();
    }

    private void setupLoader() {
        if (mLoader == null) {
            mEventHandlers = new EventHandlers(this);

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
                        String.valueOf(mLegacyChangeId),
                        mRevisionId,
                        mFile,
                        mBase,
                        Option.INSTANCE,
                        null),
                Observable.just(mCommentsA),
                Observable.just(mCommentsB),
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
