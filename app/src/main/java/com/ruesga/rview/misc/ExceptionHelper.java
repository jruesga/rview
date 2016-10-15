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
import android.support.annotation.StringRes;
import android.util.Log;

import com.ruesga.rview.R;
import com.ruesga.rview.exceptions.IllegalQueryExpressionException;
import com.ruesga.rview.exceptions.OperationFailedException;
import com.ruesga.rview.gerrit.NoConnectivityException;

import retrofit2.adapter.rxjava.HttpException;

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

    @SuppressWarnings({"ThrowableResultOfMethodCallIgnored", "ConstantConditions"})
    public static @StringRes int exceptionToMessage(Context context, String tag, Throwable cause) {
        int message;
        if (isException(cause, HttpException.class)) {
            HttpException httpException = (HttpException) getCause(cause, HttpException.class);
            final int code = httpException.code();
            switch (code) {
                case 400: //Bad request
                    message = R.string.exception_invalid_request;
                    break;
                case 403: //Forbidden
                    message = R.string.exception_insufficient_permissions;
                    break;
                case 404: //Not found
                    message = R.string.exception_not_found;
                    break;
                default:
                    message = R.string.exception_bad_request;
                    break;
            }

        } else if (isException(cause, OperationFailedException.class)) {
            message = R.string.exception_operation_failed;

        } else if (isException(cause, NoConnectivityException.class)) {
            message = R.string.exception_no_network_available;

        } else if (isException(cause, IllegalQueryExpressionException.class)) {
            message = R.string.exception_invalid_request;

        } else {
            message = R.string.exception_request_error;
        }

        // Log an return the translated message
        if (exceptionToLevel(cause) == 0) {
            Log.e(tag, context.getString(message), cause);
        } else {
            Log.w(tag, context.getString(message));
        }
        return message;
    }

    public static int exceptionToLevel(Throwable cause) {
        if (isException(cause, NoConnectivityException.class)
                || isException(cause, IllegalQueryExpressionException.class)) {
            return 1;
        }
        return 0;
    }

    public static boolean hasConnectivity(Throwable cause) {
        return !(isException(cause, NoConnectivityException.class));
    }

}
