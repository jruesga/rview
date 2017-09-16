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
package com.ruesga.rview.attachments;

import android.content.Context;
import android.content.pm.PackageManager;

import com.ruesga.rview.attachments.preferences.Preferences;

public class AttachmentsSupport {

    private final Context mContext;
    private final PackageManager mPackageManager;


    public AttachmentsSupport(Context context) {
        mContext = context;
        mPackageManager = context.getPackageManager();
    }

    public boolean isAttachmentFeatureSupported() {
        return AttachmentsProviderFactory.getAllAvailableAttachmentProviders().size() > 0;
    }

    public boolean isProviderSupported() {
        AttachmentsProvider provider =
                AttachmentsProviderFactory.getAttachmentProvider(
                    Preferences.getProvider(mContext));
        return provider.isSupported();
    }

    public boolean supportsCamera() {
        return mPackageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }
}
