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

import android.databinding.ViewDataBinding;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.ruesga.rview.adapters.BaseAdapter;
import com.ruesga.rview.annotations.ProguardIgnored;
import com.ruesga.rview.misc.AndroidHelper;
import com.ruesga.rview.widget.DelayedAutocompleteTextView;

public abstract class FilterableDialogFragment extends RevealDialogFragment {

    public static final String TAG = "FilterableDialogFragment";

    public interface OnFilterSelectedListener {
        void onFilterSelected(String newBase);
    }

    @ProguardIgnored
    public static class Model {
        public String filter;
    }

    private OnFilterSelectedListener mCallback;
    private boolean mIsUserSelection;
    private String mUserSelection;

    public FilterableDialogFragment() {
    }

    public void setOnFilterSelectedListener(OnFilterSelectedListener cb) {
        mCallback = cb;
    }

    public abstract BaseAdapter getAdapter();

    public abstract @NonNull DelayedAutocompleteTextView getFilterView();

    public abstract @StringRes int getDialogTitle();

    public abstract @StringRes int getDialogActionLabel();

    public abstract boolean isAllowEmpty();

    public abstract ViewDataBinding inflateView(LayoutInflater inflater,
            @Nullable ViewGroup container, @Nullable Bundle savedInstanceState);

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mUserSelection = savedInstanceState.getString("userSelection");
            mIsUserSelection = savedInstanceState.getBoolean("isUserSelection");
        }
    }

    @Override
    public void buildDialog(AlertDialog.Builder builder, Bundle savedInstanceState) {
        builder.setTitle(getDialogTitle())
                .setView(onCreateView(LayoutInflater.from(getContext()), null, savedInstanceState))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(getDialogActionLabel(),
                        (dialog, which) -> performNotifyFilterSelected());
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("userSelection", mUserSelection);
        outState.putBoolean("isUserSelection", mIsUserSelection);
    }

    @Nullable
    @Override
    public final View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        ViewDataBinding binding = inflateView(inflater, container, savedInstanceState);
        getFilterView().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                mIsUserSelection = false;
                enabledOrDisableButtons();
            }
        });
        getFilterView().setOnItemClickListener((parent, view, position, id) -> {
            mUserSelection = parent.getAdapter().getItem(position).toString();
            mIsUserSelection = true;
            enabledOrDisableButtons();
            AndroidHelper.hideSoftKeyboard(getContext(), getDialog().getWindow());
        });
        getFilterView().setAdapter(getAdapter());
        return binding.getRoot();
    }

    @Override
    public void onDialogReveled() {
        enabledOrDisableButtons();
    }

    private void performNotifyFilterSelected() {
        String result = transformResult(mUserSelection);
        if (!isAllowEmpty() && TextUtils.isEmpty(result)) {
            mIsUserSelection = false;
            enabledOrDisableButtons();
        } else {
            mCallback.onFilterSelected(result);
        }
    }

    public String transformResult(String result) {
        return result;
    }

    private void enabledOrDisableButtons() {
        if (getDialog() != null) {
            final AlertDialog dialog = ((AlertDialog) getDialog());
            Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (button != null) {
                button.setEnabled((isAllowEmpty() && TextUtils.isEmpty(mUserSelection))
                        || mIsUserSelection);
            }
        }
    }
}
