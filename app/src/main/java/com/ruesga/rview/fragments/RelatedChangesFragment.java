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
package com.ruesga.rview.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ruesga.rview.R;
import com.ruesga.rview.preferences.Constants;

import java.util.Locale;

public class RelatedChangesFragment extends PageableFragment {

    private static final String NULL_TOPIC = "|null|";

    private String[] mDashboardTabs;
    private String[] mDashboardFilters;

    private int mLegacyChangeId;
    private String mRevisionId;

    public static RelatedChangesFragment newInstance(int legacyChangeId, String changeId,
                String projectId, String revisionId, String topic) {
        RelatedChangesFragment fragment = new RelatedChangesFragment();
        Bundle arguments = new Bundle();
        arguments.putInt(Constants.EXTRA_LEGACY_CHANGE_ID, legacyChangeId);
        arguments.putString(Constants.EXTRA_CHANGE_ID, changeId);
        arguments.putString(Constants.EXTRA_PROJECT_ID, projectId);
        arguments.putString(Constants.EXTRA_REVISION_ID, revisionId);
        arguments.putString(Constants.EXTRA_TOPIC, TextUtils.isEmpty(topic) ? NULL_TOPIC : topic);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return createDefaultView(inflater, container);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        mDashboardTabs = getResources().getStringArray(R.array.related_changes_labels);
        mDashboardFilters = getResources().getStringArray(R.array.related_changes_filters);

        mLegacyChangeId = getArguments().getInt(Constants.EXTRA_LEGACY_CHANGE_ID);
        String changeId = getArguments().getString(Constants.EXTRA_CHANGE_ID);
        String projectId = getArguments().getString(Constants.EXTRA_PROJECT_ID);
        mRevisionId = getArguments().getString(Constants.EXTRA_REVISION_ID);
        String topic = getArguments().getString(Constants.EXTRA_PROJECT_ID);

        // Setup filters
        mDashboardFilters[1] = String.format(Locale.US, mDashboardFilters[1],
                mLegacyChangeId);
        mDashboardFilters[2] = String.format(Locale.US, mDashboardFilters[2],
                topic, mLegacyChangeId);
        mDashboardFilters[4] = String.format(Locale.US, mDashboardFilters[4],
                projectId, changeId, mLegacyChangeId);

        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public String[] getPages() {
        return mDashboardTabs;
    }

    @Override
    public int getMode() {
        return MODE_TABS;
    }

    @Override
    public Fragment getFragment(int position) {
        if (position == 0) {
            // Related changes
            return RevisionRelatedChangesFragment.newInstance(mLegacyChangeId, mRevisionId);
        }
        if (position == 3) {
            // Submitted together changes
            return SubmittedTogetherFragment.newInstance(mLegacyChangeId);
        }
        return ChangeListByFilterFragment.newInstance(mDashboardFilters[position]);
    }

    @Override
    public boolean isSwipeable() {
        final boolean isTwoPane = getResources().getBoolean(R.bool.config_is_two_pane);
        return !isTwoPane;
    }

    @Override
    public int getOffscreenPageLimit() {
        return mDashboardTabs.length + 1;
    }
}
