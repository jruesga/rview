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
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

public class UnwrappedLinearLayoutManager extends LinearLayoutManager {

    private int mOffset;

    private boolean mCanScrollHorizontally;
    private boolean mCanScrollVertically;

    private int mPrefetchedMeasuredWidth;
    private int mPrefetchedMeasuredHeight;

    private int mScrollState;

    public UnwrappedLinearLayoutManager(Context context) {
        this(context, -1, -1);
    }

    public UnwrappedLinearLayoutManager(Context context,
            int prefetchedMeasuredChildWidth, int prefetchedMeasuredChildHeight) {
        super(context);
        mPrefetchedMeasuredWidth = prefetchedMeasuredChildWidth;
        mPrefetchedMeasuredHeight = prefetchedMeasuredChildHeight;
    }

    public void setPrefetchedMeasuredWidth(int prefetchedMeasuredWidth) {
        mPrefetchedMeasuredWidth = prefetchedMeasuredWidth;
    }

    public void setPrefetchedMeasuredHeight(int prefetchedMeasuredHeight) {
        mPrefetchedMeasuredHeight = prefetchedMeasuredHeight;
    }

    public void requestBindViews() {
        if (mScrollState != RecyclerView.SCROLL_STATE_DRAGGING) {
            bindViews();
        }
    }

    @Override
    public void onScrollStateChanged(int state) {
        mScrollState = state;
    }

    @Override
    public boolean canScrollHorizontally() {
        if (!mCanScrollHorizontally) {
            if (getOrientation() == LinearLayoutManager.HORIZONTAL) {
                return super.canScrollHorizontally();
            }

            if (getChildCount() == 0) {
                return false;
            }

            final View view = getChildAt(0);
            int decoratedRight = getDecoratedRight(view);
            int decoratedLeft = getDecoratedLeft(view);
            int decoratedWidth = decoratedRight - decoratedLeft;
            if (view instanceof ViewGroup) {
                decoratedWidth = getMeasuredChildWidth((ViewGroup) view);
            }
            mCanScrollHorizontally = decoratedWidth > getHorizontalSpace();
        }
        return mCanScrollHorizontally;
    }

    @Override
    public boolean canScrollVertically() {
        if (!mCanScrollVertically) {
            if (getOrientation() == LinearLayoutManager.VERTICAL) {
                return super.canScrollVertically();
            }

            if (getChildCount() == 0) {
                return false;
            }

            final View view = getChildAt(0);
            int decoratedBottom = getDecoratedBottom(view);
            int decoratedTop = getDecoratedTop(view);
            int decoratedHeight = decoratedBottom - decoratedTop;
            if (view instanceof ViewGroup) {
                decoratedHeight = getMeasuredChildWidth((ViewGroup) view);
            }
            mCanScrollVertically = decoratedHeight > getVerticalSpace();
        }
        return mCanScrollVertically;
    }

    @Override
    public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler,
            RecyclerView.State state) {
        if (getOrientation() == LinearLayoutManager.HORIZONTAL) {
            return super.scrollHorizontallyBy(dx, recycler, state);
        }

        if (getChildCount() == 0) {
            return 0;
        }

        final View view = getChildAt(0);
        int decoratedRight = getDecoratedRight(view);
        int decoratedLeft = getDecoratedLeft(view);
        int decoratedWidth = decoratedRight - decoratedLeft;
        if (view instanceof ViewGroup) {
            decoratedWidth = getMeasuredChildWidth((ViewGroup) view);
            decoratedRight = decoratedLeft + decoratedWidth;
        }
        int horizontalSpace = getHorizontalSpace();

        if (decoratedWidth < horizontalSpace) {
            return 0;
        }

        int delta;
        if (dx > 0) {
            int rightOffset = horizontalSpace - decoratedRight + getPaddingRight();
            delta = Math.max(-dx, rightOffset);
        } else {
            int leftOffset = -decoratedLeft + getPaddingLeft();
            delta = Math.min(-dx, leftOffset);
        }
        mOffset += delta;

        bindViews();

        if (Math.abs(mOffset) >= decoratedWidth) {
            mOffset = -decoratedWidth;
            return 0;
        }
        return -delta;
    }

    @Override
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler,
            RecyclerView.State state) {
        if (getOrientation() == LinearLayoutManager.VERTICAL) {
            return super.scrollVerticallyBy(dy, recycler, state);
        }

        if (getChildCount() == 0) {
            return 0;
        }

        final View view = getChildAt(0);
        int decoratedBottom = getDecoratedBottom(view);
        int decoratedTop = getDecoratedTop(view);
        int decoratedHeight = decoratedBottom - decoratedTop;
        if (view instanceof ViewGroup) {
            decoratedHeight = getMeasuredChildHeight((ViewGroup) view);
            decoratedBottom = decoratedTop + decoratedHeight;
        }
        int verticalSpace = getVerticalSpace();

        if (decoratedHeight < verticalSpace) {
            return 0;
        }

        int delta;
        if (dy > 0) {
            int bottomOffset = verticalSpace - decoratedBottom + getPaddingBottom();
            delta = Math.max(-dy, bottomOffset);
        } else {
            int topOffset = -decoratedTop + getPaddingTop();
            delta = Math.min(-dy, topOffset);
        }
        mOffset += delta;

        bindViews();

        if (Math.abs(mOffset) >= decoratedHeight) {
            mOffset = -decoratedHeight;
            return 0;
        }
        return -delta;
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        super.onLayoutChildren(recycler, state);
        bindViews();
    }

    private int getHorizontalSpace() {
        return getWidth() - getPaddingRight() - getPaddingLeft();
    }

    private int getVerticalSpace() {
        return getHeight() - getPaddingBottom() - getPaddingTop();
    }

    private int getMeasuredChildWidth(ViewGroup v) {
        if (mPrefetchedMeasuredWidth != -1) {
            return mPrefetchedMeasuredWidth;
        }

        int count = v.getChildCount();
        int width = 0;
        for (int i = 0; i < count; i++) {
            View child = v.getChildAt(i);
            if (child.getVisibility() != View.GONE) {
                width += child.getMeasuredWidth();
            }
        }
        return width;
    }

    private int getMeasuredChildHeight(ViewGroup v) {
        if (mPrefetchedMeasuredHeight != -1) {
            return mPrefetchedMeasuredHeight;
        }

        int count = v.getChildCount();
        int height = 0;
        for (int i = 0; i < count; i++) {
            View child = v.getChildAt(i);
            if (child.getVisibility() != View.GONE) {
                height += child.getMeasuredHeight();
            }
        }
        return height;
    }

    private void bindViews() {
        int orientation = getOrientation();
        int start = findFirstVisibleItemPosition();
        int end = findLastVisibleItemPosition();
        for (int i = start; i <= end; i++) {
            View v = findViewByPosition(i);
            if (v != null) {
                if (orientation == LinearLayoutManager.VERTICAL) {
                    v.setLeft(mOffset);
                } else {
                    v.setTop(mOffset);
                }
            }
        }
    }
}
