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

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Keep;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;

import com.ruesga.rview.R;
import com.ruesga.rview.databinding.IncludedInDialogBinding;
import com.ruesga.rview.gerrit.GerritApi;
import com.ruesga.rview.gerrit.filter.ChangeQuery;
import com.ruesga.rview.gerrit.model.ChangeInfo;
import com.ruesga.rview.gerrit.model.IncludedInInfo;
import com.ruesga.rview.misc.ActivityHelper;
import com.ruesga.rview.misc.ModelHelper;
import com.ruesga.rview.preferences.Constants;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import me.tatarka.rxloader2.RxLoader;
import me.tatarka.rxloader2.RxLoaderManager;
import me.tatarka.rxloader2.RxLoaderManagerCompat;
import me.tatarka.rxloader2.RxLoaderObserver;
import me.tatarka.rxloader2.safe.SafeObservable;

public class IncludedInDialogFragment extends RevealDialogFragment {

    public static final String TAG = "IncludedInDlgFragment";

    private final RxLoaderObserver<IncludedInInfo> mRequestIncludedInObserver
            = new RxLoaderObserver<IncludedInInfo>() {
        @Override
        public void onNext(IncludedInInfo includedIn) {
            mModel.loading = false;
            mModel.empty = includedIn == null ||
                    includedIn.branches == null || includedIn.branches.length == 0;
            mModel.includedInInfo = includedIn;
            update();
        }

        @Override
        public void onError(Throwable e) {
            mModel.loading = false;
            mModel.empty = true;
            mModel.includedInInfo = null;
            update();
        }
    };

    @Keep
    public static class Model {
        public boolean loading;
        public boolean empty;
        public IncludedInInfo includedInInfo;
    }

    public static IncludedInDialogFragment newInstance(ChangeInfo change) {
        IncludedInDialogFragment fragment = new IncludedInDialogFragment();
        Bundle arguments = new Bundle();
        arguments.putInt(Constants.EXTRA_LEGACY_CHANGE_ID, change.legacyChangeId);
        fragment.setArguments(arguments);
        return fragment;
    }

    private int mLegacyChangeId;
    private Model mModel;

    private IncludedInDialogBinding mBinding;
    private RxLoader<IncludedInInfo> mIncludedInLoader;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //noinspection ConstantConditions
        mLegacyChangeId = getArguments().getInt(Constants.EXTRA_LEGACY_CHANGE_ID);
        mModel = new Model();
    }

    @Override
    public final void onDestroyView() {
        super.onDestroyView();
        mBinding.unbind();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        RxLoaderManager loaderManager = RxLoaderManagerCompat.get(this);
        mIncludedInLoader = loaderManager.create(
                "request_included_in", doRequestIncludedIn(), mRequestIncludedInObserver);
        performFetchIncludeIn();

    }
    @Override
    public void buildDialog(AlertDialog.Builder builder, Bundle savedInstanceState) {
        LayoutInflater inflater = LayoutInflater.from(builder.getContext());
        mBinding = DataBindingUtil.inflate(inflater, R.layout.included_in_dialog, null, true);
        mBinding.branches.listenTo(link ->  {
            if (getActivity() != null) {
                String title = getString(R.string.change_details_branch);
                ChangeQuery filter = new ChangeQuery().branch(link);
                ActivityHelper.openChangeListByFilterActivity(
                        getActivity(), title, filter, false, false);
            }
        });
        mBinding.setModel(mModel);

        builder.setTitle(R.string.included_in_dialog_title)
                .setView(mBinding.getRoot())
                .setPositiveButton(R.string.action_close, null);
    }

    @SuppressWarnings("ConstantConditions")
    private Observable<IncludedInInfo> doRequestIncludedIn() {
        final Context ctx = getActivity();
        final GerritApi api = ModelHelper.getGerritApi(ctx);
        return SafeObservable.fromNullCallable(() ->
                api.getChangeIncludedIn(String.valueOf(mLegacyChangeId)).blockingFirst())
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread());
    }

    private void performFetchIncludeIn() {
        mModel.loading = true;
        mModel.empty = false;
        mModel.includedInInfo = null;
        update();

        mIncludedInLoader.clear();
        mIncludedInLoader.restart();
    }

    private void update() {
        if (mBinding != null) {
            mBinding.setModel(mModel);
            if (mModel.includedInInfo != null && mModel.includedInInfo.branches != null) {
                mBinding.branches
                        .withLinks(mModel.includedInInfo.branches)
                        .update();
            }
        }
    }
}
