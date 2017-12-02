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
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.google.gson.annotations.SerializedName;
import com.ruesga.rview.gerrit.filter.ChangeQuery;
import com.ruesga.rview.misc.SerializationManager;

import java.util.UUID;

public class CustomFilter implements Parcelable, Comparable<CustomFilter> {
    @SerializedName("id") public String mId;
    @SerializedName("name") public String mName;
    @SerializedName("query") public ChangeQuery mQuery;

    public CustomFilter(String name, ChangeQuery query) {
        this.mId = UUID.randomUUID().toString();
        this.mName = name;
        this.mQuery = query;
    }

    protected CustomFilter(Parcel in) {
        if (in.readInt() == 1) {
            mId = in.readString();
        }
        if (in.readInt() == 1) {
            mName = in.readString();
        }
        if (in.readInt() == 1) {
            mQuery = SerializationManager.getInstance().fromJson(
                    in.readString(), ChangeQuery.class);
        }
    }

    public static final Creator<CustomFilter> CREATOR = new Creator<CustomFilter>() {
        @Override
        public CustomFilter createFromParcel(Parcel in) {
            return new CustomFilter(in);
        }

        @Override
        public CustomFilter[] newArray(int size) {
            return new CustomFilter[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(!TextUtils.isEmpty(mId) ? 1 : 0);
        if (!TextUtils.isEmpty(mId)) {
            parcel.writeString(mId);
        }
        parcel.writeInt(!TextUtils.isEmpty(mName) ? 1 : 0);
        if (!TextUtils.isEmpty(mName)) {
            parcel.writeString(mName);
        }
        parcel.writeInt(mQuery != null ? 1 : 0);
        if (mQuery != null) {
            parcel.writeString(SerializationManager.getInstance().toJson(mQuery));
        }
    }

    @Override
    public int compareTo(@NonNull CustomFilter repository) {
        int ret = mName.compareTo(repository.mName);
        if (ret == 0) {
            ret = mId.compareTo(repository.mId);
        }
        return ret;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CustomFilter that = (CustomFilter) o;

        return mId != null ? mId.equals(that.mId) : that.mId == null;

    }

    @Override
    public int hashCode() {
        return mId != null ? mId.hashCode() : 0;
    }
}