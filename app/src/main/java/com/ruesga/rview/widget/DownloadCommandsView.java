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
import com.ruesga.rview.databinding.DownloadCommandItemBinding;
import com.ruesga.rview.fragments.DownloadDialogFragment;
import com.ruesga.rview.gerrit.model.FetchInfo;

import java.util.ArrayList;
import java.util.List;

import androidx.databinding.DataBindingUtil;

public class DownloadCommandsView extends LinearLayout {
    private final List<DownloadCommandItemBinding> mBindings = new ArrayList<>();
    private DownloadDialogFragment.EventHandlers mHandlers;

    private FetchInfo mFetchInfo;

    public DownloadCommandsView(Context context) {
        this(context, null);
    }

    public DownloadCommandsView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DownloadCommandsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOrientation(VERTICAL);
    }

    public DownloadCommandsView from(FetchInfo info) {
        mFetchInfo = info;
        return this;
    }

    public DownloadCommandsView with(DownloadDialogFragment.EventHandlers handlers) {
        mHandlers = handlers;
        return this;
    }

    public void update() {
        LayoutInflater layoutInflater = LayoutInflater.from(getContext());

        int count = mFetchInfo.commands.size();
        String[] types = mFetchInfo.commands.keySet().toArray(new String[count]);
        int children = getChildCount();
        if (count > children) {
            for (int i = children; i < count; i++) {
                DownloadCommandItemBinding binding = DataBindingUtil.inflate(
                        layoutInflater, R.layout.download_command_item, this, false);
                addView(binding.getRoot());
                mBindings.add(binding);
            }
        }
        for (int i = 0; i < count; i++) {
            DownloadCommandItemBinding binding = mBindings.get(i);
            binding.setType(types[i]);
            binding.setCommand(mFetchInfo.commands.get(types[i]));
            binding.setHandlers(mHandlers);
            binding.getRoot().setVisibility(View.VISIBLE);
        }
        for (int i = count; i < children; i++) {
            DownloadCommandItemBinding binding = mBindings.get(i);
            binding.getRoot().setVisibility(View.GONE);
        }
    }
}
