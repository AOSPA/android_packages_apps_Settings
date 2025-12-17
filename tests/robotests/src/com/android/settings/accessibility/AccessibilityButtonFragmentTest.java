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

package com.android.settings.accessibility;

import static com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.SOFTWARE;

import static com.google.common.truth.Truth.assertThat;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.platform.test.annotations.RequiresFlagsDisabled;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;
import android.provider.SearchIndexableResource;
import android.provider.Settings;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.core.app.ApplicationProvider;

import com.android.internal.accessibility.util.ShortcutUtils;
import com.android.settings.R;
import com.android.settings.accessibility.buttonshortcutsetting.ui.ButtonShortcutSettingScreen;
import com.android.settings.testutils.XmlTestUtils;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;

import java.util.List;

/** Tests for {@link AccessibilityButtonFragment}. */
@RunWith(RobolectricTestRunner.class)
public class AccessibilityButtonFragmentTest {
    @Rule
    public CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private FragmentScenario<AccessibilityButtonFragment> mFragmentScenario;

    @After
    public void cleanUp() {
        if (mFragmentScenario != null) {
            mFragmentScenario.close();
        }
    }

    @Test
    public void getMetricsCategory_returnsCorrectCategory() {
        assertThat(new AccessibilityButtonFragment().getMetricsCategory()).isEqualTo(
                SettingsEnums.ACCESSIBILITY_BUTTON_SETTINGS);
    }

    @Test
    public void getLogTag_returnsCorrectTag() {
        assertThat(new AccessibilityButtonFragment().getLogTag())
                .isEqualTo("AccessibilityButtonFragment");
    }

    @Test
    public void launchFragment_setCorrectTitle() {
        mFragmentScenario = launchFragment();

        mFragmentScenario.onFragment(fragment -> {
            assertThat(fragment.getActivity().getTitle().toString()).isEqualTo(
                    mContext.getString(R.string.accessibility_button_title));
        });
    }

    @Test
    public void getPreferenceScreenBindingKey_returnsCorrespondingScreenKey() {
        assertThat(new AccessibilityButtonFragment().getPreferenceScreenBindingKey(mContext))
                .isEqualTo(ButtonShortcutSettingScreen.KEY);
    }

    @RequiresFlagsDisabled(Flags.FLAG_CATALYST_A11Y_BUTTON_SHORTCUT_SETTING)
    @Test
    public void getNonIndexableKeys_noTargets_doesNotExistInXmlLayout() {
        Settings.Secure.putStringForUser(mContext.getContentResolver(),
                ShortcutUtils.convertToKey(SOFTWARE), "", mContext.getUserId());
        final List<String> niks = AccessibilityButtonFragment.SEARCH_INDEX_DATA_PROVIDER
                .getNonIndexableKeys(mContext);
        final List<String> keys =
                XmlTestUtils.getKeysFromPreferenceXml(mContext,
                        R.xml.accessibility_button_settings);

        assertThat(keys).isNotNull();
        assertThat(niks).containsAtLeastElementsIn(keys);
    }

    @RequiresFlagsDisabled(Flags.FLAG_CATALYST_A11Y_BUTTON_SHORTCUT_SETTING)
    @Test
    public void getNonIndexableKeys_hasTargets_expectedKeys() {
        Settings.Secure.putStringForUser(mContext.getContentResolver(),
                ShortcutUtils.convertToKey(SOFTWARE), "Foo", mContext.getUserId());
        final List<String> niks = AccessibilityButtonFragment.SEARCH_INDEX_DATA_PROVIDER
                .getNonIndexableKeys(mContext);

        // Some keys should show up anyway, as they're flagged as unsearchable in the xml.
        assertThat(niks).containsAtLeast(
                "accessibility_button_preview",
                "accessibility_button_footer");
    }

    @RequiresFlagsDisabled(Flags.FLAG_CATALYST_A11Y_BUTTON_SHORTCUT_SETTING)
    @Test
    public void getSearchIndexDataProvider_verifyXmlResourcesToIndex() {
        List<SearchIndexableResource> searchIndexableResource =
                AccessibilityButtonFragment.SEARCH_INDEX_DATA_PROVIDER.getXmlResourcesToIndex(
                        mContext, /* enabled= */ true);
        assertThat(searchIndexableResource.getFirst().xmlResId)
                .isEqualTo(R.xml.accessibility_button_settings);
    }

    @RequiresFlagsEnabled(Flags.FLAG_CATALYST_A11Y_BUTTON_SHORTCUT_SETTING)
    @Test
    public void getSearchIndexDataProvider_returnsNull() {
        List<SearchIndexableResource> searchIndexableResource =
                AccessibilityButtonFragment.SEARCH_INDEX_DATA_PROVIDER.getXmlResourcesToIndex(
                        mContext, /* enabled= */ true);

        assertThat(searchIndexableResource).isNull();
    }

    private FragmentScenario<AccessibilityButtonFragment> launchFragment() {
        return FragmentScenario.launch(AccessibilityButtonFragment.class);
    }
}
