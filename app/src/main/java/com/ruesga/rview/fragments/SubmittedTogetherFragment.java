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

import com.ruesga.rview.gerrit.GerritApi;
import com.ruesga.rview.gerrit.model.ChangeInfo;
import com.ruesga.rview.gerrit.model.ChangeOptions;
import com.ruesga.rview.gerrit.model.ServerVersion;
import com.ruesga.rview.misc.ModelHelper;
import com.ruesga.rview.preferences.Constants;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class SubmittedTogetherFragment extends ChangeListFragment {

    private static final List<ChangeOptions> OPTIONS = new ArrayList<ChangeOptions>() {{
        add(ChangeOptions.DETAILED_ACCOUNTS);
        add(ChangeOptions.LABELS);
        add(ChangeOptions.REVIEWED);
    }};

    public static SubmittedTogetherFragment newInstance(int legacyChangeId) {
        SubmittedTogetherFragment fragment = new SubmittedTogetherFragment();
        Bundle arguments = new Bundle();
        arguments.putString(Constants.EXTRA_LEGACY_CHANGE_ID, String.valueOf(legacyChangeId));
        fragment.setArguments(arguments);
        return fragment;
    }

    @SuppressWarnings("ConstantConditions")
    public Observable<List<ChangeInfo>> fetchChanges(Integer count, Integer start) {
        final String legacyChangeId = getArguments().getString(Constants.EXTRA_LEGACY_CHANGE_ID);
        final Context ctx = getActivity();
        final GerritApi api = ModelHelper.getGerritApi(ctx);
        return Observable.zip(
                Observable.just(getCurrentData(start == 0)),
                Observable.fromCallable(() -> {
                    ServerVersion version = api.getServerVersion().blockingFirst();
                    if (version.getVersion() >= 2.12) {
                        return fetchChanges(api,
                                api.getChangesSubmittedTogether(
                                    legacyChangeId, null).blockingFirst());
                    }
                    return new ArrayList<>();
                }),
                Observable.just(0),
                this::combineChanges
            )
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread());
    }

    @Override
    public void fetchNewItems() {
        getChangesLoader().clear();
        getChangesLoader().restart(getItemsToFetch(), 0);
    }

    @Override
    public void fetchMoreItems() {
        // This is not used here
    }

    @Override
    boolean hasMoreItems(int size, int expected) {
        return false;
    }

    @SuppressWarnings("Convert2streamapi")
    private List<ChangeInfo> fetchChanges(GerritApi api, List<ChangeInfo> changes) {
        List<ChangeInfo> c = new ArrayList<>(changes.size());
        for (ChangeInfo change : changes) {
            c.add(api.getChange(String.valueOf(change.legacyChangeId), OPTIONS).blockingFirst());
        }
        return c;
    }

}
