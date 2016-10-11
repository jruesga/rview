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
import com.ruesga.rview.gerrit.model.SuggestedReviewerInfo;
import com.ruesga.rview.misc.ModelHelper;
import com.ruesga.rview.model.Account;
import com.ruesga.rview.preferences.Preferences;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ReviewersAdapter extends BaseAdapter implements Filterable {

    private static final int MAX_RESULTS = 5;

    private Context mContext;
    private String mLegacyChangeId;
    private List<SuggestedReviewerInfo> mReviewers = new ArrayList<>();
    private final ReviewerFilter mFilter;

    public ReviewersAdapter(Context context, int legacyChangeId) {
        mContext = context;
        mLegacyChangeId = String.valueOf(legacyChangeId);
        mFilter = new ReviewerFilter(this);
    }

    @Override
    public int getCount() {
        return mReviewers.size();
    }

    @Override
    public String getItem(int position) {
        return (String) formatSuggestionReviewer(mReviewers.get(position), false);
    }

    public SuggestedReviewerInfo getSuggestedReviewerAt(int position) {
        return mReviewers.get(position);
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

        SuggestedReviewerInfo reviewer = mReviewers.get(position);
        ReviewerDropdownItemBinding binding = (ReviewerDropdownItemBinding) v.getTag();
        binding.item.setText(formatSuggestionReviewer(reviewer, true));
        binding.setIsGroup(reviewer.account == null);
        binding.executePendingBindings();

        return v;
    }

    @Override
    public Filter getFilter() {
        return mFilter;
    }

    private CharSequence formatSuggestionReviewer(
            SuggestedReviewerInfo reviewer, boolean highlight) {
        String text = null;
        if (reviewer.account != null) {
            text = ModelHelper.formatAccountWithEmail(reviewer.account);
        } else if (reviewer.group != null) {
            text = reviewer.group.name;
        }
        if (highlight) {
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


    private static class ReviewerFilter extends Filter {

        private final WeakReference<ReviewersAdapter> mAdapter;
        private final Account mAccount;
        private final GerritApi mGerritApi;
        private String mLegacyChangeId;
        private CharSequence mConstraint;

        private ReviewerFilter(ReviewersAdapter adapter) {
            mAdapter = new WeakReference<>(adapter);
            mGerritApi = ModelHelper.getGerritApi(adapter.mContext);
            mAccount = Preferences.getAccount(adapter.mContext);
            mLegacyChangeId = adapter.mLegacyChangeId;
        }

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults results = new FilterResults();
            if (constraint != null) {
                List<SuggestedReviewerInfo> reviewers =
                        fetchSuggestedReviewers(constraint.toString());
                removeSelfFromReviewers(reviewers);

                results.values = reviewers;
                results.count = reviewers.size();
            }
            return results;
        }

        @Override
        @SuppressWarnings("unchecked")
        protected void publishResults(CharSequence constraint, FilterResults results) {
            ReviewersAdapter adapter = mAdapter.get();
            if (adapter != null) {
                mConstraint = constraint;
                adapter.mReviewers.clear();
                if (results.count > 0) {
                    adapter.mReviewers.addAll((List<SuggestedReviewerInfo>) results.values);
                    adapter.notifyDataSetChanged();
                } else {
                    adapter.notifyDataSetInvalidated();
                }
            }
        }

        @SuppressWarnings("ConstantConditions")
        private List<SuggestedReviewerInfo> fetchSuggestedReviewers(String query) {
            return mGerritApi.getChangeSuggestedReviewers(mLegacyChangeId, query, MAX_RESULTS)
                    .toBlocking()
                    .first();
        }

        private void removeSelfFromReviewers(List<SuggestedReviewerInfo> reviewers) {
            if (mAccount != null) {
                Iterator<SuggestedReviewerInfo> it = reviewers.iterator();
                while (it.hasNext()) {
                    SuggestedReviewerInfo reviewer = it.next();
                    if (reviewer.account.accountId == mAccount.mAccount.accountId) {
                        it.remove();
                        return;
                    }
                }
            }
        }
    }

}
