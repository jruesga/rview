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
package com.ruesga.rview.wizard;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;

public abstract class WizardChooserFragment extends Fragment {

    public static <T extends WizardChooserFragment> WizardChooserFragment newInstance(Class<T> typeof) {
        try {
            return typeof.newInstance();
        } catch (Exception ex) {
            // ignore
        }
        return null;
    }

    private int mParentPage;

    public abstract @StringRes int getTitle();

    public abstract Intent getResult();

    public final void setParentPage(int page) {
        mParentPage = page;
    }

    public final int getParentPage() {
        return mParentPage;
    }

    public boolean hasAcceptButton() {
        return false;
    }

    public boolean isAcceptButtonEnabled() {
        return true;
    }

    public final void close() {
        final WizardActivity activity = ((WizardActivity) getActivity());
        //noinspection ConstantConditions
        activity.performChooserClose(this, Activity.RESULT_OK, getResult());
    }

    public final void cancel() {
        final WizardActivity activity = ((WizardActivity) getActivity());
        //noinspection ConstantConditions
        activity.performChooserClose(this, Activity.RESULT_CANCELED, null);
    }
}
