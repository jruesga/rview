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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringHelper {

    public static final String NON_PRINTABLE_CHAR = "\u0001";

    private static final Pattern A_NON_WORD_CHARACTER_AT_END
            = Pattern.compile(".*[^(\\w,;)]", Pattern.MULTILINE);
    private static final Pattern A_NON_WORD_CHARACTER_AT_START
            = Pattern.compile("[^\\w].*", Pattern.MULTILINE);


    private static final Pattern QUOTE1 = Pattern.compile("^> ", Pattern.MULTILINE);
    private static final Pattern QUOTE2 = Pattern.compile("^>", Pattern.MULTILINE);
    private static final Pattern REPLACED_QUOTE1 = Pattern.compile(NON_PRINTABLE_CHAR + ">");
    private static final Pattern REPLACED_QUOTE2 = Pattern.compile(NON_PRINTABLE_CHAR + " > ");
    private static final Pattern REPLACED_QUOTE3 = Pattern.compile(NON_PRINTABLE_CHAR + " ");
    private static final Pattern REPLACED_QUOTE4 = Pattern.compile(NON_PRINTABLE_CHAR + "\n");
    private static final Pattern REPLACED_QUOTE5 = Pattern.compile(
            "(\\w)(\n" + NON_PRINTABLE_CHAR +")(\\w)", Pattern.MULTILINE);

    public static String removeLineBreaks(String message) {
        String[] lines = message.split("\\r?\\n");
        StringBuilder sb = new StringBuilder();
        int count = lines.length;
        for (int i = 0; i < count; i++) {
            String line = lines[i].trim();
            sb.append(line);
            if (line.isEmpty() ||
                    A_NON_WORD_CHARACTER_AT_END.matcher(line).matches()) {
                sb.append("\n");
            } else if (i < (count - 1)) {
                String next =  lines[i + 1].trim();
                if (next.isEmpty()
                        || A_NON_WORD_CHARACTER_AT_START.matcher(next).matches()) {
                    sb.append("\n");
                } else {
                    sb.append(" ");
                }
            }
        }
        return sb.toString().trim();
    }

    public static String obtainMessageFromQuote(String message) {
        String msg = QUOTE1.matcher(message).replaceAll(NON_PRINTABLE_CHAR);
        msg = QUOTE2.matcher(msg).replaceAll(NON_PRINTABLE_CHAR);
        do {
            final String m = msg;
            msg = REPLACED_QUOTE1.matcher(msg).replaceAll(NON_PRINTABLE_CHAR + NON_PRINTABLE_CHAR);
            msg = REPLACED_QUOTE2.matcher(msg).replaceAll(NON_PRINTABLE_CHAR + NON_PRINTABLE_CHAR);
            msg = REPLACED_QUOTE3.matcher(msg).replaceAll(NON_PRINTABLE_CHAR);
            msg = REPLACED_QUOTE4.matcher(msg).replaceAll(NON_PRINTABLE_CHAR + " \n");
            Matcher matcher = REPLACED_QUOTE5.matcher(msg);
            if (matcher.find()) {
                msg = matcher.replaceAll("$1 $3");
            }
            if (msg.equals(m)) {
                break;
            }
        } while (true);
        return msg;
    }

    public static String obtainQuoteFromMessage(String quote) {
        // TODO Convert to quote
        return quote;
    }

    public static int countOccurrences(String find, String in) {
        int count = 0;
        Pattern pattern = Pattern.compile(find);
        Matcher matcher = pattern.matcher(in);
        while (matcher.find()) count++;
        return count;
    }
}
