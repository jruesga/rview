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
import com.ruesga.rview.databinding.ChangeAndPatchSetInfoItemBinding;
import com.ruesga.rview.databinding.ChangeDetailsFragmentBinding;
import com.ruesga.rview.databinding.ChangeInfoItemBinding;
import com.ruesga.rview.databinding.FileInfoItemBinding;
import com.ruesga.rview.databinding.HeaderItemBinding;
import com.ruesga.rview.databinding.MessageItemBinding;
import com.ruesga.rview.databinding.PatchSetInfoItemBinding;
import com.ruesga.rview.gerrit.GerritApi;
import com.ruesga.rview.gerrit.model.AccountInfo;
import com.ruesga.rview.gerrit.model.ChangeInfo;
import com.ruesga.rview.gerrit.model.ChangeMessageInfo;
import com.ruesga.rview.gerrit.model.ChangeOptions;
import com.ruesga.rview.gerrit.model.FileInfo;
import com.ruesga.rview.gerrit.model.RevisionInfo;
import com.ruesga.rview.gerrit.model.SubmitType;
import com.ruesga.rview.misc.ModelHelper;
import com.ruesga.rview.misc.PicassoHelper;
import com.ruesga.rview.widget.DividerItemDecoration;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
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
    public static class Model {
        public boolean hasData = true;
    }

    @ProguardIgnored
    public static class HeaderItemModel {
        public String title;
    }

    @ProguardIgnored
    public static class FileItemModel {
        public String file;
        public FileInfo info;
    }

    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        private final HeaderItemBinding mBinding;
        public HeaderViewHolder(HeaderItemBinding binding) {
            super(binding.getRoot());
            mBinding = binding;
            binding.executePendingBindings();
        }
    }

    public static class ChangeAndPatchSetInfoViewHolder extends RecyclerView.ViewHolder {
        private final ChangeAndPatchSetInfoItemBinding mBinding;
        public ChangeAndPatchSetInfoViewHolder(ChangeAndPatchSetInfoItemBinding binding) {
            super(binding.getRoot());
            mBinding = binding;
            binding.executePendingBindings();
        }
    }

    public static class ChangeInfoViewHolder extends RecyclerView.ViewHolder {
        private final ChangeInfoItemBinding mBinding;
        public ChangeInfoViewHolder(ChangeInfoItemBinding binding) {
            super(binding.getRoot());
            mBinding = binding;
            binding.executePendingBindings();
        }
    }

    public static class PatchSetInfoViewHolder extends RecyclerView.ViewHolder {
        private final PatchSetInfoItemBinding mBinding;
        public PatchSetInfoViewHolder(PatchSetInfoItemBinding binding) {
            super(binding.getRoot());
            mBinding = binding;
            binding.executePendingBindings();
        }
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
            binding.executePendingBindings();
        }
    }

    private static class ChangeAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int HEADER_VIEW = 0;
        private static final int CHANGE_AND_PATCH_SET_INFO_VIEW = 1;
        private static final int PATCH_SET_INFO_VIEW = 2;
        private static final int CHANGE_INFO_VIEW = 3;
        private static final int FILE_INFO_VIEW = 4;
        private static final int MESSAGE_VIEW = 5;

        private ChangeInfo mChange;
        private SubmitType mSubmitType;
        private final List<FileItemModel> mFiles = new ArrayList<>();
        private String mCurrentRevision;

        private final HeaderItemModel[] mHeaderModels;
        private final boolean mIsTwoPane;

        private final Picasso mPicasso;
        private final AccountInfo mBuildBotSystemAccount;

        public ChangeAdapter(ChangeDetailsFragment fragment) {
            final Resources res = fragment.getResources();
            mChange = null;
            mPicasso = PicassoHelper.getPicassoClient(fragment.getContext());
            mIsTwoPane = res.getBoolean(R.bool.config_is_two_pane);

            mBuildBotSystemAccount = new AccountInfo();
            mBuildBotSystemAccount.name = res.getString(R.string.account_build_bot_system_name);

            String[] titles = res.getStringArray(R.array.change_details_headers);
            int headers = titles.length;
            mHeaderModels = new HeaderItemModel[headers];
            for (int i = 0; i < headers; i++) {
                mHeaderModels[i] = new HeaderItemModel();
                mHeaderModels[i].title = titles[i];
            }
        }

        void update(DataResponse response) {
            mChange = response.mChange;
            mSubmitType = response.mSubmitType;
            if (mCurrentRevision == null || !mChange.revisions.containsKey(mCurrentRevision)) {
                mCurrentRevision = mChange.currentRevision;
            }

            mFiles.clear();
            Map<String, FileInfo> files =  mChange.revisions.get(mCurrentRevision).files;
            if (files != null) {
                for (String key : files.keySet()) {
                    FileItemModel model = new FileItemModel();
                    model.file = key;
                    model.info = files.get(key);
                    mFiles.add(model);
                }
            }
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            switch (viewType) {
                case HEADER_VIEW:
                    return new HeaderViewHolder(DataBindingUtil.inflate(
                            inflater, R.layout.header_item, parent, false));
                case CHANGE_AND_PATCH_SET_INFO_VIEW:
                    return new ChangeAndPatchSetInfoViewHolder(DataBindingUtil.inflate(
                            inflater, R.layout.change_and_patch_set_info_item, parent, false));
                case PATCH_SET_INFO_VIEW:
                    return new PatchSetInfoViewHolder(DataBindingUtil.inflate(
                            inflater, R.layout.patch_set_info_item, parent, false));
                case CHANGE_INFO_VIEW:
                    return new ChangeInfoViewHolder(DataBindingUtil.inflate(
                            inflater, R.layout.change_info_item, parent, false));
                case FILE_INFO_VIEW:
                    return new FileInfoViewHolder(DataBindingUtil.inflate(
                            inflater, R.layout.file_info_item, parent, false));
                case MESSAGE_VIEW:
                    return new MessageViewHolder(DataBindingUtil.inflate(
                            inflater, R.layout.message_item, parent, false));
            }
            return null;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof HeaderViewHolder) {
                HeaderViewHolder headerViewHolder = (HeaderViewHolder) holder;
                headerViewHolder.mBinding.setModel(getHeaderItemModelFromPosition(position));

            } else if (holder instanceof ChangeAndPatchSetInfoViewHolder) {
                ChangeAndPatchSetInfoViewHolder vh = (ChangeAndPatchSetInfoViewHolder) holder;

                // Headers
                vh.mBinding.setPatchSetHeader(mHeaderModels[0]);
                vh.mBinding.setChangeHeader(mHeaderModels[1]);

                // Patch set
                vh.mBinding.setChangeId(mChange.changeId);
                vh.mBinding.setRevisionId(mCurrentRevision);
                RevisionInfo revision = mChange.revisions.get(mCurrentRevision);
                vh.mBinding.setRevision(revision);
                vh.mBinding.patchSetItem.parentCommits.from(revision.commit);

                // Change
                vh.mBinding.changeItem.owner.with(mPicasso).from(mChange.owner);
                vh.mBinding.changeItem.reviewers.with(mPicasso).withRemovableReviewers(true)
                        .from(mChange);
                vh.mBinding.changeItem.labels.with(mPicasso).from(mChange);
                vh.mBinding.setChange(mChange);
                vh.mBinding.setSubmitType(mSubmitType);

            } else if (holder instanceof PatchSetInfoViewHolder) {
                PatchSetInfoViewHolder patchSetInfoViewHolder = (PatchSetInfoViewHolder) holder;
                patchSetInfoViewHolder.mBinding.setChangeId(mChange.changeId);
                patchSetInfoViewHolder.mBinding.setRevision(mCurrentRevision);
                RevisionInfo revision = mChange.revisions.get(mCurrentRevision);
                patchSetInfoViewHolder.mBinding.setModel(revision);
                patchSetInfoViewHolder.mBinding.parentCommits.from(revision.commit);

            } else if (holder instanceof ChangeInfoViewHolder) {
                ChangeInfoViewHolder changeInfoViewHolder = (ChangeInfoViewHolder) holder;
                changeInfoViewHolder.mBinding.owner.with(mPicasso).from(mChange.owner);
                changeInfoViewHolder.mBinding.reviewers.with(mPicasso).withRemovableReviewers(true)
                        .from(mChange);
                changeInfoViewHolder.mBinding.labels.with(mPicasso).from(mChange);
                changeInfoViewHolder.mBinding.setModel(mChange);
                changeInfoViewHolder.mBinding.setSubmitType(mSubmitType);

            } else if (holder instanceof FileInfoViewHolder) {
                FileInfoViewHolder fileInfoViewHolder = (FileInfoViewHolder) holder;
                fileInfoViewHolder.mBinding.setModel(getFileInfoFromPosition(position));

            } else if (holder instanceof MessageViewHolder) {
                ChangeMessageInfo message = getMessageFromPosition(position);
                if (message.author == null) {
                    message.author = mBuildBotSystemAccount;
                }
                MessageViewHolder messageViewHolder = (MessageViewHolder) holder;
                messageViewHolder.mBinding.setModel(message);
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (isHeaderView(position)) {
                return HEADER_VIEW;
            }
            if (isChangeAndPatchSetInfoView(position)) {
                return CHANGE_AND_PATCH_SET_INFO_VIEW;
            }
            if (isPatchSetInfoView(position)) {
                return PATCH_SET_INFO_VIEW;
            }
            if (isChangeInfoView(position)) {
                return CHANGE_INFO_VIEW;
            }
            if (isFileInfoView(position)) {
                return FILE_INFO_VIEW;
            }
            return MESSAGE_VIEW;
        }

        @Override
        public int getItemCount() {
            if (mChange == null) {
                return 0;
            }

            int headersItems = mHeaderModels.length;
            int patchSetInfoItems = 1;
            int changeInfoItems = 1;
            int fileItems = getFileItems();
            int messageItems = getMessageItems();
            return headersItems + changeInfoItems + patchSetInfoItems + fileItems + messageItems;
        }

        private boolean isHeaderView(int position) {
            if (mIsTwoPane) {
                return position == 1 || position == getFileItems() + 2;
            }
            return position == 0 || position == 2 || position == 4
                    || position == getFileItems() + 4 + 1;
        }

        private boolean isChangeAndPatchSetInfoView(int position) {
            return mIsTwoPane && position == 0;
        }

        private boolean isPatchSetInfoView(int position) {
            return !mIsTwoPane && position == 1;
        }

        private boolean isChangeInfoView(int position) {
            return !mIsTwoPane && position == 3;
        }

        private boolean isFileInfoView(int position) {
            if (mIsTwoPane) {
                return position > 1 && position <= (1 + getFileItems());
            }
            return position > 4 && position <= (4 + getFileItems());
        }

        private int getFileItems() {
            return mFiles.size();
        }

        private int getMessageItems() {
            ChangeMessageInfo[] messages =  mChange.messages;
            return messages == null ? 0 : messages.length;
        }

        private HeaderItemModel getHeaderItemModelFromPosition(int position) {
            if (!mIsTwoPane && position == 0) {
                return mHeaderModels[0];
            }
            if (!mIsTwoPane && position == 2) {
                return mHeaderModels[1];
            }
            if ((!mIsTwoPane && position == 4) || (mIsTwoPane && position == 1)) {
                return mHeaderModels[2];
            }
            return mHeaderModels[3];
        }

        private FileItemModel getFileInfoFromPosition(int position) {
            if (mIsTwoPane) {
                return mFiles.get(position - 2);
            }
            return mFiles.get(position - 5);
        }

        private ChangeMessageInfo getMessageFromPosition(int position) {
            if (mIsTwoPane) {
                return mChange.messages[position - 3 - getFileItems()];
            }
            return mChange.messages[position - 6 - getFileItems()];
        }
    }

    public static class DataResponse {
        ChangeInfo mChange;
        SubmitType mSubmitType;
    }

    private final RxLoaderObserver<DataResponse> mChangeObserver =
            new RxLoaderObserver<DataResponse>() {
                @Override
                public void onNext(DataResponse result) {
                    mModel.hasData = result != null;
                    mAdapter.update(result);
                    mAdapter.notifyDataSetChanged();
                    mBinding.setModel(mModel);
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

    private ChangeDetailsFragmentBinding mBinding;
    private ChangeAdapter mAdapter;

    private final Model mModel = new Model();
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
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(
                inflater, R.layout.change_details_fragment, container, false);
        mBinding.setModel(mModel);
        mBinding.refresh.setEnabled(false);
        return mBinding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Configure the adapter
        mAdapter = new ChangeAdapter(this);
        mBinding.list.setLayoutManager(new LinearLayoutManager(
                getActivity(), LinearLayoutManager.VERTICAL, false));
        mBinding.list.addItemDecoration(new DividerItemDecoration(
                getContext(), LinearLayoutManager.VERTICAL));
        mBinding.list.setAdapter(mAdapter);

        // Configure the refresh
        setupSwipeToRefresh();

        // Fetch or join current loader
        RxLoaderManager loaderManager = RxLoaderManagerCompat.get(this);
        mChangeLoader = loaderManager.create(this::fetchChange, mChangeObserver)
                .start(mChangeId);
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
        return api.getChange(changeId, OPTIONS)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .flatMap(change -> {
                    DataResponse response = new DataResponse();
                    response.mChange = change;
                    if (change != null && (mAdapter.mCurrentRevision == null
                            || change.currentRevision.equals(mAdapter.mCurrentRevision))) {
                        response.mSubmitType = api.getChangeRevisionSubmitType(
                                String.valueOf(change.legacyChangeId),
                                change.currentRevision).toBlocking().first();
                    }
                    return Observable.just(response);
                })
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

    private void showProgress(boolean show) {
        ((BaseActivity) getActivity()).changeInProgressStatus(show);
        mBinding.refresh.setEnabled(!show);
    }
}
