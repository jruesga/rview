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
import android.os.AsyncTask;
import android.view.Display;
import android.view.WindowManager;

import com.ruesga.rview.misc.BitmapUtils;

import java.io.File;

public class AsyncImageDiffProcessor extends AsyncTask<Void, Void, Drawable[]> {

    public interface OnImageDiffProcessEndedListener {
        void onImageDiffProcessEnded(Drawable[] drawables);
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
    protected Drawable[] doInBackground(Void... params) {
        Drawable[] drawables = new Drawable[2];
        drawables[0] = loadAsDrawable(mLeft);
        drawables[1] = loadAsDrawable(mRight);
        return drawables;
    }

    @Override
    protected void onPostExecute(Drawable[] drawables) {
        mCallback.onImageDiffProcessEnded(drawables);
    }

    private Drawable loadAsDrawable(File file) {
        if (file != null) {
            Bitmap bitmap = BitmapUtils.decodeBitmap(file, mSize, mSize);
            if (bitmap != null) {
                return new BitmapDrawable(mContext.getResources(), bitmap);
            }
        }
        return null;
    }
}
