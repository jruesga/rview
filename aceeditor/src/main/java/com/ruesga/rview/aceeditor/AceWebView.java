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
package com.ruesga.rview.aceeditor;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.view.NestedScrollingChild;
import android.support.v4.view.NestedScrollingChildHelper;
import android.support.v4.view.ViewCompat;
import android.text.InputType;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.webkit.ClientCertRequest;
import android.webkit.ConsoleMessage;
import android.webkit.GeolocationPermissions;
import android.webkit.HttpAuthHandler;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.PermissionRequest;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.SafeBrowsingResponse;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import static android.view.ViewTreeObserver.OnGlobalLayoutListener;

/**
 * This is a {@link WebView} subclass to deal with IME issues with the Ace Code Editor library. See
 * bellow for a detailed of bug threads describing the problem. Currently version is mostly working
 * on most of the keyboard devices, but there could be some of them with bad behaviours because of
 * the described bug. This implementation disables the IME input method of the {@link WebView}.
 * <p />
 * It also provides a @{link NestedScrollingChild} implementation to deal with
 * {@link android.support.design.widget.CoordinatorLayout}
 *
 * @see "https://bugs.chromium.org/p/chromium/issues/detail?id=118639"
 * @see "https://github.com/ajaxorg/ace/issues/2964"
 * @see "https://github.com/ajaxorg/ace/issues/1917"
 * @see "https://github.com/takahirom/webview-in-coordinatorlayout"
 */
public class AceWebView extends WebView implements NestedScrollingChild {
    private static final String JS_REGISTER_INTERFACE =
            "javascript:" +
                    "var ace_editor = ace.edit(document.getElementsByClassName('ace_editor')[0].id);" +
                    "var ace_last_touch = [0, 0];" +
                    "ace_editor.container.addEventListener('touchstart', function(e) {" +
                    "    ace_last_touch = [e.touches[0].clientX, e.touches[0].clientY + 20];" +
                    "});" +
                    "function ace_request_focus() {" +
                    "    ace_editor.container.getElementsByClassName('ace_text-input')[0].focus();" +
                    "    var coords = ace_editor.renderer.screenToTextCoordinates(ace_last_touch[0], ace_last_touch[1]);" +
                    "    ace_editor.gotoLine(coords.row, coords.column, true);" +
                    "}";
    private static final String JS_REQUEST_FOCUS = "javascript: ace_request_focus();";

    private int mLastY;
    private long mLastTouchDownEvent;
    private boolean mHasMoveEvent;
    private final int[] mScrollOffset = new int[2];
    private final int[] mScrollConsumed = new int[2];
    private int mNestedOffsetY;
    private NestedScrollingChildHelper mChildHelper;
    private OnLongClickListener mWrappedLongClickListener = null;
    private final Handler mHandler;
    private boolean mJsRegistered = false;

    private WebChromeClient mWrappedWebChromeClient = new WebChromeClient();
    private WebViewClient mWrappedWebViewClient = new WebViewClient();

    private ActionMode mActionMode = null;
    private ClipboardManager mClipboard;
    private boolean mIsKeyboardVisible = false;
    private final ActionMode.Callback actionModeCallback = new android.view.ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.aceeditor_action_mode_menu, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            if (!hasPasteAction()) {
                menu.removeItem(R.id.menu_paste);
            }
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if (item.getItemId() == R.id.menu_cut) {
                dispatchKeyEvent(new KeyEvent(0, 0,
                        KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_X, 0, KeyEvent.META_CTRL_ON));
                requestFocus();
            } else if (item.getItemId() == R.id.menu_copy) {
                dispatchKeyEvent(new KeyEvent(0, 0,
                        KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_C, 0, KeyEvent.META_CTRL_ON));
                requestFocus();
            } else if (item.getItemId() == R.id.menu_paste) {
                dispatchKeyEvent(new KeyEvent(0, 0,
                        KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_V, 0, KeyEvent.META_CTRL_ON));
                requestFocus();
            } else if (item.getItemId() == R.id.menu_select_all) {
                dispatchKeyEvent(new KeyEvent(0, 0,
                        KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_A, 0, KeyEvent.META_CTRL_ON));
            }
            mode.finish();
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mActionMode = null;
        }
    };

    public AceWebView(Context context) {
        super(context);
        mChildHelper = new NestedScrollingChildHelper(this);
        mClipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        mHandler = new Handler();

        setNestedScrollingEnabled(true);
        // TODO for now the selection-handles scripts doesn't support non-chromium webviews
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            configureActionMode();
        }
        super.setWebChromeClient(mWebChromeClient);
        super.setWebViewClient(mWebViewClient);

        final OnGlobalLayoutListener listener = new OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                getViewTreeObserver().removeOnGlobalLayoutListener(this);

                Rect r = new Rect();
                getWindowVisibleDisplayFrame(r);

                int heightDiff = getRootView().getHeight() - (r.bottom - r.top);
                float dp = heightDiff / getResources().getDisplayMetrics().density;
                mIsKeyboardVisible = dp > 200;
            }
        };
        getViewTreeObserver().addOnGlobalLayoutListener(listener);
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void configureActionMode() {
        setClickable(false);
        setLongClickable(false);
        setHapticFeedbackEnabled(false);

        super.setOnClickListener(v -> {});
        super.setOnLongClickListener(v -> {
            // Ensure the editor has the focus
            loadUrl(JS_REQUEST_FOCUS);

            mHandler.postDelayed(() -> {
                // Select the current word
                if (mActionMode != null) {
                    dispatchKeyEvent(new KeyEvent(0, 0,
                            KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_K, 0,
                            KeyEvent.META_ALT_ON | KeyEvent.META_SHIFT_ON));
                    requestFocus();
                }

                // Start the action mode
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    mActionMode = startActionMode(actionModeCallback, ActionMode.TYPE_FLOATING);
                } else {
                    mActionMode = startActionMode(actionModeCallback);
                }
            }, 250L);

            return mWrappedLongClickListener == null ||
                mWrappedLongClickListener.onLongClick(v);
        });
    }

    @Override
    public void setWebViewClient(WebViewClient client) {
        mWrappedWebViewClient = client;
    }

    @Override
    public void setWebChromeClient(WebChromeClient client) {
        mWrappedWebChromeClient = client;
    }

    @Override
    public final void setOnLongClickListener(@Nullable OnLongClickListener l) {
        mWrappedLongClickListener = l;
    }

    @Override
    public boolean dispatchKeyEventPreIme(KeyEvent event) {
        if (mActionMode != null && event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            mActionMode.finish();
            return !mIsKeyboardVisible;
        }
        return super.dispatchKeyEventPreIme(event);
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        InputConnection inputConnection = super.onCreateInputConnection(outAttrs);
        outAttrs.imeOptions = EditorInfo.IME_NULL;
        outAttrs.inputType = InputType.TYPE_NULL;
        return inputConnection;
    }

    @Override
    @SuppressLint("ClickableViewAccessibility")
    public boolean onTouchEvent(MotionEvent ev) {
        boolean returnValue = false;

        MotionEvent event = MotionEvent.obtain(ev);
        final int action = ev.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            mNestedOffsetY = 0;
        }
        int eventY = (int) event.getY();
        event.offsetLocation(0, mNestedOffsetY);
        switch (action) {
            case MotionEvent.ACTION_MOVE:
                int deltaY = mLastY - eventY;
                if (dispatchNestedPreScroll(0, deltaY, mScrollConsumed, mScrollOffset)) {
                    deltaY -= mScrollConsumed[1];
                    mLastY = eventY - mScrollOffset[1];
                    event.offsetLocation(0, -mScrollOffset[1]);
                    mNestedOffsetY += mScrollOffset[1];
                }
                returnValue = super.onTouchEvent(event);

                if (dispatchNestedScroll(0, mScrollOffset[1], 0, deltaY, mScrollOffset)) {
                    event.offsetLocation(0, mScrollOffset[1]);
                    mNestedOffsetY += mScrollOffset[1];
                    mLastY -= mScrollOffset[1];
                }
                mHasMoveEvent = true;
                break;
            case MotionEvent.ACTION_DOWN:
                returnValue = super.onTouchEvent(event);
                mLastY = eventY;
                startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL);
                mLastTouchDownEvent = System.currentTimeMillis();
                mHasMoveEvent = false;
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                long delta = (System.currentTimeMillis() - mLastTouchDownEvent);
                returnValue = super.onTouchEvent(event);
                stopNestedScroll();
                if (!mHasMoveEvent && delta < ViewConfiguration.getTapTimeout()) {
                    if (mActionMode != null) {
                        mActionMode.finish();
                    }
                }
                break;
        }
        return returnValue;
    }

    @Override
    public void setNestedScrollingEnabled(boolean enabled) {
        mChildHelper.setNestedScrollingEnabled(enabled);
    }

    @Override
    public boolean isNestedScrollingEnabled() {
        return mChildHelper.isNestedScrollingEnabled();
    }

    @Override
    public boolean startNestedScroll(int axes) {
        return mChildHelper.startNestedScroll(axes);
    }

    @Override
    public void stopNestedScroll() {
        mChildHelper.stopNestedScroll();
    }

    @Override
    public boolean hasNestedScrollingParent() {
        return mChildHelper.hasNestedScrollingParent();
    }

    @Override
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed,
                                        int dyUnconsumed, int[] offsetInWindow) {
        return mChildHelper.dispatchNestedScroll(
                dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow) {
        return mChildHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
        return mChildHelper.dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
        return mChildHelper.dispatchNestedPreFling(velocityX, velocityY);
    }

    private boolean hasPasteAction() {
        return mClipboard.hasPrimaryClip()
                && mClipboard.getPrimaryClipDescription().hasMimeType(
                ClipDescription.MIMETYPE_TEXT_PLAIN);
    }


    @SuppressWarnings({"deprecation", "FieldCanBeLocal"})
    private final WebChromeClient mWebChromeClient = new WebChromeClient() {
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            mWrappedWebChromeClient.onProgressChanged(view, newProgress);
        }

        @Override
        public void onReceivedTitle(WebView view, String title) {
            mWrappedWebChromeClient.onReceivedTitle(view, title);
        }

        @Override
        public void onReceivedIcon(WebView view, Bitmap icon) {
            mWrappedWebChromeClient.onReceivedIcon(view, icon);
        }

        @Override
        public void onReceivedTouchIconUrl(WebView view, String url, boolean precomposed) {
            mWrappedWebChromeClient.onReceivedTouchIconUrl(view, url, precomposed);
        }

        @Override
        public void onShowCustomView(View view, CustomViewCallback callback) {
            mWrappedWebChromeClient.onShowCustomView(view, callback);
        }

        @Override
        public void onShowCustomView(View view, int requestedOrientation,
                                     CustomViewCallback callback) {
            mWrappedWebChromeClient.onShowCustomView(view, requestedOrientation, callback);
        }

        @Override
        public void onHideCustomView() {
            mWrappedWebChromeClient.onHideCustomView();
        }

        @Override
        public boolean onCreateWindow(WebView view, boolean isDialog,
                                      boolean isUserGesture, Message resultMsg) {
            return mWrappedWebChromeClient.onCreateWindow(view, isDialog, isUserGesture, resultMsg);
        }

        @Override
        public void onRequestFocus(WebView view) {
            mWrappedWebChromeClient.onRequestFocus(view);
        }

        @Override
        public void onCloseWindow(WebView window) {
            mWrappedWebChromeClient.onCloseWindow(window);
        }

        @Override
        public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
            return mWrappedWebChromeClient.onJsAlert(view, url, message, result);
        }

        @Override
        public boolean onJsConfirm(WebView view, String url, String message, JsResult result) {
            return mWrappedWebChromeClient.onJsConfirm(view, url, message, result);
        }

        @Override
        public boolean onJsPrompt(WebView view, String url, String message, String defaultValue,
                                  JsPromptResult result) {
            return mWrappedWebChromeClient.onJsPrompt(view, url, message, defaultValue, result);
        }

        @Override
        public boolean onJsBeforeUnload(WebView view, String url, String message, JsResult result) {
            return mWrappedWebChromeClient.onJsBeforeUnload(view, url, message, result);
        }

        @Override
        public void onExceededDatabaseQuota(String url, String databaseIdentifier, long quota,
                                            long estimatedDatabaseSize, long totalQuota, WebStorage.QuotaUpdater quotaUpdater) {
            mWrappedWebChromeClient.onExceededDatabaseQuota(url, databaseIdentifier, quota,
                    estimatedDatabaseSize, totalQuota, quotaUpdater);
        }

        @Override
        public void onReachedMaxAppCacheSize(long requiredStorage, long quota,
                                             WebStorage.QuotaUpdater quotaUpdater) {
            mWrappedWebChromeClient.onReachedMaxAppCacheSize(requiredStorage, quota, quotaUpdater);
        }

        @Override
        public void onGeolocationPermissionsShowPrompt(String origin,
                                                       GeolocationPermissions.Callback callback) {
            mWrappedWebChromeClient.onGeolocationPermissionsShowPrompt(origin, callback);
        }

        @Override
        public void onGeolocationPermissionsHidePrompt() {
            mWrappedWebChromeClient.onGeolocationPermissionsHidePrompt();
        }

        @Override
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        public void onPermissionRequest(PermissionRequest request) {
            mWrappedWebChromeClient.onPermissionRequest(request);
        }

        @Override
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        public void onPermissionRequestCanceled(PermissionRequest request) {
            mWrappedWebChromeClient.onPermissionRequestCanceled(request);
        }

        @Override
        public boolean onJsTimeout() {
            return mWrappedWebChromeClient.onJsTimeout();
        }

        @Override
        public void onConsoleMessage(String message, int lineNumber, String sourceID) {
            mWrappedWebChromeClient.onConsoleMessage(message, lineNumber, sourceID);
        }

        @Override
        public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
            return mWrappedWebChromeClient.onConsoleMessage(consoleMessage);
        }

        @Override
        public Bitmap getDefaultVideoPoster() {
            return mWrappedWebChromeClient.getDefaultVideoPoster();
        }

        @Override
        public View getVideoLoadingProgressView() {
            return mWrappedWebChromeClient.getVideoLoadingProgressView();
        }

        @Override
        public void getVisitedHistory(ValueCallback<String[]> callback) {
            mWrappedWebChromeClient.getVisitedHistory(callback);
        }

        @Override
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback,
                                         FileChooserParams fileChooserParams) {
            return mWrappedWebChromeClient.onShowFileChooser(
                    webView, filePathCallback, fileChooserParams);
        }
    };

    @SuppressWarnings({"deprecation", "FieldCanBeLocal"})
    private final WebViewClient mWebViewClient = new WebViewClient() {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return mWrappedWebViewClient.shouldOverrideUrlLoading(view, url);
        }

        @Override
        @TargetApi(Build.VERSION_CODES.N)
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            return mWrappedWebViewClient.shouldOverrideUrlLoading(view, request);
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            mWrappedWebViewClient.onPageStarted(view, url, favicon);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            // TODO for now the selection-handles scripts doesn't support non-chromium webviews
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                mHandler.postDelayed(() -> {
                    if (!mJsRegistered) {
                        loadUrl(JS_REGISTER_INTERFACE);
                        mJsRegistered = true;
                    }
                }, 100L);
            }
            mWrappedWebViewClient.onPageFinished(view, url);
        }

        @Override
        public void onLoadResource(WebView view, String url) {
            mWrappedWebViewClient.onLoadResource(view, url);
        }

        @Override
        @TargetApi(Build.VERSION_CODES.M)
        public void onPageCommitVisible(WebView view, String url) {
            mWrappedWebViewClient.onPageCommitVisible(view, url);
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
            return mWrappedWebViewClient.shouldInterceptRequest(view, url);
        }

        @Override
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        public WebResourceResponse shouldInterceptRequest(WebView view,
                                                          WebResourceRequest request) {
            return mWrappedWebViewClient.shouldInterceptRequest(view, request);
        }

        @Override
        public void onTooManyRedirects(WebView view, Message cancelMsg, Message continueMsg) {
            mWrappedWebViewClient.onTooManyRedirects(view, cancelMsg, continueMsg);
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description,
                                    String failingUrl) {
            mWrappedWebViewClient.onReceivedError(view, errorCode, description, failingUrl);
        }

        @Override
        @TargetApi(Build.VERSION_CODES.M)
        public void onReceivedError(WebView view, WebResourceRequest request,
                                    WebResourceError error) {
            mWrappedWebViewClient.onReceivedError(view, request, error);
        }

        @Override
        @TargetApi(Build.VERSION_CODES.M)
        public void onReceivedHttpError(WebView view, WebResourceRequest request,
                                        WebResourceResponse errorResponse) {
            mWrappedWebViewClient.onReceivedHttpError(view, request, errorResponse);
        }

        @Override
        public void onFormResubmission(WebView view, Message dontResend, Message resend) {
            mWrappedWebViewClient.onFormResubmission(view, dontResend, resend);
        }

        @Override
        public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
            mWrappedWebViewClient.doUpdateVisitedHistory(view, url, isReload);
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            mWrappedWebViewClient.onReceivedSslError(view, handler, error);
        }

        @Override
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        public void onReceivedClientCertRequest(WebView view, ClientCertRequest request) {
            mWrappedWebViewClient.onReceivedClientCertRequest(view, request);
        }

        @Override
        public void onReceivedHttpAuthRequest(WebView view, HttpAuthHandler handler,
                                              String host, String realm) {
            mWrappedWebViewClient.onReceivedHttpAuthRequest(view, handler, host, realm);
        }

        @Override
        public boolean shouldOverrideKeyEvent(WebView view, KeyEvent event) {
            return mWrappedWebViewClient.shouldOverrideKeyEvent(view, event);
        }

        @Override
        public void onUnhandledKeyEvent(WebView view, KeyEvent event) {
            mWrappedWebViewClient.onUnhandledKeyEvent(view, event);
        }

        @Override
        public void onScaleChanged(WebView view, float oldScale, float newScale) {
            mWrappedWebViewClient.onScaleChanged(view, oldScale, newScale);
        }

        @Override
        public void onReceivedLoginRequest(WebView view, String realm, String account,
                                           String args) {
            mWrappedWebViewClient.onReceivedLoginRequest(view, realm, account, args);
        }

        @Override
        @TargetApi(Build.VERSION_CODES.O)
        public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
            return mWrappedWebViewClient.onRenderProcessGone(view, detail);
        }

        @Override
        @TargetApi(Build.VERSION_CODES.O_MR1)
        public void onSafeBrowsingHit(WebView view, WebResourceRequest request, int threatType,
                                      SafeBrowsingResponse callback) {
            mWrappedWebViewClient.onSafeBrowsingHit(view, request, threatType, callback);
        }
    };
}
