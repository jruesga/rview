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
package com.ruesga.rview.gerrit.model;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

/**
 * @link "https://github.com/jruesga/gerrit-cloud-notifications-plugin/blob/master/src/main/resources/Documentation/fcm.md#CloudNotificationInfo"
 */
public class CloudNotificationInfo {
    @SerializedName("device") public String device;
    @SerializedName("token") public String token;
    @SerializedName("registeredOn") public Date registeredOn;
    @SerializedName("events") public int events = 0;
    @SerializedName("responseMode") public CloudNotificationResponseMode responseMode;
}

