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
package com.ruesga.rview.widget;

import android.content.Context;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.View;

import com.google.android.flexbox.FlexboxLayout;
import com.ruesga.rview.R;
import com.ruesga.rview.gerrit.model.AccountInfo;
import com.ruesga.rview.gerrit.model.ApprovalInfo;
import com.ruesga.rview.gerrit.model.ChangeInfo;
import com.ruesga.rview.gerrit.model.ReviewerStatus;
import com.ruesga.rview.misc.ModelHelper;
import com.ruesga.rview.widget.AccountChipView.OnAccountChipClickedListener;
import com.ruesga.rview.widget.AccountChipView.OnAccountChipRemovedListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ReviewersView extends FlexboxLayout {
    private Picasso mPicasso;
    private boolean mIsRemovableReviewers;
    private boolean mIsFilterCIAccounts;
    private ReviewerStatus mReviewerStatus = null;
    private OnAccountChipClickedListener mOnAccountChipClickedListener;
    private OnAccountChipRemovedListener mOnAccountChipRemovedListener;
    private Object mTag;

    public ReviewersView(Context context) {
        this(context, null);
    }

    public ReviewersView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ReviewersView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public ReviewersView with(Picasso picasso) {
        mPicasso = picasso;
        return this;
    }

    private ReviewersView from(List<AccountInfo> reviewers, List<Integer> removableReviewers) {
        int margin = (int) getContext().getResources().getDimension(R.dimen.chips_margin);
        boolean rtl = ViewCompat.getLayoutDirection(this) == ViewCompat.LAYOUT_DIRECTION_RTL;

        List<AccountInfo> filteredReviewers = mIsFilterCIAccounts
                ? ModelHelper.filterCIAccounts(getContext(), reviewers) : reviewers;
        int count = filteredReviewers.size();
        int children = getChildCount();
        if (count > children) {
            for (int i = children; i < count; i++) {
                addView(new AccountChipView(getContext()));
                View v = getChildAt(getChildCount() - 1);
                FlexboxLayout.LayoutParams params =
                        ((FlexboxLayout.LayoutParams) v.getLayoutParams());
                params.setMargins(rtl ? margin : 0, 0, rtl ? 0 : margin, margin);
            }
        }
        for (int i = 0; i < count; i++) {
            AccountChipView view = (AccountChipView) getChildAt(i);
            view.with(mPicasso)
                    .removable(mIsRemovableReviewers &&
                            removableReviewers.contains(filteredReviewers.get(i).accountId))
                    .listenOn(mOnAccountChipClickedListener)
                    .listenOn(mOnAccountChipRemovedListener)
                    .withTag(mTag)
                    .from(filteredReviewers.get(i));
            view.setVisibility(View.VISIBLE);
        }
        for (int i = count; i < children; i++) {
            AccountChipView view = (AccountChipView) getChildAt(i);
            view.setVisibility(View.GONE);
        }

        return this;
    }

    public ReviewersView from(List<AccountInfo> reviewers, AccountInfo[] removableReviewers) {
        List<Integer> removableReviewersAccountIds = new ArrayList<>();
        if (mIsRemovableReviewers && removableReviewers != null) {
            for (AccountInfo reviewer : removableReviewers) {
                removableReviewersAccountIds.add(reviewer.accountId);
            }
        }
        return from(ModelHelper.sortReviewers(getContext(), reviewers), removableReviewersAccountIds);
    }

    public ReviewersView from(ChangeInfo change) {
        List<Integer> removableReviewers = new ArrayList<>();
        if (mIsRemovableReviewers && change.removableReviewers != null) {
            for (AccountInfo reviewer : change.removableReviewers) {
                removableReviewers.add(reviewer.accountId);
            }
        }

        List<AccountInfo> reviewers = change.reviewers != null
                ? fromReviewers(change) : fromLabels(change);
        return from(reviewers, removableReviewers);
    }

    public ReviewersView withRemovableReviewers(boolean removable) {
        mIsRemovableReviewers = removable;
        return this;
    }

    public ReviewersView withTag(Object tag) {
        mTag = tag;
        return this;
    }

    public ReviewersView withFilterCIAccounts(boolean filter) {
        mIsFilterCIAccounts = filter;
        return this;
    }

    public ReviewersView withReviewerStatus(ReviewerStatus status) {
        mReviewerStatus = status;
        return this;
    }

    public ReviewersView listenOn(OnAccountChipClickedListener cb) {
        mOnAccountChipClickedListener = cb;
        return this;
    }

    public ReviewersView listenOn(OnAccountChipRemovedListener cb) {
        mOnAccountChipRemovedListener = cb;
        return this;
    }

    private List<AccountInfo> fromReviewers(ChangeInfo change) {
        List<AccountInfo> reviewers = new ArrayList<>();
        if (mReviewerStatus == null) {
            for (ReviewerStatus status : change.reviewers.keySet()) {
                AccountInfo[] accounts = change.reviewers.get(status);
                if (accounts != null) {
                    reviewers.addAll(Arrays.asList(change.reviewers.get(status)));
                }
            }
        } else if (change.reviewers.containsKey(mReviewerStatus)) {
            AccountInfo[] accounts = change.reviewers.get(mReviewerStatus);
            if (accounts != null) {
                reviewers.addAll(Arrays.asList(accounts));
            }
        }
        return ModelHelper.sortReviewers(getContext(), reviewers);
    }

    @SuppressWarnings("Convert2streamapi")
    private List<AccountInfo> fromLabels(ChangeInfo change) {
        List<Integer> accountIds = new ArrayList<>();
        List<AccountInfo> reviewers = new ArrayList<>();
        if (mReviewerStatus == null) {
            for (String label : change.labels.keySet()) {
                if (change.labels.get(label).all !=  null) {
                    for (ApprovalInfo approval : change.labels.get(label).all) {
                        if (!accountIds.contains(approval.owner.accountId)) {
                            accountIds.add(approval.owner.accountId);
                            reviewers.add(approval.owner);
                        }
                    }
                }
            }
        } else  if (mReviewerStatus.equals(ReviewerStatus.REVIEWER)) {
            for (String label : change.labels.keySet()) {
                if (change.labels.get(label).all != null) {
                    for (ApprovalInfo approval : change.labels.get(label).all) {
                        if (!accountIds.contains(approval.owner.accountId)) {
                            accountIds.add(approval.owner.accountId);
                            reviewers.add(approval.owner);
                        }
                    }
                }
            }
        }
        return ModelHelper.sortReviewers(getContext(), reviewers);
    }
}
