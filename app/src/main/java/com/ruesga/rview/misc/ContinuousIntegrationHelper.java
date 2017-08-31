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

import android.util.Log;

import com.ruesga.rview.gerrit.model.ChangeMessageInfo;
import com.ruesga.rview.model.ContinuousIntegrationInfo;
import com.ruesga.rview.model.ContinuousIntegrationInfo.BuildStatus;
import com.ruesga.rview.model.Repository;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;

import static com.ruesga.rview.widget.RegExLinkifyTextView.WEB_LINK_REGEX;

public class ContinuousIntegrationHelper {

    private static final String TAG = "ContinuousIntegration";

    public static List<ContinuousIntegrationInfo> extractContinuousIntegrationInfo(
            int patchSet, ChangeMessageInfo[] messages, Repository repository) {
        List<ContinuousIntegrationInfo> ci = new ArrayList<>();
        Set<String> ciNames = new HashSet<>();
        try {
            for (ChangeMessageInfo message : messages) {
                if (message.revisionNumber == patchSet) {
                    if (ModelHelper.isCIAccount(message.author, repository)) {
                        final String[] lines = message.message.split("\n");
                        String prevLine = null;
                        for (String line : lines) {
                            // A CI line/s should have:
                            //   - Not a reply
                            //   - Have an url (to the ci backend)
                            //   - Have a build status
                            //   - Have a job name

                            if (line.trim().startsWith(">")) {
                                continue;
                            }

                            Matcher webMatcher = WEB_LINK_REGEX.mPattern.matcher(line);
                            if (webMatcher.find()) {
                                String url = webMatcher.group();
                                if (url == null) {
                                    continue;
                                }
                                if (url.endsWith("/.") || url.endsWith(")") || url.endsWith("]")) {
                                    url = url.substring(0, url.length() - 1);
                                }

                                BuildStatus status = null;
                                String name = null;
                                for (int i = 0; i <= 1; i++) {
                                    String l = i == 0 ? line : prevLine;
                                    if (status == null) {
                                        status = getBuildStatus(l);
                                    }
                                    if (name == null) {
                                        name = getJobName(l, status, url);
                                    }
                                    if (status != null && name != null && !ciNames.contains(name)) {
                                        ci.add(new ContinuousIntegrationInfo(name, url, status));
                                        ciNames.add(name);
                                        break;
                                    }
                                }
                            }

                            prevLine = line;
                        }
                    }
                }
            }
        } catch (Exception ex) {
            Log.d(TAG, "Failed to obtain continuous integration", ex);
            ci.clear();
        }
        return ci;
    }

    private static BuildStatus getBuildStatus(String line) {
        if (line == null || line.trim().length() == 0) {
            return null;
        }

        if (line.contains("PASS: ") || line.contains(": SUCCESS")
                || (line.contains(" succeeded (") && line.contains(" - ")) || line.contains("\u2705")) {
            return BuildStatus.SUCCESS;
        }
        if (line.contains("FAIL: ") || line.contains(": FAILURE")
                || (line.contains(" failed (") && line.contains(" - ")) || line.contains("\u274C")) {
            return BuildStatus.FAILURE;
        }
        if (line.contains("SKIPPED: ") || line.contains(": ABORTED")) {
            return BuildStatus.SKIPPED;
        }
        return null;
    }

    private static String getJobName(String line, BuildStatus status, String url) throws Exception {
        if (line == null || line.trim().length() == 0) {
            return null;
        }

        String l = line.replace(url, "");
        String decodedUrl = URLDecoder.decode(url, "UTF-8");

        // Try to extract the name from the url
        int c = 0;
        for (String part : decodedUrl.split("/")) {
            c++;

            if (c <= 3 || part.length() == 0) {
                continue;
            }

            if (StringHelper.isOnlyNumeric(part)) {
                continue;
            }

            if (l.toLowerCase().contains(part.toLowerCase())) {
                // Found a valid job name
                return part;
            }
        }

        // Extract from the initial line
        int end = l.indexOf(" : ");
        if (end > 0) {
            int start = l.indexOf(" ");
            return l.substring(start == -1 ? 0 : start, end).trim();
        }

        // Extract from Url
        if (status != null) {
            List<String> parts = Arrays.asList(decodedUrl.split("/"));
            Collections.reverse(parts);
            c = 0;
            for (String part : parts) {
                c++;
                if (part.equalsIgnoreCase("console") || part.equalsIgnoreCase("build")
                        || part.equalsIgnoreCase("builds") || part.equalsIgnoreCase("job")) {
                    continue;
                }

                if (c <= 1) {
                    continue;
                }

                if (StringHelper.isOnlyNumeric(part)) {
                    continue;
                }

                // Found a valid job name
                return part;
            }
        }

        // Can't find a valid job name
        return null;
    }
}
