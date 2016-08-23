package com.ruesga.rview;

import android.app.Application;

import com.airbnb.rxgroups.ObservableManager;

public class RviewApplication extends Application {
    private final ObservableManager mObservableManager = new ObservableManager();

    @Override public void onCreate() {
        super.onCreate();
    }

    public ObservableManager observableManager() {
        return mObservableManager;
    }
}
