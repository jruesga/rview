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
package com.ruesga.rview.services;

import android.text.TextUtils;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.ruesga.rview.gerrit.model.AccountInfo;
import com.ruesga.rview.misc.FowlerNollVo;
import com.ruesga.rview.misc.ModelHelper;
import com.ruesga.rview.misc.NotificationsHelper;
import com.ruesga.rview.misc.SerializationManager;
import com.ruesga.rview.model.Account;
import com.ruesga.rview.model.Notification;
import com.ruesga.rview.preferences.Preferences;
import com.ruesga.rview.providers.NotificationEntity;

import java.util.Map;
import java.util.UUID;

public class NotificationService extends FirebaseMessagingService {

    private static final String TAG = "NotificationService";

    @Override
    public void onMessageReceived(RemoteMessage message) {
        // Process the message
        String messageId = message.getMessageId();
        if (TextUtils.isEmpty(messageId)) {
            // Some old samsung devices return a null message id
            // http://stackoverflow.com/questions/38007894/fcm-android-null-message-id
            // Since we only need a unique identifier, just generate a random one for this cases
            messageId = UUID.randomUUID().toString();
        }
        final long notificationId = FowlerNollVo.fnv1_64(messageId.getBytes()).longValue();
        final Notification notification = createNotification(message.getData());
        if (notification == null) {
            // Skip this message. It was an unparseable message
            Log.w(TAG, "Received an unparseable messsage. Skip it.");
            return;
        }
        String serializedNotification = SerializationManager.getInstance().toJson(notification);
        Log.i(TAG, "Received notification: " + serializedNotification);

        final int groupId = NotificationsHelper.generateGroupId(notification);
        final Account account = ModelHelper.getAccountFromHash(this, notification.token);
        if (account == null) {
            Log.w(TAG, "Can't handle notification because because account is unknown");
            return;
        }

        // Did we missed an unregistration operation?
        if (!Preferences.isAccountNotificationsEnabled(this, account)) {
            Log.i(TAG, "Can't handle notification because account has disabled notifications");

            // Register device
            DeviceRegistrationService.register(this, account);
            return;
        }

        if (!NotificationsHelper.canHandleNotification(this, notification)) {
            Log.i(TAG, "Can't handle notification because it's unsupported");
            return;
        }

        // Save notification into the provider
        NotificationEntity entity = new NotificationEntity(notificationId, groupId,
                account.getAccountHash(), notification.when, notification);
        NotificationEntity.addOrUpdate(this, entity);

        // Display the notification
        NotificationsHelper.createNotification(this, account, groupId, true);
    }

    private Notification createNotification(Map<String, String> data) {
        if (data == null || data.isEmpty()) {
            return null;
        }
        Notification notification = new Notification();
        notification.when = Long.valueOf(data.get("when")) * 1000L;
        notification.who = SerializationManager.getInstance().fromJson(
                data.get("who"), AccountInfo.class);
        notification.token = data.get("token");
        notification.event = Integer.valueOf(data.get("event"));
        notification.change = data.get("change");
        notification.legacyChangeId = Integer.valueOf(data.get("legacyChangeId"));
        notification.revision = data.get("revision");
        notification.project = data.get("project");
        notification.branch = data.get("branch");
        notification.topic = data.get("topic");
        notification.subject = data.get("subject");
        notification.extra = data.get("extra");
        return notification;
    }

}
