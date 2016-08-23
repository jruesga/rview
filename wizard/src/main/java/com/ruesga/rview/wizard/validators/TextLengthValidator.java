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
package com.ruesga.rview.wizard.validators;

import android.content.Context;
import android.widget.EditText;

import com.ruesga.rview.wizard.R;

public class TextLengthValidator implements Validator<EditText> {
    public static final int NO_RESTRICTION = -1;

    private final Context mContext;
    private final int mMinLength;
    private final int mMaxLength;

    public TextLengthValidator(Context context, int min, int max) {
        mContext = context;
        mMinLength = min;
        mMaxLength = max;
    }

    @Override
    public boolean validate(EditText v) {
        final CharSequence s = v.getText();
        return !(mMinLength != NO_RESTRICTION && s.length() < mMinLength)
                && !(mMaxLength != NO_RESTRICTION && s.length() > mMaxLength);
    }

    @Override
    public CharSequence getMessage() {
        return mContext.getString(R.string.validator_invalid_text_length);
    }
}
