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
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import com.ruesga.rview.R;
import com.ruesga.rview.databinding.LabelItemBinding;
import com.ruesga.rview.gerrit.model.AccountInfo;
import com.ruesga.rview.gerrit.model.ChangeInfo;
import com.ruesga.rview.misc.ModelHelper;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import static com.ruesga.rview.widget.AccountChipView.*;

public class LabelsView extends LinearLayout {
    private List<LabelItemBinding> mBindings = new ArrayList<>();
    private Picasso mPicasso;
    private boolean mIsRemovableReviewers;
    private AccountInfo[] mRemovableReviewers;
    private OnAccountChipClickedListener mOnAccountChipClickedListener;
    private OnAccountChipRemovedListener mOnAccountChipRemovedListener;

    public LabelsView(Context context) {
        this(context, null);
    }

    public LabelsView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LabelsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOrientation(VERTICAL);
    }

    public LabelsView with(Picasso picasso) {
        mPicasso = picasso;
        return this;
    }

    public LabelsView withRemovableReviewers(boolean removable, AccountInfo[] removableReviewers) {
        mIsRemovableReviewers = removable;
        mRemovableReviewers = removableReviewers;
        return this;
    }

    public LabelsView listenOn(OnAccountChipClickedListener cb) {
        mOnAccountChipClickedListener = cb;
        return this;
    }

    public LabelsView listenOn(OnAccountChipRemovedListener cb) {
        mOnAccountChipRemovedListener = cb;
        return this;
    }

    public LabelsView from(ChangeInfo change) {
        LayoutInflater layoutInflater = LayoutInflater.from(getContext());
        List<String> labels = ModelHelper.sortLabels(change.labels);

        int count = labels.size();
        int children = getChildCount();
        if (count > children) {
            for (int i = children; i < count; i++) {
                LabelItemBinding binding = DataBindingUtil.inflate(
                        layoutInflater, R.layout.label_item, this, false);
                addView(binding.getRoot());
                mBindings.add(binding);
            }
        }
        for (int i = 0; i < count; i++) {
            LabelItemBinding binding = mBindings.get(i);
            binding.setLabel(labels.get(i));
            binding.scores
                    .with(mPicasso)
                    .withRemovableReviewers(mIsRemovableReviewers, mRemovableReviewers)
                    .listenOn(mOnAccountChipClickedListener)
                    .listenOn(mOnAccountChipRemovedListener)
                    .withTag(labels.get(i))
                    .from(change.owner, change.labels.get(labels.get(i)));
            binding.getRoot().setVisibility(View.VISIBLE);
        }
        for (int i = count; i < children; i++) {
            LabelItemBinding binding = mBindings.get(i);
            binding.getRoot().setVisibility(View.GONE);
        }

        return this;
    }
}
