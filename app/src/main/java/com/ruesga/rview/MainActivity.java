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
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {

    private static final int INVALID_ITEM = -1;

    private static final int MESSAGE_NAVIGATE_TO = 0;

    private final Handler.Callback mMessenger = new Handler.Callback() {
        @Override
        public boolean handleMessage(Message message) {
            if (message.what == MESSAGE_NAVIGATE_TO) {
                performNavigateTo();
                return true;
            }
            return false;
        }
    };

    @BindView(R.id.toolbar) Toolbar mToolbar;
    @BindView(R.id.drawer_layout) DrawerLayout mDrawerLayout;
    @BindView(R.id.drawer_navigation_view) NavigationView mNavigationView;
    private TextView mAccountName;
    private TextView mAccountServer;
    private ImageView mAccountSwitcher;
    private boolean mAccountExpanded;

    private int mCurrentNavigationItemId = INVALID_ITEM;

    private Handler mUiHandler;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUiHandler = new Handler(mMessenger);

        onRestoreInstanceState(savedInstanceState);

        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);
        setupToolbar();
        setupNavigationHeader();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedState) {
        if (savedState != null) {
            super.onRestoreInstanceState(savedState);
            mAccountExpanded = savedState.getBoolean("navbar_expanded", false);
            mCurrentNavigationItemId = savedState.getInt("navbar_current_item", INVALID_ITEM);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("navbar_expanded", mAccountExpanded);
        outState.putInt("navbar_current_item", mCurrentNavigationItemId);
    }

    private void setupToolbar() {
        setSupportActionBar(mToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
            ActionBarDrawerToggle drawerToggle =
                    new ActionBarDrawerToggle(this, mDrawerLayout, mToolbar, 0, 0);
            mDrawerLayout.addDrawerListener(drawerToggle);
            drawerToggle.syncState();
        }
    }

    private void setupNavigationHeader() {
        // Since header layout is inject in a delayed in 23.1.0 and up, just need
        // to bind its views manually
        View navigationHeader = mNavigationView.getHeaderView(0);
        mAccountName = ButterKnife.findById(navigationHeader, R.id.account_name);
        mAccountServer = ButterKnife.findById(navigationHeader, R.id.account_server);
        mAccountSwitcher = ButterKnife.findById(navigationHeader, R.id.account_switcher);
        View accountInfo = ButterKnife.findById(navigationHeader, R.id.account_info);
        accountInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                performShowAccount(!mAccountExpanded);
            }
        });

        // Hide account options
        performShowAccount(mAccountExpanded);

        // Listen for click events and select the current one
        mNavigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                requestNavigateTo(item.getItemId());
                return true;
            }
        });
        requestNavigateTo(mCurrentNavigationItemId == INVALID_ITEM
                ? R.id.menu_open : mCurrentNavigationItemId);
    }

    private void performShowAccount(boolean show) {
        final Menu menu = mNavigationView.getMenu();
        menu.setGroupVisible(R.id.category_all, !show);
        menu.setGroupVisible(R.id.category_my_menu, !show);
        menu.setGroupVisible(R.id.category_my_account, show);
        menu.setGroupVisible(R.id.category_other_accounts, show);
        mAccountSwitcher.setImageResource(
                show ? R.drawable.ic_arrow_drop_up : R.drawable.ic_arrow_drop_down);
        mAccountExpanded = show;

        // Sync the current selection status
        if (mCurrentNavigationItemId != -1) {
            performSelectItem(mCurrentNavigationItemId, true);
        }
    }

    private void requestNavigateTo(int itemId) {
        // Select the item
        final boolean navigate = performSelectItem(itemId, false);

        // Close the drawer
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawer(GravityCompat.START, true);
        }

        if (navigate) {
            Message.obtain(mUiHandler, MESSAGE_NAVIGATE_TO).sendToTarget();
        }
    }

    private boolean performSelectItem(int itemId, boolean force) {
        final Menu menu = mNavigationView.getMenu();
        final MenuItem item = menu.findItem(itemId);
        if (item != null) {
            boolean navigate = force || !item.isCheckable() || itemId != mCurrentNavigationItemId;
            if (item.isCheckable()) {
                mNavigationView.setCheckedItem(item.getItemId());
            }
            mCurrentNavigationItemId = itemId;
            return navigate;
        }
        return false;
    }

    private void performNavigateTo() {
        final Menu menu = mNavigationView.getMenu();
        final MenuItem item = menu.findItem(mCurrentNavigationItemId);
        if (item == null) {
            return;
        }

        switch (item.getItemId()) {
            case R.id.menu_account_settings:
                break;
            case R.id.menu_delete_account:
                break;
            case R.id.menu_add_account:
                break;

            default:
                if (item.getGroupId() == R.id.category_other_accounts) {
                    // Choose account
                } else {
                    // Filter
                    if (getSupportActionBar() != null) {
                        getSupportActionBar().setTitle(item.getTitle());
                        getSupportActionBar().setSubtitle("status:open");
                    }

                    // Navigate to filter fragment
                }
                break;
        }

        // Hide account info
        performShowAccount(false);
    }
}
