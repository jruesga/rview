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
package com.ruesga.rview.text;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.style.ReplacementSpan;
import android.util.DisplayMetrics;
import android.util.TypedValue;

public class TagSpan extends ReplacementSpan {
    private final int mBackgroundColor;
    private final int mForegroundColor;

    private final float mMarginLeftRight;
    private final float mMarginTopBottom;
    private final float mRadius;

    private final RectF mRect = new RectF();

    public TagSpan(Context context, @ColorInt int backgroundColor, @ColorInt int foregroundColor) {
        mBackgroundColor = backgroundColor;
        mForegroundColor = foregroundColor;

        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        mMarginLeftRight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3, metrics);
        mMarginTopBottom = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, metrics);
        mRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3, metrics);
    }

    @Override
    public int getSize(@NonNull Paint paint, CharSequence text, int start, int end,
            @Nullable Paint.FontMetricsInt fm) {
        return Math.round(paint.measureText(text, start, end) + (mMarginLeftRight * 2));
    }

    @Override
    public void draw(@NonNull Canvas canvas, CharSequence text, int start, int end,
            float x, int top, int y, int bottom, @NonNull Paint paint) {
        // Draw background
        mRect.set(
                x,
                top - mMarginTopBottom,
                x + paint.measureText(text, start, end) + (mMarginLeftRight * 2),
                bottom + mMarginTopBottom);
        paint.setColor(mBackgroundColor);
        canvas.drawRoundRect(mRect, mRadius, mRadius, paint);

        // Draw foreground
        float tx = x + mMarginLeftRight;
        paint.setColor(mBackgroundColor);
        paint.setColor(mForegroundColor);
        paint.setAntiAlias(true);
        paint.setFakeBoldText(true);
        canvas.drawText(text, start, end, tx, y, paint);
    }
}
