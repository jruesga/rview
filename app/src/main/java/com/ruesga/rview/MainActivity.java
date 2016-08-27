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
import android.support.annotation.StringRes;
import android.support.design.internal.NavigationMenu;
import android.support.design.internal.NavigationSubMenu;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;

import com.airbnb.rxgroups.ObservableManager;
import com.ruesga.rview.annotations.ProguardIgnored;
import com.ruesga.rview.databinding.ActivityMainBinding;
import com.ruesga.rview.databinding.NavigationHeaderBinding;
import com.ruesga.rview.fragments.ChangesFragment;
import com.ruesga.rview.fragments.ExceptionHandler;
import com.ruesga.rview.fragments.ObservableManagerProvider;
import com.ruesga.rview.fragments.UiInteractor;
import com.ruesga.rview.misc.AndroidHelper;
import com.ruesga.rview.misc.CacheHelper;
import com.ruesga.rview.misc.ExceptionHelper;
import com.ruesga.rview.model.Account;
import com.ruesga.rview.preferences.Preferences;
import com.ruesga.rview.wizards.SetupAccountActivity;

import java.util.List;

public class MainActivity extends AppCompatActivity
        implements ObservableManagerProvider, ExceptionHandler, UiInteractor {

    private static final int INVALID_ITEM = -1;

    private static final int REQUEST_WIZARD = 1;

    private static final int MESSAGE_NAVIGATE_TO = 0;
    private static final int MESSAGE_DELETE_ACCOUNT = 1;

    private static final int OTHER_ACCOUNTS_GROUP_BASE_ID = 100;

    private static final int DEFAULT_MENU = R.id.menu_open;

    @ProguardIgnored
    public static class Model implements Parcelable {
        public String accountName;
        public String accountRepository;
        public boolean isAccountExpanded;
        public int currentNavigationItemId = INVALID_ITEM;
        public boolean isInProgress = false;

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

    private ActivityMainBinding mBinding;
    private NavigationHeaderBinding mHeaderDrawerBinding;

    private Model mModel;
    private final EventHandlers mEventHandlers = new EventHandlers(this);

    private Account mAccount;
    private List<Account> mAccounts;

    private Handler mUiHandler;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mUiHandler = new Handler(mMessenger);

        onRestoreInstanceState(savedInstanceState);

        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        if (mModel == null) {
            mModel = new Model();
        }

        loadAccounts();
        launchAddAccountIfNeeded();
        setupToolbar();
        setupNavigationDrawer();

        mBinding.setModel(mModel);
        mBinding.setHandlers(mEventHandlers);
        mHeaderDrawerBinding.setModel(mModel);
        mHeaderDrawerBinding.setHandlers(mEventHandlers);


        // Navigate to current item
        requestNavigateTo(mModel.currentNavigationItemId == INVALID_ITEM
                ? DEFAULT_MENU : mModel.currentNavigationItemId);
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
        }
    }

    @Override
    public ObservableManager getObservableManager() {
        return ((RviewApplication) getApplication()).observableManager();
    }

    @Override
    public void handleException(String tag, Throwable cause) {
        showError(ExceptionHelper.exceptionToMessage(this, tag, cause));
    }

    @Override
    public void changeInProgressStatus(boolean status) {
        mModel.isInProgress = status;
        mBinding.setModel(mModel);
    }

    private void loadAccounts() {
        mAccount = Preferences.getAccount(this);
        mAccounts = Preferences.getAccounts(this);
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
        if (item == null || mAccount == null) {
            return;
        }

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

            case R.id.menu_dashboard:
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
                    return;
                }

                // Is a filter menu?
                String filter = getQueryFilterExpressionFromMenuItemOrder(item.getOrder());
                if (filter != null) {
                    openFilterFragment(item.getTitle(), filter);
                }

                break;
        }

        // Hide account info
        performShowAccount(false);
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

        // Refresh the ui
        updateCurrentAccountDrawerInfo();
        updateAccountsDrawerInfo();

        // And navigate to the default menu
        requestNavigateTo(DEFAULT_MENU);
    }

    private void requestAccountDeletion() {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.account_deletion_title)
                .setMessage(R.string.account_deletion_message)
                .setPositiveButton(R.string.action_delete, (dialogInterface, i) -> {
                    Message.obtain(mUiHandler, MESSAGE_DELETE_ACCOUNT).sendToTarget();
                })
                .create();
        dialog.show();
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
        Preferences.removeAccountPreferences(this, mAccount);
        CacheHelper.removeAccountCacheDir(this, mAccount);
        mAccount = null;

        // Show message
        Snackbar.make(mBinding.pageContentLayout.getRoot(),
                R.string.account_deletion_confirm_message, Snackbar.LENGTH_SHORT).show();

        performAccountSwitch();

        // Have any account? No, then launch setup account wizard
        launchAddAccountIfNeeded();
    }

    private void openAccountSettings() {
        if (mAccount != null) {
            // FIXME Open account settings activity
        }
    }

    private void openFilterFragment(CharSequence title, String filter) {
        // Setup the title
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
            getSupportActionBar().setSubtitle(filter);
        }

        // Open the filter fragment
        FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
        Fragment oldFragment = getSupportFragmentManager().findFragmentByTag("list");
        if (oldFragment != null) {
            tx.remove(oldFragment);
        }
        Fragment newFragment = ChangesFragment.newInstance(filter);
        tx.replace(R.id.content, newFragment, "list").commit();
    }

    private boolean launchAddAccountIfNeeded() {
        if (mAccount == null) {
            Intent i = new Intent(this, SetupAccountActivity.class);
            startActivityForResult(i, REQUEST_WIZARD);
            return true;
        }
        return false;
    }

    private String getQueryFilterExpressionFromMenuItemOrder(int order) {
        int[] orders = getResources().getIntArray(R.array.query_filters_orders);
        String[] filters = getResources().getStringArray(R.array.query_filters_values);

        int count = orders.length;
        for (int i = 0; i < count; i++) {
            if (orders[i] == order) {
                return filters[i];
            }
        }
        return null;
    }

    private void showError(@StringRes int message) {
        AndroidHelper.showErrorSnackbar(this, mBinding.pageContentLayout.getRoot(), message);
    }

    private void showWarning(@StringRes int message) {
        AndroidHelper.showWarningSnackbar(this, mBinding.pageContentLayout.getRoot(), message);
    }
}