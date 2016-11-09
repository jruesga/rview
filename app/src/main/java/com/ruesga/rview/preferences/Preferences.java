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
package com.ruesga.rview.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.support.annotation.NonNull;

import com.google.gson.Gson;
import com.ruesga.rview.R;
import com.ruesga.rview.gerrit.model.DownloadFormat;
import com.ruesga.rview.misc.SerializationManager;
import com.ruesga.rview.model.Account;
import com.ruesga.rview.model.CustomFilter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.ruesga.rview.preferences.Constants.DEFAULT_ANONYMOUS_HOME;
import static com.ruesga.rview.preferences.Constants.DEFAULT_AUTHENTICATED_HOME;
import static com.ruesga.rview.preferences.Constants.DEFAULT_DISPLAY_FORMAT;
import static com.ruesga.rview.preferences.Constants.DEFAULT_FETCHED_ITEMS;
import static com.ruesga.rview.preferences.Constants.MY_FILTERS_GROUP_BASE_ID;
import static com.ruesga.rview.preferences.Constants.PREF_ACCOUNT;
import static com.ruesga.rview.preferences.Constants.PREF_ACCOUNTS;
import static com.ruesga.rview.preferences.Constants.PREF_ACCOUNT_CUSTOM_FILTERS;
import static com.ruesga.rview.preferences.Constants.PREF_ACCOUNT_DIFF_MODE;
import static com.ruesga.rview.preferences.Constants.PREF_ACCOUNT_DISPLAY_FORMAT;
import static com.ruesga.rview.preferences.Constants.PREF_ACCOUNT_DOWNLOAD_FORMAT;
import static com.ruesga.rview.preferences.Constants.PREF_ACCOUNT_FETCHED_ITEMS;
import static com.ruesga.rview.preferences.Constants.PREF_ACCOUNT_HIGHLIGHT_INTRALINE_DIFFS;
import static com.ruesga.rview.preferences.Constants.PREF_ACCOUNT_HIGHLIGHT_TABS;
import static com.ruesga.rview.preferences.Constants.PREF_ACCOUNT_HIGHLIGHT_TRAILING_WHITESPACES;
import static com.ruesga.rview.preferences.Constants.PREF_ACCOUNT_HIGHLIGHT_UNREVIEWED;
import static com.ruesga.rview.preferences.Constants.PREF_ACCOUNT_HOME_PAGE;
import static com.ruesga.rview.preferences.Constants.PREF_ACCOUNT_INLINE_COMMENT_IN_MESSAGES;
import static com.ruesga.rview.preferences.Constants.PREF_ACCOUNT_MESSAGES_FOLDED;
import static com.ruesga.rview.preferences.Constants.PREF_ACCOUNT_SEARCH_MODE;
import static com.ruesga.rview.preferences.Constants.PREF_ACCOUNT_USE_CUSTOM_TABS;
import static com.ruesga.rview.preferences.Constants.PREF_ACCOUNT_WRAP_MODE;
import static com.ruesga.rview.preferences.Constants.PREF_ACCOUNT_TEXT_SIZE_FACTOR;
import static com.ruesga.rview.preferences.Constants.PREF_IS_FIRST_RUN;

public class Preferences {

    private static String getPreferencesName(Context context) {
        return context.getPackageName();
    }

    private static SharedPreferences getPreferences(Context context) {
        return context.getSharedPreferences(getPreferencesName(context), Context.MODE_PRIVATE);
    }

    public static String getAccountPreferencesName(Account account) {
        return account.getAccountHash();
    }

    private static SharedPreferences getAccountPreferences(Context context, Account account) {
        return context.getSharedPreferences(getAccountPreferencesName(account), Context.MODE_PRIVATE);
    }

    public static String getDefaultHomePageForAccount(Account account) {
        if (account == null) {
            return DEFAULT_ANONYMOUS_HOME;
        }
        return account.hasAuthenticatedAccessMode() ?
                DEFAULT_AUTHENTICATED_HOME : DEFAULT_ANONYMOUS_HOME;
    }

    public static boolean isFirstRun(Context context) {
        return getPreferences(context).getBoolean(PREF_IS_FIRST_RUN, true);
    }

    public static void setFirstRun(Context context) {
        Editor editor = getPreferences(context).edit();
        editor.putBoolean(PREF_IS_FIRST_RUN, false);
        editor.apply();
    }

    public static Account getAccount(Context context) {
        final Gson gson = SerializationManager.getInstance();
        String value = getPreferences(context).getString(PREF_ACCOUNT, null);
        if (value == null) {
            return null;
        }
        return gson.fromJson(value, Account.class);
    }

    public static void setAccount(Context context, Account account) {
        final Gson gson = SerializationManager.getInstance();
        Editor editor = getPreferences(context).edit();
        if (account != null) {
            editor.putString(PREF_ACCOUNT, gson.toJson(account));
        } else {
            editor.remove(PREF_ACCOUNT);
        }
        editor.apply();
    }

    public static List<Account> getAccounts(Context context) {
        final Gson gson = SerializationManager.getInstance();
        Set<String> set = getPreferences(context).getStringSet(PREF_ACCOUNTS, null);
        List<Account> accounts = new ArrayList<>();
        if (set != null) {
            for (String s : set) {
                accounts.add(gson.fromJson(s, Account.class));
            }
            Collections.sort(accounts);
        }
        return accounts;
    }

    public static List<Account> addAccount(Context context, @NonNull Account account) {
        List<Account> accounts = getAccounts(context);
        accounts.add(account);
        saveAccounts(context, accounts);
        return accounts;
    }

    public static List<Account>  removeAccount(Context context, @NonNull Account account) {
        List<Account> accounts = getAccounts(context);
        accounts.remove(account);
        saveAccounts(context, accounts);
        return accounts;
    }

    @SuppressWarnings("Convert2streamapi")
    private static void saveAccounts(Context context, List<Account> accounts) {
        final Gson gson = SerializationManager.getInstance();
        Set<String> set = new HashSet<>();
        for (Account acct : accounts) {
            set.add(gson.toJson(acct));
        }

        Editor editor = getPreferences(context).edit();
        editor.putStringSet(PREF_ACCOUNTS, set);
        editor.apply();
    }

    public static void removeAccountPreferences(Context context, Account account) {
        Editor editor = getAccountPreferences(context, account).edit();
        editor.clear();
        editor.apply();
    }

    public static String getAccountHomePage(Context context, Account account) {
        if (account == null) {
            return getDefaultHomePageForAccount(null);
        }
        return getAccountPreferences(context, account).getString(PREF_ACCOUNT_HOME_PAGE,
                getDefaultHomePageForAccount(account));
    }

    public static int getAccountHomePageId(Context context, Account account) {
        if (account == null) {
            return context.getResources().getIdentifier(Constants.DEFAULT_ANONYMOUS_HOME,
                    "id", context.getPackageName());
        }

        String homePage = getAccountHomePage(context, account);
        if (homePage.startsWith(Constants.CUSTOM_FILTER_PREFIX)) {
            String id = homePage.substring(Constants.CUSTOM_FILTER_PREFIX.length());
            List<CustomFilter> filters = Preferences.getAccountCustomFilters(context, account);
            if (filters != null) {
                int i = 0;
                for (CustomFilter filter : filters) {
                    if (filter.mId.equals(id)) {
                        return MY_FILTERS_GROUP_BASE_ID + i;
                    }
                    i++;
                }
            }
        }
        int resId = context.getResources().getIdentifier(homePage, "id", context.getPackageName());
        if (resId == 0) {
            return context.getResources().getIdentifier(getDefaultHomePageForAccount(account),
                    "id", context.getPackageName());
        }
        return resId;
    }

    public static int getAccountFetchedItems(Context context, Account account) {
        if (account == null) {
            return Integer.valueOf(DEFAULT_FETCHED_ITEMS);
        }
        return Integer.valueOf(getAccountPreferences(context, account).getString(
                PREF_ACCOUNT_FETCHED_ITEMS, DEFAULT_FETCHED_ITEMS));
    }

    public static String getAccountDisplayFormat(Context context, Account account) {
        if (account == null) {
            return DEFAULT_DISPLAY_FORMAT;
        }
        return getAccountPreferences(context, account).getString(
                PREF_ACCOUNT_DISPLAY_FORMAT, DEFAULT_DISPLAY_FORMAT);
    }

    public static boolean isAccountHighlightUnreviewed(Context context, Account account) {
        return account == null || getAccountPreferences(context, account)
                .getBoolean(PREF_ACCOUNT_HIGHLIGHT_UNREVIEWED, true);
    }

    public static boolean isAccountUseCustomTabs(Context context, Account account) {
        return account == null || getAccountPreferences(context, account)
                .getBoolean(PREF_ACCOUNT_USE_CUSTOM_TABS, true);
    }

    public static DownloadFormat getAccountDownloadFormat(Context context, Account account) {
        if (account == null) {
            return DownloadFormat.TBZ2;
        }
        return DownloadFormat.valueOf(
                getAccountPreferences(context, account).getString(
                        PREF_ACCOUNT_DOWNLOAD_FORMAT, DownloadFormat.TBZ2.toString()));
    }

    public static String getAccountDiffMode(Context context, Account account) {
        String def = context.getResources().getBoolean(R.bool.config_is_table)
                ? Constants.DIFF_MODE_SIDE_BY_SIDE : Constants.DIFF_MODE_UNIFIED;
        if (account == null) {
            return def;
        }
        return getAccountPreferences(context, account).getString(PREF_ACCOUNT_DIFF_MODE, def);
    }

    public static void setAccountDiffMode(Context context, Account account, String mode) {
        if (account == null) {
            return;
        }

        Editor editor = getAccountPreferences(context, account).edit();
        editor.putString(PREF_ACCOUNT_DIFF_MODE, mode);
        editor.apply();
    }

    public static boolean getAccountWrapMode(Context context, Account account) {
        return account == null ||
                getAccountPreferences(context, account).getBoolean(PREF_ACCOUNT_WRAP_MODE, true);
    }

    public static void setAccountWrapMode(Context context, Account account, boolean wrap) {
        if (account == null) {
            return;
        }

        Editor editor = getAccountPreferences(context, account).edit();
        editor.putBoolean(PREF_ACCOUNT_WRAP_MODE, wrap);
        editor.apply();
    }

    public static float getAccountTextSizeFactor(Context context, Account account) {
        if (account == null) {
            return Constants.DEFAULT_TEXT_SIZE_NORMAL;
        }

        return getAccountPreferences(context, account).getFloat(
                PREF_ACCOUNT_TEXT_SIZE_FACTOR, Constants.DEFAULT_TEXT_SIZE_NORMAL);
    }

    public static void setAccountTextSizeFactor(
            Context context, Account account, float textSizeFactor) {
        if (account == null) {
            return;
        }

        Editor editor = getAccountPreferences(context, account).edit();
        editor.putFloat(PREF_ACCOUNT_TEXT_SIZE_FACTOR, textSizeFactor);
        editor.apply();
    }

    public static boolean isAccountHighlightTabs(Context context, Account account) {
        return account == null || getAccountPreferences(
                context, account).getBoolean(PREF_ACCOUNT_HIGHLIGHT_TABS, true);
    }

    public static void setAccountHighlightTabs(
            Context context, Account account, boolean highlight) {
        if (account == null) {
            return;
        }

        Editor editor = getAccountPreferences(context, account).edit();
        editor.putBoolean(PREF_ACCOUNT_HIGHLIGHT_TABS, highlight);
        editor.apply();
    }

    public static boolean isAccountHighlightTrailingWhitespaces(Context context, Account account) {
        return account == null || getAccountPreferences(
                context, account).getBoolean(PREF_ACCOUNT_HIGHLIGHT_TRAILING_WHITESPACES, true);
    }

    public static void setAccountHighlightTrailingWhitespaces(
            Context context, Account account, boolean highlight) {
        if (account == null) {
            return;
        }

        Editor editor = getAccountPreferences(context, account).edit();
        editor.putBoolean(PREF_ACCOUNT_HIGHLIGHT_TRAILING_WHITESPACES, highlight);
        editor.apply();
    }

    public static boolean isAccountHighlightIntralineDiffs(Context context, Account account) {
        return account == null || getAccountPreferences(
                context, account).getBoolean(PREF_ACCOUNT_HIGHLIGHT_INTRALINE_DIFFS, true);
    }

    public static void setAccountHighlightIntralineDiffs(
            Context context, Account account, boolean highlight) {
        if (account == null) {
            return;
        }

        Editor editor = getAccountPreferences(context, account).edit();
        editor.putBoolean(PREF_ACCOUNT_HIGHLIGHT_INTRALINE_DIFFS, highlight);
        editor.apply();
    }


    public static int getAccountSearchMode(Context context, Account account) {
        if (account == null) {
            return Constants.SEARCH_MODE_CHANGE;
        }
        return getAccountPreferences(context, account).getInt(
                PREF_ACCOUNT_SEARCH_MODE, Constants.SEARCH_MODE_CHANGE);
    }

    public static void setAccountSearchMode(Context context, Account account, int mode) {
        if (account == null) {
            return;
        }

        Editor editor = getAccountPreferences(context, account).edit();
        editor.putInt(PREF_ACCOUNT_SEARCH_MODE, mode);
        editor.apply();
    }

    public static boolean isAccountMessagesFolded(Context context, Account account) {
        return account == null || getAccountPreferences(
                context, account).getBoolean(PREF_ACCOUNT_MESSAGES_FOLDED, false);
    }

    public static boolean isAccountInlineCommentInMessages(Context context, Account account) {
        return account == null || getAccountPreferences(
                context, account).getBoolean(PREF_ACCOUNT_INLINE_COMMENT_IN_MESSAGES, true);
    }

    public static List<CustomFilter> getAccountCustomFilters(Context context, Account account) {
        if (account == null) {
            return null;
        }

        Set<String> set = getAccountPreferences(context, account)
                .getStringSet(PREF_ACCOUNT_CUSTOM_FILTERS, null);
        if (set == null) {
            return null;
        }

        boolean save = false;
        List<CustomFilter> filters = new ArrayList<>(set.size());
        for (String s : set) {
            CustomFilter cf = SerializationManager.getInstance().fromJson(s, CustomFilter.class);
            if (cf.mId == null) {
                cf.mId = UUID.randomUUID().toString();
                save = true;
            }
            filters.add(cf);
        }
        Collections.sort(filters);

        if (save) {
            setAccountCustomFilters(context, account, filters);
        }
        return filters;
    }

    @SuppressWarnings("Convert2streamapi")
    public static void setAccountCustomFilters(
            Context context, Account account, List<CustomFilter> filters) {
        if (account == null) {
            return;
        }

        Editor editor = getAccountPreferences(context, account).edit();
        if (filters == null || filters.isEmpty()) {
            editor.remove(PREF_ACCOUNT_CUSTOM_FILTERS);
        } else {
            Set<String> set = new HashSet<>();
            for (CustomFilter cf : filters) {
                set.add(SerializationManager.getInstance().toJson(cf));
            }
            editor.putStringSet(PREF_ACCOUNT_CUSTOM_FILTERS, set);
        }
        editor.apply();
    }

    public static void saveAccountCustomFilter(
            Context context, Account account, CustomFilter filter) {
        if (account == null) {
            return;
        }

        Set<String> set = getAccountPreferences(context, account)
                .getStringSet(PREF_ACCOUNT_CUSTOM_FILTERS, null);
        if (set == null) {
            set = new HashSet<>();
        } else {
            // https://android-review.googlesource.com/#/c/134312/
            set = new HashSet<>(set);
        }

        // Remove and readd the custom filter
        for (String s : set) {
            CustomFilter cf = SerializationManager.getInstance().fromJson(s, CustomFilter.class);
            if (cf.equals(filter)) {
                set.remove(s);
            }
        }
        set.add(SerializationManager.getInstance().toJson(filter));

        Editor editor = getAccountPreferences(context, account).edit();
        editor.putStringSet(PREF_ACCOUNT_CUSTOM_FILTERS, set);
        editor.apply();
    }
}
