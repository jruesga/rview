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
import android.util.Base64;

import com.google.gson.annotations.SerializedName;
import com.ruesga.rview.gerrit.model.AccountInfo;
import com.ruesga.rview.misc.ModelHelper;
import com.ruesga.rview.misc.SerializationManager;

public class Account implements Parcelable, Comparable<Account> {
    public static final int ANONYMOUS_ACCOUNT_ID = -1;

    @SerializedName("repo") public Repository mRepository;
    @SerializedName("account") public AccountInfo mAccount;
    @SerializedName("token") public String mToken;
    @SerializedName("notificationsSenderId") public String mNotificationsSenderId;

    public Account() {
    }

    protected Account(Parcel in) {
        mRepository = SerializationManager.getInstance().fromJson(in.readString(), Repository.class);
        mAccount = SerializationManager.getInstance().fromJson(in.readString(), AccountInfo.class);
        if (in.readInt() == 1) {
            mToken = in.readString();
        }
        if (in.readInt() == 1) {
            mNotificationsSenderId = in.readString();
        }
    }

    public String getAccountDisplayName() {
        return ModelHelper.getAccountDisplayName(mAccount);
    }

    public String getRepositoryDisplayName() {
        if (!TextUtils.isEmpty(mRepository.mName)) {
            return mRepository.mName;
        }
        return mRepository.mUrl;
    }

    public boolean hasAuthenticatedAccessMode() {
        return mAccount.accountId != ANONYMOUS_ACCOUNT_ID;
    }

    public boolean hasNotificationsSupport() {
        return !TextUtils.isEmpty(mNotificationsSenderId);
    }

    public boolean isSameAs(Account account) {
        return mRepository.mName.equals(account.mRepository.mName)
                && mRepository.mUrl.equals(account.mRepository.mUrl)
                && mAccount.accountId == account.mAccount.accountId;
    }

    public String getAccountHash() {
        String hashId = mRepository.mName + "-" + mRepository.mUrl + "-" + mAccount.accountId;
        return Base64.encodeToString(hashId.getBytes(), Base64.NO_WRAP);
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
        parcel.writeInt(mToken != null ? 1 : 0);
        if (mToken != null) {
            parcel.writeString(mToken);
        }
        parcel.writeInt(hasNotificationsSupport() ? 1 : 0);
        if (hasNotificationsSupport()) {
            parcel.writeString(mNotificationsSenderId);
        }
    }

    @Override
    public int compareTo(@NonNull Account account) {
        int compare = mRepository.compareTo(mRepository);
        if (compare == 0) {
            if (mAccount == null && account.mAccount == null) {
                return 0;
            } else if (mAccount != null && account.mAccount != null) {
                compare = getRepositoryDisplayName().compareTo(account.getRepositoryDisplayName());
                if (compare == 0) {
                    compare = getAccountDisplayName().compareTo(account.getAccountDisplayName());
                }
            } else if (mAccount == null) {
                compare = 1;
            } else {
                compare = -1;
            }
        }
        return compare;
    }
}