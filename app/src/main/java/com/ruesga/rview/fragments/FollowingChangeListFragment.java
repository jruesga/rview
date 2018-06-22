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

import com.ruesga.rview.gerrit.model.AccountInfo;
import com.ruesga.rview.model.Account;
import com.ruesga.rview.model.EmptyState;
import com.ruesga.rview.preferences.Preferences;

import java.util.Set;

public class FollowingChangeListFragment extends ChangeListByFilterFragment {

    private long mLastHash = -1;

    @Override
    public void onResume() {
        super.onResume();
        if (mLastHash != -1 && mLastHash != computeHash()) {
            fetchNewItems();
        }
    }

    public static FollowingChangeListFragment newInstance() {
        FollowingChangeListFragment fragment = new FollowingChangeListFragment();
        Bundle arguments = new Bundle();
        arguments.putBoolean(EXTRA_HAS_SEARCH, true);
        arguments.putBoolean(EXTRA_HAS_FAB, false);
        fragment.setArguments(arguments);
        return fragment;
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
        // TODO: this is a very simple hashing but it should serve for now. Implement
        // a more robust hash algorithm
        long hash = 0;
        Set<AccountInfo> following = followingAccounts();
        if (following != null) {
            for (AccountInfo acct : following) {
                hash += acct.accountId;
            }
        }
        return hash;
    }
}
