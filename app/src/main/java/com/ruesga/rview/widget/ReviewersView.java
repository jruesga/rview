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

import com.ruesga.rview.R;
import com.ruesga.rview.gerrit.model.AccountInfo;
import com.ruesga.rview.gerrit.model.ApprovalInfo;
import com.ruesga.rview.gerrit.model.ChangeInfo;
import com.ruesga.rview.gerrit.model.ReviewerStatus;
import com.ruesga.rview.misc.AndroidHelper;
import com.ruesga.rview.widget.AccountChipView.OnAccountChipClickedListener;
import com.ruesga.rview.widget.AccountChipView.OnAccountChipRemovedListener;
import com.squareup.picasso.Picasso;

import org.apmem.tools.layouts.FlowLayout;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ReviewersView extends FlowLayout {
    private Picasso mPicasso;
    private boolean mIsRemovableReviewers;
    private OnAccountChipClickedListener mOnAccountChipClickedListener;
    private OnAccountChipRemovedListener mOnAccountChipRemovedListener;

    public ReviewersView(Context context) {
        this(context, null);
    }

    public ReviewersView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ReviewersView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOrientation(HORIZONTAL);
    }

    public ReviewersView with(Picasso picasso) {
        mPicasso = picasso;
        return this;
    }

    private ReviewersView from(List<AccountInfo> reviewers, List<Integer> removableReviewers) {
        int margin = (int) getContext().getResources().getDimension(R.dimen.chips_margin);
        boolean rtl = ViewCompat.getLayoutDirection(this) == ViewCompat.LAYOUT_DIRECTION_RTL;

        int count = reviewers.size();
        int children = getChildCount();
        if (count > children) {
            for (int i = children; i < count; i++) {
                addView(new AccountChipView(getContext()));
                View v = getChildAt(getChildCount() - 1);
                FlowLayout.LayoutParams params = ((FlowLayout.LayoutParams) v.getLayoutParams());
                params.setMargins(rtl ? margin : 0, 0, rtl ? 0 : margin, margin);
            }
        }
        for (int i = 0; i < count; i++) {
            AccountChipView view = (AccountChipView) getChildAt(i);
            view.with(mPicasso)
                    .removable(mIsRemovableReviewers &&
                            removableReviewers.contains(reviewers.get(i).accountId))
                    .listenOn(mOnAccountChipClickedListener)
                    .listenOn(mOnAccountChipRemovedListener)
                    .from(reviewers.get(i));
            view.setVisibility(View.VISIBLE);
        }
        for (int i = count; i < children; i++) {
            AccountChipView view = (AccountChipView) getChildAt(i);
            view.setVisibility(View.GONE);
        }

        return this;
    }

    public ReviewersView from(List<AccountInfo> reviewers) {
        mIsRemovableReviewers = false;
        return from(reviewers, null);
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

    public ReviewersView listenOn(OnAccountChipClickedListener cb) {
        mOnAccountChipClickedListener = cb;
        return this;
    }

    public ReviewersView listenOn(OnAccountChipRemovedListener cb) {
        mOnAccountChipRemovedListener = cb;
        return this;
    }

    @Override
    public boolean isDebugDraw() {
        // FIXME: super.isDebugDraw() does reflection calls that cause
        // IllegalArgumentExceptions being thrown, causing scrolling slowness,
        // thus we avoid it being called here. This should probably be fixed upstream.
        return false;
    }

    private List<AccountInfo> fromReviewers(ChangeInfo change) {
        List<AccountInfo> reviewers = new ArrayList<>();
        for (ReviewerStatus status : change.reviewers.keySet()) {
            AccountInfo[] accounts = change.reviewers.get(status);
            if (accounts != null) {
                reviewers.addAll(Arrays.asList(change.reviewers.get(status)));
            }
        }
        return sortReviewers(reviewers);
    }

    @SuppressWarnings("Convert2streamapi")
    private List<AccountInfo> fromLabels(ChangeInfo change) {
        List<Integer> accountIds = new ArrayList<>();
        List<AccountInfo> reviewers = new ArrayList<>();
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
        return sortReviewers(reviewers);
    }

    private List<AccountInfo> sortReviewers(List<AccountInfo> reviewers) {
        final Collator collator = Collator.getInstance(
                AndroidHelper.getCurrentLocale(getContext()));
        Collections.sort(reviewers, (a1, a2) -> collator.compare(a1.name, a2.name));
        return reviewers;
    }
}
