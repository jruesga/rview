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

import com.ruesga.rview.model.Repository;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

public class UriHelper {

    public static final String CUSTOM_URI_TOKENIZER = ":";

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

    public static String extractChangeId(Uri uri, Repository repository) {
        return extractChangeId(uri.toString(), repository);
    }

    public static String extractChangeId(String q, Repository repository) {
        // Sanitize urls with polygerrit flags
        String u = q.replaceAll("/\\?polygerrit=\\d", "/");
        if (u.endsWith("?polygerrit=0") || u.endsWith("?polygerrit=1")) {
            u = u.substring(0, u.length() - 13);
        }

        String repoUrl = repository.mUrl.substring(repository.mUrl.indexOf("://") + 3);
        int idx = u.indexOf(repoUrl);
        if (idx == -1) {
            return "-1";
        }
        String query = u.substring(idx + repoUrl.length());
        if (!query.startsWith("/")) {
            query = "/" + query;
        }

        String target = "-1";
        if (query.contains("/+/")) {
            int start = query.indexOf("/+/") + 3;
            target = query.substring(start);
        } else if (query.startsWith("/#/c/") || query.startsWith("/c/")) {
            int start = query.indexOf("/c/") + 3;
            target = query.substring(start);
        } else {
            int start = query.indexOf("/") + 1;
            if (start > 0) {
                target = query.substring(start);
            }
        }

        // Clean up the target
        if (target.endsWith("/")) {
            target = target.substring(0, target.length() - 1);
        }
        return target.replaceAll("//", "/").replaceAll("/", CUSTOM_URI_TOKENIZER);
    }
}
