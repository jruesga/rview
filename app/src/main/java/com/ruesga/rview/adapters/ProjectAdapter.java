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
import com.ruesga.rview.gerrit.model.ProjectType;
import com.ruesga.rview.misc.ModelHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ProjectAdapter extends FilterableAdapter {

    private static final int MAX_SUGGESTIONS = 5;

    private final String mProjectId;

    public ProjectAdapter(Context context) {
        this(context, null);
    }

    public ProjectAdapter(Context context, String projectId) {
        super(context);
        mProjectId = projectId;
    }

    @Override
    @SuppressWarnings({"ConstantConditions", "Convert2streamapi"})
    public List<CharSequence> getResults(CharSequence constraint) {
        String filter = constraint.toString();
        final GerritApi api = ModelHelper.getGerritApi(getContext());
        Set<String> projects = api.getProjects(MAX_SUGGESTIONS, null, null, null, filter,
                null, null, null, ProjectType.ALL, null).blockingFirst().keySet();
        List<CharSequence> results = new ArrayList<>(projects.size());
        for (String project : projects) {
            if (mProjectId == null || !mProjectId.equals(project)) {
                results.add(project);
            }
        }
        return results;
    }
}
