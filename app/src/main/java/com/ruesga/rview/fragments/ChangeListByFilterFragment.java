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
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.ruesga.rview.BaseActivity;
import com.ruesga.rview.R;
import com.ruesga.rview.gerrit.GerritApi;
import com.ruesga.rview.gerrit.filter.ChangeQuery;
import com.ruesga.rview.gerrit.model.ChangeInfo;
import com.ruesga.rview.gerrit.model.ChangeInput;
import com.ruesga.rview.gerrit.model.ChangeOptions;
import com.ruesga.rview.gerrit.model.InitialChangeStatus;
import com.ruesga.rview.misc.ActivityHelper;
import com.ruesga.rview.misc.ModelHelper;
import com.ruesga.rview.preferences.Preferences;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import me.tatarka.rxloader2.RxLoader1;
import me.tatarka.rxloader2.RxLoaderManager;
import me.tatarka.rxloader2.RxLoaderObserver;
import me.tatarka.rxloader2.safe.SafeObservable;

public class ChangeListByFilterFragment extends ChangeListFragment
        implements NewChangeDialogFragment.OnNewChangeRequestedListener {

    private static final List<ChangeOptions> OPTIONS = new ArrayList<ChangeOptions>() {{
        add(ChangeOptions.DETAILED_ACCOUNTS);
        add(ChangeOptions.LABELS);
        add(ChangeOptions.REVIEWED);
    }};

    private final RxLoaderObserver<ChangeInfo> mNewChangeObserver =
        new RxLoaderObserver<ChangeInfo>() {
            @Override
            public void onNext(ChangeInfo change) {
                mNewChangeLoader.clear();
                showProgress(false);

                ActivityHelper.openChangeDetails(getContext(), change, true, false);
            }

            @Override
            public void onError(Throwable error) {
                mNewChangeLoader.clear();
                handleException(error);
                showProgress(false);
            }

            @Override
            public void onStarted() {
                showProgress(true);
            }
        };

    private static final String EXTRA_FILTER = "filter";
    private static final String EXTRA_HAS_SEARCH = "hasSearch";
    private static final String EXTRA_HAS_FAB = "hasFab";

    private RxLoader1<ChangeInput, ChangeInfo> mNewChangeLoader;

    public static ChangeListByFilterFragment newInstance(String filter) {
        return newInstance(filter, false, false);
    }

    public static ChangeListByFilterFragment newInstance(
            String filter, boolean hasSearch, boolean hasFab) {
        ChangeListByFilterFragment fragment = new ChangeListByFilterFragment();
        Bundle arguments = new Bundle();
        arguments.putString(EXTRA_FILTER, filter);
        arguments.putBoolean(EXTRA_HAS_SEARCH, hasSearch);
        arguments.putBoolean(EXTRA_HAS_FAB, hasFab);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(getArguments().getBoolean(EXTRA_HAS_SEARCH, false));
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.search, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_search) {
            // Show search fragment
            ActivityHelper.openSearchActivity(getActivity());
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    void setupLoaders(RxLoaderManager loaderManager) {
        mNewChangeLoader = loaderManager.create("new_change", this::performCreateNewChange, mNewChangeObserver);
    }

    @Override
    BaseActivity.OnFabPressedListener getFabPressedListener() {
        if (!getArguments().getBoolean(EXTRA_HAS_FAB, false)) {
            return null;
        }

        return fab -> {
            NewChangeDialogFragment fragment = NewChangeDialogFragment.newInstance(0, fab);
            fragment.show(getChildFragmentManager(), NewChangeDialogFragment.TAG);
        };
    }

    @SuppressWarnings("ConstantConditions")
    public Observable<List<ChangeInfo>> fetchChanges(Integer count, Integer start) {
        final ChangeQuery query = ChangeQuery.parse(getArguments().getString(EXTRA_FILTER));
        final Context ctx = getActivity();
        final GerritApi api = ModelHelper.getGerritApi(ctx);
        return Observable.zip(
                Observable.just(getCurrentData(start <= 0)),
                api.getChanges(query, count, Math.max(0, start), OPTIONS),
                Observable.just(count),
                this::combineChanges
            )
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread());
    }

    @Override
    public void fetchNewItems() {
        final int count = Preferences.getAccountFetchedItems(
                getContext(), Preferences.getAccount(getContext()));
        final int start = 0;
        getChangesLoader().clear();
        getChangesLoader().restart(count, start);
    }

    @Override
    public void fetchMoreItems() {
        // Fetch more
        final int itemsToFetch = Preferences.getAccountFetchedItems(
                getContext(), Preferences.getAccount(getContext()));
        final int count = itemsToFetch + FETCHED_MORE_CHANGES_THRESHOLD;
        final int start = getCurrentData(false).size() - FETCHED_MORE_CHANGES_THRESHOLD;
        getChangesLoader().clear();
        getChangesLoader().restart(count, start);
    }

    @Override
    public void onNewChangeRequested(
            int requestCode, String project, String branch, String topic, String subject) {
        ChangeInput input = new ChangeInput();
        input.project = project;
        input.branch = branch;
        input.topic = topic;
        input.subject = subject;
        input.status = InitialChangeStatus.DRAFT;

        mNewChangeLoader.clear();
        mNewChangeLoader.restart(input);
    }

    @SuppressWarnings("ConstantConditions")
    private Observable<ChangeInfo> performCreateNewChange(final ChangeInput input) {
        final Context ctx = getActivity();
        final GerritApi api = ModelHelper.getGerritApi(ctx);
        return SafeObservable.fromNullCallable(() -> api.createChange(input).blockingFirst())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }
}
