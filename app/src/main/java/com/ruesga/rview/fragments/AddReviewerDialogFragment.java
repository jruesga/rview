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
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Keep;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import com.ruesga.rview.R;
import com.ruesga.rview.adapters.SuggestedReviewersAdapter;
import com.ruesga.rview.databinding.AddReviewerDialogBinding;
import com.ruesga.rview.gerrit.model.AddReviewerState;
import com.ruesga.rview.gerrit.model.SuggestedReviewerInfo;
import com.ruesga.rview.misc.AndroidHelper;
import com.ruesga.rview.preferences.Constants;

public class AddReviewerDialogFragment extends RevealDialogFragment {

    public static final String TAG = "AddReviewerDialogFragment";

    private static final String EXTRA_REVIEWER_STATE = "reviewer_state";

    @Keep
    public static class Model {
        public String reviewer;
    }

    public interface OnReviewerAdded {
        void onReviewerAdded(String reviewer, AddReviewerState state);
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
            String text = s.toString();
            enabledOrDisableButtons(text);

            mModel.reviewer = text;
            mBinding.setModel(mModel);
        }
    };


    public static AddReviewerDialogFragment newInstance(
            int legacyChangeId, AddReviewerState reviewerState, View anchor) {
        AddReviewerDialogFragment fragment = new AddReviewerDialogFragment();
        Bundle arguments = new Bundle();
        arguments.putInt(Constants.EXTRA_LEGACY_CHANGE_ID, legacyChangeId);
        arguments.putString(EXTRA_REVIEWER_STATE, reviewerState.name());
        arguments.putParcelable(EXTRA_ANCHOR, computeViewOnScreen(anchor));
        fragment.setArguments(arguments);
        return fragment;
    }

    private AddReviewerDialogBinding mBinding;
    private final Model mModel = new Model();

    private int mLegacyChangeId;
    private AddReviewerState mReviewerState;

    public AddReviewerDialogFragment() {
    }

    @Override
    public void buildDialog(AlertDialog.Builder builder, Bundle savedInstanceState) {
        LayoutInflater inflater = LayoutInflater.from(builder.getContext());
        mBinding = DataBindingUtil.inflate(inflater, R.layout.add_reviewer_dialog, null, true);
        mBinding.reviewer.addTextChangedListener(mTextWatcher);
        mBinding.reviewer.setOnItemClickListener((parent, view, position, id) -> {
            SuggestedReviewerInfo reviewer =
                    ((SuggestedReviewersAdapter) (parent.getAdapter())).getSuggestedReviewerAt(position);
            mModel.reviewer = reviewer.account != null
                    ? String.valueOf(reviewer.account.accountId) : reviewer.group.id;
            mBinding.setModel(mModel);
            AndroidHelper.hideSoftKeyboard(getContext(), getDialog().getWindow());
        });
        mBinding.setModel(mModel);
        SuggestedReviewersAdapter adapter = new SuggestedReviewersAdapter(
                mBinding.getRoot().getContext(), mLegacyChangeId);
        mBinding.reviewer.setAdapter(adapter);

        builder.setTitle(mReviewerState.equals(AddReviewerState.REVIEWER)
                    ? R.string.change_details_add_reviewer : R.string.change_details_add_cc)
                .setView(mBinding.getRoot())
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_add, (dialog, which) -> performAddReviewer());
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLegacyChangeId = getArguments().getInt(Constants.EXTRA_LEGACY_CHANGE_ID);
        mReviewerState = AddReviewerState.valueOf(getArguments().getString(EXTRA_REVIEWER_STATE));
    }

    @Override
    public void onDialogReveled() {
        enabledOrDisableButtons(mBinding.reviewer.getText().toString());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mBinding.unbind();
    }

    private void performAddReviewer() {
        final Activity a = getActivity();
        final Fragment f = getParentFragment();
        if (f != null && f instanceof OnReviewerAdded) {
            ((OnReviewerAdded) f).onReviewerAdded(mModel.reviewer, mReviewerState);
        } else if (a != null && a instanceof OnReviewerAdded) {
            ((OnReviewerAdded) a).onReviewerAdded(mModel.reviewer, mReviewerState);
        }
    }

    private void enabledOrDisableButtons(String query) {
        if (getDialog() != null) {
            final AlertDialog dialog = ((AlertDialog) getDialog());
            Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (button != null) {
                button.setEnabled(query.length() >= mBinding.reviewer.getThreshold());
            }
        }
    }
}
