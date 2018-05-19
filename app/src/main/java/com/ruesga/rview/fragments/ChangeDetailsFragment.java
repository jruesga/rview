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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.OpenableColumns;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.ListPopupWindow;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.gson.reflect.TypeToken;
import com.ruesga.rview.BaseActivity;
import com.ruesga.rview.R;
import com.ruesga.rview.adapters.PatchSetsAdapter;
import com.ruesga.rview.adapters.SimpleDropDownAdapter;
import com.ruesga.rview.attachments.Attachment;
import com.ruesga.rview.attachments.AttachmentDropView.OnAttachmentsDroppedListener;
import com.ruesga.rview.attachments.AttachmentsProvider;
import com.ruesga.rview.attachments.AttachmentsProviderFactory;
import com.ruesga.rview.attachments.AttachmentsSupport;
import com.ruesga.rview.attachments.AuthenticationException;
import com.ruesga.rview.attachments.Provider;
import com.ruesga.rview.attachments.fragments.ProviderChooserFragment;
import com.ruesga.rview.attachments.services.AttachmentsContentUploadService;
import com.ruesga.rview.databinding.ChangeDetailsFragmentBinding;
import com.ruesga.rview.databinding.FileInfoItemBinding;
import com.ruesga.rview.databinding.MessageItemBinding;
import com.ruesga.rview.databinding.MoreFilesBinding;
import com.ruesga.rview.databinding.TotalAddedDeletedBinding;
import com.ruesga.rview.exceptions.OperationFailedException;
import com.ruesga.rview.gerrit.GerritApi;
import com.ruesga.rview.gerrit.filter.ChangeQuery;
import com.ruesga.rview.gerrit.model.AbandonInput;
import com.ruesga.rview.gerrit.model.AccountInfo;
import com.ruesga.rview.gerrit.model.ActionInfo;
import com.ruesga.rview.gerrit.model.AddReviewerResultInfo;
import com.ruesga.rview.gerrit.model.AddReviewerState;
import com.ruesga.rview.gerrit.model.ApprovalInfo;
import com.ruesga.rview.gerrit.model.AssigneeInfo;
import com.ruesga.rview.gerrit.model.AssigneeInput;
import com.ruesga.rview.gerrit.model.ChangeEditMessageInput;
import com.ruesga.rview.gerrit.model.ChangeInfo;
import com.ruesga.rview.gerrit.model.ChangeInput;
import com.ruesga.rview.gerrit.model.ChangeMessageInfo;
import com.ruesga.rview.gerrit.model.ChangeOptions;
import com.ruesga.rview.gerrit.model.ChangeStatus;
import com.ruesga.rview.gerrit.model.CherryPickInput;
import com.ruesga.rview.gerrit.model.CommentInfo;
import com.ruesga.rview.gerrit.model.ConfigInfo;
import com.ruesga.rview.gerrit.model.DeleteVoteInput;
import com.ruesga.rview.gerrit.model.DescriptionInput;
import com.ruesga.rview.gerrit.model.DraftActionType;
import com.ruesga.rview.gerrit.model.Features;
import com.ruesga.rview.gerrit.model.FileInfo;
import com.ruesga.rview.gerrit.model.HashtagsInput;
import com.ruesga.rview.gerrit.model.InitialChangeStatus;
import com.ruesga.rview.gerrit.model.MoveInput;
import com.ruesga.rview.gerrit.model.NotifyType;
import com.ruesga.rview.gerrit.model.PrivateInput;
import com.ruesga.rview.gerrit.model.RebaseInput;
import com.ruesga.rview.gerrit.model.RestoreInput;
import com.ruesga.rview.gerrit.model.RevertInput;
import com.ruesga.rview.gerrit.model.ReviewInfo;
import com.ruesga.rview.gerrit.model.ReviewInput;
import com.ruesga.rview.gerrit.model.ReviewerInput;
import com.ruesga.rview.gerrit.model.ReviewerStatus;
import com.ruesga.rview.gerrit.model.ReviewerUpdateInfo;
import com.ruesga.rview.gerrit.model.RevisionInfo;
import com.ruesga.rview.gerrit.model.SideType;
import com.ruesga.rview.gerrit.model.SubmitInput;
import com.ruesga.rview.gerrit.model.SubmitType;
import com.ruesga.rview.gerrit.model.TopicInput;
import com.ruesga.rview.gerrit.model.WorkInProgressInput;
import com.ruesga.rview.misc.ActivityHelper;
import com.ruesga.rview.misc.AndroidHelper;
import com.ruesga.rview.misc.BitmapUtils;
import com.ruesga.rview.misc.CacheHelper;
import com.ruesga.rview.misc.ContinuousIntegrationHelper;
import com.ruesga.rview.misc.ExceptionHelper;
import com.ruesga.rview.misc.FileHelper;
import com.ruesga.rview.misc.ModelHelper;
import com.ruesga.rview.misc.RviewImageHelper;
import com.ruesga.rview.misc.SerializationManager;
import com.ruesga.rview.misc.StringHelper;
import com.ruesga.rview.model.Account;
import com.ruesga.rview.model.ContinuousIntegrationInfo;
import com.ruesga.rview.model.EmptyState;
import com.ruesga.rview.model.Repository;
import com.ruesga.rview.model.UnresolvedComment;
import com.ruesga.rview.preferences.Constants;
import com.ruesga.rview.preferences.Preferences;
import com.ruesga.rview.widget.AccountChipView;
import com.ruesga.rview.widget.AccountChipView.OnAccountChipClickedListener;
import com.ruesga.rview.widget.AccountChipView.OnAccountChipRemovedListener;
import com.ruesga.rview.widget.AttachmentsView.OnAttachmentDroppedListener;
import com.ruesga.rview.widget.AttachmentsView.OnAttachmentPressedListener;
import com.ruesga.rview.widget.ContinuousIntegrationView.OnContinuousIntegrationPressed;
import com.ruesga.rview.widget.DividerItemDecoration;
import com.ruesga.rview.widget.LinesWithCommentsView.OnLineClickListener;
import com.ruesga.rview.widget.TagEditTextView.Tag;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import me.tatarka.rxloader2.RxLoader;
import me.tatarka.rxloader2.RxLoader1;
import me.tatarka.rxloader2.RxLoader2;
import me.tatarka.rxloader2.RxLoaderManager;
import me.tatarka.rxloader2.RxLoaderManagerCompat;
import me.tatarka.rxloader2.RxLoaderObserver;
import me.tatarka.rxloader2.safe.Empty;
import me.tatarka.rxloader2.safe.SafeObservable;

import static com.ruesga.rview.attachments.preferences.Constants.ATTACHMENT_PROVIDER_CHANGED_ACTION;

public class ChangeDetailsFragment extends Fragment implements
        AddReviewerDialogFragment.OnReviewerAdded,
        EditAssigneeDialogFragment.OnAssigneeSelected,
        FilterableDialogFragment.OnFilterSelectedListener,
        EditDialogFragment.OnEditChanged,
        ConfirmDialogFragment.OnActionConfirmed,
        TagEditDialogFragment.OnTagEditChanged,
        GalleryChooserFragment.OnGallerySelectedListener,
        SnippetFragment.OnSnippetSavedListener,
        ProviderChooserFragment.OnAttachmentProviderSelectedListener,
        FilesDialogFragment.OnFilePressed {

    private static final String TAG = "ChangeDetailsFragment";

    private static final List<ChangeOptions> OPTIONS = new ArrayList<ChangeOptions>() {{
        add(ChangeOptions.DETAILED_ACCOUNTS);
        add(ChangeOptions.DETAILED_LABELS);
        add(ChangeOptions.ALL_REVISIONS);
        add(ChangeOptions.ALL_FILES);
        add(ChangeOptions.ALL_COMMITS);
        add(ChangeOptions.MESSAGES);
        add(ChangeOptions.REVIEWED);
        add(ChangeOptions.CHANGE_ACTIONS);
        add(ChangeOptions.CHECK);
        add(ChangeOptions.WEB_LINKS);
        add(ChangeOptions.DOWNLOAD_COMMANDS);
        add(ChangeOptions.REVIEWER_UPDATES);
    }};
    private static final List<ChangeOptions> MESSAGES_OPTIONS = new ArrayList<ChangeOptions>() {{
        add(ChangeOptions.DETAILED_ACCOUNTS);
        add(ChangeOptions.MESSAGES);
        add(ChangeOptions.REVIEWER_UPDATES);
    }};

    private static final Pattern COMMENTS_PATTERN
            = Pattern.compile("(^|\\s)(\\(\\d+ (inline )?comment(s)?\\))$", Pattern.MULTILINE);

    private static final int DIFF_REQUEST_CODE = 99;
    private static final int EDIT_REQUEST_CODE = 98;

    private static final int REQUEST_CODE_REBASE = 0;
    private static final int REQUEST_CODE_CHERRY_PICK = 1;
    private static final int REQUEST_CODE_MOVE_BRANCH = 2;
    private static final int REQUEST_CODE_EDIT_MESSAGE = 3;
    private static final int REQUEST_CODE_CHANGE_TOPIC = 4;
    private static final int REQUEST_CODE_ABANDON_CHANGE = 5;
    private static final int REQUEST_CODE_RESTORE_CHANGE = 6;
    private static final int REQUEST_CODE_REVERT_CHANGE = 7;
    private static final int REQUEST_CODE_FOLLOW_UP_CHANGE = 8;
    private static final int REQUEST_CODE_SUBMIT_CHANGE = 9;
    private static final int REQUEST_CODE_EDIT_REVISION_DESCRIPTION = 10;
    private static final int REQUEST_CODE_MARK_PRIVATE = 11;
    private static final int REQUEST_CODE_MARK_WIP = 12;
    private static final int REQUEST_CODE_TAGS = 97;
    private static final int REQUEST_CODE_URL_CHOOSER = 98;

    private static final int REQUEST_ATTACHMENT_CAMERA = 1001;
    private static final int REQUEST_ATTACHMENT_FILE = 1002;

    @Keep
    public static class ListModel {
        @StringRes
        public int header;
        public String selector;
        public boolean visible;
        public boolean empty;
        public int emptyText;
        public final Map<String, String> actions = new HashMap<>();
        private ListModel(int headerLabel, int emptyTextLabel) {
            header = headerLabel;
            selector = null;
            visible = false;
            empty = true;
            emptyText = emptyTextLabel;
        }
    }

    @Keep
    public static class Model {
        boolean isLocked = false;
        boolean isAuthenticated = false;
        public ListModel filesListModel = new ListModel(R.string.change_details_header_files,
                R.string.change_details_header_files_empty);
        public ListModel msgListModel = new ListModel(R.string.change_details_header_messages,
                R.string.change_details_header_messages_empty);
    }

    @Keep
    @SuppressWarnings({"UnusedParameters", "unused"})
    public static class EventHandlers {
        private final ChangeDetailsFragment mFragment;

        public EventHandlers(ChangeDetailsFragment fragment) {
            mFragment = fragment;
        }

        public void onPatchSetPressed(View v) {
            mFragment.showPatchSetChooser(v);
        }

        public void onListHeaderSelectorPressed(View v) {
            String id = (String) v.getTag();
            if (!TextUtils.isEmpty(id)) {
                switch (id) {
                    case "files":
                        mFragment.showDiffAgainstChooser(v);
                        break;
                }
            }
        }

        public void onListHeaderActionPressed(View v) {
            String id = (String) v.getTag();
            if (!TextUtils.isEmpty(id)) {
                switch (id) {
                    case "files-action1":
                        mFragment.showEditChangeActivity();
                        break;

                    case "messages-action1":
                        mFragment.toggleTaggedMessages();
                        break;
                    case "messages-action2":
                        mFragment.toggleCIMessages();
                        break;
                }
            }
        }

        public void onStarredPressed(View v) {
            mFragment.performStarred(!v.isSelected());
        }

        public void onSharePressed(View v) {
            mFragment.performShare();
        }

        public void onEditAssigneePressed(View v) {
            mFragment.performShowEditAssigneeDialog(v);
        }

        public void onAddReviewerPressed(View v) {
            mFragment.performShowAddReviewerDialog(AddReviewerState.REVIEWER, v);
        }

        public void onAddCCPressed(View v) {
            mFragment.performShowAddReviewerDialog(AddReviewerState.CC, v);
        }

        public void onAddMeAsReviewerPressed(View v) {
            Account account = Preferences.getAccount(v.getContext());
            if (account != null) {
                mFragment.onReviewerAdded(
                        String.valueOf(account.mAccount.accountId),
                        AddReviewerState.REVIEWER);
            }
        }

        public void onBranchEditPressed(View v) {
            mFragment.performShowMoveBranchDialog(v);
        }

        public void onTopicEditPressed(View v) {
            mFragment.performShowChangeTopicDialog(v);
        }

        public void onTagsEditPressed(View v) {
            mFragment.performShowChangeTagsDialog(v);
        }

        public void onRelatedChangesPressed(View v) {
            mFragment.performOpenRelatedChanges();
        }

        public void onIncludedInPressed(View v) {
            mFragment.performOpenIncludedInDialog();
        }

        public void onDownloadPatchSetPressed(View v) {
            mFragment.performOpenDownloadDialog();
        }

        public void onViewPatchSetPressed(View v) {
            mFragment.performViewPatchSet();
        }

        public void onEditMessagePressed(View v) {
            mFragment.performEditMessage(v);
        }

        public void onReviewPressed(View v) {
            mFragment.performReview();
        }

        public void onReplyCommentPressed(View v) {
            mFragment.performReplyComment((int) v.getTag());
        }

        public void onActionPressed(View v) {
            mFragment.performAction(v.getId(), v);
        }

        public void onWebLinkPressed(View v) {
            String url = (String) v.getTag();
            if (url != null) {
                ActivityHelper.openUriInCustomTabs(mFragment.getActivity(),
                        ActivityHelper.resolveRepositoryUri(v.getContext(), url));
            }
        }

        public void onFileItemPressed(View v) {
            mFragment.performOpenFileDiff((String) v.getTag());
        }

        public void onMoreFilesPressed(View v) {
            mFragment.performShowMoreFiles(v);
        }

        public void onApplyFilterPressed(View v) {
            mFragment.performApplyFilter(v);
        }

        public void onMessageAvatarPressed(View v) {
            int position = (int) v.getTag();
            AccountInfo account = mFragment.mResponse.mChange.messages[position].author;
            onAccountChipPressed(account, null);
        }

        public void onMessagePressed(View v) {
            int position = (int) v.getTag();
            mFragment.performMessageClick(position);
        }

        private void onNavigateToComment(View v) {
            CommentInfo comment = (CommentInfo) v.getTag();
            mFragment.performNavigateToComment(comment);
        }

        public void onEditRevisionDescriptionPressed(View v) {
            mFragment.performEditRevisionDescription(v);
        }

        private void onAttachmentPressed(Attachment attachment) {
            mFragment.performOpenAttachment(attachment);
        }

        private void onAttachmentDropped(Attachment attachment) {
            mFragment.performDropAttachment(attachment);
        }

        public void onAttachmentChooser(View v) {
            String action = (String) v.getTag();
            mFragment.performOpenAttachmentChooser(v, action);
        }

        public void onSearchPressed(View v) {
            mFragment.performShowMoreFiles(v);
        }

        private void onAccountChipPressed(AccountInfo account, Object tag) {
            mFragment.performAccountClicked(account, tag);
        }
    }

    @Keep
    public static class FileItemModel {
        public String file;
        public FileInfo info;
        public int totalAdded;
        public int totalDeleted;
        public boolean hasGraph = true;
        public int inlineComments;
        public int draftComments;
    }

    private static class FileInfoViewHolder extends RecyclerView.ViewHolder {
        private final FileInfoItemBinding mBinding;
        FileInfoViewHolder(FileInfoItemBinding binding) {
            super(binding.getRoot());
            mBinding = binding;
            binding.executePendingBindings();
        }
    }

    private static class TotalAddedDeletedViewHolder extends RecyclerView.ViewHolder {
        private final TotalAddedDeletedBinding mBinding;
        TotalAddedDeletedViewHolder(TotalAddedDeletedBinding binding) {
            super(binding.getRoot());
            mBinding = binding;
            binding.executePendingBindings();
        }
    }

    private static class MoreFilesViewHolder extends RecyclerView.ViewHolder {
        private final MoreFilesBinding mBinding;
        MoreFilesViewHolder(MoreFilesBinding binding) {
            super(binding.getRoot());
            mBinding = binding;
            binding.executePendingBindings();
        }
    }

    private static class MessageViewHolder extends RecyclerView.ViewHolder {
        private final MessageItemBinding mBinding;
        MessageViewHolder(MessageItemBinding binding, boolean isMessagesFolded) {
            super(binding.getRoot());
            mBinding = binding;
            mBinding.setFolded(isMessagesFolded);
            binding.executePendingBindings();
        }
    }

    private static class FileAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final List<FileItemModel> mFiles = new ArrayList<>();
        private final List<FileItemModel> mAllFiles = new ArrayList<>();
        private FileItemModel mTotals;
        private final EventHandlers mEventHandlers;
        private Boolean mIsShortFilenames;
        private boolean mHasSplitFiles;

        // This limit will split items into another screen to avoid freeze the screen, caused
        // by the nested recycler view.
        private static final int MAX_ITEMS = 50;

        private static final int FILE_ITEM_VIEW_TYPE = 0;
        private static final int TOTAL_ITEM_VIEW_TYPE = 1;
        private static final int MORE_ITEMS_VIEW_TYPE = 2;

        FileAdapter(EventHandlers handlers, boolean isShortFilenames) {
            mEventHandlers = handlers;
            mIsShortFilenames = isShortFilenames;
        }

        List<FileItemModel> getAllItems() {
            return new ArrayList<>(mAllFiles);
        }

        void update(ListModel listModel, Map<String, FileInfo> files, Map<String,
                Integer> inlineComments, Map<String, Integer> draftComments) {
            mFiles.clear();
            mAllFiles.clear();
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
                // Do not compute commit message
                if (key.equals(Constants.COMMIT_MESSAGE)) {
                    continue;
                }

                FileInfo info = files.get(key);
                if (info.linesInserted != null) {
                    added += info.linesInserted;
                }
                if (info.linesDeleted != null) {
                    deleted += info.linesDeleted;
                }
            }

            // Create a model from each file
            int count = 0;
            for (String key : files.keySet()) {
                FileItemModel model = new FileItemModel();
                model.file = key;
                model.info = files.get(key);
                model.totalAdded = added;
                model.totalDeleted = deleted;
                model.inlineComments =
                        inlineComments.containsKey(key) ? inlineComments.get(key) : 0;
                model.draftComments =
                        draftComments.containsKey(key) ? draftComments.get(key) : 0;
                model.hasGraph = !key.equals(Constants.COMMIT_MESSAGE) &&
                        ((model.info.linesInserted != null && model.info.linesInserted > 0) ||
                                (model.info.linesDeleted != null && model.info.linesDeleted > 0) ||
                                model.inlineComments > 0 || model.draftComments > 0);
                if (key.equals(Constants.COMMIT_MESSAGE)) {
                    mFiles.add(0, model);
                    mAllFiles.add(0, model);
                } else {
                    if (count < MAX_ITEMS) {
                        mFiles.add(model);
                    }
                    mAllFiles.add(model);
                }
                count++;
            }

            mHasSplitFiles = mFiles.size() != mAllFiles.size();

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

            listModel.empty = mFiles.isEmpty();
            notifyDataSetChanged();
        }

        @Override
        public int getItemCount() {
            return mFiles.size() + (mHasSplitFiles ? 1 : 0) + (mTotals != null ? 1 : 0);
        }

        @Override
        public int getItemViewType(int position) {
            if (position == getItemCount() - 1) {
                return TOTAL_ITEM_VIEW_TYPE;
            }
            if (mHasSplitFiles && position == getItemCount() - 2) {
                return MORE_ITEMS_VIEW_TYPE;
            }
            return FILE_ITEM_VIEW_TYPE;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            if (viewType == TOTAL_ITEM_VIEW_TYPE) {
                return new TotalAddedDeletedViewHolder(DataBindingUtil.inflate(
                        inflater, R.layout.total_added_deleted, parent, false));
            } else if (viewType == MORE_ITEMS_VIEW_TYPE) {
                return new MoreFilesViewHolder(DataBindingUtil.inflate(
                        inflater, R.layout.more_files, parent, false));
            }
            return new FileInfoViewHolder(DataBindingUtil.inflate(
                    inflater, R.layout.file_info_item, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof TotalAddedDeletedViewHolder) {
                TotalAddedDeletedBinding binding = ((TotalAddedDeletedViewHolder) holder).mBinding;
                binding.addedVsDeleted.with(mTotals);
                binding.setModel(mTotals);
            } else if (holder instanceof MoreFilesViewHolder) {
                MoreFilesBinding binding = ((MoreFilesViewHolder) holder).mBinding;
                binding.setMore(mAllFiles.size() - mFiles.size());
                binding.setHandlers(mEventHandlers);
            } else {
                FileItemModel model = mFiles.get(position);
                FileInfoItemBinding binding = ((FileInfoViewHolder) holder).mBinding;
                binding.addedVsDeleted.with(model);
                binding.setIsShortFileName(mIsShortFilenames);
                binding.setModel(model);
                binding.setHandlers(mEventHandlers);
            }
        }
    }

    private static class MessageAdapter extends RecyclerView.Adapter<MessageViewHolder> {
        private final AccountInfo mBuildBotSystemAccount;
        private EventHandlers mEventHandlers;
        private ChangeMessageInfo[] mMessages;
        private Map<String, LinkedHashMap<String, List<CommentInfo>>> mMessagesWithComments;
        private boolean[] mFolded;
        private final boolean mIsAuthenticated;
        private final boolean mIsFolded;

        private boolean mIsHideTaggedMessages;
        private Repository mRepository;

        private final OnLineClickListener mLineClickListener =
                v -> mEventHandlers.onNavigateToComment(v);

        private final OnAttachmentPressedListener mAttachmentPressedListener
                = attachment -> mEventHandlers.onAttachmentPressed(attachment);

        private final AccountChipView.OnAccountChipClickedListener mAccountPressedListener
                = (account, tag) -> mEventHandlers.onAccountChipPressed(account, tag);

        MessageAdapter(ChangeDetailsFragment fragment, EventHandlers handlers,
                boolean isAuthenticated, boolean isFolded) {
            final Resources res = fragment.getResources();
            mEventHandlers = handlers;
            mIsAuthenticated = isAuthenticated;
            mIsFolded = isFolded;

            mBuildBotSystemAccount = new AccountInfo();
            mBuildBotSystemAccount.name = res.getString(R.string.account_build_bot_system_name);
        }

        void changeFoldedStatus(int position) {
            mFolded[position] = !mFolded[position];
            notifyItemChanged(position);
        }

        void updateHideTaggedMessages(boolean isHideTaggedMessages) {
            mIsHideTaggedMessages = isHideTaggedMessages;
        }

        void updateHideCIMessages(Repository repo) {
            mRepository = repo;
        }

        void update(ListModel listModel, ChangeInfo change,
                Map<String, LinkedHashMap<String, List<CommentInfo>>> messagesWithComments,
                ReviewerUpdateInfo[] reviewerUpdates) {
            mMessages = filterTaggedMessages(filterCiAccountsMessages(
                    joinWithReviewerUpdates(change.messages, reviewerUpdates)));
            mMessagesWithComments = messagesWithComments;
            change.messages = mMessages;

            int count = mMessages.length;
            boolean[] old = mFolded;
            mFolded = new boolean[count];
            for (int i = 0; i < count; i++) {
                mFolded[i] = old != null && old.length > i ? old[i] : mIsFolded;
            }

            listModel.empty = count == 0;
            notifyDataSetChanged();
        }

        @Override
        public int getItemCount() {
            return mMessages != null ? mMessages.length : 0;
        }

        String getMessage(int position) {
            return mMessages[position].message;
        }

        @NonNull
        @Override
        public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            return new MessageViewHolder(DataBindingUtil.inflate(
                    inflater, R.layout.message_item, parent, false), mIsFolded);
        }

        @Override
        public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
            final Context context = holder.mBinding.getRoot().getContext();
            ChangeMessageInfo message = mMessages[position];
            if (message.author == null) {
                message.author = mBuildBotSystemAccount;
            }
            Map<String, List<CommentInfo>> comments = mMessagesWithComments.get(message.id);

            List<Attachment> attachments = StringHelper.extractAllAttachments(message);
            RviewImageHelper.bindAvatar(context, message.author, holder.mBinding.avatar,
                    RviewImageHelper.getDefaultAvatar(context, R.color.primaryDarkForeground));
            if (ModelHelper.isOnBehalfOf(message)) {
                RviewImageHelper.bindAvatar(context, message.realAuthor, holder.mBinding.onBehalfOfAvatar,
                        RviewImageHelper.getDefaultAvatar(context, R.color.primaryDarkForeground));
            }
            holder.mBinding.setIsAuthenticated(mIsAuthenticated);
            holder.mBinding.setIndex(position);
            holder.mBinding.setModel(message);
            holder.mBinding.reviewerUpdates
                    .listenOn(mAccountPressedListener)
                    .from(message._reviewer_updates);
            holder.mBinding.comments
                    .listenOn(mLineClickListener)
                    .from(comments);
            holder.mBinding.attachmentsView
                    .listenOn(mAttachmentPressedListener)
                    .from(attachments);
            holder.mBinding.setFolded(mFolded[position]);
            holder.mBinding.setAttachments(attachments);
            holder.mBinding.setHandlers(mEventHandlers);
            holder.mBinding.setFoldHandlers(mIsFolded ? mEventHandlers : null);
        }

        private ChangeMessageInfo[] filterTaggedMessages(ChangeMessageInfo[] messages) {
            if (!mIsHideTaggedMessages) {
                return messages;
            }

            ArrayList<ChangeMessageInfo> msgs = new ArrayList<>();
            for (ChangeMessageInfo msg : messages) {
                if (TextUtils.isEmpty(msg.tag)) {
                    msgs.add(msg);
                }
            }
            return msgs.toArray(new ChangeMessageInfo[msgs.size()]);
        }

        private ChangeMessageInfo[] filterCiAccountsMessages(ChangeMessageInfo[] messages) {
            if (mRepository == null || TextUtils.isEmpty(mRepository.mCiAccounts)) {
                return messages;
            }

            Pattern pattern = Pattern.compile(mRepository.mCiAccounts, Pattern.MULTILINE);
            ArrayList<ChangeMessageInfo> msgs = new ArrayList<>();
            for (ChangeMessageInfo msg : messages) {
                if (msg.author == null || msg.author.name == null ||
                        !pattern.matcher(msg.author.name).matches()) {
                    msgs.add(msg);
                }
            }
            return msgs.toArray(new ChangeMessageInfo[msgs.size()]);
        }

        private ChangeMessageInfo[] joinWithReviewerUpdates(ChangeMessageInfo[] messages,
                ReviewerUpdateInfo[] reviewerUpdate) {
            if (mIsHideTaggedMessages) {
                return messages;
            }
            if (reviewerUpdate == null || reviewerUpdate.length == 0) {
                return messages;
            }

            List<ChangeMessageInfo> msgs = new ArrayList<>(Arrays.asList(messages));
            for (ReviewerUpdateInfo ru : reviewerUpdate) {
                int i = 0;
                boolean found = false;
                Iterator<ChangeMessageInfo> it = msgs.iterator();
                while (it.hasNext()) {
                    ChangeMessageInfo msg = it.next();
                    if (msg.date.compareTo(ru.updated) == 0) {
                        if (!(ModelHelper.AUTOGENERATED_TAG_REVIEWER_UPDATE.equals(msg.tag) ||
                                ModelHelper.AUTOGENERATED_TAG_DELETE_REVIEWER.equals(msg.tag))) {
                            msgs.add(i, ModelHelper.createReviewerUpdateMessage(ru));
                            found = true;
                            break;
                        }

                        if (found) {
                            it.remove();
                            continue;
                        }
                        msg._reviewer_updates.add(ru);
                        msg.tag = ModelHelper.AUTOGENERATED_TAG_REVIEWER_UPDATE;
                        found = true;

                    } else if (msg.date.compareTo(ru.updated) > 0) {
                        if (found) {
                            break;
                        }
                        msgs.add(i, ModelHelper.createReviewerUpdateMessage(ru));
                        found = true;
                        break;
                    }
                    i++;
                }

                if (!found) {
                    msgs.add(ModelHelper.createReviewerUpdateMessage(ru));
                }
            }

            return msgs.toArray(new ChangeMessageInfo[msgs.size()]);
        }
    }

    private static class DataResponse {
        ChangeInfo mChange;
        SubmitType mSubmitType;
        Map<String, FileInfo> mFiles;
        Map<String, ActionInfo> mActions;
        Map<String, Integer> mInlineComments;
        Map<String, Integer> mDraftComments;
        ConfigInfo mProjectConfig;
        Map<String, LinkedHashMap<String, List<CommentInfo>>> mMessagesWithComments = new HashMap<>();
        Map<Integer, UnresolvedComment> mUnresolvedComments = new HashMap<>();
        List<ContinuousIntegrationInfo> mCI;
    }

    private final RxLoaderObserver<DataResponse> mChangeObserver =
            new RxLoaderObserver<DataResponse>() {
        @Override
        public void onNext(DataResponse result) {
            mResponse = result;

            mModel.isLocked = false;
            updateLocked();

            updateAuthenticatedAndOwnerStatus();

            ChangeInfo change = null;
            mEmptyState.state = result != null
                    ? EmptyState.NORMAL_STATE : EmptyState.NO_RESULTS_STATE;
            mBinding.setEmpty(mEmptyState);
            if (result != null) {
                change = result.mChange;
                if (TextUtils.isEmpty(mCurrentRevision)
                        || !change.revisions.containsKey(mCurrentRevision)) {
                    mCurrentRevision = ModelHelper.extractBestRevisionId(change);
                }
                //noinspection ConstantConditions
                ((BaseActivity) getActivity()).setAnalyticsBase(mCurrentRevision);

                // Check supported features
                final GerritApi api = ModelHelper.getGerritApi(getActivity());
                boolean supportTaggedMessages = api != null
                        && api.supportsFeature(Features.TAGGED_MESSAGES);
                Repository repo = ModelHelper.findRepositoryForAccount(getContext(), mAccount);
                boolean supportCIMessages = repo != null && !TextUtils.isEmpty(repo.mCiAccounts);

                sortRevisions(change);
                updatePatchSetInfo(result);
                updateChangeInfo(result);
                updateReviewInfo(result);

                mModel.filesListModel.selector = resolveDiffAgainstSelectorText();
                mModel.filesListModel.visible = result.mFiles != null && !result.mFiles.isEmpty();
                mModel.filesListModel.actions.clear();
                ChangeStatus status = result.mChange.status;
                if (mModel.isAuthenticated
                        && !ChangeStatus.MERGED.equals(status)
                        && !ChangeStatus.ABANDONED.equals(status)
                        && mCurrentRevision.equals(result.mChange.currentRevision)) {
                    mModel.filesListModel.actions.put("action1", getString(R.string.action_edit));
                }
                mFileAdapter.update(mModel.filesListModel, result.mFiles,
                        result.mInlineComments, result.mDraftComments);
                mModel.msgListModel.visible =
                        change.messages != null && change.messages.length > 0;
                if (supportTaggedMessages) {
                    mModel.msgListModel.actions.put("action1", getString(mHideTaggedMessages
                            ? R.string.change_details_action_show_tagged_messages
                            : R.string.change_details_action_hide_tagged_messages));
                }
                if (supportCIMessages) {
                    mModel.msgListModel.actions.put("action2", getString(mHideCIMessages
                            ? R.string.change_details_action_show_ci_messages
                            : R.string.change_details_action_hide_ci_messages));
                }
                mMessageAdapter.update(mModel.msgListModel, change,
                        result.mMessagesWithComments, change.reviewerUpdates);
            }

            // Invalidate the diff cache. we have new data
            CacheHelper.removeAccountDiffCacheDir(getContext());

            mBinding.setModel(mModel);
            mBinding.setHandlers(mEventHandlers);
            showProgress(false, change);
        }

        @Override
        public void onError(Throwable error) {
            mModel.isLocked = false;
            updateLocked();

            mEmptyState.state = ExceptionHelper.resolveEmptyState(error);
            mBinding.setEmpty(mEmptyState);
            mChangeLoader.clear();
            //noinspection ConstantConditions
            ((BaseActivity) getActivity()).handleException(TAG, error, mEmptyHandlers);
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
            if (mResponse == null) {
                return;
            }

            mResponse.mChange.starred = value;
            updateChangeInfo(mResponse);

            mStarredLoader.clear();
        }

        @Override
        public void onError(Throwable error) {
            //noinspection ConstantConditions
            ((BaseActivity) getActivity()).handleException(TAG, error, mEmptyHandlers);

            mStarredLoader.clear();
        }
    };

    private final RxLoaderObserver<Boolean> mChangeEditMessageObserver
            = new RxLoaderObserver<Boolean>() {
        @Override
        public void onNext(Boolean value) {
            // Switch to the new revision
            mCurrentRevision = mDiffAgainstRevision = null;
            //noinspection ConstantConditions
            ((BaseActivity) getActivity()).setAnalyticsBase(mCurrentRevision);
            forceRefresh();

            mChangeEditMessageLoader.clear();
        }

        @Override
        public void onError(Throwable error) {
            //noinspection ConstantConditions
            ((BaseActivity) getActivity()).handleException(TAG, error, mEmptyHandlers);

            mChangeEditMessageLoader.clear();
        }
    };

    private final RxLoaderObserver<Boolean> mChangeEditRevisionDescriptionObserver
            = new RxLoaderObserver<Boolean>() {
        @Override
        public void onNext(Boolean value) {
            forceRefresh();

            mChangeEditRevisionDescriptionLoader.clear();
        }

        @Override
        public void onError(Throwable error) {
            //noinspection ConstantConditions
            ((BaseActivity) getActivity()).handleException(TAG, error, mEmptyHandlers);

            mChangeEditRevisionDescriptionLoader.clear();
        }
    };

    private final RxLoaderObserver<Pair<ReviewInput, ReviewInfo>> mReviewObserver
            = new RxLoaderObserver<Pair<ReviewInput, ReviewInfo>>() {

        private Runnable mUiDelayedProcessingNotification = () -> internalSetProcessing(true);

        @Override
        public void onNext(Pair<ReviewInput, ReviewInfo> review) {
            setProcessing(false);
            mReviewLoader.clear();

            // CleanUp attachment list
            ArrayList<Attachment> attachments = new ArrayList<>(mAttachments);
            mAttachments.clear();
            mBinding.reviewInfo.setAttachmentsSupport(mAttachmentsSupport);

            // Clean the message box
            mBinding.reviewInfo.reviewComment.setText(null);
            //noinspection ConstantConditions
            AndroidHelper.hideSoftKeyboard(getContext(), getActivity().getWindow());

            // Update the messages (since it was update at server side, we can temporary
            // update the message list until a full refresh happens)
            ModelHelper.updateChangeMessageInfo(
                    getActivity(), mAccount, mResponse.mChange, review.first);
            mMessageAdapter.update(mModel.msgListModel, mResponse.mChange,
                    mResponse.mMessagesWithComments, mResponse.mChange.reviewerUpdates);

            // Fetch the whole change
            forceRefresh();

            // Upload the content of the attachments (skip url shortcuts)
            if (!attachments.isEmpty()) {
                AttachmentsContentUploadService.enqueueWork(getActivity(), attachments);
            }
        }

        @Override
        public void onError(Throwable error) {
            setProcessing(false);
            //noinspection ConstantConditions
            ((BaseActivity) getActivity()).handleException(TAG, error, mEmptyHandlers);

            // Try to update attachment bar in case provider was invalidated
            mBinding.reviewInfo.setAttachmentsSupport(mAttachmentsSupport);

            mReviewLoader.clear();
        }

        @Override
        public void onStarted() {
            setProcessing(true);
        }

        private void setProcessing(boolean locked) {
            mUiHandler.removeCallbacks(mUiDelayedProcessingNotification);
            if (locked) {
                mUiHandler.postDelayed(mUiDelayedProcessingNotification, 300L);
            } else {
                internalSetProcessing(false);
            }
        }

        private void internalSetProcessing(boolean locked) {
            mBinding.reviewInfo.setIsProcessing(locked);
            mModel.isLocked = locked;
            updateLocked();
        }
    };

    private final RxLoaderObserver<String> mChangeTopicObserver = new RxLoaderObserver<String>() {
        @Override
        public void onNext(String newTopic) {
            if (mResponse == null) {
                return;
            }

            mResponse.mChange.topic = newTopic;
            updateChangeInfo(mResponse);

            // Refresh messages
            performMessagesRefresh();

            mChangeTopicLoader.clear();
        }

        @Override
        public void onError(Throwable error) {
            //noinspection ConstantConditions
            ((BaseActivity) getActivity()).handleException(TAG, error, mEmptyHandlers);

            mChangeTopicLoader.clear();
        }
    };

    private final RxLoaderObserver<String[]> mChangeTagsObserver = new RxLoaderObserver<String[]>() {
        @Override
        public void onNext(String[] newTags) {
            if (mResponse == null) {
                return;
            }

            mResponse.mChange.hashtags = newTags;
            updateChangeInfo(mResponse);

            performMessagesRefresh();

            mChangeTagsLoader.clear();
        }

        @Override
        public void onError(Throwable error) {
            //noinspection ConstantConditions
            ((BaseActivity) getActivity()).handleException(TAG, error, mEmptyHandlers);

            mChangeTagsLoader.clear();
        }
    };

    private final RxLoaderObserver<ChangeInfo> mMessagesRefreshObserver
            = new RxLoaderObserver<ChangeInfo>() {
        @Override
        public void onNext(ChangeInfo change) {
            if (mResponse == null) {
                return;
            }

            // We don't fetch messages's comments from this observer, but this
            // only happens from topic change, which implies a partial refresh
            // and the possibility that we are out-of-sync is low, compared to
            // the effort of fetching messages and comments (a user refresh
            // will fix the out-of-sync problem).
            mResponse.mChange.messages = change.messages;
            mResponse.mChange.reviewerUpdates = change.reviewerUpdates;
            mModel.msgListModel.visible = change.messages != null && change.messages.length > 0;
            mMessageAdapter.update(mModel.msgListModel, change,
                    mResponse.mMessagesWithComments, mResponse.mChange.reviewerUpdates);
            mBinding.setModel(mModel);
        }

        @Override
        public void onError(Throwable error) {
            //noinspection ConstantConditions
            ((BaseActivity) getActivity()).handleException(TAG, error, mEmptyHandlers);

            mMessagesRefreshLoader.clear();
        }
    };

    private final RxLoaderObserver<Map<String, Integer>> mDraftsRefreshObserver
            = new RxLoaderObserver<Map<String, Integer>>() {
        @Override
        public void onNext(Map<String, Integer> drafts) {
            if (mResponse == null) {
                return;
            }

            mResponse.mDraftComments = drafts;

            mModel.filesListModel.visible = mResponse.mFiles != null && !mResponse.mFiles.isEmpty();
            mFileAdapter.update(mModel.filesListModel, mResponse.mFiles,
                    mResponse.mInlineComments, mResponse.mDraftComments);
            mBinding.setModel(mModel);

            mDraftsRefreshLoader.clear();
        }

        @Override
        public void onError(Throwable error) {
            //noinspection ConstantConditions
            ((BaseActivity) getActivity()).handleException(TAG, error, mEmptyHandlers);

            mDraftsRefreshLoader.clear();
        }
    };

    private final RxLoaderObserver<AccountInfo> mRemoveReviewerObserver
            = new RxLoaderObserver<AccountInfo>() {
        @Override
        public void onNext(AccountInfo account) {
            if (mResponse == null) {
                return;
            }

            // Update internal objects
            if (mResponse.mChange.reviewers != null) {
                for (ReviewerStatus status : mResponse.mChange.reviewers.keySet()) {
                    AccountInfo[] reviewers = mResponse.mChange.reviewers.get(status);
                    mResponse.mChange.reviewers.put(status,
                            ModelHelper.removeAccount(getActivity(), account, reviewers));
                }
            }
            if (mResponse.mChange.labels != null) {
                for (String label : mResponse.mChange.labels.keySet()) {
                    ApprovalInfo[] approvals = mResponse.mChange.labels.get(label).all;
                    mResponse.mChange.labels.get(label).all =
                            ModelHelper.removeApproval(account, approvals);
                }
            }
            mResponse.mChange.removableReviewers = ModelHelper.removeAccount(
                    getActivity(), account, mResponse.mChange.removableReviewers);

            updateChangeInfo(mResponse);

            performMessagesRefresh();

            mRemoveReviewerLoader.clear();
        }

        @Override
        public void onError(Throwable error) {
            //noinspection ConstantConditions
            ((BaseActivity) getActivity()).handleException(TAG, error, mEmptyHandlers);

            mRemoveReviewerLoader.clear();
        }
    };

    private final RxLoaderObserver<Pair<String, AccountInfo>> mRemoveReviewerVoteObserver
            = new RxLoaderObserver<Pair<String, AccountInfo>>() {
        @Override
        public void onNext(Pair<String, AccountInfo> vote) {
            if (mResponse == null) {
                return;
            }

            // Update internal objects
            if (mResponse.mChange.labels != null &&
                    mResponse.mChange.labels.containsKey(vote.first)) {
                mResponse.mChange.labels.get(vote.first).all =
                    ModelHelper.removeApproval(vote.second,
                            mResponse.mChange.labels.get(vote.first).all);
            }
            updateChangeInfo(mResponse);

            performMessagesRefresh();

            mRemoveReviewerVoteLoader.clear();
        }

        @Override
        public void onError(Throwable error) {
            //noinspection ConstantConditions
            ((BaseActivity) getActivity()).handleException(TAG, error, mEmptyHandlers);

            mRemoveReviewerVoteLoader.clear();
        }
    };

    private final RxLoaderObserver<AddReviewerResultInfo> mAddReviewerObserver
            = new RxLoaderObserver<AddReviewerResultInfo>() {
        @Override
        public void onNext(AddReviewerResultInfo result) {
            if (mResponse == null) {
                return;
            }

            if (!TextUtils.isEmpty(result.error) || result.confirm) {
                onError(new OperationFailedException(String.format(Locale.US,
                        "error: %s; confirm: %s", result.error, String.valueOf(result.confirm))));
                return;
            }

            // Update internal objects

            // Update reviewers
            if (mResponse.mChange.reviewers == null) {
                mResponse.mChange.reviewers = new HashMap<>();
            }
            if (result.reviewers != null) {
                AccountInfo[] reviewers = mResponse.mChange.reviewers.get(ReviewerStatus.REVIEWER);
                if (reviewers != null) {
                    mResponse.mChange.reviewers.put(ReviewerStatus.REVIEWER,
                            ModelHelper.addReviewers(result.reviewers, reviewers));
                } else {
                    mResponse.mChange.reviewers.put(ReviewerStatus.REVIEWER, result.reviewers);
                }
            }
            if (result.ccs != null) {
                AccountInfo[] ccs = mResponse.mChange.reviewers.get(ReviewerStatus.CC);
                if (ccs != null) {
                    mResponse.mChange.reviewers.put(ReviewerStatus.CC,
                            ModelHelper.addReviewers(result.ccs, ccs));
                } else {
                    mResponse.mChange.reviewers.put(ReviewerStatus.CC, result.ccs);
                }
            }

            // Update labels
            if (result.reviewers != null && mResponse.mChange.labels != null) {

                for (String label : mResponse.mChange.labels.keySet()) {
                    ApprovalInfo[] approvals = mResponse.mChange.labels.get(label).all;
                    mResponse.mChange.labels.get(label).all =
                            ModelHelper.updateApprovals(result.reviewers, label, approvals);
                }
            }
            ModelHelper.updateRemovableReviewers(getContext(), mResponse.mChange, result);

            updateChangeInfo(mResponse);

            mAddReviewerLoader.clear();
        }

        @Override
        public void onError(Throwable error) {
            //noinspection ConstantConditions
            ((BaseActivity) getActivity()).handleException(TAG, error, mEmptyHandlers);

            mAddReviewerLoader.clear();
        }
    };

    private final RxLoaderObserver<AssigneeInfo> mEditAssigneeObserver
            = new RxLoaderObserver<AssigneeInfo>() {
        @Override
        public void onNext(AssigneeInfo result) {
            if (mResponse == null) {
                return;
            }

            mResponse.mChange.assignee = result._new;
            if (result._new != null) {
                // Update internal objects
                if (mResponse.mChange.reviewers != null) {
                    // Update reviewers
                    AccountInfo[] reviewers = mResponse.mChange.reviewers.get(ReviewerStatus.REVIEWER);
                    mResponse.mChange.reviewers.put(ReviewerStatus.REVIEWER,
                            ModelHelper.addReviewers(new AccountInfo[]{result._new}, reviewers));
                }
                ModelHelper.addRemovableReviewer(mResponse.mChange, result._new);
            }

            updateChangeInfo(mResponse);

            performMessagesRefresh();

            mEditAssigneeLoader.clear();
        }

        @Override
        public void onError(Throwable error) {
            //noinspection ConstantConditions
            ((BaseActivity) getActivity()).handleException(TAG, error, mEmptyHandlers);

            mEditAssigneeLoader.clear();
        }
    };

    private final RxLoaderObserver<Object> mActionObserver = new RxLoaderObserver<Object>() {
        @Override
        public void onNext(Object value) {
            if (Empty.NULL.equals(value)) {
                // The change was deleted. Redirect to parent
                ActivityHelper.performFinishActivity(getActivity(), true);
                return;
            }

            if (value instanceof ChangeInfo) {
                // Move to the new change
                ActivityHelper.openChangeDetails(getContext(), (ChangeInfo) value, false, false);
                return;
            }

            // Refresh the change
            forceRefresh();

            mActionLoader.clear();
        }

        @Override
        public void onError(Throwable error) {
            //noinspection ConstantConditions
            ((BaseActivity) getActivity()).handleException(TAG, error, mEmptyHandlers);

            mActionLoader.clear();
        }
    };

    private final RxLoaderObserver<ChangeInfo> mMoveBranchObserver
            = new RxLoaderObserver<ChangeInfo>() {
        @Override
        public void onNext(ChangeInfo value) {
            // Refresh the change
            forceRefresh();

            mMoveBranchLoader.clear();
        }

        @Override
        public void onError(Throwable error) {
            //noinspection ConstantConditions
            ((BaseActivity) getActivity()).handleException(TAG, error, mEmptyHandlers);

            mMoveBranchLoader.clear();
        }
    };

    private final RxLoaderObserver<Attachment> mAttachmentDownloadObserver
            = new RxLoaderObserver<Attachment>() {
        @Override
        public void onNext(Attachment attachment) {
            mAttachmentDownloadLoader.clear();
            if (mDialog != null) {
                mDialog.dismiss();
            }
            mDialog = null;

            performInternalOpenAttachment(attachment);
        }

        @Override
        public void onError(Throwable error) {
            //noinspection ConstantConditions
            ((BaseActivity) getActivity()).handleException(TAG, error);

            mAttachmentDownloadLoader.clear();
            if (mDialog != null) {
                mDialog.dismiss();
            }
            mDialog = null;
        }

        @Override
        public void onStarted() {
            if (mDialog != null) {
                mDialog.dismiss();
            }
            mDialog = new ProgressDialog(getActivity());
            mDialog.setMessage(getString(R.string.fetching_file));
            mDialog.setCancelable(false);
            mDialog.show();
        }
    };

    private BroadcastReceiver mAttachmentProviderChanged = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            onAttachmentProviderSelection(
                    AttachmentsProviderFactory.getAttachmentProvider(context).getType());
        }
    };

    private OnAttachmentsDroppedListener mOnAttachmentDroppedListener = attachments -> {
        for (Attachment attachment : attachments) {
            addPendingAttachments(attachment);
        }
    };

    @Keep
    public static class EmptyEventHandlers extends EmptyState.EventHandlers {
        private ChangeDetailsFragment mFragment;

        EmptyEventHandlers(ChangeDetailsFragment fragment) {
            mFragment = fragment;
        }

        public void onRetry(View v) {
            mFragment.forceRefresh();
        }
    }

    private Handler mUiHandler;

    private final OnAccountChipClickedListener mOnAccountChipClickedListener
            = this::performAccountClicked;
    private final OnAccountChipRemovedListener mOnReviewerRemovedListener
            = this::performRemoveReviewer;
    private final OnAccountChipRemovedListener mOnReviewerRemovedVoteListener
            = this::performRemoveReviewerVote;
    private final OnAccountChipRemovedListener mOnAssigneeRemovedListener =
            (account, tag) -> onAssigneeSelected(null);
    private final OnContinuousIntegrationPressed mOnContinuousIntegrationPressed
            = this::performContinuousIntegrationPressed;

    private ChangeDetailsFragmentBinding mBinding;
    private ProgressDialog mDialog;

    private FileAdapter mFileAdapter;
    private MessageAdapter mMessageAdapter;

    private EventHandlers mEventHandlers;
    private AttachmentsSupport mAttachmentsSupport;
    private final Model mModel = new Model();
    private final EmptyState mEmptyState = new EmptyState();
    private EmptyEventHandlers mEmptyHandlers;
    private String mCurrentRevision;
    private String mDiffAgainstRevision;
    private DataResponse mResponse;
    private final List<RevisionInfo> mAllRevisions = new ArrayList<>();
    private final List<RevisionInfo> mAllRevisionsWithBase = new ArrayList<>();
    private final ArrayList<Attachment> mAttachments = new ArrayList<>();

    private boolean mHideTaggedMessages;
    private boolean mHideCIMessages;

    private RxLoader1<String, DataResponse> mChangeLoader;
    private RxLoader1<Boolean, Boolean> mStarredLoader;
    private RxLoader1<ChangeEditMessageInput, Boolean> mChangeEditMessageLoader;
    private RxLoader1<DescriptionInput, Boolean> mChangeEditRevisionDescriptionLoader;
    private RxLoader1<ReviewInput, Pair<ReviewInput, ReviewInfo>> mReviewLoader;
    private RxLoader2<String, AddReviewerState, AddReviewerResultInfo> mAddReviewerLoader;
    private RxLoader1<String, AssigneeInfo> mEditAssigneeLoader;
    private RxLoader1<AccountInfo, AccountInfo> mRemoveReviewerLoader;
    private RxLoader1<Pair<String, AccountInfo>, Pair<String, AccountInfo>> mRemoveReviewerVoteLoader;
    private RxLoader1<String, String> mChangeTopicLoader;
    private RxLoader2<String[], String[], String[]> mChangeTagsLoader;
    private RxLoader<ChangeInfo> mMessagesRefreshLoader;
    private RxLoader<Map<String, Integer>> mDraftsRefreshLoader;
    private RxLoader2<String, String[], Object> mActionLoader;
    private RxLoader1<String, ChangeInfo> mMoveBranchLoader;
    private RxLoader1<Attachment, Attachment> mAttachmentDownloadLoader;
    private int mLegacyChangeId;

    private Map<String, Integer> savedReview;

    private Account mAccount;

    private boolean mIsInlineCommentsInMessages;

    public static ChangeDetailsFragment newInstance(int changeId) {
        return newInstance(changeId, null, null);
    }

    public static ChangeDetailsFragment newInstance(int changeId, String revision, String base) {
        ChangeDetailsFragment fragment = new ChangeDetailsFragment();
        Bundle arguments = new Bundle();
        arguments.putInt(Constants.EXTRA_LEGACY_CHANGE_ID, changeId);
        arguments.putString(Constants.EXTRA_REVISION, revision);
        arguments.putString(Constants.EXTRA_BASE, base);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUiHandler = new Handler();
        //noinspection ConstantConditions
        mLegacyChangeId = getArguments().getInt(
                Constants.EXTRA_LEGACY_CHANGE_ID, Constants.INVALID_CHANGE_ID);
        mCurrentRevision = getArguments().getString(Constants.EXTRA_REVISION);
        mDiffAgainstRevision = getArguments().getString(Constants.EXTRA_BASE);
        mEmptyHandlers = new EmptyEventHandlers(this);

        if (savedInstanceState != null) {
            mCurrentRevision = savedInstanceState.getString(
                    "current_revision", mCurrentRevision);
            mDiffAgainstRevision = savedInstanceState.getString(
                    "diff_against_revision", mDiffAgainstRevision);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(
                inflater, R.layout.change_details_fragment, container, false);
        mBinding.setModel(mModel);
        mBinding.setEmpty(mEmptyState);
        mBinding.setEmptyHandlers(mEmptyHandlers);
        mBinding.attachmentsDrop.listenTo(mOnAttachmentDroppedListener);
        startLoadersWithValidContext(savedInstanceState);

        // Force HW acceleration in case the activity disabled it in minidrawer mode
        mBinding.getRoot().setLayerType(View.LAYER_TYPE_HARDWARE, null);
        return mBinding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        startLoadersWithValidContext(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Check attachment status
        if (mBinding != null && mAttachmentsSupport != null) {
            mBinding.reviewInfo.setAttachmentsSupport(mAttachmentsSupport);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        Map<String, Integer> review = mBinding.reviewInfo.reviewLabels.getReview(false);
        outState.putString("review", SerializationManager.getInstance().toJson(review));
        outState.putString("current_revision", mCurrentRevision);
        outState.putString("diff_against_revision", mDiffAgainstRevision);
        outState.putBoolean("hideTaggedMessages", mHideTaggedMessages);
        outState.putBoolean("hideCIMessages", mHideCIMessages);
        outState.putParcelableArrayList("attachments", mAttachments);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == DIFF_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                // Current revision
                String revisionId = data.getStringExtra(Constants.EXTRA_REVISION_ID);
                if (revisionId != null && !revisionId.equals(mCurrentRevision)) {
                    // Change to the current revision
                    mCurrentRevision = revisionId;
                    //noinspection ConstantConditions
                    ((BaseActivity) getActivity()).setAnalyticsBase(mCurrentRevision);

                    // Restore diff against to base
                    mDiffAgainstRevision = null;
                    forceRefresh();
                    return;
                }

                // Diff against revision
                String diffAgainst = resolveDiffAgainstRevision(
                        data.getStringExtra(Constants.EXTRA_BASE));
                boolean changed = (diffAgainst == null && mDiffAgainstRevision != null) ||
                        (diffAgainst != null && mDiffAgainstRevision == null) ||
                        (diffAgainst != null && !diffAgainst.equals(mDiffAgainstRevision));
                if (changed) {
                    // Change to the diff against revision revision
                    if (diffAgainst != null && diffAgainst.equals(mCurrentRevision)) {
                        mDiffAgainstRevision = null;
                    } else {
                        mDiffAgainstRevision = diffAgainst;
                    }
                    forceRefresh();
                    return;
                }

                // Drafts changed
                boolean dataChanged = data.getBooleanExtra(Constants.EXTRA_DATA_CHANGED, false);
                if (dataChanged) {
                    // Refresh drafts comments
                    performDraftsRefresh();
                }
            }
        } else if (requestCode == EDIT_REQUEST_CODE) {
            // Remove the cache, in case the user request to enter again in edit mode
            CacheHelper.removeAccountDiffCacheDir(getContext());

            // If the user publish the edit, then reload the whole change
            if (resultCode == Activity.RESULT_OK) {
                mCurrentRevision = mDiffAgainstRevision = null;
                //noinspection ConstantConditions
                ((BaseActivity) getActivity()).setAnalyticsBase(mCurrentRevision);
                forceRefresh();
            }
        } else if (requestCode == REQUEST_ATTACHMENT_CAMERA && resultCode == Activity.RESULT_OK) {
            //noinspection ConstantConditions
            File image = FileHelper.getMostRecentFile(getContext().getFilesDir());
            if (image != null) {
                Attachment attachment = new Attachment();
                attachment.mLocalUri = Uri.fromFile(image);
                attachment.mName = StringHelper.getFileNameWithoutExtension(image);
                attachment.mSize = image.length();
                attachment.mMimeType = StringHelper.getMimeType(image);
                addPendingAttachments(attachment);
            }
        } else if (requestCode == REQUEST_ATTACHMENT_FILE && resultCode == Activity.RESULT_OK) {
            if (data.getData() != null) {
                Cursor c = null;
                try {
                    //noinspection ConstantConditions
                    ContentResolver cr = getContext().getContentResolver();
                    c = cr.query(data.getData(), null, null, null, null);
                    if (c != null) {
                        c.moveToFirst();
                        Attachment attachment = new Attachment();
                        attachment.mLocalUri = data.getData();
                        attachment.mName = StringHelper.getFileNameWithoutExtension(
                                new File(c.getString(
                                        c.getColumnIndex(OpenableColumns.DISPLAY_NAME))));
                        attachment.mSize = c.getLong(c.getColumnIndex(OpenableColumns.SIZE));
                        attachment.mMimeType = cr.getType(attachment.mLocalUri);
                        addPendingAttachments(attachment);
                    }
                } finally {
                    try {
                        if (c != null) {
                            c.close();
                        }
                    } catch (Exception ex) {
                        // ignore
                    }
                }
            }
        }
    }

    private void startLoadersWithValidContext(Bundle savedInstanceState) {
        if (getActivity() == null) {
            return;
        }

        if (mFileAdapter == null) {
            mAttachmentsSupport = new AttachmentsSupport(getContext());

            IntentFilter filter = new IntentFilter();
            filter.addAction(ATTACHMENT_PROVIDER_CHANGED_ACTION);
            //noinspection ConstantConditions
            LocalBroadcastManager.getInstance(getContext()).registerReceiver(
                    mAttachmentProviderChanged, filter);

            mAccount = Preferences.getAccount(getContext());
            if (mAccount != null) {
                mModel.isAuthenticated = mAccount.hasAuthenticatedAccessMode();
            }
            updateAuthenticatedAndOwnerStatus();

            mHideTaggedMessages = Preferences.isAccountToggleTaggedMessages(getContext(), mAccount);
            mHideCIMessages = Preferences.isAccountToggleCIAccountsMessages(getContext(), mAccount);
            if (savedInstanceState != null) {
                mHideTaggedMessages = savedInstanceState.getBoolean(
                        "hideTaggedMessages", mHideTaggedMessages);
                mHideCIMessages = savedInstanceState.getBoolean(
                        "hideCIMessages", mHideCIMessages);
                List<Attachment> attachments = savedInstanceState.getParcelableArrayList("attachments");
                mAttachments.clear();
                if (attachments != null) {
                    mAttachments.addAll(attachments);
                }
            }

            Repository repo = null;
            if (mHideCIMessages) {
                repo = ModelHelper.findRepositoryForAccount(getContext(), mAccount);
            }

            boolean isMessagesFolded = Preferences.isAccountMessagesFolded(getContext(), mAccount);
            mIsInlineCommentsInMessages = Preferences.isAccountInlineCommentInMessages(
                    getContext(), mAccount);

            mEventHandlers = new EventHandlers(this);

            mFileAdapter = new FileAdapter(mEventHandlers,
                    Preferences.isAccountShortFilenames(getContext(), mAccount));
            mBinding.fileInfo.list.setLayoutManager(new LinearLayoutManager(
                    getActivity(), LinearLayoutManager.VERTICAL, false) {
                @Override
                public boolean canScrollHorizontally() {
                    return false;
                }

                @Override
                public boolean canScrollVertically() {
                    return false;
                }
            });
            mBinding.fileInfo.list.setNestedScrollingEnabled(true);
            mBinding.fileInfo.list.setAdapter(mFileAdapter);


            mMessageAdapter = new MessageAdapter(this, mEventHandlers,
                    mModel.isAuthenticated, isMessagesFolded);
            mMessageAdapter.updateHideTaggedMessages(mHideTaggedMessages);
            mMessageAdapter.updateHideCIMessages(repo);
            int leftPadding = getResources().getDimensionPixelSize(
                    R.dimen.message_list_left_padding);
            DividerItemDecoration messageDivider = new DividerItemDecoration(
                    getContext(), LinearLayoutManager.VERTICAL);
            messageDivider.setMargins(leftPadding, 0);
            mBinding.messageInfo.list.setLayoutManager(new LinearLayoutManager(
                    getActivity(), LinearLayoutManager.VERTICAL, false) {
                @Override
                public boolean canScrollHorizontally() {
                    return false;
                }

                @Override
                public boolean canScrollVertically() {
                    return false;
                }
            });
            mBinding.messageInfo.list.setNestedScrollingEnabled(true);
            mBinding.messageInfo.list.addItemDecoration(messageDivider);
            mBinding.messageInfo.list.setAdapter(mMessageAdapter);

            mBinding.fastScroller.listenTo(() -> {
                mBinding.nestedScroll.fullScroll(View.FOCUS_DOWN);
                mBinding.fastScroller.hide();
            });

            mBinding.nestedScroll.setOnScrollChangeListener(
                (NestedScrollView.OnScrollChangeListener)
                        (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                    float h = mBinding.nestedScroll.getHeight();
                    float h14 = h / 4;
                    float mt = mBinding.messageInfo.getRoot().getTop();
                    float mh = mBinding.messageInfo.getRoot().getHeight();
                    float rt = mBinding.reviewInfo.getRoot().getTop() == 0
                            ? mt + mh : mBinding.reviewInfo.getRoot().getTop();

                    if ((mh / h) >= 2.5) {
                        if (scrollY < mt || (scrollY + h + h14) > rt) {
                            mBinding.fastScroller.hide();
                        } else if (scrollY > mt) {
                            mBinding.fastScroller.show(R.string.change_details_fast_scroll_msg);
                        }
                    }
                });

            // Restore user temporary review state
            if (savedInstanceState != null) {
                String review = savedInstanceState.getString("review", null);
                if (review != null) {
                    Type type = new TypeToken<Map<String, Integer>>(){}.getType();
                    savedReview = SerializationManager.getInstance().fromJson(review, type);
                }
            }

            // Configure the refresh
            setupSwipeToRefresh();

            // Fetch or join current loader
            RxLoaderManager loaderManager = RxLoaderManagerCompat.get(this);
            mChangeLoader = loaderManager.create("fetch", this::fetchChange, mChangeObserver);
            mStarredLoader = loaderManager.create("starred", this::starChange, mStarredObserver);
            mChangeEditMessageLoader = loaderManager.create(
                    "edit:message", this::editMessage, mChangeEditMessageObserver);
            mChangeEditRevisionDescriptionLoader = loaderManager.create(
                    "edit:description", this::editRevisionDescription,
                    mChangeEditRevisionDescriptionObserver);
            mReviewLoader = loaderManager.create("review", this::reviewChange, mReviewObserver);
            mChangeTopicLoader = loaderManager.create(
                    "change_topic", this::changeTopic, mChangeTopicObserver);
            mChangeTagsLoader = loaderManager.create(
                    "change_tags", this::changeTags, mChangeTagsObserver);
            mAddReviewerLoader = loaderManager.create(
                    "add_reviewer", this::addReviewer, mAddReviewerObserver);
            mEditAssigneeLoader = loaderManager.create(
                    "edit_assignee", this::editAssignee, mEditAssigneeObserver);
            mRemoveReviewerLoader = loaderManager.create(
                    "remove_reviewer", this::removeReviewer, mRemoveReviewerObserver);
            mRemoveReviewerVoteLoader = loaderManager.create(
                    "remove_reviewer_vote", this::removeReviewerVote, mRemoveReviewerVoteObserver);
            mMessagesRefreshLoader = loaderManager.create(
                    "messages_refresh", fetchMessages(), mMessagesRefreshObserver);
            mActionLoader = loaderManager.create(
                    "action", this::doAction, mActionObserver);
            mMoveBranchLoader = loaderManager.create(
                    "move_branch", this::doMoveBranch, mMoveBranchObserver);
            mAttachmentDownloadLoader = loaderManager.create(
                    "download_attachment", this::doAttachmentDownload, mAttachmentDownloadObserver);
            mDraftsRefreshLoader = loaderManager.create(
                    "drafts_refresh", fetchDrafts(), mDraftsRefreshObserver);
            mChangeLoader.start(String.valueOf(mLegacyChangeId));
        }
    }

    @Override
    public final void onDestroyView() {
        super.onDestroyView();
        mBinding.unbind();
        mUiHandler.removeCallbacksAndMessages(null);
        if (mDialog != null) {
            mDialog.dismiss();
        }
        mDialog = null;
        //noinspection ConstantConditions
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(
                mAttachmentProviderChanged);
    }

    private void updatePatchSetInfo(DataResponse response) {
        mBinding.patchSetInfo.setChangeId(response.mChange.changeId);
        mBinding.patchSetInfo.setRevision(mCurrentRevision);
        RevisionInfo revision = response.mChange.revisions.get(mCurrentRevision);
        mBinding.patchSetInfo.setChange(response.mChange);
        mBinding.patchSetInfo.setConfig(response.mProjectConfig);
        mBinding.patchSetInfo.setModel(revision);
        final int maxRevision = computeMaxRevisionNumber(response.mChange.revisions.values());
        //noinspection ConstantConditions
        final String patchSetText = getContext().getString(R.string.change_details_header_patchsets,
                revision.number, maxRevision);
        mBinding.patchSetInfo.setPatchset(patchSetText);
        mBinding.patchSetInfo.setHandlers(mEventHandlers);
        mBinding.patchSetInfo.parentCommits.with(mEventHandlers).from(revision.commit);
        mBinding.patchSetInfo.setIsCurrentRevision(
                mCurrentRevision.equals(response.mChange.currentRevision));
        mBinding.patchSetInfo.setHasData(true);
    }

    @SuppressWarnings("ConstantConditions")
    private void updateChangeInfo(DataResponse response) {
        final Context ctx = getActivity();
        if (ctx == null) {
            return;
        }
        boolean open = !ChangeStatus.MERGED.equals(response.mChange.status) &&
            !ChangeStatus.ABANDONED.equals(response.mChange.status);
        final GerritApi api = ModelHelper.getGerritApi(ctx);
        boolean supportVotes = api.supportsFeature(Features.VOTES);
        boolean supportAssignee = api.supportsFeature(Features.ASSIGNEE);
        boolean canAssignee = response.mActions.containsKey(ModelHelper.ACTION_ASSIGNEE);
        boolean supportsCC = api.supportsFeature(Features.CC) &&
                mAccount.getServerInfo() != null && mAccount.getServerInfo().noteDbEnabled;

        mBinding.changeInfo.owner
                .listenOn(mOnAccountChipClickedListener)
                .from(response.mChange.owner);
        mBinding.changeInfo.assignee
                .removable(mAccount.hasAuthenticatedAccessMode() && supportAssignee && canAssignee)
                .listenOn(mOnAccountChipClickedListener)
                .listenOn(mOnAssigneeRemovedListener)
                .from(response.mChange.assignee);
        mBinding.changeInfo.reviewers
                .listenOn(mOnAccountChipClickedListener)
                .listenOn(mOnReviewerRemovedListener)
                .withRemovableReviewers(mAccount.hasAuthenticatedAccessMode() && open)
                .withFilterCIAccounts(mHideCIMessages)
                .withReviewerStatus(supportsCC ? ReviewerStatus.REVIEWER : null)
                .from(response.mChange);
        if (supportsCC) {
            mBinding.changeInfo.cc
                    .listenOn(mOnAccountChipClickedListener)
                    .listenOn(mOnReviewerRemovedListener)
                    .withRemovableReviewers(mAccount.hasAuthenticatedAccessMode() && open)
                    .withFilterCIAccounts(mHideCIMessages)
                    .withReviewerStatus(ReviewerStatus.CC)
                    .from(response.mChange);
        }
        mBinding.changeInfo.labels
                .withRemovableReviewers(mAccount.hasAuthenticatedAccessMode() &&
                        open && supportVotes, response.mChange.removableReviewers)
                .listenOn(mOnAccountChipClickedListener)
                .listenOn(mOnReviewerRemovedVoteListener)
                .from(response.mChange);
        mBinding.changeInfo.setModel(response.mChange);
        mBinding.changeInfo.setSubmitType(response.mSubmitType);
        mBinding.changeInfo.setServerInfo(mAccount.getServerInfo());
        mBinding.changeInfo.setActions(response.mActions);
        mBinding.changeInfo.setHandlers(mEventHandlers);
        mBinding.changeInfo.setHasData(true);
        mBinding.changeInfo.setIsReviewer(ModelHelper.isReviewer(
                mAccount.mAccount, response.mChange));
        mBinding.changeInfo.setIsTwoPane(getResources().getBoolean(R.bool.config_is_two_pane));
        mBinding.changeInfo.setIsCurrentRevision(
                mCurrentRevision.equals(response.mChange.currentRevision));
        mBinding.changeInfo.setCii(response.mCI);
        mBinding.changeInfo.ci
                .listenOn(mOnContinuousIntegrationPressed)
                .from(response.mCI);
    }

    private void updateReviewInfo(DataResponse response) {
        mBinding.reviewInfo.setHasData(true);
        mBinding.reviewInfo.setModel(response.mChange);
        mBinding.reviewInfo.setHandlers(mEventHandlers);
        mBinding.reviewInfo.setIsCurrentRevision(
                mCurrentRevision.equals(response.mChange.currentRevision));
        mBinding.reviewInfo.reviewLabels.from(response.mChange, savedReview);
        mBinding.reviewInfo.setHasReviewAttachments(mAttachments.size() > 0);
        AttachmentsProvider attachmentProvider =
                AttachmentsProviderFactory.getAttachmentProvider(getContext());
        if (!mAttachments.isEmpty() && !attachmentProvider.isSupported()) {
            mAttachments.clear();
        }
        mBinding.reviewInfo.setAttachmentsSupport(mAttachmentsSupport);
        mBinding.reviewInfo.reviewAttachments
                .listenOn((OnAttachmentDroppedListener) this::performDropAttachment)
                .from(mAttachments);
        savedReview = null;
    }

    private int computeMaxRevisionNumber(Collection<RevisionInfo> revisions) {
        int max = 0;
        for (RevisionInfo revision : revisions) {
            max = Math.max(revision.number, max);
        }
        return max;
    }

    @SuppressWarnings("ConstantConditions")
    private Observable<DataResponse> fetchChange(String changeId) {
        final Context ctx = getActivity();
        final GerritApi api = ModelHelper.getGerritApi(ctx);
        return SafeObservable.fromNullCallable(() -> {
                DataResponse dataResponse = new DataResponse();
                dataResponse.mChange = api.getChange(
                        changeId, OPTIONS).blockingFirst();

                // Obtain the project configuration
                if (dataResponse.mChange != null) {
                    // Request project config
                    dataResponse.mProjectConfig = api.getProjectConfig(
                            dataResponse.mChange.project).blockingFirst();

                    final String revId = !TextUtils.isEmpty(mCurrentRevision) ? mCurrentRevision
                            : ModelHelper.extractBestRevisionId(dataResponse.mChange);

                    // Only request actions when we don't know which actions
                    // the change could have for the user. In other case, we
                    // have some logic to deal with basic actions.
                    // Request actions could be a heavy operation in old and complex
                    // changes, so just try to omit it.
                    ChangeStatus status = dataResponse.mChange.status;
                    if (mAccount.hasAuthenticatedAccessMode()
                            && !ChangeStatus.MERGED.equals(status)
                            && !ChangeStatus.ABANDONED.equals(status)) {
                        dataResponse.mActions = api.getChangeRevisionActions(
                                changeId, revId).blockingFirst();
                    } else {
                        // At least a cherry-pick action should be present if user
                        // is authenticated
                        dataResponse.mActions = new HashMap<>();
                        if (mAccount.hasAuthenticatedAccessMode()) {
                            dataResponse.mActions.put(
                                    ModelHelper.ACTION_CHERRY_PICK, new ActionInfo());
                        }
                    }
                }

                return dataResponse;
            })
            .flatMap(dataResponse -> {
                    final String revId = !TextUtils.isEmpty(mCurrentRevision) ? mCurrentRevision
                            : ModelHelper.extractBestRevisionId(dataResponse.mChange);
                    return Observable.zip(
                        SafeObservable.fromNullCallable(() -> dataResponse),
                        api.getChangeRevisionFiles(
                                changeId, revId, mDiffAgainstRevision, null),
                        api.getChangeRevisionSubmitType(changeId, revId),
                        api.getChangeRevisionComments(changeId, revId),
                        SafeObservable.fromNullCallable(() -> {
                            if (mDiffAgainstRevision != null) {
                                return api.getChangeRevisionComments(
                                        changeId, mDiffAgainstRevision).blockingFirst();
                            }
                            return new HashMap<>();
                        }),
                        SafeObservable.fromNullCallable(() -> {
                            // Do no fetch drafts if the account is not authenticated
                            if (mAccount.hasAuthenticatedAccessMode()) {
                                return api.getChangeRevisionDrafts(
                                        changeId, revId).blockingFirst();
                            }
                            return new HashMap<>();
                        }),
                        SafeObservable.fromNullCallable(() -> {
                            // Do no fetch drafts if the account is not authenticated
                            if (mDiffAgainstRevision != null &&
                                    mAccount.hasAuthenticatedAccessMode()) {
                                return api.getChangeRevisionDrafts(
                                        changeId, mDiffAgainstRevision).blockingFirst();
                            }
                            return new HashMap<>();
                        }),
                        SafeObservable.fromNullCallable(() -> {
                            // Fetch external CI servers to obtain job statuses
                            Repository repository =
                                    ModelHelper.findRepositoryForAccount(ctx, mAccount);
                            if (!Preferences.isAccountShowCIStatuses(ctx, mAccount)
                                    || repository == null
                                    || TextUtils.isEmpty(repository.mCiAccounts)) {
                                return new ArrayList<>();
                            }

                            if (!dataResponse.mChange.revisions.containsKey(revId)) {
                                return new ArrayList<>();
                            }
                            int revNumber = dataResponse.mChange.revisions.get(revId).number;
                            return ContinuousIntegrationHelper.getContinuousIntegrationStatus(
                                    repository, changeId, revNumber);
                        }),
                        this::combineResponse
                    );
                }
            )
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread());
    }

    @SuppressWarnings("ConstantConditions")
    private Observable<Boolean> starChange(final Boolean starred) {
        final Context ctx = getActivity();
        final GerritApi api = ModelHelper.getGerritApi(ctx);
        return SafeObservable.fromNullCallable(() -> {
                    Observable<Void> call;
                    if (starred) {
                        call = api.putDefaultStarOnChange(
                                GerritApi.SELF_ACCOUNT, String.valueOf(mLegacyChangeId));
                    } else {
                        call = api.deleteDefaultStarFromChange(
                                GerritApi.SELF_ACCOUNT, String.valueOf(mLegacyChangeId));
                    }
                    call.blockingFirst();
                    return starred;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    @SuppressWarnings("ConstantConditions")
    private Observable<Boolean> editMessage(final ChangeEditMessageInput input) {
        final Context ctx = getActivity();
        final GerritApi api = ModelHelper.getGerritApi(ctx);
        return SafeObservable.fromNullCallable(() -> {
                    api.setChangeEditMessage(String.valueOf(mLegacyChangeId), input).blockingFirst();
                    api.publishChangeEdit(String.valueOf(mLegacyChangeId)).blockingFirst();
                    return true;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    @SuppressWarnings("ConstantConditions")
    private Observable<Boolean> editRevisionDescription(final DescriptionInput input) {
        final Context ctx = getActivity();
        final GerritApi api = ModelHelper.getGerritApi(ctx);
        return SafeObservable.fromNullCallable(() -> {
                    api.setChangeRevisionDescription(
                            String.valueOf(mLegacyChangeId), mCurrentRevision, input)
                                .blockingFirst();
                    return true;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    @SuppressWarnings("ConstantConditions")
    private Observable<Pair<ReviewInput, ReviewInfo>> reviewChange(final ReviewInput input) {
        final Context ctx = getActivity();
        final GerritApi api = ModelHelper.getGerritApi(ctx);
        return SafeObservable.fromNullCallable(() -> {
                        // Create attachments metadata
                        if (!mAttachments.isEmpty()) {
                            performCreateAttachmentMetadata(input);
                        }

                        // Create the review
                        ReviewInfo response = api.setChangeRevisionReview(
                                String.valueOf(mLegacyChangeId), mCurrentRevision, input)
                                .blockingFirst();
                        return new Pair<>(input, response);
                    }
                )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }


    @SuppressWarnings("ConstantConditions")
    @SuppressLint("CheckResult")
    private Observable<String> changeTopic(final String newTopic) {
        final Context ctx = getActivity();
        final GerritApi api = ModelHelper.getGerritApi(ctx);
        return SafeObservable.fromNullCallable(() -> {
                    if (!TextUtils.isEmpty(newTopic)) {
                        TopicInput input = new TopicInput();
                        input.topic = newTopic;
                        api.setChangeTopic(String.valueOf(mLegacyChangeId), input).blockingFirst();
                    } else {
                        api.deleteChangeTopic(String.valueOf(mLegacyChangeId)).blockingFirst();
                    }
                    return newTopic;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    @SuppressWarnings("ConstantConditions")
    private Observable<String[]> changeTags(final String[] add, final String[] remove) {
        final Context ctx = getActivity();
        final GerritApi api = ModelHelper.getGerritApi(ctx);
        return SafeObservable.fromNullCallable(() -> {
                    HashtagsInput input = new HashtagsInput();
                    input.add = Arrays.asList(add);
                    input.remove = Arrays.asList(remove);
                    return api.setChangeHashtags(
                            String.valueOf(mLegacyChangeId), input).blockingFirst();
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    @SuppressWarnings("ConstantConditions")
    private Observable<AddReviewerResultInfo> addReviewer(
            final String reviewer, AddReviewerState state) {
        final Context ctx = getActivity();
        final GerritApi api = ModelHelper.getGerritApi(ctx);
        return SafeObservable.fromNullCallable(() -> {
                    ReviewerInput input = new ReviewerInput();
                    input.reviewerId = reviewer;
                    input.state = state;
                    return api.addChangeReviewer(String.valueOf(mLegacyChangeId), input)
                            .blockingFirst();
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    @SuppressWarnings("ConstantConditions")
    @SuppressLint("CheckResult")
    private Observable<AssigneeInfo> editAssignee(final String assignee) {
        final Context ctx = getActivity();
        final GerritApi api = ModelHelper.getGerritApi(ctx);
        return SafeObservable.fromNullCallable(() -> {
                    AssigneeInfo info = new AssigneeInfo();
                    info.old = mResponse.mChange.assignee;
                    if (TextUtils.isEmpty(assignee)) {
                        // Remove assignee
                        api.deleteChangeAssignee(String.valueOf(mLegacyChangeId))
                                .blockingFirst();
                    } else {
                        // Set assignee
                        AssigneeInput input = new AssigneeInput();
                        input.assignee = assignee;
                        info._new = api.setChangeAssignee(
                                String.valueOf(mLegacyChangeId), input).blockingFirst();
                    }
                    return info;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    @SuppressWarnings("ConstantConditions")
    private Observable<AccountInfo> removeReviewer(final AccountInfo account) {
        final Context ctx = getActivity();
        final GerritApi api = ModelHelper.getGerritApi(ctx);
        return SafeObservable.fromNullCallable(() -> {
                    api.deleteChangeReviewer(
                            String.valueOf(mLegacyChangeId),
                            ModelHelper.toAccountId(account)).blockingFirst();
                    return account;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    @SuppressWarnings("ConstantConditions")
    private Observable<Pair<String, AccountInfo>> removeReviewerVote(
            final Pair<String, AccountInfo> vote) {
        // TODO Evaluate to use deleteChangeRevisionReviewersVote 2.14+'s method
        // for a safer deletion
        final Context ctx = getActivity();
        final GerritApi api = ModelHelper.getGerritApi(ctx);
        return SafeObservable.fromNullCallable(() -> {
                    DeleteVoteInput input = new DeleteVoteInput();

                    api.deleteChangeReviewerVote(
                            String.valueOf(mLegacyChangeId),
                            String.valueOf(vote.second.accountId),
                            String.valueOf(vote.first),
                            input).blockingFirst();
                    return vote;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    @SuppressWarnings("ConstantConditions")
    private Observable<ChangeInfo> fetchMessages() {
        final Context ctx = getActivity();
        final GerritApi api = ModelHelper.getGerritApi(ctx);
        return SafeObservable.fromNullCallable(() ->
                    api.getChange(
                        String.valueOf(mLegacyChangeId), MESSAGES_OPTIONS).blockingFirst())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    @SuppressWarnings("ConstantConditions")
    private Observable<Map<String, Integer>> fetchDrafts() {
        final Context ctx = getActivity();
        final GerritApi api = ModelHelper.getGerritApi(ctx);
        return SafeObservable.fromNullCallable(() -> {
                    // Do no fetch drafts if the account is not authenticated
                    if (mAccount.hasAuthenticatedAccessMode()) {
                        Map<String, List<CommentInfo>> drafts =
                                api.getChangeRevisionDrafts(String.valueOf(mLegacyChangeId),
                                        mCurrentRevision).blockingFirst();
                        Map<String, Integer> draftComments = new HashMap<>();
                        if (drafts != null) {
                            for (String file : drafts.keySet()) {
                                draftComments.put(file, drafts.get(file).size());
                            }
                        }
                        return draftComments;
                    }
                    return new HashMap<String, Integer>();
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    @SuppressWarnings("ConstantConditions")
    private Observable<Object> doAction(final String action, final String[] params) {
        final Context ctx = getActivity();
        final GerritApi api = ModelHelper.getGerritApi(ctx);
        return SafeObservable.fromNullCallable(() -> {
                    switch (action) {
                        case ModelHelper.ACTION_CHERRY_PICK:
                            return performCherryPickChange(api, params[0], params[1]);
                        case ModelHelper.ACTION_REBASE:
                            return performRebaseChange(api, params[0]);
                        case ModelHelper.ACTION_ABANDON:
                            performAbandonChange(api, params[0]);
                            break;
                        case ModelHelper.ACTION_RESTORE:
                            performRestoreChange(api, params[0]);
                            break;
                        case ModelHelper.ACTION_REVERT:
                            return performRevertChange(api, params[0]);
                        case ModelHelper.ACTION_PUBLISH_DRAFT:
                            return performPublishDraft(api);
                        case ModelHelper.ACTION_DELETE_CHANGE:
                            performDeleteChange(api);
                            break;
                        case ModelHelper.ACTION_FOLLOW_UP:
                            return performFollowUp(api, params[0]);
                        case ModelHelper.ACTION_MARK_PRIVATE:
                            return performMarkPrivate(api, params[0]);
                        case ModelHelper.ACTION_MARK_WIP:
                            return performMarkWIP(api, params[0]);
                        case ModelHelper.ACTION_MARK_REVIEWED:
                            return performMarkReviewed(api);
                        case ModelHelper.ACTION_IGNORE:
                            return performMarkIgnore(api);
                        case ModelHelper.ACTION_SUBMIT:
                            performSubmitChange(api);
                            break;
                    }
                    return Empty.NULL;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    @SuppressWarnings("ConstantConditions")
    private Observable<ChangeInfo> doMoveBranch(final String newBranch) {
        final Context ctx = getActivity();
        final GerritApi api = ModelHelper.getGerritApi(ctx);
        return SafeObservable.fromNullCallable(() -> {
                    MoveInput input = new MoveInput();
                    input.destinationBranch = newBranch;
                    return api.moveChange(String.valueOf(mLegacyChangeId), input).blockingFirst();
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    private Observable<Attachment> doAttachmentDownload(final Attachment attachment) {
        return SafeObservable.fromNullCallable(() -> {
                    CacheHelper.downloadAttachmentFile(getContext(), attachment);
                    return attachment;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    private void setupSwipeToRefresh() {
        //noinspection ConstantConditions
        mBinding.refresh.setColorSchemeColors(
                ContextCompat.getColor(getContext(), R.color.accent));
        mBinding.refresh.setOnRefreshListener(this::forceRefresh);
    }

    private void forceRefresh() {
        startLoadersWithValidContext(null);

        // Check that activity was attached before refresh
        if (mChangeLoader != null) {
            mChangeLoader.clear();
            mChangeLoader.restart(String.valueOf(mLegacyChangeId));
        }
    }

    private void showProgress(boolean show, ChangeInfo change) {
        BaseActivity activity = (BaseActivity) getActivity();
        if (show) {
            //noinspection ConstantConditions
            activity.onRefreshStart(this);
        } else {
            //noinspection ConstantConditions
            activity.onRefreshEnd(this, change);
        }
        mBinding.refresh.setRefreshing(false);
    }

    private DataResponse combineResponse(
            DataResponse response, Map<String, FileInfo> files, SubmitType submitType,
            Map<String, List<CommentInfo>> revisionComments,
            Map<String, List<CommentInfo>> baseRevisionComments,
            Map<String, List<CommentInfo>> revisionDraftComments,
            Map<String, List<CommentInfo>> baseRevisionDraftComments,
            List<ContinuousIntegrationInfo> ci) {
        // Map inline and draft comments
        Map<String, Integer> inlineComments = new HashMap<>();
        if (revisionComments != null) {
            for (String file : revisionComments.keySet()) {
                inlineComments.put(file, computeNumberOfComments(revisionComments.get(file)));
            }
        }
        Map<String, Integer> draftComments = new HashMap<>();
        if (revisionDraftComments != null) {
            for (String file : revisionDraftComments.keySet()) {
                draftComments.put(file, computeNumberOfComments(revisionDraftComments.get(file)));
            }
        }

        // Map inline and draft comments from diff against revision
        if (baseRevisionComments != null) {
            for (String file : baseRevisionComments.keySet()) {
                int count = computeNumberOfComments(baseRevisionComments.get(file));
                if (inlineComments.containsKey(file)) {
                    count += inlineComments.get(file);
                }
                inlineComments.put(file, count);
            }
        }
        if (baseRevisionDraftComments != null) {
            for (String file : baseRevisionDraftComments.keySet()) {
                int count = computeNumberOfComments(baseRevisionDraftComments.get(file));
                if (draftComments.containsKey(file)) {
                    count += draftComments.get(file);
                }
                draftComments.put(file, count);
            }
        }

        // Fetch revision comments
        fetchNeededRevisionComments(response);

        // Join the actions
        response.mFiles = files;
        if (response.mActions == null) {
            response.mActions = response.mChange.actions;
        } else {
            response.mActions.putAll(response.mChange.actions);
        }

        // Continuous Integration
        boolean showCI = Preferences.isAccountShowCIStatuses(getActivity(), mAccount);
        Repository repository = ModelHelper.findRepositoryForAccount(getActivity(), mAccount);
        boolean supportsCI = repository != null && !TextUtils.isEmpty(repository.mCiAccounts);
        if (!showCI || !supportsCI) {
            response.mCI = null;
        } else {
            response.mCI = ci;
            if (getActivity() != null && ci.isEmpty()) {
                final String revisionId = !TextUtils.isEmpty(mCurrentRevision) ? mCurrentRevision
                        : ModelHelper.extractBestRevisionId(response.mChange);
                if (response.mChange.revisions.containsKey(revisionId)) {
                    response.mCI = ContinuousIntegrationHelper.extractContinuousIntegrationInfo(
                            response.mChange.revisions.get(revisionId).number,
                            response.mChange.messages, repository);
                }
            }
        }

        response.mSubmitType = submitType;
        response.mInlineComments = inlineComments;
        response.mDraftComments = draftComments;
        return response;
    }

    private void performOpenFileDiff(String file) {
        // Resolve base diff
        String base = null;
        if (mDiffAgainstRevision != null) {
            base = String.valueOf(mResponse.mChange.revisions.get(mDiffAgainstRevision).number);
        }
        String current = String.valueOf(mResponse.mChange.revisions.get(mCurrentRevision).number);

        ArrayList<String> files = new ArrayList<>(mResponse.mFiles.keySet());
        ActivityHelper.openDiffViewerActivity(this, mResponse.mChange, files, mResponse.mFiles,
                mCurrentRevision, base, current, file, null, DIFF_REQUEST_CODE);
    }

    private void performNavigateToComment(CommentInfo comment) {
        // Resolve base diff
        String base = null;
        if (mDiffAgainstRevision != null) {
            base = String.valueOf(mResponse.mChange.revisions.get(mDiffAgainstRevision).number);
        }
        String current = String.valueOf(comment.patchSet);
        String revision = mCurrentRevision;
        for (String rev : mResponse.mChange.revisions.keySet()) {
            if (mResponse.mChange.revisions.get(rev).number == comment.patchSet) {
                revision = rev;
                break;
            }
        }

        ActivityHelper.openDiffViewerActivity(this, mResponse.mChange, null, null,
                revision, base, current, comment.path, comment.id, DIFF_REQUEST_CODE);

    }

    private void sortRevisions(ChangeInfo change) {
        mAllRevisions.clear();
        for (String revision : change.revisions.keySet()) {
            RevisionInfo rev = change.revisions.get(revision);
            rev.commit.commit = revision;
            mAllRevisions.add(rev);
        }
        Collections.sort(mAllRevisions, (o1, o2) -> Integer.compare(o2.number, o1.number));

        // All revisions + base - current revision
        mAllRevisionsWithBase.clear();
        mAllRevisionsWithBase.addAll(mAllRevisions);
        int count = mAllRevisionsWithBase.size();
        for (int i = 0; i < count; i++) {
            RevisionInfo revision = mAllRevisionsWithBase.get(i);
            if (revision.commit.commit.equals(mCurrentRevision)) {
                mAllRevisionsWithBase.remove(i);
                break;
            }
        }
        mAllRevisionsWithBase.add(new RevisionInfo());
    }

    private void showPatchSetChooser(View anchor) {
        if (isLocked()) {
            return;
        }

        //noinspection ConstantConditions
        final ListPopupWindow popupWindow = new ListPopupWindow(getContext());
        PatchSetsAdapter adapter = new PatchSetsAdapter(getContext(),
                mAllRevisions, mResponse.mUnresolvedComments, mCurrentRevision);
        popupWindow.setAnchorView(anchor);
        popupWindow.setAdapter(adapter);
        popupWindow.setContentWidth(adapter.measureContentWidth());
        popupWindow.setOnItemClickListener((parent, view, position, id) -> {
            popupWindow.dismiss();
            mCurrentRevision = mAllRevisions.get(position).commit.commit;
            //noinspection ConstantConditions
            ((BaseActivity) getActivity()).setAnalyticsBase(mCurrentRevision);
            // Restore diff against to base
            mDiffAgainstRevision = null;
            forceRefresh();
        });
        popupWindow.setModal(true);
        popupWindow.show();
    }

    private void showDiffAgainstChooser(View anchor) {
        if (isLocked()) {
            return;
        }

        //noinspection ConstantConditions
        final ListPopupWindow popupWindow = new ListPopupWindow(getContext());
        PatchSetsAdapter adapter = new PatchSetsAdapter(getContext(),
                mAllRevisionsWithBase, mResponse.mUnresolvedComments, mDiffAgainstRevision);
        popupWindow.setAnchorView(anchor);
        popupWindow.setAdapter(adapter);
        popupWindow.setContentWidth(adapter.measureContentWidth());
        popupWindow.setOnItemClickListener((parent, view, position, id) -> {
            popupWindow.dismiss();
            String commit = null;
            if (mAllRevisionsWithBase.get(position).commit != null) {
                commit = mAllRevisionsWithBase.get(position).commit.commit;
            }
            mDiffAgainstRevision = commit;
            forceRefresh();
        });
        popupWindow.setModal(true);
        popupWindow.show();
    }

    private void showEditChangeActivity() {
        if (!isLocked()) {
            ActivityHelper.editChange(this,
                    mResponse.mChange.legacyChangeId, mResponse.mChange.changeId, mCurrentRevision,
                    EDIT_REQUEST_CODE);
        }
    }

    private void toggleTaggedMessages() {
        if (!isLocked()) {
            mModel.isLocked = true;
            if (mModel.msgListModel.actions.containsKey("action1")) {
                mHideTaggedMessages = !mHideTaggedMessages;
                mModel.msgListModel.actions.put("action1", getString(mHideTaggedMessages
                        ? R.string.change_details_action_show_tagged_messages
                        : R.string.change_details_action_hide_tagged_messages));

                mMessageAdapter.updateHideTaggedMessages(mHideTaggedMessages);
                mMessageAdapter.update(mModel.msgListModel, mResponse.mChange,
                        mResponse.mMessagesWithComments, mResponse.mChange.reviewerUpdates);
                mBinding.setModel(mModel);
            }
            mModel.isLocked = false;
        }
    }

    private void toggleCIMessages() {
        if (!isLocked()) {
            mModel.isLocked = true;
            if (mModel.msgListModel.actions.containsKey("action2")) {
                mHideCIMessages = !mHideCIMessages;
                mModel.msgListModel.actions.put("action2", getString(mHideCIMessages
                        ? R.string.change_details_action_show_ci_messages
                        : R.string.change_details_action_hide_ci_messages));

                Repository repo = null;
                if (mHideCIMessages) {
                    repo = ModelHelper.findRepositoryForAccount(getContext(), mAccount);
                }
                mMessageAdapter.updateHideCIMessages(repo);
                mMessageAdapter.update(mModel.msgListModel, mResponse.mChange,
                        mResponse.mMessagesWithComments, mResponse.mChange.reviewerUpdates);
                updateChangeInfo(mResponse);
                mBinding.setModel(mModel);
            }
            mModel.isLocked = false;
        }
    }

    private void performStarred(boolean starred) {
        if (!isLocked()) {
            mStarredLoader.clear();
            mStarredLoader.restart(starred);
        }
    }

    private void performContinuousIntegrationPressed(ContinuousIntegrationInfo ci) {
        if (!TextUtils.isEmpty(ci.mUrl)) {
            ActivityHelper.openUriInCustomTabs(getActivity(), Uri.parse(ci.mUrl), true);
        }
    }

    @SuppressWarnings("unused")
    private void performAccountClicked(AccountInfo account, Object tag) {
        if (account.accountId == 0) {
            // Nothing relevant to display
            return;
        }

        ChangeQuery filter = new ChangeQuery().owner(ModelHelper.getSafeAccountOwner(account));
        String title = getString(R.string.account_details);
        String displayName = ModelHelper.getAccountDisplayName(account);
        String extra = SerializationManager.getInstance().toJson(account);
        ActivityHelper.openStatsActivity(getContext(), title, displayName,
                StatsFragment.ACCOUNT_STATS, ModelHelper.toAccountId(account), filter, extra);
    }

    @SuppressWarnings("unused")
    private void performRemoveReviewer(AccountInfo account, Object tag) {
        if (!isLocked()) {
            mRemoveReviewerLoader.clear();
            mRemoveReviewerLoader.restart(account);
        }
    }

    private void performRemoveReviewerVote(AccountInfo account, Object tag) {
        if (!isLocked()) {
            mRemoveReviewerVoteLoader.clear();
            mRemoveReviewerVoteLoader.restart(new Pair<>((String) tag, account));
        }
    }

    private void performMessagesRefresh() {
        if (!isLocked()) {
            mMessagesRefreshLoader.clear();
            mMessagesRefreshLoader.restart();
        }
    }

    private void performDraftsRefresh() {
        if (!isLocked()) {
            mDraftsRefreshLoader.clear();
            mDraftsRefreshLoader.restart();
        }
    }

    private void performReview() {
        if (!isLocked()) {
            AttachmentsProvider provider =
                    AttachmentsProviderFactory.getAttachmentProvider(getContext());
            if (!mAttachments.isEmpty() && !provider.isSupported()) {
                mAttachments.clear();
                mBinding.reviewInfo.setAttachmentsSupport(mAttachmentsSupport);
                //noinspection ConstantConditions
                ((BaseActivity) getActivity()).showWarning(R.string.attachment_provider_not_supported);
                return;
            }


            String message = StringHelper.obtainQuoteFromMessage(
                    mBinding.reviewInfo.reviewComment.getText().toString());
            Map<String, Integer> review = mBinding.reviewInfo.reviewLabels.getReview(false);

            ReviewInput input = new ReviewInput();
            input.drafts = DraftActionType.PUBLISH_ALL_REVISIONS;
            input.strictLabels = true;
            if (!review.isEmpty()) {
                input.labels = review;
            }
            if (!TextUtils.isEmpty(message)) {
                input.message = message;
            }
            input.omitDuplicateComments = true;
            input.notify = NotifyType.ALL;

            mReviewLoader.clear();
            mReviewLoader.restart(input);
        }
    }

    private void updateLocked() {
        mBinding.patchSetInfo.setIsLocked(isLocked());
        mBinding.changeInfo.setIsLocked(isLocked());
        mBinding.reviewInfo.setIsLocked(isLocked());
        mBinding.executePendingBindings();
    }

    private void updateAuthenticatedAndOwnerStatus() {
        mBinding.patchSetInfo.setIsAuthenticated(mModel.isAuthenticated);
        mBinding.changeInfo.setIsAuthenticated(mModel.isAuthenticated);
        mBinding.reviewInfo.setIsAuthenticated(mModel.isAuthenticated);

        final boolean isOwner = mModel.isAuthenticated && mResponse != null
                && mResponse.mChange.owner.accountId == mAccount.mAccount.accountId;
        mBinding.patchSetInfo.setIsOwner(isOwner);
        mBinding.changeInfo.setIsOwner(isOwner);
        mBinding.executePendingBindings();
    }

    private void performOpenRelatedChanges() {
        ActivityHelper.openRelatedChangesActivity(
                getContext(), mResponse.mChange, mCurrentRevision);
    }

    @SuppressWarnings("ConstantConditions")
    private void performViewPatchSet() {
        final Context ctx = getActivity();
        final GerritApi api = ModelHelper.getGerritApi(ctx);
        RevisionInfo revision = mResponse.mChange.revisions.get(mCurrentRevision);
        Uri uri = api.getRevisionUri(
                String.valueOf(mLegacyChangeId), String.valueOf(revision.number));

        ActivityHelper.openUriInCustomTabs(getActivity(), uri, true);
    }

    @SuppressWarnings("ConstantConditions")
    private void performEditMessage(View v) {
        String title = getString(R.string.change_edit_message_title);
        String action = getString(R.string.action_edit);
        String hint = getString(R.string.change_edit_message_hint);

        String message = mResponse.mChange.revisions.get(mCurrentRevision).commit.message;

        EditDialogFragment fragment = EditDialogFragment.newInstance(title, null, message,
                action, hint, false, true, true, null, v, REQUEST_CODE_EDIT_MESSAGE, null);
        fragment.show(getChildFragmentManager(), EditDialogFragment.TAG);
    }

    @SuppressWarnings("ConstantConditions")
    private void performEditRevisionDescription(View v) {
        String title = getString(R.string.change_edit_revision_description_title);
        String action = getString(R.string.action_edit);
        String hint = getString(R.string.change_edit_revision_description_hint);

        String message = mResponse.mChange.revisions.get(mCurrentRevision).description;

        EditDialogFragment fragment = EditDialogFragment.newInstance(title, null, message,
                action, hint, false, true, true, null, v,
                REQUEST_CODE_EDIT_REVISION_DESCRIPTION, null);
        fragment.show(getChildFragmentManager(), EditDialogFragment.TAG);
    }

    private void performOpenIncludedInDialog() {
        IncludedInDialogFragment fragment = IncludedInDialogFragment.newInstance(
                mResponse.mChange);
        fragment.show(getChildFragmentManager(), IncludedInDialogFragment.TAG);
    }

    private void performOpenDownloadDialog() {
        DownloadDialogFragment fragment = DownloadDialogFragment.newInstance(
                mResponse.mChange, mCurrentRevision);
        fragment.show(getChildFragmentManager(), DownloadDialogFragment.TAG);
    }

    @SuppressWarnings("ConstantConditions")
    private void performShare() {
        final Context ctx = getActivity();
        final GerritApi api = ModelHelper.getGerritApi(ctx);
        Uri uri = api.getChangeUri(String.valueOf(mLegacyChangeId));

        String action = getString(R.string.action_share);
        String title = getString(R.string.change_details_title, mLegacyChangeId);
        ActivityHelper.share(getContext(), action, title, uri.toString());
    }

    private void performOpenAttachment(Attachment attachment) {
        String mimetype = attachment.mMimeType;
        if ("application/octet-stream".equals(attachment.mMimeType)) {
            attachment.mMimeType = StringHelper.getMimeType(new File(attachment.mName));
        }

        if (mimetype.equals("application/internet-shortcut")
                || mimetype.equals("application/x-url")) {
            ActivityHelper.openUriInCustomTabs(getActivity(), Uri.parse(attachment.mUrl));
        } else {
            if (CacheHelper.hasAttachmentFile(getContext(), attachment)) {
                // was previously cached
                performInternalOpenAttachment(attachment);
            } else {
                // it must be previously downloaded
                mAttachmentDownloadLoader.clear();
                mAttachmentDownloadLoader.restart(attachment);
            }
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void performDropAttachment(Attachment attachment) {
        Iterator<Attachment> it = mAttachments.iterator();
        while (it.hasNext()) {
            Attachment a = it.next();
            if (a.mLocalUri.equals(attachment.mLocalUri)) {
                it.remove();
                if (CacheHelper.hasAttachmentFile(getContext(), a)) {
                    CacheHelper.getAttachmentFile(getContext(), a).delete();
                }

                updateReviewInfo(mResponse);
                break;
            }
        }
    }

    private void performOpenAttachmentChooser(View v, String action) {
        switch (action) {
            case "camera":
                Intent i = AndroidHelper.createCaptureImageIntent(getContext());
                if (i == null) {
                    //noinspection ConstantConditions
                    ((BaseActivity) getActivity()).showWarning(
                            R.string.change_attachments_camera_capture_failure);
                    return;
                }
                startActivityForResult(i, REQUEST_ATTACHMENT_CAMERA);
                break;

            case "media":
                GalleryChooserFragment gcf = GalleryChooserFragment.newInstance();
                gcf.show(getChildFragmentManager(), GalleryChooserFragment.TAG);
                break;

            case "link":
                String url = AndroidHelper.obtainUrlFromClipboard(getContext());
                EditDialogFragment edf = EditDialogFragment.newInstance(
                        getString(R.string.url_chooser_dialog_title), null, url,
                        getString(R.string.action_attach),
                        getString(R.string.url_chooser_dialog_hint),
                        false, false, false, StringHelper.WEB_REGEXP, v,
                        REQUEST_CODE_URL_CHOOSER, null);
                edf.show(getChildFragmentManager(), GalleryChooserFragment.TAG);

                break;

            case "file":
                if (AndroidHelper.isKitkatOrGreater()) {
                    i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                } else {
                    i = new Intent(Intent.ACTION_GET_CONTENT);
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                }
                i.setType("*/*");
                startActivityForResult(Intent.createChooser(i,
                        getString(R.string.file_chooser_dialog_title)), REQUEST_ATTACHMENT_FILE);
                break;

            case "code":
                SnippetFragment sf = SnippetFragment.newInstance(getContext());
                sf.show(getChildFragmentManager(), SnippetFragment.TAG);
                break;

            case "settings":
                ProviderChooserFragment pcf = ProviderChooserFragment.newInstance();
                pcf.show(getChildFragmentManager(), ProviderChooserFragment.TAG);
                break;
        }
    }

    private void performInternalOpenAttachment(Attachment attachment) {
        // If attachment is a text file and is lower than 1MB, then open on our internal
        // snippet editor
        if (attachment.mMimeType.startsWith("text/") && attachment.mSize < 1048576) {
            SnippetFragment sf = SnippetFragment.newInstance(getContext(),
                    Uri.fromFile(CacheHelper.getAttachmentFile(getContext(), attachment)),
                    attachment.mMimeType);
            sf.show(getChildFragmentManager(), SnippetFragment.TAG);
            return;
        }

        String action = getString(R.string.action_share);
        //noinspection ConstantConditions
        Uri uri = FileProvider.getUriForFile(getContext(), "com.ruesga.rview.content",
                CacheHelper.getAttachmentFile(getContext(), attachment));
        ActivityHelper.open(getContext(), action, uri, attachment.mMimeType);
    }

    private void performShowAddReviewerDialog(AddReviewerState reviewerState, View v) {
        AddReviewerDialogFragment fragment =
                AddReviewerDialogFragment.newInstance(mLegacyChangeId, reviewerState, v);
        fragment.show(getChildFragmentManager(), AddReviewerDialogFragment.TAG);
    }

    private void performShowEditAssigneeDialog(View v) {
        EditAssigneeDialogFragment fragment = EditAssigneeDialogFragment.newInstance(v);
        fragment.show(getChildFragmentManager(), EditAssigneeDialogFragment.TAG);
    }

    private void performShowMoveBranchDialog(View v) {
        BranchChooserDialogFragment fragment = BranchChooserDialogFragment.newInstance(
                R.string.move_branch_title, R.string.action_move, mResponse.mChange.project,
                mResponse.mChange.branch, null, v, REQUEST_CODE_MOVE_BRANCH);
        fragment.show(getChildFragmentManager(), BranchChooserDialogFragment.TAG);
    }

    private void performShowChangeTopicDialog(View v) {
        String title = getString(R.string.change_topic_title);
        String action = getString(R.string.action_change);
        String hint = getString(R.string.change_topic_hint);

        EditDialogFragment fragment = EditDialogFragment.newInstance(title, null,
                mResponse.mChange.topic, action, hint, true, true, false, null, v,
                REQUEST_CODE_CHANGE_TOPIC, null);
        fragment.show(getChildFragmentManager(), EditDialogFragment.TAG);
    }

    private void performShowChangeTagsDialog(View v) {
        final Tag[] tags = mBinding.changeInfo.tagsLabels.getTags();
        String title = getString(R.string.change_tags_title);
        String action = getString(R.string.action_save);
        TagEditDialogFragment fragment = TagEditDialogFragment.newInstance(
                title, tags, action, v, REQUEST_CODE_TAGS);
        fragment.show(getChildFragmentManager(), TagEditDialogFragment.TAG);
    }

    private void performShowRebaseDialog(View v) {
        BaseChooserDialogFragment fragment = BaseChooserDialogFragment.newInstance(
                mLegacyChangeId, mResponse.mChange.project, mResponse.mChange.branch, v,
                REQUEST_CODE_REBASE);
        fragment.show(getChildFragmentManager(), BaseChooserDialogFragment.TAG);
    }

    private void performShowCherryPickDialog(View v) {
        String message = mResponse.mChange.revisions.get(mCurrentRevision).commit.message;
        BranchChooserDialogFragment fragment = BranchChooserDialogFragment.newInstance(
                R.string.change_action_cherrypick, R.string.change_action_cherrypick,
                mResponse.mChange.project, mResponse.mChange.branch, message, v,
                REQUEST_CODE_CHERRY_PICK);
        fragment.show(getChildFragmentManager(), BranchChooserDialogFragment.TAG);
    }

    private void performShowRequestMessageDialog(View v, String title,
            String action, String hint, String text, boolean canBeEmpty, int requestCode) {
        EditDialogFragment fragment = EditDialogFragment.newInstance(
                title, null, text, action, hint, canBeEmpty, true, true, null, v, requestCode, null);
        fragment.show(getChildFragmentManager(), EditDialogFragment.TAG);
    }

    private void performConfirmDialog(View v, String message, int requestCode) {
        ConfirmDialogFragment fragment = ConfirmDialogFragment.newInstance(message, v, requestCode);
        fragment.show(getChildFragmentManager(), ConfirmDialogFragment.TAG);
    }

    private void performShowMoreAction(View anchor) {
        SimpleDropDownAdapter<Integer> adapter = createMoreActionsAdapter();
        if (adapter == null) {
            return;
        }
        //noinspection ConstantConditions
        final ListPopupWindow popupWindow = new ListPopupWindow(getContext());
        popupWindow.setAnchorView(anchor);
        popupWindow.setAdapter(adapter);
        popupWindow.setContentWidth(adapter.measureContentWidth());
        popupWindow.setOnItemClickListener((parent, view, position, id) -> {
            popupWindow.dismiss();
            performAction(adapter.getId(position), anchor);
        });
        popupWindow.setModal(true);
        popupWindow.show();
    }

    private SimpleDropDownAdapter<Integer> createMoreActionsAdapter() {
        if (mResponse == null || mResponse.mChange == null || mResponse.mActions == null) {
            return null;
        }

        final boolean isOwner = mResponse.mChange.owner.accountId == mAccount.mAccount.accountId;
        final boolean isNew = mResponse.mChange.status.equals(ChangeStatus.NEW);
        final boolean isDraft = mResponse.mChange.status.equals(ChangeStatus.DRAFT);

        final List<String> options = new ArrayList<>();
        final List<Integer> ids = new ArrayList<>();

        // Cherry-Pick
        if (mResponse.mActions.containsKey(ModelHelper.ACTION_CHERRY_PICK)) {
            options.add(getString(R.string.change_action_cherrypick));
            ids.add(R.id.cherrypick);
        }
        // FollowUp
        if (mResponse.mActions.containsKey(ModelHelper.ACTION_FOLLOW_UP)) {
            options.add(getString(R.string.change_action_follow_up));
            ids.add(R.id.follow_up);
        }
        // Move
        if (mResponse.mActions.containsKey(ModelHelper.ACTION_MOVE)) {
            options.add(getString(R.string.change_action_move));
            ids.add(R.id.move);
        }
        // Delete
        if (mResponse.mActions.containsKey(ModelHelper.ACTION_DELETE_CHANGE) && isOwner && isDraft) {
            options.add(getString(R.string.change_action_delete_change));
            ids.add(R.id.delete_change);
        }

        if (ModelHelper.isEqualsOrGreaterVersionThan(mAccount, 2.15d)) {
            // Private
            if (isOwner && isNew) {
                options.add(getString(mResponse.mChange.isPrivate
                        ? R.string.change_action_mark_public : R.string.change_action_mark_private));
                ids.add(R.id.mark_private);
            }
            // Work In Progress
            if (isOwner && isNew) {
                options.add(getString(mResponse.mChange.isWorkInProgress
                        ? R.string.change_action_mark_ready : R.string.change_action_mark_wip));
                ids.add(R.id.mark_wip);
            }
            // Mute
            if (ModelHelper.isEqualsOrGreaterVersionThan(mAccount, 2.15d) && !isOwner) {
                options.add(getString(mResponse.mChange.stars != null
                        && Arrays.asList(mResponse.mChange.stars).contains(ModelHelper.ACTION_IGNORE)
                        ? R.string.change_action_unmute : R.string.change_action_mute));
                ids.add(R.id.mute);
            }
            // Reviewed
            options.add(getString(mResponse.mChange.reviewed
                    ? R.string.change_action_mark_unreviewed : R.string.change_action_mark_reviewed));
            ids.add(R.id.mark_reviewed);
        }

        return new SimpleDropDownAdapter<>(getContext(), options, null,
                ids.toArray(new Integer[ids.size()]), null);
    }



    private void performReplyComment(int position) {
        String currentMessage = mBinding.reviewInfo.reviewComment.getText().toString();
        String replyMessage = mMessageAdapter.getMessage(position);
        String msg = StringHelper.quoteMessage(currentMessage, replyMessage);
        mBinding.reviewInfo.reviewComment.setText(msg);
        mBinding.reviewInfo.reviewComment.setSelection(msg.length());
        mBinding.nestedScroll.fullScroll(View.FOCUS_DOWN);
    }

    private void performApplyFilter(View v) {
        String title = null;
        ChangeQuery filter = null;
        switch (v.getId()) {
            case R.id.project:
                title = getString(R.string.change_details_project);
                String project = ((TextView) v).getText().toString();
                filter = new ChangeQuery().project(project);
                ActivityHelper.openStatsActivity(getContext(), title, project,
                        StatsFragment.PROJECT_STATS, project, filter, null);
                return;
            case R.id.branch:
                title = getString(R.string.change_details_branch);
                filter = new ChangeQuery().branch(((TextView) v).getText().toString());
                break;
            case R.id.topic:
                title = getString(R.string.change_details_topic);
                filter = new ChangeQuery().topic(((TextView) v).getText().toString());
                break;
        }
        ActivityHelper.openChangeListByFilterActivity(getActivity(), title, filter, false, false);
    }

    private void performAction(int id, View anchor) {
        if (!isLocked()) {
            String title;
            String action;
            String hint;
            switch (id) {
                case R.id.cherrypick:
                    performShowCherryPickDialog(anchor);
                    break;

                case R.id.rebase:
                    performShowRebaseDialog(anchor);
                    break;

                case R.id.abandon:
                    action = getString(R.string.change_action_abandon);
                    hint = getString(R.string.actions_comment_hint);
                    performShowRequestMessageDialog(
                            anchor, action, action, hint, null, true, REQUEST_CODE_ABANDON_CHANGE);
                    break;

                case R.id.restore:
                    action = getString(R.string.change_action_restore);
                    hint = getString(R.string.actions_comment_hint);
                    performShowRequestMessageDialog(
                            anchor, action, action, hint, null, true, REQUEST_CODE_RESTORE_CHANGE);
                    break;

                case R.id.revert: {
                    action = getString(R.string.change_action_revert);
                    hint = getString(R.string.actions_message_hint);
                    String message = getString(R.string.revert_msg_template,
                            mResponse.mChange.revisions.get(mCurrentRevision).commit.subject,
                            mCurrentRevision);
                    performShowRequestMessageDialog(
                            anchor, action, action, hint, message, true, REQUEST_CODE_REVERT_CHANGE);
                    break;
                }

                case R.id.publish_draft:
                    mActionLoader.clear();
                    mActionLoader.restart(ModelHelper.ACTION_PUBLISH_DRAFT, null);
                    break;

                case R.id.delete_change:
                    //noinspection ConstantConditions
                    AlertDialog dialog = new AlertDialog.Builder(getContext())
                            .setTitle(R.string.delete_draft_change_title)
                            .setMessage(R.string.delete_draft_change_confirm)
                            .setPositiveButton(R.string.action_delete, (dialog1, which) -> {
                                    mActionLoader.clear();
                                    mActionLoader.restart(ModelHelper.ACTION_DELETE_CHANGE, null);
                                })
                            .setNegativeButton(R.string.action_cancel, null)
                            .create();
                    dialog.show();
                    break;

                case R.id.follow_up:
                    action = getString(R.string.change_action_follow_up);
                    hint = getString(R.string.actions_message_hint);
                    performShowRequestMessageDialog(
                            anchor, action, action, hint, null, false, REQUEST_CODE_FOLLOW_UP_CHANGE);
                    break;

                case R.id.move:
                    performShowMoveBranchDialog(anchor);
                    break;

                case R.id.mark_private:
                    title = getString(mResponse.mChange.isPrivate
                            ? R.string.change_action_mark_public
                            : R.string.change_action_mark_private);
                    action = getString(R.string.action_set);
                    hint = getString(R.string.actions_comment_hint);
                    performShowRequestMessageDialog(
                            anchor, title, action, hint, null, true, REQUEST_CODE_MARK_PRIVATE);
                    break;

                case R.id.mark_wip:
                    title = getString(mResponse.mChange.isWorkInProgress
                            ? R.string.change_action_mark_ready
                            : R.string.change_action_mark_wip);
                    action = getString(R.string.action_set);
                    hint = getString(R.string.actions_comment_hint);
                    performShowRequestMessageDialog(
                            anchor, title, action, hint, null, true, REQUEST_CODE_MARK_WIP);
                    break;

                case R.id.mark_reviewed:
                    mActionLoader.clear();
                    mActionLoader.restart(ModelHelper.ACTION_MARK_REVIEWED, null);
                    break;

                case R.id.mute:
                    mActionLoader.clear();
                    mActionLoader.restart(ModelHelper.ACTION_IGNORE, null);
                    break;

                case R.id.submit:
                    String message = getString(R.string.actions_confirm_submit);
                    performConfirmDialog(anchor, message, REQUEST_CODE_SUBMIT_CHANGE);
                    break;

                case R.id.more:
                    performShowMoreAction(anchor);
                    break;
            }
        }
    }

    @SuppressLint("CheckResult")
    private void performSubmitChange(GerritApi api) {
        SubmitInput input = new SubmitInput();
        input.notify = NotifyType.ALL;
        api.submitChange(String.valueOf(mLegacyChangeId), input).blockingFirst();
    }

    private ChangeInfo performRebaseChange(GerritApi api, String base) {
        RebaseInput input = new RebaseInput();
        input.base = base;
        return api.rebaseChange(String.valueOf(mLegacyChangeId), input).blockingFirst();
    }

    @SuppressLint("CheckResult")
    private void performAbandonChange(GerritApi api, String msg) {
        AbandonInput input = new AbandonInput();
        input.notify = NotifyType.ALL;
        if (!TextUtils.isEmpty(msg)) {
            input.message = msg;
        }
        api.abandonChange(String.valueOf(mLegacyChangeId), input).blockingFirst();
    }

    @SuppressLint("CheckResult")
    private void performRestoreChange(GerritApi api, String msg) {
        RestoreInput input = new RestoreInput();
        if (!TextUtils.isEmpty(msg)) {
            input.message = msg;
        }
        api.restoreChange(String.valueOf(mLegacyChangeId), input).blockingFirst();
    }

    private ChangeInfo performRevertChange(GerritApi api, String msg) {
        RevertInput input = new RevertInput();
        input.notify = NotifyType.ALL;
        if (!TextUtils.isEmpty(msg)) {
            input.message = msg;
        }
        return api.revertChange(String.valueOf(mLegacyChangeId), input).blockingFirst();
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("CheckResult")
    private boolean performPublishDraft(GerritApi api) {
        api.publishChangeDraftRevision(String.valueOf(mLegacyChangeId), mCurrentRevision)
                .blockingFirst();
        return true;
    }

    @SuppressLint("CheckResult")
    private void performDeleteChange(GerritApi api) {
        api.deleteChange(String.valueOf(mLegacyChangeId)).blockingFirst();
    }

    private ChangeInfo performFollowUp(GerritApi api, String subject) {
        ChangeInput change = new ChangeInput();
        change.baseChange = mResponse.mChange.id;
        change.branch = mResponse.mChange.branch;
        change.project = mResponse.mChange.project;
        change.status = InitialChangeStatus.DRAFT;
        if (!TextUtils.isEmpty(mResponse.mChange.topic)) {
            change.topic = mResponse.mChange.topic;
        }
        change.subject = subject;
        return api.createChange(change).blockingFirst();
    }

    private ChangeInfo performCherryPickChange(GerritApi api, String branch, String msg) {
        String changeId = String.valueOf(mResponse.mChange.legacyChangeId);
        CherryPickInput input = new CherryPickInput();
        input.destination = branch;
        input.message = msg;
        return api.cherryPickChangeRevision(changeId, mCurrentRevision, input).blockingFirst();
    }

    @SuppressLint("CheckResult")
    private boolean performMarkPrivate(GerritApi api, String message) {
        String changeId = String.valueOf(mResponse.mChange.legacyChangeId);
        final PrivateInput input = new PrivateInput();
        if (!TextUtils.isEmpty(message)) {
            input.message = message;
        }

        if (mResponse.mChange.isPrivate) {
            api.unmarkChangeAsPrivate(changeId, input).blockingFirst();
        } else {
            api.markChangeAsPrivate(changeId, input).blockingFirst();
        }
        return true;
    }

    @SuppressLint("CheckResult")
    private boolean performMarkWIP(GerritApi api, String message) {
        String changeId = String.valueOf(mResponse.mChange.legacyChangeId);
        final WorkInProgressInput input = new WorkInProgressInput();
        if (!TextUtils.isEmpty(message)) {
            input.message = message;
        }

        if (mResponse.mChange.isWorkInProgress) {
            api.setChangeReadyForReview(changeId, input).blockingFirst();
        } else {
            api.setChangeWorkInProgress(changeId, input).blockingFirst();
        }
        return true;
    }

    @SuppressLint("CheckResult")
    private boolean performMarkReviewed(GerritApi api) {
        String changeId = String.valueOf(mResponse.mChange.legacyChangeId);
        if (mResponse.mChange.reviewed) {
            api.markChangeAsUnreviewed(changeId).blockingFirst();
        } else {
            api.markChangeAsReviewed(changeId).blockingFirst();
        }
        return true;
    }

    @SuppressLint("CheckResult")
    private boolean performMarkIgnore(GerritApi api) {
        String changeId = String.valueOf(mResponse.mChange.legacyChangeId);
        boolean ignored = mResponse.mChange.stars != null
                && Arrays.asList(mResponse.mChange.stars).contains(ModelHelper.ACTION_IGNORE);
        if (ignored) {
            api.unignoreChange(changeId).blockingFirst();
        } else {
            api.ignoreChange(changeId).blockingFirst();
        }
        return true;
    }

    private void performMessageClick(int position) {
        mMessageAdapter.changeFoldedStatus(position);
    }


    @SuppressWarnings("ConstantConditions")
    private void fetchNeededRevisionComments(DataResponse response) {
        if (!mIsInlineCommentsInMessages) {
            return;
        }

        // Determine which patchset needs request comments for
        List<Integer> revisionsWithComments = new ArrayList<>();
        if (response.mChange.messages != null) {
            for (ChangeMessageInfo message : response.mChange.messages) {
                if (message.message != null && COMMENTS_PATTERN.matcher(message.message).find()) {
                    if (!revisionsWithComments.contains(message.revisionNumber)) {
                        revisionsWithComments.add(message.revisionNumber);
                    }
                }
            }
        }

        // Fetch comments of needed revisions
        final Context ctx = getActivity();
        final GerritApi api = ModelHelper.getGerritApi(ctx);
        response.mMessagesWithComments.clear();
        response.mUnresolvedComments.clear();
        for (int rev : revisionsWithComments) {
            try {
                Map<String, List<CommentInfo>> comments = api.getChangeRevisionComments(
                        String.valueOf(response.mChange.legacyChangeId),
                        String.valueOf(rev)).blockingFirst();
                if (comments == null) {
                    continue;
                }

                updateMessageComments(response, comments);
                if (api.supportsFeature(Features.UNRESOLVED_COMMENTS)) {
                    updateUnresolvedComments(response, comments);
                }
            } catch (Exception ex) {
                Log.e(TAG, "Can't match comments for messages.", ex);
            }
        }
    }

    @SuppressWarnings("Convert2streamapi")
    private void updateMessageComments(
            DataResponse response, Map<String, List<CommentInfo>> comments) {
        final Map<String, LinkedHashMap<String, List<CommentInfo>>> mwc =
                response.mMessagesWithComments;

        // Match comments with messages
        for (ChangeMessageInfo message : response.mChange.messages) {
            if (message.message != null && COMMENTS_PATTERN.matcher(message.message).find()) {
                for (String file : comments.keySet()) {
                    List<CommentInfo> items = comments.get(file);
                    if (items != null) {
                        for (CommentInfo comment : items) {
                            comment.path = file;
                            if (comment.updated.compareTo(message.date) == 0 &&
                                    comment.author.accountId == message.author.accountId) {
                                if (!mwc.containsKey(message.id)) {
                                    mwc.put(message.id, new LinkedHashMap<>());
                                }

                                final LinkedHashMap<String, List<CommentInfo>> filesAndComments
                                        = mwc.get(message.id);
                                if (!filesAndComments.containsKey(file)) {
                                    filesAndComments.put(file, new ArrayList<>());
                                }

                                List<CommentInfo> list = filesAndComments.get(file);
                                comment.patchSet = message.revisionNumber;
                                list.add(comment);
                            }
                        }
                    }
                }
            }
        }
    }

    private void updateUnresolvedComments(
            DataResponse response, Map<String, List<CommentInfo>> comments) {
        final Map<Integer, UnresolvedComment> unresolvedComments = response.mUnresolvedComments;

        for (Map.Entry<String, List<CommentInfo>> entries : comments.entrySet()) {
            for (CommentInfo comment : entries.getValue()) {
                if (!unresolvedComments.containsKey(comment.patchSet)) {
                    unresolvedComments.put(comment.patchSet, new UnresolvedComment());
                }

                UnresolvedComment uc = unresolvedComments.get(comment.patchSet);
                uc.mTotal++;
                if (comment.unresolved) {
                    uc.mUnresolved++;
                }
            }
        }
    }

    private String resolveDiffAgainstSelectorText() {
        if (mDiffAgainstRevision == null) {
            return getString(R.string.change_details_diff_against,
                    getString(R.string.options_base));
        }

        for (RevisionInfo revision : mAllRevisions) {
            if (revision.commit.commit.equals(mDiffAgainstRevision)) {
                return getString(R.string.change_details_diff_against,
                        getString(R.string.change_details_diff_against_format,
                                revision.number, revision.commit.commit.substring(0, 10)));
            }
        }

        return null;
    }

    private String resolveDiffAgainstRevision(String base) {
        if (base == null || mResponse == null || mResponse.mChange == null
                || mResponse.mChange.revisions == null) {
            return null;
        }

        int number = Integer.valueOf(base);
        for (String rev : mResponse.mChange.revisions.keySet()) {
            if (mResponse.mChange.revisions.get(rev).number == number) {
                return rev;
            }
        }
        return null;
    }

    private int computeNumberOfComments(List<CommentInfo> comments) {
        int count = 0;
        for (CommentInfo comment : comments) {
            if (mDiffAgainstRevision == null || !SideType.PARENT.equals(comment.side)) {
                count++;
            }
        }
        return count;
    }

    private boolean isLocked() {
        return mResponse == null || mResponse.mChange == null || mModel.isLocked;
    }

    @Override
    public void onReviewerAdded(String reviewer, AddReviewerState reviewerState) {
        if (!isLocked()) {
            mAddReviewerLoader.clear();
            mAddReviewerLoader.restart(reviewer, reviewerState);
        }
    }

    @Override
    public void onAssigneeSelected(String assignee) {
        if (!isLocked()) {
            mEditAssigneeLoader.clear();
            mEditAssigneeLoader.restart(assignee == null ? "" : assignee);
        }
    }

    @Override
    public void onFilterSelected(int requestCode, Object[] o) {
        if (!isLocked()) {
            switch (requestCode) {
                case REQUEST_CODE_REBASE:
                    mActionLoader.clear();
                    mActionLoader.restart(ModelHelper.ACTION_REBASE, new String[]{(String) o[0]});
                    break;
                case REQUEST_CODE_CHERRY_PICK:
                    String[] result = (String[]) o[0];
                    mActionLoader.clear();
                    mActionLoader.restart(ModelHelper.ACTION_CHERRY_PICK,
                            new String[]{result[0], result[1]});
                    break;
                case REQUEST_CODE_MOVE_BRANCH:
                    mMoveBranchLoader.clear();
                    mMoveBranchLoader.restart((String) o[0]);
                    break;
            }
        }
    }

    @Override
    public void onEditChanged(int requestCode, Bundle requestData, String newValue) {
        if (!isLocked()) {
            switch (requestCode) {
                case REQUEST_CODE_EDIT_MESSAGE:
                    ChangeEditMessageInput changeEditMessageInput = new ChangeEditMessageInput();
                    changeEditMessageInput.message = newValue;
                    mChangeEditMessageLoader.clear();
                    mChangeEditMessageLoader.restart(changeEditMessageInput);
                    break;

                case REQUEST_CODE_CHANGE_TOPIC:
                    mChangeTopicLoader.clear();
                    mChangeTopicLoader.restart(newValue);
                    break;

                case REQUEST_CODE_ABANDON_CHANGE:
                    mActionLoader.clear();
                    mActionLoader.restart(
                            ModelHelper.ACTION_ABANDON, new String[]{newValue});
                    break;

                case REQUEST_CODE_RESTORE_CHANGE:
                    mActionLoader.clear();
                    mActionLoader.restart(
                            ModelHelper.ACTION_RESTORE, new String[]{newValue});
                    break;

                case REQUEST_CODE_REVERT_CHANGE:
                    mActionLoader.clear();
                    mActionLoader.restart(
                            ModelHelper.ACTION_REVERT, new String[]{newValue});
                    break;

                case REQUEST_CODE_FOLLOW_UP_CHANGE:
                    mActionLoader.clear();
                    mActionLoader.restart(
                            ModelHelper.ACTION_FOLLOW_UP, new String[]{newValue});
                    break;

                case REQUEST_CODE_MARK_PRIVATE:
                    mActionLoader.clear();
                    mActionLoader.restart(
                            ModelHelper.ACTION_MARK_PRIVATE, new String[]{newValue});
                    break;

                case REQUEST_CODE_MARK_WIP:
                    mActionLoader.clear();
                    mActionLoader.restart(
                            ModelHelper.ACTION_MARK_WIP, new String[]{newValue});
                    break;

                case REQUEST_CODE_EDIT_REVISION_DESCRIPTION:
                    DescriptionInput descriptionInput = new DescriptionInput();
                    descriptionInput.description = newValue;
                    mChangeEditRevisionDescriptionLoader.clear();
                    mChangeEditRevisionDescriptionLoader.restart(descriptionInput);
                    break;

                case REQUEST_CODE_URL_CHOOSER:
                    Attachment attachment = new Attachment();
                    // Sanitize the url
                    if (!(newValue.toLowerCase(Locale.US).startsWith("http://") ||
                            newValue.toLowerCase(Locale.US).startsWith("https://"))) {
                        newValue = "http://" + newValue;
                    }
                    attachment.mLocalUri = Uri.parse(newValue);
                    String name = attachment.mLocalUri.getLastPathSegment();
                    attachment.mName = name == null ? newValue : name;
                    attachment.mMimeType = "application/internet-shortcut";
                    addPendingAttachments(attachment);
                    break;
            }
        }
    }

    @Override
    public void onTagEditChanged(int requestCode, Tag[] tags) {
        // Generate tags to remove and to add
        List<String> oldTags = new ArrayList<>(Arrays.asList(mResponse.mChange.hashtags));
        List<String> newTags = new ArrayList<>();
        for (Tag tag : tags) {
            newTags.add(tag.toPlainTag().toString());
        }
        List<String> copy = new ArrayList<>(newTags);

        newTags.removeAll(oldTags);
        oldTags.removeAll(copy);

        // Save the tags
        mChangeTagsLoader.clear();
        mChangeTagsLoader.restart(
                newTags.toArray(new String[newTags.size()]),
                oldTags.toArray(new String[oldTags.size()]));
    }

    @Override
    public void onActionConfirmed(int requestCode) {
        if (!isLocked()) {
            switch (requestCode) {
                case REQUEST_CODE_SUBMIT_CHANGE:
                    mActionLoader.clear();
                    mActionLoader.restart(ModelHelper.ACTION_SUBMIT, null);
                    break;
            }
        }
    }

    private void performShowMoreFiles(View v) {
        FilesDialogFragment fragment = FilesDialogFragment.newInstance(
                mFileAdapter.getAllItems(), false, v);
        fragment.show(getChildFragmentManager(), FilesDialogFragment.TAG);
    }

    @Override
    public void onFilePressed(String file) {
        performOpenFileDiff(file);
    }

    @Override
    public void onGallerySelection(List<GalleryChooserFragment.MediaItem> selection) {
        for (GalleryChooserFragment.MediaItem media : selection) {
            Attachment attachment = new Attachment();
            attachment.mLocalUri = media.mUri;
            attachment.mName = media.mTitle;
            attachment.mSize = media.mSize;
            attachment.mMimeType = media.mMimeType;
            addPendingAttachments(attachment);
        }
    }

    @Override
    public void onSnippetSaved(Uri uri, String mimeType, long size) {
        Attachment attachment = new Attachment();
        attachment.mLocalUri = uri;
        attachment.mName = getString(R.string.snippet_dialog_snippet_file_name);
        attachment.mSize = size;
        attachment.mMimeType = mimeType;
        addPendingAttachments(attachment);
    }

    private void addPendingAttachments(Attachment attachment) {
        ModelHelper.addAttachment(attachment, mAttachments);
        updateReviewInfo(mResponse);
        mUiHandler.post(() -> mBinding.nestedScroll.fullScroll(View.FOCUS_DOWN));
    }

    @Override
    public void onAttachmentProviderSelection(Provider provider) {
        mBinding.reviewInfo.setAttachmentsSupport(mAttachmentsSupport);

        // Initialize the provider, if it isn't initialized previously
        AttachmentsProvider attachmentProvider =
                AttachmentsProviderFactory.getAttachmentProvider(getContext());
        if (!attachmentProvider.isSupported()) {
            attachmentProvider.initialize(getChildFragmentManager());
        }
    }

    private void performCreateAttachmentMetadata(final ReviewInput input)
            throws AuthenticationException {
        AttachmentsProvider attachmentsProvider =
                AttachmentsProviderFactory.getAttachmentProvider(getContext());
        if (!attachmentsProvider.isAvailable()) {
            return;
        }
        if (!attachmentsProvider.isSupported()) {
            // Cannot upload to server
            throw new AuthenticationException();
        }

        // Before sent the attachment, optimize the image attachments?
        if (Preferences.isAccountImageAttachmentsOptimizations(getContext(), mAccount)) {
            Bitmap.CompressFormat format =
                    Preferences.getAccountImageAttachmentsOptimizationsFormat(
                            getContext(), mAccount);
            // Adjust quality between 60/100 from 0/10
            int quality = 60 + (Preferences.getAccountImageAttachmentsOptimizationsQuality(
                    getContext(), mAccount) * 4);
            String mimeType = "image/" + format.name().toLowerCase(Locale.US);
            for (Attachment attachment : mAttachments) {
                if (attachment.mMimeType.startsWith("image/")
                        && !attachment.mMimeType.equals(mimeType)) {
                    //noinspection ConstantConditions
                    ContentResolver cr = getContext().getContentResolver();
                    try {
                        File out = BitmapUtils.optimizeImage(getContext(),
                                cr.openInputStream(attachment.mLocalUri), format, quality);
                        if (out != null) {
                            attachment.mLocalUri = Uri.fromFile(out);
                            attachment.mMimeType = mimeType;
                            attachment.mSize = out.length();
                        }
                    } catch (Exception ex) {
                        // Something fails optimizing the attachment
                        Log.w(TAG, "Something fails optimizing the attachment", ex);
                    }
                }
            }
        }

        // Create attachment metadata in the remote server. Later we uploaded the content
        // in a background service
        attachmentsProvider.createAttachmentsMetadata(mAttachments);

        // Build attachment format in review message
        StringBuilder sb = new StringBuilder();
        for (Attachment attachment : mAttachments) {
            if (!TextUtils.isEmpty(attachment.mId) && !TextUtils.isEmpty(attachment.mUrl)) {
                Attachment info = new Attachment();
                info.mName = attachment.mName;
                info.mMimeType = attachment.mMimeType;
                info.mSize = attachment.mSize;

                sb.append(String.format(Locale.US, "![ATTACHMENT:%s](%s)",
                        SerializationManager.getInstance().toJson(info), attachment.mUrl));
                sb.append("\n");
            }
        }
        if (sb.length() != 0) {
            if (input.message == null) {
                input.message = "";
            }
            input.message += "\n\n" + sb.toString();
        }
    }
}
