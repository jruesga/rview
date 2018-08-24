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
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import com.ruesga.rview.R;
import com.ruesga.rview.databinding.CommitItemBinding;
import com.ruesga.rview.fragments.ChangeDetailsFragment;
import com.ruesga.rview.gerrit.model.CommitInfo;

import java.util.ArrayList;
import java.util.List;

import androidx.databinding.DataBindingUtil;

public class ParentCommitsView extends LinearLayout {
    private final List<CommitItemBinding> mBindings = new ArrayList<>();
    private ChangeDetailsFragment.EventHandlers mEventHandlers;

    public ParentCommitsView(Context context) {
        this(context, null);
    }

    public ParentCommitsView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ParentCommitsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOrientation(VERTICAL);
    }

    public ParentCommitsView with(ChangeDetailsFragment.EventHandlers handlers) {
        mEventHandlers = handlers;
        return this;
    }

    public ParentCommitsView from(CommitInfo commit) {
        LayoutInflater layoutInflater = LayoutInflater.from(getContext());

        int count = commit.parents.length;
        int children = getChildCount();
        if (count > children) {
            for (int i = children; i < count; i++) {
                CommitItemBinding binding = DataBindingUtil.inflate(
                        layoutInflater, R.layout.commit_item, this, false);
                addView(binding.getRoot());
                mBindings.add(binding);
            }
        }
        for (int i = 0; i < count; i++) {
            CommitItemBinding binding = mBindings.get(i);
            binding.setRevision(commit.parents[i].commit);
            binding.setCommit(commit.parents[i]);
            binding.setHandlers(mEventHandlers);
            binding.getRoot().setVisibility(View.VISIBLE);
        }
        for (int i = count; i < children; i++) {
            CommitItemBinding binding = mBindings.get(i);
            binding.getRoot().setVisibility(View.GONE);
        }

        return this;
    }
}
