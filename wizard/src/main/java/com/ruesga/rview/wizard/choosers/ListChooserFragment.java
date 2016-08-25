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
package com.ruesga.rview.wizard.choosers;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.airbnb.rxgroups.AutoResubscribe;
import com.airbnb.rxgroups.GroupLifecycleManager;
import com.airbnb.rxgroups.ObservableGroup;
import com.airbnb.rxgroups.ObservableManager;
import com.airbnb.rxgroups.ResubscriptionObserver;
import com.ruesga.rview.wizard.R;
import com.ruesga.rview.wizard.WizardActivity;
import com.ruesga.rview.wizard.WizardChooserFragment;
import com.ruesga.rview.wizard.annotations.ProguardIgnored;
import com.ruesga.rview.wizard.databinding.ListChooserBinding;
import com.ruesga.rview.wizard.databinding.ListChooserItemBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.schedulers.Schedulers;

public abstract class ListChooserFragment extends WizardChooserFragment {

    public static class ListChooserItemViewHolder extends RecyclerView.ViewHolder {
        private final ListChooserItemBinding mBinding;
        public ListChooserItemViewHolder(ListChooserItemBinding binding) {
            super(binding.getRoot());
            mBinding = binding;
            binding.executePendingBindings();
        }
    }

    @ProguardIgnored
    public static class ItemModel {
        public String title;
        public String summary;
    }

    @ProguardIgnored
    public static class Model {
        public boolean hasData = true;
    }

    @ProguardIgnored
    public static class ItemEventHandlers {
        ListChooserFragment mFragment;

        public ItemEventHandlers(ListChooserFragment fragment) {
            mFragment = fragment;
        }

        public void onItemPressed(View view) {
            ItemModel item = (ItemModel) view.getTag();
            mFragment.onItemClick(item);
        }
    }

    private static class ListAdapter extends RecyclerView.Adapter<ListChooserItemViewHolder> {
        private final List<ItemModel> mData = new ArrayList<>();
        private final ItemEventHandlers mHandlers;

        public ListAdapter(ListChooserFragment fragment) {
            mHandlers = new ItemEventHandlers(fragment);
        }

        private void addAll(List<ItemModel> data) {
            mData.addAll(data);
        }

        private void clear() {
            mData.clear();
        }

        @Override
        public ListChooserItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            return new ListChooserItemViewHolder((ListChooserItemBinding)
                    DataBindingUtil.inflate(inflater, R.layout.list_chooser_item, parent, false));
        }

        @Override
        public void onBindViewHolder(ListChooserItemViewHolder holder, int position) {
            ItemModel item = mData.get(position);
            holder.mBinding.item.setTag(item);
            holder.mBinding.setHandlers(mHandlers);
            holder.mBinding.setModel(item);
        }

        @Override
        public int getItemCount() {
            return mData.size();
        }
    }

    @AutoResubscribe
    public final ResubscriptionObserver<List<ItemModel>> mDataProducerObserver
            = new ResubscriptionObserver<List<ItemModel>>() {
        @Override
        public void onCompleted() {
        }

        @Override
        public void onError(Throwable e) {
            mAdapter.clear();
            mAdapter.notifyDataSetChanged();
            mModel.hasData = false;
            mBinding.setModel(mModel);
            ((WizardActivity)getActivity()).showMessage(
                    getString(R.string.chooser_failed_to_fetch_data));

        }

        @Override
        public void onNext(List<ItemModel> result) {
            mAdapter.clear();
            mAdapter.addAll(result);
            mAdapter.notifyDataSetChanged();
            mModel.hasData = result != null && !result.isEmpty();
            mBinding.setModel(mModel);
        }

        @Override
        public Object resubscriptionTag() {
            return getClass().getSimpleName();
        }
    };

    private GroupLifecycleManager mGroupLifecycleManager;
    private ObservableGroup mObservableGroup;

    private ListChooserBinding mBinding;
    private Model mModel = new Model();

    private ListAdapter mAdapter;
    private ItemModel mSelectedItem;

    public ListChooserFragment() {
    }

    @Override
    public final Intent getResult() {
        if (mSelectedItem == null) {
            return null;
        }
        return toResult(mSelectedItem);
    }

    @Nullable
    @Override
    public final View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.list_chooser, container, false);
        mBinding.setModel(mModel);
        return mBinding.getRoot();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mGroupLifecycleManager.onSaveInstanceState(outState);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Configure the adapter
        mAdapter = new ListAdapter(this);
        mBinding.list.setLayoutManager(new LinearLayoutManager(
                getActivity(), LinearLayoutManager.VERTICAL, false));
        mBinding.list.setAdapter(mAdapter);

        // Configure RxGroups to manage rxjava fragment lifecycle
        ObservableManager manager = ((WizardActivity) getActivity()).getObservableManager();
        mGroupLifecycleManager = GroupLifecycleManager.onCreate(manager,
                savedInstanceState, this);
        mObservableGroup = mGroupLifecycleManager.group();
        mGroupLifecycleManager.onResume();
        refreshItems();
    }

    @Override
    public final void onDestroyView() {
        super.onDestroyView();
        mBinding.unbind();
        mGroupLifecycleManager.onPause();
        mGroupLifecycleManager.onDestroy(getActivity());
    }

    @NonNull
    public abstract Callable<List<ItemModel>> getDataProducer();

    @NonNull
    public abstract Intent toResult(ItemModel item);

    @Override
    public final boolean hasAcceptButton() {
        return false;
    }

    private void onItemClick(ItemModel item) {
        mSelectedItem = item;
        close();
    }

    private void refreshItems() {
        Observable.fromCallable(getDataProducer())
                .subscribeOn(Schedulers.io())
                .compose(mObservableGroup.<List<ItemModel>>transform(getClass().getSimpleName()))
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(new Action0() {
                    @Override
                    public void call() {
                        ((WizardActivity)getActivity()).changeInProgressStatus(true);
                    }
                })
                .doOnTerminate(new Action0() {
                    @Override
                    public void call() {
                        ((WizardActivity)getActivity()).changeInProgressStatus(false);
                    }
                })
                .subscribe(mDataProducerObserver);
    }
}
