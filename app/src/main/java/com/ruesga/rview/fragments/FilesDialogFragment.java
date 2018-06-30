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

import android.app.Activity;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.gson.reflect.TypeToken;
import com.ruesga.rview.R;
import com.ruesga.rview.databinding.FileInfoItemBinding;
import com.ruesga.rview.fragments.ChangeDetailsFragment.FileItemModel;
import com.ruesga.rview.misc.FowlerNollVo;
import com.ruesga.rview.misc.SerializationManager;
import com.ruesga.rview.preferences.Constants;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FilesDialogFragment extends ListDialogFragment<FileItemModel, Void> {

    public static final String TAG = "FilesDialogFragment";

    private static final String EXTRA_SHORT_FILE_NAMES = "is_short_file_names";

    @Keep
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
        private final List<FileItemModel> mFiles = new ArrayList<>();
        private final boolean mIsShortFilenames;
        private final FilesDialogFragment.EventHandlers mEventHandlers;

        private FilesAdapter(FilesDialogFragment fragment, boolean shortFileNames) {
            setHasStableIds(true);
            mIsShortFilenames = shortFileNames;
            mEventHandlers = new EventHandlers(fragment);
        }

        public void addAll(List<FileItemModel> files) {
            mFiles.clear();
            mFiles.addAll(files);
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            return new FileViewHolder(DataBindingUtil.inflate(
                    inflater, R.layout.file_info_item, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            FileItemModel model = mFiles.get(position);

            FileViewHolder itemViewHolder = (FileViewHolder) holder;
            itemViewHolder.mBinding.addedVsDeleted.with(model);
            itemViewHolder.mBinding.setIsShortFileName(mIsShortFilenames);
            itemViewHolder.mBinding.setModel(model);
            itemViewHolder.mBinding.setHandlers(mEventHandlers);
        }

        @Override
        public long getItemId(int position) {
            return FowlerNollVo.fnv1_64(mFiles.get(position).file.getBytes()).longValue();
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

    private FilesAdapter mAdapter;
    private List<FileItemModel> mFiles;

    public FilesDialogFragment() {
    }

    @Override
    public RecyclerView.Adapter<RecyclerView.ViewHolder> getAdapter() {
        if (mAdapter == null) {
            Type type = new TypeToken<List<FileItemModel>>() {}.getType();
            //noinspection ConstantConditions
            mFiles = SerializationManager.getInstance().fromJson(
                    getArguments().getString(Constants.EXTRA_FILES), type);
            mAdapter = new FilesAdapter(this, getArguments().getBoolean(EXTRA_SHORT_FILE_NAMES));
            mAdapter.addAll(mFiles);
        }
        return mAdapter;
    }

    @Override
    public int getTitle() {
        return R.string.change_details_header_files;
    }

    @Override
    public List<FileItemModel> onFilterChanged(String newFilter) {
        List<FileItemModel> filteredFiles = new ArrayList<>();
        for (FileItemModel item : mFiles) {
            if (item.file.toLowerCase(Locale.US).contains(newFilter)) {
                filteredFiles.add(item);
            }
        }
        return filteredFiles;
    }

    @Override
    public boolean onDataRefreshed(List<FileItemModel> data) {
        mAdapter.addAll(data);
        mAdapter.notifyDataSetChanged();
        return data.isEmpty();
    }

    private void performFilePressed(String file) {
        dismiss();

        final Activity a = getActivity();
        final Fragment f = getParentFragment();
        if (f instanceof OnFilePressed) {
            ((OnFilePressed) f).onFilePressed(file);
        } else if (a instanceof OnFilePressed) {
            ((OnFilePressed) a).onFilePressed(file);
        }
    }
}
