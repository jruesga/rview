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
package com.ruesga.rview.attachments.gdrive;

import android.content.ContentResolver;
import android.content.Context;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.ruesga.rview.attachments.Attachment;
import com.ruesga.rview.attachments.AttachmentsProvider;
import com.ruesga.rview.attachments.AuthenticationInfo;
import com.ruesga.rview.attachments.Provider;
import com.ruesga.rview.attachments.R;
import com.ruesga.rview.attachments.gdrive.model.Constants;
import com.ruesga.rview.attachments.gdrive.model.FileMetadata;
import com.ruesga.rview.attachments.gdrive.model.FileMetadataInput;
import com.ruesga.rview.attachments.gdrive.model.FileMetadataPageInfo;
import com.ruesga.rview.attachments.gdrive.model.GranteeType;
import com.ruesga.rview.attachments.gdrive.model.PermissionMetadataInput;
import com.ruesga.rview.attachments.gdrive.model.RoleType;
import com.ruesga.rview.attachments.gdrive.oauth.OAuthProxyFragment;
import com.ruesga.rview.attachments.preferences.Preferences;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.RequestBody;

public class GDriveAttachmentsProvider implements AttachmentsProvider {

    public static final String TAG = "GDriveAttachProvider";

    private final Context mContext;

    private static final String ATTACHMENT_PACKAGE = "com.ruesga.rview.attachments";

    public GDriveAttachmentsProvider(Context context) {
        mContext = context.getApplicationContext();
    }

    public Provider getType() {
        return Provider.GDRIVE;
    }

    @StringRes
    public int getName() {
        return R.string.attachment_provider_gdrive;
    }

    @DrawableRes
    public int getIcon() {
        return R.drawable.ic_attachment_gdrive;
    }

    @Override
    public boolean isAvailable() {
        final String clientId = mContext.getString(R.string.gdrive_client_id);
        final String secret = mContext.getString(R.string.gdrive_client_secret);
        return !TextUtils.isEmpty(clientId) && !TextUtils.isEmpty(secret);
    }

    @Override
    public boolean isSupported() {
        final boolean playServiceAvailable =
                GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(mContext)
                    == ConnectionResult.SUCCESS;
        AuthenticationInfo auth = Preferences.getAuthenticationInfo(mContext, Provider.GDRIVE);
        boolean hasToken = auth != null && !TextUtils.isEmpty(auth.accessToken);
        return playServiceAvailable && isAvailable() && hasToken;
    }

    @Override
    public boolean initialize(FragmentManager fragmentManager) {
        AuthenticationInfo auth = Preferences.getAuthenticationInfo(mContext, Provider.GDRIVE);
        boolean hasToken = auth != null && !TextUtils.isEmpty(auth.accessToken);
        if (!hasToken) {
            OAuthProxyFragment fragment = OAuthProxyFragment.newInstance();
            fragment.show(fragmentManager);
            return false;
        }
        return true;
    }

    @Override
    public boolean createAttachmentsMetadata(List<Attachment> attachments) {
        // Check
        final String folderId = createFolderIfNeeded();

        GDriveRestApiClient api = GDriveRestApiClient.newClientInstance(mContext);
        for (Attachment attachment : attachments) {
            // Don't upload url attachments
            if (attachment.mMimeType.equals("application/internet-shortcut")
                    || attachment.mMimeType.equals("application/x-url")) {
                attachment.mId = attachment.mUrl = attachment.mLocalUri.toString();
                continue;
            }


            Log.i(TAG, "Creating and sharing attachment metadata: " + attachment.mName
                    + " from " + attachment.mLocalUri);
            // Create metadata
            FileMetadataInput file = new FileMetadataInput();
            file.name = attachment.mName;
            file.mimeType = attachment.mMimeType;
            file.parents = new String[]{folderId};
            FileMetadata metadata = api.createFileMetadata(file).blockingFirst();

            // Create permissions
            PermissionMetadataInput permission = new PermissionMetadataInput();
            permission.type = GranteeType.anyone;
            permission.role = RoleType.reader;
            permission.allowFileDiscovery = false;
            api.createFilePermission(metadata.id, permission).blockingFirst();
            Log.i(TAG, "New attachment metadata created: " + metadata.id);

            // Assign the shared url
            boolean image = file.mimeType.startsWith("image/") && !file.mimeType.contains("+xml");
            final String mode = image ? Constants.SHARED_MODE_VIEW : Constants.SHARED_MODE_DOWNLOAD;
            attachment.mId = metadata.id;
            attachment.mUrl = String.format(Locale.US, Constants.SHARED_URL, mode, metadata.id);
            Log.i(TAG, "New attachment url: " + attachment.mUrl);
        }

        return true;
    }

    @Override
    public boolean uploadAttachmentsContent(List<Attachment> attachments) {
        GDriveRestApiClient api = GDriveRestApiClient.newClientInstance(mContext);
        Log.i(TAG, "Uploading " + attachments.size() + " content attachments");
        boolean success = true;
        for (Attachment attachment : attachments) {
            // Don't upload url attachments
            if (attachment.mMimeType.equals("application/internet-shortcut")
                    || attachment.mMimeType.equals("application/x-url")) {
                attachment.mUrl = attachment.mLocalUri.toString();
                continue;
            }

            try {
                File temp = createTemporaryAttachmentFile(attachment);
                if (temp != null) {
                    try {
                        Log.i(TAG, "Uploading attachment: " + attachment.mId);

                        // Upload the attachment
                        RequestBody content = RequestBody.create(
                                MediaType.parse("application/octet-stream"), temp);
                        api.uploadFileContent(attachment.mId, content).blockingFirst();

                        Log.i(TAG, "Attachment uploaded successfully: " + attachment.mId);
                    } finally {
                        //noinspection ResultOfMethodCallIgnored
                        temp.delete();
                    }
                }

            } catch (Exception ex) {
                Log.e(TAG, "Failed to upload attachment content: " + attachment.mId, ex);
                success = false;
            }
        }

        return success;
    }

    private String createFolderIfNeeded() {
        GDriveRestApiClient api = GDriveRestApiClient.newClientInstance(mContext);
        FileMetadataPageInfo page = api.listFiles(null, null, "name='" + ATTACHMENT_PACKAGE + "'")
                .blockingFirst();
        for (FileMetadata file : page.files) {
            if (ATTACHMENT_PACKAGE.equals(file.name)
                    && Constants.GOOGLE_FOLDER_MIME_TYPE.equals(file.mimeType)) {
                // Folder exists
                Log.i(TAG, ATTACHMENT_PACKAGE + " folder exists in GDrive: " + file.id);
                return file.id;
            }
        }

        // Folder doesn't exists
        Log.i(TAG, ATTACHMENT_PACKAGE + " folder doesn't exists in GDrive. Create a new folder");
        FileMetadataInput folder = new FileMetadataInput();
        folder.name = ATTACHMENT_PACKAGE;
        folder.mimeType = Constants.GOOGLE_FOLDER_MIME_TYPE;
        folder.parents = new String[]{Constants.ROOT_FOLDER};
        FileMetadata metadata = api.createFileMetadata(folder).blockingFirst();
        Log.i(TAG, ATTACHMENT_PACKAGE + " folder created in GDrive: " + metadata.id);
        return metadata.id;
    }

    private File createTemporaryAttachmentFile(Attachment attachment) throws IOException {
        InputStream is = null;
        OutputStream os = null;
        try {
            ContentResolver cr = mContext.getContentResolver();
            is = cr.openInputStream(attachment.mLocalUri);
            if (is == null) {
                return null;
            }

            File temp = new File(mContext.getFilesDir(),
                    System.currentTimeMillis() + "." + attachment.mId);
            os = new FileOutputStream(temp, false);

            // Copy the attachment to a temporary file
            byte[] data = new byte[4096];
            int read;
            while ((read = is.read(data, 0, 4096)) != -1) {
                os.write(data, 0, read);
            }
            os.close();

            return temp;

        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException ex) {
                // Ignore
            }
            try {
                if (os != null) {
                    os.close();
                }
            } catch (IOException ex) {
                // Ignore
            }
        }
    }
}
