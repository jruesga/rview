/*
 * Copyright (C) 2017 Jorge Ruesga
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

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.ruesga.rview.R;
import com.ruesga.rview.gerrit.model.AccountInfo;
import com.ruesga.rview.misc.FowlerNollVo;
import com.ruesga.rview.model.Account;
import com.ruesga.rview.model.EmptyState;
import com.ruesga.rview.preferences.Preferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class FollowingChangeListFragment extends ChangeListByFilterFragment {

    private long mLastHash = -1;

    @Override
    public void onResume() {
        super.onResume();
        fetchNewItemsIfInfoChanged();
    }

    public static FollowingChangeListFragment newInstance() {
        FollowingChangeListFragment fragment = new FollowingChangeListFragment();
        Bundle arguments = new Bundle();
        arguments.putBoolean(EXTRA_HAS_SEARCH, false);
        arguments.putBoolean(EXTRA_HAS_FAB, false);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.following_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_start_following) {
            showStartFollowingDialog();
            return true;
        }
        if (item.getItemId() == R.id.menu_following) {
            showFollowingDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected String getFilter() {
        Set<AccountInfo> following = followingAccounts();
        if (following == null || following.isEmpty()) {
            return null;
        }
        mLastHash = computeHash();

        StringBuilder sb = new StringBuilder();
        for (AccountInfo acct : following) {
            sb.append(" OR owner:").append(acct.accountId);
        }
        return sb.delete(0, 4).toString();
    }

    int getNotResultEmptyState() {
        return hasFollowingAccounts()
                ? EmptyState.NO_RESULTS_STATE : EmptyState.NO_FOLLOWING_STATE;
    }

    boolean hasMoreItems(int size, int expected) {
        if (hasFollowingAccounts()) {
            return super.hasMoreItems(size, expected);
        }
        showProgress(false);
        return false;
    }

    private Set<AccountInfo> followingAccounts() {
        Account account = Preferences.getAccount(getContext());
        return Preferences.getAccountFollowingState(getContext(), account);
    }

    private boolean hasFollowingAccounts() {
        Set<AccountInfo> following = followingAccounts();
        return following != null && !following.isEmpty();
    }

    private long computeHash() {
        long hash = 0;
        Set<AccountInfo> set = followingAccounts();
        if (set != null) {
            List<AccountInfo> following = new ArrayList<>(set);
            Collections.sort(following, (acct1, acct2) ->
                    Integer.compare(acct1.accountId, acct2.accountId));
            StringBuilder hashing = new StringBuilder();
            for (AccountInfo acct : following) {
                hashing.append(acct.accountId).append("|");
            }
            hash = FowlerNollVo.fnv1_64(hashing.toString().getBytes()).longValue();
        }
        return hash;
    }

    private void showStartFollowingDialog() {
        StartFollowingChooserDialogFragment fragment = StartFollowingChooserDialogFragment.newInstance();
        fragment.show(getChildFragmentManager(), StartFollowingChooserDialogFragment.TAG);
    }

    private void showFollowingDialog() {
        FollowingChooserDialogFragment fragment = FollowingChooserDialogFragment.newInstance();
        fragment.show(getChildFragmentManager(), FollowingChooserDialogFragment.TAG);
    }

    public void onFollowingDialogDismissed() {
        fetchNewItemsIfInfoChanged();
    }

    private void fetchNewItemsIfInfoChanged() {
        if (mLastHash != -1 && mLastHash != computeHash()) {
            fetchNewItems();
        }
    }
}
