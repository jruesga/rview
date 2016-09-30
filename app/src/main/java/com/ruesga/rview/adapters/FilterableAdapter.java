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
import android.databinding.DataBindingUtil;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;

import com.ruesga.rview.R;
import com.ruesga.rview.databinding.DropdownItemBinding;

import java.util.ArrayList;
import java.util.List;

public abstract class FilterableAdapter extends android.widget.BaseAdapter implements Filterable {

    private static final int MAX_RESULTS = 5;

    private Context mContext;
    private List<CharSequence> mResults;
    private List<CharSequence> mFilteredResults = new ArrayList<>();
    private final ResultFilter mFilter = new ResultFilter();

    public FilterableAdapter(Context context) {
        mContext = context;
    }

    public Context getContext() {
        return mContext;
    }

    @Override
    public int getCount() {
        return mFilteredResults.size();
    }

    @Override
    public CharSequence getItem(int position) {
        return mFilteredResults.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
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

        CharSequence result = mFilteredResults.get(position);
        DropdownItemBinding binding = (DropdownItemBinding) v.getTag();
        binding.item.setText(result);
        binding.executePendingBindings();

        return v;
    }

    @Override
    public Filter getFilter() {
        return mFilter;
    }

    public abstract List<CharSequence> getResults();


    private class ResultFilter extends Filter {

        private final List<CharSequence> mTemp = new ArrayList<>();

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults results = new FilterResults();
            if (constraint != null) {
                // Fetch if needed
                if (mResults == null) {
                    mResults = getResults();
                }

                // Filter results
                mTemp.clear();
                if (mResults != null) {
                    for (CharSequence v : mResults) {
                        if (v.toString().contains(constraint)) {
                            mTemp.add(v);
                        }
                        if (mTemp.size() >= MAX_RESULTS) {
                            break;
                        }
                    }
                }

                results.values = mTemp;
                results.count = mTemp.size();
            }
            return results;
        }

        @Override
        @SuppressWarnings("unchecked")
        protected void publishResults(CharSequence constraint, FilterResults results) {
            mFilteredResults.clear();
            if (results.count > 0) {
                mFilteredResults.addAll((List<CharSequence>) results.values);
                notifyDataSetChanged();
            } else {
                notifyDataSetInvalidated();
            }
        }
    }

}
