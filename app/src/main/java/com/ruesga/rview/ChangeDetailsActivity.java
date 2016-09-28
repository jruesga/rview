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
import com.ruesga.rview.misc.StringHelper;
import com.ruesga.rview.model.Account;
import com.ruesga.rview.preferences.Constants;
import com.ruesga.rview.preferences.Preferences;

import java.util.List;

import me.tatarka.rxloader.RxLoaderManager;
import me.tatarka.rxloader.RxLoaderManagerCompat;
import me.tatarka.rxloader.RxLoaderObserver;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBinding = DataBindingUtil.setContentView(this, R.layout.content);

        boolean isTwoPanel = getResources().getBoolean(R.bool.config_is_two_pane);
        if (isTwoPanel) {
            // Tablets have a two panel layout in landscape, so finish the current activity
            // to show the change in the proper activity
            finish();
            return;
        }

        // Check we have valid arguments
        if (getIntent() == null) {
            finish();
            return;
        }

        // Setup the title
        setupToolbar();

        if (getIntent().getData() != null) {
            Account account = Preferences.getAccount(this);
            if (account == null) {
                // Not ready to handle it
                notifyInvalidArgsAndFinish();
                return;
            }

            Uri data = getIntent().getData();
            String scheme = data.getScheme();
            if (!scheme.equals(getPackageName())
                    && !scheme.equals("http") && !scheme.equals("https")) {
                notifyInvalidArgsAndFinish();
                return;
            }

            // Gather change id
            String host = data.getHost();
            String query = StringHelper.getSafeLastPathSegment(data);
            if (TextUtils.isEmpty(query)) {
                notifyInvalidArgsAndFinish();
                return;
            }

            ChangeQuery filter;
            switch (host) {
                case "change":
                    if (!StringHelper.GERRIT_CHANGE.matcher(query).matches()) {
                        notifyInvalidArgsAndFinish();
                        return;
                    }
                    filter = new ChangeQuery().change(query);
                    performGatherChangeId(filter);
                    break;

                case "commit":
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

        FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
        Fragment fragment;
        if (savedInstanceState != null) {
            fragment = getSupportFragmentManager().getFragment(savedInstanceState, FRAGMENT_TAG);
        } else {
            fragment = ChangeDetailsFragment.newInstance(legacyChangeId);
        }
        tx.replace(R.id.content, fragment, FRAGMENT_TAG).commit();
    }

    @SuppressWarnings("ConstantConditions")
    private Observable<ChangeInfo> fetchChangeId(ChangeQuery query) {
        final GerritApi api = ModelHelper.getGerritApi(this);
        return Observable.fromCallable(() -> {
                    List<ChangeInfo> changes = api.getChanges(query, 1, 0, null).toBlocking().first();
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
