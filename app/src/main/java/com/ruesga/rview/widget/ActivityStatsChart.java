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

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.AsyncTask;
import android.support.v4.view.ViewCompat;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.AccelerateInterpolator;

import com.ruesga.rview.R;
import com.ruesga.rview.model.Stats;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class ActivityStatsChart extends View {

    private class AggregateStatsTask extends AsyncTask<Void, Void, Void> {
        private final List<Stats> mStats;

        AggregateStatsTask(List<Stats> stats) {
            mStats = stats;
        }

        @Override
        protected Void doInBackground(Void... params) {
            updateView(aggregateStats(mStats));
            return null;
        }

        @Override
        protected void onPostExecute(Void v) {
            // Refresh the view
            animateChart();
        }

        @SuppressLint("UseSparseArrays")
        private Map<Long, Integer> aggregateStats(List<Stats> stats) {
            Map<Long, Integer> aggregatedStats = new HashMap<>();
            Calendar e = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            truncateCalendar(e);
            Calendar s = (Calendar) e.clone();
            s.add(Calendar.DAY_OF_YEAR, (MAX_DAYS - 1) * -1);

            while (s.compareTo(e) <= 0) {
                aggregatedStats.put(s.getTimeInMillis(), 0);
                s.add(Calendar.DAY_OF_YEAR, 1);
            }

            Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            for (Stats stat : stats) {
                c.setTimeInMillis(stat.mDate.getTime());
                truncateCalendar(c);

                // If the event is not in our map just ignore it
                long timestamp = c.getTimeInMillis();
                if (aggregatedStats.containsKey(timestamp)) {
                    aggregatedStats.put(timestamp, aggregatedStats.get(timestamp) + 1);
                }
            }

            return aggregatedStats;
        }

        private void truncateCalendar(Calendar c) {
            c.set(Calendar.HOUR_OF_DAY, 0);
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);
        }

        private void updateView(Map<Long, Integer> aggregatedStats) {
            float[] data = new float[MAX_DAYS];
            float max = 0f;
            float min = -1f;
            int i = 0;
            for (Long key : aggregatedStats.keySet()) {
                data[i] = aggregatedStats.get(key);
                i++;
            }
            for (float v : data) {
                max = Math.max(max, v);
                if (min == -1) {
                    min = v;
                } else {
                    min = Math.min(min, v);
                }
            }

            // Swap the data
            synchronized (mLock) {
                mData = data;
                mMinVal = Math.abs(min);
                mMaxVal = max;
                computeDrawObjects();
            }
        }
    }


    private static final int MAX_Y_TICKS_LABELS = 5;
    private static final int MAX_DAYS = 30;

    private float[] mData;
    private final String[] mYTicksLabels = new String[MAX_Y_TICKS_LABELS];
    private float mMinVal, mMaxVal;
    private boolean mDataAvailable;

    private final RectF mViewArea = new RectF();
    private final PointF mPoint = new PointF();

    private final Path mLinePath = new Path();
    private final Path mAreaPath = new Path();

    private final Object mLock = new Object();

    private final Paint mLinePaint;
    private final Paint mAreaPaint;
    private final Paint mGridLinesPaint;
    private final TextPaint mTicksPaint;

    private final DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.getDefault());
    private final DecimalFormat mYTicksFormmater = new DecimalFormat("#0", symbols);
    private final DecimalFormat mYTicksDecFormmater = new DecimalFormat("#0.00", symbols);

    private ValueAnimator mAnimator;
    private float mAnimationDelta = 0f;

    private AggregateStatsTask mTask;

    public ActivityStatsChart(Context context) {
        this(context, null);
    }

    public ActivityStatsChart(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ActivityStatsChart(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        final Resources r = getResources();
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        int color = Color.DKGRAY;
        int textColor = Color.WHITE;

        Resources.Theme theme = context.getTheme();
        TypedArray a = theme.obtainStyledAttributes(
                attrs, R.styleable.ActivityStatsChart, defStyleAttr, 0);
        int n = a.getIndexCount();
        for (int i = 0; i < n; i++) {
            int attr = a.getIndex(i);
            switch (attr) {
                case R.styleable.ActivityStatsChart_charLineColor:
                    color = a.getColor(attr, color);
                    break;

                case R.styleable.ActivityStatsChart_charLineTextColor:
                    textColor = a.getColor(attr, textColor);
                    break;
            }
        }
        a.recycle();

        mLinePaint = new Paint();
        mLinePaint.setStyle(Paint.Style.STROKE);
        mLinePaint.setColor(color);
        mLinePaint.setStrokeWidth(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 1.5f, r.getDisplayMetrics()));

        mAreaPaint = new Paint(mLinePaint);
        mAreaPaint.setStyle(Paint.Style.FILL);
        mAreaPaint.setAlpha(180);

        mGridLinesPaint = new Paint();
        mGridLinesPaint.setStyle(Paint.Style.STROKE);
        mGridLinesPaint.setStrokeWidth(
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 0.5f, r.getDisplayMetrics()));
        mGridLinesPaint.setAlpha(90);
        mGridLinesPaint.setPathEffect(new DashPathEffect(new float[]{10, 10}, 0));

        mTicksPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG | Paint.LINEAR_TEXT_FLAG);
        mTicksPaint.setColor(textColor);
        mTicksPaint.setTextAlign(Paint.Align.RIGHT);
        mTicksPaint.setAlpha(180);
        mTicksPaint.setTextSize(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, 8f, r.getDisplayMetrics()));

        // Ensure we have a background. Otherwise it will not draw anything
        if (getBackground() == null) {
            setBackgroundColor(Color.TRANSPARENT);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (mTask != null) {
            mTask.cancel(true);
        }

        if (mAnimator != null && mAnimator.isRunning()) {
            mAnimator.cancel();
        }
    }

    public void update(List<Stats> stats) {
        if (mTask != null) {
            mTask.cancel(true);
        }

        mTask = new AggregateStatsTask(stats);
        mTask.execute();
    }

    private void computeDrawObjects() {
        final float steps = (mMaxVal - mMinVal) * 0.1f;
        final float min = Math.max(0, mMinVal - steps);
        final float max = mMaxVal + steps;

        // Choose the right formatter based on the max/min diff
        final DecimalFormat formatter = (max - min) < (MAX_Y_TICKS_LABELS * 2)
                ? mYTicksDecFormmater : mYTicksFormmater;

        mLinePath.reset();
        mAreaPath.reset();
        if (mData != null && mData.length > 0) {
            for (int i = 0; i < mData.length; i++) {
                fillPointFromValue(i, mData[i], min, max);
                if (i == 0) {
                    mLinePath.moveTo(mPoint.x, mPoint.y);
                    mAreaPath.moveTo(mPoint.x, mPoint.y);
                } else {
                    mLinePath.lineTo(mPoint.x, mPoint.y);
                    mAreaPath.lineTo(mPoint.x, mPoint.y);
                }
            }
            mAreaPath.lineTo(mViewArea.right, mViewArea.bottom);
            mAreaPath.lineTo(mViewArea.left, mViewArea.bottom);
            mAreaPath.close();

            float gap = mViewArea.height() / (MAX_Y_TICKS_LABELS * 1f);
            for (int i = 0; i < MAX_Y_TICKS_LABELS; i++) {
                float v = max - ((max - min) * (gap * (i + 1))) / mViewArea.height();
                mYTicksLabels[i] = formatter.format(v);
            }

            mDataAvailable = true;
        } else {
            float y = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 2f, getResources().getDisplayMetrics());
            mLinePath.moveTo(mViewArea.left, mViewArea.bottom - y);
            mLinePath.lineTo(mViewArea.right, mViewArea.bottom - y);
            mAreaPath.addRect(mViewArea.left, mViewArea.bottom - y,
                    mViewArea.right, mViewArea.bottom, Path.Direction.CCW);

            mDataAvailable = false;
        }
    }

    private void fillPointFromValue(int pos, float value, float min, float max) {
        mPoint.x = mViewArea.left + ((mViewArea.width() / (mData.length - 1)) * pos);
        mPoint.y = mViewArea.bottom - (((value - min) * mViewArea.height()) / (max - min));
        mPoint.y = Math.max(mViewArea.bottom - (mViewArea.height() * mAnimationDelta), mPoint.y);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw the grid lines
        float gap = mViewArea.height() / (MAX_Y_TICKS_LABELS * 1f);
        float y = mViewArea.top + gap;
        for (int i = 0; i < MAX_Y_TICKS_LABELS; i++) {
            canvas.drawLine(mViewArea.left, y, mViewArea.right, y, mGridLinesPaint);
            y += gap;
        }

        // Draw the points data
        canvas.drawPath(mAreaPath, mAreaPaint);
        canvas.drawPath(mLinePath, mLinePaint);

        if (mDataAvailable) {
            // Draw ticks labels
            float margin = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 2f, getResources().getDisplayMetrics());
            y = mViewArea.top + gap;
            for (int i = 0; i < MAX_Y_TICKS_LABELS; i++) {
                canvas.drawText(mYTicksLabels[i], mViewArea.right - margin, y - margin, mTicksPaint);
                y += gap;
            }
        }
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

    private void animateChart() {
        // Animate the chart
        if (mAnimator != null && mAnimator.isRunning()) {
            mAnimator.cancel();
        }
        mAnimationDelta = 0f;
        mAnimator = ValueAnimator.ofFloat(0f, 1f);
        mAnimator.setInterpolator(new AccelerateInterpolator());
        mAnimator.setDuration(350L);
        mAnimator.addUpdateListener(animation -> {
            mAnimationDelta = animation.getAnimatedFraction();
            computeDrawObjects();
            ViewCompat.postInvalidateOnAnimation(this);
        });
        mAnimator.start();
    }
}
