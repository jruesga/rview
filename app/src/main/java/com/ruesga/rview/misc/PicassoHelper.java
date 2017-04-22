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
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.ImageView;

import com.jakewharton.picasso.OkHttp3Downloader;
import com.ruesga.rview.R;
import com.ruesga.rview.gerrit.model.AccountInfo;
import com.ruesga.rview.model.Account;
import com.ruesga.rview.preferences.Preferences;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import com.squareup.picasso.Transformation;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import okhttp3.Cache;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class PicassoHelper {

    @SuppressLint("StaticFieldLeak")
    private static Picasso sPicasso;

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private final static Set<Target> sTargets = new HashSet<>();

    public static Picasso getPicassoClient(Context context) {
        if (sPicasso == null) {
            final File cacheDir = CacheHelper.getAvatarsCacheDir(context);
            OkHttpClient client = new OkHttpClient.Builder()
                    .addNetworkInterceptor(chain -> {
                        Response originalResponse = chain.proceed(chain.request());
                        return CacheHelper.addCacheControl(originalResponse.newBuilder()).build();
                    })
                    .addInterceptor(chain -> {
                        Response response = chain.proceed(chain.request());

                        // Skip Gravatars identicons. They are ugly.
                        String url = response.request().url().toString();
                        String contentDisposition = response.header("Content-Disposition");
                        if (isGravatarIdenticonUrl(url) && TextUtils.isEmpty(contentDisposition)) {
                            // Gravatars identicons doesn't have an inline content disposition
                            response.close();
                            return null;
                        }
                        return response;
                    })
                    .cache(new Cache(cacheDir, CacheHelper.MAX_DISK_CACHE))
                    .build();
            OkHttp3Downloader downloader = new OkHttp3Downloader(client);
            sPicasso = new Picasso.Builder(context.getApplicationContext())
                    .defaultBitmapConfig(Bitmap.Config.ARGB_8888)
                    .downloader(downloader)
                    .build();
        }
        return sPicasso;
    }

    public static Drawable getDefaultAvatar(Context context, @ColorRes int color) {
        Drawable drawable = ContextCompat.getDrawable(context, R.drawable.ic_account_circle);
        DrawableCompat.setTint(drawable, ContextCompat.getColor(context, color));
        return drawable;
    }

    public static void bindAvatar(Context context, Picasso picasso, AccountInfo account,
                ImageView view, Drawable placeholder) {
        final Account acct = Preferences.getAccount(context);
        final List<String> avatarUrls = ModelHelper.getAvatarUrl(context, acct, account);
        loadWithFallbackUrls(picasso, view, placeholder, avatarUrls);
    }

    public static void bindAvatar(Context context, Picasso picasso, Account acct,
            AccountInfo account, MenuItem item, Drawable placeholder) {
        final List<String> avatarUrls = ModelHelper.getAvatarUrl(context, acct, account);
        loadWithFallbackUrls(context, picasso, item, placeholder, avatarUrls);
    }

    private static void loadWithFallbackUrls(final Picasso picasso, final ImageView view,
            final Drawable placeholder, final List<String> urls) {
        final String nextUrl;
        synchronized (urls) {
            nextUrl = urls.isEmpty() ? null : urls.get(0);
        }
        if (nextUrl != null) {
            picasso.load(nextUrl)
                    .placeholder(placeholder)
                    .transform(new CircleTransform())
                    .into(view, new Callback() {
                        @Override
                        public void onSuccess() {
                            synchronized (urls) {
                                urls.clear();
                                urls.add(nextUrl);
                            }
                        }

                        @Override
                        public void onError() {
                            // Next url
                            synchronized (urls) {
                                if (urls.contains(nextUrl)) {
                                    urls.remove(nextUrl);
                                }
                            }
                            loadWithFallbackUrls(picasso, view, placeholder, urls);
                        }
                    });
        } else {
            // Placeholder
            view.setImageDrawable(placeholder);
        }
    }

    private static void loadWithFallbackUrls(final Context context, final Picasso picasso,
            final Object into, final Drawable placeholder, final List<String> urls) {
        final String nextUrl;
        synchronized (urls) {
            nextUrl = urls.isEmpty() ? null : urls.get(0);
        }

        final Target target = new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom loadedFrom) {
                sTargets.remove(this);

                synchronized (urls) {
                    urls.clear();
                    urls.add(nextUrl);
                }

                BitmapDrawable dw = new BitmapDrawable(context.getResources(), bitmap);
                drawInto(dw);
            }

            @Override
            public void onBitmapFailed(Drawable drawable) {
                sTargets.remove(this);

                // Next url
                synchronized (urls) {
                    if (urls.contains(nextUrl)) {
                        urls.remove(nextUrl);
                    }
                }
                loadWithFallbackUrls(context, picasso, into, placeholder, urls);
            }

            @Override
            public void onPrepareLoad(Drawable drawable) {
                drawInto(drawable);
            }

            private void drawInto(Drawable dw) {
                if (into instanceof ImageView) {
                    ((ImageView) into).setImageDrawable(dw);

                } else if (into instanceof MenuItem) {
                    // Resize to fit into menu icon
                    int size = context.getResources().getDimensionPixelSize(
                            com.ruesga.rview.drawer.R.dimen.drawer_navigation_icon_size);
                    Bitmap resized = Bitmap.createScaledBitmap(
                            BitmapUtils.convertToBitmap(dw), size, size, false);
                    Drawable icon = new BitmapDrawable(context.getResources(), resized);
                    dw.setBounds(0, 0, size, size);
                    ((MenuItem) into).setIcon(icon);
                }
            }
        };
        sTargets.add(target);

        if (nextUrl != null) {
            picasso.load(nextUrl)
                    .placeholder(placeholder)
                    .transform(new CircleTransform())
                    .into(target);
        } else {
            // Placeholder
            target.onPrepareLoad(placeholder);
        }
    }

    // http://stackoverflow.com/questions/26112150/android-create-circular-image-with-picasso
    private static class CircleTransform implements Transformation {
        @Override
        public Bitmap transform(Bitmap source) {
            int size = Math.min(source.getWidth(), source.getHeight());

            int x = (source.getWidth() - size) / 2;
            int y = (source.getHeight() - size) / 2;

            Bitmap squaredBitmap = Bitmap.createBitmap(source, x, y, size, size);
            if (squaredBitmap != source) {
                source.recycle();
            }

            // Some images could return a null config, just fallback to a default format
            Bitmap.Config config = source.getConfig() != null
                    ? source.getConfig() : Bitmap.Config.ARGB_8888;
            Bitmap bitmap = Bitmap.createBitmap(size, size, config);

            Canvas canvas = new Canvas(bitmap);
            Paint paint = new Paint();
            BitmapShader shader = new BitmapShader(squaredBitmap,
                    BitmapShader.TileMode.CLAMP, BitmapShader.TileMode.CLAMP);
            paint.setShader(shader);
            paint.setAntiAlias(true);

            float r = size / 2f;
            canvas.drawCircle(r, r, r, paint);

            squaredBitmap.recycle();
            return bitmap;
        }

        @Override
        public String key() {
            return "circle";
        }
    }

    private static boolean isGravatarIdenticonUrl(String url) {
        return url.contains("www.gravatar.com") && url.contains("identicon");
    }
}
