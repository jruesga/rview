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
package com.ruesga.rview.gerrit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ruesga.rview.gerrit.adapters.GerritApprovalInfoAdapter;
import com.ruesga.rview.gerrit.adapters.GerritBas64Adapter;
import com.ruesga.rview.gerrit.adapters.GerritServerVersionAdapter;
import com.ruesga.rview.gerrit.adapters.GerritUtcDateAdapter;
import com.ruesga.rview.gerrit.model.ApprovalInfo;
import com.ruesga.rview.gerrit.model.Base64Data;
import com.ruesga.rview.gerrit.model.ServerVersion;

import java.util.Date;

public class GsonHelper {

    public static GsonBuilder createGerritGsonBuilder(
            boolean nonExecutable, PlatformAbstractionLayer abstractionLayer) {
        GsonBuilder builder = new GsonBuilder()
                .setVersion(GerritApi.API_VERSION)
                .registerTypeAdapter(Date.class, new GerritUtcDateAdapter())
                .registerTypeAdapter(ServerVersion.class, new GerritServerVersionAdapter())
                .registerTypeAdapter(ApprovalInfo.class, new GerritApprovalInfoAdapter())
                .registerTypeAdapter(Base64Data.class, new GerritBas64Adapter(abstractionLayer))
                .setLenient();
        if (nonExecutable) {
            builder.generateNonExecutableJson();
        }
        return builder;
    }
}
