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
import android.support.v7.preference.PreferenceFragmentCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ruesga.rview.R;
import com.ruesga.rview.model.Account;
import com.ruesga.rview.model.CustomFilter;
import com.ruesga.rview.preferences.Constants;
import com.ruesga.rview.preferences.Preferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.ruesga.rview.preferences.Constants.PREF_ACCOUNT_FETCHED_ITEMS;
import static com.ruesga.rview.preferences.Constants.PREF_ACCOUNT_HOME_PAGE;

public class AccountSettingsFragment extends PreferenceFragmentCompat
        implements OnPreferenceChangeListener {

    public static AccountSettingsFragment newInstance() {
        return new AccountSettingsFragment();
    }

    private ListPreference mHomePage;

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference.equals(mHomePage)) {
            updateHomePageSummary((String) newValue);
        }
        return true;
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
        Account account = Preferences.getAccount(getContext());
        getPreferenceManager().setSharedPreferencesName(
                Preferences.getAccountPreferencesName(account));

        setPreferencesFromResource(R.xml.account_preferences, rootKey);

        configureHomePage(account);
        configureFetchItems();
    }

    private void configureHomePage(Account account) {
        String[] namesArray = getResources().getStringArray(R.array.query_filters_ids_names);
        String[] titlesArray = getResources().getStringArray(R.array.query_filters_ids_titles);
        String[] authArray = getResources().getStringArray(R.array.query_filters_auth);

        boolean authenticated = account.hasAuthenticatedAccessMode();
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

        List<CustomFilter> filters = Preferences.getAccountCustomFilters(getContext(), account);
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
        mHomePage.setDefaultValue(Preferences.getDefaultHomePageForAccount(account));
        String value = Preferences.getAccountHomePage(getContext(), account);
        if (!names.contains(value)) {
            value = Preferences.getDefaultHomePageForAccount(account);
        }
        mHomePage.setValue(value);
        updateHomePageSummary(mHomePage.getValue());
        mHomePage.setOnPreferenceChangeListener(this);
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
}
