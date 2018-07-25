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

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ruesga.rview.R;
import com.ruesga.rview.databinding.ProjectDetailsViewBinding;
import com.ruesga.rview.gerrit.GerritApi;
import com.ruesga.rview.gerrit.filter.ChangeQuery;
import com.ruesga.rview.gerrit.filter.TimeUnit;
import com.ruesga.rview.gerrit.model.AccountInfo;
import com.ruesga.rview.gerrit.model.ChangeInfo;
import com.ruesga.rview.gerrit.model.DashboardInfo;
import com.ruesga.rview.gerrit.model.ProjectInfo;
import com.ruesga.rview.misc.ActivityHelper;
import com.ruesga.rview.misc.ModelHelper;
import com.ruesga.rview.misc.SerializationManager;
import com.ruesga.rview.preferences.Constants;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import me.tatarka.rxloader2.safe.SafeObservable;

public class ProjectStatsPageFragment
        extends StatsPageFragment<ProjectStatsPageFragment.ProjectStatsInfo> {

    private static final String TAG = "ProjectStatsPageFrgm";

    private ProjectDetailsViewBinding mBinding;
    private String mProjectName;

    static class ProjectStatsInfo {
        private ProjectInfo mProjectInfo;
        private List<DashboardInfo> mDashboards;
    }

    public static ProjectStatsPageFragment newFragment(String projectName) {
        ProjectStatsPageFragment fragment = new ProjectStatsPageFragment();
        Bundle arguments = new Bundle();
        arguments.putString(Constants.EXTRA_ID, projectName);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //noinspection ConstantConditions
        mProjectName = getArguments().getString(Constants.EXTRA_ID);
    }

    @Override
    public View inflateDetails(LayoutInflater inflater, @Nullable ViewGroup container) {
        mBinding = DataBindingUtil.inflate(
                inflater, R.layout.project_details_view, container, false);
        return mBinding.getRoot();
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public Observable<ProjectStatsInfo> fetchDetails() {
        final GerritApi api = ModelHelper.getGerritApi(getContext());
        return SafeObservable.fromNullCallable(() -> {
            ProjectStatsInfo stats = new ProjectStatsInfo();
            stats.mProjectInfo = api.getProject(mProjectName).blockingFirst();
            stats.mDashboards = fetchDashboards(api, stats.mProjectInfo);
            return stats;
        });
    }

    private List<DashboardInfo> fetchDashboards(GerritApi api, ProjectInfo project) {
        List<DashboardInfo> dashboards = new ArrayList<>(
                api.getProjectDashboards(project.name).blockingFirst());
        if (!TextUtils.isEmpty(project.parent)) {
            try {
                dashboards.addAll(fetchDashboards(
                        api, api.getProject(project.parent).blockingFirst()));
            } catch (Exception e) {
                Log.w(TAG, "Cannot fetch '" + project.parent + "'project dashboards", e);
                // Stop fetching here
            }
        }
        return dashboards;

    }

    @Override
    public ChangeQuery getStatsQuery() {
        return new ChangeQuery().project(mProjectName).and(
                new ChangeQuery().negate(new ChangeQuery().age(TimeUnit.DAYS, getMaxDays())));
    }

    @Override
    public void bindDetails(ProjectStatsInfo result) {
        mBinding.setModel(result.mProjectInfo);
        mBinding.dashboards
                .listenOn(dashboard -> ActivityHelper.openDashboardActivity(
                        getContext(), dashboard, false))
                .from(result.mProjectInfo, result.mDashboards);
        mBinding.executePendingBindings();

    }

    @Override
    public String getStatsFragmentTag() {
        return TAG;
    }

    @Override
    public String getDescription(ChangeInfo change) {
        return change.project;
    }

    @Override
    public String getCrossDescription(ChangeInfo change) {
        return ModelHelper.formatAccountWithEmail(change.owner);
    }

    @Override
    public String getSerializedCrossItem(ChangeInfo change) {
        return SerializationManager.getInstance().toJson(change.owner);
    }

    @Override
    public void openCrossItem(String item) {
        AccountInfo account = SerializationManager.getInstance().fromJson(item, AccountInfo.class);
        String title = getString(R.string.account_details);
        String id = String.valueOf(account.accountId);
        String displayName = ModelHelper.getAccountDisplayName(account);
        ChangeQuery filter = new ChangeQuery().owner(id);
        ActivityHelper.openStatsActivity(
                getContext(), title, displayName, StatsFragment.ACCOUNT_STATS, id, filter, item);
    }
}
