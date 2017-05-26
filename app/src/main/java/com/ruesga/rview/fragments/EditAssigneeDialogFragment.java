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
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import com.ruesga.rview.R;
import com.ruesga.rview.adapters.AccountsAdapter;
import com.ruesga.rview.databinding.EditAssigneeDialogBinding;
import com.ruesga.rview.gerrit.model.AccountInfo;
import com.ruesga.rview.misc.AndroidHelper;

public class EditAssigneeDialogFragment extends RevealDialogFragment {

    public static final String TAG = "EditAssigneeDialogFragment";

    @Keep
    public static class Model {
        public String assignee;
    }

    public interface OnAssigneeSelected {
        void onAssigneeSelected(String assignee);
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

            mModel.assignee = text;
            mBinding.setModel(mModel);
        }
    };


    public static EditAssigneeDialogFragment newInstance(View anchor) {
        EditAssigneeDialogFragment fragment = new EditAssigneeDialogFragment();
        Bundle arguments = new Bundle();
        arguments.putParcelable(EXTRA_ANCHOR, computeViewOnScreen(anchor));
        fragment.setArguments(arguments);
        return fragment;
    }

    private EditAssigneeDialogBinding mBinding;
    private final Model mModel = new Model();

    public EditAssigneeDialogFragment() {
    }

    @Override
    public void buildDialog(AlertDialog.Builder builder, Bundle savedInstanceState) {
        LayoutInflater inflater = LayoutInflater.from(builder.getContext());
        mBinding = DataBindingUtil.inflate(inflater, R.layout.edit_assignee_dialog, null, true);
        mBinding.assignee.addTextChangedListener(mTextWatcher);
        mBinding.assignee.setOnItemClickListener((parent, view, position, id) -> {
            AccountInfo account = ((AccountsAdapter) (parent.getAdapter())).getAccountAt((position));
            mModel.assignee = String.valueOf(account.accountId);
            mBinding.setModel(mModel);
            AndroidHelper.hideSoftKeyboard(getContext(), getDialog().getWindow());
        });
        mBinding.setModel(mModel);
        AccountsAdapter adapter = new AccountsAdapter(mBinding.getRoot().getContext());
        mBinding.assignee.setAdapter(adapter);

        builder.setTitle(R.string.edit_assignee_title)
                .setView(mBinding.getRoot())
                .setIcon(ContextCompat.getDrawable(getContext(), R.drawable.ic_account_circle))
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_assign, (dialog, which) -> performAssigneeSelected());
    }

    @Override
    public void onDialogReveled() {
        enabledOrDisableButtons(mBinding.assignee.getText().toString());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mBinding.unbind();
    }

    private void performAssigneeSelected() {
        final Activity a = getActivity();
        final Fragment f = getParentFragment();
        if (f != null && f instanceof OnAssigneeSelected) {
            ((OnAssigneeSelected) f).onAssigneeSelected(mModel.assignee);
        } else if (a != null && a instanceof OnAssigneeSelected) {
            ((OnAssigneeSelected) a).onAssigneeSelected(mModel.assignee);
        }
    }

    private void enabledOrDisableButtons(String query) {
        if (getDialog() != null) {
            final AlertDialog dialog = ((AlertDialog) getDialog());
            Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (button != null) {
                button.setEnabled(query.length() >= mBinding.assignee.getThreshold());
            }
        }
    }
}
