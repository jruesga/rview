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
package com.ruesga.rview;

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.res.TypedArray;
import android.databinding.DataBindingUtil;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.support.annotation.DrawableRes;
import android.support.annotation.Keep;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.ListPopupWindow;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.animation.AccelerateInterpolator;

import com.arlib.floatingsearchview.FloatingSearchView;
import com.arlib.floatingsearchview.suggestions.model.SearchSuggestion;
import com.google.gson.annotations.Since;
import com.ruesga.rview.adapters.SimpleDropDownAdapter;
import com.ruesga.rview.databinding.SearchActivityBinding;
import com.ruesga.rview.gerrit.GerritApi;
import com.ruesga.rview.gerrit.annotations.Until;
import com.ruesga.rview.gerrit.filter.ChangeQuery;
import com.ruesga.rview.gerrit.filter.Option;
import com.ruesga.rview.gerrit.filter.antlr.QueryParseException;
import com.ruesga.rview.gerrit.model.AccountInfo;
import com.ruesga.rview.gerrit.model.DocResult;
import com.ruesga.rview.gerrit.model.ProjectInfo;
import com.ruesga.rview.gerrit.model.ProjectType;
import com.ruesga.rview.gerrit.model.ServerVersion;
import com.ruesga.rview.misc.ActivityHelper;
import com.ruesga.rview.misc.AndroidHelper;
import com.ruesga.rview.misc.ModelHelper;
import com.ruesga.rview.misc.StringHelper;
import com.ruesga.rview.model.Account;
import com.ruesga.rview.preferences.Constants;
import com.ruesga.rview.preferences.Preferences;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import me.tatarka.rxloader2.RxLoader1;
import me.tatarka.rxloader2.RxLoader2;
import me.tatarka.rxloader2.RxLoaderManager;
import me.tatarka.rxloader2.RxLoaderManagerCompat;
import me.tatarka.rxloader2.RxLoaderObserver;
import me.tatarka.rxloader2.safe.SafeObservable;

public class SearchActivity extends AppCompatDelegateActivity {

    private static final int MAX_SUGGESTIONS = 5;

    private static final int FETCH_SUGGESTIONS_MESSAGE = 1;
    private static final int SHOW_HISTORY_MESSAGE = 2;

    private static final String EXTRA_REVEALED = "revealed";

    @Keep
    @SuppressWarnings({"UnusedParameters", "unused"})
    public static class EventHandlers {
        private SearchActivity mActivity;

        public EventHandlers(SearchActivity activity) {
            mActivity = activity;
        }

        public void onDismissByOutsideTouch(View v) {
            mActivity.exitReveal();
        }
    }

    private static class Suggestion implements SearchSuggestion {

        private final String mFilter;
        private final String mPartial;
        private final String mSuggestionText;
        private final String mSuggestionData;
        private final int mSuggestionIcon;
        private final boolean mHistory;

        Suggestion(String filter, String partial, String suggestion, String data,
                   @DrawableRes int icon, boolean history) {
            mFilter = filter;
            mPartial = partial;
            mSuggestionText = suggestion;
            mSuggestionData = data;
            mSuggestionIcon = icon;
            mHistory = history;
        }

        Suggestion(Parcel in) {
            mFilter = in.readString();
            mPartial = in.readString();
            mSuggestionText = in.readString();
            if (in.readInt() == 1) {
                mSuggestionData = in.readString();
            } else {
                mSuggestionData = null;
            }
            mSuggestionIcon = in.readInt();
            mHistory = in.readInt() == 1;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int flags) {
            parcel.writeString(mFilter);
            parcel.writeString(mPartial);
            parcel.writeString(mSuggestionText);
            parcel.writeInt(TextUtils.isEmpty(mSuggestionData) ? 0 : 1);
            if (!TextUtils.isEmpty(mSuggestionData)) {
                parcel.writeString(mSuggestionData);
            }
            parcel.writeInt(mSuggestionIcon);
            parcel.writeInt(mHistory ? 1 : 0);
        }

        @Override
        public String getBody() {
            return mPartial + mSuggestionText;
        }

        public static final Creator<Suggestion> CREATOR = new Creator<Suggestion>() {
            @Override
            public Suggestion createFromParcel(Parcel in) {
                return new Suggestion(in);
            }

            @Override
            public Suggestion[] newArray(int size) {
                return new Suggestion[size];
            }
        };
    }

    private static class AccountInfoResult {
        String mFilter;
        String mPartial;
        List<AccountInfo> mAccounts;
    }

    private static class ProjectInfoResult {
        String mFilter;
        String mPartial;
        List<String> mProjects;
    }

    private static class DocInfoResult {
        String mFilter;
        List<DocResult> mDocs;
    }

    private final RxLoaderObserver<AccountInfoResult> mAccountSuggestionsObserver
            = new RxLoaderObserver<AccountInfoResult>() {
        @Override
        public void onNext(AccountInfoResult response) {
            if (mBinding.searchView != null) {
                List<Suggestion> suggestions = new ArrayList<>(response.mAccounts.size());
                for (AccountInfo account : response.mAccounts) {
                    String suggestion = getString(
                            R.string.account_suggest_format, account.name, account.email);
                    suggestions.add(new Suggestion(response.mFilter, response.mPartial,
                            suggestion, null, R.drawable.ic_search, false));
                }
                mBinding.searchView.swapSuggestions(suggestions);
            }
        }
    };

    @SuppressWarnings("Convert2streamapi")
    private final RxLoaderObserver<ProjectInfoResult> mProjectSuggestionsObserver
            = new RxLoaderObserver<ProjectInfoResult>() {
        @Override
        public void onNext(ProjectInfoResult result) {
            if (mBinding.searchView != null) {
                List<Suggestion> suggestions = new ArrayList<>(result.mProjects.size());
                for (String project : result.mProjects) {
                    try {
                        suggestions.add(new Suggestion(
                                result.mFilter, result.mPartial,
                                URLDecoder.decode(project, "UTF-8"), null,
                                R.drawable.ic_search, false));
                    } catch (UnsupportedEncodingException ex) {
                        // Ignore
                    }
                }
                mBinding.searchView.swapSuggestions(suggestions);
            }
        }
    };

    private final RxLoaderObserver<DocInfoResult> mDocSuggestionsObserver
            = new RxLoaderObserver<DocInfoResult>() {
        @Override
        public void onNext(DocInfoResult response) {
            if (mBinding.searchView != null) {
                List<Suggestion> suggestions = new ArrayList<>(response.mDocs.size());
                for (DocResult doc : response.mDocs) {
                    suggestions.add(new Suggestion(response.mFilter, "",
                            doc.title, doc.url, R.drawable.ic_search, false));
                }
                mBinding.searchView.swapSuggestions(suggestions);
            }
        }
    };

    private Handler.Callback mMessenger = message -> {
        if (message.what == FETCH_SUGGESTIONS_MESSAGE) {
            performFilter((String) message.obj);
        } else if (message.what == SHOW_HISTORY_MESSAGE) {
            performShowHistory();
        }
        return false;
    };

    private Handler mHandler;

    private Account mAccount;
    private SearchActivityBinding mBinding;
    private int mCurrentOption;
    private int[] mIcons;

    private RxLoader2<String, String, AccountInfoResult> mAccountSuggestionsLoader;
    private RxLoader2<String, String, ProjectInfoResult> mProjectSuggestionsLoader;
    private RxLoader1<String, DocInfoResult> mDocSuggestionsLoader;

    private List<String> mSuggestions;

    private static final ServerVersion MIN_VERSION = new ServerVersion(
            String.valueOf(GerritApi.MIN_API_VERSION));

    @Override
    @SuppressLint("RestrictedApi")
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new Handler(mMessenger);
        mAccount = Preferences.getAccount(this);
        mCurrentOption = Preferences.getAccountSearchMode(this, mAccount);

        fillSuggestions();

        mBinding = DataBindingUtil.setContentView(this, R.layout.search_activity);
        mBinding.setHandlers(new EventHandlers(this));

        mIcons = loadSearchIcons();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.menu_search);
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            getSupportActionBar().setDefaultDisplayHomeAsUpEnabled(false);
        }

        // Configure the suggestions loaders
        RxLoaderManager loaderManager = RxLoaderManagerCompat.get(this);
        mAccountSuggestionsLoader = loaderManager.create(
                "accounts", this::fetchAccountSuggestions, mAccountSuggestionsObserver);
        mProjectSuggestionsLoader = loaderManager.create(
                "projects", this::fetchProjectSuggestions, mProjectSuggestionsObserver);
        mDocSuggestionsLoader = loaderManager.create(
                "docs", this::fetchDocSuggestions, mDocSuggestionsObserver);

        // Configure the search view
        mBinding.searchView.setOnSearchListener(new FloatingSearchView.OnSearchListener() {
            @Override
            public boolean onSuggestionClicked(SearchSuggestion suggestion) {
                final Suggestion s = (Suggestion) suggestion;

                // Let type more
                if (!s.mHistory && mCurrentOption == Constants.SEARCH_MODE_CUSTOM) {
                    return true;
                }

                // Directly complete the search
                performSearch(s.mSuggestionText, s.mSuggestionData);
                return false;
            }

            @Override
            public void onSearchAction(String currentQuery) {
                performSearch(currentQuery, null);
            }
        });
        mBinding.searchView.setOnQueryChangeListener((oldFilter, newFilter) -> {
            mHandler.removeMessages(SHOW_HISTORY_MESSAGE);
            mHandler.removeMessages(FETCH_SUGGESTIONS_MESSAGE);
            final Message msg;
            if (TextUtils.isEmpty(newFilter)) {
                clearSuggestions();
                msg = Message.obtain(mHandler, SHOW_HISTORY_MESSAGE);
            } else {
                msg = Message.obtain(mHandler, FETCH_SUGGESTIONS_MESSAGE, newFilter);
                msg.arg1 = mCurrentOption;
            }
            mHandler.sendMessageDelayed(msg, 500L);
        });
        mBinding.searchView.setOnBindSuggestionCallback(
                (v, imageView, textView, suggestion, position) -> {
            final Suggestion s = (Suggestion) suggestion;
            textView.setText(performFilterHighlight(s));
            if (s.mSuggestionIcon != 0) {
                Drawable dw = ContextCompat.getDrawable(this, s.mSuggestionIcon);
                DrawableCompat.setTint(dw, ContextCompat.getColor(
                        this, R.color.gray_active_icon));
                imageView.setImageDrawable(dw);
            } else {
                imageView.setImageDrawable(null);
            }
        });
        mBinding.searchView.setOnMenuItemClickListener(item -> performShowOptions());
        mBinding.searchView.setOnFocusChangeListener(new FloatingSearchView.OnFocusChangeListener() {
            @Override
            public void onFocus() {
                mHandler.removeMessages(FETCH_SUGGESTIONS_MESSAGE);
                mHandler.removeMessages(SHOW_HISTORY_MESSAGE);
                final Message msg = Message.obtain(mHandler, SHOW_HISTORY_MESSAGE);
                mHandler.sendMessageDelayed(msg, 500L);
            }

            @Override
            public void onFocusCleared() {
                // Ignore
            }
        });
        mBinding.searchView.setOnClearSearchActionListener(this::performShowHistory);
        clearSuggestions();

        mBinding.searchView.setCustomIcon(ContextCompat.getDrawable(this, mIcons[mCurrentOption]));

        configureSearchHint();

        boolean revealed = false;
        if (savedInstanceState != null) {
            revealed = savedInstanceState.getBoolean(EXTRA_REVEALED, false);
        }
        if (!revealed) {
            enterReveal();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(EXTRA_REVEALED, true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ActivityHelper.LIST_RESULT_CODE && resultCode == RESULT_OK) {
            // Directly finish this activity. The search data was used
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        exitReveal();
    }

    @Override
    public void onPause() {
        super.onPause();

        if (AndroidHelper.isLollipopOrGreater()) {
            overridePendingTransition(0, 0);
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_MENU:
                    performShowOptions();
                    return true;
            }
        }

        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean onKeyDown(int keycode, KeyEvent e) {
        switch(keycode) {
            case KeyEvent.KEYCODE_MENU:
                performShowOptions();
                return true;
        }

        return super.onKeyDown(keycode, e);
    }

    private void configureSearchHint() {
        switch (mCurrentOption) {
            case Constants.SEARCH_MODE_CHANGE:
                mBinding.searchView.setSearchHint(getString(R.string.search_by_change_hint));
                break;
            case Constants.SEARCH_MODE_COMMIT:
                mBinding.searchView.setSearchHint(getString(R.string.search_by_commit_hint));
                break;
            case Constants.SEARCH_MODE_PROJECT:
                mBinding.searchView.setSearchHint(getString(R.string.search_by_project_hint));
                break;
            case Constants.SEARCH_MODE_USER:
                mBinding.searchView.setSearchHint(getString(R.string.search_by_user_hint));
                break;
            case Constants.SEARCH_MODE_COMMIT_MESSAGE:
                mBinding.searchView.setSearchHint(getString(R.string.search_by_commit_message_hint));
                break;
            case Constants.SEARCH_MODE_CUSTOM:
                mBinding.searchView.setSearchHint(getString(R.string.search_by_custom_hint));
                break;
            case Constants.SEARCH_MODE_DOCS:
                mBinding.searchView.setSearchHint(getString(R.string.search_by_docs_hint));
                break;
        }

        mBinding.searchView.setSearchText(null);
    }

    private void performShowOptions() {
        final ListPopupWindow popupWindow = new ListPopupWindow(this);
        ArrayList<String> values = new ArrayList<>(
                Arrays.asList(getResources().getStringArray(R.array.search_options_labels)));
        String value = values.get(mCurrentOption);
        SimpleDropDownAdapter<Integer> adapter =
                new SimpleDropDownAdapter<>(this, values, mIcons, value);
        popupWindow.setAnchorView(mBinding.anchor);
        popupWindow.setDropDownGravity(Gravity.END);
        popupWindow.setAdapter(adapter);
        popupWindow.setContentWidth(adapter.measureContentWidth());
        popupWindow.setOnItemClickListener((parent, view, position, id) -> {
            popupWindow.dismiss();
            mCurrentOption = position;
            Preferences.setAccountSearchMode(this, mAccount, mCurrentOption);
            configureSearchHint();
            mBinding.searchView.setCustomIcon(ContextCompat.getDrawable(this, mIcons[position]));
            clearSuggestions();

            mHandler.removeMessages(FETCH_SUGGESTIONS_MESSAGE);
            mHandler.removeMessages(SHOW_HISTORY_MESSAGE);
            final Message msg = Message.obtain(mHandler, SHOW_HISTORY_MESSAGE);
            mHandler.sendMessageDelayed(msg, 500L);
        });
        popupWindow.setModal(true);
        popupWindow.show();
    }

    private void performShowHistory() {
        if (!TextUtils.isEmpty(mBinding.searchView.getText())) {
            return;
        }

        ArrayList<Suggestion> suggestions = new ArrayList<>();
        if (mCurrentOption != Constants.SEARCH_MODE_DOCS) {
            String[] history = Preferences.getAccountSearchHistory(this, mAccount, mCurrentOption);
            if (history != null) {
                for (String s : history) {
                    suggestions.add(new Suggestion("", "", s, null, R.drawable.ic_history, true));
                }
            }
            Collections.reverse(suggestions);
        }
        mBinding.searchView.swapSuggestions(suggestions);
    }

    private void performFilter(String filter) {
        if (TextUtils.isEmpty(filter)) {
            return;
        }

        switch (mCurrentOption) {
            case Constants.SEARCH_MODE_CHANGE:
            case Constants.SEARCH_MODE_COMMIT:
            case Constants.SEARCH_MODE_COMMIT_MESSAGE:
                // We cannot show suggestion on this modes
                break;
            case Constants.SEARCH_MODE_PROJECT:
                requestProjectSuggestions(filter, "");
                break;
            case Constants.SEARCH_MODE_USER:
                requestAccountSuggestions(filter, "");
                break;
            case Constants.SEARCH_MODE_CUSTOM:
                requestCustomSuggestions(filter);
                break;
            case Constants.SEARCH_MODE_DOCS:
                requestDocSuggestions(filter);
                break;
        }
    }

    private void clearSuggestions() {
        mBinding.searchView.swapSuggestions(new ArrayList<>());
    }

    private void performSearch(String query, String data) {
        if (TextUtils.isEmpty(query)) {
            clearSuggestions();
            return;
        }

        ChangeQuery filter = null;
        switch (mCurrentOption) {
            case Constants.SEARCH_MODE_CHANGE:
                boolean isLegacyChangeNumber;
                try {
                    int i = Integer.parseInt(query);
                    isLegacyChangeNumber = i > 0;
                } catch (NumberFormatException ex) {
                    isLegacyChangeNumber = false;
                }

                if (isLegacyChangeNumber || StringHelper.GERRIT_CHANGE.matcher(query).matches()) {
                    filter = new ChangeQuery().change(query);
                } else {
                    // Not a valid filter
                    AndroidHelper.showErrorSnackbar(
                            this, mBinding.getRoot(), R.string.search_not_a_valid_change);
                    return;
                }

                break;
            case Constants.SEARCH_MODE_COMMIT:
                if (StringHelper.GERRIT_COMMIT.matcher(query).matches()) {
                    filter = new ChangeQuery().commit(query);
                } else {
                    // Not a valid filter
                    AndroidHelper.showErrorSnackbar(
                            this, mBinding.getRoot(), R.string.search_not_a_valid_commit);
                    return;
                }
                break;
            case Constants.SEARCH_MODE_PROJECT:
                filter = new ChangeQuery().project(query);
                break;
            case Constants.SEARCH_MODE_USER:
                String cleanedQuery = clearOwnerQuery(query);
                filter = new ChangeQuery().owner(cleanedQuery);
                break;
            case Constants.SEARCH_MODE_COMMIT_MESSAGE:
                filter = new ChangeQuery().message(query);
                break;
            case Constants.SEARCH_MODE_CUSTOM:
                try {
                    filter = ChangeQuery.parse(query);
                } catch (QueryParseException ex) {
                    // Not a valid filter
                    AndroidHelper.showErrorSnackbar(
                            this, mBinding.getRoot(), R.string.search_not_a_valid_custom_query);
                    return;
                }
                break;
            case Constants.SEARCH_MODE_DOCS:
                if (!TextUtils.isEmpty(data)) {
                    final GerritApi api = ModelHelper.getGerritApi(this);
                    //noinspection ConstantConditions
                    ActivityHelper.openUriInCustomTabs(this, api.getDocumentationUri(data));
                    finish();
                }
                return;
        }

        // Open the activity
        ActivityHelper.openChangeListByFilterActivity(this, null, filter, true, false);

        // Persist history
        String history = mCurrentOption != Constants.SEARCH_MODE_CUSTOM
                ? query : String.valueOf(filter);
        Preferences.addAccountSearchHistory(this, mAccount, mCurrentOption, history);
    }

    private int[] loadSearchIcons() {
        TypedArray ta = getResources().obtainTypedArray(R.array.search_options_icons);
        int count = ta.length();
        int[] icons = new int[count];
        for (int i = 0; i < count; i++) {
            icons[i] = ta.getResourceId(i, -1);
        }
        ta.recycle();
        return icons;
    }

    @SuppressWarnings("ConstantConditions")
    private Observable<AccountInfoResult> fetchAccountSuggestions(String filter, String partial) {
        final GerritApi api = ModelHelper.getGerritApi(this);
        return SafeObservable.fromNullCallable(() -> {
                    AccountInfoResult result = new AccountInfoResult();
                    result.mFilter = filter;
                    result.mPartial = partial;
                    result.mAccounts =
                            api.getAccountsSuggestions(
                                    filter, MAX_SUGGESTIONS, Option.INSTANCE).blockingFirst();
                    return result;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    private void requestAccountSuggestions(String filter, String partial) {
        mAccountSuggestionsLoader.clear();
        mAccountSuggestionsLoader.restart(filter, partial);
    }

    @SuppressWarnings("ConstantConditions")
    private Observable<ProjectInfoResult> fetchProjectSuggestions(String filter, String partial) {
        final GerritApi api = ModelHelper.getGerritApi(this);
        return SafeObservable.fromNullCallable(() -> {
                    ProjectInfoResult result = new ProjectInfoResult();
                    result.mFilter = filter;
                    result.mPartial = partial;
                    result.mProjects = new ArrayList<>(
                            api.getProjects(MAX_SUGGESTIONS, null, null, null, filter,
                                null, null, null, ProjectType.ALL, null).blockingFirst().keySet());
                    return result;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    private void requestProjectSuggestions(String filter, String partial) {
        mProjectSuggestionsLoader.clear();
        mProjectSuggestionsLoader.restart(filter, partial);
    }

    @SuppressWarnings("ConstantConditions")
    private Observable<DocInfoResult> fetchDocSuggestions(String filter) {
        final GerritApi api = ModelHelper.getGerritApi(this);
        return SafeObservable.fromNullCallable(() -> {
                    DocInfoResult result = new DocInfoResult();
                    result.mFilter = filter;
                    result.mDocs = api.findDocumentation(filter).blockingFirst();
                    if (result.mDocs.size() > MAX_SUGGESTIONS) {
                        result.mDocs = result.mDocs.subList(0, MAX_SUGGESTIONS);
                    }
                    return result;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    private void requestDocSuggestions(String filter) {
        mDocSuggestionsLoader.clear();
        mDocSuggestionsLoader.restart(filter);
    }

    @SuppressWarnings("Convert2streamapi")
    private void requestCustomSuggestions(String filter) {
        // Do no perform suggestion when there are selection or cursor is not at the end
        // of the textview
        if (mBinding.searchView.getSelectionStart() != mBinding.searchView.getSelectionStart()
                || mBinding.searchView.getSelectionStart() < mBinding.searchView.getText().length()) {
            clearSuggestions();
            return;
        }

        // Extract the current filter
        int pos = filter.lastIndexOf(" ");
        pos = pos == -1 ? 0 : ++pos;
        String currentFilter = filter.substring(pos);
        String partial = filter.substring(0, pos);

        // Some sanitize checks
        if (TextUtils.isEmpty(currentFilter)) {
            clearSuggestions();
            return;
        }
        char c = filter.charAt(pos);
        if (!Character.isLetter(c) && !Character.isDigit(c)) {
            clearSuggestions();
            return;
        }


        // Extract the token
        pos = currentFilter.indexOf(":");
        if (pos != -1) {
            String token = currentFilter.substring(0, pos);
            currentFilter = currentFilter.substring(pos + 1);
            partial += token + ":";
            if (TextUtils.isEmpty(currentFilter)) {
                clearSuggestions();
                return;
            }

            final int index = Arrays.asList(ChangeQuery.FIELDS_NAMES).indexOf(token);
            if (index != -1) {
                Double minVersion = ChangeQuery.SUPPORTED_FROM_VERSION[index];
                Double maxVersion = ChangeQuery.UNSUPPORTED_FROM_VERSION[index];
                ServerVersion serverVersion =
                        mAccount == null ? MIN_VERSION : mAccount.getServerVersion();
                if ((minVersion == null || (serverVersion != null
                        && minVersion <= serverVersion.getVersion())) &&
                    (maxVersion == null || (serverVersion != null
                            && maxVersion >= serverVersion.getVersion()))) {
                    Class clazz = ChangeQuery.SUGGEST_TYPES[index];
                    if (clazz != null) {
                        if (clazz.equals(AccountInfo.class)) {
                            requestAccountSuggestions(currentFilter, partial);
                            return;
                        } else if (clazz.equals(ProjectInfo.class)) {
                            requestProjectSuggestions(currentFilter, partial);
                            return;
                        }
                    }
                }
            }
        }

        final List<Suggestion> suggestions = new ArrayList<>();
        String f = partial + currentFilter;
        pos = f.trim().lastIndexOf(" ");
        if (pos != -1) {
            partial = f.substring(0, pos + 1);
            f = f.substring(pos + 1);
        } else if (f.contains(":")) {
            partial = "";
        }
        for (String s : mSuggestions) {
            if (s.startsWith(f) && !s.trim().equals(f)) {
                suggestions.add(new Suggestion(f, partial, s, null, R.drawable.ic_search, false));
            }
        }
        mBinding.searchView.swapSuggestions(suggestions);
    }

    private CharSequence performFilterHighlight(Suggestion suggestion) {
        Spannable span = Spannable.Factory.getInstance().newSpannable(suggestion.mSuggestionText);
        int pos = 0;
        final Locale locale = AndroidHelper.getCurrentLocale(getApplicationContext());
        final String suggestionNoCase = suggestion.mSuggestionText.toLowerCase(locale);
        final String filterNoCase = suggestion.mFilter.toLowerCase(locale);
        while ((pos = suggestionNoCase.indexOf(filterNoCase, pos)) != -1) {
            final int length = suggestion.mFilter.length();
            if (length == 0) {
                break;
            }
            final StyleSpan bold = new StyleSpan(android.graphics.Typeface.BOLD);
            final ForegroundColorSpan color = new ForegroundColorSpan(
                    ContextCompat.getColor(this, R.color.accent));
            span.setSpan(bold, pos, pos + length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            span.setSpan(color, pos, pos + length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            pos += length;
            if (pos >= suggestionNoCase.length()) {
                break;
            }
        }
        return span;
    }

    private void fillSuggestions() {
        mSuggestions = new ArrayList<>();
        int count = ChangeQuery.FIELDS_NAMES.length;
        for (int i = 0; i < count; i++) {
            Double minVersion = ChangeQuery.SUPPORTED_FROM_VERSION[i];
            Double maxVersion = ChangeQuery.UNSUPPORTED_FROM_VERSION[i];
            ServerVersion serverVersion =
                    mAccount == null ? MIN_VERSION : mAccount.getServerVersion();
            if ((minVersion == null || (serverVersion != null
                    && minVersion <= serverVersion.getVersion())) &&
                (maxVersion == null || (serverVersion != null
                        && maxVersion >= serverVersion.getVersion()))) {
                mSuggestions.add(ChangeQuery.FIELDS_NAMES[i] + ":");
                if (ChangeQuery.SUGGEST_TYPES[i] != null && ChangeQuery.SUGGEST_TYPES[i].isEnum()) {
                    for (Object o : ChangeQuery.SUGGEST_TYPES[i].getEnumConstants()) {
                        try {
                            Since since = o.getClass().getDeclaredField(o.toString())
                                    .getAnnotation(Since.class);
                            if (since != null && since.value() > serverVersion.getVersion()) {
                                continue;
                            }
                        } catch (NoSuchFieldException ex) {
                            // Ignore
                        }

                        try {
                            Until until = o.getClass().getDeclaredField(o.toString())
                                    .getAnnotation(Until.class);
                            if (until != null && until.value() <= serverVersion.getVersion()) {
                                continue;
                            }
                        } catch (NoSuchFieldException ex) {
                            // Ignore
                        }

                        String val = String.valueOf(o).toLowerCase(Locale.US);
                        mSuggestions.add(ChangeQuery.FIELDS_NAMES[i] + ":" + val + " ");
                    }
                }
            }
        }

        Collections.sort(mSuggestions);
    }

    private String clearOwnerQuery(String query) {
        if (query.startsWith("\"") && query.endsWith("\"") && query.length() >= 3) {
            return query.substring(1, query.length() - 1);
        }
        return query;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void enterReveal() {
        if (AndroidHelper.isLollipopOrGreater()) {
            ViewCompat.postOnAnimation(mBinding.toolbar,
                    new RevealAnimationRunnable(mBinding.toolbar, mBinding.searchView, true));
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void exitReveal() {
        if (!AndroidHelper.isLollipopOrGreater()) {
            finish();
            return;
        }

        ViewCompat.postOnAnimation(mBinding.toolbar,
                new RevealAnimationRunnable(mBinding.toolbar, mBinding.searchView, false));
    }

    private class RevealAnimationRunnable implements Runnable {
        private final View mTarget;
        private final View mParent;
        private final boolean mIn;

        RevealAnimationRunnable(View parent, View target, boolean in) {
            mTarget = target;
            mParent = parent;
            mIn = in;
        }

        @Override
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        public void run() {
            int cx = mParent.getMeasuredWidth();
            int cy = mParent.getMeasuredHeight() / 2;
            Animator anim = ViewAnimationUtils.createCircularReveal(
                    mTarget, cx, cy, mIn ? 0 : cx, mIn ? cx : 0);
            anim.setInterpolator(new AccelerateInterpolator());
            anim.setDuration(mIn ? 350L : 250L);
            anim.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                    if (!mIn) {
                        mBinding.toolbar.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    mBinding.searchView.setVisibility(mIn ? View.VISIBLE : View.INVISIBLE);
                    if (!mIn) {
                        finish();
                    }
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                }

                @Override
                public void onAnimationRepeat(Animator animation) {
                }
            });
            anim.start();
        }
    }
}
