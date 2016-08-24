/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.ruesga.rview.drawer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.support.design.internal.ForegroundLinearLayout;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.AccessibilityDelegateCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.accessibility.AccessibilityEventCompat;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.support.v7.view.menu.MenuItemImpl;
import android.support.v7.view.menu.MenuView;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewStub;
import android.widget.CheckedTextView;
import android.widget.FrameLayout;
import android.widget.TextView;

@SuppressWarnings({"unused", "deprecation"})
@SuppressLint("PrivateResource")
public class NavigationMenuItemView extends ForegroundLinearLayout implements MenuView.ItemView {

    public static final String SEPARATOR = "|";
    public static final String SEPARATOR_REGEXP = "\\|";

    private static final int[] CHECKED_STATE_SET = {android.R.attr.state_checked};

    private final int mIconSize;

    private boolean mNeedsEmptyIcon;

    private boolean mCheckable;

    private final View mView;
    private final CheckedImageView mIcon;
    private final CheckedTextView mTextTitle;
    private final TextView mTextSummary;
    private final TextView mNotifications;

    private FrameLayout mActionArea;

    private MenuItemImpl mItemData;

    private ColorStateList mIconTintList;

    private boolean mHasIconTintList;

    private Drawable mEmptyDrawable;

    private final AccessibilityDelegateCompat mAccessibilityDelegate
            = new AccessibilityDelegateCompat() {

        @Override
        public void onInitializeAccessibilityNodeInfo(View host,
                                                      AccessibilityNodeInfoCompat info) {
            super.onInitializeAccessibilityNodeInfo(host, info);
            info.setCheckable(mCheckable);
        }

    };

    public NavigationMenuItemView(Context context) {
        this(context, null);
    }

    public NavigationMenuItemView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NavigationMenuItemView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOrientation(HORIZONTAL);
        LayoutInflater.from(context).inflate(com.ruesga.rview.drawer.R.layout.drawer_design_navigation_menu_item, this, true);
        mIconSize = context.getResources().getDimensionPixelSize(
                android.support.design.R.dimen.design_navigation_icon_size);
        mView = findViewById(com.ruesga.rview.drawer.R.id.design_menu_item_area);
        mIcon = (CheckedImageView) findViewById(com.ruesga.rview.drawer.R.id.design_menu_item_icon);
        mIcon.setDuplicateParentStateEnabled(true);
        mTextTitle = (CheckedTextView) findViewById(com.ruesga.rview.drawer.R.id.design_menu_item_text);
        mTextTitle.setDuplicateParentStateEnabled(true);
        ViewCompat.setAccessibilityDelegate(mTextTitle, mAccessibilityDelegate);
        mTextSummary = (TextView) findViewById(com.ruesga.rview.drawer.R.id.design_menu_item_subtext);
        mTextSummary.setDuplicateParentStateEnabled(true);
        ViewCompat.setAccessibilityDelegate(mTextSummary, mAccessibilityDelegate);
        mNotifications = (TextView) findViewById(com.ruesga.rview.drawer.R.id.design_menu_item_notifications);
        mNotifications.setDuplicateParentStateEnabled(true);
        ViewCompat.setAccessibilityDelegate(mNotifications, mAccessibilityDelegate);
    }

    @Override
    public void initialize(MenuItemImpl itemData, int menuType) {
        mItemData = itemData;

        setVisibility(itemData.isVisible() ? VISIBLE : GONE);

        if (getBackground() == null) {
            setBackground(createDefaultBackground());
        }

        setCheckable(itemData.isCheckable());
        setChecked(itemData.isChecked());
        setEnabled(itemData.isEnabled());
        setTitle(itemData.getTitle());
        setIcon(itemData.getIcon());
        setActionView(itemData.getActionView());
        adjustAppearance();
    }

    private boolean shouldExpandActionArea() {
        return mItemData.getTitle() == null &&
                mItemData.getIcon() == null &&
                mItemData.getActionView() != null;
    }

    private void adjustAppearance() {
        if (shouldExpandActionArea()) {
            // Expand the actionView area
            mView.setVisibility(View.GONE);
            if (mActionArea != null) {
                LayoutParams params = (LayoutParams) mActionArea.getLayoutParams();
                params.width = LayoutParams.MATCH_PARENT;
                mActionArea.setLayoutParams(params);
            }
        } else {
            mView.setVisibility(View.VISIBLE);
            if (mActionArea != null) {
                LayoutParams params = (LayoutParams) mActionArea.getLayoutParams();
                params.width = LayoutParams.WRAP_CONTENT;
                mActionArea.setLayoutParams(params);
            }
        }
    }

    public void recycle() {
        if (mActionArea != null) {
            mActionArea.removeAllViews();
        }
    }

    private void setActionView(View actionView) {
        if (actionView != null) {
            if (mActionArea == null) {
                mActionArea = (FrameLayout) ((ViewStub) findViewById(
                        android.support.design.R.id.design_menu_item_action_area_stub)).inflate();
            }
            mActionArea.removeAllViews();
            mActionArea.addView(actionView);
        }
    }

    private StateListDrawable createDefaultBackground() {
        TypedValue value = new TypedValue();
        if (getContext().getTheme().resolveAttribute(
                android.support.v7.appcompat.R.attr.colorControlHighlight, value, true)) {
            StateListDrawable drawable = new StateListDrawable();
            drawable.addState(CHECKED_STATE_SET, new ColorDrawable(value.data));
            drawable.addState(EMPTY_STATE_SET, new ColorDrawable(Color.TRANSPARENT));
            return drawable;
        }
        return null;
    }

    @Override
    public MenuItemImpl getItemData() {
        return mItemData;
    }

    @Override
    public void setTitle(CharSequence title) {
        // Since can't create a wrapper around MenuItemImpl without rewrite the entire
        // support implementation, just use the title to fill the other values of
        // interest (summary and number notifications). Just use a separator | from title string
        // to determine <title>|<summary>|<notification count>
        // Callers must guarantee the sanity of the title string. This is a bad hack  :(, but
        // allow us to display the information we want on screen.
        if (!TextUtils.isEmpty(title)) {
            String[] v = title.toString().split(SEPARATOR_REGEXP);

            // Title
            mTextTitle.setText(v[0]);

            // Summary
            if (v.length >= 2 && !TextUtils.isEmpty(v[1])) {
                mTextSummary.setText(v[1]);
                mTextSummary.setVisibility(View.VISIBLE);
            } else {
                mTextSummary.setVisibility(View.GONE);
            }

            // Notifications
            if (v.length >= 3 && !TextUtils.isEmpty(v[2])) {
                Integer notifications = null;
                try {
                    notifications = Integer.parseInt(v[2]);
                } catch (NumberFormatException ex) {
                    // ignore
                }
                if (notifications != null && notifications >= 0) {
                    mNotifications.setText(notifications > 99
                            ? "+99" : String.valueOf(notifications));
                    mNotifications.setVisibility(View.VISIBLE);
                } else {
                    mNotifications.setVisibility(View.GONE);
                }

            } else {
                mNotifications.setVisibility(View.GONE);
            }
            return;
        }
        mTextTitle.setText(null);
    }

    @Override
    public void setCheckable(boolean checkable) {
        refreshDrawableState();
        if (mCheckable != checkable) {
            mCheckable = checkable;
            mAccessibilityDelegate.sendAccessibilityEvent(mTextTitle,
                    AccessibilityEventCompat.TYPE_WINDOW_CONTENT_CHANGED);
        }
    }

    @Override
    public void setChecked(boolean checked) {
        refreshDrawableState();
        mTextTitle.setChecked(checked);
        mIcon.setChecked(checked);
    }

    @Override
    public void setShortcut(boolean showShortcut, char shortcutKey) {
    }

    @Override
    public void setIcon(Drawable icon) {
        if (icon != null) {
            if (mHasIconTintList) {
                Drawable.ConstantState state = icon.getConstantState();
                icon = DrawableCompat.wrap(state == null ? icon : state.newDrawable()).mutate();
                DrawableCompat.setTintList(icon, mIconTintList);
            }
            icon.setBounds(0, 0, mIconSize, mIconSize);
        } else if (mNeedsEmptyIcon) {
            if (mEmptyDrawable == null) {
                mEmptyDrawable = ResourcesCompat.getDrawable(getResources(),
                        android.support.design.R.drawable.navigation_empty_icon, getContext().getTheme());
                if (mEmptyDrawable != null) {
                    mEmptyDrawable.setBounds(0, 0, mIconSize, mIconSize);
                }
            }
            icon = mEmptyDrawable;
        }
        mIcon.setImageDrawable(icon);
    }

    @Override
    public boolean prefersCondensedTitle() {
        return false;
    }

    @Override
    public boolean showsIcon() {
        return true;
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
        if (mItemData != null && mItemData.isCheckable() && mItemData.isChecked()) {
            mergeDrawableStates(drawableState, CHECKED_STATE_SET);
        }
        return drawableState;
    }

    void setIconTintList(ColorStateList tintList) {
        mIconTintList = tintList;
        mHasIconTintList = mIconTintList != null;
        if (mItemData != null) {
            // Update the icon so that the tint takes effect
            setIcon(mItemData.getIcon());
        }
    }

    public void setTextAppearance(Context context, int textAppearance) {
        mTextTitle.setTextAppearance(context, textAppearance);
        mTextSummary.setTextAppearance(context, textAppearance);
    }

    public void setTextColor(ColorStateList colors) {
        mTextTitle.setTextColor(colors);
        mTextSummary.setTextColor(colors);
    }

    public void setNeedsEmptyIcon(boolean needsEmptyIcon) {
        mNeedsEmptyIcon = needsEmptyIcon;
    }

}
