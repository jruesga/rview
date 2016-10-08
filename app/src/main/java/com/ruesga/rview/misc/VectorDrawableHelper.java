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

import android.graphics.Color;
import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class VectorDrawableHelper {

    private static final String TAG = "VectorDrawableHelper";

    private static final String SVG_NAMESPACE = "http://www.w3.org/2000/svg";


    public static CharSequence toSvg(Reader in) {
        return writeSvg(parseVector(in));
    }

    private static CharSequence writeSvg(Vector vector) {
        if (vector != null) {
            try {
                CharArrayWriter writer = new CharArrayWriter();
                XmlSerializer serializer = Xml.newSerializer();
                serializer.setOutput(writer);
                serializer.startTag(SVG_NAMESPACE, "svg");
                serializer.attribute(SVG_NAMESPACE, "width",
                        String.format(Locale.US, "%f", vector.mWidth));
                serializer.attribute(SVG_NAMESPACE, "height",
                        String.format(Locale.US, "%f", vector.mHeight));
                serializer.attribute(SVG_NAMESPACE, "viewBox",
                        String.format(Locale.US, "0 0 %f %f", vector.mWidth, vector.mHeight));
                for (Group group : vector.mGroups)  {
                    serializer.startTag(SVG_NAMESPACE, "g");
                    for (Path path : group.mPaths)  {
                        serializePath(serializer, path);
                    }
                    serializer.endTag(SVG_NAMESPACE, "g");
                }
                for (Path path : vector.mPaths)  {
                    serializePath(serializer, path);
                }
                serializer.endTag(SVG_NAMESPACE, "svg");
                serializer.endDocument();
                serializer.flush();
                return writer.toString();

            } catch (Exception ex) {
                Log.e(TAG, "Can't write svg xml", ex);
            }
        }
        return null;
    }

    private static void serializePath(XmlSerializer serializer, Path path) throws IOException {
        serializer.startTag(SVG_NAMESPACE, "path");
        serializer.attribute(SVG_NAMESPACE, "d", path.mPathData);
        if (path.mFillColor != null) {
            serializer.attribute(SVG_NAMESPACE, "fill", toSvgColor(path.mFillColor));
        }
        serializer.endTag(SVG_NAMESPACE, "path");
    }

    private static String toSvgColor(String color) {
        int c = Color.parseColor(color);
        return String.format("#%02X%02X%02X", Color.red(c), Color.green(c), Color.blue(c));
    }

    @SuppressWarnings("ConstantConditions")
    private static Vector parseVector(Reader in) {

        try {
            XmlPullParserFactory xmlFactory = XmlPullParserFactory.newInstance();
            xmlFactory.setNamespaceAware(true);
            XmlPullParser xmlParser = xmlFactory.newPullParser();
            xmlParser.setInput(in);

            Vector vector = null;
            Group group = null;
            Path path;
            while (xmlParser.next() != XmlPullParser.END_DOCUMENT) {
                String name = xmlParser.getName();
                int type = xmlParser.getEventType();
                if (name == null) {
                    continue;
                }

                if (type == XmlPullParser.START_TAG) {
                    switch (name) {
                        case "vector":
                            vector = new Vector();
                            vector.mWidth = getAttributeValueAsDouble(xmlParser, "viewportWidth");
                            vector.mHeight = getAttributeValueAsDouble(xmlParser, "viewportHeight");
                            break;
                        case "group":
                            if (vector != null) {
                                group = new Group();
                                vector.mGroups.add(group);
                            }
                            break;

                        case "path":
                            path = new Path();
                            path.mFillColor = getAttributeValue(xmlParser, "fillColor");
                            path.mPathData = getAttributeValue(xmlParser, "pathData");

                            if (group != null) {
                                group.mPaths.add(path);
                            } else if (vector != null) {
                                vector.mPaths.add(path);
                            }
                            break;
                    }
                } else if (type == XmlPullParser.END_TAG) {
                    switch (name) {
                        case "group":
                            group = null;
                            break;
                    }
                }

                xmlParser.next();
            }

            return vector;

        } catch (Exception ex) {
            Log.e(TAG, "Can't parse vector xml", ex);
        }

        return null;
    }

    private static String getAttributeValue(XmlPullParser xmlParser, String attrName) {
        int count = xmlParser.getAttributeCount();
        for (int i = 0; i < count; i++) {
            String name = xmlParser.getAttributeName(i);
            if (name.equals(attrName)) {
                return xmlParser.getAttributeValue(i);
            }
        }

        return null;
    }

    private static Double getAttributeValueAsDouble(XmlPullParser xmlParser, String attrName) {
        try {
            return Double.valueOf(getAttributeValue(xmlParser, attrName));
        } catch (Exception ex) {
            return null;
        }
    }


    private static class Path {
        private String mPathData;
        private String mFillColor;

    }

    private static class Group {
        private final List<Path> mPaths = new ArrayList<>();
    }

    private static class Vector {
        private final List<Path> mPaths = new ArrayList<>();
        private final List<Group> mGroups = new ArrayList<>();
        private double mHeight;
        private double mWidth;
    }
}
