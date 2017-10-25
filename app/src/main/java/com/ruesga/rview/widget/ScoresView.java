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

import android.annotation.SuppressLint;
import android.content.Context;
import android.databinding.DataBindingUtil;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ruesga.rview.R;
import com.ruesga.rview.databinding.ReviewScoreItemBinding;

import java.util.ArrayList;
import java.util.List;

public class ScoresView extends LinearLayout {

    public interface OnScoreChanged {
        void onScoreChanged(ScoresView view, int newScore);
    }

    private final List<ReviewScoreItemBinding> mBindings = new ArrayList<>();

    private final List<Integer> mAllValues = new ArrayList<>();
    private final List<Integer> mPermittedValues = new ArrayList<>();
    private int mValue;

    private OnScoreChanged mCallback;

    private OnClickListener mClickListener = v ->
            changeScore(getSafeScore(((TextView) v).getText().toString()));

    public ScoresView(Context context) {
        this(context, null);
    }

    public ScoresView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScoresView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOrientation(LinearLayout.HORIZONTAL);
    }

    public ScoresView withAllValues(@NonNull List<Integer> values){
        mAllValues.clear();
        mAllValues.addAll(values);
        return this;
    }

    public ScoresView withPermittedValues(@NonNull List<Integer> values){
        mPermittedValues.clear();
        mPermittedValues.addAll(values);
        return this;
    }

    public ScoresView withValue(@Nullable Integer value){
        mValue = value == null ? 0 : value;
        return this;
    }

    public ScoresView listenTo(OnScoreChanged cb) {
        mCallback = cb;
        return this;
    }

    public int getValue() {
        return mValue;
    }

    @SuppressLint("RestrictedApi")
    public ScoresView update() {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        int count = mAllValues.size();
        int children = getChildCount();
        if (count > children) {
            for (int i = children; i < count; i++) {
                ReviewScoreItemBinding binding = DataBindingUtil.inflate(
                        inflater, R.layout.review_score_item, this, false);
                addView(binding.getRoot());
                mBindings.add(binding);
            }
        }
        for (int i = 0; i < count; i++) {
            ReviewScoreItemBinding binding = mBindings.get(i);
            Integer score = mAllValues.get(i);
            binding.setScore(score);
            boolean permitted = mPermittedValues.contains(score);
            binding.setPermitted(permitted);
            binding.scoreItem.setOnClickListener(permitted ? mClickListener : null);
            binding.scoreItem.setSupportBackgroundTintList(
                    ContextCompat.getColorStateList(getContext(), toBackgroundColor(score)));
            binding.getRoot().setVisibility(View.VISIBLE);
        }
        for (int i = count; i < children; i++) {
            ReviewScoreItemBinding binding = mBindings.get(i);
            binding.getRoot().setVisibility(View.GONE);
        }

        return this;
    }


    @SuppressLint("RestrictedApi")
    private void changeScore(int score) {
        mValue = score;
        int children = getChildCount();
        for (int i = 0; i < children; i++) {
            ReviewScoreItemBinding binding = mBindings.get(i);
            int s = getSafeScore(binding.scoreItem.getText().toString());
            binding.scoreItem.setSupportBackgroundTintList(
                    ContextCompat.getColorStateList(getContext(), toBackgroundColor(s)));
        }

        if (mCallback != null) {
            mCallback.onScoreChanged(this, score);
        }
    }

    private int toBackgroundColor(int score) {
        if (score == mValue) {
            return score < 0 ? R.color.rejected : score > 0 ? R.color.approved : R.color.noscore;
        }
        return R.color.unscored;
    }

    private int getSafeScore(String score) {
        // Java 1.6 doesn't recognize +1 as a valid positive number, so just
        // trim the score appropriately.
        return Integer.valueOf(score.trim().replaceAll("\\+", ""));
    }
}
