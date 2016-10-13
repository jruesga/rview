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
package com.ruesga.rview.widget;

import android.content.Context;
import android.content.res.Resources;
import android.databinding.DataBindingUtil;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.util.Pair;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.LayoutManager;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.google.gson.reflect.TypeToken;
import com.ruesga.rview.R;
import com.ruesga.rview.annotations.ProguardIgnored;
import com.ruesga.rview.databinding.DiffAdviseItemBinding;
import com.ruesga.rview.databinding.DiffCommentItemBinding;
import com.ruesga.rview.databinding.DiffDecoratorItemBinding;
import com.ruesga.rview.databinding.DiffSkipItemBinding;
import com.ruesga.rview.databinding.DiffSourceItemBinding;
import com.ruesga.rview.databinding.DiffViewBinding;
import com.ruesga.rview.gerrit.model.CommentInfo;
import com.ruesga.rview.gerrit.model.DiffInfo;
import com.ruesga.rview.misc.SerializationManager;
import com.ruesga.rview.tasks.AsyncImageDiffProcessor;
import com.ruesga.rview.tasks.AsyncImageDiffProcessor.OnImageDiffProcessEndedListener;
import com.ruesga.rview.tasks.AsyncTextDiffProcessor;
import com.ruesga.rview.tasks.AsyncTextDiffProcessor.OnTextDiffProcessEndedListener;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

public class DiffView extends FrameLayout {

    public static final int SIDE_BY_SIDE_MODE = 0;
    public static final int UNIFIED_MODE = 1;
    public static final int IMAGE_MODE = 2;

    private static final int SOURCE_VIEW_TYPE = 0;
    private static final int SKIP_VIEW_TYPE = 1;
    private static final int COMMENT_VIEW_TYPE = 2;
    private static final int ADVISE_VIEW_TYPE = 3;
    private static final int DECORATOR_VIEW_TYPE = 4;

    @ProguardIgnored
    @SuppressWarnings({"UnusedParameters", "unused"})
    public static class EventHandlers {
        private final DiffView mView;

        public EventHandlers(DiffView view) {
            mView = view;
        }

        public void onNewDraftPressed(View v) {
            if (mView.mCanEdit && mView.mOnCommentListener != null && v.getTag() != null) {
                String[] s = ((String) v.getTag()).split("/");
                mView.mOnCommentListener.onNewDraft(
                        v, Boolean.parseBoolean(s[0]), Integer.valueOf(s[1]));
            }
        }

        public void onReplyPressed(View v) {
            if (mView.mOnCommentListener != null) {
                String[] s = ((String) v.getTag()).split("/");
                Integer line = s[2] == null || s[2].equals("null") ? null : Integer.valueOf(s[2]);
                mView.mOnCommentListener.onReply(v, s[0], s[1], line);
            }
        }

        public void onDonePressed(View v) {
            if (mView.mOnCommentListener != null) {
                String[] s = ((String) v.getTag()).split("/");
                Integer line = s[2] == null || s[2].equals("null") ? null : Integer.valueOf(s[2]);
                mView.mOnCommentListener.onDone(v, s[0], s[1], line);
            }
        }

        public void onEditPressed(View v) {
            if (mView.mOnCommentListener != null) {
                String[] s = ((String) v.getTag()).split("/");
                String msg = (String) v.getTag(R.id.tag_key);
                Integer line = s[3] == null || s[3].equals("null") ? null : Integer.valueOf(s[3]);
                mView.mOnCommentListener.onEditDraft(
                        v, s[0], s[1], s[2], line, msg);
            }
        }

        public void onDeletePressed(View v) {
            if (mView.mOnCommentListener != null) {
                String[] s = ((String) v.getTag()).split("/");
                mView.mOnCommentListener.onDeleteDraft(v, s[0], s[1]);
            }
        }

        public void onSkipLinePressed(View v) {
            int position = (int) v.getTag();
            mView.onSkipLinePressed(position);
        }
    }

    public interface OnCommentListener {
        void onNewDraft(View v, boolean left, Integer line);

        void onReply(View v, String revisionId, String commentId, Integer line);

        void onDone(View v, String revisionId, String commentId, Integer line);

        void onEditDraft(View v, String revisionId, String draftId,
                String inReplyTo, Integer line, String msg);

        void onDeleteDraft(View v, String revisionId, String draftId);
    }

    private static class DiffSourceViewHolder extends RecyclerView.ViewHolder {
        private DiffSourceItemBinding mBinding;

        DiffSourceViewHolder(DiffSourceItemBinding binding) {
            super(binding.getRoot());
            mBinding = binding;
            mBinding.executePendingBindings();
        }
    }

    private static class DiffSkipViewHolder extends RecyclerView.ViewHolder {
        private DiffSkipItemBinding mBinding;

        DiffSkipViewHolder(DiffSkipItemBinding binding) {
            super(binding.getRoot());
            mBinding = binding;
            mBinding.executePendingBindings();
        }
    }

    private static class DiffCommentViewHolder extends RecyclerView.ViewHolder {
        private DiffCommentItemBinding mBinding;

        DiffCommentViewHolder(DiffCommentItemBinding binding) {
            super(binding.getRoot());
            mBinding = binding;
            mBinding.executePendingBindings();
        }
    }

    private static class DiffAdviseViewHolder extends RecyclerView.ViewHolder {
        private DiffAdviseItemBinding mBinding;

        DiffAdviseViewHolder(DiffAdviseItemBinding binding) {
            super(binding.getRoot());
            mBinding = binding;
            mBinding.executePendingBindings();
        }
    }

    private static class DiffDecoratorViewHolder extends RecyclerView.ViewHolder {
        private DiffDecoratorItemBinding mBinding;

        DiffDecoratorViewHolder(DiffDecoratorItemBinding binding) {
            super(binding.getRoot());
            mBinding = binding;
            mBinding.executePendingBindings();
        }
    }

    public static abstract class AbstractModel {
    }

    @ProguardIgnored
    public static class DiffInfoModel extends AbstractModel {
        public String lineNumberA;
        public String lineNumberB;
        public int colorA;
        public int colorB;
        public CharSequence lineA;
        public CharSequence lineB;
    }

    @ProguardIgnored
    public static class CommentModel extends AbstractModel {
        public CommentInfo commentA;
        public CommentInfo commentB;
        public boolean isDraft;
        public DiffInfoModel diff;
    }

    @ProguardIgnored
    public static class SkipLineModel extends AbstractModel {
        public String msg;
        public DiffInfoModel[] skippedLines;
    }

    @ProguardIgnored
    public static class AdviseModel extends AbstractModel {
        public String msg;
    }

    @ProguardIgnored
    public static class DecoratorModel extends AbstractModel {
    }

    @ProguardIgnored
    public static class DiffViewMeasurement {
        public float width = -1;
        public float lineWidth = -1;
        public float lineNumWidth = -1;

        private void clear() {
            width = -1;
            lineWidth = -1;
            lineNumWidth = -1;
        }
    }

    @ProguardIgnored
    public static class ImageDiffModel {
        public Drawable left;
        public String sizeLeft;
        public String dimensionsLeft;
        public Drawable right;
        public String sizeRight;
        public String dimensionsRight;
    }


    private class DiffAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private LayoutInflater mLayoutInflater;
        private final List<AbstractModel> mModel = new ArrayList<>();
        private final DiffViewMeasurement mDiffViewMeasurement = new DiffViewMeasurement();
        private final int mMode;

        DiffAdapter(int mode) {
            mLayoutInflater = LayoutInflater.from(getContext());
            mMode = mode;
        }

        private void update(List<AbstractModel> diffs) {
            mModel.clear();
            mModel.addAll(diffs);
            mDiffViewMeasurement.clear();
            computeViewChildMeasuresIfNeeded();
            notifyDataSetChanged();
        }

        private void showSkippedLinesAt(int position) {
            AbstractModel model = mDiffAdapter.mModel.get(position);
            if (model instanceof SkipLineModel) {
                SkipLineModel m = (SkipLineModel) model;
                mDiffAdapter.mModel.remove(position);
                int count = m.skippedLines.length;
                for (int i = 0; i < count; i++, position++) {
                    mDiffAdapter.mModel.add(position, m.skippedLines[i]);
                }
            }
            computeViewChildMeasuresIfNeeded();
            mDiffAdapter.notifyDataSetChanged();
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == SOURCE_VIEW_TYPE) {
                return new DiffSourceViewHolder(DataBindingUtil.inflate(
                        mLayoutInflater, R.layout.diff_source_item, parent, false));

            } else if (viewType == SKIP_VIEW_TYPE) {
                return new DiffSkipViewHolder(DataBindingUtil.inflate(
                        mLayoutInflater, R.layout.diff_skip_item, parent, false));

            } else if (viewType == COMMENT_VIEW_TYPE) {
                return new DiffCommentViewHolder(DataBindingUtil.inflate(
                        mLayoutInflater, R.layout.diff_comment_item, parent, false));

            } else if (viewType == ADVISE_VIEW_TYPE) {
                return new DiffAdviseViewHolder(DataBindingUtil.inflate(
                        mLayoutInflater, R.layout.diff_advise_item, parent, false));

            } else if (viewType == DECORATOR_VIEW_TYPE) {
                return new DiffDecoratorViewHolder(DataBindingUtil.inflate(
                        mLayoutInflater, R.layout.diff_decorator_item, parent, false));
            }
            return null;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder vh, int position) {
            AbstractModel model = mModel.get(position);

            if (vh instanceof DiffSourceViewHolder) {
                DiffSourceViewHolder holder = ((DiffSourceViewHolder) vh);
                DiffInfoModel diff = (DiffInfoModel) model;
                if (mMode == UNIFIED_MODE) {
                    CharSequence text = diff.lineA != null ? diff.lineA : diff.lineB;
                    holder.mBinding.diffA.setText(text, TextView.BufferType.NORMAL);
                } else {
                    holder.mBinding.diffA.setText(diff.lineA, TextView.BufferType.NORMAL);
                    holder.mBinding.diffB.setText(diff.lineB, TextView.BufferType.NORMAL);
                }

                holder.mBinding.setWrap(isWrapMode());
                holder.mBinding.setMode(mMode);
                holder.mBinding.setModel(diff);
                holder.mBinding.setMeasurement(mDiffViewMeasurement);
                if (mCanEdit) {
                    holder.mBinding.setHandlers(mEventHandlers);
                }
                holder.mBinding.executePendingBindings();

            } else if (vh instanceof DiffSkipViewHolder) {
                DiffSkipViewHolder holder = ((DiffSkipViewHolder) vh);
                SkipLineModel skip = (SkipLineModel) model;
                holder.mBinding.setWrap(isWrapMode());
                holder.mBinding.setModel(skip);
                holder.mBinding.setHandlers(mEventHandlers);
                holder.mBinding.setIndex(position);
                holder.mBinding.setMeasurement(mDiffViewMeasurement);
                holder.mBinding.executePendingBindings();

            } else if (vh instanceof DiffCommentViewHolder) {
                DiffCommentViewHolder holder = ((DiffCommentViewHolder) vh);
                CommentModel comment = (CommentModel) model;
                holder.mBinding.setCanEdit(mCanEdit);
                holder.mBinding.setWrap(isWrapMode());
                holder.mBinding.setMode(mMode);
                holder.mBinding.setModel(comment);
                holder.mBinding.setMeasurement(mDiffViewMeasurement);
                holder.mBinding.setHandlers(mEventHandlers);
                if (comment.commentA != null) {
                    holder.mBinding.actionsA.edit.setTag(R.id.tag_key, comment.commentA.message);
                }
                if (comment.commentB != null) {
                    holder.mBinding.actionsB.edit.setTag(R.id.tag_key, comment.commentB.message);
                }
                holder.mBinding.executePendingBindings();

            } else if (vh instanceof DiffAdviseViewHolder) {
                DiffAdviseViewHolder holder = ((DiffAdviseViewHolder) vh);
                AdviseModel advise = (AdviseModel) model;
                holder.mBinding.setWrap(isWrapMode());
                holder.mBinding.setMeasurement(mDiffViewMeasurement);
                holder.mBinding.setAdvise(advise.msg);
                holder.mBinding.executePendingBindings();

            } else if (vh instanceof DiffDecoratorViewHolder) {
                DiffDecoratorViewHolder holder = ((DiffDecoratorViewHolder) vh);
                holder.mBinding.setWrap(isWrapMode());
                holder.mBinding.setMeasurement(mDiffViewMeasurement);
                holder.mBinding.executePendingBindings();
            }

            if (mLayoutManager instanceof UnwrappedLinearLayoutManager) {
                ((UnwrappedLinearLayoutManager) mLayoutManager).requestBindViews();
            }
        }

        @Override
        public int getItemViewType(int position) {
            AbstractModel model = mModel.get(position);
            if (model instanceof DiffInfoModel) {
                return SOURCE_VIEW_TYPE;
            }
            if (model instanceof SkipLineModel) {
                return SKIP_VIEW_TYPE;
            }
            if (model instanceof CommentModel) {
                return COMMENT_VIEW_TYPE;
            }
            if (model instanceof AdviseModel) {
                return ADVISE_VIEW_TYPE;
            }
            return DECORATOR_VIEW_TYPE;
        }

        @Override
        public int getItemCount() {
            return mModel.size();
        }

        @SuppressWarnings("Convert2streamapi")
        private void computeViewChildMeasuresIfNeeded() {
            boolean wrap = isWrapMode();
            if (!mModel.isEmpty()) {
                final Resources res = getResources();
                TextPaint paint = new TextPaint();
                paint.setTextSize(res.getDimension(R.dimen.diff_line_text_size));
                float padding = res.getDimension(R.dimen.diff_line_text_padding);

                for (AbstractModel model : mModel) {
                    if (model instanceof DiffInfoModel) {
                        measureDiffInfoModel((DiffInfoModel) model, wrap, paint, padding);
                    }
                }

                // Give line number a minimum width
                mDiffViewMeasurement.lineNumWidth = Math.max(
                        mDiffViewMeasurement.lineNumWidth,
                        res.getDimension(R.dimen.diff_line_number_min_width));

                // Adjust padding
                mDiffViewMeasurement.lineNumWidth += (padding * 2f);
                float diffIndicatorWidth = mMode == UNIFIED_MODE
                        ? res.getDimension(R.dimen.diff_line_indicator_width) : 0;
                float separatorWidth = (mMode == UNIFIED_MODE ? 2 : 3) *
                        res.getDimension(R.dimen.diff_line_separator_width);
                float decorWidth = 2 * mDiffViewMeasurement.lineNumWidth +
                        diffIndicatorWidth + separatorWidth;

                mDiffViewMeasurement.width = decorWidth +
                        (mMode == UNIFIED_MODE ? 1 : 2) * mDiffViewMeasurement.lineWidth;

                if (mDiffViewMeasurement.width < getWidth()) {
                    mDiffViewMeasurement.width = getWidth();
                    if (mMode == UNIFIED_MODE) {
                        mDiffViewMeasurement.lineWidth = getWidth() - decorWidth;
                    } else {
                        mDiffViewMeasurement.lineWidth = (getWidth() - decorWidth) / 2;
                    }
                }

                // Update prefetch width to improve performance
                if (mLayoutManager instanceof UnwrappedLinearLayoutManager) {
                    ((UnwrappedLinearLayoutManager) mLayoutManager).setPrefetchedMeasuredWidth(
                            (int) Math.ceil(mDiffViewMeasurement.width));
                }

            } else {
                // Remove prefetched width
                if (mLayoutManager instanceof UnwrappedLinearLayoutManager) {
                    ((UnwrappedLinearLayoutManager) mLayoutManager).setPrefetchedMeasuredWidth(-1);
                }
            }
        }

        private void measureDiffInfoModel(
                DiffInfoModel diff, boolean wrap, TextPaint paint, float padding) {
            if (wrap) {
                mDiffViewMeasurement.lineWidth = MATCH_PARENT;
            } else {
                if (mMode == UNIFIED_MODE) {
                    // All lines are displayed in A
                    CharSequence line = diff.lineA != null ? diff.lineA : diff.lineB;
                    mDiffViewMeasurement.lineWidth = Math.max(
                            mDiffViewMeasurement.lineWidth,
                            paint.measureText(String.valueOf(line)) + padding);
                } else {
                    // Lines are displayed in A and B and both have the same size
                    if (diff.lineA != null) {
                        String lineA = String.valueOf(diff.lineA);
                        mDiffViewMeasurement.lineWidth = Math.max(
                                mDiffViewMeasurement.lineWidth,
                                paint.measureText(lineA) + padding);
                    }
                    if (diff.lineB != null) {
                        String lineB = String.valueOf(diff.lineB);
                        mDiffViewMeasurement.lineWidth = Math.max(
                                mDiffViewMeasurement.lineWidth,
                                paint.measureText(lineB) + padding);
                    }
                }
            }

            if (diff.lineNumberA != null) {
                mDiffViewMeasurement.lineNumWidth = Math.max(
                        mDiffViewMeasurement.lineNumWidth,
                        paint.measureText(String.valueOf(diff.lineNumberA)));
            }
            if (diff.lineNumberB != null) {
                mDiffViewMeasurement.lineNumWidth = Math.max(
                        mDiffViewMeasurement.lineNumWidth,
                        paint.measureText(String.valueOf(diff.lineNumberB)));
            }
        }
    }

    private OnTextDiffProcessEndedListener mTextProcessorListener
            = new OnTextDiffProcessEndedListener() {
        @Override
        public void onTextDiffProcessEnded(List<AbstractModel> model) {
            if (mNeedsNewLayoutManager || !mLayoutManager.equals(mTmpLayoutManager)) {
                mDiffAdapter = new DiffView.DiffAdapter(mDiffMode);
                if (mTmpLayoutManager != null) {
                    mLayoutManager = mTmpLayoutManager;
                }
                mRecyclerView.setLayoutManager(mLayoutManager);
                mRecyclerView.setAdapter(mDiffAdapter);
                mNeedsNewLayoutManager = false;
            }
            mDiffAdapter.update(model);
            mTmpLayoutManager = null;
        }
    };

    private OnImageDiffProcessEndedListener mImageProcessorListener
            = new OnImageDiffProcessEndedListener() {
        @Override
        public void onImageDiffProcessEnded(ImageDiffModel model) {
            mBinding.setImageDiffModel(model);
            mBinding.executePendingBindings();
        }
    };

    private DiffViewBinding mBinding;

    private final RecyclerView mRecyclerView;
    private DiffAdapter mDiffAdapter;
    private LayoutManager mLayoutManager;
    private LayoutManager mTmpLayoutManager;

    private boolean mHighlightTabs;
    private boolean mHighlightTrailingWhitespaces;
    private boolean mCanEdit;
    private int mDiffMode = UNIFIED_MODE;
    private DiffInfo mDiffInfo;
    private Pair<List<CommentInfo>, List<CommentInfo>> mComments;
    private Pair<List<CommentInfo>, List<CommentInfo>> mDrafts;
    private File mLeftContent;
    private File mRightContent;
    private OnCommentListener mOnCommentListener;

    private boolean mNeedsNewLayoutManager;

    private EventHandlers mEventHandlers;

    private AsyncTextDiffProcessor mTextDiffTask;
    private AsyncImageDiffProcessor mImageDiffTask;

    public DiffView(Context context) {
        this(context, null);
    }

    public DiffView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DiffView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mEventHandlers = new EventHandlers(this);
        mBinding = DataBindingUtil.inflate(
                LayoutInflater.from(context), R.layout.diff_view, this, false);

        mRecyclerView = mBinding.diffList;
        mLayoutManager = new LinearLayoutManager(context);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mDiffAdapter = new DiffAdapter(mDiffMode);
        mRecyclerView.setAdapter(mDiffAdapter);
        mRecyclerView.setVerticalScrollBarEnabled(true);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                MATCH_PARENT, MATCH_PARENT);
        addView(mBinding.getRoot(), params);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mBinding.unbind();

        // Stops running things now
        stopTasks();
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        SavedState savedState = new SavedState(super.onSaveInstanceState());
        savedState.mHighlightTabs = mHighlightTabs;
        savedState.mHighlightTrailingWhitespaces = mHighlightTrailingWhitespaces;
        savedState.mCanEdit = mCanEdit;
        savedState.mDiffMode = mDiffMode;
        savedState.mDiffInfo = SerializationManager.getInstance().toJson(mDiffInfo);
        savedState.mComments = SerializationManager.getInstance().toJson(mComments);
        savedState.mDrafts = SerializationManager.getInstance().toJson(mDrafts);
        return savedState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        //begin boilerplate code so parent classes can restore state
        if(!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState savedState = (SavedState) state;
        super.onRestoreInstanceState(savedState.getSuperState());

        mHighlightTabs = savedState.mHighlightTabs;
        mHighlightTrailingWhitespaces = savedState.mHighlightTrailingWhitespaces;
        mCanEdit = savedState.mCanEdit;
        mDiffMode = savedState.mDiffMode;
        mDiffInfo = SerializationManager.getInstance().fromJson(
                savedState.mDiffInfo, DiffInfo.class);
        Type type = new TypeToken<Pair<List<CommentInfo>, List<CommentInfo>>>(){}.getType();
        mComments = SerializationManager.getInstance().fromJson(savedState.mComments, type);
        mDrafts = SerializationManager.getInstance().fromJson(savedState.mDrafts, type);
    }

    public DiffView from(DiffInfo diff) {
        mDiffInfo = diff;
        return this;
    }

    public DiffView withComments(Pair<List<CommentInfo>, List<CommentInfo>> comments) {
        mComments = comments;
        return this;
    }

    public DiffView withDrafts(Pair<List<CommentInfo>, List<CommentInfo>> drafts) {
        mDrafts = drafts;
        return this;
    }

    public DiffView withLeftContent(File path) {
        mLeftContent = path;
        return this;
    }

    public DiffView withRightContent(File path) {
        mRightContent = path;
        return this;
    }

    public DiffView canEdit(boolean canEdit) {
        mCanEdit = canEdit;
        return this;
    }

    public DiffView highlightTabs(boolean highlight) {
        mHighlightTabs = highlight;
        return this;
    }

    public DiffView highlightTrailingWhitespaces(boolean highlight) {
        mHighlightTrailingWhitespaces = highlight;
        return this;
    }

    public DiffView wrap(boolean wrap) {
        if (isWrapMode() != wrap) {
            mTmpLayoutManager = wrap
                    ? new LinearLayoutManager(getContext())
                    : new UnwrappedLinearLayoutManager(getContext());
        } else {
            mTmpLayoutManager = mLayoutManager;
        }
        return this;
    }

    public DiffView mode(int mode) {
        if (mDiffMode != mode) {
            mDiffMode = mode;
            mNeedsNewLayoutManager = true;
            mBinding.setMode(mode);
        }
        return this;
    }

    public DiffView listenOn(OnCommentListener cb) {
        mOnCommentListener = cb;
        return this;
    }

    public void update() {
        stopTasks();

        if (mDiffMode != IMAGE_MODE) {
            mTextDiffTask = new AsyncTextDiffProcessor(getContext(), mDiffMode, mDiffInfo, mComments,
                    mDrafts, mHighlightTabs, mHighlightTrailingWhitespaces, mTextProcessorListener);
            mTextDiffTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            mImageDiffTask = new AsyncImageDiffProcessor(getContext(),
                    mLeftContent, mRightContent, mImageProcessorListener);
            mImageDiffTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    private boolean isWrapMode() {
        return mLayoutManager == null || !(mLayoutManager instanceof UnwrappedLinearLayoutManager);
    }

    private void stopTasks() {
        // Stops running things now
        if (mTextDiffTask != null) {
            mTextDiffTask.cancel(true);
        }
        if (mImageDiffTask != null) {
            mImageDiffTask.cancel(true);
        }
    }

    private void onSkipLinePressed(int position) {
        mDiffAdapter.showSkippedLinesAt(position);
    }

    static class SavedState extends BaseSavedState {
        boolean mHighlightTabs;
        boolean mHighlightTrailingWhitespaces;
        boolean mCanEdit;
        int mDiffMode;
        String mDiffInfo;
        String mComments;
        String mDrafts;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            mHighlightTabs = in.readInt() == 1;
            mHighlightTrailingWhitespaces = in.readInt() == 1;
            mCanEdit = in.readInt() == 1;
            mDiffMode = in.readInt();
            mDiffInfo = in.readString();
            mComments = in.readString();
            mDrafts = in.readString();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(mHighlightTabs ? 1 : 0);
            out.writeInt(mHighlightTrailingWhitespaces ? 1 : 0);
            out.writeInt(mCanEdit ? 1 : 0);
            out.writeInt(mDiffMode);
            out.writeString(mDiffInfo);
            out.writeString(mComments);
            out.writeString(mDrafts);
        }

        //required field that makes Parcelables from a Parcel
        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
                    public SavedState createFromParcel(Parcel in) {
                        return new SavedState(in);
                    }
                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }
                };
    }
}
