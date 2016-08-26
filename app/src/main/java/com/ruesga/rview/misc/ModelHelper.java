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

import com.ruesga.rview.gerrit.Authorization;
import com.ruesga.rview.gerrit.GerritApi;
import com.ruesga.rview.gerrit.GerritServiceFactory;
import com.ruesga.rview.model.Account;
import com.ruesga.rview.preferences.Preferences;

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

}
