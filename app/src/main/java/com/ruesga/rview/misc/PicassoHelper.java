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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.util.Log;
import android.widget.ImageView;

import com.jakewharton.picasso.OkHttp3Downloader;
import com.ruesga.rview.R;
import com.ruesga.rview.gerrit.model.AccountInfo;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

import java.io.File;
import java.util.List;

import okhttp3.Cache;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

public class PicassoHelper {

    private static final String TAG = "Networking";

    public static Picasso getPicassoClient(Context context) {
        final File cacheDir = CacheHelper.getAccountCacheDir(context);
        OkHttpClient client = new OkHttpClient.Builder()
                .addNetworkInterceptor(chain -> {
                    Response originalResponse = chain.proceed(chain.request());
                    return CacheHelper.addCacheControl(originalResponse.newBuilder()).build();
                })
                .cache(new Cache(cacheDir, CacheHelper.MAX_DISK_CACHE))
                .build();
        OkHttp3Downloader downloader = new OkHttp3Downloader(client);
        return new Picasso.Builder(context)
                .downloader(downloader)
                .build();
    }

    public static Drawable getDefaultAvatar(Context context, @ColorRes int color) {
        Drawable drawable = ContextCompat.getDrawable(context, R.drawable.ic_account_circle);
        DrawableCompat.setTint(drawable, ContextCompat.getColor(context, color));
        return drawable;
    }

    public static void bindAvatar(Context context, Picasso picasso, AccountInfo account,
            ImageView view, Drawable placeholder) {
        final List<String> avatarUrls = ModelHelper.getAvatarUrl(context, account);
        loadWithFallbackUrls(picasso, view, placeholder, avatarUrls);
    }

    private static void loadWithFallbackUrls(final Picasso picasso, final ImageView view,
            final Drawable placeholder, final List<String> urls) {
        if (!urls.isEmpty()) {
            picasso.load(urls.get(0))
                    .placeholder(placeholder)
                    .transform(new CircleTransform())
                    .into(view, new Callback() {
                        @Override
                        public void onSuccess() {
                        }

                        @Override
                        public void onError() {
                            // Next url
                            urls.remove(0);
                            loadWithFallbackUrls(picasso, view, placeholder, urls);
                        }
                    });
        } else {
            // Placeholder
            view.setImageDrawable(placeholder);
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

            Bitmap bitmap = Bitmap.createBitmap(size, size, source.getConfig());

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

}
