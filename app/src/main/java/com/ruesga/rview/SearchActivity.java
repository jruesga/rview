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
package com.ruesga.rview;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ListPopupWindow;
import android.text.TextUtils;
import android.view.Gravity;

import com.arlib.floatingsearchview.FloatingSearchView;
import com.arlib.floatingsearchview.suggestions.model.SearchSuggestion;
import com.ruesga.rview.adapters.SimpleDropDownAdapter;
import com.ruesga.rview.databinding.SearchActivityBinding;
import com.ruesga.rview.gerrit.filter.ChangeQuery;
import com.ruesga.rview.gerrit.filter.antlr.QueryParseException;
import com.ruesga.rview.misc.ActivityHelper;
import com.ruesga.rview.misc.AndroidHelper;
import com.ruesga.rview.misc.StringHelper;
import com.ruesga.rview.preferences.Constants;
import com.ruesga.rview.preferences.Preferences;

import java.util.ArrayList;
import java.util.Arrays;

public class SearchActivity extends AppCompatActivity {

    private SearchActivityBinding mBinding;
    private int mCurrentOption;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBinding = DataBindingUtil.setContentView(this, R.layout.search_activity);

        setupToolbar();
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.menu_search);
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            getSupportActionBar().setDefaultDisplayHomeAsUpEnabled(false);
        }

        // Configure the search view
        mBinding.searchView.setOnSearchListener(new FloatingSearchView.OnSearchListener() {
            @Override
            public void onSuggestionClicked(SearchSuggestion searchSuggestion) {
            }

            @Override
            public void onSearchAction(String currentQuery) {
                performSearch(currentQuery);
            }
        });
        mBinding.searchView.setOnMenuItemClickListener(item -> performShowOptions());

        mCurrentOption = Preferences.getAccountSearchMode(this, Preferences.getAccount(this));
        configureSearchHint();
    }

    protected void setupToolbar() {
        setSupportActionBar(mBinding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }
    }

    private void configureSearchHint() {
        switch (mCurrentOption) {
            case Constants.SEARCH_MODE_CHANGE:
                mBinding.searchView.setSearchHint(getString(R.string.search_by_change_hint));
                break;
            case Constants.SEARCH_MODE_COMMIT:
                mBinding.searchView.setSearchHint(getString(R.string.search_by_commit_hint));
                break;
            case Constants.SEARCH_MODE_USER:
                mBinding.searchView.setSearchHint(getString(R.string.search_by_user_hint));
                break;
            case Constants.SEARCH_MODE_COMMIT_MESSAGE:
                mBinding.searchView.setSearchHint(getString(R.string.search_by_commit_message_hint));
                break;
            case Constants.SEARCH_MODE_CUSTOM:
                mBinding.searchView.setSearchHint(getString(R.string.search_by_custom_hint));
                break;
        }

        mBinding.searchView.setSearchText(null);
    }

    private void performShowOptions() {
        final ListPopupWindow popupWindow = new ListPopupWindow(this);
        ArrayList<String> values = new ArrayList<>(
                Arrays.asList(getResources().getStringArray(R.array.search_options_labels)));
        SimpleDropDownAdapter adapter = new SimpleDropDownAdapter(
                this, values, values.get(mCurrentOption));
        popupWindow.setAnchorView(mBinding.searchView);
        popupWindow.setDropDownGravity(Gravity.END);
        popupWindow.setAdapter(adapter);
        popupWindow.setContentWidth(adapter.measureContentWidth());
        popupWindow.setOnItemClickListener((parent, view, position, id) -> {
            popupWindow.dismiss();
            mCurrentOption = position;
            Preferences.setAccountSearchMode(this, Preferences.getAccount(this), mCurrentOption);
            configureSearchHint();
        });
        popupWindow.setModal(true);
        popupWindow.show();
    }

    private void performSearch(String query) {
        if (TextUtils.isEmpty(query)) {
            return;
        }

        ChangeQuery filter = null;
        switch (mCurrentOption) {
            case Constants.SEARCH_MODE_CHANGE:
                boolean isLegacyChangeNumber;
                try {
                    int i = Integer.parseInt(query);
                    isLegacyChangeNumber = i > 0;
                } catch (NumberFormatException ex) {
                    isLegacyChangeNumber = false;
                }

                if (isLegacyChangeNumber || StringHelper.GERRIT_CHANGE.matcher(query).matches()) {
                    filter = new ChangeQuery().change(String.valueOf(query));
                } else {
                    // Not a valid filter
                    AndroidHelper.showErrorSnackbar(
                            this, mBinding.getRoot(), R.string.search_not_a_valid_change);
                }

                break;
            case Constants.SEARCH_MODE_COMMIT:
                if (StringHelper.GERRIT_COMMIT.matcher(query).matches()) {
                    filter = new ChangeQuery().commit(String.valueOf(query));
                } else {
                    // Not a valid filter
                    AndroidHelper.showErrorSnackbar(
                            this, mBinding.getRoot(), R.string.search_not_a_valid_commit);
                }
                break;
            case Constants.SEARCH_MODE_USER:
                filter = new ChangeQuery().owner(String.valueOf(query));
                break;
            case Constants.SEARCH_MODE_COMMIT_MESSAGE:
                filter = new ChangeQuery().message(String.valueOf(query));
                break;
            case Constants.SEARCH_MODE_CUSTOM:
                try {
                    filter = ChangeQuery.parse(query);
                } catch (QueryParseException ex) {
                    // Not a valid filter
                    AndroidHelper.showErrorSnackbar(
                            this, mBinding.getRoot(), R.string.search_not_a_valid_custom_query);
                    return;
                }
                break;
        }

        // Open the activity
        ActivityHelper.openChangeListByFilterActivity(this, null, filter);
    }
}
