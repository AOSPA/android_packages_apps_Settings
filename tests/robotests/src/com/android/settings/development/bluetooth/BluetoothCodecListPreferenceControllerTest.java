/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.development.bluetooth;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothCodecConfig;
import android.bluetooth.BluetoothCodecStatus;
import android.bluetooth.BluetoothCodecType;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;

import androidx.lifecycle.LifecycleOwner;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceScreen;

import com.android.settings.development.BluetoothA2dpConfigStore;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RunWith(RobolectricTestRunner.class)
public class BluetoothCodecListPreferenceControllerTest {
    private static final String DEVICE_ADDRESS = "00:11:22:33:44:55";
    private static final String TEST_ENTRY = "TEST_ENTRY";
    private static final String TEST_ENTRY_VALUE = "1000";

    @Mock private BluetoothA2dp mBluetoothA2dp;
    @Mock private BluetoothAdapter mBluetoothAdapter;
    @Mock private PreferenceScreen mScreen;
    @Mock private AbstractBluetoothPreferenceController.Callback mCallback;

    private BluetoothCodecListPreferenceController mController;
    private ListPreference mPreference;
    private BluetoothA2dpConfigStore mBluetoothA2dpConfigStore;
    private BluetoothCodecStatus mCodecStatus;
    private List<BluetoothCodecType> mCodecTypes;
    private List<BluetoothCodecConfig> mCodecConfigs;
    private BluetoothDevice mActiveDevice;
    private Context mContext;
    private BluetoothCodecConfig mCodecConfigSBC;
    private BluetoothCodecConfig mCodecConfigAAC;
    private LifecycleOwner mLifecycleOwner;
    private Lifecycle mLifecycle;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        mBluetoothA2dpConfigStore = spy(new BluetoothA2dpConfigStore());
        mActiveDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(DEVICE_ADDRESS);
        mController =
                new BluetoothCodecListPreferenceController(
                        mContext, mLifecycle, mBluetoothA2dpConfigStore, mCallback);
        mController.mBluetoothAdapter = mBluetoothAdapter;
        mPreference = spy(new ListPreference(mContext));
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
        mController.displayPreference(mScreen);

        mCodecConfigSBC = createCodecConfig(BluetoothCodecConfig.SOURCE_CODEC_TYPE_SBC);
        mCodecConfigAAC =
                createCodecConfig(
                        BluetoothCodecConfig.SOURCE_CODEC_TYPE_AAC,
                        BluetoothCodecConfig.SAMPLE_RATE_88200,
                        BluetoothCodecConfig.BITS_PER_SAMPLE_24,
                        BluetoothCodecConfig.CHANNEL_MODE_STEREO);
        mCodecConfigs =
                Arrays.asList(
                        mCodecConfigSBC,
                        mCodecConfigAAC,
                        createCodecConfig(BluetoothCodecConfig.SOURCE_CODEC_TYPE_APTX),
                        createCodecConfig(BluetoothCodecConfig.SOURCE_CODEC_TYPE_OPUS),
                        createCodecConfig(BluetoothCodecConfig.SOURCE_CODEC_TYPE_APTX_HD),
                        createCodecConfig(BluetoothCodecConfig.SOURCE_CODEC_TYPE_LDAC));
        mCodecTypes =
                mCodecConfigs.stream()
                        .map(BluetoothCodecConfig::getExtendedCodecType)
                        .collect(Collectors.toList());

        when(mBluetoothAdapter.getActiveDevices(eq(BluetoothProfile.A2DP)))
                .thenReturn(Arrays.asList(mActiveDevice));
        when(mBluetoothA2dp.getSupportedCodecTypes()).thenReturn(mCodecTypes);
    }

    private BluetoothCodecConfig createCodecConfig(
            int codecType, int sampleRate, int bitsPerSample, int channelMode) {
        return new BluetoothCodecConfig.Builder()
                .setCodecType(codecType)
                .setCodecPriority(BluetoothCodecConfig.CODEC_PRIORITY_HIGHEST)
                .setSampleRate(sampleRate)
                .setBitsPerSample(bitsPerSample)
                .setChannelMode(channelMode)
                .build();
    }

    private BluetoothCodecConfig createCodecConfig(int codecType) {
        return createCodecConfig(codecType, 0, 0, 0);
    }

    @Test
    public void writeConfigurationValues_setsCodecAndChoosesHighestConfig() {
        mCodecStatus =
                new BluetoothCodecStatus.Builder()
                        .setCodecConfig(mCodecConfigSBC)
                        .setCodecsSelectableCapabilities(mCodecConfigs)
                        .build();
        when(mBluetoothA2dp.getCodecStatus(mActiveDevice)).thenReturn(mCodecStatus);
        mController.onBluetoothServiceConnected(mBluetoothA2dp);

        for (BluetoothCodecConfig config : mCodecConfigs) {
            assertTrue(
                    mController.writeConfigurationValues(
                            String.valueOf(config.getExtendedCodecType().getCodecId())));
            verify(mBluetoothA2dpConfigStore, atLeastOnce())
                    .setCodecType(config.getExtendedCodecType());
        }

        // Verify highest config is chosen for AAC
        assertTrue(
                mController.writeConfigurationValues(
                        String.valueOf(mCodecConfigAAC.getExtendedCodecType().getCodecId())));
        verify(mBluetoothA2dpConfigStore, atLeastOnce())
                .setCodecPriority(BluetoothCodecConfig.CODEC_PRIORITY_HIGHEST);
        verify(mBluetoothA2dpConfigStore, atLeastOnce())
                .setSampleRate(BluetoothCodecConfig.SAMPLE_RATE_88200);
        verify(mBluetoothA2dpConfigStore, atLeastOnce())
                .setBitsPerSample(BluetoothCodecConfig.BITS_PER_SAMPLE_24);
        verify(mBluetoothA2dpConfigStore, atLeastOnce())
                .setChannelMode(BluetoothCodecConfig.CHANNEL_MODE_STEREO);
    }

    @Test
    public void
            onPreferenceChange_whenOptionalCodecsEnabled_notifiesCallbackAndEnablesPreference() {
        when(mBluetoothA2dp.isOptionalCodecsEnabled(mActiveDevice))
                .thenReturn(BluetoothA2dp.OPTIONAL_CODECS_PREF_ENABLED);
        mCodecStatus =
                new BluetoothCodecStatus.Builder()
                        .setCodecConfig(mCodecConfigAAC)
                        .setCodecsSelectableCapabilities(mCodecConfigs)
                        .build();
        when(mBluetoothA2dp.getCodecStatus(mActiveDevice)).thenReturn(mCodecStatus);
        mController.onBluetoothServiceConnected(mBluetoothA2dp);

        assertTrue(
                mController.onPreferenceChange(
                        mPreference,
                        String.valueOf(mCodecConfigAAC.getExtendedCodecType().getCodecId())));

        verify(mCallback).onBluetoothCodecChanged();
        assertTrue(mPreference.isEnabled());
    }

    @Test
    public void onPreferenceChange_whenListPreferenceIsNull_shouldReturnFalse() {
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(null);
        assertFalse(
                mController.onPreferenceChange(
                        mPreference,
                        String.valueOf(mCodecConfigAAC.getExtendedCodecType().getCodecId())));
    }

    @Test
    public void onPreferenceChange_whenUnknownCodecId_shouldReturnFalse() {
        assertFalse(mController.onPreferenceChange(mPreference, TEST_ENTRY_VALUE));
    }

    @Test
    public void onPreferenceChange_whenOptionalCodecsEnabled_shouldReturnTrueForAllCodecs() {
        when(mBluetoothA2dp.isOptionalCodecsEnabled(mActiveDevice))
                .thenReturn(BluetoothA2dp.OPTIONAL_CODECS_PREF_ENABLED);
        mController.onBluetoothServiceConnected(mBluetoothA2dp);

        for (BluetoothCodecConfig codecConfig : mCodecConfigs) {
            mCodecStatus =
                    new BluetoothCodecStatus.Builder()
                            .setCodecConfig(codecConfig)
                            .setCodecsSelectableCapabilities(mCodecConfigs)
                            .build();
            when(mBluetoothA2dp.getCodecStatus(mActiveDevice)).thenReturn(mCodecStatus);
            assertTrue(
                    "Failed for codec: " + codecConfig.getExtendedCodecType().getCodecName(),
                    mController.onPreferenceChange(
                            mPreference,
                            String.valueOf(codecConfig.getExtendedCodecType().getCodecId())));
        }
    }

    @Test
    public void updateState_whenOptionalCodecsEnabled_sortsCodecsAndNotifiesCallback() {
        mCodecStatus =
                new BluetoothCodecStatus.Builder()
                        .setCodecConfig(mCodecConfigSBC)
                        .setCodecsSelectableCapabilities(mCodecConfigs)
                        .build();
        when(mBluetoothA2dp.getCodecStatus(mActiveDevice)).thenReturn(mCodecStatus);
        when(mBluetoothA2dp.isOptionalCodecsEnabled(mActiveDevice))
                .thenReturn(BluetoothA2dp.OPTIONAL_CODECS_PREF_ENABLED);
        mController.onBluetoothServiceConnected(mBluetoothA2dp);

        ArgumentCaptor<String[]> entriesCaptor = ArgumentCaptor.forClass(String[].class);
        verify(mPreference).setEntries(entriesCaptor.capture());
        String[] entries = entriesCaptor.getValue();
        assertThat(entries)
                .asList()
                .containsExactlyElementsIn(
                        mCodecTypes.stream()
                                .map(BluetoothCodecType::getCodecName)
                                .collect(Collectors.toList()))
                .inOrder();

        ArgumentCaptor<String[]> entryValuesCaptor = ArgumentCaptor.forClass(String[].class);
        verify(mPreference).setEntryValues(entryValuesCaptor.capture());
        String[] entryValues = entryValuesCaptor.getValue();
        assertThat(entryValues)
                .asList()
                .containsExactlyElementsIn(
                        mCodecTypes.stream()
                                .map(c -> String.valueOf(c.getCodecId()))
                                .collect(Collectors.toList()))
                .inOrder();

        assertTrue(
                mController.onPreferenceChange(
                        mPreference,
                        String.valueOf(mCodecConfigAAC.getExtendedCodecType().getCodecId())));
        verify(mCallback).onBluetoothCodecChanged();
    }

    @Test
    public void onHDAudioEnabled_setsPreferenceEnabled() {
        mCodecStatus =
                new BluetoothCodecStatus.Builder()
                        .setCodecConfig(mCodecConfigSBC)
                        .setCodecsSelectableCapabilities(mCodecConfigs)
                        .build();
        when(mBluetoothA2dp.getCodecStatus(mActiveDevice)).thenReturn(mCodecStatus);
        when(mBluetoothA2dp.isOptionalCodecsEnabled(mActiveDevice))
                .thenReturn(BluetoothA2dp.OPTIONAL_CODECS_PREF_ENABLED);
        mController.onBluetoothServiceConnected(mBluetoothA2dp);

        mController.onHDAudioEnabled(true);
        assertThat(mPreference.isEnabled()).isTrue();
    }

    @Test
    public void setupListPreference_whenInvalidLists_shouldSetupDefault() {
        mController.setupListPreference(
                Arrays.asList(TEST_ENTRY),
                Arrays.asList(TEST_ENTRY_VALUE, TEST_ENTRY_VALUE),
                "",
                "");
        assertThat(mPreference.getValue()).isNull();
        assertThat(mPreference.getSummary()).isNull();
        assertThat(mPreference.isEnabled()).isFalse();

        mController.setupListPreference(Arrays.asList(TEST_ENTRY), new ArrayList<>(), "", "");
        assertThat(mPreference.getValue()).isNull();
        assertThat(mPreference.getSummary()).isNull();
        assertThat(mPreference.isEnabled()).isFalse();
    }

    @Test
    public void getBluetoothCodecStatus_whenServiceNotConnectedOrStatusNull_shouldReturnNull() {
        mController.onBluetoothServiceConnected(null);
        assertThat(mController.getBluetoothCodecStatus()).isNull();

        mController.onBluetoothServiceConnected(mBluetoothA2dp);
        when(mBluetoothA2dp.getCodecStatus(mActiveDevice)).thenReturn(null);
        assertThat(mController.getBluetoothCodecStatus()).isNull();
    }

    @Test
    public void getCurrentCodecConfig_whenValid_shouldReturnConfig() {
        mCodecStatus = new BluetoothCodecStatus.Builder().setCodecConfig(mCodecConfigAAC).build();
        when(mBluetoothA2dp.getCodecStatus(mActiveDevice)).thenReturn(mCodecStatus);
        mController.onBluetoothServiceConnected(mBluetoothA2dp);

        assertThat(mController.getCurrentCodecConfig()).isEqualTo(mCodecConfigAAC);
    }

    @Test
    public void isHDAudioEnabled_whenEnabled_shouldReturnTrue() {
        mController.onBluetoothServiceConnected(mBluetoothA2dp);
        when(mBluetoothA2dp.isOptionalCodecsEnabled(mActiveDevice))
                .thenReturn(BluetoothA2dp.OPTIONAL_CODECS_PREF_ENABLED);
        assertTrue(mController.isHDAudioEnabled());
    }

    @Test
    public void onBluetoothServiceConnected_initializesConfigStore() {
        mCodecStatus =
                new BluetoothCodecStatus.Builder()
                        .setCodecConfig(mCodecConfigAAC)
                        .setCodecsSelectableCapabilities(mCodecConfigs)
                        .build();
        when(mBluetoothA2dp.getCodecStatus(mActiveDevice)).thenReturn(mCodecStatus);
        mController.onBluetoothServiceConnected(mBluetoothA2dp);

        verify(mBluetoothA2dpConfigStore).setCodecType(mCodecConfigAAC.getExtendedCodecType());
        verify(mBluetoothA2dpConfigStore).setSampleRate(mCodecConfigAAC.getSampleRate());
        verify(mBluetoothA2dpConfigStore).setBitsPerSample(mCodecConfigAAC.getBitsPerSample());
        verify(mBluetoothA2dpConfigStore).setChannelMode(mCodecConfigAAC.getChannelMode());
        verify(mBluetoothA2dpConfigStore)
                .setCodecPriority(BluetoothCodecConfig.CODEC_PRIORITY_HIGHEST);
        verify(mBluetoothA2dpConfigStore)
                .setCodecSpecific1Value(mCodecConfigAAC.getCodecSpecific1());
    }

    @Test
    public void onPreferenceChange_whenBluetoothA2dpIsNull_shouldReturnFalse() {
        mController.onBluetoothServiceConnected(null);
        assertFalse(
                mController.onPreferenceChange(
                        mPreference,
                        String.valueOf(mCodecConfigAAC.getExtendedCodecType().getCodecId())));
    }

    @Test
    public void onPreferenceChange_whenConfigStoreIsNull_shouldReturnFalse() {
        mController =
                new BluetoothCodecListPreferenceController(mContext, mLifecycle, null, mCallback);
        mController.onBluetoothServiceConnected(mBluetoothA2dp);
        assertFalse(
                mController.onPreferenceChange(
                        mPreference,
                        String.valueOf(mCodecConfigAAC.getExtendedCodecType().getCodecId())));
    }

    @Test
    public void onPreferenceChange_whenActiveDeviceIsNull_shouldReturnFalse() {
        when(mBluetoothAdapter.getActiveDevices(eq(BluetoothProfile.A2DP)))
                .thenReturn(new ArrayList<>());
        mController.onBluetoothServiceConnected(mBluetoothA2dp);
        assertFalse(
                mController.onPreferenceChange(
                        mPreference,
                        String.valueOf(mCodecConfigAAC.getExtendedCodecType().getCodecId())));
    }

    @Test
    public void onPreferenceChange_whenOptionalCodecsDisabled_shouldReturnFalse() {
        when(mBluetoothA2dp.isOptionalCodecsEnabled(mActiveDevice))
                .thenReturn(BluetoothA2dp.OPTIONAL_CODECS_PREF_DISABLED);
        mController.onBluetoothServiceConnected(mBluetoothA2dp);
        assertFalse(
                mController.onPreferenceChange(
                        mPreference,
                        String.valueOf(mCodecConfigAAC.getExtendedCodecType().getCodecId())));
    }

    @Test
    public void writeConfigurationValues_whenBluetoothA2dpIsNull_shouldReturnFalse() {
        mController.onBluetoothServiceConnected(null);
        assertFalse(
                mController.writeConfigurationValues(
                        String.valueOf(mCodecConfigAAC.getExtendedCodecType().getCodecId())));
    }

    @Test
    public void writeConfigurationValues_whenCodecStatusIsNull_shouldReturnFalse() {
        when(mBluetoothA2dp.getCodecStatus(mActiveDevice)).thenReturn(null);
        mController.onBluetoothServiceConnected(mBluetoothA2dp);
        assertFalse(
                mController.writeConfigurationValues(
                        String.valueOf(mCodecConfigAAC.getExtendedCodecType().getCodecId())));
    }

    @Test
    public void updateState_whenCodecStatusIsNull_shouldSetDefaultPreference() {
        when(mBluetoothA2dp.getCodecStatus(mActiveDevice)).thenReturn(null);
        mController.onBluetoothServiceConnected(mBluetoothA2dp);
        mController.updateState(mPreference);
        assertThat(mPreference.getValue()).isNull();
        assertThat(mPreference.getSummary()).isNull();
        assertThat(mPreference.isEnabled()).isFalse();
    }

    @Test
    public void updateState_whenCurrentCodecConfigIsNull_shouldSetDefaultPreference() {
        mCodecStatus =
                new BluetoothCodecStatus.Builder()
                        .setCodecsSelectableCapabilities(mCodecConfigs)
                        .build();
        when(mBluetoothA2dp.getCodecStatus(mActiveDevice)).thenReturn(mCodecStatus);
        mController.onBluetoothServiceConnected(mBluetoothA2dp);
        mController.updateState(mPreference);
        assertThat(mPreference.getValue()).isNull();
        assertThat(mPreference.getSummary()).isNull();
        assertThat(mPreference.isEnabled()).isFalse();
    }

    @Test
    public void onHDAudioEnabled_whenListPreferenceIsNull_shouldNotCrash() {
        mController.mListPreference = null;
        mController.onHDAudioEnabled(true);
        // No crash
    }

    @Test
    public void initConfigStore_whenConfigOrStoreIsNull_shouldNotCrash() {
        when(mBluetoothA2dp.getCodecStatus(mActiveDevice)).thenReturn(null);
        mController.onBluetoothServiceConnected(mBluetoothA2dp);
        mController.initConfigStore();
        // No crash

        mController =
                new BluetoothCodecListPreferenceController(mContext, mLifecycle, null, mCallback);
        mController.onBluetoothServiceConnected(mBluetoothA2dp);
        mController.initConfigStore();
        // No crash
    }

    @Test
    public void setupDefaultListPreference_whenListPreferenceIsNull_shouldNotCrash() {
        mController.mListPreference = null;
        mController.setupDefaultListPreference();
        // No crash
    }

    @Test
    public void setupListPreference_whenListPreferenceIsNull_shouldNotCrash() {
        mController.mListPreference = null;
        mController.setupListPreference(new ArrayList<>(), new ArrayList<>(), "", "");
        // No crash
    }

    @Test
    public void getCurrentCodecConfig_whenServiceNotConnected_shouldReturnNull() {
        mController.onBluetoothServiceConnected(null);
        assertThat(mController.getCurrentCodecConfig()).isNull();
    }

    @Test
    public void getCurrentCodecConfig_whenCodecStatusIsNull_shouldReturnNull() {
        mController.onBluetoothServiceConnected(mBluetoothA2dp);
        when(mBluetoothA2dp.getCodecStatus(mActiveDevice)).thenReturn(null);
        assertThat(mController.getCurrentCodecConfig()).isNull();
    }

    @Test
    public void isHDAudioEnabled_whenServiceNotConnected_shouldReturnFalse() {
        mController.onBluetoothServiceConnected(null);
        assertFalse(mController.isHDAudioEnabled());
    }

    @Test
    public void isHDAudioEnabled_whenOptionalCodecsDisabled_shouldReturnFalse() {
        mController.onBluetoothServiceConnected(mBluetoothA2dp);
        when(mBluetoothA2dp.isOptionalCodecsEnabled(mActiveDevice))
                .thenReturn(BluetoothA2dp.OPTIONAL_CODECS_PREF_DISABLED);
        assertFalse(mController.isHDAudioEnabled());
    }
}
