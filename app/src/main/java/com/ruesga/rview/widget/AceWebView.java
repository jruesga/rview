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
import android.text.InputType;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.webkit.WebView;

/**
 * This is a {@link WebView} subclass to deal with IME issues with the Ace Code Editor library.
 * Some Keyboards, like the Google Keyboard one, send composite key events (keycode 229)
 * which breaks the Ace's keycode handling (which as version 2.8, is still currently not
 * fixed).
 * <p />
 * This implementation disables the IME input method of the {@link WebView}.
 *
 * @see "https://bugs.chromium.org/p/chromium/issues/detail?id=118639"
 * @see "https://github.com/ajaxorg/ace/issues/2964"
 * @see "https://github.com/ajaxorg/ace/issues/1917"
 */
public class AceWebView extends WebView {
    public AceWebView(Context context) {
        super(context);
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        InputConnection inputConnection = super.onCreateInputConnection(outAttrs);
        outAttrs.imeOptions = EditorInfo.IME_NULL;
        outAttrs.imeOptions = outAttrs.imeOptions | EditorInfo.IME_FLAG_FORCE_ASCII;
        outAttrs.inputType = InputType.TYPE_NULL;
        return inputConnection;
    }
}
