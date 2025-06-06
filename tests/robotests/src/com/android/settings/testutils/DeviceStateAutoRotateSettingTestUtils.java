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

package com.android.settings.testutils;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import static android.hardware.devicestate.DeviceState.PROPERTY_FOLDABLE_DISPLAY_CONFIGURATION_OUTER_PRIMARY;
import static android.hardware.devicestate.DeviceState.PROPERTY_FOLDABLE_HARDWARE_CONFIGURATION_FOLD_IN_CLOSED;

import android.content.res.Resources;
import android.hardware.devicestate.DeviceState;
import android.hardware.devicestate.DeviceStateManager;

import java.util.List;
import java.util.Set;

/**
 * Helper for testing device state auto rotate setting
 */
public class DeviceStateAutoRotateSettingTestUtils {

    public static final DeviceState DEFAULT_DEVICE_STATE = new DeviceState(
            new DeviceState.Configuration.Builder(0, "DEFAULT")
                    .build()
    );

    public static final DeviceState FOLDED_DEVICE_STATE = new DeviceState(
            new DeviceState.Configuration.Builder(1, "FOLDED")
                    .setPhysicalProperties(
                            Set.of(
                                    PROPERTY_FOLDABLE_HARDWARE_CONFIGURATION_FOLD_IN_CLOSED,
                                    PROPERTY_FOLDABLE_DISPLAY_CONFIGURATION_OUTER_PRIMARY
                            )
                    )
                    .build()
    );

    /**
     * Mock {@link mockResources} and DeviceStateManager to return device state auto rotate
     * enabled or disabled based on value passed for {@link enable}.
     */
    public static void setDeviceStateRotationLockEnabled(boolean enable, Resources mockResources,
            DeviceStateManager mockDeviceStateManager) {
        String[] perDeviceStateRotationLockDefaults = new String[0];
        if (enable) {
            perDeviceStateRotationLockDefaults = new String[]{"0:1"};
        }
        when(mockResources.getStringArray(
                com.android.internal.R.array.config_perDeviceStateRotationLockDefaults))
                .thenReturn(perDeviceStateRotationLockDefaults);

        if (mockDeviceStateManager != null) {
            final List<DeviceState> deviceStates;
            if (enable) {
                deviceStates = List.of(DEFAULT_DEVICE_STATE, FOLDED_DEVICE_STATE);
            } else {
                deviceStates = List.of(DEFAULT_DEVICE_STATE);
            }
            doReturn(deviceStates).when(mockDeviceStateManager).getSupportedDeviceStates();
        }
    }
}
