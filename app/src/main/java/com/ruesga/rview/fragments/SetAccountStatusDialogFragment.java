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

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import com.ruesga.rview.BaseActivity;
import com.ruesga.rview.R;
import com.ruesga.rview.databinding.SetAccountStatusDialogBinding;
import com.ruesga.rview.gerrit.GerritApi;
import com.ruesga.rview.gerrit.model.AccountStatusInput;
import com.ruesga.rview.misc.EmojiHelper;
import com.ruesga.rview.misc.ExceptionHelper;
import com.ruesga.rview.misc.ModelHelper;
import com.ruesga.rview.misc.SerializationManager;
import com.ruesga.rview.model.Account;
import com.ruesga.rview.preferences.Preferences;
import com.ruesga.rview.services.AccountStatusFetcherService;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.databinding.DataBindingUtil;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import me.tatarka.rxloader2.RxLoader1;
import me.tatarka.rxloader2.RxLoaderManager;
import me.tatarka.rxloader2.RxLoaderManagerCompat;
import me.tatarka.rxloader2.RxLoaderObserver;
import me.tatarka.rxloader2.safe.SafeObservable;

public class SetAccountStatusDialogFragment extends RevealDialogFragment {

    public static final String TAG = "SetAccountStatus";

    private static final String STATE_MODEL = "state:model";
    private static final String STATE_ORIGINAL_STATUS = "state:original";

    @Keep
    public static class Model {
        public String description;
        public String status;
        public boolean isSuggestion;
    }

    @Keep
    public static class EventHandlers {
        private SetAccountStatusDialogFragment mFragment;

        public EventHandlers(SetAccountStatusDialogFragment fragment) {
            mFragment = fragment;
        }

        public void onClearPressed(View v) {
            mFragment.onClearPressed();
        }
    }

    public static SetAccountStatusDialogFragment newInstance() {
        return new SetAccountStatusDialogFragment();
    }

    private RxLoader1<String, String> mSetAccountStateLoader;

    private final RxLoaderObserver<String> mSetAccountStateObserver = new RxLoaderObserver<String>() {
        @Override
        public void onNext(String status) {
            mAccount.mAccount.status = status;
            Preferences.addOrUpdateAccount(getActivity(), mAccount);

            Intent i = new Intent(AccountStatusFetcherService.ACCOUNT_STATUS_FETCHER_ACTION);
            i.putExtra(AccountStatusFetcherService.EXTRA_ACCOUNT, mAccount.getAccountHash());
            //noinspection ConstantConditions
            LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(i);
            dismiss();
        }

        @Override
        public void onError(Throwable error) {
            //noinspection ConstantConditions
            ((BaseActivity) getActivity()).showToast(
                    ExceptionHelper.exceptionToMessage(getActivity(), TAG, error));
            dismiss();
        }
    };

    private final TextWatcher mTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            String newDescription = s.toString();
            String newStatus = EmojiHelper.getSuggestedEmojiFromDescription(
                    getActivity(), newDescription);
            if (TextUtils.isEmpty(newDescription)) {
                newStatus = null;
            }

            mModel.status = newStatus;
            mModel.isSuggestion = EmojiHelper.isSuggestedEmojiStatus(getActivity(), newStatus);
            mBinding.setModel(mModel);
        }
    };

    private final BroadcastReceiver mAccountStatusChangedReceiver = new BroadcastReceiver() {
        @Override
        @SuppressWarnings("ConstantConditions")
        public void onReceive(Context context, Intent intent) {
            if (mAccount != null && intent != null) {
                String account = intent.getStringExtra(AccountStatusFetcherService.EXTRA_ACCOUNT);
                if (mAccount.getAccountHash().equals(account)) {
                    mAccount = Preferences.getAccount(context);
                    updateModel(mAccount.mAccount.status);
                }
            }
        }
    };

    private SetAccountStatusDialogBinding mBinding;
    private Model mModel = new Model();
    private EventHandlers mEventHandlers;
    private Account mAccount;

    private String mOriginalStatus;

    public SetAccountStatusDialogFragment() {
        mEventHandlers = new EventHandlers(this);
    }

    @Override
    public void buildDialog(AlertDialog.Builder builder, Bundle savedInstanceState) {
        LayoutInflater inflater = LayoutInflater.from(builder.getContext());
        mBinding = DataBindingUtil.inflate(inflater, R.layout.set_account_status_dialog, null, true);
        mBinding.status.addTextChangedListener(mTextWatcher);
        mBinding.suggestions.listenTo(this::updateModel);
        mBinding.setModel(mModel);
        mBinding.setHandlers(mEventHandlers);

        builder.setTitle(R.string.account_status_dialog_title)
                .setView(mBinding.getRoot())
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_set, null);
        startLoadersWithValidContext(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(STATE_MODEL, SerializationManager.getInstance().toJson(mModel));
        outState.putString(STATE_ORIGINAL_STATUS, mOriginalStatus);
        super.onSaveInstanceState(outState);
    }

    @Override
    @SuppressLint("RestrictedApi")
    public void setupDialog(Dialog dialog, int style) {
        super.setupDialog(dialog, style);

        dialog.setOnShowListener(dialog1 -> {
            Button button = ((AlertDialog) dialog1).getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(v -> performAccountStateChanged());
        });
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        startLoadersWithValidContext(savedInstanceState);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mBinding != null) {
            mBinding.status.removeTextChangedListener(mTextWatcher);
            mBinding.unbind();
        }

        // Unregister services
        if (getActivity() != null) {
            LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(
                    mAccountStatusChangedReceiver);
        }
    }

    @SuppressWarnings("ConstantConditions")
    private void startLoadersWithValidContext(Bundle savedInstanceState) {
        if (getActivity() == null && mBinding != null) {
            return;
        }

        if (mAccount == null) {
            mAccount = Preferences.getAccount(getActivity());
            if (savedInstanceState != null) {
                mOriginalStatus = savedInstanceState.getString(STATE_ORIGINAL_STATUS);
                mModel = SerializationManager.getInstance().fromJson(
                        savedInstanceState.getString(STATE_MODEL), Model.class);
                mBinding.setModel(mModel);
            } else {
                mOriginalStatus = EmojiHelper.getSuggestedDescriptionFromEmoji(
                        getActivity(), mAccount.mAccount.status);
                updateModel(mAccount.mAccount.status);
            }

            // Fetch or join current loader
            RxLoaderManager loaderManager = RxLoaderManagerCompat.get(this);
            mSetAccountStateLoader = loaderManager.create(
                    "set", this::setAccountState, mSetAccountStateObserver);

            IntentFilter filter = new IntentFilter();
            filter.addAction(AccountStatusFetcherService.ACCOUNT_STATUS_FETCHER_ACTION);
            LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                    mAccountStatusChangedReceiver, filter);
            fetchAccountStatus(mAccount);
        }
    }

    private void performAccountStateChanged() {
        if (TextUtils.equals(mModel.description, mOriginalStatus)) {
            dismiss();
            return;
        }

        mSetAccountStateLoader.clear();
        mSetAccountStateLoader.restart(mModel.description);
    }

    private void updateModel(String status) {
        mModel.description = EmojiHelper.getSuggestedDescriptionFromEmoji(getActivity(), status);
        mModel.status = status;
        mModel.isSuggestion = EmojiHelper.isSuggestedEmojiStatus(getActivity(), status);
        if (mBinding != null) {
            mBinding.setModel(mModel);
        }
    }

    @SuppressWarnings("ConstantConditions")
    private Observable<String> setAccountState(final String status) {
        final Context ctx = getActivity();
        final GerritApi api = ModelHelper.getGerritApi(ctx);
        return SafeObservable.fromNullCallable(() -> {
                AccountStatusInput input = new AccountStatusInput();
                input.status = status;
                if (!TextUtils.isEmpty(status)) {
                    input.status = EmojiHelper.getSuggestedEmojiFromDescription(
                            getActivity(), status);
                }
                api.setAccountStatus(GerritApi.SELF_ACCOUNT, input).blockingFirst();
                return input.status;
            })
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread());
    }

    private void onClearPressed() {
        mModel.description = null;
        mModel.status = null;
        mModel.isSuggestion = false;
        mBinding.setModel(mModel);
    }

    private void fetchAccountStatus(Account account) {
        // This is running while the app is in foreground so there is not risk on
        // perform this operation. In addition, we need the account information asap, so
        // just try to start the service directly.
        try {
            Intent intent = new Intent(getActivity(), AccountStatusFetcherService.class);
            intent.setAction(AccountStatusFetcherService.ACCOUNT_STATUS_FETCHER_ACTION);
            intent.putExtra(AccountStatusFetcherService.EXTRA_ACCOUNT, account.getAccountHash());
            //noinspection ConstantConditions
            getActivity().startService(intent);
        } catch (IllegalStateException ex) {
            Log.w(TAG, "Can't start account fetcher service.", ex);
        }
    }
}
