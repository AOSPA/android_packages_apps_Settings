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

package com.android.settings.security;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class ShowSecretsTouchPreferenceControllerTest {

    @Mock private PreferenceScreen mScreen;

    private Context mContext;
    private ShowSecretsTouchPreferenceController mController;
    private Preference mPreference;
    private boolean mIsSplitSystemEnabled;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = new TestShowSecretsTouchPreferenceController(mContext);
        mPreference = new Preference(mContext);
        mPreference.setKey(mController.getPreferenceKey());
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
    }

    @Test
    public void isAvailable_splitDisabled_isFalse() {
        mIsSplitSystemEnabled = false;
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_splitEnabled_isTrue() {
        mIsSplitSystemEnabled = true;
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isChecked_settingIsOff_false() {
        final ContentResolver contentResolver = mContext.getContentResolver();
        Settings.Secure.putInt(contentResolver, Settings.Secure.TEXT_SHOW_PASSWORD_TOUCH, 0);

        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void isChecked_settingIsOn_true() {
        final ContentResolver contentResolver = mContext.getContentResolver();
        Settings.Secure.putInt(contentResolver, Settings.Secure.TEXT_SHOW_PASSWORD_TOUCH, 1);
        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void changePref_turnOn_shouldChangeSettingTo1() {
        final ContentResolver contentResolver = mContext.getContentResolver();
        Settings.Secure.putInt(contentResolver, Settings.Secure.TEXT_SHOW_PASSWORD_TOUCH, 0);
        assertThat(
                        Settings.Secure.getInt(
                                contentResolver, Settings.Secure.TEXT_SHOW_PASSWORD_TOUCH, 1))
                .isEqualTo(0);
        assertThat(mController.isChecked()).isFalse();

        mController.onPreferenceChange(mPreference, true);

        assertThat(mController.isChecked()).isTrue();
        assertThat(
                        Settings.Secure.getInt(
                                contentResolver, Settings.Secure.TEXT_SHOW_PASSWORD_TOUCH, 0))
                .isEqualTo(1);
        assertThat(Settings.System.getInt(contentResolver, Settings.System.TEXT_SHOW_PASSWORD, 0))
                .isEqualTo(1);
    }

    @Test
    public void changePref_turnOff_shouldChangeSettingTo0() {
        final ContentResolver contentResolver = mContext.getContentResolver();
        Settings.Secure.putInt(contentResolver, Settings.Secure.TEXT_SHOW_PASSWORD_TOUCH, 1);
        assertThat(
                        Settings.Secure.getInt(
                                contentResolver, Settings.Secure.TEXT_SHOW_PASSWORD_TOUCH, 0))
                .isEqualTo(1);
        assertThat(mController.isChecked()).isTrue();

        mController.onPreferenceChange(mPreference, false);

        assertThat(mController.isChecked()).isFalse();
        assertThat(
                        Settings.Secure.getInt(
                                contentResolver, Settings.Secure.TEXT_SHOW_PASSWORD_TOUCH, 1))
                .isEqualTo(0);
        assertThat(Settings.System.getInt(contentResolver, Settings.System.TEXT_SHOW_PASSWORD, 1))
                .isEqualTo(0);
    }

    private class TestShowSecretsTouchPreferenceController
            extends ShowSecretsTouchPreferenceController {
        TestShowSecretsTouchPreferenceController(Context context) {
            super(context);
        }

        @Override
        boolean areSplitSettingsEnabled() {
            return mIsSplitSystemEnabled;
        }
    }
}
