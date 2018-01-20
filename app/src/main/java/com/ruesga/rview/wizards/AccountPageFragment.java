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
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.PopupMenu;
import android.text.Editable;
import android.text.Html;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.ViewGroup;

import com.ruesga.rview.R;
import com.ruesga.rview.databinding.WizardAccountPageFragmentBinding;
import com.ruesga.rview.exceptions.NoActivityAttachedException;
import com.ruesga.rview.exceptions.UnsupportedServerVersionException;
import com.ruesga.rview.gerrit.Authorization;
import com.ruesga.rview.gerrit.GerritApi;
import com.ruesga.rview.gerrit.GerritServiceFactory;
import com.ruesga.rview.gerrit.NoConnectivityException;
import com.ruesga.rview.gerrit.model.AccountInfo;
import com.ruesga.rview.gerrit.model.ServerInfo;
import com.ruesga.rview.gerrit.model.ServerVersion;
import com.ruesga.rview.misc.ActivityHelper;
import com.ruesga.rview.misc.ExceptionHelper;
import com.ruesga.rview.misc.SerializationManager;
import com.ruesga.rview.model.Account;
import com.ruesga.rview.preferences.Constants;
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
import static com.ruesga.rview.misc.ExceptionHelper.isServerUnavailableException;

public class AccountPageFragment extends WizardPageFragment {

    private static final String TAG = "AccountPageFragment";

    public static final String STATE_ACCOUNT_ACCESS_MODE = "account.access.mode";
    public static final String STATE_ACCOUNT_USERNAME = "account.username";
    public static final String STATE_ACCOUNT_PASSWORD = "account.password";
    public static final String STATE_ACCOUNT_INFO = "account.info";
    private static final String STATE_ACCOUNT_CONFIRMED = "account.confirmed";
    public static final String STATE_REPO_NAME = "repo.name";
    public static final String STATE_REPO_URL = "repo.url";
    public static final String STATE_REPO_TRUST_ALL_CERTIFICATES = "repo.trustAllCertificates";
    public static final String STATE_SINGLE_PAGE = "page.single.page";
    public static final String STATE_AUTHENTICATION_FAILURE = "page.authentication.failure";
    public static final String STATE_GERRIT_VERSION = "gerrit.version";
    public static final String STATE_GERRIT_CONFIG = "gerrit.config";

    @Keep
    @SuppressWarnings("unused")
    public static class Model {
        public Spanned message;
        public String username;
        public String password;
        private AccountInfo accountInfo;
        public boolean authenticatedAccess;
        private String repoName;
        private String repoUrl;
        private boolean repoTrustAllCertificates;
        private boolean wasConfirmed;
        public boolean singlePage;
        private boolean authenticationFailure;

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

    @Keep
    @SuppressWarnings("unused")
    public static class EventHandlers {
        AccountPageFragment mFragment;
        public EventHandlers(AccountPageFragment fragment) {
            mFragment = fragment;
        }

        public void onClickPressed(View view) {
            if (view.getId() == R.id.access_mode_switcher_layout) {
                boolean checked = mFragment.mBinding.accessModeSwitcher.isChecked();
                mFragment.mBinding.accessModeSwitcher.setChecked(!checked);
                if (checked && mFragment.getActivity() != null) {
                    ((WizardActivity) mFragment.getActivity()).closeKeyboardIfNeeded();
                }
            }
        }
    }

    private WizardAccountPageFragmentBinding mBinding;
    private Model mModel = new Model();
    private EventHandlers mEventHandlers;
    private ServerVersion mServerVersion;
    private ServerInfo mServerInfo;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle state) {
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
            if (!isChecked && getActivity() != null) {
                ((WizardActivity) getActivity()).closeKeyboardIfNeeded();
            }

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

        bundle.putString(STATE_REPO_NAME, mModel.repoName);
        bundle.putString(STATE_REPO_URL, mModel.repoUrl);
        bundle.putBoolean(STATE_REPO_TRUST_ALL_CERTIFICATES, mModel.repoTrustAllCertificates);

        bundle.putBoolean(STATE_AUTHENTICATION_FAILURE, mModel.authenticationFailure);
        bundle.putBoolean(STATE_SINGLE_PAGE, mModel.singlePage);

        if (mServerVersion != null) {
            bundle.putString(STATE_GERRIT_VERSION,
                    SerializationManager.getInstance().toJson(mServerVersion));

        }
        if (mServerInfo != null) {
            bundle.putString(STATE_GERRIT_CONFIG,
                    SerializationManager.getInstance().toJson(mServerInfo));

        }
        return bundle;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void restoreState(Context context, Bundle savedState) {
        if (savedState.containsKey(STATE_AUTHENTICATION_FAILURE)) {
            mModel.authenticationFailure = mModel.singlePage =
                    savedState.getBoolean(STATE_AUTHENTICATION_FAILURE);
        } else if (savedState.containsKey(STATE_SINGLE_PAGE)) {
            mModel.singlePage = savedState.getBoolean(STATE_SINGLE_PAGE);
        }
        mModel.message = Html.fromHtml(context.getString(
                mModel.authenticationFailure
                        ? R.string.account_wizard_account_authentication_failure_page_message
                        : R.string.account_wizard_account_page_message,
                savedState.getString(STATE_REPO_NAME)));
        mModel.repoName = savedState.getString(STATE_REPO_NAME);
        mModel.repoUrl = savedState.getString(STATE_REPO_URL);
        mModel.repoTrustAllCertificates =
                savedState.getBoolean(STATE_REPO_TRUST_ALL_CERTIFICATES, false);
        mModel.authenticatedAccess = savedState.getBoolean(STATE_ACCOUNT_ACCESS_MODE, false);
        mModel.username = savedState.getString(STATE_ACCOUNT_USERNAME);
        mModel.password = savedState.getString(STATE_ACCOUNT_PASSWORD);
        if (savedState.containsKey(STATE_ACCOUNT_INFO)) {
            mModel.accountInfo = SerializationManager.getInstance().fromJson(
                    savedState.getString(STATE_ACCOUNT_INFO), AccountInfo.class);
        }
        if (savedState.containsKey(STATE_ACCOUNT_CONFIRMED)) {
            mModel.wasConfirmed = savedState.getBoolean(STATE_ACCOUNT_CONFIRMED);
        }

        String serverVersion = savedState.getString(STATE_GERRIT_VERSION);
        if (!TextUtils.isEmpty(serverVersion)) {
            mServerVersion = SerializationManager.getInstance().fromJson(
                    serverVersion, ServerVersion.class);
        }
        String serverConfig = savedState.getString(STATE_GERRIT_CONFIG);
        if (!TextUtils.isEmpty(serverConfig)) {
            mServerInfo = SerializationManager.getInstance().fromJson(
                    serverConfig, ServerInfo.class);
        }

        if (mBinding != null) {
            mBinding.setModel(mModel);
        }
    }

    @Override
    public int getPageTitle() {
        return mModel.authenticationFailure
                ? R.string.account_wizard_account_authentication_failure_page_title
                : R.string.account_wizard_account_page_title;
    }

    @Override
    public int getPageLayout() {
        return R.layout.wizard_account_page_fragment;
    }

    public boolean canRequireKeyboard() {
        return !mModel.singlePage;
    }

    @Override
    public boolean hasBackAction() {
        return !mModel.singlePage;
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
        return mModel.singlePage ? R.string.action_done : R.string.action_next;
    }

    @Override
    public int getForwardActionDrawable() {
        return mModel.singlePage ? 0 : R.drawable.ic_chevron_right;
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
            if (mModel.wasConfirmed && mModel.accountInfo != null) {
                return Boolean.TRUE;
            }
            try {
                if (!mModel.authenticatedAccess) {
                    String anonymousCowardName = getAnonymousCowardName();
                    if (anonymousCowardName == null) {
                        anonymousCowardName = getString(R.string.account_anonymous_coward);
                    }
                    mModel.accountInfo = new AccountInfo();
                    mModel.accountInfo.accountId = Account.ANONYMOUS_ACCOUNT_ID;
                    mModel.accountInfo.name = anonymousCowardName;
                    return checkServerVersion();
                }

                // Check if the activity is attached
                if (getActivity() == null) {
                    throw new NoActivityAttachedException();
                }

                // Login and obtain version. Also call getAnonymousCowardName to obtain
                // server config
                boolean ret = logIn() && checkServerVersion();
                try {
                    getAnonymousCowardName();
                } catch (Exception ex) {
                    // Ignore
                }
                return ret;
            } catch (Exception ex) {
                postUpdateErrorMessage(ex);
                mModel.wasConfirmed = false;
                throw  ex;
            }
        };
    }

    private boolean logIn() {
        //noinspection ConstantConditions
        Context ctx = getActivity().getApplicationContext();
        final String username = mModel.authenticatedAccess ? mModel.username : null;
        final String password = mModel.authenticatedAccess ? mModel.password : null;
        Authorization authorization = new Authorization(
                username, password, mModel.repoTrustAllCertificates);
        GerritApi client = GerritServiceFactory.getInstance(ctx, mModel.repoUrl, authorization);
        mModel.accountInfo = client.getAccount(GerritApi.SELF_ACCOUNT).blockingFirst();
        return mModel.accountInfo != null;
    }

    private boolean checkServerVersion() throws UnsupportedServerVersionException {
        //noinspection ConstantConditions
        Context ctx = getActivity().getApplicationContext();
        final String username = mModel.authenticatedAccess ? mModel.username : null;
        final String password = mModel.authenticatedAccess ? mModel.password : null;
        Authorization authorization = new Authorization(
                username, password, mModel.repoTrustAllCertificates);
        GerritApi client = GerritServiceFactory.getInstance(ctx, mModel.repoUrl, authorization);
        ServerVersion version = client.getServerVersion().blockingFirst();
        if (version.getVersion() < Constants.MINIMAL_SUPPORTED_VERSION) {
            throw new UnsupportedServerVersionException();
        }
        mServerVersion = version;
        return true;
    }

    private String getAnonymousCowardName() {
        try {
            //noinspection ConstantConditions
            Context ctx = getActivity().getApplicationContext();
            final String username = mModel.authenticatedAccess ? mModel.username : null;
            final String password = mModel.authenticatedAccess ? mModel.password : null;
            Authorization authorization = new Authorization(
                    username, password, mModel.repoTrustAllCertificates);
            GerritApi client = GerritServiceFactory.getInstance(ctx, mModel.repoUrl, authorization);
            ServerInfo serverInfo = client.getServerInfo().blockingFirst();
            mServerInfo = serverInfo;
            return serverInfo.user.anonymousCowardName;
        } catch (Exception ex) {
            if (ExceptionHelper.isAuthenticationException(ex)) {
                // Unauthorized. We can't access this gerrit instance in anonymous mode
                throw ex;
            }
            Log.w(TAG, "Gerrit repository doesn't provided server configuration.");
        }
        return null;
    }

    private void postUpdateErrorMessage(final Exception cause) {
        if (mBinding != null) {
            mBinding.accountUsername.post(() -> {
                // Just ignore it if we don't have a valid context
                if (!(cause instanceof NoActivityAttachedException)) {
                    final Context context = mBinding.accountUsername.getContext();
                    if (isException(cause, UnsupportedServerVersionException.class)) {
                        //noinspection ConstantConditions
                        ((WizardActivity) getActivity()).showMessage(context.getString(
                                R.string.exception_unsupported_server_version));
                    } else if (isException(cause, NoConnectivityException.class)) {
                        //noinspection ConstantConditions
                        ((WizardActivity) getActivity()).showMessage(context.getString(
                                R.string.exception_no_network_available));
                    } else if (isServerUnavailableException(cause)) {
                        //noinspection ConstantConditions
                        ((WizardActivity) getActivity()).showMessage(context.getString(
                                R.string.exception_server_cannot_be_reached));

                    } else {
                        if (!mModel.authenticatedAccess &&
                                ExceptionHelper.isAuthenticationException(cause)) {
                            Log.e(TAG, "Server request authentication access", cause);
                            //noinspection ConstantConditions
                            ((WizardActivity) getActivity()).showMessage(context.getString(
                                    R.string.exception_unsupported_guess_mode));
                        } else if (!mModel.authenticatedAccess &&
                                ExceptionHelper.isResourceNotFoundException(cause)) {
                            Log.e(TAG, "Server request not found", cause);
                            //noinspection ConstantConditions
                            ((WizardActivity) getActivity()).showMessage(context.getString(
                                    R.string.exception_invalid_endpoint));
                        } else {
                            Log.e(TAG, "Invalid user or password", cause);
                            mBinding.accountUsername.setError(context.getString(
                                    R.string.exception_invalid_user_password));
                        }
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

        //noinspection ConstantConditions
        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle(R.string.menu_help)
                .setMessage(R.string.account_wizard_account_page_help)
                .setPositiveButton(R.string.action_close, null)
                .create();
        dialog.show();
    }
}
