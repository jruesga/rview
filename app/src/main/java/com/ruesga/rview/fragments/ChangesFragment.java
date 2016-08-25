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

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
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
import com.ruesga.rview.R;
import com.ruesga.rview.RviewApplication;
import com.ruesga.rview.annotations.ProguardIgnored;
import com.ruesga.rview.databinding.ChangesFragmentBinding;
import com.ruesga.rview.databinding.ChangesItemBinding;
import com.ruesga.rview.gerrit.GerritApi;
import com.ruesga.rview.gerrit.GerritServiceFactory;
import com.ruesga.rview.gerrit.filter.ChangeQuery;
import com.ruesga.rview.gerrit.filter.StatusType;
import com.ruesga.rview.gerrit.model.ChangeInfo;
import com.ruesga.rview.gerrit.model.ChangeOptions;
import com.ruesga.rview.model.Account;
import com.ruesga.rview.preferences.Preferences;
import com.ruesga.rview.widget.DividerItemDecoration;
import com.ruesga.rview.wizard.WizardActivity;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.schedulers.Schedulers;

public class ChangesFragment extends Fragment {

    public static class ItemViewHolder extends RecyclerView.ViewHolder {
        private final ChangesItemBinding mBinding;
        public ItemViewHolder(ChangesItemBinding binding) {
            super(binding.getRoot());
            mBinding = binding;
            binding.executePendingBindings();
        }
    }

    @com.ruesga.rview.wizard.annotations.ProguardIgnored
    public static class ItemEventHandlers {
        ChangesFragment mFragment;

        public ItemEventHandlers(ChangesFragment fragment) {
            mFragment = fragment;
        }

        public void onItemPressed(View view) {
            ChangeInfo item = (ChangeInfo) view.getTag();
            mFragment.onItemClick(item);
        }
    }

    @ProguardIgnored
    public static class Model {
        public boolean hasData = true;
    }

    private static class ChangesAdapter extends RecyclerView.Adapter<ItemViewHolder> {
        private final List<ChangeInfo> mData = new ArrayList<>();
        private final ItemEventHandlers mHandlers;

        public ChangesAdapter(ChangesFragment fragment) {
            setHasStableIds(true);
            mHandlers = new ItemEventHandlers(fragment);
        }

        private void addAll(List<ChangeInfo> data) {
            mData.addAll(data);
        }

        @Override
        public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            return new ItemViewHolder((ChangesItemBinding)
                    DataBindingUtil.inflate(inflater, R.layout.changes_item, parent, false));
        }

        @Override
        public void onBindViewHolder(ItemViewHolder holder, int position) {
            ChangeInfo item = mData.get(position);
            holder.itemView.setTag(item);
            holder.mBinding.setModel(item);
            holder.mBinding.setHandlers(mHandlers);
        }

        @Override
        public long getItemId(int position) {
            return mData.get(position).id.hashCode();
        }

        @Override
        public int getItemCount() {
            return mData.size();
        }
    }

    @AutoResubscribe
    public final ResubscriptionObserver<List<ChangeInfo>> mChangesObserver
            = new ResubscriptionObserver<List<ChangeInfo>>() {
        @Override
        public void onCompleted() {
        }

        @Override
        public void onError(Throwable e) {
            e.printStackTrace();
            mModel.hasData = false;
            mBinding.setModel(mModel);
        }

        @Override
        public void onNext(List<ChangeInfo> changes) {
            mAdapter.addAll(changes);
            mAdapter.notifyDataSetChanged();
            mModel.hasData = changes != null && !changes.isEmpty();
            mBinding.setModel(mModel);
        }

        @Override
        public Object resubscriptionTag() {
            return getClass().getSimpleName();
        }
    };

    private GroupLifecycleManager mGroupLifecycleManager;
    private ObservableGroup mObservableGroup;

    private ChangesFragmentBinding mBinding;
    private final Model mModel = new Model();

    private ChangesAdapter mAdapter;

    public static ChangesFragment newInstance(String filter) {
        ChangesFragment fragment = new ChangesFragment();
        Bundle arguments = new Bundle();
        arguments.putString("filter", filter);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(
                inflater, R.layout.changes_fragment, container, false);
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
        mAdapter = new ChangesAdapter(this);
        mBinding.list.setLayoutManager(new LinearLayoutManager(
                getActivity(), LinearLayoutManager.VERTICAL, false));
        mBinding.list.addItemDecoration(new DividerItemDecoration(
                getContext(), LinearLayoutManager.VERTICAL));
        mBinding.list.setAdapter(mAdapter);

        // Configure RxGroups to manage rxjava fragment lifecycle
        ObservableManager manager =
                ((RviewApplication) getActivity().getApplication()).observableManager();
        mGroupLifecycleManager = GroupLifecycleManager.onCreate(manager,
                savedInstanceState, this);
        mObservableGroup = mGroupLifecycleManager.group();
        mGroupLifecycleManager.onResume();
        fetchChanges();
    }

    @Override
    public final void onDestroyView() {
        super.onDestroyView();
        mBinding.unbind();
        mGroupLifecycleManager.onPause();
        mGroupLifecycleManager.onDestroy(getActivity());
    }

    @SuppressWarnings("ConstantConditions")
    private void fetchChanges() {
        final Context ctx = getActivity().getApplicationContext();
        Account account = Preferences.getAccount(ctx);
        GerritApi api = GerritServiceFactory.getInstance(ctx, account.mRepository.mUrl);
ChangeQuery query = new ChangeQuery().status(StatusType.OPEN);
        Observable<List<ChangeInfo>> call = api.getChanges(query, 50, 0, new ChangeOptions[]{ChangeOptions.DETAILED_ACCOUNTS});
        call.subscribeOn(Schedulers.io())
                .compose(mObservableGroup.<List<ChangeInfo>>transform(getClass().getSimpleName()))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mChangesObserver);
    }

    private void onItemClick(ChangeInfo item) {

    }
}
