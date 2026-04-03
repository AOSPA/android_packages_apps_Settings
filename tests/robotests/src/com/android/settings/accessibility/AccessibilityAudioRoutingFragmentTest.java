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
package com.android.settings.accessibility;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.os.SystemProperties;
import android.util.FeatureFlagUtils;

import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link AccessibilityAudioRoutingFragment}. */
@RunWith(RobolectricTestRunner.class)
public class AccessibilityAudioRoutingFragmentTest {

    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Spy
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private static final String ASHA_PROFILE_CENTRAL_PROPERTY =
            "bluetooth.profile.asha.central.enabled";
    private static final String HAP_PROFILE_CLIENT_PROPERTY =
            "bluetooth.profile.hap.client.enabled";

    @After
    public void tearDown() {
        SystemProperties.set(ASHA_PROFILE_CENTRAL_PROPERTY, "");
        SystemProperties.set(HAP_PROFILE_CLIENT_PROPERTY, "");
    }

    @Test
    public void deviceSupportsHearingAidAndPageEnabled_isPageSearchEnabled_returnTrue() {
        FeatureFlagUtils.setEnabled(mContext,
                FeatureFlagUtils.SETTINGS_AUDIO_ROUTING, true);
        SystemProperties.set(ASHA_PROFILE_CENTRAL_PROPERTY, "true");

        assertThat(AccessibilityAudioRoutingFragment.isPageSearchEnabled(mContext)).isTrue();
    }

    @Test
    public void deviceDoesNotSupportHearingAidAndPageEnabled_isPageSearchEnabled_returnFalse() {
        FeatureFlagUtils.setEnabled(mContext,
                FeatureFlagUtils.SETTINGS_AUDIO_ROUTING, true);
        SystemProperties.set(ASHA_PROFILE_CENTRAL_PROPERTY, "false");
        SystemProperties.set(HAP_PROFILE_CLIENT_PROPERTY, "false");

        assertThat(AccessibilityAudioRoutingFragment.isPageSearchEnabled(mContext)).isFalse();
    }

    @Test
    public void deviceSupportsHearingAidAndPageDisabled_isPageSearchEnabled_returnFalse() {
        FeatureFlagUtils.setEnabled(mContext,
                FeatureFlagUtils.SETTINGS_AUDIO_ROUTING, false);
        SystemProperties.set(ASHA_PROFILE_CENTRAL_PROPERTY, "true");

        assertThat(AccessibilityAudioRoutingFragment.isPageSearchEnabled(mContext)).isFalse();
    }
}
