/*
 * Copyright (C) 2017 Jorge Ruesga
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
import android.os.Handler;
import android.support.annotation.Keep;
import android.support.annotation.StringRes;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;

import com.ruesga.rview.R;
import com.ruesga.rview.databinding.ListDialogBinding;

public abstract class ListDialogFragment extends RevealDialogFragment {

    private static final int MESSAGE_FILTER_CHANGED = 1;

    @Keep
    @SuppressWarnings("UnusedParameters")
    public static class EventHandlers {
        private ListDialogFragment mFragment;

        public EventHandlers(ListDialogFragment fragment) {
            mFragment = fragment;
        }

        public void onClearFilter(View v) {
            mFragment.performClearFilter();
        }
    }

    private TextWatcher mTextChangedListener = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            mHandler.removeMessages(MESSAGE_FILTER_CHANGED);
            mHandler.sendEmptyMessageDelayed(MESSAGE_FILTER_CHANGED, 350L);
            mBinding.setFilter(s.toString());
        }
    };

    private ListDialogBinding mBinding;
    private final Handler mHandler;

    public ListDialogFragment() {
        mHandler = new Handler(msg -> {
            if (msg.what == MESSAGE_FILTER_CHANGED) {
                if (mBinding != null) {
                    mBinding.setEmpty(onFilterChanged(mBinding.search.getText().toString()));
                }
            }
            return true;
        });
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("filter", mBinding.search.getText().toString());
    }

    @Override
    public final void buildDialog(AlertDialog.Builder builder, Bundle savedInstanceState) {
        LayoutInflater inflater = LayoutInflater.from(builder.getContext());
        mBinding = DataBindingUtil.inflate(inflater, R.layout.list_dialog, null, true);
        mBinding.list.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        mBinding.list.setAdapter(getAdapter());
        if (savedInstanceState != null) {
            mBinding.setFilter(savedInstanceState.getString("filter"));
        }
        mBinding.search.addTextChangedListener(mTextChangedListener);
        mBinding.setEmpty(false);
        mBinding.setHandlers(new EventHandlers(this));

        builder.setTitle(getTitle())
                .setView(mBinding.getRoot())
                .setNegativeButton(R.string.action_cancel, null);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mBinding.search.removeTextChangedListener(mTextChangedListener);
        mBinding.unbind();
    }

    public void performClearFilter() {
        mBinding.setFilter("");
        mBinding.search.setText("");
    }

    public abstract RecyclerView.Adapter<RecyclerView.ViewHolder> getAdapter();

    public abstract @StringRes int getTitle();

    public abstract boolean onFilterChanged(String newFilter);
}
