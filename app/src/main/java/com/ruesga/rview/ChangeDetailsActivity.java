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

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import com.ruesga.rview.databinding.ContentBinding;
import com.ruesga.rview.fragments.ChangeDetailsFragment;
import com.ruesga.rview.gerrit.GerritApi;
import com.ruesga.rview.gerrit.filter.ChangeQuery;
import com.ruesga.rview.gerrit.model.ChangeInfo;
import com.ruesga.rview.gerrit.model.ChangeOptions;
import com.ruesga.rview.misc.ActivityHelper;
import com.ruesga.rview.misc.CacheHelper;
import com.ruesga.rview.misc.ModelHelper;
import com.ruesga.rview.misc.NotificationsHelper;
import com.ruesga.rview.misc.StringHelper;
import com.ruesga.rview.misc.UriHelper;
import com.ruesga.rview.model.Account;
import com.ruesga.rview.preferences.Constants;
import com.ruesga.rview.preferences.Preferences;
import com.ruesga.rview.providers.NotificationEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
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

    private static final List<ChangeOptions> OPTIONS = new ArrayList<ChangeOptions>() {{
        add(ChangeOptions.ALL_REVISIONS);
        add(ChangeOptions.ALL_FILES);
    }};

    private static class ChangeDetailsRequest {
        private ChangeQuery mFilter;
        private String[] mRevAndBase;
        private String mFile;
    }
    private static class ChangeDetailsResponse {
        private ChangeInfo mChange;
        private String[] mRevAndBase;
        private String mFile;
    }

    private final RxLoaderObserver<ChangeDetailsResponse> mChangeObserver
            = new RxLoaderObserver<ChangeDetailsResponse>() {
        @Override
        public void onNext(ChangeDetailsResponse result) {
            if (result != null) {
                // Clear the cache
                CacheHelper.removeAccountDiffCacheDir(ChangeDetailsActivity.this);

                final ChangeInfo change = result.mChange;
                final int legacyChangeId = change.legacyChangeId;
                final String changeId = change.changeId;

                final String base;
                final String baseRevisionId;
                final String revisionId;
                if (result.mRevAndBase == null) {
                    base = baseRevisionId = revisionId = null;
                } else {
                    base = result.mRevAndBase[0];
                    baseRevisionId = obtainRevisionId(change, result.mRevAndBase[0]);
                    revisionId = obtainRevisionId(change, result.mRevAndBase[1]);
                }

                if (TextUtils.isEmpty(result.mFile)) {
                    // Open the details view
                    performShowChange(null, legacyChangeId, changeId, revisionId, baseRevisionId);
                } else {
                    // Directly open the diff view
                    performShowDiffFile(change, base, revisionId, result.mFile);
                }
            } else {
                // Not ready to handle it
                notifyInvalidArgsAndFinish();
            }
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
                    Pattern pattern = StringHelper.GERRIT_CHANGE;
                    if (!pattern.matcher(query).matches()) {
                        notifyInvalidArgsAndFinish();
                        return;
                    }
                    filter = new ChangeQuery().change(query);
                    performGatherChangeId(filter);
                    break;
                case Constants.CUSTOM_URI_CHANGE_ID:
                    pattern = StringHelper.GERRIT_CHANGE_ID;
                    String[] q = query.split(UriHelper.CUSTOM_URI_TOKENIZER);
                    if (!pattern.matcher(q[0]).matches()) {
                        notifyInvalidArgsAndFinish();
                        return;
                    }
                    filter = new ChangeQuery().change(q[0]);
                    String file = rebuildFileInfo(q);
                    String[] revAndBase = extractRevisionAndBase(q);
                    performGatherChangeId(filter, revAndBase, file);
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

                    notifyInvalidArgsAndFinish();
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
            performShowChange(savedInstanceState, legacyChangeId, changeId, null, null);
        }
    }

    private void performGatherChangeId(ChangeQuery filter) {
        performGatherChangeId(filter, null, null);
    }

    private void performGatherChangeId(ChangeQuery filter, String[] revAndBase, String file) {
        ChangeDetailsRequest request = new ChangeDetailsRequest();
        request.mFilter = filter;
        request.mRevAndBase = revAndBase;
        request.mFile = file;

        RxLoaderManager loaderManager = RxLoaderManagerCompat.get(this);
        loaderManager.create("fetch", this::fetchChangeId, mChangeObserver).start(request);
    }

    private void performShowChange(Bundle savedInstanceState, int legacyChangeId,
            String changeId, String currentRevision, String base) {
        if (hasStateSaved()) {
            return;
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getString(R.string.change_details_title, legacyChangeId));
            getSupportActionBar().setSubtitle(changeId);
        }

        FragmentTransaction tx = getSupportFragmentManager().beginTransaction()
                .setReorderingAllowed(false);
        Fragment fragment;
        if (savedInstanceState != null) {
            fragment = getSupportFragmentManager().getFragment(savedInstanceState, FRAGMENT_TAG);
        } else {
            fragment = ChangeDetailsFragment.newInstance(legacyChangeId, currentRevision, base);
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

    private void performShowDiffFile(
            ChangeInfo change, String base, String revisionId, String file) {
        if (hasStateSaved()) {
            return;
        }
        String current = String.valueOf(change.revisions.get(revisionId).number);
        ActivityHelper.openDiffViewerActivity(
                this, change, /*files*/null, /*info*/null, revisionId, base, current, file, null, 0);
        finish();
    }

    @SuppressWarnings("ConstantConditions")
    private Observable<ChangeDetailsResponse> fetchChangeId(ChangeDetailsRequest request) {
        final GerritApi api = ModelHelper.getGerritApi(this);
        return SafeObservable.fromNullCallable(() -> {
                List<ChangeInfo> changes = api.getChanges(
                        request.mFilter, 1, 0, OPTIONS).blockingFirst();
                if (changes != null && !changes.isEmpty()) {
                    ChangeDetailsResponse result = new ChangeDetailsResponse();
                    result.mChange = changes.get(0);
                    result.mRevAndBase = request.mRevAndBase;
                    result.mFile = request.mFile;
                    return result;
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
                R.string.exception_cannot_handle_link, String.valueOf(getIntent().getData())),
                Toast.LENGTH_SHORT).show();
        ActivityHelper.performFinishActivity(this, false);
    }

    private String[] extractRevisionAndBase(String[] tokens) {
        String[] ret = new String[2];
        if (tokens.length >= 2) {
            String q = tokens[1];
            if (q.contains("..")) {
                ret = q.split("\\.\\.");
            } else {
                ret[1] = q;
            }
        }
        return ret;
    }

    private String rebuildFileInfo(String[] tokens) {
        StringBuilder sb = new StringBuilder();
        int count = tokens.length;
        if (count >= 3) {
            for (int i = 2; i < count; i++) {
                sb.append(tokens[i]);
                if (i < (count - 1)) {
                    sb.append("/");
                }
            }
        }
        return sb.toString();
    }

    private String obtainRevisionId(ChangeInfo change, String revision) {
        String revisionId = null;
        if (!TextUtils.isEmpty(revision) && change.revisions != null) {
            for (String rev : change.revisions.keySet()) {
                if (change.revisions.containsKey(rev) &&
                        String.valueOf(change.revisions.get(rev).number).equals(revision)
                        || rev.equals(revision)) {
                    revisionId = rev;
                    break;
                }
            }
        }
        return revisionId;
    }

}
