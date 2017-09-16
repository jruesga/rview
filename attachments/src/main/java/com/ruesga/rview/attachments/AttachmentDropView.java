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

import android.content.ClipData;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.DragEvent;
import android.view.View;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AttachmentDropView extends View implements View.OnDragListener {

    public interface OnAttachmentsDroppedListener {
        void onAttachmentsDropped(List<Attachment> attachments);
    }


    private OnAttachmentsDroppedListener mCallback;

    public AttachmentDropView(Context context) {
        this(context, null);
    }

    public AttachmentDropView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AttachmentDropView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOnDragListener(this);
    }

    public AttachmentDropView listenTo(OnAttachmentsDroppedListener cb) {
        mCallback = cb;
        return this;
    }

    @Override
    public boolean onDrag(View view, DragEvent dragEvent) {
        switch (dragEvent.getAction()) {
            case DragEvent.ACTION_DRAG_STARTED:
                AttachmentsProvider provider =
                        AttachmentsProviderFactory.getAttachmentProvider(getContext());
                return mCallback != null && provider.isSupported();
            case DragEvent.ACTION_DRAG_ENTERED:
                setBackgroundColor(ContextCompat.getColor(getContext(), R.color.attachmentOverlay));
                return isValidDragEvent(dragEvent.getClipData());
            case DragEvent.ACTION_DRAG_ENDED:
                setBackgroundColor(ContextCompat.getColor(getContext(), android.R.color.transparent));
                return true;
            case DragEvent.ACTION_DRAG_EXITED:
            case DragEvent.ACTION_DRAG_LOCATION:
                return true;
            case DragEvent.ACTION_DROP:
                if (isValidDragEvent(dragEvent.getClipData())) {
                    final List<Attachment> attachments = extractAttachments(dragEvent.getClipData());
                    post(() -> mCallback.onAttachmentsDropped(attachments));
                    return true;
                }
                // Fallback
        }
        return false;
    }

    private boolean isValidDragEvent(ClipData data) {
        if (data == null || data.getItemCount() == 0) {
            return false;
        }

        boolean valid = true;
        int count = data.getItemCount();
        for (int i = 0; i < count; i++) {
            Uri uri = data.getItemAt(i).getUri();
            if (uri == null) {
                valid = false;
                break;
            }
        }

        return valid;
    }

    private List<Attachment> extractAttachments(ClipData data) {
        int count = data.getItemCount();
        List<Attachment> attachments = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            Attachment attachment = new Attachment();
            attachment.mLocalUri = data.getItemAt(i).getUri();
            switch (attachment.mLocalUri.getScheme()) {
                case ContentResolver.SCHEME_CONTENT:
                    fillAttachmentFromContentUri(attachment);
                    break;
                case ContentResolver.SCHEME_FILE:
                    fillAttachmentFromFileUri(attachment);
                    break;
                case "http":
                case "https":
                    fillAttachmentFromUrl(attachment);
                    break;
                default:
                    fillAttachmentFromUnknownScheme(attachment);
                    break;
            }
            attachments.add(attachment);
        }
        return attachments;
    }

    private void fillAttachmentFromContentUri(Attachment attachment) {
        ContentResolver cr = getContext().getContentResolver();
        attachment.mMimeType = cr.getType(attachment.mLocalUri);
        if (TextUtils.isEmpty(attachment.mMimeType)) {
            attachment.mMimeType = "application/octet-stream";
        }

        Cursor c = null;
        try {
            c = cr.query(attachment.mLocalUri, null, null, null, null);
            if (c != null) {
                c.moveToFirst();
                attachment.mName = new File(
                        c.getString(
                                c.getColumnIndex(OpenableColumns.DISPLAY_NAME))).getName();
                attachment.mSize = c.getLong(c.getColumnIndex(OpenableColumns.SIZE));
            }
        } finally {
            try {
                if (c != null) {
                    c.close();
                }
            } catch (Exception ex) {
                // ignore
            }
        }

    }

    private void fillAttachmentFromFileUri(Attachment attachment) {
        File file = new File(attachment.mLocalUri.getPath());
        attachment.mMimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.getName());
        if (TextUtils.isEmpty(attachment.mMimeType)) {
            attachment.mMimeType = "application/octet-stream";
        }
        attachment.mName = file.getName();
        if (attachment.mName.lastIndexOf(".") > 0) {
            attachment.mName = attachment.mName.substring(0, attachment.mName.lastIndexOf("."));
        }
        attachment.mSize = file.length();

    }

    private void fillAttachmentFromUrl(Attachment attachment) {
        attachment.mMimeType = "application/internet-shortcut";
        String name = attachment.mLocalUri.getLastPathSegment();
        attachment.mName = name == null ? "Unnamed" : name;
        attachment.mSize = 0;

    }

    private void fillAttachmentFromUnknownScheme(Attachment attachment) {
        attachment.mMimeType = "application/octet-stream";
        attachment.mName = "Unnamed";
        attachment.mSize = 0;

    }
}
