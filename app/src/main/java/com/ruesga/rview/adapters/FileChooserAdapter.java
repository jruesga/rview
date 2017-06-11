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
import com.ruesga.rview.gerrit.model.FileInfo;
import com.ruesga.rview.misc.ModelHelper;
import com.ruesga.rview.preferences.Constants;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FileChooserAdapter extends FilterableAdapter {

    private final String mLegacyChangeId;
    private final String mRevisionId;

    public FileChooserAdapter(Context context, String legacyChangeId, String revisionId) {
        super(context);
        mLegacyChangeId = legacyChangeId;
        mRevisionId = revisionId;
    }

    @Override
    @SuppressWarnings({"ConstantConditions"})
    public List<CharSequence> getResults(CharSequence constraint) {
        final GerritApi api = ModelHelper.getGerritApi(getContext());
        List<String> files =
                api.getChangeRevisionFilesSuggestion(mLegacyChangeId, mRevisionId, null, null,
                        constraint.toString()).blockingFirst();
        List<CharSequence> results = new ArrayList<>();
        for (String file : files) {
            if (file.equals(Constants.COMMIT_MESSAGE)) {
                continue;
            }
            results.add(file);
        }
        return results;
    }

    @Override
    public boolean needsConstraintForQuery() {
        return true;
    }
}
