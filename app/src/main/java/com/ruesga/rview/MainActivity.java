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
import android.support.annotation.Nullable;
import android.support.design.internal.NavigationMenu;
import android.support.design.internal.NavigationSubMenu;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;

import com.ruesga.rview.annotations.ProguardIgnored;
import com.ruesga.rview.databinding.ContentBinding;
import com.ruesga.rview.databinding.NavigationHeaderBinding;
import com.ruesga.rview.fragments.ChangeListByFilterFragment;
import com.ruesga.rview.fragments.DashboardFragment;
import com.ruesga.rview.gerrit.model.ChangeInfo;
import com.ruesga.rview.misc.CacheHelper;
import com.ruesga.rview.misc.Formatter;
import com.ruesga.rview.misc.PicassoHelper;
import com.ruesga.rview.model.Account;
import com.ruesga.rview.preferences.Preferences;
import com.ruesga.rview.wizards.SetupAccountActivity;

import java.util.List;

public class MainActivity extends ChangeListBaseActivity {

    private static final int REQUEST_WIZARD = 1;
    private static final int REQUEST_ACCOUNT_SETTINGS = 2;

    private static final int MESSAGE_NAVIGATE_TO = 0;
    private static final int MESSAGE_DELETE_ACCOUNT = 1;

    private static final int OTHER_ACCOUNTS_GROUP_BASE_ID = 100;

    @ProguardIgnored
    public static class Model implements Parcelable {
        public String accountName;
        public String accountRepository;
        public boolean isAccountExpanded;

        int currentNavigationItemId = INVALID_ITEM;
        String filterName;
        String filterQuery;
        int selectedChangeId = INVALID_ITEM;

        public Model() {
        }

        protected Model(Parcel in) {
            accountName = in.readString();
            accountRepository = in.readString();
            isAccountExpanded = in.readByte() != 0;
            currentNavigationItemId = in.readInt();
            if (in.readByte() == 1) {
                filterName = in.readString();
            }
            if (in.readByte() == 1) {
                filterQuery = in.readString();
            }
            selectedChangeId = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(accountName);
            dest.writeString(accountRepository);
            dest.writeByte((byte) (isAccountExpanded ? 1 : 0));
            dest.writeInt(currentNavigationItemId);
            dest.writeByte((byte) (filterName != null ? 1 : 0));
            dest.writeString(filterName);
            dest.writeByte((byte) (filterQuery != null ? 1 : 0));
            dest.writeString(filterQuery);
            dest.writeInt(selectedChangeId);
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

    @ProguardIgnored
    @SuppressWarnings({"UnusedParameters", "unused"})
    public static class EventHandlers {
        private final MainActivity mActivity;

        public EventHandlers(MainActivity activity) {
            mActivity = activity;
        }

        public void onSwitcherPressed(View v) {
            mActivity.performShowAccount(!mActivity.mModel.isAccountExpanded);
        }
    }

    private final Handler.Callback mMessenger = message -> {
        if (message.what == MESSAGE_NAVIGATE_TO) {
            performNavigateTo();
            return true;
        }
        if (message.what == MESSAGE_DELETE_ACCOUNT) {
            performDeleteAccount();
            return true;
        }
        return false;
    };

    private ContentBinding mBinding;
    private NavigationHeaderBinding mHeaderDrawerBinding;

    private Model mModel;
    private final EventHandlers mEventHandlers = new EventHandlers(this);

    private Account mAccount;
    private List<Account> mAccounts;

    private Handler mUiHandler;

    private boolean mIsTwoPane;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        mUiHandler = new Handler(mMessenger);
        mIsTwoPane = getResources().getBoolean(R.bool.config_is_two_pane);

        onRestoreInstanceState(savedInstanceState);

        super.onCreate(savedInstanceState);

        mBinding = DataBindingUtil.setContentView(this, R.layout.content);
        if (mModel == null) {
            mModel = new Model();
        }

        setupActivity();
        loadAccounts();
        launchAddAccountIfNeeded();
        setupNavigationDrawer();

        mHeaderDrawerBinding.setModel(mModel);
        mHeaderDrawerBinding.setHandlers(mEventHandlers);

        if (getSupportActionBar() != null) {
            if (mModel.filterName != null) {
                getSupportActionBar().setTitle(mModel.filterName);
                if (mModel.filterQuery != null) {
                    getSupportActionBar().setSubtitle(mModel.filterQuery);
                }
            }
        }

        int defaultMenu = Preferences.getAccountHomePageId(this, mAccount);
        if (savedInstanceState != null) {
            Fragment detailsFragment = getSupportFragmentManager().getFragment(
                    savedInstanceState, FRAGMENT_TAG_DETAILS);
            Fragment listFragment = getSupportFragmentManager().getFragment(
                    savedInstanceState, FRAGMENT_TAG_LIST);
            if (listFragment != null) {
                FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
                tx.replace(R.id.content, listFragment, FRAGMENT_TAG_LIST);
                if (detailsFragment != null) {
                    tx.replace(R.id.details, detailsFragment, FRAGMENT_TAG_DETAILS);
                }
                tx.commit();

            } else {
                // Navigate to current item
                requestNavigateTo(mModel.currentNavigationItemId == INVALID_ITEM
                        ? defaultMenu : mModel.currentNavigationItemId);
            }
        } else {
            // Navigate to current item
            requestNavigateTo(mModel.currentNavigationItemId == INVALID_ITEM
                    ? defaultMenu : mModel.currentNavigationItemId);
        }
    }

    @Override
    public int getSelectedChangeId() {
        return mModel.selectedChangeId;
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

        //Save the fragment's instance
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG_LIST);
        if (fragment != null) {
            getSupportFragmentManager().putFragment(outState, FRAGMENT_TAG_LIST, fragment);
        }
        fragment = getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG_DETAILS);
        if (fragment != null) {
            getSupportFragmentManager().putFragment(outState, FRAGMENT_TAG_DETAILS, fragment);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_WIZARD) {
            if (resultCode == RESULT_OK) {
                // Save the account
                Account newAccount = data.getParcelableExtra(SetupAccountActivity.EXTRA_ACCOUNT);
                boolean accountExists = false;
                for (Account account : mAccounts) {
                    // Current account
                    if (account.isSameAs(newAccount)) {
                        accountExists = true;
                        break;
                    }
                }

                if (!accountExists) {
                    mAccount = newAccount;
                    mAccounts = Preferences.addAccount(this, mAccount);
                    Preferences.setAccount(this, mAccount);
                    Formatter.refreshCachedPreferences(this);
                    CacheHelper.createAccountCacheDir(this, mAccount);
                } else {
                    showWarning(R.string.account_exists);
                }

                // Switch to the new account
                performAccountSwitch();
            } else {
                // If we don't have account, then close the activity.
                // Otherwise, do nothing
                if (Preferences.getAccount(this) == null) {
                    finish();
                }
            }
        } else if (requestCode == REQUEST_ACCOUNT_SETTINGS) {
            // Refresh current view
            if (mModel.currentNavigationItemId == INVALID_ITEM) {
                mModel.currentNavigationItemId = Preferences.getAccountHomePageId(this, mAccount);
            }
            Formatter.refreshCachedPreferences(this);
            performNavigateTo();
        }
    }

    @Override
    public ContentBinding getContentBinding() {
        return mBinding;
    }

    @Override
    public DrawerLayout getDrawerLayout() {
        return mBinding.drawerLayout;
    }

    private void loadAccounts() {
        mAccount = Preferences.getAccount(this);
        mAccounts = Preferences.getAccounts(this);
        if (mAccount != null) {
            Formatter.refreshCachedPreferences(this);
        }
    }

    private void setupNavigationDrawer() {
        // Bind the header, in code rather than in layout so we can have access to
        // binding variables
        mHeaderDrawerBinding = DataBindingUtil.inflate(LayoutInflater.from(this),
                R.layout.navigation_header, mBinding.drawerNavigationView, false);
        mBinding.drawerNavigationView.addHeaderView(mHeaderDrawerBinding.getRoot());
        performShowAccount(mModel.isAccountExpanded);

        // Listen for click events and select the current one
        mBinding.drawerNavigationView.setNavigationItemSelectedListener(item -> {
            requestNavigateTo(item.getItemId());
            return true;
        });

        // Update the accounts and current account information
        if (mAccount != null) {
            updateCurrentAccountDrawerInfo();
            updateAccountsDrawerInfo();
        }
    }

    private void performShowAccount(boolean show) {
        final Menu menu = mBinding.drawerNavigationView.getMenu();
        menu.setGroupVisible(R.id.category_all, !show);
        menu.setGroupVisible(R.id.category_my_menu, !show &&
                (mAccount != null && mAccount.hasAuthenticatedAccessMode()));
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
            if (item.isCheckable()) {
                boolean changed = itemId != mModel.currentNavigationItemId;
                mBinding.drawerNavigationView.setCheckedItem(item.getItemId());
                mModel.currentNavigationItemId = itemId;
                return force || changed;
            } else {
                performNavigationAction(item);
            }
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

    private void performNavigationAction(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_account_settings:
                openAccountSettings();
                break;
            case R.id.menu_delete_account:
                requestAccountDeletion();
                break;
            case R.id.menu_add_account:
                Intent i = new Intent(this, SetupAccountActivity.class);
                startActivityForResult(i, REQUEST_WIZARD);
                break;
            default:
                // Other accounts group?
                if (item.getGroupId() == R.id.category_other_accounts) {
                    int accountIndex = item.getItemId() - OTHER_ACCOUNTS_GROUP_BASE_ID;
                    Account account = mAccounts.get(accountIndex);
                    if (account != null) {
                        mAccount = account;
                        performAccountSwitch();
                    }
                }
                break;
        }
    }

    private void performNavigateTo() {
        final Menu menu = mBinding.drawerNavigationView.getMenu();
        final MenuItem item = menu.findItem(mModel.currentNavigationItemId);
        if (item == null || mAccount == null) {
            return;
        }

        switch (item.getItemId()) {
            case R.id.menu_dashboard:
                openDashboardFragment();
                break;

            default:
                // Is a filter menu?
                mModel.filterQuery = getQueryFilterExpressionFromMenuItemId(item.getItemId());
                if (mModel.filterQuery != null) {
                    mModel.filterName = item.getTitle().toString();
                    openFilterFragment(mModel.filterName, mModel.filterQuery);
                }
                break;
        }
    }

    private void updateAccountsDrawerInfo() {
        // Remove all accounts and re-add them
        final NavigationMenu menu = (NavigationMenu) mBinding.drawerNavigationView.getMenu();
        int otherAccountGroupIndex = menu.findGroupIndex(R.id.category_other_accounts);
        MenuItem group = menu.getItem(otherAccountGroupIndex);
        SubMenu otherAccountsSubMenu = group.getSubMenu();
        int count = otherAccountsSubMenu.size() - 1;
        for (int i = count; i > 0; i--) {
            ((NavigationSubMenu) otherAccountsSubMenu).removeItemAt(i);
        }
        int i = 0;
        for (Account account : mAccounts) {
            // Current account
            if (mAccount.isSameAs(account)) {
                i++;
                continue;
            }

            int id = OTHER_ACCOUNTS_GROUP_BASE_ID + i;
            String title = account.getAccountDisplayName();
            MenuItem item = otherAccountsSubMenu.add(group.getGroupId(), id, Menu.NONE, title);
            item.setIcon(R.drawable.ic_account_circle);
            i++;
        }
    }

    private void updateCurrentAccountDrawerInfo() {
        mModel.accountName = mAccount.getAccountDisplayName();
        mModel.accountRepository = mAccount.getRepositoryDisplayName();
        mHeaderDrawerBinding.setModel(mModel);

        PicassoHelper.bindAvatar(this, PicassoHelper.getPicassoClient(this),
                mAccount.mAccount,
                mHeaderDrawerBinding.accountAvatar,
                PicassoHelper.getDefaultAvatar(this, android.R.color.white));
    }

    private void performAccountSwitch() {
        // Check that we are in a valid status before update the ui
        if (mAccount == null) {
            if (mAccounts.size() == 0) {
                return;
            }

            // Use the first one account
            mAccount = mAccounts.get(0);
        }
        Preferences.setAccount(this, mAccount);
        Formatter.refreshCachedPreferences(this);

        // Refresh the ui
        updateCurrentAccountDrawerInfo();
        updateAccountsDrawerInfo();
        performShowAccount(false);

        // And navigate to the default menu
        requestNavigateTo(Preferences.getAccountHomePageId(this, mAccount));
    }

    private void requestAccountDeletion() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.account_deletion_title)
                .setMessage(R.string.account_deletion_message)
                .setPositiveButton(R.string.action_delete, (dialogInterface, i) ->
                        Message.obtain(mUiHandler, MESSAGE_DELETE_ACCOUNT).sendToTarget())
                .setNegativeButton(R.string.action_cancel, null)
                .create();
        dialog.show();
        performShowAccount(false);
    }

    private void performDeleteAccount() {
        // Remove the account
        boolean accountDeleted = false;
        int count = mAccounts.size();
        for (int i = 0; i < count; i++) {
            Account account = mAccounts.get(i);
            if (account.isSameAs(mAccount)) {
                accountDeleted = true;
                mAccounts.remove(i);
                break;
            }
        }

        if (!accountDeleted) {
            showError(R.string.account_deletion_confirm_message);
            return;
        }

        // Remove the account
        Preferences.removeAccount(this, mAccount);
        Preferences.setAccount(this, null);
        Formatter.refreshCachedPreferences(this);
        Preferences.removeAccountPreferences(this, mAccount);
        CacheHelper.removeAccountCacheDir(this, mAccount);
        mAccount = null;

        // Show message
        Snackbar.make(mBinding.getRoot(), R.string.account_deletion_confirm_message,
                Snackbar.LENGTH_SHORT).show();

        performAccountSwitch();

        // Have any account? No, then launch setup account wizard
        launchAddAccountIfNeeded();
    }

    private void openAccountSettings() {
        if (mAccount != null) {
            Intent i = new Intent(this, AccountSettingsActivity.class);
            startActivityForResult(i, REQUEST_ACCOUNT_SETTINGS);
            performShowAccount(false);
        }
    }

    private void openDashboardFragment() {
        mModel.filterName = getString(R.string.menu_dashboard);
        mModel.filterQuery = null;

        // Setup the title
        invalidateTabs();
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.menu_dashboard);
            getSupportActionBar().setSubtitle(null);
        }

        // Open the dashboard fragment
        mModel.selectedChangeId = INVALID_ITEM;
        FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
        Fragment oldFragment = getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG_LIST);
        if (oldFragment != null) {
            tx.remove(oldFragment);
        }
        oldFragment = getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG_DETAILS);
        if (oldFragment != null) {
            tx.remove(oldFragment);
        }
        Fragment newFragment = DashboardFragment.newInstance();
        tx.replace(R.id.content, newFragment, FRAGMENT_TAG_LIST).commit();
    }

    private void openFilterFragment(CharSequence title, String filter) {
        // Setup the title and tabs
        invalidateTabs();
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
            getSupportActionBar().setSubtitle(filter);
        }

        // Open the filter fragment
        mModel.selectedChangeId = INVALID_ITEM;
        FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
        Fragment oldFragment = getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG_LIST);
        if (oldFragment != null) {
            tx.remove(oldFragment);
        }
        oldFragment = getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG_DETAILS);
        if (oldFragment != null) {
            tx.remove(oldFragment);
        }
        Fragment newFragment = ChangeListByFilterFragment.newInstance(filter);
        tx.replace(R.id.content, newFragment, FRAGMENT_TAG_LIST).commit();
    }

    private boolean launchAddAccountIfNeeded() {
        if (mAccount == null) {
            Intent i = new Intent(this, SetupAccountActivity.class);
            startActivityForResult(i, REQUEST_WIZARD);
            return true;
        }
        return false;
    }

    private String getQueryFilterExpressionFromMenuItemId(int itemId) {
        String[] names = getResources().getStringArray(R.array.query_filters_ids_names);
        String[] filters = getResources().getStringArray(R.array.query_filters_values);

        int count = names.length;
        for (int i = 0; i < count; i++) {
            int id = getResources().getIdentifier(names[i], "id", getPackageName());
            if (itemId == id) {
                return filters[i];
            }
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> void onRefreshEnd(T result) {
        super.onRefreshEnd(result);
        if (result == null) {
            return;
        }

        if (mIsTwoPane && result instanceof List) {
            List<ChangeInfo> changes = (List<ChangeInfo>) result;
            if (!changes.isEmpty() && mModel.selectedChangeId == INVALID_ITEM) {
                onChangeItemPressed(changes.get(0));
            }
        } else if (result instanceof ChangeInfo) {
            mModel.selectedChangeId = ((ChangeInfo) result).legacyChangeId;
        }
    }
}
