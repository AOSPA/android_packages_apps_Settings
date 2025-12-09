/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may not use this file except in compliance with the License.
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

package com.android.settings.datetime;

import static android.app.time.Capabilities.CAPABILITY_NOT_ALLOWED;
import static android.app.time.Capabilities.CAPABILITY_NOT_SUPPORTED;
import static android.app.time.Capabilities.CAPABILITY_POSSESSED;
import static android.app.time.LocationTimeZoneAlgorithmStatus.PROVIDER_STATUS_NOT_PRESENT;
import static android.app.time.LocationTimeZoneAlgorithmStatus.PROVIDER_STATUS_NOT_READY;
import static android.app.time.DetectorStatusTypes.DETECTION_ALGORITHM_STATUS_RUNNING;
import static android.app.time.DetectorStatusTypes.DETECTOR_STATUS_RUNNING;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.app.time.Capabilities;
import android.app.time.LocationTimeZoneAlgorithmStatus;
import android.app.time.TelephonyTimeZoneAlgorithmStatus;
import android.app.time.TimeManager;
import android.app.time.TimeZoneCapabilities;
import android.app.time.TimeZoneCapabilitiesAndConfig;
import android.app.time.TimeZoneConfiguration;
import android.app.time.TimeZoneDetectorStatus;
import android.content.Context;
import android.os.UserHandle;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.timezone.flags.Flags;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
@EnableFlags({
    Flags.FLAG_ENABLE_TIME_ZONE_OFFSET_CHANGE_BROADCAST,
    Flags.FLAG_TIME_ZONE_OFFSET_CHANGE_NOTIFICATIONS
})
public class TimeZoneOffsetChangeNotificationsPreferenceControllerTest {

    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    private Context mContext;
    @Mock
    private TimeManager mTimeManager;
    private TimeZoneOffsetChangeNotificationsPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getSystemService(TimeManager.class)).thenReturn(mTimeManager);
        mController =
                new TimeZoneOffsetChangeNotificationsPreferenceController(mContext, "test_key");
    }

    @Test
    public void getAvailabilityStatus_capabilityNotAllowed_isUnsupported() {
        TimeZoneCapabilitiesAndConfig capabilitiesAndConfig = createCapabilitiesAndConfig(false);
        when(mTimeManager.getTimeZoneCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                TimeZoneOffsetChangeNotificationsPreferenceController.UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailabilityStatus_capabilityPossessed_isAvailable() {
        TimeZoneCapabilitiesAndConfig capabilitiesAndConfig = createCapabilitiesAndConfig(true);
        when(mTimeManager.getTimeZoneCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                TimeZoneOffsetChangeNotificationsPreferenceController.AVAILABLE);
    }

    @Test
    public void isChecked_returnsConfigurationValue() {
        TimeZoneCapabilitiesAndConfig capabilitiesAndConfig = createCapabilitiesAndConfig(true);
        when(mTimeManager.getTimeZoneCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);
        assertThat(mController.isChecked()).isTrue();

        capabilitiesAndConfig = createCapabilitiesAndConfig(false);
        when(mTimeManager.getTimeZoneCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);
        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void setChecked_updatesConfiguration() {
        when(mTimeManager.updateTimeZoneConfiguration(any())).thenReturn(true);
        mController.setChecked(true);
        mController.setChecked(false);
    }

    static TimeZoneCapabilitiesAndConfig createCapabilitiesAndConfig(boolean notificationsEnabled) {
        int notificationsCapability = notificationsEnabled ? Capabilities.CAPABILITY_POSSESSED
                : Capabilities.CAPABILITY_NOT_SUPPORTED;

        TimeZoneDetectorStatus status = new TimeZoneDetectorStatus(DETECTOR_STATUS_RUNNING,
                new TelephonyTimeZoneAlgorithmStatus(DETECTION_ALGORITHM_STATUS_RUNNING),
                new LocationTimeZoneAlgorithmStatus(DETECTION_ALGORITHM_STATUS_RUNNING,
                        PROVIDER_STATUS_NOT_READY, null,
                        PROVIDER_STATUS_NOT_PRESENT, null));
        TimeZoneCapabilities capabilities = new TimeZoneCapabilities.Builder(UserHandle.SYSTEM)
                .setConfigureAutoDetectionEnabledCapability(Capabilities.CAPABILITY_POSSESSED)
                .setUseLocationEnabled(true)
                .setConfigureGeoDetectionEnabledCapability(Capabilities.CAPABILITY_POSSESSED)
                .setSetManualTimeZoneCapability(Capabilities.CAPABILITY_POSSESSED)
                .setConfigureNotificationsEnabledCapability(Capabilities.CAPABILITY_POSSESSED)
                .setConfigureTimeZoneOffsetChangeNotificationsEnabledCapability(
                        notificationsCapability)
                .build();
        TimeZoneConfiguration config = new TimeZoneConfiguration.Builder()
                .setAutoDetectionEnabled(true)
                .setGeoDetectionEnabled(true)
                .setNotificationsEnabled(true)
                .setTimeZoneOffsetChangeNotificationsEnabled(notificationsEnabled)
                .build();
        return new TimeZoneCapabilitiesAndConfig(status, capabilities, config);
    }
}