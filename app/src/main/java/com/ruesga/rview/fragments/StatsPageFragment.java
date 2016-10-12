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
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ruesga.rview.BaseActivity;
import com.ruesga.rview.R;
import com.ruesga.rview.databinding.StatsPageFragmentBinding;

import me.tatarka.rxloader.RxLoaderManager;
import me.tatarka.rxloader.RxLoaderManagerCompat;
import me.tatarka.rxloader.RxLoaderObserver;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public abstract class StatsPageFragment<T> extends Fragment {

    private StatsPageFragmentBinding mBinding;

    private final RxLoaderObserver<T> mDetailsObserver = new RxLoaderObserver<T>() {
        @Override
        public void onNext(T result) {
            bindDetails(result);
            showProgress(false, result);
        }

        @Override
        public void onError(Throwable e) {
            ((BaseActivity) getActivity()).handleException(getStatsFragmentTag(), e);
            showProgress(false, null);
        }

        @Override
        public void onStarted() {
            showProgress(true, null);
        }
    };

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.stats_page_fragment, container, false);
        mBinding.details.addView(inflateDetails(inflater, mBinding.details));
        startLoadersWithValidContext();
        return mBinding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        startLoadersWithValidContext();
    }

    private void startLoadersWithValidContext() {
        if (getActivity() == null) {
            return;
        }

        RxLoaderManager loaderManager = RxLoaderManagerCompat.get(this);
        loaderManager.create("details", internalFetchDetails(), mDetailsObserver).start();
    }

    public abstract View inflateDetails(LayoutInflater inflater, @Nullable ViewGroup container);

    public abstract Observable<T> fetchDetails();

    public abstract void bindDetails(T result);

    public abstract String getStatsFragmentTag();

    private void showProgress(boolean show, T result) {
        BaseActivity activity = (BaseActivity) getActivity();
        if (show) {
            activity.onRefreshStart();
        } else {
            activity.onRefreshEnd(result);
        }
    }

    private Observable<T> internalFetchDetails() {
        return fetchDetails()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }
}
