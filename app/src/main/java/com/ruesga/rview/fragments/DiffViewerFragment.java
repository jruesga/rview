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

import com.ruesga.rview.BaseActivity;
import com.ruesga.rview.R;
import com.ruesga.rview.adapters.SimpleDropDownAdapter;
import com.ruesga.rview.annotations.ProguardIgnored;
import com.ruesga.rview.databinding.DiffActionsHeaderBinding;
import com.ruesga.rview.databinding.DiffBaseChooserHeaderBinding;
import com.ruesga.rview.databinding.DiffViewerFragmentBinding;
import com.ruesga.rview.drawer.DrawerNavigationView.OnDrawerNavigationItemSelectedListener;
import com.ruesga.rview.fragments.FileDiffViewerFragment.OnDiffCompleteListener;
import com.ruesga.rview.gerrit.model.ChangeInfo;
import com.ruesga.rview.gerrit.model.FileStatus;
import com.ruesga.rview.misc.CacheHelper;
import com.ruesga.rview.misc.FowlerNollVo;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

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
                    mRevisionId, mFile, base, revision, mMode, mWrap,
                    mHighlightTabs, mHighlightTrailingWhitespaces);
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
                    case R.id.highlight_tabs:
                        mHighlightTabs = !mHighlightTabs;
                        Preferences.setAccountHighlightTabs(getContext(), mAccount, mHighlightTabs);
                        break;
                    case R.id.highlight_trailing_whitespaces:
                        mHighlightTrailingWhitespaces = !mHighlightTrailingWhitespaces;
                        Preferences.setAccountHighlightTrailingWhitespaces(
                                getContext(), mAccount, mHighlightTrailingWhitespaces);
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
    private final List<String> mFiles = new ArrayList<>();
    private String mRevisionId;
    private String mFile;
    private String mBase;

    private final List<String> mAllRevisions = new ArrayList<>();

    private int mMode = -1;
    private boolean mWrap;
    private boolean mHighlightTabs;
    private boolean mHighlightTrailingWhitespaces;

    private boolean mIsBinary = false;
    private boolean mHasImagePreview = false;

    private int mCurrentFile;

    private Account mAccount;

    private Intent mResult;

    public static DiffViewerFragment newInstance(String revisionId, String file) {
        DiffViewerFragment fragment = new DiffViewerFragment();
        Bundle arguments = new Bundle();
        arguments.putString(Constants.EXTRA_REVISION_ID, revisionId);
        arguments.putString(Constants.EXTRA_FILE, file);
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
        mFile = state.getString(Constants.EXTRA_FILE_ID);
        mBase = state.getString(Constants.EXTRA_BASE);
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
            loadFiles();
            loadRevisions();

            // Get diff user preferences
            mAccount = Preferences.getAccount(getContext());
            String diffMode = Preferences.getAccountDiffMode(getContext(), mAccount);
            if (mMode == -1) {
                mMode = diffMode.equals(Constants.DIFF_MODE_SIDE_BY_SIDE)
                        ? DiffView.SIDE_BY_SIDE_MODE : DiffView.UNIFIED_MODE;
            }
            mWrap = Preferences.getAccountWrapMode(getContext(), mAccount);
            mHighlightTabs = Preferences.isAccountHighlightTabs(getContext(), mAccount);
            mHighlightTrailingWhitespaces =
                    Preferences.isAccountHighlightTrailingWhitespaces(getContext(), mAccount);

            // Configure the pages adapter
            BaseActivity activity = ((BaseActivity) getActivity());
            activity.configurePages(mAdapter, position -> {
                mFile = mFiles.get(position);
                mCurrentFile = position;
            });
            activity.getContentBinding().pagerController.currentPage(mCurrentFile);



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
        outState.putInt("mode", mMode);
        outState.putBoolean("is_binary", mIsBinary);
        outState.putBoolean("has_image_preview", mHasImagePreview);
        outState.putString(Constants.EXTRA_REVISION_ID, mRevisionId);
        if (mResult != null) {
            outState.putParcelable(Constants.EXTRA_DATA, mResult);
        }
    }

    private void loadFiles() {
        mFiles.clear();
        // Revisions doesn't include the COMMIT_MSG
        mFiles.add(Constants.COMMIT_MESSAGE);
        mCurrentFile = 0;
        int i = 1;
        for (String file : mChange.revisions.get(mRevisionId).files.keySet()) {
            if (file.equals(mFile)) {
                mCurrentFile = i;
            }
            mFiles.add(file);
            i++;
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
        menu.findItem(R.id.highlight).setVisible(mMode != DiffView.IMAGE_MODE);
        menu.findItem(R.id.highlight_tabs).setIcon(mHighlightTabs ? checkMark : uncheckMark);
        menu.findItem(R.id.highlight_trailing_whitespaces).setIcon(
                mHighlightTrailingWhitespaces ? checkMark : uncheckMark);

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
                try {
                    mBase = String.valueOf(Integer.parseInt(revisions.get(position)));
                } catch (NumberFormatException ex) {
                    // 0 based
                    mBase = null;
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
                    mResult.putExtra(Constants.EXTRA_BASE, mRevisionId);
                    getActivity().setResult(Activity.RESULT_OK, mResult);
                }
            }

            // Close the drawer
            ((BaseActivity) getActivity()).closeOptionsDrawer();

            // Refresh the view
            forceRefresh();
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

}
