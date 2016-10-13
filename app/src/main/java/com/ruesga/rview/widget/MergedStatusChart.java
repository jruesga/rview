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
package com.ruesga.rview.widget;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.v4.view.ViewCompat;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import com.ruesga.rview.R;
import com.ruesga.rview.gerrit.model.ChangeStatus;
import com.ruesga.rview.model.Stats;

import java.util.List;

public class MergedStatusChart extends View {

    private final Object mLock = new Object();

    private final RectF mViewArea = new RectF();
    private final RectF mOpenRect = new RectF();
    private final RectF mMergedRect = new RectF();
    private final RectF mAbandonedRect = new RectF();

    private final Paint mOpenPaint;
    private final Paint mMergedPaint;
    private final Paint mAbandonedPaint;
    private final TextPaint mLabelPaint;

    private float mHeightBarPadding;
    private float mMinBarWidth;

    private int mTotal = 0;
    private int mOpen = 0;
    private int mMerged = 0;
    private int mAbandoned = 0;

    private float mLabelPadding;
    private float mLabelHeight;

    public MergedStatusChart(Context context) {
        this(context, null);
    }

    public MergedStatusChart(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MergedStatusChart(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        final Resources res = getResources();
        mHeightBarPadding = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 5, res.getDisplayMetrics());
        mMinBarWidth = 0f;
        int openColor = Color.WHITE;
        int mergedColor = Color.WHITE;
        int abandonedColor = Color.WHITE;

        Resources.Theme theme = context.getTheme();
        TypedArray a = theme.obtainStyledAttributes(
                attrs, R.styleable.MergedStatusChart, defStyleAttr, 0);
        int n = a.getIndexCount();
        for (int i = 0; i < n; i++) {
            int attr = a.getIndex(i);
            switch (attr) {
                case R.styleable.MergedStatusChart_heightBarPadding:
                    mHeightBarPadding = a.getDimension(attr, mHeightBarPadding);

                case R.styleable.MergedStatusChart_minBarWidth:
                    mMinBarWidth = a.getDimension(attr, mMinBarWidth);
                    break;

                case R.styleable.MergedStatusChart_openColor:
                    openColor = a.getColor(attr, openColor);
                    break;

                case R.styleable.MergedStatusChart_mergedColor:
                    mergedColor = a.getColor(attr, mergedColor);
                    break;

                case R.styleable.MergedStatusChart_abandonedColor:
                    abandonedColor = a.getColor(attr, abandonedColor);
                    break;
            }
        }
        a.recycle();

        mOpenPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mOpenPaint.setStyle(Paint.Style.FILL);
        mMergedPaint = new Paint(mOpenPaint);
        mAbandonedPaint = new Paint(mOpenPaint);
        mOpenPaint.setColor(openColor);
        mMergedPaint.setColor(mergedColor);
        mAbandonedPaint.setColor(abandonedColor);

        mLabelPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG | Paint.LINEAR_TEXT_FLAG);
        mLabelPaint.setTextAlign(Paint.Align.RIGHT);
        mLabelPaint.setFakeBoldText(true);
        mLabelPaint.setColor(Color.WHITE);
        mLabelPaint.setAlpha(180);
        mLabelPaint.setTextSize(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, 8f, res.getDisplayMetrics()));
        mLabelPadding = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, 2f, res.getDisplayMetrics());
        Rect bounds = new Rect();
        mLabelPaint.getTextBounds("0", 0, 1, bounds);
        mLabelHeight = bounds.height();

        if (getBackground() == null) {
            setBackgroundColor(Color.TRANSPARENT);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawRect(mOpenRect, mOpenPaint);
        canvas.drawRect(mMergedRect, mMergedPaint);
        canvas.drawRect(mAbandonedRect, mAbandonedPaint);

        canvas.drawText(
                String.valueOf(mOpen),
                mOpenRect.right - mLabelPadding,
                mOpenRect.top + (mOpenRect.height() / 2) + (mLabelHeight / 2),
                mLabelPaint);
        canvas.drawText(
                String.valueOf(mMerged),
                mMergedRect.right - mLabelPadding,
                mMergedRect.top + (mMergedRect.height() / 2) + (mLabelHeight / 2),
                mLabelPaint);
        canvas.drawText(
                String.valueOf(mAbandoned),
                mAbandonedRect.right - mLabelPadding,
                mAbandonedRect.top + (mAbandonedRect.height() / 2) + (mLabelHeight / 2),
                mLabelPaint);
    }

    public void update(List<Stats> stats) {
        int total = 0, open = 0, merged = 0, abandoned = 0;
        for (Stats s : stats) {
            if (s.mStatus.equals(ChangeStatus.NEW)) {
                open++;
            } else if (s.mStatus.equals(ChangeStatus.MERGED)) {
                merged++;
            } else if (s.mStatus.equals(ChangeStatus.ABANDONED)) {
                abandoned++;
            }
            total++;
        }
        synchronized (mLock) {
            mOpen = open;
            mMerged = merged;
            mAbandoned = abandoned;
            mTotal = total;
            computeDrawObjects();
        }
        ViewCompat.postInvalidateOnAnimation(this);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        // View area
        mViewArea.set(
                getPaddingLeft(),
                getPaddingTop(),
                getWidth() - getPaddingRight(),
                getHeight() - getPaddingBottom());

        // Create the drawing objects
        synchronized (mLock) {
            computeDrawObjects();
        }
    }

    private void computeDrawObjects() {
        float barHeight = (mViewArea.height() - (mHeightBarPadding * 4)) / 3;
        float totalWidth = mViewArea.width();
        mOpenRect.set(
                0,
                mHeightBarPadding,
                Math.max((totalWidth * mOpen / mTotal), mMinBarWidth),
                mHeightBarPadding + barHeight);
        mMergedRect.set(
                0,
                (mHeightBarPadding * 2) + barHeight,
                Math.max((totalWidth * mMerged / mTotal), mMinBarWidth),
                (mHeightBarPadding * 2) + (barHeight * 2));
        mAbandonedRect.set(
                0,
                (mHeightBarPadding * 3) + (barHeight * 2),
                Math.max((totalWidth * mAbandoned / mTotal), mMinBarWidth),
                (mHeightBarPadding * 3) + (barHeight * 3));
    }
}
