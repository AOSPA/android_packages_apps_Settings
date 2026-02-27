/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.bluetooth;

import static android.bluetooth.AudioInputControl.MUTE_DISABLED;
import static android.bluetooth.AudioInputControl.MUTE_MUTED;
import static android.bluetooth.AudioInputControl.MUTE_NOT_MUTED;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import static com.android.settings.bluetooth.BluetoothDetailsAmbientVolumePreferenceController.KEY_AMBIENT_VOLUME_SLIDER;
import static com.android.settingslib.bluetooth.HearingAidInfo.DeviceSide.SIDE_LEFT;
import static com.android.settingslib.bluetooth.HearingAidInfo.DeviceSide.SIDE_RIGHT;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.bluetooth.hearingdevices.ui.AmbientVolumeUi;
import com.android.settingslib.widget.Expandable;
import com.android.settingslib.widget.SettingsThemeHelper;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.primitives.Ints;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A preference group of ambient volume controls.
 *
 * <p> It consists of a header with an expand icon and volume sliders for unified control and
 * separated control for devices in the same set. Toggle the expand icon will make the UI switch
 * between unified and separated control.
 */
public class AmbientVolumePreference extends PreferenceGroup implements AmbientVolumeUi,
        Expandable {

    private static final int ORDER_AMBIENT_VOLUME_CONTROL_UNIFIED = 0;
    private static final int ORDER_AMBIENT_VOLUME_CONTROL_SEPARATED = 1;

    private static final String METRIC_KEY_AMBIENT_SLIDER = "ambient_slider";
    private static final String METRIC_KEY_AMBIENT_MUTE = "ambient_mute";
    private static final String METRIC_KEY_AMBIENT_EXPAND = "ambient_expand";

    @Nullable
    private AmbientVolumeUiListener mListener;
    @Nullable
    private View mExpandIcon;
    @Nullable
    private ImageView mVolumeIcon;

    private final BiMap<Integer, AmbientVolumeSliderPreference> mSideToSliderMap =
            HashBiMap.create();
    private boolean mExpandable = true;
    private boolean mExpanded = false;
    private int mMetricsCategory;

    private final OnPreferenceChangeListener mPreferenceChangeListener =
            (slider, v) -> {
                if (slider instanceof AmbientVolumeSliderPreference
                        && v instanceof final Integer value) {
                    final Integer side = mSideToSliderMap.inverse().get(slider);
                    if (side != null) {
                        logMetrics(METRIC_KEY_AMBIENT_SLIDER, side);
                        if (mListener != null) {
                            mListener.onSliderValueChange(side, value);
                        }
                    }
                    return true;
                }
                return false;
            };

    public AmbientVolumePreference(@NonNull Context context) {
        super(context, null);
        if (SettingsThemeHelper.isExpressiveTheme(context)) {
            setLayoutResource(com.android.settingslib.widget.theme
                    .R.layout.settingslib_expressive_preference);
            setWidgetLayoutResource(com.android.settingslib.widget.preference.expandable
                    .R.layout.settingslib_widget_expandable_icon);
        } else {
            setLayoutResource(R.layout.preference_ambient_volume);
        }
        setIcon(com.android.settingslib.R.drawable.ic_ambient_volume);
        setTitle(R.string.bluetooth_ambient_volume_control);
        setSelectable(false);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        holder.setDividerAllowedAbove(false);
        holder.setDividerAllowedBelow(false);

        mVolumeIcon = holder.itemView.requireViewById(com.android.internal.R.id.icon);
        mVolumeIcon.getDrawable().mutate().setTint(getContext().getColor(
                com.android.internal.R.color.materialColorOnSurface));
        updateVolumeIcon();

        mExpandIcon = holder.itemView.requireViewById(R.id.expand_icon);
        View expandIconView = holder.itemView.findViewById(com.android.internal.R.id.widget_frame);
        if (expandIconView == null) {
            expandIconView = mExpandIcon;
        }
        expandIconView.setOnClickListener(v -> {
            if (!isControlExpandable()) {
                return;
            }
            setControlExpanded(!mExpanded);
            logMetrics(METRIC_KEY_AMBIENT_EXPAND, mExpanded ? 1 : 0);
            if (mListener != null) {
                mListener.onExpandIconClick();
            }
        });
        updateExpandIcon();
    }

    @Override
    public void setControlExpandable(boolean expandable) {
        if (mExpandable != expandable) {
            mExpandable = expandable;
            if (!mExpandable) {
                setControlExpanded(false);
            }
            updateExpandIcon();
        }
    }

    @Override
    public boolean isControlExpandable() {
        return mExpandable;
    }

    @Override
    public void setControlExpanded(boolean expanded) {
        if (mExpanded != expanded) {
            mExpanded = expanded;
            updateExpandIcon();
            updateLayout();
        }
    }

    @Override
    public boolean isControlExpanded() {
        return mExpanded;
    }

    @Override
    public void setSliderMuteState(int side, int muteState) {
        AmbientVolumeSliderPreference slider = mSideToSliderMap.get(side);
        if (slider != null) {
            slider.setMuteState(muteState);
            updateMuteStateFromSide(side);
        }
    }

    @Override
    public int getSliderMuteState(int side) {
        AmbientVolumeSliderPreference slider = mSideToSliderMap.get(side);
        return slider == null ? MUTE_DISABLED : slider.getMuteState();
    }

    @Override
    public void setListener(@Nullable AmbientVolumeUiListener listener) {
        mListener = listener;
    }

    @Override
    public void setupSliders(@NonNull Set<Integer> sides) {
        sides.forEach(side -> createSlider(side, ORDER_AMBIENT_VOLUME_CONTROL_SEPARATED + side));
        createSlider(SIDE_UNIFIED, ORDER_AMBIENT_VOLUME_CONTROL_UNIFIED);

        if (!mSideToSliderMap.isEmpty()) {
            for (int side : VALID_SIDES) {
                final AmbientVolumeSliderPreference slider = mSideToSliderMap.get(side);
                if (slider != null && findPreference(slider.getKey()) == null) {
                    addPreference(slider);
                }
            }
        }
        updateLayout();
    }

    @Override
    public void setSliderEnabled(int side, boolean enabled) {
        AmbientVolumeSliderPreference slider = mSideToSliderMap.get(side);
        if (slider != null) {
            slider.setEnabled(enabled);
            updateVolumeIcon();
        }
    }

    @Override
    public void setSliderValue(int side, int value) {
        AmbientVolumeSliderPreference slider = mSideToSliderMap.get(side);
        if (slider != null && slider.getValue() != value && slider.getMin() <= value
                && slider.getMax() >= value) {
            slider.setValue(value);
            updateVolumeIcon();
        }
    }

    @Override
    public void setSliderRange(int side, int min, int max) {
        AmbientVolumeSliderPreference slider = mSideToSliderMap.get(side);
        if (slider != null) {
            slider.setMin(min);
            slider.setMax(max);
        }
    }

    /** Sets the metrics category. */
    public void setMetricsCategory(int category) {
        mMetricsCategory = category;
    }

    private void updateLayout() {
        mSideToSliderMap.forEach((side, slider) -> {
            if (side == SIDE_UNIFIED) {
                slider.setVisible(!mExpanded);
            } else {
                slider.setVisible(mExpanded);
            }
        });
        updateVolumeIcon();
    }

    private void updateVolumeIcon() {
        int leftLevel, rightLevel;
        if (isControlExpanded()) {
            AmbientVolumeSliderPreference leftSlider = mSideToSliderMap.get(SIDE_LEFT);
            AmbientVolumeSliderPreference rightSlider = mSideToSliderMap.get(SIDE_RIGHT);
            leftLevel = leftSlider == null ? 0 : leftSlider.getVolumeLevel();
            rightLevel = rightSlider == null ? 0 : rightSlider.getVolumeLevel();
        } else {
            AmbientVolumeSliderPreference unifiedSlider = mSideToSliderMap.get(SIDE_UNIFIED);
            final int unifiedLevel = unifiedSlider == null ? 0 : unifiedSlider.getVolumeLevel();
            leftLevel = unifiedLevel;
            rightLevel = unifiedLevel;
        }
        int volumeLevel = Ints.constrainToRange(
                leftLevel * AMBIENT_VOLUME_LEVEL_NUMBER + rightLevel,
                AMBIENT_VOLUME_LEVEL_MIN,
                AMBIENT_VOLUME_LEVEL_MAX);
        if (mVolumeIcon != null) {
            mVolumeIcon.setImageLevel(volumeLevel);
        }
    }

    private void updateExpandIcon() {
        if (mExpandIcon == null) {
            return;
        }
        mExpandIcon.setVisibility(isControlExpandable() ? VISIBLE : GONE);
        mExpandIcon.setRotation(isControlExpanded() ? ROTATION_EXPANDED : ROTATION_COLLAPSED);
        if (isControlExpandable()) {
            final int stringRes = isControlExpanded()
                    ? R.string.bluetooth_ambient_volume_control_collapse
                    : R.string.bluetooth_ambient_volume_control_expand;
            mExpandIcon.setContentDescription(getContext().getString(stringRes));
        } else {
            mExpandIcon.setContentDescription(null);
        }
    }

    private void updateMuteStateFromSide(int side) {
        if (side == SIDE_UNIFIED) {
            // propagate the mute state to all other sliders
            mSideToSliderMap.forEach((entrySide, entrySlider) -> {
                if (entrySide != SIDE_UNIFIED) {
                    entrySlider.setMuteState(getSliderMuteState(SIDE_UNIFIED));
                }
            });
        } else {
            AmbientVolumeSliderPreference unifiedSlider = mSideToSliderMap.get(SIDE_UNIFIED);
            if (unifiedSlider != null) {
                List<AmbientVolumeSliderPreference> sideSliders =
                        mSideToSliderMap.entrySet().stream()
                                .filter(entry -> entry.getKey() != SIDE_UNIFIED)
                                .map(Map.Entry::getValue)
                                .toList();
                if (sideSliders.stream().anyMatch(s -> s.getMuteState() == MUTE_NOT_MUTED)) {
                    unifiedSlider.setMuteState(MUTE_NOT_MUTED);
                } else if (sideSliders.stream().allMatch(s -> s.getMuteState() == MUTE_DISABLED)) {
                    unifiedSlider.setMuteState(MUTE_DISABLED);
                } else {
                    unifiedSlider.setMuteState(MUTE_MUTED);
                }
            }
        }
        updateVolumeIcon();
    }

    private void createSlider(int side, int order) {
        if (mSideToSliderMap.containsKey(side)) {
            return;
        }
        int titleResId = 0;
        int contentResId;
        if (side == SIDE_LEFT) {
            titleResId = R.string.bluetooth_ambient_volume_control_left;
            contentResId = R.string.bluetooth_ambient_volume_control_left_description;
        } else if (side == SIDE_RIGHT) {
            titleResId = R.string.bluetooth_ambient_volume_control_right;
            contentResId = R.string.bluetooth_ambient_volume_control_right_description;
        } else {
            contentResId = R.string.bluetooth_ambient_volume_control_description;
        }
        String title = titleResId == 0 ? null : getContext().getString(titleResId);
        String content = contentResId == 0 ? null : getContext().getString(contentResId);

        AmbientVolumeSliderPreference slider = new AmbientVolumeSliderPreference(getContext());
        slider.setKey(KEY_AMBIENT_VOLUME_SLIDER + "_" + side);
        slider.setOrder(order);
        slider.setTitle(title);
        slider.setSliderContentDescription(content);
        slider.setOnPreferenceChangeListener(mPreferenceChangeListener);
        slider.setOnMuteIconClickListener((v) -> {
            updateMuteStateFromSide(side);

            boolean muted = slider.getMuteState() == MUTE_MUTED;
            logMetrics(METRIC_KEY_AMBIENT_MUTE, muted ? 1 : 0);
            if (mListener != null) {
                mListener.onSliderMuteChange(side, muted);
            }
        });
        mSideToSliderMap.put(side, slider);
    }

    private void logMetrics(String key, int value) {
        FeatureFactory.getFeatureFactory().getMetricsFeatureProvider().changed(
                mMetricsCategory, key, value);
    }

    @Override
    public boolean isExpanded() {
        // isExpanded() is different from isControlExpanded(), this is at the point of view if a
        // preference group shows any of its child preference.
        // Should always return true for AmbientVolumePreference as it always shows at least one
        // child preference no matter in collapsed or expanded mode.
        return true;
    }

    // No-op, as this preference is always expanded. See isExpanded().
    @Override
    public void setExpanded(boolean expanded) {}

    @VisibleForTesting
    Map<Integer, AmbientVolumeSliderPreference> getSliders() {
        return mSideToSliderMap;
    }
}
