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

import com.ruesga.rview.model.Account;
import com.ruesga.rview.preferences.Preferences;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

import okhttp3.Response;

public class CacheHelper {

    public static final long MAX_AGE_CACHE = 60 * 60 * 24 * 5L;
    public static final long MAX_DISK_CACHE = 5 * 1024 * 1024L;

    private static final String DIFF_CACHE_FOLDER = "diff";

    public static final String CACHE_CHANGE_JSON = "change.json";
    public static final String CACHE_DIFF_JSON = "diff.json";
    public static final String CACHE_COMMENTS_JSON = "comments.json";
    public static final String CACHE_DRAFT_JSON = "drafts.json";
    public static final String CACHE_CONTENT = "content";
    public static final String CACHE_PARENT = "parent";

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

    public static boolean hasAccountDiffCacheDir(Context context, String name) {
        return hasAccountDiffCacheDir(context, Preferences.getAccount(context), name);
    }

    public static boolean hasAccountDiffCacheDir(Context context, Account account, String name) {
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

    public static Response.Builder addCacheControl(Response.Builder builder) {
        return builder.header("Cache-Control", "max-age=" + MAX_AGE_CACHE);
    }
}
