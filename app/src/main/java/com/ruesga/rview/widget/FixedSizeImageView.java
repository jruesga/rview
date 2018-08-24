/*
 * Copyright (C) 2013 The CyanogenMod Project
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

package com.ruesga.rview.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatImageView;

/**
 * A subclass of ImageView that assumes to be fixed size
 * (not wrap_content / match_parent). Doing so it can
 * optimize the drawable change code paths.
 */
public class FixedSizeImageView extends AppCompatImageView {
    private boolean mSuppressLayoutRequest;

    public FixedSizeImageView(Context context) {
        super(context);
    }

    public FixedSizeImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FixedSizeImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void setImageResource(int resId) {
        mSuppressLayoutRequest = true;
        super.setImageResource(resId);
        mSuppressLayoutRequest = false;
    }

    @Override
    public void setImageURI(Uri uri) {
        mSuppressLayoutRequest = true;
        super.setImageURI(uri);
        mSuppressLayoutRequest = false;
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
        mSuppressLayoutRequest = true;
        super.setImageDrawable(drawable);
        mSuppressLayoutRequest = false;
    }

    @Override
    public void requestLayout() {
        if (!mSuppressLayoutRequest) {
            super.requestLayout();
        }
    }
}