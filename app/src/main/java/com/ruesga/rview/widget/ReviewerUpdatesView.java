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
import com.ruesga.rview.databinding.ReviewerUpdatesItemBinding;
import com.ruesga.rview.gerrit.model.AccountInfo;
import com.ruesga.rview.gerrit.model.ReviewerStatus;
import com.ruesga.rview.gerrit.model.ReviewerUpdateInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static com.ruesga.rview.widget.AccountChipView.OnAccountChipClickedListener;

public class ReviewerUpdatesView extends LinearLayout {
    private List<ReviewerUpdatesItemBinding> mBindings = new ArrayList<>();
    private OnAccountChipClickedListener mOnAccountChipClickedListener;

    public ReviewerUpdatesView(Context context) {
        this(context, null);
    }

    public ReviewerUpdatesView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ReviewerUpdatesView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOrientation(VERTICAL);
    }

    public ReviewerUpdatesView listenOn(OnAccountChipClickedListener cb) {
        mOnAccountChipClickedListener = cb;
        return this;
    }

    public ReviewerUpdatesView from(List<ReviewerUpdateInfo> reviewerUpdates) {
        LayoutInflater layoutInflater = LayoutInflater.from(getContext());
        Map<ReviewerStatus, List<AccountInfo>> data = getReviewerUpdatesData(reviewerUpdates);

        int count = data.size();
        int children = getChildCount();
        if (count > children) {
            for (int i = children; i < count; i++) {
                ReviewerUpdatesItemBinding binding = DataBindingUtil.inflate(
                        layoutInflater, R.layout.reviewer_updates_item, this, false);
                addView(binding.getRoot());
                mBindings.add(binding);
            }
        }
        int i = 0;
        for (Map.Entry<ReviewerStatus, List<AccountInfo>> entry : data.entrySet()) {
            ReviewerUpdatesItemBinding binding = mBindings.get(i);
            final String label = getResources().getStringArray(
                    R.array.reviewer_update_states)[entry.getKey().ordinal()];
            binding.setLabel(label);
            binding.reviewers
                    .withRemovableReviewers(false)
                    .listenOn(mOnAccountChipClickedListener)
                    .withTag(label)
                    .from(entry.getValue(), new AccountInfo[]{});
            binding.getRoot().setVisibility(View.VISIBLE);
            i++;
        }
        for (i = count; i < children; i++) {
            ReviewerUpdatesItemBinding binding = mBindings.get(i);
            binding.getRoot().setVisibility(View.GONE);
        }

        return this;
    }

    private Map<ReviewerStatus, List<AccountInfo>> getReviewerUpdatesData(
            List<ReviewerUpdateInfo> reviewerUpdates) {
        Map<ReviewerStatus, List<AccountInfo>> data = new TreeMap<>();
        for (ReviewerUpdateInfo ru : reviewerUpdates) {
            if (!data.containsKey(ru._extendedState)) {
                data.put(ru._extendedState, new ArrayList<>());
            }
            data.get(ru._extendedState).add(ru.reviewer);
        }
        return data;
    }
}
