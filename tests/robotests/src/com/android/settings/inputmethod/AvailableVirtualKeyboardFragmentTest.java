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

package com.android.settings.inputmethod;

import static com.android.settings.flags.Flags.FLAG_DISABLE_KEYBOARD_SETTINGS_IN_DEMO_MODE;
import static com.android.settings.dashboard.profileselector.ProfileSelectFragment.EXTRA_PROFILE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;

import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.dashboard.profileselector.ProfileSelectFragment;
import com.android.settings.testutils.shadow.SettingsShadowResources;
import com.android.settings.testutils.shadow.ShadowDashboardFragment;
import com.android.settings.testutils.shadow.ShadowInputMethodManagerWithMethodList;
import com.android.settings.testutils.shadow.ShadowSecureSettings;
import com.android.settingslib.inputmethod.InputMethodPreference;
import com.android.settingslib.inputmethod.InputMethodSettingValuesWrapper;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        ShadowSecureSettings.class,
        ShadowInputMethodManagerWithMethodList.class,
        SettingsShadowResources.class,
        ShadowDashboardFragment.class
})
public class AvailableVirtualKeyboardFragmentTest {

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Mock
    private InputMethodManager mInputMethodManager;
    @Mock
    private UserManager mUserManager;
    @Mock
    private InputMethodSettingValuesWrapper mValuesWrapper;
    @Mock
    private PreferenceScreen mPreferenceScreen;
    @Mock
    private PreferenceManager mPreferenceManager;
    @Mock
    private InputMethodPreference mInputMethodPreference;
    private Context mContext;
    private AvailableVirtualKeyboardFragment mFragment;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        initFragment();
        initMock();
    }

    @After
    public void tearDown() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.DEVICE_DEMO_MODE, 0);
    }

    @Test
    public void onAttachPersonalProfile_noProfileParent() {
        doReturn(null).when(mUserManager).getProfileParent(any(UserHandle.class));

        mFragment.onAttach(mContext);

        assertThat(mFragment.mUserAwareContext).isEqualTo(mContext);
    }

    @Test
    public void onAttachPersonalProfile_hasProfileParent() {
        final UserHandle profileParent = new UserHandle(0);
        final Context mockContext = mock(Context.class);
        doReturn(profileParent).when(mUserManager).getProfileParent(any(UserHandle.class));
        doReturn(mockContext).when(mContext).createContextAsUser(any(UserHandle.class), anyInt());

        mFragment.onAttach(mContext);

        assertThat(mFragment.mUserAwareContext).isEqualTo(mockContext);
    }

    @Test
    public void onCreatePreferences_shouldAddResource() {
        mFragment.onAttach(mContext);

        mFragment.onCreatePreferences(new Bundle(), "test");

        verify(mFragment).addPreferencesFromResource(R.xml.available_virtual_keyboard);
    }

    @Test
    public void onResume_refreshAllInputMethodAndSubtypes() {
        mFragment.onAttach(mContext);

        mFragment.onResume();

        // One invocation is in onResume(), another is in updateInputMethodPreferenceViews().
        verify(mValuesWrapper, times(2)).refreshAllInputMethodAndSubtypes();
    }

    @Test
    public void onResume_updateInputMethodPreferenceViews() {
        mFragment.onAttach(mContext);

        mFragment.onResume();

        verify(mFragment).updateInputMethodPreferenceViews();
    }

    @Test
    public void onSaveInputMethodPreference_refreshAllInputMethodAndSubtypes() {
        mFragment.onAttach(mContext);

        mFragment.onSaveInputMethodPreference(mInputMethodPreference);

        verify(mValuesWrapper).refreshAllInputMethodAndSubtypes();
    }

    @Test
    public void updateInputMethodPreferenceViews_callsExpectedMethods() {
        mFragment.onAttach(mContext);

        mFragment.updateInputMethodPreferenceViews();

        verify(mValuesWrapper).getInputMethodList();
        verify(mInputMethodManager).getEnabledInputMethodListAsUser(any(UserHandle.class));
    }

    @Test
    public void updateInputMethodPreferenceViews_addExpectedInputMethodPreference() {
        final int inputMethodNums = 5;
        mFragment.onAttach(mContext);
        when(mValuesWrapper.getInputMethodList()).thenReturn(createFakeInputMethodInfoList(
                "test", inputMethodNums));

        mFragment.updateInputMethodPreferenceViews();

        assertThat(mFragment.mInputMethodPreferenceList).hasSize(inputMethodNums);
    }

    @Test
    public void searchIndexProvider_shouldIndexResource() {
        final List<SearchIndexableResource> indexRes =
                AvailableVirtualKeyboardFragment.SEARCH_INDEX_DATA_PROVIDER
                        .getXmlResourcesToIndex(RuntimeEnvironment.application, true /* enabled */);

        assertThat(indexRes).isNotNull();
        assertThat(indexRes.get(0).xmlResId).isEqualTo(mFragment.getPreferenceScreenResId());
    }

    @Test
    @EnableFlags(FLAG_DISABLE_KEYBOARD_SETTINGS_IN_DEMO_MODE)
    public void updateInputMethodPreferenceViews_inDemoMode_disablesPreferenceScreen() {
        mFragment.onAttach(mContext);
        // Put the device in demo mode.
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.DEVICE_DEMO_MODE, 1);

        SettingsShadowResources.overrideResource(
                R.bool.config_disable_keyboard_settings_in_demo_mode, true);

        mFragment.updateInputMethodPreferenceViews();

        verify(mPreferenceScreen).setEnabled(false);
    }

    @Test
    public void updateInputMethodPreferenceViews_notInDemoMode_doesNotDisablePreferenceScreen() {
        mFragment.onAttach(mContext);
        mFragment.updateInputMethodPreferenceViews();

        // Expect no call to mPreferenceScreen.setEnabled().
        verify(mPreferenceScreen, never()).setEnabled(anyBoolean());
    }

    @Test
    @EnableFlags(FLAG_DISABLE_KEYBOARD_SETTINGS_IN_DEMO_MODE)
    public void isPageSearchEnabled_inDemoMode_returnsFalse() {
        // Put the device in demo mode.
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.DEVICE_DEMO_MODE, 1);

        SettingsShadowResources.overrideResource(
                R.bool.config_disable_keyboard_settings_in_demo_mode, true);

        assertThat(AvailableVirtualKeyboardFragment.SEARCH_INDEX_DATA_PROVIDER
                .isPageSearchEnabled(mContext)).isFalse();
    }

    @Test
    public void isPageSearchEnabled_notInDemoMode_returnsTrue() {
        assertThat(AvailableVirtualKeyboardFragment.SEARCH_INDEX_DATA_PROVIDER
                .isPageSearchEnabled(mContext)).isTrue();
    }

    private void initFragment() {
        final Bundle bundle = new Bundle();
        bundle.putInt(EXTRA_PROFILE, ProfileSelectFragment.ProfileType.PERSONAL);
        mFragment = spy(new AvailableVirtualKeyboardFragment());
        mFragment.setArguments(bundle);
        mFragment.mInputMethodSettingValues = mValuesWrapper;
        ReflectionHelpers.setField(mFragment, "mPreferenceManager", mPreferenceManager);
    }

    private void initMock() {
        when(mFragment.getContext()).thenReturn(mContext);
        when(mFragment.getPreferenceScreen()).thenReturn(mPreferenceScreen);
        when(mPreferenceManager.getContext()).thenReturn(mContext);
        when(mContext.getSystemService(InputMethodManager.class)).thenReturn(mInputMethodManager);
        when(mContext.getSystemService(UserManager.class)).thenReturn(mUserManager);
    }

    private List<InputMethodInfo> createFakeInputMethodInfoList(final String name, int num) {
        List<InputMethodSubtype> subtypes = new ArrayList<>();

        subtypes.add(new InputMethodSubtype.InputMethodSubtypeBuilder()
                .build());
        subtypes.add(new InputMethodSubtype.InputMethodSubtypeBuilder()
                .build());

        final ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.serviceInfo = new ServiceInfo();
        resolveInfo.serviceInfo.packageName = "com.android.ime";
        resolveInfo.serviceInfo.name = name;
        resolveInfo.serviceInfo.applicationInfo = new ApplicationInfo();
        resolveInfo.serviceInfo.applicationInfo.enabled = true;

        List<InputMethodInfo> inputMethodInfoList = new ArrayList<>();
        for (int i = 0; i < num; i++) {
            inputMethodInfoList.add(new InputMethodInfo(
                    resolveInfo,
                    false /* isAuxIme */,
                    "TestSettingsActivity",
                    subtypes,
                    0 /* isDefaultResId */,
                    true /* forceDefault */));
        }
        return inputMethodInfoList;
    }
}
