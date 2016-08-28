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

    public static Response.Builder addCacheControl(Response.Builder builder) {
        return builder.header("Cache-Control", "max-age=" + MAX_AGE_CACHE);
    }
}
