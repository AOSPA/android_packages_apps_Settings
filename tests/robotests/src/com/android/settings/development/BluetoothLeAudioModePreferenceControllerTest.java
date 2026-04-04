/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.development;

import static com.android.settings.development.BluetoothLeAudioModePreferenceController
        .LE_AUDIO_DYNAMIC_SWITCHER_MODE_PROPERTY;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothStatusCodes;
import android.content.Context;
import android.os.SystemProperties;

import androidx.preference.ListPreference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Arrays;

@RunWith(RobolectricTestRunner.class)
public class BluetoothLeAudioModePreferenceControllerTest {

    @Mock
    private PreferenceScreen mPreferenceScreen;
    @Mock
    private DevelopmentSettingsDashboardFragment mFragment;
    @Mock
    private BluetoothAdapter mBluetoothAdapter;
    @Mock
    private ListPreference mPreference;

    private Context mContext;
    private BluetoothLeAudioModePreferenceController mController;
    private String[] mListValues;
    private String[] mListSummaries;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mListValues = mContext.getResources().getStringArray(
                R.array.bluetooth_leaudio_mode_values);
        mListSummaries = mContext.getResources().getStringArray(
                R.array.bluetooth_leaudio_mode);
    }

    private void setupUnicastBroadcastSupportStatus(boolean bapUnicastClientEnabled,
            boolean bapBroadcastSourceEnabled) {
        SystemProperties.set("bluetooth.profile.bap.unicast.client.enabled",
                String.valueOf(bapUnicastClientEnabled));
        SystemProperties.set("bluetooth.profile.bap.broadcast.source.enabled",
                String.valueOf(bapBroadcastSourceEnabled));
        mController = spy(new BluetoothLeAudioModePreferenceController(mContext, mFragment));
        when(mPreferenceScreen.findPreference(mController.getPreferenceKey()))
                .thenReturn(mPreference);
        mController.mBluetoothAdapter = mBluetoothAdapter;
        mController.displayPreference(mPreferenceScreen);
    }

    @Test
    public void constructor_broadcastEnabled_includesBroadcastOption() {
        setupUnicastBroadcastSupportStatus(true, true);
        assertThat(Arrays.asList(mController.mListValues).contains("broadcast")).isTrue();
    }

    @Test
    public void constructor_broadcastDisabled_removesBroadcastOption() {
        setupUnicastBroadcastSupportStatus(true, false);
        assertThat(Arrays.asList(mController.mListValues).contains("broadcast")).isFalse();
    }

    @Test
    public void isAvailable_unicastEnabled_returnsTrue() {
        setupUnicastBroadcastSupportStatus(true, true);
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_unicastDisabled_returnsFalse() {
        setupUnicastBroadcastSupportStatus(false, true);
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void updateState_leAudioBroadcastSupported_setsCorrectValueAndSummary() {
        setupUnicastBroadcastSupportStatus(true, true);
        when(mBluetoothAdapter.isLeAudioBroadcastSourceSupported())
                .thenReturn(BluetoothStatusCodes.FEATURE_SUPPORTED);

        mController.updateState(mPreference);

        verify(mPreference).setValue("broadcast");
        int broadcastIndex = Arrays.asList(mListValues).indexOf("broadcast");
        verify(mPreference).setSummary(mListSummaries[broadcastIndex]);
    }

    @Test
    public void updateState_leAudioUnicastSupported_setsCorrectValueAndSummary() {
        setupUnicastBroadcastSupportStatus(true, true);
        when(mBluetoothAdapter.isLeAudioBroadcastSourceSupported())
                .thenReturn(BluetoothStatusCodes.FEATURE_NOT_SUPPORTED);
        when(mBluetoothAdapter.isLeAudioSupported())
                .thenReturn(BluetoothStatusCodes.FEATURE_SUPPORTED);

        mController.updateState(mPreference);

        verify(mPreference).setValue("unicast");
        int unicastIndex = Arrays.asList(mListValues).indexOf("unicast");
        verify(mPreference).setSummary(mListSummaries[unicastIndex]);
    }

    @Test
    public void updateState_leAudioNotSupported_setsCorrectValueAndSummary() {
        setupUnicastBroadcastSupportStatus(true, true);
        when(mBluetoothAdapter.isLeAudioBroadcastSourceSupported())
                .thenReturn(BluetoothStatusCodes.FEATURE_NOT_SUPPORTED);
        when(mBluetoothAdapter.isLeAudioSupported())
                .thenReturn(BluetoothStatusCodes.FEATURE_NOT_SUPPORTED);

        mController.updateState(mPreference);

        verify(mPreference).setValue("disabled");
        int disabledIndex = Arrays.asList(mListValues).indexOf("disabled");
        verify(mPreference).setSummary(mListSummaries[disabledIndex]);
    }

    @Test
    public void onRebootDialogConfirmed_changeLeAudioMode_shouldSetLeAudioMode() {
        setupUnicastBroadcastSupportStatus(true, true);
        mController.mChanged = true;
        SystemProperties.set(LE_AUDIO_DYNAMIC_SWITCHER_MODE_PROPERTY, mListValues[0]);
        mController.mNewMode = mListValues[1];

        mController.onRebootDialogConfirmed();
        assertThat(SystemProperties.get(LE_AUDIO_DYNAMIC_SWITCHER_MODE_PROPERTY, mListValues[0])
                        .equals(mController.mNewMode)).isTrue();
    }

    @Test
    public void onRebootDialogConfirmed_notChangeLeAudioMode_shouldNotSetLeAudioMode() {
        setupUnicastBroadcastSupportStatus(true, true);
        mController.mChanged = false;
        SystemProperties.set(LE_AUDIO_DYNAMIC_SWITCHER_MODE_PROPERTY, mListValues[0]);
        mController.mNewMode = mListValues[1];

        mController.onRebootDialogConfirmed();
        assertThat(SystemProperties.get(LE_AUDIO_DYNAMIC_SWITCHER_MODE_PROPERTY, mListValues[0])
                        .equals(mController.mNewMode)).isFalse();
    }

    @Test
    public void onRebootDialogCanceled_shouldNotSetLeAudioMode() {
        setupUnicastBroadcastSupportStatus(true, true);
        mController.mChanged = true;
        SystemProperties.set(LE_AUDIO_DYNAMIC_SWITCHER_MODE_PROPERTY, mListValues[0]);
        mController.mNewMode = mListValues[1];

        mController.onRebootDialogCanceled();
        assertThat(SystemProperties.get(LE_AUDIO_DYNAMIC_SWITCHER_MODE_PROPERTY, mListValues[0])
                        .equals(mController.mNewMode)).isFalse();
    }

    @Test
    public void onBluetoothTurnOff_shouldNotChangeLeAudioMode() {
        setupUnicastBroadcastSupportStatus(true, true);
        SystemProperties.set(LE_AUDIO_DYNAMIC_SWITCHER_MODE_PROPERTY, mListValues[1]);
        when(mBluetoothAdapter.isEnabled())
                .thenReturn(false);

        mController.updateState(mPreference);
        final String mode = SystemProperties
                .get(LE_AUDIO_DYNAMIC_SWITCHER_MODE_PROPERTY, mListValues[0]);
        assertThat(mode.equals(mListValues[1])).isTrue();
    }
}