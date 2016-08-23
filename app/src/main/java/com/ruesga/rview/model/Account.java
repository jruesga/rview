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

import com.google.gson.annotations.SerializedName;
import com.ruesga.rview.gerrit.model.AccountInfo;
import com.ruesga.rview.misc.SerializationManager;

public class Account implements Parcelable {
    @SerializedName("repo") public Repository mRepository;
    @SerializedName("account") public AccountInfo mAccount;
    @SerializedName("token") public String mToken;

    public Account() {
    }

    protected Account(Parcel in) {
        mRepository = SerializationManager.getInstance().fromJson(in.readString(), Repository.class);
        mAccount = SerializationManager.getInstance().fromJson(in.readString(), AccountInfo.class);
        mToken = in.readString();
    }

    public static final Creator<Account> CREATOR = new Creator<Account>() {
        @Override
        public Account createFromParcel(Parcel in) {
            return new Account(in);
        }

        @Override
        public Account[] newArray(int size) {
            return new Account[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(SerializationManager.getInstance().toJson(mRepository));
        parcel.writeString(SerializationManager.getInstance().toJson(mAccount));
        parcel.writeString(mToken);
    }
}