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
package com.ruesga.rview.attachments.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.support.annotation.RestrictTo;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ruesga.rview.attachments.AuthenticationInfo;
import com.ruesga.rview.attachments.Provider;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class Preferences {

    private static final Gson mGson = new GsonBuilder().create();

    private static String getPreferencesName(Context context) {
        return context.getPackageName() + ".attachments";
    }

    private static SharedPreferences getPreferences(Context context) {
        return context.getSharedPreferences(getPreferencesName(context), Context.MODE_PRIVATE);
    }

    public static Provider getProvider(Context context) {
        return Provider.valueOf(getPreferences(context).getString(
                Constants.PREF_PROVIDER, Provider.NONE.name()));
    }

    public static void setProvider(Context context, Provider provider) {
        Editor editor = getPreferences(context).edit();
        editor.putString(Constants.PREF_PROVIDER, provider.name());
        editor.apply();
    }

    public static AuthenticationInfo getAuthenticationInfo(Context context, Provider provider) {
        final String key = Constants.PREF_AUTH + "." + provider.name();
        String json = getPreferences(context).getString(key, null);
        if (json == null) {
            return null;
        }
        return mGson.fromJson(json, AuthenticationInfo.class);
    }

    public static void setAuthenticationInfo(
            Context context, Provider provider, AuthenticationInfo auth) {
        final String key = Constants.PREF_AUTH + "." + provider.name();
        Editor editor = getPreferences(context).edit();
        editor.putString(key, mGson.toJson(auth));
        editor.apply();
    }
}
