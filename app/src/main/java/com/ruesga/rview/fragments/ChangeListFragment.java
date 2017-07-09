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
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Keep;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ruesga.rview.BaseActivity;
import com.ruesga.rview.OnChangeItemListener;
import com.ruesga.rview.R;
import com.ruesga.rview.databinding.ChangesFragmentBinding;
import com.ruesga.rview.databinding.ChangesItemBinding;
import com.ruesga.rview.databinding.FetchingMoreItemBinding;
import com.ruesga.rview.gerrit.filter.ChangeQuery;
import com.ruesga.rview.gerrit.model.AccountInfo;
import com.ruesga.rview.gerrit.model.ChangeInfo;
import com.ruesga.rview.misc.ActivityHelper;
import com.ruesga.rview.misc.ExceptionHelper;
import com.ruesga.rview.misc.ModelHelper;
import com.ruesga.rview.misc.PicassoHelper;
import com.ruesga.rview.misc.SerializationManager;
import com.ruesga.rview.model.EmptyState;
import com.ruesga.rview.preferences.Preferences;
import com.ruesga.rview.widget.DividerItemDecoration;
import com.ruesga.rview.widget.EndlessRecyclerViewScrollListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.reactivex.Observable;
import me.tatarka.rxloader2.RxLoader2;
import me.tatarka.rxloader2.RxLoaderManager;
import me.tatarka.rxloader2.RxLoaderManagerCompat;
import me.tatarka.rxloader2.RxLoaderObserver;

public abstract class ChangeListFragment extends Fragment implements SelectableFragment {

    private static final String TAG = "ChangeListFragment";

    public static final int NO_SELECTION = -1;

    public static final int FETCHED_MORE_CHANGES_THRESHOLD = 10;

    private static final int MESSAGE_FETCH_MORE_ITEMS = 0;

    private static final String EXTRA_CHANGE_ID = "changeId";

    public static class ItemViewHolder extends RecyclerView.ViewHolder {
        private final ChangesItemBinding mBinding;
        ItemViewHolder(ChangesItemBinding binding) {
            super(binding.getRoot());
            mBinding = binding;
            binding.executePendingBindings();
        }
    }

    public static class FetchingMoreViewHolder extends RecyclerView.ViewHolder {
        FetchingMoreViewHolder(FetchingMoreItemBinding binding) {
            super(binding.getRoot());
            binding.executePendingBindings();
        }
    }

    @Keep
    @SuppressWarnings("unused")
    public static class ItemEventHandlers {
        ChangeListFragment mFragment;

        ItemEventHandlers(ChangeListFragment fragment) {
            mFragment = fragment;
        }

        public void onItemPressed(View view) {
            ChangeInfo item = (ChangeInfo) view.getTag();
            mFragment.onItemClick(item);
        }

        public void onAvatarPressed(View view) {
            AccountInfo account = (AccountInfo) view.getTag();
            mFragment.onAvatarClick(account);
        }
    }

    private static class ChangesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int FETCHING_MODE_ITEM_VIEW = 0;
        private static final int CHANGE_ITEM_VIEW = 1;

        final List<ChangeInfo> mData = new ArrayList<>();
        private final ItemEventHandlers mHandlers;
        private final Picasso mPicasso;
        private final Context mContext;

        private int mChangeId = NO_SELECTION;

        ChangesAdapter(ChangeListFragment fragment) {
            setHasStableIds(true);
            mHandlers = new ItemEventHandlers(fragment);
            mContext = fragment.getContext();
            mPicasso = PicassoHelper.getPicassoClient(mContext);
        }

        private void clear() {
            mData.clear();
        }

        private void add(ChangeInfo change) {
            mData.add(change);
        }

        private void addAll(List<ChangeInfo> changes) {
            mData.addAll(changes);
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            switch (viewType) {
                case FETCHING_MODE_ITEM_VIEW:
                    return new FetchingMoreViewHolder(DataBindingUtil.inflate(
                            inflater, R.layout.fetching_more_item, parent, false));
                case CHANGE_ITEM_VIEW:
                    return new ItemViewHolder(DataBindingUtil.inflate(
                            inflater, R.layout.changes_item, parent, false));
            }
            return null;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof ItemViewHolder) {
                ChangeInfo item = mData.get(position);
                ItemViewHolder itemViewHolder = (ItemViewHolder) holder;
                itemViewHolder.itemView.setSelected(mChangeId == item.legacyChangeId);
                itemViewHolder.mBinding.item.setTag(item);
                itemViewHolder.mBinding.scores.setScores(item.labels);
                itemViewHolder.mBinding.setModel(item);
                itemViewHolder.mBinding.setHandlers(mHandlers);
                PicassoHelper.bindAvatar(mContext, mPicasso, item.owner,
                        itemViewHolder.mBinding.avatar,
                        PicassoHelper.getDefaultAvatar(mContext, R.color.primaryDarkForeground));
            }
        }

        @Override
        public long getItemId(int position) {
            if (mData.get(position).id == null) {
                return -1;
            }
            return mData.get(position).id.hashCode();
        }

        @Override
        public int getItemCount() {
            return mData.size();
        }

        @Override
        public int getItemViewType(int position) {
            return mData.get(position).id == null ? FETCHING_MODE_ITEM_VIEW : CHANGE_ITEM_VIEW;
        }
    }

    private final RxLoaderObserver<List<ChangeInfo>> mLoaderObserver =
            new RxLoaderObserver<List<ChangeInfo>>() {
        @Override
        public void onNext(List<ChangeInfo> result) {
            mAdapter.clear();
            mAdapter.addAll(result);
            mAdapter.notifyDataSetChanged();
            mEmptyState.state = result != null && !result.isEmpty()
                    ? EmptyState.NORMAL_STATE : EmptyState.NO_RESULTS_STATE;
            mBinding.setEmpty(mEmptyState);
        }

        @Override
        public void onError(Throwable error) {
            // Hide your progress indicator and show that there was an error.
            mEmptyState.state = ExceptionHelper.resolveEmptyState(error);
            mBinding.setEmpty(mEmptyState);

            mChangesLoader.clear();
            handleException(error);
        }

        @Override
        public void onStarted() {
            showProgress(true);
        }

        @Override
        public void onComplete() {
            showProgress(false);
            ((BaseActivity) getActivity()).setupFab(getFabPressedListener());
        }
    };

    private final Handler.Callback mUiMessenger = message -> {
        switch (message.what) {
            case MESSAGE_FETCH_MORE_ITEMS:
                showMoreView();
                fetchMoreItems();
                return true;
        }
        return false;
    };

    @Keep
    public static class EmptyEventHandlers extends EmptyState.EventHandlers {
        private ChangeListFragment mFragment;

        EmptyEventHandlers(ChangeListFragment fragment) {
            mFragment = fragment;
        }

        public void onRetry(View v) {
            mFragment.getChangesLoader().restart(mFragment.mItemsToFetch, 0);
        }
    }

    private Handler mUiHandler;
    private ChangesFragmentBinding mBinding;
    private final EmptyState mEmptyState = new EmptyState();
    private EmptyEventHandlers mEmptyHandler;
    private boolean mIsTwoPanel;
    private int mItemsToFetch;

    private ChangesAdapter mAdapter;
    private EndlessRecyclerViewScrollListener mEndlessScroller;

    private RxLoader2<Integer, Integer, List<ChangeInfo>> mChangesLoader;

    abstract Observable<List<ChangeInfo>> fetchChanges(Integer count, Integer start);

    abstract void fetchNewItems();

    abstract void fetchMoreItems();

    int getItemsToFetch() {
        return mItemsToFetch;
    }

    boolean hasMoreItems(int size, int expected) {
        return size < expected;
    }

    RxLoader2<Integer, Integer, List<ChangeInfo>> getChangesLoader() {
        return mChangesLoader;
    }

    List<ChangeInfo> getCurrentData(boolean forceRefresh) {
        if (forceRefresh) {
            return Collections.unmodifiableList(new ArrayList<>());
        }
        return Collections.unmodifiableList(mAdapter.mData);
    }

    void notifyNoMoreItems() {
        mBinding.list.removeOnScrollListener(mEndlessScroller);
    }

    BaseActivity.OnFabPressedListener getFabPressedListener() {
        return null;
    }

    List<ChangeInfo> combineChanges(
            List<ChangeInfo> oldChanges, List<ChangeInfo> newChanges, Integer count) {
        // Check if we end fetching changes
        if (hasMoreItems(newChanges.size(), count)) {
            notifyNoMoreItems();
        }

        List<ChangeInfo> combined = new ArrayList<>(oldChanges);
        if (!oldChanges.isEmpty() && oldChanges.get(oldChanges.size() - 1).id == null) {
            combined.remove(oldChanges.size() - 1);
        }
        for (ChangeInfo newChange : newChanges) {
            boolean exists = false;
            for (ChangeInfo change : combined) {
                if (newChange.id.equals(change.id)) {
                    exists = true;
                    break;
                }
            }

            if (!exists) {
                combined.add(newChange);
            }
        }

        return combined;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUiHandler = new Handler(mUiMessenger);
        mEmptyHandler = new EmptyEventHandlers(this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(
                inflater, R.layout.changes_fragment, container, false);
        mBinding.setEmpty(mEmptyState);
        mBinding.setEmptyHandlers(mEmptyHandler);
        startLoadersWithValidContext(savedInstanceState);
        return mBinding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        startLoadersWithValidContext(savedInstanceState);
    }

    private void startLoadersWithValidContext(Bundle savedState) {
        if (getActivity() == null) {
            return;
        }

        mIsTwoPanel = getResources().getBoolean(R.bool.config_is_two_pane);
        if (mAdapter == null) {
            mItemsToFetch = Preferences.getAccountFetchedItems(
                    getContext(), Preferences.getAccount(getContext()));

            // Configure the adapter
            mAdapter = new ChangesAdapter(this);
            if (savedState != null) {
                mAdapter.mChangeId = savedState.getInt(EXTRA_CHANGE_ID, NO_SELECTION);
                if (mAdapter.mChangeId != NO_SELECTION) {
                    notifyItemRestored();
                }
            }

            mBinding.list.setLayoutManager(new LinearLayoutManager(
                    getActivity(), LinearLayoutManager.VERTICAL, false));
            mBinding.list.addItemDecoration(new DividerItemDecoration(
                    getContext(), LinearLayoutManager.VERTICAL));
            mBinding.list.setAdapter(mAdapter);
            mEndlessScroller = new EndlessRecyclerViewScrollListener(
                    mBinding.list.getLayoutManager()) {
                @Override
                public void onLoadMore(int page, int totalItemsCount) {
                    Message.obtain(mUiHandler, MESSAGE_FETCH_MORE_ITEMS).sendToTarget();
                }
            };
            mEndlessScroller.setVisibleThreshold(2);
            mBinding.list.addOnScrollListener(mEndlessScroller);

            // Configure the refresh
            setupSwipeToRefresh();

            // Fetch or join current loader
            RxLoaderManager loaderManager = RxLoaderManagerCompat.get(this);
            setupLoaders(loaderManager);
            mChangesLoader = loaderManager.create(this::fetchChanges, mLoaderObserver);
            mChangesLoader.start(mItemsToFetch, 0);

            if (mIsTwoPanel) {
                // Hide fab until things were loaded (for dual panel layout we must register
                // an scroll listener directly, because CoordinatorLayout can't directly access
                // the floating action button)
                ((BaseActivity) getActivity()).registerFabWithRecyclerView(mBinding.list);
            }
            ((BaseActivity) getActivity()).setupFab(null);
        }
    }

    @Override
    public final void onDestroyView() {
        super.onDestroyView();
        mBinding.unbind();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(EXTRA_CHANGE_ID, mAdapter != null ? mAdapter.mChangeId : NO_SELECTION);
    }

    void setupLoaders(RxLoaderManager loaderManager) {
    }

    private void setupSwipeToRefresh() {
        mBinding.refresh.setColorSchemeColors(
                ContextCompat.getColor(getContext(), R.color.accent));
        mBinding.refresh.setOnRefreshListener(() -> {
            mBinding.refresh.setRefreshing(false);

            // Readd the endless scroll
            mEndlessScroller.loadCompleted();
            mBinding.list.removeOnScrollListener(mEndlessScroller);
            mBinding.list.addOnScrollListener(mEndlessScroller);

            fetchNewItems();
        });
    }

    void showProgress(boolean show) {
        if (mEndlessScroller == null || !mEndlessScroller.isLoading()) {
            BaseActivity activity = (BaseActivity) getActivity();
            if (show) {
                activity.onRefreshStart(this);
            } else {
                activity.onRefreshEnd(this, Collections.unmodifiableList(mAdapter.mData));
            }
        } else if (!show) {
            mEndlessScroller.loadCompleted();
        }
    }

    void handleException(Throwable error) {
        ((BaseActivity) getActivity()).handleException(TAG, error, mEmptyHandler);
    }

    private void onItemClick(ChangeInfo item) {
        if (mIsTwoPanel && mAdapter.mChangeId != item.legacyChangeId) {
            mAdapter.mChangeId = item.legacyChangeId;
            mAdapter.notifyDataSetChanged();
        }
        ((OnChangeItemListener) getActivity()).onChangeItemPressed(item);
    }

    private void onAvatarClick(AccountInfo account) {
        ChangeQuery filter = new ChangeQuery().owner(ModelHelper.getSafeAccountOwner(account));
        String title = getString(R.string.account_details);
        String displayName = ModelHelper.getAccountDisplayName(account);
        String extra = SerializationManager.getInstance().toJson(account);
        ActivityHelper.openStatsActivity(getContext(), title, displayName,
                StatsFragment.ACCOUNT_STATS, String.valueOf(account.accountId), filter, extra);
    }

    private void notifyItemRestored() {
        ((OnChangeItemListener) getActivity()).onChangeItemRestored(mAdapter.mChangeId);
    }

    @Override
    public void onFragmentSelected() {
        ((BaseActivity) getActivity()).setUseTwoPanel(true);
        if (mAdapter == null || mAdapter.mData.isEmpty()) {
            ((OnChangeItemListener) getActivity()).onChangeItemSelected(NO_SELECTION);
        } else {
            int changeId = mAdapter.mData.get(0).legacyChangeId;
            ((OnChangeItemListener) getActivity()).onChangeItemSelected(changeId);
        }
    }

    private void showMoreView() {
        // Add the fetching more waiting view
        mAdapter.add(new ChangeInfo());
        mAdapter.notifyItemInserted(mAdapter.mData.size() - 1);
    }
}
