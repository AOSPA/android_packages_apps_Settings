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

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.res.Configuration
import android.os.Bundle
import android.platform.test.annotations.RequiresFlagsEnabled
import android.platform.test.flag.junit.CheckFlagsRule
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import android.security.Flags
import android.security.Flags.FLAG_AAPM_FEATURE_DISABLE_INSECURE_WIFI_AUTOJOIN_V2
import android.security.advancedprotection.AdvancedProtectionManager
import android.security.advancedprotection.AdvancedProtectionManager.FEATURE_ID_DISALLOW_CELLULAR_2G
import android.security.advancedprotection.AdvancedProtectionManager.SUPPORT_DIALOG_TYPE_BLOCKED_INTERACTION
import android.security.advancedprotection.AdvancedProtectionManager.SUPPORT_DIALOG_TYPE_DISABLED_SETTING
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ActivityScenario.launch
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RequiresFlagsEnabled(Flags.FLAG_AAPM_API, FLAG_AAPM_FEATURE_DISABLE_INSECURE_WIFI_AUTOJOIN_V2)
@RunWith(AndroidJUnit4::class)
class ActionDisabledByAdvancedProtectionDialogTest {
    @get:Rule val composeTestRule = createEmptyComposeRule()

    @get:Rule val checkFlagsRule: CheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()

    private val mockPackageManager = mock<PackageManager>()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private lateinit var wifiAutojoinIntent: Intent

    @Before
    fun setUp() {
        val hasGmsFeature =
            try {
                // Check for a known GMS-dependent package
                context.packageManager.getPackageInfo("com.google.android.gms", 0)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }

        if (!hasGmsFeature) {
            // Skip the test gracefully if the dependency is missing
            assumeTrue(
                "Skipping test on AOSP build (GMS/Advanced Protection dependencies missing)",
                false,
            )
        }

        wifiAutojoinIntent =
            AdvancedProtectionManager.createSupportIntent(
                AdvancedProtectionManager.FEATURE_ID_DISALLOW_INSECURE_WIFI_AUTOJOIN,
                SUPPORT_DIALOG_TYPE_DISABLED_SETTING,
            )
    }

    @Test
    fun blockedInteractionDialog_showsCorrectTitleAndMessage() {
        val intent =
            AdvancedProtectionManager.createSupportIntent(
                FEATURE_ID_DISALLOW_CELLULAR_2G,
                SUPPORT_DIALOG_TYPE_BLOCKED_INTERACTION,
            )

        launchDialogActivity(intent) {
            composeTestRule
                .onNodeWithText(context.getString(R.string.disabled_by_advanced_protection_title))
                .assertIsDisplayed()
            composeTestRule
                .onNodeWithText(
                    context.getString(R.string.disabled_by_advanced_protection_action_message)
                )
                .assertIsDisplayed()
        }
    }

    @Test
    fun disabled2gSettingDialog_showsCorrectTitleAndMessage() {
        val intent =
            AdvancedProtectionManager.createSupportIntent(
                FEATURE_ID_DISALLOW_CELLULAR_2G,
                SUPPORT_DIALOG_TYPE_DISABLED_SETTING,
            )

        launchDialogActivity(intent) {
            composeTestRule
                .onNodeWithText(context.getString(R.string.disabled_by_advanced_protection_title))
                .assertIsDisplayed()
            composeTestRule
                .onNodeWithText(
                    context.getString(
                        R.string.disabled_by_advanced_protection_setting_is_on_message
                    )
                )
                .assertIsDisplayed()
        }
    }

    @Test
    fun disabledMteSettingDialog_showsCorrectTitleAndMessage() {
        val intent =
            AdvancedProtectionManager.createSupportIntent(
                AdvancedProtectionManager.FEATURE_ID_ENABLE_MTE,
                SUPPORT_DIALOG_TYPE_DISABLED_SETTING,
            )

        launchDialogActivity(intent) {
            composeTestRule
                .onNodeWithText(context.getString(R.string.disabled_by_advanced_protection_title))
                .assertIsDisplayed()
            composeTestRule
                .onNodeWithText(
                    context.getString(
                        R.string.disabled_by_advanced_protection_setting_is_on_message
                    )
                )
                .assertIsDisplayed()
        }
    }

    @Test
    fun disabledInstallUnknownSourcesSettingDialog_showsCorrectTitleAndMessage() {
        val intent =
            AdvancedProtectionManager.createSupportIntent(
                AdvancedProtectionManager.FEATURE_ID_DISALLOW_INSTALL_UNKNOWN_SOURCES,
                SUPPORT_DIALOG_TYPE_DISABLED_SETTING,
            )

        launchDialogActivity(intent) {
            composeTestRule
                .onNodeWithText(context.getString(R.string.disabled_by_advanced_protection_title))
                .assertIsDisplayed()
            composeTestRule
                .onNodeWithText(
                    context.getString(
                        R.string.disabled_by_advanced_protection_setting_is_off_message
                    )
                )
                .assertIsDisplayed()
        }
    }

    @Test
    fun helpIntentDoesNotExist_getSupportButtonIfExists_returnsNull() {
        launchDialogActivity(defaultIntent) { scenario ->
            scenario.onActivity { activity ->
                val spyActivity = spyOnActivityHelpIntentUri(activity, /* uriToReturn */ null)

                val button = spyActivity.getSupportButtonIfExists()
                assertNull(button)
            }
        }
    }

    @Test
    fun helpIntentExistsAndDoesNotResolveToActivity_getSupportButtonIfExists_returnsNull() {
        launchDialogActivity(defaultIntent) { scenario ->
            scenario.onActivity { activity ->
                val spyActivity = spyOnActivityHelpIntentUri(activity, helpIntentUri)
                mockResolveActivity(spyActivity, /* resolveInfoToReturn */ null)

                val button = spyActivity.getSupportButtonIfExists()
                assertNull(button)
            }
        }
    }

    @Test
    fun helpIntentExistsAndResolvesToActivity_getSupportButtonIfExists_returnsButton() {
        launchDialogActivity(defaultIntent) { scenario ->
            scenario.onActivity { activity ->
                val spyActivity = spyOnActivityHelpIntentUri(activity, helpIntentUri)
                val resolveInfoToReturn =
                    ResolveInfo().apply {
                        activityInfo = ActivityInfo().apply { packageName = HELP_INTENT_PKG_NAME }
                    }
                mockResolveActivity(spyActivity, resolveInfoToReturn)

                // 1. Check the button is returned.
                val button = spyActivity.getSupportButtonIfExists()
                assertNotNull(button)

                // 2. Check the button has correct text.
                assertEquals(
                    context.getString(R.string.disabled_by_advanced_protection_help_button_title),
                    button!!.text,
                )

                // 3. Check the button's onClick launches the help activity and finishes the dialog.
                button.onClick()

                val intentCaptor = ArgumentCaptor.forClass(Intent::class.java)
                verify(spyActivity).startActivity(intentCaptor.capture())
                val launchedIntent = intentCaptor.value
                assertEquals(HELP_INTENT_ACTION, launchedIntent.action)
                assertEquals(HELP_INTENT_PKG_NAME, launchedIntent.`package`)
            }
        }
    }

    @Test
    fun insecureWifiAutoJoinDialog_withLearnMoreHyperlink_showsCorrectStrings() {
        launchDialogActivity(wifiAutojoinIntent) { scenario ->
            scenario.onActivity { activity ->
                val spyActivity = spyOnActivityHelpIntentUri(activity, helpIntentUri)

                val resolveInfoToReturn =
                    ResolveInfo().apply {
                        activityInfo = ActivityInfo().apply { packageName = HELP_INTENT_PKG_NAME }
                    }
                mockResolveActivity(spyActivity, resolveInfoToReturn)
            }

            composeTestRule
                .onNodeWithText(context.getString(R.string.wifi_autojoin_disabled_title))
                .assertIsDisplayed()

            composeTestRule
                .onNodeWithText(
                    context.getString(R.string.wifi_autojoin_disabled_body),
                    substring = true,
                )
                .assertIsDisplayed()

            composeTestRule
                .onNodeWithText(context.getString(R.string.learn_more), substring = true)
                .performScrollTo()
                .assertIsDisplayed()

            composeTestRule
                .onNodeWithText(context.getString(R.string.wifi_autojoin_disabled_positive_button))
                .assertIsDisplayed()

            composeTestRule
                .onNodeWithText(context.getString(R.string.wifi_autojoin_disabled_negative_button))
                .assertIsDisplayed()
        }
    }

    @Test
    fun insecureWifiAutoJoinDialog_withoutLearnMoreHyperlink_showsCorrectStrings() {
        launchDialogActivity(wifiAutojoinIntent) { scenario ->
            scenario.onActivity { activity ->
                spyOnActivityHelpIntentUri(activity, /* uriToReturn */ null)
            }

            composeTestRule
                .onNodeWithText(context.getString(R.string.wifi_autojoin_disabled_title))
                .assertIsDisplayed()
            composeTestRule
                .onNodeWithText(
                    context.getString(R.string.wifi_autojoin_disabled_body),
                    substring = true,
                )
                .assertIsDisplayed()
            composeTestRule
                .onNodeWithText(context.getString(R.string.wifi_autojoin_disabled_positive_button))
                .assertIsDisplayed()
            composeTestRule
                .onNodeWithText(context.getString(R.string.wifi_autojoin_disabled_negative_button))
                .assertIsDisplayed()

            composeTestRule
                .onNodeWithText(context.getString(R.string.learn_more))
                .assertDoesNotExist()
        }
    }

    @Test
    fun insecureWifiAutoJoinDialog_clickTurnOnAnyway_returnsResultOk() {
        val intent =
            wifiAutojoinIntent.setComponent(
                ComponentName(context, ActionDisabledByAdvancedProtectionDialog::class.java)
            )
        val scenario =
            ActivityScenario.launchActivityForResult<ActionDisabledByAdvancedProtectionDialog>(
                intent
            )

        composeTestRule
            .onNodeWithText(context.getString(R.string.wifi_autojoin_disabled_negative_button))
            .performClick()

        val result = scenario.result
        assertEquals(Activity.RESULT_OK, result.resultCode)

        scenario.close()
    }

    @Test
    fun insecureWifiAutoJoinDialog_clickBack_returnsResultCanceled() {
        val intent =
            wifiAutojoinIntent.setComponent(
                ComponentName(context, ActionDisabledByAdvancedProtectionDialog::class.java)
            )
        val scenario =
            ActivityScenario.launchActivityForResult<ActionDisabledByAdvancedProtectionDialog>(
                intent
            )

        composeTestRule
            .onNodeWithText(context.getString(R.string.wifi_autojoin_disabled_positive_button))
            .performClick()

        val result = scenario.result
        assertEquals(Activity.RESULT_CANCELED, result.resultCode)

        scenario.close()
    }

    @Test
    fun insecureWifiAutoJoinDialog_clickLearnMoreHyperlink_launchesHelpIntent() {
        launchDialogActivity(wifiAutojoinIntent) { scenario ->
            scenario.onActivity { activity ->
                val spyActivity = spy(activity)
                val spyResources = spy(spyActivity.resources)
                doReturn(spyResources).whenever(spyActivity).resources
                doReturn(helpIntentUri).whenever(spyResources).getString(helpUriResourceId)

                doReturn(mockPackageManager).whenever(spyActivity).packageManager
                val resolveInfoToReturn =
                    ResolveInfo().apply {
                        activityInfo = ActivityInfo().apply { packageName = HELP_INTENT_PKG_NAME }
                    }
                doReturn(resolveInfoToReturn)
                    .whenever(mockPackageManager)
                    .resolveActivity(any(), anyInt())

                val supportButton = spyActivity.getSupportButtonIfExists()
                assertNotNull(supportButton)

                val onClickAction = supportButton!!.onClick
                onClickAction()

                val intentCaptor = ArgumentCaptor.forClass(Intent::class.java)
                verify(spyActivity).startActivity(intentCaptor.capture())
                val launchedIntent = intentCaptor.value

                assertEquals(HELP_INTENT_ACTION, launchedIntent.action)
                assertEquals(HELP_INTENT_PKG_NAME, launchedIntent.`package`)
            }
        }
    }

    @Test
    fun shouldShowIcon_portraitMode_returnsTrue() {
        // We use launchDialogActivity to get a valid activity instance on the main thread
        launchDialogActivity(defaultIntent) { scenario ->
            scenario.onActivity { activity ->
                val config =
                    Configuration().apply {
                        orientation = Configuration.ORIENTATION_PORTRAIT
                        fontScale = 2.0f // Even huge font is fine in portrait (scrolls)
                        screenHeightDp = 800
                    }

                assertTrue(activity.shouldShowIcon(config))
            }
        }
    }

    @Test
    fun shouldShowIcon_landscape_normalConfig_returnsTrue() {
        launchDialogActivity(defaultIntent) { scenario ->
            scenario.onActivity { activity ->
                val config =
                    Configuration().apply {
                        orientation = Configuration.ORIENTATION_LANDSCAPE
                        fontScale = 1.0f
                        screenHeightDp = 420 // Taller than threshold
                    }

                assertTrue(activity.shouldShowIcon(config))
            }
        }
    }

    @Test
    fun shouldShowIcon_landscape_largeFont_returnsFalse() {
        launchDialogActivity(defaultIntent) { scenario ->
            scenario.onActivity { activity ->
                val config =
                    Configuration().apply {
                        orientation = Configuration.ORIENTATION_LANDSCAPE
                        fontScale = 1.2f // Larger than 1.1f threshold
                        screenHeightDp = 420
                    }

                assertFalse(activity.shouldShowIcon(config))
            }
        }
    }

    @Test
    fun shouldShowIcon_landscape_smallDisplaySize_returnsFalse() {
        launchDialogActivity(defaultIntent) { scenario ->
            scenario.onActivity { activity ->
                val config =
                    Configuration().apply {
                        orientation = Configuration.ORIENTATION_LANDSCAPE
                        fontScale = 1.0f
                        screenHeightDp = 320 // Smaller than 400dp threshold
                    }

                assertFalse(activity.shouldShowIcon(config))
            }
        }
    }

    private fun spyOnActivityHelpIntentUri(
        activity: ActionDisabledByAdvancedProtectionDialog,
        uriToReturn: String?,
    ): ActionDisabledByAdvancedProtectionDialog {
        val spyActivity = spy(activity)
        val spyResources = spy(spyActivity.resources)
        doReturn(spyResources).whenever(spyActivity).resources
        doReturn(uriToReturn).whenever(spyResources).getString(helpUriResourceId)
        return spyActivity
    }

    private fun mockResolveActivity(
        spyActivity: ActionDisabledByAdvancedProtectionDialog,
        resolveInfoToReturn: ResolveInfo?,
    ) {
        doReturn(mockPackageManager).whenever(spyActivity).packageManager
        doReturn(resolveInfoToReturn).whenever(mockPackageManager).resolveActivity(any(), anyInt())
    }

    private fun launchDialogActivity(
        intent: Intent,
        onScenario: (ActivityScenario<ActionDisabledByAdvancedProtectionDialog>) -> Unit,
    ) {
        intent.setComponent(
            ComponentName(context, ActionDisabledByAdvancedProtectionDialog::class.java)
        )
        launch<ActionDisabledByAdvancedProtectionDialog>(intent).use(onScenario)
    }

    class HelpTestActivity : Activity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            finish()
        }
    }

    private companion object {
        val defaultIntent =
            AdvancedProtectionManager.createSupportIntent(
                FEATURE_ID_DISALLOW_CELLULAR_2G,
                SUPPORT_DIALOG_TYPE_BLOCKED_INTERACTION,
            )

        const val HELP_INTENT_PKG_NAME = "com.android.settings.tests.spa_unit"
        const val HELP_INTENT_ACTION = "$HELP_INTENT_PKG_NAME.HELP_ACTION"
        val helpIntent = Intent(HELP_INTENT_ACTION).setPackage(HELP_INTENT_PKG_NAME)
        val helpIntentUri = helpIntent.toUri(Intent.URI_INTENT_SCHEME)
        val helpUriResourceId =
            com.android.internal.R.string.config_help_url_action_disabled_by_advanced_protection
    }
}
