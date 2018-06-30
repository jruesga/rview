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
package com.ruesga.rview.widget;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.support.annotation.VisibleForTesting;
import android.text.Spannable;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SoundEffectConstants;
import android.view.View;

import com.ruesga.rview.misc.ActivityHelper;
import com.ruesga.rview.misc.ModelHelper;
import com.ruesga.rview.misc.StringHelper;
import com.ruesga.rview.misc.UriHelper;
import com.ruesga.rview.model.Repository;
import com.ruesga.rview.preferences.Constants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.saket.bettermovementmethod.BetterLinkMovementMethod;

public class RegExLinkifyTextView extends StyleableTextView {
    private static final String TAG = "RegExLinkifyTextView";

    public static class RegExLink {
        interface RegExLinkExtractor {
            String extractLink(String group);
        }

        public final String mType;
        public final Pattern mPattern;
        private final String mLink;
        private final boolean mMultiGroup;
        private final RegExLinkExtractor mExtractor;

        public RegExLink(String type, String regEx, String link, boolean multiGroup) {
            this(type, regEx, link, multiGroup, null);
        }

        private RegExLink(String type, String regEx, String link,
                boolean multiGroup, RegExLinkExtractor extractor) {
            mType = type;
            mPattern = Pattern.compile(regEx,
                    Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
            mLink = link;
            mMultiGroup = multiGroup;
            mExtractor = extractor;
        }
    }

    public static final RegExLink EMAIL_REGEX = new RegExLink(
            "email",
            StringHelper.EMAIL_REGEXP,
            "mailto:$1",
            false);
    public static final RegExLink WEB_LINK_REGEX = new RegExLink(
            "web",
            StringHelper.WEB_REGEXP,
            "$1",
            false);
    public static final RegExLink GERRIT_CHANGE_ID_REGEX = new RegExLink(
            Constants.CUSTOM_URI_CHANGE,
            StringHelper.CHANGE_ID_REGEXP,
            "com.ruesga.rview://" + Constants.CUSTOM_URI_CHANGE + "/$1",
            false);
    public static final RegExLink GERRIT_COMMIT_REGEX = new RegExLink(
            Constants.CUSTOM_URI_COMMIT,
            StringHelper.COMMIT_REGEXP,
            "com.ruesga.rview://" + Constants.CUSTOM_URI_COMMIT + "/$1",
            false);

    private final List<RegExLink> mRegEx = new ArrayList<>();

    public RegExLinkifyTextView(Context context) {
        this(context, null);
    }

    public RegExLinkifyTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RegExLinkifyTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setMovementMethod(BetterLinkMovementMethod.getInstance());
        addRegEx(EMAIL_REGEX, WEB_LINK_REGEX);
    }

    public static List<RegExLink> createRepositoryRegExpLinks(Repository repository) {
        String uri = repository.mUrl.substring(
                repository.mUrl.toLowerCase(Locale.US).indexOf("://") + 3);
        if (!uri.endsWith("/")) {
            uri += "/";
        }

        List<RegExLink> regexLinks = new ArrayList<>();

        // Changes
        regexLinks.add(new RegExLink(
                Constants.CUSTOM_URI_CHANGE_ID,
                "http(s)?://" + uri + "((\\?polygerrit=\\d)?(#/)?c/)?(\\d)+(/(((\\d)+\\.\\.)?(\\d)+)?(/(\\S)*+)?)?",
                "com.ruesga.rview://" + Constants.CUSTOM_URI_CHANGE_ID + "/$1",
                false,
                group -> UriHelper.extractChangeId(group, repository)));
        regexLinks.add(new RegExLink(
                Constants.CUSTOM_URI_CHANGE_ID,
                "http(s)?://" + uri + "(\\?polygerrit=\\d)?(#/)?c/[\\w|\\d|\\/]*/\\+/\\d+(/(\\S)*+)?",
                "com.ruesga.rview://" + Constants.CUSTOM_URI_CHANGE_ID + "/$1",
                false,
                group -> UriHelper.extractChangeId(group, repository)));

        // Queries
        regexLinks.add(new RegExLink(
                Constants.CUSTOM_URI_QUERY,
                "http(s)?://" + uri + "(\\?polygerrit=\\d)?(#/)?q/.*(\\\\s|$)",
                "$1",
                false,
                null));

        // Dashboards
        regexLinks.add(new RegExLink(
                Constants.CUSTOM_URI_DASHBOARD,
                "http(s)?://" + uri + "(#/)?dashboard/\\S+",
                "$1",
                false,
                null));

        return regexLinks;
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public void setText(CharSequence text, BufferType type) {
        if (text != null) {
            Spannable span = text instanceof Spannable
                    ? (Spannable) text : Spannable.Factory.getInstance().newSpannable(text);
            if (mRegEx != null) {
                for (final RegExLink regEx : mRegEx) {
                    if (regEx == null) {
                        continue;
                    }

                    final Matcher matcher = regEx.mPattern.matcher(text);
                    while (matcher.find()) {
                        final String link = replaceLink(regEx, matcher);
                        if (link == null) {
                            return;
                        }

                        // Try to deal with ".", ")", "]" catches by the regexp (this shouldn't
                        // be the case for the 99% of the urls). Also trim up spaces.
                        String group = matcher.group();
                        int start = matcher.start();
                        int end = matcher.end();
                        if (group == null || start == -1 || end == -1) {
                            return;
                        }
                        if (group.endsWith(".") || group.endsWith(")") || group.endsWith("]")) {
                            group = group.substring(0, group.length() - 1);
                            end--;
                        }
                        while (group.startsWith(" ") || group.startsWith("\n")) {
                            group = group.substring(1);
                            start++;
                        }
                        while (group.endsWith(" ") || group.endsWith("\n")) {
                            group = group.substring(0, group.length() - 1);
                            end++;
                        }

                        // Remove previous spans
                        ClickableSpan[] old = span.getSpans(start, end, ClickableSpan.class);
                        if (old != null) {
                            for (ClickableSpan s : old) {
                                span.removeSpan(s);
                            }
                        }

                        // Avoid to apply more than one clickable span over the same range
                        ClickableSpan[] spans = span.getSpans(start, end, ClickableSpan.class);
                        if (spans != null && spans.length > 0) {
                            continue;
                        }

                        span.setSpan(new ClickableSpan() {
                            @Override
                            public void onClick(View v) {
                                // Click on span doesn't provide sound feedback it the text view doesn't
                                // handle a click event. Just perform a click effect.
                                v.playSoundEffect(SoundEffectConstants.CLICK);

                                Uri uri = StringHelper.buildUriAndEnsureScheme(link);
                                boolean isHttpScheme = uri.getScheme().equals("http")
                                        || uri.getScheme().equals("https");
                                if (!isHttpScheme ||
                                        ModelHelper.canAnyAccountHandleUrl(getContext(), link)) {
                                    ActivityHelper.handleUri(getContext(), uri);
                                } else {
                                    ActivityHelper.openUriInCustomTabs((Activity) getContext(), uri);
                                }
                            }
                        }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }
            }
            super.setText(span, BufferType.SPANNABLE);
            return;
        }
        super.setText(text, type);
    }

    public void addRegEx(RegExLink... regexs) {
        mRegEx.addAll(Arrays.asList(regexs));
        setText(getText(), BufferType.SPANNABLE);
    }

    @VisibleForTesting
    public static String replaceLink(
            RegExLink regEx, Matcher matcher) {
        List<String> groups = new ArrayList<>();
        if (regEx.mMultiGroup) {
            final int count = matcher.groupCount();
            for(int i = 1; i <= count; i++) {
                String s = matcher.group(i);
                groups.add(s == null ? "" : s);
            }
        } else {
            groups.add(matcher.group());
        }

        if (groups.isEmpty()) {
            Log.w(TAG, "RegExLinkify empty groups => pattern: '" + regEx.mPattern + "'; link: '"
                    + "'; multiGroup: " + regEx.mMultiGroup);
            return null;
        }

        // Try to deal with ".", ")", "]" catches by the regexp (this shouldn't
        // be the case for the 99% of the urls). Also trim up spaces.
        final int count = groups.size();
        for(int i = 0; i < count; i++) {
            String group = groups.get(i);
            if (i == (count - 1)) {
                if (group.endsWith(".") || group.endsWith(")") || group.endsWith("]")) {
                    group = group.substring(0, group.length() - 1);
                }
            }
            while (group.startsWith(" ") || group.startsWith("\n")) {
                group = group.substring(1);
            }
            while (group.endsWith(" ") || group.endsWith("\n")) {
                group = group.substring(0, group.length() - 1);
            }

            // Extract url link
            if (regEx.mExtractor != null) {
                group = regEx.mExtractor.extractLink(group);
            }

            groups.set(i, group.trim());
        }

        // Replace the link
        String link = regEx.mLink;
        for(int i = 0; i < count; i++) {
            String group = groups.get(i);
            link = link.replace("$"+(i+1), group == null ? "" : group);
        }
        return link;
    }
}
