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
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.ruesga.rview.BaseActivity;
import com.ruesga.rview.R;
import com.ruesga.rview.databinding.FileDiffViewerFragmentBinding;
import com.ruesga.rview.gerrit.GerritApi;
import com.ruesga.rview.gerrit.filter.ChangeQuery;
import com.ruesga.rview.gerrit.filter.Option;
import com.ruesga.rview.gerrit.model.ChangeInfo;
import com.ruesga.rview.gerrit.model.CommentInfo;
import com.ruesga.rview.gerrit.model.CommentInput;
import com.ruesga.rview.gerrit.model.ContextType;
import com.ruesga.rview.gerrit.model.DiffContentInfo;
import com.ruesga.rview.gerrit.model.DiffInfo;
import com.ruesga.rview.gerrit.model.FileStatus;
import com.ruesga.rview.gerrit.model.IgnoreWhitespaceType;
import com.ruesga.rview.gerrit.model.SideType;
import com.ruesga.rview.gerrit.model.WhitespaceType;
import com.ruesga.rview.misc.CacheHelper;
import com.ruesga.rview.misc.FowlerNollVo;
import com.ruesga.rview.misc.ModelHelper;
import com.ruesga.rview.misc.SerializationManager;
import com.ruesga.rview.model.Account;
import com.ruesga.rview.preferences.Constants;
import com.ruesga.rview.preferences.Preferences;
import com.ruesga.rview.tasks.AsyncImageDiffProcessor;
import com.ruesga.rview.widget.DiffView;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import me.tatarka.rxloader2.RxLoader;
import me.tatarka.rxloader2.RxLoader2;
import me.tatarka.rxloader2.RxLoaderManager;
import me.tatarka.rxloader2.RxLoaderManagerCompat;
import me.tatarka.rxloader2.RxLoaderObserver;
import me.tatarka.rxloader2.safe.SafeObservable;
import okhttp3.ResponseBody;

public class FileDiffViewerFragment extends Fragment {

    private static final String TAG = "FileDiffViewerFragment";

    public interface OnDiffCompleteListener {
        void onDiffComplete(boolean isBinary, boolean hasImagePreview);
        void onNewDraftCreated(String revision, String draftId);
        void onDraftUpdated(String revision, String draftId);
        void onDraftDeleted(String revision, String draftId);
    }

    public static class FileDiffResponse {
        DiffInfo diff;
        Pair<List<CommentInfo>, List<CommentInfo>> comments;
        Pair<List<CommentInfo>, List<CommentInfo>> draftComments;

        File leftContent;
        File rightContent;
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

    private final RxLoaderObserver<Object> mActionObserver = new RxLoaderObserver<Object>() {
        @Override
        public void onNext(Object value) {
            // Force refresh
            mDraftsLoader.clear();
            mDraftsLoader.restart();
        }

        @Override
        public void onError(Throwable error) {
            ((BaseActivity) getActivity()).handleException(TAG, error);
        }
    };

    private final RxLoaderObserver<Boolean> mReviewedObserver
            = new RxLoaderObserver<Boolean>() {
        @Override
        public void onNext(Boolean result) {
        }
    };

    private DiffView.OnCommentListener mCommentListener = new DiffView.OnCommentListener() {
        @Override
        public void onNewDraft(View v, boolean left, Integer line) {
            final String baseRevision = mBase == null ? "0" : mBase;
            String rev = left ? baseRevision : mRevision;
            performShowDraftMessageDialog(v, line, left, null,
                    newValue -> {
                        mActionLoader.clear();
                        mActionLoader.restart(ModelHelper.ACTION_CREATE_DRAFT,
                            new String[]{rev, null,
                                    line == null ? null : String.valueOf(line), newValue});
                    });
        }

        @Override
        public void onReply(View v, String revisionId, String commentId, Integer line) {
            boolean left = !(revisionId.equals(mRevision));
            performShowDraftMessageDialog(v, line, left, null,
                    newValue -> {
                        mActionLoader.clear();
                        mActionLoader.restart(ModelHelper.ACTION_CREATE_DRAFT,
                            new String[]{revisionId, commentId,
                                    line == null ? null : String.valueOf(line), newValue}) ;
                    });
        }

        @Override
        public void onDone(View v, String revisionId, String commentId, Integer line) {
            String msg = getString(R.string.draft_reply_done);
            mActionLoader.clear();
            mActionLoader.restart(ModelHelper.ACTION_CREATE_DRAFT,
                    new String[]{revisionId, commentId,
                            line == null ? null : String.valueOf(line), msg});
        }

        @Override
        public void onEditDraft(View v, String revisionId, String draftId,
                String inReplyTo, Integer line, String msg) {
            boolean left = !(revisionId.equals(mRevision));
            performShowDraftMessageDialog(v, line, left, msg,
                    newValue -> {
                        mActionLoader.clear();
                        mActionLoader.restart(ModelHelper.ACTION_UPDATE_DRAFT,
                            new String[]{revisionId, draftId, inReplyTo,
                                    line == null ? null : String.valueOf(line), newValue});
                    });
        }

        @Override
        public void onDeleteDraft(View v, String revisionId, String draftId) {
            mActionLoader.clear();
            mActionLoader.restart(ModelHelper.ACTION_DELETE_DRAFT,
                    new String[]{revisionId, draftId});
        }
    };

    private FileDiffResponse mResponse;
    private FileDiffViewerFragmentBinding mBinding;
    private Handler mHandler;

    private RxLoader<FileDiffResponse> mLoader;
    private RxLoader2<String, String[], Object> mActionLoader;
    private RxLoader<Boolean> mReviewedLoader;
    private RxLoader<FileDiffResponse> mDraftsLoader;

    private ChangeInfo mChange;

    private String mRevisionId;
    private String mFile;
    private String mComment;
    private String mFileHash;
    private String mBase;
    private String mRevision;

    private Account mAccount;

    private int mMode;
    private boolean mWrap;
    private float mTextSizeFactor;
    private boolean mHighlightTabs;
    private boolean mHighlightTrailingWhitespaces;
    private boolean mHighlightIntralineDiffs;
    private int mScrollToPosition = -1;

    public static FileDiffViewerFragment newInstance(String revisionId, String file,
            String comment, int base, int revision, int mode, boolean wrap, float textSizeFactor,
            boolean highlightTabs, boolean highlightTrailingWhitespaces,
            boolean highlightIntralineDiffs, int scrollToPosition) {
        FileDiffViewerFragment fragment = new FileDiffViewerFragment();
        Bundle arguments = new Bundle();
        arguments.putString(Constants.EXTRA_REVISION_ID, revisionId);
        arguments.putString(Constants.EXTRA_FILE, file);
        if (comment != null) {
            arguments.putString(Constants.EXTRA_COMMENT, comment);
        }
        arguments.putInt(Constants.EXTRA_BASE, base);
        arguments.putInt(Constants.EXTRA_REVISION, revision);
        arguments.putInt("mode", mode);
        arguments.putBoolean("wrap", wrap);
        arguments.putFloat("textSizeFactor", textSizeFactor);
        arguments.putBoolean("highlight_tabs", highlightTabs);
        arguments.putBoolean("highlight_trailing_whitespaces", highlightTrailingWhitespaces);
        arguments.putBoolean("highlight_intraline_diffs", highlightIntralineDiffs);
        arguments.putInt("scrollToPosition", scrollToPosition);
        fragment.setArguments(arguments);
        return fragment;
    }

    private void update() {
        if (mResponse != null) {
            // Only refresh the diff view if we can display text differences
            mHandler.postDelayed(() -> {
                // Check if activity is still attached
                if (getActivity() == null ) {
                    return;
                }

                DiffView v = mBinding.diff
                    .file(mFile)
                    .from(mResponse.diff)
                    .withLeftContent(mResponse.leftContent)
                    .withRightContent(mResponse.rightContent)
                    .withComments(mResponse.comments)
                    .withDrafts(mResponse.draftComments)
                    .mode(mMode)
                    .textSizeFactor(mTextSizeFactor)
                    .wrap(mWrap)
                    .listenOn(mCommentListener)
                    .canEdit(mAccount.hasAuthenticatedAccessMode())
                    .highlightTabs(mHighlightTabs)
                    .highlightTrailingWhitespaces(mHighlightTrailingWhitespaces)
                    .highlightIntralineDiffs(mHighlightIntralineDiffs);
                if (mComment != null) {
                    v.scrollToComment(mComment);
                } else if (mScrollToPosition != -1) {
                    v.scrollToPosition(mScrollToPosition);
                }
                v.update();
            },
            250L);
        }
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new Handler();

        mRevisionId = getArguments().getString(Constants.EXTRA_REVISION_ID);
        mFile = getArguments().getString(Constants.EXTRA_FILE);
        mFileHash = FowlerNollVo.fnv1a_64(mFile.getBytes()).toString();
        mComment = getArguments().getString(Constants.EXTRA_COMMENT);
        int base = getArguments().getInt(Constants.EXTRA_BASE);
        mBase = base == 0 ? null : String.valueOf(base);
        mRevision = String.valueOf(getArguments().getInt(Constants.EXTRA_REVISION));
        mMode = getArguments().getInt("mode");
        mWrap = getArguments().getBoolean("wrap");
        mTextSizeFactor = getArguments().getFloat("textSizeFactor");
        mHighlightTabs = getArguments().getBoolean("highlight_tabs");
        mHighlightTrailingWhitespaces = getArguments().getBoolean("highlight_trailing_whitespaces");
        mHighlightIntralineDiffs = getArguments().getBoolean("highlight_intraline_diffs");
        mScrollToPosition = getArguments().getInt("scrollToPosition", -1);
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
            mAccount = Preferences.getAccount(getContext());

            try {
                // Deserialize the change
                mChange = SerializationManager.getInstance().fromJson(
                        new String(CacheHelper.readAccountDiffCacheFile(
                                getContext(), CacheHelper.CACHE_CHANGE_JSON)), ChangeInfo.class);
                if (mChange == null) {
                    Log.e(TAG, "Change cached data is null. Exiting...");
                    Toast.makeText(getContext(),
                            R.string.exception_item_not_found, Toast.LENGTH_SHORT).show();
                    getActivity().finish();
                    return;
                }
                if (mChange.revisions == null) {
                    Log.e(TAG, "Change has no revisions. Exiting...");
                    Toast.makeText(getContext(),
                            R.string.exception_item_not_found, Toast.LENGTH_SHORT).show();
                    getActivity().finish();
                    return;
                }

            } catch (IOException ex) {
                Log.e(TAG, "Failed to load change cached data", ex);
                Toast.makeText(getContext(),
                        R.string.exception_item_not_found, Toast.LENGTH_SHORT).show();
                getActivity().finish();
                return;
            }

            // Fetch or join current loader
            RxLoaderManager loaderManager = RxLoaderManagerCompat.get(this);
            mActionLoader = loaderManager.create(
                    "file-diff-action" + hashCode(), this::doAction, mActionObserver);
            mReviewedLoader = loaderManager.create(
                    "file-reviewed" + hashCode(), performReviewedStatus(), mReviewedObserver);
            mDraftsLoader = loaderManager.create(
                    "file-drafts" + hashCode(), fetchDrafts(), mObserver);
            mLoader = loaderManager.create("file-diff-" + hashCode(), fetchDiffs(), mObserver);
            mLoader.start();
        }
    }

    @Override
    public final void onDestroyView() {
        super.onDestroyView();
        mBinding.unbind();
    }

    @SuppressWarnings("ConstantConditions")
    private Observable<FileDiffResponse> fetchDiffs() {
        // Generate ids
        final String baseRevision = mBase == null ? "0" : mBase;
        final String diffCacheId = baseRevision + "_" + mRevision + "_" + mFileHash + "_";
        final Integer base = mBase == null ? null : Integer.valueOf(mBase);
        final Type commentType = new TypeToken<Map<String, List<CommentInfo>>>(){}.getType();
        final boolean isBinary = !mFile.equals(Constants.COMMIT_MESSAGE)
                && mChange.revisions.get(mRevisionId).files.get(mFile) != null
                && mChange.revisions.get(mRevisionId).files.get(mFile).binary;
        final boolean isSameBase =
                (base != null && base == mChange.revisions.get(mRevisionId).number);

        final Context ctx = getActivity();
        final GerritApi api = ModelHelper.getGerritApi(ctx);
        return Observable.zip(
                withCached(
                        SafeObservable.fromNullCallable(() -> {
                            if (!isBinary) {
                                boolean rectify = (isSameBase
                                        && mFile.equals(Constants.COMMIT_MESSAGE));

                                final Integer b = rectify ? null : base;
                                DiffInfo diff =
                                        api.getChangeRevisionFileDiff(
                                            String.valueOf(mChange.legacyChangeId),
                                            mRevisionId,
                                            mFile,
                                            b,
                                            Option.INSTANCE,
                                            null,
                                            WhitespaceType.IGNORE_NONE,
                                            IgnoreWhitespaceType.NONE,
                                            ContextType.ALL)
                                            .blockingFirst();
                                if (rectify) {
                                    // Server doesn't return any content diff, so just ensure
                                    // the return structure has the proper data
                                    diff.content[0].ab = diff.content[0].b;
                                    diff.content[0].b = null;
                                }
                                return diff;
                            }

                            DiffInfo diff = new DiffInfo();
                            diff.binary = true;
                            return diff;
                        }),
                        DiffInfo.class,
                        diffCacheId + CacheHelper.CACHE_DIFF_JSON
                ),
                withCached(
                        SafeObservable.fromNullCallable(() -> {
                            if (!isBinary && mBase != null) {
                                return api.getChangeRevisionComments(
                                        String.valueOf(mChange.legacyChangeId), baseRevision)
                                        .blockingFirst();
                            }
                            return new HashMap<>();
                        }),
                        commentType,
                        baseRevision + "_" + CacheHelper.CACHE_COMMENTS_JSON),
                withCached(
                        SafeObservable.fromNullCallable(() -> {
                            if (!isBinary) {
                                return api.getChangeRevisionComments(
                                        String.valueOf(mChange.legacyChangeId), mRevisionId)
                                        .blockingFirst();
                            }
                            return new HashMap<>();
                        }),
                        commentType,
                        mRevision + "_" + CacheHelper.CACHE_COMMENTS_JSON),
                withCached(
                        SafeObservable.fromNullCallable(() -> {
                            // Do no fetch drafts if the account is not authenticated
                            if (!isBinary && mBase != null && mAccount.hasAuthenticatedAccessMode()) {
                                return api.getChangeRevisionDrafts(
                                        String.valueOf(mChange.legacyChangeId), baseRevision)
                                        .blockingFirst();
                            }
                            return new HashMap<>();
                        }),
                        commentType,
                        baseRevision + "_" + CacheHelper.CACHE_DRAFT_JSON),
                withCached(
                        SafeObservable.fromNullCallable(() -> {
                            // Do no fetch drafts if the account is not authenticated
                            if (!isBinary && mAccount.hasAuthenticatedAccessMode()) {
                                return api.getChangeRevisionDrafts(
                                        String.valueOf(mChange.legacyChangeId), mRevisionId)
                                        .blockingFirst();
                            }
                            return new HashMap<>();
                        }),
                        commentType,
                        mRevision + "_" + CacheHelper.CACHE_DRAFT_JSON),
                this::combine
            )
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread());
    }

    @SuppressWarnings("ConstantConditions")
    private Observable<FileDiffResponse> fetchDrafts() {
        // Generate ids
        final String baseRevision = mBase == null ? "0" : mBase;
        final boolean isBinary = !mFile.equals(Constants.COMMIT_MESSAGE)
                && mChange.revisions.get(mRevisionId).files.get(mFile) != null
                && mChange.revisions.get(mRevisionId).files.get(mFile).binary;

        final Context ctx = getActivity();
        final GerritApi api = ModelHelper.getGerritApi(ctx);
        return Observable.zip(
                    SafeObservable.fromNullCallable(() -> {
                        // Do no fetch drafts if the account is not authenticated
                        if (!isBinary && mBase != null && mAccount.hasAuthenticatedAccessMode()) {
                            return api.getChangeRevisionDrafts(
                                    String.valueOf(mChange.legacyChangeId), baseRevision)
                                    .blockingFirst();
                        }
                        return new HashMap<>();
                    }),
                    SafeObservable.fromNullCallable(() -> {
                        // Do no fetch drafts if the account is not authenticated
                        if (!isBinary && mAccount.hasAuthenticatedAccessMode()) {
                            return api.getChangeRevisionDrafts(
                                    String.valueOf(mChange.legacyChangeId), mRevisionId)
                                    .blockingFirst();
                        }
                        return new HashMap<>();
                    }),
                    this::combineDrafts
                )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    @SuppressWarnings("ConstantConditions")
    private Observable<Boolean> performReviewedStatus() {
        final Context ctx = getActivity();
        final GerritApi api = ModelHelper.getGerritApi(ctx);
        return SafeObservable.fromNullCallable(() -> {
                    api.setChangeRevisionFileAsReviewed(
                                String.valueOf(mChange.legacyChangeId), mRevisionId, mFile
                            ).blockingFirst();
                    return true;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    private FileDiffResponse combine(DiffInfo diff, Map<String, List<CommentInfo>> commentsA,
            Map<String, List<CommentInfo>> commentsB, Map<String, List<CommentInfo>> draftsA,
            Map<String, List<CommentInfo>> draftsB) {
        int base = mBase == null ? 0 : Integer.parseInt(mBase);
        int revision = Integer.parseInt(mRevision);

        FileDiffResponse response = new FileDiffResponse();
        response.diff = diff;
        response.comments = new Pair<>(
                mapCommentsToList(commentsA, base == 0 ? commentsB : null, base, base, true),
                mapCommentsToList(commentsB, null, revision, base, false));
        response.draftComments = new Pair<>(
                mapCommentsToList(draftsA, base == 0 ? draftsB : null, base, base, true),
                mapCommentsToList(draftsB, null, revision, base, false));

        // Cache the fetched data
        try {
            final String baseRevision = mBase == null ? "0" : mBase;
            final String diffCacheId = baseRevision + "_" + mRevision + "_" + mFileHash + "_";

            CacheHelper.writeAccountDiffCacheFile(getContext(),
                    diffCacheId + CacheHelper.CACHE_DIFF_JSON,
                    SerializationManager.getInstance().toJson(diff).getBytes());
            CacheHelper.writeAccountDiffCacheFile(getContext(),
                    baseRevision + "_" + CacheHelper.CACHE_COMMENTS_JSON,
                    SerializationManager.getInstance().toJson(commentsA).getBytes());
            CacheHelper.writeAccountDiffCacheFile(getContext(),
                    mRevision + "_" + CacheHelper.CACHE_COMMENTS_JSON,
                    SerializationManager.getInstance().toJson(commentsB).getBytes());
            CacheHelper.writeAccountDiffCacheFile(getContext(),
                    baseRevision + "_" + CacheHelper.CACHE_DRAFT_JSON,
                    SerializationManager.getInstance().toJson(draftsA).getBytes());
            CacheHelper.writeAccountDiffCacheFile(getContext(),
                    mRevision + "_" + CacheHelper.CACHE_DRAFT_JSON,
                    SerializationManager.getInstance().toJson(draftsB).getBytes());
        } catch (IOException ex) {
            Log.e(TAG, "Failed to save diff cached data", ex);
        }

        // We do need to download the file content
        fetchRevisionsContentIfNeeded(response);

        // Change mode and options according to the type of supported diffs
        boolean hasImagePreview = AsyncImageDiffProcessor.hasImagePreview(
                new File(mFile), response.rightContent);
        if (getParentFragment() != null && getParentFragment() instanceof OnDiffCompleteListener) {
            ((OnDiffCompleteListener) getParentFragment()).onDiffComplete(
                    response.diff.binary, hasImagePreview);
        }
        int mode = mMode;
        applyModeRestrictions(response.diff.binary, hasImagePreview);
        if (mode != mMode) {
            // We need to re-fetch the content in case, we changed from text to image diff mode.
            // Nothing is fetched if it doesn't really needed
            fetchRevisionsContentIfNeeded(response);
        }

        // Mark the file as reviewed
        if (mAccount.hasAuthenticatedAccessMode()) {
            mReviewedLoader.clear();
            mReviewedLoader.restart();
        }

        return response;
    }

    private FileDiffResponse combineDrafts(
            Map<String, List<CommentInfo>> draftsA, Map<String, List<CommentInfo>> draftsB) {

        int base = mBase == null ? 0 : Integer.parseInt(mBase);
        int revision = Integer.parseInt(mRevision);

        mResponse.draftComments = new Pair<>(
                mapCommentsToList(draftsA, base == 0 ? draftsB : null, base, base, true),
                mapCommentsToList(draftsB, null, revision, base, false));

        // Cache the fetched data
        try {
            final String baseRevision = mBase == null ? "0" : mBase;

            CacheHelper.writeAccountDiffCacheFile(getContext(),
                    baseRevision + "_" + CacheHelper.CACHE_DRAFT_JSON,
                    SerializationManager.getInstance().toJson(draftsA).getBytes());
            CacheHelper.writeAccountDiffCacheFile(getContext(),
                    mRevision + "_" + CacheHelper.CACHE_DRAFT_JSON,
                    SerializationManager.getInstance().toJson(draftsB).getBytes());
        } catch (IOException ex) {
            Log.e(TAG, "Failed to save drafts cached data", ex);
        }

        return mResponse;
    }

    private List<CommentInfo> mapCommentsToList(Map<String, List<CommentInfo>> comments,
            Map<String, List<CommentInfo>> otherSideComments, int base,
            int parentBase, boolean isA) {
        List<CommentInfo> commentList = comments != null ? comments.get(mFile) : null;
        List<CommentInfo> otherCommentList =
                otherSideComments != null ? otherSideComments.get(mFile) : null;
        if (commentList == null) {
            commentList = new ArrayList<>();
        }

        List<CommentInfo> copy = new ArrayList<>();
        for (CommentInfo comment : commentList) {
            if (comment.side == null && base != parentBase && !isA) {
                comment.side = SideType.REVISION;
            } else if (SideType.REVISION.equals(comment.side) && isA) {
                comment.side = null;
            }

            if (otherCommentList == null && SideType.PARENT.equals(comment.side)) {
                continue;
            }
            comment.patchSet = SideType.PARENT.equals(comment.side) ? parentBase : base;
            copy.add(comment);
        }
        if (base == 0 && otherCommentList != null) {
            //noinspection Convert2streamapi
            for (CommentInfo comment : otherCommentList) {
                if (SideType.PARENT.equals(comment.side)) {
                    comment.patchSet = SideType.PARENT.equals(comment.side) ? parentBase : base;
                    copy.add(comment);
                }
            }
        }
        Collections.sort(copy,
                (CommentInfo lhs, CommentInfo rhs) -> lhs.updated.compareTo(rhs.updated));
        return copy;
    }

    @SuppressWarnings("ConstantConditions")
    private Observable<Object> doAction(final String action, final String[] params) {
        final Context ctx = getActivity();
        final GerritApi api = ModelHelper.getGerritApi(ctx);
        return SafeObservable.fromNullCallable(() -> {
                    switch (action) {
                        case ModelHelper.ACTION_CREATE_DRAFT:
                            Integer line = params[2] == null ? null : Integer.valueOf(params[2]);
                            return performCreateDraft(api,
                                    params[0], params[1], line, params[3]);
                        case ModelHelper.ACTION_UPDATE_DRAFT:
                            line = params[3] == null ? null : Integer.valueOf(params[3]);
                            return performUpdateDraft(api, params[0], params[1], params[2],
                                    line, params[4]);
                        case ModelHelper.ACTION_DELETE_DRAFT:
                            performDeleteDraft(api, params[0], params[1]);
                            break;
                    }
                    return Boolean.TRUE;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    private void performShowDraftMessageDialog(View v, Integer line, boolean isLeftSide,
            String comment, EditDialogFragment.OnEditChanged cb) {
        final String side = isLeftSide
                ? getString(R.string.options_base_left)
                : getString(R.string.options_base_right);
        final String subtitle;
        if (line == null) {
            // File comment
            subtitle = getString(R.string.draft_file_comment, side);
        } else {
            // Line comment
            subtitle = getString(R.string.draft_line_comment, line, side);
        }

        EditDialogFragment fragment = EditDialogFragment.newInstance(
                getString(R.string.draft_title), subtitle, comment,
                    getString(R.string.action_save), getString(R.string.draft_hint), false, true, true, v);
        fragment.setOnEditChanged(cb);
        fragment.show(getChildFragmentManager(), EditDialogFragment.TAG);
    }

    private CommentInfo performCreateDraft(GerritApi api, String revision,
            String commentId, Integer line, String msg) {
        int base = Integer.parseInt(revision);
        SideType side = base == 0 ? SideType.PARENT : SideType.REVISION;
        String rev = base == 0 ? mRevisionId : revision;

        CommentInput input = new CommentInput();
        if (!TextUtils.isEmpty(commentId)) {
            input.inReplyTo = commentId;
        }
        input.message = msg;
        input.message = msg;
        if (line != null) {
            input.line = line;
        }
        input.path = mFile;
        input.side = side;
        CommentInfo comment = api.createChangeRevisionDraft(
                String.valueOf(mChange.legacyChangeId), rev, input).blockingFirst();

        if (getParentFragment() != null && getParentFragment() instanceof OnDiffCompleteListener) {
            ((OnDiffCompleteListener) getParentFragment()).onNewDraftCreated(revision, comment.id);
        }

        return comment;
    }

    private CommentInfo performUpdateDraft(GerritApi api, String revision,
            String draftId, String inReplyTo, Integer line, String msg) {
        int base = Integer.parseInt(revision);
        SideType side = base == 0 ? SideType.PARENT : SideType.REVISION;
        String rev = base == 0 ? mRevisionId : revision;

        CommentInput input = new CommentInput();
        input.id = draftId;
        input.inReplyTo = inReplyTo;
        input.message = msg;
        if (line != null) {
            input.line = line;
        }
        input.path = mFile;
        input.side = side;
        CommentInfo comment = api.updateChangeRevisionDraft(
                String.valueOf(mChange.legacyChangeId), rev, draftId, input).blockingFirst();

        if (getParentFragment() != null && getParentFragment() instanceof OnDiffCompleteListener) {
            ((OnDiffCompleteListener) getParentFragment()).onDraftUpdated(revision, draftId);
        }

        return comment;
    }

    private void performDeleteDraft(GerritApi api, String revision, String draftId) {
        int base = Integer.parseInt(revision);
        String rev = base == 0 ? mRevisionId : revision;
        api.deleteChangeRevisionDraft(String.valueOf(mChange.legacyChangeId), rev, draftId)
                .blockingFirst();

        if (getParentFragment() != null && getParentFragment() instanceof OnDiffCompleteListener) {
            ((OnDiffCompleteListener) getParentFragment()).onDraftDeleted(revision, draftId);
        }
    }

    private void showProgress(boolean show) {
        BaseActivity activity = (BaseActivity) getActivity();
        if (show) {
            activity.onRefreshStart(this);
        } else {
            activity.onRefreshEnd(this, null);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> Observable<T> withCached(Observable<T> call, Type type, String name) {
        try {
            if (CacheHelper.hasAccountDiffCache(getContext(), name)) {
                T o = SerializationManager.getInstance().fromJson(
                        new String(CacheHelper.readAccountDiffCacheFile(
                                getContext(), name)), type);
                return Observable.just((o));
            }
        } catch (IOException ex) {
            Log.e(TAG, "Failed to load diff cached data: " + name, ex);
        } catch (JsonParseException ex) {
            Log.e(TAG, "Failed to parse diff cached data: " + name, ex);
        }
        return call;
    }

    private void fetchRevisionsContentIfNeeded(FileDiffResponse response) {
        FileStatus fileStatus = FileStatus.A;
        if (mChange.revisions.get(mRevisionId).files.containsKey(mFile)) {
            fileStatus = mChange.revisions.get(mRevisionId).files.get(mFile).status;
        }

        // If is not a binary file, we can use the diff information to build the file
        // instead of fetch it from the network
        if (!response.diff.binary) {
            if (!fileStatus.equals(FileStatus.A)) {
                response.leftContent = writeCachedContent(response, mBase, true);
            }
            if (!fileStatus.equals(FileStatus.D)) {
                response.rightContent = writeCachedContent(response, mRevision, false);
            }
            return;
        }

        if (mMode == DiffView.IMAGE_MODE) {
            final String baseRevision = mBase == null ? "0" : mBase;

            // Base revision
            if (!fileStatus.equals(FileStatus.A)) {
                if (baseRevision.equals("0")) {
                    // Base file is only available from the parent commit, so we need to
                    // fetch by commit to recover the parent id, and then fetch the revision
                    // to download the file
                    String parentRevision =
                            mChange.revisions.get(mRevisionId).commit.parents[0].commit;
                    ChangeInfo parent = fetchParentChange(parentRevision);
                    if (parent != null) {
                        response.leftContent = fetchCachedContent(
                                String.valueOf(parent.legacyChangeId), parentRevision, baseRevision);
                    } else {
                        response.leftContent = null;
                    }
                } else {
                    response.leftContent = fetchCachedContent(
                            String.valueOf(mChange.legacyChangeId), baseRevision, baseRevision);
                }
            }

            // Current revision
            if (!fileStatus.equals(FileStatus.D)) {
                response.rightContent = fetchCachedContent(
                        String.valueOf(mChange.legacyChangeId), mRevisionId, mRevision);
            }
        }
    }

    @SuppressWarnings("ConstantConditions")
    private File fetchCachedContent(String changeId, String revision, String base) {
        String name = base + "_" + mFileHash + "_" + CacheHelper.CACHE_CONTENT;
        File fetchedFile = new File(CacheHelper.getAccountDiffCacheDir(getContext()), name);
        if (!fetchedFile.exists()) {
            try {
                final Context ctx = getActivity();
                final GerritApi api = ModelHelper.getGerritApi(ctx);
                ResponseBody content = api.getChangeRevisionFileContent(
                        changeId,
                        revision,
                        mFile)
                        .blockingFirst();
                CacheHelper.writeAccountDiffCacheFile(
                        getContext(),
                        mAccount,
                        name,
                        Base64.decode(content.bytes(), Base64.NO_WRAP));
            } catch (Exception ex) {
                Log.e(TAG, "Can't download file content " + mFile + "; Revision: " + revision, ex);
                fetchedFile = null;
            }
        }
        return fetchedFile;
    }

    @SuppressWarnings("ConstantConditions")
    private File writeCachedContent(FileDiffResponse response, String base, boolean isA) {
        String name = base + "_" + mFileHash + "_" + CacheHelper.CACHE_CONTENT;
        File writtenFile = new File(CacheHelper.getAccountDiffCacheDir(getContext()), name);
        if (!writtenFile.exists()) {
            if (writeDiffToFile(response, writtenFile, isA)) {
                return writtenFile;
            }
            return null;
        }
        return writtenFile;
    }

    @SuppressWarnings("ConstantConditions")
    private ChangeInfo fetchParentChange(String parentRevision) {
        try {
            String name = parentRevision + "_" + CacheHelper.CACHE_PARENT;
            File parent = new File(CacheHelper.getAccountDiffCacheDir(getContext()), name);
            if (!parent.exists()) {
                // Fetch the change
                final Context ctx = getActivity();
                final GerritApi api = ModelHelper.getGerritApi(ctx);
                ChangeQuery query = new ChangeQuery().commit(parentRevision);
                List<ChangeInfo> changes = api.getChanges(query, 1, 0, null).blockingFirst();
                ChangeInfo change = changes.size() > 0 ? changes.get(0) : null;

                CacheHelper.writeAccountDiffCacheFile(getContext(),
                        name, SerializationManager.getInstance().toJson(change).getBytes());
                return change;
            } else {
                // Use the fetched change
                return SerializationManager.getInstance().fromJson(
                    new String(CacheHelper.readAccountDiffCacheFile(getContext(), name)),
                    ChangeInfo.class);
            }

        } catch (Exception ex) {
            Log.e(TAG, "Can't fetch parent change " + mChange.legacyChangeId, ex);
        }
        return null;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private boolean writeDiffToFile(FileDiffResponse response, File file, boolean isA) {
        Writer writer = null;
        boolean hasData = false;
        try {
            writer = new BufferedWriter(new FileWriter(file));

            DiffContentInfo[] content = response.diff.content;
            for (DiffContentInfo c : content) {
                // AB
                if (c.ab != null) {
                    for (String s : c.ab) {
                        writer.write(s + "\n");
                        hasData = true;
                    }
                }

                if (isA) {
                    // A
                    if (c.a != null) {
                        for (String s : c.a) {
                            writer.write(s + "\n");
                            hasData = true;
                        }
                    }
                } else {
                    // B
                    if (c.b != null) {
                        for (String s : c.b) {
                            writer.write(s + "\n");
                            hasData = true;
                        }
                    }
                }
            }

            return hasData;

        } catch (IOException ex) {
            Log.e(TAG, "Can't write diffs to file " + file.getAbsolutePath(), ex);
            hasData = false;

        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException e) {
                // Ignore
            }

            if (!hasData) {
                file.delete();
            }
        }

        return false;
    }

    private void applyModeRestrictions(boolean isBinary, boolean hasImagePreview) {
        if (mMode == DiffView.IMAGE_MODE && !hasImagePreview) {
            String mode = Preferences.getAccountDiffMode(getContext(), mAccount);
            mMode = mode.equals(Constants.DIFF_MODE_SIDE_BY_SIDE)
                    ? DiffView.SIDE_BY_SIDE_MODE : DiffView.UNIFIED_MODE;
        } else if (mMode != DiffView.IMAGE_MODE && isBinary && hasImagePreview) {
            mMode = DiffView.IMAGE_MODE;
        }
    }

    DiffView.OnCommentListener getCommentListener() {
        return mCommentListener;
    }

    int getScrollPosition() {
        return mBinding.diff.getScrollPosition();
    }
}
