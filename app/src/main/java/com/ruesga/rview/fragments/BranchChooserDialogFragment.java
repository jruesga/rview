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
import com.ruesga.rview.adapters.BranchAdapter;
import com.ruesga.rview.adapters.FilterableAdapter;
import com.ruesga.rview.databinding.BranchChooserDialogBinding;
import com.ruesga.rview.preferences.Constants;
import com.ruesga.rview.widget.DelayedAutocompleteTextView;

public class BranchChooserDialogFragment extends FilterableDialogFragment {

    public static final String TAG = "BranchChooserDialogFragment";

    public static BranchChooserDialogFragment newInstance(int title, int action,
            String projectId, String branch, String message, View anchor, int requestCode) {
        BranchChooserDialogFragment fragment = new BranchChooserDialogFragment();
        Bundle arguments = new Bundle();
        arguments.putInt(Constants.EXTRA_TITLE, title);
        arguments.putInt(Constants.EXTRA_ACTION, action);
        arguments.putString(Constants.EXTRA_PROJECT_ID, projectId);
        arguments.putString(Constants.EXTRA_BRANCH, branch);
        if (message != null) {
            arguments.putString(Constants.EXTRA_MESSAGE, message);
        }
        arguments.putParcelable(EXTRA_ANCHOR, computeViewOnScreen(anchor));
        arguments.putInt(EXTRA_REQUEST_CODE, requestCode);
        fragment.setArguments(arguments);
        return fragment;
    }

    private BranchChooserDialogBinding mBinding;
    private BranchAdapter mAdapter;

    private int mTitleRes;
    private int mActionRes;

    private String mProjectId;
    private String mBranch;
    private String mMessage;

    private boolean mIsValidated;

    public BranchChooserDialogFragment() {
    }

    @Override
    public int getFilterableItems() {
        return 1;
    }

    @Override
    public FilterableAdapter[] getAdapter() {
        return new FilterableAdapter[]{mAdapter};
    }

    @NonNull
    @Override
    public DelayedAutocompleteTextView[] getFilterView() {
        return new DelayedAutocompleteTextView[]{mBinding.branch};
    }

    @Override
    public int getDialogTitle() {
        return mTitleRes;
    }

    @Override
    public int getDialogActionLabel() {
        return mActionRes;
    }

    @Override
    public boolean isAllowEmpty() {
        return false;
    }

    @Override
    public boolean isSelectionRequired(int pos) {
        return true;
    }

    @Override
    public boolean isValidated() {
        return mIsValidated;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mTitleRes = getArguments().getInt(Constants.EXTRA_TITLE);
        mActionRes = getArguments().getInt(Constants.EXTRA_ACTION);
        mProjectId = getArguments().getString(Constants.EXTRA_PROJECT_ID);
        mBranch = getArguments().getString(Constants.EXTRA_BRANCH);
        mMessage = getArguments().getString(Constants.EXTRA_MESSAGE);
    }

    @Override
    public ViewDataBinding inflateView(LayoutInflater inflater,
            @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(
                inflater, R.layout.branch_chooser_dialog, container, true);
        if (mMessage != null) {
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
        } else {
            mIsValidated = true;
            mBinding.messageBlock.setVisibility(View.GONE);
        }
        mAdapter = new BranchAdapter(mBinding.getRoot().getContext(), mProjectId, mBranch);
        return mBinding;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mBinding.unbind();
    }

    @Override
    public Object transformResult(int pos, String result) {
        if (TextUtils.isEmpty(result)) {
            return null;
        }
        if (mMessage == null) {
            return result;
        }

        String msg = mBinding.message.getText().toString();
        if (TextUtils.isEmpty(msg)) {
            return null;
        }
        return new String[]{result, msg};
    }

}
