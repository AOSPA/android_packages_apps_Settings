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

package com.android.settings.inputmethod;

import static com.android.settings.flags.Flags.FLAG_DISABLE_KEYBOARD_SETTINGS_IN_DEMO_MODE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.Settings;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;

import androidx.core.text.BidiFormatter;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.testutils.shadow.SettingsShadowResources;

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

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = SettingsShadowResources.class)
public class VirtualKeyboardPreferenceControllerTest {

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Mock
    private Context mContext;
    @Mock
    private InputMethodManager mImm;
    @Mock
    private DevicePolicyManager mDpm;
    @Mock
    private PackageManager mPm;
    @Mock
    private Preference mPreference;

    private VirtualKeyboardPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mContext.getSystemService(Context.DEVICE_POLICY_SERVICE)).thenReturn(mDpm);
        when(mContext.getSystemService(Context.INPUT_METHOD_SERVICE)).thenReturn(mImm);
        when(mContext.getPackageManager()).thenReturn(mPm);
        mController = new VirtualKeyboardPreferenceController(mContext);
    }

    @After
    public void tearDown() {
        Settings.Global.putInt(RuntimeEnvironment.application.getContentResolver(),
                Settings.Global.DEVICE_DEMO_MODE, 0);
    }

    @Test
    public void testVirtualKeyboard_byDefault_shouldBeShown() {
        final Context context = spy(RuntimeEnvironment.application);
        mController = new VirtualKeyboardPreferenceController(context);
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void testVirtualKeyboard_ifDisabled_shouldNotBeShown() {
        final Context context = spy(RuntimeEnvironment.application);
        mController = new VirtualKeyboardPreferenceController(context);
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    @EnableFlags(FLAG_DISABLE_KEYBOARD_SETTINGS_IN_DEMO_MODE)
    public void isAvailable_inDemoMode_returnsFalse() {
        final Context context = spy(RuntimeEnvironment.application);
        mController = new VirtualKeyboardPreferenceController(context);

        // Put the device in demo mode.
        Settings.Global.putInt(context.getContentResolver(),
                Settings.Global.DEVICE_DEMO_MODE, 1);

        SettingsShadowResources.overrideResource(
                R.bool.config_disable_keyboard_settings_in_demo_mode, true);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void updateState_noEnabledIMEs_setEmptySummary() {
        mController.updateState(mPreference);

        verify(mPreference).setSummary(com.android.settingslib.R.string.summary_empty);
    }

    @Test
    public void updateState_singleIme_setImeLabelToSummary() {
        when(mDpm.getPermittedInputMethodsForCurrentUser()).thenReturn(null);
        final ComponentName componentName = new ComponentName("pkg", "cls");
        final List<InputMethodInfo> imis = new ArrayList<>();
        imis.add(mock(InputMethodInfo.class));
        when(imis.get(0).getPackageName()).thenReturn(componentName.getPackageName());
        when(mImm.getEnabledInputMethodList()).thenReturn(imis);
        when(imis.get(0).loadLabel(mPm)).thenReturn("label");

        mController.updateState(mPreference);

        verify(mPreference)
                .setSummary(mContext.getString(R.string.keyboard_apps_category_summary, "label"));
    }

    @Test
    public void updateState_multiImeWithMixedLocale_setImeLabelToSummary() {
        final BidiFormatter formatter = BidiFormatter.getInstance();
        final ComponentName componentName = new ComponentName("pkg", "cls");
        final List<InputMethodInfo> imis = new ArrayList<>();
        final String label1 = "label";
        final String label2 = "Keyboard מִקְלֶדֶת";
        imis.add(mock(InputMethodInfo.class));
        imis.add(mock(InputMethodInfo.class));

        when(mDpm.getPermittedInputMethodsForCurrentUser()).thenReturn(null);
        when(mImm.getEnabledInputMethodList()).thenReturn(imis);
        when(imis.get(0).getPackageName()).thenReturn(componentName.getPackageName());
        when(imis.get(0).loadLabel(mPm)).thenReturn(label1);
        when(imis.get(1).getPackageName()).thenReturn(componentName.getPackageName());
        when(imis.get(1).loadLabel(mPm)).thenReturn(label2);

        mController.updateState(mPreference);

        verify(mPreference)
                .setSummary(
                        mContext.getString(
                                R.string.keyboard_apps_category_summary,
                                formatter.unicodeWrap(label1)
                                        + " and "
                                        + formatter.unicodeWrap(label2)));
    }
}
