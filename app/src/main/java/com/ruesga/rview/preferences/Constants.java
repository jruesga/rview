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
package com.ruesga.rview.preferences;

public class Constants {
    /**
     * The minimal Gerrit server version supported.
     */
    public static final double MINIMAL_SUPPORTED_VERSION = 2.11d;

    public static final String EXTRA_CHANGE_ID = "changeId";
    public static final String EXTRA_LEGACY_CHANGE_ID = "legacyChangeId";
    public static final String EXTRA_REVISION_ID = "revisionId";
    public static final String EXTRA_FILE_ID = "fileId";


    public static final String DEFAULT_AUTHENTICATED_HOME = "menu_dashboard";
    public static final String DEFAULT_ANONYMOUS_HOME = "menu_open";
    public static final String DEFAULT_FETCHED_ITEMS = "25";
    public static final String DEFAULT_DISPLAY_FORMAT = "name";

    public static final String ACCOUNT_DISPLAY_FORMAT_NAME = "name";
    public static final String ACCOUNT_DISPLAY_FORMAT_EMAIL = "email";
    public static final String ACCOUNT_DISPLAY_FORMAT_USERNAME = "username";

    // --- Preference keys
    public static final String PREF_IS_FIRST_RUN = "first_run";
    public static final String PREF_ACCOUNT = "account";
    public static final String PREF_ACCOUNTS = "accounts";

    // -- Account preferences keys
    public static final String PREF_ACCOUNT_HOME_PAGE = "account_home_page";
    public static final String PREF_ACCOUNT_FETCHED_ITEMS = "account_fetched_items";
    public static final String PREF_ACCOUNT_DISPLAY_FORMAT = "account_display_format";
    public static final String PREF_ACCOUNT_HIGHLIGHT_UNREVIEWED = "account_highlight_unreviewed";
}
