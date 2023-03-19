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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.ruesga.rview.BaseActivity;
import com.ruesga.rview.R;
import com.ruesga.rview.Reloadable;
import com.ruesga.rview.gerrit.model.DashboardInfo;
import com.ruesga.rview.misc.ModelHelper;
import com.ruesga.rview.misc.SerializationManager;
import com.ruesga.rview.model.Account;
import com.ruesga.rview.preferences.Constants;
import com.ruesga.rview.preferences.Preferences;

import java.util.ArrayList;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class DashboardFragment extends PageableFragment {

    private String[] mDefaultDashboardTabs;
    private String[] mDefaultDashboardFilters;
    private String[] mDefaultDashboardReverse;

    private DashboardInfo mDashboard;
    private String[] mDashboardTabs;

    public static DashboardFragment newInstance() {
        return new DashboardFragment();
    }

    public static DashboardFragment newInstance(DashboardInfo dashboard) {
        DashboardFragment fragment = new DashboardFragment();
        Bundle arguments = new Bundle();
        arguments.putString(Constants.EXTRA_DASHBOARD,
                SerializationManager.getInstance().toJson(dashboard));
        fragment.setArguments(arguments);
        return fragment;
    }

    @SuppressWarnings("unused")
    public static DashboardFragment newFragment(ArrayList<String> args) {
        DashboardFragment fragment = new DashboardFragment();
        Bundle arguments = new Bundle();
        arguments.putString(Constants.EXTRA_DASHBOARD, args.get(0));
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(getArguments() == null ||
                !getArguments().containsKey(Constants.EXTRA_DASHBOARD));
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.dashboard_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_dashboard) {
            showDashboardChooserDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        final Account account = Preferences.getAccount(getActivity());

        if (getArguments() != null && getArguments().containsKey(Constants.EXTRA_DASHBOARD)) {
            mDashboard = SerializationManager.getInstance().fromJson(
                    getArguments().getString(Constants.EXTRA_DASHBOARD), DashboardInfo.class);
        } else {
            mDashboard = Preferences.getAccountDashboard(getActivity(), account);
        }

        // Dashboard changed between versions just sure to use the proper ones
        if (ModelHelper.isEqualsOrGreaterVersionThan(account, 3.7d)) {
            mDefaultDashboardTabs = getResources().getStringArray(R.array.dashboard_titles_3_7);
            mDefaultDashboardFilters = getResources().getStringArray(R.array.dashboard_filters_3_7);
            mDefaultDashboardReverse = getResources().getStringArray(
                    Preferences.isAccountDashboardOngoingSort(getActivity(), account)
                            ? R.array.dashboard_sort_inverse_3_7
                            : R.array.dashboard_sort_3_7);
        } else if (ModelHelper.isEqualsOrGreaterVersionThan(account, 3.0d)) {
            mDefaultDashboardTabs = getResources().getStringArray(R.array.dashboard_titles_3_0);
            mDefaultDashboardFilters = getResources().getStringArray(R.array.dashboard_filters_3_0);
            mDefaultDashboardReverse = getResources().getStringArray(
                    Preferences.isAccountDashboardOngoingSort(getActivity(), account)
                            ? R.array.dashboard_sort_inverse_3_0
                            : R.array.dashboard_sort_3_0);
        } else if (ModelHelper.isEqualsOrGreaterVersionThan(account, 2.15d)) {
            mDefaultDashboardTabs = getResources().getStringArray(R.array.dashboard_titles_2_15);
            mDefaultDashboardFilters = getResources().getStringArray(R.array.dashboard_filters_2_15);
            mDefaultDashboardReverse = getResources().getStringArray(
                    Preferences.isAccountDashboardOngoingSort(getActivity(), account)
                            ? R.array.dashboard_sort_inverse_2_15
                            : R.array.dashboard_sort_2_15);
        } else if (ModelHelper.isEqualsOrGreaterVersionThan(account, 2.14d)) {
            mDefaultDashboardFilters = getResources().getStringArray(R.array.dashboard_filters_2_14);
        }

        if (mDefaultDashboardTabs == null) {
            mDefaultDashboardTabs = getResources().getStringArray(R.array.dashboard_titles);
        }
        if (mDefaultDashboardFilters == null) {
            mDefaultDashboardFilters = getResources().getStringArray(R.array.dashboard_filters);
        }
        if (mDefaultDashboardReverse == null) {
            mDefaultDashboardReverse = getResources().getStringArray(
                    Preferences.isAccountDashboardOngoingSort(getActivity(), account)
                            ? R.array.dashboard_sort_inverse
                            : R.array.dashboard_sort);
        }

        onDashboardSelected(mDashboard);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public String[] getPages() {
        return mDashboardTabs;
    }

    @Override
    public Fragment getFragment(int position) {
        if (isDefaultDashboard()) {
            return ChangeListByFilterFragment.newInstance(mDefaultDashboardFilters[position],
                    Boolean.parseBoolean(mDefaultDashboardReverse[position]), true, true);
        }
        return ChangeListByFilterFragment.newInstance(mDashboard.sections[position].query,
                false, true, true);
    }

    @Override
    public boolean isSwipeable() {
        final boolean isTwoPane = getResources().getBoolean(R.bool.config_is_two_pane);
        return !isTwoPane;
    }

    @Override
    public int getOffscreenPageLimit() {
        if (isDefaultDashboard()) {
            return mDefaultDashboardTabs.length + 1;
        }
        return mDashboard.sections.length + 1;
    }

    @SuppressWarnings("ConstantConditions")
    public void onDashboardChooserDialogDismissed(DashboardInfo dashboard) {
        mDashboard = dashboard;
        ((Reloadable) getActivity()).performReload();
        onDashboardSelected(mDashboard);
    }

    public void showDashboardChooserDialog() {
        DashboardChooserDialogFragment fragment = DashboardChooserDialogFragment.newInstance();
        fragment.show(getChildFragmentManager(), DashboardChooserDialogFragment.TAG);
    }

    private boolean isDefaultDashboard() {
        return mDashboard == null || Constants.DASHBOARD_DEFAULT_ID.equals(mDashboard.id);
    }

    @SuppressWarnings("ConstantConditions")
    public void onDashboardSelected(DashboardInfo dashboard) {
        ((BaseActivity) getActivity()).setSafeActionBarTitle(
                getActivity().getString(R.string.menu_dashboard),
                dashboard.title);

        // Cache arrays
        if (isDefaultDashboard()) {
            mDashboardTabs = mDefaultDashboardTabs;
        } else {
            int count = mDashboard.sections.length;
            String[] tabs = new String[count];
            for (int i = 0; i < count; i++) {
                tabs[i] = mDashboard.sections[i].name;
            }
            mDashboardTabs = tabs;
        }
    }
}
