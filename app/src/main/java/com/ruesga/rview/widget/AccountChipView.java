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
package com.ruesga.rview.widget;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.support.annotation.Keep;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import com.ruesga.rview.R;
import com.ruesga.rview.databinding.AccountChipBinding;
import com.ruesga.rview.gerrit.model.AccountInfo;
import com.ruesga.rview.misc.PicassoHelper;
import com.squareup.picasso.Picasso;

public class AccountChipView extends FrameLayout {

    public interface OnAccountChipClickedListener {
        void onAccountChipClicked(AccountInfo account, Object tag);
    }

    public interface OnAccountChipRemovedListener {
        void onAccountChipRemoved(AccountInfo account, Object tag);
    }

    @Keep
    public static class EventHandlers {
        private AccountChipView mView;

        public EventHandlers(AccountChipView v) {
            mView = v;
        }

        public void onChipPressed(View v) {
            mView.onAccountChipClicked((AccountInfo) v.getTag());
        }

        public void onRemovePressed(View v) {
            mView.onAccountChipRemoved((AccountInfo) v.getTag());
        }
    }

    private AccountChipBinding mBinding;
    private Picasso mPicasso;
    private OnAccountChipClickedListener mOnAccountChipClickedListener;
    private OnAccountChipRemovedListener mOnAccountChipRemovedListener;
    private Object mTag;

    public AccountChipView(Context context) {
        this(context, null);
    }

    public AccountChipView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AccountChipView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        LayoutInflater layoutInflater = LayoutInflater.from(context);
        EventHandlers handlers = new EventHandlers(this);
        mBinding = DataBindingUtil.inflate(layoutInflater, R.layout.account_chip, this, false);
        mBinding.setRemovable(false);
        mBinding.setHandlers(handlers);
        addView(mBinding.getRoot());
    }

    public AccountChipView withTag(Object tag) {
        mTag = tag;
        return this;
    }

    public AccountChipView with(Picasso picasso) {
        mPicasso = picasso;
        return this;
    }

    public AccountChipView removable(boolean removable) {
        mBinding.setRemovable(removable);
        return this;
    }

    public AccountChipView listenOn(OnAccountChipClickedListener cb) {
        mOnAccountChipClickedListener = cb;
        return this;
    }

    public AccountChipView listenOn(OnAccountChipRemovedListener cb) {
        mOnAccountChipRemovedListener = cb;
        return this;
    }

    public AccountChipView from(AccountInfo account) {
        PicassoHelper.bindAvatar(getContext(), mPicasso, account, mBinding.avatar,
                PicassoHelper.getDefaultAvatar(getContext(), R.color.primaryDarkForeground));
        mBinding.setModel(account);
        return this;
    }

    private void onAccountChipClicked(AccountInfo account) {
        if (mOnAccountChipClickedListener != null) {
            mOnAccountChipClickedListener.onAccountChipClicked(account, mTag);
        }
    }

    private void onAccountChipRemoved(AccountInfo account) {
        if (mOnAccountChipRemovedListener != null) {
            mOnAccountChipRemovedListener.onAccountChipRemoved(account, mTag);
        }
    }
}
