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
import com.ruesga.rview.gerrit.filter.ChangeQuery;
import com.ruesga.rview.preferences.Constants;

import java.util.ArrayList;

public class StatsFragment extends PageableFragment {

    public static final int ACCOUNT_STATS = 0;
    public static final int PROJECT_STATS = 1;

    private String[] mTabs;
    private String mFilter;
    private int mType;
    private String mId;
    private String mExtra;


    @SuppressWarnings("unused")
    public static StatsFragment newFragment(ArrayList<String> args) {
        StatsFragment fragment = new StatsFragment();
        Bundle arguments = new Bundle();
        arguments.putInt(Constants.EXTRA_TYPE, Integer.valueOf(args.get(0)));
        arguments.putString(Constants.EXTRA_ID, args.get(1));
        arguments.putString(Constants.EXTRA_FILTER, args.get(2));
        arguments.putString(Constants.EXTRA_FRAGMENT_EXTRA, args.get(3));
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        mType = getArguments().getInt(Constants.EXTRA_TYPE);
        mId = getArguments().getString(Constants.EXTRA_ID);
        mExtra = getArguments().getString(Constants.EXTRA_FRAGMENT_EXTRA);
        String filter = getArguments().getString(Constants.EXTRA_FILTER);
        mFilter = ChangeQuery.parse(filter).toString();

        mTabs = new String[]{
                getString(R.string.stats_info_title),
                getString(R.string.stats_changes_title)};

        ((BaseActivity) getActivity()).setUseTwoPanel(false);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public String[] getPages() {
        return mTabs;
    }

    @Override
    public Fragment getFragment(int position) {
        if (position == 0) {
            // Stats page
            switch (mType) {
                case ACCOUNT_STATS:
                    return AccountStatsPageFragment.newFragment(mId, mExtra);
                case PROJECT_STATS:
                    return ProjectStatsPageFragment.newFragment(mId);
            }

            return null;
        }

        // Changes page
        return ChangeListByFilterFragment.newInstance(mFilter);
    }

    @Override
    public boolean isSwipeable() {
        final boolean isTwoPane = getResources().getBoolean(R.bool.config_is_two_pane);
        return !isTwoPane;
    }

    @Override
    public int getOffscreenPageLimit() {
        return 2;
    }
}
