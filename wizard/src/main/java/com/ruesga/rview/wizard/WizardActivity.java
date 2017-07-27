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
package com.ruesga.rview.wizard;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.databinding.BindingAdapter;
import android.databinding.DataBindingUtil;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Keep;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;

import com.ruesga.rview.wizard.databinding.ActivityWizardBinding;

import java.util.ArrayList;
import java.util.concurrent.Callable;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import me.tatarka.rxloader2.RxLoader;
import me.tatarka.rxloader2.RxLoaderManager;
import me.tatarka.rxloader2.RxLoaderManagerCompat;
import me.tatarka.rxloader2.RxLoaderObserver;
import me.tatarka.rxloader2.safe.SafeObservable;

import static android.content.res.Configuration.ORIENTATION_PORTRAIT;

public abstract class WizardActivity extends AppCompatActivity {

    @Keep
    @SuppressWarnings("unused")
    public static class WizardWorkflow {
        public String title;
        public boolean hasBack;
        public boolean isBackEnabled;
        public String backLabel;
        public int backDrawable;
        public boolean hasForward;
        public boolean isForwardEnabled;
        public String forwardLabel;
        public int forwardDrawable;
        public boolean hasPageOptionsMenu;
        public boolean isInProgress;
        public boolean hasCancel;

        @BindingAdapter({"srcDrawable"})
        public static void setButtonResource(Button button, int resource) {
            final Context ctx = button.getContext();
            Drawable drawable = null;
            if (resource != 0) {
                drawable = ContextCompat.getDrawable(ctx, resource);
            }
            button.setCompoundDrawablesWithIntrinsicBounds(
                    button.getId() == R.id.page_action_back ? drawable : null,
                    null,
                    button.getId() == R.id.page_action_forward ? drawable : null,
                    null);
        }
    }

    @Keep
    @SuppressWarnings("unused")
    public static class WorkFlowHandlers {
        private final WizardActivity mActivity;

        public WorkFlowHandlers(WizardActivity activity) {
            mActivity = activity;
        }

        public void onActionPressed(View view) {
            mActivity.performActionPressed(view);
        }

        public void onPageOptionsMenuPressed(View view) {
            if (!mActivity.mWorkflow.isInProgress) {
                mActivity.performShowPageOptionsMenu(view);
            }
        }
    }

    private final RxLoaderObserver<Boolean> mBackObserver =
            new RxLoaderObserver<Boolean>() {
                @Override
                public void onNext(Boolean result) {
                    if (mIsActivityConfigured && result && !mHasStateSaved) {
                        doPerformActionPressed(false);
                    }
                }

                @Override
                public void onError(Throwable e) {
                    mWorkflow.isBackEnabled = false;
                }
            };
    private final RxLoaderObserver<Boolean> mForwardObserver =
            new RxLoaderObserver<Boolean>() {
                @Override
                public void onNext(Boolean result) {
                    if (mIsActivityConfigured && result && !mHasStateSaved) {
                        doPerformActionPressed(true);
                    }
                }

                @Override
                public void onError(Throwable e) {
                    mWorkflow.isForwardEnabled = false;
                }
            };

    private static final String STATE_CURRENT_PAGE = "current_page";
    private static final String STATE_CURRENT_CHOOSER = "current_chooser";
    private static final String STATE_IS_IN_PROGRESS = "is_in_progress";

    private final ArrayList<WizardPageFragment> mPages = new ArrayList<>();
    private boolean mArePagesConfigured;
    private boolean mIsActivityConfigured;

    private ActivityWizardBinding mBinding;
    private final WizardWorkflow mWorkflow = new WizardWorkflow();
    private FragmentManager mFragmentManager;
    private WizardPageFragment mCurrentPageFragment;
    private String mCurrentChooserFragmentTag;
    private int mCurrentPage = 0;
    private boolean mHasStateSaved;
    private boolean mHasStateRestored;

    private Animator mHeaderAnimator;
    private boolean mIsHeaderAnimatorRunning;

    private int mMinHeaderHeight;
    private int mMaxHeaderHeight;
    private boolean mIsExtendedHeaderLayoutSupported;

    private final Bundle mData = new Bundle();

    private Pair<RxLoader<Boolean>,RxLoader<Boolean>>[] mLoaders;

    protected WizardActivity() {
    }

    @SuppressWarnings("unchecked")
    @Override
    protected final void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        mFragmentManager = getSupportFragmentManager();

        // Load the wizard pages
        setupPages();
        mArePagesConfigured = true;
        if (mPages.isEmpty()) {
            Log.w(getTag(), "Wizard has no pages!!");
            finish();
            return;
        }
        if (savedInstanceState != null) {
            restoreInstance(savedInstanceState);
        }

        if (mCurrentPage < 0 || mCurrentPage >= mPages.size()) {
            performNavigateToPage(mCurrentPage);
            return;
        }


        // Configure the view
        setupStatusBar();
        final Resources res = getResources();
        mMinHeaderHeight = (int) res.getDimension(R.dimen.wizard_min_actionbar_size);
        mMaxHeaderHeight = (int) res.getDimension(R.dimen.wizard_max_actionbar_size);

        // Bind the views
        boolean isTablet = res.getBoolean(R.bool.config_isTablet);
        mIsExtendedHeaderLayoutSupported = !isTablet &&
                res.getConfiguration().orientation == ORIENTATION_PORTRAIT;
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_wizard);
        mBinding.setHandlers(new WorkFlowHandlers(this));
        if (mIsExtendedHeaderLayoutSupported) {
            mBinding.pageHeader.getLayoutParams().height =
                    mIsExtendedHeaderLayoutSupported && mPages.get(mCurrentPage).hasExtendedHeader()
                            ? mMaxHeaderHeight : mMinHeaderHeight;
        }

        // Prepared the back and forward loaders
        mLoaders = new Pair[mPages.size()];
        RxLoaderManager loaderManager = RxLoaderManagerCompat.get(this);
        int i = 0;
        for (WizardPageFragment page : mPages) {
            Callable<Boolean> back = page.doBackAction();
            Callable<Boolean> forward = page.doForwardAction();
            RxLoader<Boolean> backLoader = null;
            if (back != null) {
                backLoader = loaderManager.create("lb" + i, actionObserver(back), mBackObserver);
            }
            RxLoader<Boolean> forwardLoader = null;
            if (forward != null) {
                forwardLoader = loaderManager.create("lf" + i, actionObserver(forward),
                        mForwardObserver);
            }
            mLoaders[i] = new Pair<>(backLoader, forwardLoader);
            i++;
        }

        // Navigate and draw page information
        if (mCurrentChooserFragmentTag != null) {
            WizardChooserFragment chooser = (WizardChooserFragment)
                    mFragmentManager.findFragmentByTag(mCurrentChooserFragmentTag);
            performOpenChooserPage(chooser);
            bindChooserWorkflow(chooser);
        } else {
            performNavigateToPage(mCurrentPage);
        }
        mIsActivityConfigured = true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBinding != null) {
            mBinding.unbind();
        }
        if (mHeaderAnimator != null && mIsHeaderAnimatorRunning) {
            mHeaderAnimator.cancel();
        }
        mHeaderAnimator = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        mHasStateSaved = false;
    }

    private void restoreInstance(Bundle savedInstanceState) {
        if (mHasStateRestored) {
            return;
        }
        mCurrentPage = savedInstanceState.getInt(STATE_CURRENT_PAGE, 0);
        mCurrentChooserFragmentTag = savedInstanceState.getString(STATE_CURRENT_CHOOSER);
        mWorkflow.isInProgress = savedInstanceState.getBoolean(STATE_IS_IN_PROGRESS, false);
        for (WizardPageFragment page : mPages) {
            page.restoreState(this, savedInstanceState);
            final Bundle bundle = page.savedState();
            if (bundle != null) {
                mData.putAll(bundle);
            }
        }
        mHasStateSaved = false;
        mHasStateRestored = true;
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedState) {
        if (savedState != null) {
            super.onRestoreInstanceState(savedState);
            restoreInstance(savedState);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_CURRENT_PAGE, mCurrentPage);
        outState.putString(STATE_CURRENT_CHOOSER, mCurrentChooserFragmentTag);
        outState.putBoolean(STATE_IS_IN_PROGRESS, mWorkflow.isInProgress);
        savePagesState(outState);
        mHasStateSaved = true;
        mHasStateRestored = false;
    }

    public abstract void setupPages();

    public Intent onWizardFinished(Bundle savedState) {
        return null;
    }

    public final <T extends WizardPageFragment> void addPage(Class<T> page) {
        addPage(mPages.size(), page);
    }

    public final <T extends WizardPageFragment> void addPage(
            int index, Class<T> page) {
        checkPagesSetupStatus();
        WizardPageFragment fragment =
                (WizardPageFragment) mFragmentManager.findFragmentByTag(page.getSimpleName());
        mPages.add(index, fragment != null ? fragment : WizardPageFragment.newInstance(page));
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void setupStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            TypedValue value = new TypedValue();
            TypedArray a = obtainStyledAttributes(value.data, new int[]{R.attr.colorPrimaryDark});
            getWindow().setStatusBarColor(a.getColor(0, 0));
            a.recycle();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
    }

    public void showMessage(String message) {
        Snackbar.make(mBinding.pageContentLayout.pageContentRootView,
                message, Snackbar.LENGTH_LONG).show();
    }

    private String getTag() {
        return getClass().getSimpleName();
    }

    @Override
    public void onBackPressed() {
        if (mIsHeaderAnimatorRunning) {
            return;
        }
        if (mCurrentChooserFragmentTag != null) {
            WizardChooserFragment chooser = (WizardChooserFragment)
                    mFragmentManager.findFragmentByTag(mCurrentChooserFragmentTag);
            performChooserClose(chooser, RESULT_CANCELED, null);
            return;
        }
        if (mCurrentPageFragment != null && mCurrentPageFragment.hasBackAction()) {
            performNavigateToPage(--mCurrentPage);
            return;
        }
        super.onBackPressed();
    }

    public void onValidationChanged(WizardPageFragment page) {
        mWorkflow.isBackEnabled = page.canPerformBackAction();
        mWorkflow.isForwardEnabled = page.canPerformForwardAction();
        mBinding.setWorkflow(mWorkflow);
    }

    void performOpenChooserPage(WizardChooserFragment chooser) {
        // Save the old fragment
        if (mCurrentPageFragment != null) {
            final Bundle bundle = mCurrentPageFragment.savedState();
            if (bundle != null) {
                mData.putAll(bundle);
            }
        }

        // Close the keyboard
        closeKeyboardIfNeeded();

        // Mark the parent page of the chooser
        chooser.setParentPage(mCurrentPage);

        // Navigate to chooser
        mCurrentChooserFragmentTag = chooser.getClass().getSimpleName();
        FragmentTransaction transaction = mFragmentManager.beginTransaction()
                .setReorderingAllowed(false);
        if (mCurrentPageFragment != null) {
            transaction.remove(mCurrentPageFragment);
        }
        transaction.replace(R.id.page_content, chooser, mCurrentChooserFragmentTag);
        transaction.setCustomAnimations(android.R.anim.fade_out, android.R.anim.fade_in);
        transaction.commit();

        // Animate the header if needed
        if (mIsExtendedHeaderLayoutSupported) {
            mHeaderAnimator = createHeaderAnimation(
                    mBinding.pageHeader.getLayoutParams().height, mMinHeaderHeight);
            mHeaderAnimator.start();
        }

        // Bind the workflow
        bindChooserWorkflow(chooser);
    }

    void performChooserClose(WizardChooserFragment fragment, int resultCode, Intent data) {
        internalNavigateToPage(fragment.getParentPage(), fragment);
        mCurrentPageFragment.onChooserResult(resultCode, data);
        mCurrentChooserFragmentTag = null;
    }

    @SuppressLint("CommitTransaction")
    private void performNavigateToPage(int page) {
        if (page >= mPages.size()) {
            // End of wizard
            Bundle state = new Bundle();
            savePagesState(state);
            setResult(RESULT_OK, onWizardFinished(state));
            finish();
            return;
        } else if (page < 0) {
            // How we got here?
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        // Save the old fragment
        WizardPageFragment oldFragment = mCurrentPageFragment;
        if (mCurrentPageFragment != null) {
            final Bundle bundle = mCurrentPageFragment.savedState();
            if (bundle != null) {
                mData.putAll(bundle);
            }
        }

        internalNavigateToPage(page, oldFragment);
    }

    private void internalNavigateToPage(int page, Fragment oldFragment) {
        // And restore the state of current fragment
        mCurrentPageFragment = mPages.get(page);
        mCurrentPageFragment.restoreState(this, mData);

        if (!mCurrentPageFragment.canRequireKeyboard()) {
            closeKeyboardIfNeeded();
        }

        // Navigate to page
        FragmentTransaction transaction = mFragmentManager.beginTransaction()
                .setReorderingAllowed(false);
        if (oldFragment != null) {
            transaction.remove(oldFragment);
        }
        transaction.replace(R.id.page_content, mCurrentPageFragment,
                mCurrentPageFragment.getClass().getSimpleName());
        transaction.setCustomAnimations(android.R.anim.fade_out, android.R.anim.fade_in);
        transaction.commit();

        // Animate the header if needed
        if (mIsExtendedHeaderLayoutSupported) {
            int nextHeaderHeight = mCurrentPageFragment.hasExtendedHeader()
                    ? mMaxHeaderHeight : mMinHeaderHeight;
            int currentHeaderHeight = mBinding.pageHeader.getLayoutParams().height;
            mHeaderAnimator = createHeaderAnimation(currentHeaderHeight, nextHeaderHeight);
            mHeaderAnimator.start();
        }

        // Configure the workflow
        bindPageWorkflow(mCurrentPageFragment);
        mCurrentPage = page;
    }

    private void bindPageWorkflow(WizardPageFragment fragment) {
        mWorkflow.title = getSafeStringResource(fragment.getPageTitle());
        mWorkflow.hasBack = fragment.hasBackAction();
        mWorkflow.backLabel = getSafeStringResource(fragment.getBackActionLabel());
        mWorkflow.backDrawable = fragment.getBackActionDrawable();
        mWorkflow.isBackEnabled = fragment.canPerformBackAction();
        mWorkflow.hasForward = fragment.hasForwardAction();
        mWorkflow.forwardLabel = getSafeStringResource(fragment.getForwardActionLabel());
        mWorkflow.forwardDrawable = fragment.getForwardActionDrawable();
        mWorkflow.isForwardEnabled = fragment.canPerformForwardAction();
        mWorkflow.hasPageOptionsMenu = fragment.hasPageOptionsMenu();
        mWorkflow.hasCancel = false;
        mBinding.setWorkflow(mWorkflow);
    }

    private void bindChooserWorkflow(WizardChooserFragment fragment) {
        mWorkflow.title = getSafeStringResource(fragment.getTitle());
        mWorkflow.hasBack = false;
        mWorkflow.hasForward = fragment.hasAcceptButton();
        if (mWorkflow.hasForward) {
            mWorkflow.forwardDrawable = 0;
            mWorkflow.forwardLabel = getString(R.string.accept);
            mWorkflow.isForwardEnabled = fragment.isAcceptButtonEnabled();
        }
        mWorkflow.hasCancel = true;
        mWorkflow.hasPageOptionsMenu = false;
        mBinding.setWorkflow(mWorkflow);
    }

    private String getSafeStringResource(@StringRes int resource) {
        if (resource == 0) {
            return null;
        }
        return getString(resource);
    }

    private void checkPagesSetupStatus() {
        if (mArePagesConfigured) {
            throw new IllegalStateException("Pages can't no be modified after setup.");
        }
    }

    private void performShowPageOptionsMenu(View view) {
        if (mCurrentPageFragment != null && mCurrentPageFragment.getPageOptionsMenu() > 0) {
            PopupMenu popup = new PopupMenu(this, view, Gravity.BOTTOM);
            popup.inflate(mCurrentPageFragment.getPageOptionsMenu());
            popup.setOnMenuItemClickListener(
                    mCurrentPageFragment.getPageOptionsMenuOnItemClickListener());
            popup.show();
        }
    }

    private Animator createHeaderAnimation(int from, int to) {
        final ValueAnimator animator = ValueAnimator.ofInt(from, to);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.setDuration(250L);
        animator.addUpdateListener(animation -> {
            final View v = mBinding.pageHeader;
            v.getLayoutParams().height = (Integer) animation.getAnimatedValue();
            v.requestLayout();
        });
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                mWorkflow.isBackEnabled = false;
                mWorkflow.isForwardEnabled = false;
                mBinding.setWorkflow(mWorkflow);
                mIsHeaderAnimatorRunning = true;
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                if (mCurrentPageFragment != null) {
                    onValidationChanged(mCurrentPageFragment);
                }
                mBinding.setWorkflow(mWorkflow);
                mIsHeaderAnimatorRunning = false;
            }

            @Override
            public void onAnimationCancel(Animator animator) {
            }

            @Override
            public void onAnimationRepeat(Animator animator) {
            }
        });
        return animator;
    }

    public void closeKeyboardIfNeeded() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager inputMethodManager =
                    (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void performActionPressed(View v) {
        final boolean forward = v.getId() == R.id.page_action_forward;
        final boolean cancel = v.getId() == R.id.page_action_cancel;

        // Check if there is an opened chooser
        if (mCurrentChooserFragmentTag != null){
            if (cancel) {
                WizardChooserFragment chooser = (WizardChooserFragment)
                        mFragmentManager.findFragmentByTag(mCurrentChooserFragmentTag);
                chooser.cancel();
                return;
            }
            if (forward) {
                WizardChooserFragment chooser = (WizardChooserFragment)
                        mFragmentManager.findFragmentByTag(mCurrentChooserFragmentTag);
                chooser.close();
                return;
            }
        }

        // Check that all field are validated, before continue. This should
        // be passed before getting here, but...
        if (forward) {
            mCurrentPageFragment.triggerAllValidators(null);
            if (!mCurrentPageFragment.canPerformForwardAction()) {
                mWorkflow.isForwardEnabled = false;
                mBinding.setWorkflow(mWorkflow);
                return;
            }
        }

        // Determine if need to do some operation before continue with the
        RxLoader<Boolean> loader= null;
        if (mCurrentPage < mLoaders.length) {
            loader = forward ? mLoaders[mCurrentPage].second : mLoaders[mCurrentPage].first;
        }
        if (loader != null) {
            loader.clear();
            loader.restart();
        } else {
            // Run directly
            doPerformActionPressed(forward);
        }
    }

    private Observable<Boolean> actionObserver(Callable<Boolean> call) {
        return SafeObservable.fromNullCallable(call)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(disposable -> changeInProgressStatus(true))
                .doOnTerminate(() -> changeInProgressStatus(false));
    }

    private void doPerformActionPressed(boolean forward) {
        performNavigateToPage(forward ? ++mCurrentPage : --mCurrentPage);
    }

    public void changeInProgressStatus(boolean inProgress) {
        mWorkflow.isInProgress = inProgress;
        mBinding.setWorkflow(mWorkflow);
    }

    private void savePagesState(Bundle outState) {
        for (WizardPageFragment page : mPages) {
            Bundle bundle = page.savedState();
            if (bundle != null) {
                outState.putAll(bundle);
            }
        }
    }
}
