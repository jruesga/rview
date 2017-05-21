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

package com.ruesga.rview.widget;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.os.ParcelableCompat;
import android.support.v4.os.ParcelableCompatCreatorCallbacks;
import android.support.v4.view.AbsSavedState;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.text.method.KeyListener;
import android.text.style.ImageSpan;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.LinearLayout;

import com.ruesga.rview.R;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A {@link View} to edit hash tags(#) and user tags(@).
 *
 * TODO Add support for RTL
 */
@SuppressWarnings("unused")
public class TagEditTextView extends LinearLayout {

    private class TagEditText extends AppCompatEditText {
        public TagEditText(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public TagEditText(Context context, AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);
        }

        @Override
        protected void onSelectionChanged(int selStart, int selEnd) {
            super.onSelectionChanged(selStart, selEnd);
            int minSelPos = mTagList.size();
            if (selStart < minSelPos) {
                setSelection(minSelPos);
            }
        }

        @Override
        public boolean onTouchEvent(@NonNull MotionEvent event) {
            if (!isEnabled()) {
                return false;
            }

            int action = event.getAction();
            switch (action) {
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_DOWN:
                    int x = (int) event.getX();
                    int y = (int) event.getY();

                    // Compute where the user has touched (is in a remove tag area?)
                    int w = getWidth();
                    int x1 = 0, y1 = 0;
                    for (Tag tag : mTagList) {
                        x1 += tag.w;
                        if (x1 > w) {
                            x1 = tag.w;
                            y1 = tag.h;
                        }
                        if ((x > (x1 - tag.w) && x < x1) && (y > y1 && y < (y1 + tag.h))) {
                            if (x >= (x1 - mChipRemoveAreaWidth)) {
                                // User click in a remove tag area
                                if (action == MotionEvent.ACTION_UP) {
                                    onTagRemoveClick(tag);
                                    playSoundEffect(SoundEffectConstants.CLICK);
                                }
                                return true;
                            }

                            // Tag clicked
                            if (action == MotionEvent.ACTION_UP) {
                                if (mTagClickCallBack != null) {
                                    mTagClickCallBack.onTagClick(tag);
                                    playSoundEffect(SoundEffectConstants.CLICK);
                                }
                            }
                        }
                    }
                    break;
            }
            return super.onTouchEvent(event);
        }
    }

    public interface OnTagEventListener {
        void onTagCreate(Tag tag);
        void onTagRemove(Tag tag);
    }

    public interface OnTagClickListener {
        void onTagClick(Tag tag);
    }

    public interface OnComputedTagEndedListener {
        void onComputedTagEnded();
    }

    public static class Tag {
        public CharSequence mTag;
        private int mColor;

        private int w;
        private int h;

        public CharSequence toPlainTag() {
            if (mTag != null && mTag.length() > 2) {
                return mTag.subSequence(1, mTag.length());
            }
            return "";
        }

        public Tag copy() {
            Tag tag = new Tag();
            tag.mTag = mTag;
            tag.mColor = mColor;
            return tag;
        }

        private String toSavedState() {
            String tag = "";
            if (mTag != null && mTag.length() > 2) {
                tag = mTag.toString();
            }
            return mColor + "|" + tag;
        }

        private void fromSavedState(String savedState) {
            String[] s = savedState.split("\\|");
            mColor = Integer.valueOf(s[0]);
            mTag = s[1];
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Tag tag = (Tag) o;

            return mTag != null ? mTag.equals(tag.mTag) : tag.mTag == null;

        }

        @Override
        public int hashCode() {
            return mTag != null ? mTag.hashCode() : 0;
        }
    }

    public enum TAG_MODE {
        HASH,
        USER
    }

    private final TextWatcher mEditListener = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (mLockEdit) {
                return;
            }
            if (count == 0 && start == (mTagList.size() - 1)) {
                mLockEdit = true;
                try {
                    Editable e = mTagEdit.getEditableText();
                    ImageSpan[] spans = e.getSpans(start, start + 1, ImageSpan.class);
                    for (ImageSpan span : spans) {
                        e.removeSpan(span);
                    }
                    Tag tag = mTagList.get(start);
                    mTagList.remove(start);

                    notifyTagRemoved(tag);

                } finally {
                    mLockEdit = false;
                }
            }
        }

        @Override
        public void afterTextChanged(Editable s) {
            // Prevent any pending message to be called
            mHandler.removeMessages(MESSAGE_CREATE_CHIP);
            performComputeChipsLocked(s);
        }
    };

    private Handler.Callback mTagMessenger = new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_CREATE_CHIP:
                    Editable s = mTagEdit.getEditableText();
                    s.insert(s.length(), CHIP_REPLACEMENT_CHAR);
                    break;
            }
            return false;
        }
    };

    private static final Pattern HASH_TAG_PATTERN = Pattern.compile(
            "(?<=^|(?<=[^a-zA-Z0-9-_\\\\.]))#([\\p{L}]+[\\p{L}0-9_]+)");
    private static final Pattern USER_TAG_PATTERN = Pattern.compile(
            "(?<=^|(?<=[^a-zA-Z0-9-_\\\\.]))@([\\p{L}]+[\\p{L}0-9_]+)");
    private static final Pattern NON_UNICODE_CHAR_PATTERN = Pattern.compile("[^\\p{L}0-9_#@]");

    private static final String VALID_TAGS = "#@";

    private static final String CHIP_SEPARATOR_CHAR = " ";
    private static final String CHIP_REPLACEMENT_CHAR = ".";

    private static final int MESSAGE_CREATE_CHIP = 0;

    private static final long CREATE_CHIP_LENGTH_THRESHOLD = 3L;
    private static final long CREATE_CHIP_DEFAULT_DELAYED_TIMEOUT = 1500L;

    private static float ONE_PIXEL = 0f;
    private static final Typeface CHIP_TYPEFACE = Typeface.create("Helvetica", Typeface.BOLD);
    private static final String CHIP_REMOVE_TEXT = " | x ";
    private Paint mChipBgPaint;
    private Paint mChipFgPaint;
    private int mChipRemoveAreaWidth;
    private int mChipBackgroundColor = 0;

    private TagEditText mTagEdit;
    private List<Tag> mTagList = new ArrayList<>();

    private long mTriggerTagCreationThreshold;
    private boolean mReadOnly;
    private KeyListener mEditModeKeyListener;

    private TAG_MODE mDefaultTagMode;
    private boolean mSupportsUserTags = true;

    private Handler mHandler;
    private final List<OnTagEventListener> mTagEventCallBacks = new ArrayList<>();
    private OnTagClickListener mTagClickCallBack = null;
    private final List<OnComputedTagEndedListener> mComputeTagCallbacks = new ArrayList<>();

    private boolean mLockEdit;

    public TagEditTextView(Context ctx) {
        this(ctx, null);
    }

    public TagEditTextView(Context ctx, AttributeSet attrs) {
        this(ctx, attrs, 0);
    }

    public TagEditTextView(Context ctx, AttributeSet attrs, int defStyleAttr) {
        super(ctx, attrs, defStyleAttr);
        init(ctx, attrs, defStyleAttr);
    }

    private void init(Context ctx, AttributeSet attrs, int defStyleAttr) {
        mHandler = new Handler(mTagMessenger);
        mTriggerTagCreationThreshold = CREATE_CHIP_DEFAULT_DELAYED_TIMEOUT;

        Resources.Theme theme = ctx.getTheme();
        TypedArray a = theme.obtainStyledAttributes(
                attrs, R.styleable.TagEditTextView, defStyleAttr, 0);

        mReadOnly = a.getBoolean(R.styleable.TagEditTextView_readonly, false);
        mDefaultTagMode = TAG_MODE.HASH;

        // Create the internal EditText that holds the tag logic
        mTagEdit = mReadOnly
                ? new TagEditText(ctx, attrs, defStyleAttr)
                : new TagEditText(ctx, attrs);
        mTagEdit.setLayoutParams(new LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, 0));
        mTagEdit.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        mTagEdit.addTextChangedListener(mEditListener);
        mTagEdit.setTextIsSelectable(false);
        mTagEdit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        mTagEdit.setOnFocusChangeListener((v, hasFocus) -> {
            // Remove any pending message
            mHandler.removeMessages(MESSAGE_CREATE_CHIP);
        });
        mTagEdit.setCustomSelectionActionModeCallback(new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {

            }
        });
        addView(mTagEdit);

        // Configure the window mode for landscape orientation, to disallow hide the
        // EditText control, and show characters instead of chips
        int orientation = ctx.getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (ctx instanceof Activity) {
                Window window = ((Activity) ctx).getWindow();
                if (window != null) {
                    window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
                    mTagEdit.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
                }
            }
        }

        // Save the keyListener for later restore
        mEditModeKeyListener = mTagEdit.getKeyListener();

        // Initialize resources for chips
        mChipBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        mChipFgPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        mChipFgPaint.setTextSize(mTagEdit.getTextSize() * (mReadOnly ? 1 : 0.8f));
        if (CHIP_TYPEFACE != null) {
            mChipFgPaint.setTypeface(CHIP_TYPEFACE);
        }
        mChipFgPaint.setTextAlign(Paint.Align.LEFT);

        // Calculate the width area used to remove the tag in the chip
        mChipRemoveAreaWidth = (int) (mChipFgPaint.measureText(CHIP_REMOVE_TEXT) + 0.5f);

        if (ONE_PIXEL <= 0) {
            Resources res = getResources();
            ONE_PIXEL = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 1, res.getDisplayMetrics());
        }

        int n = a.getIndexCount();
        for (int i = 0; i < n; i++) {
            int attr = a.getIndex(i);
            switch (attr) {
                case R.styleable.TagEditTextView_supportUserTags:
                    setSupportsUserTags(a.getBoolean(attr, false));
                    break;

                case R.styleable.TagEditTextView_chipBackgroundColor:
                    mChipBackgroundColor = a.getColor(attr, mChipBackgroundColor);
                    break;

                case R.styleable.TagEditTextView_chipTextColor:
                    mChipFgPaint.setColor(a.getColor(attr, Color.WHITE));
                    break;
            }
        }
        a.recycle();
    }

    public void computeTags(OnComputedTagEndedListener cb) {
        mHandler.removeMessages(MESSAGE_CREATE_CHIP);
        Editable s = mTagEdit.getEditableText();
        s = s.append(" ");
        performComputeChipsLocked(s);
    }

    private void onTagRemoveClick(final Tag tag) {
        Editable s = mTagEdit.getEditableText();
        int position = mTagList.indexOf(tag);
        mLockEdit = true;
        mTagList.remove(position);
        ImageSpan[] spans = s.getSpans(position, position + 1, ImageSpan.class);
        for (ImageSpan span : spans) {
            s.removeSpan(span);
        }
        s.delete(position, position + 1);
        mLockEdit = false;

        notifyTagRemoved(tag);
    }

    public boolean isSupportsUserTags() {
        return mSupportsUserTags;
    }

    public void setSupportsUserTags(boolean supportsUserTags) {
        mSupportsUserTags = supportsUserTags;
        mDefaultTagMode = TAG_MODE.HASH;
        if (!supportsUserTags) {
            for (Tag tag : getTags()) {
                if (tag.mTag.charAt(0) == VALID_TAGS.charAt(1)) {
                    tag.mTag = VALID_TAGS.substring(0, 1) + tag.mTag.subSequence(1, tag.mTag.length());
                }
            }
            refresh();
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        internalReadOnlyMode();
        refresh();
    }

    public boolean getReadOnlyMode() {
        return mReadOnly;
    }

    public void setReadOnlyMode(boolean readOnly) {
        mReadOnly = readOnly;
        internalReadOnlyMode();
    }

    private void internalReadOnlyMode() {
        boolean enabled = mReadOnly || isEnabled();
        mTagEdit.setCursorVisible(!enabled);
        mTagEdit.setFocusable(!enabled);
        mTagEdit.setFocusableInTouchMode(!enabled);
        mTagEdit.setKeyListener(enabled ? null : mEditModeKeyListener);
        mTagEdit.setEnabled(!enabled);
        mTagEdit.setFocusable(!enabled);
        mTagEdit.setFocusableInTouchMode(!enabled);
    }

    public TAG_MODE getDefaultTagMode() {
        return mDefaultTagMode;
    }

    public void setDefaultTagMode(TAG_MODE defaultTagMode) {
        this.mDefaultTagMode = defaultTagMode;
    }

    public long getTriggerTagCreationThreshold() {
        return mTriggerTagCreationThreshold;
    }

    public void setTriggerTagCreationThreshold(long triggerTagCreationThreshold) {
        this.mTriggerTagCreationThreshold = triggerTagCreationThreshold;
    }

    public void addTagEventListener(OnTagEventListener callback) {
        if (!mTagEventCallBacks.contains(callback)) {
            mTagEventCallBacks.add(callback);
        }
    }

    public void removeTagEventListener(OnTagEventListener callback) {
        if (mTagEventCallBacks.contains(callback)) {
            mTagEventCallBacks.remove(callback);
        }
    }

    public void setOnTagClickListener(OnTagClickListener callback) {
        mTagClickCallBack = callback;
    }

    private void refresh() {
        setTags(getTags());
    }

    public Tag[] getTags() {
        Tag[] tags = new Tag[mTagList.size()];
        int count = mTagList.size();
        for (int i = 0; i < count; i++) {
            tags[i] = mTagList.get(i).copy();
        }
        return tags;
    }

    public void setTags(Tag[] tags) {
        // Delete any existent data
        mTagEdit.getEditableText().clearSpans();
        int count = mTagList.size() - 1;
        for (int i = count; i >= 0; i--) {
            onTagRemoveClick(mTagList.get(i));
        }
        mTagEdit.setText("");

        // Filter invalid tags
        for (Tag tag : tags) {
            Matcher hashTagMatcher = HASH_TAG_PATTERN.matcher(tag.mTag);
            Matcher userTagMatcher = USER_TAG_PATTERN.matcher(tag.mTag);
            if (hashTagMatcher.matches() || (mSupportsUserTags && userTagMatcher.matches())) {
                mTagList.add(tag);
            }
        }

        // Build the spans
        SpannableStringBuilder builder;
        if (tags.length > 0) {
            final String text = String.format("%" + tags.length + "s", CHIP_SEPARATOR_CHAR)
                    .replaceAll(CHIP_SEPARATOR_CHAR, CHIP_REPLACEMENT_CHAR);
            builder = new SpannableStringBuilder(text);
        } else {
            builder = new SpannableStringBuilder("");
        }

        int pos = 0;
        for (final Tag tag : mTagList) {
            Bitmap b = createTagChip(tag);
            tag.w = b.getWidth();
            tag.h = b.getHeight();
            ImageSpan span = new ImageSpan(getContext(), b, ImageSpan.ALIGN_BOTTOM);
            builder.setSpan(span, pos, pos + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            pos++;

            notifyTagCreated(tag);
        }
        mTagEdit.setText(builder);
        mTagEdit.setSelection(mTagEdit.getText().length());
    }

    private void performComputeChipsLocked(Editable s) {
        // If we are removing skip chip creation code
        if (mLockEdit) {
            return;
        }

        // Check if we need to create a new chip
        mLockEdit = true;
        try {
            String text = s.toString();
            int textLength = text.length();
            boolean isCreateChip = false;
            boolean nextIsTag = false;
            if (textLength > 0) {
                String lastChar = text.substring(textLength - 1);
                if (lastChar.charAt(0) != VALID_TAGS.charAt(1) || mSupportsUserTags) {
                    isCreateChip = NON_UNICODE_CHAR_PATTERN.matcher(lastChar).matches();
                    nextIsTag = VALID_TAGS.contains(lastChar);
                }
            }
            if (isCreateChip || nextIsTag) {
                createChip(s, nextIsTag);
                notifyComputeTagEnded();
            } else if (mTriggerTagCreationThreshold > 0) {
                int start = mTagList.size();
                String tagText = s.subSequence(start, textLength).toString().trim();
                if (tagText.length() >= CREATE_CHIP_LENGTH_THRESHOLD) {
                    mHandler.removeMessages(MESSAGE_CREATE_CHIP);
                    mHandler.sendMessageDelayed(
                            mHandler.obtainMessage(MESSAGE_CREATE_CHIP),
                            mTriggerTagCreationThreshold);
                }
            }
        } finally {
            mLockEdit = false;
        }
    }

    private void createChip(Editable s, boolean nextIsTag) {
        int start = mTagList.size();
        int end = s.length() + (nextIsTag ? -1 : 0);
        String tagText = s.subSequence(start, end).toString().trim();
        tagText = NON_UNICODE_CHAR_PATTERN.matcher(tagText).replaceAll("");
        if (tagText.isEmpty() || tagText.length() <= 1) {
            // User is still writing
            return;
        }
        String charText = tagText.substring(0, 1);
        if (!VALID_TAGS.contains(charText) ||
                (charText.charAt(0) == VALID_TAGS.charAt(1) && !mSupportsUserTags)) {
            char tag = mDefaultTagMode == TAG_MODE.HASH
                    ? VALID_TAGS.charAt(0) : VALID_TAGS.charAt(1);
            tagText = tag + tagText;
        }

        // Replace the new tag
        s.replace(start, end, CHIP_REPLACEMENT_CHAR);

        // Create the tag and its spannable
        final Tag tag = new Tag();
        tag.mTag = NON_UNICODE_CHAR_PATTERN.matcher(tagText).replaceAll("");
        Bitmap b = createTagChip(tag);
        ImageSpan span = new ImageSpan(getContext(), b);
        s.setSpan(span, start, start + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        tag.w = b.getWidth();
        tag.h = b.getHeight();
        mTagList.add(tag);

        notifyTagCreated(tag);
    }

    private Bitmap createTagChip(Tag tag) {
        // Create the tag string (prepend/append spaces to better ux). Create a clickable
        // area for deleting the tag in non-readonly mode
        String tagText = String.format(" %s " +
                (mReadOnly || !isEnabled() ? "" : CHIP_REMOVE_TEXT), tag.mTag);

        // Create a new color for the tag if necessary
        if (tag.mColor == 0) {
            if (mChipBackgroundColor == 0) {
                tag.mColor = newRandomColor();
            } else {
                tag.mColor = mChipBackgroundColor;
            }
        }
        mChipBgPaint.setColor((isEnabled()) ? tag.mColor : Color.LTGRAY);

        // Measure the chip rect
        Rect bounds = new Rect();
        mChipFgPaint.getTextBounds(tagText, 0, tagText.length(), bounds);
        int padding = (int) ONE_PIXEL * 2;
        int w = (int) (mChipFgPaint.measureText(tagText) + (padding * 2));
        int h = bounds.height() + (padding * 4);
        float baseline = h / 2  + bounds.height() / 2;

        // Create the bitmap
        Bitmap bitmap = Bitmap.createBitmap(w + padding, h + padding, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // Draw the bitmap
        canvas.drawRoundRect(new RectF(0, (padding / 2), w, h), 6, 6, mChipBgPaint);
        canvas.drawText(tagText, (padding / 2), baseline, mChipFgPaint);
        return bitmap;
    }

    public static int newRandomColor() {
        int random = (int) (Math.floor(Math.random() * 0xff0f0f0f) + 0xff000000);
        int color = Color.argb(0xff, Color.red(random), Color.green(random), Color.blue(random));

        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] *= 0.8f; // value component
        color = Color.HSVToColor(hsv);
        return color;
    }

    private void notifyTagCreated(final Tag tag) {
        ViewCompat.postOnAnimation(this, () -> {
            Tag copy = tag.copy();
            for (OnTagEventListener cb : mTagEventCallBacks) {
                cb.onTagCreate(copy);
            }
        });

    }

    private void notifyTagRemoved(final Tag tag) {
        ViewCompat.postOnAnimation(this, () -> {
            Tag copy = tag.copy();
            for (OnTagEventListener cb : mTagEventCallBacks) {
                cb.onTagRemove(copy);
            }
        });
    }

    private void notifyComputeTagEnded() {
        ViewCompat.postOnAnimation(this, () -> {
            int count = mComputeTagCallbacks.size();
            for (int i = count - 1; i >= 0; i--) {
                mComputeTagCallbacks.get(i).onComputedTagEnded();
                mComputeTagCallbacks.remove(i);
            }
        });
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        SavedState savedState = new SavedState(super.onSaveInstanceState());
        savedState.mTags = new ArrayList<>(mTagList);
        return savedState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        //begin boilerplate code so parent classes can restore state
        if(!(state instanceof SavedState)) {
            // TODO Something got a bad state from a wrong class
            // "Wrong state class, expecting View State but received class
            // android.widget.TextView$SavedState instead. This usually happens when two views of
            // different type have the same id in the same hierarchy. This view's id
            // is id/tags_labels. Make sure other views do not use the same id."
            // Not sure where this comes from, since tags_labels is unique and
            // this class has a consistent layout, but receiving the state of a TextView.
            // For now just ensure we don't crash the app because a wrong saved state.
            try {
                super.onRestoreInstanceState(state);
            } catch (IllegalArgumentException ex) {
                // Ignore
            }
            return;
        }

        SavedState savedState = (SavedState) state;
        super.onRestoreInstanceState(savedState.getSuperState());

        mTagList = new ArrayList<>(savedState.mTags);
        refresh();
    }

    @SuppressWarnings("WeakerAccess")
    public static class SavedState extends AbsSavedState {
        public List<Tag> mTags;

        public SavedState(Parcel in, ClassLoader loader) {
            super(in, loader);
            int count = in.readInt();
            mTags = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                Tag tag = new Tag();
                tag.fromSavedState(in.readString());
                mTags.add(tag);
            }
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            int count = mTags == null ? 0 : mTags.size();
            dest.writeInt(count);
            for (int i = 0; i < count; i++) {
                dest.writeString(mTags.get(i).toSavedState());
            }
        }

        public static final Parcelable.Creator<SavedState> CREATOR
                = ParcelableCompat.newCreator(new ParcelableCompatCreatorCallbacks<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel parcel, ClassLoader loader) {
                return new SavedState(parcel, loader);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        });
    }
}
