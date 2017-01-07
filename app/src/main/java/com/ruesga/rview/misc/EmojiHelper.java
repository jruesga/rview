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

import android.os.Build;

import java.util.regex.Pattern;

public final class EmojiHelper {
    private static final String[] TEXT =
            {
                    "<3",
                    "^_^",
                    "-_-",
                    "o.O",
                    ">:O",
                    "X(",
                    "\\o/",
                    "3:)",
                    "O:)",
                    ">:(",
                    "B-)",
                    "B|",
                    ":'(",
                    ":*",
                    ":P",
                    ":D",
                    ":'D",
                    ":))",
                    ":((",
                    ":O",
                    ";)",
                    ":/",
                    ":-)",
                    ":)",
                    ":(",
                    ":-(",
                    ":-B",
                    ":-?",
                    "|-)",
                    ":-&",
                    ":P",
                    ":D",
                    ":S"
            };
    private static final Pattern[] REGEXP1 =
            {
                    Pattern.compile("^<3", Pattern.MULTILINE),
                    Pattern.compile("^\\^_\\^", Pattern.MULTILINE),
                    Pattern.compile("^-_-", Pattern.MULTILINE),
                    Pattern.compile("^o.O", Pattern.MULTILINE),
                    Pattern.compile("^>:O", Pattern.MULTILINE),
                    Pattern.compile("^X\\(", Pattern.MULTILINE),
                    Pattern.compile("^\\\\o/", Pattern.MULTILINE),
                    Pattern.compile("^3:\\)", Pattern.MULTILINE),
                    Pattern.compile("^O:\\)", Pattern.MULTILINE),
                    Pattern.compile("^>:\\(", Pattern.MULTILINE),
                    Pattern.compile("^B-\\)", Pattern.MULTILINE),
                    Pattern.compile("^B\\|", Pattern.MULTILINE),
                    Pattern.compile("^:'\\(", Pattern.MULTILINE),
                    Pattern.compile("^:\\*", Pattern.MULTILINE),
                    Pattern.compile("^:P", Pattern.MULTILINE),
                    Pattern.compile("^:D", Pattern.MULTILINE),
                    Pattern.compile("^:'D", Pattern.MULTILINE),
                    Pattern.compile("^:\\)\\)", Pattern.MULTILINE),
                    Pattern.compile("^:\\(\\(", Pattern.MULTILINE),
                    Pattern.compile("^:O", Pattern.MULTILINE),
                    Pattern.compile("^;\\)", Pattern.MULTILINE),
                    Pattern.compile("^:/", Pattern.MULTILINE),
                    Pattern.compile("^:-\\)", Pattern.MULTILINE),
                    Pattern.compile("^:\\)", Pattern.MULTILINE),
                    Pattern.compile("^:\\(", Pattern.MULTILINE),
                    Pattern.compile("^:-\\(", Pattern.MULTILINE),
                    Pattern.compile("^:-B", Pattern.MULTILINE),
                    Pattern.compile("^:-\\?", Pattern.MULTILINE),
                    Pattern.compile("^\\|-\\)", Pattern.MULTILINE),
                    Pattern.compile("^:-&", Pattern.MULTILINE),
                    Pattern.compile("^:P", Pattern.MULTILINE),
                    Pattern.compile("^:D", Pattern.MULTILINE),
                    Pattern.compile("^:S", Pattern.MULTILINE)
            };
    private static final Pattern[] REGEXP2 =
            {
                    Pattern.compile(" <3"),
                    Pattern.compile(" \\^_\\^"),
                    Pattern.compile(" -_-"),
                    Pattern.compile(" o.O"),
                    Pattern.compile(" >:O"),
                    Pattern.compile(" X\\("),
                    Pattern.compile(" \\\\o/"),
                    Pattern.compile(" 3:\\)"),
                    Pattern.compile(" O:\\)"),
                    Pattern.compile(" >:\\("),
                    Pattern.compile(" B-\\)"),
                    Pattern.compile(" B\\|"),
                    Pattern.compile(" :'\\("),
                    Pattern.compile(" :\\*"),
                    Pattern.compile(" :P"),
                    Pattern.compile(" :D"),
                    Pattern.compile(" :'D"),
                    Pattern.compile(" :\\)\\)"),
                    Pattern.compile(" :\\(\\("),
                    Pattern.compile(" :O"),
                    Pattern.compile(" ;\\)"),
                    Pattern.compile(" :/"),
                    Pattern.compile(" :-\\)"),
                    Pattern.compile(" :\\)"),
                    Pattern.compile(" :\\("),
                    Pattern.compile(" :-\\("),
                    Pattern.compile(" :-B"),
                    Pattern.compile(" :-\\?"),
                    Pattern.compile(" \\|-\\)"),
                    Pattern.compile(" :-&"),
                    Pattern.compile(" :P"),
                    Pattern.compile(" :D"),
                    Pattern.compile(" :S")
            };
    private static final int[] CODES =
            {
                    0x1F60D, // <3
                    0x1F60A, // ^_^
                    0x1F611, // -_-
                    0x1F632, // o.O
                    0x1F62C, // >:O
                    0x1F62C, // X(
                    0x1F64C, // \o/
                    0x1F47F, // 3:)
                    0x1F607, // O:)
                    0x1F620, // >:(
                    0x1F60E, // B-)
                    0x1F60E, // B|
                    0x1F622, // :'(
                    0x1F61A, // :*
                    0x1F61B, // :P
                    0x1F601, // :D
                    0x1F602, // :'D
                    0x1F606, // :))
                    0x1F62D, // :((
                    0x1F62E, // :O
                    0x1F609, // ;)
                    0x1F615, // :/
                    0x1F600, // :-)
                    0x1F600, // :)
                    0x1F641, // :(
                    0x1F641, // :-(
                    0x1F913, // :-B
                    0x1F914, // :-?
                    0x1F634, // |-)
                    0x1F912, // :-&
                    0x1F61B, // :P
                    0x1F601, // :D
                    0x1F615  // :S
            };

    public static String createEmoji(String msg) {
        // Only KitKat and up have a colorful emoji support
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            int count = TEXT.length;
            for (int i = 0; i < count; i++) {
                if (msg.contains(TEXT[i])) {
                    String emoji = getEmijoByUnicode(CODES[i]);
                    msg = REGEXP1[i].matcher(msg).replaceAll(emoji);
                    msg = REGEXP2[i].matcher(msg).replaceAll(" " + emoji);
                }
            }
        }
        return msg;
    }

    public static String getEmijoByUnicode(int unicode){
        return new String(Character.toChars(unicode));
    }
}
