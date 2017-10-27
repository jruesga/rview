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

import android.app.Activity;
import android.databinding.ViewDataBinding;
import android.os.Bundle;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Button;

import com.ruesga.rview.R;
import com.ruesga.rview.adapters.FilterableAdapter;
import com.ruesga.rview.misc.AndroidHelper;
import com.ruesga.rview.widget.DelayedAutocompleteTextView;

public abstract class FilterableDialogFragment extends RevealDialogFragment {

    public static final String TAG = "FilterableDialogFragment";

    public static final String EXTRA_REQUEST_CODE = "request_code";

    public interface OnFilterSelectedListener {
        void onFilterSelected(int requestCode, Object[] o);
    }

    @Keep
    public static class Model {
        public String filter;
    }

    private boolean mIsUserSelection[];
    private String mUserSelection[];
    private int mRequestCode;

    public FilterableDialogFragment() {
    }

    public abstract int getFilterableItems();

    public abstract FilterableAdapter[] getAdapter();

    public abstract @NonNull DelayedAutocompleteTextView[] getFilterView();

    public abstract @StringRes int getDialogTitle();

    public abstract @StringRes int getDialogActionLabel();

    public abstract boolean isAllowEmpty();

    public abstract boolean isSelectionRequired(int pos);

    public abstract ViewDataBinding inflateView(LayoutInflater inflater,
            @Nullable ViewGroup container, @Nullable Bundle savedInstanceState);

    public boolean isValidated() {
        return true;
    }

    public int getRequestCode() {
        return mRequestCode;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //noinspection ConstantConditions
        mRequestCode = getArguments().getInt(EXTRA_REQUEST_CODE, 0);
        int count = getFilterableItems();
        mIsUserSelection = new boolean[count];
        mUserSelection = new String[count];

        if (savedInstanceState != null) {
            for (int i = 0; i < count; i++) {
                mUserSelection[i] = savedInstanceState.getString("userSelection" + i);
                mIsUserSelection[i] = savedInstanceState.getBoolean("isUserSelection" + i);
            }
        }
    }

    @Override
    public void buildDialog(AlertDialog.Builder builder, Bundle savedInstanceState) {
        LayoutInflater inflater = LayoutInflater.from(builder.getContext());
        ViewDataBinding binding = inflateView(inflater, null, savedInstanceState);

        int count = getFilterableItems();
        for (int i = 0; i < count; i++) {
            final int item = i;
            getFilterView()[i].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    mUserSelection[item] = null;
                    mIsUserSelection[item] = false;
                    enabledOrDisableButtons();
                }
            });
            getFilterView()[i].setOnItemClickListener((parent, view, position, id) -> {
                mUserSelection[item] = parent.getAdapter().getItem(position).toString();
                mIsUserSelection[item] = true;
                enabledOrDisableButtons();
                AndroidHelper.hideSoftKeyboard(getContext(), getDialog().getWindow());
                onItemSelected(item, mUserSelection[item]);
            });
            getFilterView()[i].setAdapter(getAdapter()[i]);
        }

        builder.setTitle(getDialogTitle())
                .setView(binding.getRoot())
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(getDialogActionLabel(),
                        (dialog, which) -> performNotifyFilterSelected());
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        int count = getFilterableItems();
        for (int i = 0; i < count; i++) {
            outState.putString("userSelection" + i, mUserSelection[i]);
            outState.putBoolean("isUserSelection" + i, mIsUserSelection[i]);
        }
    }

    public void onItemSelected(int pos, String value) {
    }

    @Override
    public void onDialogReveled() {
        enabledOrDisableButtons();
    }

    public boolean handleResult(int requestCode, Object[] result) {
        return false;
    }

    private void performNotifyFilterSelected() {
        int count = getFilterableItems();
        Object[] result = new Object[count];
        for (int i = 0; i < count; i++) {
            result[i] = transformResult(i, mUserSelection[i]);
        }

        boolean valid = true;
        if (!isAllowEmpty()) {
            for (int i = 0; i < count; i++) {
                if (result[i] == null) {
                    mIsUserSelection[i] = false;
                    valid = false;
                }
            }

            if (!valid) {
                valid = enabledOrDisableButtons();
            }
        }

        if (valid && !handleResult(mRequestCode, result)) {
            Activity a = getActivity();
            Fragment f = getParentFragment();
            if (f != null && f instanceof OnFilterSelectedListener) {
                ((OnFilterSelectedListener) f).onFilterSelected(mRequestCode, result);
            } else if (a != null && a instanceof OnFilterSelectedListener) {
                ((OnFilterSelectedListener) a).onFilterSelected(mRequestCode, result);
            }
        }
    }

    public Object transformResult(int pos, String result) {
        return result;
    }

    public boolean isValidated(int pos) {
        return false;
    }

    boolean enabledOrDisableButtons() {
        if (getDialog() != null) {
            final AlertDialog dialog = ((AlertDialog) getDialog());
            Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (button != null) {
                boolean valid = isValidSelection() && isValidated();
                button.setEnabled(valid);
                return valid;
            }
        }
        return false;
    }

    private boolean isValidSelection() {
        int count = getFilterableItems();
        for (int i = 0; i < count; i++) {
            if (isValidated(i)) {
                continue;
            }
            CharSequence text = getFilterView()[i].getText();
            if (isAllowEmpty() && TextUtils.isEmpty(text)) {
                continue;
            }
            if (isSelectionRequired(i) && !mIsUserSelection[i]) {
                return false;
            }
            if (TextUtils.isEmpty(text)) {
                return false;
            }
        }
        return true;
    }
}
