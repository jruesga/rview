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
package com.ruesga.rview.tasks;

import android.content.Context;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.Pair;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;

import com.ruesga.rview.R;
import com.ruesga.rview.gerrit.model.CommentInfo;
import com.ruesga.rview.gerrit.model.DiffContentInfo;
import com.ruesga.rview.gerrit.model.DiffInfo;
import com.ruesga.rview.misc.StringHelper;
import com.ruesga.rview.widget.DiffView;
import com.ruesga.rview.widget.DiffView.DiffInfoModel;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AsyncTextDiffProcessor extends AsyncTask<Void, Void, List<DiffView.AbstractModel>> {

    public interface OnTextDiffProcessEndedListener {
        void onTextDiffProcessEnded(List<DiffView.AbstractModel> model);
    }

    private static final int SKIPPED_LINES = 10;

    private final Pattern HIGHLIGHT_TRAIL_SPACES_PATTERN
            = Pattern.compile("\\s+$", Pattern.MULTILINE);

    private final Context mContext;
    private final int mMode;
    private final boolean mIsBinary;
    private final DiffContentInfo[] mDiffs;
    private final Pair<List<CommentInfo>, List<CommentInfo>> mComments;
    private final Pair<List<CommentInfo>, List<CommentInfo>> mDrafts;
    private final boolean mHighlightTabs;
    private final boolean mHighlightTrailingWhitespaces;
    private final OnTextDiffProcessEndedListener mCallback;

    public AsyncTextDiffProcessor(Context context, int mode, DiffInfo diff,
            Pair<List<CommentInfo>, List<CommentInfo>> comments,
            Pair<List<CommentInfo>, List<CommentInfo>> drafts,
            boolean highlightTabs, boolean highlightTrailingWhitespaces,
            OnTextDiffProcessEndedListener cb) {
        mContext = context;
        mMode = mode;
        mIsBinary = diff.binary;
        mDiffs = diff.content;
        mComments = comments;
        mDrafts = drafts;
        mHighlightTabs = highlightTabs;
        mHighlightTrailingWhitespaces = highlightTrailingWhitespaces;
        mCallback = cb;
    }

    @Override
    protected List<DiffView.AbstractModel> doInBackground(Void... params) {
        return processDrafts(processComments(processDiffs()));
    }

    @Override
    protected void onPostExecute(List<DiffView.AbstractModel> model) {
        mCallback.onTextDiffProcessEnded(model);
    }

    private List<DiffView.AbstractModel> processDiffs() {
        List<DiffView.AbstractModel> model;
        if (mMode == DiffView.SIDE_BY_SIDE_MODE) {
            model = processSideBySideDiffs();
        } else {
            model = processUnifiedDiffs();
        }
        if (!model.isEmpty()) {
            model.add(new DiffView.DecoratorModel());
        }
        return model;
    }

    private List<DiffView.AbstractModel> processSideBySideDiffs() {
        final List<DiffView.AbstractModel> model = new ArrayList<>();
        addBinaryAdviseIfNeeded(model);

        if (mDiffs == null) {
            return model;
        }

        int lineNumberA = 0;
        int lineNumberB = 0;

        final Spannable.Factory spannableFactory = Spannable.Factory.getInstance();
        final int noColor = ContextCompat.getColor(mContext, android.R.color.transparent);
        final int addedBgColor = ContextCompat.getColor(
                mContext, R.color.diffAddedBackgroundColor);
        final int addedFgColor = ContextCompat.getColor(
                mContext, R.color.diffAddedForegroundColor);
        final int deletedBgColor = ContextCompat.getColor(
                mContext, R.color.diffDeletedBackgroundColor);
        final int deletedFgColor = ContextCompat.getColor(
                mContext, R.color.diffDeletedForegroundColor);


        boolean noDiffs = mDiffs.length == 1 && mDiffs[0].a == null  && mDiffs[0].b == null;
        int j = 0;
        for (DiffContentInfo diff : mDiffs) {
            if (diff.ab != null) {
                // Unchanged lines
                int[] p = processUnchangedLines(
                        diff, model, j, lineNumberA, lineNumberB, noColor, noDiffs);
                lineNumberA = p[0];
                lineNumberB = p[0];
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
                            Spannable span = spannableFactory.newSpannable(prepareTabs(line));
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
                            // No intraline data, but it still could differ at start or at end
                            processNoIntralineDataA(diff, m, line, deletedFgColor);
                        }
                        m.colorA = deletedBgColor;
                        posA += line.length() + 1;
                    }

                    if (diff.b != null && i < diff.b.length) {
                        String line = diff.b[i];
                        m.lineNumberB = String.valueOf(++lineNumberB);
                        if (diff.editB != null) {
                            Spannable span = spannableFactory.newSpannable(prepareTabs(line));
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
                            // No intraline data, but it still could differ at start or at end
                            processNoIntralineDataB(diff, m, line, addedFgColor);
                        }
                        m.colorB = addedBgColor;
                        posB += line.length() + 1;
                    }
                    processHighlights(m);
                    model.add(m);
                }
            }
            j++;
        }
        return model;
    }

    private List<DiffView.AbstractModel> processUnifiedDiffs() {
        final List<DiffView.AbstractModel> model = new ArrayList<>();
        addBinaryAdviseIfNeeded(model);

        if (mDiffs == null) {
            return model;
        }

        int lineNumberA = 0;
        int lineNumberB = 0;

        final Spannable.Factory spannableFactory = Spannable.Factory.getInstance();
        final int noColor = ContextCompat.getColor(mContext, android.R.color.transparent);
        final int addedBgColor = ContextCompat.getColor(
                mContext, R.color.diffAddedBackgroundColor);
        final int addedFgColor = ContextCompat.getColor(
                mContext, R.color.diffAddedForegroundColor);
        final int deletedBgColor = ContextCompat.getColor(
                mContext, R.color.diffDeletedBackgroundColor);
        final int deletedFgColor = ContextCompat.getColor(
                mContext, R.color.diffDeletedForegroundColor);

        boolean noDiffs = mDiffs.length == 1 && mDiffs[0].a == null  && mDiffs[0].b == null;
        int j = 0;
        for (DiffContentInfo diff : mDiffs) {
            if (diff.ab != null) {
                // Unchanged lines
                int[] p = processUnchangedLines(
                        diff, model, j, lineNumberA, lineNumberB, noColor, noDiffs);
                lineNumberA = p[0];
                lineNumberB = p[0];
            } else {
                if (diff.a != null) {
                    int pos = 0;
                    for (String line : diff.a) {
                        DiffInfoModel m = new DiffInfoModel();
                        m.lineNumberA = String.valueOf(++lineNumberA);
                        if (diff.editA != null) {
                            Spannable span = spannableFactory.newSpannable(prepareTabs(line));
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
                            // No intraline data, but it still could differ at start or at end
                            processNoIntralineDataA(diff, m, line, deletedFgColor);
                        }
                        m.colorA = deletedBgColor;
                        m.colorB = noColor;
                        processHighlights(m);
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
                            Spannable span = spannableFactory.newSpannable(prepareTabs(line));
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
                            // No intraline data, but it still could differ at start or at end
                            processNoIntralineDataB(diff, m, line, addedFgColor);
                        }
                        m.colorA = addedBgColor;
                        m.colorB = noColor;
                        processHighlights(m);
                        model.add(m);
                        pos += line.length() + 1;
                    }
                }
            }
            j++;
        }
        return model;
    }

    private void addBinaryAdviseIfNeeded(List<DiffView.AbstractModel> model) {
        if (mIsBinary) {
            DiffView.AdviseModel advise = new DiffView.AdviseModel();
            advise.msg = mContext.getString(R.string.diff_viewer_binary_file);
            model.add(advise);
        }
    }

    private int[] processUnchangedLines(DiffContentInfo diff, List<DiffView.AbstractModel> model,
            int j, int lineNumberA, int lineNumberB, int noColor, boolean noDiffs) {
        if (noDiffs) {
            DiffView.AdviseModel advise = new DiffView.AdviseModel();
            advise.msg = mContext.getString(R.string.diff_viewer_no_diffs);
            model.add(advise);
        }

        int count = diff.ab.length;
        int skipStartAt = -1, skippedLines = -1;
        boolean breakAfterSkip = false;
        if (!noDiffs) {
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
        }

        for (int i = 0; i < count; i++) {
            if (!noDiffs) {
                if (skipStartAt != -1 && skipStartAt == i) {
                    lineNumberA += skippedLines;
                    lineNumberB += skippedLines;
                    i += skippedLines;
                    DiffView.SkipLineModel skip = new DiffView.SkipLineModel();
                    skip.msg = mContext.getResources().getQuantityString(
                            R.plurals.skipped_lines, skippedLines, skippedLines);
                    model.add(skip);
                    if (breakAfterSkip) {
                        break;
                    }
                }
            }

            String line = diff.ab[i];
            DiffInfoModel m = new DiffInfoModel();
            m.lineNumberA = String.valueOf(++lineNumberA);
            m.lineNumberB = String.valueOf(++lineNumberB);
            m.lineA = prepareTabs(line);
            m.lineB = prepareTabs(line);
            m.colorA = noColor;
            m.colorB = noColor;
            processHighlights(m);
            model.add(m);
        }

        return new int[]{lineNumberA, lineNumberB};
    }

    private void processNoIntralineDataA(
            DiffContentInfo diff, DiffInfoModel m, String line, int color) {
        final Spannable.Factory spannableFactory = Spannable.Factory.getInstance();
        if (diff.a != null && diff.b != null
                && diff.a.length == 1 && diff.b.length == 1) {
            Spannable span = spannableFactory.newSpannable(prepareTabs(line));
            int z = diff.a[0].indexOf(diff.b[0]);
            if (z != -1) {
                if (z > 0) {
                    span.setSpan(new BackgroundColorSpan(color),
                            0, z, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                if (z + diff.b[0].length() < diff.a[0].length()) {
                    z = z + diff.b[0].length();
                    span.setSpan(new BackgroundColorSpan(color),
                            z, diff.a[0].length(),
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
            m.lineA = span;
        } else {
            m.lineA = prepareTabs(line);
        }
    }

    private void processNoIntralineDataB(
            DiffContentInfo diff, DiffInfoModel m, String line, int color) {
        final Spannable.Factory spannableFactory = Spannable.Factory.getInstance();
        if (diff.a != null && diff.b != null
                && diff.a.length == 1 && diff.b.length == 1) {
            Spannable span = spannableFactory.newSpannable(prepareTabs(line));
            int z = diff.b[0].indexOf(diff.a[0]);
            if (z != -1) {
                if (z > 0) {
                    span.setSpan(new BackgroundColorSpan(color),
                            0, z, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                if (z + diff.a[0].length() < diff.b[0].length()) {
                    z = z + diff.a[0].length();
                    span.setSpan(new BackgroundColorSpan(color),
                            z, diff.b[0].length(),
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
            m.lineB = span;
        } else {
            m.lineB = prepareTabs(line);
        }
    }

    private void processHighlights(DiffInfoModel model) {
        if (model.lineA != null) {
            model.lineA = processHighlightTrailingSpaces(processHighlightTabs(model.lineA));
        }
        if (model.lineB != null) {
            model.lineB = processHighlightTrailingSpaces(processHighlightTabs(model.lineB));
        }
    }

    private CharSequence processHighlightTabs(CharSequence text) {
        if (!mHighlightTabs || !text.toString().contains(StringHelper.NON_PRINTABLE_CHAR)) {
            return text;
        }

        int color = ContextCompat.getColor(mContext, R.color.diffHighlightColor);
        SpannableStringBuilder ssb = new SpannableStringBuilder(text);
        String line = text.toString();
        int index = line.length();
        while ((index = line.lastIndexOf(StringHelper.NON_PRINTABLE_CHAR, index)) != -1) {
            ssb.replace(index, index + 1, "\u00BB    ");
            ssb.setSpan(new ForegroundColorSpan(color),
                    index, index + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            ssb.setSpan(new StyleSpan(Typeface.BOLD),
                    index, index + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            index--;
        }
        return ssb;
    }

    private CharSequence processHighlightTrailingSpaces(CharSequence text) {
        if (!mHighlightTrailingWhitespaces) {
            return text;
        }

        int color = ContextCompat.getColor(mContext, R.color.diffHighlightColor);
        final Spannable.Factory spannableFactory = Spannable.Factory.getInstance();
        String line = text.toString();
        final Matcher matcher = HIGHLIGHT_TRAIL_SPACES_PATTERN.matcher(line);
        if (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            Spannable span = spannableFactory.newSpannable(text);
            span.setSpan(new BackgroundColorSpan(color),
                    start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            return span;
        }
        return text;
    }

    private String prepareTabs(String line) {
        return line.replaceAll("\t", StringHelper.NON_PRINTABLE_CHAR);
    }

    private List<DiffView.AbstractModel> processComments(List<DiffView.AbstractModel> model) {
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

    private List<DiffView.AbstractModel> processDrafts(List<DiffView.AbstractModel> model) {
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

    private void addCommentsToModel(List<DiffView.AbstractModel> model,
            List<CommentInfo> comments, boolean isA, boolean isDraft) {
        if (comments == null) {
            return;
        }
        int count = comments.size();
        for (int i = 0; i < count; i++) {
            CommentInfo comment = comments.get(i);
            if (comment.line == null && comment.range == null) {
                // File comment
                DiffView.CommentModel commentModel = new DiffView.CommentModel();
                commentModel.diff = null;
                commentModel.isDraft = isDraft;
                if (mMode == DiffView.UNIFIED_MODE) {
                    commentModel.commentA = comment;
                } else {
                    if (isA) {
                        commentModel.commentA = comment;
                    } else {
                        commentModel.commentB = comment;
                    }
                }
                model.add(0, commentModel);
                continue;
            }

            int pos = findLineInModel(model, isA, comment.line);
            if (pos != -1) {
                if (mMode == DiffView.UNIFIED_MODE) {
                    DiffInfoModel diff = (DiffInfoModel) model.get(findDiffForComment(model, pos));
                    DiffView.CommentModel commentModel = new DiffView.CommentModel();
                    commentModel.diff = diff;
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
                        DiffInfoModel diff = (DiffInfoModel) model.get(
                                findDiffForComment(model, reusablePos));
                        DiffView.CommentModel commentModel =
                                (DiffView.CommentModel) model.get(reusablePos);
                        commentModel.diff = diff;
                        commentModel.isDraft = isDraft;
                        if (isA) {
                            commentModel.commentA = comment;
                        } else {
                            commentModel.commentB = comment;
                        }
                    } else {
                        DiffInfoModel diff = (DiffInfoModel) model.get(
                                findDiffForComment(model, pos));
                        DiffView.CommentModel commentModel = new DiffView.CommentModel();
                        commentModel.diff = diff;
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

    private int findLineInModel(List<DiffView.AbstractModel> model, boolean isA, int line) {
        int count = model.size();
        for (int i = 0; i < count; i++) {
            DiffView.AbstractModel m = model.get(i);
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

    private int findDiffForComment(List<DiffView.AbstractModel> model, int pos) {
        for (int i = pos; i >= 0; i--) {
            if (model.get(i) instanceof DiffInfoModel) {
                return i;
            }
        }
        return -1;
    }

    private int findReusableCommentView(List<DiffView.AbstractModel> model, int pos, boolean isA) {
        int count = model.size();
        for (int i = pos + 1; i < count; i++) {
            DiffView.AbstractModel m = model.get(i);
            if (!(m instanceof DiffView.CommentModel)) {
                break;
            }

            DiffView.CommentModel comment = (DiffView.CommentModel) m;
            if ((isA && comment.commentA == null && comment.commentB != null)
                    || (!isA && comment.commentB == null && comment.commentA != null)) {
                return i;
            }
        }
        return -1;
    }

    private int findNextPositionWithoutComment(List<DiffView.AbstractModel> model, int pos) {
        int count = model.size();
        for (int i = pos + 1; i < count; i++) {
            DiffView.AbstractModel m = model.get(i);
            if (!(m instanceof DiffView.CommentModel)) {
                return i;
            }
        }
        return -1;
    }
}
