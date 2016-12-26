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
import android.net.Uri;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

public class UriHelper {

    public static Uri createCustomUri(Context ctx, String kind, String query) {
        return Uri.parse(ctx.getPackageName() + "://" + kind + "/" + query);
    }

    public static String extractQuery(Uri uri) {
        String url = uri.toString();
        int pos = url.indexOf("/q/");
        if (pos == -1) {
            return null;
        }
        try {
            String query = url.substring(pos + 3);
            // Double url decode to ensure is all has the proper format to be parsed
            return URLDecoder.decode(URLDecoder.decode(query, "UTF-8"), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // Ignore
        }
        return null;
    }

    public static String extractChangeId(Uri uri) {
        return extractChangeId(uri.toString());
    }

    public static String extractChangeId(String q) {
        int pos = q.indexOf("/c/");
        String target = "-1";
        if (pos != -1) {
            int start = q.indexOf("/c/") + 3;
            target = q.substring(start);
        } else {
            int start = q.indexOf("/", 9) + 1;
            if (start != -1) {
                target = q.substring(start);
            }
        }

        // Clean up the target
        if (target.endsWith("/")) {
            target = target.substring(target.lastIndexOf("/", target.length() - 2) + 1);
        }
        return target.replaceAll("//", "/").replaceAll("/", "_");
    }
}
