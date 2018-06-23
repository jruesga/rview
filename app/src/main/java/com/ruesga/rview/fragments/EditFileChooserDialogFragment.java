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
import android.content.Context;
import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.os.Bundle;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.ruesga.rview.R;
import com.ruesga.rview.adapters.FileChooserAdapter;
import com.ruesga.rview.adapters.FilterableAdapter;
import com.ruesga.rview.databinding.EditFileChooserDialogBinding;
import com.ruesga.rview.misc.ModelHelper;
import com.ruesga.rview.preferences.Constants;
import com.ruesga.rview.widget.DelayedAutocompleteTextView;

public class EditFileChooserDialogFragment extends FilterableDialogFragment {

    public static final String TAG = "EditFileChooserDialogFragment";

    @Keep
    public enum MODE {
        ADD, DELETE, RENAME
    }

    public interface OnEditFileChosen {
        void onEditFileChosen(int requestCode, MODE mode, String oldValue, String newValue);
    }

    private static final String EXTRA_MODE = "mode";
    private static final String EXTRA_TITLE = "title";
    private static final String EXTRA_ACTION = "action";
    private static final String EXTRA_VALUE = "value";
    private static final String EXTRA_HINT_1 = "hint1";
    private static final String EXTRA_HINT_2 = "hint2";
    private static final String EXTRA_PREV_FILES = "prev_files";

    @Keep
    public static class Model {
        public MODE mode;
        public String value1;
        public String value2;
        public String hint1;
        public String hint2;
        private boolean value1Locked;
        public boolean valid;
    }

    public static EditFileChooserDialogFragment newAddInstance(Context context, int requestCode,
            int legacyChangeId, String revisionId, View anchor) {
        EditFileChooserDialogFragment fragment = new EditFileChooserDialogFragment();
        Bundle arguments = new Bundle();
        arguments.putInt(Constants.EXTRA_LEGACY_CHANGE_ID, legacyChangeId);
        arguments.putString(Constants.EXTRA_REVISION_ID, revisionId);
        arguments.putString(EXTRA_MODE, MODE.ADD.name());
        arguments.putInt(EXTRA_TITLE, R.string.edit_file_add);
        arguments.putInt(EXTRA_ACTION, R.string.action_add);
        arguments.putString(EXTRA_HINT_2, context.getString(R.string.edit_file_add_delete_hint));
        arguments.putParcelable(EXTRA_ANCHOR, computeViewOnScreen(anchor));
        arguments.putInt(EXTRA_REQUEST_CODE, requestCode);
        fragment.setArguments(arguments);
        return fragment;
    }

    public static EditFileChooserDialogFragment newDeleteInstance(Context context, int requestCode,
            int legacyChangeId, String revisionId, View anchor) {
        EditFileChooserDialogFragment fragment = new EditFileChooserDialogFragment();
        Bundle arguments = new Bundle();
        arguments.putInt(Constants.EXTRA_LEGACY_CHANGE_ID, legacyChangeId);
        arguments.putString(Constants.EXTRA_REVISION_ID, revisionId);
        arguments.putString(EXTRA_MODE, MODE.DELETE.name());
        arguments.putInt(EXTRA_TITLE, R.string.edit_file_delete);
        arguments.putInt(EXTRA_ACTION, R.string.action_delete);
        arguments.putString(EXTRA_HINT_2, context.getString(R.string.edit_file_add_delete_hint));
        arguments.putParcelable(EXTRA_ANCHOR, computeViewOnScreen(anchor));
        arguments.putInt(EXTRA_REQUEST_CODE, requestCode);
        fragment.setArguments(arguments);
        return fragment;
    }

    public static EditFileChooserDialogFragment newRenameInstance(Context context, int requestCode,
            int legacyChangeId, String revisionId, String source, String[] prevFiles, View anchor) {
        EditFileChooserDialogFragment fragment = new EditFileChooserDialogFragment();
        Bundle arguments = new Bundle();
        arguments.putInt(Constants.EXTRA_LEGACY_CHANGE_ID, legacyChangeId);
        arguments.putString(Constants.EXTRA_REVISION_ID, revisionId);
        arguments.putString(EXTRA_MODE, MODE.RENAME.name());
        arguments.putInt(EXTRA_TITLE, R.string.edit_file_rename);
        arguments.putInt(EXTRA_ACTION, R.string.action_rename);
        arguments.putString(EXTRA_HINT_1, context.getString(R.string.edit_file_rename_old_hint));
        arguments.putString(EXTRA_HINT_2, context.getString(R.string.edit_file_rename_new_hint));
        arguments.putString(EXTRA_VALUE, source);
        arguments.putParcelable(EXTRA_ANCHOR, computeViewOnScreen(anchor));
        arguments.putInt(EXTRA_REQUEST_CODE, requestCode);
        if (prevFiles != null) {
            arguments.putStringArray(EXTRA_PREV_FILES, prevFiles);
        }
        fragment.setArguments(arguments);
        return fragment;
    }

    private EditFileChooserDialogBinding mBinding;
    private FileChooserAdapter mAdapter1;
    private FileChooserAdapter mAdapter2;
    private final Model mModel = new Model();
    private int mTitle;
    private int mAction;
    private String[] mPrevFiles;

    public EditFileChooserDialogFragment() {
    }

    @Override
    public int getFilterableItems() {
        return mModel.mode.equals(MODE.RENAME) ? 2 : 1;
    }

    @Override
    public FilterableAdapter[] getAdapter() {
        if (mModel.mode.equals(MODE.RENAME)) {
            return new FilterableAdapter[]{mAdapter1, mAdapter2};
        }
        return new FilterableAdapter[]{mAdapter1};
    }

    @NonNull
    @Override
    public DelayedAutocompleteTextView[] getFilterView() {
        if (mModel.mode.equals(MODE.RENAME)) {
            return new DelayedAutocompleteTextView[]{mBinding.edit1, mBinding.edit2};
        }
        return new DelayedAutocompleteTextView[]{mBinding.edit2};
    }

    @Override
    public int getDialogTitle() {
        return mTitle;
    }

    @Override
    public int getDialogActionLabel() {
        return mAction;
    }

    @Override
    public boolean isAllowEmpty() {
        return false;
    }

    @Override
    public boolean isSelectionRequired(int pos) {
        switch (mModel.mode) {
            case DELETE:
                return true;
            case RENAME:
                return !mModel.value1Locked && pos == 0;
        }
        return false;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        //noinspection ConstantConditions
        mModel.mode = MODE.valueOf(getArguments().getString(EXTRA_MODE));
        mModel.value1 = getArguments().getString(EXTRA_VALUE);
        mModel.hint1 = getArguments().getString(EXTRA_HINT_1);
        mModel.hint2 = getArguments().getString(EXTRA_HINT_2);
        mModel.value1Locked = !TextUtils.isEmpty(mModel.value1);
        mTitle = getArguments().getInt(EXTRA_TITLE);
        mAction = getArguments().getInt(EXTRA_ACTION);
        mPrevFiles = getArguments().getStringArray(EXTRA_PREV_FILES);

        if (savedInstanceState != null) {
            mModel.value1Locked = savedInstanceState.getBoolean("value1Locked", mModel.value1Locked);
        }

        super.onCreate(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean("value1Locked", mModel.value1Locked);
    }

    @Override
    public ViewDataBinding inflateView(LayoutInflater inflater,
            @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        //noinspection ConstantConditions
        int legacyChangeId = getArguments().getInt(Constants.EXTRA_LEGACY_CHANGE_ID);
        String revisionId = getArguments().getString(Constants.EXTRA_REVISION_ID);
        mBinding = DataBindingUtil.inflate(inflater, R.layout.edit_file_chooser_dialog, container, true);
        if (mModel.mode.equals(MODE.RENAME) && mModel.value1Locked) {
            mBinding.edit1.setEnabled(false);
        }
        mBinding.setModel(mModel);

        mAdapter1 = new FileChooserAdapter(mBinding.getRoot().getContext(),
                String.valueOf(legacyChangeId), revisionId);
        mAdapter2 = new FileChooserAdapter(mBinding.getRoot().getContext(),
                String.valueOf(legacyChangeId), revisionId);
        return mBinding;
    }

    @Override
    public void onDialogReveled() {
        super.onDialogReveled();
        EditText v = mModel.mode.equals(MODE.RENAME) && mBinding.edit1.isEnabled()
                ? mBinding.edit1 : mBinding.edit2;
        v.clearFocus();
        v.requestFocus();
        v.selectAll();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mBinding.unbind();
    }


    @Override
    @SuppressWarnings("SimplifiableIfStatement")
    public boolean isValidated() {
        // COMMIT_MESSAGE is not a valid name
        if (ModelHelper.isCommitMessage(mModel.value1)
                || ModelHelper.isCommitMessage(mModel.value2)) {
            return false;
        }

        if (!mModel.mode.equals(MODE.RENAME)) {
            return true;
        }

        return !mModel.value1.equals(mModel.value2) && isValidRenameFile(mModel.value2);
    }

    private boolean isValidRenameFile(String v) {
        if (mPrevFiles != null) {
            for (String f : mPrevFiles) {
                if (f.equals(v)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public boolean handleResult(int requestCode, Object[] result) {
        Activity a = getActivity();
        Fragment f = getParentFragment();
        if (f instanceof OnEditFileChosen) {
            ((OnEditFileChosen) f).onEditFileChosen(
                    getRequestCode(), mModel.mode,  mModel.value1, mModel.value2);
        } else if (a instanceof OnEditFileChosen) {
            ((OnEditFileChosen) a).onEditFileChosen(
                    getRequestCode(), mModel.mode,  mModel.value1, mModel.value2);
        }
        return true;
    }
}
