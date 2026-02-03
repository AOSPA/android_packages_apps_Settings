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

package com.android.settings.safetycenter

import android.app.Application
import android.app.compat.CompatChanges
import android.content.Context
import android.hardware.SensorPrivacyManager
import android.os.UserManager
import android.permission.flags.Flags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.safetycenter.SafetyCenterData
import android.safetycenter.SafetyCenterManager
import android.text.ShowSecretsSetting
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.swipeUp
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.safetycenter.SafetyCenterTestUtils.EMPTY_SC_DATA
import com.android.settings.safetycenter.SafetyCenterTestUtils.TEST_ACTION
import com.android.settings.safetycenter.SafetyCenterTestUtils.USER_PERSONAL
import com.android.settings.safetycenter.SafetyCenterTestUtils.createEntry
import com.android.settings.safetycenter.SafetyCenterTestUtils.createScData
import com.android.settings.safetycenter.ui.PrivacyControlsFragment
import com.android.settingslib.safetycenter.SafetySourcePreference
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.anyInt
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import org.robolectric.Shadows
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.ShadowSafetyCenterManager
import org.robolectric.shadows.ShadowUserManager

@RunWith(AndroidJUnit4::class)
@EnableFlags(Flags.FLAG_OPEN_SAFETY_CENTER_APIS)
class PrivacyControlsFragmentTest {
    @get:Rule val setFlagsRule = SetFlagsRule()
    private lateinit var mApplication: Application
    @Mock private lateinit var mSensorPrivacyManager: SensorPrivacyManager
    private lateinit var shadowSafetyCenterManager: ShadowSafetyCenterManager

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        mApplication = ApplicationProvider.getApplicationContext()
        val userManager = mApplication.getSystemService(UserManager::class.java)!!
        val shadowUserManager = Shadow.extract(userManager) as ShadowUserManager
        val safetyCenterManager = mApplication.getSystemService(SafetyCenterManager::class.java)!!
        shadowSafetyCenterManager = Shadow.extract(safetyCenterManager)

        Shadows.shadowOf(mApplication)
            .setSystemService(Context.SENSOR_PRIVACY_SERVICE, mSensorPrivacyManager)

        shadowUserManager.addUser(USER_PERSONAL.identifier, "Personal", 0)
        shadowSafetyCenterManager.setSafetyCenterEnabled(true)

        whenever(mSensorPrivacyManager.supportsSensorToggle(anyInt())).thenReturn(true)
    }

    private fun runTest(data: SafetyCenterData, testBlock: (PrivacyControlsFragment) -> Unit) {
        shadowSafetyCenterManager.setSafetyCenterData(data)
        val scenario = launchFragmentInContainer<PrivacyControlsFragment>()
        scenario.onFragment { fragment ->
            ShadowLooper.idleMainLooper()
            testBlock(fragment)
        }
        scenario.close()
    }

    @Test
    fun shouldShowAllStaticPreferences() {
        runTest(EMPTY_SC_DATA) { _ ->
            onView(withText(mApplication.getString(R.string.privacy_sources_title)))
                .check(matches(isDisplayed()))

            onView(withText(mApplication.getString(R.string.app_permissions)))
                .check(matches(isDisplayed()))

            onView(withText(mApplication.getString(R.string.privacy_controls_title)))
                .check(matches(isDisplayed()))

            onView(withText(mApplication.getString(R.string.camera_toggle_title)))
                .check(matches(isDisplayed()))

            onView(withText(mApplication.getString(R.string.mic_toggle_title)))
                .check(matches(isDisplayed()))

            onView(isRoot()).perform(swipeUp())
            onView(withText(mApplication.getString(R.string.show_clip_access_notification)))
                .check(matches(isDisplayed()))

            onView(isRoot()).perform(swipeUp())
            if (
                com.android.text.flags.Flags.splitShowPasswordsToTouchAndPhysical() &&
                    CompatChanges.isChangeEnabled(
                        ShowSecretsSetting.SPLIT_SHOW_PASSWORDS_TO_TOUCH_AND_PHYSICAL
                    )
            ) {
                onView(withText(mApplication.getString(R.string.show_secrets_touch_summary)))
                    .check(matches(isDisplayed()))
                onView(withText(mApplication.getString(R.string.show_secrets_physical_summary)))
                    .check(doesNotExist())
            } else {
                onView(withText(mApplication.getString(R.string.show_password)))
                    .check(matches(isDisplayed()))
            }

            onView(isRoot()).perform(swipeUp())
            onView(withText(mApplication.getString(R.string.location_settings)))
                .check(matches(isDisplayed()))
        }
    }

    @Test
    fun healthConnectPref_whenEntryExists_isVisibleAndClickable() {
        val entry =
            createEntry(
                id = "healthConnectEntry",
                title = "Health Connect",
                userHandle = USER_PERSONAL,
                sourceId = ANDROID_HEALTH_CONNECT_SOURCE_ID,
                summary = "Manage connected apps",
            )

        runTest(createScData(listOf(entry))) { fragment ->
            val preference =
                fragment.findPreference<SafetySourcePreference>(HEALTH_CONNECT_PREFERENCE_KEY)
            assertThat(preference?.isVisible).isTrue()
            assertThat(preference?.title.toString()).isEqualTo(entry.title)
            assertThat(preference?.summary.toString()).isEqualTo(entry.summary)

            preference?.performClick()
            ShadowLooper.idleMainLooper()
            val startedIntent = shadowOf(mApplication).nextStartedActivity
            assertThat(startedIntent).isNotNull()
            assertThat(startedIntent.action).isEqualTo(TEST_ACTION)
        }
    }

    @Test
    fun healthConnectPref_whenNoEntry_isHidden() {
        runTest(EMPTY_SC_DATA) { fragment ->
            val preference =
                fragment.findPreference<SafetySourcePreference>(HEALTH_CONNECT_PREFERENCE_KEY)
            assertThat(preference?.isVisible).isFalse()
        }
    }

    @Test
    fun appFunctionAccessPref_whenEntryExists_isVisibleAndClickable() {
        val entry =
            createEntry(
                id = "appFunctionAccessEntry",
                title = "App Function Access",
                userHandle = USER_PERSONAL,
                sourceId = ANDROID_APP_FUNCTION_ACCESS_SOURCE_ID,
                summary = "Control app functions",
            )

        runTest(createScData(listOf(entry))) { fragment ->
            val preference =
                fragment.findPreference<SafetySourcePreference>(APP_FUNCTION_ACCESS_PREFERENCE_KEY)
            assertThat(preference?.isVisible).isTrue()
            assertThat(preference?.title.toString()).isEqualTo(entry.title)
            assertThat(preference?.summary.toString()).isEqualTo(entry.summary)

            preference?.performClick()
            ShadowLooper.idleMainLooper()
            val startedIntent = shadowOf(mApplication).nextStartedActivity
            assertThat(startedIntent).isNotNull()
            assertThat(startedIntent.action).isEqualTo(TEST_ACTION)
        }
    }

    @Test
    fun appFunctionAccessPref_whenNoEntry_isHidden() {
        runTest(EMPTY_SC_DATA) { fragment ->
            val preference =
                fragment.findPreference<SafetySourcePreference>(APP_FUNCTION_ACCESS_PREFERENCE_KEY)
            assertThat(preference?.isVisible).isFalse()
        }
    }

    @Test
    fun locationDataSharingPref_whenEntryExists_isVisibleAndClickable() {
        val entry =
            createEntry(
                id = "locationDataSharingEntry",
                title = "Data Sharing Updates",
                userHandle = USER_PERSONAL,
                sourceId = ANDROID_PRIVACY_APP_DATA_SHARING_UPDATES_SOURCE_ID,
                summary = "See how apps share location",
            )

        runTest(createScData(listOf(entry))) { fragment ->
            val preference =
                fragment.findPreference<SafetySourcePreference>(
                    LOCATION_DATA_SHARING_UPDATES_PREFERENCE_KEY
                )
            assertThat(preference?.isVisible).isTrue()
            assertThat(preference?.title.toString()).isEqualTo(entry.title)
            assertThat(preference?.summary.toString()).isEqualTo(entry.summary)

            preference?.performClick()
            ShadowLooper.idleMainLooper()
            val startedIntent = shadowOf(mApplication).nextStartedActivity
            assertThat(startedIntent).isNotNull()
            assertThat(startedIntent.action).isEqualTo(TEST_ACTION)
        }
    }

    @Test
    fun locationDataSharingPref_whenNoEntry_isHidden() {
        runTest(EMPTY_SC_DATA) { fragment ->
            val preference =
                fragment.findPreference<SafetySourcePreference>(
                    LOCATION_DATA_SHARING_UPDATES_PREFERENCE_KEY
                )
            assertThat(preference?.isVisible).isFalse()
        }
    }

    @Test
    fun redirectIfEmpty_withNoSafetySources_doesNotRedirect() {
        runTest(EMPTY_SC_DATA) { fragment ->
            // PrivacyControlsFragment remains visible even without safety source preferences
            // because it contains other privacy settings.
            val nextIntent = shadowOf(fragment.requireActivity()).nextStartedActivity

            assertThat(nextIntent).isNull()
        }
    }

    companion object {
        private const val HEALTH_CONNECT_PREFERENCE_KEY = "health_connect"
        private const val APP_FUNCTION_ACCESS_PREFERENCE_KEY = "app_function_access"
        private const val LOCATION_DATA_SHARING_UPDATES_PREFERENCE_KEY =
            "location_data_sharing_updates"

        private const val ANDROID_HEALTH_CONNECT_SOURCE_ID = "AndroidHealthConnect"
        private const val ANDROID_APP_FUNCTION_ACCESS_SOURCE_ID = "AndroidAppFunctionAccess"
        private const val ANDROID_PRIVACY_APP_DATA_SHARING_UPDATES_SOURCE_ID =
            "AndroidPrivacyAppDataSharingUpdates"
    }
}
