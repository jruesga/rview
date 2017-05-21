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

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.RemoteInput;
import android.support.v4.content.ContextCompat;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;

import com.google.gson.reflect.TypeToken;
import com.ruesga.rview.ChangeDetailsActivity;
import com.ruesga.rview.NotificationsActivity;
import com.ruesga.rview.R;
import com.ruesga.rview.gerrit.model.AccountInfo;
import com.ruesga.rview.gerrit.model.CloudNotificationEvents;
import com.ruesga.rview.model.Account;
import com.ruesga.rview.preferences.Constants;
import com.ruesga.rview.providers.NotificationEntity;
import com.ruesga.rview.receivers.NotificationReceiver;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class NotificationsHelper {

    private static final String TAG = "Notifications";

    private static final String NOTIFICATION_KEY_GROUP = "rview-";

    public static int generateGroupId(com.ruesga.rview.model.Notification notification) {
        String hash = notification.token + "/" + notification.change;
        return FowlerNollVo.fnv1_32(hash.getBytes()).intValue();
    }

    public static int generateGroupId(Account account, String changeId) {
        String hash = account.getAccountHash() + "/" + changeId;
        return FowlerNollVo.fnv1_32(hash.getBytes()).intValue();
    }

    public static void dismissNotification(Context ctx, int groupId) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(ctx);
        notificationManager.cancel(groupId);
    }

    @SuppressWarnings("Convert2streamapi")
    public static void recreateNotifications(Context ctx) {
        List<NotificationEntity> entities = NotificationEntity.getAllNotifications(ctx, true, true);
        SparseArray<Account> notifications = new SparseArray<>();
        for (NotificationEntity entity : entities) {
            if (notifications.indexOfKey(entity.mGroupId) < 0) {
                notifications.put(entity.mGroupId,
                        ModelHelper.getAccountFromHash(ctx, entity.mAccountId));
            }
        }

        int count = notifications.size();
        for (int i = 0; i < count; i++) {
            int groupId = notifications.keyAt(i);
            Account account = notifications.valueAt(i);
            dismissNotification(ctx, groupId);
            createNotification(ctx, account, groupId, false);
        }
    }

    public static boolean canHandleNotification(
            Context ctx, com.ruesga.rview.model.Notification notification) {
        switch (notification.event) {
            case CloudNotificationEvents.CHANGE_ABANDONED_EVENT:
            case CloudNotificationEvents.CHANGE_MERGED_EVENT:
            case CloudNotificationEvents.CHANGE_RESTORED_EVENT:
            case CloudNotificationEvents.CHANGE_REVERTED_EVENT:
            case CloudNotificationEvents.COMMENT_ADDED_EVENT:
            case CloudNotificationEvents.DRAFT_PUBLISHED_EVENT:
            case CloudNotificationEvents.HASHTAG_CHANGED_EVENT:
            case CloudNotificationEvents.PATCHSET_CREATED_EVENT:
            case CloudNotificationEvents.TOPIC_CHANGED_EVENT:
            case CloudNotificationEvents.ASSIGNEE_CHANGED_EVENT:
            case CloudNotificationEvents.VOTE_DELETED_EVENT:
                return true;

            case CloudNotificationEvents.REVIEWER_ADDED_EVENT:
                // Made this event looks like like an advise about when others added the
                // current user to the change (like email notifications). Ignore the
                // rest of the add or delete reviewer events
                Account me = ModelHelper.getAccountFromHash(ctx, notification.token);
                List<AccountInfo> reviewers = getReviewers(notification.extra);
                for (AccountInfo reviewer : reviewers) {
                    if (me != null && isSameAccount(me.mAccount, reviewer)) {
                        return true;
                    }
                }
                return false;
        }
        return false;
    }

    @SuppressWarnings("Convert2streamapi")
    public static void createNotification(
            Context ctx, Account account, long groupId, boolean feedback) {
        List<NotificationEntity> entities =
                NotificationEntity.getAllGroupNotifications(ctx, groupId, true, true);
        if (entities.isEmpty()) {
            Log.w(TAG, "There isn't notification to display for group " + groupId);
            return;
        }

        // Determine the best suitable way to display notifications
        if (entities.size() == 1) {
            // Single notification
            createSingleNotification(ctx, account, entities.get(0), feedback);
        } else {
            // Group notification
            createGroupNotifications(ctx, account, entities, feedback);
        }

        // Create an account group summary notification
        if (AndroidHelper.isNougatOrGreater()) {
            List<NotificationEntity> accountEntities =
                    NotificationEntity.getAllAccountNotifications(
                            ctx, account.getAccountHash(), true, true);
            Set<String> notifications = new LinkedHashSet<>();
            for (NotificationEntity entity : accountEntities) {
                notifications.add(entity.mNotification.subject);
            }

            if (notifications.size() > 1) {
                createSummaryGroupNotification(ctx, account, notifications, feedback);
            }
        }
    }

    private static void createSingleNotification(
            Context ctx, Account account, NotificationEntity entity, boolean feedback) {
        if (AndroidHelper.isNougatOrGreater()) {
            List<NotificationEntity> entities = new ArrayList<>();
            entities.add(entity);
            createBundledNotifications(ctx, account, entities, feedback);
        } else {
            NotificationCompat.Builder builder =
                    createNotificationBuilder(ctx, account, entity, feedback);
            builder.setStyle(
                    new NotificationCompat.BigTextStyle()
                            .bigText(getContentMessage(ctx, entity, false, false)))
                    .setGroup(NOTIFICATION_KEY_GROUP + account.getAccountHash());
            publishNotification(ctx, builder.build(), entity.mGroupId);
        }
    }

    private static void createGroupNotifications(
            Context ctx, Account account, List<NotificationEntity> entities, boolean feedback) {
        if (AndroidHelper.isNougatOrGreater()) {
            createBundledNotifications(ctx, account, entities, feedback);
        } else {
            createInboxNotification(ctx, account, entities, feedback);
        }
    }

    private static void createInboxNotification(
            Context ctx, Account account, List<NotificationEntity> entities, boolean feedback) {
        NotificationEntity lastEntity = entities.get(entities.size() - 1);
        NotificationCompat.Builder builder =
                createNotificationBuilder(ctx, account, lastEntity, feedback);
        NotificationCompat.InboxStyle style = new NotificationCompat.InboxStyle()
                .setBigContentTitle(lastEntity.mNotification.subject)
                .setSummaryText(getNotificationSubText(account));
        for (NotificationEntity entity : entities) {
            style.addLine(getContentMessage(ctx, entity, false, true));
        }
        builder.setStyle(style)
                .setNumber(entities.size())
                .setGroup(NOTIFICATION_KEY_GROUP  + account.getAccountHash());

        publishNotification(ctx, builder.build(), lastEntity.mGroupId);
    }

    @TargetApi(Build.VERSION_CODES.N)
    private static void createBundledNotifications(
            Context ctx, Account account, List<NotificationEntity> entities, boolean feedback) {

        NotificationEntity lastEntity = entities.get(entities.size() - 1);
        NotificationCompat.Builder builder =
                createNotificationBuilder(ctx, account, lastEntity, feedback);
        NotificationCompat.MessagingStyle style = new NotificationCompat.MessagingStyle("")
                .setConversationTitle(lastEntity.mNotification.subject);
        for (NotificationEntity entity : entities) {
            String author = getEventAuthor(ctx, entity);
            style.addMessage(getContentMessage(ctx, entity, true, false), entity.mWhen, author);
        }
        builder.setStyle(style)
                .setNumber(entities.size())
                .setGroup(NOTIFICATION_KEY_GROUP + account.getAccountHash());

        createInlineReply(ctx, builder, lastEntity);

        publishNotification(ctx, builder.build(), lastEntity.mGroupId);
    }

    @TargetApi(Build.VERSION_CODES.N)
    private static void createSummaryGroupNotification(
            Context ctx, Account account, Set<String> notifications, boolean feedback) {
        int notificationId = FowlerNollVo.fnv1_32(account.getAccountHash().getBytes()).intValue();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx)
                .setContentTitle(getNotificationSubText(account))
                .setSubText(getNotificationSubText(account))
                .setSmallIcon(R.drawable.ic_stat_notify)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_SOCIAL)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setDefaults(feedback ? NotificationCompat.DEFAULT_ALL : 0)
                .setColor(ContextCompat.getColor(ctx, R.color.primaryDark))
                .setContentIntent(
                        getViewAccountChangesPendingIntent(ctx, account, notificationId))
                .setDeleteIntent(
                        getDeleteAccountNotificationPendingIntent(ctx, account, notificationId))
                .setOnlyAlertOnce(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        NotificationCompat.InboxStyle style = new NotificationCompat.InboxStyle()
                .setSummaryText(getNotificationSubText(account));
        builder.setStyle(style)
                .setNumber(notifications.size())
                .setGroupSummary(true)
                .setGroup(NOTIFICATION_KEY_GROUP + account.getAccountHash());

        publishNotification(ctx, builder.build(), notificationId);
    }


    private static NotificationCompat.Builder createNotificationBuilder(
            Context ctx, Account account, NotificationEntity entity, boolean feedback) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx);
        return builder
                .setContentTitle(entity.mNotification.subject)
                .setContentText(getContentTitle(ctx, entity, true))
                .setSubText(getNotificationSubText(account))
                .setSmallIcon(R.drawable.ic_stat_notify)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_SOCIAL)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setDefaults(feedback ? NotificationCompat.DEFAULT_ALL : 0)
                .setWhen(entity.mWhen)
                .setColor(ContextCompat.getColor(ctx, R.color.primaryDark))
                .setContentIntent(getViewChangePendingIntent(ctx, entity))
                .setDeleteIntent(getDeleteGroupNotificationPendingIntent(ctx, entity.mGroupId))
                .setOnlyAlertOnce(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
    }

    private static PendingIntent getViewChangePendingIntent(
            Context ctx, NotificationEntity entity) {
        Intent intent = new Intent(ctx, ChangeDetailsActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra(Constants.EXTRA_ACCOUNT_HASH, entity.mAccountId);
        intent.putExtra(Constants.EXTRA_NOTIFICATION_GROUP_ID, entity.mGroupId);
        intent.putExtra(Constants.EXTRA_CHANGE_ID, entity.mNotification.change);
        intent.putExtra(Constants.EXTRA_LEGACY_CHANGE_ID, entity.mNotification.legacyChangeId);
        intent.putExtra(Constants.EXTRA_HAS_PARENT, false);
        intent.putExtra(Constants.EXTRA_FORCE_SINGLE_PANEL, true);

        return PendingIntent.getActivity(
                ctx, entity.mGroupId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private static PendingIntent getViewAccountChangesPendingIntent(
            Context ctx, Account account, int notificationId) {
        Intent intent = new Intent(ctx, NotificationsActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra(Constants.EXTRA_ACCOUNT_HASH, account.getAccountHash());
        intent.putExtra(Constants.EXTRA_HAS_PARENT, false);

        return PendingIntent.getActivity(
                ctx, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private static PendingIntent getDeleteGroupNotificationPendingIntent(
            Context ctx, int groupId) {
        Intent intent = new Intent(NotificationReceiver.ACTION_NOTIFICATION_DISMISSED);
        intent.putExtra(Constants.EXTRA_NOTIFICATION_GROUP_ID, groupId);

        return PendingIntent.getBroadcast(
                ctx, groupId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private static PendingIntent getDeleteAccountNotificationPendingIntent(
            Context ctx, Account account, int notificationId) {
        Intent intent = new Intent(NotificationReceiver.ACTION_NOTIFICATION_DISMISSED);
        intent.putExtra(Constants.EXTRA_ACCOUNT_HASH, account.getAccountHash());

        return PendingIntent.getBroadcast(
                ctx, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private static PendingIntent getReplyPendingIntent(
            Context ctx, NotificationEntity entity) {
        Intent intent = new Intent(NotificationReceiver.ACTION_NOTIFICATION_REPLY);
        intent.putExtra(Constants.EXTRA_LEGACY_CHANGE_ID, entity.mNotification.legacyChangeId);
        intent.putExtra(Constants.EXTRA_ACCOUNT_HASH, entity.mAccountId);
        intent.putExtra(Constants.EXTRA_NOTIFICATION_GROUP_ID, entity.mGroupId);

        return PendingIntent.getBroadcast(
                ctx, entity.mGroupId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private static String getNotificationSubText(Account account) {
        return account.getRepositoryDisplayName() + " / " + account.getAccountDisplayName();
    }

    private static void publishNotification(Context ctx, Notification notification, int groupId) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(ctx);
        notificationManager.notify(groupId, notification);
    }

    @TargetApi(Build.VERSION_CODES.N)
    private static void createInlineReply(
            Context ctx, NotificationCompat.Builder builder, NotificationEntity entity) {
        RemoteInput remoteInput = new RemoteInput.Builder(Constants.EXTRA_COMMENT)
                .setLabel(ctx.getString(R.string.change_details_review_hint))
                .setAllowFreeFormInput(true)
                .build();
        NotificationCompat.Action action =
                new NotificationCompat.Action.Builder(
                        R.drawable.ic_send,
                        ctx.getString(R.string.action_reply),
                        getReplyPendingIntent(ctx, entity))
                .addRemoteInput(remoteInput)
                .setAllowGeneratedReplies(false)
                .build();
        builder.addAction(action);
    }

    private static CharSequence getContentTitle(
            Context ctx, NotificationEntity entity, boolean withEventAuthor) {
        String event = getNotificationTitle(ctx, entity);
        if (event == null) {
            return null;
        }
        if (!withEventAuthor) {
            return event;
        }

        String author = getEventAuthor(ctx, entity);
        return String.format("%s %s", author, event);
    }

    @SuppressWarnings("deprecation")
    public static CharSequence getContentMessage(
            Context ctx, NotificationEntity entity, boolean bundled, boolean inbox) {
        final String event = getNotificationTitle(ctx, entity);
        if (event == null) {
            return null;
        }

        final String message = getNotificationMessage(entity);

        if (bundled) {
            return Html.fromHtml(String.format("%s%s", event, message));
        }
        String author = getEventAuthor(ctx, entity);
        return Html.fromHtml(String.format("<b>%s</b> %s%s", author, event,
                inbox ? "" : "<br/>" + message.trim().replaceAll("\n", "<br/>")));
    }

    private static String getEventAuthor(Context ctx, NotificationEntity entity) {
        String author = null;
        if (entity.mNotification.who != null) {
            author = ModelHelper.getAccountDisplayName(entity.mNotification.who);
        }
        if (author == null) {
            author = ctx.getString(R.string.account_anonymous_coward);
        }
        return author;
    }

    private static String getNotificationTitle(Context ctx, NotificationEntity entity) {
        switch (entity.mNotification.event) {
            case CloudNotificationEvents.CHANGE_ABANDONED_EVENT:
                return ctx.getString(R.string.notification_content_title_1);
            case CloudNotificationEvents.CHANGE_MERGED_EVENT:
                return ctx.getString(R.string.notification_content_title_2);
            case CloudNotificationEvents.CHANGE_RESTORED_EVENT:
                return ctx.getString(R.string.notification_content_title_4);
            case CloudNotificationEvents.CHANGE_REVERTED_EVENT:
                return ctx.getString(R.string.notification_content_title_8);
            case CloudNotificationEvents.COMMENT_ADDED_EVENT:
                return ctx.getString(R.string.notification_content_title_16);
            case CloudNotificationEvents.DRAFT_PUBLISHED_EVENT:
                return ctx.getString(R.string.notification_content_title_32);
            case CloudNotificationEvents.HASHTAG_CHANGED_EVENT:
                return ctx.getString(R.string.notification_content_title_64);
            case CloudNotificationEvents.REVIEWER_ADDED_EVENT:
                // Made this event looks like like an advise about when others added the
                // current user to the change (like email notifications). Ignore the
                // rest of the add or delete reviewer events
                Account me = ModelHelper.getAccountFromHash(ctx, entity.mAccountId);
                List<AccountInfo> reviewers = getReviewers(entity.mNotification.extra);
                for (AccountInfo reviewer : reviewers) {
                    if (me != null && isSameAccount(me.mAccount, reviewer)) {
                        return ctx.getString(R.string.notification_content_title_128);
                    }
                }
            case CloudNotificationEvents.PATCHSET_CREATED_EVENT:
                return ctx.getString(R.string.notification_content_title_512);
            case CloudNotificationEvents.TOPIC_CHANGED_EVENT:
                return ctx.getString(R.string.notification_content_title_1024);
            case CloudNotificationEvents.ASSIGNEE_CHANGED_EVENT:
                AccountInfo assignee = SerializationManager.getInstance().fromJson(
                        entity.mNotification.extra, AccountInfo.class);
                return ctx.getString(R.string.notification_content_title_2048,
                        ModelHelper.getAccountDisplayName(assignee));
            case CloudNotificationEvents.VOTE_DELETED_EVENT:
                return ctx.getString(R.string.notification_content_title_4096);
        }
        return null;
    }

    private static String getNotificationMessage(NotificationEntity entity) {
        String msg = " ";
        switch (entity.mNotification.event) {
            case CloudNotificationEvents.COMMENT_ADDED_EVENT:
                msg += entity.mNotification.extra;
        }
        return msg;
    }

    @SuppressWarnings("RedundantIfStatement")
    private static boolean isSameAccount(AccountInfo o1, AccountInfo o2) {
        // Since not always account return
        if (o1.accountId == o2.accountId) {
            return true;
        }
        if (o1.username != null && o2.username != null && o1.username.equals(o2.username)) {
            return true;
        }
        if (o1.email != null && o2.email != null && o1.email.equals(o2.email)) {
            return true;
        }
        return false;
    }

    private static List<AccountInfo> getReviewers(String serialized) {
        if (TextUtils.isEmpty(serialized)) {
            return new ArrayList<>();
        }

        List<AccountInfo> reviewers = new ArrayList<>();
        if (serialized.startsWith("[")) {
            // Version 2.14. Returns an array of added reviewers
            Type type = new TypeToken<List<AccountInfo>>(){}.getType();
            reviewers.addAll(SerializationManager.getInstance().fromJson(serialized, type));
        } else {
            // Version 2.13. Returns the added reviewer
            reviewers.add(SerializationManager.getInstance().fromJson(
                    serialized, AccountInfo.class));
        }

        return reviewers;
    }
}
