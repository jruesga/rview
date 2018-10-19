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
package com.ruesga.rview.receivers;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.format.DateUtils;
import android.util.Log;

import com.ruesga.rview.misc.CacheHelper;

import java.io.File;
import java.util.Calendar;

public class CacheCleanerReceiver extends BroadcastReceiver {

    private static final String TAG = "CacheCleanerReceiver";

    public static final String ACTION_CLEAN_CACHE = "com.ruesga.rview.action.CLEAN_CACHE";

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent != null ? intent.getAction() : null;
        if (ACTION_CLEAN_CACHE.equals(action)) {
            cleanCache(context, false);
        }
    }

    public static void cleanCache(Context context, boolean force) {
        cleanAttachmentCache(context, false);
        cleanPrivateDirectory(context, force);
        schedule(context);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static void cleanAttachmentCache(Context context, boolean force) {
        long yesterday = System.currentTimeMillis() - DateUtils.DAY_IN_MILLIS;
        File cacheDir = CacheHelper.getAttachmentCacheDir(context);
        for (File attachment : cacheDir.listFiles()) {
            if (force || attachment.lastModified() < yesterday) {
                Log.d(TAG, "Deleting cached attachment: " + attachment.getAbsolutePath());
                attachment.delete();
            }
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static void cleanPrivateDirectory(Context context, boolean force) {
        long yesterday = System.currentTimeMillis() - DateUtils.DAY_IN_MILLIS;
        File storageDir = context.getFilesDir();
        if (storageDir.exists()) {
            for (File file : storageDir.listFiles()) {
                if (force || (!file.isDirectory() && file.lastModified() < yesterday)) {
                    Log.d(TAG, "Deleting picture: " + file.getAbsolutePath());
                    file.delete();
                }
            }
        }
    }

    public static void schedule(Context context) {
        // Awake every day at 2:05PM
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 14);
        c.set(Calendar.MINUTE, 5);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        long now = System.currentTimeMillis();
        if (c.getTimeInMillis() < now) {
            c.add(Calendar.DAY_OF_YEAR, 1);
        }
        long due = c.getTimeInMillis();

        Intent i = new Intent(context, CacheCleanerReceiver.class);
        i.setAction(CacheCleanerReceiver.ACTION_CLEAN_CACHE);
        PendingIntent pi = PendingIntent.getBroadcast(
                context, 1000, i, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.set(AlarmManager.RTC_WAKEUP, due, pi);
    }
}
