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

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import me.tatarka.rxloader2.RxLoader;
import me.tatarka.rxloader2.RxLoaderManager;
import me.tatarka.rxloader2.RxLoaderManagerCompat;
import me.tatarka.rxloader2.RxLoaderObserver;
import me.tatarka.rxloader2.safe.SafeObservable;

public abstract class ListDialogFragment<T> extends RevealDialogFragment {

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

    private final RxLoaderObserver<List<T>> mFilterObserver = new RxLoaderObserver<List<T>>() {
                @Override
                public void onNext(List<T> filter) {
                    if (mBinding != null) {
                        mBinding.setEmpty(onDataRefreshed(filter));
                    }
                }

                @Override
                public void onError(Throwable error) {
                    if (mBinding != null) {
                        mBinding.setEmpty(onDataRefreshed(new ArrayList<>()));
                    }
                }
            };

    private ListDialogBinding mBinding;
    private final Handler mHandler;
    private RxLoader<List<T>> mLoader;

    public ListDialogFragment() {
        mHandler = new Handler(msg -> {
            if (msg.what == MESSAGE_FILTER_CHANGED) {
                restartData();
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

        startLoadersWithValidContext();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        startLoadersWithValidContext();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mBinding.search.removeTextChangedListener(mTextChangedListener);
        mBinding.unbind();
    }

    private void startLoadersWithValidContext() {
        RxLoaderManager loaderManager = RxLoaderManagerCompat.get(this);
        mLoader = loaderManager.create(refreshItems(), mFilterObserver);
    }

    private void restartData() {
        mLoader.clear();
        mLoader.restart();
    }

    @SuppressWarnings("ConstantConditions")
    private Observable<List<T>> refreshItems() {
        return SafeObservable.fromCallable(() -> {
                if (mBinding != null) {
                    return onFilterChanged(mBinding.search.getText().toString());
                }
                return new ArrayList<T>();
            })
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread());
    }

    public void performClearFilter() {
        mBinding.setFilter("");
        mBinding.search.setText("");
    }

    public void refresh() {
        mHandler.removeMessages(MESSAGE_FILTER_CHANGED);
        mHandler.sendEmptyMessageDelayed(MESSAGE_FILTER_CHANGED, 1L);
        mBinding.setFilter(mBinding.search.getText().toString());
    }

    public abstract RecyclerView.Adapter<RecyclerView.ViewHolder> getAdapter();

    public abstract @StringRes int getTitle();

    public abstract List<T> onFilterChanged(String newFilter);

    public abstract boolean onDataRefreshed(List<T> data);
}
