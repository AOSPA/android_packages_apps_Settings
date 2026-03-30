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

import static android.provider.Settings.Secure.ACCESSIBILITY_BUTTON_MODE_FLOATING_MENU;
import static android.view.WindowInsets.Type.displayCutout;
import static android.view.WindowInsets.Type.systemBars;
import static android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_GESTURAL;

import static com.android.internal.accessibility.AccessibilityShortcutController.MAGNIFICATION_COMPONENT_NAME;
import static com.android.internal.accessibility.AccessibilityShortcutController.MAGNIFICATION_CONTROLLER_NAME;
import static com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.GESTURE;
import static com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.HARDWARE;
import static com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.KEY_GESTURE;
import static com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.QUICK_SETTINGS;
import static com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.SOFTWARE;
import static com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.TOP_ROW_KEY;
import static com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.TRIPLETAP;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Insets;
import android.graphics.Rect;
import android.icu.text.CaseMap;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.WindowManager;
import android.view.WindowMetrics;
import android.view.accessibility.AccessibilityManager;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType;
import com.android.internal.accessibility.util.ShortcutUtils;
import com.android.server.accessibility.Flags;
import com.android.settings.R;
import com.android.settings.inputmethod.InputPeripheralsSettingsUtils;
import com.android.settings.utils.LocaleUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Provides utility methods to accessibility settings only. */
public final class AccessibilityUtil {
    private static final String TAG = AccessibilityUtil.class.getSimpleName();

    // LINT.IfChange(shortcut_type_ui_order)
    public static final int[] SHORTCUTS_ORDER_IN_UI = {
            KEY_GESTURE,
            QUICK_SETTINGS,
            SOFTWARE, // FAB displays before gesture. Navbar displays without gesture.
            GESTURE,
            HARDWARE,
            TOP_ROW_KEY,
            TRIPLETAP,
    };
    // LINT.ThenChange(/src/com/android/settings/accessibility/shortcuts/ui/EditShortcutsScreen.kt:shortcut_type_ui_order)

    private AccessibilityUtil(){}

    /**
     * Annotation for different accessibilityService fragment UI type.
     *
     * {@code VOLUME_SHORTCUT_TOGGLE} for displaying basic accessibility service fragment but
     * only hardware shortcut allowed.
     * {@code INVISIBLE_TOGGLE} for displaying basic accessibility service fragment without
     * switch bar.
     * {@code TOGGLE} for displaying basic accessibility service fragment.
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            AccessibilityServiceFragmentType.VOLUME_SHORTCUT_TOGGLE,
            AccessibilityServiceFragmentType.INVISIBLE_TOGGLE,
            AccessibilityServiceFragmentType.TOGGLE,
    })

    public @interface AccessibilityServiceFragmentType {
        int VOLUME_SHORTCUT_TOGGLE = 0;
        int INVISIBLE_TOGGLE = 1;
        int TOGGLE = 2;
    }

    // TODO(b/147021230): Will move common functions and variables to
    //  android/internal/accessibility folder
    private static final char COMPONENT_NAME_SEPARATOR = ':';
    private static final TextUtils.SimpleStringSplitter sStringColonSplitter =
            new TextUtils.SimpleStringSplitter(COMPONENT_NAME_SEPARATOR);

    /**
     * Denotes the quick setting tooltip type.
     *
     * {@code GUIDE_TO_EDIT} for QS tiles that need to be added by editing.
     * {@code GUIDE_TO_DIRECT_USE} for QS tiles that have been auto-added already.
     */
    public @interface QuickSettingsTooltipType {
        int GUIDE_TO_EDIT = 0;
        int GUIDE_TO_DIRECT_USE = 1;
    }

    /** Denotes the accessibility enabled status */
    @Retention(RetentionPolicy.SOURCE)
    public @interface State {
        int OFF = 0;
        int ON = 1;
    }

    /**
     * Returns On/Off string according to the setting which specifies the integer value 1 or 0. This
     * setting is defined in the secure system settings {@link android.provider.Settings.Secure}.
     */
    public static CharSequence getSummary(
            Context context, String settingsSecureKey, @StringRes int enabledString,
            @StringRes int disabledString) {
        boolean enabled = Settings.Secure.getIntForUser(context.getContentResolver(),
                settingsSecureKey, State.OFF, context.getUserId()) == State.ON;
        return context.getResources().getText(enabled ? enabledString : disabledString);
    }

    /**
     * Capitalizes a string by capitalizing the first character and making the remaining characters
     * lower case.
     */
    public static String capitalize(String stringToCapitalize) {
        if (stringToCapitalize == null) {
            return null;
        }

        StringBuilder capitalizedString = new StringBuilder();
        if (stringToCapitalize.length() > 0) {
            capitalizedString.append(stringToCapitalize.substring(0, 1).toUpperCase());
            if (stringToCapitalize.length() > 1) {
                capitalizedString.append(stringToCapitalize.substring(1).toLowerCase());
            }
        }
        return capitalizedString.toString();
    }

    /** Determines if a gesture navigation bar is being used. */
    public static boolean isGestureNavigateEnabled(Context context) {
        return Settings.Secure.getIntForUser(context.getContentResolver(),
                Settings.Secure.NAVIGATION_MODE, -1, context.getUserId())
                == NAV_BAR_MODE_GESTURAL;
    }

    /**
     * Determines if the accessibility button location can be configured.
     */
    public static boolean isAccessibilityButtonLocationConfigurable(Context context) {
        // 3-button navigation always allows configuring the button location.
        if (!isGestureNavigateEnabled(context)) {
            return true;
        }

        // Conditions to force floating menu mode:
        // 1. Flag is off (original behavior): Always use floating menu in gestural nav.
        // 2. Flag is on: Only use floating menu if nav bar can move (i.e. not persistent).
        final boolean navBarCanMove = context.getResources().getBoolean(
                com.android.internal.R.bool.config_navBarCanMove);
        return android.view.accessibility.Flags.allowA11yButtonOnLargeScreen() && !navBarCanMove;
    }

    /** Determines if a accessibility floating menu is being used. */
    public static boolean isFloatingMenuEnabled(Context context) {
        return Settings.Secure.getIntForUser(context.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_BUTTON_MODE, /* def= */ -1, context.getUserId())
                == ACCESSIBILITY_BUTTON_MODE_FLOATING_MENU;
    }

    /** Determines if a touch explore is being used. */
    public static boolean isTouchExploreEnabled(Context context) {
        final AccessibilityManager am = context.getSystemService(AccessibilityManager.class);
        return am.isTouchExplorationEnabled();
    }

    /** Determines if a touch shortcut should be made available. */
    public static boolean isTouchShortcutAvailable(Context context) {
        final boolean isTouchEnabled = context.getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)
                || context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_FAKETOUCH);

        return isTouchEnabled
                || !com.android.settings.accessibility.Flags.desktopMagnificationSettingsPolish();
    }

    /**
     * Gets the corresponding fragment type of a given accessibility service.
     *
     * @param accessibilityServiceInfo The accessibilityService's info
     * @return int from {@link AccessibilityServiceFragmentType}
     */
    static @AccessibilityServiceFragmentType int getAccessibilityServiceFragmentType(
            AccessibilityServiceInfo accessibilityServiceInfo) {
        final int targetSdk = accessibilityServiceInfo.getResolveInfo()
                .serviceInfo.applicationInfo.targetSdkVersion;
        final boolean requestA11yButton = (accessibilityServiceInfo.flags
                & AccessibilityServiceInfo.FLAG_REQUEST_ACCESSIBILITY_BUTTON) != 0;

        if (targetSdk <= Build.VERSION_CODES.Q) {
            return AccessibilityServiceFragmentType.VOLUME_SHORTCUT_TOGGLE;
        }
        return requestA11yButton
                ? AccessibilityServiceFragmentType.INVISIBLE_TOGGLE
                : AccessibilityServiceFragmentType.TOGGLE;
    }

    /**
     * Gets the corresponding user shortcut type of a given accessibility service.
     *
     * @param context The current context.
     * @param componentName The component name that need to be checked existed in Settings.
     * @return The user shortcut type if component name existed in {@code UserShortcutType} string
     * Settings.
     */
    public static int getUserShortcutTypesFromSettings(@NonNull Context context,
            @NonNull ComponentName componentName) {
        // TODO(b/147990389): Delete the branching logic for getting componentNameString after we
        //  migrated to MAGNIFICATION_COMPONENT_NAME.
        String componentNameString;
        if (componentName.equals(MAGNIFICATION_COMPONENT_NAME)) {
            componentNameString = MAGNIFICATION_CONTROLLER_NAME;
        } else {
            componentNameString = componentName.flattenToString();
        }
        int shortcutTypes = UserShortcutType.DEFAULT;
        for (int shortcutType : AccessibilityUtil.SHORTCUTS_ORDER_IN_UI) {
            if (shortcutType == KEY_GESTURE && !isKeyboardShortcutSettingAvailable()) {
                continue;
            }
            if (ShortcutUtils.isShortcutContained(
                    context, shortcutType, componentNameString)) {
                shortcutTypes |= shortcutType;
            }
        }

        return shortcutTypes;
    }

    /**
     * Gets the width of the screen.
     *
     * @param context the current context.
     * @return the width of the screen in terms of pixels.
     */
    public static int getScreenWidthPixels(Context context) {
        final Resources resources = context.getResources();
        final int screenWidthDp = resources.getConfiguration().screenWidthDp;

        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, screenWidthDp,
                resources.getDisplayMetrics()));
    }

    /**
     * Gets the height of the screen.
     *
     * @param context the current context.
     * @return the height of the screen in terms of pixels.
     */
    public static int getScreenHeightPixels(Context context) {
        final Resources resources = context.getResources();
        final int screenHeightDp = resources.getConfiguration().screenHeightDp;

        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, screenHeightDp,
                resources.getDisplayMetrics()));
    }

    /**
     * Gets the bounds of the display window excluding the insets of the system bar and display
     * cut out.
     *
     * @param context the current context.
     * @return the bounds of the display window.
     */
    public static Rect getDisplayBounds(Context context) {
        final WindowManager windowManager = context.getSystemService(WindowManager.class);
        final WindowMetrics metrics = windowManager.getCurrentWindowMetrics();

        final Rect displayBounds = metrics.getBounds();
        final Insets displayInsets = metrics.getWindowInsets().getInsetsIgnoringVisibility(
                systemBars() | displayCutout());
        displayBounds.inset(displayInsets);

        return displayBounds;
    }

    /**
     * Indicates if the accessibility service belongs to a system App.
     * @param info AccessibilityServiceInfo
     * @return {@code true} if the App is a system App.
     */
    public static boolean isSystemApp(@NonNull AccessibilityServiceInfo info) {
        return info.getResolveInfo().serviceInfo.applicationInfo.isSystemApp();
    }

    /**
     * Assembles a localized string describing the provided shortcut types.
     */
    @NonNull
    public static CharSequence getShortcutSummaryList(@NonNull Context context, int shortcutTypes) {
        final List<CharSequence> list = new ArrayList<>();

        for (int shortcutType : AccessibilityUtil.SHORTCUTS_ORDER_IN_UI) {
            if (!isKeyboardShortcutSettingAvailable()) {
                shortcutTypes = removeTypeFromShortcutTypes(shortcutTypes, KEY_GESTURE);
            }

            if ((shortcutTypes & shortcutType) == shortcutType) {
                list.add(switch (shortcutType) {
                    case QUICK_SETTINGS -> context.getText(
                            R.string.accessibility_feature_shortcut_setting_summary_quick_settings);
                    case SOFTWARE -> context.getText(
                            R.string.accessibility_shortcut_edit_summary_software);
                    case GESTURE -> context.getText(
                            R.string.accessibility_shortcut_edit_summary_software_gesture);
                    case HARDWARE -> context.getText(
                            R.string.accessibility_shortcut_hardware_keyword);
                    case TRIPLETAP -> context.getText(
                            R.string.accessibility_shortcut_triple_tap_keyword);
                    case KEY_GESTURE -> context.getText(
                            R.string.accessibility_shortcut_keyboard_keyword);
                    default -> "";
                });
            }
        }

        if (list.isEmpty()) {
            Log.e(TAG, "With empty shortcut list, the preference should not be checked, "
                    + "and this method should not be called");
            return "";
        }

        list.sort(CharSequence::compare);
        return CaseMap.toTitle().wholeString().noLowercase().apply(Locale.getDefault(), /* iter= */
                null, LocaleUtils.getConcatenatedString(list));
    }

    /**
     * @return true if the keyboard shortcut setting is available for use, false otherwise.
     */
    public static boolean isKeyboardShortcutSettingAvailable() {
        return Flags.enableKeyGestureShortcutSettings()
                && InputPeripheralsSettingsUtils.isHardKeyboard();
    }

    /**
     * Removes a specific shortcut type from a bitmask of shortcut types.
     * @param shortcutTypes int containing bitmask of shortcut types.
     * @param typeToRemove int shortcut type to remove.
     * @return updated bitmask of shortcutTypes without the typeToRemove shortcut type.
     */
    public static int removeTypeFromShortcutTypes(int shortcutTypes, int typeToRemove) {
        return shortcutTypes & ~typeToRemove;
    }
}
