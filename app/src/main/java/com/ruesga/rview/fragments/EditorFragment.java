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

import android.app.Activity;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ListPopupWindow;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;

import com.google.gson.JsonParseException;
import com.ruesga.rview.BaseActivity;
import com.ruesga.rview.R;
import com.ruesga.rview.adapters.SimpleDropDownAdapter;
import com.ruesga.rview.annotations.ProguardIgnored;
import com.ruesga.rview.databinding.EditActionsHeaderBinding;
import com.ruesga.rview.databinding.EditFileChooserHeaderBinding;
import com.ruesga.rview.databinding.EditorFragmentBinding;
import com.ruesga.rview.drawer.DrawerNavigationView.OnDrawerNavigationItemSelectedListener;
import com.ruesga.rview.gerrit.GerritApi;
import com.ruesga.rview.gerrit.model.FileInfo;
import com.ruesga.rview.misc.AndroidHelper;
import com.ruesga.rview.misc.CacheHelper;
import com.ruesga.rview.misc.FowlerNollVo;
import com.ruesga.rview.misc.ModelHelper;
import com.ruesga.rview.misc.StringHelper;
import com.ruesga.rview.model.Account;
import com.ruesga.rview.preferences.Constants;
import com.ruesga.rview.preferences.Preferences;
import com.ruesga.rview.widget.EditorView;
import com.ruesga.rview.widget.EditorView.OnContentChangedListener;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import me.tatarka.rxloader2.RxLoader;
import me.tatarka.rxloader2.RxLoaderManager;
import me.tatarka.rxloader2.RxLoaderManagerCompat;
import me.tatarka.rxloader2.RxLoaderObserver;
import me.tatarka.rxloader2.safe.SafeObservable;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;

public class EditorFragment extends Fragment implements KeyEventBindable {

    private static final String TAG = "EditorFragment";

    private static final int READ_CONTENT_MESSAGE = 0;

    private interface OnSavedContentReady {
        void onContentSaved();
    }

    @ProguardIgnored
    public static class Model {
        public String file;
    }

    @ProguardIgnored
    @SuppressWarnings("unused")
    public static class EventHandlers {
        private final EditorFragment mFragment;

        public EventHandlers(EditorFragment fragment) {
            mFragment = fragment;
        }

        public void onFileChooserPressed(View v) {
            mFragment.performFileChooser(v);
        }

        public void onActionPressed(View v) {
            mFragment.closeDrawer();

            String action = (String)v.getTag();
            switch (action) {
                case "restore":
                    mFragment.performRestore();
                    break;
                case "discard":
                    mFragment.performCancelEdit();
                    break;
                case "publish":
                    mFragment.performPublishEdit();
                    break;
            }
        }
    }

    private OnDrawerNavigationItemSelectedListener mOptionsItemListener = new OnDrawerNavigationItemSelectedListener() {
        @Override
        public boolean onDrawerNavigationItemSelected(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.wrap_mode_on:
                    mWrap = true;
                    Preferences.setAccountWrapMode(getContext(), mAccount, mWrap);
                    mBinding.editor.setWrap(mWrap);
                    break;
                case R.id.wrap_mode_off:
                    mWrap = false;
                    Preferences.setAccountWrapMode(getContext(), mAccount, mWrap);
                    mBinding.editor.setWrap(mWrap);
                    break;
                case R.id.text_size_smaller:
                    mTextSizeFactor = Constants.DEFAULT_TEXT_SIZE_SMALLER;
                    Preferences.setAccountTextSizeFactor(
                            getContext(), mAccount, mTextSizeFactor);
                    mBinding.editor.setTextSize(12);
                    break;
                case R.id.text_size_normal:
                    mTextSizeFactor = Constants.DEFAULT_TEXT_SIZE_NORMAL;
                    Preferences.setAccountTextSizeFactor(
                            getContext(), mAccount, mTextSizeFactor);
                    mBinding.editor.setTextSize(14);
                    break;
                case R.id.text_size_bigger:
                    mTextSizeFactor = Constants.DEFAULT_TEXT_SIZE_BIGGER;
                    Preferences.setAccountTextSizeFactor(
                            getContext(), mAccount, mTextSizeFactor);
                    mBinding.editor.setTextSize(16);
                    break;
            }

            // Close the drawer
            ((BaseActivity) getActivity()).closeOptionsDrawer();
            return true;
        }
    };

    private final RxLoaderObserver<Map<String, FileInfo>> mFilesObserver
            = new RxLoaderObserver<Map<String, FileInfo>>() {
        @Override
        @SuppressWarnings("Convert2streamapi")
        public void onNext(Map<String, FileInfo> files) {
            // Update files
            mFile = null;
            mFiles.clear();
            for (String file : files.keySet()) {
                if (!files.get(file).binary) {
                    mFiles.add(file);
                }
            }
            createFileHashes();

            // At least commit message should be present
            mFile = mFiles.get(0);
            mCurrentFile = 0;

            updateModel();
            requestFileContent();

            showProgress(false);
        }

        @Override
        public void onError(Throwable error) {
            ((BaseActivity) getActivity()).handleException(TAG, error, null);
            showProgress(false);
        }

        @Override
        public void onStarted() {
            showProgress(true);
        }
    };

    private final RxLoaderObserver<Boolean> mPublishObserver = new RxLoaderObserver<Boolean>() {
        @Override
        @SuppressWarnings("Convert2streamapi")
        public void onNext(Boolean result) {
            getActivity().setResult(Activity.RESULT_OK);
            getActivity().finish();
            showProgress(false);

            mPublishLoader.clear();
        }

        @Override
        public void onError(Throwable error) {
            ((BaseActivity) getActivity()).handleException(TAG, error, null);
            showProgress(false);

            // Try to cancel all published changes
            requestCancelEdit();

            mPublishLoader.clear();
        }

        @Override
        public void onStarted() {
            showProgress(true);
        }
    };

    private final RxLoaderObserver<Boolean> mCancelObserver = new RxLoaderObserver<Boolean>() {
        @Override
        @SuppressWarnings("Convert2streamapi")
        public void onNext(Boolean result) {
            mCancelLoader.clear();
        }
    };

    private final RxLoaderObserver<byte[]> mContentObserver = new RxLoaderObserver<byte[]>() {
        @Override
        public void onNext(byte[] content) {
            byte[] data;
            if (content == null || content.length == 0) {
                data = new byte[]{};
            } else {
                data = content;
            }

            try {
                CacheHelper.writeAccountDiffCacheFile(
                        getContext(), getContentCachedFileName(mFile), data);
            } catch (IOException ex) {
                Log.w(TAG, "Failed to store content for " + mFile);
            }

            // The response is base64 encoded
            mBinding.editor.scrollTo(0, 0);
            mBinding.editor.loadEncodedContent(mFile, data);

            showProgress(false);

            mContentLoader.clear();
        }

        @Override
        public void onError(Throwable error) {
            ((BaseActivity) getActivity()).handleException(TAG, error, null);
            showProgress(false);

            mContentLoader.clear();
        }

        @Override
        public void onStarted() {
            showProgress(true);
        }
    };

    private OnContentChangedListener mContentChangedListener = new OnContentChangedListener() {
        @Override
        public void onContentChanged() {
            mIsDirty = true;
            mUiHandler.removeMessages(READ_CONTENT_MESSAGE);
            mUiHandler.sendEmptyMessageDelayed(READ_CONTENT_MESSAGE, 500L);
        }
    };

    private Handler.Callback mOnChangeCallback = msg -> {
        if (msg.what == READ_CONTENT_MESSAGE) {
            readFileContent(null);
        }
        return false;
    };

    private EditorFragmentBinding mBinding;
    private EditFileChooserHeaderBinding mFileChooserBinding;
    private EditActionsHeaderBinding mEditActionsBinding;

    private final Model mModel = new Model();
    private EventHandlers mEventHandlers;

    private int mLegacyChangeId;
    private String mChangeId;

    private ArrayList<String> mFiles = new ArrayList<>();
    private ArrayList<String> mFilesHashes = new ArrayList<>();
    private String mFile;
    private int mCurrentFile;
    private boolean mIsDirty;

    private RxLoader<byte[]> mContentLoader;
    private RxLoader<Boolean> mCancelLoader;
    private RxLoader<Boolean> mPublishLoader;

    private Account mAccount;
    private boolean mWrap;
    private float mTextSizeFactor;

    private Handler mUiHandler;

    public static EditorFragment newInstance(int legacyChangeId, String changeId) {
        EditorFragment fragment = new EditorFragment();
        Bundle arguments = new Bundle();
        arguments.putInt(Constants.EXTRA_LEGACY_CHANGE_ID, legacyChangeId);
        arguments.putString(Constants.EXTRA_CHANGE_ID, changeId);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mEventHandlers = new EventHandlers(this);
        mUiHandler = new Handler(mOnChangeCallback);

        Bundle state = (savedInstanceState != null) ? savedInstanceState : getArguments();
        mLegacyChangeId = state.getInt(Constants.EXTRA_LEGACY_CHANGE_ID);
        mChangeId = state.getString(Constants.EXTRA_CHANGE_ID);

        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(
                inflater, R.layout.editor_fragment, container, false);
        mBinding.editor.listenOn(mContentChangedListener);
        return mBinding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        startLoadersWithValidContext(savedInstanceState);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (mBinding != null) {
            mBinding.unbind();
        }
        if (mFileChooserBinding != null) {
            mFileChooserBinding.unbind();
        }
        if (mEditActionsBinding != null) {
            mEditActionsBinding.unbind();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(Constants.EXTRA_LEGACY_CHANGE_ID, mLegacyChangeId);
        outState.putString(Constants.EXTRA_CHANGE_ID, mChangeId);

        outState.putStringArrayList("files", mFiles);
        outState.putString(Constants.EXTRA_FILE, mFile);
        outState.putInt("current_file", mCurrentFile);
        outState.putBoolean("is_dirty", mIsDirty);
    }

    private void startLoadersWithValidContext(@Nullable Bundle savedInstanceState) {
        // Configure the diff_options menu
        BaseActivity activity = ((BaseActivity) getActivity());
        activity.configureOptionsTitle(getString(R.string.menu_edit_options));
        activity.configureOptionsMenu(R.menu.edit_options_menu, mOptionsItemListener);

        mAccount = Preferences.getAccount(getContext());
        mWrap = Preferences.getAccountWrapMode(getContext(), mAccount);
        mTextSizeFactor = Preferences.getAccountTextSizeFactor(getContext(), mAccount);

        mFileChooserBinding = DataBindingUtil.inflate(LayoutInflater.from(getContext()),
                R.layout.edit_file_chooser_header, activity.getOptionsMenu(), false);
        mFileChooserBinding.setHandlers(mEventHandlers);
        activity.getOptionsMenu().addHeaderView(mFileChooserBinding.getRoot());

        mEditActionsBinding = DataBindingUtil.inflate(LayoutInflater.from(getContext()),
            R.layout.edit_actions_header, activity.getOptionsMenu(), false);
        mEditActionsBinding.setHandlers(mEventHandlers);
        activity.getOptionsMenu().addHeaderView(mEditActionsBinding.getRoot());

        // Load the files
        RxLoaderManager loaderManager = RxLoaderManagerCompat.get(this);
        mContentLoader = loaderManager.create("content", fetchContent(), mContentObserver);
        mPublishLoader = loaderManager.create("publish", publishEdit(), mPublishObserver);
        if (savedInstanceState != null) {
            mFile = savedInstanceState.getString(Constants.EXTRA_FILE);
            mFiles = savedInstanceState.getStringArrayList("files");
            mCurrentFile = savedInstanceState.getInt("current_file");
            mIsDirty = savedInstanceState.getBoolean("is_dirty");
            createFileHashes();

            updateModel();
            requestFileContent();
        } else {
            // Cancel any previous edit and request files
            mCancelLoader = loaderManager.create("cancel", cancelEdit(), mCancelObserver).start();
            loaderManager.create("files", fetchFiles(), mFilesObserver).start();
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
                AndroidHelper.hideSoftKeyboard(getContext(), getActivity().getWindow());
                break;
        }
        return false;
    }

    private void openOptionsMenu() {
        // Update diff_options
        BaseActivity activity =  ((BaseActivity) getActivity());
        Menu menu = activity.getOptionsMenu().getMenu();
        menu.findItem(R.id.wrap_mode_on).setChecked(mWrap);
        menu.findItem(R.id.wrap_mode_off).setChecked(!mWrap);
        menu.findItem(R.id.text_size_smaller).setChecked(
                mTextSizeFactor == Constants.DEFAULT_TEXT_SIZE_SMALLER);
        menu.findItem(R.id.text_size_normal).setChecked(
                mTextSizeFactor == Constants.DEFAULT_TEXT_SIZE_NORMAL);
        menu.findItem(R.id.text_size_bigger).setChecked(
                mTextSizeFactor == Constants.DEFAULT_TEXT_SIZE_BIGGER);

        mEditActionsBinding.setIsDirty(mBinding.editor.isDirty());
        mEditActionsBinding.setCanPublish(mIsDirty);

        // Open drawer
        activity.openOptionsDrawer();
    }

    private void showProgress(boolean show) {
        BaseActivity activity = (BaseActivity) getActivity();
        if (show) {
            activity.onRefreshStart(this);
        } else {
            activity.onRefreshEnd(this, null);
        }
    }

    @SuppressWarnings("ConstantConditions")
    private Observable<Map<String, FileInfo>> fetchFiles() {
        final GerritApi api = ModelHelper.getGerritApi(getContext());
        return SafeObservable.fromNullCallable(() ->
                api.getChangeRevisionFiles(
                        String.valueOf(mLegacyChangeId), GerritApi.CURRENT_REVISION, null, null, null)
                        .blockingFirst())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    @SuppressWarnings("ConstantConditions")
    private Observable<byte[]> fetchContent() {
        final GerritApi api = ModelHelper.getGerritApi(getContext());
        return SafeObservable.fromNullCallable(() ->
                withCached(SafeObservable.fromNullCallable(() -> {
                        ResponseBody body =
                            api.getChangeRevisionFileContent(String.valueOf(mLegacyChangeId),
                                    GerritApi.CURRENT_REVISION, mFile).blockingFirst();
                        return body.bytes();
                })).blockingFirst())
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread());
    }

    @SuppressWarnings("ConstantConditions")
    private Observable<Boolean> cancelEdit() {
        final GerritApi api = ModelHelper.getGerritApi(getContext());
        return SafeObservable.fromNullCallable(() -> {
                    api.deleteChangeEdit(String.valueOf(mLegacyChangeId)).blockingFirst();
                    return true;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    private Observable<Boolean> publishEdit() {
        return SafeObservable.fromNullCallable(() -> {
                    publishEditChanges();
                    return true;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    @SuppressWarnings("Convert2streamapi")
    private void performFileChooser(View v) {
        final List<String> files = new ArrayList<>();
        for (String file : mFiles) {
            files.add(new File(file).getName());
        }

        final ListPopupWindow popupWindow = new ListPopupWindow(getContext());
        SimpleDropDownAdapter adapter = new SimpleDropDownAdapter(
                getContext(), files, new File(mFile).getName());
        popupWindow.setAnchorView(v);
        popupWindow.setAdapter(adapter);
        popupWindow.setContentWidth(adapter.measureContentWidth());
        popupWindow.setOnItemClickListener((parent, view, position, id) -> {
            popupWindow.dismiss();

            // Close the drawer
            BaseActivity activity = (BaseActivity) getActivity();
            activity.closeOptionsDrawer();

            // Change to the new file
            if (position != mCurrentFile) {
                if (mBinding.editor.isDirty()) {
                    readFileContent(null);
                }

                mFile = mFiles.get(position);
                mCurrentFile = position;
                updateModel();

                requestFileContent();
            }
        });
        popupWindow.setModal(true);
        popupWindow.show();
    }

    @SuppressWarnings("ConstantConditions")
    private void updateModel() {
        mModel.file = mFile == null ? null : new File(mFile).getName();
        if (getActivity() != null &&
                ((AppCompatActivity) getActivity()).getSupportActionBar() != null) {
            ((AppCompatActivity) getActivity()).getSupportActionBar().setSubtitle(mModel.file);
        }

        mFileChooserBinding.setModel(mModel);
    }

    private void requestFileContent() {
        mContentLoader.clear();
        mContentLoader.restart();
    }

    private void requestCancelEdit() {
        mCancelLoader.clear();
        mCancelLoader.restart();
    }

    private void requestPublishEdit() {
        mPublishLoader.clear();
        mPublishLoader.restart();
    }

    private void readFileContent(final OnSavedContentReady cb) {
        final String file = mFile;
        mBinding.editor.readContent(
            new EditorView.OnReadContentReadyListener() {
                @Override
                public void onReadContentReady(byte[] content) {
                    if (content.length > 0) {
                        String name = getEditCachedFileName(file);
                        Log.i(TAG, new String(Base64.decode(content, Base64.NO_WRAP)));
                        try {
                            CacheHelper.writeAccountDiffCacheFile(getContext(), name, content);
                        } catch (IOException ex) {
                            Log.w(TAG, "Failed to store edit for " + file);
                        }
                    }

                    if (cb != null) {
                        cb.onContentSaved();
                    }
                }

                @Override
                public void onContentUnchanged() {
                    if (cb != null) {
                        cb.onContentSaved();
                    }
                }
            });
    }

    @SuppressWarnings("unchecked")
    private Observable<byte[]> withCached(Observable<byte[]> call) {
        final String editName = getEditCachedFileName(mFile);
        final String contentName = getContentCachedFileName(mFile);
        try {
            // Has edit?
            if (CacheHelper.hasAccountDiffCache(getContext(), editName)) {
                byte[] o = CacheHelper.readAccountDiffCacheFile(getContext(), editName);
                if (o != null) {
                    return Observable.just(o);
                }
            }

            // Has content?
            if (CacheHelper.hasAccountDiffCache(getContext(), contentName)) {
                byte[] o = CacheHelper.readAccountDiffCacheFile(getContext(), contentName);
                if (o != null) {
                    return Observable.just(o);
                }
            }
        } catch (IOException ex) {
            Log.e(TAG, "Failed to load cached data: " + mFile, ex);
        } catch (JsonParseException ex) {
            Log.e(TAG, "Failed to parse cached data: " + mFile, ex);
        }
        return call;
    }

    @Override
    public boolean onKeyDown(int keycode, KeyEvent e) {
        switch(keycode) {
            case KeyEvent.KEYCODE_MENU:
                openOptionsMenu();
                return true;

            case KeyEvent.KEYCODE_BACK:
                if (mBinding.editor.isDirty()) {
                    readFileContent(this::performCancelEdit);
                    return true;
                } else if (mIsDirty) {
                    performCancelEdit();
                    return true;
                }
        }
        return false;
    }

    private void performRestore() {
        AlertDialog dialog = new AlertDialog.Builder(getContext())
            .setTitle(R.string.change_edit_restore_file_title)
            .setMessage(R.string.change_edit_restore_file_message)
            .setPositiveButton(R.string.action_restore, (dialogInterface, i) -> {
                CacheHelper.removeAccountDiffCacheFile(
                        getContext(), getEditCachedFileName(mFile));
                requestFileContent();
            })
            .setNegativeButton(R.string.action_cancel, null)
            .create();
        dialog.show();
    }

    private void performCancelEdit() {
        AlertDialog dialog = new AlertDialog.Builder(getContext())
            .setTitle(R.string.change_edit_discard_title)
            .setMessage(R.string.change_edit_discard_message)
            .setPositiveButton(R.string.action_discard, (dialogInterface, i) -> {
                getActivity().setResult(Activity.RESULT_CANCELED);
                getActivity().finish();
            })
            .setNegativeButton(R.string.action_cancel, null)
            .create();
        dialog.show();
    }

    private void performPublishEdit() {
        readFileContent(() -> {
            AlertDialog dialog = new AlertDialog.Builder(getContext())
                    .setTitle(R.string.change_edit_publish_title)
                    .setMessage(R.string.change_edit_publish_message)
                    .setPositiveButton(R.string.action_publish,
                            (dialogInterface, i) -> requestPublishEdit())
                    .setNegativeButton(R.string.action_cancel, null)
                    .create();
            dialog.show();
        });
    }

    private void createFileHashes() {
        mFilesHashes.clear();
        for (String file : mFiles) {
            mFilesHashes.add(FowlerNollVo.fnv1a_64(file.getBytes()).toString());
        }
    }

    private String getContentCachedFileName(String file) {
        return FowlerNollVo.fnv1a_64(file.getBytes()).toString() + ".content";
    }

    private String getEditCachedFileName(String file) {
        return FowlerNollVo.fnv1a_64(file.getBytes()).toString() + ".edit";
    }

    private void closeDrawer() {
        ((BaseActivity) getActivity()).closeOptionsDrawer();
    }

    @SuppressWarnings("ConstantConditions")
    private void publishEditChanges() throws IOException {
        final GerritApi api = ModelHelper.getGerritApi(getContext());
        File dir = CacheHelper.getAccountDiffCacheDir(getContext());
        File[] edits = dir.listFiles((dir1, name) -> {
            return name.endsWith(".edit");
        });

        // Send every edit to the server
        for (File edit : edits) {
            String hash = edit.getName().substring(0, edit.getName().length() - 5);
            String file = mFiles.get(mFilesHashes.indexOf(hash));

            // Extract the mime/type of the file
            String ext = StringHelper.getFileExtension(new File(file));
            MediaType mediaType = null;
            if (ext != null) {
                String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
                if (mimeType != null) {
                    mediaType = MediaType.parse(mimeType);
                }
            }
            if (mediaType == null) {
                mediaType = MediaType.parse("application/octet-stream");
            }

            // Send the request
            RequestBody body = RequestBody.create(mediaType, readEditContent(file));
            api.setChangeEdit(String.valueOf(mLegacyChangeId), file, body).blockingFirst();
        }

        // And now publish the edit
        api.publishChangeEdit(String.valueOf(mLegacyChangeId)).blockingFirst();
    }

    private byte[] readEditContent(String file) throws IOException {
        byte[] o = CacheHelper.readAccountDiffCacheFile(
                getContext(), getEditCachedFileName(file));
        if (o == null || o.length == 0) {
            return new byte[0];
        }
        return Base64.decode(o, Base64.NO_WRAP);
    }
}
