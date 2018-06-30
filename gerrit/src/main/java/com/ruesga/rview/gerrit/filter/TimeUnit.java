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
package com.ruesga.rview.gerrit.filter;

import java.util.Locale;

public enum TimeUnit {
    SECONDS("s", "sec", "second", "seconds"),
    MINUTES("m", "min", "minute", "minutes"),
    HOURS("h", "hour", "hours"),
    DAYS("d", "day", "days"),
    WEEKS("w", "week", "week"),
    MONTHS("mon", "month", "months"),
    YEARS("y", "year", "years");

    public final String[] mUnits;
    TimeUnit(String... units) {
        mUnits = units;
    }

    public String toQuery(int value) {
        return String.format(Locale.US, "%d%s", value, mUnits[0]);
    }
}
