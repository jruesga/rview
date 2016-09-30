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
import android.databinding.ViewDataBinding;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ruesga.rview.R;
import com.ruesga.rview.adapters.CherryPickAdapter;
import com.ruesga.rview.adapters.FilterableAdapter;
import com.ruesga.rview.databinding.CherryPickChooserDialogBinding;
import com.ruesga.rview.preferences.Constants;
import com.ruesga.rview.widget.DelayedAutocompleteTextView;

public class CherryPickChooserDialogFragment extends FilterableDialogFragment {

    public static final String TAG = "CherryPickChooserDialogFragment";

    public static CherryPickChooserDialogFragment newInstance(
            String projectId, String branch, String message, View anchor) {
        CherryPickChooserDialogFragment fragment = new CherryPickChooserDialogFragment();
        Bundle arguments = new Bundle();
        arguments.putString(Constants.EXTRA_PROJECT_ID, projectId);
        arguments.putString(Constants.EXTRA_BRANCH, branch);
        arguments.putString(Constants.EXTRA_MESSAGE, message);
        arguments.putParcelable(EXTRA_ANCHOR, computeViewOnScreen(anchor));
        fragment.setArguments(arguments);
        return fragment;
    }

    private CherryPickChooserDialogBinding mBinding;
    private CherryPickAdapter mAdapter;

    private String mProjectId;
    private String mBranch;
    private String mMessage;

    private boolean mIsValidated;

    public CherryPickChooserDialogFragment() {
    }

    @Override
    public FilterableAdapter getAdapter() {
        return mAdapter;
    }

    @NonNull
    @Override
    public DelayedAutocompleteTextView getFilterView() {
        return mBinding.branch;
    }

    @Override
    public int getDialogTitle() {
        return R.string.change_action_cherrypick;
    }

    @Override
    public int getDialogActionLabel() {
        return R.string.change_action_cherrypick;
    }

    @Override
    public boolean isAllowEmpty() {
        return false;
    }

    @Override
    public boolean isValidated() {
        return mIsValidated;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mProjectId = getArguments().getString(Constants.EXTRA_PROJECT_ID);
        mBranch = getArguments().getString(Constants.EXTRA_BRANCH);
        mMessage = getArguments().getString(Constants.EXTRA_MESSAGE);
    }

    @Override
    public ViewDataBinding inflateView(LayoutInflater inflater,
            @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.cherry_pick_chooser_dialog, container, true);
        mBinding.message.setText(mMessage);
        mIsValidated = !mMessage.isEmpty();
        mBinding.message.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                mIsValidated = !s.toString().isEmpty();

            }
        });
        mAdapter = new CherryPickAdapter(mBinding.getRoot().getContext(), mProjectId, mBranch);
        return mBinding;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mBinding.unbind();
    }

    @Override
    public Object transformResult(String result) {
        if (TextUtils.isEmpty(result)) {
            return null;
        }
        String msg = mBinding.message.getText().toString();
        if (TextUtils.isEmpty(msg)) {
            return null;
        }

        return new String[]{result, msg};
    }

}
