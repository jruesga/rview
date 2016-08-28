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
import android.text.Html;
import android.widget.TextView;

import com.ruesga.rview.annotations.ProguardIgnored;
import com.ruesga.rview.gerrit.model.ChangeInfo;
import com.ruesga.rview.gerrit.model.LabelInfo;

import org.ocpsoft.prettytime.PrettyTime;

import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ProguardIgnored
public class Formatter {
    private static final Map<Locale, PrettyTime> sPrettyTimeMap = new HashMap<>();
    private static final Pattern sShortLabelPattern = Pattern.compile("[A-Z]+");

    @BindingAdapter("prettyDateTime")
    public static void toPrettyDateTime(TextView view, Date date) {
        Locale locale = AndroidHelper.getCurrentLocale(view.getContext());
        if (!sPrettyTimeMap.containsKey(locale)) {
            sPrettyTimeMap.put(locale, new PrettyTime(locale));
        }
        view.setText(sPrettyTimeMap.get(locale).format(date));
    }

    @BindingAdapter("labels")
    public static void toLabels(TextView view, Map<String, LabelInfo> labels) {
        // Compute and sort labels
        if (labels == null) {
            return;
        }
        Map<String, String> map = new TreeMap<>();
        for (String label : labels.keySet()) {
            final LabelInfo info = labels.get(label);
            final String s;
            if (info.blocking) {
                s = "\u2717";
            } else if (info.rejected != null) {
                s = "-2";
            } else if (info.disliked != null) {
                s = "-1";
            } else if (info.recommended != null) {
                s = "+1";
            } else if (info.approved != null) {
                s = "\u2713";
            } else {
                s = "-";
            }

            map.put(toShortLabel(label), s);
        }

        // Print labels
        StringBuilder sb = new StringBuilder();
        for (String label : map.keySet()) {
            sb.append("  <b>").append(label).append("</b>: ").append(map.get(label));
        }
        view.setText(Html.fromHtml(sb.toString()));
    }

    private static String toShortLabel(String label) {
        Matcher matcher = sShortLabelPattern.matcher(label);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            sb.append(matcher.group(0));
        }
        return sb.toString();
    }
}
