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
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.os.ParcelableCompat;
import android.support.v4.os.ParcelableCompatCreatorCallbacks;
import android.support.v4.view.AbsSavedState;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import com.ruesga.rview.R;

public class ExpandableViewLayout extends FrameLayout {

    private int mMaxHeight;
    private boolean mExpanded;
    private View mExpandableControl;
    private int mChildViewHeight;

    public ExpandableViewLayout(Context context) {
        this(context, null);
    }

    public ExpandableViewLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ExpandableViewLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        Resources.Theme theme = context.getTheme();
        TypedArray a = theme.obtainStyledAttributes(
                attrs, R.styleable.ExpandableViewLayout, defStyleAttr, 0);
        int n = a.getIndexCount();
        for (int i = 0; i < n; i++) {
            int attr = a.getIndex(i);
            switch (attr) {
                case R.styleable.ExpandableViewLayout_maxHeight:
                    mMaxHeight = a.getDimensionPixelSize(attr, mMaxHeight);
                    break;

                case R.styleable.ExpandableViewLayout_expanded:
                    mExpanded = a.getBoolean(attr, false);
                    break;
            }
        }
        a.recycle();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        if (getChildCount() != 2) {
            throw new IllegalStateException(
                    "ExpandableViewLayout needs 2 views (main view and expandable control view)");
        }

        // Obtain the expandable control
        mExpandableControl = getChildAt(1);
        mExpandableControl.setOnClickListener(view -> {
            mExpanded = !mExpanded;
            setExpandableControlVisibility();
            requestLayout();
        });
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Obtain the main view height
        View view = getChildAt(0);
        view.measure(MeasureSpec.makeMeasureSpec(widthMeasureSpec, MeasureSpec.AT_MOST),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        mChildViewHeight = view.getMeasuredHeight();
        setExpandableControlVisibility();

        // Measure this view according
        if (!mExpanded && mMaxHeight > 0) {
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(mMaxHeight, MeasureSpec.AT_MOST);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    private void setExpandableControlVisibility() {
        final boolean expanded = mExpanded || mMaxHeight == 0 || mMaxHeight > mChildViewHeight;
        mExpandableControl.setVisibility(expanded ? View.GONE : View.VISIBLE);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        SavedState savedState = new SavedState(super.onSaveInstanceState());
        savedState.mExpanded = mExpanded;
        return savedState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        //begin boilerplate code so parent classes can restore state
        if(!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState savedState = (SavedState) state;
        super.onRestoreInstanceState(savedState.getSuperState());

        mExpanded = savedState.mExpanded;
        setExpandableControlVisibility();
        requestLayout();
    }

    @SuppressWarnings("WeakerAccess")
    public static class SavedState extends AbsSavedState {
        public boolean mExpanded;

        public SavedState(Parcel in, ClassLoader loader) {
            super(in, loader);
            mExpanded = in.readInt() == 1;
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(mExpanded ? 1 : 0);
        }

        public static final Parcelable.Creator<SavedState> CREATOR
                = ParcelableCompat.newCreator(new ParcelableCompatCreatorCallbacks<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel parcel, ClassLoader loader) {
                return new SavedState(parcel, loader);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        });
    }
}
