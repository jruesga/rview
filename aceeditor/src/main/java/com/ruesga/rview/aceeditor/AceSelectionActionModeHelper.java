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
package com.ruesga.rview.aceeditor;

import android.annotation.SuppressLint;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.util.Pair;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

class AceSelectionActionModeHelper {
    public interface OnSelectionItemPressed {
        boolean onSelectionItemPressed(int itemId, String label);
    }

    static final int OPTION_CUT = 0;
    static final int OPTION_COPY = 1;
    static final int OPTION_PASTE = 2;
    static final int OPTION_SELECT_ALL = 3;

    private final Context mContext;
    private final LayoutInflater mLayoutInflater;
    private final ClipboardManager mClipboard;
    private final DisplayMetrics mMetrics;
    private PopupWindow mPopup;

    private int mInitialX;
    private int mInitialY;
    private int mPrimaryWidth;
    private int mPrimaryHeight;
    private int mSecondaryWidth;
    private int mSecondaryHeight;

    private ViewGroup mPrimaryOptionsViewBlock;
    private ViewGroup mSecondaryOptionsViewUpperBlock;
    private ViewGroup mSecondaryOptionsViewLowerBlock;
    private ViewGroup mPrimaryOptionsView;
    private ViewGroup mSecondaryOptionsUpperView;
    private ViewGroup mSecondaryOptionsLowerView;

    private OnSelectionItemPressed mOnSelectionItemPressedListener;
    private final List<Pair<String, Intent>> mOptions = new ArrayList<>();

    private boolean mHasSelection;

    private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mOnSelectionItemPressedListener != null) {
                final int itemId = (int) v.getTag();
                if (mOnSelectionItemPressedListener.onSelectionItemPressed(
                        itemId, mOptions.get(itemId).first)) {
                    dismiss();
                }
            }
        }
    };

    AceSelectionActionModeHelper(@NonNull Context context) {
        mContext = context;
        mLayoutInflater = LayoutInflater.from(mContext);
        mClipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        mMetrics = context.getResources().getDisplayMetrics();

        // Add options
        String[] options = context.getResources().getStringArray(R.array.ace_selection);
        for (String option : options) {
            mOptions.add(new Pair<>(option, null));
        }
        // Resolve extra intents
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent i = new Intent()
                    .setAction(Intent.ACTION_PROCESS_TEXT)
                    .setType("text/plain");
            PackageManager pm = context.getPackageManager();
            for (ResolveInfo ri : pm.queryIntentActivities(i, 0)) {
                Intent intent = new Intent()
                        .setAction(Intent.ACTION_PROCESS_TEXT)
                        .setType("text/plain")
                        .putExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, true)
                        .setClassName(ri.activityInfo.packageName, ri.activityInfo.name);
                mOptions.add(new Pair<>(ri.loadLabel(pm).toString(), intent));
            }
        }
    }

    public void listenOn(OnSelectionItemPressed cb) {
        mOnSelectionItemPressedListener = cb;
    }

    void dismiss() {
        if (mPopup != null) {
            mPopup.dismiss();
            mPopup = null;
        }
    }

    boolean isShowing() {
        return mPopup != null && mPopup.isShowing();
    }

    int getDefaultHeight() {
        return (int) mMetrics.density * 64;
    }

    public void show(View anchor) {
        int[] xy = new int[2];
        anchor.getLocationOnScreen(xy);
        show(anchor, xy[0], xy[1] + anchor.getHeight());
    }

    public void show(View anchor, int x, int y) {
        if (mPopup != null && mPopup.isShowing()) {
            mPopup.dismiss();
        }

        mInitialX = x;
        mInitialY = y;
        createPopUp();
        mPopup.showAtLocation(anchor, Gravity.NO_GRAVITY, x, y);
    }

    void hasSelection(boolean hasSelection) {
        mHasSelection = hasSelection;
    }

    void processExternalAction(int itemId, String selection) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent i = mOptions.get(itemId).second;
            i.putExtra(Intent.EXTRA_PROCESS_TEXT, selection);
            mContext.startActivity(i);
        }
    }

    @SuppressLint("InflateParams")
    private void createPopUp() {
        ViewGroup contentView = (ViewGroup) mLayoutInflater.inflate(
                R.layout.ace_action_mode_layout, null, false);
        mPrimaryOptionsViewBlock =
                contentView.findViewById(R.id.ace_selection_primary_options_block);
        mSecondaryOptionsViewUpperBlock =
                contentView.findViewById(R.id.ace_selection_secondary_options_upper_block);
        mSecondaryOptionsViewLowerBlock =
                contentView.findViewById(R.id.ace_selection_secondary_options_lower_block);
        mPrimaryOptionsView = contentView.findViewById(R.id.ace_selection_primary_options);
        mSecondaryOptionsUpperView =
                contentView.findViewById(R.id.ace_selection_secondary_upper_options);
        mSecondaryOptionsLowerView =
                contentView.findViewById(R.id.ace_selection_secondary_lower_options);
        View overflow = contentView.findViewById(R.id.ace_selection_overflow);
        overflow.setOnClickListener(v -> onShowMoreOptions());
        View backUpper = contentView.findViewById(R.id.ace_selection_back_upper);
        backUpper.setOnClickListener(v -> onHideMoreOptions());
        View backLower = contentView.findViewById(R.id.ace_selection_back_lower);
        backLower.setOnClickListener(v -> onHideMoreOptions());
        createOptions();
        mSecondaryOptionsViewUpperBlock.setVisibility(View.GONE);
        mSecondaryOptionsViewLowerBlock.setVisibility(View.GONE);

        mPopup = new PopupWindow(contentView, WRAP_CONTENT, WRAP_CONTENT, false);
        mPopup.setBackgroundDrawable(ContextCompat.getDrawable(
                mContext, R.drawable.ace_selection_bg));
        mPopup.setOutsideTouchable(true);
        mPopup.setTouchable(true);
        mPopup.setOnDismissListener(this::dismiss);
    }

    private void createOptions() {
        final int maxWidth = (int) Math.min(
                600 * mMetrics.density, (75 * mMetrics.widthPixels / 100));

        mPrimaryOptionsView.removeAllViews();
        mSecondaryOptionsUpperView.removeAllViews();
        mSecondaryOptionsLowerView.removeAllViews();
        mPrimaryWidth = mPrimaryHeight = mSecondaryWidth = mSecondaryHeight = 0;
        mSecondaryHeight = (int) mMetrics.density * 8;

        boolean hasRoom = true;
        boolean first = true;
        int count = mOptions.size();
        final boolean upper = isUpperBlock();
        for (int i = 0; i < count; i++) {
            if (!hasOption(i)) {
                continue;
            }

            View v = createOption(hasRoom, i, mOptions.get(i).first);
            int[] measuring = measureView(v);
            if (!first && maxWidth < (mPrimaryWidth + measuring[0])) {
                v = createOption(false, i, mOptions.get(i).first);
                measuring = measureView(v);
                hasRoom = false;
            }

            if (hasRoom) {
                mPrimaryWidth += measuring[0];
                mPrimaryHeight = Math.max(measuring[1], mPrimaryHeight);
            } else {
                mSecondaryWidth = Math.max(measuring[0], mSecondaryWidth);
                mSecondaryHeight += measuring[1];
            }

            ViewGroup parent = hasRoom
                    ? mPrimaryOptionsView
                    : upper ? mSecondaryOptionsUpperView : mSecondaryOptionsLowerView;
            parent.addView(v);

            first = false;
        }
    }

    private TextView createOption(boolean hasRoom, int id, String text) {
        final int layoutId = hasRoom
                ? R.layout.ace_primary_option_layout : R.layout.ace_secondary_option_layout;
        TextView view = (TextView) mLayoutInflater.inflate(layoutId, null, false);
        view.setText(text);
        view.setTag(id);
        view.setOnClickListener(mOnClickListener);
        return view;
    }

    private void onShowMoreOptions() {
        final boolean upper = isUpperBlock();
        mPrimaryWidth = mPrimaryOptionsViewBlock.getWidth();
        mPrimaryOptionsViewBlock.setVisibility(View.GONE);
        mSecondaryOptionsViewUpperBlock.setVisibility(upper ? View.VISIBLE : View.GONE);
        mSecondaryOptionsViewLowerBlock.setVisibility(upper ? View.GONE : View.VISIBLE);
        if (upper) {
            mPopup.update(mInitialX + (mPrimaryWidth - mSecondaryWidth),
                    mInitialY - mSecondaryHeight, mSecondaryWidth, mPrimaryHeight + mSecondaryHeight);
        } else {
            mPopup.update(mInitialX + (mPrimaryWidth - mSecondaryWidth),
                    mInitialY, mSecondaryWidth, mPrimaryHeight + mSecondaryHeight);
        }
    }

    private void onHideMoreOptions() {
        mPrimaryOptionsViewBlock.setVisibility(View.VISIBLE);
        mSecondaryOptionsViewUpperBlock.setVisibility(View.GONE);
        mSecondaryOptionsViewLowerBlock.setVisibility(View.GONE);
        mPopup.update(mInitialX, mInitialY, mPrimaryWidth, mPrimaryHeight);
    }

    private boolean hasOption(int option) {
        if (option == OPTION_PASTE) {
            return mClipboard.hasPrimaryClip()
                    && mClipboard.getPrimaryClipDescription().hasMimeType(
                            ClipDescription.MIMETYPE_TEXT_PLAIN);
        } else if (!mHasSelection & (option == OPTION_CUT || option == OPTION_COPY)) {
            return false;
        }
        return true;
    }

    private int[] measureView(View v) {
        int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        v.measure(widthMeasureSpec, heightMeasureSpec);
        return new int[]{v.getMeasuredWidth(), v.getMeasuredHeight()};
    }

    private boolean isUpperBlock() {
        return mInitialY > (mMetrics.heightPixels / 2);
    }
}
