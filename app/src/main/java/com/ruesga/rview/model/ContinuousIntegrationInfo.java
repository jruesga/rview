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
package com.ruesga.rview.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

public class ContinuousIntegrationInfo implements Parcelable, Comparable<ContinuousIntegrationInfo> {
    public enum BuildStatus {
        SUCCESS, FAILURE, SKIPPED, RUNNING
    }

    @SerializedName("name") public String mName;
    @SerializedName("url") public String mUrl;
    @SerializedName("status") public BuildStatus mStatus;

    public ContinuousIntegrationInfo(String name, String url, BuildStatus status) {
        mName = name;
        mUrl = url;
        mStatus = status;
    }

    protected ContinuousIntegrationInfo(Parcel in) {
        mName = in.readString();
        mUrl = in.readString();
        mStatus = BuildStatus.valueOf(in.readString());
    }

    public static final Creator<ContinuousIntegrationInfo> CREATOR =
            new Creator<ContinuousIntegrationInfo>() {
        @Override
        public ContinuousIntegrationInfo createFromParcel(Parcel in) {
            return new ContinuousIntegrationInfo(in);
        }

        @Override
        public ContinuousIntegrationInfo[] newArray(int size) {
            return new ContinuousIntegrationInfo[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(mName);
        parcel.writeString(mUrl);
        parcel.writeString(mStatus.name());
    }

    @Override
    public int compareTo(@NonNull ContinuousIntegrationInfo ci) {
        int compare = mName.compareToIgnoreCase(ci.mName);
        if (compare == 0) {
            compare = mUrl.compareTo(ci.mUrl);
        }
        return compare;
    }
}