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
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.text.TextUtils;
import android.widget.Toast;

import com.ruesga.rview.databinding.ContentBinding;
import com.ruesga.rview.fragments.ChangeDetailsFragment;
import com.ruesga.rview.gerrit.GerritApi;
import com.ruesga.rview.gerrit.filter.ChangeQuery;
import com.ruesga.rview.gerrit.model.ChangeInfo;
import com.ruesga.rview.misc.ModelHelper;
import com.ruesga.rview.misc.NotificationsHelper;
import com.ruesga.rview.misc.StringHelper;
import com.ruesga.rview.model.Account;
import com.ruesga.rview.preferences.Constants;
import com.ruesga.rview.preferences.Preferences;
import com.ruesga.rview.providers.NotificationEntity;

import java.util.List;
import java.util.regex.Pattern;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import me.tatarka.rxloader2.RxLoaderManager;
import me.tatarka.rxloader2.RxLoaderManagerCompat;
import me.tatarka.rxloader2.RxLoaderObserver;
import me.tatarka.rxloader2.safe.SafeObservable;

import static com.ruesga.rview.preferences.Constants.EXTRA_FORCE_SINGLE_PANEL;

public class ChangeDetailsActivity extends BaseActivity {

    private static final String FRAGMENT_TAG = "details";

    private final RxLoaderObserver<ChangeInfo> mChangeObserver = new RxLoaderObserver<ChangeInfo>() {
                @Override
                public void onNext(ChangeInfo change) {
                    performShowChange(null, change.legacyChangeId, change.changeId);
                }

                @Override
                public void onError(Throwable error) {
                    notifyInvalidArgsAndFinish();
                }
            };

    private ContentBinding mBinding;

    @SuppressWarnings("Convert2streamapi")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBinding = DataBindingUtil.setContentView(this, R.layout.content);

        // Check we have valid arguments
        if (getIntent() == null) {
            finish();
            return;
        }

        boolean forceSinglePanel = getIntent().getBooleanExtra(EXTRA_FORCE_SINGLE_PANEL, false);
        boolean isTwoPanel = getResources().getBoolean(R.bool.config_is_two_pane);
        if (!forceSinglePanel & isTwoPanel) {
            // Tablets have a two panel layout in landscape, so finish the current activity
            // to show the change in the proper activity
            finish();
            return;
        }

        // Force single panel?
        if (forceSinglePanel) {
            setUseTwoPanel(false);
            setForceSinglePanel(true);
        }

        // Setup the title
        setupActivity();

        if (getIntent().getData() != null) {
            Account account = Preferences.getAccount(this);
            if (account == null) {
                // Not ready to handle it
                notifyInvalidArgsAndFinish();
                return;
            }

            // Check scheme
            Uri data = getIntent().getData();
            String scheme = data.getScheme();
            if (!scheme.equals(getPackageName())) {
                notifyInvalidArgsAndFinish();
                return;
            }

            // Retrieve the host and the request id
            String host = data.getHost();
            String query = StringHelper.getSafeLastPathSegment(data);
            if (TextUtils.isEmpty(query)) {
                notifyInvalidArgsAndFinish();
                return;
            }

            ChangeQuery filter;
            switch (host) {
                case Constants.CUSTOM_URI_CHANGE:
                case Constants.CUSTOM_URI_CHANGE_ID:
                    Pattern pattern = host.equals(Constants.CUSTOM_URI_CHANGE)
                            ? StringHelper.GERRIT_CHANGE : StringHelper.GERRIT_CHANGE_ID;
                    if (!pattern.matcher(query).matches()) {
                        notifyInvalidArgsAndFinish();
                        return;
                    }
                    filter = new ChangeQuery().change(query);
                    performGatherChangeId(filter);
                    break;

                case Constants.CUSTOM_URI_COMMIT:
                    if (!StringHelper.GERRIT_COMMIT.matcher(query).matches()) {
                        notifyInvalidArgsAndFinish();
                        return;
                    }
                    filter = new ChangeQuery().commit(query);
                    performGatherChangeId(filter);
                    break;

                default:
                    try {
                        int legacyChangeId = Integer.valueOf(query);
                        filter = new ChangeQuery().change(String.valueOf(legacyChangeId));
                        performGatherChangeId(filter);
                        return;
                    } catch (NumberFormatException ex) {
                        // Ignore. Not a valid change-id
                    }

                    Toast.makeText(this, R.string.exception_cannot_handle_link,
                            Toast.LENGTH_SHORT).show();
                    finish();
                    break;
            }
        } else {
            // Set the account if requested
            String accountId = getIntent().getStringExtra(Constants.EXTRA_ACCOUNT_HASH);
            if (!TextUtils.isEmpty(accountId)) {
                Preferences.setAccount(this, ModelHelper.getAccountFromHash(this, accountId));
            }

            // Open the change directly
            int legacyChangeId = getIntent().getIntExtra(Constants.EXTRA_LEGACY_CHANGE_ID, -1);
            if (legacyChangeId == -1) {
                finish();
                return;
            }
            String changeId = getIntent().getStringExtra(Constants.EXTRA_CHANGE_ID);
            performShowChange(savedInstanceState, legacyChangeId, changeId);
        }
    }

    private void performGatherChangeId(ChangeQuery filter) {
        RxLoaderManager loaderManager = RxLoaderManagerCompat.get(this);
        loaderManager.create("fetch", this::fetchChangeId, mChangeObserver).start(filter);
    }

    private void performShowChange(Bundle savedInstanceState, int legacyChangeId, String changeId) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getString(R.string.change_details_title, legacyChangeId));
            getSupportActionBar().setSubtitle(changeId);
        }

        FragmentTransaction tx = getSupportFragmentManager().beginTransaction()
                .setAllowOptimization(false);
        Fragment fragment;
        if (savedInstanceState != null) {
            fragment = getSupportFragmentManager().getFragment(savedInstanceState, FRAGMENT_TAG);
        } else {
            fragment = ChangeDetailsFragment.newInstance(legacyChangeId);
        }
        tx.replace(R.id.content, fragment, FRAGMENT_TAG).commit();

        // Dismiss notifications associated to this change
        Account account = Preferences.getAccount(this);
        if (account != null && changeId != null) {
            int groupId = NotificationsHelper.generateGroupId(account, changeId);
            NotificationsHelper.dismissNotification(this, groupId);
            NotificationEntity.markGroupNotificationsAsRead(this, groupId);
            NotificationEntity.dismissGroupNotifications(this, groupId);
        }
    }

    @SuppressWarnings("ConstantConditions")
    private Observable<ChangeInfo> fetchChangeId(ChangeQuery query) {
        final GerritApi api = ModelHelper.getGerritApi(this);
        return SafeObservable.fromNullCallable(() -> {
                List<ChangeInfo> changes = api.getChanges(query, 1, 0, null).blockingFirst();
                if (changes != null && !changes.isEmpty()) {
                    return changes.get(0);
                }
                return null;
            })
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread());
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        //Save the fragment's instance
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG);
        if (fragment != null) {
            getSupportFragmentManager().putFragment(outState, FRAGMENT_TAG, fragment);
        }
    }

    @Override
    public DrawerLayout getDrawerLayout() {
        return null;
    }

    @Override
    public ContentBinding getContentBinding() {
        return mBinding;
    }

    private void notifyInvalidArgsAndFinish() {
        Toast.makeText(this, getString(
                R.string.exception_cannot_handle_link, getIntent().getData().toString()),
                Toast.LENGTH_SHORT).show();
        finish();
    }
}
