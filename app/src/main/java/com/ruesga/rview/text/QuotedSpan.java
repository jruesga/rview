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
package com.ruesga.rview.text;

import android.annotation.SuppressLint;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.ColorInt;
import android.text.Layout;
import android.text.style.QuoteSpan;

@SuppressLint("ParcelCreator")
public class QuotedSpan extends QuoteSpan {

    private final int mWidth;
    private final int mMargin;
    private final int mIndent;
    private final int mMaxIndent;

    public QuotedSpan(@ColorInt int color, int width, int margin, int indent, int maxIndent) {
        super(color);
        mWidth = width;
        mMargin = margin;
        mIndent = indent;
        mMaxIndent = maxIndent;
    }

    @Override
    public int getLeadingMargin(boolean first) {
        return mWidth + (mMaxIndent * mMargin) + mMargin;
    }

    @Override
    public void drawLeadingMargin(Canvas c, Paint p, int x, int dir, int top, int baseline,
            int bottom, CharSequence text, int start, int end, boolean first, Layout layout) {
        Paint.Style style = p.getStyle();
        int color = p.getColor();

        p.setStyle(Paint.Style.FILL);
        p.setColor(getColor());

        int xx = (mMargin * mIndent) + x;
        c.drawRect(xx, top, xx + dir * mWidth, bottom, p);

        p.setStyle(style);
        p.setColor(color);
    }
}
