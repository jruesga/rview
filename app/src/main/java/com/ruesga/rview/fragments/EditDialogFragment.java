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
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import com.ruesga.rview.R;
import com.ruesga.rview.databinding.EditDialogBinding;

import java.util.regex.Pattern;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;

public class EditDialogFragment extends RevealDialogFragment {

    public static final String TAG = "EditDialogFragment";

    private static final String EXTRA_TITLE = "title";
    private static final String EXTRA_SUBTITLE = "subtitle";
    private static final String EXTRA_VALUE = "value";
    private static final String EXTRA_HINT = "hint";
    private static final String EXTRA_ACTION = "action";
    private static final String EXTRA_ALLOW_EMPTY = "allow_empty";
    private static final String EXTRA_ALLOW_SUGGESTIONS = "allow_suggestions";
    private static final String EXTRA_USE_MULTI_LINE = "multi_line";
    private static final String EXTRA_REGEXP = "regexp";

    private static final String EXTRA_REQUEST_CODE = "request_code";
    private static final String EXTRA_REQUEST_DATA= "request_data";

    @Keep
    public static class Model {
        public String subtitle;
        public String value;
        public String hint;
        boolean allowEmpty;
        public boolean isMultiLine;
        public Pattern regexp;
    }

    public interface OnEditChanged {
        void onEditChanged(int requestCode, @Nullable Bundle requestData, String newValue);
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


    public static EditDialogFragment newInstance(String title, String subtitle, String value,
            String action, String hint, boolean allowEmpty, boolean allowSuggestions,
            boolean multiLine, String regexp, View anchor, int requestCode,
            @Nullable Bundle data) {
        EditDialogFragment fragment = new EditDialogFragment();
        Bundle arguments = new Bundle();
        arguments.putString(EXTRA_TITLE, title);
        if (!TextUtils.isEmpty(subtitle)) {
            arguments.putString(EXTRA_SUBTITLE, subtitle);
        }
        arguments.putString(EXTRA_VALUE, value);
        arguments.putString(EXTRA_ACTION, action);
        arguments.putString(EXTRA_HINT, hint);
        arguments.putBoolean(EXTRA_ALLOW_EMPTY, allowEmpty);
        arguments.putBoolean(EXTRA_ALLOW_SUGGESTIONS, allowSuggestions);
        arguments.putBoolean(EXTRA_USE_MULTI_LINE, multiLine);
        if (!TextUtils.isEmpty(regexp)) {
            arguments.putString(EXTRA_REGEXP, regexp);
        }
        arguments.putParcelable(EXTRA_ANCHOR, computeViewOnScreen(anchor));
        arguments.putInt(EXTRA_REQUEST_CODE, requestCode);
        if (data != null) {
            arguments.putBundle(EXTRA_REQUEST_DATA, data);
        }
        fragment.setArguments(arguments);
        return fragment;
    }

    private int mRequestCode;
    private Bundle mRequestData;
    private Pattern mRegExp = null;

    protected EditDialogBinding mBinding;
    private final Model mModel = new Model();

    private String mOriginalValue;

    public EditDialogFragment() {
    }

    @Override
    public void buildDialog(AlertDialog.Builder builder, Bundle savedInstanceState) {
        //noinspection ConstantConditions
        String title = getArguments().getString(EXTRA_TITLE);
        String action = getArguments().getString(EXTRA_ACTION);
        if (TextUtils.isEmpty(action)) {
            action = getString(R.string.action_change);
        }

        boolean allowSuggestions = getArguments().getBoolean(EXTRA_ALLOW_SUGGESTIONS, false);
        int inputType = InputType.TYPE_CLASS_TEXT |
                (mModel.isMultiLine ? InputType.TYPE_TEXT_FLAG_MULTI_LINE : 0) |
                (allowSuggestions ? 0 : InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

        LayoutInflater inflater = LayoutInflater.from(builder.getContext());
        mBinding = DataBindingUtil.inflate(inflater, R.layout.edit_dialog, null, true);
        mBinding.edit.setInputType(inputType);
        mBinding.edit.addTextChangedListener(mTextWatcher);
        mBinding.setModel(mModel);

        builder.setTitle(title)
                .setView(mBinding.getRoot())
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(action, (dialog, which) -> performEditChanged());
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //noinspection ConstantConditions
        mRequestCode = getArguments().getInt(EXTRA_REQUEST_CODE);
        if (getArguments().containsKey(EXTRA_REQUEST_DATA)) {
            mRequestData = getArguments().getBundle(EXTRA_REQUEST_DATA);
        }
        if (getArguments().containsKey(EXTRA_REGEXP)) {
            //noinspection ConstantConditions
            mRegExp = Pattern.compile(getArguments().getString(EXTRA_REGEXP));
        }

        mModel.subtitle = getArguments().getString(EXTRA_SUBTITLE);
        if (savedInstanceState != null) {
            mModel.value = savedInstanceState.getString(EXTRA_VALUE, null);
        } else {
            mModel.value = getArguments().getString(EXTRA_VALUE);
        }
        mModel.hint = getArguments().getString(EXTRA_HINT);
        mModel.allowEmpty = getArguments().getBoolean(EXTRA_ALLOW_EMPTY, false);
        mModel.isMultiLine = getArguments().getBoolean(EXTRA_USE_MULTI_LINE, false);
        mOriginalValue = "";
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

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        String v = mBinding.edit.getText().toString();
        if (!TextUtils.isEmpty(v)) {
            outState.putString(EXTRA_VALUE, v);
        }
    }

    private void performEditChanged() {
        Activity a = getActivity();
        Fragment f = getParentFragment();
        if (f instanceof OnEditChanged) {
            ((OnEditChanged) f).onEditChanged(mRequestCode, mRequestData, mModel.value);
        } else if (a instanceof OnEditChanged) {
            ((OnEditChanged) a).onEditChanged(mRequestCode, mRequestData, mModel.value);
        }
    }

    private void enabledOrDisableButtons(String value) {
        if (getDialog() != null) {
            final AlertDialog dialog = ((AlertDialog) getDialog());
            Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (button != null) {
                button.setEnabled(
                        (!value.equals(mOriginalValue) || (value.isEmpty() && mModel.allowEmpty))
                        && (!value.isEmpty() || (value.isEmpty() && mModel.allowEmpty))
                        && (mRegExp == null || mRegExp.matcher(value).matches()));
            }
        }
    }
}
