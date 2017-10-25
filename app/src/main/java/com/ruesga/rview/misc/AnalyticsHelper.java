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
package com.ruesga.rview.misc;

import android.content.Context;
import android.os.Bundle;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.ruesga.rview.R;
import com.ruesga.rview.model.Account;

public class AnalyticsHelper {

    public static void appStarted(Context context) {
        try {
            if (!checkIsAnalyticsEnabled(context)) {
                return;
            }
            FirebaseAnalytics analytics = FirebaseAnalytics.getInstance(context);
            if (analytics != null) {
                analytics.logEvent(FirebaseAnalytics.Event.APP_OPEN, null);
            }
        } catch (Throwable ex) {
            // Don't fail because analytics
        }
    }

    public static void accountEvent(Context context, Account account, boolean created) {
        try {
            if (!checkIsAnalyticsEnabled(context)) {
                return;
            }
            FirebaseAnalytics analytics = FirebaseAnalytics.getInstance(context);
            if (analytics != null) {
                Bundle bundle = new Bundle();
                bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "repository");
                bundle.putString(FirebaseAnalytics.Param.ITEM_NAME,
                        account.getRepositoryDisplayName());
                bundle.putString(FirebaseAnalytics.Param.GROUP_ID,
                        UriHelper.anonymize(
                                UriHelper.sanitizeEndpoint(account.mRepository.mUrl)));
                bundle.putBoolean("authenticated", account.hasAuthenticatedAccessMode());
                bundle.putBoolean("created", created);
                analytics.logEvent(FirebaseAnalytics.Event.JOIN_GROUP, bundle);
            }
        } catch (Throwable ex) {
            // Don't fail because analytics
        }
    }

    public static void accountSelected(Context context, Account account) {
        try {
            if (!checkIsAnalyticsEnabled(context)) {
                return;
            }
            FirebaseAnalytics analytics = FirebaseAnalytics.getInstance(context);
            if (analytics != null) {
                Bundle bundle = new Bundle();
                bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "repository");
                bundle.putString(FirebaseAnalytics.Param.ITEM_NAME,
                        account.getRepositoryDisplayName());
                bundle.putString(FirebaseAnalytics.Param.ITEM_ID,
                        UriHelper.anonymize(
                                UriHelper.sanitizeEndpoint(account.mRepository.mUrl)));
                bundle.putBoolean("authenticated", account.hasAuthenticatedAccessMode());
                analytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
            }
        } catch (Throwable ex) {
            // Don't fail because analytics
        }
    }

    private static boolean checkIsAnalyticsEnabled(Context context) {
        return !context.getResources().getBoolean(R.bool.fcm_disable_analytics);
    }
}
