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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioDeviceAttributes;
import android.media.AudioDeviceInfo;
import android.media.audiopolicy.AudioProductStrategy;

import androidx.preference.SwitchPreferenceCompat;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.testutils.shadow.ShadowBluetoothAdapter;
import com.android.settings.testutils.shadow.ShadowBluetoothUtils;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.CachedBluetoothDeviceManager;
import com.android.settingslib.bluetooth.HearingAidAudioRoutingConstants;
import com.android.settingslib.bluetooth.HearingAidAudioRoutingHelper;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.MoreExecutors;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import java.util.ArrayList;
import java.util.List;

/** Tests for {@link HearingDeviceAudioRoutingBaseSwitchPreferenceController}. */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowBluetoothAdapter.class, ShadowBluetoothUtils.class})
public class HearingDeviceAudioRoutingBaseSwitchPreferenceControllerTest {

    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Spy
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private static final String TEST_DEVICE_ADDRESS = "00:A1:A1:A1:A1:A1";
    private final SwitchPreferenceCompat mSwitchPreference = new SwitchPreferenceCompat(mContext);

    @Mock
    private LocalBluetoothManager mLocalBluetoothManager;
    @Mock
    private LocalBluetoothProfileManager mLocalBluetoothProfileManager;
    @Mock
    private CachedBluetoothDeviceManager mCachedDeviceManager;
    @Mock
    private AudioProductStrategy mAudioProductStrategyNotif;
    @Mock
    private CachedBluetoothDevice mCachedBluetoothDevice;
    @Mock
    private BluetoothDevice mBluetoothDevice;
    @Mock
    private HearingAidAudioRoutingHelper mAudioRoutingHelper;
    private TestHearingDeviceAudioRoutingBaseSwitchPreferenceController mController;

    @Before
    public void setUp() {
        final AudioDeviceAttributes hearingDeviceAttribute = new AudioDeviceAttributes(
                AudioDeviceAttributes.ROLE_OUTPUT,
                AudioDeviceInfo.TYPE_HEARING_AID,
                TEST_DEVICE_ADDRESS);
        ShadowBluetoothUtils.sLocalBluetoothManager = mLocalBluetoothManager;

        when(mLocalBluetoothManager.getCachedDeviceManager()).thenReturn(mCachedDeviceManager);
        when(mLocalBluetoothManager.getProfileManager()).thenReturn(mLocalBluetoothProfileManager);
        when(mCachedBluetoothDevice.getDevice()).thenReturn(mBluetoothDevice);
        when(mBluetoothDevice.getAnonymizedAddress()).thenReturn(TEST_DEVICE_ADDRESS);
        when(mCachedBluetoothDevice.getAddress()).thenReturn(TEST_DEVICE_ADDRESS);
        when(mCachedBluetoothDevice.isHearingDevice()).thenReturn(true);
        when(mCachedBluetoothDevice.isActiveDevice(BluetoothProfile.HEARING_AID)).thenReturn(true);
        when(mCachedDeviceManager.getCachedDevicesCopy()).thenReturn(
                ImmutableList.of(mCachedBluetoothDevice));
        doReturn(hearingDeviceAttribute).when(
                mAudioRoutingHelper).getMatchedHearingDeviceAttributesForOutput(any());
        when(mAudioRoutingHelper.getSupportedStrategies(any())).thenReturn(
                List.of(mAudioProductStrategyNotif));

        mController = new TestHearingDeviceAudioRoutingBaseSwitchPreferenceController(mContext,
                "test_key", mAudioRoutingHelper, mLocalBluetoothManager);
    }

    @Test
    public void updateState_routingValueAuto_checkedAndExpectedSummary() {
        mController.setRoutingValue(mContext, HearingAidAudioRoutingConstants.RoutingValue.AUTO);

        mController.updateState(mSwitchPreference);

        assertThat(mSwitchPreference.isChecked()).isTrue();
        assertThat(mSwitchPreference.getSummary().toString()).isEqualTo(
                mContext.getString(
                        R.string.accessibility_hearing_device_routing_hearing_device_summary));
    }

    @Test
    public void updateState_routingValueBuiltInDevice_uncheckedAndExpectedSummary() {
        mController.setRoutingValue(mContext,
                HearingAidAudioRoutingConstants.RoutingValue.BUILTIN_DEVICE);

        mController.updateState(mSwitchPreference);

        assertThat(mSwitchPreference.isChecked()).isFalse();
        assertThat(mSwitchPreference.getSummary().toString()).isEqualTo(
                mContext.getString(
                        R.string.accessibility_hearing_device_routing_device_speaker_summary));
    }

    @Test
    public void onPreferenceChange_checked_saveAutoAndSetRoutingAuto() {
        mController.onPreferenceChange(mSwitchPreference, true);
        ShadowLooper.idleMainLooper();

        assertThat(mController.getRoutingValue(mContext)).isEqualTo(
                HearingAidAudioRoutingConstants.RoutingValue.AUTO);
        verify(mAudioRoutingHelper).configureRoutingStrategies(any(), any(),
                eq(HearingAidAudioRoutingConstants.RoutingValue.AUTO));
    }

    @Test
    public void onPreferenceChange_unchecked_saveBuiltInAndSetRoutingBuiltIn() {
        mController.onPreferenceChange(mSwitchPreference, false);
        ShadowLooper.idleMainLooper();

        assertThat(mController.getRoutingValue(mContext)).isEqualTo(
                HearingAidAudioRoutingConstants.RoutingValue.BUILTIN_DEVICE);
        verify(mAudioRoutingHelper).configureRoutingStrategies(any(), any(),
                eq(HearingAidAudioRoutingConstants.RoutingValue.BUILTIN_DEVICE));
    }

    @Test
    public void onPreferenceChange_noSupportedStrategies_returnFalse() {
        when(mAudioRoutingHelper.getSupportedStrategies(any())).thenReturn(new ArrayList<>());

        boolean handled = mController.onPreferenceChange(mSwitchPreference, true);

        assertThat(handled).isFalse();
        verify(mAudioRoutingHelper, never()).configureRoutingStrategies(any(), any(),
                anyInt());
    }

    private static class TestHearingDeviceAudioRoutingBaseSwitchPreferenceController extends
            HearingDeviceAudioRoutingBaseSwitchPreferenceController {

        private static int sSavedRoutingValue;

        TestHearingDeviceAudioRoutingBaseSwitchPreferenceController(Context context,
                String preferenceKey, HearingAidAudioRoutingHelper audioRoutingHelper,
                LocalBluetoothManager localBluetoothManager) {
            super(context, preferenceKey, audioRoutingHelper, MoreExecutors.directExecutor(),
                    localBluetoothManager);
        }

        @Override
        protected int[] getAudioAttributeUsages() {
            return new int[]{AudioAttributes.USAGE_NOTIFICATION};
        }

        @Override
        protected void setRoutingValue(Context context, int routingValue) {
            sSavedRoutingValue = routingValue;
        }

        @Override
        protected int getRoutingValue(Context context) {
            return sSavedRoutingValue;
        }
    }
}
