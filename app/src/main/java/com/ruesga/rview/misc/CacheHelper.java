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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.support.v4.content.FileProvider;

import com.ruesga.rview.attachments.Attachment;
import com.ruesga.rview.attachments.EmptyMetadataException;
import com.ruesga.rview.gerrit.NoConnectivityException;
import com.ruesga.rview.model.Account;
import com.ruesga.rview.preferences.Preferences;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class CacheHelper {

    public static final long MAX_AGE_CACHE = 60 * 60 * 24 * 5L;
    public static final long MAX_DISK_CACHE = 50 * 1024 * 1024L;

    private static final String IMAGES_CACHE_FOLDER = "images";
    private static final String AVATARS_CACHE_FOLDER = "avatars";
    private static final String DIFF_CACHE_FOLDER = "diff";
    private static final String ATTACHMENT_CACHE_FOLDER = "attachments";

    public static final String CACHE_CHANGE_JSON = "change.json";
    public static final String CACHE_FILES_JSON = "files.json";
    public static final String CACHE_FILES_INFO_JSON = "files_info.json";
    public static final String CACHE_DIFF_JSON = "diff.json";
    public static final String CACHE_COMMENTS_JSON = "comments.json";
    public static final String CACHE_DRAFT_JSON = "drafts.json";
    public static final String CACHE_CONTENT = "content";
    public static final String CACHE_BLAME = "blame";
    public static final String CACHE_PARENT = "parent";

    public static Response.Builder addCacheControl(Response.Builder builder) {
        return builder.header("Cache-Control", "max-age=" + MAX_AGE_CACHE);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static File getImagesCacheDir(Context context) {
        File cacheDir = new File(context.getCacheDir(), IMAGES_CACHE_FOLDER);
        if (!cacheDir.exists()) {
            cacheDir.mkdir();
        }
        return cacheDir;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static File getAvatarsCacheDir(Context context) {
        File cacheDir = new File(context.getCacheDir(), AVATARS_CACHE_FOLDER);
        if (!cacheDir.exists()) {
            cacheDir.mkdir();
        }
        return cacheDir;
    }

    public static Uri createNewTemporaryFileUri(Context context, String suffix) throws IOException {
        String ts = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        File storageDir = context.getFilesDir();
        if (storageDir.exists() || storageDir.mkdirs()) {
            File file = new File(storageDir.getAbsolutePath(), ts + suffix);
            if (file.createNewFile()) {
                return FileProvider.getUriForFile(context, "com.ruesga.rview.content", file);
            }
        }
        return null;
    }


    public static File getAccountCacheDir(Context context) {
        return getAccountCacheDir(context, Preferences.getAccount(context));
    }

    public static File getAccountCacheDir(Context context, Account account) {
        return new File(context.getCacheDir(), account.getAccountHash());
    }

    public static boolean createAccountCacheDir(Context context) {
        return createAccountCacheDir(context, Preferences.getAccount(context));
    }

    public static boolean createAccountCacheDir(Context context, Account account) {
        final File cacheDir = getAccountCacheDir(context, account);
        return cacheDir.exists() || cacheDir.mkdirs();
    }

    public static void removeAccountCacheDir(Context context) {
        removeAccountCacheDir(context, Preferences.getAccount(context));
    }

    public static void removeAccountCacheDir(Context context, Account account) {
        final File cacheDir = getAccountCacheDir(context, account);
        if (cacheDir.exists()) {
            try {
                FileUtils.deleteDirectory(cacheDir);
            } catch (IOException ex) {
                // Ignore
            }
        }
    }


    public static File getAccountDiffCacheDir(Context context) {
        return getAccountDiffCacheDir(context, Preferences.getAccount(context));
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static File getAccountDiffCacheDir(Context context, Account account) {
        createAccountCacheDir(context, account);
        File cacheDir = new File(getAccountCacheDir(context, account), DIFF_CACHE_FOLDER);
        if (!cacheDir.exists()) {
            cacheDir.mkdir();
        }
        return cacheDir;
    }

    public static void removeAccountDiffCacheDir(Context context) {
        removeAccountDiffCacheDir(context, Preferences.getAccount(context));
    }

    public static void removeAccountDiffCacheDir(Context context, Account account) {
        final File cacheDir = getAccountDiffCacheDir(context, account);
        if (cacheDir.exists()) {
            try {
                FileUtils.deleteDirectory(cacheDir);
            } catch (IOException ex) {
                // Ignore
            }
        }
    }

    public static boolean hasAccountDiffCache(Context context, String name) {
        return hasAccountDiffCache(context, Preferences.getAccount(context), name);
    }

    public static boolean hasAccountDiffCache(Context context, Account account, String name) {
        return new File(getAccountDiffCacheDir(context, account), name).exists();
    }

    public static byte[] readAccountDiffCacheFile(Context context, String name) throws IOException {
        return readAccountDiffCacheFile(context, Preferences.getAccount(context), name);
    }

    public static byte[] readAccountDiffCacheFile(Context context, Account account, String name)
            throws IOException {
        return FileUtils.readFileToByteArray(
                new File(getAccountDiffCacheDir(context, account), name));
    }

    public static void writeAccountDiffCacheFile(Context context, String name, byte[] data)
            throws IOException {
        writeAccountDiffCacheFile(context, Preferences.getAccount(context), name, data);
    }

    public static void writeAccountDiffCacheFile(
            Context context, Account account, String name, byte[] data) throws IOException {
        FileUtils.writeByteArrayToFile(
                new File(getAccountDiffCacheDir(context, account), name), data);
    }

    public static void removeAccountDiffCacheFile(Context context, String name) {
        removeAccountDiffCacheFile(context, Preferences.getAccount(context), name);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void removeAccountDiffCacheFile(Context context, Account account, String name) {
        final File cacheDir = getAccountDiffCacheDir(context, account);
        if (cacheDir.exists()) {
            new File(getAccountDiffCacheDir(context, account), name).delete();
        }
    }



    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static File getAttachmentCacheDir(Context context) {
        File cacheDir = new File(context.getCacheDir(), ATTACHMENT_CACHE_FOLDER);
        if (!cacheDir.exists()) {
            cacheDir.mkdir();
        }
        return cacheDir;
    }

    @SuppressWarnings("ConstantConditions")
    public static File getAttachmentFile(Context context, Attachment attachment) {
        String fileName = Preferences.getAccount(context).getAccountHash()
                + "_" + attachment.mMessageId
                + "_" + attachment.mName;
        return new File(getAttachmentCacheDir(context), fileName);
    }

    public static boolean hasAttachmentFile(Context context, Attachment attachment) {
        return getAttachmentFile(context, attachment).exists();
    }

    public static void downloadAttachmentFile(Context context, Attachment attachment)
            throws IOException {
        OkHttpClient client = new OkHttpClient.Builder()
                .addNetworkInterceptor(chain -> {
                    Response originalResponse = chain.proceed(chain.request());
                    return CacheHelper.addCacheControl(originalResponse.newBuilder()).build();
                })
                .readTimeout(20000, java.util.concurrent.TimeUnit.MILLISECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .addInterceptor(chain -> {
                    ConnectivityManager cm = (ConnectivityManager) context.getSystemService(
                            Context.CONNECTIVITY_SERVICE);
                    NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                    if (activeNetwork != null && activeNetwork.isConnectedOrConnecting()) {
                        return chain.proceed(chain.request());
                    }
                    throw new NoConnectivityException();
                })
                .build();

        Request request = new Request.Builder().url(attachment.mUrl).build();
        Response response = client.newCall(request).execute();
        ResponseBody body = response.body();
        if (body == null) {
            throw new IOException("body was null");
        }
        try {
            if (!response.isSuccessful()) {
                throw new IOException("failed to download file: " + response.code());
            }
            if (body.contentLength() == 0) {
                throw new EmptyMetadataException(attachment.mUrl);
            }

            InputStream is = body.byteStream();
            OutputStream os = new BufferedOutputStream(new FileOutputStream(
                    getAttachmentFile(context, attachment)), 4096);
            try {
                IOUtils.copy(is, os, 4096);
            } finally {
                IOUtils.closeQuietly(is);
                IOUtils.closeQuietly(os);
                response.close();
            }
        } finally {
            try {
                response.close();
            } catch (Exception ex) {
                // Ignore
            }
        }
    }
}
