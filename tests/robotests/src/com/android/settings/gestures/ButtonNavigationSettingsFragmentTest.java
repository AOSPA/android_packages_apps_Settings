/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.gestures;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowPackageManager;

@RunWith(RobolectricTestRunner.class)
public class ButtonNavigationSettingsFragmentTest {
    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private ButtonNavigationSettingsFragment mFragment;

    @Before
    public void setUp() {
        mFragment = spy(new ButtonNavigationSettingsFragment());
    }

    @Test
    @RequiresFlagsEnabled(com.android.settings.flags.Flags.FLAG_CATALYST_SETTINGS_SEARCH)
    public void searchIndexer_getResources_isNull() {
        assertThat(ButtonNavigationSettingsFragment.SEARCH_INDEX_DATA_PROVIDER
                .getXmlResourcesToIndex(mContext, true)).isNull();
    }

    @Test
    public void getPreferenceScreenBindingKey_returnsCorrectKey() {
        assertThat(mFragment.getPreferenceScreenBindingKey(mContext)).isEqualTo(
                ButtonNavigationSettingsScreen.KEY);
    }

    private static void addPackageToPackageManager(Context context, String pkg) {
        ShadowPackageManager shadowPm = shadowOf(context.getPackageManager());
        PackageInfo pi = new PackageInfo();
        pi.packageName = pkg;
        shadowPm.installPackage(pi);
    }

}
