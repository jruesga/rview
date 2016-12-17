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
import com.ruesga.rview.annotations.ProguardIgnored;
import com.ruesga.rview.databinding.LineWithCommentViewBinding;
import com.ruesga.rview.gerrit.model.CommentInfo;

import java.util.ArrayList;
import java.util.List;

public class LinesWithCommentsView extends LinearLayout {
    public interface OnLineClickListener {
        void onLineClick(View v);
    }

    @ProguardIgnored
    @SuppressWarnings({"UnusedParameters", "unused"})
    public static class EventHandlers {
        private final LinesWithCommentsView mView;

        public EventHandlers(LinesWithCommentsView view) {
            mView = view;
        }

        public void onItemClicked(View v) {
            if (mView.mClickListener != null) {
                mView.mClickListener.onLineClick(v);
            }
        }
    }

    private final List<LineWithCommentViewBinding> mBindings = new ArrayList<>();
    private final EventHandlers mHandlers;
    private OnLineClickListener mClickListener;

    public LinesWithCommentsView(Context context) {
        this(context, null);
    }

    public LinesWithCommentsView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LinesWithCommentsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mHandlers = new EventHandlers(this);
    }

    public LinesWithCommentsView from(List<CommentInfo> comments) {
        setOrientation(VERTICAL);

        LayoutInflater layoutInflater = LayoutInflater.from(getContext());

        int count = comments.size();
        int children = getChildCount();
        if (count > children) {
            for (int i = children; i < count; i++) {
                LineWithCommentViewBinding binding = DataBindingUtil.inflate(
                        layoutInflater, R.layout.line_with_comment_view, this, false);
                addView(binding.getRoot());
                mBindings.add(binding);
            }
        }
        int n = 0;
        for (CommentInfo comment : comments) {
            LineWithCommentViewBinding binding = mBindings.get(n);
            binding.setModel(comment);
            if (mClickListener != null) {
                binding.setHandlers(mHandlers);
            }
            binding.getRoot().setVisibility(View.VISIBLE);
            n++;
        }
        for (int i = count; i < children; i++) {
            LineWithCommentViewBinding binding = mBindings.get(i);
            binding.getRoot().setVisibility(View.GONE);
        }

        return this;
    }

    public LinesWithCommentsView listenOn(OnLineClickListener cb) {
        mClickListener = cb;
        return this;
    }
}
