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

import com.ruesga.rview.attachments.gdrive.GDriveAttachmentsProvider;
import com.ruesga.rview.attachments.none.NoneAttachmentsProvider;
import com.ruesga.rview.attachments.preferences.Preferences;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class AttachmentsProviderFactory {

    private static final Map<Provider, AttachmentsProvider> sProviders = new TreeMap<>();

    public static void initialize(Context context) {
        sProviders.put(Provider.NONE, new NoneAttachmentsProvider());
        sProviders.put(Provider.GDRIVE, new GDriveAttachmentsProvider(context));
    }

    public static List<AttachmentsProvider> getAllAvailableAttachmentProviders() {
        ArrayList<AttachmentsProvider> providers = new ArrayList<>(sProviders.values());
        Iterator<AttachmentsProvider> it = providers.iterator();
        while (it.hasNext()) {
            AttachmentsProvider provider = it.next();
            if (!provider.isAvailable()) {
                it.remove();
            }
        }
        return providers;
    }

    public static AttachmentsProvider getAttachmentProvider(Context context) {
        return sProviders.get(Preferences.getProvider(context));
    }

    public static AttachmentsProvider getAttachmentProvider(Provider provider) {
        return sProviders.get(provider);
    }
}
