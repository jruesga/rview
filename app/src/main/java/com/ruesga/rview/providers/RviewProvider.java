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

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;

public class RviewProvider extends ContentProvider {

    private static final String TAG = "RviewProvider";

    private static final boolean DEBUG = false;

    public static final String AUTHORITY = "com.ruesga.rview";

    private DatabaseHelper mOpenHelper;

    private static final int NOTIFICATIONS_DATA = 1;
    private static final int NOTIFICATIONS_DATA_ID = 2;

    private static final UriMatcher sURLMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        sURLMatcher.addURI(AUTHORITY, NotificationEntity.TABLE_NAME, NOTIFICATIONS_DATA);
        sURLMatcher.addURI(AUTHORITY, NotificationEntity.TABLE_NAME + "/*", NOTIFICATIONS_DATA_ID);
    }

    public RviewProvider() {
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public String getType(@NonNull Uri uri) {
        int match = sURLMatcher.match(uri);
        switch (match) {
            case NOTIFICATIONS_DATA:
                return "vnd.android.cursor.dir/" + NotificationEntity.TABLE_NAME;
            case NOTIFICATIONS_DATA_ID:
                return "vnd.android.cursor.item/" + NotificationEntity.TABLE_NAME;
            default:
                throw new IllegalArgumentException("Unknown URL");
        }
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection,
                        String where, String[] args, String sort) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        // Generate the body of the query
        int match = sURLMatcher.match(uri);
        switch (match) {
            case NOTIFICATIONS_DATA:
                qb.setTables(NotificationEntity.TABLE_NAME);
                break;
            case NOTIFICATIONS_DATA_ID:
                qb.setTables(NotificationEntity.TABLE_NAME);
                qb.appendWhere(NotificationEntity._ID + "=");
                qb.appendWhere(checkAndReturnValidId(uri));
                break;
            default:
                throw new IllegalArgumentException("Unknown URL " + uri);
        }

        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        Cursor c = qb.query(db, projection, where, args, null, null, sort);
        if (c == null) {
            Log.e(TAG, "Failed query URL: " + uri);
        } else {
            final Context ctx = getContext();
            if (ctx != null) {
                c.setNotificationUri(ctx.getContentResolver(), uri);
            }
        }
        return c;
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues initialValues) {
        long rowId;
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        Uri contentUri;
        switch (sURLMatcher.match(uri)) {
            case NOTIFICATIONS_DATA:
                rowId = db.insert(NotificationEntity.TABLE_NAME, null, initialValues);
                contentUri = NotificationEntity.CONTENT_URI;
                break;
            default:
                throw new IllegalArgumentException("Cannot insert from URL: " + uri);
        }

        Uri ret = ContentUris.withAppendedId(contentUri, rowId);
        notifyChange(ret, null);
        return ret;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String where, String[] args) {
        int count;
        String id = uri.getLastPathSegment();
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        switch (sURLMatcher.match(uri)) {
            case NOTIFICATIONS_DATA:
                count = db.update(NotificationEntity.TABLE_NAME, values, where, args);
                break;
            case NOTIFICATIONS_DATA_ID:
                count = db.update(NotificationEntity.TABLE_NAME, values,
                        NotificationEntity._ID + " = ?", new String[]{id});
                break;
            default: {
                throw new UnsupportedOperationException("Cannot update URL: " + uri);
            }
        }

        if (count > 0) {
            notifyChange(uri, id);
        }
        return count;
    }

    @Override
    public int delete(@NonNull Uri uri, String where, String[] args) {
        int count;
        String pk;
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        switch (sURLMatcher.match(uri)) {
            case NOTIFICATIONS_DATA:
                count = db.delete(NotificationEntity.TABLE_NAME, where, args);
                break;
            case NOTIFICATIONS_DATA_ID:
                pk = checkAndReturnValidId(uri);
                where = NotificationEntity._ID + "=" + pk;
                if (!TextUtils.isEmpty(where)) {
                    where += " AND (" + where + ")";
                }
                count = db.delete(NotificationEntity.TABLE_NAME, where, args);
                break;
            default:
                throw new IllegalArgumentException("Cannot delete from URL: " + uri);
        }

        if (count > 0) {
            notifyChange(uri, null);
        }
        return count;
    }

    @NonNull
    @Override
    public ContentProviderResult[] applyBatch(@NonNull ArrayList<ContentProviderOperation> ops)
            throws OperationApplicationException {
        ContentProviderResult[] results = null;
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            results = super.applyBatch(ops);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return results;
    }

    private void notifyChange(Uri uri, String id) {
        final Context ctx = getContext();
        if (ctx != null) {
            if (DEBUG) {
                if (id != null) {
                    Log.v(TAG, "notifyChange() id: " + id + "; url " + uri);
                } else {
                    Log.v(TAG, "notifyChange() url: " + uri);
                }
            }
            ctx.getContentResolver().notifyChange(uri, null);
        }
    }

    private String checkAndReturnValidId(Uri uri) {
        try {
            if (TextUtils.isEmpty(uri.getLastPathSegment())) {
                throw new IllegalArgumentException("Invalid id " + uri);
            }
            return String.valueOf(Long.parseLong(uri.getLastPathSegment()));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid id " + uri);
        }
    }
}
