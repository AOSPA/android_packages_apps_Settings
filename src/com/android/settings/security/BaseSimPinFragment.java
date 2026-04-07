/*
 * Copyright (C) 2026 The Android Open Source Project
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

package com.android.settings.security;

import android.app.settings.SettingsEnums;

import androidx.annotation.Nullable;

import com.android.settings.dashboard.DashboardFragment;

public abstract class BaseSimPinFragment extends DashboardFragment implements
        EnterSimPinDialogFragment.SimPinEntryListener {
    protected static final String TAG = "SimPinFragment";

    @Nullable
    protected EnterSimPinDialogFragment.SimPinEntryListener mCurrentListener;

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.AUTOMATIC_SIM_PIN_MANAGEMENT;
    }

    public void setCurrentListener(
            @Nullable EnterSimPinDialogFragment.SimPinEntryListener listener) {
        mCurrentListener = listener;
    }

    @Nullable
    private EnterSimPinDialogFragment.SimPinEntryListener getAndClearListener() {
        EnterSimPinDialogFragment.SimPinEntryListener listener = mCurrentListener;
        mCurrentListener = null;
        return listener;
    }

    @Override
    public void onPinEntered(String pin) {
        EnterSimPinDialogFragment.SimPinEntryListener listener = getAndClearListener();
        if (listener == null) {
            return;
        }

        listener.onPinEntered(pin);
    }

    @Override
    public void onEntryCancelled() {
        EnterSimPinDialogFragment.SimPinEntryListener listener = getAndClearListener();
        if (listener == null) {
            return;
        }

        listener.onEntryCancelled();
    }
}
