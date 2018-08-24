/*
 * Copyright (C) 2017 Jorge Ruesga
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
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.ruesga.rview.R;
import com.ruesga.rview.aceeditor.AceEditorView;
import com.ruesga.rview.aceeditor.AceEditorView.OnContentChangedListener;
import com.ruesga.rview.databinding.SnippetContentBinding;
import com.ruesga.rview.misc.ActivityHelper;
import com.ruesga.rview.misc.AndroidHelper;
import com.ruesga.rview.misc.CacheHelper;
import com.ruesga.rview.misc.ViewHelper;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.FileProvider;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;

public class SnippetFragment extends BottomSheetBaseFragment {

    public static final String TAG = "SnippetFragment";

    private static final String[] STORAGE_PERMISSIONS  = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE"
    };

    private static final int READ_CONTENT_MESSAGE = 0;

    private static final String EXTRA_SNIPPET_URI = "snippet_uri";
    private static final String EXTRA_TEMP_SNIPPET_URI = "temp_snippet_uri";
    private static final String EXTRA_SNIPPET_MIMETYPE = "snippet_mime_type";

    // This is just to ensure the editor set a text/plain mode
    private static final String DEFAULT_SNIPPED_NAME = "Unnamed";

    private SnippetContentBinding mBinding;
    private final Handler mUiHandler;
    private Uri mUri;
    private String mMimeType;
    private boolean mReadOnly;
    private boolean mDirty;
    private long mContentSize;
    private boolean mNeedPermissions;
    private OnContentChangedListener mContentChangedListener = this::onContentChanged;

    public interface OnSnippetSavedListener {
        void onSnippetSaved(Uri uri, String mimeType, long size);
    }

    public SnippetFragment() {
        mUiHandler = new Handler(msg -> {
            if (msg.what == READ_CONTENT_MESSAGE) {
                readFileContent();
            }
            return false;
        });
    }

    public static SnippetFragment newInstance(Context context) {
        return newInstance(context, null, "text/plain");
    }

    public static SnippetFragment newInstance(Context context, Uri snippet, String mimeType) {
        SnippetFragment fragment = new SnippetFragment();
        Bundle arguments = new Bundle();
        if (snippet != null) {
            arguments.putParcelable(EXTRA_SNIPPET_URI, snippet);
            arguments.putString(EXTRA_SNIPPET_MIMETYPE, mimeType);
        } else {
            try {
                Uri uri = CacheHelper.createNewTemporaryFileUri(context, ".snippet");
                arguments.putParcelable(EXTRA_TEMP_SNIPPET_URI, uri);
            } catch (IOException ex) {
                Log.e(TAG, "Can't create temporary snippet", ex);
            }
        }

        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //noinspection ConstantConditions
        mUri = getArguments().getParcelable(EXTRA_SNIPPET_URI);
        mMimeType = getArguments().getString(EXTRA_SNIPPET_MIMETYPE, "text/plain");
        mReadOnly = mUri != null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (mBinding != null) {
            mBinding.unbind();
        }
    }

    @Override
    public void inflateContent(ViewGroup parent) {
        LayoutInflater li = LayoutInflater.from(getContext());
        mBinding = DataBindingUtil.inflate(li, R.layout.snippet_content, parent, true);

        mBinding.editor
                .setReadOnly(mReadOnly)
                .setWrap(true)
                .setTextSize(14)
                .setNotifyMimeTypeChanges(!mReadOnly);
        if (!mReadOnly) {
            mBinding.editor.listenOn(mContentChangedListener);
        }
    }

    public void onContentLayoutChanged(ViewGroup parent) {
        ViewGroup.MarginLayoutParams lp =
                (ViewGroup.MarginLayoutParams) mBinding.editor.getLayoutParams();
        lp.height = parent.getMinimumHeight() -
                lp.topMargin - lp.bottomMargin -
                mBinding.editor.getPaddingTop() - mBinding.editor.getPaddingBottom();
    }

    @Override
    public String[] requiredPermissions() {
        return STORAGE_PERMISSIONS;
    }

    @Override
    public void onPermissionDenied() {
        mNeedPermissions = true;
        mReadOnly = true;
        mBinding.editor.setReadOnly(true);
        mBinding.editor.setNotifyMimeTypeChanges(false);
        loadContent(getString(R.string.snippet_dialog_snippet_permissions).getBytes());
    }

    @Override
    public boolean allowExpandedState() {
        return false;
    }

    @Override
    public void onContentReady() {
        //noinspection ConstantConditions
        Uri snippetUri = getArguments().getParcelable(EXTRA_SNIPPET_URI);
        if (snippetUri == null) {
            snippetUri = getArguments().getParcelable(EXTRA_TEMP_SNIPPET_URI);
        }
        if (snippetUri != null) {
            try {
                //noinspection ConstantConditions
                ContentResolver cr = getContext().getContentResolver();
                try (InputStream is = cr.openInputStream(snippetUri)) {
                    if (is != null) {
                        byte[] data = new byte[is.available()];
                        IOUtils.read(is, data);
                        loadContent(data);
                        return;
                    }
                }
            } catch (IOException ex) {
                Log.e(TAG, "Cannot load content stream: " + snippetUri, ex);
            }

            // Display an advise
            CoordinatorLayout decorator = ViewHelper.findFirstParentViewOfType(getContentView(),
                    CoordinatorLayout.class);
            if (decorator != null) {
                AndroidHelper.showErrorSnackbar(getContext(), decorator,
                        R.string.snippet_dialog_error);
            }
        }
    }

    private void loadContent(byte[] data) {
        final String fileName;
        if (mNeedPermissions) {
            fileName = DEFAULT_SNIPPED_NAME + ".txt";
        } else {
            String ext = AceEditorView.resolveExtensionFromMimeType(mMimeType);
            fileName = DEFAULT_SNIPPED_NAME + "." + ext;
        }

        mBinding.editor.scrollTo(0, 0);
        mBinding.editor.loadEncodedContent(fileName, Base64.encode(data, Base64.NO_WRAP));
    }

    @Override
    public int getTitle() {
        return R.string.snippet_dialog_title;
    }

    public @DrawableRes int getActionResId() {
        return mReadOnly ? R.drawable.ic_open_in_new : R.drawable.ic_paste;
    }

    @Override
    public void onDonePressed() {
        if (mNeedPermissions) {
            return;
        }
        readFileContent();
        Thread.yield();
        if (!mReadOnly && mContentSize > 0) {
            //noinspection ConstantConditions
            Uri snippetUri = getArguments().getParcelable(EXTRA_TEMP_SNIPPET_URI);
            if (snippetUri != null) {
                Activity a = getActivity();
                Fragment f = getParentFragment();
                if (f instanceof OnSnippetSavedListener) {
                    ((OnSnippetSavedListener) f).onSnippetSaved(snippetUri, mMimeType, mContentSize);
                } else if (a instanceof OnSnippetSavedListener) {
                    ((OnSnippetSavedListener) a).onSnippetSaved(snippetUri, mMimeType, mContentSize);
                }
            }
        }
    }

    public void onActionPressed() {
        if (mNeedPermissions) {
            return;
        }
        if (!mReadOnly) {
            // Paste
            //noinspection ConstantConditions
            ClipboardManager clipboard =
                    (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard.getPrimaryClip() != null &&
                    clipboard.getPrimaryClip().getItemCount() > 0) {
                ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
                CharSequence data = item.getText();
                if (data != null) {
                    byte[] content = data.toString().getBytes();
                    loadContent(content);
                    mUiHandler.post(() -> saveContent(content));
                }
            }

        } else {
            // Open in external
            String action = getString(R.string.action_share);
            //noinspection ConstantConditions
            Uri uri = FileProvider.getUriForFile(getContext(), "com.ruesga.rview.content",
                    new File(mUri.getPath()));
            ActivityHelper.open(getContext(), action, uri, "text/plain");
            dismiss();
        }
    }

    private void onContentChanged() {
        mDirty = true;
        mUiHandler.removeMessages(READ_CONTENT_MESSAGE);
        mUiHandler.sendEmptyMessageDelayed(READ_CONTENT_MESSAGE, 500L);
    }

    private void readFileContent() {
        if (mDirty) {
            mBinding.editor.readContent(
                    new AceEditorView.OnReadContentReadyListener() {
                        @Override
                        public void onReadContentReady(byte[] content, String mimeType) {
                            if (content.length > 0) {
                                content = Base64.decode(content, Base64.NO_WRAP);
                            }
                            saveContent(content);
                            if (mimeType != null) {
                                mMimeType = mimeType;
                            }
                        }

                        @Override
                        public void onContentUnchanged() {
                        }
                    }, !mReadOnly);
            mDirty = false;
        }
    }

    private synchronized void saveContent(byte[] content) {
        //noinspection ConstantConditions
        Uri snippetUri = getArguments().getParcelable(EXTRA_TEMP_SNIPPET_URI);
        if (snippetUri != null && getActivity() != null) {
            try {
                ContentResolver cr = getActivity().getContentResolver();
                try (OutputStream os = cr.openOutputStream(snippetUri)) {
                    IOUtils.write(content, os);
                    mContentSize = content.length;
                }
            } catch (IOException ex) {
                Log.e(TAG, "Cannot load content stream: " + snippetUri, ex);
            }
        }
    }
}
