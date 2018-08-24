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
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ruesga.rview.R;
import com.ruesga.rview.databinding.FollowingChooserItemBinding;
import com.ruesga.rview.gerrit.model.AccountInfo;
import com.ruesga.rview.misc.Formatter;
import com.ruesga.rview.misc.RviewImageHelper;
import com.ruesga.rview.model.Account;
import com.ruesga.rview.preferences.Preferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

public class FollowingChooserDialogFragment extends ListDialogFragment<AccountInfo, Void> {

    public static final String TAG = "FollowDialogFragment";

    @Keep
    public static class EventHandlers {
        private FollowingChooserDialogFragment mFragment;

        public EventHandlers(FollowingChooserDialogFragment fragment) {
            mFragment = fragment;
        }

        public void onUnfollowPressed(View v) {
            AccountInfo acct = (AccountInfo) v.getTag();
            mFragment.performUnfollowPressed(acct);
        }
    }

    private static class FollowingViewHolder extends RecyclerView.ViewHolder {
        private final FollowingChooserItemBinding mBinding;
        private FollowingViewHolder(FollowingChooserItemBinding binding) {
            super(binding.getRoot());
            mBinding = binding;
            binding.executePendingBindings();
        }
    }

    private static class FollowingAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final List<AccountInfo> mFollowing = new ArrayList<>();
        private final FollowingChooserDialogFragment.EventHandlers mEventHandlers;
        private final Context mContext;

        private FollowingAdapter(FollowingChooserDialogFragment fragment) {
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
            return new FollowingViewHolder(DataBindingUtil.inflate(
                    inflater, R.layout.following_chooser_item, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            AccountInfo model = mFollowing.get(position);

            FollowingViewHolder itemViewHolder = (FollowingViewHolder) holder;
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


    public static FollowingChooserDialogFragment newInstance() {
        return new FollowingChooserDialogFragment();
    }

    private FollowingAdapter mAdapter;
    private List<AccountInfo> mFollowing;

    public FollowingChooserDialogFragment() {
    }

    @Override
    public RecyclerView.Adapter<RecyclerView.ViewHolder> getAdapter() {
        if (mAdapter == null) {
            mFollowing = followingAccounts();

            mAdapter = new FollowingAdapter(this);
            mAdapter.addAll(mFollowing);
        }
        return mAdapter;
    }

    @Override
    public int getTitle() {
        return R.string.menu_following;
    }

    @Override
    public List<AccountInfo> onFilterChanged(String newFilter) {
        List<AccountInfo> filtered = new ArrayList<>();
        for (AccountInfo item : mFollowing) {
            String displayName = Formatter.toAccountDisplayName(item);
            if (displayName.toLowerCase(Locale.US).contains(newFilter)) {
                filtered.add(item);
            }
        }
        return filtered;
    }

    @Override
    public boolean onDataRefreshed(List<AccountInfo> data) {
        mAdapter.addAll(data);
        mAdapter.notifyDataSetChanged();
        return data.isEmpty();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);

        final Fragment f = getParentFragment();
        if (f instanceof FollowingChangeListFragment) {
            ((FollowingChangeListFragment) f).onFollowingDialogDismissed();
        }
    }

    private List<AccountInfo> followingAccounts() {
        Account account = Preferences.getAccount(getContext());
        Set<AccountInfo> set = Preferences.getAccountFollowingState(getContext(), account);
        List<AccountInfo> following = new ArrayList<>(set);
        Collections.sort(following, (acct1, acct2) ->
                Formatter.toAccountDisplayName(acct1).compareTo(Formatter.toAccountDisplayName(acct2)));
        return following;
    }

    private void performUnfollowPressed(AccountInfo acct) {
        Account account = Preferences.getAccount(getContext());
        Preferences.setAccountFollowingState(getContext(), account, acct, false);
        mFollowing = followingAccounts();
        refresh();
    }
}
