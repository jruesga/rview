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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Keep;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.text.Editable;
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
import com.ruesga.rview.databinding.WizardRepositoryPageFragmentBinding;
import com.ruesga.rview.exceptions.NoActivityAttachedException;
import com.ruesga.rview.exceptions.UnsupportedServerVersionException;
import com.ruesga.rview.gerrit.GerritApi;
import com.ruesga.rview.gerrit.GerritServiceFactory;
import com.ruesga.rview.gerrit.NoConnectivityException;
import com.ruesga.rview.gerrit.model.ServerVersion;
import com.ruesga.rview.misc.ExceptionHelper;
import com.ruesga.rview.model.Repository;
import com.ruesga.rview.preferences.Constants;
import com.ruesga.rview.wizard.WizardActivity;
import com.ruesga.rview.wizard.WizardChooserFragment;
import com.ruesga.rview.wizard.WizardPageFragment;
import com.ruesga.rview.wizard.misc.TextChangedWatcher;
import com.ruesga.rview.wizard.validators.NonEmptyTextValidator;
import com.ruesga.rview.wizard.validators.TextInputValidatorObserver;
import com.ruesga.rview.wizard.validators.Validator;
import com.ruesga.rview.wizard.validators.WebUrlValidator;

import java.util.Locale;
import java.util.concurrent.Callable;

import static com.ruesga.rview.misc.ExceptionHelper.isException;

public class RepositoryPageFragment extends WizardPageFragment {

    private static final String TAG = "RepositoryPageFragment";

    public static final String STATE_REPO_NAME = "repo.name";
    public static final String STATE_REPO_URL = "repo.url";
    public static final String STATE_REPO_TRUST_ALL_CERTIFICATES = "repo.trustAllCertificates";
    private static final String STATE_REPO_CONFIRMED_URL = "repo.confirmed.url";

    @Keep
    @SuppressWarnings("unused")
    public static class Model {
        public String name;
        public String url;
        public boolean trustAllCertificates;
        public String urlConfirmed;
        public boolean wasConfirmed;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public boolean getTrustAllCertificates() {
            return trustAllCertificates;
        }

        public void setTrustAllCertificates(boolean trustAllCertificates) {
            this.trustAllCertificates = trustAllCertificates;
        }
    }

    private WizardRepositoryPageFragmentBinding mBinding;
    private Model mModel = new Model();

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
        final Context context = inflater.getContext();
        mBinding = DataBindingUtil.inflate(inflater, getPageLayout(), container, false);
        setValidatorsForView(mBinding.repositoryNameEdit, new TextInputValidatorObserver(this),
                new NonEmptyTextValidator(context));
        setValidatorsForView(mBinding.repositoryUrlEdit, new TextInputValidatorObserver(this),
                new NonEmptyTextValidator(context), new WebUrlValidator(context));
        bindPredefinedRepositoriesLink();
        mBinding.setModel(mModel);
        mBinding.repositoryUrlEdit.addTextChangedListener(new TextChangedWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                String url = s.toString();
                mModel.wasConfirmed = mModel.urlConfirmed != null &&
                        !mModel.urlConfirmed.isEmpty() && url.equals(mModel.urlConfirmed);
            }
        });
        mBinding.repositoryTrustAllCertificates.setOnCheckedChangeListener(
                (compoundButton, fromUser) -> {
            if (fromUser) {
                mModel.wasConfirmed = false;
            }
        });
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
        bundle.putString(STATE_REPO_NAME, mModel.name);
        bundle.putString(STATE_REPO_URL, mModel.url);
        bundle.putBoolean(STATE_REPO_TRUST_ALL_CERTIFICATES, mModel.trustAllCertificates);
        bundle.putString(STATE_REPO_CONFIRMED_URL, mModel.urlConfirmed);
        return bundle;
    }

    @Override
    public void restoreState(Context context, Bundle savedState) {
        mModel.name = savedState.getString(STATE_REPO_NAME);
        mModel.url = savedState.getString(STATE_REPO_URL);
        mModel.trustAllCertificates = savedState.getBoolean(STATE_REPO_TRUST_ALL_CERTIFICATES, false);
        mModel.urlConfirmed = savedState.getString(STATE_REPO_CONFIRMED_URL);
        mModel.wasConfirmed = mModel.url != null && mModel.urlConfirmed != null
                && !mModel.urlConfirmed.isEmpty() && mModel.url.equals(mModel.urlConfirmed);
        if (mBinding != null) {
            mBinding.setModel(mModel);
        }
    }

    @Override
    public int getPageTitle() {
        return R.string.account_wizard_repository_page_title;
    }

    @Override
    public int getPageLayout() {
        return R.layout.wizard_repository_page_fragment;
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
    public Callable<Boolean> doForwardAction() {
        // Check if the url passed is a valid Gerrit endpoint
        return () -> {
            if (mModel.wasConfirmed && !TextUtils.isEmpty(mModel.name)
                    && !TextUtils.isEmpty(mModel.url)) {
                return Boolean.TRUE;
            }

            try {
                // Check if the activity is attached
                if (getActivity() == null) {
                    throw new NoActivityAttachedException();
                }
                return checkServerVersion();
            } catch (Exception ex) {
                if (ExceptionHelper.isAuthenticationException(ex)) {
                    // Unauthorized. We will check the access later
                    return Boolean.TRUE;
                }
                postUpdateErrorMessage(ex);
                mModel.urlConfirmed = null;
                mModel.wasConfirmed = false;
                throw  ex;
            }
        };
    }

    private boolean checkServerVersion() throws Exception {
        Context ctx = getActivity().getApplicationContext();
        ServerVersion version;
        String endpoint;
        if (!mModel.url.toLowerCase().startsWith("http://")
                && !mModel.url.toLowerCase().startsWith("https://")) {
            // Test first with HTTPS and HTTP
            Log.i(TAG, "Gerrit endpoint \"" + mModel.url + "\" doesn't provide any"
                    + " schema information. Trying HTTPS and HTTP endpoints.");
            try {
                endpoint = "https://" + mModel.url;
                Log.i(TAG, "Trying to resolve Gerrit repository: " + endpoint);
                GerritApi client = GerritServiceFactory.getInstance(ctx, endpoint);
                version = client.getServerVersion().blockingFirst();
                mModel.url = endpoint;
            } catch (Exception ex) {
                endpoint = "http://" + mModel.url;
                Log.i(TAG, "Trying to resolve Gerrit repository: " + endpoint);
                GerritApi client = GerritServiceFactory.getInstance(ctx, endpoint);
                version = client.getServerVersion().blockingFirst();
                mModel.url = endpoint;
            }
        } else {
            // Use the provided endpoint
            endpoint = mModel.url;
            Log.i(TAG, "Trying to resolve Gerrit repository: " + endpoint);
            GerritApi client = GerritServiceFactory.getInstance(ctx, endpoint);
            version = client.getServerVersion().blockingFirst();
        }
        Log.i(TAG, "Gerrit repository resolved. Server version " + version);

        if (version.getVersion() >= Constants.MINIMAL_SUPPORTED_VERSION) {
            mModel.url = endpoint;
            mModel.urlConfirmed = mModel.url;
            mModel.wasConfirmed = true;
            return true;
        }
        throw new UnsupportedServerVersionException();
    }

    private void postUpdateErrorMessage(final Exception cause) {
        if (mBinding != null) {
            mBinding.repositoryUrl.post(() -> {
                final Context context = mBinding.repositoryUrl.getContext();
                if (isException(cause, UnsupportedServerVersionException.class)) {
                    Log.w(TAG, "Gerrit server is unsupported");
                    mBinding.repositoryUrl.setError(context.getString(
                            R.string.exception_unsupported_server_version));
                } else if (isException(cause, NoConnectivityException.class)) {
                    ((WizardActivity) getActivity()).showMessage(context.getString(
                            R.string.exception_no_network_available));
                } else {
                    // Just ignore it if we don't have a valid context
                    if (!(cause instanceof NoActivityAttachedException)) {
                        Log.e(TAG, "Gerrit repository not resolved", cause);
                        mBinding.repositoryUrl.setError(context.getString(
                                R.string.exception_invalid_endpoint));
                    }
                }
            });
        }
    }

    private void bindPredefinedRepositoriesLink() {
        mBinding.repositoryPredefined.setMovementMethod(new LinkMovementMethod());
        String msg = getString(R.string.account_wizard_repository_page_message2);
        String link = getString(R.string.account_wizard_repository_page_message2_predefined);
        String text = String.format(Locale.getDefault(), msg, link);
        int pos = msg.indexOf("%1$s");

        // Create a clickable span
        Spannable span = Spannable.Factory.getInstance().newSpannable(text);
        if (pos >= 0) {
            span.setSpan(new ClickableSpan() {
                @Override
                public void onClick(View v) {
                    // Click on span doesn't provide sound feedback it the text view doesn't
                    // handle a click event. Just perform a click effect.
                    v.playSoundEffect(SoundEffectConstants.CLICK);
                    performOpenPredefinedRepositories();
                }
            }, pos, pos + link.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        mBinding.repositoryPredefined.setText(span);
    }

    public void onChooserResult(int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && data != null) {
            Repository repository = data.getParcelableExtra(
                    PredefinedRepositoriesChooserFragment.EXTRA_REPOSITORY);
            if (repository == null) {
                return;
            }
            mModel.name = repository.mName;
            mModel.url = repository.mUrl;
            mModel.trustAllCertificates = repository.mTrustAllCertificates;
            mModel.urlConfirmed = repository.mUrl;
            mModel.wasConfirmed = true;
            if (mBinding != null) {
                mBinding.setModel(mModel);
            }
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
            case R.id.repository_name_edit:
                layout = mBinding.repositoryName;
                break;
            case R.id.repository_url_edit:
                layout = mBinding.repositoryUrl;
                break;
        }
        return layout;
    }

    private void performOpenPredefinedRepositories() {
        openChooserPage(WizardChooserFragment.newInstance(
                PredefinedRepositoriesChooserFragment.class));
    }
}
