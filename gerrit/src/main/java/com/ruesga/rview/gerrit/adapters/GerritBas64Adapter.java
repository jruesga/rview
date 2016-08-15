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

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.ruesga.rview.gerrit.PlatformAbstractionLayer;
import com.ruesga.rview.gerrit.model.Base64Data;

import java.lang.reflect.Type;

public class GerritBas64Adapter implements JsonDeserializer<Base64Data>, JsonSerializer<Base64Data> {

    private final PlatformAbstractionLayer mAbstractionLayer;

    public GerritBas64Adapter(PlatformAbstractionLayer abstractionLayer) {
        mAbstractionLayer = abstractionLayer;
    }

    public Base64Data deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx)
            throws JsonParseException {
        Base64Data data = new Base64Data();
        data.data = mAbstractionLayer.decodeBase64(json.getAsString().getBytes());
        return data;
    }

    public JsonElement serialize(Base64Data src, Type typeof, JsonSerializationContext ctx) {
        return new JsonPrimitive(new String(mAbstractionLayer.encodeBase64(src.data)));
    }
}
