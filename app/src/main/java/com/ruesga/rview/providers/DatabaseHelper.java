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
package com.ruesga.rview.providers;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "DatabaseHelper";

    private static final int VERSION = 1;
    private static final String DATABASE_NAME = "rview.db";

    DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.i(TAG, "Database created at version level " + VERSION);
        recreateDatabase(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i(TAG, "Database was upgraded from " + oldVersion + " to " + newVersion + ".");
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i(TAG, "Database was downgraded from " + oldVersion + " to " + newVersion + ".");
        recreateDatabase(db);
    }

    private void recreateDatabase(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + NotificationEntity.TABLE_NAME + ";");
        createNotificationsTable(db);
    }

    private void createNotificationsTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + NotificationEntity.TABLE_NAME + " (" +
                NotificationEntity._ID + " INTEGER PRIMARY KEY NOT NULL, " +
                NotificationEntity.GROUP_ID + " INTEGER NOT NULL, " +
                NotificationEntity.ACCOUNT_ID + " TEXT NOT NULL, " +
                NotificationEntity.WHEN + " INTEGER NOT NULL, " +
                NotificationEntity.NOTIFICATION + " TEXT NOT NULL);");
        db.execSQL("CREATE INDEX " + NotificationEntity.TABLE_NAME + "_"
                + NotificationEntity.GROUP_ID + "_idx ON " +
                NotificationEntity.TABLE_NAME +"(" + NotificationEntity.GROUP_ID + ");");
        db.execSQL("CREATE INDEX " + NotificationEntity.TABLE_NAME + "_"
                + NotificationEntity.ACCOUNT_ID + "_idx ON " +
                NotificationEntity.TABLE_NAME +"(" + NotificationEntity.ACCOUNT_ID + ");");
        Log.i(TAG, NotificationEntity.TABLE_NAME + " table created.");
    }
}