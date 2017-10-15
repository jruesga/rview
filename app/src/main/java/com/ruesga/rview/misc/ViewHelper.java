/*
 * Copyright (C) 2017 Jorge Ruesga
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

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

public class ViewHelper {

    @SuppressWarnings("unchecked")
    public static <T extends View> T findFirstParentViewOfType(View v, Class<T> clazz) {
        ViewParent parent = v.getParent();
        while (parent != null) {
            if (parent.getClass().equals(clazz) || clazz.isAssignableFrom(parent.getClass())) {
                return (T) parent;
            }
            parent = parent.getParent();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static <T extends View> T findFirstChildViewOfType(ViewGroup v, Class<T> clazz) {
        int count = v.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = v.getChildAt(i);
            if (child.getClass().equals(clazz) || clazz.isAssignableFrom(child.getClass())) {
                return (T) child;
            }
            if (child instanceof ViewGroup) {
                View vv = findFirstChildViewOfType((ViewGroup) child, clazz);
                if (vv != null) {
                    return (T) vv;
                }
            }
        }
        return null;
    }
}
