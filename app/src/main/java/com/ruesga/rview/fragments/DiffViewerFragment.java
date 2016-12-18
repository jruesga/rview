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
package com.ruesga.rview.fragments;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.graphics.drawable.Drawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.ListPopupWindow;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.ruesga.rview.BaseActivity;
import com.ruesga.rview.R;
import com.ruesga.rview.adapters.SimpleDropDownAdapter;
import com.ruesga.rview.annotations.ProguardIgnored;
import com.ruesga.rview.databinding.DiffActionsHeaderBinding;
import com.ruesga.rview.databinding.DiffBaseChooserHeaderBinding;
import com.ruesga.rview.databinding.DiffViewerFragmentBinding;
import com.ruesga.rview.drawer.DrawerNavigationView.OnDrawerNavigationItemSelectedListener;
import com.ruesga.rview.fragments.FileDiffViewerFragment.OnDiffCompleteListener;
import com.ruesga.rview.gerrit.GerritApi;
import com.ruesga.rview.gerrit.model.ChangeInfo;
import com.ruesga.rview.gerrit.model.FileInfo;
import com.ruesga.rview.gerrit.model.FileStatus;
import com.ruesga.rview.misc.CacheHelper;
import com.ruesga.rview.misc.FowlerNollVo;
import com.ruesga.rview.misc.ModelHelper;
import com.ruesga.rview.misc.SerializationManager;
import com.ruesga.rview.model.Account;
import com.ruesga.rview.preferences.Constants;
import com.ruesga.rview.preferences.Preferences;
import com.ruesga.rview.widget.DiffView;
import com.ruesga.rview.widget.PagerControllerLayout.PagerControllerAdapter;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import me.tatarka.rxloader2.RxLoader2;
import me.tatarka.rxloader2.RxLoaderManager;
import me.tatarka.rxloader2.RxLoaderManagerCompat;
import me.tatarka.rxloader2.RxLoaderObserver;
import me.tatarka.rxloader2.safe.SafeObservable;

public class DiffViewerFragment extends Fragment implements KeyEventBindable, OnDiffCompleteListener {

    private static final String TAG = "DiffViewerFragment";

    private static final int REQUEST_PERMISSION_LEFT = 100;
    private static final int REQUEST_PERMISSION_RIGHT = 101;
    private static final String[] PERMISSIONS  = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE"
    };

    @ProguardIgnored
    public static class Model {
        public String baseLeft;
        public String baseRight;

        public boolean hasCommentAction;
        public boolean hasLeftDownloadAction;
        public boolean hasRightDownloadAction;
    }

    @ProguardIgnored
    @SuppressWarnings("unused")
    public static class EventHandlers {
        private final DiffViewerFragment mFragment;

        public EventHandlers(DiffViewerFragment fragment) {
            mFragment = fragment;
        }

        public void onBaseChooserPressed(View v) {
            mFragment.performShowBaseChooser(v);
        }

        public void onActionPressed(View v) {
            final boolean isLeft = (boolean) v.getTag();
            switch (v.getId()) {
                case R.id.comment:
                    mFragment.performFileComment(v, isLeft);
                    break;
                case R.id.download:
                    mFragment.requestPermissionsOrDownloadFile(isLeft);
                    break;
            }
        }
    }

    private PagerControllerAdapter<String> mAdapter = new PagerControllerAdapter<String>() {

        @Override
        public FragmentManager getFragmentManager() {
            return getChildFragmentManager();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            if (position < 0 || position >= getCount()) {
                return null;
            }
            return new File(mFiles.get(position)).getName();
        }

        @Override
        public String getItem(int position) {
            if (position < 0 || position >= getCount()) {
                return null;
            }
            return mFiles.get(position);
        }

        @Override
        public int getCount() {
            return mFiles.size();
        }

        @Override
        public Fragment getFragment(int position) {
            int base = 0;
            try {
                base = Integer.valueOf(mBase);
            } catch (Exception ex) {
                // Ignore
            }
            int revision = mChange.revisions.get(mRevisionId).number;

            final FileDiffViewerFragment fragment = FileDiffViewerFragment.newInstance(
                    mRevisionId, mFile, mComment, base, revision, mMode, mWrap, mTextSizeFactor,
                    mHighlightTabs, mHighlightTrailingWhitespaces, mHighlightIntralineDiffs);
            mFragment = new WeakReference<>(fragment);
            return fragment;
        }

        @Override
        public int getTarget() {
            return R.id.diff_content;
        }
    };

    private OnDrawerNavigationItemSelectedListener mOptionsItemListener
            = new OnDrawerNavigationItemSelectedListener() {
        @Override
        public boolean onDrawerNavigationItemSelected(MenuItem item) {
            if (mFragment != null && mFragment.get() != null) {
                switch (item.getItemId()) {
                    case R.id.diff_mode_unified:
                        mMode = DiffView.UNIFIED_MODE;
                        Preferences.setAccountDiffMode(
                                getContext(), mAccount, Constants.DIFF_MODE_UNIFIED);
                        break;
                    case R.id.diff_mode_side_by_side:
                        mMode = DiffView.SIDE_BY_SIDE_MODE;
                        Preferences.setAccountDiffMode(
                                getContext(), mAccount, Constants.DIFF_MODE_SIDE_BY_SIDE);
                        break;
                    case R.id.diff_mode_image:
                        mMode = DiffView.IMAGE_MODE;
                        break;
                    case R.id.wrap_mode_on:
                        mWrap = true;
                        Preferences.setAccountWrapMode(getContext(), mAccount, mWrap);
                        break;
                    case R.id.wrap_mode_off:
                        mWrap = false;
                        Preferences.setAccountWrapMode(getContext(), mAccount, mWrap);
                        break;
                    case R.id.text_size_smaller:
                        mTextSizeFactor = Constants.DEFAULT_TEXT_SIZE_SMALLER;
                        Preferences.setAccountTextSizeFactor(
                                getContext(), mAccount, mTextSizeFactor);
                        break;
                    case R.id.text_size_normal:
                        mTextSizeFactor = Constants.DEFAULT_TEXT_SIZE_NORMAL;
                        Preferences.setAccountTextSizeFactor(
                                getContext(), mAccount, mTextSizeFactor);
                        break;
                    case R.id.text_size_bigger:
                        mTextSizeFactor = Constants.DEFAULT_TEXT_SIZE_BIGGER;
                        Preferences.setAccountTextSizeFactor(
                                getContext(), mAccount, mTextSizeFactor);
                        break;
                    case R.id.highlight_tabs:
                        mHighlightTabs = !mHighlightTabs;
                        Preferences.setAccountHighlightTabs(getContext(), mAccount, mHighlightTabs);
                        break;
                    case R.id.highlight_trailing_whitespaces:
                        mHighlightTrailingWhitespaces = !mHighlightTrailingWhitespaces;
                        Preferences.setAccountHighlightTrailingWhitespaces(
                                getContext(), mAccount, mHighlightTrailingWhitespaces);
                        break;
                    case R.id.highlight_intraline_diffs:
                        mHighlightIntralineDiffs = !mHighlightIntralineDiffs;
                        Preferences.setAccountHighlightIntralineDiffs(
                                getContext(), mAccount, mHighlightIntralineDiffs);
                        break;
                }
            }

            // Close the drawer and force a refresh of the UI (give some time to
            // ensure the drawer is closed)
            ((BaseActivity) getActivity()).closeOptionsDrawer();
            mHandler.postDelayed(() -> forceRefresh(), 250L);
            return true;
        }
    };

    private final RxLoaderObserver<ArrayList<String>> mFilesObserver
            = new RxLoaderObserver<ArrayList<String>>() {
        @Override
        public void onNext(ArrayList<String> response) {
            // Refresh the page controller
            mFiles.clear();
            mFiles.addAll(new ArrayList<>(response));
            mCurrentFile = mFiles.indexOf(mFile);

            try {
                String current = String.valueOf(mChange.revisions.get(mRevisionId).number);
                String prefix = (mBase == null ? "0" : mBase) + "_" + current + "_";
                CacheHelper.writeAccountDiffCacheFile(getContext(),
                        prefix + CacheHelper.CACHE_FILES_JSON,
                        SerializationManager.getInstance().toJson(mFiles).getBytes());
            } catch (IOException ex) {
                Log.e(TAG, "Failed to save files cached data", ex);
            }

            configurePageController((BaseActivity) getActivity(), true);

            // Refresh the view
            forceRefresh();
        }

        @Override
        public void onError(Throwable error) {
            ((BaseActivity) getActivity()).handleException(TAG, error);
        }
    };

    @Override
    public boolean onKeyDown(int keycode, KeyEvent e) {
        switch(keycode) {
            case KeyEvent.KEYCODE_MENU:
                ((BaseActivity) getActivity()).openOptionsDrawer();
                return true;
        }

        return false;
    }

    @Override
    public void onDiffComplete(boolean isBinary, boolean hasImagePreview) {
        mIsBinary = isBinary;
        mHasImagePreview = hasImagePreview;
        applyModeRestrictions();
        updateModel();
    }

    @Override
    public void onNewDraftCreated(String revision, String draftId) {
        if (mResult == null) {
            mResult = new Intent();
        }
        mResult.putExtra(Constants.EXTRA_DATA_CHANGED, true);
        getActivity().setResult(Activity.RESULT_OK, mResult);
    }

    @Override
    public void onDraftDeleted(String revision, String draftId) {
        if (mResult == null) {
            mResult = new Intent();
        }
        mResult.putExtra(Constants.EXTRA_DATA_CHANGED, true);
        getActivity().setResult(Activity.RESULT_OK, mResult);
    }

    @Override
    public void onDraftUpdated(String revision, String draftId) {
        // Ignore
    }

    private DiffViewerFragmentBinding mBinding;
    private DiffBaseChooserHeaderBinding mBaseChooserBinding;
    private DiffActionsHeaderBinding mActionsBinding;
    private Model mModel = new Model();
    private EventHandlers mEventHandlers;

    private WeakReference<FileDiffViewerFragment> mFragment;
    private Handler mHandler;

    private ChangeInfo mChange;
    private final ArrayList<String> mFiles = new ArrayList<>();
    private String mRevisionId;
    private String mFile;
    private String mBase;
    private String mComment;

    private RxLoader2<String, String, ArrayList<String>> mFilesLoader;

    private final List<String> mAllRevisions = new ArrayList<>();

    private int mMode = -1;
    private boolean mWrap;
    private float mTextSizeFactor;
    private boolean mHighlightTabs;
    private boolean mHighlightTrailingWhitespaces;
    private boolean mHighlightIntralineDiffs;

    private boolean mIsBinary = false;
    private boolean mHasImagePreview = false;

    private int mCurrentFile;

    private Account mAccount;

    private Intent mResult;

    public static DiffViewerFragment newInstance(
            String revisionId, String base, String file, String comment) {
        DiffViewerFragment fragment = new DiffViewerFragment();
        Bundle arguments = new Bundle();
        arguments.putString(Constants.EXTRA_REVISION_ID, revisionId);
        arguments.putString(Constants.EXTRA_BASE, base);
        arguments.putString(Constants.EXTRA_FILE, file);
        if (comment != null) {
            arguments.putString(Constants.EXTRA_COMMENT, comment);
        }
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mEventHandlers = new EventHandlers(this);
        mHandler = new Handler();

        Bundle state = (savedInstanceState != null) ? savedInstanceState : getArguments();
        mRevisionId = state.getString(Constants.EXTRA_REVISION_ID);
        mFile = state.getString(Constants.EXTRA_FILE);
        mBase = state.getString(Constants.EXTRA_BASE);
        mComment = state.getString(Constants.EXTRA_COMMENT);
        if (state.containsKey(Constants.EXTRA_DATA)) {
            mResult = state.getParcelable(Constants.EXTRA_DATA);
        }
        if (savedInstanceState != null) {
            mMode = savedInstanceState.getInt("mode", -1);
            mIsBinary = savedInstanceState.getBoolean("is_binary", false);
            mHasImagePreview = savedInstanceState.getBoolean("has_image_preview", false);
            applyModeRestrictions();
        }

        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(
                inflater, R.layout.diff_viewer_fragment, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle state) {
        super.onActivityCreated(state);

        try {
            // Deserialize the change
            mChange = SerializationManager.getInstance().fromJson(
                    new String(CacheHelper.readAccountDiffCacheFile(
                            getContext(), CacheHelper.CACHE_CHANGE_JSON)), ChangeInfo.class);

            // Deserialize the files
            mFiles.clear();
            String current = String.valueOf(mChange.revisions.get(mRevisionId).number);
            String prefix = (mBase == null ? "0" : mBase) + "_" + current + "_";
            String name = prefix + CacheHelper.CACHE_FILES_JSON;
            if (CacheHelper.hasAccountDiffCache(getContext(), name)) {
                Type type = new TypeToken<List<String>>() {}.getType();
                List<String> files = SerializationManager.getInstance().fromJson(
                        new String(CacheHelper.readAccountDiffCacheFile(getContext(), name)), type);
                mFiles.addAll(files);
            }
            if (!mFiles.isEmpty()) {
                mCurrentFile = mFiles.indexOf(mFile);
            }

            // Load revisions
            loadRevisions();

            // Get diff user preferences
            mAccount = Preferences.getAccount(getContext());
            String diffMode = Preferences.getAccountDiffMode(getContext(), mAccount);
            if (mMode == -1) {
                mMode = diffMode.equals(Constants.DIFF_MODE_SIDE_BY_SIDE)
                        ? DiffView.SIDE_BY_SIDE_MODE : DiffView.UNIFIED_MODE;
            }
            mWrap = Preferences.getAccountWrapMode(getContext(), mAccount);
            mTextSizeFactor = Preferences.getAccountTextSizeFactor(getContext(), mAccount);
            mHighlightTabs = Preferences.isAccountHighlightTabs(getContext(), mAccount);
            mHighlightTrailingWhitespaces =
                    Preferences.isAccountHighlightTrailingWhitespaces(getContext(), mAccount);
            mHighlightIntralineDiffs =
                    Preferences.isAccountHighlightIntralineDiffs(getContext(), mAccount);

            // Configure the pages adapter
            BaseActivity activity = ((BaseActivity) getActivity());
            configurePageController(activity, false);

            // Configure the diff_options menu
            activity.configureOptionsTitle(getString(R.string.menu_diff_options));
            activity.configureOptionsMenu(R.menu.diff_options_menu, mOptionsItemListener);

            mBaseChooserBinding = DataBindingUtil.inflate(LayoutInflater.from(getContext()),
                    R.layout.diff_base_chooser_header, activity.getOptionsMenu(), false);
            mBaseChooserBinding.setHandlers(mEventHandlers);
            activity.getOptionsMenu().addHeaderView(mBaseChooserBinding.getRoot());

            mActionsBinding = DataBindingUtil.inflate(LayoutInflater.from(getContext()),
                    R.layout.diff_actions_header, activity.getOptionsMenu(), false);
            mActionsBinding.setHandlers(mEventHandlers);
            mActionsBinding.setHandlers(mEventHandlers);
            activity.getOptionsMenu().addHeaderView(mActionsBinding.getRoot());

            updateModel();

            if (mResult != null) {
                getActivity().setResult(Activity.RESULT_OK, mResult);
            }

            RxLoaderManager loaderManager = RxLoaderManagerCompat.get(this);
            mFilesLoader = loaderManager.create(
                    "files" + hashCode(), this::loadFiles, mFilesObserver);
            if (mFiles.isEmpty()) {
                performLoadFiles();
            }

        } catch (IOException ex) {
            Log.e(TAG, "Failed to load change cached data", ex);
            getActivity().finish();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mBinding != null) {
            mBinding.unbind();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.diff_options, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_diff_options:
                openOptionsMenu();
                break;
        }
        return false;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(Constants.EXTRA_REVISION_ID, mRevisionId);
        outState.putString(Constants.EXTRA_FILE, mFile);
        outState.putString(Constants.EXTRA_BASE, mBase);
        if (mComment != null) {
            outState.putString(Constants.EXTRA_COMMENT, mComment);
        }
        outState.putInt("mode", mMode);
        outState.putBoolean("is_binary", mIsBinary);
        outState.putBoolean("has_image_preview", mHasImagePreview);
        outState.putString(Constants.EXTRA_REVISION_ID, mRevisionId);
        if (mResult != null) {
            outState.putParcelable(Constants.EXTRA_DATA, mResult);
        }
    }

    @SuppressWarnings("Convert2streamapi")
    private void loadRevisions() {
        mAllRevisions.clear();
        for (String revision : mChange.revisions.keySet()) {
            mAllRevisions.add(String.valueOf(mChange.revisions.get(revision).number));
        }
        Collections.sort(mAllRevisions, (o1, o2) -> {
            int a = Integer.valueOf(o1);
            int b = Integer.valueOf(o2);
            if (a < b) {
                return -1;
            }
            if (a > b) {
                return 1;
            }
            return 0;
        });
    }

    private void openOptionsMenu() {
        Drawable checkMark = ContextCompat.getDrawable(getContext(), R.drawable.ic_check_box);
        Drawable uncheckMark = ContextCompat.getDrawable(
                getContext(), R.drawable.ic_check_box_outline);

        // Update diff_options
        BaseActivity activity =  ((BaseActivity) getActivity());
        Menu menu = activity.getOptionsMenu().getMenu();
        menu.findItem(R.id.diff_mode_side_by_side).setChecked(mMode == DiffView.SIDE_BY_SIDE_MODE);
        menu.findItem(R.id.diff_mode_unified).setChecked(mMode == DiffView.UNIFIED_MODE);
        menu.findItem(R.id.diff_mode_image).setChecked(mMode == DiffView.IMAGE_MODE);
        menu.findItem(R.id.diff_mode_side_by_side).setVisible(!mIsBinary || !mHasImagePreview);
        menu.findItem(R.id.diff_mode_unified).setVisible(!mIsBinary || !mHasImagePreview);
        menu.findItem(R.id.diff_mode_image).setVisible(mHasImagePreview);
        menu.findItem(R.id.wrap_mode).setVisible(mMode != DiffView.IMAGE_MODE);
        menu.findItem(R.id.wrap_mode_on).setChecked(mWrap);
        menu.findItem(R.id.wrap_mode_off).setChecked(!mWrap);
        menu.findItem(R.id.text_size_smaller).setChecked(
                mTextSizeFactor == Constants.DEFAULT_TEXT_SIZE_SMALLER);
        menu.findItem(R.id.text_size_normal).setChecked(
                mTextSizeFactor == Constants.DEFAULT_TEXT_SIZE_NORMAL);
        menu.findItem(R.id.text_size_bigger).setChecked(
                mTextSizeFactor == Constants.DEFAULT_TEXT_SIZE_BIGGER);
        menu.findItem(R.id.highlight).setVisible(mMode != DiffView.IMAGE_MODE);
        menu.findItem(R.id.highlight_tabs).setIcon(mHighlightTabs ? checkMark : uncheckMark);
        menu.findItem(R.id.highlight_trailing_whitespaces).setIcon(
                mHighlightTrailingWhitespaces ? checkMark : uncheckMark);
        menu.findItem(R.id.highlight_intraline_diffs).setIcon(
                mHighlightIntralineDiffs ? checkMark : uncheckMark);

        // Open drawer
        activity.openOptionsDrawer();
    }

    @SuppressWarnings("ConstantConditions")
    private void updateModel() {
        mModel.baseLeft = mBase == null ? getString(R.string.options_base) : mBase;
        mModel.baseRight = String.valueOf(mChange.revisions.get(mRevisionId).number);

        // Actions
        if (mChange != null) {
            FileStatus status = FileStatus.A;
            if (mChange.revisions.get(mRevisionId).files.containsKey(mFile)) {
                status = mChange.revisions.get(mRevisionId).files.get(mFile).status;
            }
            mModel.hasCommentAction =
                    mAccount.hasAuthenticatedAccessMode() && mMode != DiffView.IMAGE_MODE;
            mModel.hasLeftDownloadAction = !status.equals(FileStatus.A);
            mModel.hasRightDownloadAction = !status.equals(FileStatus.D);
        }

        mBaseChooserBinding.setModel(mModel);
        mActionsBinding.setModel(mModel);
    }

    @SuppressWarnings("Convert2streamapi")
    private void performShowBaseChooser(View v) {
        final List<String> revisions = new ArrayList<>(mAllRevisions);
        String value;
        if (v.getId() == R.id.baseLeft) {
            String baseText = getString(R.string.options_base);
            value = mBase == null ? baseText : mBase;
            revisions.add(0, baseText);
        } else {
            value = String.valueOf(mChange.revisions.get(mRevisionId).number);
        }

        final ListPopupWindow popupWindow = new ListPopupWindow(getContext());
        SimpleDropDownAdapter adapter = new SimpleDropDownAdapter(
                getContext(), revisions, value);
        popupWindow.setAnchorView(v);
        popupWindow.setAdapter(adapter);
        popupWindow.setContentWidth(adapter.measureContentWidth());
        popupWindow.setOnItemClickListener((parent, view, position, id) -> {
            popupWindow.dismiss();
            if (v.getId() == R.id.baseLeft) {
                String base = mBase;
                if (position == 0) {
                    mBase = null;
                } else {
                    mBase = String.valueOf(Integer.parseInt(revisions.get(position)));
                }

                boolean changed = (base == null && mBase != null) ||
                        (base != null && mBase == null) ||
                        (base != null && !base.equals(mBase));
                if (changed) {
                    if (mResult == null) {
                        mResult = new Intent();
                    }
                    mResult.putExtra(Constants.EXTRA_BASE, mBase);
                    getActivity().setResult(Activity.RESULT_OK, mResult);
                }
            } else {
                String revisionId = mRevisionId;
                int rev = Integer.parseInt(revisions.get(position));
                for (String revision : mChange.revisions.keySet()) {
                    if (mChange.revisions.get(revision).number == rev) {
                        revisionId = revision;
                    }
                }

                if (!revisionId.equals(mRevisionId)) {
                    mRevisionId = revisionId;

                    if (mResult == null) {
                        mResult = new Intent();
                    }
                    mResult.putExtra(Constants.EXTRA_REVISION_ID, mRevisionId);
                    getActivity().setResult(Activity.RESULT_OK, mResult);
                }
            }

            // Close the drawer
            BaseActivity activity = (BaseActivity) getActivity();
            activity.closeOptionsDrawer();

            // Refresh files
            performLoadFiles();
        });
        popupWindow.setModal(true);
        popupWindow.show();
    }

    private void forceRefresh() {
        // Check if activity is still attached
        if (getActivity() == null) {
            return;
        }

        updateModel();
        mAdapter.notifyDataSetChanged();
    }

    private void applyModeRestrictions() {
        if (mMode == DiffView.IMAGE_MODE && !mHasImagePreview) {
            String mode = Preferences.getAccountDiffMode(getContext(), mAccount);
            mMode = mode.equals(Constants.DIFF_MODE_SIDE_BY_SIDE)
                    ? DiffView.SIDE_BY_SIDE_MODE : DiffView.UNIFIED_MODE;
        } else if (mMode != DiffView.IMAGE_MODE && mIsBinary && mHasImagePreview) {
            mMode = DiffView.IMAGE_MODE;
        }
    }

    private void performFileComment(View v, boolean isLeft) {
        ((BaseActivity) getActivity()).closeOptionsDrawer();

        FileDiffViewerFragment fragment = mFragment.get();
        if (fragment != null) {
            fragment.getCommentListener().onNewDraft(v, isLeft, null);
        }
    }

    private void requestPermissionsOrDownloadFile(boolean isLeft) {
        ((BaseActivity) getActivity()).closeOptionsDrawer();

        int readPermissionCheck = ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.READ_EXTERNAL_STORAGE);
        int writePermissionCheck = ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (readPermissionCheck !=  PackageManager.PERMISSION_GRANTED ||
                writePermissionCheck !=  PackageManager.PERMISSION_GRANTED) {
            requestPermissions(PERMISSIONS,
                    isLeft ? REQUEST_PERMISSION_LEFT : REQUEST_PERMISSION_RIGHT);
        } else {
            // Permissions granted
            performFileDownload(isLeft);
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSION_LEFT || requestCode == REQUEST_PERMISSION_RIGHT) {
            boolean granted = true;
            for (int permissionGrant : grantResults) {
                if (permissionGrant !=  PackageManager.PERMISSION_GRANTED) {
                    granted = false;
                    break;
                }
            }

            // Now, download the file
            if (granted) {
                performFileDownload(requestCode == REQUEST_PERMISSION_LEFT);
            }
        }
    }

    private void performFileDownload(boolean isLeft) {
        String revision = String.valueOf(mChange.revisions.get(mRevisionId).number);
        String fileHash = FowlerNollVo.fnv1a_64(mFile.getBytes()).toString();
        String base = isLeft ? mBase == null ? "0" : String.valueOf(mBase) : revision;
        String name = base + "_" + fileHash + "_" + CacheHelper.CACHE_CONTENT;
        File src = new File(CacheHelper.getAccountDiffCacheDir(getContext()), name);
        if (src.exists()) {
            name = base + "_" + new File(mFile).getName().toLowerCase(Locale.US);

            // Copy source file to download folder
            File downloadFolder = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS);
            File dst = new File(downloadFolder, name);
            int i = 0;
            while(dst.exists()) {
                i++;
                dst = new File(downloadFolder, "(" + i + ") " + name);
            }
            try {
                FileUtils.copyFile(src, dst);
                final String msg = getString(
                        R.string.notification_file_download_text_success, dst.getName());

                // Scan the file
                MediaScannerConnection.scanFile(
                        getContext(),
                        new String[]{dst.getAbsolutePath()},
                        null,
                        (path, uri) ->
                                mHandler.post(() -> showDownloadNotification(uri, msg)));

            } catch (IOException ex) {
                Log.e(TAG, "Failed to copy " + src + " to " + dst, ex);

                final String msg = getString(
                        R.string.notification_file_download_text_failure, dst.getName());
                Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showDownloadNotification(Uri uri, String msg) {
        final Context ctx = getContext();

        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.setData(uri);
        PendingIntent pi = PendingIntent.getActivity(ctx, 0, i, PendingIntent.FLAG_ONE_SHOT);

        // Create a notification
        Notification notification = new NotificationCompat.Builder(getContext())
                .setContentTitle(getString(R.string.notification_file_download_title))
                .setContentText(msg)
                .setTicker(msg)
                .setSmallIcon(R.drawable.ic_file_download)
                .setAutoCancel(true)
                .setContentIntent(pi)
                .setGroup("rview-downloads")
                .setWhen(System.currentTimeMillis())
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();
        NotificationManagerCompat nm = NotificationManagerCompat.from(ctx);
        nm.notify(notification.hashCode(), notification);

        Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show();
    }

    private void configurePageController(BaseActivity activity, boolean refresh) {
        activity.invalidatePages();
        activity.configurePages(mAdapter, position -> {
            mFile = mFiles.get(position);
            mCurrentFile = position;
        });
        activity.getContentBinding().pagerController.currentPage(mCurrentFile, refresh);
        if (refresh) {
            mAdapter.notifyDataSetChanged();
        }
    }

    @SuppressWarnings("ConstantConditions")
    private Observable<ArrayList<String>> loadFiles(
            final String revisionId, final String baseRevisionId) {
        final Context ctx = getActivity();
        final GerritApi api = ModelHelper.getGerritApi(ctx);

        Type type = new TypeToken<ArrayList<String>>(){}.getType();
        String current = String.valueOf(mChange.revisions.get(mRevisionId).number);
        String prefix = (mBase == null ? "0" : mBase) + "_" + current + "_";

        return withCached(
                    SafeObservable.fromNullCallable(() -> {
                        Map<String, FileInfo> files =
                                api.getChangeRevisionFiles(
                                    String.valueOf(mChange.legacyChangeId),
                                    revisionId, baseRevisionId, null, null).blockingFirst();
                        return new ArrayList<>(files.keySet());
                    }),
                    type,
                    prefix + CacheHelper.CACHE_FILES_JSON
                )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    private void performLoadFiles() {
        // Do not
        boolean fetch = true;
        if (mBase != null) {
            if (mChange.revisions.get(mRevisionId).number == Integer.parseInt(mBase)) {
                fetch = false;
            }
        }

        if (fetch) {
            mFilesLoader.clear();
            mFilesLoader.restart(mRevisionId, mBase);
        } else {
            // Both have the same files, so use the files of the current revision
            // Revision doesn't contains the COMMIT_MESSAGE, so just add as well
            ArrayList<String> files = new ArrayList<>(
                    mChange.revisions.get(mRevisionId).files.keySet());
            files.add(0, Constants.COMMIT_MESSAGE);
            mFilesObserver.onNext(files);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> Observable<T> withCached(Observable<T> call, Type type, String name) {
        try {
            if (CacheHelper.hasAccountDiffCache(getContext(), name)) {
                T o = SerializationManager.getInstance().fromJson(
                        new String(CacheHelper.readAccountDiffCacheFile(
                                getContext(), name)), type);
                return Observable.just((o));
            }
        } catch (IOException ex) {
            Log.e(TAG, "Failed to load diff cached data: " + name, ex);
        } catch (JsonParseException ex) {
            Log.e(TAG, "Failed to parse diff cached data: " + name, ex);
        }
        return call;
    }
}
