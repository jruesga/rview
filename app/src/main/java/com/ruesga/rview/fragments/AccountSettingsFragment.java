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

import android.os.Bundle;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.Preference.OnPreferenceClickListener;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.TwoStatePreference;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.firebase.iid.FirebaseInstanceId;
import com.ruesga.rview.R;
import com.ruesga.rview.attachments.AttachmentsProviderFactory;
import com.ruesga.rview.gerrit.GerritApi;
import com.ruesga.rview.gerrit.model.CloudNotificationsConfigInfo;
import com.ruesga.rview.gerrit.model.Features;
import com.ruesga.rview.misc.ActivityHelper;
import com.ruesga.rview.misc.ModelHelper;
import com.ruesga.rview.model.Account;
import com.ruesga.rview.model.CustomFilter;
import com.ruesga.rview.model.Repository;
import com.ruesga.rview.preferences.Constants;
import com.ruesga.rview.preferences.Preferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import me.tatarka.rxloader2.RxLoader;
import me.tatarka.rxloader2.RxLoaderManager;
import me.tatarka.rxloader2.RxLoaderManagerCompat;
import me.tatarka.rxloader2.RxLoaderObserver;
import me.tatarka.rxloader2.safe.SafeObservable;

import static com.ruesga.rview.preferences.Constants.PREF_ACCOUNT_ATTACHMENTS_CATEGORY;
import static com.ruesga.rview.preferences.Constants.PREF_ACCOUNT_CI_CATEGORY;
import static com.ruesga.rview.preferences.Constants.PREF_ACCOUNT_DASHBOARD_CATEGORY;
import static com.ruesga.rview.preferences.Constants.PREF_ACCOUNT_DISPLAY_CATEGORY;
import static com.ruesga.rview.preferences.Constants.PREF_ACCOUNT_DISPLAY_STATUSES;
import static com.ruesga.rview.preferences.Constants.PREF_ACCOUNT_EXTERNAL_CATEGORY;
import static com.ruesga.rview.preferences.Constants.PREF_ACCOUNT_FETCHED_ITEMS;
import static com.ruesga.rview.preferences.Constants.PREF_ACCOUNT_HANDLE_LINKS;
import static com.ruesga.rview.preferences.Constants.PREF_ACCOUNT_HOME_PAGE;
import static com.ruesga.rview.preferences.Constants.PREF_ACCOUNT_MESSAGES_CATEGORY;
import static com.ruesga.rview.preferences.Constants.PREF_ACCOUNT_NOTIFICATIONS_ADVISE;
import static com.ruesga.rview.preferences.Constants.PREF_ACCOUNT_NOTIFICATIONS_CATEGORY;
import static com.ruesga.rview.preferences.Constants.PREF_ACCOUNT_SEARCH_CLEAR;
import static com.ruesga.rview.preferences.Constants.PREF_ACCOUNT_TOGGLE_TAGGED_MESSAGES;
import static com.ruesga.rview.preferences.Constants.PREF_ACCOUNT_TOGGLE_CI_MESSAGES;

public class AccountSettingsFragment extends PreferenceFragmentCompat
        implements OnPreferenceChangeListener, OnPreferenceClickListener {

    public static AccountSettingsFragment newInstance() {
        return new AccountSettingsFragment();
    }

    private final RxLoaderObserver<CloudNotificationsConfigInfo> mNotificationsSupportObserver
            = new RxLoaderObserver<CloudNotificationsConfigInfo>() {
        @Override
        public void onNext(CloudNotificationsConfigInfo config) {
            if (config != null) {
                mAccount.mNotificationsSenderId = config.senderId;
                Preferences.addOrUpdateAccount(getContext(), mAccount);
                Preferences.setAccount(getContext(), mAccount);
                enableNotificationsSupport();
            }

            mNotificationsSupportLoader.clear();
        }
    };

    private Account mAccount;

    private ListPreference mHomePage;
    private Preference mSearchHistoryClear;
    private PreferenceCategory mNotificationsCategory;
    private Preference mNotificationsAdvise;
    private Preference mNotificationsEnabled;
    private Preference mNotificationsEvents;
    private TwoStatePreference mHandleLinks;

    private RxLoader<CloudNotificationsConfigInfo> mNotificationsSupportLoader;

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference.equals(mHomePage)) {
            updateHomePageSummary((String) newValue);
        } else if (preference.equals(mHandleLinks)) {
            ModelHelper.setAccountUrlHandlingStatus(getContext(), mAccount, (boolean) newValue);
        }
        return true;
}

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference.equals(mNotificationsAdvise)) {
            ActivityHelper.openUriInCustomTabs(
                    getActivity(), getString(R.string.link_cloud_notifications_plugin));
            return true;
        } else if (preference.equals(mSearchHistoryClear)) {
            Preferences.clearAccountSearchHistory(getContext(), mAccount);
            mSearchHistoryClear.setEnabled(false);
            Toast.makeText(getContext(), R.string.account_settings_search_clear_message,
                    Toast.LENGTH_SHORT).show();
            return true;
        }
        return false;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Remove the divider
        View v = super.onCreateView(inflater, container, savedInstanceState);
        setDivider(null);
        return v;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        mAccount = Preferences.getAccount(getContext());
        getPreferenceManager().setSharedPreferencesName(
                Preferences.getAccountPreferencesName(mAccount));

        setPreferencesFromResource(R.xml.account_preferences, rootKey);

        configureHomePage();
        configureDisplayStatuses();
        configureToggleTagged();
        configureToggleCI();
        configureSearch();
        configureFetchItems();
        configureDashboard();
        configureNotifications();
        configureHandleLinks();
        configureAttachments();
        configureContinuousIntegration();
    }

    private void configureHomePage() {
        String[] namesArray = getResources().getStringArray(R.array.query_filters_ids_names);
        String[] titlesArray = getResources().getStringArray(R.array.query_filters_ids_titles);
        String[] authArray = getResources().getStringArray(R.array.query_filters_auth);

        boolean authenticated = mAccount.hasAuthenticatedAccessMode();
        List<String> names = new ArrayList<>();
        List<String> titles = new ArrayList<>();
        int i = 0;
        for (String auth : authArray) {
            if (authenticated || !Boolean.valueOf(auth)) {
                names.add(namesArray[i]);
                titles.add(titlesArray[i]);
                i++;
            }
        }

        List<CustomFilter> filters = Preferences.getAccountCustomFilters(getContext(), mAccount);
        if (filters != null) {
            for (CustomFilter filter : filters) {
                names.add(Constants.CUSTOM_FILTER_PREFIX + filter.mId);
                titles.add(filter.mName);
            }
        }

        // Add the entries y values
        mHomePage = (ListPreference) findPreference(PREF_ACCOUNT_HOME_PAGE);
        mHomePage.setEntries(titles.toArray(new String[titles.size()]));
        mHomePage.setEntryValues(names.toArray(new String[names.size()]));
        mHomePage.setDefaultValue(Preferences.getDefaultHomePageForAccount(mAccount));
        String value = Preferences.getAccountHomePage(getContext(), mAccount);
        if (!names.contains(value)) {
            value = Preferences.getDefaultHomePageForAccount(mAccount);
        }
        mHomePage.setValue(value);
        updateHomePageSummary(mHomePage.getValue());
        mHomePage.setOnPreferenceChangeListener(this);
    }

    @SuppressWarnings("ConstantConditions")
    private void configureDisplayStatuses() {
        PreferenceCategory displayCategory =
                (PreferenceCategory) findPreference(PREF_ACCOUNT_DISPLAY_CATEGORY);
        Preference displayStatuses = findPreference(PREF_ACCOUNT_DISPLAY_STATUSES);
        boolean supportAccountStatus = ModelHelper.getGerritApi(getActivity()).supportsFeature(
                Features.ACCOUNT_STATUS, mAccount.mServerVersion);
        if (!supportAccountStatus) {
            displayCategory.removePreference(displayStatuses);
        }
    }

    private void configureToggleTagged() {
        PreferenceCategory messagesCategory =
                (PreferenceCategory) findPreference(PREF_ACCOUNT_MESSAGES_CATEGORY);
        Preference toggleTagged = findPreference(PREF_ACCOUNT_TOGGLE_TAGGED_MESSAGES);
        if (toggleTagged != null) {
            final GerritApi api = ModelHelper.getGerritApi(getActivity());
            boolean supportTaggedMessages = api != null
                    && api.supportsFeature(Features.TAGGED_MESSAGES);
            if (!supportTaggedMessages) {
                messagesCategory.removePreference(toggleTagged);
            }
        }
    }

    private void configureToggleCI() {
        PreferenceCategory messagesCategory =
                (PreferenceCategory) findPreference(PREF_ACCOUNT_MESSAGES_CATEGORY);
        Preference toggleCI = findPreference(PREF_ACCOUNT_TOGGLE_CI_MESSAGES);
        if (toggleCI != null) {
            Repository repository = ModelHelper.findRepositoryForAccount(getContext(), mAccount);
            if (repository == null || TextUtils.isEmpty(repository.mCiAccounts)) {
                messagesCategory.removePreference(toggleCI);
            }
        }
    }

    private void configureSearch() {
        mSearchHistoryClear = findPreference(PREF_ACCOUNT_SEARCH_CLEAR);
        mSearchHistoryClear.setOnPreferenceClickListener(this);
        mSearchHistoryClear.setEnabled(Preferences.hasAccountSearchHistory(getContext(), mAccount));
    }

    private void configureFetchItems() {
        ListPreference fetchedItems = (ListPreference) findPreference(PREF_ACCOUNT_FETCHED_ITEMS);
        CharSequence[] values = fetchedItems.getEntryValues();
        int count = values.length;
        CharSequence[] labels = new CharSequence[count];
        for (int i = 0; i < count; i++) {
            labels[i] = getString(R.string.account_settings_fetched_items_format, values[i]);
        }
        fetchedItems.setEntries(labels);
    }

    private void updateHomePageSummary(String value) {
        int index = Arrays.asList(mHomePage.getEntryValues()).indexOf(value);
        mHomePage.setSummary(mHomePage.getEntries()[index]);
    }

    private void configureDashboard() {
        if (mAccount == null || !mAccount.hasAuthenticatedAccessMode()) {
            PreferenceCategory dashboardCategory = (PreferenceCategory) findPreference(PREF_ACCOUNT_DASHBOARD_CATEGORY);
            getPreferenceScreen().removePreference(dashboardCategory);
        }
    }

    private void configureNotifications() {
        // Fetch or join current loader
        if (mNotificationsSupportLoader == null) {
            RxLoaderManager loaderManager = RxLoaderManagerCompat.get(this);
            mNotificationsSupportLoader = loaderManager.create(
                    checkNotificationsSupport(), mNotificationsSupportObserver);
        }

        mNotificationsCategory =
                (PreferenceCategory) findPreference(PREF_ACCOUNT_NOTIFICATIONS_CATEGORY);
        mNotificationsAdvise =  findPreference(PREF_ACCOUNT_NOTIFICATIONS_ADVISE);
        mNotificationsAdvise.setOnPreferenceClickListener(this);
        mNotificationsEnabled =  findPreference(Constants.PREF_ACCOUNT_NOTIFICATIONS);
        mNotificationsEvents =  findPreference(Constants.PREF_ACCOUNT_NOTIFICATIONS_EVENTS);

        if (!mAccount.hasAuthenticatedAccessMode()) {
            if (mNotificationsCategory != null) {
                getPreferenceScreen().removePreference(mNotificationsCategory);
                mNotificationsCategory = null;
            }
        } else if (mAccount.hasNotificationsSupport()) {
            enableNotificationsSupport();
        } else {
            // Check notification support to server
            mNotificationsSupportLoader.restart();
        }
    }

    private void configureHandleLinks() {
        PreferenceCategory category =
                (PreferenceCategory) findPreference(PREF_ACCOUNT_EXTERNAL_CATEGORY);
        mHandleLinks = (TwoStatePreference) findPreference(PREF_ACCOUNT_HANDLE_LINKS);
        if (!ModelHelper.canAccountHandleUrls(getContext(), mAccount)) {
            if (mHandleLinks != null) {
                category.removePreference(mHandleLinks);
                mHandleLinks = null;
            }
        } else {
            mHandleLinks.setChecked(Preferences.isAccountHandleLinks(getContext(), mAccount)
                    && ModelHelper.isAccountUrlHandlingEnabled(getContext(), mAccount));
            mHandleLinks.setOnPreferenceChangeListener(this);
        }
    }

    private void configureAttachments() {
        PreferenceCategory category =
                (PreferenceCategory) findPreference(PREF_ACCOUNT_ATTACHMENTS_CATEGORY);
        if (category != null &&
                AttachmentsProviderFactory.getAllAvailableAttachmentProviders().size() == 0) {
            getPreferenceScreen().removePreference(category);
        }
    }

    private void configureContinuousIntegration() {
        PreferenceCategory category =
                (PreferenceCategory) findPreference(PREF_ACCOUNT_CI_CATEGORY);
        if (category != null) {
            Repository repository = ModelHelper.findRepositoryForAccount(getActivity(), mAccount);
            if (repository == null || TextUtils.isEmpty(repository.mCiAccounts)) {
                getPreferenceScreen().removePreference(category);
            }
        }
    }

    @SuppressWarnings("ConstantConditions")
    private Observable<CloudNotificationsConfigInfo> checkNotificationsSupport() {
        final GerritApi api = ModelHelper.getGerritApi(getContext());
        return SafeObservable.fromNullCallable(() -> {
                String device = FirebaseInstanceId.getInstance().getToken();
                if (TextUtils.isEmpty(device)) {
                    // Just use a default one. We don't need a real device to test
                    // notifications support.
                    device = "test";
                }
                return api.getCloudNotificationsConfig().blockingFirst();
            })
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread());
    }

    private void enableNotificationsSupport() {
        if (mNotificationsAdvise != null) {
            mNotificationsCategory.removePreference(mNotificationsAdvise);
            mNotificationsAdvise = null;
        }
        mNotificationsEnabled.setEnabled(true);
        mNotificationsEvents.setEnabled(true);
    }
}
