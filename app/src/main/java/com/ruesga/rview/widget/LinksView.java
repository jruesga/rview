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
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;

import com.google.android.flexbox.FlexboxLayout;
import com.ruesga.rview.R;
import com.ruesga.rview.databinding.LinkViewBinding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LinksView extends FlexboxLayout {

    public interface OnLinkClick {
        void onLinkClick(String link);
    }

    private OnClickListener mClickListener = v ->
            onLinkClick((String) v.getTag());

    private final List<LinkViewBinding> mBindings = new ArrayList<>();
    private final LayoutInflater mInflater;
    private OnLinkClick mCallback;

    private final List<String> mLinks = new ArrayList<>();

    public LinksView(Context context) {
        this(context, null);
    }

    public LinksView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LinksView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mInflater = LayoutInflater.from(context);
    }

    public LinksView withLinks(@NonNull String[] links){
        mLinks.clear();
        mLinks.addAll(Arrays.asList(links));
        return this;
    }

    public LinksView listenTo(OnLinkClick cb) {
        mCallback = cb;
        return this;
    }

    public LinksView update() {
        int count = mLinks.size();
        int children = getChildCount();
        if (count > children) {
            for (int i = children; i < count; i++) {
                LinkViewBinding binding = DataBindingUtil.inflate(
                        mInflater, R.layout.link_view, this, false);
                addView(binding.getRoot());
                mBindings.add(binding);
            }
        }
        for (int i = 0; i < count; i++) {
            LinkViewBinding binding = mBindings.get(i);
            String link = mLinks.get(i);
            binding.setLink(link);
            binding.getRoot().setTag(link);
            binding.getRoot().setOnClickListener(mCallback == null ? null : mClickListener);
            binding.getRoot().setVisibility(View.VISIBLE);
        }
        for (int i = count; i < children; i++) {
            LinkViewBinding binding = mBindings.get(i);
            binding.getRoot().setVisibility(View.GONE);
        }

        return this;
    }

    private void onLinkClick(String link) {
        if (mCallback != null) {
            mCallback.onLinkClick(link);
        }
    }
}
