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
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ruesga.rview.wizard.R;
import com.ruesga.rview.wizard.WizardActivity;
import com.ruesga.rview.wizard.WizardChooserFragment;
import com.ruesga.rview.wizard.databinding.ListChooserBinding;
import com.ruesga.rview.wizard.databinding.ListChooserItemBinding;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import me.tatarka.rxloader2.RxLoader;
import me.tatarka.rxloader2.RxLoaderManager;
import me.tatarka.rxloader2.RxLoaderManagerCompat;
import me.tatarka.rxloader2.RxLoaderObserver;

public abstract class ListChooserFragment extends WizardChooserFragment {

    private static final int MESSAGE_FILTER_CHANGED = 1;

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
            mModel.filter = s.toString();
            mBinding.setModel(mModel);
        }
    };

    public static class ListChooserItemViewHolder extends RecyclerView.ViewHolder {
        private final ListChooserItemBinding mBinding;
        public ListChooserItemViewHolder(ListChooserItemBinding binding) {
            super(binding.getRoot());
            mBinding = binding;
            binding.executePendingBindings();
        }
    }

    @Keep
    public static class ItemModel {
        public String title;
        public String summary;
        public boolean trustAllCertificates;
    }

    @Keep
    public static class Model {
        public boolean hasData = true;
        public boolean hasFilter = false;
        public String filter = "";
    }

    @Keep
    @SuppressWarnings("UnusedParameters")
    public static class EventHandlers {
        private ListChooserFragment mFragment;

        public EventHandlers(ListChooserFragment fragment) {
            mFragment = fragment;
        }

        public void onClearFilter(View v) {
            mFragment.performClearFilter();
        }
    }

    @Keep
    @SuppressWarnings("unused")
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

        @NonNull
        @Override
        public ListChooserItemViewHolder onCreateViewHolder(
                @NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            return new ListChooserItemViewHolder(
                    DataBindingUtil.inflate(inflater, R.layout.list_chooser_item, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ListChooserItemViewHolder holder, int position) {
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

    private final RxLoaderObserver<List<ItemModel>> mDataProducerObserver =
            new RxLoaderObserver<List<ItemModel>>() {
                @Override
                public void onNext(List<ItemModel> result) {
                    mAdapter.clear();
                    mAdapter.addAll(result);
                    mAdapter.notifyDataSetChanged();
                    mModel.hasData = result != null && !result.isEmpty();
                    mBinding.setModel(mModel);
                }

                @Override
                public void onError(Throwable error) {
                    mAdapter.clear();
                    mAdapter.notifyDataSetChanged();
                    mModel.hasData = false;
                    mBinding.setModel(mModel);
                    //noinspection ConstantConditions
                    ((WizardActivity)getActivity()).showMessage(
                            getString(R.string.chooser_failed_to_fetch_data));
                }
            };


    private ListChooserBinding mBinding;
    private final Handler mHandler;
    private Model mModel = new Model();

    private ListAdapter mAdapter;
    private ItemModel mSelectedItem;

    private RxLoader<List<ItemModel>> mLoader;

    public ListChooserFragment() {
        mHandler = new Handler(msg -> {
            if (msg.what == MESSAGE_FILTER_CHANGED) {
                if (mBinding != null) {
                    fetchData();
                }
            }
            return true;
        });
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
    public final View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.list_chooser, container, false);
        mBinding.search.addTextChangedListener(mTextChangedListener);
        mModel.hasFilter = supportFiltering();
        mBinding.setModel(mModel);
        return mBinding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mBinding.setHandlers(new EventHandlers(this));

        // Configure the adapter
        mAdapter = new ListAdapter(this);
        mBinding.list.setLayoutManager(new LinearLayoutManager(
                getActivity(), RecyclerView.VERTICAL, false));
        mBinding.list.setAdapter(mAdapter);

        // Fetch or join current loader
        RxLoaderManager loaderManager = RxLoaderManagerCompat.get(this);
        mLoader = loaderManager.create(refreshItems(), mDataProducerObserver);
        fetchData();
    }

    @Override
    public final void onDestroyView() {
        super.onDestroyView();
        mBinding.unbind();
    }

    @NonNull
    public abstract Observable<List<ItemModel>> getDataProducer();

    @NonNull
    public abstract Intent toResult(ItemModel item);

    @Override
    public final boolean hasAcceptButton() {
        return false;
    }

    public boolean supportFiltering() {
        return false;
    }

    @SuppressWarnings("ConstantConditions")
    private void onItemClick(ItemModel item) {
        mSelectedItem = item;
        ((WizardActivity) getActivity()).closeKeyboardIfNeeded();
        close();
    }

    private void fetchData() {
        mLoader.clear();
        mLoader.restart();
    }

    @SuppressWarnings("ConstantConditions")
    private Observable<List<ItemModel>> refreshItems() {
        return getDataProducer()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe(disposable ->
                    ((WizardActivity)getActivity()).changeInProgressStatus(true))
            .doOnTerminate(() ->
                    ((WizardActivity)getActivity()).changeInProgressStatus(false));
    }

    private void performClearFilter() {
        mModel.filter = "";
        mBinding.setModel(mModel);
        fetchData();
    }

    public String getFilter() {
        return mModel.filter;
    }
}
