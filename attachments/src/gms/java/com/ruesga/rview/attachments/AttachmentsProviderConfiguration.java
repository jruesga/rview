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
package com.ruesga.rview.attachments;

import android.content.Context;

import com.ruesga.rview.attachments.gdrive.GDriveAttachmentsProvider;
import com.ruesga.rview.attachments.none.NoneAttachmentsProvider;

import java.util.Map;
import java.util.TreeMap;

public class AttachmentsProviderConfiguration {
    public static Map<Provider, AttachmentsProvider> availableProviders(Context context) {
        Map<Provider, AttachmentsProvider> providers = new TreeMap<>();
        providers.put(Provider.NONE, new NoneAttachmentsProvider());
        providers.put(Provider.GDRIVE, new GDriveAttachmentsProvider(context));
        return  providers;
    }
}
