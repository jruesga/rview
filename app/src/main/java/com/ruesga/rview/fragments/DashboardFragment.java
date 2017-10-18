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

import com.ruesga.rview.R;
import com.ruesga.rview.misc.ModelHelper;
import com.ruesga.rview.model.Account;
import com.ruesga.rview.preferences.Preferences;

public class DashboardFragment extends PageableFragment {

    private String[] mDashboardTabs;
    private String[] mDashboardFilters;
    private String[] mDashboardReverse;

    public static DashboardFragment newInstance() {
        return new DashboardFragment();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        final Account account = Preferences.getAccount(getActivity());

        // Dashboard changed between versions just sure to use the proper ones
        if (ModelHelper.isEqualsOrGreaterVersionThan(account, 2.15d)) {
            mDashboardTabs = getResources().getStringArray(R.array.dashboard_titles_2_15);
            mDashboardFilters = getResources().getStringArray(R.array.dashboard_filters_2_15);
            mDashboardReverse = getResources().getStringArray(
                    Preferences.isAccountDashboardOngoingSort(getActivity(), account)
                            ? R.array.dashboard_sort_inverse_2_15
                            : R.array.dashboard_sort_2_15);
        } else if (ModelHelper.isEqualsOrGreaterVersionThan(account, 2.14d)) {
            mDashboardFilters = getResources().getStringArray(R.array.dashboard_filters_2_14);
        }

        if (mDashboardTabs == null) {
            mDashboardTabs = getResources().getStringArray(R.array.dashboard_titles);
        }
        if (mDashboardFilters == null) {
            mDashboardFilters = getResources().getStringArray(R.array.dashboard_filters);
        }
        if (mDashboardReverse == null) {
            mDashboardReverse = getResources().getStringArray(
                    Preferences.isAccountDashboardOngoingSort(getActivity(), account)
                            ? R.array.dashboard_sort_inverse
                            : R.array.dashboard_sort);
        }

        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public String[] getPages() {
        return mDashboardTabs;
    }

    @Override
    public Fragment getFragment(int position) {
        return ChangeListByFilterFragment.newInstance(mDashboardFilters[position],
                Boolean.parseBoolean(mDashboardReverse[position]), true, true);
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
