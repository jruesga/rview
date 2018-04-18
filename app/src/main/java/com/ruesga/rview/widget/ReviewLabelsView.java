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
import android.support.v7.widget.LinearLayoutCompat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import com.ruesga.rview.R;
import com.ruesga.rview.databinding.ReviewLabelItemBinding;
import com.ruesga.rview.gerrit.model.ApprovalInfo;
import com.ruesga.rview.gerrit.model.ChangeInfo;
import com.ruesga.rview.gerrit.model.ChangeStatus;
import com.ruesga.rview.misc.ModelHelper;
import com.ruesga.rview.model.Account;
import com.ruesga.rview.preferences.Preferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

public class ReviewLabelsView extends LinearLayout {

    public interface OnReviewChanged {
        void onReviewChanged(String label, int score);
    }

    private final List<ReviewLabelItemBinding> mLabelBindings = new ArrayList<>();
    private final List<ScoresView> mScoresViews = new ArrayList<>();
    private final List<String> mLabels = new ArrayList<>();

    private LinearLayout mLabelsLayout;
    private LinearLayout mScoresLayout;

    private final Object mLock = new Object();

    private ScoresView.OnScoreChanged mScoreChangedListener = new ScoresView.OnScoreChanged() {
        @Override
        public void onScoreChanged(ScoresView view, int newScore) {
            if (mCallback != null) {
                String label = mLabels.get(mScoresViews.indexOf(view));
                mCallback.onReviewChanged(label, newScore);
            }
        }
    };

    private final Account mAccount;
    private OnReviewChanged mCallback;

    public ReviewLabelsView(Context context) {
        this(context, null);
    }

    public ReviewLabelsView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ReviewLabelsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOrientation(HORIZONTAL);

        mAccount = Preferences.getAccount(context);

        // Separate labels and scores
        mLabelsLayout = new LinearLayout(context);
        mLabelsLayout.setOrientation(LinearLayout.VERTICAL);
        addView(mLabelsLayout, new LinearLayoutCompat.LayoutParams(WRAP_CONTENT, WRAP_CONTENT));
        mScoresLayout = new LinearLayout(context);
        mScoresLayout.setOrientation(LinearLayout.VERTICAL);
        addView(mScoresLayout, new LinearLayoutCompat.LayoutParams(WRAP_CONTENT, WRAP_CONTENT));
    }

    public ReviewLabelsView from(ChangeInfo change, Map<String, Integer> review) {
        synchronized (mLock) {
            mLabels.clear();
            mLabels.addAll(ModelHelper.sortPermittedLabels(change.permittedLabels));
            updateLabels();
            updateScores(change, review);
        }
        return this;
    }

    public ReviewLabelsView listenTo(OnReviewChanged cb) {
        mCallback = cb;
        return this;
    }

    public Map<String, Integer> getReview(boolean onlyReviewed) {
        synchronized (mLock) {
            int children = Math.min(mScoresLayout.getChildCount(), mLabels.size());
            Map<String, Integer> review = new LinkedHashMap<>(children);
            for (int i = 0; i < children; i++) {
                ScoresView view = mScoresViews.get(i);
                String label = mLabels.get(i);
                int score = view.getValue();
                if (!onlyReviewed || score != 0) {
                    review.put(label, score);
                }
            }
            return review;
        }
    }

    private void updateLabels() {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        int count = mLabels.size();
        int children = mLabelsLayout.getChildCount();
        if (count > children) {
            for (int i = children; i < count; i++) {
                ReviewLabelItemBinding binding = DataBindingUtil.inflate(
                        inflater, R.layout.review_label_item, this, false);
                mLabelsLayout.addView(binding.getRoot());
                mLabelBindings.add(binding);
            }
        }
        for (int i = 0; i < count; i++) {
            ReviewLabelItemBinding binding = mLabelBindings.get(i);
            binding.setLabel(mLabels.get(i));
            binding.getRoot().setVisibility(View.VISIBLE);
        }
        for (int i = count; i < children; i++) {
            ReviewLabelItemBinding binding = mLabelBindings.get(i);
            binding.getRoot().setVisibility(View.GONE);
        }
    }

    private void updateScores(ChangeInfo change, Map<String, Integer> savedReview) {
        List<Integer> allScores = computeAllScores(change);

        int count = mLabels.size();
        int children = mScoresLayout.getChildCount();
        if (count > children) {
            for (int i = children; i < count; i++) {
                ScoresView view = new ScoresView(getContext());
                mScoresLayout.addView(view);
                mScoresViews.add(view);
            }
        }
        for (int i = 0; i < count; i++) {
            String label = mLabels.get(i);
            ScoresView view = mScoresViews.get(i);
            Integer[] scores = change.permittedLabels.get(label);
            if (scores == null) {
                scores = new Integer[0];
            }

            Integer value;
            if (savedReview != null) {
                value = savedReview.get(label);
            } else {
                value = computeValue(change, label);
            }

            view.withAllValues(allScores)
                .withPermittedValues(Arrays.asList(scores))
                .withValue(value)
                .listenTo(mScoreChangedListener)
                .update();

            if (ChangeStatus.MERGED.equals(change.status) && scores.length <= 1) {
                mLabelsLayout.getChildAt(i).setVisibility(View.GONE);
                view.setVisibility(View.GONE);
            } else {
                view.setVisibility(View.VISIBLE);
            }
        }
        for (int i = count; i < children; i++) {
            ScoresView view = mScoresViews.get(i);
            view.setVisibility(View.GONE);
        }
    }

    private List<Integer> computeAllScores(ChangeInfo change) {
        List<Integer> allScores = new ArrayList<>();
        if (change.permittedLabels != null) {
            for (String label : change.permittedLabels.keySet()) {
                Integer[] scores = change.permittedLabels.get(label);
                if (scores != null) {
                    for (Integer score : scores) {
                        if (!allScores.contains(score)) {
                            allScores.add(score);
                        }
                    }
                }
            }
            Collections.sort(allScores);
        }
        return allScores;
    }

    private Integer computeValue(ChangeInfo change, String label) {
        if (mAccount == null) {
            return null;
        }

        ApprovalInfo[] approvals = change.labels.get(label).all;
        if (approvals == null || approvals.length == 0) {
            return null;
        }
        for (ApprovalInfo approval : approvals) {
            if (approval.owner.accountId == mAccount.mAccount.accountId) {
                return approval.value;
            }
        }

        return null;
    }

}
