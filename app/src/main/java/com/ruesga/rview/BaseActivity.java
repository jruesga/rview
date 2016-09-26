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

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.StringRes;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import com.ruesga.rview.annotations.ProguardIgnored;
import com.ruesga.rview.databinding.ContentBinding;
import com.ruesga.rview.fragments.PaginableFragment.PageFragmentAdapter;
import com.ruesga.rview.fragments.SelectableFragment;
import com.ruesga.rview.misc.ActivityHelper;
import com.ruesga.rview.misc.AndroidHelper;
import com.ruesga.rview.misc.ExceptionHelper;

public abstract class BaseActivity extends AppCompatActivity implements OnRefreshListener {

    public static final String FRAGMENT_TAG_LIST = "list";
    public static final String FRAGMENT_TAG_DETAILS = "details";

    @ProguardIgnored
    public static class Model implements Parcelable {
        public boolean isInProgress = false;
        public boolean hasTabs = false;
        public boolean useTowPane = true;

        public Model() {
        }

        protected Model(Parcel in) {
            isInProgress = in.readByte() != 0;
            hasTabs = in.readByte() != 0;
            useTowPane = in.readByte() != 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeByte((byte) (isInProgress ? 1 : 0));
            dest.writeByte((byte) (hasTabs ? 1 : 0));
            dest.writeByte((byte) (useTowPane ? 1 : 0));
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public static final Creator<Model> CREATOR = new Creator<Model>() {
            @Override
            public Model createFromParcel(Parcel in) {
                return new Model(in);
            }

            @Override
            public Model[] newArray(int size) {
                return new Model[size];
            }
        };
    }


    private Model mModel = new Model();

    private ViewPager mViewPager;

    public abstract DrawerLayout getDrawerLayout();

    public abstract ContentBinding getContentBinding();

    protected void setupToolbar() {
        setSupportActionBar(getContentBinding().toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
            if (getDrawerLayout() != null) {
                ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(
                        this, getDrawerLayout(), getContentBinding().toolbar, 0, 0);
                getDrawerLayout().addDrawerListener(drawerToggle);
                drawerToggle.syncState();
            }
        }
    }

    protected void setUseTwoPanel(boolean useTwoPanel) {
        mModel.useTowPane = useTwoPanel;
        getContentBinding().setModel(mModel);
    }

    protected void invalidateTabs() {
        mModel.hasTabs = false;
        getContentBinding().tabs.setupWithViewPager(null);
        getContentBinding().setModel(mModel);
    }

    public void configureTabs(ViewPager pager, boolean fixedMode) {
        mViewPager = pager;
        mModel.hasTabs = true;
        getContentBinding().tabs.setTabMode(
                fixedMode ? TabLayout.MODE_FIXED : TabLayout.MODE_SCROLLABLE);
        getContentBinding().tabs.setupWithViewPager(pager);
        getContentBinding().tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                PageFragmentAdapter adapter = (PageFragmentAdapter) mViewPager.getAdapter();
                Fragment fragment = adapter.getCacheFragment(tab.getPosition());
                if (fragment != null && fragment instanceof SelectableFragment) {
                    ((SelectableFragment) fragment).onFragmentSelected();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
        pager.getAdapter().notifyDataSetChanged();
        getContentBinding().setModel(mModel);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedState) {
        if (savedState != null) {
            super.onRestoreInstanceState(savedState);
            mModel = savedState.getParcelable(getClass().getSimpleName()
                    + "_base_activity_model");
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(getClass().getSimpleName()
                + "_base_activity_model", mModel);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                return ActivityHelper.performFinishActivity(this, false);
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void showError(@StringRes int message) {
        AndroidHelper.showErrorSnackbar(this, getContentBinding().getRoot(), message);
    }

    public void showWarning(@StringRes int message) {
        AndroidHelper.showWarningSnackbar(this, getContentBinding().getRoot(), message);
    }

    public void handleException(String tag, Throwable cause) {
        showError(ExceptionHelper.exceptionToMessage(this, tag, cause));
    }

    private void changeInProgressStatus(boolean status) {
        mModel.isInProgress = status;
        getContentBinding().setModel(mModel);
    }

    @Override
    public void onRefreshStart() {
        changeInProgressStatus(true);
    }

    @Override
    public <T> void onRefreshEnd(T result) {
        changeInProgressStatus(false);
    }
}
