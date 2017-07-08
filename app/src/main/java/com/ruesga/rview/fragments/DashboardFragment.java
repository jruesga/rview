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

import com.ruesga.rview.BaseActivity;
import com.ruesga.rview.R;

public class DashboardFragment extends PageableFragment {

    private String[] mDashboardTabs;
    private String[] mDashboardFilters;

    public static DashboardFragment newInstance() {
        return new DashboardFragment();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        mDashboardTabs = getResources().getStringArray(R.array.dashboard_titles);
        mDashboardFilters = getResources().getStringArray(R.array.dashboard_filters);

        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public String[] getPages() {
        return mDashboardTabs;
    }

    @Override
    public Fragment getFragment(int position) {
        return ChangeListByFilterFragment.newInstance(mDashboardFilters[position], true, true);
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
