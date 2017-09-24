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

import android.app.Activity;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.gson.reflect.TypeToken;
import com.ruesga.rview.R;
import com.ruesga.rview.databinding.FileInfoItemBinding;
import com.ruesga.rview.databinding.ListDialogBinding;
import com.ruesga.rview.fragments.ChangeDetailsFragment.FileItemModel;
import com.ruesga.rview.misc.SerializationManager;
import com.ruesga.rview.preferences.Constants;

import java.lang.reflect.Type;
import java.util.List;

public class FilesDialogFragment extends RevealDialogFragment {

    public static final String TAG = "FilesDialogFragment";

    private static final String EXTRA_SHORT_FILE_NAMES = "is_short_file_names";

    @Keep
    @SuppressWarnings({"UnusedParameters", "unused"})
    public static class EventHandlers extends ChangeDetailsFragment.EventHandlers {
        private FilesDialogFragment mFragment;

        public EventHandlers(FilesDialogFragment fragment) {
            super(null);
            mFragment = fragment;
        }

        @Override
        public void onFileItemPressed(View v) {
            String file = (String) v.getTag();
            mFragment.performFilePressed(file);
        }
    }

    public interface OnFilePressed {
        void onFilePressed(String file);
    }

    private static class FileViewHolder extends RecyclerView.ViewHolder {
        private final FileInfoItemBinding mBinding;
        private FileViewHolder(FileInfoItemBinding binding) {
            super(binding.getRoot());
            mBinding = binding;
            binding.executePendingBindings();
        }
    }

    private static class FilesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final List<FileItemModel> mFiles;
        private final boolean mIsShortFilenames;
        private final FilesDialogFragment.EventHandlers mEventHandlers;

        private FilesAdapter(FilesDialogFragment fragment,
                List<FileItemModel> files, boolean shortFileNames) {
            setHasStableIds(true);
            mFiles = files;
            mIsShortFilenames = shortFileNames;
            mEventHandlers = new EventHandlers(fragment);
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            return new FileViewHolder(DataBindingUtil.inflate(
                    inflater, R.layout.file_info_item, parent, false));
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            FileItemModel model = mFiles.get(position);

            FileViewHolder itemViewHolder = (FileViewHolder) holder;
            itemViewHolder.mBinding.addedVsDeleted.with(model);
            itemViewHolder.mBinding.setIsShortFileName(mIsShortFilenames);
            itemViewHolder.mBinding.setModel(model);
            itemViewHolder.mBinding.setHandlers(mEventHandlers);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getItemCount() {
            return mFiles.size();
        }
    }


    public static FilesDialogFragment newInstance(
            @NonNull List<FileItemModel> files, boolean shortFileNames, View anchor) {
        FilesDialogFragment fragment = new FilesDialogFragment();
        Bundle arguments = new Bundle();
        arguments.putString(Constants.EXTRA_FILES, SerializationManager.getInstance().toJson(files));
        arguments.putBoolean(EXTRA_SHORT_FILE_NAMES, shortFileNames);
        arguments.putParcelable(EXTRA_ANCHOR, computeViewOnScreen(anchor));
        fragment.setArguments(arguments);
        return fragment;
    }

    private ListDialogBinding mBinding;

    public FilesDialogFragment() {
    }

    @Override
    public void buildDialog(AlertDialog.Builder builder, Bundle savedInstanceState) {
        LayoutInflater inflater = LayoutInflater.from(builder.getContext());
        mBinding = DataBindingUtil.inflate(inflater, R.layout.list_dialog, null, true);

        Type type = new TypeToken<List<FileItemModel>>(){}.getType();
        FilesAdapter adapter = new FilesAdapter(this,
                SerializationManager.getInstance().fromJson(
                        getArguments().getString(Constants.EXTRA_FILES), type),
                getArguments().getBoolean(EXTRA_SHORT_FILE_NAMES));
        mBinding.list.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        mBinding.list.setAdapter(adapter);

        builder.setTitle(R.string.change_details_header_files)
                .setView(mBinding.getRoot())
                .setNegativeButton(R.string.action_cancel, null);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mBinding.unbind();
    }

    private void performFilePressed(String file) {
        dismiss();

        final Activity a = getActivity();
        final Fragment f = getParentFragment();
        if (f != null && f instanceof OnFilePressed) {
            ((OnFilePressed) f).onFilePressed(file);
        } else if (a != null && a instanceof OnFilePressed) {
            ((OnFilePressed) a).onFilePressed(file);
        }
    }
}
