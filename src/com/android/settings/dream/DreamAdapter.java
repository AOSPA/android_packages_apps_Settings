/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.dream;

import static android.service.dreams.Flags.dreamsSwitcher;
import static android.service.dreams.Flags.dreamsV2;

import android.annotation.LayoutRes;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.icu.text.MessageFormat;
import android.text.TextUtils;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;
import com.android.settingslib.utils.ColorUtil;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter which displays list of items for the user to select.
 */
public class DreamAdapter<DreamItemT extends IDreamItem>
        extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final List<DreamItemT> mItemList;
    private final MessageFormat mOrdinalFormat =
        new MessageFormat("{0,ordinal}", Locale.getDefault());
    private final boolean mAllowMultiSelection;
    private int mLastSelectedPos = -1;
    private boolean mEnabled = true;
    private SparseIntArray mLayouts = new SparseIntArray();

    /**
     * View holder for each {@link IDreamItem}.
     */
    private class DreamViewHolder extends RecyclerView.ViewHolder {
        private final TextView mTitleView;
        private final TextView mSummaryView;
        private final ImageView mPreviewView;
        private final ImageView mPreviewPlaceholderView;
        private final Context mContext;
        private final float mDisabledAlphaValue;

        DreamViewHolder(View view, Context context) {
            super(view);
            mContext = context;
            mPreviewView = view.findViewById(R.id.preview);
            mPreviewPlaceholderView = view.findViewById(R.id.preview_placeholder);
            mTitleView = view.findViewById(R.id.title_text);
            mSummaryView = view.findViewById(R.id.summary_text);
            mDisabledAlphaValue = ColorUtil.getDisabledAlpha(context);
        }

        /**
         * Bind the view at the given position, populating the view with the provided data.
         */
        public void bindView(IDreamItem item, int position) {
            mTitleView.setText(item.getTitle());

            final CharSequence summary = item.getSummary();
            if (TextUtils.isEmpty(summary)) {
                mSummaryView.setVisibility(View.GONE);
            } else {
                mSummaryView.setText(summary);
                mSummaryView.setVisibility(View.VISIBLE);
            }

            final Drawable icon;
            if (supportMultipleSelection() && item.getOrder() >= 0) {
                icon = new NumberedIconDrawable(
                    mContext, item.getOrder() + 1, R.color.dream_card_color_state_list);
            } else {
                icon = item.isActive()
                        ? mContext.getDrawable(R.drawable.ic_dream_check_circle)
                        : item.getIcon().mutate();
            }
            final int iconSize = mContext.getResources().getDimensionPixelSize(
                    R.dimen.dream_item_icon_size);
            icon.setBounds(0, 0, iconSize, iconSize);
            mTitleView.setCompoundDrawablesRelative(icon, null, null, null);

            if (dreamsV2()) {
                mTitleView.setEllipsize(TextUtils.TruncateAt.MARQUEE);
                mTitleView.setSingleLine();

                final int drawablePadding = mContext.getResources().getDimensionPixelSize(
                        R.dimen.dream_item_icon_padding_new);
                mTitleView.setCompoundDrawablePadding(drawablePadding);
            }

            itemView.setOnClickListener(v -> {
                item.onItemClicked();
                if (!supportMultipleSelection()) {
                    if (mLastSelectedPos > -1 && mLastSelectedPos != position) {
                        notifyItemChanged(mLastSelectedPos);
                    }
                    notifyItemChanged(position);
                }
            });

            final boolean isActive = item.isActive();
            itemView.setSelected(isActive);
            if (supportMultipleSelection()) {
                ViewCompat.setAccessibilityDelegate(itemView, new AccessibilityDelegateCompat() {
                    @Override
                    public void onInitializeAccessibilityNodeInfo(View host,
                            AccessibilityNodeInfoCompat info) {
                        super.onInitializeAccessibilityNodeInfo(host, info);
                        info.setCheckable(true);
                        info.setChecked(isActive);
                        // To avoid Talkback announcing "selected, checked...", we unset
                        // selected here for the accessibility service. The view itself is
                        // still selected to reflect the correct visual state.
                        info.setSelected(false);
                    }
                });
                itemView.setClickable(true);
                if (item.getOrder() >= 0) {
                    String ordinal = mOrdinalFormat.format(new Object[]{item.getOrder() + 1});
                    itemView.setContentDescription(item.getTitle() + ", " + ordinal);
                } else {
                    itemView.setContentDescription(item.getTitle());
                }
            } else {
                if (isActive) {
                    mLastSelectedPos = position;
                }
                itemView.setClickable(!isActive);
            }

            if (item.viewType() != DreamItemViewTypes.NO_DREAM_ITEM) {
                final Drawable previewImage = item.getPreviewImage();
                if (previewImage != null) {
                    mPreviewView.setImageDrawable(previewImage);
                    mPreviewView.setClipToOutline(true);
                    mPreviewPlaceholderView.setVisibility(View.GONE);
                } else {
                    mPreviewView.setImageDrawable(null);
                    mPreviewPlaceholderView.setVisibility(View.VISIBLE);
                }

                configureButtons(item);
                setEnabledStateOnViews(itemView, mEnabled);
            }
        }

        private void configureButtons(IDreamItem item) {
            if (dreamsV2()) {
                configureNewPreviewButton(item);
            }

            configureCustomizeButton(item);
        }

        private void configureNewPreviewButton(IDreamItem item) {
            final View previewButton = itemView.findViewById(R.id.preview_button);
            if (previewButton == null) {
                // Do nothing if the button is null (it can be null when the grid of dreams is
                // presented during dock setup; see b/412208020 for more info).
                return;
            }

            // Show preview button if dream is active and enabled
            previewButton.setVisibility(
                    item.isActive() && mEnabled ? View.VISIBLE : View.GONE);
            previewButton.setSelected(false);
            previewButton.setOnClickListener(v -> item.onPreviewClicked());
            previewButton.setContentDescription(
                    mContext.getString(R.string.dream_preview_button_description, item.getTitle()));
        }

        private void configureCustomizeButton(IDreamItem item) {
            final int customizeButtonResId =
                    dreamsV2() ? R.id.customize_button_new : R.id.customize_button;
            final Button customizeButton = itemView.findViewById(customizeButtonResId);
            if (customizeButton == null) {
                // Do nothing if the button is null (it can be null when the grid of dreams is
                // presented during dock setup; see b/412208020 for more info).
                return;
            }

            if (dreamsV2()) {
                customizeButton.setEllipsize(TextUtils.TruncateAt.MARQUEE);
                customizeButton.setSingleLine();
                customizeButton.setSelected(true);
                ViewCompat.setAccessibilityDelegate(customizeButton,
                        new AccessibilityDelegateCompat() {
                            @Override
                            public void onInitializeAccessibilityNodeInfo(View host,
                                    AccessibilityNodeInfoCompat info) {
                                super.onInitializeAccessibilityNodeInfo(host, info);
                                info.setSelected(false);
                            }
                        });
            } else {
                customizeButton.setSelected(false);
                ViewCompat.setAccessibilityDelegate(customizeButton, null);
            }
            customizeButton.setVisibility(
                    item.allowCustomization() && mEnabled ? View.VISIBLE : View.GONE);

            customizeButton.setOnClickListener(v -> item.onCustomizeClicked());
            customizeButton.setContentDescription(
                    mContext.getResources().getString(R.string.customize_button_description,
                            item.getTitle()));
        }

        /**
         * Makes sure the view (and any children) get the enabled state changed.
         */
        private void setEnabledStateOnViews(@NonNull View v, boolean enabled) {
            v.setEnabled(enabled);

            if (v instanceof ViewGroup) {
                final ViewGroup vg = (ViewGroup) v;
                for (int i = vg.getChildCount() - 1; i >= 0; i--) {
                    setEnabledStateOnViews(vg.getChildAt(i), enabled);
                }
            } else {
                v.setAlpha(enabled ? 1 : mDisabledAlphaValue);
            }
        }
    }

    public DreamAdapter(SparseIntArray layouts, List<DreamItemT> itemList) {
        this(layouts, itemList, /* allowMultiSelection= */ false);
    }

    public DreamAdapter(
            SparseIntArray layouts, List<DreamItemT> itemList, boolean allowMultiSelection) {
        mItemList = itemList;
        mLayouts = layouts;
        mAllowMultiSelection = allowMultiSelection;
    }

    public DreamAdapter(@LayoutRes int layoutRes, List<DreamItemT> itemList) {
        this(layoutRes, itemList, /* allowMultiSelection= */ false);
    }

    public DreamAdapter(
            @LayoutRes int layoutRes, List<DreamItemT> itemList, boolean allowMultiSelection) {
        mItemList = itemList;
        mLayouts.append(DreamItemViewTypes.DREAM_ITEM, layoutRes);
        mAllowMultiSelection = allowMultiSelection;
    }

    void setItemList(List<DreamItemT> itemList) {
        mItemList.clear();
        mItemList.addAll(itemList);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup,
                                                      @DreamItemViewTypes.ViewType int viewType) {
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(mLayouts.get(viewType), viewGroup, false);
        return new DreamViewHolder(view, viewGroup.getContext());
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int i) {
        ((DreamViewHolder) viewHolder).bindView(mItemList.get(i), i);
    }

    @Override
    public @DreamItemViewTypes.ViewType int getItemViewType(int position) {
        return mItemList.get(position).viewType();
    }

    @Override
    public int getItemCount() {
        return mItemList.size();
    }

    private boolean supportMultipleSelection() {
        return dreamsSwitcher() && mAllowMultiSelection;
    }

    /**
     * Sets the enabled state of all items.
     */
    public void setEnabled(boolean enabled) {
        if (mEnabled != enabled) {
            mEnabled = enabled;
            notifyDataSetChanged();
        }
    }

    /**
     * Gets the enabled state of all items.
     */
    public boolean getEnabled() {
        return mEnabled;
    }

    /**
     * Rotates the items in the list.
     *
     * @param fromPos The position to start rotating from.
     * @param toPos The position to rotate to.
     */
    public void rotateItems(int fromPos, int toPos) {
        if (fromPos == toPos) {
            return;
        }

        if (fromPos < toPos) {
            // Move down, rotate other items up.
            List<DreamItemT> subList = mItemList.subList(fromPos, toPos + 1);
            Collections.rotate(subList, -1);
        } else {
            // Move up, rotate other items down.
            List<DreamItemT> subList = mItemList.subList(toPos, fromPos + 1);
            Collections.rotate(subList, 1);
        }
        notifyItemMoved(fromPos, toPos);
    }

    List<DreamItemT> getItems() {
        return mItemList;
    }
}
