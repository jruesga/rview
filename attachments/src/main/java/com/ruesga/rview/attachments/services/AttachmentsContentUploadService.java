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
package com.ruesga.rview.attachments.services;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.ruesga.rview.attachments.Attachment;
import com.ruesga.rview.attachments.AttachmentsProvider;
import com.ruesga.rview.attachments.AttachmentsProviderFactory;
import com.ruesga.rview.attachments.R;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;

public class AttachmentsContentUploadService extends JobIntentService {

    private static final String TAG = "AttachUploadService";

    public static final String UPLOAD_ATTACHMENT_CONTENT_ACTION =
            "com.ruesga.rview.attachments.actions.REGISTER_DEVICE";
    public static final String EXTRA_ATTACHMENTS = "attachments";

    private static final int JOB_ID = 1001;

    public AttachmentsContentUploadService() {
        super();
    }

    public static void enqueueWork(Context context, ArrayList<Attachment> attachments) {
        Intent work = new Intent();
        work.setAction(UPLOAD_ATTACHMENT_CONTENT_ACTION);
        work.putParcelableArrayListExtra(EXTRA_ATTACHMENTS, attachments);
        enqueueWork(context, AttachmentsContentUploadService.class, JOB_ID, work);
    }

    @Override
    @SuppressWarnings("Convert2streamapi")
    protected void onHandleWork(@NonNull Intent intent) {
        if (UPLOAD_ATTACHMENT_CONTENT_ACTION.equals(intent.getAction())) {
            AttachmentsProvider provider = AttachmentsProviderFactory.getAttachmentProvider(this);
            if (!provider.isSupported()) {
                Log.w(TAG, "Can't upload attachments. Provider is not supported");
                notifyUploadError();
                return;
            }


            List<Attachment> attachments =
                    intent.getParcelableArrayListExtra(EXTRA_ATTACHMENTS);
            if (attachments == null || attachments.isEmpty()) {
                Log.w(TAG, "No attachments to upload");
                return;
            }

            // Upload every attachment
            if (!provider.uploadAttachmentsContent(attachments)) {
                Log.w(TAG, "Failed to upload all attachments's content");
                notifyUploadError();
            }
        }
    }

    private void notifyUploadError() {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> Toast.makeText(AttachmentsContentUploadService.this,
                R.string.attachment_cannot_upload_attachment,
                Toast.LENGTH_SHORT).show());
    }

}
