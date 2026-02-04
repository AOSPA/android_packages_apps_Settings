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

package com.android.settings.accessibility;

import static com.android.settingslib.bluetooth.HearingAidAudioRoutingConstants.RoutingValue;

import android.bluetooth.BluetoothProfile;
import android.content.ContentResolver;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioDeviceAttributes;
import android.media.audiopolicy.AudioProductStrategy;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.SwitchPreferenceCompat;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.CachedBluetoothDeviceManager;
import com.android.settingslib.bluetooth.HearingAidAudioRoutingHelper;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

import com.google.common.annotations.VisibleForTesting;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Abstract class for providing audio routing {@link SwitchPreferenceCompat} common control for
 * hearing devices specifically.
 */
public abstract class HearingDeviceAudioRoutingBaseSwitchPreferenceController extends
        BasePreferenceController implements Preference.OnPreferenceChangeListener {

    private static final String TAG = "HARoutingBaseSwitchPreferenceController";
    private static final boolean DEBUG = false;

    private final HearingAidAudioRoutingHelper mAudioRoutingHelper;
    @Nullable
    private final LocalBluetoothManager mBluetoothManager;
    private final Executor mExecutor;

    @Nullable
    private CachedBluetoothDeviceManager mCachedDeviceManager;

    public HearingDeviceAudioRoutingBaseSwitchPreferenceController(Context context,
            String preferenceKey) {
        this(context, preferenceKey,
                new HearingAidAudioRoutingHelper(context),
                Executors.newSingleThreadExecutor(),
                LocalBluetoothManager.getInstance(context, /* onInitCallback= */ null));
    }

    @VisibleForTesting
    protected HearingDeviceAudioRoutingBaseSwitchPreferenceController(Context context,
            String preferenceKey, HearingAidAudioRoutingHelper audioRoutingHelper,
            Executor executor, @Nullable LocalBluetoothManager localBluetoothManager) {
        super(context, preferenceKey);
        mAudioRoutingHelper = audioRoutingHelper;
        mExecutor = executor;
        mBluetoothManager = localBluetoothManager;

        if (mBluetoothManager != null) {
            mCachedDeviceManager = mBluetoothManager.getCachedDeviceManager();
        }
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (preference instanceof SwitchPreferenceCompat pref) {
            final int routingValue = getRoutingValue(mContext);
            // If audio is routed to the built-in speaker, the switch should be OFF
            // (i.e., not routing to hearing aid). Otherwise, it's ON.
            final boolean isRouteToHearing = (routingValue == RoutingValue.AUTO
                    || routingValue == RoutingValue.HEARING_DEVICE);

            pref.setChecked(isRouteToHearing);
            updatePreferenceSummary(pref, isRouteToHearing);
        }
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
        if (preference instanceof SwitchPreferenceCompat pref) {
            final boolean isRouteToHearing = (Boolean) newValue;
            final CachedBluetoothDevice activeHearingDevice = getActiveHearingDevice();
            final AudioDeviceAttributes hearingDeviceAttributes;
            if (activeHearingDevice != null) {
                hearingDeviceAttributes =
                        mAudioRoutingHelper.getMatchedHearingDeviceAttributesForOutput(
                                activeHearingDevice);
            } else {
                // user just set the value without connecting to the active hearing device
                hearingDeviceAttributes = null;
            }

            final List<AudioProductStrategy> supportedStrategies =
                    mAudioRoutingHelper.getSupportedStrategies(getAudioAttributeUsages());
            if (supportedStrategies.isEmpty()) {
                Log.w(TAG, ": No supported strategies found for the given audio attributes.");
                return false;
            }

            mExecutor.execute(() -> {
                if (isRouteToHearing) {
                    setRoutingValue(mContext, RoutingValue.AUTO);
                    trySetAudioRouting(supportedStrategies, hearingDeviceAttributes,
                            RoutingValue.AUTO);
                } else {
                    setRoutingValue(mContext, RoutingValue.BUILTIN_DEVICE);
                    trySetAudioRouting(supportedStrategies, hearingDeviceAttributes,
                            RoutingValue.BUILTIN_DEVICE);

                }
                mContext.getMainExecutor().execute(() -> updateState(pref));
            });
            return true;
        }
        return false;
    }

    protected void updatePreferenceSummary(SwitchPreferenceCompat preference,
            boolean isRouteToHearing) {
        if (isRouteToHearing) {
            preference.setSummary(
                    R.string.accessibility_hearing_device_routing_hearing_device_summary);
        } else {
            preference.setSummary(
                    R.string.accessibility_hearing_device_routing_device_speaker_summary);
        }
    }

    @Nullable
    private CachedBluetoothDevice getActiveHearingDevice() {
        if (mBluetoothManager == null || mCachedDeviceManager == null) {
            Log.w(TAG, "BluetoothManager or CachedDeviceManager is null.");
            return null;
        }

        for (CachedBluetoothDevice device : mCachedDeviceManager.getCachedDevicesCopy()) {
            if (device.isHearingDevice() && (device.isActiveDevice(BluetoothProfile.HEARING_AID)
                    || device.isActiveDevice(BluetoothProfile.LE_AUDIO))) {
                return device;
            }
        }
        return null;
    }

    private void trySetAudioRouting(List<AudioProductStrategy> supportedStrategies,
            @Nullable AudioDeviceAttributes audioDeviceAttributes, @RoutingValue int routingValue) {
        if (supportedStrategies.isEmpty() || audioDeviceAttributes == null) {
            return;
        }

        boolean result = mAudioRoutingHelper.configureRoutingStrategies(
                supportedStrategies, audioDeviceAttributes, routingValue);
        if (!result) {
            if (DEBUG) {
                Log.d(TAG, "Fail to configureRoutingStrategies for routingValue: "
                        + routingValue);
            }
        }
    }

    /**
     * Gets a list of usage values defined in {@link AudioAttributes} that are used to identify
     * {@link AudioProductStrategy} to configure audio routing.
     */
    protected abstract @AudioAttributes.AttributeUsage int[] getAudioAttributeUsages();
    /**
     * Sets the routing value.
     *
     * @param context the valid context used to get the {@link ContentResolver}
     * @param routingValue one of the value defined in {@link RoutingValue}
     */
    protected abstract void setRoutingValue(Context context, int routingValue);

    /**
     * Gets the routing value and used to reflect status on {@link SwitchPreferenceCompat}.
     *
     * @param context the valid context used to get the {@link ContentResolver}
     * @return one of the value defined in {@link RoutingValue}
     */
    protected abstract int getRoutingValue(Context context);
}
