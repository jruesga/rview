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
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.MenuRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SlidingPaneLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ruesga.rview.annotations.ProguardIgnored;
import com.ruesga.rview.databinding.ContentBinding;
import com.ruesga.rview.drawer.DrawerNavigationView;
import com.ruesga.rview.drawer.DrawerNavigationView.OnDrawerNavigationItemSelectedListener;
import com.ruesga.rview.fragments.PageableFragment.PageFragmentAdapter;
import com.ruesga.rview.fragments.SelectableFragment;
import com.ruesga.rview.misc.ActivityHelper;
import com.ruesga.rview.misc.AndroidHelper;
import com.ruesga.rview.misc.ExceptionHelper;
import com.ruesga.rview.misc.ModelHelper;
import com.ruesga.rview.model.Account;
import com.ruesga.rview.model.EmptyState;
import com.ruesga.rview.preferences.Preferences;
import com.ruesga.rview.widget.PagerControllerLayout;
import com.ruesga.rview.widget.PagerControllerLayout.OnPageSelectionListener;
import com.ruesga.rview.widget.PagerControllerLayout.PagerControllerAdapter;

import java.util.List;

import javax.net.ssl.SSLException;

public abstract class BaseActivity extends AppCompatActivity implements OnRefreshListener {

    public static final String FRAGMENT_TAG_LIST = "list";
    public static final String FRAGMENT_TAG_DETAILS = "details";

    @ProguardIgnored
    public static class Model implements Parcelable {
        public boolean isInProgress = false;
        public boolean hasTabs = false;
        public boolean hasPages = false;
        public boolean useTowPane = true;
        public boolean hasMiniDrawer = true;
        public boolean hasForceSinglePanel = false;

        public Model() {
        }

        protected Model(Parcel in) {
            isInProgress = in.readByte() != 0;
            hasTabs = in.readByte() != 0;
            hasPages = in.readByte() != 0;
            useTowPane = in.readByte() != 0;
            hasMiniDrawer = in.readByte() != 0;
            hasForceSinglePanel = in.readByte() != 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeByte((byte) (isInProgress ? 1 : 0));
            dest.writeByte((byte) (hasTabs ? 1 : 0));
            dest.writeByte((byte) (hasPages ? 1 : 0));
            dest.writeByte((byte) (useTowPane ? 1 : 0));
            dest.writeByte((byte) (hasMiniDrawer ? 1 : 0));
            dest.writeByte((byte) (hasForceSinglePanel ? 1 : 0));
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

    private Handler mUiHandler;

    private Model mModel = new Model();
    private ViewPager mViewPager;
    private boolean mHasStateSaved;

    private AlertDialog mDialog;

    private SlidingPaneLayout mMiniDrawerLayout;

    public abstract DrawerLayout getDrawerLayout();

    public abstract ContentBinding getContentBinding();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        mUiHandler = new Handler();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mDialog != null) {
            mDialog.dismiss();
            mDialog = null;
        }
    }

    protected void setupActivity() {
        mMiniDrawerLayout = (SlidingPaneLayout) getContentBinding().getRoot()
                .findViewById(R.id.mini_drawer_layout);
        if (mMiniDrawerLayout != null) {
            // Disable hardware acceleration to deal with draw glitches
            getContentBinding().getRoot().setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        setSupportActionBar(getContentBinding().toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);

            if (mMiniDrawerLayout != null && mModel.useTowPane) {
                configureMiniDrawer();
            } else {
                configureFullDrawer();
            }

            configureOptionsDrawer();
        }
    }

    private void configureMiniDrawer() {
        if (getDrawerLayout() != null) {
            mModel.hasMiniDrawer = true;

            ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(
                    this, getDrawerLayout(), getContentBinding().toolbar, 0, 0);
            getDrawerLayout().addDrawerListener(drawerToggle);
            drawerToggle.syncState();
            getContentBinding().drawerLayout.setDrawerLockMode(
                    DrawerLayout.LOCK_MODE_LOCKED_CLOSED, GravityCompat.START);
            getContentBinding().toolbar.setNavigationOnClickListener(view -> {
                if (mMiniDrawerLayout.isOpen()) {
                    mMiniDrawerLayout.closePane();
                } else {
                    mMiniDrawerLayout.openPane();
                }
            });
            mMiniDrawerLayout.closePane();
            getContentBinding().drawerNavigationView.configureWithMiniDrawer(mMiniDrawerLayout);

        } else {
            // Don't use mini drawer
            mModel.hasMiniDrawer = false;
        }
        getContentBinding().setModel(mModel);
    }

    SlidingPaneLayout getMiniDrawerLayout() {
        return mMiniDrawerLayout;
    }

    private void configureFullDrawer() {
        if (getDrawerLayout() != null) {
            ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(
                    this, getDrawerLayout(), getContentBinding().toolbar, 0, 0);
            getDrawerLayout().addDrawerListener(drawerToggle);
            drawerToggle.syncState();

            getContentBinding().drawerLayout.setDrawerLockMode(
                    DrawerLayout.LOCK_MODE_UNLOCKED,
                    getContentBinding().drawerNavigationView);
        } else {
            if (!mModel.hasForceSinglePanel) {
                getContentBinding().drawerLayout.setDrawerLockMode(
                        DrawerLayout.LOCK_MODE_LOCKED_CLOSED,
                        getContentBinding().drawerNavigationView);
            } else {
                // Someones is requesting a single panel in a multipanel layout
                // Just hide the multipanel
                mModel.hasMiniDrawer = false;
            }
        }
    }

    private void configureOptionsDrawer() {
        // Options is open/closed programatically
        getContentBinding().drawerLayout.setDrawerLockMode(
                DrawerLayout.LOCK_MODE_LOCKED_CLOSED,
                getContentBinding().drawerOptionsView);

        // Listen for options close
        getContentBinding().drawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {

            }

            @Override
            public void onDrawerOpened(View drawerView) {

            }

            @Override
            public void onDrawerClosed(View drawerView) {
                if (getContentBinding().drawerOptionsView == drawerView) {
                    getContentBinding().drawerLayout.setDrawerLockMode(
                            DrawerLayout.LOCK_MODE_LOCKED_CLOSED,
                            getContentBinding().drawerOptionsView);
                }
            }

            @Override
            public void onDrawerStateChanged(int newState) {

            }
        });
    }

    public boolean hasStateSaved() {
        return mHasStateSaved;
    }

    public void setUseTwoPanel(boolean useTwoPanel) {
        mModel.useTowPane = useTwoPanel;
        getContentBinding().setModel(mModel);
    }

    public void setForceSinglePanel(boolean useTwoPanel) {
        mModel.useTowPane = useTwoPanel;
        getContentBinding().setModel(mModel);
    }

    public void invalidateTabs() {
        mModel.hasPages = false;
        mModel.hasTabs = false;
        getContentBinding().tabs.setupWithViewPager(null);
        getContentBinding().setModel(mModel);
    }

    public void configureTabs(ViewPager viewPager, boolean fixedMode) {
        mViewPager = viewPager;
        mModel.hasPages = false;
        mModel.hasTabs = true;
        getContentBinding().tabs.setTabMode(
                fixedMode ? TabLayout.MODE_FIXED : TabLayout.MODE_SCROLLABLE);
        getContentBinding().tabs.setupWithViewPager(viewPager);
        getContentBinding().tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                PageFragmentAdapter adapter = (PageFragmentAdapter) mViewPager.getAdapter();
                Fragment fragment = adapter.getCachedFragment(tab.getPosition());
                if (fragment != null && fragment instanceof SelectableFragment) {
                    mUiHandler.post(((SelectableFragment) fragment)::onFragmentSelected);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
        viewPager.getAdapter().notifyDataSetChanged();
        getContentBinding().setModel(mModel);
    }

    public void invalidatePages() {
        mModel.hasPages = false;
        mModel.hasTabs = false;
        getContentBinding().pagerController.listenOn(null).with(null);
        getContentBinding().setModel(mModel);
    }

    public void configurePages(PagerControllerAdapter adapter, OnPageSelectionListener cb) {
        mViewPager = null;
        mModel.hasPages = true;
        mModel.hasTabs = false;
        getContentBinding().pagerController
                .listenOn(position -> {
                    if (getSupportActionBar() != null) {
                        getSupportActionBar().setSubtitle(
                                position == PagerControllerLayout.INVALID_PAGE
                                        ? null : adapter.getPageTitle(position));

                        if (cb != null) {
                            cb.onPageSelected(position);
                        }
                    }
                })
                .with(adapter);
        getContentBinding().setModel(mModel);
    }

    public void configureOptionsMenu(@MenuRes int menu, OnDrawerNavigationItemSelectedListener cb) {
        if (menu != 0) {
            getOptionsMenu().inflateMenu(menu);
            getOptionsMenu().setDrawerNavigationItemSelectedListener(cb);
        }
    }

    public void configureOptionsTitle(String title) {
        TextView tv = (TextView) getContentBinding().drawerOptionsView
                .getHeaderView(0).findViewById(R.id.options_title);
        tv.setText(title);
    }

    @Override
    public void onResume() {
        super.onResume();
        mHasStateSaved = false;
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedState) {
        if (savedState != null) {
            super.onRestoreInstanceState(savedState);
            mModel = savedState.getParcelable(getClass().getSimpleName()
                    + "_base_activity_model");
        }
        mHasStateSaved = false;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(getClass().getSimpleName()
                + "_base_activity_model", mModel);
        mHasStateSaved = true;
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

    @Override
    public void onBackPressed() {
        if (getContentBinding() != null && getContentBinding().drawerLayout != null) {
            if (getContentBinding().drawerLayout.isDrawerOpen(
                    getContentBinding().drawerOptionsView)) {
                closeOptionsDrawer();
                return;
            }
        }
        super.onBackPressed();
    }

    public void showError(@StringRes int message) {
        AndroidHelper.showErrorSnackbar(this, getContentBinding().getRoot(), message);
    }

    public void showWarning(@StringRes int message) {
        AndroidHelper.showWarningSnackbar(this, getContentBinding().getRoot(), message);
    }

    public void handleException(String tag, Throwable cause) {
        handleException(tag, cause, null);
    }

    public void handleException(String tag, Throwable cause,
            final EmptyState.EventHandlers emptyHandler) {
        int level = ExceptionHelper.exceptionToLevel(cause);
        int res = ExceptionHelper.exceptionToMessage(this, tag, cause);
        if (level == 0) {
            showError(res);
        } else {
            showWarning(res);
        }

        // Handle SSL exceptions, so we can ask the user for temporary trust the server connection
        final Account account = Preferences.getAccount(this);
        if (account != null && emptyHandler != null
                && ExceptionHelper.isException(cause, SSLException.class)
                && (account.mRepository == null || !account.mRepository.mTrustAllCertificates)
                && !ModelHelper.hasTemporaryTrustAllCertificatesAccessRequested(account)) {
            // Ask the user for temporary access
            mDialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.untrusted_connection_title)
                    .setMessage(R.string.untrusted_connection_message)
                    .setIcon(R.drawable.ic_warning)
                    .setPositiveButton(R.string.untrusted_connection_action_trust,
                            (dialogInterface, i) -> {
                        ModelHelper.setTemporaryTrustAllCertificatesAccessGrant(account, true);
                        emptyHandler.onRetry(null);
                    })
                    .setNegativeButton(R.string.untrusted_connection_action_no_trust,
                            (dialogInterface, i) ->
                                    ModelHelper.setTemporaryTrustAllCertificatesAccessGrant(
                                            account, false))
                    .setOnCancelListener(dialogInterface ->
                            ModelHelper.setTemporaryTrustAllCertificatesAccessGrant(
                                    account, false))
                    .setOnDismissListener(dialogInterface -> mDialog = null)
                    .create();
            mDialog.show();
        }
    }

    private void changeInProgressStatus(boolean status) {
        mModel.isInProgress = status;
        getContentBinding().setModel(mModel);
    }

    @Override
    public void onRefreshStart(Fragment from) {
        changeInProgressStatus(true);
    }

    @Override
    public <T> void onRefreshEnd(Fragment from, T result) {
        changeInProgressStatus(false);
    }

    public void openOptionsDrawer() {
        if (!getContentBinding().drawerLayout.isDrawerOpen(getContentBinding().drawerOptionsView)) {
            getContentBinding().drawerLayout.setDrawerLockMode(
                    DrawerLayout.LOCK_MODE_UNLOCKED,
                    getContentBinding().drawerOptionsView);
            getContentBinding().drawerLayout.openDrawer(getContentBinding().drawerOptionsView);
        }
    }

    public void closeOptionsDrawer() {
        if (getContentBinding().drawerLayout.isDrawerOpen(getContentBinding().drawerOptionsView)) {
            getContentBinding().drawerLayout.closeDrawer(getContentBinding().drawerOptionsView);
        }
        getContentBinding().drawerLayout.setDrawerLockMode(
                DrawerLayout.LOCK_MODE_LOCKED_CLOSED,
                getContentBinding().drawerOptionsView);
    }

    public DrawerNavigationView getOptionsMenu() {
        return getContentBinding().drawerOptionsView;
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        if (fragments != null) {
            for (Fragment fragment : fragments) {
                fragment.onRequestPermissionsResult(requestCode, permissions, grantResults);
            }
        }
    }
}
