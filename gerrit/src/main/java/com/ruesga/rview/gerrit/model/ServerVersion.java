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
package com.ruesga.rview.gerrit.model;

import java.util.Locale;

/**
 * Server version [mayor.minor.build]
 */
public class ServerVersion {
    public int major;
    public int minor;
    public String build;

    public ServerVersion(String version) {
        String[] v = version.split("\\.");
        if (v.length == 2 && version.contains("-")) {
            build = version.substring(version.indexOf("-") + 1);
            v = version.substring(0, version.indexOf("-")).split("\\.");
        }
        if (v.length > 0) {
            major = readSafeValue(v[0]);
        }
        if (v.length > 1) {
            minor = readSafeValue(v[1]);
        }
        if (v.length > 2) {
            build = v[2];
        }
    }

    public double getVersion() {
        return Double.parseDouble(String.format(Locale.getDefault(), "%d.%d", major, minor));
    }

    private static int readSafeValue(String v) {
        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException nfe) {
            return 0;
        }
    }

    @Override
    public String toString() {
        if (isEmpty(build)) {
            return String.format(Locale.getDefault(), "%d.%d", major, minor);
        }
        return String.format(Locale.getDefault(), "%d.%d.%s", major, minor, build);
    }

    private static boolean isEmpty(String src) {
        return src == null || src.length() == 0;
    }
}
