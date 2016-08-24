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
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.GravityCompat;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.ruesga.rview.databinding.ActivityMainBinding;
import com.ruesga.rview.databinding.NavigationHeaderBinding;
import com.ruesga.rview.drawer.NavigationView;
import com.ruesga.rview.model.Account;
import com.ruesga.rview.preferences.Preferences;
import com.ruesga.rview.wizards.SetupAccountActivity;

public class MainActivity extends AppCompatActivity {

    private static final int INVALID_ITEM = -1;

    private static final int REQUEST_WIZARD = 1;

    private static final int MESSAGE_NAVIGATE_TO = 0;

    public static class Model implements Parcelable {
        public String accountName;
        public String accountRepository;
        public boolean isAccountExpanded;
        public int currentNavigationItemId = INVALID_ITEM;

        public Model() {
        }

        protected Model(Parcel in) {
            accountName = in.readString();
            accountRepository = in.readString();
            isAccountExpanded = in.readByte() != 0;
            currentNavigationItemId = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(accountName);
            dest.writeString(accountRepository);
            dest.writeByte((byte) (isAccountExpanded ? 1 : 0));
            dest.writeInt(currentNavigationItemId);
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

    public static class EventHandlers {
        private final MainActivity mActivity;

        public EventHandlers(MainActivity activity) {
            mActivity = activity;
        }

        public void onSwitcherPressed(View v) {
            mActivity.performShowAccount(!mActivity.mModel.isAccountExpanded);
        }
    }

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

    private ActivityMainBinding mBinding;
    private NavigationHeaderBinding mHeaderDrawerBinding;

    private Model mModel;
    private final EventHandlers mEventHandlers = new EventHandlers(this);

    private Handler mUiHandler;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        launchAddGerritAccountIfNeeded();
        mUiHandler = new Handler(mMessenger);

        onRestoreInstanceState(savedInstanceState);

        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        if (mModel == null) {
            mModel = new Model();
        }
// FIXME Remove
mModel.accountName = "Anonymous Coward";
mModel.accountRepository = "CyanogenMod";

        setupToolbar();
        setupNavigationHeader();

        mBinding.setModel(mModel);
        mBinding.setHandlers(mEventHandlers);
        mHeaderDrawerBinding.setModel(mModel);
        mHeaderDrawerBinding.setHandlers(mEventHandlers);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedState) {
        if (savedState != null) {
            super.onRestoreInstanceState(savedState);
            mModel = savedState.getParcelable(getClass().getSimpleName() + "_model");
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(getClass().getSimpleName() + "_model", mModel);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_WIZARD) {
            if (resultCode == RESULT_OK) {
                // Save the account
                Account account = data.getParcelableExtra(SetupAccountActivity.EXTRA_ACCOUNT);
                Preferences.addAccount(this, account);
                Preferences.setAccount(this, account);
                selectAccount(account);
            } else {
                // If we don't have account, then close the activity.
                // Otherwise, do nothing
                if (Preferences.getAccount(this) == null) {
                    finish();
                }
            }
        }
    }

    private void setupToolbar() {
        setSupportActionBar(mBinding.pageContentLayout.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
            if (mBinding.drawerLayout != null) {
                ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(
                        this, mBinding.drawerLayout, mBinding.pageContentLayout.toolbar, 0, 0);
                mBinding.drawerLayout.addDrawerListener(drawerToggle);
                drawerToggle.syncState();
            }
        }
    }

    private void setupNavigationHeader() {
        // Bind the header, in code rather than in layout so we can have access to
        // binding variables
        mHeaderDrawerBinding = DataBindingUtil.inflate(LayoutInflater.from(this),
                R.layout.navigation_header, mBinding.drawerNavigationView, false);
        mBinding.drawerNavigationView.addHeaderView(mHeaderDrawerBinding.getRoot());
        performShowAccount(mModel.isAccountExpanded);

        // Listen for click events and select the current one
        mBinding.drawerNavigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                requestNavigateTo(item.getItemId());
                return true;
            }
        });
        requestNavigateTo(mModel.currentNavigationItemId == INVALID_ITEM
                ? R.id.menu_open : mModel.currentNavigationItemId);
    }

    private void performShowAccount(boolean show) {
        final Menu menu = mBinding.drawerNavigationView.getMenu();
        menu.setGroupVisible(R.id.category_all, !show);
        menu.setGroupVisible(R.id.category_my_menu, !show);
        menu.setGroupVisible(R.id.category_my_account, show);
        menu.setGroupVisible(R.id.category_other_accounts, show);
        mModel.isAccountExpanded = show;
        mHeaderDrawerBinding.setModel(mModel);

        // Sync the current selection status
        if (mModel.currentNavigationItemId != -1) {
            performSelectItem(mModel.currentNavigationItemId, true);
        }
    }

    private boolean performSelectItem(int itemId, boolean force) {
        final Menu menu = mBinding.drawerNavigationView.getMenu();
        final MenuItem item = menu.findItem(itemId);
        if (item != null) {
            boolean navigate = force || !item.isCheckable()
                    || itemId != mModel.currentNavigationItemId;
            if (item.isCheckable()) {
                mBinding.drawerNavigationView.setCheckedItem(item.getItemId());
            }
            mModel.currentNavigationItemId = itemId;
            return navigate;
        }
        return false;
    }

    private void requestNavigateTo(int itemId) {
        // Select the item
        final boolean navigate = performSelectItem(itemId, false);

        // Close the drawer
        if (mBinding.drawerLayout != null) {
            if (mBinding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                mBinding.drawerLayout.closeDrawer(GravityCompat.START, true);
            }
        }

        if (navigate) {
            Message.obtain(mUiHandler, MESSAGE_NAVIGATE_TO).sendToTarget();
        }
    }

    private void performNavigateTo() {
        final Menu menu = mBinding.drawerNavigationView.getMenu();
        final MenuItem item = menu.findItem(mModel.currentNavigationItemId);
        if (item == null) {
            return;
        }

        switch (item.getItemId()) {
            case R.id.menu_account_settings:
                break;
            case R.id.menu_delete_account:
                break;
            case R.id.menu_add_account:
                Intent i = new Intent(this, SetupAccountActivity.class);
                startActivityForResult(i, REQUEST_WIZARD);
                break;

            default:
                if (item.getGroupId() == R.id.category_other_accounts) {
                    // TODO Choose account
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

    private void selectAccount(Account account) {
        mModel.accountName = account.getAccountDisplayName();
        mModel.accountRepository = account.getRepositoryDisplayName();
        mHeaderDrawerBinding.setModel(mModel);
    }

    private boolean launchAddGerritAccountIfNeeded() {
// FIXME Uncomment
//        if (Preferences.getAccount(this) == null) {
//            Intent i = new Intent(this, SetupAccountActivity.class);
//            startActivityForResult(i, REQUEST_WIZARD);
//            return true;
//        }
        return false;
    }
}
