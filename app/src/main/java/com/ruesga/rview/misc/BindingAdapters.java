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

import android.databinding.BindingAdapter;
import android.graphics.drawable.Drawable;
import android.support.annotation.Keep;
import android.support.design.widget.AppBarLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ruesga.rview.R;
import com.ruesga.rview.gerrit.GerritApi;
import com.ruesga.rview.gerrit.model.Features;
import com.ruesga.rview.widget.TagEditTextView;
import com.ruesga.rview.widget.TagEditTextView.Tag;

import java.util.List;
import java.util.Map;

@Keep
@SuppressWarnings("unused")
public class BindingAdapters {

    @BindingAdapter("toolbarScrollFlags")
    public static void toolbarScrollFlags(Toolbar toolbar, boolean hasTabs) {
        AppBarLayout.LayoutParams params = ((AppBarLayout.LayoutParams) toolbar.getLayoutParams());
        params.setScrollFlags(hasTabs ? AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL
                | AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS : 0);
    }

    @BindingAdapter("wrapLayoutWidth")
    public static void wrapLayoutWidth(View v, boolean wrap) {
        v.getLayoutParams().width = wrap
                ? ViewGroup.LayoutParams.MATCH_PARENT
                : ViewGroup.LayoutParams.WRAP_CONTENT;
        v.requestLayout();
    }

    @BindingAdapter("bindLayoutWidth")
    public static void bindLayoutWidth(View v, float width) {
        v.getLayoutParams().width = width < 0
                ? ViewGroup.LayoutParams.MATCH_PARENT
                : (int) Math.ceil(width);
    }

    @BindingAdapter("bindLayoutWeight")
    public static void bindLayoutWeight(View v, float weight) {
        ((LinearLayout.LayoutParams)v.getLayoutParams()).weight = weight;
    }

    @BindingAdapter("bindSelected")
    public static void bindSelected(View v, boolean selected) {
        v.setSelected(selected);
    }

    @BindingAdapter("bindResourceDrawable")
    public static void bindResourceDrawable(ImageView v, Integer resource) {
        if (resource == null || resource == 0) {
            v.setImageDrawable(null);
        } else {
            v.setImageDrawable(ContextCompat.getDrawable(v.getContext(), resource));
        }
    }

    @BindingAdapter("bindDrawable")
    public static void bindDrawable(ImageView v, Drawable drawable) {
        v.setImageDrawable(drawable);
    }

    @BindingAdapter("bindUnwrappedText")
    public static void bindUnwrappedText(TextView v, String text) {
        if (TextUtils.isEmpty(text)) {
            v.setText(null);
            return;
        }

        // Remove line breaks
        v.setText(text.replaceAll("\r\n", " ").replaceAll("\r", " ").replaceAll("\n", " "));
    }

    @BindingAdapter("bindHtml")
    @SuppressWarnings("deprecation")
    public static void bindHtml(TextView v, String text) {
        if (TextUtils.isEmpty(text)) {
            v.setText(null);
            return;
        }

        // Remove line breaks
        v.setText(Html.fromHtml(text));
    }

    @BindingAdapter("bindNull")
    public static void bindNull(View v, Object o) {
        v.setVisibility(o == null ? View.GONE : View.VISIBLE);
    }

    @BindingAdapter("bindEmptyList")
    public static void bindEmptyList(View v, List<?> o) {
        v.setVisibility(o == null || o.isEmpty() ? View.GONE : View.VISIBLE);
    }

    @BindingAdapter("bindEmptyMap")
    public static void bindEmptyMap(View v, Map<?, ?> o) {
        v.setVisibility(o == null || o.isEmpty() ? View.GONE : View.VISIBLE);
    }

    @BindingAdapter("bindBackgroundTint")
    public static void bindBackgroundTint(View v, int color) {
        final Drawable dw = v.getBackground();
        DrawableCompat.setTint(dw, color);
        v.setBackground(dw);
    }

    @BindingAdapter(value={"bindToFeature", "bindToBoolean"})
    public static void bindToFeature(View v, Features feature, Boolean value) {
        GerritApi api = ModelHelper.getGerritApi(v.getContext());
        boolean supported = value && api != null && api.supportsFeature(feature);
        v.setVisibility(supported ? View.VISIBLE : View.GONE);
    }

    @BindingAdapter({"bindTextImageText", "bindTextImageColor", "bindTextImageTextSize"})
    public static void bindTextImageText(ImageView v, String text, Integer color, Float textSize) {
        if (text == null) {
            v.setImageBitmap(null);
        }
        v.setImageBitmap(BitmapUtils.text2Bitmap(text, color, textSize));
    }

    @BindingAdapter({"bindTags"})
    public static void bindTags(TagEditTextView v, String[] tags) {
        if (tags == null) {
            v.setTags(null);
            return;
        }

        int count = tags.length;
        Tag[] t = new Tag[count];
        for (int i = 0; i < count; i++) {
            t[i] = new Tag(TagEditTextView.TAG_MODE.HASH,
                    tags[i], ContextCompat.getColor(v.getContext(), R.color.noscore));
        }
        v.setTags(t);
    }
}
