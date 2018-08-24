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
import com.ruesga.rview.databinding.FileAndCommentsViewBinding;
import com.ruesga.rview.gerrit.model.CommentInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import androidx.databinding.DataBindingUtil;

public class FilesAndCommentsView extends LinearLayout {
    private List<FileAndCommentsViewBinding> mBindings = new ArrayList<>();
    private LinesWithCommentsView.OnLineClickListener mClickListener;

    public FilesAndCommentsView(Context context) {
        this(context, null);
    }

    public FilesAndCommentsView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FilesAndCommentsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public FilesAndCommentsView from(Map<String, List<CommentInfo>> filesAndComments) {
        setOrientation(VERTICAL);

        LayoutInflater layoutInflater = LayoutInflater.from(getContext());

        int count = filesAndComments == null ? 0 : filesAndComments.size();
        int children = getChildCount();
        if (count > children) {
            for (int i = children; i < count; i++) {
                FileAndCommentsViewBinding binding = DataBindingUtil.inflate(
                        layoutInflater, R.layout.file_and_comments_view, this, false);
                addView(binding.getRoot());
                mBindings.add(binding);
            }
        }
        if (filesAndComments != null) {
            int n = 0;
            for (Map.Entry<String, List<CommentInfo>> entry : filesAndComments.entrySet()) {
                FileAndCommentsViewBinding binding = mBindings.get(n);
                String file = entry.getKey();
                binding.setFile(file);
                binding.lineComments.listenOn(mClickListener);
                binding.lineComments.from(entry.getValue());
                binding.getRoot().setVisibility(View.VISIBLE);
                n++;
            }
        }
        for (int i = count; i < children; i++) {
            FileAndCommentsViewBinding binding = mBindings.get(i);
            binding.getRoot().setVisibility(View.GONE);
        }

        return this;
    }

    public FilesAndCommentsView listenOn(LinesWithCommentsView.OnLineClickListener cb) {
        mClickListener = cb;
        return this;
    }

}
