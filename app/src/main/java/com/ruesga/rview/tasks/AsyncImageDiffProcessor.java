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
package com.ruesga.rview.tasks;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PictureDrawable;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.Display;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;

import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGParseException;
import com.ruesga.rview.R;
import com.ruesga.rview.misc.BitmapUtils;
import com.ruesga.rview.misc.VectorDrawableConverter;
import com.ruesga.rview.widget.DiffView.ImageDiffModel;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Locale;

public class AsyncImageDiffProcessor extends AsyncTask<Void, Void, ImageDiffModel> {

    private static final String TAG = "AsyncImageDiffProcessor";

    public interface OnImageDiffProcessEndedListener {
        void onImageDiffProcessEnded(ImageDiffModel model);
    }

    private final Context mContext;
    private final OnImageDiffProcessEndedListener mCallback;
    private final File mLeft;
    private final File mRight;
    private final int mSize;

    public AsyncImageDiffProcessor(Context context, File left, File right,
            OnImageDiffProcessEndedListener cb) {
        mContext = context;
        mCallback = cb;
        mLeft = left;
        mRight = right;

        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        mSize = Math.max(size.x, size.y);
    }

    @Override
    protected ImageDiffModel doInBackground(Void... params) {
        ImageDiffModel model = new ImageDiffModel();
        if (mLeft != null) {
            Pair<Drawable, int[]> l = loadAsDrawable(mLeft);
            if (l != null && l.first != null) {
                model.left = l.first;
                int size = ((int) Math.floor(mLeft.length() / 1024)) + 1;
                model.sizeLeft = mContext.getString(R.string.diff_viewer_image_size, size);
                model.dimensionsLeft = mContext.getString(
                        R.string.diff_viewer_image_dimensions, l.second[0], l.second[1]);
            }
        }
        if (mRight != null) {
            Pair<Drawable, int[]> r = loadAsDrawable(mRight);
            if (r != null && r.first != null) {
                model.right = r.first;
                int size = ((int) Math.floor(mRight.length() / 1024)) + 1;
                model.sizeRight = mContext.getString(R.string.diff_viewer_image_size, size);
                model.dimensionsRight = mContext.getString(
                        R.string.diff_viewer_image_dimensions, r.second[0], r.second[1]);
            }
        }

        if (model.sizeLeft == null) {
            model.sizeLeft = "-";
        }
        if (model.dimensionsLeft == null) {
            model.dimensionsLeft = "-";
        }

        if (model.sizeRight == null) {
            model.sizeRight = "-";
        }
        if (model.dimensionsRight == null) {
            model.dimensionsRight = "-";
        }

        return model;
    }

    @Override
    protected void onPostExecute(ImageDiffModel model) {
        mCallback.onImageDiffProcessEnded(model);
    }

    private Pair<Drawable, int[]> loadAsDrawable(File file) {
        // 1.- Decode bitmap
        int[] size = BitmapUtils.decodeBitmapSize(file);
        if (size[0] != -1 && size[1] != -1) {
            return loadFromBitmap(file, size);
        }

        // Read the xml header
        String header = readXmlHeader(file);
        if (header == null) {
            return null;
        }

        // 2.- Decode SVG
        if (isSvg(file, header)) {
            return loadFromSvg(file);
        }

        // 3.- Decode Vector Drawable
        if (isVectorDrawable(file, header)) {
            return loadFromVectorDrawable(file);
        }

        return null;
    }

    private static boolean isSvg(File file, String header) {
        return !(file == null || !file.exists()) && hasXmlTag(header, "svg");
    }

    private static boolean isVectorDrawable(File file, String header) {
        return !(file == null || !file.exists()) && hasXmlTag(header, "vector");
    }

    private Pair<Drawable, int[]> loadFromBitmap(File file, int[] size) {
        Bitmap bitmap = BitmapUtils.decodeBitmap(file, mSize, mSize);
        if (bitmap != null) {
            return new Pair<>(new BitmapDrawable(mContext.getResources(), bitmap), size);
        }

        Log.e(TAG, "Can't load " + file.getAbsolutePath() + " as image.");
        return null;
    }

    @SuppressWarnings("TryWithIdenticalCatches")
    private Pair<Drawable, int[]> loadFromSvg(File file) {
        try {
            return loadSvg(new BufferedInputStream(new FileInputStream(file)));

        } catch (IOException ex) {
            Log.e(TAG, "Can't parse " + file.getAbsolutePath() + " as SVG.", ex);

        } catch (SVGParseException ex) {
            Log.e(TAG, "Can't parse " + file.getAbsolutePath() + " as SVG.", ex);
        }

        return null;
    }

    @SuppressWarnings("TryWithIdenticalCatches")
    private Pair<Drawable, int[]> loadFromVectorDrawable(File file) {
        try {
            // Convert the vector drawable to a svg document
            CharSequence svgDocument = VectorDrawableConverter.toSvg(
                    new BufferedReader(new FileReader(file)));
            return loadSvg(new ByteArrayInputStream(svgDocument.toString().getBytes()));

        } catch (Exception ex) {
            Log.e(TAG, "Can't parse " + file.getAbsolutePath() + " as VectorDrawable.", ex);

        }

        return null;
    }

    private Pair<Drawable, int[]> loadSvg(InputStream is) throws IOException, SVGParseException {
        SVG svg = SVG.getFromInputStream(is);
        int[] size = new int[2];
        size[0] = (int) svg.getDocumentViewBox().width();
        size[1] = (int) svg.getDocumentViewBox().height();
        svg.setDocumentWidth(mSize);
        svg.setDocumentHeight(mSize);
        return new Pair<>(new PictureDrawable(svg.renderToPicture()), size);
    }

    private static String readXmlHeader(File file) {
        if (file != null && file.exists()) {
            Reader reader = null;
            try {
                reader = new BufferedReader(new FileReader(file));
                char[] data = new char[1024];
                int read = reader.read(data, 0, 1024);
                if (read != -1) {
                    return new String(data, 0, read);
                }
            } catch (IOException ex) {
                // Ignore
            } finally {
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (IOException ex) {
                    // Ignore
                }
            }
        }
        return null;
    }

    private static boolean hasXmlTag(String header, String xmlTag) {
        return header.contains("<" + xmlTag + " ") || header.contains(":" + xmlTag + " ");
    }

    public static boolean hasImagePreview(File file, File content) {
        String name = file.getName().toLowerCase(Locale.US);
        String ext = "";
        int p = name.lastIndexOf(".");
        if (p != -1) {
            ext = name.substring(p + 1);
        }
        String mimeType = "";
        if (!TextUtils.isEmpty(ext)) {
            MimeTypeMap a = MimeTypeMap.getSingleton();
            mimeType = a.getMimeTypeFromExtension(ext);
        }
        String header = readXmlHeader(content);

        // 1.- Any image
        // 2.- A svg
        // 3.- A xml with "svg" tag or "vector" tag
        return (mimeType != null && mimeType.startsWith("image/")) || ext.equals("svg")
                || (header != null && ext.equals("xml")
                    && (hasXmlTag(header, "svg") || hasXmlTag(header, "vector")));
    }
}
