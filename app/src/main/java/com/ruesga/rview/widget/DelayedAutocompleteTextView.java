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
package com.ruesga.rview.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;

import com.ruesga.rview.R;

import androidx.appcompat.widget.AppCompatAutoCompleteTextView;

// http://makovkastar.github.io/blog/2014/04/12/android-autocompletetextview-with-suggestions-from-a-web-service/
public class DelayedAutocompleteTextView extends AppCompatAutoCompleteTextView {

    private static final int MESSAGE_TEXT_CHANGED = 1;

    private Handler mHandler;
    private int mDelay;

    public DelayedAutocompleteTextView(Context context) {
        super(context);
        init(context, null, 0);
    }

    public DelayedAutocompleteTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public DelayedAutocompleteTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        mHandler = new Handler(msg -> {
            switch (msg.what) {
                case MESSAGE_TEXT_CHANGED:
                    requestFiltering((CharSequence) msg.obj, msg.arg1);
                    return true;
            }
            return false;
        });

        TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.DelayedAutocompleteTextView, defStyleAttr, 0);
        mDelay = a.getInt(R.styleable.DelayedAutocompleteTextView_delay, 500);
        a.recycle();
    }

    @Override
    protected void performFiltering(CharSequence constraint, int keyCode) {
        mHandler.removeMessages(MESSAGE_TEXT_CHANGED);
        Message message = Message.obtain(mHandler, MESSAGE_TEXT_CHANGED);
        message.obj = constraint;
        message.arg1 = keyCode;
        mHandler.sendMessageDelayed(mHandler.obtainMessage(MESSAGE_TEXT_CHANGED, constraint), mDelay);
    }

    private void requestFiltering(CharSequence constraint, int keyCode) {
        super.performFiltering(constraint, keyCode);
    }
}
