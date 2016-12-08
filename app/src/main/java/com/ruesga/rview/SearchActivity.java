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

import android.animation.Animator;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.res.TypedArray;
import android.databinding.DataBindingUtil;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.ListPopupWindow;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.animation.AccelerateInterpolator;

import com.arlib.floatingsearchview.FloatingSearchView;
import com.arlib.floatingsearchview.suggestions.model.SearchSuggestion;
import com.ruesga.rview.adapters.SimpleDropDownAdapter;
import com.ruesga.rview.annotations.ProguardIgnored;
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

public class SearchActivity extends AppCompatDelegateActivity {

    @SuppressWarnings("UnusedParameters")
    @ProguardIgnored
    public static class EventHandlers {
        private SearchActivity mActivity;

        public EventHandlers(SearchActivity activity) {
            mActivity = activity;
        }

        public void onDismissByOutsideTouch(View v) {
            mActivity.exitReveal();
        }
    }

    private SearchActivityBinding mBinding;
    private int mCurrentOption;
    private int[] mIcons;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBinding = DataBindingUtil.setContentView(this, R.layout.search_activity);
        mBinding.setHandlers(new EventHandlers(this));

        mIcons = loadSearchIcons();

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
        mBinding.searchView.setCustomIcon(ContextCompat.getDrawable(this, mIcons[mCurrentOption]));

        configureSearchHint();

        enterReveal();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ActivityHelper.LIST_RESULT_CODE && resultCode == RESULT_OK) {
            // Directly finish this activity. The search data was used
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        exitReveal();
    }

    @Override
    public void onPause() {
        super.onPause();

        if (AndroidHelper.isLollipopOrGreater()) {
            overridePendingTransition(0, 0);
        }
    }

    @Override
    public boolean onKeyDown(int keycode, KeyEvent e) {
        switch(keycode) {
            case KeyEvent.KEYCODE_MENU:
                mBinding.searchView.openMenu(true);
                return true;
        }

        return super.onKeyDown(keycode, e);
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
        String value = values.get(mCurrentOption);
        SimpleDropDownAdapter adapter = new SimpleDropDownAdapter(this, values, mIcons, value);
        popupWindow.setAnchorView(mBinding.searchView);
        popupWindow.setDropDownGravity(Gravity.END);
        popupWindow.setAdapter(adapter);
        popupWindow.setContentWidth(adapter.measureContentWidth());
        popupWindow.setOnItemClickListener((parent, view, position, id) -> {
            popupWindow.dismiss();
            mCurrentOption = position;
            Preferences.setAccountSearchMode(this, Preferences.getAccount(this), mCurrentOption);
            configureSearchHint();
            mBinding.searchView.setCustomIcon(ContextCompat.getDrawable(this, mIcons[position]));
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
                    return;
                }

                break;
            case Constants.SEARCH_MODE_COMMIT:
                if (StringHelper.GERRIT_COMMIT.matcher(query).matches()) {
                    filter = new ChangeQuery().commit(String.valueOf(query));
                } else {
                    // Not a valid filter
                    AndroidHelper.showErrorSnackbar(
                            this, mBinding.getRoot(), R.string.search_not_a_valid_commit);
                    return;
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
        ActivityHelper.openChangeListByFilterActivity(this, null, filter, true);
    }

    private int[] loadSearchIcons() {
        TypedArray ta = getResources().obtainTypedArray(R.array.search_options_icons);
        int count = ta.length();
        int[] icons = new int[count];
        for (int i = 0; i < count; i++) {
            icons[i] = ta.getResourceId(i, -1);
        }
        ta.recycle();
        return icons;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void enterReveal() {
        if (AndroidHelper.isLollipopOrGreater()) {
            final View v = mBinding.searchView;
            ViewCompat.postOnAnimation(v, () -> {
                int cx = v.getMeasuredWidth();
                int cy = v.getMeasuredHeight() / 2;
                int r = v.getWidth() / 2;
                Animator anim = ViewAnimationUtils.createCircularReveal(v, cx, cy, 0, r);
                anim.setInterpolator(new AccelerateInterpolator());
                anim.setDuration(250L);
                anim.addListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mBinding.background.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {
                    }
                });
                anim.start();
            });
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void exitReveal() {
        if (AndroidHelper.isLollipopOrGreater()) {
            final View v = mBinding.toolbar;
            ViewCompat.postOnAnimation(v, () -> {
                int cx = v.getMeasuredWidth();
                int cy = v.getMeasuredHeight() / 2;
                int r = v.getWidth() / 2;
                Animator anim = ViewAnimationUtils.createCircularReveal(v, cx, cy, r, 0);
                anim.setInterpolator(new AccelerateInterpolator());
                anim.setDuration(250L);
                anim.addListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        mBinding.background.setVisibility(View.GONE);
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mBinding.searchView.setVisibility(View.INVISIBLE);
                        finish();
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {
                    }
                });
                anim.start();
            });
        } else {
            finish();
        }
    }
}
