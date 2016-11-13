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
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;

import com.ruesga.rview.R;
import com.ruesga.rview.annotations.ProguardIgnored;
import com.ruesga.rview.databinding.TagEditDialogBinding;
import com.ruesga.rview.widget.TagEditTextView.Tag;

import java.util.ArrayList;

public class TagEditDialogFragment extends RevealDialogFragment {

    public static final String TAG = "TagEditDialogFragment";

    private static final String EXTRA_TITLE = "title";
    private static final String EXTRA_TAGS = "tags";
    private static final String EXTRA_ACTION = "action";

    @ProguardIgnored
    public static class Model {
        public String subtitle;
        private ArrayList<Tag> tags = new ArrayList<>();
        public String hint;
    }

    public interface OnEditChanged {
        void onEditChanged(Tag[] newTags);
    }


    public static TagEditDialogFragment newInstance(
            String title, Tag[] tags, String action, View anchor) {
        TagEditDialogFragment fragment = new TagEditDialogFragment();
        Bundle arguments = new Bundle();
        arguments.putString(EXTRA_TITLE, title);
        ArrayList<String> serializedTags = new ArrayList<>(tags.length);
        for (Tag tag : tags) {
            serializedTags.add(tag.toPlainTag().toString());
        }
        arguments.putStringArrayList(EXTRA_TAGS, serializedTags);
        arguments.putString(EXTRA_ACTION, action);
        arguments.putParcelable(EXTRA_ANCHOR, computeViewOnScreen(anchor));
        fragment.setArguments(arguments);
        return fragment;
    }

    private TagEditDialogBinding mBinding;
    private final Model mModel = new Model();
    private OnEditChanged mCallback;

    public TagEditDialogFragment() {
    }

    public void setOnEditChanged(OnEditChanged cb) {
        mCallback = cb;
    }

    @Override
    public void buildDialog(AlertDialog.Builder builder, Bundle savedInstanceState) {
        String title = getArguments().getString(EXTRA_TITLE);
        String action = getArguments().getString(EXTRA_ACTION);
        if (TextUtils.isEmpty(action)) {
            action = getString(R.string.action_save);
        }

        LayoutInflater inflater = LayoutInflater.from(builder.getContext());
        mBinding = DataBindingUtil.inflate(inflater, R.layout.tag_edit_dialog, null, true);
        mBinding.tagsEditor.setTags(mModel.tags.toArray(new Tag[mModel.tags.size()]));
        mBinding.setModel(mModel);

        builder.setTitle(title)
                .setView(mBinding.getRoot())
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(action, (dialog, which) -> performEditChanged());
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ArrayList<String> tags = getArguments().getStringArrayList(EXTRA_TAGS);
        mModel.tags.clear();
        if (tags != null) {
            for (String tag : tags) {
                Tag t = new Tag();
                t.mTag = "#" + tag;
                mModel.tags.add(t);
            }
        }
    }

    @Override
    public void onDialogReveled() {
        mBinding.tagsEditor.clearFocus();
        mBinding.tagsEditor.requestFocus();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mBinding.unbind();
    }

    private void performEditChanged() {
        mBinding.tagsEditor.computeTags(null);
        if (mCallback != null) {
            mCallback.onEditChanged(mBinding.tagsEditor.getTags());
        }
    }
}
