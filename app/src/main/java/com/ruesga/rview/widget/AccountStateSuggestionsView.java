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
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import com.ruesga.rview.R;
import com.ruesga.rview.databinding.AccountStateSuggestionItemBinding;

import androidx.annotation.Keep;
import androidx.databinding.DataBindingUtil;

public class AccountStateSuggestionsView extends LinearLayout {

    public interface OnAccountStateSuggestionItemPressedListener {
        void onAccountStateSuggestionItemPressed(String emoji);
    }

    @Keep
    public static class Model {
        public String emoji;
        public String description;
    }

    @Keep
    public static class EventHandlers {
        private AccountStateSuggestionsView mView;

        public EventHandlers(AccountStateSuggestionsView view) {
            mView = view;
        }

        public void onItemPressed(View v) {
            String emoji = (String) v.getTag();
            mView.onItemPressed(emoji);
        }
    }


    private OnAccountStateSuggestionItemPressedListener mCallback;

    public AccountStateSuggestionsView(Context context) {
        this(context, null);
    }

    public AccountStateSuggestionsView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AccountStateSuggestionsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOrientation(VERTICAL);
        EventHandlers handlers = new EventHandlers(this);

        String[] codes = getResources().getStringArray(R.array.emoji_status_codes);
        String[] descriptions = getResources().getStringArray(R.array.emoji_status_descriptions);

        LayoutInflater inflater = LayoutInflater.from(context);
        int count = codes.length;
        for (int i = 0; i < count; i++) {
            AccountStateSuggestionItemBinding binding = DataBindingUtil.inflate(
                    inflater, R.layout.account_state_suggestion_item, this, false);
            Model model = new Model();
            model.emoji = codes[i];
            model.description = descriptions[i];
            binding.setModel(model);
            binding.setHandlers(handlers);
            addView(binding.getRoot());
        }
    }

    public AccountStateSuggestionsView listenTo(OnAccountStateSuggestionItemPressedListener cb) {
        mCallback = cb;
        return this;
    }

    private void onItemPressed(String emoji) {
        if (mCallback != null) {
            mCallback.onAccountStateSuggestionItemPressed(emoji);
        }
    }
}
