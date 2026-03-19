/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.settings.accessibility;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.provider.Settings;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.widget.FocusIndicatorDrawable;
import com.android.settingslib.widget.SettingsPreferenceGroupAdapter;
import com.android.settingslib.widget.SettingsThemeHelper;

/**
 * PreferenceAdapterInSuw is a temporary fix on the padding issue introduced in the expressive style
 * in SettingsPreferenceGroupAdapter. This adapter should be removed once the padding issue is
 * resolved in the SettingsLib.
 *
 * <p>TODO(b/403645956): Remove this adapter
 */
public class PreferenceAdapterInSuw extends SettingsPreferenceGroupAdapter {

    private static final int FOCUS_INDICATOR_CORNER_RADIUS_DP = 16;
    private static final int FOCUS_INDICATOR_HORIZONTAL_PADDING_ADJUSTMENT_DP = 45;
    private static final int FOCUS_INDICATOR_VERTICAL_PADDING_ADJUSTMENT_DP = -3;

    private final int mListPreferredItemPaddingStart;
    private final int mListPreferredItemPaddingEnd;
    private final int mContentPadding;

    public PreferenceAdapterInSuw(@NonNull PreferenceGroup preferenceGroup) {
        super(preferenceGroup);
        TypedArray resolvedAttributes =
                preferenceGroup
                        .getContext()
                        .obtainStyledAttributes(
                                new int[] {
                                    android.R.attr.listPreferredItemPaddingStart,
                                    android.R.attr.listPreferredItemPaddingEnd
                                });
        mListPreferredItemPaddingStart = resolvedAttributes.getDimensionPixelSize(0, 0);
        mListPreferredItemPaddingEnd = resolvedAttributes.getDimensionPixelSize(1, 0);
        resolvedAttributes.recycle();
        mContentPadding =
                preferenceGroup
                        .getContext()
                        .getResources()
                        .getDimensionPixelSize(
                                com.android.settingslib.widget.theme.R.dimen
                                        .settingslib_expressive_space_small1);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        View view = holder.itemView;
        if (Flags.enableInsetFocusRingsInSuwReadOnly()) {
            Context context = view.getContext();
            if (context != null
                    && context.getResources()
                            .getBoolean(
                                    com.android.internal.R.bool.config_enableInsetFocusRingsInSuw)
                    && Settings.Global.getInt(
                                    context.getContentResolver(),
                                    Settings.Global.DEVICE_PROVISIONED,
                                    0)
                            == 0) {
                // These changes add the focus ring indicator to rows and options across the
                // top-level Vision Settings page and most subpages.
                FocusIndicatorDrawable.Builder builder =
                        new FocusIndicatorDrawable.Builder(context);
                EffectivePositionInfo info = calculateEffectivePositionAndCount(position);

                if (info.mPosition != -1) {
                    configureFocusIndicator(builder, info.mPosition, info.mCount);
                } else {
                    // Default to rounding all corners if the item is not part of a selectable
                    // block.
                    builder.withCornerRadius(16);
                }

                Drawable focusDrawable = builder.build();
                view.setForeground(focusDrawable);
            }
        }

        int paddingTop = view.getPaddingTop();
        int paddingBottom = view.getPaddingBottom();

        if (shouldApplyPadding(view.getContext(), position)) {
            view.setPaddingRelative(
                    mListPreferredItemPaddingStart + mContentPadding,
                    paddingTop,
                    mListPreferredItemPaddingEnd + mContentPadding,
                    paddingBottom);
        } else {
            view.setPaddingRelative(
                    mListPreferredItemPaddingStart,
                    paddingTop,
                    mListPreferredItemPaddingEnd,
                    paddingBottom);
        }
    }

    /**
     * Configures the {@link FocusIndicatorDrawable.Builder} for a given item. Subclasses can
     * override this to provide custom focus ring appearances.
     *
     * @param builder The builder to configure.
     * @param position The adapter position of the item.
     * @param count The number of items in the adapter.
     */
    protected void configureFocusIndicator(
            @NonNull FocusIndicatorDrawable.Builder builder,
            int position,
            int count) {
        builder.withHorizontalPaddingAdjustment(FOCUS_INDICATOR_HORIZONTAL_PADDING_ADJUSTMENT_DP)
                .withVerticalPaddingAdjustment(FOCUS_INDICATOR_VERTICAL_PADDING_ADJUSTMENT_DP)
                .withCornerRadius(FOCUS_INDICATOR_CORNER_RADIUS_DP)
                .withPositionalCornerRadii(position, count);
    }

    private boolean shouldApplyPadding(@NonNull Context context, int position) {
        if (SettingsThemeHelper.isExpressiveTheme(context)) {
            return getRoundCornerDrawableRes(position, /* isSelected= */ false) != 0;
        }
        return false;
    }

    /**
     * A data class to hold information about an item's position within a contiguous block of
     * selectable preferences. This is used to apply correct positional styling, such as rounding
     * the corners of the first and last items in the block.
     */
    private static class EffectivePositionInfo {
        /**
         * The 0-indexed mPosition of the item within its contiguous block of selectable items. If
         * the original item is not selectable, this will be {@code -1}.
         */
        final int mPosition;

        /**
         * The total number of selectable items in the contiguous block. If the original item is not
         * selectable, this will be {@code 0}.
         */
        final int mCount;

        EffectivePositionInfo(int position, int mCount) {
            this.mPosition = position;
            this.mCount = mCount;
        }
    }

    /**
     * Calculates the "effective" position and count for an item within a contiguous block of
     * selectable preferences. This allows for correct styling of items that are visually grouped,
     * such as applying rounded corners to only the top and bottom items of the group.
     *
     * <p>For example, in a list with [Unselectable, Selectable, Selectable, Unselectable], an item
     * at adapter position 2 (the second selectable one) would have an effective position of 1 and
     * an effective count of 2.
     *
     * @param currentPosition The adapter position of the item to check.
     * @return An {@link EffectivePositionInfo} containing the item's relative position and the
     *     total count of the block. If the item at {@code currentPosition} is not selectable, the
     *     returned position will be -1 and the count will be 0.
     */
    private EffectivePositionInfo calculateEffectivePositionAndCount(int currentPosition) {
        if (!getItem(currentPosition).isSelectable()) {
            return new EffectivePositionInfo(-1, 0);
        }

        // Find the start of the contiguous block of selectable items by searching backwards.
        int blockStartIndex = currentPosition;
        while (blockStartIndex > 0 && getItem(blockStartIndex - 1).isSelectable()) {
            blockStartIndex--;
        }

        // Find the end of the contiguous block of selectable items by searching forwards.
        int blockEndIndex = currentPosition;
        while (blockEndIndex < getItemCount() - 1 && getItem(blockEndIndex + 1).isSelectable()) {
            blockEndIndex++;
        }

        int effectiveCount = (blockEndIndex - blockStartIndex) + 1;
        int effectivePosition = currentPosition - blockStartIndex;

        return new EffectivePositionInfo(effectivePosition, effectiveCount);
    }
}
