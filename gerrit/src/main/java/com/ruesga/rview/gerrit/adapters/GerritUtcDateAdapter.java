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

import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class GerritUtcDateAdapter implements JsonDeserializer<Date>, JsonSerializer<Date> {

    private final DateFormat mDateFormat;

    public GerritUtcDateAdapter() {
        mDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
        mDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    @Override
    public Date deserialize(JsonElement json, Type typeof, JsonDeserializationContext ctx)
            throws JsonParseException {
        try {
            synchronized (mDateFormat) {
                return mDateFormat.parse(json.getAsString());
            }
        } catch (ParseException e) {
            throw new JsonParseException(e);
        }
    }

    @Override
    public JsonElement serialize(Date src, Type typeof, JsonSerializationContext ctx) {
        synchronized (mDateFormat) {
            return new JsonPrimitive(mDateFormat.format(src));
        }
    }
}
