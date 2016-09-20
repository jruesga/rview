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

import android.app.Activity;
import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.content.Context;
import android.content.res.Resources;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.ListPopupWindow;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ruesga.rview.BaseActivity;
import com.ruesga.rview.R;
import com.ruesga.rview.adapters.PatchSetsAdapter;
import com.ruesga.rview.annotations.ProguardIgnored;
import com.ruesga.rview.databinding.ChangeDetailsFragmentBinding;
import com.ruesga.rview.databinding.FileInfoItemBinding;
import com.ruesga.rview.databinding.MessageItemBinding;
import com.ruesga.rview.databinding.TotalAddedDeletedBinding;
import com.ruesga.rview.gerrit.GerritApi;
import com.ruesga.rview.gerrit.model.AccountInfo;
import com.ruesga.rview.gerrit.model.ApprovalInfo;
import com.ruesga.rview.gerrit.model.ChangeInfo;
import com.ruesga.rview.gerrit.model.ChangeMessageInfo;
import com.ruesga.rview.gerrit.model.ChangeOptions;
import com.ruesga.rview.gerrit.model.CommentInfo;
import com.ruesga.rview.gerrit.model.ConfigInfo;
import com.ruesga.rview.gerrit.model.DownloadFormat;
import com.ruesga.rview.gerrit.model.FileInfo;
import com.ruesga.rview.gerrit.model.ReviewerStatus;
import com.ruesga.rview.gerrit.model.RevisionInfo;
import com.ruesga.rview.gerrit.model.SubmitType;
import com.ruesga.rview.misc.AndroidHelper;
import com.ruesga.rview.misc.ModelHelper;
import com.ruesga.rview.misc.PicassoHelper;
import com.ruesga.rview.model.Account;
import com.ruesga.rview.preferences.Constants;
import com.ruesga.rview.preferences.Preferences;
import com.ruesga.rview.widget.AccountChipView.OnAccountChipClickedListener;
import com.ruesga.rview.widget.AccountChipView.OnAccountChipRemovedListener;
import com.ruesga.rview.widget.DividerItemDecoration;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.tatarka.rxloader.RxLoader1;
import me.tatarka.rxloader.RxLoaderManager;
import me.tatarka.rxloader.RxLoaderManagerCompat;
import me.tatarka.rxloader.RxLoaderObserver;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class ChangeDetailsFragment extends Fragment {

    private static final String TAG = "ChangeDetailsFragment";

    private static final int INVALID_CHANGE_ID = -1;

    private static final List<ChangeOptions> OPTIONS = new ArrayList<ChangeOptions>() {{
        add(ChangeOptions.DETAILED_ACCOUNTS);
        add(ChangeOptions.DETAILED_LABELS);
        add(ChangeOptions.ALL_REVISIONS);
        add(ChangeOptions.ALL_COMMITS);
        add(ChangeOptions.ALL_FILES);
        add(ChangeOptions.MESSAGES);
        add(ChangeOptions.CHANGE_ACTIONS);
        add(ChangeOptions.REVIEWED);
        add(ChangeOptions.CHECK);
        add(ChangeOptions.PUSH_CERTIFICATES);
        add(ChangeOptions.COMMIT_FOOTERS);
        add(ChangeOptions.WEB_LINKS);
    }};

    @ProguardIgnored
    public static class ListModel {
        @StringRes
        public int header;
        public boolean visible;
        private ListModel(int h) {
            header = h;
            visible = false;
        }
    }

    @ProguardIgnored
    public static class Model {
        public boolean hasData = true;
        boolean isLocked = false;
        boolean isAuthenticated = false;
        public ListModel filesListModel = new ListModel(R.string.change_details_header_files);
        public ListModel msgListModel = new ListModel(R.string.change_details_header_messages);
    }

    @ProguardIgnored
    @SuppressWarnings({"UnusedParameters", "unused"})
    public static class EventHandlers {
        private final ChangeDetailsFragment mFragment;

        public EventHandlers(ChangeDetailsFragment fragment) {
            mFragment = fragment;
        }

        public void onPatchSetPressed(View v) {
            mFragment.showPatchSetChooser(v);
        }

        public void onStarredPressed(View v) {
            mFragment.performStarred(!v.isSelected());
        }

        public void onAddReviewerPressed(View v) {

        }

        public void onRelatedChangesPressed(View v) {

        }

        public void onDownloadPatchSetPressed(View v) {
            mFragment.performDownloadPatchSet();
        }

        public void onWebLinkPressed(View v) {
            String url = (String) v.getTag();
            if (url != null) {
                AndroidHelper.openUriInCustomTabs(mFragment.getActivity(), url);
            }
        }

        public void onFileItemClick(View v) {
            mFragment.onFileItemClick((String) v.getTag());
        }
    }

    @ProguardIgnored
    public static class FileItemModel {
        public String file;
        public FileInfo info;
        public int totalAdded;
        public int totalDeleted;
        public boolean hasGraph = true;
        public int inlineComments;
    }

    public static class FileInfoViewHolder extends RecyclerView.ViewHolder {
        private final FileInfoItemBinding mBinding;
        FileInfoViewHolder(FileInfoItemBinding binding) {
            super(binding.getRoot());
            mBinding = binding;
            binding.executePendingBindings();
        }
    }

    public static class TotalAddedDeletedViewHolder extends RecyclerView.ViewHolder {
        private final TotalAddedDeletedBinding mBinding;
        TotalAddedDeletedViewHolder(TotalAddedDeletedBinding binding) {
            super(binding.getRoot());
            mBinding = binding;
            binding.executePendingBindings();
        }
    }

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        private final MessageItemBinding mBinding;
        MessageViewHolder(MessageItemBinding binding) {
            super(binding.getRoot());
            mBinding = binding;
            mBinding.setExpanded(false);
            binding.executePendingBindings();
        }
    }

    private static class FileAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final List<FileItemModel> mFiles = new ArrayList<>();
        private FileItemModel mTotals;
        private final EventHandlers mEventHandlers;

        FileAdapter(EventHandlers handlers) {
            mEventHandlers = handlers;
        }

        void update(Map<String, FileInfo> files, Map<String, Integer> inlineComments) {
            mFiles.clear();
            mTotals = null;
            if (files == null) {
                notifyDataSetChanged();
                return;
            }

            int added = 0;
            int deleted = 0;
            // Compute the added and deleted from revision instead from the change
            // to be accurate with the current revision info
            for (String key : files.keySet()) {
                FileInfo info = files.get(key);
                if (info.linesInserted != null) {
                    added += info.linesInserted;
                }
                if (info.linesDeleted != null) {
                    deleted += info.linesDeleted;
                }
            }

            // Create a model from each file
            for (String key : files.keySet()) {
                FileItemModel model = new FileItemModel();
                model.file = key;
                model.info = files.get(key);
                model.totalAdded = added;
                model.totalDeleted = deleted;
                model.inlineComments =
                        inlineComments.containsKey(key) ? inlineComments.get(key) : 0;
                model.hasGraph =
                        (model.info.linesInserted != null && model.info.linesInserted > 0) ||
                                (model.info.linesDeleted != null && model.info.linesDeleted > 0) ||
                                model.inlineComments > 0;
                mFiles.add(model);
            }

            // And add the total
            mTotals = new FileItemModel();
            mTotals.info = new FileInfo();
            if (added > 0) {
                mTotals.info.linesInserted = added;
            }
            if (deleted > 0) {
                mTotals.info.linesDeleted = deleted;
            }
            mTotals.totalAdded = added;
            mTotals.totalDeleted = deleted;

            notifyDataSetChanged();
        }

        @Override
        public int getItemCount() {
            return mFiles.size() + (mTotals != null ? 1 : 0);
        }

        @Override
        public int getItemViewType(int position) {
            return position == getItemCount() - 1 ? 1 : 0;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            if (viewType == 1) {
                return new TotalAddedDeletedViewHolder(DataBindingUtil.inflate(
                        inflater, R.layout.total_added_deleted, parent, false));
            } else {
                return new FileInfoViewHolder(DataBindingUtil.inflate(
                        inflater, R.layout.file_info_item, parent, false));
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof TotalAddedDeletedViewHolder) {
                TotalAddedDeletedBinding binding = ((TotalAddedDeletedViewHolder) holder).mBinding;
                binding.addedVsDeleted.with(mTotals);
                binding.setModel(mTotals);
            } else {
                FileItemModel model = mFiles.get(position);
                FileInfoItemBinding binding = ((FileInfoViewHolder) holder).mBinding;
                binding.addedVsDeleted.with(model);
                binding.setModel(model);
                binding.setHandlers(mEventHandlers);
            }
        }
    }

    private static class MessageAdapter extends RecyclerView.Adapter<MessageViewHolder> {
        private final AccountInfo mBuildBotSystemAccount;
        private final EventHandlers mEventHandlers;
        private ChangeMessageInfo[] mMessages;

        MessageAdapter(ChangeDetailsFragment fragment, EventHandlers handlers) {
            final Resources res = fragment.getResources();
            mEventHandlers = handlers;

            mBuildBotSystemAccount = new AccountInfo();
            mBuildBotSystemAccount.name = res.getString(R.string.account_build_bot_system_name);
        }


        void update(DataResponse response) {
            mMessages = response.mChange.messages;
            notifyDataSetChanged();
        }

        @Override
        public int getItemCount() {
            return mMessages != null ? mMessages.length : 0;
        }

        @Override
        public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            return new MessageViewHolder(DataBindingUtil.inflate(
                    inflater, R.layout.message_item, parent, false));
        }

        @Override
        public void onBindViewHolder(MessageViewHolder holder, int position) {
            final Context context = holder.mBinding.getRoot().getContext();
            ChangeMessageInfo message = mMessages[position];
            if (message.author == null) {
                message.author = mBuildBotSystemAccount;
            }
            PicassoHelper.bindAvatar(context, PicassoHelper.getPicassoClient(context),
                    message.author, holder.mBinding.avatar,
                    PicassoHelper.getDefaultAvatar(context, R.color.primaryDark));
            holder.mBinding.setModel(message);
            holder.mBinding.setHandlers(mEventHandlers);
        }
    }

    public static class DataResponse {
        ChangeInfo mChange;
        SubmitType mSubmitType;
        Map<String, Integer> mInlineComments;
        ConfigInfo mProjectConfig;
    }

    private final RxLoaderObserver<DataResponse> mChangeObserver =
            new RxLoaderObserver<DataResponse>() {
                @Override
                public void onNext(DataResponse result) {
                    mModel.isLocked = false;
                    updateLocked();
                    mResponse = result;

                    ChangeInfo change = null;
                    mModel.hasData = result != null;
                    if (mModel.hasData) {
                        change = result.mChange;
                        if (mCurrentRevision == null
                                || !change.revisions.containsKey(mCurrentRevision)) {
                            mCurrentRevision = change.currentRevision;
                        }

                        sortRevisions(change);
                        updatePatchSetInfo(result);
                        updateChangeInfo(result);

                        Map<String, FileInfo> files = change.revisions.get(mCurrentRevision).files;
                        mModel.filesListModel.visible = files != null && !files.isEmpty();
                        mFileAdapter.update(files, result.mInlineComments);
                        mModel.msgListModel.visible =
                                change.messages != null && change.messages.length > 0;
                        mMessageAdapter.update(result);
                    }

                    mBinding.setModel(mModel);
                    showProgress(false, change);
                }

                @Override
                public void onError(Throwable error) {
                    ((BaseActivity) getActivity()).handleException(TAG, error);
                    showProgress(false, null);
                }

                @Override
                public void onStarted() {
                    mModel.isLocked = true;
                    updateLocked();

                    showProgress(true, null);
                }
            };

    private final RxLoaderObserver<Boolean> mStarredObserver = new RxLoaderObserver<Boolean>() {
        @Override
        public void onNext(Boolean value) {
            mResponse.mChange.starred = value;
            updateChangeInfo(mResponse);
        }

        @Override
        public void onError(Throwable error) {
            ((BaseActivity) getActivity()).handleException(TAG, error);
        }
    };

    private final RxLoaderObserver<AccountInfo> mRemoveReviewerObserver
            = new RxLoaderObserver<AccountInfo>() {
        @Override
        public void onNext(AccountInfo account) {
            // Update internal objects
            if (mResponse.mChange.reviewers != null) {
                for (ReviewerStatus status : mResponse.mChange.reviewers.keySet()) {
                    mResponse.mChange.reviewers.put(status,
                            removeAccount(account, mResponse.mChange.reviewers.get(status)));
                }
            }
            if (mResponse.mChange.labels != null) {
                for (String label : mResponse.mChange.labels.keySet()) {
                    if (mResponse.mChange.labels.get(label).all !=  null) {
                        mResponse.mChange.labels.get(label).all =
                                removeApproval(account, mResponse.mChange.labels.get(label).all);
                    }
                }
            }
            mResponse.mChange.removableReviewers = removeAccount(
                    account, mResponse.mChange.removableReviewers);

            updateChangeInfo(mResponse);
        }

        @Override
        public void onError(Throwable error) {
            ((BaseActivity) getActivity()).handleException(TAG, error);
        }

        private AccountInfo[] removeAccount(AccountInfo account, AccountInfo[] accounts) {
            if (accounts == null) {
                return null;
            }
            List<AccountInfo> newAccounts = new ArrayList<>();
            for (AccountInfo a : accounts) {
                if (a.accountId != account.accountId) {
                    newAccounts.add(a);
                }
            }
            return newAccounts.toArray(new AccountInfo[newAccounts.size()]);
        }

        private ApprovalInfo[] removeApproval(AccountInfo account, ApprovalInfo[] approvals) {
            if (approvals == null) {
                return null;
            }
            List<ApprovalInfo> newApprovals = new ArrayList<>();
            for (ApprovalInfo a : approvals) {
                if (a.owner != null && a.owner.accountId != account.accountId) {
                    newApprovals.add(a);
                }
            }
            return newApprovals.toArray(new ApprovalInfo[newApprovals.size()]);
        }
    };

    private final OnAccountChipClickedListener mOnAccountChipClickedListener
            = this::performAccountClicked;
    private final OnAccountChipRemovedListener mOnAccountChipRemovedListener
            = this::performRemoveAccount;

    private ChangeDetailsFragmentBinding mBinding;
    private Picasso mPicasso;

    private FileAdapter mFileAdapter;
    private MessageAdapter mMessageAdapter;

    private EventHandlers mEventHandlers;
    private final Model mModel = new Model();
    private String mCurrentRevision;
    private DataResponse mResponse;
    private final List<RevisionInfo> mAllRevisions = new ArrayList<>();

    private RxLoader1<String, DataResponse> mChangeLoader;
    private RxLoader1<Boolean, Boolean> mStarredLoader;
    private RxLoader1<AccountInfo, AccountInfo> mRemoveRevieverLoader;
    private int mLegacyChangeId;

    public static ChangeDetailsFragment newInstance(int changeId) {
        ChangeDetailsFragment fragment = new ChangeDetailsFragment();
        Bundle arguments = new Bundle();
        arguments.putInt(Constants.EXTRA_LEGACY_CHANGE_ID, changeId);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLegacyChangeId = getArguments().getInt(
                Constants.EXTRA_LEGACY_CHANGE_ID, INVALID_CHANGE_ID);
        mPicasso = PicassoHelper.getPicassoClient(getContext());
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(
                inflater, R.layout.change_details_fragment, container, false);
        mBinding.setModel(mModel);
        mBinding.refresh.setEnabled(false);
        startLoadersWithValidContext();
        return mBinding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        startLoadersWithValidContext();
    }

    private void updatePatchSetInfo(DataResponse response) {
        mBinding.patchSetInfo.setChangeId(response.mChange.changeId);
        mBinding.patchSetInfo.setRevision(mCurrentRevision);
        RevisionInfo revision = response.mChange.revisions.get(mCurrentRevision);
        mBinding.patchSetInfo.setChange(response.mChange);
        mBinding.patchSetInfo.setConfig(response.mProjectConfig);
        mBinding.patchSetInfo.setModel(revision);
        final String pathset = getContext().getString(R.string.change_details_header_patchsets,
                revision.number, response.mChange.revisions.size());
        mBinding.patchSetInfo.setPatchset(pathset);
        mBinding.patchSetInfo.setHandlers(mEventHandlers);
        mBinding.patchSetInfo.parentCommits.with(mEventHandlers).from(revision.commit);
        mBinding.patchSetInfo.setHasData(true);
    }

    private void updateChangeInfo(DataResponse response) {
        mBinding.changeInfo.owner.with(mPicasso).from(response.mChange.owner);
        mBinding.changeInfo.reviewers.with(mPicasso)
                .listenOn(mOnAccountChipClickedListener)
                .listenOn(mOnAccountChipRemovedListener)
                .withRemovableReviewers(true)
                .from(response.mChange);
        mBinding.changeInfo.labels.with(mPicasso).from(response.mChange);
        mBinding.changeInfo.setModel(response.mChange);
        mBinding.changeInfo.setSubmitType(response.mSubmitType);
        mBinding.changeInfo.setHandlers(mEventHandlers);
        mBinding.changeInfo.setHasData(true);
        mBinding.changeInfo.setIsTwoPane(getResources().getBoolean(R.bool.config_is_two_pane));
        mBinding.changeInfo.setIsCurrentRevision(
                mCurrentRevision.equals(response.mChange.currentRevision));
    }

    private void startLoadersWithValidContext() {
        if (getActivity() == null) {
            return;
        }

        if (mFileAdapter == null) {
            // Set authenticated mode
            Account account = Preferences.getAccount(getContext());
            if (account != null) {
                mModel.isAuthenticated = account.hasAuthenticatedAccessMode();
            }
            updateAuthenticated();


            mEventHandlers = new EventHandlers(this);

            mFileAdapter = new FileAdapter(mEventHandlers);
            mBinding.fileInfo.list.setLayoutManager(new LinearLayoutManager(
                    getActivity(), LinearLayoutManager.VERTICAL, false));
            mBinding.fileInfo.list.setNestedScrollingEnabled(false);
            mBinding.fileInfo.list.setAdapter(mFileAdapter);

            mMessageAdapter = new MessageAdapter(this, mEventHandlers);
            int leftPadding = getResources().getDimensionPixelSize(
                    R.dimen.message_list_left_padding);
            DividerItemDecoration messageDivider = new DividerItemDecoration(
                    getContext(), LinearLayoutManager.VERTICAL);
            messageDivider.setMargins(leftPadding, 0);
            mBinding.messageInfo.list.setLayoutManager(new LinearLayoutManager(
                    getActivity(), LinearLayoutManager.VERTICAL, false));
            mBinding.messageInfo.list.addItemDecoration(messageDivider);
            mBinding.messageInfo.list.setNestedScrollingEnabled(false);
            mBinding.messageInfo.list.setAdapter(mMessageAdapter);

            // Configure the refresh
            setupSwipeToRefresh();

            // Fetch or join current loader
            RxLoaderManager loaderManager = RxLoaderManagerCompat.get(this);
            mChangeLoader = loaderManager.create("fetch", this::fetchChange, mChangeObserver)
                    .start(String.valueOf(mLegacyChangeId));
            mStarredLoader = loaderManager.create("starred", this::starredChange, mStarredObserver);
            mRemoveRevieverLoader = loaderManager.create(
                    "remove_reviewer", this::removeReviewer, mRemoveReviewerObserver);
        }
    }

    @Override
    public final void onDestroyView() {
        super.onDestroyView();
        mBinding.unbind();
    }

    @SuppressWarnings("ConstantConditions")
    private Observable<DataResponse> fetchChange(String changeId) {
        final Context ctx = getActivity();
        final GerritApi api = ModelHelper.getGerritApi(ctx);
        final String revision = mCurrentRevision == null
                ? GerritApi.CURRENT_REVISION : mCurrentRevision;
        return Observable.zip(
                    Observable.fromCallable(() -> {
                        DataResponse dataResponse = new DataResponse();
                        dataResponse.mChange = api.getChange(
                                changeId, OPTIONS).toBlocking().first();

                        // Obtain the project configuration
                        if (dataResponse.mChange != null) {
                            dataResponse.mProjectConfig = api.getProjectConfig(
                                    dataResponse.mChange.project).toBlocking().first();
                        }

                        return dataResponse;
                    }),
                    api.getChangeRevisionSubmitType(changeId, revision),
                    api.getChangeRevisionComments(changeId, revision),
                    this::combineResponse
                )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    @SuppressWarnings("ConstantConditions")
    private Observable<Boolean> starredChange(final Boolean starred) {
        final Context ctx = getActivity();
        final GerritApi api = ModelHelper.getGerritApi(ctx);
        return Observable.fromCallable(() -> {
            Observable<Void> call;
            if (starred) {
                call = api.putDefaultStarOnChange(
                        GerritApi.SELF_ACCOUNT, String.valueOf(mLegacyChangeId));
            } else {
                call = api.deleteDefaultStarFromChange(
                        GerritApi.SELF_ACCOUNT, String.valueOf(mLegacyChangeId));
            }
            call.toBlocking().first();
            return starred;
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    @SuppressWarnings("ConstantConditions")
    private Observable<AccountInfo> removeReviewer(final AccountInfo account) {
        final Context ctx = getActivity();
        final GerritApi api = ModelHelper.getGerritApi(ctx);
        return Observable.fromCallable(() -> {
                    api.deleteChangeReviewer(
                            String.valueOf(mLegacyChangeId),
                            String.valueOf(account.accountId))
                        .toBlocking().first();
                    return account;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    private void setupSwipeToRefresh() {
        mBinding.refresh.setColorSchemeColors(
                ContextCompat.getColor(getContext(), R.color.accent));
        mBinding.refresh.setOnRefreshListener(this::forceRefresh);
        mBinding.refresh.setEnabled(false);
    }

    private void forceRefresh() {
        startLoadersWithValidContext();
        mChangeLoader.restart(String.valueOf(mLegacyChangeId));
    }

    private void showProgress(boolean show, ChangeInfo change) {
        BaseActivity activity = (BaseActivity) getActivity();
        if (show) {
            activity.onRefreshStart();
        } else {
            activity.onRefreshEnd(change);
        }
        mBinding.refresh.setEnabled(!show);
    }

    private DataResponse combineResponse(DataResponse response, SubmitType submitType,
                Map<String, List<CommentInfo>> revisionComments) {
        // Map inline comments
        Map<String, Integer> inlineComments = new HashMap<>();
        if (revisionComments != null) {
            for (String file : revisionComments.keySet()) {
                inlineComments.put(file, revisionComments.get(file).size());
            }
        }

        response.mSubmitType = submitType;
        response.mInlineComments = inlineComments;
        return response;
    }

    private void onFileItemClick(String file) {
        /* FIXME Restore when activity is build
        Intent i = new Intent(getContext(), DiffViewerActivity.class);
        i.putExtra(Constants.EXTRA_LEGACY_CHANGE_ID, mLegacyChangeId);
        i.putExtra(Constants.EXTRA_REVISION_ID, mCurrentRevision);
        i.putExtra(Constants.EXTRA_FILE_ID, file);
        startActivity(i);*/
    }

    private void sortRevisions(ChangeInfo change) {
        mAllRevisions.clear();
        for (String revision : change.revisions.keySet()) {
            RevisionInfo rev = change.revisions.get(revision);
            rev.commit.commit = revision;
            mAllRevisions.add(rev);
        }
        Collections.sort(mAllRevisions, (o1, o2) -> {
            if (o1.number > o2.number) {
                return -1;
            }
            if (o1.number < o2.number) {
                return 1;
            }
            return 0;
        });
    }

    private void showPatchSetChooser(View anchor) {
        if (mModel.isLocked) {
            return;
        }

        final ListPopupWindow popupWindow = new ListPopupWindow(getContext());
        PatchSetsAdapter adapter = new PatchSetsAdapter(
                getContext(), mAllRevisions, mCurrentRevision);
        popupWindow.setAnchorView(anchor);
        popupWindow.setAdapter(adapter);
        popupWindow.setWidth(adapter.measureContentWidth());
        popupWindow.setOnItemClickListener((parent, view, position, id) -> {
            popupWindow.dismiss();
            mCurrentRevision = mAllRevisions.get(position).commit.commit;
            forceRefresh();
        });
        popupWindow.show();
    }

    private void performStarred(boolean starred) {
        mStarredLoader.restart(starred);
    }

    private void performAccountClicked(AccountInfo account) {
        // TODO Open change list with account filter
    }

    private void performRemoveAccount(AccountInfo account) {
        mRemoveRevieverLoader.restart(account);
    }

    private void updateLocked() {
        mBinding.patchSetInfo.setIsLocked(mModel.isLocked);
        mBinding.changeInfo.setIsLocked(mModel.isLocked);
        mBinding.executePendingBindings();
    }

    private void updateAuthenticated() {
        mBinding.patchSetInfo.setIsAuthenticated(mModel.isAuthenticated);
        mBinding.changeInfo.setIsAuthenticated(mModel.isAuthenticated);
        mBinding.executePendingBindings();
    }

    @SuppressWarnings("ConstantConditions")
    private void performDownloadPatchSet() {
        final Context ctx = getActivity();
        final GerritApi api = ModelHelper.getGerritApi(ctx);
        Uri uri = api.getDownloadPatchSetUri(
                String.valueOf(mLegacyChangeId), mCurrentRevision, DownloadFormat.TBZ2);

        // Use the download manager to perform the download
        DownloadManager downloadManager =
                (DownloadManager) getContext().getSystemService(Activity.DOWNLOAD_SERVICE);
        Request request = new Request(uri)
                        .setAllowedOverMetered(false)
                        .setAllowedOverRoaming(false)
                        .setMimeType("application/x-tar-bz2")
                        .setNotificationVisibility(Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.allowScanningByMediaScanner();
        downloadManager.enqueue(request);
    }
}
