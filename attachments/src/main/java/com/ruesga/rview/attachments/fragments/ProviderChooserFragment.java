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
package com.ruesga.rview.attachments.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ruesga.rview.attachments.AttachmentsProvider;
import com.ruesga.rview.attachments.AttachmentsProviderFactory;
import com.ruesga.rview.attachments.AuthenticationInfo;
import com.ruesga.rview.attachments.Provider;
import com.ruesga.rview.attachments.R;
import com.ruesga.rview.attachments.databinding.AttachmentProviderChooserBinding;
import com.ruesga.rview.attachments.databinding.AttachmentProviderItemBinding;
import com.ruesga.rview.attachments.preferences.Preferences;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class ProviderChooserFragment extends DialogFragment {

    public static final String TAG = "ProviderChooserFragment";

    @Keep
    @SuppressWarnings("unused")
    public static class EventHandlers {
        private final ProviderChooserFragment mFragment;

        public EventHandlers(ProviderChooserFragment fragment) {
            mFragment = fragment;
        }

        public void onProviderPressed(View v) {
            mFragment.onProviderSelected((Provider) v.getTag());
        }

        public void onProviderSignoff(View v) {
            mFragment.onProviderSignoff((Provider) v.getTag());
        }
    }

    private static class ProviderViewHolder extends RecyclerView.ViewHolder {
        private final AttachmentProviderItemBinding mBinding;
        private ProviderViewHolder(AttachmentProviderItemBinding binding) {
            super(binding.getRoot());
            mBinding = binding;
            binding.executePendingBindings();
        }
    }

    public interface OnAttachmentProviderSelectedListener {
        void onAttachmentProviderSelection(Provider provider);
    }

    private static class ProviderAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private final ProviderChooserFragment mFragment;
        private final List<AttachmentsProvider> mProviders;
        private final Provider mCurrentProvider;
        private final EventHandlers mHandlers;

        private ProviderAdapter(ProviderChooserFragment fragment) {
            setHasStableIds(true);
            mFragment = fragment;
            mHandlers = new EventHandlers(fragment);
            mProviders = new ArrayList<>(
                    AttachmentsProviderFactory.getAllAvailableAttachmentProviders());
            mCurrentProvider = Preferences.getProvider(mFragment.getContext());
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            return new ProviderViewHolder(DataBindingUtil.inflate(
                    inflater, R.layout.attachment_provider_item, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            AttachmentsProvider provider = mProviders.get(position);
            AuthenticationInfo auth = Preferences.getAuthenticationInfo(
                    mFragment.getContext(), provider.getType());

            ProviderViewHolder itemViewHolder = (ProviderViewHolder) holder;
            itemViewHolder.mBinding.setIcon(provider.getIcon());
            itemViewHolder.mBinding.setText(mFragment.getString(provider.getName()));
            if (auth != null) {
                itemViewHolder.mBinding.setAccount(auth.accountEmail);
            } else {
                itemViewHolder.mBinding.setAccount(null);
            }
            itemViewHolder.mBinding.setIsSelected(provider.getType().equals(mCurrentProvider));
            itemViewHolder.mBinding.setProvider(provider.getType());
            itemViewHolder.mBinding.setHandlers(mHandlers);
        }

        @Override
        public long getItemId(int position) {
            return mProviders.get(position).getType().hashCode();
        }

        @Override
        public int getItemCount() {
            return mProviders.size();
        }
    }


    private AttachmentProviderChooserBinding mBinding;
    private AlertDialog mDialog;

    public static ProviderChooserFragment newInstance() {
        return new ProviderChooserFragment();
    }

    public ProviderChooserFragment() {
    }

    @NonNull
    @Override
    public final Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        buildDialog(builder);
        return builder.create();
    }

    public void buildDialog(AlertDialog.Builder builder) {
        LayoutInflater inflater = LayoutInflater.from(builder.getContext());
        mBinding = DataBindingUtil.inflate(
                inflater, R.layout.attachment_provider_chooser, null, false);

        builder.setTitle(R.string.attachment_dialog_chooser_title)
                .setView(mBinding.getRoot())
                .setNegativeButton(R.string.attachment_dialog_chooser_cancel, null);

        mBinding.list.setLayoutManager(new LinearLayoutManager(
                getContext(), RecyclerView.VERTICAL, false));
        mBinding.list.setAdapter(new ProviderAdapter(this));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (mDialog != null) {
            mDialog.dismiss();
        }
        if (mBinding != null) {
            mBinding.unbind();
        }
    }

    private void onProviderSelected(Provider provider) {
        Provider currentProvider = Preferences.getProvider(getContext());
        if (currentProvider.equals(provider)) {
            dismiss();
            return;
        }

        Preferences.setProvider(getContext(), provider);
        notifyOnProviderSelected(provider);
        dismiss();
    }

    private void onProviderSignoff(Provider provider) {
        mDialog = new AlertDialog.Builder(getContext())
                .setTitle(R.string.attachment_dialog_signoff_title)
                .setMessage(R.string.attachment_dialog_signoff_message)
                .setPositiveButton(R.string.attachment_dialog_chooser_continue,
                        (dialogInterface, i) -> {
                            // This remove the internal authentication data. The next time
                            // the user choose this account, it will perform a signoff before
                            // the signon. Also, choose the NONE provider
                            Preferences.setAuthenticationInfo(getContext(), provider, null);
                            Preferences.setProvider(getContext(), Provider.NONE);
                            notifyOnProviderSelected(Provider.NONE);
                            dismiss();
                        })
                .setNegativeButton(R.string.attachment_dialog_chooser_cancel,
                        (dialogInterface, i) -> dialogInterface.cancel())
                .setOnCancelListener(dialogInterface -> dismiss())
                .create();
        mDialog.show();
    }


    private void notifyOnProviderSelected(Provider provider) {
        Activity a = getActivity();
        Fragment f = getParentFragment();
        if (f instanceof OnAttachmentProviderSelectedListener) {
            ((OnAttachmentProviderSelectedListener) f).onAttachmentProviderSelection(provider);
        } else if (a instanceof OnAttachmentProviderSelectedListener) {
            ((OnAttachmentProviderSelectedListener) a).onAttachmentProviderSelection(provider);
        }
    }
}
