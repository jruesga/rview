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
package com.ruesga.rview.attachments.gdrive.model;

import com.google.gson.annotations.SerializedName;

public class ImageMediaMetadata {
    @SerializedName("width") public int width;
    @SerializedName("height") public int height;
    @SerializedName("rotation") public int rotation;
    @SerializedName("location") public LocationMetadata location;
    @SerializedName("time") public String time;
    @SerializedName("cameraMake") public String cameraMake;
    @SerializedName("cameraModel") public String cameraModel;
    @SerializedName("exposureTime") public Float exposureTime;
    @SerializedName("aperture") public Float aperture;
    @SerializedName("flashUsed") public Boolean flashUsed;
    @SerializedName("focalLength") public Float focalLength;
    @SerializedName("isoSpeed") public Integer isoSpeed;
    @SerializedName("meteringMode") public String meteringMode;
    @SerializedName("sensor") public String sensor;
    @SerializedName("exposureMode") public String exposureMode;
    @SerializedName("colorSpace") public String colorSpace;
    @SerializedName("whiteBalance") public String whiteBalance;
    @SerializedName("exposureBias") public Float exposureBias;
    @SerializedName("maxApertureValue") public Float maxApertureValue;
    @SerializedName("subjectDistance") public Integer subjectDistance;
    @SerializedName("lens") public String lens;
}
