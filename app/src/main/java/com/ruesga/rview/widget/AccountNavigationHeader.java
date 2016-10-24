package com.ruesga.rview.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import com.ruesga.rview.R;
import com.ruesga.rview.drawer.OnMiniDrawerNavigationOpenStatusChangedListener;

public class AccountNavigationHeader extends FrameLayout
        implements OnMiniDrawerNavigationOpenStatusChangedListener {

    private View mAccountInfoView;

    public AccountNavigationHeader(Context context) {
        this(context, null);
    }

    public AccountNavigationHeader(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AccountNavigationHeader(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void onMiniDrawerNavigationOpenStatusChanged(float offset) {
        if (mAccountInfoView == null) {
            mAccountInfoView = findViewById(R.id.account_info);
        }
        mAccountInfoView.setAlpha(offset);
    }
}
