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

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager.TaskDescription;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;

import com.ruesga.rview.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AndroidHelper {

    private static final String TAG = "AndroidHelper";

    public static boolean isJellyBeanOrGreater() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
    }

    public static boolean isKitkatOrGreater() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
    }

    public static boolean isLollipopOrGreater() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

    public static boolean isNougatOrGreater() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N;
    }

    public static boolean isApiNougatOrGreater() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
    }

    public static boolean isApi28OrGreater() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.P;
    }

    @TargetApi(Build.VERSION_CODES.N)
    @SuppressWarnings("deprecation")
    public static Locale getCurrentLocale(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return context.getResources().getConfiguration().getLocales().get(0);
        }
        return context.getResources().getConfiguration().locale;
    }

    public static void showErrorSnackbar(Context context, @NonNull View parent,
            @StringRes int message) {
        Snackbar snackbar = Snackbar.make(parent, message, Snackbar.LENGTH_LONG);
        View v = snackbar.getView();
        v.setBackgroundColor(ContextCompat.getColor(context, R.color.accent));
        snackbar.show();
    }

    public static void showWarningSnackbar(Context context, @NonNull View parent,
            @StringRes int message) {
        Snackbar snackbar = Snackbar.make(parent, message, Snackbar.LENGTH_LONG);
        View v = snackbar.getView();
        v.setBackgroundColor(ContextCompat.getColor(context, R.color.alert));
        snackbar.show();
    }

    public  static void hideSoftKeyboard(Context context, Window window) {
        if (window == null) {
            return;
        }

        View view = window.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) context.getSystemService(
                    Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    public static String loadRawResourceAsStream(Context ctx) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                ctx.getResources().openRawResource(R.raw.repositories)))) {
            char[] chars = new char[4096];
            int read;
            while ((read = reader.read(chars, 0, 4096)) != -1) {
                sb.append(chars, 0, read);
            }
        }
        return sb.toString();
    }

    @TargetApi(Build.VERSION_CODES.P)
    @SuppressLint("Deprecated")
    public static void configureTaskDescription(Activity activity) {
        if (isLollipopOrGreater()) {
            final TaskDescription taskDesc;
            if (isApi28OrGreater()) {
                taskDesc = new TaskDescription(null, R.mipmap.ic_launcher,
                        ContextCompat.getColor(activity, R.color.primaryDark));
            } else {
                Bitmap icon = BitmapFactory.decodeResource(
                        activity.getResources(), R.mipmap.ic_launcher);
                taskDesc = new TaskDescription(
                        null, icon, ContextCompat.getColor(activity, R.color.primaryDark));
            }
            activity.setTaskDescription(taskDesc);
        }
    }

    public static Intent createCaptureImageIntent(Context context) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(context.getPackageManager()) == null) {
            return null;
        }

        try {
            Uri uri = CacheHelper.createNewTemporaryFileUri(context, ".jpg");
            if (uri != null) {
                intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
                return intent;
            }

        } catch (IOException ex) {
            Log.e(TAG, "Cannot create image capture intent", ex);
        }
        Log.e(TAG, "Cannot create image capture intent: null");
        return null;
    }

    public static String obtainUrlFromClipboard(Context context) {
        String url = null;
        ClipboardManager clipboard =
                (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null && clipboard.getPrimaryClip() != null
                && clipboard.getPrimaryClip().getItemCount() > 0) {
            ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
            CharSequence data = item.getText();
            if (data != null) {
                Matcher m = Pattern.compile(StringHelper.WEB_REGEXP).matcher(data);
                if (m.find()) {
                    url = m.group();
                }
            }
        }
        return url;
    }

    @TargetApi(Build.VERSION_CODES.P)
    public static boolean isRoaming(ConnectivityManager cm) {
        if (isApi28OrGreater()) {
            NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING);
        }

        NetworkInfo ni = cm.getActiveNetworkInfo();
        return ni.isRoaming();
    }
}
