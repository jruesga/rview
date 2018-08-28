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

import com.ruesga.rview.R;
import com.ruesga.rview.databinding.DropdownItemBinding;

import java.util.List;

import androidx.databinding.DataBindingUtil;

public class SimpleDropDownAdapter<T> extends BaseAdapter {

    private Context mContext;
    private List<String> mValues;
    private List<String> mLabels;
    private int[] mIcons;
    private T[] mIds;
    private String mValue;

    public SimpleDropDownAdapter(Context context, List<String> values,
            List<String> labels, String value) {
        this(context, values, labels, null, value);
    }

    public SimpleDropDownAdapter(Context context, List<String> values,
            List<String> labels, int[] icons, String value) {
        this(context, values, labels, icons, null, value);
    }

    public SimpleDropDownAdapter(Context context, List<String> values, List<String> labels,
            int[] icons, T[] ids, String value) {
        mContext = context;
        mValues = values;
        mLabels = labels;
        mIcons = icons;
        mValue = value;
        mIds = ids;
    }

    @Override
    public int getCount() {
        return mValues.size();
    }

    @Override
    public String getItem(int position) {
        return mValues.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public T getId(int position) {
        return mIds[position];
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        if (v == null) {
            DropdownItemBinding binding = DataBindingUtil.inflate(
                    LayoutInflater.from(mContext), R.layout.dropdown_item, parent, false);
            v = binding.getRoot();
            v.setTag(binding);
        }

        String value= mValues.get(position);
        String label = mLabels.get(position);
        Integer icon = null;
        if (mIcons != null) {
            icon = mIcons[position];
        }
        DropdownItemBinding binding = (DropdownItemBinding) v.getTag();
        binding.setText(label);
        binding.setIcon(icon);
        binding.setMaxLines(1);
        binding.setIsSelected(value.equals(mValue));
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
