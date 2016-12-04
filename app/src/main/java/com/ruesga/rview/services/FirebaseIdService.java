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
package com.ruesga.rview.services;

import android.content.Intent;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceIdService;

public class FirebaseIdService extends FirebaseInstanceIdService {

    private static final String TAG = "FirebaseIdService";

    @Override
    public void onTokenRefresh() {
        // Obtain the new token
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Firebase device token changed. Registering all accounts");
        }

        // Register all accounts
        Intent intent = new Intent(this, DeviceRegistrationService.class);
        intent.setAction(DeviceRegistrationService.REGISTER_DEVICE_ACTION);
        startService(intent);
    }
}