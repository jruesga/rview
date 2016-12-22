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
package com.ruesga.rview.wizards;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.PopupMenu;
import android.text.Editable;
import android.text.Html;
import android.text.Spannable;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.ViewGroup;

import com.ruesga.rview.R;
import com.ruesga.rview.annotations.ProguardIgnored;
import com.ruesga.rview.databinding.WizardAccountPageFragmentBinding;
import com.ruesga.rview.exceptions.NoActivityAttachedException;
import com.ruesga.rview.gerrit.Authorization;
import com.ruesga.rview.gerrit.GerritApi;
import com.ruesga.rview.gerrit.GerritServiceFactory;
import com.ruesga.rview.gerrit.NoConnectivityException;
import com.ruesga.rview.gerrit.model.AccountInfo;
import com.ruesga.rview.gerrit.model.ServerInfo;
import com.ruesga.rview.misc.ActivityHelper;
import com.ruesga.rview.misc.SerializationManager;
import com.ruesga.rview.model.Account;
import com.ruesga.rview.wizard.WizardActivity;
import com.ruesga.rview.wizard.WizardPageFragment;
import com.ruesga.rview.wizard.misc.TextChangedWatcher;
import com.ruesga.rview.wizard.validators.DependencyValidator;
import com.ruesga.rview.wizard.validators.NonEmptyTextValidator;
import com.ruesga.rview.wizard.validators.TextInputValidatorObserver;
import com.ruesga.rview.wizard.validators.Validator;

import java.util.Locale;
import java.util.concurrent.Callable;

import static com.ruesga.rview.misc.ExceptionHelper.isException;

public class AccountPageFragment extends WizardPageFragment {

    private static final String TAG = "AccountPageFragment";

    public static final String STATE_ACCOUNT_ACCESS_MODE = "account.access.mode";
    public static final String STATE_ACCOUNT_USERNAME = "account.username";
    public static final String STATE_ACCOUNT_PASSWORD = "account.password";
    public static final String STATE_ACCOUNT_INFO = "account.info";
    private static final String STATE_ACCOUNT_CONFIRMED = "account.confirmed";
    private static final String STATE_REPO_NAME = "repo.name";
    private static final String STATE_REPO_URL = "repo.url";
    public static final String STATE_REPO_TRUST_ALL_CERTIFICATES = "repo.trustAllCertificates";

    @ProguardIgnored
    @SuppressWarnings("unused")
    public static class Model {
        public Spanned message;
        public String username;
        public String password;
        public AccountInfo accountInfo;
        public boolean authenticatedAccess;
        private String repoUrl;
        private boolean repoTrustAllCertificates;
        private boolean wasConfirmed;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    @ProguardIgnored
    @SuppressWarnings("unused")
    public static class EventHandlers {
        AccountPageFragment mFragment;
        public EventHandlers(AccountPageFragment fragment) {
            mFragment = fragment;
        }

        public void onClickPressed(View view) {
            if (view.getId() == R.id.access_mode_switcher_layout) {
                mFragment.mBinding.accessModeSwitcher.setChecked(
                        !mFragment.mBinding.accessModeSwitcher.isChecked());
            }
        }
    }

    private WizardAccountPageFragmentBinding mBinding;
    private Model mModel = new Model();
    private EventHandlers mEventHandlers;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
        final Context context = inflater.getContext();
        if (mEventHandlers == null) {
            mEventHandlers = new EventHandlers(this);
        }

        mBinding = DataBindingUtil.inflate(inflater, getPageLayout(), container, false);
        bindHelpLinks();

        DependencyValidator dependencyValidator = new DependencyValidator(
                mBinding.accessModeSwitcher, new NonEmptyTextValidator(context));
        setValidatorsForView(mBinding.accountUsernameEdit, new TextInputValidatorObserver(this),
                dependencyValidator);
        setValidatorsForView(mBinding.accountPasswordEdit, new TextInputValidatorObserver(this),
                dependencyValidator);

        mBinding.setModel(mModel);
        mBinding.setHandlers(mEventHandlers);

        mBinding.accessModeSwitcher.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            mModel.authenticatedAccess = isChecked;
            mBinding.accountUsername.setError(null);
            mBinding.accountUsername.clearFocus();
            mBinding.accountPassword.setError(null);
            mBinding.accountPassword.clearFocus();
            mBinding.accessModeSwitcher.requestFocus();
            mBinding.setModel(mModel);
            triggerAllValidators(compoundButton);
            mModel.wasConfirmed = false;
        });

        mBinding.accountUsernameEdit.addTextChangedListener(new TextChangedWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                mModel.wasConfirmed = false;
            }
        });
        mBinding.accountPasswordEdit.addTextChangedListener(new TextChangedWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                mModel.wasConfirmed = false;
            }
        });

        // Check validators (to ensure all them will be cleaned)
        if (!mModel.authenticatedAccess) {
            triggerAllValidators(null);
        }

        return mBinding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mBinding.unbind();
    }

    @Override
    public Bundle savedState() {
        Bundle bundle = new Bundle();
        bundle.putBoolean(STATE_ACCOUNT_ACCESS_MODE, mModel.authenticatedAccess);
        bundle.putString(STATE_ACCOUNT_USERNAME, mModel.username);
        bundle.putString(STATE_ACCOUNT_PASSWORD, mModel.password);
        bundle.putString(STATE_ACCOUNT_INFO,
                SerializationManager.getInstance().toJson(mModel.accountInfo));
        bundle.putBoolean(STATE_ACCOUNT_CONFIRMED, mModel.wasConfirmed);
        return bundle;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void restoreState(Context context, Bundle savedState) {
        mModel.message = Html.fromHtml(context.getString(
                R.string.account_wizard_account_page_message,
                savedState.getString(STATE_REPO_NAME)));
        mModel.repoUrl = savedState.getString(STATE_REPO_URL);
        mModel.repoTrustAllCertificates =
                savedState.getBoolean(STATE_REPO_TRUST_ALL_CERTIFICATES, false);
        mModel.authenticatedAccess = savedState.getBoolean(STATE_ACCOUNT_ACCESS_MODE, false);
        mModel.username = savedState.getString(STATE_ACCOUNT_USERNAME);
        mModel.password = savedState.getString(STATE_ACCOUNT_PASSWORD);
        mModel.accountInfo = SerializationManager.getInstance().fromJson(
                savedState.getString(STATE_ACCOUNT_INFO), AccountInfo.class);
        mModel.wasConfirmed = savedState.getBoolean(STATE_ACCOUNT_CONFIRMED);
        if (mBinding != null) {
            mBinding.setModel(mModel);
        }
    }

    @Override
    public int getPageTitle() {
        return R.string.account_wizard_account_page_title;
    }

    @Override
    public int getPageLayout() {
        return R.layout.wizard_account_page_fragment;
    }

    public boolean canRequireKeyboard() {
        return true;
    }

    @Override
    public boolean hasBackAction() {
        return true;
    }

    @Override
    public int getBackActionLabel() {
        return R.string.action_previous;
    }

    @Override
    public int getBackActionDrawable() {
        return R.drawable.ic_chevron_left;
    }

    @Override
    public boolean hasForwardAction() {
        return true;
    }

    @Override
    public int getForwardActionLabel() {
        return R.string.action_next;
    }

    @Override
    public int getForwardActionDrawable() {
        return R.drawable.ic_chevron_right;
    }

    @Override
    public boolean hasPageOptionsMenu() {
        return true;
    }

    @Override
    public int getPageOptionsMenu() {
        return R.menu.wizard_account_page;
    }

    @Override
    public PopupMenu.OnMenuItemClickListener getPageOptionsMenuOnItemClickListener() {
        return item -> {
            if (item.getItemId() == R.id.menu_help) {
                openHelp();
            }
            return false;
        };
    }

    @Override
    public Callable<Boolean> doForwardAction() {
        // Check if the url passed is a valid Gerrit endpoint
        return () -> {
            if (mModel.wasConfirmed) {
                return Boolean.TRUE;
            }

            if (!mModel.authenticatedAccess) {
                String anonymousCowardName = getAnonymousCowardName();
                if (anonymousCowardName == null) {
                    anonymousCowardName = getString(R.string.account_anonymous_coward);
                }
                mModel.accountInfo = new AccountInfo();
                mModel.accountInfo.accountId = Account.ANONYMOUS_ACCOUNT_ID;
                mModel.accountInfo.name = anonymousCowardName;
                return true;
            }

            try {
                // Check if the activity is attached
                if (getActivity() == null) {
                    throw new NoActivityAttachedException();
                }
                return logIn();
            } catch (Exception ex) {
                postUpdateErrorMessage(ex);
                mModel.wasConfirmed = false;
                throw  ex;
            }
        };
    }

    private boolean logIn() {
        Context ctx = getActivity().getApplicationContext();
        Authorization authorization = new Authorization(
                mModel.username, mModel.password, mModel.repoTrustAllCertificates);
        GerritApi client = GerritServiceFactory.getInstance(ctx, mModel.repoUrl, authorization);
        mModel.accountInfo = client.getAccount(GerritApi.SELF_ACCOUNT).blockingFirst();
        return mModel.accountInfo != null;
    }

    private String getAnonymousCowardName() {
        try {
            Context ctx = getActivity().getApplicationContext();
            Authorization authorization = new Authorization(
                    mModel.username, mModel.password, mModel.repoTrustAllCertificates);
            GerritApi client = GerritServiceFactory.getInstance(ctx, mModel.repoUrl, authorization);
            ServerInfo serverInfo = client.getServerInfo().blockingFirst();
            return serverInfo.user.anonymousCowardName;
        } catch (Exception ex) {
            Log.w(TAG, "Gerrit repository doesn't provided server configuration.");
        }
        return null;
    }

    private void postUpdateErrorMessage(final Exception cause) {
        if (mBinding != null) {
            mBinding.accountUsername.post(() -> {
                final Context context = mBinding.accountUsername.getContext();
                if (isException(cause, NoConnectivityException.class)) {
                    ((WizardActivity) getActivity()).showMessage(context.getString(
                            R.string.exception_no_network_available));
                } else {
                    // Just ignore it if we don't have a valid context
                    if (!(cause instanceof NoActivityAttachedException)) {
                        Log.e(TAG, "Invalid user or password", cause);
                        mBinding.accountUsername.setError(context.getString(
                                R.string.exception_invalid_user_password));
                    }
                }
            });
        }
    }

    public void onViewValidated(View v, Validator failed) {
        TextInputLayout layout = getTextInputLayoutFromView(v);
        if (layout != null) {
            layout.setError(failed == null ? null : failed.getMessage());
        }
    }

    private TextInputLayout getTextInputLayoutFromView(View v) {
        TextInputLayout layout = null;
        switch (v.getId()) {
            case R.id.account_username_edit:
                layout = mBinding.accountUsername;
                break;
            case R.id.account_password_edit:
                layout = mBinding.accountPassword;
                break;
        }
        return layout;
    }

    private void bindHelpLinks() {
        mBinding.accountAuthenticationHint.setMovementMethod(LinkMovementMethod.getInstance());
        String msg = getString(R.string.account_wizard_account_page_authenticated_access_hint);
        String link = getString(R.string.account_wizard_account_page_password_hint_here);
        String text = String.format(Locale.getDefault(), msg, link, link);


        // Link to http-password url
        Spannable span = Spannable.Factory.getInstance().newSpannable(text);
        int pos = msg.indexOf("%1$s");
        if (pos >= 0) {
            span.setSpan(new ClickableSpan() {
                @Override
                public void onClick(View v) {
                    // Click on span doesn't provide sound feedback it the text view doesn't
                    // handle a click event. Just perform a click effect.
                    v.playSoundEffect(SoundEffectConstants.CLICK);
                    openHttpPasswordUrl();
                }
            }, pos, pos + link.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        // Link to sign url
        pos = msg.indexOf("%2$s");
        if (pos >= 0) {
            span.setSpan(new ClickableSpan() {
                @Override
                public void onClick(View v) {
                    // Click on span doesn't provide sound feedback it the text view doesn't
                    // handle a click event. Just perform a click effect.
                    v.playSoundEffect(SoundEffectConstants.CLICK);
                    openSignInUrl();
                }
            }, pos, pos + link.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        mBinding.accountAuthenticationHint.setText(span);
    }

    private void openHttpPasswordUrl() {
        if (getActivity() == null) {
            // Activity was disposed. Ignore this event.
            return;
        }
        final String url = mModel.repoUrl + GerritApi.HTTP_PASSWORD_URI;
        ActivityHelper.openUriInCustomTabs(getActivity(), url, true);
    }

    private void openSignInUrl() {
        if (getActivity() == null) {
            // Activity was disposed. Ignore this event.
            return;
        }
        final String url = mModel.repoUrl + GerritApi.LOGIN_URI;
        ActivityHelper.openUriInCustomTabs(getActivity(), url, true);
    }

    private void openHelp() {
        if (getActivity() == null) {
            // Activity was disposed. Ignore this event.
            return;
        }

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle(R.string.menu_help)
                .setMessage(R.string.account_wizard_account_page_help)
                .setPositiveButton(R.string.action_close, null)
                .create();
        dialog.show();
    }
}
