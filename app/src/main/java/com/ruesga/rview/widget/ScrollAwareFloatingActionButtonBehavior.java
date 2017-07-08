package com.ruesga.rview.widget;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.FloatingActionButton.OnVisibilityChangedListener;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

@Keep
public class ScrollAwareFloatingActionButtonBehavior extends FloatingActionButton.Behavior {

    private OnVisibilityChangedListener mFabVisibilityListener = new OnVisibilityChangedListener() {
        @Override
        public void onShown(FloatingActionButton fab) {
            super.onShown(fab);
        }

        @Override
        public void onHidden(FloatingActionButton fab) {
            super.onHidden(fab);
            fab.setVisibility(View.INVISIBLE);
        }
    };

    private final float mHideMinScrollSlop;
    private final float mShowMinScrollSlop;

    public ScrollAwareFloatingActionButtonBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);

        final Resources r = context.getResources();
        mHideMinScrollSlop = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 5, r.getDisplayMetrics());
        mShowMinScrollSlop = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 5, r.getDisplayMetrics()) * -1f;
    }

    @Override
    public boolean layoutDependsOn(
            CoordinatorLayout parent, FloatingActionButton child, View dependency) {
        return dependency instanceof RecyclerView;
    }

    @Override
    public boolean onStartNestedScroll(@NonNull CoordinatorLayout coordinatorLayout,
            @NonNull FloatingActionButton child, @NonNull View directTargetChild,
            @NonNull View target, int axes, int type) {
        return axes == ViewCompat.SCROLL_AXIS_VERTICAL ||
                super.onStartNestedScroll(
                        coordinatorLayout, child, directTargetChild, target, axes, type);
    }

    @Override
    public void onNestedScroll(@NonNull CoordinatorLayout coordinatorLayout,
            @NonNull FloatingActionButton child, @NonNull View target,
            int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int type) {

        super.onNestedScroll(coordinatorLayout, child, target, dxConsumed, dyConsumed,
                dxUnconsumed, dyUnconsumed, type);

        if (dyConsumed > mHideMinScrollSlop && child.getVisibility() == View.VISIBLE) {
            child.hide(mFabVisibilityListener);
        } else if (dyConsumed < mShowMinScrollSlop && child.getVisibility() != View.VISIBLE) {
            child.show(mFabVisibilityListener);
        }
    }
}
