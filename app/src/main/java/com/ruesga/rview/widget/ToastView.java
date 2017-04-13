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

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.databinding.DataBindingUtil;
import android.support.annotation.Keep;
import android.support.annotation.StringRes;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.widget.FrameLayout;

import com.ruesga.rview.R;
import com.ruesga.rview.databinding.ToastBinding;

public class ToastView extends FrameLayout {

    private static final int STATE_IDLE = 0;
    private static final int STATE_SHOWING = 1;
    private static final int STATE_HIDING = 2;

    public interface OnToastPressedListener {
        void onToastPressed();
    }

    @Keep
    public static class EventHandlers {
        private ToastView mView;

        public EventHandlers(ToastView v) {
            mView = v;
        }

        public void onToastPressed(View v) {
            mView.onToastClicked();
        }
    }

    private ToastBinding mBinding;

    private AnimatorSet mAnimatorSet;
    private int mAnimateState = STATE_IDLE;

    private OnToastPressedListener mOnToastPressedListener;

    public ToastView(Context context) {
        this(context, null);
    }

    public ToastView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ToastView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setVisibility(View.GONE);

        LayoutInflater layoutInflater = LayoutInflater.from(context);
        EventHandlers handlers = new EventHandlers(this);
        mBinding = DataBindingUtil.inflate(layoutInflater, R.layout.toast, this, false);
        mBinding.setHandlers(handlers);
        addView(mBinding.getRoot());
    }

    public ToastView listenTo(OnToastPressedListener cb) {
        mOnToastPressedListener = cb;
        return this;
    }

    public void show(@StringRes int msg) {
        if (mAnimateState == STATE_SHOWING || getVisibility() == View.VISIBLE) {
            return;
        }
        mAnimateState = STATE_SHOWING;

        if (mAnimatorSet != null) {
            mAnimatorSet.cancel();
        }

        Animator alphaAnimator = ObjectAnimator.ofFloat(mBinding.toast, "alpha", getAlpha(), 1f);
        alphaAnimator.setDuration(100);

        final String message = getResources().getString(msg);
        final int startWidth = getResources().getDimensionPixelSize(R.dimen.toast_min_width);
        final int endWidth = (int) mBinding.toast.getPaint().measureText(message);
        ValueAnimator widthAnimator = ValueAnimator.ofInt(startWidth, endWidth);
        widthAnimator.setStartDelay(25);
        widthAnimator.setDuration(250);
        widthAnimator.addUpdateListener(animation -> {
            mBinding.toast.getLayoutParams().width = (Integer) animation.getAnimatedValue();
            mBinding.toast.requestLayout();
        });

        mAnimatorSet = new AnimatorSet();
        mAnimatorSet.setInterpolator(new AccelerateInterpolator());
        mAnimatorSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                mBinding.setText(null);
                mBinding.executePendingBindings();
                mBinding.toast.getLayoutParams().width = startWidth;
                setVisibility(VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mBinding.setText(message);
                mBinding.executePendingBindings();
                mBinding.toast.getLayoutParams().width = ViewGroup.LayoutParams.WRAP_CONTENT;
                mBinding.toast.requestLayout();
                mAnimateState = STATE_IDLE;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });
        mAnimatorSet.playSequentially(alphaAnimator, widthAnimator);
        mAnimatorSet.start();
    }

    public void hide() {
        if (mAnimateState == STATE_HIDING || getVisibility() == GONE) {
            return;
        }
        mAnimateState = STATE_HIDING;

        if (mAnimatorSet != null) {
            mAnimatorSet.cancel();
        }

        Animator alphaAnimator = ObjectAnimator.ofFloat(mBinding.toast, "alpha", getAlpha(), 0f);
        alphaAnimator.setDuration(50);

        final int startWidth = mBinding.toast.getWidth();
        final int endWidth = getResources().getDimensionPixelSize(R.dimen.toast_min_width);
        ValueAnimator widthAnimator = ValueAnimator.ofInt(startWidth, endWidth);
        widthAnimator.setDuration(250);
        widthAnimator.addUpdateListener(animation -> {
            mBinding.toast.getLayoutParams().width = (Integer) animation.getAnimatedValue();
            mBinding.toast.requestLayout();
        });

        mAnimatorSet = new AnimatorSet();
        mAnimatorSet.setInterpolator(new AccelerateInterpolator());
        mAnimatorSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                mBinding.setText(null);
                mBinding.executePendingBindings();
                mBinding.toast.getLayoutParams().width = startWidth;
                mBinding.toast.requestLayout();
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                setVisibility(GONE);
                mAnimateState = STATE_IDLE;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });
        mAnimatorSet.playSequentially(widthAnimator, alphaAnimator);
        mAnimatorSet.start();
    }

    private void onToastClicked() {
        if (mAnimateState == STATE_IDLE && mOnToastPressedListener != null) {
            mOnToastPressedListener.onToastPressed();
        }
    }
}
