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

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.text.format.DateUtils;

import com.ruesga.rview.BuildConfig;
import com.ruesga.rview.misc.SerializationManager;
import com.ruesga.rview.model.Notification;

import java.util.ArrayList;
import java.util.List;

public class NotificationEntity implements BaseColumns, Parcelable {

    static final String TABLE_NAME = "notifications";

    private static final long MAX_NOTIFICATION_TIME_TO_LIVE = DateUtils.DAY_IN_MILLIS * 15L;

    public static final Uri CONTENT_URI = Uri.parse("content://" + BuildConfig.APPLICATION_ID + "/" + TABLE_NAME);

    static final String GROUP_ID = "group_id";
    static final String ACCOUNT_ID = "account_id";
    static final String WHEN = "_when";
    static final String READ = "read";
    static final String DISMISSED = "dismissed";
    static final String NOTIFICATION = "notification";

    private static final String[] ALL_FIELDS_PROJECTION = {
            _ID,
            GROUP_ID,
            ACCOUNT_ID,
            WHEN,
            READ,
            DISMISSED,
            NOTIFICATION
    };

    private static final int MESSAGE_ID_IDX = 0;
    private static final int GROUP_ID_IDX = 1;
    private static final int ACCOUNT_ID_IDX = 2;
    private static final int WHEN_IDX = 3;
    private static final int READ_IDX = 4;
    private static final int DISMISSED_IDX = 5;
    private static final int NOTIFICATION_IDX = 6;

    public final long mMessageId;
    public final int mGroupId;
    public final String mAccountId;
    public final long mWhen;
    public final boolean mRead;
    public final boolean mDismissed;
    public final Notification mNotification;

    public static List<NotificationEntity> getAllNotifications(
            Context context, boolean unread, boolean undismissed) {
        List<NotificationEntity> stats = new ArrayList<>();
        ContentResolver cr = context.getContentResolver();
        String where = null;
        if (unread) {
            where = READ + " = 0";
        }
        if (undismissed) {
            if (TextUtils.isEmpty(where)) {
                where = DISMISSED + " = 0";
            } else {
                where += " and " + DISMISSED + " = 0";
            }
        }
        String sort = ACCOUNT_ID + " ASC, " + GROUP_ID + " ASC, " + WHEN + " ASC";
        Cursor c  = cr.query(CONTENT_URI, ALL_FIELDS_PROJECTION, where, null, sort);
        if (c != null) {
            try {
                if (c.moveToFirst()) {
                    do {
                        stats.add(new NotificationEntity(c));
                    } while (c.moveToNext());
                }
            } finally {
                c.close();
            }
        }
        return stats;
    }

    public static List<NotificationEntity> getAllGroupNotifications(
            Context context, long groupId, boolean unread, boolean undismissed) {
        List<NotificationEntity> stats = new ArrayList<>();
        ContentResolver cr = context.getContentResolver();
        String where = GROUP_ID + " = ?";
        if (unread) {
            where += " and " + READ + " = 0";
        }
        if (undismissed) {
            where += " and " + DISMISSED + " = 0";
        }
        String[] args = {String.valueOf(groupId)};
        String sort = WHEN + " ASC";
        Cursor c  = cr.query(CONTENT_URI, ALL_FIELDS_PROJECTION, where, args, sort);
        if (c != null) {
            try {
                if (c.moveToFirst()) {
                    do {
                        stats.add(new NotificationEntity(c));
                    } while (c.moveToNext());
                }
            } finally {
                c.close();
            }
        }
        return stats;
    }

    public static List<NotificationEntity> getAllAccountNotifications(
            Context context, String accountId, boolean unread, boolean undismissed) {
        List<NotificationEntity> stats = new ArrayList<>();
        ContentResolver cr = context.getContentResolver();
        String where = ACCOUNT_ID + " = ?";
        if (unread) {
            where += " and " + READ + " = 0";
        }
        if (undismissed) {
            where += " and " + DISMISSED + " = 0";
        }
        String[] args = {String.valueOf(accountId)};
        String sort = WHEN + " ASC";
        Cursor c  = cr.query(CONTENT_URI, ALL_FIELDS_PROJECTION, where, args, sort);
        if (c != null) {
            try {
                if (c.moveToFirst()) {
                    do {
                        stats.add(new NotificationEntity(c));
                    } while (c.moveToNext());
                }
            } finally {
                c.close();
            }
        }
        return stats;
    }

    public static void addOrUpdate(Context context, NotificationEntity entity) {
        ContentResolver cr = context.getContentResolver();
        Uri uri = ContentUris.withAppendedId(CONTENT_URI, entity.mMessageId);
        ContentValues values = new ContentValues();
        values.put(GROUP_ID, entity.mGroupId);
        values.put(ACCOUNT_ID, entity.mAccountId);
        values.put(WHEN, entity.mWhen);
        values.remove(READ);
        values.remove(DISMISSED);
        values.put(NOTIFICATION, SerializationManager.getInstance().toJson(entity.mNotification));

        if (cr.update(uri, values, null, null) == 0) {
            values.put(_ID, entity.mMessageId);
            cr.insert(CONTENT_URI, values);
        }
    }

    public static void markGroupNotificationsAsRead(Context context, int groupId) {
        ContentResolver cr = context.getContentResolver();
        ContentValues values = new ContentValues();
        values.put(READ, 1);
        String where = GROUP_ID + " = ?";
        String[] args = new String[]{String.valueOf(groupId)};
        cr.update(CONTENT_URI, values, where, args);

        // Truncate notifications
        truncateNotifications(context);
    }

    public static void markAccountNotificationsAsRead(Context context, String accountId) {
        ContentResolver cr = context.getContentResolver();
        ContentValues values = new ContentValues();
        values.put(READ, 1);
        String where = ACCOUNT_ID + " = ?";
        String[] args = new String[]{accountId};
        cr.update(CONTENT_URI, values, where, args);

        // Truncate notifications
        truncateNotifications(context);
    }

    public static void dismissGroupNotifications(Context context, int groupId) {
        ContentResolver cr = context.getContentResolver();
        ContentValues values = new ContentValues();
        values.put(DISMISSED, 1);
        String where = GROUP_ID + " = ?";
        String[] args = new String[]{String.valueOf(groupId)};
        cr.update(CONTENT_URI, values, where, args);

        // Truncate notifications
        truncateNotifications(context);
    }

    public static void dismissAccountNotifications(Context context, String accountId) {
        ContentResolver cr = context.getContentResolver();
        ContentValues values = new ContentValues();
        values.put(DISMISSED, 1);
        String where = ACCOUNT_ID + " = ?";
        String[] args = new String[]{accountId};
        cr.update(CONTENT_URI, values, where, args);

        // Truncate notifications
        truncateNotifications(context);
    }

    public static void deleteAccountNotifications(Context context, String accountId) {
        ContentResolver cr = context.getContentResolver();
        String where = ACCOUNT_ID + " = ?";
        String[] args = new String[]{String.valueOf(accountId)};
        cr.delete(CONTENT_URI, where, args);
    }

    public static void truncateNotifications(Context context) {
        ContentResolver cr = context.getContentResolver();
        String where = WHEN + " <= ?";
        String[] args = new String[]{String.valueOf(
                System.currentTimeMillis() - MAX_NOTIFICATION_TIME_TO_LIVE)};
        cr.delete(CONTENT_URI, where, args);
    }

    public NotificationEntity(long messageId, int groupId, String accountId,
            long when, Notification notification) {
        mMessageId = messageId;
        mGroupId = groupId;
        mAccountId = accountId;
        mWhen = when;
        mRead = false;
        mDismissed = false;
        mNotification = notification;
    }

    public NotificationEntity(Cursor c) {
        mMessageId = c.getLong(MESSAGE_ID_IDX);
        mGroupId = c.getInt(GROUP_ID_IDX);
        mAccountId = c.getString(ACCOUNT_ID_IDX);
        mWhen = c.getLong(WHEN_IDX);
        mRead = c.getInt(READ_IDX) == 1;
        mDismissed = c.getInt(DISMISSED_IDX) == 1;
        mNotification = SerializationManager.getInstance().fromJson(
                c.getString(NOTIFICATION_IDX), Notification.class);
    }

    protected NotificationEntity(Parcel in) {
        mMessageId = in.readLong();
        mGroupId = in.readInt();
        mAccountId = in.readString();
        mWhen = in.readLong();
        mRead = in.readInt() == 1;
        mDismissed = in.readInt() == 1;
        mNotification = SerializationManager.getInstance().fromJson(
                in.readString(), Notification.class);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeLong(mMessageId);
        parcel.writeLong(mGroupId);
        parcel.writeString(mAccountId);
        parcel.writeLong(mWhen);
        parcel.writeInt(mRead ? 1 : 0);
        parcel.writeInt(mDismissed ? 1 : 0);
        parcel.writeString(SerializationManager.getInstance().toJson(mNotification));
    }

    public static final Creator<NotificationEntity> CREATOR = new Creator<NotificationEntity>() {
        @Override
        public NotificationEntity createFromParcel(Parcel in) {
            return new NotificationEntity(in);
        }

        @Override
        public NotificationEntity[] newArray(int size) {
            return new NotificationEntity[size];
        }
    };
}
