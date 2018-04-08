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
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.databinding.DataBindingUtil;
import android.graphics.drawable.Drawable;
import android.support.annotation.Keep;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;

import com.google.android.flexbox.FlexboxLayout;
import com.ruesga.rview.R;
import com.ruesga.rview.databinding.ScoreItemBinding;
import com.ruesga.rview.gerrit.model.LabelInfo;
import com.ruesga.rview.misc.AndroidHelper;
import com.ruesga.rview.misc.BitmapUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScoreLabelsView extends FlexboxLayout {
    @Keep
    public static class Model {
        public boolean visible = false;
        public String label;
        public String score;
        public ColorStateList color;
    }

    private static final Pattern sShortLabelPattern = Pattern.compile("[A-Z]+|-[a-z]");
    private static final Map<String, String> sShortLabelCache = new HashMap<>();

    private final Map<String, Model> mScores = new TreeMap<>();
    private final List<ScoreItemBinding> mBindings = new ArrayList<>();
    private final LayoutInflater mInflater;
    private boolean mIsShortLabels;

    private final ColorStateList mApprovedColor, mRejectedColor, mNoScoreColor;

    public ScoreLabelsView(Context context) {
        this(context, null);
    }

    public ScoreLabelsView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScoreLabelsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mInflater = LayoutInflater.from(context);

        mApprovedColor = ContextCompat.getColorStateList(getContext(), R.color.approved);
        mRejectedColor = ContextCompat.getColorStateList(getContext(), R.color.rejected);
        mNoScoreColor = ContextCompat.getColorStateList(getContext(), R.color.noscore);

        Resources.Theme theme = context.getTheme();
        TypedArray a = theme.obtainStyledAttributes(
                attrs, R.styleable.ScoreLabelsView, defStyleAttr, 0);
        int n = a.getIndexCount();
        for (int i = 0; i < n; i++) {
            int attr = a.getIndex(i);
            switch (attr) {
                case R.styleable.ScoreLabelsView_shortLabels:
                    mIsShortLabels = a.getBoolean(attr, false);
                    break;
            }
        }
        a.recycle();
    }

    public void setScores(Map<String, LabelInfo> scores) {
        if (scores.size() == 0) {
            mScores.clear();
            setVisibility(View.GONE);
            return;
        } else {
            setVisibility(View.VISIBLE);
        }

        sortScores(scores);
        int i = 0;
        for (String label : mScores.keySet()) {
            if (i >= getChildCount()) {
                ScoreItemBinding binding = DataBindingUtil.inflate(
                        mInflater, R.layout.score_item, this, false);
                addView(binding.getRoot());
                mBindings.add(binding);
            }

            Model model = mScores.get(label);
            model.visible = true;
            ScoreItemBinding binding = mBindings.get(i);

            if (!AndroidHelper.isLollipopOrGreater()) {
                Drawable dw = ContextCompat.getDrawable(getContext(), R.drawable.bg_tag);
                binding.scoreLayout.setBackground(BitmapUtils.tintDrawable(
                        getResources(), dw, model.color.getDefaultColor()));
            } else {
                ViewCompat.setBackgroundTintList(binding.scoreLayout, model.color);
            }
            binding.setModel(model);
            i++;
        }
        while (i < getChildCount()) {
            ScoreItemBinding binding = mBindings.get(i);
            Model model = new Model();
            binding.setModel(model);
            i++;
        }
    }

    private void sortScores(Map<String, LabelInfo> scores) {
        mScores.clear();
        if (scores != null) {
            for (String label : scores.keySet()) {
                final LabelInfo info = scores.get(label);
                final Model model = new Model();
                model.label = (mIsShortLabels ? toShortLabel(label) : label) + ": ";
                if (info.blocking || info.rejected != null) {
                    model.score = "\u2717";
                    model.color = mRejectedColor;
                } else if (info.approved != null) {
                    model.score = "\u2713";
                    model.color = mApprovedColor;
                } else if (info.disliked != null) {
                    model.score = "-1";
                    model.color = mRejectedColor;
                } else if (info.recommended != null) {
                    model.score = "+1";
                    model.color = mApprovedColor;
                } else {
                    model.score = " ";
                    model.color = mNoScoreColor;
                }
                model.visible = true;
                mScores.put(model.label, model);
            }
        }
    }

    private static String toShortLabel(String label) {
        if (!sShortLabelCache.containsKey(label)) {
            Matcher matcher = sShortLabelPattern.matcher(label);
            StringBuilder sb = new StringBuilder();
            while (matcher.find()) {
                sb.append(matcher.group(0).toUpperCase().replaceAll("-", ""));
            }
            sShortLabelCache.put(label, sb.toString());
        }
        return sShortLabelCache.get(label);
    }
}
