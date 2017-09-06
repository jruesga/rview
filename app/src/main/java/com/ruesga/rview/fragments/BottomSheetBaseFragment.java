/*
 * Copyright (C) 2017 Jorge Ruesga
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

import android.app.Dialog;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.os.Handler;
import android.support.annotation.DrawableRes;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.content.ContextCompat;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.ruesga.rview.R;
import com.ruesga.rview.databinding.BottomSheetBaseDialogBinding;
import com.ruesga.rview.misc.ArgbEvaluator;
import com.ruesga.rview.misc.ViewHelper;

import net.yslibrary.android.keyboardvisibilityevent.KeyboardVisibilityEvent;
import net.yslibrary.android.keyboardvisibilityevent.Unregistrar;

public abstract class BottomSheetBaseFragment extends BottomSheetDialogFragment {

    public static final String TAG = "BottomSheetBaseFragment";

    private static final int REQUEST_PERMISSIONS = 101;

    @Keep
    @SuppressWarnings("unused")
    public static class EventHandlers {
        private final BottomSheetBaseFragment mFragment;

        public EventHandlers(BottomSheetBaseFragment fragment) {
            mFragment = fragment;
        }

        public void onDonePressed(View v) {
            mFragment.onDonePressed();
            mFragment.mHandler.post(mFragment::dismiss);
        }

        public void onActionPressed(View v) {
            mFragment.onActionPressed();
        }
    }

    private BottomSheetBaseDialogBinding mBinding;
    private final Handler mHandler;
    private Unregistrar mKeyboardEvent;
    private int mMaxWidth;

    public BottomSheetBaseFragment() {
        mHandler = new Handler();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (mKeyboardEvent != null) {
            mKeyboardEvent.unregister();
        }
        if (mBinding != null) {
            mBinding.unbind();
        }

        if (getActivity().getResources().getBoolean(R.bool.config_is_table)) {
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }
    }

    @Override
    public void setupDialog(Dialog dialog, int style) {
        super.setupDialog(dialog, style);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }

        // Don't allow rotation on tablets while on BottomSheet (because dual panel)
        if (getActivity().getResources().getBoolean(R.bool.config_is_table)) {
            int currentOrientation = getResources().getConfiguration().orientation;
            if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                getActivity().setRequestedOrientation(
                        ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            } else {
                getActivity().setRequestedOrientation(
                        ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
            }
        }

        final LayoutInflater li = LayoutInflater.from(getContext());
        mBinding = DataBindingUtil.inflate(li, R.layout.bottom_sheet_base_dialog, null, false);
        mBinding.setTitle(getString(getTitle()));
        mBinding.setBackgroundColor(Color.TRANSPARENT);
        mBinding.setForegroundColor(obtainColorStyle(android.R.attr.textColorPrimary));
        mBinding.setActionDrawable(getActionResId());
        mBinding.setHandlers(new EventHandlers(this));

        View root = mBinding.getRoot();
        dialog.setContentView(root);

        mKeyboardEvent = KeyboardVisibilityEvent.registerEventListener(
                getActivity(), isOpen -> adjustContentHeight());

        CoordinatorLayout.LayoutParams params =
                (CoordinatorLayout.LayoutParams) ((View) root.getParent()).getLayoutParams();
        CoordinatorLayout.Behavior behavior = params.getBehavior();
        if (behavior != null && behavior instanceof BottomSheetBehavior) {
            int peekHeight = getResources().getDimensionPixelSize(
                    R.dimen.bottom_sheet_peek_height);
            final BottomSheetBehavior bottomSheetBehavior = ((BottomSheetBehavior) behavior);
            bottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
                final ArgbEvaluator INTERPOLATOR = ArgbEvaluator.newInstance();
                final int BG_START = Color.TRANSPARENT;
                final int BG_END = ContextCompat.getColor(getContext(), R.color.primaryDark);
                final int FG_START = obtainColorStyle(android.R.attr.textColorPrimary);
                final int FG_END = obtainColorStyle(android.R.attr.textColorPrimaryInverse);

                @Override
                public void onStateChanged(@NonNull View bottomSheet, int newState) {
                    if (!allowDraggingState() && newState != BottomSheetBehavior.STATE_DRAGGING) {
                        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                    } else if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                        if (getDialog().isShowing()) {
                            dismiss();
                        }
                    }
                }

                @Override
                public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                    int bgColor = ((Integer) INTERPOLATOR.evaluate(slideOffset, BG_START, BG_END));
                    int fgColor = ((Integer) INTERPOLATOR.evaluate(slideOffset, FG_START, FG_END));
                    mBinding.setBackgroundColor(bgColor);
                    mBinding.setForegroundColor(fgColor);
                    mBinding.executePendingBindings();
                }
            });
            bottomSheetBehavior.setPeekHeight(peekHeight);
            bottomSheetBehavior.setHideable(false);

            root.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    root.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    CoordinatorLayout decorator = ViewHelper.findFirstParentViewOfType(root,
                            CoordinatorLayout.class);
                    if (decorator != null) {
                        setFixedContentWidth(decorator);
                        mBinding.content.setMinimumHeight(
                                (allowExpandedState() ? decorator.getHeight() : peekHeight)
                                        - mBinding.toolbar.getHeight());
                        inflateContent(mBinding.content);
                        onContentLayoutChanged(mBinding.content);
                        requestPermissionsIfNeeded(REQUEST_PERMISSIONS);
                    } else {
                        dismiss();
                    }
                }
            });
        } else {
            dismiss();
        }
    }

    private int obtainColorStyle(int attribute) {
        TypedValue typedValue = new TypedValue();
        TypedArray a = getContext().obtainStyledAttributes(
                typedValue.data, new int[] { attribute });
        int color = a.getColor(0, 0);
        a.recycle();
        return color;
    }

    private void requestPermissionsIfNeeded(int requestCode) {
        String[] permissions = requiredPermissions();
        if (permissions != null && permissions.length > 0) {
            for (String permission : permissions) {
                if (ContextCompat.checkSelfPermission(getActivity(), permission)
                        != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(permissions, requestCode);
                    break;
                }
            }
        }

        // Permissions granted
        onContentReady();
    }

    @Override
    public final void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSIONS) {
            for (int permissionGrant : grantResults) {
                if (permissionGrant != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
            }

            // Permissions granted
            onContentReady();
        }
    }

    private void adjustContentHeight() {
        int peekHeight = getResources().getDimensionPixelSize(
                R.dimen.bottom_sheet_peek_height);
        CoordinatorLayout decorator = ViewHelper.findFirstParentViewOfType(mBinding.getRoot(),
                CoordinatorLayout.class);
        if (decorator != null) {
            int height = (allowExpandedState() ? decorator.getHeight() : peekHeight)
                    - mBinding.toolbar.getHeight();
            mBinding.content.setMinimumHeight(height);
            onContentLayoutChanged(mBinding.content);
        }
    }

    public ViewGroup getContentView() {
        return mBinding.content;
    }

    public final int getMaxWidth() {
        return mMaxWidth;
    }

    public abstract @StringRes int getTitle();

    public abstract void inflateContent(ViewGroup parent);

    public @DrawableRes int getActionResId() {
        return 0;
    }

    public boolean allowDraggingState() {
        return true;
    }

    public boolean allowExpandedState() {
        return true;
    }

    public String[] requiredPermissions() {
        return null;
    }

    public void onContentLayoutChanged(ViewGroup parent) {
    }

    public void onContentReady() {
    }

    public void onActionPressed() {
    }

    public void onDonePressed() {
    }

    private void setFixedContentWidth(CoordinatorLayout decorator) {
        int contentWidth = getResources().getDimensionPixelSize(
                R.dimen.bottom_sheet_peek_max_width);
        FrameLayout.LayoutParams layoutParams =
                (FrameLayout.LayoutParams) decorator.getLayoutParams();
        layoutParams.gravity = Gravity.CENTER_HORIZONTAL;
        mMaxWidth = layoutParams.width = Math.min(contentWidth, decorator.getMeasuredWidth());
    }
}
