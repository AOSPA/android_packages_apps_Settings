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

package com.android.settings.development;

import static com.android.settings.development.EnableWebAppMinterPreferenceController.SETTING_VALUE_OFF;
import static com.android.settings.development.EnableWebAppMinterPreferenceController.SETTING_VALUE_ON;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.Settings;

import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Collections;

@RunWith(RobolectricTestRunner.class)
public class EnableWebAppMinterPreferenceControllerTest {
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    @Mock private SwitchPreference mPreference;
    @Mock private PreferenceScreen mPreferenceScreen;
    @Mock private PackageManager mPackageManager;

    private Context mContext;
    private EnableWebAppMinterPreferenceController mController;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = org.mockito.Mockito.spy(RuntimeEnvironment.application);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);

        mController = new EnableWebAppMinterPreferenceController(mContext);
        when(mPreferenceScreen.findPreference(mController.getPreferenceKey()))
                .thenReturn(mPreference);
        mController.displayPreference(mPreferenceScreen);
    }

    private void setIsAvailable(boolean isAvailable) {
        ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.serviceInfo = new ServiceInfo();
        resolveInfo.serviceInfo.metaData = new Bundle();
        resolveInfo.serviceInfo.metaData.putBoolean(
                "com.android.webapp.developer_option_visible", isAvailable);
        when(mPackageManager.queryIntentServices(any(Intent.class), anyInt()))
                .thenReturn(Collections.singletonList(resolveInfo));
    }

    @Test
    public void isAvailable_whenFlagIsEnabled_shouldReturnTrue() {
        mSetFlagsRule.enableFlags(com.android.webapp.flags.Flags.FLAG_ENABLE_WEB_APP_SERVICE_V2);
        setIsAvailable(true);
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_whenFlagIsDisabled_shouldReturnFalse() {
        mSetFlagsRule.disableFlags(com.android.webapp.flags.Flags.FLAG_ENABLE_WEB_APP_SERVICE_V2);
        setIsAvailable(true);
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_whenFlagEnabledButShouldNotShow_shouldReturnFalse() {
        mSetFlagsRule.enableFlags(com.android.webapp.flags.Flags.FLAG_ENABLE_WEB_APP_SERVICE_V2);
        setIsAvailable(false);
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_whenFlagEnabledButNoMetadata_shouldReturnFalse() {
        mSetFlagsRule.enableFlags(com.android.webapp.flags.Flags.FLAG_ENABLE_WEB_APP_SERVICE_V2);
        ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.serviceInfo = new ServiceInfo();
        // metaData is null
        when(mPackageManager.queryIntentServices(any(Intent.class), anyInt()))
                .thenReturn(Collections.singletonList(resolveInfo));
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_whenFlagEnabledButNoService_shouldReturnFalse() {
        mSetFlagsRule.enableFlags(com.android.webapp.flags.Flags.FLAG_ENABLE_WEB_APP_SERVICE_V2);
        when(mPackageManager.queryIntentServices(any(Intent.class), anyInt()))
                .thenReturn(Collections.emptyList());
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    @EnableFlags(com.android.webapp.flags.Flags.FLAG_ENABLE_WEB_APP_SERVICE_V2)
    public void onPreferenceChange_settingEnabled_enableWebAppMinterShouldBeOn() {
        mController.onPreferenceChange(mPreference, true /* new value */);

        final int mode =
                Settings.Global.getInt(
                        mContext.getContentResolver(),
                        Settings.Global.ENABLE_WEBAPP_MINTER,
                        -1 /* default */);

        assertThat(mode).isEqualTo(SETTING_VALUE_ON);
    }

    @Test
    @EnableFlags(com.android.webapp.flags.Flags.FLAG_ENABLE_WEB_APP_SERVICE_V2)
    public void onPreferenceChange_settingDisabled_enableWebAppMinterShouldBeOff() {
        mController.onPreferenceChange(mPreference, false /* new value */);

        final int mode =
                Settings.Global.getInt(
                        mContext.getContentResolver(),
                        Settings.Global.ENABLE_WEBAPP_MINTER,
                        -1 /* default */);

        assertThat(mode).isEqualTo(SETTING_VALUE_OFF);
    }

    @Test
    @EnableFlags(com.android.webapp.flags.Flags.FLAG_ENABLE_WEB_APP_SERVICE_V2)
    public void updateState_settingDisabled_preferenceShouldNotBeChecked() {
        Settings.Global.putInt(
                mContext.getContentResolver(),
                Settings.Global.ENABLE_WEBAPP_MINTER,
                SETTING_VALUE_OFF);
        mController.updateState(mPreference);

        verify(mPreference).setChecked(false);
    }

    @Test
    @EnableFlags(com.android.webapp.flags.Flags.FLAG_ENABLE_WEB_APP_SERVICE_V2)
    public void updateState_settingEnabled_preferenceShouldBeChecked() {
        Settings.Global.putInt(
                mContext.getContentResolver(),
                Settings.Global.ENABLE_WEBAPP_MINTER,
                SETTING_VALUE_ON);
        mController.updateState(mPreference);

        verify(mPreference).setChecked(true);
    }

    @Test
    @EnableFlags(com.android.webapp.flags.Flags.FLAG_ENABLE_WEB_APP_SERVICE_V2)
    public void onDeveloperOptionsSwitchDisabled_shouldDisablePreference() {
        mController.onDeveloperOptionsSwitchDisabled();

        final int mode =
                Settings.Global.getInt(
                        mContext.getContentResolver(),
                        Settings.Global.ENABLE_WEBAPP_MINTER,
                        -1 /* default */);

        assertThat(mode).isEqualTo(SETTING_VALUE_OFF);
        verify(mPreference).setChecked(false);
    }
}
