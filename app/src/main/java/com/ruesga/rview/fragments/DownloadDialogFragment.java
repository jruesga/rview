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
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.net.ConnectivityManagerCompat;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ListPopupWindow;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ruesga.rview.R;
import com.ruesga.rview.adapters.SimpleDropDownAdapter;
import com.ruesga.rview.databinding.DownloadDialogBinding;
import com.ruesga.rview.gerrit.GerritApi;
import com.ruesga.rview.gerrit.model.ChangeInfo;
import com.ruesga.rview.gerrit.model.DownloadFormat;
import com.ruesga.rview.gerrit.model.FetchInfo;
import com.ruesga.rview.gerrit.model.PatchFileFormat;
import com.ruesga.rview.misc.ActivityHelper;
import com.ruesga.rview.misc.ModelHelper;
import com.ruesga.rview.misc.SerializationManager;
import com.ruesga.rview.preferences.Constants;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import me.tatarka.rxloader2.RxLoader1;
import me.tatarka.rxloader2.RxLoaderManager;
import me.tatarka.rxloader2.RxLoaderManagerCompat;
import me.tatarka.rxloader2.RxLoaderObserver;
import me.tatarka.rxloader2.safe.SafeObservable;

public class DownloadDialogFragment extends RevealDialogFragment {

    public static final String TAG = "DownloadDialogFragment";

    private static final int REQUEST_DOWNLOAD_PATCH_FILE_BASE64_PERMISSION = 101;
    private static final int REQUEST_DOWNLOAD_PATCH_FILE_ZIP_PERMISSION = 102;
    private static final int REQUEST_DOWNLOAD_ARCHIVE_TGZ_PERMISSION = 110;
    private static final int REQUEST_DOWNLOAD_ARCHIVE_TAR_PERMISSION = 111;
    private static final int REQUEST_DOWNLOAD_ARCHIVE_TBZ2_PERMISSION = 112;
    private static final int REQUEST_DOWNLOAD_ARCHIVE_TXZ_PERMISSION = 113;
    private static final String[] STORAGE_PERMISSIONS  = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE"
    };

    private static class Request {
        private int mStatus;
        private int mRequestCode;
    }

    private static class Download {
        private Uri mUri;
        private String mFileName;
    }

    private final RxLoaderObserver<Request> mRequestDownloadObserver
            = new RxLoaderObserver<Request>() {
        @Override
        public void onNext(Request request) {
            switch (request.mStatus) {
                case 0: // Ready
                    requestPermissionsOrDownload(request.mRequestCode);
                    break;
                case 1: // No network available
                case 2: // Metered network
                    askDownload(request);
                    break;
            }
        }
    };

    private final RxLoaderObserver<Download> mDownloadObserver = new RxLoaderObserver<Download>() {
        @Override
        public void onNext(Download download) {
            mDownloadLoader.clear();
            download(download);
        }
    };

    @Keep
    public static class Model {
        public String downloadType;
        public Boolean hasDownloadType;
    }

    public static DownloadDialogFragment newInstance(ChangeInfo change, String revisionId) {
        DownloadDialogFragment fragment = new DownloadDialogFragment();
        Bundle arguments = new Bundle();
        Gson gson = SerializationManager.getInstance();
        arguments.putInt(Constants.EXTRA_LEGACY_CHANGE_ID, change.legacyChangeId);
        arguments.putString(Constants.EXTRA_REVISION_ID, revisionId);
        String downloadCommands = gson.toJson(change.revisions.get(revisionId).fetch);
        arguments.putString(Constants.EXTRA_DATA, downloadCommands);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Keep
    @SuppressWarnings({"UnusedParameters", "unused"})
    public static class EventHandlers {
        private final DownloadDialogFragment mFragment;

        public EventHandlers(DownloadDialogFragment fragment) {
            mFragment = fragment;
        }

        public void onDownloadActionPressed(View v) {
            switch (v.getId()) {
                case R.id.patch_file_base64:
                    mFragment.performDownloadPatchFile(PatchFileFormat.BASE64);
                    break;
                case R.id.patch_file_zip:
                    mFragment.performDownloadPatchFile(PatchFileFormat.ZIP);
                    break;

                case R.id.archive_tgz:
                    mFragment.performDownloadArchive(DownloadFormat.TGZ);
                    break;
                case R.id.archive_tar:
                    mFragment.performDownloadArchive(DownloadFormat.TAR);
                    break;
                case R.id.archive_tbz2:
                    mFragment.performDownloadArchive(DownloadFormat.TBZ2);
                    break;
                case R.id.archive_txz:
                    mFragment.performDownloadArchive(DownloadFormat.TXZ);
                    break;
            }
        }

        public void onDownloadTypeChooserPressed(View v) {
            mFragment.showDownloadTypeChooser(v);

        }

        public void onCommandCopyPressed(View v) {
            String s = ((String) v.getTag());
            int pos = s.indexOf("|");
            String label = s.substring(0, pos);
            String command = s.substring(pos + 1);
            mFragment.performCopyCommand(label, command);
        }
    }


    private RxLoader1<Integer, Request> mRequestDownloadLoader;
    private RxLoader1<Integer, Download> mDownloadLoader;

    private int mLegacyChangeId;
    private String mRevisionId;
    private Map<String, FetchInfo> mDownloadCommands;
    private List<String> mDownloadTypes = new ArrayList<>();

    private DownloadDialogBinding mBinding;
    private Model mModel = new Model();
    private EventHandlers mHandlers;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandlers = new EventHandlers(this);
        Gson gson = SerializationManager.getInstance();
        mLegacyChangeId = getArguments().getInt(Constants.EXTRA_LEGACY_CHANGE_ID);
        mRevisionId = getArguments().getString(Constants.EXTRA_REVISION_ID);
        Type type = new TypeToken<Map<String, FetchInfo>>() {}.getType();
        mDownloadCommands = gson.fromJson(getArguments().getString(Constants.EXTRA_DATA), type);
        mDownloadTypes.addAll(mDownloadCommands.keySet());
        Collections.sort(mDownloadTypes);

        if (savedInstanceState != null) {
            mModel.downloadType = savedInstanceState.getString("download_type");
        } else {
            mModel.downloadType = mDownloadTypes.get(0);
        }
        mModel.hasDownloadType = mDownloadTypes.size() > 1;
    }

    @Override
    public final void onDestroyView() {
        super.onDestroyView();
        mBinding.unbind();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        RxLoaderManager loaderManager = RxLoaderManagerCompat.get(this);
        mRequestDownloadLoader = loaderManager.create(
                "request_download", this::doRequestDownload, mRequestDownloadObserver);
        mDownloadLoader = loaderManager.create("download", this::doDownload, mDownloadObserver);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("download_type", mModel.downloadType);
    }

    @Override
    public void buildDialog(AlertDialog.Builder builder, Bundle savedInstanceState) {
        LayoutInflater inflater = LayoutInflater.from(builder.getContext());
        mBinding = DataBindingUtil.inflate(inflater, R.layout.download_dialog, null, true);
        mBinding.downloadCommands
                .from(mDownloadCommands.get(mModel.downloadType))
                .with(mHandlers)
                .update();
        mBinding.setModel(mModel);
        mBinding.setHandlers(mHandlers);

        builder.setTitle(R.string.download_commands_dialog_title)
                .setView(mBinding.getRoot())
                .setPositiveButton(R.string.action_close, null);
    }

    public void performDownloadPatchFile(PatchFileFormat format) {
        if (format.equals(PatchFileFormat.BASE64)) {
            performRequestDownload(REQUEST_DOWNLOAD_PATCH_FILE_BASE64_PERMISSION);
        } else if (format.equals(PatchFileFormat.ZIP)) {
            performRequestDownload(REQUEST_DOWNLOAD_PATCH_FILE_ZIP_PERMISSION);
        }
    }

    public void performDownloadArchive(DownloadFormat format) {
        if (format.equals(DownloadFormat.TGZ)) {
            performRequestDownload(REQUEST_DOWNLOAD_ARCHIVE_TGZ_PERMISSION);
        } else if (format.equals(DownloadFormat.TAR)) {
            performRequestDownload(REQUEST_DOWNLOAD_ARCHIVE_TAR_PERMISSION);
        } else if (format.equals(DownloadFormat.TBZ2)) {
            performRequestDownload(REQUEST_DOWNLOAD_ARCHIVE_TBZ2_PERMISSION);
        } else if (format.equals(DownloadFormat.TXZ)) {
            performRequestDownload(REQUEST_DOWNLOAD_ARCHIVE_TXZ_PERMISSION);
        }
    }

    @SuppressWarnings("ConstantConditions")
    private Observable<Request> doRequestDownload(Integer requestCode) {
        return SafeObservable.fromNullCallable(() -> {
                if (getActivity() != null) {
                    Request request = new Request();
                    request.mRequestCode = requestCode;
                    ConnectivityManager cm =  (ConnectivityManager) getActivity()
                            .getSystemService(Activity.CONNECTIVITY_SERVICE);
                    NetworkInfo ni = cm.getActiveNetworkInfo();
                    if (ni == null) {
                        request.mStatus = 1;
                        return request;
                    }
                    if (ConnectivityManagerCompat.isActiveNetworkMetered(cm) || ni.isRoaming()) {
                        request.mStatus = 2;
                        return request;
                    }
                    return request;
                }
                throw new IllegalStateException();
            })
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread());
    }

    @SuppressWarnings("ConstantConditions")
    private Observable<Download> doDownload(Integer requestCode) {
        return SafeObservable.fromNullCallable(() -> createDownload(requestCode))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    private void askDownload(Request request) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext())
                .setTitle(R.string.download_metered_network_title);
        if (request.mStatus == 1) {
            // No network available
            builder.setMessage(R.string.empty_states_not_connectivity);
            builder.setPositiveButton(R.string.action_close, (dialog, which) -> dialog.dismiss());
        } else {
            // Metered or Roaming connection
            builder.setMessage(R.string.download_metered_network_confirm);
            builder.setPositiveButton(R.string.action_continue, (dialog, which) -> {
                requestPermissionsOrDownload(request.mRequestCode);
                dialog.dismiss();
            });
            builder.setNegativeButton(R.string.action_cancel, null);
        }

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void requestPermissionsOrDownload(int requestCode) {
        int readPermissionCheck = ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.READ_EXTERNAL_STORAGE);
        int writePermissionCheck = ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (readPermissionCheck !=  PackageManager.PERMISSION_GRANTED ||
                writePermissionCheck !=  PackageManager.PERMISSION_GRANTED) {
            requestPermissions(STORAGE_PERMISSIONS, requestCode);
        } else {
            // Permissions granted
            performDownload(requestCode);
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_DOWNLOAD_PATCH_FILE_BASE64_PERMISSION
                || requestCode == REQUEST_DOWNLOAD_PATCH_FILE_ZIP_PERMISSION
                || requestCode == REQUEST_DOWNLOAD_ARCHIVE_TGZ_PERMISSION
                || requestCode == REQUEST_DOWNLOAD_ARCHIVE_TAR_PERMISSION
                || requestCode == REQUEST_DOWNLOAD_ARCHIVE_TBZ2_PERMISSION
                || requestCode == REQUEST_DOWNLOAD_ARCHIVE_TXZ_PERMISSION) {
            boolean granted = true;
            for (int permissionGrant : grantResults) {
                if (permissionGrant !=  PackageManager.PERMISSION_GRANTED) {
                    granted = false;
                    break;
                }
            }

            // Now, download the file
            if (granted) {
                performDownload(requestCode);
            }
        }
    }

    private void performRequestDownload(int requestCode) {
        mRequestDownloadLoader.clear();
        mRequestDownloadLoader.restart(requestCode);
    }

    private void performDownload(int requestCode) {
        mDownloadLoader.clear();
        mDownloadLoader.restart(requestCode);
    }

    @SuppressWarnings("ConstantConditions")
    private Download createDownload(int requestCode) {
        final Context ctx = getActivity();
        final GerritApi api = ModelHelper.getGerritApi(ctx);
        Download download = new Download();
        switch (requestCode) {
            case REQUEST_DOWNLOAD_PATCH_FILE_BASE64_PERMISSION:
                download.mUri = api.getPatchFileRevisionUri(
                        String.valueOf(mLegacyChangeId), mRevisionId, PatchFileFormat.BASE64);
                download.mFileName = mRevisionId + "." + PatchFileFormat.BASE64.mExtension;
                return download;
            case REQUEST_DOWNLOAD_PATCH_FILE_ZIP_PERMISSION:
                download.mUri = api.getPatchFileRevisionUri(
                        String.valueOf(mLegacyChangeId), mRevisionId, PatchFileFormat.ZIP);
                download.mFileName = mRevisionId + "." + PatchFileFormat.ZIP.mExtension;
                return download;
            case REQUEST_DOWNLOAD_ARCHIVE_TGZ_PERMISSION:
                download.mUri = api.getDownloadRevisionUri(
                        String.valueOf(mLegacyChangeId), mRevisionId, DownloadFormat.TGZ);
                download.mFileName = mRevisionId + "." + DownloadFormat.TGZ.mExtension;
                return download;
            case REQUEST_DOWNLOAD_ARCHIVE_TAR_PERMISSION:
                download.mUri = api.getDownloadRevisionUri(
                        String.valueOf(mLegacyChangeId), mRevisionId, DownloadFormat.TAR);
                download.mFileName = mRevisionId + "." + DownloadFormat.TAR.mExtension;
                return download;
            case REQUEST_DOWNLOAD_ARCHIVE_TBZ2_PERMISSION:
                download.mUri = api.getDownloadRevisionUri(
                        String.valueOf(mLegacyChangeId), mRevisionId, DownloadFormat.TBZ2);
                download.mFileName = mRevisionId + "." + DownloadFormat.TBZ2.mExtension;
                return download;
            case REQUEST_DOWNLOAD_ARCHIVE_TXZ_PERMISSION:
                download.mUri = api.getDownloadRevisionUri(
                        String.valueOf(mLegacyChangeId), mRevisionId, DownloadFormat.TXZ);
                download.mFileName = mRevisionId + "." + DownloadFormat.TXZ.mExtension;
                return download;
        }
        throw new IllegalArgumentException("Invalid request code: " + requestCode);
    }

    private void download(Download download) {
        // Check we still in a valid context
        if (getActivity() == null) {
            return;
        }

        // Do not pass mimetype. Let the download manager resolve it
        ActivityHelper.downloadUri(getContext(), download.mUri, download.mFileName, null);

        // Close the dialog
        dismiss();
    }

    private void showDownloadTypeChooser(View anchor) {
        final ListPopupWindow popupWindow = new ListPopupWindow(getContext());
        SimpleDropDownAdapter<Integer> adapter = new SimpleDropDownAdapter<>(
                getContext(), mDownloadTypes, mModel.downloadType);
        popupWindow.setAnchorView(anchor);
        popupWindow.setAdapter(adapter);
        popupWindow.setContentWidth(adapter.measureContentWidth());
        popupWindow.setOnItemClickListener((parent, view, position, id) -> {
            popupWindow.dismiss();

            // Update the view
            mModel.downloadType = mDownloadTypes.get(position);
            mBinding.downloadCommands
                    .from(mDownloadCommands.get(mModel.downloadType))
                    .update();
            mBinding.setModel(mModel);
            mBinding.executePendingBindings();
        });
        popupWindow.setModal(true);
        popupWindow.show();
    }

    private void performCopyCommand(String label, String command) {
        // Check we still in a valid context
        if (getActivity() == null) {
            return;
        }

        // Copy to clipboard
        ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(
                Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(label, command);
        clipboard.setPrimaryClip(clip);

        // Show a confirmation message
        final String msg = getString(R.string.download_commands_dialog_copy_message, label);
        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
    }
}
