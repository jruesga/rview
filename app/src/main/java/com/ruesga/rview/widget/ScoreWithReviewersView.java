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
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.ruesga.rview.R;
import com.ruesga.rview.databinding.ScoreWithReviewItemBinding;
import com.ruesga.rview.gerrit.model.AccountInfo;
import com.ruesga.rview.gerrit.model.ApprovalInfo;
import com.ruesga.rview.gerrit.model.LabelInfo;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ScoreWithReviewersView extends LinearLayout {
    private List<ScoreWithReviewItemBinding> mBindings = new ArrayList<>();
    private Picasso mPicasso;

    public ScoreWithReviewersView(Context context) {
        this(context, null);
    }

    public ScoreWithReviewersView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScoreWithReviewersView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public ScoreWithReviewersView with(Picasso picasso) {
        mPicasso = picasso;
        return this;
    }

    public ScoreWithReviewersView from(LabelInfo label) {
        setOrientation(VERTICAL);

        LayoutInflater layoutInflater = LayoutInflater.from(getContext());
        Map<Integer, List<AccountInfo>> scores = sortByScores(label);

        int count = scores.size();
        int children = getChildCount();
        if (count > children) {
            for (int i = children; i < count; i++) {
                ScoreWithReviewItemBinding binding = DataBindingUtil.inflate(
                        layoutInflater, R.layout.score_with_review_item, this, false);
                addView(binding.getRoot(), new LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                mBindings.add(binding);
            }
        }
        int n = 0;
        for(Map.Entry<Integer, List<AccountInfo>> entry : scores.entrySet()) {
            ScoreWithReviewItemBinding binding = mBindings.get(n);
            int value = entry.getKey();
            binding.setScore(value);
            binding.scoreItem.setSupportBackgroundTintList(
                    ContextCompat.getColorStateList(getContext(),
                        value < 0 ? R.color.rejected : R.color.approved));
            binding.reviewers.with(mPicasso).from(entry.getValue());
            binding.getRoot().setVisibility(View.VISIBLE);
            n++;
        }
        for (int i = count; i < children; i++) {
            ScoreWithReviewItemBinding binding = mBindings.get(i);
            binding.getRoot().setVisibility(View.GONE);
        }

        return this;
    }

    private Map<Integer, List<AccountInfo>> sortByScores(LabelInfo label) {
        Map<Integer, List<AccountInfo>> scores = new TreeMap<>();
        if (label.all != null) {
            for (ApprovalInfo approval : label.all) {
                if (approval.value == null || approval.value == 0) {
                    continue;
                }
                if (!scores.containsKey(approval.value)) {
                    scores.put(approval.value, new ArrayList<>());
                }
                scores.get(approval.value).add(approval.owner);
            }
        }
        return scores;
    }
}
