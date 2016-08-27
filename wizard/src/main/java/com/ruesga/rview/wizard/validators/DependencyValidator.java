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

import android.view.View;
import android.widget.CompoundButton;

public class DependencyValidator implements Validator {

    private final CompoundButton mView;
    private final Validator[] mValidators;
    private int mFailed = -1;

    public DependencyValidator(CompoundButton v, Validator... validators) {
        mView = v;
        mValidators = validators;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean validate(View v) {
        mFailed = -1;
        if (!mView.isChecked()) {
            return true;
        }
        int i = 0;
        for (Validator validator : mValidators) {
            if (!validator.validate(v)) {
                mFailed = i;
                return false;
            }
            i++;
        }
        return true;
    }

    @Override
    public String getMessage() {
        if (mFailed == -1) {
            return null;
        }
        return mValidators[mFailed].getMessage().toString();
    }
}
