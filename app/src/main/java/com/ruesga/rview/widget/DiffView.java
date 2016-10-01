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
import android.databinding.DataBindingUtil;
import android.os.AsyncTask;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.Pair;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.LayoutManager;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.BackgroundColorSpan;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.google.gson.reflect.TypeToken;
import com.ruesga.rview.R;
import com.ruesga.rview.annotations.ProguardIgnored;
import com.ruesga.rview.databinding.DiffCommentItemBinding;
import com.ruesga.rview.databinding.DiffSkipItemBinding;
import com.ruesga.rview.databinding.DiffSourceItemBinding;
import com.ruesga.rview.gerrit.model.CommentInfo;
import com.ruesga.rview.gerrit.model.DiffContentInfo;
import com.ruesga.rview.misc.SerializationManager;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

public class DiffView extends FrameLayout {

    private static final int SKIPPED_LINES = 10;

    public static final int SIDE_BY_SIDE_MODE = 0;
    public static final int UNIFIED_MODE = 1;

    private static final int SOURCE_VIEW_TYPE = 0;
    private static final int SKIP_VIEW_TYPE = 1;
    private static final int COMMENT_VIEW_TYPE = 2;

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

    public static abstract class AbstractModel {
    }

    @ProguardIgnored
    public static class DiffInfoModel extends AbstractModel {
        public String lineNumberA;
        public String lineNumberB;
        public int colorA;
        public int colorB;
        CharSequence lineA;
        CharSequence lineB;
    }

    @ProguardIgnored
    public static class CommentModel extends AbstractModel {
        public CommentInfo commentA;
        public CommentInfo commentB;
        public boolean isDraft;
    }

    @ProguardIgnored
    public static class SkipLineModel extends AbstractModel {
        public String msg;
    }

    @ProguardIgnored
    public static class DiffViewMeasurement {
        public int width = -1;
        public int lineAWidth = -1;
        public int lineBWidth = -1;
        public int lineNumAWidth = -1;
        public int lineNumBWidth = -1;

        private void clear() {
            width = -1;
            lineAWidth = -1;
            lineBWidth = -1;
            lineNumAWidth = -1;
            lineNumBWidth = -1;
        }
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

        void update(List<AbstractModel> diffs) {
            mModel.clear();
            mModel.addAll(diffs);
            mDiffViewMeasurement.clear();
            computeViewChildMeasuresIfNeeded();
            notifyDataSetChanged();
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

            } else if (vh instanceof DiffSkipViewHolder) {
                DiffSkipViewHolder holder = ((DiffSkipViewHolder) vh);
                SkipLineModel skip = (SkipLineModel) model;
                holder.mBinding.setWrap(isWrapMode());
                holder.mBinding.setMode(mMode);
                holder.mBinding.setModel(skip);
                holder.mBinding.setMeasurement(mDiffViewMeasurement);

            } else if (vh instanceof DiffCommentViewHolder) {
                DiffCommentViewHolder holder = ((DiffCommentViewHolder) vh);
                CommentModel comment = (CommentModel) model;
                holder.mBinding.setWrap(isWrapMode());
                holder.mBinding.setMode(mMode);
                holder.mBinding.setModel(comment);
                holder.mBinding.setMeasurement(mDiffViewMeasurement);
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
            return COMMENT_VIEW_TYPE;
        }

        @Override
        public int getItemCount() {
            return mModel.size();
        }

        private void computeViewChildMeasuresIfNeeded() {
            boolean wrap = isWrapMode();
            if (!mModel.isEmpty()) {
                int dp = (int) getResources().getDisplayMetrics().density;
                TextPaint paint = new TextPaint();
                paint.setTextSize(12 * dp);
                int padding = 3 * dp;

                for (AbstractModel model : mModel) {
                    if (model instanceof DiffInfoModel) {
                        DiffInfoModel diff = (DiffInfoModel) model;

                        if (wrap) {
                            mDiffViewMeasurement.lineAWidth = MATCH_PARENT;
                            mDiffViewMeasurement.lineBWidth = MATCH_PARENT;
                        } else {
                            if (mMode == UNIFIED_MODE) {
                                // All lines are displayed in A
                                CharSequence line = diff.lineA != null ? diff.lineA : diff.lineB;
                                int width = (int) paint.measureText(String.valueOf(line)) + padding;
                                mDiffViewMeasurement.lineAWidth = Math.max(
                                        mDiffViewMeasurement.lineAWidth, width);
                            } else {
                                // All lines are displayed in A
                                if (diff.lineA != null) {
                                    String lineA = String.valueOf(diff.lineA);
                                    int width = (int) paint.measureText(lineA) + padding;
                                    mDiffViewMeasurement.lineAWidth = Math.max(
                                            mDiffViewMeasurement.lineAWidth, width);
                                }
                                if (diff.lineB != null) {
                                    String lineB = String.valueOf(diff.lineB);
                                    int width = (int) paint.measureText(lineB) + padding;
                                    mDiffViewMeasurement.lineBWidth = Math.max(
                                            mDiffViewMeasurement.lineBWidth, width);
                                }
                            }
                        }

                        if (diff.lineNumberA != null) {
                            mDiffViewMeasurement.lineNumAWidth = Math.max(
                                    mDiffViewMeasurement.lineNumAWidth,
                                    (int) paint.measureText(String.valueOf(diff.lineNumberA)));
                        }
                        if (diff.lineNumberB != null) {
                            mDiffViewMeasurement.lineNumBWidth = Math.max(
                                    mDiffViewMeasurement.lineNumBWidth,
                                    (int) paint.measureText(String.valueOf(diff.lineNumberB)));
                        }
                    }
                }

                // User same size for A y B number and apply a minimum
                mDiffViewMeasurement.lineNumAWidth = mDiffViewMeasurement.lineNumBWidth =
                        Math.max(mDiffViewMeasurement.lineNumAWidth,
                                mDiffViewMeasurement.lineNumBWidth);
                mDiffViewMeasurement.lineNumAWidth = mDiffViewMeasurement.lineNumBWidth =
                        Math.max(mDiffViewMeasurement.lineNumAWidth, 20 * dp);

                // Adjust padding
                mDiffViewMeasurement.lineNumAWidth += (padding * 2);
                mDiffViewMeasurement.lineNumBWidth += (padding * 2);
                int diffIndicatorWidth = 16 * dp;
                mDiffViewMeasurement.width =
                        mDiffViewMeasurement.lineNumAWidth + mDiffViewMeasurement.lineNumBWidth +
                        mDiffViewMeasurement.lineAWidth + mDiffViewMeasurement.lineBWidth +
                        diffIndicatorWidth + (dp * 2);

                if (mDiffViewMeasurement.width < getWidth()) {
                    mDiffViewMeasurement.width = getWidth();
                    if (mMode == UNIFIED_MODE) {
                        mDiffViewMeasurement.lineAWidth = getWidth() -
                                mDiffViewMeasurement.lineNumAWidth -
                                mDiffViewMeasurement.lineNumBWidth -
                                diffIndicatorWidth - (dp * 2);
                    }
                }
            }
            long end = System.currentTimeMillis();
        }
    }

    private class AsyncDiffProcessor extends AsyncTask<Void, Void, List<AbstractModel>> {
        private final int mMode;
        private final DiffContentInfo[] mDiffs;
        private final Pair<List<CommentInfo>, List<CommentInfo>> mComments;

        AsyncDiffProcessor(int mode, DiffContentInfo[] diffs,
                Pair<List<CommentInfo>, List<CommentInfo>> comments,
                Pair<List<CommentInfo>, List<CommentInfo>> drafts) {
            mMode = mode;
            mDiffs = diffs;
            mComments = comments;
            mDrafts = drafts;
        }

        @Override
        protected List<AbstractModel> doInBackground(Void... params) {
            return processDrafts(processComments(processDiffs()));
        }

        @Override
        protected void onPostExecute(List<AbstractModel> model) {
            mDiffAdapter = new DiffAdapter(mDiffMode);
            mLayoutManager = mTmpLayoutManager;
            mRecyclerView.setLayoutManager(mLayoutManager);
            mRecyclerView.setAdapter(mDiffAdapter);
            mDiffAdapter.update(model);
        }

        private List<AbstractModel> processDiffs() {
            if (mMode == SIDE_BY_SIDE_MODE) {
                return processSideBySideDiffs();
            }
            return processUnifiedDiffs();
        }

        private List<AbstractModel> processSideBySideDiffs() {
            if (mDiffs == null) {
                return new ArrayList<>();
            }

            int lineNumberA = 0;
            int lineNumberB = 0;

            final Spannable.Factory spannableFactory = Spannable.Factory.getInstance();
            final int noColor = ContextCompat.getColor(getContext(), android.R.color.transparent);
            final int addedBgColor = ContextCompat.getColor(
                    getContext(), R.color.diffAddedBackgroundColor);
            final int addedFgColor = ContextCompat.getColor(
                    getContext(), R.color.diffAddedForegroundColor);
            final int deletedBgColor = ContextCompat.getColor(
                    getContext(), R.color.diffDeletedBackgroundColor);
            final int deletedFgColor = ContextCompat.getColor(
                    getContext(), R.color.diffDeletedForegroundColor);

            List<AbstractModel> model = new ArrayList<>();
            int j = 0;
            for (DiffContentInfo diff : mDiffs) {
                if (diff.ab != null) {
                    // Unchanged lines
                    int count = diff.ab.length;
                    int skipStartAt = -1, skippedLines = -1;
                    boolean breakAfterSkip = false;
                    if (j == 0 && diff.ab.length > SKIPPED_LINES) {
                        skipStartAt = 0;
                        skippedLines = count - SKIPPED_LINES - skipStartAt;
                    } else if (j == (mDiffs.length - 1) && diff.ab.length > SKIPPED_LINES) {
                        skipStartAt = SKIPPED_LINES;
                        skippedLines = count - skipStartAt;
                        breakAfterSkip = true;
                    } else if (diff.ab.length > (SKIPPED_LINES * 2)) {
                        skipStartAt = SKIPPED_LINES;
                        skippedLines = count - SKIPPED_LINES - skipStartAt;
                    }

                    for (int i = 0; i < count; i++) {
                        if (skipStartAt != -1 && skipStartAt == i) {
                            lineNumberA += skippedLines;
                            lineNumberB += skippedLines;
                            i += skippedLines;
                            SkipLineModel skip = new SkipLineModel();
                            skip.msg = getResources().getQuantityString(
                                    R.plurals.skipped_lines, skippedLines, skippedLines);
                            model.add(skip);
                            if (breakAfterSkip) {
                                break;
                            }
                        }

                        String line = diff.ab[i];
                        DiffInfoModel m = new DiffInfoModel();
                        m.lineNumberA = String.valueOf(++lineNumberA);
                        m.lineNumberB = String.valueOf(++lineNumberB);
                        m.lineA = line;
                        m.lineB = line;
                        m.colorA = noColor;
                        m.colorB = noColor;
                        model.add(m);
                    }
                } else {
                    int posA = 0;
                    int posB = 0;
                    int count = Math.max(
                            diff.a == null ? 0 : diff.a.length,
                            diff.b == null ? 0 : diff.b.length);
                    for (int i = 0; i < count; i++) {
                        DiffInfoModel m = new DiffInfoModel();
                        m.colorA = noColor;
                        m.colorB = noColor;

                        if (diff.a != null && i < diff.a.length) {
                            String line = diff.a[i];
                            m.lineNumberA = String.valueOf(++lineNumberA);
                            if (diff.editA != null) {
                                Spannable span = spannableFactory.newSpannable(line);
                                int s2 = 0;
                                for (ArrayList<Integer> intra : diff.editA) {
                                    int s1 = s2 + intra.get(0);
                                    s2 = s1 + intra.get(1);
                                    int l = posA + line.length();
                                    if ((s1 >= posA && s1 <= l) || (s2 >= posA && s2 <= l)
                                            || (s1 <= posA && s2 >= l)) {
                                        span.setSpan(new BackgroundColorSpan(deletedFgColor),
                                                Math.max(posA, s1) - posA, Math.min(l, s2) - posA,
                                                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                                    }
                                }
                                m.lineA = span;
                            } else {
                                m.lineA = line;
                            }
                            m.colorA = deletedBgColor;
                            posA += line.length() + 1;
                        }

                        if (diff.b != null && i < diff.b.length) {
                            String line = diff.b[i];
                            m.lineNumberB = String.valueOf(++lineNumberB);
                            if (diff.editB != null) {
                                Spannable span = spannableFactory.newSpannable(line);
                                int s2 = 0;
                                for (ArrayList<Integer> intra : diff.editB) {
                                    int s1 = s2 + intra.get(0);
                                    s2 = s1 + intra.get(1);
                                    int l = posB + line.length();
                                    if ((s1 >= posB && s1 <= l) || (s2 >= posB && s2 <= l)
                                            || (s1 <= posB && s2 >= l)) {
                                        span.setSpan(new BackgroundColorSpan(addedFgColor),
                                                Math.max(posB, s1) - posB, Math.min(l, s2) - posB,
                                                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                                    }
                                }
                                m.lineB = span;
                            } else {
                                m.lineB = line;
                            }
                            m.colorB = addedBgColor;
                            posB += line.length() + 1;
                        }

                        model.add(m);
                    }
                }
                j++;
            }
            return model;
        }

        private List<AbstractModel> processUnifiedDiffs() {
            if (mDiffs == null) {
                return new ArrayList<>();
            }

            int lineNumberA = 0;
            int lineNumberB = 0;

            final Spannable.Factory spannableFactory = Spannable.Factory.getInstance();
            final int noColor = ContextCompat.getColor(getContext(), android.R.color.transparent);
            final int addedBgColor = ContextCompat.getColor(
                    getContext(), R.color.diffAddedBackgroundColor);
            final int addedFgColor = ContextCompat.getColor(
                    getContext(), R.color.diffAddedForegroundColor);
            final int deletedBgColor = ContextCompat.getColor(
                    getContext(), R.color.diffDeletedBackgroundColor);
            final int deletedFgColor = ContextCompat.getColor(
                    getContext(), R.color.diffDeletedForegroundColor);

            List<AbstractModel> model = new ArrayList<>();
            int j = 0;
            for (DiffContentInfo diff : mDiffs) {
                if (diff.ab != null) {
                    // Unchanged lines
                    int count = diff.ab.length;
                    int skipStartAt = -1, skippedLines = -1;
                    boolean breakAfterSkip = false;
                    if (j == 0 && diff.ab.length > SKIPPED_LINES) {
                        skipStartAt = 0;
                        skippedLines = count - SKIPPED_LINES - skipStartAt;
                    } else if (j == (mDiffs.length - 1) && diff.ab.length > SKIPPED_LINES) {
                        skipStartAt = SKIPPED_LINES;
                        skippedLines = count - skipStartAt;
                        breakAfterSkip = true;
                    } else if (diff.ab.length > (SKIPPED_LINES * 2)) {
                        skipStartAt = SKIPPED_LINES;
                        skippedLines = count - SKIPPED_LINES - skipStartAt;
                    }

                    for (int i = 0; i < count; i++) {
                        if (skipStartAt != -1 && skipStartAt == i) {
                            lineNumberA += skippedLines;
                            lineNumberB += skippedLines;
                            i += skippedLines;
                            SkipLineModel skip = new SkipLineModel();
                            skip.msg = getResources().getQuantityString(
                                    R.plurals.skipped_lines, skippedLines, skippedLines);
                            model.add(skip);
                            if (breakAfterSkip) {
                                break;
                            }
                        }

                        String line = diff.ab[i];
                        DiffInfoModel m = new DiffInfoModel();
                        m.lineNumberA = String.valueOf(++lineNumberA);
                        m.lineNumberB = String.valueOf(++lineNumberB);
                        m.lineA = line;
                        m.lineB = line;
                        m.colorA = noColor;
                        m.colorB = noColor;
                        model.add(m);
                    }
                } else {
                    if (diff.a != null) {
                        int pos = 0;
                        for (String line : diff.a) {
                            DiffInfoModel m = new DiffInfoModel();
                            m.lineNumberA = String.valueOf(++lineNumberA);
                            if (diff.editA != null) {
                                Spannable span = spannableFactory.newSpannable(line);
                                int s2 = 0;
                                for (ArrayList<Integer> intra : diff.editA) {
                                    int s1 = s2 + intra.get(0);
                                    s2 = s1 + intra.get(1);
                                    int l = pos + line.length();
                                    if ((s1 >= pos && s1 <= l) || (s2 >= pos && s2 <= l)
                                            || (s1 <= pos && s2 >= l)) {
                                        span.setSpan(new BackgroundColorSpan(deletedFgColor),
                                                Math.max(pos, s1) - pos, Math.min(l, s2) - pos,
                                                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                                    }
                                }
                                m.lineA = span;
                            } else {
                                m.lineA = line;
                            }
                            m.colorA = deletedBgColor;
                            m.colorB = noColor;
                            model.add(m);
                            pos += line.length() + 1;
                        }
                    }
                    if (diff.b != null) {
                        int pos = 0;
                        for (String line : diff.b) {
                            DiffInfoModel m = new DiffInfoModel();
                            m.lineNumberB = String.valueOf(++lineNumberB);
                            if (diff.editB != null) {
                                Spannable span = spannableFactory.newSpannable(line);
                                int s2 = 0;
                                for (ArrayList<Integer> intra : diff.editB) {
                                    int s1 = s2 + intra.get(0);
                                    s2 = s1 + intra.get(1);
                                    int l = pos + line.length();
                                    if ((s1 >= pos && s1 <= l) || (s2 >= pos && s2 <= l)
                                            || (s1 <= pos && s2 >= l)) {
                                        span.setSpan(new BackgroundColorSpan(addedFgColor),
                                                Math.max(pos, s1) - pos, Math.min(l, s2) - pos,
                                                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                                    }
                                }
                                m.lineB = span;
                            } else {
                                m.lineB = line;
                            }
                            m.colorA = addedBgColor;
                            m.colorB = noColor;
                            model.add(m);
                            pos += line.length() + 1;
                        }
                    }
                }
                j++;
            }
            return model;
        }

        private List<AbstractModel> processComments(List<AbstractModel> model) {
            if (mComments != null) {
                // Comments on A
                if (mComments.second != null) {
                    addCommentsToModel(model, mComments.first, true, false);
                }

                // Comments on B
                if (mComments.second != null) {
                    addCommentsToModel(model, mComments.second, false, false);
                }
            }
            return model;
        }

        private List<AbstractModel> processDrafts(List<AbstractModel> model) {
            if (mDrafts != null) {
                // Comments on A
                if (mDrafts.second != null) {
                    addCommentsToModel(model, mDrafts.first, true, true);
                }

                // Comments on B
                if (mDrafts.second != null) {
                    addCommentsToModel(model, mDrafts.second, false, true);
                }
            }
            return model;
        }

        private void addCommentsToModel(List<AbstractModel> model,
                List<CommentInfo> comments, boolean isA, boolean isDraft) {
            if (comments == null) {
                return;
            }
            int count = comments.size();
            for (int i = 0; i < count; i++) {
                CommentInfo comment = comments.get(i);
                int pos = findLineInModel(model, isA, comment.line);
                if (pos != -1) {
                    if (mMode == UNIFIED_MODE) {
                        CommentModel commentModel = new CommentModel();
                        commentModel.isDraft = isDraft;
                        commentModel.commentA = comment;
                        int nextPos = findNextPositionWithoutComment(model, pos);
                        if (nextPos != -1) {
                            model.add(nextPos, commentModel);
                        } else {
                            model.add(pos + 1, commentModel);
                        }
                    } else {
                        int reusablePos = findReusableCommentView(model, pos, isA);
                        if (reusablePos != -1) {
                            CommentModel commentModel = (CommentModel) model.get(reusablePos);
                            commentModel.isDraft = isDraft;
                            if (isA) {
                                commentModel.commentA = comment;
                            } else {
                                commentModel.commentB = comment;
                            }
                        } else {
                            CommentModel commentModel = new CommentModel();
                            commentModel.isDraft = isDraft;
                            if (isA) {
                                commentModel.commentA = comment;
                            } else {
                                commentModel.commentB = comment;
                            }
                            int nextPos = findNextPositionWithoutComment(model, pos);
                            if (nextPos != -1) {
                                model.add(nextPos, commentModel);
                            } else {
                                model.add(pos + 1, commentModel);
                            }
                        }
                    }
                }
            }
        }

        private int findLineInModel(List<AbstractModel> model, boolean isA, int line) {
            int count = model.size();
            for (int i = 0; i < count; i++) {
                AbstractModel m = model.get(i);
                if (m instanceof DiffInfoModel) {
                    DiffInfoModel diff = (DiffInfoModel) m;
                    if (isA && diff.lineNumberA != null
                            && Integer.valueOf(diff.lineNumberA) == line) {
                        return i;
                    } else if (!isA && diff.lineNumberB != null
                            && Integer.valueOf(diff.lineNumberB) == line) {
                        return i;
                    }
                }
            }
            return -1;
        }

        private int findReusableCommentView(List<AbstractModel> model, int pos, boolean isA) {
            int count = model.size();
            for (int i = pos + 1; i < count; i++) {
                AbstractModel m = model.get(i);
                if (!(m instanceof CommentModel)) {
                    break;
                }

                CommentModel comment = (CommentModel) m;
                if ((isA && comment.commentA == null && comment.commentB != null)
                        || (!isA && comment.commentB == null && comment.commentA != null)) {
                    return i;
                }
            }
            return -1;
        }

        private int findNextPositionWithoutComment(List<AbstractModel> model, int pos) {
            int count = model.size();
            for (int i = pos + 1; i < count; i++) {
                AbstractModel m = model.get(i);
                if (!(m instanceof CommentModel)) {
                    return i;
                }
            }
            return -1;
        }
    }


    private final RecyclerView mRecyclerView;
    private DiffAdapter mDiffAdapter;
    private LayoutManager mLayoutManager;
    private LayoutManager mTmpLayoutManager;

    private int mDiffMode = UNIFIED_MODE;
    private DiffContentInfo[] mAllDiffs;
    private Pair<List<CommentInfo>, List<CommentInfo>> mComments;
    private Pair<List<CommentInfo>, List<CommentInfo>> mDrafts;

    private AsyncDiffProcessor mTask;

    public DiffView(Context context) {
        this(context, null);
    }

    public DiffView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DiffView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                MATCH_PARENT, MATCH_PARENT);
        mRecyclerView = new RecyclerView(context);
        mLayoutManager = new LinearLayoutManager(context);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mDiffAdapter = new DiffAdapter(mDiffMode);
        mRecyclerView.setAdapter(mDiffAdapter);
        mRecyclerView.setVerticalScrollBarEnabled(true);
        addView(mRecyclerView, params);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        update();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        // Stops running things now
        if (mTask != null) {
            mTask.cancel(true);
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        SavedState savedState = new SavedState(super.onSaveInstanceState());
        savedState.mDiffMode = mDiffMode;
        savedState.mAllDiffs = SerializationManager.getInstance().toJson(mAllDiffs);
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

        mDiffMode = savedState.mDiffMode;
        Type type = new TypeToken<DiffContentInfo[]>(){}.getType();
        mAllDiffs = SerializationManager.getInstance().fromJson(savedState.mAllDiffs, type);
    }

    public DiffView from(DiffContentInfo[] allDiffs) {
        mAllDiffs = allDiffs;
        return this;
    }

    public DiffView withComments(Pair<List<CommentInfo>, List<CommentInfo>> comments) {
        mComments = comments;
        return this;
    }

    public DiffView withDrafts(Pair<List<CommentInfo>, List<CommentInfo>> drafts) {
        mDrafts = mDrafts;
        return this;
    }

    public DiffView wrap(boolean wrap) {
        mTmpLayoutManager = wrap
                ? new LinearLayoutManager(getContext())
                : new UnwrappedLinearLayoutManager(getContext());
        return this;
    }

    public DiffView mode(int mode) {
        mDiffMode = mode;
        return this;
    }

    public void update() {
        if (mTask != null) {
            mTask.cancel(true);
        }

        mTask = new AsyncDiffProcessor(mDiffMode, mAllDiffs, mComments, mDrafts);
        mTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private boolean isWrapMode() {
        return mLayoutManager == null || !(mLayoutManager instanceof UnwrappedLinearLayoutManager);
    }


    static class SavedState extends BaseSavedState {
        int mDiffMode;
        String mAllDiffs;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            this.mDiffMode = in.readInt();
            this.mAllDiffs = in.readString();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(this.mDiffMode);
            out.writeString(this.mAllDiffs);
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
