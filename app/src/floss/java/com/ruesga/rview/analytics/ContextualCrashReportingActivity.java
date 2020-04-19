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

import androidx.appcompat.app.AppCompatActivity;

import com.ruesga.rview.model.Account;

public abstract class ContextualCrashReportingActivity
        extends AppCompatActivity implements CrashReporting {
    @Override
    public void setAnalyticsAccount(Account account) {
    }

    @Override
    public void setAnalyticsChangeId(String changeId) {
    }

    @Override
    public void setAnalyticsRevisionId(String revisionId) {
    }

    @Override
    public void setAnalyticsFileId(String fileId) {
    }

    @Override
    public void setAnalyticsBase(String base) {
    }

    @Override
    public void setAnalyticsFilter(String filter) {
    }
}
