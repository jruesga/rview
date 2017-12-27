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
import com.ruesga.rview.attachments.EmptyMetadataException;
import com.ruesga.rview.exceptions.IllegalQueryExpressionException;
import com.ruesga.rview.exceptions.OperationFailedException;
import com.ruesga.rview.model.Account;
import com.ruesga.rview.model.EmptyState;
import com.ruesga.rview.preferences.Preferences;

import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.PortUnreachableException;
import java.net.SocketTimeoutException;
import java.util.HashSet;
import java.util.Set;

public class ExceptionHelper {
    private static final Set<String> sPreviousAccountAuthenticationFailures = new HashSet<>();

    public static final String EXTRA_AUTHENTICATION_FAILURE = "authentication_failure";

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

    @SuppressWarnings({"ThrowableResultOfMethodCallIgnored", "ConstantConditions", "deprecation"})
    public static @StringRes int exceptionToMessage(Context context, String tag, Throwable cause) {
        int message;
        if (isException(cause, retrofit2.HttpException.class)) {
            retrofit2.HttpException httpException =
                    (retrofit2.HttpException)
                            getCause(cause, retrofit2.HttpException.class);
            message = httpCode2MessageResource(httpException.code());

        } else if (isException(cause, retrofit2.adapter.rxjava2.HttpException.class)) {
            retrofit2.adapter.rxjava2.HttpException httpException =
                    (retrofit2.adapter.rxjava2.HttpException)
                            getCause(cause, retrofit2.adapter.rxjava2.HttpException.class);
            message = httpCode2MessageResource(httpException.code());

        } else if (isException(cause, OperationFailedException.class)) {
            message = R.string.exception_operation_failed;

        } else if (isException(cause, com.ruesga.rview.gerrit.NoConnectivityException.class)
                || isException(cause, com.ruesga.rview.attachments.NoConnectivityException.class)) {
            message = R.string.exception_no_network_available;

        } else if (isException(cause, EmptyMetadataException.class)) {
            message = R.string.exception_data_not_available;

        } else if (isException(cause, com.ruesga.rview.attachments.AuthenticationException.class)) {
            message = R.string.exception_attachment_provider_not_logged;

        } else if (isException(cause, ConnectException.class)
                || isException(cause, NoRouteToHostException.class)
                || isException(cause, PortUnreachableException.class)
                || isException(cause, SocketTimeoutException.class)) {
            message = R.string.exception_server_cannot_be_reached;

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

    private static int httpCode2MessageResource(int code) {
        switch (code) {
            case 400: //Bad request
                return R.string.exception_invalid_request;
            case 401: //Unauthorized
                return R.string.exception_invalid_user_password;
            case 403: //Forbidden
                return R.string.exception_insufficient_permissions;
            case 404: //Not found
                return R.string.exception_request_data_not_found;
            case 409: //Conflict
                return R.string.exception_conflict;
            default:
                return R.string.exception_bad_request;
        }
    }

    public static int exceptionToLevel(Throwable cause) {
        if (isException(cause, com.ruesga.rview.gerrit.NoConnectivityException.class)
                || isException(cause, com.ruesga.rview.attachments.NoConnectivityException.class)
                || isHttpException(cause, 409)
                || isException(cause, IllegalQueryExpressionException.class)
                || isException(cause, EmptyMetadataException.class)) {
            return 1;
        }
        return 0;
    }

    public static boolean hasConnectivity(Throwable cause) {
        return !(isException(cause, com.ruesga.rview.gerrit.NoConnectivityException.class)
                || isException(cause, com.ruesga.rview.attachments.NoConnectivityException.class));
    }

    public static boolean hasServerConnectivity(Throwable cause) {
        return !(isException(cause, ConnectException.class)
                || isException(cause, NoRouteToHostException.class)
                || isException(cause, PortUnreachableException.class)
                || isException(cause, SocketTimeoutException.class));
    }

    public static boolean isAuthenticationException(Throwable cause) {
        return isHttpException(cause, 401);
    }

    public static boolean isResourceNotFoundException(Throwable cause) {
        return isHttpException(cause, 404);
    }

    public static boolean isServerUnavailableException(Throwable cause) {
        return isHttpException(cause, 503) ||
                (isException(cause, ConnectException.class)
                || isException(cause, NoRouteToHostException.class)
                || isException(cause, PortUnreachableException.class)
                || isException(cause, SocketTimeoutException.class));
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

    public static int resolveEmptyState(Throwable error) {
        if (!ExceptionHelper.hasConnectivity(error)) {
            return EmptyState.NOT_CONNECTIVITY_STATE;
        }
        if (!ExceptionHelper.hasServerConnectivity(error)) {
            return EmptyState.SERVER_CANNOT_BE_REACHED;
        }
        return EmptyState.ERROR_STATE;
    }

    public static synchronized boolean hasPreviousAuthenticationFailure(Context context) {
        Account account = Preferences.getAccount(context);
        return account != null &&
                sPreviousAccountAuthenticationFailures.contains(account.getAccountHash());
    }

    public static synchronized void markAsAuthenticationFailure(Context context) {
        Account account = Preferences.getAccount(context);
        if (account != null) {
            sPreviousAccountAuthenticationFailures.add(account.getAccountHash());
        }
    }

    public static synchronized void clearAuthenticationFailure(Context context) {
        Account account = Preferences.getAccount(context);
        if (account != null) {
            sPreviousAccountAuthenticationFailures.remove(account.getAccountHash());
        }
    }
}
