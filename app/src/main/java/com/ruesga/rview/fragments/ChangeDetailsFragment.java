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
import android.content.res.Resources;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ruesga.rview.BaseActivity;
import com.ruesga.rview.R;
import com.ruesga.rview.annotations.ProguardIgnored;
import com.ruesga.rview.databinding.ChangeDetailsFragmentBinding;
import com.ruesga.rview.databinding.FileInfoItemBinding;
import com.ruesga.rview.databinding.MessageItemBinding;
import com.ruesga.rview.gerrit.GerritApi;
import com.ruesga.rview.gerrit.model.AccountInfo;
import com.ruesga.rview.gerrit.model.ChangeInfo;
import com.ruesga.rview.gerrit.model.ChangeMessageInfo;
import com.ruesga.rview.gerrit.model.ChangeOptions;
import com.ruesga.rview.gerrit.model.CommentInfo;
import com.ruesga.rview.gerrit.model.FileInfo;
import com.ruesga.rview.gerrit.model.RevisionInfo;
import com.ruesga.rview.gerrit.model.SubmitType;
import com.ruesga.rview.misc.ModelHelper;
import com.ruesga.rview.misc.PicassoHelper;
import com.ruesga.rview.widget.DividerItemDecoration;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
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

    public static final String EXTRA_CHANGE_ID = "changeId";
    public static final String EXTRA_LEGACY_CHANGE_ID = "legacyChangeId";

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
        add(ChangeOptions.REVIEWER_UPDATES);
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

        public void onMessageExpandedCollapsed(View v) {
            mFragment.mMessageAdapter.performExpandCollapseMessage((int) v.getTag());
        }
    }

    @ProguardIgnored
    public static class FileItemModel {
        public String file;
        public FileInfo info;
        public boolean isTotal;
        public int totalAdded;
        public int totalDeleted;
        public boolean hasGraph = true;
        public int inlineComments;
    }

    public static class FileInfoViewHolder extends RecyclerView.ViewHolder {
        private final FileInfoItemBinding mBinding;
        public FileInfoViewHolder(FileInfoItemBinding binding) {
            super(binding.getRoot());
            mBinding = binding;
            binding.executePendingBindings();
        }
    }

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        private final MessageItemBinding mBinding;
        public MessageViewHolder(MessageItemBinding binding) {
            super(binding.getRoot());
            mBinding = binding;
            mBinding.setExpanded(false);
            binding.executePendingBindings();
        }
    }

    private static class FileAdapter extends RecyclerView.Adapter<FileInfoViewHolder> {
        private final List<FileItemModel> mFiles = new ArrayList<>();

        void update(Map<String, FileInfo> files, Map<String, Integer> inlineComments) {
            mFiles.clear();
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
            FileItemModel total = new FileItemModel();
            total.info = new FileInfo();
            if (added > 0) {
                total.info.linesInserted = added;
            }
            if (deleted > 0) {
                total.info.linesDeleted = deleted;
            }
            total.isTotal = true;
            total.totalAdded = added;
            total.totalDeleted = deleted;
            mFiles.add(total);

            notifyDataSetChanged();
        }

        @Override
        public int getItemCount() {
            return mFiles != null ? mFiles.size() : 0;
        }

        @Override
        public FileInfoViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            return new FileInfoViewHolder(DataBindingUtil.inflate(
                    inflater, R.layout.file_info_item, parent, false));
        }

        @Override
        public void onBindViewHolder(FileInfoViewHolder holder, int position) {
            FileItemModel model = mFiles.get(position);
            holder.mBinding.addedVsDeleted.with(model);
            holder.mBinding.setModel(model);
        }
    }

    private static class MessageAdapter extends RecyclerView.Adapter<MessageViewHolder> {
        private final AccountInfo mBuildBotSystemAccount;
        private final EventHandlers mEventHandlers;
        private ChangeMessageInfo[] mMessages;
        private boolean[] mExpanded;

        public MessageAdapter(ChangeDetailsFragment fragment) {
            final Resources res = fragment.getResources();

            mEventHandlers = new EventHandlers(fragment);

            mBuildBotSystemAccount = new AccountInfo();
            mBuildBotSystemAccount.name = res.getString(R.string.account_build_bot_system_name);
        }


        void update(ChangeMessageInfo[] messages) {
            mMessages = messages;
            mExpanded = new boolean[messages != null ? messages.length : 0];
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
            ChangeMessageInfo message = mMessages[position];
            boolean expanded = mExpanded[position];
            if (message.author == null) {
                message.author = mBuildBotSystemAccount;
            }
            holder.mBinding.setExpanded(expanded);
            holder.mBinding.setModel(message);
            holder.mBinding.setHandlers(mEventHandlers);
            holder.mBinding.getRoot().setTag(position);
        }

        private void performExpandCollapseMessage(int position) {
            mExpanded[position] = !mExpanded[position];
            notifyItemChanged(position);
        }
    }

    public static class DataResponse {
        ChangeInfo mChange;
        SubmitType mSubmitType;
        Map<String, Integer> mInlineComments;
    }

    private final RxLoaderObserver<DataResponse> mChangeObserver =
            new RxLoaderObserver<DataResponse>() {
                @Override
                public void onNext(DataResponse result) {
                    mModel.hasData = result != null;

                    final ChangeInfo change = result.mChange;
                    if (mCurrentRevision == null
                            || !change.revisions.containsKey(mCurrentRevision)) {
                        mCurrentRevision = change.currentRevision;
                    }

                    updatePatchSetInfo(change);
                    updateChangeInfo(change, result.mSubmitType);

                    Map<String, FileInfo> files =  change.revisions.get(mCurrentRevision).files;
                    mModel.filesListModel.visible = files != null && !files.isEmpty();
                    mFileAdapter.update(files, result.mInlineComments);

                    mModel.msgListModel.visible =
                            change.messages != null && change.messages.length > 0;
                    mMessageAdapter.update(change.messages);

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
                    showProgress(true, null);
                }
            };

    private ChangeDetailsFragmentBinding mBinding;
    private Picasso mPicasso;

    private FileAdapter mFileAdapter;
    private MessageAdapter mMessageAdapter;

    private final Model mModel = new Model();
    private String mCurrentRevision;

    private RxLoader1<String, DataResponse> mChangeLoader;
    private String mChangeId;

    public static ChangeDetailsFragment newInstance(int changeId) {
        ChangeDetailsFragment fragment = new ChangeDetailsFragment();
        Bundle arguments = new Bundle();
        arguments.putInt(EXTRA_CHANGE_ID, changeId);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mChangeId = String.valueOf(getArguments().getInt(EXTRA_CHANGE_ID, 0));
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

    private void updatePatchSetInfo(ChangeInfo change) {
        mBinding.patchSetInfo.setChangeId(change.changeId);
        mBinding.patchSetInfo.setRevision(mCurrentRevision);
        RevisionInfo revision = change.revisions.get(mCurrentRevision);
        mBinding.patchSetInfo.setModel(revision);
        mBinding.patchSetInfo.parentCommits.from(revision.commit);

    }

    private void updateChangeInfo(ChangeInfo change, SubmitType submitType) {
        mBinding.changeInfo.owner.with(mPicasso).from(change.owner);
        mBinding.changeInfo.reviewers.with(mPicasso)
                .withRemovableReviewers(true)
                .from(change);
        mBinding.changeInfo.labels.with(mPicasso).from(change);
        mBinding.changeInfo.setModel(change);
        mBinding.changeInfo.setSubmitType(submitType);
    }

    private void startLoadersWithValidContext() {
        if (getActivity() == null) {
            return;
        }

        if (mFileAdapter == null) {
            mFileAdapter = new FileAdapter();
            mBinding.fileInfo.list.setLayoutManager(new LinearLayoutManager(
                    getActivity(), LinearLayoutManager.VERTICAL, false));;
            mBinding.fileInfo.list.addItemDecoration(new DividerItemDecoration(
                    getContext(), LinearLayoutManager.VERTICAL));
            mBinding.fileInfo.list.setAdapter(mFileAdapter);

            mMessageAdapter = new MessageAdapter(this);
            mBinding.messageInfo.list.setLayoutManager(new LinearLayoutManager(
                    getActivity(), LinearLayoutManager.VERTICAL, false));;
            mBinding.messageInfo.list.addItemDecoration(new DividerItemDecoration(
                    getContext(), LinearLayoutManager.VERTICAL));
            mBinding.messageInfo.list.setAdapter(mMessageAdapter);

            // Configure the refresh
            setupSwipeToRefresh();

            // Fetch or join current loader
            RxLoaderManager loaderManager = RxLoaderManagerCompat.get(this);
            mChangeLoader = loaderManager.create(this::fetchChange, mChangeObserver)
                    .start(mChangeId);
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
                    api.getChange(changeId, OPTIONS),
                    api.getChangeRevisionSubmitType(mChangeId, revision),
                    api.getChangeRevisionComments(mChangeId, revision),
                    this::combineResponse
                )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    private void setupSwipeToRefresh() {
        mBinding.refresh.setColorSchemeColors(
                ContextCompat.getColor(getContext(), R.color.accent));
        mBinding.refresh.setOnRefreshListener(() -> {
            mBinding.refresh.setRefreshing(false);
            mChangeLoader.restart(mChangeId);
        });
        mBinding.refresh.setEnabled(false);
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

    private DataResponse combineResponse(ChangeInfo change, SubmitType submitType,
            Map<String, List<CommentInfo>> revisionComments) {
        // Map inline comments
        Map<String, Integer> inlineComments = new HashMap<>();
        if (revisionComments != null) {
            for (String file : revisionComments.keySet()) {
                inlineComments.put(file, revisionComments.get(file).size());
            }
        }

        DataResponse response = new DataResponse();
        response.mChange = change;
        response.mSubmitType = submitType;
        response.mInlineComments = inlineComments;
        return response;
    }

}
