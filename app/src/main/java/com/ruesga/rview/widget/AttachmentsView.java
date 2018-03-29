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
package com.ruesga.rview.widget;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.ruesga.rview.R;
import com.ruesga.rview.attachments.Attachment;
import com.ruesga.rview.databinding.AttachmentItemBinding;
import com.ruesga.rview.databinding.AttachmentsViewBinding;

import java.util.ArrayList;
import java.util.List;

import static android.support.v7.widget.LinearLayoutManager.HORIZONTAL;

public class AttachmentsView extends FrameLayout {

    @Keep
    @SuppressWarnings("unused")
    public static class EventHandlers {
        private final AttachmentsView mView;

        public EventHandlers(AttachmentsView view) {
            mView = view;
        }

        public void onAttachmentPressed(View v) {
            Attachment attachment = (Attachment) v.getTag();
            mView.onAttachmentPressed(attachment);
        }

        public void onAttachmentDropped(View v) {
            Attachment attachment = (Attachment) v.getTag();
            mView.onAttachmentDropped(attachment);
        }
    }

    public interface OnAttachmentPressedListener {
        void onAttachmentPressed(Attachment attachment);
    }

    public interface OnAttachmentDroppedListener {
        void onAttachmentDropped(Attachment attachment);
    }

    public static class AttachmentsViewHolder extends RecyclerView.ViewHolder {
        private final AttachmentItemBinding mBinding;
        private AttachmentsViewHolder(AttachmentItemBinding binding) {
            super(binding.getRoot());
            mBinding = binding;
            binding.executePendingBindings();
        }
    }

    private static class AttachmentsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private final AttachmentsView mView;
        private final List<Attachment> mAttachments = new ArrayList<>();
        private final Context mContext;
        private final EventHandlers mPressHandlers;
        private final EventHandlers mDropHandlers;

        private AttachmentsAdapter(AttachmentsView view) {
            setHasStableIds(true);
            mView = view;
            mContext = view.getContext();
            mPressHandlers = new EventHandlers(view);
            mDropHandlers = new EventHandlers(view);
        }

        private void clear() {
            mAttachments.clear();
        }

        private void addAll(List<Attachment> attachments) {
            mAttachments.addAll(attachments);
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            return new AttachmentsViewHolder(DataBindingUtil.inflate(
                    inflater, R.layout.attachment_item, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            Attachment item = mAttachments.get(position);
            AttachmentsViewHolder itemViewHolder = (AttachmentsViewHolder) holder;
            itemViewHolder.mBinding.setModel(item);
            itemViewHolder.mBinding.setPressHandlers(
                    mView.mOnAttachmentPressedListener == null ? null : mPressHandlers);
            itemViewHolder.mBinding.setDropHandlers(
                    mView.mOnAttachmentDropListener == null ? null : mDropHandlers);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getItemCount() {
            return mAttachments.size();
        }
    }

    private RecyclerView mList;
    private OnAttachmentPressedListener mOnAttachmentPressedListener;
    private OnAttachmentDroppedListener mOnAttachmentDropListener;

    public AttachmentsView(Context context) {
        this(context, null);
    }

    public AttachmentsView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AttachmentsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        LayoutInflater layoutInflater = LayoutInflater.from(getContext());
        AttachmentsViewBinding binding = DataBindingUtil.inflate(
                layoutInflater, R.layout.attachments_view, this, false);
        mList = (RecyclerView) binding.getRoot();
        mList.setLayoutManager(new LinearLayoutManager(context, HORIZONTAL, false));
        mList.setAdapter(new AttachmentsAdapter(this));
        addView(binding.getRoot());
    }

    public AttachmentsView from(List<Attachment> attachments) {
        AttachmentsAdapter adapter = (AttachmentsAdapter) mList.getAdapter();
        adapter.clear();
        if (attachments != null) {
            adapter.addAll(attachments);
        }
        adapter.notifyDataSetChanged();
        return this;
    }

    public AttachmentsView listenOn(OnAttachmentPressedListener cb) {
        mOnAttachmentPressedListener = cb;
        return this;
    }

    public AttachmentsView listenOn(OnAttachmentDroppedListener cb) {
        mOnAttachmentDropListener = cb;
        return this;
    }

    private void onAttachmentPressed(Attachment attachment) {
        if (mOnAttachmentPressedListener != null) {
            mOnAttachmentPressedListener.onAttachmentPressed(attachment);
        }
    }

    private void onAttachmentDropped(Attachment attachment) {
        if (mOnAttachmentDropListener != null) {
            mOnAttachmentDropListener.onAttachmentDropped(attachment);
        }
    }
}
