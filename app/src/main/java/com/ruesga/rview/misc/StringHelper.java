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

import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.ruesga.rview.attachments.Attachment;
import com.ruesga.rview.gerrit.model.ChangeMessageInfo;

import java.io.File;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringHelper {

    private static final String TAG = "StringHelper";

    public static final String NON_PRINTABLE_CHAR = "\u0001";
    public static final String NON_PRINTABLE_CHAR2 = "\u0002";

    private static final Pattern A_NON_WORD_CHARACTER_AT_END = Pattern.compile(".*[^0-9a-zA-Z,;]");
    private static final Pattern A_NON_WORD_CHARACTER_AT_START = Pattern.compile("[^a-zA-Z].*");
    private static final Pattern A_REF_LINK_LINE = Pattern.compile("([0-9a-zA-Z-])+: .*");
    private static final Pattern NON_WHITESPACE = Pattern.compile("\\w");

    private static final String QUOTE_START_TAG = "[QUOTE]";
    private static final String QUOTE_END_TAG = "[/QUOTE]";

    private static final Pattern QUOTE_REGEXP
            = Pattern.compile("^\\[QUOTE](.+?)\\[/QUOTE]$", Pattern.MULTILINE | Pattern.DOTALL);

    public static final String EMAIL_REGEXP = "[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,4}";
    public static final String WEB_REGEXP ="((ht|f)tp(s?):\\/\\/|www\\.)(([\\w\\-]+\\.)" +
            "{1,}?([\\w\\-.~]+\\/?)*[\\p{Alnum}.,%_=?&#\\-+()\\[\\]\\*$~@!:/{};']*)";
    public static final String CHANGE_ID_REGEXP = "(^|\\s)(I[0-9a-f]{8,40})";
    public static final String COMMIT_REGEXP = "(^|\\s|[:.,!?\\(\\[\\{])([0-9a-f]{7,40})\\b";

    public static final Pattern GERRIT_CHANGE = Pattern.compile("I[0-9a-f]{8,40}");
    public static final Pattern GERRIT_COMMIT = Pattern.compile("[0-9a-f]{7,40}");
    public static final Pattern GERRIT_CHANGE_ID = Pattern.compile("\\d+");
    public static final Pattern GERRIT_ENCODED_CHANGE_ID = Pattern.compile("\\d+:\\d+(\\.\\.\\d+)?:.*");

    public static final Pattern LINK_HTML_REGEXP = Pattern.compile("href=\"(.*?)\"");

    private static final Pattern NUMERIC_REGEXP = Pattern.compile("\\d+");

    private static final Pattern PARAGRAPHS_REGEXP = Pattern.compile("\\r?\\n\\r?\\n\\r?\\n");

    private static final Pattern ATTACHMENT_REGEXP = Pattern.compile("!\\[ATTACHMENT:\\{.*\\}\\]\\(" + WEB_REGEXP + "\\)");

    public static final Pattern PATCHSET_LINE_PATTERN = Pattern.compile("^Patch Set [\\d]+: .*");
    public static final Pattern VOTE_PATTERN = Pattern.compile("( ([\\w-]+([+-]\\d+)|([-])[\\w-]+))");
    public static final Pattern VOTE_SCORE_PATTERN = Pattern.compile("[+-]\\d+");

    public static String cleanUpParagraphs(String message) {
        String msg = message;
        do {
            Matcher m = PARAGRAPHS_REGEXP.matcher(msg);
            if (!m.find()) {
                break;
            }
            msg = m.replaceAll("\n\n");
        } while (true);
        return msg;
    }

    public static String[] obtainParagraphs(String message) {
        return message.split("\\r?\\n\\r?\\n");
    }

    public static boolean isQuote(String p) {
        return p.startsWith("> ") || p.startsWith(" > ");
    }

    public static boolean isPreFormat(final String p) {
        return p.contains("\n ") || p.contains("\n\t") || p.startsWith(" ")
                || p.startsWith("\t");
    }

    public static boolean isList(final String p) {
        return p.contains("\n- ") || p.contains("\n* ") || p.startsWith("- ")
                || p.startsWith("* ") || p.startsWith(NON_PRINTABLE_CHAR + "- ")
                || p.startsWith(NON_PRINTABLE_CHAR + "* ");
    }

    public static String removeLineBreaks(String message) {
        String[] lines = message.split("\\r?\\n");
        StringBuilder sb = new StringBuilder();
        int count = lines.length;
        boolean trim = false;
        for (int i = 0; i < count; i++) {
            String line = lines[i];
            String trimmed = line.trim();
            sb.append(trim ? trimmed : line);
            if (trimmed.isEmpty() || A_NON_WORD_CHARACTER_AT_END.matcher(trimmed).matches()
                    || A_REF_LINK_LINE.matcher(trimmed).matches()) {
                sb.append("\n");
                trim = false;
            } else if (i < (count - 1)) {
                String next = lines[i + 1].trim();
                if (next.isEmpty() || A_NON_WORD_CHARACTER_AT_START.matcher(next).matches()) {
                    sb.append("\n");
                    trim = false;
                } else {
                    sb.append(" ");
                    trim = true;
                }
            } else {
                trim = false;
            }
        }
        return sb.toString().trim();
    }

    public static String prepareForQuote(String message) {
        String[] lines = message.split("\\r?\\n");
        StringBuilder sb = new StringBuilder();
        int count = lines.length;
        int currentIndent = -1;
        boolean startQuote = true;
        for (int i = 0; i < count; i++) {
            String line = lines[i];
            int lineIndent = computeQuoteIndentation(line);
            if (currentIndent == -1) {
                currentIndent = lineIndent;
            }
            String next = (i < (count - 1)) ? lines[i + 1] : "";
            int nextIndent = computeQuoteIndentation(next);
            int diffIndent = currentIndent - nextIndent;
            String stripped = stripQuoteIndentation(next.trim());
            boolean nextLineIsSingleWord = !stripped.contains(" ") && !stripped.isEmpty();
            boolean saltIndent = !((diffIndent == 1 && !nextLineIsSingleWord) || diffIndent < 0);
            if (lineIndent == 0 || (currentIndent != nextIndent && !saltIndent)) {
                if (sb.length() == 0) {
                    sb.append(line);
                } else if (sb.substring(sb.length() - 1).equals("\n")) {
                    sb.append(line.trim());
                } else {
                    sb.append(" ");
                    sb.append(stripQuoteIndentation(line.trim()));
                }
                sb.append("\n");
                startQuote = true;
                currentIndent = lineIndent;
            } else {
                if (startQuote || !saltIndent) {
                    sb.append(line.trim());
                    startQuote = false;
                } else {
                    sb.append(" ");
                    sb.append(stripQuoteIndentation(line.trim()));
                }
            }

        }
        return sb.toString();
    }

    public static String obtainQuote(String message) {
        String[] lines = message.split("\\r?\\n");
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            int lineIndent = computeQuoteIndentation(line);
            if (lineIndent > 0) {
                for (int j = 0; j < lineIndent; j++) {
                    sb.append(NON_PRINTABLE_CHAR);
                }
                int start = firstNonQuoteChar(line);
                if (start != -1) {
                    sb.append(line.substring(start));
                } else {
                    sb.append(" ");
                }
            } else {
                sb.append(line);
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private static int computeQuoteIndentation(String line) {
        String l = line.replaceAll(" ", "");
        int c = l.length();
        int indent = 0;
        for (int i = 0; i < c; i++) {
            if (l.charAt(i) != '>') {
                break;
            }
            indent++;
        }
        return indent;
    }

    private static String stripQuoteIndentation(String line) {
        int c = line.length();
        int i = 0;
        for (; i < c; i++) {
            char cc = line.charAt(i);
            if (cc != ' ' && cc != '>') {
                break;
            }
        }
        return i < line.length() ? line.substring(i) : "";
    }

    private static int firstNonQuoteChar(String line) {
        int c = line.length();
        for (int i = 0; i < c; i++) {
            char cc = line.charAt(i);
            if (cc != ' ' && cc != '>') {
                return i;
            }
        }
        return -1;
    }

    public static String obtainQuoteFromMessage(String msg) {
        Matcher matcher = QUOTE_REGEXP.matcher(msg);
        if (matcher.find()) {
            StringBuilder sb = new StringBuilder();
            int last = 0;
            do {
                int start = matcher.start(1) - QUOTE_START_TAG.length();
                int end = matcher.end(1) + QUOTE_END_TAG.length();
                if (last < start) {
                    sb.append(msg.substring(last, start));
                }

                String quote = (" > " + matcher.group(1))
                        .replaceAll("\n", "\n > ")
                        .replaceAll(NON_PRINTABLE_CHAR, " > ");
                while (true) {
                    String m = quote;
                    quote = quote.replaceAll("> {2}>", "> >");
                    if (quote.equals(m)) {
                        break;
                    }
                }
                sb.append(quote);
                last = end;
            } while (matcher.find());
            if (last < msg.length()) {
                sb.append(msg.substring(last));
            }
            return sb.toString();
        }
        return msg;
    }

    public static String obtainPreFormatMessage(String msg) {
        // Remove any pre-formated whitespace
        final Matcher matcher = NON_WHITESPACE.matcher(msg);
        if (matcher.find() && matcher.start() > 0) {
            final String r = (String.format("%1$-" + matcher.start() + "s", " "));
            final String t = (String.format("%1$-" + matcher.start() + "s", "\t"));
            msg = msg.replaceFirst(r, "").replaceAll("\n" + r, "\n")
                    .replaceFirst(t, "").replaceAll("\n" + t, "\n");
        }

        return NON_PRINTABLE_CHAR2 + msg + NON_PRINTABLE_CHAR2;
    }

    public static String quoteMessage(String prev, String msg) {
        if (msg.contains("\n\n")) {
            msg = msg.substring(msg.indexOf("\n\n") + 2);
        }
        msg = QUOTE_START_TAG + StringHelper.obtainQuote(msg)  + QUOTE_END_TAG + "\n\n";
        if (!TextUtils.isEmpty(prev) && !prev.endsWith("\n")) {
            msg = prev + "\n" + msg;
        } else {
            msg = prev + msg;
        }
        return msg;
    }

    public static String removeExtraLines(String msg) {
        if (msg.length() == 0) {
            return msg;
        }
        int p = msg.length();
        while (p > 0 && (msg.charAt(p - 1) == '\n' || msg.charAt(p - 1) == '\r')) {
            p--;
        }
        return msg.substring(0, p);
    }

    public static int countOccurrences(String find, String in) {
        return countOccurrences(find, in, 0, in.length());
    }

    public static int countOccurrences(String find, String in, int start, int end) {
        int count = 0;
        Pattern pattern = Pattern.compile(find);
        Matcher matcher = pattern.matcher(in);
        while (matcher.find()) {
            if (matcher.start() >= start && matcher.end() <= end) {
                count++;
            }
        }
        return count;
    }

    public static Uri buildUriAndEnsureScheme(String src) {
        Uri uri = Uri.parse(src);
        if (uri.getScheme() == null) {
            // Assume we are talking about an http url
            uri = Uri.parse("http://" + src);
        }
        return uri;
    }

    public static String getSafeLastPathSegment(Uri uri) {
        if (uri == null) {
            return null;
        }

        if (uri.getLastPathSegment() != null) {
            return uri.getLastPathSegment();
        }

        String u = uri.toString();
        if (u.endsWith("/")) {
            u = u.substring(0, u.length() - 1);
        }
        int pos = u.lastIndexOf("/", 9);
        if (pos != -1) {
            return u.substring(u.lastIndexOf("/") + 1);
        }
        return null;
    }

    public static String getFileExtension(File file) {
        if (file == null) {
            return null;
        }
        final String name = file.getName();
        int pos = name.lastIndexOf(".");
        if (pos != -1 && !name.endsWith(".")) {
            return name.substring(pos + 1);
        }
        return null;
    }

    public static String getFileNameWithoutExtension(File file) {
        if (file == null) {
            return null;
        }
        String name = file.getName();
        String ext = getFileExtension(file);
        if (ext == null) {
            return name;
        }
        return name.substring(0, name.length() - ext.length() - 1);
    }

    public static String getMimeType(File file) {
        // Extract the mime/type of the file
        String ext = getFileExtension(file);
        String mediaType = null;
        if (ext != null) {
            mediaType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
        }
        if (mediaType == null) {
            mediaType = "application/octet-stream";
        }
        return mediaType;
    }

    public static boolean isOnlyNumeric(String s) {
        return NUMERIC_REGEXP.matcher(s).matches();
    }

    public static String removeAllAttachments(String s) {
        if (s == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (String l : s.split("\n")) {
            final String ll = ATTACHMENT_REGEXP.matcher(l).replaceAll("");
            if ((ll.isEmpty() && !l.isEmpty()) || ll.trim().equals(">")) {
                continue;
            }
            sb.append(ll).append("\n");
        }
        return sb.toString().trim();
    }

    public static List<Attachment> extractAllAttachments(ChangeMessageInfo message) {
        List<Attachment> attachments = new ArrayList<>();
        if (message != null && message.message != null) {
            for (String l : message.message.split("\n")) {
                if (!l.trim().startsWith(">")) {
                    Matcher m = ATTACHMENT_REGEXP.matcher(l);
                    while (m.find()) {
                        final String group = m.group();
                        try {
                            Attachment attachment = SerializationManager.getInstance().fromJson(
                                    group.substring("![ATTACHMENT:{".length() - 1,
                                            group.indexOf("}](") + 1), Attachment.class);
                            attachment.mMessageId = message.id;
                            attachment.mUrl = group.substring(
                                    group.indexOf("}](") + 3, group.length() - 1);
                            if (!TextUtils.isEmpty(attachment.mName)
                                    && !TextUtils.isEmpty(attachment.mMimeType)
                                    && !TextUtils.isEmpty(attachment.mUrl)) {
                                attachments.add(attachment);
                            }
                        } catch (Exception ex) {
                            Log.w(TAG, "Can't parse attachment: " + group, ex);
                        }
                    }
                }
            }
        }
        return attachments;
    }

    public static String extractLinkFromHtml(String html) {
        if (html == null) {
            return null;
        }
        Matcher matcher = LINK_HTML_REGEXP.matcher(html);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    public static String firstLine(String msg) {
        int pos = msg.indexOf("\n");
        if (pos > 0) {
            return msg.substring(0, pos);
        }
        return msg;
    }

    public static Number parseNumberWithSign(String number) {
        try {
            final DecimalFormat df = new DecimalFormat("+#;-#");
            return df.parse(number);
        } catch (ParseException ex) {
            // ignore
        }
        return 0;
    }

    public static String fold(String message) {
        return message.replaceAll("\r\n", " ").replaceAll("\r", " ").replaceAll("\n", " ");
    }
}
