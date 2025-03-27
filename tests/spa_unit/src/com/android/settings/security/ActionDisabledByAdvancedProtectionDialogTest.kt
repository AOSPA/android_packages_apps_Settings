/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.settings.security

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.platform.test.annotations.RequiresFlagsEnabled
import android.platform.test.flag.junit.CheckFlagsRule
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import android.security.Flags
import android.security.advancedprotection.AdvancedProtectionManager
import android.security.advancedprotection.AdvancedProtectionManager.FEATURE_ID_DISALLOW_CELLULAR_2G
import android.security.advancedprotection.AdvancedProtectionManager.FEATURE_ID_DISALLOW_WEP
import android.security.advancedprotection.AdvancedProtectionManager.SUPPORT_DIALOG_TYPE_BLOCKED_INTERACTION
import android.security.advancedprotection.AdvancedProtectionManager.SUPPORT_DIALOG_TYPE_DISABLED_SETTING
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ActivityScenario.launch
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RequiresFlagsEnabled(Flags.FLAG_AAPM_API)
@RunWith(AndroidJUnit4::class)
class ActionDisabledByAdvancedProtectionDialogTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ActionDisabledByAdvancedProtectionDialog>()

    @get:Rule
    val checkFlagsRule: CheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()

    val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun blockedInteractionDialog_showsCorrectTitleAndMessage() {
        val intent = AdvancedProtectionManager.createSupportIntent(
            FEATURE_ID_DISALLOW_CELLULAR_2G,
            SUPPORT_DIALOG_TYPE_BLOCKED_INTERACTION
        )

        launchDialogActivity(intent) {
            composeTestRule
                .onNodeWithText(context.getString(R.string.disabled_by_advanced_protection_title))
                .assertIsDisplayed()
            composeTestRule
                .onNodeWithText(context.getString(
                    R.string.disabled_by_advanced_protection_action_message))
                .assertIsDisplayed()
        }
    }

    @Test
    fun wepBlockedInteraction_showsCorrectTitleAndMessage() {
        val intent = AdvancedProtectionManager.createSupportIntent(
            FEATURE_ID_DISALLOW_WEP,
            SUPPORT_DIALOG_TYPE_BLOCKED_INTERACTION
        )

        launchDialogActivity(intent) {
            composeTestRule
                .onNodeWithText(context.getString(R.string.disabled_by_advanced_protection_title))
                .assertIsDisplayed()
            composeTestRule
                .onNodeWithText(context.getString(
                    R.string.disabled_by_advanced_protection_wep_action_message))
                .assertIsDisplayed()
        }
    }

    @Test
    fun disabled2gSettingDialog_showsCorrectTitleAndMessage() {
        val intent = AdvancedProtectionManager.createSupportIntent(
            FEATURE_ID_DISALLOW_CELLULAR_2G,
            SUPPORT_DIALOG_TYPE_DISABLED_SETTING
        )

        launchDialogActivity(intent) {
            composeTestRule
                .onNodeWithText(context.getString(R.string.disabled_by_advanced_protection_title))
                .assertIsDisplayed()
            composeTestRule
                .onNodeWithText(context.getString(
                    R.string.disabled_by_advanced_protection_setting_is_on_message))
                .assertIsDisplayed()
        }
    }

    @Test
    fun disabledMteSettingDialog_showsCorrectTitleAndMessage() {
        val intent = AdvancedProtectionManager.createSupportIntent(
            AdvancedProtectionManager.FEATURE_ID_ENABLE_MTE,
            SUPPORT_DIALOG_TYPE_DISABLED_SETTING
        )

        launchDialogActivity(intent) {
            composeTestRule
                .onNodeWithText(context.getString(R.string.disabled_by_advanced_protection_title))
                .assertIsDisplayed()
            composeTestRule
                .onNodeWithText(context.getString(
                    R.string.disabled_by_advanced_protection_setting_is_on_message))
                .assertIsDisplayed()
        }
    }

    @Test
    fun disabledWepSettingDialog_showsCorrectTitleAndMessage() {
        val intent = AdvancedProtectionManager.createSupportIntent(
            FEATURE_ID_DISALLOW_WEP,
            SUPPORT_DIALOG_TYPE_DISABLED_SETTING
        )

        launchDialogActivity(intent) {
            composeTestRule
                .onNodeWithText(context.getString(R.string.disabled_by_advanced_protection_title))
                .assertIsDisplayed()
            composeTestRule
                .onNodeWithText(context.getString(
                    R.string.disabled_by_advanced_protection_setting_is_off_message))
                .assertIsDisplayed()
        }
    }

    @Test
    fun disabledInstallUnknownSourcesSettingDialog_showsCorrectTitleAndMessage() {
        val intent = AdvancedProtectionManager.createSupportIntent(
            AdvancedProtectionManager.FEATURE_ID_DISALLOW_INSTALL_UNKNOWN_SOURCES,
            SUPPORT_DIALOG_TYPE_DISABLED_SETTING
        )

        launchDialogActivity(intent) {
            composeTestRule
                .onNodeWithText(context.getString(R.string.disabled_by_advanced_protection_title))
                .assertIsDisplayed()
            composeTestRule
                .onNodeWithText(context.getString(
                    R.string.disabled_by_advanced_protection_setting_is_off_message))
                .assertIsDisplayed()
        }
    }

    private fun launchDialogActivity(
        intent: Intent,
        onScenario: (ActivityScenario<ActionDisabledByAdvancedProtectionDialog>) -> Unit
    ) {
        intent.setComponent(
            ComponentName(
                context,
                ActionDisabledByAdvancedProtectionDialog::class.java
            )
        )
        launch<ActionDisabledByAdvancedProtectionDialog>(intent).use(onScenario)
    }

    private companion object {
        val defaultIntent = AdvancedProtectionManager.createSupportIntent(
            FEATURE_ID_DISALLOW_CELLULAR_2G,
            SUPPORT_DIALOG_TYPE_BLOCKED_INTERACTION
        )
    }
}
