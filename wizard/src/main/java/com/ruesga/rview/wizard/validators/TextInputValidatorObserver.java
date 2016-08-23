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

import android.text.Editable;
import android.widget.EditText;

import com.ruesga.rview.wizard.WizardPageFragment;
import com.ruesga.rview.wizard.misc.TextChangedWatcher;

public class TextInputValidatorObserver extends ValidatorObserver<EditText> {

    private TextChangedWatcher mTextWatcher = new TextChangedWatcher() {
        @Override
        public void afterTextChanged(Editable s) {
            triggerValidations(mObserved);
        }
    };

    private EditText mObserved;

    public TextInputValidatorObserver(WizardPageFragment page) {
        super(page);
    }

    @Override
    public void observeOn(EditText v) {
        v.addTextChangedListener(mTextWatcher);
        mObserved = v;
    }

    @Override
    public void unObserveOn(EditText v) {
        v.removeTextChangedListener(mTextWatcher);
        mObserved = null;
    }
}