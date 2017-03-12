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
import android.database.ContentObserver;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Keep;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.ruesga.rview.BaseActivity;
import com.ruesga.rview.R;
import com.ruesga.rview.databinding.NotificationItemBinding;
import com.ruesga.rview.databinding.NotificationsFragmentBinding;
import com.ruesga.rview.gerrit.filter.ChangeQuery;
import com.ruesga.rview.gerrit.model.AccountInfo;
import com.ruesga.rview.misc.ActivityHelper;
import com.ruesga.rview.misc.ExceptionHelper;
import com.ruesga.rview.misc.ModelHelper;
import com.ruesga.rview.misc.NotificationsHelper;
import com.ruesga.rview.misc.PicassoHelper;
import com.ruesga.rview.misc.SerializationManager;
import com.ruesga.rview.model.Account;
import com.ruesga.rview.model.EmptyState;
import com.ruesga.rview.providers.NotificationEntity;
import com.ruesga.rview.widget.DividerItemDecoration;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import me.tatarka.rxloader2.RxLoader;
import me.tatarka.rxloader2.RxLoaderManager;
import me.tatarka.rxloader2.RxLoaderManagerCompat;
import me.tatarka.rxloader2.RxLoaderObserver;
import me.tatarka.rxloader2.safe.SafeObservable;

import static com.ruesga.rview.preferences.Constants.EXTRA_ACCOUNT_HASH;

public class NotificationsFragment extends Fragment {

    private static final String TAG = "NotificationsFragment";

    public static NotificationsFragment newInstance(Account account) {
        NotificationsFragment fragment = new NotificationsFragment();
        Bundle arguments = new Bundle();
        arguments.putString(EXTRA_ACCOUNT_HASH, account.getAccountHash());
        fragment.setArguments(arguments);
        return fragment;
    }

    public static class NotificationViewHolder extends RecyclerView.ViewHolder {
        private final NotificationItemBinding mBinding;
        private NotificationViewHolder(NotificationItemBinding binding) {
            super(binding.getRoot());
            mBinding = binding;
            binding.executePendingBindings();
        }
    }

    @Keep
    public static class Model {
        public CharSequence subject;
        public CharSequence notification;
        public AccountInfo who;
        public Date when;
        public boolean read;
    }

    @Keep
    @SuppressWarnings("unused")
    public static class ItemEventHandlers {
        NotificationsFragment mFragment;

        ItemEventHandlers(NotificationsFragment fragment) {
            mFragment = fragment;
        }

        public void onItemPressed(View view) {
            NotificationEntity item = (NotificationEntity) view.getTag();
            mFragment.onItemClick(item);
        }

        public void onAvatarPressed(View view) {
            AccountInfo account = (AccountInfo) view.getTag();
            mFragment.onAvatarClick(account);
        }
    }

    private static class NotificationsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        final List<NotificationEntity> mNotifications = new ArrayList<>();
        private final ItemEventHandlers mHandlers;
        private final Picasso mPicasso;
        private final Context mContext;

        private NotificationsAdapter(NotificationsFragment fragment) {
            setHasStableIds(true);
            mContext = fragment.getContext();
            mHandlers = new ItemEventHandlers(fragment);
            mPicasso = PicassoHelper.getPicassoClient(mContext);
        }

        private void clear() {
            mNotifications.clear();
        }

        private void addAll(List<NotificationEntity> notifications) {
            mNotifications.addAll(notifications);
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            return new NotificationViewHolder(DataBindingUtil.inflate(
                            inflater, R.layout.notification_item, parent, false));
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            NotificationEntity item = mNotifications.get(position);
            NotificationViewHolder itemViewHolder = (NotificationViewHolder) holder;
            itemViewHolder.mBinding.item.setTag(item);

            Model model = new Model();
            model.subject = item.mNotification.subject;
            model.when = new Date(item.mNotification.when);
            model.read = item.mRead;
            model.who = item.mNotification.who;
            model.notification = NotificationsHelper.getContentMessage(mContext, item, true, false);
            itemViewHolder.mBinding.setModel(model);
            itemViewHolder.mBinding.setHandlers(mHandlers);
            PicassoHelper.bindAvatar(mContext, mPicasso, model.who,
                    itemViewHolder.mBinding.avatar,
                    PicassoHelper.getDefaultAvatar(mContext, R.color.primaryDark));
        }

        @Override
        public long getItemId(int position) {
            return mNotifications.get(position).mMessageId;
        }

        @Override
        public int getItemCount() {
            return mNotifications.size();
        }

        private boolean hasUnreadNotifications() {
            for (NotificationEntity notification : Collections.unmodifiableList(mNotifications)) {
                if (!notification.mRead) {
                    return true;
                }
            }
            return false;
        }
    }

    private final RxLoaderObserver<List<NotificationEntity>> mNotificationsObserver
            = new RxLoaderObserver<List<NotificationEntity>>() {
        @Override
        public void onNext(List<NotificationEntity> result) {
            mAdapter.clear();
            mAdapter.addAll(result);
            mAdapter.notifyDataSetChanged();
            mEmptyState.state = result != null && !result.isEmpty()
                    ? EmptyState.NORMAL_STATE : EmptyState.EMPTY_STATE;
            mBinding.setEmpty(mEmptyState);

            if (getActivity() != null) {
                getActivity().invalidateOptionsMenu();
            }
        }

        @Override
        public void onError(Throwable error) {
            mEmptyState.state = ExceptionHelper.resolveEmptyState(error);
            mBinding.setEmpty(mEmptyState);

            mAdapter.clear();
            mNotificationsLoader.clear();
            ((BaseActivity) getActivity()).handleException(TAG, error, null);

            if (getActivity() != null) {
                getActivity().invalidateOptionsMenu();
            }
        }
    };

    private class NotificationsObserver extends ContentObserver {
        private NotificationsObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange, null);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            forceRefresh();
        }
    }

    private NotificationsFragmentBinding mBinding;
    private EmptyState mEmptyState = new EmptyState();

    private RxLoader<List<NotificationEntity>> mNotificationsLoader;

    private NotificationsAdapter mAdapter;
    private NotificationsObserver mObserver;
    private Handler mUiHandler;
    private Account mAccount;

    private boolean mMenuInflated = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUiHandler = new Handler();
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(
                inflater, R.layout.notifications_fragment, container, false);
        mBinding.setEmpty(mEmptyState);
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

        if (mAdapter == null) {
            mAdapter = new NotificationsAdapter(this);
            mBinding.list.setLayoutManager(new LinearLayoutManager(
                    getActivity(), LinearLayoutManager.VERTICAL, false));
            mBinding.list.addItemDecoration(new DividerItemDecoration(
                    getContext(), LinearLayoutManager.VERTICAL));
            mBinding.list.setAdapter(mAdapter);

            mAccount = ModelHelper.getAccountFromHash(
                    getContext(), getArguments().getString(EXTRA_ACCOUNT_HASH));

            // Configure the refresh
            setupSwipeToRefresh();

            mObserver = new NotificationsObserver(mUiHandler);
            getActivity().getContentResolver().registerContentObserver(
                            NotificationEntity.CONTENT_URI, true, mObserver);

            // Fetch or join current loader
            RxLoaderManager loaderManager = RxLoaderManagerCompat.get(this);
            mNotificationsLoader = loaderManager.create(
                    fetchNotifications(), mNotificationsObserver);
            mNotificationsLoader.start();
        }
    }

    @Override
    public final void onDestroyView() {
        super.onDestroyView();
        mBinding.unbind();

        if (getActivity() != null && mObserver != null) {
            getActivity().getContentResolver().unregisterContentObserver(mObserver);
        }
    }

    private void setupSwipeToRefresh() {
        mBinding.refresh.setColorSchemeColors(
                ContextCompat.getColor(getContext(), R.color.accent));
        mBinding.refresh.setOnRefreshListener(() -> {
            mBinding.refresh.setRefreshing(false);

            // Reload
            forceRefresh();
        });
    }

    private void onItemClick(NotificationEntity notification) {
        ActivityHelper.openChangeDetails(
                getContext(),
                notification.mNotification.change,
                notification.mNotification.legacyChangeId,
                true,
                true);
    }

    private void onAvatarClick(AccountInfo account) {
        ChangeQuery filter = new ChangeQuery().owner(ModelHelper.getSafeAccountOwner(account));
        String title = getString(R.string.account_details);
        String displayName = ModelHelper.getAccountDisplayName(account);
        String extra = SerializationManager.getInstance().toJson(account);
        String accountId = account.accountId != 0
                ? String.valueOf(account.accountId) : account.username;
        ActivityHelper.openStatsActivity(getContext(), title, displayName,
                StatsFragment.ACCOUNT_STATS, accountId, filter, extra);
    }

    private void forceRefresh() {
        mNotificationsLoader.clear();
        mNotificationsLoader.restart();
    }

    private Observable<List<NotificationEntity>> fetchNotifications() {
        final Context ctx = getContext().getApplicationContext();
        return SafeObservable.fromNullCallable(() -> {
                    List<NotificationEntity> notifications =
                            NotificationEntity.getAllAccountNotifications(
                                ctx, mAccount.getAccountHash(), false, false);
                    Collections.sort(notifications, (o1, o2) ->
                            Long.valueOf(o1.mWhen).compareTo(o2.mWhen) * -1);
                    return notifications;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (!mMenuInflated) {
            inflater.inflate(R.menu.notification_options, menu);
            mMenuInflated = true;
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.menu_mark_as_read).setVisible(
                mAdapter != null && mAdapter.hasUnreadNotifications());
        menu.findItem(R.id.menu_delete_all).setVisible(
                mAdapter != null && mAdapter.getItemCount() > 0);
        super.onPrepareOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_mark_as_read:
                performMarkAsReadAccountNotifications();
                return true;
            case R.id.menu_delete_all:
                performDeleteAccountNotifications();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void performMarkAsReadAccountNotifications() {
        NotificationEntity.markAccountNotificationsAsRead(getContext(), mAccount.getAccountHash());
        NotificationEntity.dismissAccountNotifications(getContext(), mAccount.getAccountHash());
        getActivity().invalidateOptionsMenu();
    }

    private void performDeleteAccountNotifications() {
        NotificationEntity.deleteAccountNotifications(getContext(), mAccount.getAccountHash());
        NotificationEntity.dismissAccountNotifications(getContext(), mAccount.getAccountHash());
        getActivity().invalidateOptionsMenu();
    }
}
