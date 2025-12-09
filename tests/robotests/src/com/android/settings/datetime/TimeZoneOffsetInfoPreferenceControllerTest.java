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

package com.android.settings.datetime;

import static android.app.time.Capabilities.CAPABILITY_NOT_ALLOWED;
import static android.app.time.Capabilities.CAPABILITY_POSSESSED;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.app.time.TimeManager;
import android.app.time.TimeZoneCapabilitiesAndConfig;
import android.content.Context;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.timezone.flags.Flags;

import com.android.settings.R;

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

import java.util.TimeZone;

@RunWith(RobolectricTestRunner.class)
@EnableFlags({
    Flags.FLAG_ENABLE_TIME_ZONE_OFFSET_CHANGE_BROADCAST,
    Flags.FLAG_TIME_ZONE_OFFSET_CHANGE_NOTIFICATIONS
})
public class TimeZoneOffsetInfoPreferenceControllerTest {

    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    private Context mContext;
    @Mock
    private TimeManager mTimeManager;
    private TimeZoneOffsetInfoPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getSystemService(TimeManager.class)).thenReturn(mTimeManager);
        mController = new TimeZoneOffsetInfoPreferenceController(mContext, "test_key");
    }

    @Test
    public void getAvailabilityStatus_capabilityNotAllowed_isUnsupported() {
        TimeZoneCapabilitiesAndConfig capabilitiesAndConfig =
                TimeZoneOffsetChangeNotificationsPreferenceControllerTest
                        .createCapabilitiesAndConfig(false);
        when(mTimeManager.getTimeZoneCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                TimeZoneOffsetInfoPreferenceController.UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailabilityStatus_capabilityPossessed_isDisabled() {
        TimeZoneCapabilitiesAndConfig capabilitiesAndConfig =
                TimeZoneOffsetChangeNotificationsPreferenceControllerTest
                        .createCapabilitiesAndConfig(true);
        when(mTimeManager.getTimeZoneCapabilitiesAndConfig()).thenReturn(capabilitiesAndConfig);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                TimeZoneOffsetInfoPreferenceController.DISABLED_DEPENDENT_SETTING);
    }

    @Test
    public void getSummary_noDstTransition_returnsNoNextChangeString() {
        // A time zone with no DST.
        TimeZone.setDefault(TimeZone.getTimeZone("Etc/UTC"));
        String expectedSummary = mContext.getString(
                R.string.footer_time_zone_offset_change_no_next_change);

        assertThat(mController.getSummary().toString()).isEqualTo(expectedSummary);
    }
}