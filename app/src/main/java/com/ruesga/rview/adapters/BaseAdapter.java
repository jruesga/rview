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
package com.ruesga.rview.adapters;

import android.content.Context;

import com.ruesga.rview.gerrit.GerritApi;
import com.ruesga.rview.gerrit.filter.ChangeQuery;
import com.ruesga.rview.gerrit.filter.IsType;
import com.ruesga.rview.gerrit.filter.TimeUnit;
import com.ruesga.rview.gerrit.model.ChangeInfo;
import com.ruesga.rview.misc.ModelHelper;

import java.util.ArrayList;
import java.util.List;

public class BaseAdapter extends FilterableAdapter {

    private final int mLegacyChangeId;
    private final String mProjectId;
    private final String mBranch;

    public BaseAdapter(Context context, int legacyChangeId, String projectId, String branch) {
        super(context);
        mLegacyChangeId = legacyChangeId;
        mProjectId = projectId;
        mBranch = branch;
    }

    @Override
    @SuppressWarnings({"ConstantConditions", "Convert2streamapi"})
    public List<CharSequence> getResults() {
        final GerritApi api = ModelHelper.getGerritApi(getContext());
        ChangeQuery query = new ChangeQuery().project(mProjectId)
                .and(new ChangeQuery().branch(mBranch))
                .and(new ChangeQuery().is(IsType.OPEN))
                .negate(new ChangeQuery().age(TimeUnit.DAYS, 90));
        List<ChangeInfo> changes = api.getChanges(query, 1000, 0, null).blockingFirst();
        List<CharSequence> results = new ArrayList<>(changes.size());
        for (ChangeInfo change : changes) {
            // Exclude current base
            if (mLegacyChangeId != change.legacyChangeId) {
                results.add(change.legacyChangeId + ": " + change.subject);
            }
        }
        return results;
    }

}
