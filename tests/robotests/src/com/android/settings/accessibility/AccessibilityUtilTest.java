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
 * limitations under the License.
 */

package com.android.settings.accessibility;

import static com.google.common.truth.Truth.assertThat;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.icu.text.CaseMap;
import android.os.Build;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.Settings;
import android.view.WindowManagerPolicyConstants;

import androidx.test.core.app.ApplicationProvider;

import com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType;
import com.android.server.accessibility.Flags;
import com.android.settings.R;
import com.android.settings.testutils.shadow.SettingsShadowResources;
import com.android.settings.testutils.shadow.ShadowInputDevice;
import com.android.settings.utils.LocaleUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Config(shadows = {ShadowInputDevice.class, SettingsShadowResources.class})
@RunWith(RobolectricTestRunner.class)
public final class AccessibilityUtilTest {
    @Rule
    public SetFlagsRule setFlagsRule = new SetFlagsRule();
    private static final String SECURE_TEST_KEY = "secure_test_key";
    private static final String MOCK_PACKAGE_NAME = "com.mock.example";
    private static final String MOCK_CLASS_NAME = MOCK_PACKAGE_NAME + ".mock_a11y_service";
    private static final ComponentName MOCK_COMPONENT_NAME = new ComponentName(MOCK_PACKAGE_NAME,
            MOCK_CLASS_NAME);

    private Context mContext;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
    }

    @After
    public void tearDown() {
        SettingsShadowResources.reset();
    }

    @Test
    public void capitalize_shouldReturnCapitalizedString() {
        assertThat(AccessibilityUtil.capitalize(null)).isNull();
        assertThat(AccessibilityUtil.capitalize("")).isEmpty();
        assertThat(AccessibilityUtil.capitalize("Hans")).isEqualTo("Hans");
        assertThat(AccessibilityUtil.capitalize("hans")).isEqualTo("Hans");
        assertThat(AccessibilityUtil.capitalize(",hans")).isEqualTo(",hans");
        assertThat(AccessibilityUtil.capitalize("Hans, Hans")).isEqualTo("Hans, hans");
    }

    @Test
    public void getSummary_hasValueAndEqualsToOne_shouldReturnOnString() {
        setSettingsFeatureEnabled(SECURE_TEST_KEY, true);

        final CharSequence result = AccessibilityUtil.getSummary(mContext, SECURE_TEST_KEY,
                R.string.switch_on_text, R.string.switch_off_text);

        assertThat(result)
                .isEqualTo(mContext.getText(R.string.switch_on_text));
    }

    @Test
    public void getSummary_hasValueAndEqualsToZero_shouldReturnOffString() {
        setSettingsFeatureEnabled(SECURE_TEST_KEY, false);

        final CharSequence result = AccessibilityUtil.getSummary(mContext, SECURE_TEST_KEY,
                R.string.switch_on_text, R.string.switch_off_text);

        assertThat(result)
                .isEqualTo(mContext.getText(R.string.switch_off_text));
    }

    @Test
    public void getSummary_noValue_shouldReturnOffString() {
        final CharSequence result = AccessibilityUtil.getSummary(mContext, SECURE_TEST_KEY,
                R.string.switch_on_text, R.string.switch_off_text);

        assertThat(result)
                .isEqualTo(mContext.getText(R.string.switch_off_text));
    }

    @Test
    public void getShortcutSummaryList_defaultShortcut_shouldReturnEmptyString() {
        final CharSequence result = AccessibilityUtil.getShortcutSummaryList(mContext,
                UserShortcutType.DEFAULT);

        assertThat(result.isEmpty()).isEqualTo(true);
    }

    @DisableFlags(Flags.FLAG_ENABLE_KEY_GESTURE_SHORTCUT_SETTINGS)
    @Test
    public void getShortcutSummaryList_keyGestureShortcut_flagDisabled_shouldReturnEmptyString() {
        setHardwareKeyboard(true);
        final CharSequence result = AccessibilityUtil.getShortcutSummaryList(mContext,
                UserShortcutType.KEY_GESTURE);
        assertThat(result.isEmpty()).isEqualTo(true);
    }

    @EnableFlags(Flags.FLAG_ENABLE_KEY_GESTURE_SHORTCUT_SETTINGS)
    @Test
    public void getShortcutSummaryList_keyGestureShortcut_noKeyboard_shouldReturnEmptyString() {
        setHardwareKeyboard(false);
        final CharSequence result = AccessibilityUtil.getShortcutSummaryList(mContext,
                UserShortcutType.KEY_GESTURE);
        assertThat(result.isEmpty()).isEqualTo(true);
    }

    @EnableFlags(Flags.FLAG_ENABLE_KEY_GESTURE_SHORTCUT_SETTINGS)
    @Test
    public void getShortcutSummaryList_keyGestureShortcut_hasKeyboard_shouldReturnNonEmptyString() {
        setHardwareKeyboard(true);
        final CharSequence result = AccessibilityUtil.getShortcutSummaryList(mContext,
                UserShortcutType.KEY_GESTURE);

        assertThat(result.isEmpty()).isEqualTo(false);
        final CharSequence summary = CaseMap.toTitle().wholeString().noLowercase().apply(
                Locale.getDefault(),
                /* iter= */ null,
                mContext.getText(R.string.accessibility_shortcut_keyboard_keyword));
        assertThat(result.toString()).isEqualTo(summary.toString());
    }

    @EnableFlags(Flags.FLAG_ENABLE_KEY_GESTURE_SHORTCUT_SETTINGS)
    @Test
    public void getShortcutSummaryList_softwareAndKeyGestureShortcut_hasKeyboard_shouldReturnConcatenatedString() {
        setHardwareKeyboard(true);
        final CharSequence result = AccessibilityUtil.getShortcutSummaryList(mContext,
                UserShortcutType.SOFTWARE | UserShortcutType.KEY_GESTURE);

        assertThat(result.isEmpty()).isEqualTo(false);
        final List<CharSequence> list = new ArrayList<>();
        list.add(mContext.getText(R.string.accessibility_shortcut_keyboard_keyword));
        list.add(mContext.getText(R.string.accessibility_shortcut_edit_summary_software));

        list.sort(CharSequence::compare);
        final CharSequence summary = CaseMap.toTitle().wholeString().noLowercase().apply(
                Locale.getDefault(), /* iter= */
                null, LocaleUtils.getConcatenatedString(list));
        assertThat(result.toString()).isEqualTo(summary.toString());
    }

    @EnableFlags(Flags.FLAG_ENABLE_KEY_GESTURE_SHORTCUT_SETTINGS)
    @Test
    public void getShortcutSummaryList_softwareAndKeyGestureShortcut_noKeyboard_shouldReturnSoftwareString() {
        setHardwareKeyboard(false);
        final CharSequence result = AccessibilityUtil.getShortcutSummaryList(mContext,
                UserShortcutType.SOFTWARE | UserShortcutType.KEY_GESTURE);

        assertThat(result.isEmpty()).isEqualTo(false);
        final CharSequence summary = CaseMap.toTitle().wholeString().noLowercase().apply(
                Locale.getDefault(),
                /* iter= */ null,
                mContext.getText(R.string.accessibility_shortcut_edit_summary_software));
        assertThat(result.toString()).isEqualTo(summary.toString());
    }

    @Test
    public void getShortcutSummaryList_softwareShortcut_shouldReturnNonEmptyString() {
        final CharSequence result = AccessibilityUtil.getShortcutSummaryList(mContext,
                UserShortcutType.SOFTWARE);

        assertThat(result.isEmpty()).isEqualTo(false);
        final CharSequence summary = CaseMap.toTitle().wholeString().noLowercase().apply(
                Locale.getDefault(),
                /* iter= */ null,
                mContext.getText(R.string.accessibility_shortcut_edit_summary_software));
        assertThat(result.toString()).isEqualTo(summary.toString());
    }

    @Test
    public void getAccessibilityServiceFragmentType_targetSdkQ_volumeShortcutType() {
        final AccessibilityServiceInfo info = getMockAccessibilityServiceInfo();

        info.getResolveInfo().serviceInfo.applicationInfo.targetSdkVersion = Build.VERSION_CODES.Q;
        info.flags |= AccessibilityServiceInfo.FLAG_REQUEST_ACCESSIBILITY_BUTTON;

        assertThat(AccessibilityUtil.getAccessibilityServiceFragmentType(info)).isEqualTo(
                AccessibilityUtil.AccessibilityServiceFragmentType.VOLUME_SHORTCUT_TOGGLE);
    }

    @Test
    public void getAccessibilityServiceFragmentType_targetSdkR_HaveA11yButton_invisibleType() {
        final AccessibilityServiceInfo info = getMockAccessibilityServiceInfo();

        info.getResolveInfo().serviceInfo.applicationInfo.targetSdkVersion = Build.VERSION_CODES.R;
        info.flags |= AccessibilityServiceInfo.FLAG_REQUEST_ACCESSIBILITY_BUTTON;

        assertThat(AccessibilityUtil.getAccessibilityServiceFragmentType(info)).isEqualTo(
                AccessibilityUtil.AccessibilityServiceFragmentType.INVISIBLE_TOGGLE);
    }

    @Test
    public void getAccessibilityServiceFragmentType_targetSdkR_NoA11yButton_toggleType() {
        final AccessibilityServiceInfo info = getMockAccessibilityServiceInfo();

        info.getResolveInfo().serviceInfo.applicationInfo.targetSdkVersion = Build.VERSION_CODES.R;
        info.flags |= ~AccessibilityServiceInfo.FLAG_REQUEST_ACCESSIBILITY_BUTTON;

        assertThat(AccessibilityUtil.getAccessibilityServiceFragmentType(info)).isEqualTo(
                AccessibilityUtil.AccessibilityServiceFragmentType.TOGGLE);
    }

    @DisableFlags(Flags.FLAG_ENABLE_KEY_GESTURE_SHORTCUT_SETTINGS)
    @Test
    public void isKeyboardShortcutSettingAvailable_noKeyboardFlagOff_returnsFalse() {
        setHardwareKeyboard(false);
        assertThat(AccessibilityUtil.isKeyboardShortcutSettingAvailable()).isFalse();
    }

    @DisableFlags(Flags.FLAG_ENABLE_KEY_GESTURE_SHORTCUT_SETTINGS)
    @Test
    public void isKeyboardShortcutSettingAvailable_hasKeyboardFlagOff_returnsFalse() {
        setHardwareKeyboard(true);
        assertThat(AccessibilityUtil.isKeyboardShortcutSettingAvailable()).isFalse();
    }

    @EnableFlags(Flags.FLAG_ENABLE_KEY_GESTURE_SHORTCUT_SETTINGS)
    @Test
    public void isKeyboardShortcutSettingAvailable_noKeyboardFlagOn_returnsFalse() {
        setHardwareKeyboard(false);
        assertThat(AccessibilityUtil.isKeyboardShortcutSettingAvailable()).isFalse();
    }

    @EnableFlags(Flags.FLAG_ENABLE_KEY_GESTURE_SHORTCUT_SETTINGS)
    @Test
    public void isKeyboardShortcutSettingAvailable_hasKeyboardFlagOn_returnsTrue() {
        setHardwareKeyboard(true);
        assertThat(AccessibilityUtil.isKeyboardShortcutSettingAvailable()).isTrue();
    }

    @Test
    public void removeTypeFromShortcutTypes_defaultShortcut() {
        assertThat(AccessibilityUtil.removeTypeFromShortcutTypes(UserShortcutType.DEFAULT,
                UserShortcutType.KEY_GESTURE))
                .isEqualTo(UserShortcutType.DEFAULT);
    }

    @Test
    public void removeTypeFromShortcutTypes_defaultAndKeyboardShortcut() {
        assertThat(AccessibilityUtil.removeTypeFromShortcutTypes(UserShortcutType.DEFAULT
                | UserShortcutType.KEY_GESTURE, UserShortcutType.KEY_GESTURE))
                .isEqualTo(UserShortcutType.DEFAULT);
    }

    @Test
    public void removeTypeFromShortcutTypes_keyboardShortcut() {
        assertThat(AccessibilityUtil.removeTypeFromShortcutTypes(
                UserShortcutType.KEY_GESTURE, UserShortcutType.KEY_GESTURE))
                .isEqualTo(UserShortcutType.DEFAULT);
    }

    @Test
    public void removeTypeFromShortcutTypes_softwareAndKeyboardShortcut() {
        assertThat(AccessibilityUtil.removeTypeFromShortcutTypes(UserShortcutType.SOFTWARE
                | UserShortcutType.KEY_GESTURE, UserShortcutType.KEY_GESTURE))
                .isEqualTo(UserShortcutType.SOFTWARE);
    }

    @Test
    public void isAccessibilityButtonLocationConfigurable_gestureNavigationOff_returnsTrue() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.NAVIGATION_MODE, WindowManagerPolicyConstants.NAV_BAR_MODE_3BUTTON);

        assertThat(AccessibilityUtil.isAccessibilityButtonLocationConfigurable(mContext)).isTrue();
    }

    @Test
    public void isAccessibilityButtonLocationConfigurable_gestureNavigationOn_navBarCanMove() {
        Settings.Secure.putInt(mContext.getContentResolver(), Settings.Secure.NAVIGATION_MODE,
                WindowManagerPolicyConstants.NAV_BAR_MODE_GESTURAL);
        SettingsShadowResources.overrideResource(com.android.internal.R.bool.config_navBarCanMove,
                true);

        assertThat(AccessibilityUtil.isAccessibilityButtonLocationConfigurable(mContext)).isFalse();
    }

    @Test
    @EnableFlags(android.view.accessibility.Flags.FLAG_ALLOW_A11Y_BUTTON_ON_LARGE_SCREEN)
    public void isAccessibilityButtonLocationConfigurable_gestureNavigationOn_navBarCannotMove() {
        Settings.Secure.putInt(mContext.getContentResolver(), Settings.Secure.NAVIGATION_MODE,
                WindowManagerPolicyConstants.NAV_BAR_MODE_GESTURAL);
        SettingsShadowResources.overrideResource(com.android.internal.R.bool.config_navBarCanMove,
                false);

        assertThat(AccessibilityUtil.isAccessibilityButtonLocationConfigurable(mContext)).isTrue();
    }

    @Test
    @DisableFlags(android.view.accessibility.Flags.FLAG_ALLOW_A11Y_BUTTON_ON_LARGE_SCREEN)
    public void isAccessibilityButtonLocationConfigurable_flagOff() {
        Settings.Secure.putInt(mContext.getContentResolver(), Settings.Secure.NAVIGATION_MODE,
                WindowManagerPolicyConstants.NAV_BAR_MODE_GESTURAL);
        SettingsShadowResources.overrideResource(com.android.internal.R.bool.config_navBarCanMove,
                false);

        assertThat(AccessibilityUtil.isAccessibilityButtonLocationConfigurable(mContext)).isFalse();
    }

    private AccessibilityServiceInfo getMockAccessibilityServiceInfo() {
        final ApplicationInfo applicationInfo = new ApplicationInfo();
        final ServiceInfo serviceInfo = new ServiceInfo();
        applicationInfo.packageName = MOCK_PACKAGE_NAME;
        serviceInfo.packageName = MOCK_PACKAGE_NAME;
        serviceInfo.name = MOCK_CLASS_NAME;
        serviceInfo.applicationInfo = applicationInfo;

        final ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.serviceInfo = serviceInfo;

        try {
            final AccessibilityServiceInfo info = new AccessibilityServiceInfo(resolveInfo,
                    mContext);
            info.setComponentName(MOCK_COMPONENT_NAME);
            return info;
        } catch (XmlPullParserException | IOException e) {
            // Do nothing
        }

        return null;
    }

    private void setSettingsFeatureEnabled(String settingsKey, boolean enabled) {
        Settings.Secure.putInt(mContext.getContentResolver(),
                settingsKey,
                enabled ? AccessibilityUtil.State.ON : AccessibilityUtil.State.OFF);
    }

    private void setHardwareKeyboard(boolean hasConnectedKeyboard) {
        if (hasConnectedKeyboard) {
            var device = ShadowInputDevice.makeFullKeyboardInputDevicebyId(/* id= */ 1);
            ShadowInputDevice.addDevice(device.getId(), device);
        } else {
            ShadowInputDevice.reset();
        }
    }
}
