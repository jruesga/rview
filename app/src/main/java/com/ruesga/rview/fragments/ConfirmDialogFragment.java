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

import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.View;

public class ConfirmDialogFragment extends RevealDialogFragment {

    public static final String TAG = "ConfirmDialogFragment";

    public interface OnActionConfirmed {
        void onActionConfirmed();
    }

    private static final String EXTRA_TITLE = "title";
    private static final String EXTRA_MESSAGE = "message";
    private static final String EXTRA_ACTION = "action";

    private OnActionConfirmed mCallback;

    public static ConfirmDialogFragment newInstance(
            String title, String message, String action, View anchor) {
        ConfirmDialogFragment fragment = new ConfirmDialogFragment();
        Bundle arguments = new Bundle();
        arguments.putString(EXTRA_TITLE, title);
        arguments.putString(EXTRA_MESSAGE, message);
        arguments.putString(EXTRA_ACTION, action);
        arguments.putParcelable(EXTRA_ANCHOR, computeViewOnScreen(anchor));
        fragment.setArguments(arguments);
        return fragment;
    }

    public ConfirmDialogFragment() {
    }

    public void setOnActionConfirmed(OnActionConfirmed cb) {
        mCallback = cb;
    }

    @Override
    public void buildDialog(AlertDialog.Builder builder, Bundle savedInstanceState) {
        String title = getArguments().getString(EXTRA_TITLE);
        String message = getArguments().getString(EXTRA_MESSAGE);
        String action = getArguments().getString(EXTRA_ACTION);
        if (TextUtils.isEmpty(action)) {
            action = getString(android.R.string.ok);
        }
        builder.setTitle(title)
                .setMessage(message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(action, (dialog, which) -> performConfirmAction());
    }

    private void performConfirmAction() {
        if (mCallback != null) {
            mCallback.onActionConfirmed();
        }
    }

}
