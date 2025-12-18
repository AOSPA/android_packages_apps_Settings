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

package com.android.settings.system;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;

import com.android.settings.testutils.XmlTestUtils;
import com.android.settings.R;
import com.android.settings.testutils.shadow.SettingsShadowResources;
import com.android.settings.testutils.shadow.ShadowDesktopSettingsUtils;
import com.android.settings.testutils.shadow.ShadowUserManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        SettingsShadowResources.class,
        ShadowUserManager.class,
        ShadowDesktopSettingsUtils.class
})
public class SystemDashboardFragmentTest {

    private Context mContext;
    private SystemDashboardFragment mFragment;

    @Before
    public void setup() {
        SettingsShadowResources.overrideResource(
                com.android.internal.R.bool.config_supportSystemNavigationKeys, true);
        ShadowUserManager.getShadow().setIsAdminUser(true);
        mContext = RuntimeEnvironment.application;
        mFragment = spy(new SystemDashboardFragment());
        when(mFragment.getContext()).thenReturn(mContext);
    }

    @After
    public void tearDown() {
        SettingsShadowResources.reset();
    }

    @Test
    public void testNonIndexableKeys_existInXmlLayout() {
        final List<String> niks = SystemDashboardFragment.SEARCH_INDEX_DATA_PROVIDER
                .getNonIndexableKeys(mContext);
        final int xmlId = mFragment.getPreferenceScreenResId();

        final List<String> keys = XmlTestUtils.getKeysFromPreferenceXml(mContext, xmlId);

        assertThat(keys).containsAtLeastElementsIn(niks);
    }

    @Test
    public void getPreferenceScreenResId_shouldShowDeviceCategoryFalse_returnsFragmentXml() {
        ShadowDesktopSettingsUtils.setShouldShow(false);

        final int xmlId = mFragment.getPreferenceScreenResId();

        assertThat(xmlId).isEqualTo(R.xml.system_dashboard_fragment);
    }

    @Test
    public void getPreferenceScreenResId_shouldShowDeviceCategoryTrue_returnsDesktopXml() {
        ShadowDesktopSettingsUtils.setShouldShow(true);

        final int xmlId = mFragment.getPreferenceScreenResId();

        assertThat(xmlId).isEqualTo(R.xml.system_dashboard_fragment_desktop);
    }

    @Test
    public void getPreferenceScreenResId_shouldShowDeviceCategoryFalse_includesKeyboardSettings() {
        ShadowDesktopSettingsUtils.setShouldShow(false);

        final int xmlId = mFragment.getPreferenceScreenResId();
        final List<String> keys = XmlTestUtils.getKeysFromPreferenceXml(mContext, xmlId);

        assertThat(keys).contains("Keyboard_settings");
    }

    @Test
    public void getPreferenceScreenResId_shouldShowDeviceCategoryTrue_excludesKeyboardSettings() {
        ShadowDesktopSettingsUtils.setShouldShow(true);

        final int xmlId = mFragment.getPreferenceScreenResId();
        final List<String> keys = XmlTestUtils.getKeysFromPreferenceXml(mContext, xmlId);

        assertThat(keys).doesNotContain("Keyboard_settings");
    }

    @Test
    public void getPreferenceScreenResId_shouldShowDeviceCategoryFalse_includesMouseSettings() {
        ShadowDesktopSettingsUtils.setShouldShow(false);

        final int xmlId = mFragment.getPreferenceScreenResId();
        final List<String> keys = XmlTestUtils.getKeysFromPreferenceXml(mContext, xmlId);

        assertThat(keys).contains("mouse_settings");
    }

    @Test
    public void getPreferenceScreenResId_shouldShowDeviceCategoryTrue_excludesMouseSettings() {
        ShadowDesktopSettingsUtils.setShouldShow(true);

        final int xmlId = mFragment.getPreferenceScreenResId();
        final List<String> keys = XmlTestUtils.getKeysFromPreferenceXml(mContext, xmlId);

        assertThat(keys).doesNotContain("mouse_settings");
    }

    @Test
    public void getPreferenceScreenResId_shouldShowDeviceCategoryFalse_includesTrackpadSettings() {
        ShadowDesktopSettingsUtils.setShouldShow(false);

        final int xmlId = mFragment.getPreferenceScreenResId();
        final List<String> keys = XmlTestUtils.getKeysFromPreferenceXml(mContext, xmlId);

        assertThat(keys).contains("trackpad_settings");
    }

    @Test
    public void getPreferenceScreenResId_shouldShowDeviceCategoryTrue_excludesTrackpadSettings() {
        ShadowDesktopSettingsUtils.setShouldShow(true);

        final int xmlId = mFragment.getPreferenceScreenResId();
        final List<String> keys = XmlTestUtils.getKeysFromPreferenceXml(mContext, xmlId);

        assertThat(keys).doesNotContain("trackpad_settings");
    }

    @Test
    public void getPreferenceScreenResId_shouldShowDeviceCategoryFalse_includesTouchpadSettings() {
        ShadowDesktopSettingsUtils.setShouldShow(false);

        final int xmlId = mFragment.getPreferenceScreenResId();
        final List<String> keys = XmlTestUtils.getKeysFromPreferenceXml(mContext, xmlId);

        assertThat(keys).contains("touchpad_settings");
    }

    @Test
    public void getPreferenceScreenResId_shouldShowDeviceCategoryTrue_excludesTouchpadSettings() {
        ShadowDesktopSettingsUtils.setShouldShow(true);

        final int xmlId = mFragment.getPreferenceScreenResId();
        final List<String> keys = XmlTestUtils.getKeysFromPreferenceXml(mContext, xmlId);

        assertThat(keys).doesNotContain("touchpad_settings");
    }
}
