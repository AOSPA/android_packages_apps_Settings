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

package com.android.settings.accessibility;

import static com.google.common.truth.Truth.assertThat;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.platform.test.annotations.RequiresFlagsDisabled;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;
import android.provider.SearchIndexableResource;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.accessibility.actiontimeout.ui.ActionTimeoutSettingsScreen;
import com.android.settings.testutils.XmlTestUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.List;

/** Tests for {@link AccessibilityControlTimeoutPreferenceFragment}. */
@RunWith(RobolectricTestRunner.class)
public class AccessibilityControlTimeoutPreferenceFragmentTest {
    @Rule
    public CheckFlagsRule mSetFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    private final Context mContext = ApplicationProvider.getApplicationContext();
    private AccessibilityControlTimeoutPreferenceFragment mFragment;

    @Before
    public void setUp() {
        mFragment = new AccessibilityControlTimeoutPreferenceFragment();
    }

    @Test
    public void getMetricsCategory_returnsCorrectCategory() {
        assertThat(mFragment.getMetricsCategory()).isEqualTo(
                SettingsEnums.ACCESSIBILITY_TIMEOUT);
    }

    @Test
    public void getPreferenceScreenBindingKey_returnsCorrectScreenKey() {
        assertThat(mFragment.getPreferenceScreenBindingKey(mContext)).isEqualTo(
                ActionTimeoutSettingsScreen.KEY);
    }

    @Test
    public void getPreferenceScreenResId_returnsCorrectXml() {
        assertThat(mFragment.getPreferenceScreenResId()).isEqualTo(
                R.xml.accessibility_control_timeout_settings);
    }

    @Test
    public void getHelpResource_returnsCorrectHelpResource() {
        assertThat(mFragment.getHelpResource()).isEqualTo(R.string.help_url_timeout);
    }

    @Test
    public void getLogTag_returnsCorrectTag() {
        assertThat(mFragment.getLogTag()).isEqualTo(
                "AccessibilityControlTimeoutPreferenceFragment");
    }

    @RequiresFlagsDisabled(Flags.FLAG_CATALYST_TIME_TO_TAKE_ACTION_SCREEN)
    @Test
    public void getNonIndexableKeys_existInXmlLayout() {
        final List<String> niks =
                AccessibilityControlTimeoutPreferenceFragment.SEARCH_INDEX_DATA_PROVIDER
                        .getNonIndexableKeys(mContext);
        final List<String> keys =
                XmlTestUtils.getKeysFromPreferenceXml(mContext,
                        R.xml.accessibility_control_timeout_settings);

        assertThat(keys).containsAtLeastElementsIn(niks);
    }

    @RequiresFlagsDisabled(Flags.FLAG_CATALYST_TIME_TO_TAKE_ACTION_SCREEN)
    @Test
    public void getSearchIndexDataProvider_verifyXmlResourcesToIndex() {
        List<SearchIndexableResource> searchIndexableResource =
                AccessibilityControlTimeoutPreferenceFragment.SEARCH_INDEX_DATA_PROVIDER
                        .getXmlResourcesToIndex(mContext, /* enabled= */ true);
        assertThat(searchIndexableResource.getFirst().xmlResId)
                .isEqualTo(R.xml.accessibility_control_timeout_settings);
    }

    @RequiresFlagsEnabled(Flags.FLAG_CATALYST_TIME_TO_TAKE_ACTION_SCREEN)
    @Test
    public void getSearchIndexDataProvider_returnsNull() {
        List<SearchIndexableResource> searchIndexableResource =
                AccessibilityControlTimeoutPreferenceFragment.SEARCH_INDEX_DATA_PROVIDER
                        .getXmlResourcesToIndex(mContext, /* enabled= */ true);

        assertThat(searchIndexableResource).isNull();
    }
}
