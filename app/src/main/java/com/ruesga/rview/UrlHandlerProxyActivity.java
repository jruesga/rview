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
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.ruesga.rview.gerrit.filter.ChangeQuery;
import com.ruesga.rview.gerrit.filter.antlr.QueryParseException;
import com.ruesga.rview.gerrit.model.DashboardInfo;
import com.ruesga.rview.misc.ActivityHelper;
import com.ruesga.rview.misc.DashboardHelper;
import com.ruesga.rview.misc.StringHelper;
import com.ruesga.rview.misc.UriHelper;
import com.ruesga.rview.model.Account;
import com.ruesga.rview.preferences.Constants;
import com.ruesga.rview.preferences.Preferences;
import com.ruesga.rview.widget.RegExLinkifyTextView;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class UrlHandlerProxyActivity extends AppCompatActivity {

    private static final String TAG = "UrlHandlerProxyActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check we have valid arguments
        if (getIntent() == null || getIntent().getData() == null) {
            finish();
            return;
        }

        // Check we have something we allow to handle
        final Uri uri = getIntent().getData().buildUpon().clearQuery().build();
        String scheme = uri.getScheme();
        if (scheme == null || (!scheme.equals("http") && !scheme.equals("https"))) {
            finish();
            return;
        }

        // If we don't have an account, then we can handle the link for sure
        Account account = Preferences.getAccount(this);
        if (account == null) {
            openExternalHttpLinkAndFinish(uri);
            return;
        }

        // Check that we have an activity account which can handle the request
        List<Account> accounts = Preferences.getAccounts(this);
        List<Account> targetAccounts = new ArrayList<>();
        String type = "";
        for (Account acct : accounts) {
            List<RegExLinkifyTextView.RegExLink> links =
                    RegExLinkifyTextView.createRepositoryRegExpLinks(acct.mRepository);
            for (RegExLinkifyTextView.RegExLink link : links) {
                if (link.mPattern.matcher(uri.toString()).find()) {
                    targetAccounts.add(acct);

                    // We can assume safely that all matches are of the same type
                    type = link.mType;
                }
            }
        }

        // No accounts are able to handle the link
        if (targetAccounts.isEmpty()) {
            openExternalHttpLinkAndFinish(uri);
            return;
        }

        // Should we change account
        boolean isSameAccount = false;
        for (Account acct : targetAccounts) {
            if (account.getAccountHash().equals(acct.getAccountHash())) {
                isSameAccount = true;
                break;
            }
        }

        final Account prevAccount = Preferences.getAccount(this);
        if (!isSameAccount) {
            // Open a dialog to ask the user which of the configure accounts wants
            // to use to open the uri
            if (targetAccounts.size() > 1) {
                final String t = type;
                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        this, R.layout.account_chooser_item_layout);
                for (Account acct : targetAccounts) {
                    final String name = getString(R.string.account_settings_subtitle,
                            acct.getRepositoryDisplayName(), acct.getAccountDisplayName());
                    adapter.add(name);
                }

                AlertDialog dialog = new AlertDialog.Builder(this)
                        .setTitle(R.string.account_choose_title)
                        .setSingleChoiceItems(adapter, -1, (d, which) -> {
                            d.dismiss();

                            // Change to the selected account.
                            Preferences.setAccount(this, targetAccounts.get(which));

                            // An now handle the dialog
                            handleUri(t, uri, prevAccount);
                            finish();
                        })
                        .setPositiveButton(R.string.action_cancel, (d, which) -> {
                            d.dismiss();
                            finish();
                        })
                        .setOnCancelListener(d -> finish())
                        .setOnDismissListener(d -> finish())
                        .create();
                dialog.show();
                return;

            } else {
                // Use the unique account found
                Preferences.setAccount(this, targetAccounts.get(0));
            }
        }

        // Open the change details
        handleUri(type, uri, prevAccount);
        finish();
    }

    @SuppressWarnings("ConstantConditions")
    private void handleUri(String type, Uri uri, Account prevAccount) {
        // Open the change details
        switch (type) {
            case Constants.CUSTOM_URI_CHANGE_ID:
                Account acct = Preferences.getAccount(this);
                if (acct != null) {
                    String changeId = UriHelper.extractChangeId(uri, acct.mRepository);
                    if (changeId.equals("-1") || (!isChange(changeId)
                            && !isEncodedChangeId(changeId) && !isChangeId(changeId))) {
                        Toast.makeText(this, getString(
                                R.string.exception_cannot_handle_link,
                                getIntent().getData().toString()), Toast.LENGTH_SHORT).show();
                        Preferences.setAccount(this, prevAccount);
                        return;
                    }

                    ActivityHelper.openChangeDetailsByUri(this, UriHelper.createCustomUri(
                            this, Constants.CUSTOM_URI_CHANGE_ID, changeId), true, true);
                    break;
                }

                // We cannot handle this
                openExternalHttpLinkAndFinish(uri);
                Preferences.setAccount(this, prevAccount);
                break;

            case Constants.CUSTOM_URI_DASHBOARD:
                DashboardInfo dashboard = DashboardHelper.createDashboardFromUri(uri.toString());
                if (dashboard != null) {
                    ActivityHelper.openDashboardActivity(this, dashboard, true);
                    break;
                }

                // We cannot handle this
                openExternalHttpLinkAndFinish(uri);
                Preferences.setAccount(this, prevAccount);
                break;

            case Constants.CUSTOM_URI_QUERY:
                String query = UriHelper.extractQuery(uri);
                if (!TextUtils.isEmpty(query)) {
                    final ChangeQuery filter;
                    if (isCommit(query)) {
                        filter = new ChangeQuery().commit(query);
                    } else if (isChange(query) || isChangeId(query)) {
                        filter = new ChangeQuery().change(query);
                    } else {
                        // Try to parse the query
                        try {
                            filter = ChangeQuery.parse(query);
                        } catch (QueryParseException ex) {
                            // Ignore. Try to open the url.
                            Log.w(TAG, "Can parse query: " + query);
                            openExternalHttpLinkAndFinish(uri);
                            Preferences.setAccount(this, prevAccount);
                            return;
                        }
                    }

                    final Uri referrer = ActivityCompat.getReferrer(this);
                    boolean external = referrer == null ||
                            !getPackageName().equals(referrer.getAuthority());
                    ActivityHelper.openChangeListByFilterActivity(this, null, filter, true, external);
                    break;
                }

                // We cannot handle this
                openExternalHttpLinkAndFinish(uri);
                Preferences.setAccount(this, prevAccount);
                break;

            default:
                // We cannot handle this
                openExternalHttpLinkAndFinish(uri);
                Preferences.setAccount(this, prevAccount);
                break;
        }
    }

    private void openExternalHttpLinkAndFinish(Uri link) {
        String source = getIntent().getStringExtra(Constants.EXTRA_SOURCE);
        if (source != null && source.equals(getPackageName())) {
            ActivityHelper.openUriInCustomTabs(this, link, true);
        } else {
            ActivityHelper.openUri(this, link, true);
        }
        finish();
    }

    private static boolean isChange(String query) {
        return StringHelper.GERRIT_CHANGE.matcher(query).matches();
    }

    private static boolean isCommit(String query) {
        return StringHelper.GERRIT_COMMIT.matcher(query).matches();
    }

    private static boolean isChangeId(String query) {
        return StringHelper.GERRIT_CHANGE_ID.matcher(query).matches();
    }

    private static boolean isEncodedChangeId(String query) {
        return StringHelper.GERRIT_ENCODED_CHANGE_ID.matcher(query).matches();
    }
}
