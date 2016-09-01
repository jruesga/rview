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

import android.databinding.BindingAdapter;
import android.widget.TextView;

import com.ruesga.rview.R;
import com.ruesga.rview.annotations.ProguardIgnored;
import com.ruesga.rview.gerrit.model.CommitInfo;
import com.ruesga.rview.gerrit.model.GitPersonalInfo;
import com.ruesga.rview.gerrit.model.SubmitType;

import org.ocpsoft.prettytime.PrettyTime;

import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@ProguardIgnored
@SuppressWarnings("unused")
public class Formatter {
    private static final Map<Locale, PrettyTime> sPrettyTimeMap = new HashMap<>();

    @BindingAdapter("prettyDateTime")
    public static void toPrettyDateTime(TextView view, Date date) {
        if (date == null) {
            view.setText(null);
            return;
        }

        Locale locale = AndroidHelper.getCurrentLocale(view.getContext());
        if (!sPrettyTimeMap.containsKey(locale)) {
            sPrettyTimeMap.put(locale, new PrettyTime(locale));
        }
        view.setText(sPrettyTimeMap.get(locale).format(date));
    }

    @BindingAdapter("compressedText")
    public static void toCompressedText(TextView view, String value) {
        if (value == null) {
            view.setText(null);
            return;
        }
        view.setText(value.replaceAll("\n", " ").trim());
    }

    @BindingAdapter("commitMessage")
    @SuppressWarnings("deprecation")
    public static void toCommitMessage(TextView view, CommitInfo info) {
        if (info == null || info.message == null) {
            view.setText(null);
            return;
        }

        String message = info.message.substring(info.subject.length()).trim();
        view.setText(message);
    }

    @BindingAdapter("committer")
    public static void toCommitter(TextView view, GitPersonalInfo info) {
        if (info == null) {
            view.setText(null);
            return;
        }

        Locale locale = AndroidHelper.getCurrentLocale(view.getContext());
        if (!sPrettyTimeMap.containsKey(locale)) {
            sPrettyTimeMap.put(locale, new PrettyTime(locale));
        }
        String date = sPrettyTimeMap.get(locale).format(info.date);
        String txt = view.getContext().getString(
                R.string.committer_format, info.name, info.email, date);
        view.setText(txt);
    }

    @BindingAdapter("score")
    public static void toScore(TextView view, Integer score) {
        if (score == null) {
            view.setText(null);
            return;
        }

        String txt = (score > 0 ? "+" : "") + String.valueOf(score);
        view.setText(txt);
    }

    @BindingAdapter("submitType")
    public static void toSubmitType(TextView view, SubmitType submitType) {
        if (submitType == null) {
            view.setText(null);
            return;
        }

        view.setText(submitType.toString().replace("_", " "));
    }
}
