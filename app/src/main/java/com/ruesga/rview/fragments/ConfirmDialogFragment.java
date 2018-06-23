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
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.View;

import com.ruesga.rview.R;

public class ConfirmDialogFragment extends RevealDialogFragment {

    public static final String TAG = "ConfirmDialogFragment";

    public interface OnActionConfirmed {
        void onActionConfirmed(int requestCode);
    }

    private static final String EXTRA_MESSAGE = "message";

    private static final String EXTRA_REQUEST_CODE = "request_code";

    private int mRequestCode;

    public static ConfirmDialogFragment newInstance(String message, View anchor, int requestCode) {
        ConfirmDialogFragment fragment = new ConfirmDialogFragment();
        Bundle arguments = new Bundle();
        arguments.putString(EXTRA_MESSAGE, message);
        arguments.putParcelable(EXTRA_ANCHOR, computeViewOnScreen(anchor));
        arguments.putInt(EXTRA_REQUEST_CODE, requestCode);
        fragment.setArguments(arguments);
        return fragment;
    }

    public ConfirmDialogFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //noinspection ConstantConditions
        mRequestCode = getArguments().getInt(EXTRA_REQUEST_CODE);
    }

    @Override
    public void buildDialog(AlertDialog.Builder builder, Bundle savedInstanceState) {
        //noinspection ConstantConditions
        String message = getArguments().getString(EXTRA_MESSAGE);
        builder.setMessage(message)
                .setNegativeButton(R.string.action_no, null)
                .setPositiveButton(R.string.action_yes, (dialog, which) -> performConfirmAction());
    }

    private void performConfirmAction() {
        Activity a = getActivity();
        Fragment f = getParentFragment();
        if (f instanceof OnActionConfirmed) {
            ((OnActionConfirmed) f).onActionConfirmed(mRequestCode);
        } else if (a instanceof OnActionConfirmed) {
            ((OnActionConfirmed) a).onActionConfirmed(mRequestCode);
        }
    }

}
