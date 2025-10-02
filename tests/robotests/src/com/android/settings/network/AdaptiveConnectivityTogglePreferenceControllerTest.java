/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.network;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.Settings;

import com.android.settings.flags.Flags;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

// LINT.IfChange
@RunWith(RobolectricTestRunner.class)
public class AdaptiveConnectivityTogglePreferenceControllerTest {
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    private static final String PREF_KEY = "adaptive_connectivity_enabled";

    @Mock(answer = RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock
    WifiManager mWifiManager;

    private AdaptiveConnectivityTogglePreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getSystemService(Context.WIFI_SERVICE)).thenReturn(mWifiManager);
        mController = new AdaptiveConnectivityTogglePreferenceController(mContext, PREF_KEY);
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_ADAPTIVE_CONNECTIVITY_TOGGLE_SWITCHES)
    public void isAvailable_flagDisabled_shouldReturnTrue() {
        mController = new AdaptiveConnectivityTogglePreferenceController(mContext, PREF_KEY);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_ADAPTIVE_CONNECTIVITY_TOGGLE_SWITCHES)
    public void isAvailable_flagEnabled_shouldReturnFalse() {
        mController = new AdaptiveConnectivityTogglePreferenceController(mContext, PREF_KEY);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void setChecked_withTrue_shouldUpdateSetting() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ADAPTIVE_CONNECTIVITY_ENABLED, 0);

        mController.setChecked(true);

        assertThat(Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.ADAPTIVE_CONNECTIVITY_ENABLED, 1))
                .isEqualTo(1);
        verify(mWifiManager, atLeastOnce()).setWifiScoringEnabled(true);
    }

    @Test
    public void setChecked_withFalse_shouldUpdateSetting() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ADAPTIVE_CONNECTIVITY_ENABLED, 1);

        mController.setChecked(false);

        assertThat(Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.ADAPTIVE_CONNECTIVITY_ENABLED, 1))
                .isEqualTo(0);
        verify(mWifiManager).setWifiScoringEnabled(false);
    }
}
// LINT.ThenChange(AdaptiveConnectivityTogglePreferenceTest.kt)
