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

import static android.view.accessibility.AccessibilityManager.AUTOCLICK_DELAY_WITH_INDICATOR_DEFAULT;

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

import com.google.common.collect.ImmutableBiMap;

/**
 * Fragment for creating a dialog in autoclick settings.
 */
public class AutoclickDelayDialogFragment extends InstrumentedDialogFragment {

    private static final String TAG = AutoclickDelayDialogFragment.class.getSimpleName();

    public final ImmutableBiMap<Integer, Integer> RADIO_BUTTON_ID_TO_DELAY_TIME =
            new ImmutableBiMap.Builder<Integer, Integer>()
                .put(R.id.accessibility_autoclick_dialog_600ms, 600)
                .put(R.id.accessibility_autoclick_dialog_800ms, 800)
                .put(R.id.accessibility_autoclick_dialog_1sec, 1000)
                .put(R.id.accessibility_autoclick_dialog_2sec, 2000)
                .put(R.id.accessibility_autoclick_dialog_4sec, 4000)
                .buildOrThrow();

    /** Create an AutoclickDelayDialogFragment instance. */
    public static @NonNull AutoclickDelayDialogFragment newInstance() {
        return new AutoclickDelayDialogFragment();
    }

    public int getMetricsCategory() {
        return SettingsEnums.ACCESSIBILITY_TOGGLE_AUTOCLICK;
    }

    @Override
    public @NonNull Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View dialogView = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_autoclick_delay_before_click, /* root= */ null);
        getRadioButtonLabels(dialogView);
        RadioGroup radioGroup = dialogView.findViewById(
                R.id.autoclick_delay_before_click_value_group);

        AlertDialog alertDialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .setPositiveButton(android.R.string.ok,
                        (dialog, which) -> {
                            int checkedRadioButtonId =
                                    radioGroup.getCheckedRadioButtonId();

                            int delay = AUTOCLICK_DELAY_WITH_INDICATOR_DEFAULT;
                            if (RADIO_BUTTON_ID_TO_DELAY_TIME
                                    .containsKey(checkedRadioButtonId)) {
                                delay = RADIO_BUTTON_ID_TO_DELAY_TIME.get(checkedRadioButtonId);
                            }

                            // TODO(b/390460859): Add custom seekbar for other delay time values.
                            updateAutoclickDelay(delay);
                        })
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())
                .create();

        if (savedInstanceState == null) {
            initStateBasedOnDelay(radioGroup);
        }

        return alertDialog;
    }

    private void initStateBasedOnDelay(@NonNull RadioGroup radioGroup) {
        // TODO(b/390460859): Add custom seekbar for other delay time values.
        final int autoclickDelay = Settings.Secure.getInt(getContext().getContentResolver(),
                Settings.Secure.ACCESSIBILITY_AUTOCLICK_DELAY,
                AccessibilityManager.AUTOCLICK_DELAY_WITH_INDICATOR_DEFAULT);

        Integer radioButtonId = RADIO_BUTTON_ID_TO_DELAY_TIME.inverse().get(autoclickDelay);
        if (radioButtonId != null) {
            radioGroup.check(radioButtonId);
        }
    }

    private void getRadioButtonLabels(@NonNull View dialogView) {
        for (Integer radioButtonId : RADIO_BUTTON_ID_TO_DELAY_TIME.keySet()) {
            RadioButton radioButton = dialogView.findViewById(radioButtonId);
            if (radioButton != null) {
                radioButton.setText(AutoclickUtils.getAutoclickDelaySummary(
                        getContext(), R.string.accessibility_autoclick_delay_unit_second,
                        RADIO_BUTTON_ID_TO_DELAY_TIME.get(radioButtonId)));
            }
        }
    }

    /** Updates autoclick delay time. */
    public void updateAutoclickDelay(int delay) {
        Settings.Secure.putInt(
                getContext().getContentResolver(),
                Settings.Secure.ACCESSIBILITY_AUTOCLICK_DELAY,
                delay);
    }
}
