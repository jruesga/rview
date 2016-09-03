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

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ruesga.rview.BaseActivity;
import com.ruesga.rview.R;
import com.ruesga.rview.databinding.ViewPagerBinding;

import java.lang.ref.WeakReference;

public abstract class PaginableFragment extends Fragment {

    public class PageFragmentAdapter extends FragmentPagerAdapter {
        private final SparseArray<WeakReference<Fragment>> mFragments = new SparseArray<>();

        public PageFragmentAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Fragment fragment = (Fragment) super.instantiateItem(container, position);
            mFragments.put(position, new WeakReference<>(fragment));
            return fragment;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            super.destroyItem(container, position, object);
            mFragments.remove(position);
        }

        @Override
        public Fragment getItem(int position) {
            return getFragment(position);
        }

        @Override
        public int getCount() {
            return getTabs().length;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return getTabs()[position];
        }

        public Fragment getCacheFragment(int position) {
            WeakReference<Fragment> ref = mFragments.get(position);
            return ref != null ? ref.get() : null;
        }
    }

    private ViewPagerBinding mBinding;

    public abstract String[] getTabs();

    public abstract Fragment getFragment(int position);

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.view_pager, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final boolean isTwoPane = getResources().getBoolean(R.bool.config_is_two_pane);
        mBinding.viewPager.setSwipeable(!isTwoPane);
        mBinding.viewPager.setOffscreenPageLimit(getTabs().length + 1);
        mBinding.viewPager.setAdapter(new PageFragmentAdapter(getChildFragmentManager()));
        ((BaseActivity) getActivity()).configureTabs(mBinding.viewPager);
    }

    @Override
    public final void onDestroyView() {
        super.onDestroyView();
        mBinding.unbind();
    }
}
