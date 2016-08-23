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

import com.ruesga.rview.R;
import com.ruesga.rview.wizard.WizardPageFragment;

public class WelcomePageFragment extends WizardPageFragment {
    @Override
    public int getPageTitle() {
        return R.string.account_wizard_welcome_page_title;
    }

    @Override
    public int getPageLayout() {
        return R.layout.wizard_welcome_page_fragment;
    }

    public boolean hasExtendedHeader() {
        return true;
    }

    @Override
    public boolean hasForwardAction() {
        return true;
    }

    @Override
    public int getForwardActionLabel() {
        return R.string.action_let_start;
    }

    @Override
    public int getForwardActionDrawable() {
        return R.drawable.ic_chevron_right;
    }
}
