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
package com.ruesga.rview.wizards;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Keep;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ruesga.rview.R;
import com.ruesga.rview.databinding.WizardConfirmationPageFragmentBinding;
import com.ruesga.rview.wizard.WizardPageFragment;

public class ConfirmationPageFragment extends WizardPageFragment {

    private static final String STATE_REPO_NAME = "repo.name";

    @Keep
    public static class Model {
        public Spanned message;
    }

    private WizardConfirmationPageFragmentBinding mBinding;
    private Model mModel = new Model();

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
        mBinding = DataBindingUtil.inflate(inflater, getPageLayout(), container, false);
        mBinding.setModel(mModel);
        return mBinding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mBinding.unbind();
    }

    protected @StringRes int getMessageResourceId() {
        return R.string.account_wizard_confirmation_page_message;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void restoreState(Context context, Bundle savedState) {
        mModel.message = Html.fromHtml(context.getString(
                getMessageResourceId(), savedState.getString(STATE_REPO_NAME)));
        if (mBinding != null) {
            mBinding.setModel(mModel);
        }
    }

    @Override
    public int getPageTitle() {
        return R.string.account_wizard_confirmation_page_title;
    }

    @Override
    public int getPageLayout() {
        return R.layout.wizard_confirmation_page_fragment;
    }

    public boolean hasExtendedHeader() {
        return true;
    }

    @Override
    public boolean hasBackAction() {
        return true;
    }

    @Override
    public int getBackActionLabel() {
        return R.string.action_previous;
    }

    @Override
    public int getBackActionDrawable() {
        return R.drawable.ic_chevron_left;
    }

    @Override
    public boolean hasForwardAction() {
        return true;
    }

    @Override
    public int getForwardActionLabel() {
        return R.string.action_done;
    }
}
