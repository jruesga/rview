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
package com.ruesga.rview.gerrit.adapters;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.ruesga.rview.gerrit.model.AccountInfo;
import com.ruesga.rview.gerrit.model.ApprovalInfo;

import java.lang.reflect.Type;
import java.util.Date;

public class GerritApprovalInfoAdapter implements JsonDeserializer<ApprovalInfo> {

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Date.class, new GerritUtcDateAdapter())
            .create();

    @Override
    public ApprovalInfo deserialize(JsonElement json, Type typeof, JsonDeserializationContext ctx)
            throws JsonParseException {
        ApprovalInfo approvalInfo = GSON.fromJson(json, ApprovalInfo.class);
        approvalInfo.owner = GSON.fromJson(json, AccountInfo.class);
        return approvalInfo;
    }
}
