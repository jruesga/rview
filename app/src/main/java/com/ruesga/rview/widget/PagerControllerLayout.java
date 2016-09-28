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
import android.database.DataSetObserver;
import android.databinding.DataBindingUtil;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import com.ruesga.rview.R;
import com.ruesga.rview.annotations.ProguardIgnored;
import com.ruesga.rview.databinding.PagerControllerLayoutBinding;

public class PagerControllerLayout extends FrameLayout {

    @ProguardIgnored
    public static class Model {
        public CharSequence prev;
        public CharSequence next;
    }

    @ProguardIgnored
    @SuppressWarnings("UnusedParameters")
    public static class EventHandler {
        private PagerControllerLayout mView;

        EventHandler(PagerControllerLayout view) {
            mView =  view;
        }

        public void onPrevPressed(View v) {
            mView.performMovePrev();
        }

        public void onNextPressed(View v) {
            mView.performMoveNext();
        }
    }

    private ViewPager.OnPageChangeListener mPageListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            mBinding.getRoot().setAlpha(1 - positionOffset);
        }

        @Override
        public void onPageSelected(int position) {
            pageSelected(position);
        }

        @Override
        public void onPageScrollStateChanged(int state) {
        }
    };

    private DataSetObserver mDataObserver = new DataSetObserver() {
        @Override
        public void onChanged() {
            pageSelected(0);
        }

        @Override
        public void onInvalidated() {
            pageSelected(0);
        }
    };

    private PagerControllerLayoutBinding mBinding;
    private boolean mRegistered;
    private ViewPager mViewPager;

    private final Model mModel = new Model();

    public PagerControllerLayout(Context context) {
        this(context, null);
    }

    public PagerControllerLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PagerControllerLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mBinding = DataBindingUtil.inflate(
                LayoutInflater.from(context), R.layout.pager_controller_layout, this, false);
        mBinding.setHandlers(new EventHandler(this));
        addView(mBinding.getRoot());
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mRegistered) {
            mViewPager.removeOnPageChangeListener(mPageListener);
            mViewPager.getAdapter().unregisterDataSetObserver(mDataObserver);
        }
    }

    public void setupWithViewPager(ViewPager viewPager) {
        if (mRegistered) {
            viewPager.removeOnPageChangeListener(mPageListener);
            viewPager.getAdapter().unregisterDataSetObserver(mDataObserver);
        }
        if(viewPager != null) {
            viewPager.addOnPageChangeListener(mPageListener);
            viewPager.getAdapter().registerDataSetObserver(mDataObserver);
            mRegistered = true;
        }
        mViewPager = viewPager;
    }

    private void pageSelected(int position) {
        String title;
        if (mViewPager.getAdapter().getCount() > 0) {
            mModel.prev = position == 0 ? null : mViewPager.getAdapter().getPageTitle(position - 1);
            title = mViewPager.getAdapter().getPageTitle(position).toString();
            mModel.next = position >= mViewPager.getAdapter().getCount() - 1 ? null
                    : mViewPager.getAdapter().getPageTitle(position + 1);
            mBinding.setModel(mModel);
            mViewPager.setCurrentItem(position);
        } else {
            mModel.prev = null;
            title = null;
            mModel.next = null;
            mBinding.setModel(mModel);
        }

        ActionBar actionBar = ((AppCompatActivity) getContext()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setSubtitle(title);
        }
    }

    private void performMovePrev() {
        mViewPager.setCurrentItem(mViewPager.getCurrentItem() - 1, true);
    }

    private void performMoveNext() {
        mViewPager.setCurrentItem(mViewPager.getCurrentItem() + 1, true);
    }
}
