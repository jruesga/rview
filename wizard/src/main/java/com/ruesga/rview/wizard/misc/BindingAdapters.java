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
package com.ruesga.rview.wizard.misc;

import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.databinding.BindingAdapter;

public class BindingAdapters {
    @BindingAdapter("wizardBindEmpty")
    public static void bindEmpty(View v, String s) {
        v.setVisibility(TextUtils.isEmpty(s) ? View.GONE : View.VISIBLE);
    }

    @BindingAdapter("wizardBindImageTintAttr")
    public static void bindImageTintAttr(ImageView v, int colorAttr) {
        Drawable dw = v.getDrawable();
        if (dw != null) {
            dw = DrawableCompat.wrap(dw.mutate());
            int[] attrs = {colorAttr};
            TypedArray ta = v.getContext().getTheme().obtainStyledAttributes(attrs);
            int color = ta.getResourceId(0, 0);
            ta.recycle();
            if (color != 0) {
                DrawableCompat.setTint(dw, ContextCompat.getColor(v.getContext(), color));
            }
        }
        v.setImageDrawable(dw);
    }
}
