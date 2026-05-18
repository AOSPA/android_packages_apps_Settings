/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.applications.assist;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.permission.flags.Flags;
import android.platform.test.annotations.RequiresFlagsDisabled;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;

import androidx.test.core.app.ApplicationProvider;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class ManageAssistTest {

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    private final Context mContext = ApplicationProvider.getApplicationContext();

    private ManageAssist mSettings;

    @Before
    public void setUp() {
        mSettings = new ManageAssist();
    }

    @Test
    public void testGetMetricsCategory() {
        assertThat(mSettings.getMetricsCategory())
                .isEqualTo(MetricsProto.MetricsEvent.APPLICATIONS_MANAGE_ASSIST);
    }

    @Test
    public void testGetCategoryKey() {
        assertThat(mSettings.getCategoryKey()).isNull();
    }

    @Test
    public void testGetPreferenceScreenResId() {
        assertThat(mSettings.getPreferenceScreenResId()).isEqualTo(R.xml.manage_assist);
    }

    @RequiresFlagsDisabled(Flags.FLAG_ASSIST_SETTINGS_PRIVACY_IMPROVEMENTS_ENABLED)
    @Test
    public void testSearchIndexProvider_flagDisabled_hasXmlResourcesToIndex() {
        assertThat(ManageAssist.SEARCH_INDEX_DATA_PROVIDER.getXmlResourcesToIndex(mContext, true))
                .isNotNull();
    }

    @RequiresFlagsEnabled(Flags.FLAG_ASSIST_SETTINGS_PRIVACY_IMPROVEMENTS_ENABLED)
    @Test
    public void testSearchIndexProvider_flagEnabled_noXmlResourcesToIndex() {
        assertThat(ManageAssist.SEARCH_INDEX_DATA_PROVIDER.getXmlResourcesToIndex(mContext, true))
                .isNull();
    }

    @RequiresFlagsDisabled(Flags.FLAG_ASSIST_SETTINGS_PRIVACY_IMPROVEMENTS_ENABLED)
    @Test
    public void testSearchIndexProvider_flagDisabled_hasPreferenceControllers() {
        assertThat(ManageAssist.SEARCH_INDEX_DATA_PROVIDER.createPreferenceControllers(mContext))
                .isNotNull();
    }

    @RequiresFlagsEnabled(Flags.FLAG_ASSIST_SETTINGS_PRIVACY_IMPROVEMENTS_ENABLED)
    @Test
    public void testSearchIndexProvider_flagEnabled_noPreferenceControllers() {
        assertThat(ManageAssist.SEARCH_INDEX_DATA_PROVIDER.createPreferenceControllers(mContext))
                .isNull();
    }
}
