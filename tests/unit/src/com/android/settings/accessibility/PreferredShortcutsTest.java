/*
 * Copyright (C) 2020 The Android Open Source Project
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

import static android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_3BUTTON;

import static com.android.internal.accessibility.AccessibilityShortcutController.COLOR_INVERSION_COMPONENT_NAME;
import static com.android.internal.accessibility.AccessibilityShortcutController.DALTONIZER_COMPONENT_NAME;
import static com.android.internal.accessibility.AccessibilityShortcutController.MAGNIFICATION_CONTROLLER_NAME;
import static com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.GESTURE;
import static com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.SOFTWARE;

import static com.google.common.truth.Truth.assertThat;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.Settings;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.internal.accessibility.common.ShortcutConstants;
import com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType;
import com.android.internal.accessibility.util.ShortcutUtils;
import com.android.server.accessibility.Flags;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Set;

/** Tests for {@link PreferredShortcuts} */
@RunWith(AndroidJUnit4.class)
public class PreferredShortcutsTest {
    @Rule
    public SetFlagsRule setFlagsRule = new SetFlagsRule();

    private static final String PACKAGE_NAME_1 = "com.test1.example";
    private static final String CLASS_NAME_1 = PACKAGE_NAME_1 + ".test1";
    private static final ComponentName COMPONENT_NAME_1 = new ComponentName(PACKAGE_NAME_1,
            CLASS_NAME_1);
    private static final String PACKAGE_NAME_2 = "com.test2.example";
    private static final String CLASS_NAME_2 = PACKAGE_NAME_2 + ".test2";
    private static final ComponentName COMPONENT_NAME_2 = new ComponentName(PACKAGE_NAME_2,
            CLASS_NAME_2);
    private static final ContentResolver sContentResolver =
            ApplicationProvider.getApplicationContext().getContentResolver();
    private final Context mContext = ApplicationProvider.getApplicationContext();

    @Before
    public void setUp() {
        clearShortcuts();
    }

    @AfterClass
    public static void cleanUp() {
        clearShortcuts();
    }

    @Test
    public void retrieveUserShortcutType_fromSingleData_matchSavedType() {
        final int type = 1;
        final PreferredShortcut shortcut = new PreferredShortcut(COMPONENT_NAME_1.flattenToString(),
                type);

        PreferredShortcuts.saveUserShortcutType(mContext, shortcut);
        final int retrieveType = PreferredShortcuts.retrieveUserShortcutType(mContext,
                COMPONENT_NAME_1.flattenToString(), SOFTWARE);

        assertThat(retrieveType).isEqualTo(type);
    }

    @Test
    public void retrieveUserShortcutType_fromMultiData_matchSavedType() {
        final int type1 = 1;
        final int type2 = 2;
        final PreferredShortcut shortcut1 = new PreferredShortcut(
                COMPONENT_NAME_1.flattenToString(), type1);
        final PreferredShortcut shortcut2 = new PreferredShortcut(
                COMPONENT_NAME_2.flattenToString(), type2);

        PreferredShortcuts.saveUserShortcutType(mContext, shortcut1);
        PreferredShortcuts.saveUserShortcutType(mContext, shortcut2);
        final int retrieveType = PreferredShortcuts.retrieveUserShortcutType(mContext,
                COMPONENT_NAME_1.flattenToString(), SOFTWARE);

        assertThat(retrieveType).isEqualTo(type1);
    }

    @Test
    public void updatePreferredShortcutsFromSetting_magnificationWithTripleTapAndVolumeKeyShortcuts_preferredShortcutsMatches() {
        ShortcutUtils.optInValueToSettings(mContext, ShortcutConstants.UserShortcutType.HARDWARE,
                MAGNIFICATION_CONTROLLER_NAME);
        Settings.Secure.putInt(
                sContentResolver,
                Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_ENABLED,
                AccessibilityUtil.State.ON);

        PreferredShortcuts.updatePreferredShortcutsFromSettings(mContext,
                Set.of(MAGNIFICATION_CONTROLLER_NAME));
        int expectedShortcutTypes = ShortcutConstants.UserShortcutType.HARDWARE
                | ShortcutConstants.UserShortcutType.TRIPLETAP;

        assertThat(
                PreferredShortcuts.retrieveUserShortcutType(
                        mContext, MAGNIFICATION_CONTROLLER_NAME, SOFTWARE
                ))
                .isEqualTo(expectedShortcutTypes);
    }

    @Test
    public void updatePreferredShortcutsFromSetting_magnificationWithNoActiveShortcuts_noChangesOnPreferredShortcutTypes() {
        int expectedShortcutTypes = ShortcutConstants.UserShortcutType.HARDWARE
                | SOFTWARE;
        PreferredShortcuts.saveUserShortcutType(mContext,
                new PreferredShortcut(MAGNIFICATION_CONTROLLER_NAME, expectedShortcutTypes));

        PreferredShortcuts.updatePreferredShortcutsFromSettings(mContext,
                Set.of(MAGNIFICATION_CONTROLLER_NAME));

        assertThat(
                PreferredShortcuts.retrieveUserShortcutType(
                        mContext, MAGNIFICATION_CONTROLLER_NAME, SOFTWARE
                ))
                .isEqualTo(expectedShortcutTypes);
    }

    @Test
    public void updatePreferredShortcutsFromSetting_multipleComponents_preferredShortcutsMatches() {
        String target1 = COLOR_INVERSION_COMPONENT_NAME.flattenToString();
        String target2 = DALTONIZER_COMPONENT_NAME.flattenToString();

        Settings.Secure.putString(sContentResolver,
                Settings.Secure.ACCESSIBILITY_BUTTON_TARGETS, target1);
        Settings.Secure.putString(sContentResolver,
                Settings.Secure.ACCESSIBILITY_SHORTCUT_TARGET_SERVICE,
                target1 + ShortcutConstants.SERVICES_SEPARATOR + target2);

        int target1ShortcutTypes = ShortcutConstants.UserShortcutType.HARDWARE
                | SOFTWARE;
        int target2ShortcutTypes = ShortcutConstants.UserShortcutType.HARDWARE;

        PreferredShortcuts.updatePreferredShortcutsFromSettings(mContext, Set.of(target1, target2));

        assertThat(
                PreferredShortcuts.retrieveUserShortcutType(
                        mContext, target1, SOFTWARE
                ))
                .isEqualTo(target1ShortcutTypes);
        assertThat(
                PreferredShortcuts.retrieveUserShortcutType(
                        mContext, target2, SOFTWARE
                ))
                .isEqualTo(target2ShortcutTypes);
    }

    @Test
    public void updatePreferredShortcutFromSettings_colorInversionWithQsAndSoftwareShortcut_preferredShortcutsMatches() {
        String target = COLOR_INVERSION_COMPONENT_NAME.flattenToString();
        Settings.Secure.putString(sContentResolver,
                Settings.Secure.ACCESSIBILITY_BUTTON_TARGETS, target);
        Settings.Secure.putString(sContentResolver,
                Settings.Secure.ACCESSIBILITY_QS_TARGETS, target);

        PreferredShortcuts.updatePreferredShortcutsFromSettings(mContext, Set.of(target));

        int savedPreferredShortcut = PreferredShortcuts.retrieveUserShortcutType(
                mContext, target, SOFTWARE);
        assertThat(savedPreferredShortcut).isEqualTo(
                SOFTWARE | UserShortcutType.QUICK_SETTINGS);

    }

    @Test
    public void retrieveUserShortcutTypeWithoutDefault_noUserPreferredShortcuts_returnSoftwareShortcut() {
        String target = COMPONENT_NAME_1.flattenToString();

        assertThat(PreferredShortcuts.retrieveUserShortcutType(mContext, target, SOFTWARE))
                .isEqualTo(SOFTWARE);
    }

    @Test
    public void retrieveUserShortcutTypeWithDefaultAsDefault_noUserPreferredShortcuts_returnSpecifiedDefault() {
        String target = COMPONENT_NAME_1.flattenToString();

        assertThat(PreferredShortcuts.retrieveUserShortcutType(mContext, target,
                UserShortcutType.HARDWARE))
                .isEqualTo(UserShortcutType.HARDWARE);
    }

    @DisableFlags(Flags.FLAG_ENABLE_KEY_GESTURE_SHORTCUT_SETTINGS)
    @Test
    public void retrieveUserShortcutType_keyboardPreferred_flagOff_returnSpecifiedDefault() {
        String target = COMPONENT_NAME_1.flattenToString();
        final PreferredShortcut shortcut = new PreferredShortcut(
                COMPONENT_NAME_1.flattenToString(),
                UserShortcutType.KEY_GESTURE);
        PreferredShortcuts.saveUserShortcutType(mContext, shortcut);

        assertThat(PreferredShortcuts.retrieveUserShortcutType(mContext, target,
                UserShortcutType.SOFTWARE))
                .isEqualTo(UserShortcutType.SOFTWARE);
    }

    @DisableFlags(Flags.FLAG_ENABLE_KEY_GESTURE_SHORTCUT_SETTINGS)
    @Test
    public void retrieveUserShortcutType_keyboardHardwarePreferred_flagOff_returnHardware() {
        String target = COMPONENT_NAME_1.flattenToString();
        final PreferredShortcut shortcut1 = new PreferredShortcut(
                COMPONENT_NAME_1.flattenToString(),
                UserShortcutType.KEY_GESTURE);
        PreferredShortcuts.saveUserShortcutType(mContext, shortcut1);
        final PreferredShortcut shortcut2 = new PreferredShortcut(
                COMPONENT_NAME_1.flattenToString(),
                UserShortcutType.HARDWARE);
        PreferredShortcuts.saveUserShortcutType(mContext, shortcut2);

        assertThat(PreferredShortcuts.retrieveUserShortcutType(mContext, target,
                UserShortcutType.SOFTWARE))
                .isEqualTo(UserShortcutType.HARDWARE);
    }

    @DisableFlags(com.android.settings.accessibility.Flags
            .FLAG_PREFERRED_SHORTCUT_FILTERS_NAVIGATION_MODE)
    @Test
    public void retrieveUserShortcutType_preferGesture_buttonNav_flagOff_returnsGesture() {
        String target = COMPONENT_NAME_1.flattenToString();
        final PreferredShortcut shortcut = new PreferredShortcut(target, GESTURE);
        PreferredShortcuts.saveUserShortcutType(mContext, shortcut);

        Settings.Secure.putIntForUser(mContext.getContentResolver(),
                Settings.Secure.NAVIGATION_MODE, NAV_BAR_MODE_3BUTTON, mContext.getUserId());

        assertThat(PreferredShortcuts.retrieveUserShortcutType(mContext, target, GESTURE))
                .isEqualTo(GESTURE);
    }

    @EnableFlags(com.android.settings.accessibility.Flags
            .FLAG_PREFERRED_SHORTCUT_FILTERS_NAVIGATION_MODE)
    @Test
    public void retrieveUserShortcutType_preferGesture_buttonNav_returnsSoftware() {
        String target = COMPONENT_NAME_1.flattenToString();
        final PreferredShortcut shortcut = new PreferredShortcut(target, GESTURE);
        PreferredShortcuts.saveUserShortcutType(mContext, shortcut);

        Settings.Secure.putIntForUser(mContext.getContentResolver(),
                Settings.Secure.NAVIGATION_MODE, NAV_BAR_MODE_3BUTTON, mContext.getUserId());

        assertThat(PreferredShortcuts.retrieveUserShortcutType(mContext, target, GESTURE))
                .isEqualTo(SOFTWARE);
    }

    private static void clearShortcuts() {
        Settings.Secure.putString(sContentResolver,
                Settings.Secure.ACCESSIBILITY_BUTTON_TARGETS, "");
        Settings.Secure.putString(sContentResolver,
                Settings.Secure.ACCESSIBILITY_KEY_GESTURE_TARGETS, "");
        Settings.Secure.putString(sContentResolver,
                Settings.Secure.ACCESSIBILITY_SHORTCUT_TARGET_SERVICE, "");
        Settings.Secure.putString(sContentResolver,
                Settings.Secure.ACCESSIBILITY_QS_TARGETS, "");
        Settings.Secure.putInt(
                sContentResolver,
                Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_ENABLED,
                AccessibilityUtil.State.OFF);

        PreferredShortcuts.clearPreferredShortcuts(ApplicationProvider.getApplicationContext());
    }
}
