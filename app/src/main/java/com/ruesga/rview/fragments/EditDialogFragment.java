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

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.ruesga.rview.R;
import com.ruesga.rview.annotations.ProguardIgnored;
import com.ruesga.rview.databinding.EditDialogBinding;

public class EditDialogFragment extends RevealDialogFragment {

    public static final String TAG = "EditDialogFragment";

    private static final String EXTRA_TITLE = "title";
    private static final String EXTRA_VALUE = "value";
    private static final String EXTRA_HINT = "hint";
    private static final String EXTRA_ACTION = "action";
    private static final String EXTRA_ALLOW_EMPTY = "allow_empty";

    @ProguardIgnored
    public static class Model {
        public String value;
        public String hint;
        boolean allowEmpty;
    }

    public interface OnEditChanged {
        void onEditChanged(String newValue);
    }

    private final TextWatcher mTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            enabledOrDisableButtons(s.toString());
        }
    };


    public static EditDialogFragment newInstance(String title, String value,
                String action, String hint, boolean allowEmpty, View anchor) {
        EditDialogFragment fragment = new EditDialogFragment();
        Bundle arguments = new Bundle();
        arguments.putString(EXTRA_TITLE, title);
        arguments.putString(EXTRA_VALUE, value);
        arguments.putString(EXTRA_ACTION, action);
        arguments.putString(EXTRA_HINT, hint);
        arguments.putBoolean(EXTRA_ALLOW_EMPTY, allowEmpty);
        arguments.putParcelable(EXTRA_ANCHOR, computeViewOnScreen(anchor));
        fragment.setArguments(arguments);
        return fragment;
    }

    private EditDialogBinding mBinding;
    private final Model mModel = new Model();
    private OnEditChanged mCallback;

    private String mOriginalValue;

    public EditDialogFragment() {
    }

    public void setOnEditChanged(OnEditChanged cb) {
        mCallback = cb;
    }

    @Override
    public void buildDialog(AlertDialog.Builder builder, Bundle savedInstanceState) {
        String title = getArguments().getString(EXTRA_TITLE);
        String action = getArguments().getString(EXTRA_ACTION);
        if (TextUtils.isEmpty(action)) {
            action = getString(R.string.action_change);
        }
        builder.setTitle(title)
                .setView(onCreateView(LayoutInflater.from(getContext()), null, savedInstanceState))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(action, (dialog, which) -> performEditChanged());
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mModel.value = getArguments().getString(EXTRA_VALUE);
        mModel.hint = getArguments().getString(EXTRA_HINT);
        mModel.allowEmpty = getArguments().getBoolean(EXTRA_ALLOW_EMPTY, false);
        mOriginalValue = mModel.value == null ? "" : mModel.value;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.edit_dialog, container, true);
        mBinding.edit.addTextChangedListener(mTextWatcher);
        mBinding.setModel(mModel);
        return mBinding.getRoot();
    }

    @Override
    public void onDialogReveled() {
        enabledOrDisableButtons(mModel.value == null ? "" : mModel.value);
        mBinding.edit.clearFocus();
        mBinding.edit.requestFocus();
        mBinding.edit.selectAll();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mBinding.unbind();
    }

    private void performEditChanged() {
        mCallback.onEditChanged(mModel.value);
    }

    private void enabledOrDisableButtons(String value) {
        if (getDialog() != null) {
            final AlertDialog dialog = ((AlertDialog) getDialog());
            Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (button != null) {
                button.setEnabled(
                        (!value.equals(mOriginalValue) || (value.isEmpty() && mModel.allowEmpty))
                        && (!value.isEmpty() || (value.isEmpty() && mModel.allowEmpty)));
            }
        }
    }
}
