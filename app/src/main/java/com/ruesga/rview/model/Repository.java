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
package com.ruesga.rview.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.google.gson.annotations.SerializedName;

import androidx.annotation.NonNull;

public class Repository implements Parcelable, Comparable<Repository> {
    @SerializedName("name") public String mName;
    @SerializedName("url") public String mUrl;
    @SerializedName("ci_accounts") public String mCiAccounts;
    @SerializedName("ci_status_mode") public String mCiStatusMode;
    @SerializedName("ci_status_url") public String mCiStatusUrl;
    @SerializedName("trustAllCertificates") public boolean mTrustAllCertificates;

    public Repository(String name, String url, boolean trustAllCertificates) {
        this.mName = name;
        this.mUrl = url;
        mTrustAllCertificates = trustAllCertificates;
    }

    protected Repository(Parcel in) {
        mName = in.readString();
        mUrl = in.readString();
        mTrustAllCertificates = in.readInt() == 1;
        if (in.readInt() == 1) {
            mCiAccounts = in.readString();
        }
        if (in.readInt() == 1) {
            mCiStatusMode = in.readString();
        }
        if (in.readInt() == 1) {
            mCiStatusUrl = in.readString();
        }
    }

    public static final Creator<Repository> CREATOR = new Creator<Repository>() {
        @Override
        public Repository createFromParcel(Parcel in) {
            return new Repository(in);
        }

        @Override
        public Repository[] newArray(int size) {
            return new Repository[size];
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
        parcel.writeInt(mTrustAllCertificates ? 1 : 0);
        if (!TextUtils.isEmpty(mCiAccounts)) {
            parcel.writeInt(1);
            parcel.writeString(mCiAccounts);
        } else {
            parcel.writeInt(0);
        }
        if (!TextUtils.isEmpty(mCiStatusMode)) {
            parcel.writeInt(1);
            parcel.writeString(mCiStatusMode);
        } else {
            parcel.writeInt(0);
        }
        if (!TextUtils.isEmpty(mCiStatusUrl)) {
            parcel.writeInt(1);
            parcel.writeString(mCiStatusUrl);
        } else {
            parcel.writeInt(0);
        }
    }

    @Override
    public int compareTo(@NonNull Repository repository) {
        int compare = mName.compareTo(repository.mName);
        if (compare == 0) {
            compare = mUrl.compareTo(repository.mUrl);
        }
        return compare;
    }
}