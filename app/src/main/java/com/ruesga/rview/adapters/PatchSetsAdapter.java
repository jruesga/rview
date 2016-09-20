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
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.ruesga.rview.databinding.PatchSetDropdownItemBinding;
import com.ruesga.rview.gerrit.model.RevisionInfo;

import java.util.List;

public class PatchSetsAdapter extends BaseAdapter {

    private Context mContext;
    private List<RevisionInfo> mRevisions;
    private String mCurrentRevision;

    public PatchSetsAdapter(Context context, List<RevisionInfo> revisions, String currentRevision) {
        mContext = context;
        mRevisions = revisions;
        mCurrentRevision = currentRevision;
    }

    @Override
    public int getCount() {
        return mRevisions.size();
    }

    @Override
    public RevisionInfo getItem(int position) {
        return mRevisions.get(position);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).number;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        if (v == null) {
            PatchSetDropdownItemBinding binding = PatchSetDropdownItemBinding.inflate(
                    LayoutInflater.from(mContext), parent, false);
            v = binding.getRoot();
            v.setTag(binding);
        }

        RevisionInfo revision = mRevisions.get(position);
        PatchSetDropdownItemBinding binding = (PatchSetDropdownItemBinding) v.getTag();
        binding.setModel(revision);
        binding.setIsSelected(revision.commit.commit.equals(mCurrentRevision));
        binding.executePendingBindings();

        return v;
    }

    public int measureContentWidth() {
        int maxWidth = 0;
        View itemView = null;
        int itemType = 0;

        final int widthMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        final int heightMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        final int count = getCount();
        for (int i = 0; i < count; i++) {
            final int positionType = getItemViewType(i);
            if (positionType != itemType) {
                itemType = positionType;
                itemView = null;
            }

            itemView = getView(i, itemView, null);
            itemView.measure(widthMeasureSpec, heightMeasureSpec);

            final int itemWidth = itemView.getMeasuredWidth();

            if (itemWidth > maxWidth) {
                maxWidth = itemWidth;
            }
        }

        return maxWidth;
    }
}
