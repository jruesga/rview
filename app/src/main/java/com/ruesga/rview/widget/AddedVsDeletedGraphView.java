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
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.ruesga.rview.R;
import com.ruesga.rview.databinding.AddedVsDeletedBinding;
import com.ruesga.rview.fragments.ChangeDetailsFragment;

public class AddedVsDeletedGraphView extends FrameLayout {
    private AddedVsDeletedBinding mBinding;

    public AddedVsDeletedGraphView(Context context) {
        this(context, null);
    }

    public AddedVsDeletedGraphView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AddedVsDeletedGraphView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        LayoutInflater layoutInflater = LayoutInflater.from(getContext());
        mBinding = DataBindingUtil.inflate(
                layoutInflater, R.layout.added_vs_deleted, this, false);
        addView(mBinding.getRoot());
    }

    public AddedVsDeletedGraphView with(ChangeDetailsFragment.FileItemModel item) {
        float total = item.totalAdded + item.totalDeleted;
        float added = 0f;
        if (item.info.linesInserted != null) {
            added = Math.max(0.05f, item.info.linesInserted / total);
        }
        float deleted = 0f;
        if (item.info.linesDeleted != null) {
            deleted = Math.max(0.05f, item.info.linesDeleted / total);
        }
        ((LinearLayout.LayoutParams) mBinding.added.getLayoutParams()).weight = added;
        ((LinearLayout.LayoutParams) mBinding.deleted.getLayoutParams()).weight = deleted;
        ((LinearLayout.LayoutParams) mBinding.spacer.getLayoutParams()).weight = 1 - added - deleted;
        mBinding.graph.requestLayout();
        mBinding.setModel(item.info);
        return this;
    }
}
