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

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
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

public class VectorDrawableConverter {

    private static final String TAG = "VectorDrawableConverter";

    private static final String SVG_NAMESPACE = "http://www.w3.org/2000/svg";


    public static CharSequence toSvg(Context ctx, Reader in) {
        return writeSvg(ctx, parseVector(in));
    }

    private static CharSequence writeSvg(Context ctx, Vector vector) {
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
                    serializeGroup(ctx, serializer, vector, group);
                }
                for (Path path : vector.mPaths)  {
                    serializePath(ctx, serializer, path);
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

    private static void serializeGroup(Context ctx, XmlSerializer serializer,
            Vector vector, Group group) throws IOException {
        serializer.startTag(SVG_NAMESPACE, "g");
        if (group.mName != null) {
            serializer.attribute(SVG_NAMESPACE, "id", group.mName);
        }
        String transform = "";
        if (group.mRotation != null) {
            transform += " rotate("
                    + String.format(Locale.US, "%f", group.mRotation)
                    + ","
                    + (group.mPivotX == null ? "" : String.format(Locale.US, "%f", group.mPivotX))
                    + ","
                    + (group.mPivotY == null ? "" : String.format(Locale.US, "%f", group.mPivotY))
                    + ")";
        }
        if (group.mTranslateX != null || group.mTranslateY != null) {
            transform += " translate("
                    + (group.mTranslateX == null ? "0" : String.format(Locale.US, "%f", group.mTranslateX))
                    + ","
                    + (group.mTranslateY == null ? "0" : String.format(Locale.US, "%f", group.mTranslateY))
                    + ")";
        }
        if (group.mScaleX != null || group.mScaleY != null) {
            float scaleX = group.mScaleX == null ? 0f : group.mScaleX;
            float scaleY = group.mScaleY == null ? scaleX : group.mScaleY;
            float cx = (1 - scaleX) * vector.mWidth / 2;
            float cy = (1 - scaleY) * vector.mHeight / 2;
            transform += " translate("
                    + String.format(Locale.US, "%f", cx)
                    + ","
                    + String.format(Locale.US, "%f", cy)
                    + ")";
            transform += " scale("
                    + (group.mScaleX == null ? "0" : String.format(Locale.US, "%f", group.mScaleX))
                    + ","
                    + (group.mScaleY == null ? "0" : String.format(Locale.US, "%f", group.mScaleY))
                    + ")";
        }

        if (!TextUtils.isEmpty(transform)) {
            serializer.attribute(SVG_NAMESPACE, "transform", transform);
        }


        for (Path path : group.mPaths)  {
            serializePath(ctx, serializer, path);
        }
        serializer.endTag(SVG_NAMESPACE, "g");
    }

    private static void serializePath(Context ctx, XmlSerializer serializer, Path path)
            throws IOException {
        serializer.startTag(SVG_NAMESPACE, "path");
        if (path.mName != null) {
            serializer.attribute(SVG_NAMESPACE, "id", path.mName);
        }
        serializer.attribute(SVG_NAMESPACE, "d", path.mPathData);
        if (path.mFillColor != null) {
            serializer.attribute(SVG_NAMESPACE, "fill", toSvgColor(ctx, path.mFillColor));
        }
        if (path.mFillAlpha != null) {
            serializer.attribute(SVG_NAMESPACE, "fill-opacity",
                    String.format(Locale.US, "%f", path.mFillAlpha));
        }
        if (path.mStrokeColor != null) {
            serializer.attribute(SVG_NAMESPACE, "stroke", toSvgColor(ctx, path.mStrokeColor));
        }
        if (path.mStrokeWidth != null) {
            serializer.attribute(SVG_NAMESPACE, "stroke-width",
                    String.format(Locale.US, "%f", path.mStrokeWidth));
        }
        if (path.mStrokeAlpha != null) {
            serializer.attribute(SVG_NAMESPACE, "stroke-opacity",
                    String.format(Locale.US, "%f", path.mStrokeAlpha));
        }

        if (path.mStrokeLineCap != null) {
            serializer.attribute(SVG_NAMESPACE, "stroke-linecap", path.mStrokeLineCap.toString());
        }
        if (path.mStrokeLineJoin != null) {
            serializer.attribute(SVG_NAMESPACE, "stroke-linejoin", path.mStrokeLineJoin.toString());
        }
        if (path.mStrokeMiterLimit != null) {
            serializer.attribute(SVG_NAMESPACE, "stroke-miterlimit",
                    String.format(Locale.US, "%f", path.mStrokeMiterLimit));
        }
        if (path.mFillRule != null) {
            serializer.attribute(SVG_NAMESPACE, "fill-rule", path.mFillRule.toString());
        }


        serializer.endTag(SVG_NAMESPACE, "path");
    }

    private static String toSvgColor(Context ctx, String color) {
        try {
            int c = 0;
            if (color.startsWith("?") || color.startsWith("@")) {
                int start = color.indexOf(":") + 1;
                int end = color.indexOf("/") + 1;
                if (start == -1) {
                    start = 1;
                }
                String type = color.substring(start, end - 1);
                String name = color.substring(end);

                TypedValue typedValue = new TypedValue();
                Resources.Theme theme = ctx.getTheme();
                int id = ctx.getResources().getIdentifier(name, type, ctx.getPackageName());
                theme.resolveAttribute(id, typedValue, true);
                c = typedValue.data;
            } else {
                c = Color.parseColor(color);
            }
            return String.format("#%02X%02X%02X", Color.red(c), Color.green(c), Color.blue(c));
        } catch (Exception ex) {
            // Fallback to black
            return "#000000";
        }
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
                            vector.mWidth = getAttributeValueAsFloat(xmlParser, "viewportWidth");
                            vector.mHeight = getAttributeValueAsFloat(xmlParser, "viewportHeight");
                            break;
                        case "group":
                            if (vector != null) {
                                group = new Group();
                                group.mName = getAttributeValue(xmlParser, "name");
                                group.mRotation = getAttributeValueAsFloat(xmlParser, "rotation");
                                group.mPivotX = getAttributeValueAsFloat(xmlParser, "pivotX");
                                group.mPivotY = getAttributeValueAsFloat(xmlParser, "pivotY");
                                group.mScaleX = getAttributeValueAsFloat(xmlParser, "scaleX");
                                group.mScaleY = getAttributeValueAsFloat(xmlParser, "scaleY");
                                group.mTranslateX = getAttributeValueAsFloat(xmlParser, "translateX");
                                group.mTranslateY = getAttributeValueAsFloat(xmlParser, "translateY");
                                vector.mGroups.add(group);
                            }
                            break;

                        case "path":
                            path = new Path();
                            path.mName = getAttributeValue(xmlParser, "name");
                            path.mPathData = getAttributeValue(xmlParser, "pathData");
                            path.mFillColor = getAttributeValue(xmlParser, "fillColor");
                            path.mFillAlpha = getAttributeValueAsFloat(xmlParser, "fillAlpha");
                            path.mStrokeColor = getAttributeValue(xmlParser, "strokeColor");
                            path.mStrokeWidth = getAttributeValueAsFloat(xmlParser, "strokeWidth");
                            path.mStrokeAlpha = getAttributeValueAsFloat(xmlParser, "strokeAlpha");
                            String strokeLineCap = getAttributeValue(xmlParser, "strokeLineCap");
                            if (strokeLineCap != null) {
                                path.mStrokeLineCap = LineCap.valueOf(strokeLineCap);
                            }
                            String strokeLineJoin = getAttributeValue(xmlParser, "strokeLineJoin");
                            if (strokeLineJoin != null) {
                                path.mStrokeLineJoin = LineJoin.valueOf(strokeLineJoin);
                            }
                            path.mStrokeMiterLimit = getAttributeValueAsFloat(xmlParser, "strokeMiterLimit");
                            String fillRule = getAttributeValue(xmlParser, "fillRule");
                            if (fillRule != null) {
                                path.mFillRule = FillRule.valueOf(fillRule);
                            }

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

    private static Float getAttributeValueAsFloat(XmlPullParser xmlParser, String attrName) {
        try {
            return Float.valueOf(getAttributeValue(xmlParser, attrName));
        } catch (Exception ex) {
            return null;
        }
    }

    private enum LineCap {
        butt, round, square
    }

    private enum LineJoin {
        miter,round,bevel
    }

    private enum FillRule {
        nonzero,evenodd,inherit
    }

    private static class Path {
        private String mName;
        private String mPathData;
        private String mFillColor;
        private String mStrokeColor;
        private Float mStrokeWidth;
        private Float mStrokeAlpha;
        private Float mFillAlpha;
        private LineCap mStrokeLineCap;
        private LineJoin mStrokeLineJoin;
        private Float mStrokeMiterLimit;
        private FillRule mFillRule;
    }

    private static class Group {
        private String mName;
        private Float mRotation;
        private Float mPivotX;
        private Float mPivotY;
        private Float mScaleX;
        private Float mScaleY;
        private Float mTranslateX;
        private Float mTranslateY;

        private final List<Path> mPaths = new ArrayList<>();
    }

    private static class Vector {
        private final List<Path> mPaths = new ArrayList<>();
        private final List<Group> mGroups = new ArrayList<>();
        private float mHeight;
        private float mWidth;
    }
}
