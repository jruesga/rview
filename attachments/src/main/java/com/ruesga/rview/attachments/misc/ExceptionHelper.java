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
package com.ruesga.rview.attachments.misc;

public class ExceptionHelper {

    @SuppressWarnings("SimplifiableIfStatement")
    public static <T extends Throwable> boolean isException(Throwable cause, Class<T> c) {
        if (c.isInstance(cause)) {
            return true;
        }
        return cause.getCause() != null && isException(cause.getCause(), c);
    }

    public static <T extends Throwable> Throwable getCause(Throwable cause, Class<T> c) {
        if (c.isInstance(cause)) {
            return cause;
        }
        if (cause.getCause() != null) {
            return getCause(cause.getCause(), c);
        }
        return null;
    }

    public static boolean isAuthenticationException(Throwable cause) {
        return isHttpException(cause, 401);
    }

    @SuppressWarnings({"ThrowableResultOfMethodCallIgnored", "ConstantConditions", "deprecation"})
    private static boolean isHttpException(Throwable cause, int httpCode) {
        if (isException(cause, retrofit2.HttpException.class)) {
            retrofit2.HttpException httpException =
                    (retrofit2.HttpException)
                            getCause(cause, retrofit2.HttpException.class);
            return httpCode == httpException.code();
        }
        if (isException(cause, retrofit2.adapter.rxjava2.HttpException.class)) {
            retrofit2.adapter.rxjava2.HttpException httpException =
                    (retrofit2.adapter.rxjava2.HttpException)
                            getCause(cause, retrofit2.adapter.rxjava2.HttpException.class);
            return httpCode == httpException.code();
        }
        return false;
    }

}
