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

import android.databinding.DataBindingUtil;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ruesga.rview.R;
import com.ruesga.rview.databinding.DashboardChooserItemBinding;
import com.ruesga.rview.gerrit.GerritApi;
import com.ruesga.rview.gerrit.filter.Option;
import com.ruesga.rview.gerrit.model.DashboardInfo;
import com.ruesga.rview.gerrit.model.ProjectInfo;
import com.ruesga.rview.gerrit.model.ProjectType;
import com.ruesga.rview.misc.DashboardHelper;
import com.ruesga.rview.misc.FowlerNollVo;
import com.ruesga.rview.misc.ModelHelper;
import com.ruesga.rview.model.Account;
import com.ruesga.rview.preferences.Preferences;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DashboardChooserDialogFragment
        extends ListDialogFragment<DashboardChooserDialogFragment.Item, DashboardInfo> {

    public static final String TAG = "DashboardChooserDlgFrg";

    @Keep
    public static class Item {
        public String title;
        public String description;
        public Object item;

        public long id() {
            if (item instanceof DashboardInfo) {
                if (((DashboardInfo) item).id == null) {
                    return -2L;
                }

                final String id = "D/" + ((DashboardInfo) item).id;
                return FowlerNollVo.fnv1_64(id.getBytes()).longValue();
            }
            if (item instanceof ProjectInfo) {
                if (((ProjectInfo) item).id == null) {
                    return -3L;
                }

                final String id = "P/" + ((ProjectInfo) item).id;
                return FowlerNollVo.fnv1_64(id.getBytes()).longValue();
            }
            return -1L;
        }

        public boolean isDashboard() {
            return item instanceof DashboardInfo;
        }

        public boolean isProject() {
            return item instanceof ProjectInfo;
        }

        private static Item createItem(DashboardInfo dashboard) {
            Item item = new Item();
            item.item = dashboard;
            item.title = dashboard.title;
            item.description = dashboard.description;
            return item;
        }

        private static Item createItem(ProjectInfo project) {
            Item item = new Item();
            item.item = project;
            item.title = project.id;
            item.description = project.description;
            return item;
        }
    }

    @Keep
    public static class EventHandlers {
        private DashboardChooserDialogFragment mFragment;

        public EventHandlers(DashboardChooserDialogFragment fragment) {
            mFragment = fragment;
        }

        public void onItemPressed(View v) {
            Item item = (Item) v.getTag();
            mFragment.performItemPressed(item);
        }
    }

    private static class DashboardViewHolder extends RecyclerView.ViewHolder {
        private final DashboardChooserItemBinding mBinding;
        private DashboardViewHolder(DashboardChooserItemBinding binding) {
            super(binding.getRoot());
            mBinding = binding;
            binding.executePendingBindings();
        }
    }

    private static class DashboardAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final List<Item> mItems = new ArrayList<>();
        private final EventHandlers mEventHandlers;

        private DashboardAdapter(DashboardChooserDialogFragment fragment) {
            setHasStableIds(true);
            mEventHandlers = new EventHandlers(fragment);
        }

        public void addAll(List<Item> files) {
            mItems.clear();
            mItems.addAll(files);
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            return new DashboardViewHolder(DataBindingUtil.inflate(
                    inflater, R.layout.dashboard_chooser_item, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            Item item = mItems.get(position);

            DashboardViewHolder itemViewHolder = (DashboardViewHolder) holder;
            itemViewHolder.mBinding.setModel(item);
            itemViewHolder.mBinding.setHandlers(mEventHandlers);
        }

        @Override
        public long getItemId(int position) {
            return mItems.get(position).id();
        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }
    }


    public static DashboardChooserDialogFragment newInstance() {
        return new DashboardChooserDialogFragment();
    }

    private DashboardAdapter mAdapter;
    private List<Item> mItems;

    public DashboardChooserDialogFragment() {
    }

    @Override
    public RecyclerView.Adapter<RecyclerView.ViewHolder> getAdapter() {
        if (mAdapter == null) {
            mAdapter = new DashboardAdapter(this);
            List<Item> items = new ArrayList<>();
            if (getActivity() != null) {
                items.add(Item.createItem(DashboardHelper.createDefaultDashboard(getActivity())));
            }
            mAdapter.addAll(items);
            mAdapter.notifyDataSetChanged();
            refresh();
        }
        return mAdapter;
    }

    @Override
    public int getTitle() {
        return R.string.dashboard_chooser_title;
    }

    @Override
    public boolean hasLoading() {
        return true;
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public List<Item> onFilterChanged(String newFilter) {
        if (getActivity() == null) {
            return new ArrayList<>();
        }

        // Fetch dashboards (if needed)
        if (mItems == null) {
            mItems = new ArrayList<>();

            // Create the default one
            mItems.add(Item.createItem(DashboardHelper.createDefaultDashboard(getActivity())));

            // Fetch projects
            final GerritApi api = ModelHelper.getGerritApi(getContext());
            Map<String, ProjectInfo> projects = api.getProjects(null, null, null, null, null,
                    Option.INSTANCE, null, null, ProjectType.ALL, null).blockingFirst();
            for (Map.Entry<String, ProjectInfo> entries : projects.entrySet()) {
                ProjectInfo project = entries.getValue();
                project.id = entries.getKey();

                mItems.add(Item.createItem(project));
            }
        }

        List<Item> items = new ArrayList<>();
        boolean isEmptyFilter = newFilter.isEmpty();
        String lowerCaseFilter = newFilter.toLowerCase();
        for (Item item : mItems) {
            if (isEmptyFilter ||
                    (item.title.toLowerCase().contains(lowerCaseFilter))) {
                items.add(item);
            }
        }
        return items;
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public List<DashboardInfo> onSecondaryDataRequest(Item o) {
        if (o.isProject()) {
            List<DashboardInfo> dashboards = new ArrayList<>();

            // up
            DashboardInfo up = new DashboardInfo();
            up.title = getContext().getString(R.string.dashboard_chooser_up);
            dashboards.add(up);

            // project dashboards
            try {
                final GerritApi api = ModelHelper.getGerritApi(getContext());
                List<DashboardInfo> projectDashboards = api.getProjectDashboards(
                        ((ProjectInfo) o.item).id).blockingFirst();
                if (projectDashboards != null) {
                    dashboards.addAll(projectDashboards);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to fetch project dashboards for " + ((ProjectInfo) o.item).id, e);
            }

            return dashboards;
        }
        return new ArrayList<>();
    }

    @Override
    public boolean onDataRefreshed(List<Item> data) {
        mAdapter.addAll(data);
        mAdapter.notifyDataSetChanged();
        return data.isEmpty();
    }

    @Override
    public RecyclerView.Adapter<RecyclerView.ViewHolder> onSecondaryDataRefreshed(
            List<DashboardInfo> dashboards) {
        DashboardAdapter adapter = new DashboardAdapter(this);
        List<Item> items = new ArrayList<>();
        if (dashboards != null) {
            for (DashboardInfo dashboard : dashboards) {
                items.add(Item.createItem(dashboard));
            }
        }
        adapter.addAll(items);
        return adapter;
    }

    private void performItemPressed(Item item) {
        if (item.isDashboard()) {
            DashboardInfo dashboard = (DashboardInfo) item.item;
            if (dashboard.id == null) {
                hideSecondaryHierarchyPage();
            } else {
                Account account = Preferences.getAccount(getContext());
                Preferences.setAccountDashboard(getContext(), account, dashboard);
                dismiss();

                final Fragment f = getParentFragment();
                if (f instanceof DashboardFragment) {
                    ((DashboardFragment) f).onDashboardChooserDialogDismissed(dashboard);
                }
            }
        } else if (item.isProject()) {
            showSecondaryHierarchyPage(item);
        }
    }
}
