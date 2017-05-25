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
import android.text.Spannable;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;

import com.ruesga.rview.R;
import com.ruesga.rview.databinding.ReviewerDropdownItemBinding;
import com.ruesga.rview.gerrit.GerritApi;
import com.ruesga.rview.gerrit.filter.Option;
import com.ruesga.rview.gerrit.model.AccountInfo;
import com.ruesga.rview.misc.ModelHelper;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class AccountsAdapter extends BaseAdapter implements Filterable {

    private static final int MAX_RESULTS = 5;

    private Context mContext;
    private List<AccountInfo> mAccounts = new ArrayList<>();
    private final AccountFilter mFilter;

    public AccountsAdapter(Context context) {
        mContext = context;
        mFilter = new AccountFilter(this);
    }

    @Override
    public int getCount() {
        return mAccounts.size();
    }

    @Override
    public String getItem(int position) {
        return (String) formatAccount(mAccounts.get(position), false);
    }

    public AccountInfo getAccountAt(int position) {
        return mAccounts.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        if (v == null) {
            ReviewerDropdownItemBinding binding = DataBindingUtil.inflate(
                    LayoutInflater.from(mContext), R.layout.reviewer_dropdown_item, parent, false);
            v = binding.getRoot();
            v.setTag(binding);
        }

        AccountInfo account = mAccounts.get(position);
        ReviewerDropdownItemBinding binding = (ReviewerDropdownItemBinding) v.getTag();
        binding.item.setText(formatAccount(account, true));
        binding.setIsGroup(false);
        binding.executePendingBindings();

        return v;
    }

    @Override
    public Filter getFilter() {
        return mFilter;
    }

    private CharSequence formatAccount(AccountInfo account, boolean highlight) {
        String text = null;
        if (account != null) {
            text = ModelHelper.formatAccountWithEmail(account);
        }
        if (highlight && text != null) {
            return highlightOccurrences(text);
        }
        return text;
    }

    private Spannable highlightOccurrences(String s) {
        String constraint = mFilter.mConstraint.toString().toLowerCase();
        Spannable spannable = Spannable.Factory.getInstance().newSpannable(s);

        int index = 0;
        int len = constraint.length();
        String s1 = s.toLowerCase();
        while (true) {
            index = s1.indexOf(constraint, index);
            if (index == -1) {
                break;
            }

            final StyleSpan bold = new StyleSpan(android.graphics.Typeface.BOLD);
            spannable.setSpan(bold, index, index + len, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            index += len;
        }

        return spannable;
    }


    private static class AccountFilter extends Filter {

        private final WeakReference<AccountsAdapter> mAdapter;
        private final GerritApi mGerritApi;
        private CharSequence mConstraint;

        private AccountFilter(AccountsAdapter adapter) {
            mAdapter = new WeakReference<>(adapter);
            mGerritApi = ModelHelper.getGerritApi(adapter.mContext);
        }

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults results = new FilterResults();
            if (constraint != null) {
                List<AccountInfo> accounts = fetchAccounts(constraint.toString());

                results.values = accounts;
                results.count = accounts.size();
            }
            return results;
        }

        @Override
        @SuppressWarnings("unchecked")
        protected void publishResults(CharSequence constraint, FilterResults results) {
            AccountsAdapter adapter = mAdapter.get();
            if (adapter != null) {
                mConstraint = constraint;
                adapter.mAccounts.clear();
                if (results.count > 0) {
                    adapter.mAccounts.addAll((List<AccountInfo>) results.values);
                    adapter.notifyDataSetChanged();
                } else {
                    adapter.notifyDataSetInvalidated();
                }
            }
        }

        @SuppressWarnings("ConstantConditions")
        private List<AccountInfo> fetchAccounts(String query) {
            return mGerritApi.getAccountsSuggestions(query, MAX_RESULTS, Option.INSTANCE)
                    .blockingFirst();
        }
    }

}
