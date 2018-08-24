/*
 * Copyright (C) 2017 Jorge Ruesga
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
import com.ruesga.rview.databinding.DashboardItemBinding;
import com.ruesga.rview.gerrit.model.DashboardInfo;
import com.ruesga.rview.gerrit.model.ProjectInfo;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Keep;
import androidx.databinding.DataBindingUtil;

public class DashboardsView extends LinearLayout {
    private List<DashboardItemBinding> mBindings = new ArrayList<>();
    private OnDashboardClickedListener mOnDashboardClickedListener;

    @Keep
    @SuppressWarnings("unused")
    public static class ItemEventHandlers {
        DashboardsView mView;

        ItemEventHandlers(DashboardsView view) {
            mView = view;
        }

        public void onItemPressed(View view) {
            mView.performDashboardClick((DashboardInfo) view.getTag());
        }
    }

    public interface OnDashboardClickedListener {
        void onDashboardClicked(DashboardInfo dashboard);
    }

    public DashboardsView(Context context) {
        this(context, null);
    }

    public DashboardsView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DashboardsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOrientation(VERTICAL);
    }

    public DashboardsView listenOn(OnDashboardClickedListener cb) {
        mOnDashboardClickedListener = cb;
        return this;
    }

    public DashboardsView from(ProjectInfo project, List<DashboardInfo> dashboards) {
        LayoutInflater layoutInflater = LayoutInflater.from(getContext());

        int count = dashboards.size();
        int children = getChildCount();
        if (count > children) {
            for (int i = children; i < count; i++) {
                DashboardItemBinding binding = DataBindingUtil.inflate(
                        layoutInflater, R.layout.dashboard_item, this, false);
                binding.setHandlers(new ItemEventHandlers(this));
                addView(binding.getRoot());
                mBindings.add(binding);
            }
        }
        for (int i = 0; i < count; i++) {
            DashboardItemBinding binding = mBindings.get(i);
            binding.setProject(project);
            binding.setDashboard(dashboards.get(i));
            binding.getRoot().setVisibility(View.VISIBLE);
        }
        for (int i = count; i < children; i++) {
            DashboardItemBinding binding = mBindings.get(i);
            binding.getRoot().setVisibility(View.GONE);
        }

        return this;
    }

    public void performDashboardClick(DashboardInfo dashboard) {
        if (mOnDashboardClickedListener != null) {
            mOnDashboardClickedListener.onDashboardClicked(dashboard);
        }
    }
}
