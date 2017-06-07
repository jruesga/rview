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
import android.support.annotation.Keep;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;

import com.google.gson.Gson;
import com.ruesga.rview.R;
import com.ruesga.rview.databinding.FileDetailsDialogBinding;
import com.ruesga.rview.gerrit.model.FileInfo;
import com.ruesga.rview.misc.SerializationManager;
import com.ruesga.rview.misc.StringHelper;
import com.ruesga.rview.preferences.Constants;

import java.io.File;

public class FileDetailsDialogFragment extends RevealDialogFragment {

    public static final String TAG = "FileDetailsDialog";

    @Keep
    public static class Model {
        public String name;
        public String path;
        public String type;
        public long size;
        public FileInfo info;
    }

    public static FileDetailsDialogFragment newInstance(File file, long size, FileInfo details) {
        FileDetailsDialogFragment fragment = new FileDetailsDialogFragment();
        Bundle arguments = new Bundle();
        Gson gson = SerializationManager.getInstance();
        arguments.putString(Constants.EXTRA_FILE, file.getPath());
        arguments.putLong(Constants.EXTRA_SIZE, size);
        if (details != null) {
            arguments.putString(Constants.EXTRA_DATA, gson.toJson(details));
        }
        fragment.setArguments(arguments);
        return fragment;
    }



    private final Model mModel = new Model();
    private FileDetailsDialogBinding mBinding;

    @Override
    @SuppressWarnings("ConstantConditions")
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Gson gson = SerializationManager.getInstance();
        File file = new File(getArguments().getString(Constants.EXTRA_FILE));
        mModel.name = file.getName();
        mModel.path = file.getParent() == null ? "" : file.getParent();
        if (!mModel.path.endsWith("/")) {
            mModel.path += "/";
        }
        mModel.type = StringHelper.getMimeType(file);
        mModel.size = getArguments().getLong(Constants.EXTRA_SIZE);
        String json = getArguments().getString(Constants.EXTRA_DATA);
        if (json != null) {
            mModel.info = gson.fromJson(json, FileInfo.class);
            if (mModel.info != null) {
                mModel.size = mModel.info.size;
            }
        }
    }

    @Override
    public final void onDestroyView() {
        super.onDestroyView();
        mBinding.unbind();
    }

    @Override
    public void buildDialog(AlertDialog.Builder builder, Bundle savedInstanceState) {
        LayoutInflater inflater = LayoutInflater.from(builder.getContext());
        mBinding = DataBindingUtil.inflate(inflater, R.layout.file_details_dialog, null, true);
        mBinding.setModel(mModel);

        builder.setTitle(R.string.file_details_dialog_title)
                .setView(mBinding.getRoot())
                .setPositiveButton(R.string.action_close, null);
    }

}
