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
import android.util.AttributeSet;

import org.apmem.tools.layouts.FlowLayout;

public class NonDebuggableFlowLayout extends FlowLayout {
    public NonDebuggableFlowLayout(Context context) {
        super(context);
    }

    public NonDebuggableFlowLayout(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public NonDebuggableFlowLayout(Context context, AttributeSet attributeSet, int defStyle) {
        super(context, attributeSet, defStyle);
    }

    @Override
    public boolean isDebugDraw() {
        // FIXME: super.isDebugDraw() does reflection calls that cause
        // IllegalArgumentExceptions being thrown, causing scrolling slowness,
        // thus we avoid it being called here. This should probably be fixed upstream.
        return false;
    }
}
