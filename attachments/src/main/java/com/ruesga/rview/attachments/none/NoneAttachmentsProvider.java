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
package com.ruesga.rview.attachments.none;

import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.support.v4.app.FragmentManager;

import com.ruesga.rview.attachments.Attachment;
import com.ruesga.rview.attachments.AttachmentsProvider;
import com.ruesga.rview.attachments.Provider;

import java.util.List;

public class NoneAttachmentsProvider implements AttachmentsProvider {

    public NoneAttachmentsProvider() {
    }

    public Provider getType() {
        return Provider.NONE;
    }

    @StringRes
    public int getName() {
        return 0;
    }

    @DrawableRes
    public int getIcon() {
        return 0;
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public boolean isSupported() {
        return false;
    }

    @Override
    public boolean initialize(FragmentManager fragmentManager) {
        return false;
    }

    @Override
    public boolean createAttachmentsMetadata(List<Attachment> attachments) {
        return false;
    }

    @Override
    public boolean uploadAttachmentsContent(List<Attachment> attachments) {
        return false;
    }
}
