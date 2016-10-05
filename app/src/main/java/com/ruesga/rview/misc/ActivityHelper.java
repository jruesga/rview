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

import android.app.Activity;
import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.customtabs.CustomTabsIntent;
import android.support.v4.app.Fragment;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import com.ruesga.rview.ChangeDetailsActivity;
import com.ruesga.rview.ChangeListByFilterActivity;
import com.ruesga.rview.DiffViewerActivity;
import com.ruesga.rview.R;
import com.ruesga.rview.RelatedChangesActivity;
import com.ruesga.rview.SearchActivity;
import com.ruesga.rview.gerrit.filter.ChangeQuery;
import com.ruesga.rview.gerrit.model.ChangeInfo;
import com.ruesga.rview.preferences.Constants;
import com.ruesga.rview.preferences.Preferences;

public class ActivityHelper {

    public static void openUriInCustomTabs(Activity activity, String uri) {
        openUriInCustomTabs(activity, Uri.parse(uri));
    }

    public static void openUriInCustomTabs(Activity activity, Uri uri) {
        // Check user preferences
        if (!Preferences.isAccountUseCustomTabs(activity, Preferences.getAccount(activity))) {
            openUri(activity, uri);
            return;
        }

        try {
            CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
            builder.setShowTitle(true);
            builder.setToolbarColor(ContextCompat.getColor(activity, R.color.primaryDark));
            CustomTabsIntent intent = builder.build();
            intent.launchUrl(activity, uri);

        } catch (ActivityNotFoundException ex) {
            // Fallback to default browser
            openUri(activity, uri);
        }
    }

    public static void openUri(Context ctx, Uri uri) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            ctx.startActivity(intent);

        } catch (ActivityNotFoundException ex) {
            // Fallback to default browser
            String msg = ctx.getString(R.string.exception_browser_not_found, uri.toString());
            Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show();
        }
    }

    public static void shareTextPlain(Context ctx, String text, String title) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_TEXT, text);
        intent.setType("text/plain");
        ctx.startActivity(Intent.createChooser(intent, title));
    }

    public static void downloadUri(Context context, Uri uri, @Nullable String mimeType) {
        // Use the download manager to perform the download
        DownloadManager downloadManager =
                (DownloadManager) context.getSystemService(Activity.DOWNLOAD_SERVICE);
        Request request = new Request(uri)
                .setAllowedOverMetered(false)
                .setAllowedOverRoaming(false)
                .setNotificationVisibility(Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        if (mimeType != null) {
            request.setMimeType(mimeType);
        }
        request.allowScanningByMediaScanner();
        downloadManager.enqueue(request);
    }

    public static boolean performFinishActivity(Activity activity, boolean forceNavigateUp) {
        boolean hasParent = activity.getIntent().getBooleanExtra(Constants.EXTRA_HAS_PARENT, false);
        if (forceNavigateUp || !hasParent) {
            Intent upIntent = NavUtils.getParentActivityIntent(activity);
            if (NavUtils.shouldUpRecreateTask(activity, upIntent)) {
                TaskStackBuilder.create(activity)
                        .addNextIntentWithParentStack(upIntent)
                        .startActivities();
            } else {
                NavUtils.navigateUpTo(activity, upIntent);
            }
            return true;
        }
        activity.finish();
        return true;
    }

    public static void openChangeDetails(Context context, ChangeInfo change, boolean withParent) {
        Intent intent = new Intent(context, ChangeDetailsActivity.class);
        intent.putExtra(Constants.EXTRA_CHANGE_ID, change.changeId);
        intent.putExtra(Constants.EXTRA_LEGACY_CHANGE_ID, change.legacyChangeId);
        intent.putExtra(Constants.EXTRA_HAS_PARENT, withParent);
        context.startActivity(intent);
    }

    public static void openChangeListByFilterActivity(
            Context context, String title, ChangeQuery filter) {
        Intent intent = new Intent(context, ChangeListByFilterActivity.class);
        intent.putExtra(Constants.EXTRA_TITLE, title);
        intent.putExtra(Constants.EXTRA_FILTER, filter.toString());
        context.startActivity(intent);
    }

    public static void openRelatedChangesActivity(
            Context context, ChangeInfo change, String revisionId) {
        Intent intent = new Intent(context, RelatedChangesActivity.class);
        intent.putExtra(Constants.EXTRA_LEGACY_CHANGE_ID, change.legacyChangeId);
        intent.putExtra(Constants.EXTRA_CHANGE_ID, change.changeId);
        intent.putExtra(Constants.EXTRA_PROJECT_ID, change.project);
        intent.putExtra(Constants.EXTRA_REVISION_ID, revisionId);
        intent.putExtra(Constants.EXTRA_TOPIC, change.topic);
        intent.putExtra(Constants.EXTRA_HAS_PARENT, true);
        context.startActivity(intent);
    }

    public static void openDiffViewerActivity(Fragment fragment, ChangeInfo change,
            String revisionId, String file, int requestCode) {
        Intent intent = new Intent(fragment.getContext(), DiffViewerActivity.class);
        intent.putExtra(Constants.EXTRA_REVISION_ID, revisionId);
        intent.putExtra(Constants.EXTRA_FILE, file);
        intent.putExtra(Constants.EXTRA_DATA, SerializationManager.getInstance().toJson(change));
        intent.putExtra(Constants.EXTRA_HAS_PARENT, true);
        fragment.startActivityForResult(intent, requestCode);
    }

    public static void openSearchActivity(Context context) {
        Intent intent = new Intent(context, SearchActivity.class);
        context.startActivity(intent);
    }
}
