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
package com.ruesga.rview.fragments;

import android.animation.Animator;
import android.annotation.TargetApi;
import android.app.Dialog;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.animation.AccelerateInterpolator;

import com.ruesga.rview.misc.AndroidHelper;

// https://github.com/codepath/android_guides/wiki/Circular-Reveal-Animation
public abstract class RevealDialogFragment extends DialogFragment {

    public static final String EXTRA_ANCHOR = "anchor";

    private static final String EXTRA_DO_REVEAL = "do_reveal";

    private Rect mAnchorRect;

    private boolean mDoReveal;

    public RevealDialogFragment() {
    }

    public abstract void buildDialog(AlertDialog.Builder builder, Bundle savedInstanceState);

    public void onDialogReveled() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAnchorRect = getArguments().getParcelable(EXTRA_ANCHOR);

        mDoReveal = true;
        if (savedInstanceState != null) {
            mDoReveal = savedInstanceState.getBoolean(EXTRA_DO_REVEAL, true);
        }
    }

    @NonNull
    @Override
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public final Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        buildDialog(builder, savedInstanceState);
        Dialog dialog = builder.create();
        if (AndroidHelper.isLollipopOrGreater()) {
            dialog.setOnShowListener(dialogInterface -> performEnterRevealTransition());
        }
        return dialog;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // Dialog was revealed previously? Don't do it again
        outState.putBoolean(EXTRA_DO_REVEAL, mDoReveal);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void performEnterRevealTransition() {
        if (!mDoReveal || getDialog() == null || getDialog().getWindow() == null) {
            return;
        }

        final View v = getDialog().getWindow().getDecorView();
        v.setVisibility(View.VISIBLE);
        Rect dialogRect = computeViewOnScreen(v);

        int cx = v.getMeasuredWidth() / 2;
        int cy = v.getMeasuredHeight() / 2;
        if (mAnchorRect != null) {
            cx = Math.min(Math.max(mAnchorRect.centerX(), dialogRect.left), dialogRect.right) - dialogRect.left;
            cy = Math.min(Math.max(mAnchorRect.centerY(), dialogRect.top), dialogRect.bottom) - dialogRect.top;
        }

        int finalRadius = Math.max(v.getWidth(), v.getHeight()) / 2;

        Animator anim = ViewAnimationUtils.createCircularReveal(v, cx, cy, 0, finalRadius);
        anim.setDuration(350);
        anim.setInterpolator(new AccelerateInterpolator());
        anim.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                onDialogReveled();
                mDoReveal = false;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });
        anim.start();
    }

    public static Rect computeViewOnScreen(View v) {
        int[] pos = new int[2];
        v.getLocationOnScreen(pos);
        Rect rect = new Rect();
        rect.set(pos[0], pos[1], pos[0] + v.getWidth(), pos[1] + v.getHeight());
        return rect;
    }
}
