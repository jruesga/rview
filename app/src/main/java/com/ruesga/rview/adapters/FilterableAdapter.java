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
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;

import com.ruesga.rview.R;
import com.ruesga.rview.databinding.DropdownItemBinding;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public abstract class FilterableAdapter extends android.widget.BaseAdapter implements Filterable {

    private static final int MAX_RESULTS = 5;

    private Context mContext;
    private List<CharSequence> mResults;
    private List<CharSequence> mFilteredResults = new ArrayList<>();
    private final ResultFilter mFilter;

    public FilterableAdapter(Context context) {
        mContext = context;
        mFilter = new ResultFilter(this);
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

    public boolean needsConstraintForQuery() {
        return true;
    }

    public boolean needsCaseMatches() {
        return false;
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
        binding.setText(result);
        binding.setIcon(null);
        binding.setIsSelected(false);
        binding.setMaxLines(2);
        binding.executePendingBindings();

        return v;
    }

    public int getMaxResults() {
        return MAX_RESULTS;
    }

    @Override
    public Filter getFilter() {
        return mFilter;
    }

    public abstract List<CharSequence> getResults(CharSequence constraint);


    private static class ResultFilter extends Filter {

        private WeakReference<FilterableAdapter> mInnerClass;
        private final List<CharSequence> mTemp = new ArrayList<>();

        ResultFilter(FilterableAdapter adapter) {
            mInnerClass = new WeakReference<>(adapter);
        }

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterableAdapter innerClass = mInnerClass.get();

            FilterResults results = new FilterResults();
            if (innerClass != null) {
                // Fetch if needed
                if (!innerClass.needsConstraintForQuery()
                        || (innerClass.needsConstraintForQuery() && !TextUtils.isEmpty(constraint))
                        || innerClass.mResults == null) {
                    innerClass.mResults = innerClass.getResults(constraint);
                } else {
                    innerClass.mResults = null;
                }

                // Filter results
                mTemp.clear();
                if (innerClass.mResults != null) {
                    for (CharSequence v : innerClass.mResults) {
                        if (!TextUtils.isEmpty(constraint)) {
                            final String s = v.toString();
                            final String c = constraint.toString();
                            if ((innerClass.needsCaseMatches() && s.contains(c))
                                    || (!innerClass.needsCaseMatches() &&
                                            s.toLowerCase().contains(c.toLowerCase()))) {
                                mTemp.add(v);
                            }
                        }
                        final int maxResults = innerClass.getMaxResults();
                        if (maxResults > 0 &&  mTemp.size() >= maxResults) {
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
            FilterableAdapter innerClass = mInnerClass.get();
            if (innerClass != null) {
                innerClass.mFilteredResults.clear();
                if (results.count > 0) {
                    innerClass.mFilteredResults.addAll((List<CharSequence>) results.values);
                    innerClass.notifyDataSetChanged();
                } else {
                    innerClass.notifyDataSetInvalidated();
                }
            }
        }
    }

}
