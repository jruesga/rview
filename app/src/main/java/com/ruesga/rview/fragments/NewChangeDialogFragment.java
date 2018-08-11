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
import com.ruesga.rview.adapters.BranchAdapter;
import com.ruesga.rview.adapters.FilterableAdapter;
import com.ruesga.rview.adapters.ProjectAdapter;
import com.ruesga.rview.databinding.NewChangeDialogBinding;
import com.ruesga.rview.misc.SerializationManager;
import com.ruesga.rview.widget.DelayedAutocompleteTextView;

public class NewChangeDialogFragment extends FilterableDialogFragment {

    public static final String TAG = "NewChangeDialogFragment";

    private static final String EXTRA_MODEL = "model";

    public interface OnNewChangeRequestedListener {
        void onNewChangeRequested(int requestCode, String project, String branch, String topic,
                String subject, boolean isPrivate, boolean isWorkInProgress);
    }

    @Keep
    public static class Model {
        public String project;
        public String branch;
        public String topic;
        public String subject;
        public boolean isPrivate;
        public boolean isWorkInProgress = true;
    }

    public static NewChangeDialogFragment newInstance(int requestCode, View anchor) {
        NewChangeDialogFragment fragment = new NewChangeDialogFragment();
        Bundle arguments = new Bundle();
        arguments.putParcelable(EXTRA_ANCHOR, computeViewOnScreen(anchor));
        arguments.putInt(EXTRA_REQUEST_CODE, requestCode);
        fragment.setArguments(arguments);
        return fragment;
    }

    private NewChangeDialogBinding mBinding;
    private ProjectAdapter mProjectAdapter;
    private BranchAdapter mBranchAdapter;
    private Model mModel = new Model();

    public NewChangeDialogFragment() {
    }

    @Override
    public int getFilterableItems() {
        return 4;
    }

    @Override
    public FilterableAdapter[] getAdapter() {
        return new FilterableAdapter[]{mProjectAdapter, mBranchAdapter, null, null};
    }

    @NonNull
    @Override
    public DelayedAutocompleteTextView[] getFilterView() {
        return new DelayedAutocompleteTextView[]{
                mBinding.project, mBinding.branch, mBinding.topic, mBinding.subject};
    }

    @Override
    public int getDialogTitle() {
        return R.string.create_new_change_title;
    }

    @Override
    public int getDialogActionLabel() {
        return R.string.action_create;
    }

    @Override
    public boolean isAllowEmpty() {
        return true;
    }

    @Override
    public boolean isSelectionRequired(int pos) {
        return pos <= 1;
    }

    public boolean isValidated(int pos) {
        return pos == 2;
    }

    public void onItemSelected(int pos, String value) {
        if (pos == 0) {
            mBranchAdapter.setProjectId(value);
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            String model = savedInstanceState.getString(EXTRA_MODEL, null);
            if (!TextUtils.isEmpty(model)) {
                mModel = SerializationManager.getInstance().fromJson(model, Model.class);
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(EXTRA_MODEL, SerializationManager.getInstance().toJson(mModel));
    }

    @Override
    public ViewDataBinding inflateView(LayoutInflater inflater,
            @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.new_change_dialog, container, true);
        mBinding.setModel(mModel);

        mProjectAdapter = new ProjectAdapter(mBinding.getRoot().getContext());
        mBranchAdapter = new BranchAdapter(mBinding.getRoot().getContext());
        return mBinding;
    }

    @Override
    public void onDialogReveled() {
        super.onDialogReveled();
        EditText v = mBinding.project;
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
        return !TextUtils.isEmpty(mModel.project)
                && !TextUtils.isEmpty(mModel.branch)
                && !TextUtils.isEmpty(mModel.subject);
    }

    @Override
    public boolean handleResult(int requestCode, Object[] result) {
        Activity a = getActivity();
        Fragment f = getParentFragment();
        if (f instanceof OnNewChangeRequestedListener) {
            ((OnNewChangeRequestedListener) f).onNewChangeRequested(
                    getRequestCode(), mModel.project, mModel.branch, mModel.topic,
                            mModel.subject, mModel.isPrivate, mModel.isWorkInProgress);
        } else if (a instanceof OnNewChangeRequestedListener) {
            ((OnNewChangeRequestedListener) a).onNewChangeRequested(
                    getRequestCode(), mModel.project, mModel.branch, mModel.topic,
                            mModel.subject, mModel.isPrivate, mModel.isWorkInProgress);
        }
        return true;
    }
}
