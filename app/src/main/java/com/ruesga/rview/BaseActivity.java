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

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
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
import com.ruesga.rview.preferences.Constants;
import com.ruesga.rview.preferences.Preferences;
import com.ruesga.rview.widget.PagerControllerLayout;
import com.ruesga.rview.widget.PagerControllerLayout.OnPageSelectionListener;
import com.ruesga.rview.widget.PagerControllerLayout.PagerControllerAdapter;
import com.ruesga.rview.widget.ScrollAwareFloatingActionButtonBehavior;
import com.ruesga.rview.wizards.AuthorizationAccountSetupActivity;

import java.util.List;

import javax.net.ssl.SSLException;

import androidx.annotation.Keep;
import androidx.annotation.MenuRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.slidingpanelayout.widget.SlidingPaneLayout;
import androidx.viewpager.widget.ViewPager;

public abstract class BaseActivity extends AppCompatDelegateActivity implements OnRefreshListener {

    public static final String FRAGMENT_TAG_LIST = "list";
    public static final String FRAGMENT_TAG_DETAILS = "details";

    public interface OnFabPressedListener {
        void onFabPressed(FloatingActionButton fab);
    }

    @Keep
    @SuppressWarnings("unused")
    public static class EventHandlers {
        BaseActivity mActivity;

        EventHandlers(BaseActivity activity) {
            mActivity = activity;
        }

        public void onFabPressed(View view) {
            if (mActivity.mOnFabPressedListener != null) {
                mActivity.mOnFabPressedListener.onFabPressed((FloatingActionButton) view);
            }
        }
    }

    @Keep
    public static class Model implements Parcelable {
        public boolean isInProgress = false;
        public boolean hasTabs = false;
        public boolean hasPages = false;
        public boolean useTowPane = true;
        public boolean hasMiniDrawer = true;
        public boolean hasForceSinglePanel = false;
        public boolean hasFab = false;

        public Model() {
        }

        protected Model(Parcel in) {
            isInProgress = in.readByte() != 0;
            hasTabs = in.readByte() != 0;
            hasPages = in.readByte() != 0;
            useTowPane = in.readByte() != 0;
            hasMiniDrawer = in.readByte() != 0;
            hasForceSinglePanel = in.readByte() != 0;
            hasFab = in.readByte() != 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeByte((byte) (isInProgress ? 1 : 0));
            dest.writeByte((byte) (hasTabs ? 1 : 0));
            dest.writeByte((byte) (hasPages ? 1 : 0));
            dest.writeByte((byte) (useTowPane ? 1 : 0));
            dest.writeByte((byte) (hasMiniDrawer ? 1 : 0));
            dest.writeByte((byte) (hasForceSinglePanel ? 1 : 0));
            dest.writeByte((byte) (hasFab ? 1 : 0));
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
    private final EventHandlers mHandlers = new EventHandlers(this);
    private ViewPager mViewPager;
    private boolean mHasStateSaved;
    private OnFabPressedListener mOnFabPressedListener;

    private AlertDialog mDialog;

    private SlidingPaneLayout mMiniDrawerLayout;

    public abstract DrawerLayout getDrawerLayout();

    public abstract ContentBinding getContentBinding();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidHelper.configureTaskDescription(this);
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
        mMiniDrawerLayout = getContentBinding().getRoot().findViewById(R.id.mini_drawer_layout);
        if (mMiniDrawerLayout != null) {
            // Disable drawer elevation to match AppBarLayout
            View v = getContentBinding().getRoot().findViewById(R.id.drawer_navigation_view);
            ViewCompat.setElevation(v, 0);
        }

        setSupportActionBar(getContentBinding().toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);

            setupDrawer();
            configureOptionsDrawer();
        }

        getContentBinding().setHandlers(mHandlers);
    }

    private void setupDrawer() {
        if (mMiniDrawerLayout != null && mModel.useTowPane) {
            configureMiniDrawer();
        } else {
            configureFullDrawer();
        }
    }

    public void setupFab(OnFabPressedListener cb) {
        final Account account = Preferences.getAccount(this);
        mOnFabPressedListener = cb;
        mModel.hasFab = account != null && account.hasAuthenticatedAccessMode()
                && mOnFabPressedListener != null;
        if (!mModel.hasFab) {
            getContentBinding().fab.hide();
        } else {
            getContentBinding().fab.show();
        }
        getContentBinding().setModel(mModel);
    }

    public void registerFabWithRecyclerView(RecyclerView view) {
        final Account mAccount = Preferences.getAccount(BaseActivity.this);
        if (mAccount != null && mAccount.hasAuthenticatedAccessMode()) {
            view.addOnScrollListener(new RecyclerView.OnScrollListener() {
                final ScrollAwareFloatingActionButtonBehavior mBehavior =
                        new ScrollAwareFloatingActionButtonBehavior(BaseActivity.this, null);

                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    if (mOnFabPressedListener != null) {
                        int[] consumed = new int[2];
                        mBehavior.onNestedScroll(getContentBinding().pageContentLayout,
                                getContentBinding().fab, view, dx, dy, 0, 0, -1, consumed);
                    }
                }
            });
        }
    }

    private void configureMiniDrawer() {
        if (getDrawerLayout() != null) {
            mModel.hasMiniDrawer = true;

            ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(
                    this, getDrawerLayout(), getContentBinding().toolbar, 0, 0);
            getDrawerLayout().addDrawerListener(drawerToggle);
            getContentBinding().drawerLayout.setDrawerLockMode(
                    DrawerLayout.LOCK_MODE_LOCKED_CLOSED,
                    GravityCompat.START);
            drawerToggle.syncState();
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
            getDrawerLayout().setDrawerLockMode(
                    DrawerLayout.LOCK_MODE_UNLOCKED,
                    GravityCompat.START);
            drawerToggle.syncState();
        } else {
            final ViewGroup.LayoutParams params = getContentBinding().drawerLayout.getLayoutParams();
            if (!(params instanceof SlidingPaneLayout.LayoutParams)) {
                getContentBinding().drawerLayout.setDrawerLockMode(
                        DrawerLayout.LOCK_MODE_LOCKED_CLOSED,
                        GravityCompat.START);
            }
            if (mModel.hasForceSinglePanel) {
                // Someones is requesting a single panel in a multipanel layout
                // Just hide the multipanel
                mModel.hasMiniDrawer = false;
            }
        }
    }

    private void configureOptionsDrawer() {
        // Options is open/closed programmatically
        getContentBinding().drawerLayout.setDrawerLockMode(
                DrawerLayout.LOCK_MODE_LOCKED_CLOSED,
                GravityCompat.END);

        // Listen for options close
        getContentBinding().drawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {

            }

            @Override
            public void onDrawerOpened(@NonNull View drawerView) {

            }

            @Override
            public void onDrawerClosed(@NonNull View drawerView) {
                if (getContentBinding().drawerOptionsView == drawerView) {
                    getContentBinding().drawerLayout.setDrawerLockMode(
                            DrawerLayout.LOCK_MODE_LOCKED_CLOSED,
                            GravityCompat.END);
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

    public void setForceSinglePanel(boolean singlePanel) {
        mModel.hasForceSinglePanel = singlePanel;
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
                //noinspection ConstantConditions
                Fragment fragment = adapter.getCachedFragment(tab.getPosition());
                if (fragment instanceof SelectableFragment) {
                    mUiHandler.post(((SelectableFragment) fragment)::onFragmentSelected);
                }

                // Show fab if necessary
                showFab();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
        //noinspection ConstantConditions
        viewPager.getAdapter().notifyDataSetChanged();
        getContentBinding().setModel(mModel);
    }

    public void invalidatePages() {
        mModel.hasPages = false;
        mModel.hasTabs = false;
        getContentBinding().pagerController.listenOn(null).with(null);
        getContentBinding().setModel(mModel);
    }

    private void showFab() {
        final Account account = Preferences.getAccount(this);
        if (account != null && account.hasAuthenticatedAccessMode()
                && mOnFabPressedListener != null) {
            getContentBinding().fab.show();
        }
    }

    public void configurePages(PagerControllerAdapter adapter, OnPageSelectionListener cb) {
        mViewPager = null;
        mModel.hasPages = true;
        mModel.hasTabs = false;
        getContentBinding().pagerController
                .listenOn((position, fromUser) -> {
                    if (getSupportActionBar() != null) {
                        getSupportActionBar().setSubtitle(
                                position == PagerControllerLayout.INVALID_PAGE
                                        ? null : adapter.getPageTitle(position));

                        if (cb != null) {
                            cb.onPageSelected(position, fromUser);
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
        TextView tv = getContentBinding().drawerOptionsView
                .getHeaderView(0).findViewById(R.id.options_title);
        tv.setText(title);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch(event.getKeyCode()) {
                case KeyEvent.KEYCODE_MENU:
                    if (getContentBinding().drawerLayout != null &&
                            getContentBinding().drawerLayout.getDrawerLockMode(
                                    GravityCompat.START) != DrawerLayout.LOCK_MODE_LOCKED_CLOSED) {
                        if (getContentBinding().drawerLayout.isDrawerOpen(GravityCompat.START)) {
                            getContentBinding().drawerLayout.closeDrawer(GravityCompat.START);
                        } else {
                            getContentBinding().drawerLayout.openDrawer(GravityCompat.START);
                        }
                        return true;
                    } else if (getOptionsMenu() != null && getOptionsMenu().getMenu() != null
                            && getOptionsMenu().getMenu().size() > 0) {
                        if (getContentBinding().drawerLayout.isDrawerOpen(GravityCompat.END)) {
                            closeOptionsDrawer();
                        } else {
                            openOptionsDrawer();
                        }
                        return true;
                    }
                    break;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch(keyCode) {
            case KeyEvent.KEYCODE_MENU:
                if (getContentBinding().drawerLayout != null &&
                        getContentBinding().drawerLayout.getDrawerLockMode(
                                GravityCompat.START) != DrawerLayout.LOCK_MODE_LOCKED_CLOSED) {
                    if (getContentBinding().drawerLayout.isDrawerOpen(GravityCompat.START)) {
                        getContentBinding().drawerLayout.closeDrawer(GravityCompat.START);
                    } else {
                        getContentBinding().drawerLayout.openDrawer(GravityCompat.START);
                    }
                    return true;
                } else if (getOptionsMenu() != null && getOptionsMenu().getMenu() != null
                        && getOptionsMenu().getMenu().size() > 0) {
                    if (getContentBinding().drawerLayout.isDrawerOpen(GravityCompat.END)) {
                        closeOptionsDrawer();
                    } else {
                        openOptionsDrawer();
                    }
                    return true;
                }
        }
        return super.onKeyDown(keyCode, event);
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

        // We need to restore drawer layout lock state
        if (getContentBinding() != null) {
            setupDrawer();
        }
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
                boolean hasForceUp = getIntent().getBooleanExtra(Constants.EXTRA_HAS_FORCE_UP, false);
                return ActivityHelper.performFinishActivity(this, hasForceUp);
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        if (getContentBinding() != null && getContentBinding().drawerLayout != null) {
            final DrawerLayout drawer = getContentBinding().drawerLayout;
            final DrawerNavigationView optionsView = getContentBinding().drawerOptionsView;
            final DrawerNavigationView navigationView = getContentBinding().drawerNavigationView;
            if (optionsView != null && drawer.isDrawerOpen(optionsView)) {
                closeOptionsDrawer();
                return;
            }
            if (mMiniDrawerLayout == null && navigationView != null
                    && drawer.isDrawerOpen(navigationView)) {
                drawer.closeDrawer(navigationView);
                return;
            } else if (mMiniDrawerLayout != null && mMiniDrawerLayout.isOpen()) {
                mMiniDrawerLayout.closePane();
                return;
            }
        }
        if (!ActivityHelper.performFinishActivity(this, false)) {
            super.onBackPressed();
        }
    }

    public void showError(@StringRes int message) {
        AndroidHelper.showErrorSnackbar(this,
                getSnackBarTarget(getContentBinding().getRoot()), message);
    }

    public void showWarning(@StringRes int message) {
        AndroidHelper.showWarningSnackbar(this,
                getSnackBarTarget(getContentBinding().getRoot()), message);
    }

    private View getSnackBarTarget(View root) {
        View v = root.findViewById(R.id.page_content_layout);
        if (v instanceof CoordinatorLayout) {
            return v;
        }
        return root;
    }

    public void showToast(@StringRes int message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
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

        // Handle authentication denied
        if (ExceptionHelper.isAuthenticationException(cause)) {
            if (!ExceptionHelper.hasPreviousAuthenticationFailure(this)) {
                //  Handle Unauthorized exception
                ExceptionHelper.markAsAuthenticationFailure(this);
                Intent i = new Intent(BaseActivity.this, AuthorizationAccountSetupActivity.class);
                i.putExtra(ExceptionHelper.EXTRA_AUTHENTICATION_FAILURE, true);
                startActivity(i);
                return;
            }
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
                    GravityCompat.END);
            getContentBinding().drawerLayout.openDrawer(getContentBinding().drawerOptionsView);
        }
    }

    public void closeOptionsDrawer() {
        if (getContentBinding().drawerLayout.isDrawerOpen(getContentBinding().drawerOptionsView)) {
            getContentBinding().drawerLayout.closeDrawer(getContentBinding().drawerOptionsView);
        }
        getContentBinding().drawerLayout.setDrawerLockMode(
                DrawerLayout.LOCK_MODE_LOCKED_CLOSED,
                GravityCompat.END);
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

    public void setSafeActionBarTitle(String title, String subtitle) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
            getSupportActionBar().setSubtitle(subtitle);
        }
    }
}
