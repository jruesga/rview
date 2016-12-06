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
import android.app.Activity;
import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.customtabs.CustomTabsIntent;
import android.support.v4.app.Fragment;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import com.ruesga.rview.AccountSettingsActivity;
import com.ruesga.rview.ChangeDetailsActivity;
import com.ruesga.rview.ChangeListByFilterActivity;
import com.ruesga.rview.DiffViewerActivity;
import com.ruesga.rview.R;
import com.ruesga.rview.SearchActivity;
import com.ruesga.rview.TabFragmentActivity;
import com.ruesga.rview.fragments.RelatedChangesFragment;
import com.ruesga.rview.fragments.StatsFragment;
import com.ruesga.rview.gerrit.filter.ChangeQuery;
import com.ruesga.rview.gerrit.model.ChangeInfo;
import com.ruesga.rview.preferences.Constants;
import com.ruesga.rview.preferences.Preferences;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import static com.ruesga.rview.preferences.Constants.EXTRA_FORCE_SINGLE_PANEL;

public class ActivityHelper {

    public static final int LIST_RESULT_CODE = 100;

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

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @SuppressWarnings("deprecation")
    public static void share(Context ctx, String action, String title, String text) {
        try {
            Intent intent = new Intent(android.content.Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_SUBJECT, title);
            intent.putExtra(Intent.EXTRA_TEXT, text);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
            } else {
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
            }
            ctx.startActivity(Intent.createChooser(intent, action));

        } catch (ActivityNotFoundException ex) {
            // Fallback to default browser
            String msg = ctx.getString(R.string.exception_cannot_share_link);
            Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show();
        }
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

    public static void openChangeDetails(Context context, ChangeInfo change,
            boolean withParent, boolean forceSinglePanel) {
        openChangeDetails(context, change.changeId, change.legacyChangeId,
                withParent, forceSinglePanel);
    }

    public static void openChangeDetails(Context context, String changeId, int legacyChangeId,
            boolean withParent, boolean forceSinglePanel) {
        Intent intent = new Intent(context, ChangeDetailsActivity.class);
        intent.putExtra(Constants.EXTRA_CHANGE_ID, changeId);
        intent.putExtra(Constants.EXTRA_LEGACY_CHANGE_ID, legacyChangeId);
        intent.putExtra(Constants.EXTRA_HAS_PARENT, withParent);
        intent.putExtra(Constants.EXTRA_FORCE_SINGLE_PANEL, forceSinglePanel);
        context.startActivity(intent);
    }

    public static void openChangeListByFilterActivity(
            Activity activity, String title, ChangeQuery filter, boolean dirty) {
        Intent intent = new Intent(activity, ChangeListByFilterActivity.class);
        intent.putExtra(Constants.EXTRA_TITLE, title);
        intent.putExtra(Constants.EXTRA_FILTER, filter.toString());
        intent.putExtra(Constants.EXTRA_DIRTY, dirty);
        activity.startActivityForResult(intent, LIST_RESULT_CODE);
    }

    public static void openRelatedChangesActivity(
            Context ctx, ChangeInfo change, String revisionId) {
        Intent intent = new Intent(ctx, TabFragmentActivity.class);

        final String title = ctx.getString(R.string.change_details_title, change.legacyChangeId);
        ArrayList<String> args = new ArrayList<>(
                Arrays.asList(
                        new String[]{
                                String.valueOf(change.legacyChangeId),
                                change.changeId,
                                change.project,
                                revisionId,
                                change.topic}));
        intent.putExtra(Constants.EXTRA_TITLE, title);
        intent.putExtra(Constants.EXTRA_SUBTITLE, change.changeId);
        intent.putExtra(Constants.EXTRA_FRAGMENT, RelatedChangesFragment.class.getName());
        intent.putStringArrayListExtra(Constants.EXTRA_FRAGMENT_ARGS, args);
        intent.putExtra(Constants.EXTRA_HAS_PARENT, true);
        ctx.startActivity(intent);
    }

    public static void openStatsActivity(Context ctx, String title, String subtitle,
            int type, String id, ChangeQuery filter, String extra) {
        Intent intent = new Intent(ctx, TabFragmentActivity.class);

        ArrayList<String> args = new ArrayList<>(
                Arrays.asList(
                        new String[]{
                                String.valueOf(type),
                                id,
                                filter.toString(),
                                extra}));
        intent.putExtra(Constants.EXTRA_TITLE, title);
        intent.putExtra(Constants.EXTRA_SUBTITLE, subtitle);
        intent.putExtra(Constants.EXTRA_FRAGMENT, StatsFragment.class.getName());
        intent.putStringArrayListExtra(Constants.EXTRA_FRAGMENT_ARGS, args);
        intent.putExtra(Constants.EXTRA_HAS_PARENT, true);
        ctx.startActivity(intent);
    }

    public static void openDiffViewerActivity(Fragment fragment, ChangeInfo change,
          ArrayList<String> files, String revisionId, String base, String current,
          String file, int requestCode) {
        Intent intent = new Intent(fragment.getContext(), DiffViewerActivity.class);
        intent.putExtra(Constants.EXTRA_REVISION_ID, revisionId);
        intent.putExtra(Constants.EXTRA_BASE, base);
        intent.putExtra(Constants.EXTRA_FILE, file);
        intent.putExtra(Constants.EXTRA_HAS_PARENT, true);

        try {
            CacheHelper.writeAccountDiffCacheFile(fragment.getContext(),
                    CacheHelper.CACHE_CHANGE_JSON,
                    SerializationManager.getInstance().toJson(change).getBytes());

            String prefix = (base == null ? "0" : base) + "_" + current + "_";
            CacheHelper.writeAccountDiffCacheFile(fragment.getContext(),
                    prefix + CacheHelper.CACHE_FILES_JSON,
                    SerializationManager.getInstance().toJson(files).getBytes());
        } catch (IOException ex) {
            // Ignore
        }

        fragment.startActivityForResult(intent, requestCode);
    }

    public static void openSearchActivity(Context context) {
        Intent intent = new Intent(context, SearchActivity.class);
        context.startActivity(intent);
    }
}
