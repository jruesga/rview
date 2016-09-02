/*
 * https://gist.github.com/junfengren/f06c6f19e5832c62fe93b61ac8f72ff9
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

import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;

public abstract class EndlessRecyclerViewScrollListener extends RecyclerView.OnScrollListener {

    private static final int DEFAULT_VISIBLE_THRESHOLD = 5;

    // The minimum amount of items to have below your current scroll position
    // before loading more.
    private int visibleThreshold;
    // The current offset index of data you have loaded
    private int currentPage = 0;
    // The total number of items in the dataset after the last load
    private int previousTotalItemCount = 0;
    // True if we are still waiting for the last set of data to load.
    private boolean loading = false;

    RecyclerView.LayoutManager mLayoutManager;

    public EndlessRecyclerViewScrollListener(RecyclerView.LayoutManager layoutManager) {
        this.mLayoutManager = layoutManager;
        setVisibleThreshold(DEFAULT_VISIBLE_THRESHOLD);
    }

    public void setVisibleThreshold(int threshold) {
        visibleThreshold = threshold;
        if (mLayoutManager instanceof GridLayoutManager) {
            visibleThreshold = visibleThreshold *
                    ((GridLayoutManager) mLayoutManager).getSpanCount();
        } else if (mLayoutManager instanceof StaggeredGridLayoutManager) {
            visibleThreshold = visibleThreshold *
                    ((StaggeredGridLayoutManager) mLayoutManager).getSpanCount();
        }
    }

    public int getLastVisibleItem(int[] lastVisibleItemPositions) {
        int maxSize = 0;
        for (int i = 0; i < lastVisibleItemPositions.length; i++) {
            if (i == 0) {
                maxSize = lastVisibleItemPositions[i];
            }
            else if (lastVisibleItemPositions[i] > maxSize) {
                maxSize = lastVisibleItemPositions[i];
            }
        }
        return maxSize;
    }

    // This happens many times a second during a scroll, so be wary of the code you place here.
    // We are given a few useful parameters to help us work out if we need to load some more data,
    // but first we check if we are waiting for the previous load to finish.
    @Override
    public void onScrolled(RecyclerView view, int dx, int dy) {
        boolean isAtBeginning = false;
        int lastVisibleItemPosition = 0;
        int totalItemCount = mLayoutManager.getItemCount();

        if (mLayoutManager instanceof StaggeredGridLayoutManager) {
            int[] lastVisibleItemPositions = ((StaggeredGridLayoutManager) mLayoutManager).
                    findLastVisibleItemPositions(null);
            // get maximum element within the list
            lastVisibleItemPosition = getLastVisibleItem(lastVisibleItemPositions);


            int[] firstItem = new int[1];
            ((StaggeredGridLayoutManager) mLayoutManager).
                    findFirstCompletelyVisibleItemPositions(firstItem);
            isAtBeginning = firstItem[0] <= 0;

        } else if (mLayoutManager instanceof LinearLayoutManager) {
            lastVisibleItemPosition = ((LinearLayoutManager) mLayoutManager).
                    findLastVisibleItemPosition();
            isAtBeginning = ((LinearLayoutManager) mLayoutManager).
                    findFirstCompletelyVisibleItemPosition() <= 0;
        }

        // If the total item count is zero and the previous isn't, assume the
        // list is invalidated and should be reset back to initial state
        if (totalItemCount < previousTotalItemCount) {
            this.currentPage = 0;
            this.previousTotalItemCount = totalItemCount;
            if (totalItemCount == 0) {
                this.loading = true;
            }
        }
        // If it’s still loading, we check to see if the dataset count has
        // changed, if so we conclude it has finished loading and update the current page
        // number and total item count.
        if (loading && (totalItemCount > previousTotalItemCount)) {
            previousTotalItemCount = totalItemCount;
        }

        // If it isn’t currently loading, we check to see if we have breached
        // the visibleThreshold and need to reload more data.
        // If we do need to reload some more data, we execute onLoadMore to fetch the data.
        // threshold should reflect how many total columns there are too
        if (!isAtBeginning && !loading && (lastVisibleItemPosition + visibleThreshold) > totalItemCount) {
            currentPage++;
            onLoadMore(currentPage, totalItemCount);
            loading = true;
        }
    }

    public boolean isLoading() {
        return loading;
    }

    /**
     * Must be called when data is completed
     */
    public void loadCompleted() {
        loading = false;
    }

    // Defines the process for actually loading more data based on page
    public abstract void onLoadMore(int page, int totalItemsCount);

}