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
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ruesga.rview.BaseActivity;
import com.ruesga.rview.OnChangeItemPressedListener;
import com.ruesga.rview.R;
import com.ruesga.rview.annotations.ProguardIgnored;
import com.ruesga.rview.databinding.ChangesFragmentBinding;
import com.ruesga.rview.databinding.ChangesItemBinding;
import com.ruesga.rview.databinding.FetchingMoreItemBinding;
import com.ruesga.rview.gerrit.GerritApi;
import com.ruesga.rview.gerrit.filter.ChangeQuery;
import com.ruesga.rview.gerrit.model.ChangeInfo;
import com.ruesga.rview.gerrit.model.ChangeOptions;
import com.ruesga.rview.misc.ModelHelper;
import com.ruesga.rview.misc.PicassoHelper;
import com.ruesga.rview.widget.DividerItemDecoration;
import com.ruesga.rview.widget.EndlessRecyclerViewScrollListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import me.tatarka.rxloader.RxLoader2;
import me.tatarka.rxloader.RxLoaderManager;
import me.tatarka.rxloader.RxLoaderManagerCompat;
import me.tatarka.rxloader.RxLoaderObserver;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func2;
import rx.schedulers.Schedulers;

public class ChangeListFragment extends Fragment {

    private static final String TAG = "ChangeListFragment";

    private static final ChangeOptions[] OPTIONS = {
            ChangeOptions.DETAILED_ACCOUNTS, ChangeOptions.LABELS};

    private static final int FETCHED_CHANGES = 50;
    private static final int FETCHED_MORE_CHANGES_THRESHOLD = 10;

    private static final int MESSAGE_FETCH_MORE_ITEMS = 0;

    public static class ItemViewHolder extends RecyclerView.ViewHolder {
        private final ChangesItemBinding mBinding;
        public ItemViewHolder(ChangesItemBinding binding) {
            super(binding.getRoot());
            mBinding = binding;
            binding.executePendingBindings();
        }
    }

    public static class FetchingMoreViewHolder extends RecyclerView.ViewHolder {
        public FetchingMoreViewHolder(FetchingMoreItemBinding binding) {
            super(binding.getRoot());
            binding.executePendingBindings();
        }
    }

    @ProguardIgnored
    @SuppressWarnings("unused")
    public static class ItemEventHandlers {
        ChangeListFragment mFragment;

        public ItemEventHandlers(ChangeListFragment fragment) {
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

    private static class ChangesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int FETCHING_MODE_ITEM_VIEW = 0;
        private static final int CHANGE_ITEM_VIEW = 1;

        final List<ChangeInfo> mData = new ArrayList<>();
        private final ItemEventHandlers mHandlers;
        private final Picasso mPicasso;
        private final Context mContext;

        public ChangesAdapter(ChangeListFragment fragment) {
            setHasStableIds(true);
            mHandlers = new ItemEventHandlers(fragment);
            mPicasso = PicassoHelper.getPicassoClient(fragment.getContext());
            mContext = fragment.getContext();
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
                itemViewHolder.itemView.setTag(item);
                itemViewHolder.mBinding.scores.setScores(item.labels);
                itemViewHolder.mBinding.setModel(item);
                itemViewHolder.mBinding.setHandlers(mHandlers);
                PicassoHelper.bindAvatar(mContext, mPicasso, item.owner,
                        itemViewHolder.mBinding.avatar,
                        PicassoHelper.getDefaultAvatar(mContext, R.color.primaryDark));
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
            mModel.hasData = result != null && !result.isEmpty();
            mBinding.setModel(mModel);
            showProgress(false);
        }

        @Override
        public void onError(Throwable error) {
            // Hide your progress indicator and show that there was an error.
            mModel.hasData = mAdapter.mData.size() == 0;
            mBinding.setModel(mModel);
            ((BaseActivity) getActivity()).handleException(TAG, error);
            showProgress(false);
        }

        @Override
        public void onStarted() {
            showProgress(true);
        }
    };

    private final Handler.Callback mUiMessenger = message -> {
        switch (message.what) {
            case MESSAGE_FETCH_MORE_ITEMS:
                fetchMoreItems();
                return true;
        }
        return false;
    };




    private static final String EXTRA_FILTER = "filter";

    private Handler mUiHandler;
    private ChangesFragmentBinding mBinding;
    private final Model mModel = new Model();

    private ChangesAdapter mAdapter;
    private EndlessRecyclerViewScrollListener mEndlessScroller;

    private RxLoader2<Integer, Integer, List<ChangeInfo>> mChangesLoader;

    public static ChangeListFragment newInstance(String filter) {
        ChangeListFragment fragment = new ChangeListFragment();
        Bundle arguments = new Bundle();
        arguments.putString("filter", filter);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUiHandler = new Handler(mUiMessenger);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(
                inflater, R.layout.changes_fragment, container, false);
        mBinding.setModel(mModel);
        mBinding.refresh.setEnabled(false);
        return mBinding.getRoot();
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
        mChangesLoader = loaderManager.create(
                new Func2<Integer, Integer, Observable<List<ChangeInfo>>>() {
                    @Override
                    public Observable<List<ChangeInfo>> call(
                            final Integer count, final Integer start) {
                        return fetchChanges(count, start);
                    }
                },
                mLoaderObserver)
                .start(FETCHED_CHANGES, 0);
    }

    @Override
    public final void onDestroyView() {
        super.onDestroyView();
        mBinding.unbind();
    }

    @SuppressWarnings("ConstantConditions")
    private Observable<List<ChangeInfo>> fetchChanges(Integer count, Integer start) {
        final ChangeQuery query = ChangeQuery.parse(getArguments().getString(EXTRA_FILTER));
        final Context ctx = getActivity();
        final GerritApi api = ModelHelper.getGerritApi(ctx);
        return Observable.zip(
                Observable.just(Collections.unmodifiableList(mAdapter.mData)),
                api.getChanges(query, count, start, OPTIONS),
                Observable.just(count),
                this::combineChanges
            )
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread());
    }

    private void fetchNewItems() {
        // Fetch new
        mEndlessScroller.loadCompleted();
        mBinding.list.removeOnScrollListener(mEndlessScroller);
        mBinding.list.addOnScrollListener(mEndlessScroller);

        final int count = FETCHED_CHANGES;
        final int start = 0;
        mChangesLoader.restart(count, start);
    }

    private void fetchMoreItems() {
        // Add the fetching more waiting view
        mAdapter.add(new ChangeInfo());
        mAdapter.notifyItemInserted(mAdapter.mData.size() - 1);

        // Fetch more
        final int count = FETCHED_CHANGES + FETCHED_MORE_CHANGES_THRESHOLD;
        final int start = mAdapter.mData.size() - FETCHED_MORE_CHANGES_THRESHOLD;
        mChangesLoader.restart(count, start);
    }

    private List<ChangeInfo> combineChanges(
            List<ChangeInfo> oldChanges, List<ChangeInfo> newChanges, Integer count) {
        // Check if we end fetching changes
        if (newChanges.size() < count) {
            mBinding.list.removeOnScrollListener(mEndlessScroller);
        }

        for (ChangeInfo oldChange : oldChanges) {
            if (oldChange.id == null) {
                continue;
            }

            boolean exists = false;
            for (ChangeInfo newChange : newChanges) {
                if (newChange.id.equals(oldChange.id)) {
                    exists = true;
                    break;
                }
            }

            if (!exists) {
                newChanges.add(oldChange);
            }
        }

        // Sort the collection
        Collections.sort(newChanges, (o1, o2) -> {
            if (o1.id.equals(o2.id)) {
                return 0;
            }
            return o2.updated.compareTo(o1.updated);
        });

        return newChanges;
    }

    private void setupSwipeToRefresh() {
        mBinding.refresh.setColorSchemeColors(
                ContextCompat.getColor(getContext(), R.color.accent));
        mBinding.refresh.setOnRefreshListener(() -> {
            mBinding.refresh.setRefreshing(false);
            fetchNewItems();
        });
        mBinding.refresh.setEnabled(false);
    }

    private void showProgress(boolean show) {
        if (mEndlessScroller == null || !mEndlessScroller.isLoading()) {
            ((BaseActivity) getActivity()).changeInProgressStatus(show);
        } else if (!show) {
            mEndlessScroller.loadCompleted();
        }
        mBinding.refresh.setEnabled(!show);
    }

    private void onItemClick(ChangeInfo item) {
        ((OnChangeItemPressedListener) getActivity()).onChangeItemPressed(item);
    }
}
