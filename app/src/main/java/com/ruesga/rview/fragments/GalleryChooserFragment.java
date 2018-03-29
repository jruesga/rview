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
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.google.android.flexbox.AlignItems;
import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.FlexWrap;
import com.google.android.flexbox.FlexboxLayoutManager;
import com.ruesga.rview.R;
import com.ruesga.rview.databinding.GalleryChooserContentBinding;
import com.ruesga.rview.databinding.GalleryItemBinding;
import com.ruesga.rview.misc.PicassoHelper;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import me.tatarka.rxloader2.RxLoader;
import me.tatarka.rxloader2.RxLoaderManager;
import me.tatarka.rxloader2.RxLoaderManagerCompat;
import me.tatarka.rxloader2.RxLoaderObserver;
import me.tatarka.rxloader2.safe.SafeObservable;

public class GalleryChooserFragment extends BottomSheetBaseFragment {

    public static final String TAG = "GalleryChooserFragment";

    private static final String[] STORAGE_PERMISSIONS  = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE"
    };

    @SuppressWarnings("WeakerAccess")
    public static class MediaItem implements Parcelable {
        public final long mId;
        public final Uri mUri;
        public String mTitle;
        public String mMimeType;
        public int mMediaType;
        public long mSize;

        public MediaItem(@NonNull Uri uri) {
            mUri = uri;
            mId = Long.parseLong(mUri.getLastPathSegment());
        }

        private MediaItem(Parcel in) {
            mUri = Uri.CREATOR.createFromParcel(in);
            mId = Long.parseLong(mUri.getLastPathSegment());
            if (in.readInt() == 1) {
                mTitle = in.readString();
            }
            if (in.readInt() == 1) {
                mMimeType = in.readString();
            }
            mMediaType = in.readInt();
            mSize = in.readLong();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            Uri.writeToParcel(parcel, mUri);
            parcel.writeInt(mTitle != null ? 1 : 0);
            if (mTitle != null) {
                parcel.writeString(mTitle);
            }
            parcel.writeInt(mMimeType != null ? 1 : 0);
            if (mMimeType != null) {
                parcel.writeString(mMimeType);
            }
            parcel.writeInt(mMediaType);
            parcel.writeLong(mSize);
        }

        public static final Creator<MediaItem> CREATOR = new Creator<MediaItem>() {
            @Override
            public MediaItem createFromParcel(Parcel in) {
                return new MediaItem(in);
            }

            @Override
            public MediaItem[] newArray(int size) {
                return new MediaItem[size];
            }
        };

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            MediaItem mediaItem = (MediaItem) o;

            return mUri != null ? mUri.equals(mediaItem.mUri) : mediaItem.mUri == null;

        }

        @Override
        public int hashCode() {
            return mUri != null ? mUri.hashCode() : 0;
        }
    }

    public interface OnGallerySelectedListener {
        void onGallerySelection(List<MediaItem> selection);
    }

    @Keep
    @SuppressWarnings("unused")
    public static class EventHandlers {
        private final GalleryChooserFragment mFragment;

        public EventHandlers(GalleryChooserFragment fragment) {
            mFragment = fragment;
        }

        public void onImagePressed(View v) {
            int position = (int) v.getTag();
            mFragment.onImagePressed(position);
        }

        public void onRetry(View v) {
            mFragment.onRetry();
        }
    }

    private static class GalleryViewHolder extends RecyclerView.ViewHolder {
        private final GalleryItemBinding mBinding;
        private GalleryViewHolder(GalleryItemBinding binding) {
            super(binding.getRoot());
            mBinding = binding;
            binding.executePendingBindings();
        }
    }

    private static class GalleryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final Context mContext;
        private final Picasso mPicasso;
        private final int mImageWidth, mImageHeight;
        private final EventHandlers mHandlers;

        private final List<MediaItem> mImages = new ArrayList<>();
        private final List<Boolean> mSelection = new ArrayList<>();
        private final List<MediaItem> mPrevSelection;

        private final Object sync = new Object();

        private GalleryAdapter(GalleryChooserFragment fragment,
                int imageWidth, List<MediaItem> prevSelection) {
            setHasStableIds(true);
            mContext = fragment.getContext();
            mHandlers = new EventHandlers(fragment);
            mPicasso = PicassoHelper.getDefaultPicassoClient(mContext);
            mImageHeight = mImageWidth = imageWidth;
            mPrevSelection = prevSelection;
        }

        private void clear() {
            synchronized (sync) {
                mImages.clear();
                mSelection.clear();
            }
        }

        private void addAll(List<MediaItem> images) {
            synchronized (sync) {
                int count = mImages.size();
                mImages.addAll(images);
                mSelection.addAll(Arrays.asList(new Boolean[images.size()]));

                // Select previous items
                for (MediaItem prevSelection : mPrevSelection) {
                    int pos = images.indexOf(prevSelection);
                    if (pos >= 0) {
                        mSelection.set(count + pos, Boolean.TRUE);
                    }
                }
            }
        }

        private void toggleSelection(int position) {
            synchronized (sync) {
                Boolean val = mSelection.get(position);
                mSelection.set(position, val != null ? !val : Boolean.TRUE);
            }
        }

        private List<MediaItem> getSelection() {
            List<MediaItem> selection = new ArrayList<>();
            synchronized (sync) {
                int count = mImages.size();
                for (int i = 0; i < count; i++) {
                    boolean selected = mSelection.size() > i
                            && mSelection.get(i) != null && mSelection.get(i);
                    if (selected) {
                        selection.add(mImages.get(i));
                    }
                }
                mSelection.clear();
            }
            return selection;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            return new GalleryChooserFragment.GalleryViewHolder(DataBindingUtil.inflate(
                    inflater, R.layout.gallery_item, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            MediaItem mediaItem = mImages.get(position);
            Boolean selected = mSelection.get(position);

            GalleryViewHolder itemViewHolder = (GalleryViewHolder) holder;
            itemViewHolder.mBinding.getRoot().setTag(position);

            View item = itemViewHolder.mBinding.getRoot();
            item.getLayoutParams().width = mImageWidth;
            item.getLayoutParams().height = mImageHeight;
            ImageView image = itemViewHolder.mBinding.image;
            ViewGroup.LayoutParams lp = image.getLayoutParams();
            if (lp instanceof FlexboxLayoutManager.LayoutParams) {
                FlexboxLayoutManager.LayoutParams params = (FlexboxLayoutManager.LayoutParams) lp;
                params.setFlexGrow(1.0f);
            }

            itemViewHolder.mBinding.setIsSelected(selected);
            itemViewHolder.mBinding.setIsVideo(
                    mediaItem.mMediaType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO);
            itemViewHolder.mBinding.setHandlers(mHandlers);
            PicassoHelper.bindImage(mPicasso, image, null, mediaItem.mUri,
                    new Rect(0, 0, mImageWidth, mImageHeight));
        }

        @Override
        public long getItemId(int position) {
            return mImages.get(position).mId;
        }

        @Override
        public int getItemCount() {
            return mImages.size();
        }
    }

    private final RxLoaderObserver<List<MediaItem>> mMediaObserver
            = new RxLoaderObserver<List<MediaItem>>() {
        @Override
        public void onNext(List<MediaItem> images) {
            mMediaLoader.clear();

            mAdapter.clear();
            mAdapter.addAll(images);
            mAdapter.notifyDataSetChanged();

            synchronized (mSync) {
                mLoaded = true;
            }
            mLoading = mError = mNeedPermissions = false;
            mEmpty = images.size() == 0;
            updateState();
        }

        @Override
        public void onError(Throwable e) {
            mMediaLoader.clear();

            Log.e(TAG, "Failed to fetch images", e);
            mLoading = mEmpty = mNeedPermissions = false;
            mError = true;
            updateState();
        }
    };

    private static final String EXTRA_SELECTION = "selection";
    private static final String EXTRA_IS_LOADING = "is_loading";
    private static final String EXTRA_IS_EMPTY = "is_empty";
    private static final String EXTRA_IS_ERROR = "is_error";
    private static final String EXTRA_IS_NEED_PERMISSIONS = "is_need_permissions";

    private final Handler mHandler;
    private EventHandlers mHandlers;
    private final ContentObserver mContentObserver;
    private RxLoader<List<MediaItem>> mMediaLoader;
    private GalleryChooserContentBinding mBinding;
    private GalleryAdapter mAdapter;
    private int mImageWidth;
    private boolean mEmpty = false;
    private boolean mNeedPermissions = false;
    private boolean mError = false;
    private boolean mLoading = false;
    private boolean mLoaded = false;
    private final Object mSync = new Object();

    private List<MediaItem> mPrevSelection = new ArrayList<>();

    public GalleryChooserFragment() {
        mHandler = new Handler();
        mContentObserver = new ContentObserver(mHandler) {
            @Override
            public void onChange(boolean selfChange, Uri uri) {
                if (mMediaLoader != null) {
                    mMediaLoader.clear();
                    mMediaLoader.restart();
                }
            }
        };
    }

    public static GalleryChooserFragment newInstance() {
        return newInstance(new ArrayList<>());
    }

    public static GalleryChooserFragment newInstance(List<MediaItem> selection) {
        GalleryChooserFragment fragment = new GalleryChooserFragment();
        Bundle arguments = new Bundle();
        arguments.putParcelableArray(EXTRA_SELECTION,
                selection.toArray(new MediaItem[selection.size()]));
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandlers = new EventHandlers(this);

        if (savedInstanceState != null) {
            mPrevSelection = savedInstanceState.getParcelableArrayList(EXTRA_SELECTION);
            mLoading = savedInstanceState.getBoolean(EXTRA_IS_LOADING, false);
            mEmpty = savedInstanceState.getBoolean(EXTRA_IS_EMPTY, false);
            mError = savedInstanceState.getBoolean(EXTRA_IS_ERROR, false);
            mNeedPermissions = savedInstanceState.getBoolean(EXTRA_IS_NEED_PERMISSIONS, false);
        } else if (getArguments() != null) {
            Parcelable[] parcels = getArguments().getParcelableArray(EXTRA_SELECTION);
            if (parcels != null) {
                for (Parcelable parcel : parcels) {
                    mPrevSelection.add((MediaItem) parcel);
                }
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        unregisterObserver();
        if (mBinding != null) {
            mBinding.unbind();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mAdapter != null) {
            outState.putParcelableArrayList(EXTRA_SELECTION,
                    (ArrayList<MediaItem>) mAdapter.getSelection());
        } else {
            outState.putParcelableArrayList(EXTRA_SELECTION, new ArrayList<>());
        }
        outState.putBoolean(EXTRA_IS_LOADING, mLoading);
        outState.putBoolean(EXTRA_IS_EMPTY, mEmpty);
        outState.putBoolean(EXTRA_IS_ERROR, mError);
        outState.putBoolean(EXTRA_IS_NEED_PERMISSIONS, mNeedPermissions);
    }

    @Override
    public void inflateContent(ViewGroup parent) {
        LayoutInflater li = LayoutInflater.from(getContext());
        mBinding = DataBindingUtil.inflate(li, R.layout.gallery_chooser_content, parent, true);
        updateState();
        mBinding.setHandlers(mHandlers);

        // Calculate the best suitable layout width to accommodate the media views
        float desiredWidth = TypedValue.applyDimension (TypedValue.COMPLEX_UNIT_DIP,
                144, getResources().getDisplayMetrics());
        int columns = Math.round(getMaxWidth() / desiredWidth);
        mImageWidth = getMaxWidth() / columns;
    }

    private void createLoadersWithValidContext() {
        if (getActivity() == null) {
            return;
        }

        if (mAdapter == null) {
            mAdapter = new GalleryAdapter(this, mImageWidth, mPrevSelection);
            FlexboxLayoutManager layoutManager = new FlexboxLayoutManager(getContext());
            layoutManager.setFlexWrap(FlexWrap.WRAP);
            layoutManager.setFlexDirection(FlexDirection.ROW);
            layoutManager.setAlignItems(AlignItems.STRETCH);
            mBinding.list.setLayoutManager(layoutManager);
            mBinding.list.setAdapter(mAdapter);

            // Fetch or join current loader
            RxLoaderManager loaderManager = RxLoaderManagerCompat.get(this);
            mMediaLoader = loaderManager.create(fetchMedia(), mMediaObserver);

            mHandler.postDelayed(() -> {
                synchronized (mSync) {
                    if (getActivity() != null && !mLoaded) {
                        mLoading = true;
                        updateState();
                    }
                }
            }, 750);
        }
    }

    private void registerObserver() {
        //noinspection ConstantConditions
        getContext().getContentResolver().registerContentObserver(
                MediaStore.Files.getContentUri("external"), true, mContentObserver);
    }

    private void unregisterObserver() {
        //noinspection ConstantConditions
        getContext().getContentResolver().unregisterContentObserver(mContentObserver);
    }

    private Observable<List<MediaItem>> fetchMedia() {
        //noinspection ConstantConditions
        final Context ctx = getContext().getApplicationContext();
        return SafeObservable.fromNullCallable(() -> obtainImages(ctx))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    private List<MediaItem> obtainImages(Context context) {
        List<MediaItem> images = new ArrayList<>();
        String[] projection = {MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.TITLE,
                MediaStore.Files.FileColumns.MIME_TYPE,
                MediaStore.Files.FileColumns.MEDIA_TYPE,
                MediaStore.Files.FileColumns.SIZE};
        Uri uri = MediaStore.Files.getContentUri("external");
        String sort = MediaStore.Files.FileColumns.DATE_MODIFIED + " DESC";
        String where = MediaStore.Files.FileColumns.MEDIA_TYPE + " in (?, ?)";
        String[] whereArgs = {String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE),
                String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO)};
        Cursor c = context.getContentResolver().query(uri, projection, where, whereArgs, sort);
        if (c != null) {
            try {
                while (c.moveToNext()) {
                    MediaItem attachment = new MediaItem(
                            Uri.withAppendedPath(uri, c.getString(c.getColumnIndex(
                                MediaStore.Files.FileColumns._ID))));
                    attachment.mTitle = c.getString(c.getColumnIndex(
                            MediaStore.Files.FileColumns.TITLE));
                    attachment.mMimeType = c.getString(c.getColumnIndex(
                            MediaStore.Files.FileColumns.MIME_TYPE));
                    attachment.mMediaType = c.getInt(c.getColumnIndex(
                            MediaStore.Files.FileColumns.MEDIA_TYPE));
                    attachment.mSize = c.getLong(c.getColumnIndex(
                            MediaStore.Files.FileColumns.SIZE));
                    images.add(attachment);
                }
            } finally {
                try {
                    c.close();
                } catch (Exception e) {
                    // Ignore: handle exception
                }
            }
        }
        return images;
    }

    private void onImagePressed(int position) {
        mAdapter.toggleSelection(position);
        mAdapter.notifyItemChanged(position);
    }

    @Override
    public int getTitle() {
        return R.string.gallery_chooser_title;
    }

    @Override
    public String[] requiredPermissions() {
        return STORAGE_PERMISSIONS;
    }

    @Override
    public boolean allowDraggingState() {
        return !(mLoading || mEmpty || mError || mNeedPermissions);
    }

    @Override
    public void onLoading() {
        mNeedPermissions = mLoading = mError = false;
        mLoading = true;
        updateState();
    }

    @Override
    public void onPermissionDenied() {
        mEmpty = mLoading = mError = false;
        mNeedPermissions = true;
        updateState();
    }

    @Override
    public void onContentReady() {
        mNeedPermissions = false;
        updateState();

        createLoadersWithValidContext();

        // Request the data
        mMediaLoader.clear();
        mMediaLoader.restart();
        registerObserver();
    }

    @Override
    public void onDonePressed() {
        if (!(mLoading || mEmpty || mError || mNeedPermissions)) {
            Activity a = getActivity();
            Fragment f = getParentFragment();
            if (f != null && f instanceof OnGallerySelectedListener) {
                ((OnGallerySelectedListener) f).onGallerySelection(mAdapter.getSelection());
            } else if (a != null && a instanceof OnGallerySelectedListener) {
                ((OnGallerySelectedListener) a).onGallerySelection(mAdapter.getSelection());
            }
        }
    }

    public void onRetry() {
        requestPermissions();
    }

    private void updateState() {
        mBinding.setLoading(mLoading);
        mBinding.setError(mError);
        mBinding.setEmpty(mEmpty);
        mBinding.setNeedPermissions(mNeedPermissions);

        if (mEmpty) {
            mBinding.setMessage(getString(R.string.gallery_chooser_empty));
        } else if (mLoading) {
            mBinding.setMessage(getString(R.string.gallery_chooser_loading));
        } else if (mNeedPermissions) {
            mBinding.setMessage(getString(R.string.gallery_chooser_need_permissions));
        } else if (mError) {
            mBinding.setMessage(getString(R.string.gallery_chooser_error));
        }
    }
}
