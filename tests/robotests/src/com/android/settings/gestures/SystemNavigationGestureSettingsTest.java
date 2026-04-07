/*
 * Copyright (C) 2019 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.gestures;

import static android.os.UserHandle.USER_CURRENT;
import static android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_2BUTTON;
import static android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_2BUTTON_OVERLAY;
import static android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_3BUTTON;
import static android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_3BUTTON_OVERLAY;
import static android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_GESTURAL;
import static android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_GESTURAL_OVERLAY;

import static com.android.settings.gestures.SystemNavigationGestureSettings.KEY_SYSTEM_NAV_2BUTTONS;
import static com.android.settings.gestures.SystemNavigationGestureSettings.KEY_SYSTEM_NAV_3BUTTONS;
import static com.android.settings.gestures.SystemNavigationGestureSettings.KEY_SYSTEM_NAV_GESTURAL;

import static com.google.common.truth.Truth.assertThat;

import static junit.framework.Assert.assertEquals;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.content.ComponentName;
import android.content.Context;
import android.content.om.IOverlayManager;
import android.content.om.OverlayInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.SearchIndexableResource;
import android.view.LayoutInflater;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceViewHolder;
import androidx.test.core.app.ActivityScenario;

import com.android.internal.R;
import com.android.settings.testutils.shadow.SettingsShadowResources;
import com.android.settings.utils.CandidateInfoExtra;
import com.android.settingslib.search.SearchIndexableRaw;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowPackageManager;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = SettingsShadowResources.class)
public class SystemNavigationGestureSettingsTest {
    @Rule
    public SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private Context mContext;
    private SystemNavigationGestureSettings mSettings;

    @Mock
    private IOverlayManager mOverlayManager;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private OverlayInfo mOverlayInfoEnabled;
    @Mock
    private OverlayInfo mOverlayInfoDisabled;
    private ShadowPackageManager mShadowPackageManager;

    private SelectorWithWidgetPreference mSelectorWithWidgetPreference;
    private ActivityScenario<FragmentActivity> mScenario;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.getApplication());
        mShadowPackageManager = shadowOf(mContext.getPackageManager());
        mSettings = new SystemNavigationGestureSettings();

        mSelectorWithWidgetPreference = new SelectorWithWidgetPreference(mContext);
        View preferenceView = LayoutInflater.from(mContext)
                .inflate(mSelectorWithWidgetPreference.getLayoutResource(), null /* root */);
        PreferenceViewHolder preferenceViewHolder =
                PreferenceViewHolder.createInstanceForTests(preferenceView);
        mSelectorWithWidgetPreference.onBindViewHolder(preferenceViewHolder);

        when(mOverlayInfoDisabled.isEnabled()).thenReturn(false);
        when(mOverlayInfoEnabled.isEnabled()).thenReturn(true);
        when(mOverlayManager.getOverlayInfo(any(), anyInt())).thenReturn(mOverlayInfoDisabled);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
    }

    @After
    public void tearDown() {
        if (mScenario != null) {
            mScenario.close();
        }
    }

    @Test
    public void searchIndexProvider_shouldIndexResource() {
        final List<SearchIndexableResource> indexRes =
                SystemNavigationGestureSettings.SEARCH_INDEX_DATA_PROVIDER.getXmlResourcesToIndex(
                        RuntimeEnvironment.application, true /* enabled */);

        assertThat(indexRes).isNotNull();
        assertThat(indexRes.get(0).xmlResId).isEqualTo(mSettings.getPreferenceScreenResId());
    }

    @Test
    public void searchIndexProvider_gesturePackageExist_shouldBeIndexed()
            throws NameNotFoundException {
        PackageInfo info = new PackageInfo();
        when(mPackageManager.getPackageInfo(NAV_BAR_MODE_GESTURAL_OVERLAY, 0))
                .thenReturn(info);

        final List<SearchIndexableRaw> indexRaws =
                SystemNavigationGestureSettings.SEARCH_INDEX_DATA_PROVIDER
                        .getRawDataToIndex(mContext, true /* enabled */);

        assertThat(indexRaws).isNotEmpty();
    }

    @Test
    public void searchIndexProvider_noNavigationPackageExist_shouldReturnEmpty() {
        final List<SearchIndexableRaw> indexRaws =
                SystemNavigationGestureSettings.SEARCH_INDEX_DATA_PROVIDER
                        .getRawDataToIndex(mContext, true /* enabled */);

        assertThat(indexRaws).isEmpty();
    }

    @Test
    public void testGetCurrentSystemNavigationMode() {
        SettingsShadowResources.overrideResource(
                R.integer.config_navBarInteractionMode, NAV_BAR_MODE_GESTURAL);
        assertEquals(KEY_SYSTEM_NAV_GESTURAL, mSettings.getCurrentSystemNavigationMode(mContext));

        SettingsShadowResources.overrideResource(
                R.integer.config_navBarInteractionMode, NAV_BAR_MODE_3BUTTON);
        assertEquals(KEY_SYSTEM_NAV_3BUTTONS, mSettings.getCurrentSystemNavigationMode(mContext));

        SettingsShadowResources.overrideResource(
                R.integer.config_navBarInteractionMode, NAV_BAR_MODE_2BUTTON);
        assertEquals(KEY_SYSTEM_NAV_2BUTTONS, mSettings.getCurrentSystemNavigationMode(mContext));
    }

    @Test
    public void testSetCurrentSystemNavigationMode() throws Exception {
        mSettings.setCurrentSystemNavigationMode(mOverlayManager, KEY_SYSTEM_NAV_GESTURAL);
        verify(mOverlayManager, times(1)).setEnabledExclusiveInCategory(
                NAV_BAR_MODE_GESTURAL_OVERLAY, USER_CURRENT);

        mSettings.setCurrentSystemNavigationMode(mOverlayManager, KEY_SYSTEM_NAV_2BUTTONS);
        verify(mOverlayManager, times(1)).setEnabledExclusiveInCategory(
                NAV_BAR_MODE_2BUTTON_OVERLAY, USER_CURRENT);

        mSettings.setCurrentSystemNavigationMode(mOverlayManager, KEY_SYSTEM_NAV_3BUTTONS);
        verify(mOverlayManager, times(1)).setEnabledExclusiveInCategory(
                NAV_BAR_MODE_3BUTTON_OVERLAY, USER_CURRENT);
    }

    @Test
    public void initializeA11yNode_gestureNav_hasCustomClickAction() {
        PreferenceViewHolder preferenceViewHolder = setUpFragmentWithViewHolder();

        bindPreferenceExtra(KEY_SYSTEM_NAV_GESTURAL, preferenceViewHolder);

        View widget = preferenceViewHolder.findViewById(
                com.android.settingslib.widget.preference.selector.R.id.selector_extra_widget);
        AccessibilityNodeInfo info = new AccessibilityNodeInfo();
        widget.onInitializeAccessibilityNodeInfo(info);

        assertThat(info.getActionList()).contains(new AccessibilityNodeInfo.AccessibilityAction(
                AccessibilityNodeInfo.ACTION_CLICK, mContext.getString(
                com.android.settings.R.string.gesture_settings_extra_button_hint)));
    }

    @Test
    public void initializeA11yNode_buttonNav_hasCustomClickAction() {
        PreferenceViewHolder preferenceViewHolder = setUpFragmentWithViewHolder();

        bindPreferenceExtra(KEY_SYSTEM_NAV_3BUTTONS, preferenceViewHolder);

        View widget = preferenceViewHolder.findViewById(
                com.android.settingslib.widget.preference.selector.R.id.selector_extra_widget);
        AccessibilityNodeInfo info = new AccessibilityNodeInfo();
        widget.onInitializeAccessibilityNodeInfo(info);

        assertThat(info.getActionList()).contains(new AccessibilityNodeInfo.AccessibilityAction(
                AccessibilityNodeInfo.ACTION_CLICK, mContext.getString(
                com.android.settings.R.string.button_navigation_settings_extra_button_hint)));
    }

    private CandidateInfoExtra getMockCandidateInfo(String key) {
        CandidateInfoExtra info = Mockito.mock(CandidateInfoExtra.class);
        when(info.loadSummary()).thenReturn("");
        when(info.getKey()).thenReturn(key);

        assertThat(info).isInstanceOf(CandidateInfoExtra.class);
        return info;
    }

    private void bindPreferenceExtra(String key, PreferenceViewHolder preferenceViewHolder) {
        CandidateInfoExtra infoExtra = getMockCandidateInfo(key);
        mSettings.bindPreferenceExtra(mSelectorWithWidgetPreference, null,
                infoExtra, null, null);
        // Call onBindViewHolder after bindPreferenceExtra to force extra widget to populate
        mSelectorWithWidgetPreference.onBindViewHolder(preferenceViewHolder);
    }

    private PreferenceViewHolder setUpFragmentWithViewHolder() {
        mShadowPackageManager.addActivityIfNotPresent(
                new ComponentName(mContext, FragmentActivity.class));
        mScenario = ActivityScenario.launch(FragmentActivity.class);
        mScenario.onActivity(
                activity -> activity.getSupportFragmentManager().beginTransaction()
                        .add(mSettings, "TAG").commitNow()
        );
        mSelectorWithWidgetPreference = new SelectorWithWidgetPreference(mContext);
        View view = LayoutInflater.from(mContext)
                .inflate(mSelectorWithWidgetPreference.getLayoutResource(), /* root= */ null);
        return PreferenceViewHolder.createInstanceForTests(view);
    }
}
