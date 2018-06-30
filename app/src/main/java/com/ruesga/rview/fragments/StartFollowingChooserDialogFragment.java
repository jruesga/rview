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
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ruesga.rview.R;
import com.ruesga.rview.databinding.StartFollowingChooserItemBinding;
import com.ruesga.rview.gerrit.GerritApi;
import com.ruesga.rview.gerrit.filter.Option;
import com.ruesga.rview.gerrit.model.AccountInfo;
import com.ruesga.rview.misc.ModelHelper;
import com.ruesga.rview.misc.RviewImageHelper;
import com.ruesga.rview.model.Account;
import com.ruesga.rview.preferences.Preferences;

import java.util.ArrayList;
import java.util.List;

public class StartFollowingChooserDialogFragment extends ListDialogFragment<AccountInfo, Void> {

    public static final String TAG = "StartFollowDialogFrag";

    @Keep
    public static class EventHandlers {
        private StartFollowingChooserDialogFragment mFragment;

        public EventHandlers(StartFollowingChooserDialogFragment fragment) {
            mFragment = fragment;
        }

        public void onItemPressed(View v) {
            AccountInfo acct = (AccountInfo) v.getTag();
            mFragment.performFollowPressed(acct);
        }
    }

    private static class StartFollowingViewHolder extends RecyclerView.ViewHolder {
        private final StartFollowingChooserItemBinding mBinding;
        private StartFollowingViewHolder(StartFollowingChooserItemBinding binding) {
            super(binding.getRoot());
            mBinding = binding;
            binding.executePendingBindings();
        }
    }

    private static class StartFollowingAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final List<AccountInfo> mFollowing = new ArrayList<>();
        private final EventHandlers mEventHandlers;
        private final Context mContext;

        private StartFollowingAdapter(StartFollowingChooserDialogFragment fragment) {
            setHasStableIds(true);
            mEventHandlers = new EventHandlers(fragment);
            mContext = fragment.getContext();
        }

        public void addAll(List<AccountInfo> files) {
            mFollowing.clear();
            mFollowing.addAll(files);
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            return new StartFollowingViewHolder(DataBindingUtil.inflate(
                    inflater, R.layout.start_following_chooser_item, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            AccountInfo model = mFollowing.get(position);

            StartFollowingViewHolder itemViewHolder = (StartFollowingViewHolder) holder;
            itemViewHolder.mBinding.setModel(model);
            itemViewHolder.mBinding.setHandlers(mEventHandlers);
            RviewImageHelper.bindAvatar(mContext, model, itemViewHolder.mBinding.avatar,
                    RviewImageHelper.getDefaultAvatar(mContext, R.color.primaryDarkForeground));
        }

        @Override
        public long getItemId(int position) {
            return mFollowing.get(position).accountId;
        }

        @Override
        public int getItemCount() {
            return mFollowing.size();
        }
    }


    public static StartFollowingChooserDialogFragment newInstance() {
        return new StartFollowingChooserDialogFragment();
    }

    private StartFollowingAdapter mAdapter;

    public StartFollowingChooserDialogFragment() {
    }

    @Override
    public RecyclerView.Adapter<RecyclerView.ViewHolder> getAdapter() {
        if (mAdapter == null) {
            mAdapter = new StartFollowingAdapter(this);
            mAdapter.addAll(new ArrayList<>());
            refresh();
        }
        return mAdapter;
    }

    @Override
    public int getTitle() {
        return R.string.start_following_title;
    }

    @Override
    public boolean hasLoading() {
        return true;
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public List<AccountInfo> onFilterChanged(String newFilter) {
        if (getActivity() == null) {
            return new ArrayList<>();
        }
        final GerritApi api = ModelHelper.getGerritApi(getContext());
        final List<AccountInfo> filtered;
        if (newFilter.isEmpty()) {
            filtered = new ArrayList<>();
        } else {
            filtered = api.getAccountsSuggestions(
                    newFilter, 10, Option.INSTANCE).blockingFirst();
        }
        return filtered;
    }

    @Override
    public boolean onDataRefreshed(List<AccountInfo> data) {
        mAdapter.addAll(data);
        mAdapter.notifyDataSetChanged();
        return data.isEmpty();
    }

    private void performFollowPressed(AccountInfo acct) {
        Account account = Preferences.getAccount(getContext());
        Preferences.setAccountFollowingState(getContext(), account, acct, true);
        dismiss();

        final Fragment f = getParentFragment();
        if (f instanceof FollowingChangeListFragment) {
            ((FollowingChangeListFragment) f).onFollowingDialogDismissed();
        }
    }
}
