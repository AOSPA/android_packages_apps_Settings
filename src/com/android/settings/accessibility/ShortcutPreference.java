/*
 * Copyright (C) 2019 The Android Open Source Project
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
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settings.accessibility.shared.utils.SetupWizardUtilKt;
import com.android.settings.widget.FocusIndicatorDrawable;
import com.android.settingslib.widget.SettingsThemeHelper;
import com.android.settingslib.widget.TwoTargetPreference;

/**
 * Preference that can enable accessibility shortcut and let users choose which shortcut type they
 * prefer to use.
 */
public class ShortcutPreference extends TwoTargetPreference {

    private static final int HOLDER_FOCUS_INDICATOR_HORIZONTAL_PADDING_ADJUSTMENT_DP = -18;
    private static final int HOLDER_FOCUS_INDICATOR_VERTICAL_PADDING_ADJUSTMENT_DP = -4;
    private static final int HOLDER_FOCUS_INDICATOR_CORNER_RADIUS_DP = 16;
    private static final int TOGGLE_FOCUS_INDICATOR_HORIZONTAL_PADDING_ADJUSTMENT_DP = -4;
    private static final int TOGGLE_FOCUS_INDICATOR_VERTICAL_PADDING_ADJUSTMENT_DP = 5;
    private static final int TOGGLE_FOCUS_INDICATOR_CORNER_RADIUS_DP = 999; // Fully rounded.

    /**
     * Interface definition for a callback to be invoked when the toggle or settings has been
     * clicked.
     */
    public interface OnClickCallback {
        /**
         * Called when the settings view has been clicked.
         *
         * @param preference The clicked preference
         */
        void onSettingsClicked(ShortcutPreference preference);

        /**
         * Called when the toggle in ShortcutPreference has been clicked.
         *
         * @param preference The clicked preference
         */
        void onToggleClicked(ShortcutPreference preference);
    }

    private OnClickCallback mClickCallback = null;
    private boolean mChecked = false;
    private boolean mSettingsEditable = true;

    public ShortcutPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setIconSpaceReserved(false);
        // Treat onSettingsClicked as this preference's click.
        setOnPreferenceClickListener(preference -> {
            callOnSettingsClicked();
            return true;
        });
    }

    @Override
    protected int getSecondTargetResId() {
        return SettingsThemeHelper.isExpressiveTheme(getContext())
                ? com.android.settingslib.widget.theme.R.layout
                        .settingslib_expressive_preference_switch
                : androidx.preference.R.layout.preference_widget_switch_compat;
    }

    @VisibleForTesting
    public int getSwitchResId() {
        return SettingsThemeHelper.isExpressiveTheme(getContext())
                ? com.android.settingslib.widget.theme.R.id.switchWidget
                : androidx.preference.R.id.switchWidget;
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        if (SetupWizardUtilKt.shouldShowFocusRingsInSuw(getContext())) {
            holder.itemView.setForeground(
                    new FocusIndicatorDrawable.Builder(getContext())
                            .withHorizontalPaddingAdjustment(
                                    HOLDER_FOCUS_INDICATOR_HORIZONTAL_PADDING_ADJUSTMENT_DP)
                            .withVerticalPaddingAdjustment(
                                    HOLDER_FOCUS_INDICATOR_VERTICAL_PADDING_ADJUSTMENT_DP)
                            .withCornerRadius(HOLDER_FOCUS_INDICATOR_CORNER_RADIUS_DP)
                            .build());
        }

        final View widgetFrame = holder.findViewById(android.R.id.widget_frame);
        if (widgetFrame instanceof LinearLayout linearLayout) {
            linearLayout.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        }

        CompoundButton switchWidget = holder.itemView.findViewById(getSwitchResId());
        if (switchWidget != null) {
            // Consumes move events to ignore drag actions.
            switchWidget.setOnTouchListener((v, event) -> {
                return event.getActionMasked() == MotionEvent.ACTION_MOVE;
            });
            switchWidget.setContentDescription(
                    getContext().getText(R.string.accessibility_shortcut_settings));
            switchWidget.setChecked(mChecked);
            switchWidget.setOnClickListener(view -> callOnToggleClicked());
            switchWidget.setClickable(mSettingsEditable);
            switchWidget.setFocusable(mSettingsEditable);

            if (SetupWizardUtilKt.shouldShowFocusRingsInSuw(getContext())) {
                // This change adds the focus ring indicator to the toggle within the shortcut
                // toggle button row.
                Drawable focusDrawable =
                        new FocusIndicatorDrawable.Builder(getContext())
                                .withHorizontalPaddingAdjustment(
                                        TOGGLE_FOCUS_INDICATOR_HORIZONTAL_PADDING_ADJUSTMENT_DP)
                                .withVerticalPaddingAdjustment(
                                        TOGGLE_FOCUS_INDICATOR_VERTICAL_PADDING_ADJUSTMENT_DP)
                                .withCornerRadius(TOGGLE_FOCUS_INDICATOR_CORNER_RADIUS_DP)
                                .withColorRes(
                                        mChecked
                                                ? com.android.internal.R.color
                                                        .materialColorOnPrimary
                                                : com.android.internal.R.color.materialColorPrimary)
                                .build();
                switchWidget.setForeground(focusDrawable);
            }

        }

        final View divider = holder.itemView.findViewById(
                com.android.settingslib.widget.preference.twotarget.R.id.two_target_divider);
        if (divider != null) {
            divider.setVisibility(mSettingsEditable ? View.VISIBLE : View.GONE);
        }

        holder.itemView.setOnClickListener(view -> {
            if (mSettingsEditable) {
                callOnSettingsClicked();
            } else {
                callOnToggleClicked();
            }
        });
    }

    /**
     * Sets the shortcut toggle according to checked value.
     *
     * @param checked the state value of shortcut toggle
     */
    public void setChecked(boolean checked) {
        if (mChecked != checked) {
            mChecked = checked;
            notifyChanged();
        }
    }

    /**
     * Gets the checked value of shortcut toggle.
     *
     * @return the checked value of shortcut toggle
     */
    public boolean isChecked() {
        return mChecked;
    }

    /**
     * Sets the editable state of Settings view. If the view cannot edited, it makes the settings
     * and toggle be not touchable. The main ui handles touch event directly by {@link #onClick}.
     */
    public void setSettingsEditable(boolean enabled) {
        if (mSettingsEditable != enabled) {
            mSettingsEditable = enabled;
            notifyChanged();
        }
    }

    public boolean isSettingsEditable() {
        return mSettingsEditable;
    }

    /**
     * Sets the callback to be invoked when this preference is clicked by the user.
     *
     * @param callback the callback to be invoked
     */
    public void setOnClickCallback(OnClickCallback callback) {
        mClickCallback = callback;
    }

    private void callOnSettingsClicked() {
        if (mClickCallback != null) {
            mClickCallback.onSettingsClicked(this);
        }
    }

    private void callOnToggleClicked() {
        setChecked(!mChecked);
        if (mClickCallback != null) {
            mClickCallback.onToggleClicked(this);
        }
    }
}
