/*
 * Copyright (C) 2017 Jorge Ruesga
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
import android.support.annotation.Keep;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;

import com.crashlytics.android.Crashlytics;
import com.ruesga.rview.misc.ModelHelper;
import com.ruesga.rview.misc.SerializationManager;
import com.ruesga.rview.misc.UriHelper;
import com.ruesga.rview.model.Account;
import com.ruesga.rview.preferences.Constants;
import com.ruesga.rview.preferences.Preferences;

public abstract class ContextualCrashAnalyticsActivity extends AppCompatActivity {

    @Keep
    private static class ContextualCrashAnalytics {
        String repository;
        String authenticated;
        String changeId;
        String revisionId;
        String fileId;
        String base;
        String filter;
    }

    private ContextualCrashAnalytics mCrashContext;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        clearAnalyticsContext();
        if (savedInstanceState != null) {
            mCrashContext = SerializationManager.getInstance().fromJson(
                    savedInstanceState.getString("contextual_crash_analytics"),
                    ContextualCrashAnalytics.class);
            putAnalyticsContext();
        } else {
            mCrashContext = new ContextualCrashAnalytics();
            resolveAnalyticsContext();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        putAnalyticsContext();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("contextual_crash_analytics",
                SerializationManager.getInstance().toJson(mCrashContext));
    }

    private void clearAnalyticsContext() {
        setAnalyticsAccount(null, null);
        setAnalyticsChangeId(null);
        setAnalyticsRevisionId(null);
        setAnalyticsFileId(null);
        setAnalyticsBase(null);
        setAnalyticsFilter(null);
    }

    private void putAnalyticsContext() {
        setAnalyticsEnvironment();
        setAnalyticsAccount(mCrashContext.repository, mCrashContext.authenticated);
        setAnalyticsChangeId(mCrashContext.changeId);
        setAnalyticsRevisionId(mCrashContext.revisionId);
        setAnalyticsFileId(mCrashContext.fileId);
        setAnalyticsBase(mCrashContext.base);
        setAnalyticsFilter(mCrashContext.filter);
    }

    private void resolveAnalyticsContext() {
        if (getIntent() == null) {
            return;
        }

        // Environment context
        setAnalyticsEnvironment();

        // Account context
        try {
            String accountId = getIntent().getStringExtra(Constants.EXTRA_ACCOUNT_HASH);
            if (!TextUtils.isEmpty(accountId)) {
                setAnalyticsAccount(ModelHelper.getAccountFromHash(this, accountId));
            } else {
                setAnalyticsAccount(Preferences.getAccount(this));
            }
        } catch (Throwable ex) {
            // Don't fail because analytics
        }

        // Change context
        try {
            int legacyChangeId = getIntent().getIntExtra(Constants.EXTRA_LEGACY_CHANGE_ID, -1);
            if (legacyChangeId != -1) {
                setAnalyticsChangeId(String.valueOf(legacyChangeId));
            } else {
                String changeId = getIntent().getStringExtra(Constants.EXTRA_CHANGE_ID);
                if (!TextUtils.isEmpty(changeId)) {
                    setAnalyticsChangeId(String.valueOf(changeId));
                }
            }
        } catch (Throwable ex) {
            // Don't fail because analytics
        }

        // Revision context
        try {
            String revisionId = getIntent().getStringExtra(Constants.EXTRA_REVISION_ID);
            if (!TextUtils.isEmpty(revisionId)) {
                setAnalyticsRevisionId(String.valueOf(revisionId));
            } else {
                revisionId = getIntent().getStringExtra(Constants.EXTRA_REVISION);
                if (!TextUtils.isEmpty(revisionId)) {
                    setAnalyticsRevisionId(String.valueOf(revisionId));
                }
            }
        } catch (Throwable ex) {
            // Don't fail because analytics
        }

        // File context
        try {
            String fileId = getIntent().getStringExtra(Constants.EXTRA_FILE);
            if (!TextUtils.isEmpty(fileId)) {
                setAnalyticsFileId(String.valueOf(fileId));
            }
        } catch (Throwable ex) {
            // Don't fail because analytics
        }

        // Base context
        try {
            String base = getIntent().getStringExtra(Constants.EXTRA_BASE);
            if (!TextUtils.isEmpty(base)) {
                setAnalyticsBase(String.valueOf(base));
            }
        } catch (Throwable ex) {
            // Don't fail because analytics
        }

        // Filter context
        try {
            String filter = getIntent().getStringExtra(Constants.EXTRA_FILTER);
            if (!TextUtils.isEmpty(filter)) {
                setAnalyticsFilter(String.valueOf(filter));
            }
        } catch (Throwable ex) {
            // Don't fail because analytics
        }
    }

    private void setAnalyticsEnvironment() {
        try {
            if (!checkCraAvailability()) {
                return;
            }
            Crashlytics.setBool("is_tablet", getResources().getBoolean(R.bool.config_is_tablet));
            if (getIntent() != null) {
                Crashlytics.setString("intent_action", getIntent().getAction());
                Crashlytics.setString("intent_type", getIntent().getType());
                Crashlytics.setString("intent_data", getIntent().getDataString());
            }
        } catch (Throwable ex) {
            // Don't fail because analytics
        }
    }

    public void setAnalyticsAccount(Account account) {
        try {
            String repository = account == null ? null : UriHelper.anonymize(
                    UriHelper.sanitizeEndpoint(account.mRepository.mUrl));
            String authenticated = account == null ? null :
                    Boolean.toString(account.hasAuthenticatedAccessMode());
            setAnalyticsAccount(repository, authenticated);
        } catch (Throwable ex) {
            // Don't fail because analytics
        }
    }

    private void setAnalyticsAccount(String repository, String authenticated) {
        try {
            if (!checkCraAvailability()) {
                return;
            }
            mCrashContext.repository = repository;
            mCrashContext.authenticated = authenticated;
            Crashlytics.setString("repository", repository);
            Crashlytics.setString("authenticated", authenticated);
        } catch (Throwable ex) {
            // Don't fail because analytics
        }
    }

    public void setAnalyticsChangeId(String changeId) {
        try {
            if (!checkCraAvailability()) {
                return;
            }
            mCrashContext.changeId = changeId;
            Crashlytics.setString("changeId", changeId);
        } catch (Throwable ex) {
            // Don't fail because analytics
        }
    }

    public void setAnalyticsRevisionId(String revisionId) {
        try {
            if (!checkCraAvailability()) {
                return;
            }
            mCrashContext.revisionId = revisionId;
            Crashlytics.setString("revisionId", revisionId);
        } catch (Throwable ex) {
            // Don't fail because analytics
        }
    }

    public void setAnalyticsFileId(String fileId) {
        try {
            if (!checkCraAvailability()) {
                return;
            }
            mCrashContext.fileId = fileId;
            Crashlytics.setString("fileId", fileId);
        } catch (Throwable ex) {
            // Don't fail because analytics
        }
    }

    public void setAnalyticsBase(String base) {
        try {
            if (!checkCraAvailability()) {
                return;
            }
            mCrashContext.base = base;
            Crashlytics.setString("base", base);
        } catch (Throwable ex) {
            // Don't fail because analytics
        }
    }

    public void setAnalyticsFilter(String filter) {
        try {
            if (!checkCraAvailability()) {
                return;
            }
            mCrashContext.filter = filter;
            Crashlytics.setString("filter", filter);
        } catch (Throwable ex) {
            // Don't fail because analytics
        }
    }

    private boolean checkCraAvailability() {
        return !BuildConfig.DEBUG && Crashlytics.getInstance() != null;

    }
}
