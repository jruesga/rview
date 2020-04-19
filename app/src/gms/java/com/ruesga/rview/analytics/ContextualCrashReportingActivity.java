/*
 * Copyright (C) 2020 Jorge Ruesga
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
package com.ruesga.rview.analytics;

import android.os.Bundle;
import android.text.TextUtils;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.ruesga.rview.R;
import com.ruesga.rview.misc.ModelHelper;
import com.ruesga.rview.misc.SerializationManager;
import com.ruesga.rview.misc.UriHelper;
import com.ruesga.rview.model.Account;
import com.ruesga.rview.preferences.Constants;
import com.ruesga.rview.preferences.Preferences;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public abstract class ContextualCrashReportingActivity
        extends AppCompatActivity implements CrashReporting {

    @Keep
    private static class ContextualCrashReporting {
        String repository;
        String authenticated;
        String changeId;
        String revisionId;
        String fileId;
        String base;
        String filter;
    }

    private ContextualCrashReporting mContextualCrashReporting;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        clearAnalyticsContext();
        if (savedInstanceState != null) {
            mContextualCrashReporting = SerializationManager.getInstance().fromJson(
                    savedInstanceState.getString("contextual_crash_analytics"),
                    ContextualCrashReporting.class);
            putAnalyticsContext();
        } else {
            mContextualCrashReporting = new ContextualCrashReporting();
            resolveAnalyticsContext();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        putAnalyticsContext();
    }

    @Override
    @SuppressWarnings("NullableProblems")
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("contextual_crash_analytics",
                SerializationManager.getInstance().toJson(mContextualCrashReporting));
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
        setAnalyticsAccount(
                mContextualCrashReporting.repository,
                mContextualCrashReporting.authenticated);
        setAnalyticsChangeId(mContextualCrashReporting.changeId);
        setAnalyticsRevisionId(mContextualCrashReporting.revisionId);
        setAnalyticsFileId(mContextualCrashReporting.fileId);
        setAnalyticsBase(mContextualCrashReporting.base);
        setAnalyticsFilter(mContextualCrashReporting.filter);
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
                    setAnalyticsChangeId(changeId);
                }
            }
        } catch (Throwable ex) {
            // Don't fail because analytics
        }

        // Revision context
        try {
            String revisionId = getIntent().getStringExtra(Constants.EXTRA_REVISION_ID);
            if (!TextUtils.isEmpty(revisionId)) {
                setAnalyticsRevisionId(revisionId);
            } else {
                revisionId = getIntent().getStringExtra(Constants.EXTRA_REVISION);
                if (!TextUtils.isEmpty(revisionId)) {
                    setAnalyticsRevisionId(revisionId);
                }
            }
        } catch (Throwable ex) {
            // Don't fail because analytics
        }

        // File context
        try {
            String fileId = getIntent().getStringExtra(Constants.EXTRA_FILE);
            if (!TextUtils.isEmpty(fileId)) {
                setAnalyticsFileId(fileId);
            }
        } catch (Throwable ex) {
            // Don't fail because analytics
        }

        // Base context
        try {
            String base = getIntent().getStringExtra(Constants.EXTRA_BASE);
            if (!TextUtils.isEmpty(base)) {
                setAnalyticsBase(base);
            }
        } catch (Throwable ex) {
            // Don't fail because analytics
        }

        // Filter context
        try {
            String filter = getIntent().getStringExtra(Constants.EXTRA_FILTER);
            if (!TextUtils.isEmpty(filter)) {
                setAnalyticsFilter(filter);
            }
        } catch (Throwable ex) {
            // Don't fail because analytics
        }
    }

    @SuppressWarnings("ConstantConditions")
    private void setAnalyticsEnvironment() {
        try {
            if (!isCrashlyticsEnabled()) {
                return;
            }
            instance().setCustomKey("is_tablet", getResources().getBoolean(R.bool.config_is_tablet));
            if (getIntent() != null) {
                instance().setCustomKey("intent_action", getIntent().getAction());
                instance().setCustomKey("intent_type", getIntent().getType());
                instance().setCustomKey("intent_data", getIntent().getDataString());
            }
        } catch (Throwable ex) {
            // Don't fail because analytics
        }
    }

    @Override
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
            if (!isCrashlyticsEnabled()) {
                return;
            }
            mContextualCrashReporting.repository = repository;
            mContextualCrashReporting.authenticated = authenticated;
            instance().setCustomKey("repository", repository);
            instance().setCustomKey("authenticated", authenticated);
        } catch (Throwable ex) {
            // Don't fail because analytics
        }
    }

    @Override
    public void setAnalyticsChangeId(String changeId) {
        try {
            if (!isCrashlyticsEnabled()) {
                return;
            }
            mContextualCrashReporting.changeId = changeId;
            instance().setCustomKey("changeId", changeId);
        } catch (Throwable ex) {
            // Don't fail because analytics
        }
    }

    @Override
    public void setAnalyticsRevisionId(String revisionId) {
        try {
            if (!isCrashlyticsEnabled()) {
                return;
            }
            mContextualCrashReporting.revisionId = revisionId;
            instance().setCustomKey("revisionId", revisionId);
        } catch (Throwable ex) {
            // Don't fail because analytics
        }
    }

    @Override
    public void setAnalyticsFileId(String fileId) {
        try {
            if (!isCrashlyticsEnabled()) {
                return;
            }
            mContextualCrashReporting.fileId = fileId;
            instance().setCustomKey("fileId", fileId);
        } catch (Throwable ex) {
            // Don't fail because analytics
        }
    }

    @Override
    public void setAnalyticsBase(String base) {
        try {
            if (!isCrashlyticsEnabled()) {
                return;
            }
            mContextualCrashReporting.base = base;
            instance().setCustomKey("base", base);
        } catch (Throwable ex) {
            // Don't fail because analytics
        }
    }

    @Override
    public void setAnalyticsFilter(String filter) {
        try {
            if (!isCrashlyticsEnabled()) {
                return;
            }
            mContextualCrashReporting.filter = filter;
            instance().setCustomKey("filter", filter);
        } catch (Throwable ex) {
            // Don't fail because analytics
        }
    }

    @SuppressWarnings({"ConstantConditions", "BooleanMethodIsAlwaysInverted"})
    private boolean isCrashlyticsEnabled() {
        try {
            return getResources().getBoolean(R.bool.fcm_enable_crashlytics)
                    && FirebaseCrashlytics.getInstance() != null;
        } catch (RuntimeException ex) {
            return false;
        }
    }
    
    private FirebaseCrashlytics instance() {
        return FirebaseCrashlytics.getInstance();
    }
}
