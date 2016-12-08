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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.RemoteInput;
import android.util.Log;
import android.widget.Toast;

import com.ruesga.rview.R;
import com.ruesga.rview.gerrit.GerritApi;
import com.ruesga.rview.gerrit.model.DraftActionType;
import com.ruesga.rview.gerrit.model.NotifyType;
import com.ruesga.rview.gerrit.model.ReviewInfo;
import com.ruesga.rview.gerrit.model.ReviewInput;
import com.ruesga.rview.misc.ModelHelper;
import com.ruesga.rview.misc.NotificationsHelper;
import com.ruesga.rview.model.Account;
import com.ruesga.rview.preferences.Constants;
import com.ruesga.rview.providers.NotificationEntity;

import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import me.tatarka.rxloader2.safe.SafeObservable;

public class NotificationReceiver extends BroadcastReceiver {

    private static final String TAG = "NotificationReceiver";

    public static final String ACTION_NOTIFICATION_DISMISSED
            = "com.ruesga.rview.action.NOTIFICATION_DISMISSED";
    public static final String ACTION_NOTIFICATION_REPLY
            = "com.ruesga.rview.action.NOTIFICATION_REPLY";

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();

        if (Intent.ACTION_LOCALE_CHANGED.equals(action)) {
            // If locale changed then recreate the notifications to display the labels
            // in the correct language
            NotificationsHelper.recreateNotifications(context);

        } else if (ACTION_NOTIFICATION_DISMISSED.equals(action)) {
            dismissNotifications(context, intent);

        } else if (ACTION_NOTIFICATION_REPLY.equals(action)) {
            replyToNotification(context, intent);
        }
    }

    private void dismissNotifications(Context context, Intent intent) {
        int groupId = intent.getIntExtra(Constants.EXTRA_NOTIFICATION_GROUP_ID, 0);
        String accountId = intent.getStringExtra(Constants.EXTRA_ACCOUNT_HASH);
        if (groupId != 0) {
            NotificationEntity.dismissGroupNotifications(context, groupId);
        } else if (accountId != null) {
            NotificationEntity.dismissAccountNotifications(context, accountId);
        }
    }

    private void replyToNotification(Context context, Intent intent) {
        Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
        final int changeId = intent.getIntExtra(Constants.EXTRA_LEGACY_CHANGE_ID, -1);
        final String accountId = intent.getStringExtra(Constants.EXTRA_ACCOUNT_HASH);
        final int groupId = intent.getIntExtra(Constants.EXTRA_NOTIFICATION_GROUP_ID, 0);
        if (remoteInput != null && groupId != 0 && changeId >= 0 && accountId != null) {
            final Account account = ModelHelper.getAccountFromHash(context, accountId);
            if (account == null) {
                return;
            }

            CharSequence message = remoteInput.getCharSequence(Constants.EXTRA_COMMENT);
            if (message == null) {
                return;
            }

            performSendReply(context, account, groupId, changeId, message.toString());
        } else {
            // What happens here? Dismiss the notification in case, but don't mark as read
            NotificationsHelper.dismissNotification(context, groupId);
            NotificationEntity.dismissGroupNotifications(context, groupId);
        }
    }

    private void performSendReply(
            Context ctx, Account account, int groupId, int changeId, String msg) {
        final GerritApi api = ModelHelper.getGerritApi(ctx, account);
        ReviewInput input = new ReviewInput();
        input.drafts = DraftActionType.PUBLISH_ALL_REVISIONS;
        input.strictLabels = true;
        input.message = msg;
        input.omitDuplicateComments = true;
        input.notify = NotifyType.ALL;

        // Send the comment
        SafeObservable.fromNullCallable(() ->
                api.setChangeRevisionReview(String.valueOf(changeId),
                        GerritApi.CURRENT_REVISION, input).blockingFirst())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<ReviewInfo>() {
                        @Override
                        public void onSubscribe(Disposable d) {
                        }

                        @Override
                        public void onNext(ReviewInfo value) {
                            NotificationsHelper.dismissNotification(ctx, groupId);
                            NotificationEntity.dismissGroupNotifications(ctx, groupId);
                            NotificationEntity.markGroupNotificationsAsRead(ctx, groupId);
                        }

                        @Override
                        public void onError(Throwable e) {
                            Toast.makeText(ctx, R.string.exception_operation_failed,
                                    Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Failed to send reply to notification", e);
                        }

                        @Override
                        public void onComplete() {
                        }
                });
    }
}
