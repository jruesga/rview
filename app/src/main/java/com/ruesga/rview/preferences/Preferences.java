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
import com.ruesga.rview.misc.SerializationManager;
import com.ruesga.rview.model.Account;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.ruesga.rview.preferences.Constants.PREF_ACCOUNT;
import static com.ruesga.rview.preferences.Constants.PREF_ACCOUNTS;
import static com.ruesga.rview.preferences.Constants.PREF_IS_FIRST_RUN;

public class Preferences {

    private static SharedPreferences getPreferences(Context context) {
        return context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
    }

    private static SharedPreferences getAccountPreferences(Context context, Account account) {
        return context.getSharedPreferences(account.getAccountHash(), Context.MODE_PRIVATE);
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
}
