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

    public UnwrappedLinearLayoutManager(Context context) {
        super(context);
    }

    @Override
    public boolean canScrollHorizontally() {
        return true;
    }

    @Override
    public boolean canScrollVertically() {
        return true;
    }

    @Override
    public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler,
            RecyclerView.State state) {
        if (getOrientation() == LinearLayoutManager.HORIZONTAL) {
            return  super.scrollHorizontallyBy(dx, recycler, state);
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

        int start = findFirstVisibleItemPosition();
        int end = findLastVisibleItemPosition();
        for (int i = start; i <= end; i++) {
            View v = findViewByPosition(i);
            v.setLeft(mOffset);
        }

        if (Math.abs(mOffset) >= horizontalSpace) {
            mOffset = -horizontalSpace;
            return 0;
        }
        return -delta;
    }

    @Override
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler,
            RecyclerView.State state) {
        if (getOrientation() == LinearLayoutManager.VERTICAL) {
            return  super.scrollVerticallyBy(dy, recycler, state);
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

        int start = findFirstVisibleItemPosition();
        int end = findLastVisibleItemPosition();
        for (int i = start; i <= end; i++) {
            View v = findViewByPosition(i);
            v.setTop(mOffset);
        }

        if (Math.abs(mOffset) >= verticalSpace) {
            mOffset = -verticalSpace;
            return 0;
        }
        return -delta;
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        super.onLayoutChildren(recycler, state);

        int start = findFirstVisibleItemPosition();
        int end = findLastVisibleItemPosition();
        for (int i = start; i <= end; i++) {
            View v = findViewByPosition(i);
            if (getOrientation() == LinearLayoutManager.VERTICAL) {
                v.setLeft(mOffset);
            } else {
                v.setTop(mOffset);
            }
        }
    }

    private int getHorizontalSpace() {
        return getWidth() - getPaddingRight() - getPaddingLeft();
    }

    private int getVerticalSpace() {
        return getHeight() - getPaddingBottom() - getPaddingTop();
    }

    private int getMeasuredChildWidth(ViewGroup v) {
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
}
