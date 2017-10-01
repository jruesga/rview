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

import android.text.TextUtils;
import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static com.ruesga.rview.widget.RegExLinkifyTextView.WEB_LINK_REGEX;

public class ContinuousIntegrationHelper {

    private static final String TAG = "ContinuousIntegration";

    private static final String CI_REGEXP_1 = "\\* .*::((#)?\\d+::)?[(OK, )(WARN, )(IGNORE, )(ERROR, )].*";

    public static List<ContinuousIntegrationInfo> getContinuousIntegrationStatus(
            Repository repository, String changeId, int revisionNumber) {
        List<ContinuousIntegrationInfo> ci = new ArrayList<>();
        if (!TextUtils.isEmpty(repository.mCiStatusMode)
                && !TextUtils.isEmpty(repository.mCiStatusUrl)) {
            // Check status mode
            switch (repository.mCiStatusMode) {
                case "cq":
                    // BuildBot CQ status type
                    return obtainFromCQServer(repository, changeId, revisionNumber);
            }
        }
        return sort(ci);
    }

    @SuppressWarnings({"ConstantConditions", "TryFinallyCanBeTryWithResources"})
    private static List<ContinuousIntegrationInfo> obtainFromCQServer(
            Repository repository, String changeId, int revisionNumber) {
        List<ContinuousIntegrationInfo> statuses = new ArrayList<>();

        String url = repository.mCiStatusUrl
                .replaceFirst("\\{change\\}", changeId)
                .replaceFirst("\\{revision\\}", String.valueOf(revisionNumber));

        try {
            OkHttpClient okhttp = NetworkingHelper.createNetworkClient();
            Request request = new Request.Builder().url(url).build();

            Response response = okhttp.newCall(request).execute();
            try {
                String json = response.body().string();
                JsonObject root = new JsonParser().parse(json).getAsJsonObject();
                if (!root.has("builds")) {
                    return statuses;
                }
                JsonArray o = root.getAsJsonArray("builds");
                int c1 = o.size();
                for (int i = 0; i < c1; i++) {
                    JsonObject b = o.get(i).getAsJsonObject();
                    try {
                        String status = b.get("status").getAsString();
                        String ciUrl = b.get("url").getAsString();
                        String ciName = null;
                        JsonArray tags = b.get("tags").getAsJsonArray();
                        int c2 = tags.size();
                        for (int j = 0; j < c2; j++) {
                            String tag = tags.get(j).getAsJsonPrimitive().getAsString();
                            if (tag.startsWith("builder:")) {
                                ciName = tag.substring(tag.indexOf(":") + 1);
                            }
                        }

                        if (!TextUtils.isEmpty(status) && !TextUtils.isEmpty(ciUrl)
                                && !TextUtils.isEmpty(ciName)) {
                            BuildStatus buildStatus = BuildStatus.RUNNING;
                            if (status.equals("COMPLETED")) {
                                String result = b.get("result").getAsString();

                                switch (result) {
                                    case "SUCCESS":
                                        buildStatus = BuildStatus.SUCCESS;
                                        break;
                                    case "FAILURE":
                                        buildStatus = BuildStatus.FAILURE;
                                        break;
                                    default:
                                        buildStatus = BuildStatus.SKIPPED;
                                        break;
                                }
                            }

                            ContinuousIntegrationInfo c =
                                    findContinuousIntegrationInfo(statuses, ciName);
                            // Attempts are sorted in reversed order, so we need the first one
                            if (c == null) {
                                statuses.add(
                                        new ContinuousIntegrationInfo(ciName, ciUrl, buildStatus));
                            }
                        }

                    } catch (Exception ex) {
                        Log.w(TAG, "Failed to parse ci object" + b, ex);
                    }

                }

            } finally {
                response.close();
            }

        } catch (Exception ex) {
            Log.w(TAG, "Failed to obtain ci status from " + url, ex);
        }

        return statuses;
    }

    public static List<ContinuousIntegrationInfo> extractContinuousIntegrationInfo(
            int patchSet, ChangeMessageInfo[] messages, Repository repository) {
        List<ContinuousIntegrationInfo> ci = new ArrayList<>();
        Set<String> ciNames = new HashSet<>();
        try {
            boolean checkFromPrevious = false;
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
                            ContinuousIntegrationInfo cii = containsBuild(line, ci);
                            if (checkFromPrevious && cii != null) {
                                BuildStatus status = getBuildStatus(line);
                                if (cii.mStatus == null ||
                                        (status != null && !cii.mStatus.equals(status))) {
                                    cii.mStatus = status;
                                }
                                if (cii.mUrl == null && webMatcher.find()) {
                                    cii.mUrl = extractUrl(webMatcher);
                                }

                            } else if (webMatcher.find()) {
                                String url = extractUrl(webMatcher);
                                if (url == null) {
                                    continue;
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
                            } else if (line.startsWith("Building") &&
                                    line.contains("target(s): ")) {
                                String l = line.substring(line.indexOf("target(s): ") + 10);
                                String[] jobs = l.trim().replaceAll("\\.", "").split(" ");
                                for (String job : jobs) {
                                    if (!job.isEmpty()) {
                                        ci.add(new ContinuousIntegrationInfo(
                                                job, null, BuildStatus.SUCCESS));
                                    }
                                }
                                checkFromPrevious = true;
                            } else if (line.matches(CI_REGEXP_1)) {
                                cii = fromRegExp1(line);
                                if (cii != null) {
                                    ci.add(cii);
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
        return sort(ci);
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

            if (l.toLowerCase(Locale.US).contains(part.toLowerCase(Locale.US))) {
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

    private static String extractUrl(Matcher m) {
        String url = m.group();
        if (url == null) {
            return null;
        }
        if (url.endsWith("/.") || url.endsWith(")") || url.endsWith("]")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    private static ContinuousIntegrationInfo containsBuild(
            String line, List<ContinuousIntegrationInfo> cii) {
        for (ContinuousIntegrationInfo ci : cii) {
            if (!TextUtils.isEmpty(ci.mName) && line.contains(ci.mName)) {
                return ci;
            }
        }
        return null;
    }

    private static ContinuousIntegrationInfo findContinuousIntegrationInfo(
            List<ContinuousIntegrationInfo> ci, String name) {
        for (ContinuousIntegrationInfo c : ci) {
            if (c.mName.equals(name)) {
                return c;
            }
        }
        return null;
    }

    private static List<ContinuousIntegrationInfo> sort(List<ContinuousIntegrationInfo> ci) {
        Collections.sort(ci, (c1, c2) -> c1.mName.compareTo(c2.mName));
        return ci;
    }

    private static ContinuousIntegrationInfo fromRegExp1(String line) {
        try {
            String[] split = line.split("::");
            String s = split[split.length -1].substring(0, split[split.length -1].indexOf(","));
            BuildStatus status = null;
            switch (s) {
                case "OK":
                    status = BuildStatus.SUCCESS;
                    break;
                case "IGNORE":
                    status = BuildStatus.SKIPPED;
                    break;
                case "WARN":
                case "ERROR":
                    status = BuildStatus.FAILURE;
                    break;
            }
            return new ContinuousIntegrationInfo(split[0].substring(2), null, status);
        } catch (Exception ex) {
            // Ignore
        }
        return null;
    }
}
