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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.MenuRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v4.util.Pair;
import android.support.v7.widget.PopupMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ruesga.rview.wizard.validators.Validator;
import com.ruesga.rview.wizard.validators.ValidatorObserver;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;

public abstract class WizardPageFragment extends Fragment {

    private Map<View, Pair<ValidatorObserver, Validator[]>> mValidators = new LinkedHashMap<>();
    private boolean mIsValidated;

    public static <T extends WizardPageFragment> WizardPageFragment newInstance(Class<T> typeof) {
        try {
            return typeof.newInstance();
        } catch (Exception ex) {
            // ignore
        }
        return null;
    }

    public WizardPageFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle state) {
        return inflater.inflate(getPageLayout(), container, false);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onDestroyView() {
        super.onDestroyView();
        for (View v : mValidators.keySet()) {
            Pair<ValidatorObserver, Validator[]> p = mValidators.get(v);
            p.first.unObserveOn(v);
        }
        mValidators.clear();
    }

    public void restoreState(Context context, Bundle savedState) {
    }

    public Bundle savedState() {
        return null;
    }

    public abstract @StringRes int getPageTitle();

    public abstract @LayoutRes int getPageLayout();

    public boolean canRequireKeyboard() {
        return false;
    }

    public boolean hasExtendedHeader() {
        return false;
    }

    public boolean hasBackAction() {
        return false;
    }

    public boolean canPerformBackAction() {
        return true;
    }

    public @StringRes int getBackActionLabel() {
        return 0;
    }

    public @DrawableRes int getBackActionDrawable() {
        return 0;
    }

    public Callable<Boolean> doBackAction() {
        return null;
    }

    public boolean hasForwardAction() {
        return false;
    }

    public boolean canPerformForwardAction() {
        return mValidators.isEmpty() || isPageValidated();
    }

    public @StringRes int getForwardActionLabel() {
        return 0;
    }

    public @DrawableRes int getForwardActionDrawable() {
        return 0;
    }

    public Callable<Boolean> doForwardAction() {
        return null;
    }

    public boolean hasPageOptionsMenu() {
        return false;
    }

    public @MenuRes int getPageOptionsMenu() {
        return 0;
    }

    public PopupMenu.OnMenuItemClickListener getPageOptionsMenuOnItemClickListener() {
        return null;
    }

    public void onViewValidated(View v, Validator failed) {
    }

    public boolean isPageValidated() {
        return mIsValidated;
    }

    public void openChooserPage(WizardChooserFragment chooser) {
        if (getActivity() != null) {
            ((WizardActivity) getActivity()).performOpenChooserPage(chooser);
        }
    }

    public void onChooserResult(int resultCode, Intent data) {
    }

    @SuppressWarnings("unchecked")
    public void setValidatorsForView(View v, ValidatorObserver observer, Validator... validators) {
        if (mValidators.containsKey(v)) {
            Pair<ValidatorObserver, Validator[]> p = mValidators.get(v);
            p.first.unObserveOn(v);
        }
        observer.observeOn(v);
        mValidators.put(v, new Pair<>(observer, validators));
    }

    @SuppressWarnings("unchecked")
    public void triggerAllValidators(View source) {
        boolean isValidated = true;

        // Check the source of the event in first place
        Pair<ValidatorObserver, Validator[]> p = mValidators.get(source);
        if (p != null) {
            Validator failed = null;
            for (Validator validator : p.second) {
                if (!validator.validate(source)) {
                    // Validation error
                    isValidated = false;
                    failed = validator;
                    break;
                }
            }
            onViewValidated(source, failed);
        }

        // And all the rest of the views
        if (isValidated) {
            for (View v : mValidators.keySet()) {
                if (v.equals(source)) {
                    continue;
                }
                p = mValidators.get(v);
                for (Validator validator : p.second) {
                    if (!validator.validate(v)) {
                        // Validation error
                        isValidated = false;
                        break;
                    }
                }
            }
        }

        mIsValidated = isValidated;
        notifyValidationChanged();
    }

    private void notifyValidationChanged() {
        if (getActivity() != null) {
            ((WizardActivity) getActivity()).onValidationChanged(this);
        }
    }
}
