/*
 * Copyright (C) 2010 The Android Open Source Project
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

import android.animation.TypeEvaluator;
import android.os.Build;

/**
 * This evaluator can be used to perform type interpolation between integer
 * values that represent ARGB colors.
 * <p/>
 * <b>Official 4.2 source code. Solution for https://code.google.com/p/android/issues/detail?id=36158.</b>
 */
public class ArgbEvaluator implements TypeEvaluator {

    private android.animation.ArgbEvaluator mDelegate;

    /**
     * This function returns the calculated in-between value for a color
     * given integers that represent the start and end values in the four
     * bytes of the 32-bit int. Each channel is separately linearly interpolated
     * and the resulting calculated values are recombined into the return value.
     *
     * @param fraction   The fraction from the starting to the ending values
     * @param startValue A 32-bit int value representing colors in the
     *                   separate bytes of the parameter
     * @param endValue   A 32-bit int value representing colors in the
     *                   separate bytes of the parameter
     * @return A value that is calculated to be the linearly interpolated
     * result, derived by separating the start and end values into separate
     * color channels and interpolating each one separately, recombining the
     * resulting values in the same way.
     */
    public Object evaluate(float fraction, Object startValue, Object endValue) {

        Object result;

        if (mDelegate != null) {
            result = mDelegate.evaluate(fraction, startValue, endValue);
        } else {
            int startInt = (Integer) startValue;
            int startA = (startInt >> 24) & 0xff;
            int startR = (startInt >> 16) & 0xff;
            int startG = (startInt >> 8) & 0xff;
            int startB = startInt & 0xff;

            int endInt = (Integer) endValue;
            int endA = (endInt >> 24) & 0xff;
            int endR = (endInt >> 16) & 0xff;
            int endG = (endInt >> 8) & 0xff;
            int endB = endInt & 0xff;

            result = (startA + (int) (fraction * (endA - startA))) << 24 |
                    (startR + (int) (fraction * (endR - startR))) << 16 |
                    (startG + (int) (fraction * (endG - startG))) << 8 |
                    (startB + (int) (fraction * (endB - startB)));
        }

        return result;
    }

    public static ArgbEvaluator newInstance() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return new ArgbEvaluator().withDelegate(new android.animation.ArgbEvaluator());
        } else {
            return new ArgbEvaluator();
        }
    }

    private ArgbEvaluator withDelegate(android.animation.ArgbEvaluator delegate) {
        this.mDelegate = delegate;
        return this;
    }

}

