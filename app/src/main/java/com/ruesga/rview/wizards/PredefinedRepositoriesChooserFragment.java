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
package com.ruesga.rview.wizards;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import com.ruesga.rview.R;
import com.ruesga.rview.misc.ModelHelper;
import com.ruesga.rview.model.Repository;
import com.ruesga.rview.wizard.choosers.ListChooserFragment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class PredefinedRepositoriesChooserFragment extends ListChooserFragment {

    public static final String EXTRA_REPOSITORY = "repository";

    public PredefinedRepositoriesChooserFragment() {
        super();
    }

    @Override
    public @StringRes int getTitle() {
        return R.string.account_wizard_repositories_chooser_title;
    }

    @NonNull
    public Callable<List<ItemModel>> getDataProducer() {
        return this::getRepositoriesAsModel;
    }

    @NonNull
    @Override
    public Intent toResult(ItemModel item) {
        Intent i = new Intent();
        i.putExtra(EXTRA_REPOSITORY, new Repository(
                item.title, item.summary, item.trustAllCertificates));
        return i;
    }

    private List<ItemModel> getRepositoriesAsModel() {
        List<Repository> repositories = ModelHelper.getPredefinedRepositories(getContext());
        ArrayList<ItemModel> itemModels = new ArrayList<>(repositories.size());
        for (Repository repo : repositories) {
            ItemModel item = new ItemModel();
            item.title = repo.mName;
            item.summary = repo.mUrl;
            item.trustAllCertificates = repo.mTrustAllCertificates;
            itemModels.add(item);
        }
        return itemModels;
    }
}
