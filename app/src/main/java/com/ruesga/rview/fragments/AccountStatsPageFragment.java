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
package com.ruesga.rview.fragments;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ruesga.rview.R;
import com.ruesga.rview.databinding.AccountDetailsViewBinding;
import com.ruesga.rview.gerrit.GerritApi;
import com.ruesga.rview.gerrit.filter.ChangeQuery;
import com.ruesga.rview.gerrit.filter.TimeUnit;
import com.ruesga.rview.gerrit.model.AccountDetailInfo;
import com.ruesga.rview.gerrit.model.ChangeInfo;
import com.ruesga.rview.gerrit.model.EmailInfo;
import com.ruesga.rview.gerrit.model.Features;
import com.ruesga.rview.misc.ModelHelper;
import com.ruesga.rview.misc.PicassoHelper;
import com.ruesga.rview.misc.SerializationManager;
import com.ruesga.rview.model.Account;
import com.ruesga.rview.preferences.Constants;
import com.ruesga.rview.preferences.Preferences;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;

public class AccountStatsPageFragment extends StatsPageFragment<AccountDetailInfo> {

    private static final String TAG = "AccountStatsPageFragment";

    private AccountDetailsViewBinding mBinding;
    private String mAccountId;
    private AccountDetailInfo mCachedAccount;
    private Picasso mPicasso;

    public static AccountStatsPageFragment newFragment(String accountId, String account) {
        AccountStatsPageFragment fragment = new AccountStatsPageFragment();
        Bundle arguments = new Bundle();
        arguments.putString(Constants.EXTRA_ID, accountId);
        arguments.putString(Constants.EXTRA_FRAGMENT_EXTRA, account);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAccountId = getArguments().getString(Constants.EXTRA_ID);
        mCachedAccount = SerializationManager.getInstance().fromJson(
                getArguments().getString(Constants.EXTRA_FRAGMENT_EXTRA),
                AccountDetailInfo.class);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
            @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        loadWithContext();
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        loadWithContext();
        super.onActivityCreated(savedInstanceState);
    }

    private void loadWithContext() {
        if (getActivity() == null) {
            return;
        }

        if (mPicasso == null) {
            mPicasso = PicassoHelper.getPicassoClient(getContext());
        }
    }

    @Override
    public View inflateDetails(LayoutInflater inflater, @Nullable ViewGroup container) {
        mBinding = DataBindingUtil.inflate(
                inflater, R.layout.account_details_view, container, false);
        mBinding.setModel(null);
        return mBinding.getRoot();
    }

    @Override
    @SuppressWarnings({"ConstantConditions", "Convert2streamapi"})
    public Observable<AccountDetailInfo> fetchDetails() {
        final Context ctx = getActivity();
        final GerritApi api = ModelHelper.getGerritApi(ctx);
        return Observable.fromCallable(() -> {
                // Some prior versions doesn't support the details entry point
                // and some other servers doesn't allow to access this information
                // If something went wrong just fallback to the cached information.
                if (api.supportsFeature(Features.ACCOUNT_DETAILS)) {
                    try {
                        mCachedAccount = api.getAccountDetails(String.valueOf(mAccountId))
                                .toBlocking().first();
                    } catch (Exception ex) {
                        // Ignore
                    }
                }

                // Fetch secondary emails (only for self account)
                Account account = Preferences.getAccount(getContext());
                if (mCachedAccount.accountId == account.mAccount.accountId &&
                        mCachedAccount.secondaryEmails == null) {
                    try {
                        List<EmailInfo> emails = api.getAccountEmails(GerritApi.SELF_ACCOUNT)
                                .toBlocking().first();
                        if (emails != null) {
                            List<String> secondaryEmails = new ArrayList<>();
                            for (EmailInfo email : emails) {
                                if (!email.pendingConfirmation && !email.preferred) {
                                    secondaryEmails.add(email.email);
                                }
                            }
                            mCachedAccount.secondaryEmails =
                                    secondaryEmails.toArray(new String[secondaryEmails.size()]);
                        }
                    } catch (Exception ex) {
                        // Ignore
                        ex.printStackTrace();
                    }
                }
                return mCachedAccount;
            });
    }

    @Override
    public ChangeQuery getStatsQuery() {
        return new ChangeQuery().owner(mAccountId).and(
                new ChangeQuery().negate(new ChangeQuery().age(TimeUnit.DAYS, getMaxDays())));
    }

    @Override
    public void bindDetails(AccountDetailInfo result) {
        PicassoHelper.bindAvatar(getContext(), mPicasso, result, mBinding.avatar,
                PicassoHelper.getDefaultAvatar(getContext(), R.color.primaryDark));
        mBinding.setModel(result);
        mBinding.executePendingBindings();
    }

    @Override
    public String getStatsFragmentTag() {
        return TAG;
    }

    @Override
    public String getTop5StatsDescription(ChangeInfo change) {
        return change.project;
    }
}
