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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.tatarka.rxloader.RxLoader;
import me.tatarka.rxloader.RxLoader2;
import me.tatarka.rxloader.RxLoaderManager;
import me.tatarka.rxloader.RxLoaderManagerCompat;
import me.tatarka.rxloader.RxLoaderObserver;
import okhttp3.ResponseBody;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class FileDiffViewerFragment extends Fragment {

    private static final String TAG = "FileDiffViewerFragment";

    public interface OnDiffCompleteListener {
        void onDiffComplete(boolean isBinary, boolean hasImagePreview);
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
            mForceRefresh = true;
            mLoader.clear();
            mLoader.restart();
        }

        @Override
        public void onError(Throwable error) {
            ((BaseActivity) getActivity()).handleException(TAG, error);
        }
    };

    private DiffView.OnCommentListener mCommentListener = new DiffView.OnCommentListener() {
        @Override
        public void onNewDraft(View v, boolean left, int line) {
            final String baseRevision = mBase == null ? "0" : mBase;
            String rev = left ? baseRevision : mRevision;
            performDraftMessageDialog(v, null,
                    newValue -> {
                        mActionLoader.clear();
                        mActionLoader.restart(ModelHelper.ACTION_CREATE_DRAFT,
                            new String[]{rev, null, String.valueOf(line), newValue});
                    });
        }

        @Override
        public void onReply(View v, String revisionId, String commentId, int line) {
            performDraftMessageDialog(v, null,
                    newValue -> {
                        mActionLoader.clear();
                        mActionLoader.restart(ModelHelper.ACTION_CREATE_DRAFT,
                            new String[]{revisionId, commentId, String.valueOf(line), newValue}) ;
                    });
        }

        @Override
        public void onDone(View v, String revisionId, String commentId, int line) {
            String msg = getString(R.string.draft_reply_done);
            mActionLoader.clear();
            mActionLoader.restart(ModelHelper.ACTION_CREATE_DRAFT,
                    new String[]{revisionId, commentId, String.valueOf(line), msg});
        }

        @Override
        public void onEditDraft(View v, String revisionId, String draftId,
                String inReplyTo, int line, String msg) {
            performDraftMessageDialog(v, msg,
                    newValue -> {
                        mActionLoader.clear();
                        mActionLoader.restart(ModelHelper.ACTION_UPDATE_DRAFT,
                            new String[]{revisionId, draftId,
                                    inReplyTo, String.valueOf(line), newValue});
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

    private ChangeInfo mChange;

    private String mRevisionId;
    private String mFile;
    private String mFileHash;
    private String mBase;
    private String mRevision;

    private Account mAccount;

    private boolean mForceRefresh;

    private int mMode;
    private boolean mWrap;
    private boolean mHighlightTabs;
    private boolean mHighlightTrailingWhitespaces;

    public static FileDiffViewerFragment newInstance(String revisionId, String file,
            int base, int revision, int mode, boolean wrap,
            boolean highlightTabs, boolean mHighlightTrailingWhitespaces) {
        FileDiffViewerFragment fragment = new FileDiffViewerFragment();
        Bundle arguments = new Bundle();
        arguments.putString(Constants.EXTRA_REVISION_ID, revisionId);
        arguments.putString(Constants.EXTRA_FILE, file);
        arguments.putInt(Constants.EXTRA_BASE, base);
        arguments.putInt(Constants.EXTRA_REVISION, revision);
        arguments.putInt("mode", mode);
        arguments.putBoolean("wrap", wrap);
        arguments.putBoolean("highlight_tabs", highlightTabs);
        arguments.putBoolean("highlight_trailing_whitespaces", mHighlightTrailingWhitespaces);
        fragment.setArguments(arguments);
        return fragment;
    }

    private void update() {
        if (mResponse != null) {
            // Only refresh the diff view if we can display text differences
            mHandler.postDelayed(() ->
                mBinding.diff
                    .from(mResponse.diff)
                    .withLeftContent(mResponse.leftContent)
                    .withRightContent(mResponse.rightContent)
                    .withComments(mResponse.comments)
                    .withDrafts(mResponse.draftComments)
                    .mode(mMode)
                    .wrap(mWrap)
                    .listenOn(mCommentListener)
                    .canEdit(mAccount.hasAuthenticatedAccessMode())
                    .highlightTabs(mHighlightTabs)
                    .highlightTrailingWhitespaces(mHighlightTrailingWhitespaces)
                    .update(),
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
        int base = getArguments().getInt(Constants.EXTRA_BASE);
        mBase = base == 0 ? null : String.valueOf(base);
        mRevision = String.valueOf(getArguments().getInt(Constants.EXTRA_REVISION));
        mMode = getArguments().getInt("mode");
        mWrap = getArguments().getBoolean("wrap");
        mHighlightTabs = getArguments().getBoolean("highlight_tabs");
        mHighlightTrailingWhitespaces = getArguments().getBoolean("highlight_trailing_whitespaces");
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

            } catch (IOException ex) {
                Log.e(TAG, "Failed to load change cached data", ex);
                getActivity().finish();
            }

            // Fetch or join current loader
            mForceRefresh = false;
            RxLoaderManager loaderManager = RxLoaderManagerCompat.get(this);
            mLoader = loaderManager.create(
                    "file-diff-" + hashCode(), fetchDiffs(), mObserver).start();
            mActionLoader = loaderManager.create(
                    "file-diff-action" + hashCode(), this::doAction, mActionObserver);
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
                && mChange.revisions.get(mRevisionId).files.get(mFile).binary;

        final Context ctx = getActivity();
        final GerritApi api = ModelHelper.getGerritApi(ctx);
        return Observable.zip(
                withCached(
                        Observable.fromCallable(() -> {
                            if (!isBinary) {
                                return api.getChangeRevisionFileDiff(
                                        String.valueOf(mChange.legacyChangeId),
                                        mRevisionId,
                                        mFile,
                                        base,
                                        Option.INSTANCE,
                                        null,
                                        WhitespaceType.IGNORE_NONE,
                                        IgnoreWhitespaceType.NONE,
                                        ContextType.ALL)
                                        .toBlocking().first();
                            }

                            DiffInfo diff = new DiffInfo();
                            diff.binary = true;
                            return diff;
                        }),
                        DiffInfo.class,
                        diffCacheId + CacheHelper.CACHE_DIFF_JSON
                ),
                withCached(
                        Observable.fromCallable(() -> {
                            if (!isBinary && mBase != null) {
                                return api.getChangeRevisionComments(
                                        String.valueOf(mChange.legacyChangeId), baseRevision)
                                        .toBlocking().first();
                            }
                            return new HashMap<>();
                        }),
                        commentType,
                        baseRevision + "_" + CacheHelper.CACHE_COMMENTS_JSON),
                withCached(
                        Observable.fromCallable(() -> {
                            if (!isBinary) {
                                return api.getChangeRevisionComments(
                                        String.valueOf(mChange.legacyChangeId), mRevisionId)
                                        .toBlocking().first();
                            }
                            return new HashMap<>();
                        }),
                        commentType,
                        mRevision + "_" + CacheHelper.CACHE_COMMENTS_JSON),
                withCached(
                        Observable.fromCallable(() -> {
                            // Do no fetch drafts if the account is not authenticated
                            if (!isBinary && mBase != null && mAccount.hasAuthenticatedAccessMode()) {
                                return api.getChangeRevisionDrafts(
                                        String.valueOf(mChange.legacyChangeId), baseRevision)
                                        .toBlocking().first();
                            }
                            return new HashMap<>();
                        }),
                        commentType,
                        baseRevision + "_" + CacheHelper.CACHE_DRAFT_JSON),
                withCached(
                        Observable.fromCallable(() -> {
                            // Do no fetch drafts if the account is not authenticated
                            if (!isBinary && mAccount.hasAuthenticatedAccessMode()) {
                                return api.getChangeRevisionDrafts(
                                        String.valueOf(mChange.legacyChangeId), mRevisionId)
                                        .toBlocking().first();
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

    private FileDiffResponse combine(DiffInfo diff, Map<String, List<CommentInfo>> commentsA,
            Map<String, List<CommentInfo>> commentsB, Map<String, List<CommentInfo>> draftsA,
            Map<String, List<CommentInfo>> draftsB) {
        int base = mBase == null ? 0 : Integer.parseInt(mBase);
        int revision = Integer.parseInt(mRevision);

        FileDiffResponse response = new FileDiffResponse();
        response.diff = diff;
        response.comments = new Pair<>(
                setRevision(commentsA, base, base),
                setRevision(commentsB, revision, base));
        response.draftComments = new Pair<>(
                setRevision(draftsA, base, base),
                setRevision(draftsB, revision, base));

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
            Log.e(TAG, "Failed to load diff cached data", ex);
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

        return response;
    }

    private List<CommentInfo> setRevision(Map<String, List<CommentInfo>> comments,
            int base, int parentBase) {
        if (comments != null && comments.get(mFile) != null) {
            for (CommentInfo comment : comments.get(mFile)) {
                comment.patchSet = comment.side != null && comment.side.equals(SideType.PARENT)
                        ? parentBase : base;
            }
            return comments.get(mFile);
        }
        return null;
    }

    @SuppressWarnings("ConstantConditions")
    private Observable<Object> doAction(final String action, final String[] params) {
        final Context ctx = getActivity();
        final GerritApi api = ModelHelper.getGerritApi(ctx);
        return Observable.fromCallable(() -> {
                    switch (action) {
                        case ModelHelper.ACTION_CREATE_DRAFT:
                            return performCreateDraft(api,
                                    params[0], params[1], Integer.parseInt(params[2]), params[3]);
                        case ModelHelper.ACTION_UPDATE_DRAFT:
                            return performUpdateDraft(api, params[0], params[1], params[2],
                                    Integer.parseInt(params[3]), params[4]);
                        case ModelHelper.ACTION_DELETE_DRAFT:
                            performDeleteDraft(api, params[0], params[1]);
                            break;
                    }
                    return Boolean.TRUE;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    private void performDraftMessageDialog(
            View v, String comment, EditDialogFragment.OnEditChanged cb) {
        EditDialogFragment fragment = EditDialogFragment.newInstance(
                getString(R.string.draft_title), comment,
                    getString(R.string.action_save), getString(R.string.draft_hint), false, v);
        fragment.setOnEditChanged(cb);
        fragment.show(getChildFragmentManager(), EditDialogFragment.TAG);
    }

    private CommentInfo performCreateDraft(GerritApi api, String revision,
            String commentId, int line, String msg) {
        int base = Integer.parseInt(revision);
        SideType side = base == 0 ? SideType.PARENT : SideType.REVISION;
        String rev = base == 0 ? mRevisionId : revision;

        CommentInput input = new CommentInput();
        if (!TextUtils.isEmpty(commentId)) {
            input.inReplyTo = commentId;
        }
        input.message = msg;
        input.line = line;
        input.path = mFile;
        input.side = side;
        return api.createChangeRevisionDraft(
                String.valueOf(mChange.legacyChangeId), rev, input).toBlocking().first();
    }

    private CommentInfo performUpdateDraft(GerritApi api, String revision,
            String draftId, String inReplyTo, int line, String msg) {
        int base = Integer.parseInt(revision);
        SideType side = base == 0 ? SideType.PARENT : SideType.REVISION;
        String rev = base == 0 ? mRevisionId : revision;

        CommentInput input = new CommentInput();
        input.id = draftId;
        input.inReplyTo = inReplyTo;
        input.message = msg;
        input.line = line;
        input.path = mFile;
        input.side = side;
        return api.updateChangeRevisionDraft(
                String.valueOf(mChange.legacyChangeId), rev, draftId, input).toBlocking().first();
    }

    private void performDeleteDraft(GerritApi api, String revision, String draftId) {
        int base = Integer.parseInt(revision);
        String rev = base == 0 ? mRevisionId : revision;
        api.deleteChangeRevisionDraft(
                String.valueOf(mChange.legacyChangeId), rev, draftId)
                    .toBlocking().first();
    }

    private void showProgress(boolean show) {
        BaseActivity activity = (BaseActivity) getActivity();
        if (show) {
            activity.onRefreshStart();
        } else {
            activity.onRefreshEnd(null);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> Observable<T> withCached(Observable<T> call, Type type, String name) {
        try {
            if (!mForceRefresh && CacheHelper.hasAccountDiffCacheDir(getContext(), name)) {
                T o = SerializationManager.getInstance().fromJson(
                        new String(CacheHelper.readAccountDiffCacheFile(
                                getContext(), name)), type);
                return Observable.just((o));
            }
        } catch (IOException ex) {
            Log.e(TAG, "Failed to load diff cached data: " + name, ex);
        }
        return call;
    }

    private void fetchRevisionsContentIfNeeded(FileDiffResponse response) {
        // If is not a binary file, we can use the diff information to build the file
        // instead of fetch it from the network
        if (!response.diff.binary) {
            response.leftContent = writeCachedContent(response, mBase, true);
            response.rightContent = writeCachedContent(response, mRevision, false);
            return;
        }

        if (mMode == DiffView.IMAGE_MODE) {
            final String baseRevision = mBase == null ? "0" : mBase;

            // Base revision
            if (baseRevision.equals("0")) {
                // Base file is only available from the parent commit, so we need to
                // fetch by commit to recover the parent id, and then fetch the revision
                // to download the file
                String parentRevision = mChange.revisions.get(mRevisionId).commit.parents[0].commit;
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

            // Current revision
            response.rightContent = fetchCachedContent(
                    String.valueOf(mChange.legacyChangeId), mRevisionId, mRevision);
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
                        .toBlocking().first();
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
                List<ChangeInfo> changes = api.getChanges(query, 1, 0, null).toBlocking().first();
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
            mMode = Preferences.getAccountSearchMode(getContext(), mAccount);
        } else if (mMode != DiffView.IMAGE_MODE && isBinary && hasImagePreview) {
            mMode = DiffView.IMAGE_MODE;
        }
    }
}
