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
import android.text.Spannable;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.AttributeSet;
import android.view.SoundEffectConstants;
import android.view.View;
import android.widget.TextView;

import com.ruesga.rview.misc.AndroidHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegExLinkifyTextView extends TextView {
    public static class RegExLink {
        public final Pattern mPattern;
        public final String mLink;

        public RegExLink(String regEx, String link) {
            mPattern = Pattern.compile(regEx,
                    Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
            mLink = link;
        }
    }

    public static final RegExLink EMAIL_REGEX = new RegExLink(
            "[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,4}",
            "mailto:$1");
    public static final RegExLink WEB_LINK_REGEX = new RegExLink(
            "((ht|f)tp(s?):\\/\\/|www\\.)(([\\w\\-]+\\.){1,}?([\\w\\-.~]+\\/?)*[\\p{Alnum}.,%_=?&#\\-+()\\[\\]\\*$~@!:/{};']*)",
            "$1");
    public static final RegExLink GERRIT_CHANGE_ID_REGEX = new RegExLink(
            "(I[0-9a-f]{8,40})",
            "com.ruesga.rview:change//$1");
    public static final RegExLink GERRIT_COMMIT_REGEX = new RegExLink(
            "(^|\\s|[:.,!?\\(\\[\\{])([0-9a-f]{7,40})\\b",
            "com.ruesga.rview:commit//$1");

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

    @Override
    @SuppressWarnings("ConstantConditions")
    public void setText(CharSequence text, BufferType type) {
        if (text != null) {
            Spannable span = text instanceof Spannable
                    ? (Spannable) text : Spannable.Factory.getInstance().newSpannable(text);
            if (mRegEx != null) {
                for (final RegExLink regEx : mRegEx) {
                    final Matcher matcher = regEx.mPattern.matcher(text);
                    while (matcher.find()) {
                        final String url = matcher.group();
                        span.setSpan(new ClickableSpan() {
                            @Override
                            public void onClick(View v) {
                                // Click on span doesn't provide sound feedback it the text view doesn't
                                // handle a click event. Just perform a click effect.
                                String link = regEx.mLink.replace("$1", url);
                                v.playSoundEffect(SoundEffectConstants.CLICK);
                                AndroidHelper.openUriInCustomTabs((Activity) getContext(),
                                        AndroidHelper.buildUriAndEnsureScheme(link));
                            }
                        }, matcher.start(), matcher.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }
            }
            super.setText(span, BufferType.SPANNABLE);
            return;
        }
        super.setText(text, type);
    }

    public void addRegEx(RegExLink... regex) {
        mRegEx.addAll(Arrays.asList(regex));
        setText(getText(), BufferType.SPANNABLE);
    }
}
