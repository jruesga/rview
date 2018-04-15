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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.google.gson.reflect.TypeToken;
import com.ruesga.rview.R;
import com.ruesga.rview.databinding.PluginItemBinding;
import com.ruesga.rview.databinding.ServerInfoDialogBinding;
import com.ruesga.rview.gerrit.GerritApi;
import com.ruesga.rview.gerrit.filter.Option;
import com.ruesga.rview.gerrit.model.PluginInfo;
import com.ruesga.rview.gerrit.model.ServerInfo;
import com.ruesga.rview.gerrit.model.ServerVersion;
import com.ruesga.rview.misc.CacheHelper;
import com.ruesga.rview.misc.ModelHelper;
import com.ruesga.rview.misc.SerializationManager;
import com.ruesga.rview.model.Account;
import com.ruesga.rview.preferences.Preferences;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import me.tatarka.rxloader2.RxLoader;
import me.tatarka.rxloader2.RxLoaderManager;
import me.tatarka.rxloader2.RxLoaderManagerCompat;
import me.tatarka.rxloader2.RxLoaderObserver;
import me.tatarka.rxloader2.safe.SafeObservable;

public class ServerInfoDialogFragment extends RevealDialogFragment {

    public static final String TAG = "ServerInfoDlgFragment";

    private final RxLoaderObserver<ServerVersion> mRequestServerVersionObserver
            = new RxLoaderObserver<ServerVersion>() {
        @Override
        public void onNext(ServerVersion version) {
            if (mBinding != null) {
                mBinding.setVersion(version);
            }
        }

        @Override
        public void onError(Throwable e) {
            if (mBinding != null) {
                mBinding.setVersion(new ServerVersion(""));
            }
        }
    };

    private final RxLoaderObserver<ServerInfo> mRequestServerInfoObserver
            = new RxLoaderObserver<ServerInfo>() {
        @Override
        public void onNext(ServerInfo serverInfo) {
            if (mBinding != null) {
                mBinding.setServerInfo(serverInfo);
            }
        }

        @Override
        public void onError(Throwable e) {
            if (mBinding != null) {
                mBinding.setServerInfo(new ServerInfo());
            }
        }
    };

    private final RxLoaderObserver<List<PluginInfo>> mRequestPluginsObserver
            = new RxLoaderObserver<List<PluginInfo>>() {
        @Override
        public void onNext(List<PluginInfo> plugins) {
            if (mBinding != null) {
                mBinding.setFetching(false);
                mBinding.setHasPlugins(!plugins.isEmpty());

                mAdapter.clear();
                mAdapter.addAll(plugins);
                mAdapter.notifyDataSetChanged();
            }
        }

        @Override
        public void onError(Throwable e) {
            if (mBinding != null) {
                mBinding.setFetching(false);
                mBinding.setHasPlugins(false);
            }
        }
    };

    private static class PluginViewHolder extends RecyclerView.ViewHolder {
        private final PluginItemBinding mBinding;
        private PluginViewHolder(PluginItemBinding binding) {
            super(binding.getRoot());
            mBinding = binding;
            binding.executePendingBindings();
        }
    }

    private static class PluginsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        final List<PluginInfo> mPlugins = new ArrayList<>();

        private void clear() {
            mPlugins.clear();
        }

        private void addAll(List<PluginInfo> plugins) {
            mPlugins.addAll(plugins);
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            return new PluginViewHolder(DataBindingUtil.inflate(
                    inflater, R.layout.plugin_item, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            PluginInfo item = mPlugins.get(position);
            PluginViewHolder itemViewHolder = (PluginViewHolder) holder;
            itemViewHolder.mBinding.setEven(position % 2 == 0);
            itemViewHolder.mBinding.setModel(item);
        }

        @Override
        public int getItemCount() {
            return mPlugins.size();
        }
    }


    private ServerInfoDialogBinding mBinding;
    private PluginsAdapter mAdapter;
    private String[] wellKnownPluginsIds;
    private Account mAccount;

    public ServerInfoDialogFragment() {
    }

    public static ServerInfoDialogFragment newInstance() {
        return new ServerInfoDialogFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public final void onDestroyView() {
        super.onDestroyView();
        mBinding.unbind();
    }

    @Override
    public void buildDialog(AlertDialog.Builder builder, Bundle savedInstanceState) {
        LayoutInflater inflater = LayoutInflater.from(builder.getContext());
        mBinding = DataBindingUtil.inflate(inflater, R.layout.server_info_dialog, null, true);
        mBinding.setFetching(true);
        mBinding.setHasPlugins(false);

        mAdapter = new PluginsAdapter();
        mBinding.plugins.setNestedScrollingEnabled(true);
        mBinding.plugins.setLayoutManager(new LinearLayoutManager(
                getActivity(), LinearLayoutManager.VERTICAL, false));
        mBinding.plugins.setAdapter(mAdapter);


        wellKnownPluginsIds = builder.getContext().getResources().getStringArray(
                R.array.well_known_plugins_ids);

        builder.setTitle(R.string.server_info_dialog_title)
                .setView(mBinding.getRoot())
                .setPositiveButton(R.string.action_close, null);
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        RxLoaderManager loaderManager = RxLoaderManagerCompat.get(this);
        RxLoader<ServerVersion> requestServerVersionLoader = loaderManager.create(
                "request_server_version", doRequestServerVersion(), mRequestServerVersionObserver);
        RxLoader<ServerInfo> requestServerInfoLoader = loaderManager.create(
                "request_server_info", doRequestServerInfo(), mRequestServerInfoObserver);
        RxLoader<List<PluginInfo>> requestPluginsLoader = loaderManager.create(
                "request_plugins", doRequestPlugins(), mRequestPluginsObserver);

        requestServerVersionLoader.start();
        requestServerInfoLoader.start();
        requestPluginsLoader.start();

        mAccount = Preferences.getAccount(getActivity());
        mBinding.setRepository(mAccount.mRepository);
    }

    @SuppressWarnings("ConstantConditions")
    private Observable<ServerVersion> doRequestServerVersion() {
        final Context ctx = getActivity();
        final GerritApi api = ModelHelper.getGerritApi(ctx);
        return SafeObservable.fromNullCallable(
                        () -> api.getServerVersion().blockingFirst())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    @SuppressWarnings("ConstantConditions")
    private Observable<ServerInfo> doRequestServerInfo() {
        final Context ctx = getActivity();
        final GerritApi api = ModelHelper.getGerritApi(ctx);
        return SafeObservable.fromNullCallable(
                        () -> api.getServerInfo().blockingFirst())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    @SuppressWarnings("ConstantConditions")
    private Observable<List<PluginInfo>> doRequestPlugins() {
        final Context ctx = getActivity();
        final GerritApi api = ModelHelper.getGerritApi(ctx);
        return SafeObservable.fromNullCallable(() -> {
                    List<PluginInfo> plugins = new ArrayList<>();
                    try {
                        // Try to fetch plugins all at once call (only available for
                        // administrators)
                        plugins.addAll(fetchPlugins(api));
                    } catch (Exception ex) {
                        // Fetch individual plugins (well-known plugin ids)
                        plugins.addAll(fetchPluginsById(ctx, api));
                    }
                    Collections.sort(plugins, (o1, o2) -> o1.id.compareTo(o2.id));
                    return plugins;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    private Collection<PluginInfo> fetchPlugins(GerritApi api) {
        return api.getPlugins(
                Option.INSTANCE, null, null, null, null, null)
                .blockingFirst().values();
    }

    @SuppressWarnings("ConstantConditions")
    private Collection<PluginInfo> fetchPluginsById(Context ctx, GerritApi api) {
        List<PluginInfo> plugins = null;
        long age = CacheHelper.getFileCacheAge(ctx, mAccount, CacheHelper.CACHE_PLUGINS_JSON);
        if (System.currentTimeMillis() - age < DateUtils.DAY_IN_MILLIS) {
            try {
                Type type = new TypeToken<List<PluginInfo>>() {}.getType();
                plugins = SerializationManager.getInstance().fromJson(
                        new String(CacheHelper.readFileCache(
                                ctx, mAccount, CacheHelper.CACHE_PLUGINS_JSON)), type);
            } catch (Exception ex) {
                Log.e(TAG, "Failed to read plugins cache file", ex);
            }
        }

        if (plugins == null) {
            // Fetch plugins
            plugins = new ArrayList<>();
            for (String pluginId : wellKnownPluginsIds) {
                try {
                    plugins.add(api.getPluginStatus(pluginId).blockingFirst());
                } catch (Exception ex) {
                    // ignore
                }
            }
        }

        try {
            CacheHelper.writeFileCache(ctx, mAccount, CacheHelper.CACHE_PLUGINS_JSON,
                    SerializationManager.getInstance().toJson(plugins).getBytes());
        } catch (Exception ex) {
            Log.e(TAG, "Failed to serialize plugins cache file", ex);
        }
        return plugins;
    }
}
