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

import android.app.Application;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.core.CrashlyticsCore;
import com.ruesga.rview.misc.Formatter;

import io.fabric.sdk.android.Fabric;
import io.fabric.sdk.android.Kit;

public class RviewApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Install a hook to Crashlytics and Answers (only in production releases)
        CrashlyticsCore core = new CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG).build();
        Crashlytics crashlytics = new Crashlytics.Builder().core(core).build();
        Answers answers = new Answers();
        Kit[] kits = BuildConfig.DEBUG ? new Kit[]{crashlytics} : new Kit[]{crashlytics, answers};
        final Fabric fabric = new Fabric.Builder(this)
                .kits(kits)
                .debuggable(BuildConfig.DEBUG)
                .build();
        Fabric.with(fabric);

        // Initialize application resources
        Formatter.refreshCachedPreferences(getApplicationContext());
    }
}
