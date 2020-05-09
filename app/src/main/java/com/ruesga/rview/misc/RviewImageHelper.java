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
import android.graphics.Rect;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.module.AppGlideModule;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.ruesga.rview.R;
import com.ruesga.rview.gerrit.model.AccountInfo;
import com.ruesga.rview.model.Account;
import com.ruesga.rview.preferences.Preferences;

import java.io.File;
import java.io.InputStream;
import java.util.List;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import okhttp3.Cache;
import okhttp3.OkHttpClient;
import okhttp3.Response;

@GlideModule
public final class RviewImageHelper extends AppGlideModule {

    @Override
    public final boolean isManifestParsingEnabled() {
        return false;
    }

    @Override
    public final void applyOptions(@NonNull Context context, @NonNull GlideBuilder builder) {
        builder.setDefaultRequestOptions(
                new RequestOptions()
                        .format(DecodeFormat.PREFER_ARGB_8888));
    }

    @Override
    public final void registerComponents(
            @NonNull Context context, @NonNull Glide glide, @NonNull Registry registry) {
        final File cacheDir = CacheHelper.getAvatarsCacheDir(context);
        OkHttpClient client = NetworkingHelper.createNetworkClient()
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
                        return response.newBuilder().code(404).build();
                    }
                    return response;
                })
                .cache(new Cache(cacheDir, CacheHelper.MAX_DISK_CACHE))
                .build();
        OkHttpUrlLoader.Factory okhttp3 = new OkHttpUrlLoader.Factory(client);

        glide.getRegistry().replace(GlideUrl.class, InputStream.class, okhttp3);
    }


    public static Drawable getDefaultAvatar(Context context, @ColorRes int color) {
        Drawable dw = ContextCompat.getDrawable(context, R.drawable.ic_account_circle);
        if (dw != null) {
            dw = DrawableCompat.wrap(dw.mutate());
            DrawableCompat.setTint(dw, ContextCompat.getColor(context, color));
        }
        return dw;
    }

    public static void bindImage(
            Context ctx, ImageView view, Drawable placeholder, Uri url, Rect rect) {
        view.setImageDrawable(placeholder);
        GlideApp.with(ctx)
                .load(url)
                .placeholder(placeholder)
                .override(rect.width(), rect.height())
                .optionalCenterCrop()
                .error(R.drawable.ic_broken_image_with_padding)
                .transition(new DrawableTransitionOptions().crossFade())
                .into(view);
    }

    public static void bindAvatar(Context context, AccountInfo account,
                ImageView view, Drawable placeholder) {
        final Account acct = Preferences.getAccount(context);
        final List<String> avatarUrls = ModelHelper.getAvatarUrl(context, acct, account);
        boolean animate = Preferences.isAccountAnimatedAvatars(context, acct);
        view.setTag(R.id.avatar_key_id, account.accountId);
        loadWithFallbackUrls(context, account.accountId, view, placeholder, avatarUrls, 0, 0, animate);
    }

    public static void bindAvatar(Context context, Account acct,
            AccountInfo account, MenuItem item, Drawable placeholder) {
        final int size = context.getResources().getDimensionPixelSize(
                com.ruesga.rview.drawer.R.dimen.drawer_navigation_icon_size);
        boolean animate = Preferences.isAccountAnimatedAvatars(context, acct);
        final List<String> avatarUrls = ModelHelper.getAvatarUrl(context, acct, account);
        loadWithFallbackUrls(context, account.accountId, item, placeholder, avatarUrls, size, size, animate);
    }

    @SuppressWarnings({"unchecked", "deprecation"})
    private static void loadWithFallbackUrls(final Context context, int accountId, final Object into,
            final Drawable placeholder, final List<String> urls, int width, int height,
            boolean animate) {
        final String nextUrl;
        synchronized (urls) {
            nextUrl = urls.isEmpty() ? null : urls.get(0);
        }
        drawInto(into, accountId, placeholder, animate);
        if (nextUrl != null) {
            final Target target = new SimpleTarget<Drawable>() {
                @Override
                public void onResourceReady(@NonNull Drawable resource,
                    @Nullable Transition<? super Drawable> transition) {
                    synchronized (urls) {
                        urls.clear();
                        urls.add(nextUrl);
                    }
                    drawInto(into, accountId, resource, animate);
                }

                @Override
                public void onLoadFailed(@Nullable Drawable errorDrawable) {
                    // Next url
                    synchronized (urls) {
                        urls.remove(nextUrl);
                    }
                    loadWithFallbackUrls(context, accountId, into, placeholder,
                            urls, width, height, animate);
                }
            };

            GlideRequest request =
                    GlideApp.with(context)
                        .load(nextUrl)
                        .transforms(new CircleCrop())
                        .placeholder(placeholder)
                        .transition(new DrawableTransitionOptions().crossFade());
            if (width > 0 && height > 0) {
                request = request.override(width, height);
            }
            request.into(target);
        }
    }

    private static void drawInto(Object into, int accountId, Drawable dw, boolean animate) {
        if (into instanceof ImageView) {
            int newAccountId = (int)((ImageView) into).getTag(R.id.avatar_key_id);
            if (accountId != newAccountId) {
                return;
            }
            ((ImageView) into).setImageDrawable(dw);
        } else if (into instanceof MenuItem) {
            ((MenuItem) into).setIcon(dw);
        }

        // Animate
        if (animate && dw instanceof Animatable) {
            ((Animatable)dw).start();
        }
    }

    private static boolean isGravatarIdenticonUrl(String url) {
        return url.contains("www.gravatar.com") && url.contains("identicon");
    }
}
