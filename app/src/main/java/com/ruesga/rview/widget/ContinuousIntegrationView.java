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
import com.ruesga.rview.databinding.CiItemBinding;
import com.ruesga.rview.misc.AndroidHelper;
import com.ruesga.rview.misc.BitmapUtils;
import com.ruesga.rview.model.ContinuousIntegrationInfo;

import java.util.ArrayList;
import java.util.List;

public class ContinuousIntegrationView extends FlexboxLayout {

    @Keep
    @SuppressWarnings("unused")
    public static class EventHandlers {
        private final ContinuousIntegrationView mView;

        public EventHandlers(ContinuousIntegrationView view) {
            mView = view;
        }

        public void onContinuousIntegrationPressed(View v) {
            ContinuousIntegrationInfo ci = (ContinuousIntegrationInfo) v.getTag();
            mView.dispatchOnContinuousIntegrationPressed(ci);
        }
    }

    public interface OnContinuousIntegrationPressed {
        void onContinuousIntegrationPressed(ContinuousIntegrationInfo ci);
    }

    private List<CiItemBinding> mBindings = new ArrayList<>();
    private OnContinuousIntegrationPressed mOnContinuousIntegrationPressed;
    private final EventHandlers mEventHandlers;

    private final LayoutInflater mInflater;
    private final ColorStateList mSuccessColor, mFailureColor, mSkippedColor, mRunningColor;

    public ContinuousIntegrationView(Context context) {
        this(context, null);
    }

    public ContinuousIntegrationView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ContinuousIntegrationView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mEventHandlers = new EventHandlers(this);
        mInflater = LayoutInflater.from(context);

        mSuccessColor = ContextCompat.getColorStateList(getContext(), R.color.success);
        mFailureColor = ContextCompat.getColorStateList(getContext(), R.color.failure);
        mSkippedColor = ContextCompat.getColorStateList(getContext(), R.color.skipped);
        mRunningColor = ContextCompat.getColorStateList(getContext(), R.color.running);
    }

    public ContinuousIntegrationView from(List<ContinuousIntegrationInfo> ci) {
        int count = ci == null ? 0 : ci.size();
        int children = getChildCount();
        if (count > children) {
            for (int i = children; i < count; i++) {
                CiItemBinding binding = DataBindingUtil.inflate(
                        mInflater, R.layout.ci_item, this, false);
                addView(binding.getRoot());
                mBindings.add(binding);
            }
        }
        for (int i = 0; i < count; i++) {
            final ContinuousIntegrationInfo info = ci.get(i);
            CiItemBinding binding = mBindings.get(i);
            binding.setModel(info);
            setChipBackgroundColor(binding, info);
            binding.setHandlers(mEventHandlers);
            binding.getRoot().setVisibility(View.VISIBLE);
        }
        for (int i = count; i < children; i++) {
            CiItemBinding binding = mBindings.get(i);
            binding.getRoot().setVisibility(View.GONE);
        }

        return this;
    }

    public ContinuousIntegrationView listenOn(OnContinuousIntegrationPressed cb) {
        mOnContinuousIntegrationPressed = cb;
        return this;
    }

    private void setChipBackgroundColor(CiItemBinding binding, ContinuousIntegrationInfo ci) {
        final ColorStateList color;
        switch (ci.mStatus) {
            case SUCCESS:
                color = mSuccessColor;
                break;
            case FAILURE:
                color = mFailureColor;
                break;
            case SKIPPED:
                color = mSkippedColor;
                break;
            case RUNNING:
            default:
                color = mRunningColor;
                break;
        }

        if (!AndroidHelper.isLollipopOrGreater()) {
            Drawable dw = ContextCompat.getDrawable(getContext(), R.drawable.bg_tag);
            binding.ciLayout.setBackground(BitmapUtils.tintDrawable(
                    getResources(), dw, color.getDefaultColor()));
        } else {
            ViewCompat.setBackgroundTintList(binding.ciLayout, color);
        }
    }

    private void dispatchOnContinuousIntegrationPressed(ContinuousIntegrationInfo ci) {
        if (mOnContinuousIntegrationPressed != null) {
            mOnContinuousIntegrationPressed.onContinuousIntegrationPressed(ci);
        }
    }
}
