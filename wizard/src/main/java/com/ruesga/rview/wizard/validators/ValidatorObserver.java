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

import com.ruesga.rview.wizard.WizardPageFragment;

public abstract class ValidatorObserver<T extends View> {
    private final WizardPageFragment mPage;

    public ValidatorObserver(WizardPageFragment page) {
        mPage = page;
    }

    public WizardPageFragment getPage() {
        return mPage;
    }

    public abstract void observeOn(T v);

    public abstract void unObserveOn(T v);

    protected final void triggerValidations(View source) {
        mPage.triggerAllValidators(source);
    }
}
