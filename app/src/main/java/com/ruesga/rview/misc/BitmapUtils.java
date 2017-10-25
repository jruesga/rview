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

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.media.ExifInterface;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.Log;

import org.apache.commons.io.IOUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class BitmapUtils {

    private static final String TAG = "BitmapUtils";

    public static Bitmap convertToBitmap(Drawable dw) {
        Bitmap bitmap;

        if (dw instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) dw;
            if(bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        if(dw.getIntrinsicWidth() <= 0 || dw.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        } else {
            bitmap = Bitmap.createBitmap(
                    dw.getIntrinsicWidth(),
                    dw.getIntrinsicHeight(),
                    Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        dw.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        dw.draw(canvas);
        return bitmap;
    }

    public static Drawable tintDrawable(Resources res, Drawable src, int color) {
        return new BitmapDrawable(res, tintBitmap(convertToBitmap(src), color));
    }

    public static Bitmap tintBitmap(Bitmap src, int color) {
        Paint paint = new Paint();
        paint.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN));
        Bitmap dst = Bitmap.createBitmap(
                src.getWidth(), src.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(dst);
        canvas.drawBitmap(src, 0, 0, paint);
        return dst;
    }

    public static Bitmap text2Bitmap(String text, int color, float size) {
        if (TextUtils.isEmpty(text)) {
            return null;
        }

        Paint paint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextSize(size);
        paint.setColor(color);
        paint.setTextAlign(Paint.Align.LEFT);

        float baseline = -paint.ascent();
        int width = (int) (paint.measureText(text) + 0.5f);
        int height = (int) (baseline + paint.descent() + 0.5f);

        Bitmap image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(image);
        canvas.drawText(text, 0, baseline, paint);
        return image;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static File optimizeImage(Context context, InputStream source,
                Bitmap.CompressFormat format, int quality) throws IOException {
        // Copy original source to a temp file
        File f = null;
        OutputStream os = null;
        try {
            f = CacheHelper.createNewTemporaryFile(context, "." + format.name());
            if (f != null) {
                os = new BufferedOutputStream(new FileOutputStream(f), 4096);
                IOUtils.copy(source, os);
                IOUtils.closeQuietly(os);

                // Compress to webp format
                long start = System.currentTimeMillis();
                int[] size = decodeBitmapSize(f);
                if (size[0] != -1 && size[1] != -1) {
                    Rect r = new Rect(0, 0, size[0], size[1]);
                    adjustRectToMinimumSize(r, calculateMaxAvailableSize(context));
                    Bitmap src = createUnscaledBitmap(f, r.width(), r.height());

                    try {
                        long originalSize = f.length();
                        os = new BufferedOutputStream(new FileOutputStream(f), 4096);
                        src.compress(format, quality, os);
                        IOUtils.closeQuietly(os);

                        long end = System.currentTimeMillis();
                        Log.d(TAG, "Compressed " + f + " to " + format.name() + " in "
                                + (end - start) + "ms" + "; dimensions " + src.getWidth()
                                + "x" + src.getHeight() + "; size: " + f.length()
                                + "; original: "  + originalSize);
                        return f;

                    } finally {
                        src.recycle();
                    }
                } else {
                    f.delete();
                }
            }
        } catch (IOException ex) {
            IOUtils.closeQuietly(os);
            if (f != null) {
                f.delete();
            }
            throw ex;
        } finally {
            IOUtils.closeQuietly(os);
            IOUtils.closeQuietly(source);
        }

        return null;
    }


    /**
     * ScalingLogic defines how scaling should be carried out if source and
     * destination image has different aspect ratio.
     *
     * CROP: Scales the image the minimum amount while making sure that at least
     * one of the two dimensions fit inside the requested destination area.
     * Parts of the source image will be cropped to realize this.
     *
     * FIT: Scales the image the minimum amount while making sure both
     * dimensions fit inside the requested destination area. The resulting
     * destination dimensions might be adjusted to a smaller size than
     * requested.
     */
    public enum ScalingLogic {
        CROP, FIT
    }

    @SuppressWarnings("deprecation")
    public static Bitmap decodeBitmap(InputStream bitmap) {
        final Options options = new Options();
        options.inScaled = false;
        options.inDither = true;
        options.inPreferQualityOverSpeed = false;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        return BitmapFactory.decodeStream(bitmap, null, options);
    }

    @SuppressWarnings("deprecation")
    public static Bitmap decodeBitmap(File file, int dstWidth, int dstHeight) {
        // First decode with inJustDecodeBounds=true to check dimensions
        final Options options = new Options();
        options.inScaled = false;
        options.inDither = true;
        options.inPreferQualityOverSpeed = false;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        // Deprecated, but still valid for KitKat and lower apis
        options.inPurgeable = true;
        options.inInputShareable = true;
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), options);

        // Decode the bitmap with inSampleSize set
        options.inSampleSize = calculateBitmapRatio(options, dstWidth, dstHeight);
        options.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        if (bitmap == null) {
            return null;
        }

        // Test if the bitmap has exif format, and decode properly
        Bitmap out = decodeExifBitmap(file, bitmap);
        if (out != null && !out.equals(bitmap)) {
            bitmap.recycle();
        }
        return out;
    }

    public static int[] decodeBitmapSize(File file) {
        // First decode with inJustDecodeBounds=true to check dimensions
        final Options options = new Options();
        options.inScaled = false;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        return new int[] {options.outWidth, options.outHeight};
    }

    @SuppressWarnings("deprecation")
    public static Bitmap createUnscaledBitmap(File file, int dstWidth, int dstHeight) {
        // Get the dimensions of the bitmap
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        options.inDither = true;
        options.inPreferQualityOverSpeed = false;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        // Deprecated, but still valid for KitKat and lower apis
        options.inPurgeable = true;
        options.inInputShareable = true;
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), options);

        // Determine how much to scale down the image
        int photoWidth = options.outWidth;
        int photoHeight = options.outHeight;

        // Decode the image file into a Bitmap sized to fill the view
        options.inJustDecodeBounds = false;
        options.inSampleSize = Math.max(Math.round(photoWidth / dstWidth),
                Math.round(photoHeight / dstHeight));
        return decodeExifBitmap(file, BitmapFactory.decodeFile(file.getAbsolutePath(), options));
    }

    public static Bitmap createScaledBitmap(Bitmap unscaledBitmap, int dstWidth, int dstHeight,
            ScalingLogic scalingLogic) {
        if (unscaledBitmap.getWidth() == dstWidth && unscaledBitmap.getHeight() == dstHeight) {
            return unscaledBitmap;
        }
        Rect srcRect = calculateSrcRect(unscaledBitmap.getWidth(), unscaledBitmap.getHeight(),
                dstWidth, dstHeight, scalingLogic);
        Rect dstRect = calculateDstRect(unscaledBitmap.getWidth(), unscaledBitmap.getHeight(),
                dstWidth, dstHeight, scalingLogic);
        Bitmap scaledBitmap = Bitmap.createBitmap(dstRect.width(), dstRect.height(),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(scaledBitmap);
        canvas.drawBitmap(unscaledBitmap, srcRect, dstRect, new Paint(Paint.FILTER_BITMAP_FLAG));
        return scaledBitmap;
    }

    private static Bitmap decodeExifBitmap(File file, Bitmap src) {
        if (src != null) {
            try {
                // Try to load the bitmap as a bitmap file
                ExifInterface exif = new ExifInterface(file.getAbsolutePath());
                int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
                if (orientation == ExifInterface.ORIENTATION_UNDEFINED
                        || orientation == ExifInterface.ORIENTATION_NORMAL) {
                    return src;
                }
                Matrix matrix = new Matrix();
                if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
                    matrix.postRotate(90);
                } else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) {
                    matrix.postRotate(180);
                } else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) {
                    matrix.postRotate(270);
                } else if (orientation == ExifInterface.ORIENTATION_FLIP_HORIZONTAL) {
                    matrix.setScale(-1, 1);
                    matrix.postTranslate(src.getWidth(), 0);
                } else if (orientation == ExifInterface.ORIENTATION_FLIP_VERTICAL) {
                    matrix.setScale(1, -1);
                    matrix.postTranslate(0, src.getHeight());
                }
                // Rotate the bitmap
                return Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), matrix, true);
            } catch (IOException e) {
                // Ignore
            }
        }
        return src;
    }

    private static int calculateBitmapRatio(Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    public static Rect calculateSrcRect(int srcWidth, int srcHeight, int dstWidth, int dstHeight,
            ScalingLogic scalingLogic) {
        if (scalingLogic == ScalingLogic.CROP) {
            final float srcAspect = (float)srcWidth / (float)srcHeight;
            final float dstAspect = (float)dstWidth / (float)dstHeight;

            if (srcAspect > dstAspect) {
                final int srcRectWidth = (int)(srcHeight * dstAspect);
                final int srcRectLeft = (srcWidth - srcRectWidth) / 2;
                return new Rect(srcRectLeft, 0, srcRectLeft + srcRectWidth, srcHeight);
            } else {
                final int srcRectHeight = (int)(srcWidth / dstAspect);
                final int scrRectTop = (srcHeight - srcRectHeight) / 2;
                return new Rect(0, scrRectTop, srcWidth, scrRectTop + srcRectHeight);
            }
        } else {
            return new Rect(0, 0, srcWidth, srcHeight);
        }
    }

    public static Rect calculateDstRect(int srcWidth, int srcHeight, int dstWidth, int dstHeight,
            ScalingLogic scalingLogic) {
        if (scalingLogic == ScalingLogic.FIT) {
            final float srcAspect = (float)srcWidth / (float)srcHeight;
            final float dstAspect = (float)dstWidth / (float)dstHeight;

            if (srcAspect > dstAspect) {
                return new Rect(0, 0, dstWidth, (int)(dstWidth / srcAspect));
            } else {
                return new Rect(0, 0, (int)(dstHeight * srcAspect), dstHeight);
            }
        } else {
            return new Rect(0, 0, dstWidth, dstHeight);
        }
    }

    public static boolean isPowerOfTwo(Bitmap bitmap){
        return isPowerOfTwo(bitmap.getWidth(), bitmap.getHeight());
    }

    public static boolean isPowerOfTwo(int w, int h){
        return isPowerOfTwo(w) && isPowerOfTwo(h);
    }

    private static boolean isPowerOfTwo(int x) {
        while (((x % 2) == 0) && x > 1) {
            x /= 2;
        }
        return (x == 1);
    }

    public static int calculateUpperPowerOfTwo(int v) {
        v--;
        v |= v >>> 1;
        v |= v >>> 2;
        v |= v >>> 4;
        v |= v >>> 8;
        v |= v >>> 16;
        v++;
        return v;
    }

    public static void adjustRectToMinimumSize(Rect r, int size) {
        int w = r.width();
        int h = r.height();
        if (w > size || h > size) {
            if (w == h && w > size) {
                r.right = r.bottom = size;
            } else if (w < h && w > size) {
                r.right = w * size / h;
                r.bottom = size;
            } else {
                r.bottom = h * size / w;
                r.right = size;
            }
        }
    }

    public static int calculateMaxAvailableSize(Context context) {
        if (AndroidHelper.isJellyBeanOrGreater()) {
            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            ActivityManager activityManager =
                    (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            activityManager.getMemoryInfo(mi);
            return (int)((mi.totalMem / 1073741824) * 1024);
        }
        // The minimum for all android devices
        return 1024;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static int byteSizeOf(Bitmap bitmap) {
        if (AndroidHelper.isKitkatOrGreater()) {
            return bitmap.getAllocationByteCount();
        }
        return bitmap.getByteCount();
    }
}
