/*
 * Copyright (C) 2017 Jorge Ruesga
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
package com.ruesga.rview.attachments.binding;

import android.content.res.TypedArray;
import android.databinding.BindingAdapter;
import android.support.annotation.Keep;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.ImageView;

@Keep
@SuppressWarnings("unused")
public class BindingAdapters {

    @BindingAdapter("bindAttachmentSelected")
    public static void bindAttachmentSelected(View v, boolean selected) {
        v.setSelected(selected);
    }

    @BindingAdapter("bindAttachmentResourceDrawable")
    public static void bindAttachmentResourceDrawable(ImageView v, Integer resource) {
        if (resource == null || resource == 0) {
            v.setImageDrawable(null);
        } else {
            v.setImageDrawable(ContextCompat.getDrawable(v.getContext(), resource));
        }
    }

    @BindingAdapter("bindAttachmentBackgroundResource")
    public static void bindAttachmentBackgroundResource(View v, Integer resource) {
        if (resource == null || resource == 0) {
            v.setBackground(null);
        } else {
            try {
                v.setBackgroundResource(resource);
            } catch (Exception ex) {
                try {
                    TypedArray ta = v.getContext().getTheme().obtainStyledAttributes(
                            new int[]{resource});
                    v.setBackground(ta.getDrawable(0));
                    ta.recycle();
                } catch (Exception ex2) {
                    v.setBackground(null);
                }
            }
        }
    }
}
