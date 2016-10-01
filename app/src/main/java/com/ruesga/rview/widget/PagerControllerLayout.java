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
import android.support.annotation.IdRes;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import com.ruesga.rview.R;
import com.ruesga.rview.annotations.ProguardIgnored;
import com.ruesga.rview.databinding.PagerControllerLayoutBinding;

public class PagerControllerLayout extends FrameLayout {

    private static final String FRAGMENT_TAG = "pager_fragment";

    public static final int INVALID_PAGE = -1;

    public static abstract class PagerControllerAdapter<T> {
        private DataSetObserver mObserver;

        public abstract FragmentManager getFragmentManager();

        public abstract CharSequence getPageTitle(int position);

        public abstract T getItem(int position);

        public abstract int getCount();

        public abstract Fragment getFragment(int position);

        public abstract @IdRes int getTarget();

        public void notifyDataSetChanged() {
            mObserver.onChanged();
        }

        private void registerObserver(DataSetObserver observer) {
            mObserver = observer;
        }
    }

    public interface OnPageSelectionListener {
        void onPageSelected(int position);
    }

    @ProguardIgnored
    public static class Model {
        public CharSequence prev;
        public CharSequence next;
    }

    @ProguardIgnored
    @SuppressWarnings("UnusedParameters")
    public static class EventHandlers {
        private PagerControllerLayout mView;

        EventHandlers(PagerControllerLayout view) {
            mView =  view;
        }

        public void onPrevPressed(View v) {
            mView.performMovePrev();
        }

        public void onNextPressed(View v) {
            mView.performMoveNext();
        }
    }


    private DataSetObserver mObserver = new DataSetObserver() {
        @Override
        public void onChanged() {
            pageSelected(mCurrentItem);
        }
    };

    private PagerControllerLayoutBinding mBinding;
    private OnPageSelectionListener mOnPageSelectionListener;
    private PagerControllerAdapter mAdapter;
    private int mCurrentItem = INVALID_PAGE;

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
        mBinding.setHandlers(new EventHandlers(this));
        addView(mBinding.getRoot());
    }

    public PagerControllerLayout listenOn(OnPageSelectionListener cb) {
        mOnPageSelectionListener = cb;
        return this;
    }

    public PagerControllerLayout with(PagerControllerAdapter adapter) {
        mAdapter = adapter;
        if (mAdapter == null) {
            currentPage(INVALID_PAGE);
        } else {
            mAdapter.registerObserver(mObserver);
        }
        return this;
    }

    public void currentPage(int position) {
        if (mCurrentItem == position) {
            return;
        }

        if (mAdapter != null && mAdapter.getCount() > 0) {
            mModel.prev = position == 0 ? null : mAdapter.getPageTitle(position - 1);
            mModel.next = position >= mAdapter.getCount() - 1 ? null
                    : mAdapter.getPageTitle(position + 1);
            mBinding.setModel(mModel);
            pageSelected(position);
        } else {
            mModel.prev = null;
            mModel.next = null;
            mBinding.setModel(mModel);
        }
    }

    private void pageSelected(int position) {
        if (mOnPageSelectionListener != null) {
            mOnPageSelectionListener.onPageSelected(position);
        }

        FragmentTransaction tx = mAdapter.getFragmentManager().beginTransaction();
        Fragment oldFragment = mAdapter.getFragmentManager().findFragmentByTag(FRAGMENT_TAG);
        if (oldFragment != null) {
            tx.remove(oldFragment);
        }
        if (position != INVALID_PAGE) {
            tx.replace(mAdapter.getTarget(), mAdapter.getFragment(position), FRAGMENT_TAG);
        }
        tx.commit();
        mCurrentItem = position;
    }

    private void performMovePrev() {
        currentPage(mCurrentItem - 1);
    }

    private void performMoveNext() {
        currentPage(mCurrentItem + 1);
    }
}
