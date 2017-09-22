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
package com.ruesga.rview.attachments;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Keep;

import com.google.gson.annotations.SerializedName;

@Keep
public class Attachment implements Parcelable {
    @SerializedName("id") public String mId;
    @SerializedName("messageId") public String mMessageId;
    @SerializedName("name") public String mName;
    @SerializedName("mime") public String mMimeType;
    @SerializedName("url") public String mUrl;
    @SerializedName("size") public long mSize;
    @SerializedName("local_uri") public Uri mLocalUri;

    public Attachment() {
    }

    protected Attachment(Parcel in) {
        if (in.readInt() == 1) {
            mId = in.readString();
        }
        if (in.readInt() == 1) {
            mMessageId = in.readString();
        }
        if (in.readInt() == 1) {
            mName = in.readString();
        }
        if (in.readInt() == 1) {
            mMimeType = in.readString();
        }
        if (in.readInt() == 1) {
            mUrl = in.readString();
        }
        mSize = in.readLong();
        mLocalUri = Uri.CREATOR.createFromParcel(in);
    }

    public static final Creator<Attachment> CREATOR = new Creator<Attachment>() {
        @Override
        public Attachment createFromParcel(Parcel in) {
            return new Attachment(in);
        }

        @Override
        public Attachment[] newArray(int size) {
            return new Attachment[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(mId != null ? 1 : 0);
        if (mId != null) {
            parcel.writeString(mId);
        }
        parcel.writeInt(mMessageId != null ? 1 : 0);
        if (mMessageId != null) {
            parcel.writeString(mMessageId);
        }
        parcel.writeInt(mName != null ? 1 : 0);
        if (mName != null) {
            parcel.writeString(mName);
        }
        parcel.writeInt(mMimeType != null ? 1 : 0);
        if (mMimeType != null) {
            parcel.writeString(mMimeType);
        }
        parcel.writeInt(mUrl != null ? 1 : 0);
        if (mUrl != null) {
            parcel.writeString(mUrl);
        }
        parcel.writeLong(mSize);
        Uri.writeToParcel(parcel, mLocalUri);
    }
}