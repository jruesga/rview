// http://stackoverflow.com/questions/9650265/how-do-disable-paging-by-swiping-with-finger-in-viewpager-but-still-be-able-to-s
package com.ruesga.rview.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;

public class SwipeableViewPager extends ViewPager {

    private boolean mIsSwipeable;

    public SwipeableViewPager(Context context) {
        this(context, null);
    }

    public SwipeableViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        mIsSwipeable = true;
    }

    public void setSwipeable(boolean swipeable) {
        mIsSwipeable = swipeable;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return mIsSwipeable && super.onInterceptTouchEvent(event);
    }

    @Override
    @SuppressLint("ClickableViewAccessibility")
    public boolean onTouchEvent(MotionEvent event) {
        return mIsSwipeable && super.onTouchEvent(event);
    }

    @Override
    public boolean executeKeyEvent(@NonNull KeyEvent event) {
        return false;
    }
}
