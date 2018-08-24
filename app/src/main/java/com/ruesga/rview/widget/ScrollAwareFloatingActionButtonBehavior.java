package com.ruesga.rview.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton.OnVisibilityChangedListener;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

@Keep
public class ScrollAwareFloatingActionButtonBehavior extends FloatingActionButton.Behavior {

    private OnVisibilityChangedListener mFabVisibilityListener = new OnVisibilityChangedListener() {
        @Override
        @SuppressLint("RestrictedApi")
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
                TypedValue.COMPLEX_UNIT_DIP, 8, r.getDisplayMetrics());
        mShowMinScrollSlop = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 8, r.getDisplayMetrics()) * -1f;
    }

    @Override
    public boolean layoutDependsOn(@NonNull CoordinatorLayout parent,
            @NonNull FloatingActionButton child,@NonNull View dependency) {
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

        if (type != -1) {
            super.onNestedScroll(coordinatorLayout, child, target, dxConsumed, dyConsumed,
                    dxUnconsumed, dyUnconsumed, type);
        }

        if (dyConsumed > mHideMinScrollSlop && child.getVisibility() == View.VISIBLE) {
            child.hide(mFabVisibilityListener);
        } else if (dyConsumed < mShowMinScrollSlop && child.getVisibility() != View.VISIBLE) {
            child.show(mFabVisibilityListener);
        }
    }
}
