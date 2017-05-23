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
import android.text.Spannable;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.AttributeSet;
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

public class RegExLinkifyTextView extends StyleableTextView {
    public static class RegExLink {
        interface RegExLinkExtractor {
            String extractLink(String group);
        }

        public final String mType;
        public final Pattern mPattern;
        private final String mLink;
        private final RegExLinkExtractor mExtractor;

        public RegExLink(String type, String regEx, String link) {
            this(type, regEx, link, null);
        }

        private RegExLink(String type, String regEx, String link, RegExLinkExtractor extractor) {
            mType = type;
            mPattern = Pattern.compile(regEx,
                    Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
            mLink = link;
            mExtractor = extractor;
        }
    }

    public static final RegExLink EMAIL_REGEX = new RegExLink(
            "email",
            "[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,4}",
            "mailto:$1");
    public static final RegExLink WEB_LINK_REGEX = new RegExLink(
            "web",
            "((ht|f)tp(s?):\\/\\/|www\\.)(([\\w\\-]+\\.){1,}?([\\w\\-.~]+\\/?)*[\\p{Alnum}.,%_=?&#\\-+()\\[\\]\\*$~@!:/{};']*)",
            "$1");
    public static final RegExLink GERRIT_CHANGE_ID_REGEX = new RegExLink(
            Constants.CUSTOM_URI_CHANGE,
            "(^|\\s)(I[0-9a-f]{8,40})",
            "com.ruesga.rview://" + Constants.CUSTOM_URI_CHANGE + "/$1");
    public static final RegExLink GERRIT_COMMIT_REGEX = new RegExLink(
            Constants.CUSTOM_URI_COMMIT,
            "(^|\\s|[:.,!?\\(\\[\\{])([0-9a-f]{7,40})\\b",
            "com.ruesga.rview://" + Constants.CUSTOM_URI_COMMIT + "/$1");

    private final List<RegExLink> mRegEx = new ArrayList<>();

    public RegExLinkifyTextView(Context context) {
        this(context, null);
    }

    public RegExLinkifyTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RegExLinkifyTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setMovementMethod(LinkMovementMethod.getInstance());
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
                group -> UriHelper.extractChangeId(group, repository)));

        // Queries
        regexLinks.add(new RegExLink(
                Constants.CUSTOM_URI_QUERY,
                "http(s)?://" + uri + "(\\?polygerrit=\\d)?(#/)?q/.*(\\\\s|$)",
                "$1",
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
                        String group = matcher.group();
                        int start = matcher.start();
                        int end = matcher.end();

                        // Try to deal with phrases "." catches by the regexp (this shouldn't
                        // be the case for the 99% of the urls). Also trim up spaces.
                        if (group.endsWith("/.")) {
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

                        // Extract url link
                        if (regEx.mExtractor != null) {
                            group = regEx.mExtractor.extractLink(group);
                        }
                        final String url = group.trim();

                        // Remove previous spans
                        ClickableSpan[] old = span.getSpans(start, end, ClickableSpan.class);
                        if (old != null) {
                            for (ClickableSpan s : old) {
                                span.removeSpan(s);
                            }
                        }

                        span.setSpan(new ClickableSpan() {
                            @Override
                            public void onClick(View v) {
                                // Click on span doesn't provide sound feedback it the text view doesn't
                                // handle a click event. Just perform a click effect.
                                v.playSoundEffect(SoundEffectConstants.CLICK);
                                String link = regEx.mLink.replace("$1", url);

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
}
