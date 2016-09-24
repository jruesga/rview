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
package com.ruesga.rview.misc;

import android.content.Context;
import android.text.TextUtils;

import com.ruesga.rview.R;
import com.ruesga.rview.gerrit.Authorization;
import com.ruesga.rview.gerrit.GerritApi;
import com.ruesga.rview.gerrit.GerritServiceFactory;
import com.ruesga.rview.gerrit.model.AccountInfo;
import com.ruesga.rview.model.Account;
import com.ruesga.rview.preferences.Preferences;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class ModelHelper {

    public static GerritApi getGerritApi(Context applicationContext) {
        Account account = Preferences.getAccount(applicationContext);
        if (account == null) {
            return null;
        }
        Authorization authorization = new Authorization(account.mAccount.username, account.mToken);
        return GerritServiceFactory.getInstance(
                applicationContext, account.mRepository.mUrl, authorization);
    }

    public static List<String> getAvatarUrl(Context context, AccountInfo account) {
        List<String> urls = new ArrayList<>();

        // Gerrit avatars
        float maxSize = context.getResources().getDimension(R.dimen.max_avatar_size);
        if (account.avatars != null && account.avatars.length > 0) {
            int count = account.avatars.length - 1;
            boolean hasAvatarUrl = false;
            for (int i = count; i >= 0; i--) {
                if (account.avatars[i].height < maxSize) {
                    urls.add(account.avatars[i].url);
                    hasAvatarUrl = true;
                    break;
                }
            }
            if (hasAvatarUrl) {
                urls.add(account.avatars[0].url);
            }
        }

        // Github identicons
        if (account.username != null) {
            urls.add("https://github.com/identicons/" + account.username + ".png");
        }

        // Gravatar icons
        if (account.email != null) {
            urls.add("https://www.gravatar.com/avatar/"
                    + computeGravatarHash(account.email) + ".png?s=" + ((int)maxSize) + "&d=404");
        }

        return urls;
    }

    public static String formatAccountWithEmail(AccountInfo account) {
        StringBuilder sb = new StringBuilder();
        if (!TextUtils.isEmpty(account.name)) {
            sb.append(account.name);
        } else if (!TextUtils.isEmpty(account.username)) {
            sb.append(account.username);
        } else {
            sb.append(account.username);
        }
        if (!TextUtils.isEmpty(account.email)) {
            if (sb.length() == 0) {
                sb.append(account.email);
            } else {
                sb.append(" <").append(account.email).append(">");
            }
        }
        return sb.toString().trim();
    }



    @SuppressWarnings("TryWithIdenticalCatches")
    private static String computeGravatarHash(String email) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            return hex(md.digest(email.getBytes("CP1252")));
        } catch (NoSuchAlgorithmException e) {
            // Ignore
        } catch (UnsupportedEncodingException e) {
            // Ignore
        }
        return null;
    }

    private static String hex(byte[] array) {
        StringBuilder sb = new StringBuilder();
        for (byte v : array) {
            sb.append(Integer.toHexString((v & 0xFF) | 0x100).substring(1, 3));
        }
        return sb.toString();
    }
}
