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

import android.annotation.SuppressLint
import android.app.Application
import android.permission.flags.Flags
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.safetycenter.SafetyCenterData
import android.safetycenter.SafetyCenterEntry
import android.safetycenter.SafetyCenterIssue
import android.safetycenter.SafetyCenterManager
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.preference.Preference
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.swipeUp
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.safetycenter.SafetyCenterTestUtils.EMPTY_SC_DATA
import com.android.settings.safetycenter.SafetyCenterTestUtils.createEntry
import com.android.settings.safetycenter.SafetyCenterTestUtils.createIssue
import com.android.settings.safetycenter.SafetyCenterTestUtils.createScData
import com.android.settings.safetycenter.ui.SafetyCenterFragment
import com.android.settingslib.widget.preference.statusbanner.R as SettingsLibR
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowDrawable
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.ShadowSafetyCenterManager

// Suppressing MissingPermission lint: The Settings app holds the MANAGE_SAFETY_CENTER permission,
// which is required by the SafetyCenterManager APIs.
@SuppressLint("MissingPermission")
@RunWith(AndroidJUnit4::class)
class SafetyCenterFragmentTest {
    @get:Rule val setFlagsRule = SetFlagsRule()
    private lateinit var mApplication: Application
    private lateinit var shadowSafetyCenterManager: ShadowSafetyCenterManager

    @Before
    fun setUp() {
        mApplication = ApplicationProvider.getApplicationContext()
        val safetyCenterManager = mApplication.getSystemService(SafetyCenterManager::class.java)!!
        shadowSafetyCenterManager = Shadow.extract(safetyCenterManager)
        shadowSafetyCenterManager.setSafetyCenterEnabled(true)
    }

    private fun runTest(data: SafetyCenterData, testBlock: (SafetyCenterFragment) -> Unit) {
        val scenario =
            launchFragmentInContainer<SafetyCenterFragment>(themeResId = R.style.Theme_SubSettings)
        scenario.onFragment { fragment ->
            shadowSafetyCenterManager.setSafetyCenterData(data)
            ShadowLooper.idleMainLooper()
            testBlock(fragment)
        }
        scenario.close()
    }

    private fun assertIconResource(preference: Preference?, expectedResId: Int) {
        assertThat(preference?.icon).isNotNull()
        val shadowDrawable: ShadowDrawable = Shadow.extract(preference?.icon)
        assertThat(shadowDrawable.createdFromResId).isEqualTo(expectedResId)
    }

    private fun expectedDefaultDeviceUnlockSummary(): String {
        return mApplication.getString(DEFAULT_DEVICE_UNLOCK_SUMMARY_RES)
    }

    @Test
    fun fragment_onLaunch_showsAllPreferences() {
        runTest(EMPTY_SC_DATA) { _ ->
            onView(withId(SettingsLibR.id.banner_container)).check(matches(isDisplayed()))

            onView(withText(mApplication.getString(R.string.security_header)))
                .check(matches(isDisplayed()))

            onView(withText(mApplication.getString(R.string.device_unlock_subpage_title)))
                .check(matches(isDisplayed()))

            onView(withText(mApplication.getString(R.string.privacy_dashboard_title)))
                .check(matches(isDisplayed()))

            onView(withText(mApplication.getString(R.string.privacy_sources_title)))
                .check(matches(isDisplayed()))

            onView(isRoot()).perform(swipeUp())
            onView(withText(mApplication.getString(R.string.permissions_usage_title)))
                .perform(scrollTo())
                .check(matches(isDisplayed()))

            onView(withText(mApplication.getString(R.string.more_security_privacy_category_title)))
                .check(matches(isDisplayed()))

            onView(withText(mApplication.getString(R.string.more_security_privacy_settings)))
                .check(matches(isDisplayed()))
        }
    }

    // Tests for Device Unlock preference summary and icon in Safety Center main page
    @Test
    @EnableFlags(Flags.FLAG_OPEN_SAFETY_CENTER_APIS)
    fun deviceUnlockPref_whenNoData_usesDefaultSummaryAndNullIcon() {
        runTest(EMPTY_SC_DATA) { fragment ->
            val preference = fragment.findPreference<Preference>(DEVICE_UNLOCK_KEY)
            assertThat(preference?.summary.toString())
                .isEqualTo(expectedDefaultDeviceUnlockSummary())
            assertThat(preference?.icon).isNull()
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_OPEN_SAFETY_CENTER_APIS)
    fun deviceUnlockPref_withOkEntryAndNoIssues_usesDefaultSummaryAndInfoIcon() {
        val entry =
            createEntry(
                id = "TestEntry",
                title = "Entry with Severity Level OK",
                sourceId = ANDROID_LOCK_SCREEN_SOURCE_ID,
                severity = SafetyCenterEntry.ENTRY_SEVERITY_LEVEL_OK,
            )

        runTest(createScData(entries = listOf(entry))) { fragment ->
            val preference = fragment.findPreference<Preference>(DEVICE_UNLOCK_KEY)
            assertThat(preference?.summary.toString())
                .isEqualTo(expectedDefaultDeviceUnlockSummary())
            assertIconResource(preference, R.drawable.ic_safety_info)
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_OPEN_SAFETY_CENTER_APIS)
    fun deviceUnlockPref_withOkEntryAndOkIssue_usesEntrySummaryAndInfoIcon() {
        val entry =
            createEntry(
                id = "TestEntry",
                title = "Entry with Severity Level OK",
                sourceId = ANDROID_LOCK_SCREEN_SOURCE_ID,
                summary = "Entry Summary OK",
                severity = SafetyCenterEntry.ENTRY_SEVERITY_LEVEL_OK,
            )
        val issue =
            createIssue(
                id = "TestIssue",
                title = "Issue with Severity Level OK",
                summary = "Issue Summary",
                sourceIds = setOf(ANDROID_LOCK_SCREEN_SOURCE_ID),
                severity = SafetyCenterIssue.ISSUE_SEVERITY_LEVEL_OK,
            )

        runTest(createScData(entries = listOf(entry), activeIssues = listOf(issue))) { fragment ->
            val preference = fragment.findPreference<Preference>(DEVICE_UNLOCK_KEY)
            assertThat(preference?.summary.toString()).isEqualTo(entry.summary)
            assertIconResource(preference, R.drawable.ic_safety_info)
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_OPEN_SAFETY_CENTER_APIS)
    fun deviceUnlockPref_withRecommendationEntry_usesEntrySummaryAndRecoIcon() {
        val entry =
            createEntry(
                id = "TestEntry",
                title = "Entry with Severity Level Recommendation",
                sourceId = ANDROID_LOCK_SCREEN_SOURCE_ID,
                summary = "Entry Summary Recommendation",
                severity = SafetyCenterEntry.ENTRY_SEVERITY_LEVEL_RECOMMENDATION,
            )

        runTest(createScData(entries = listOf(entry))) { fragment ->
            val preference = fragment.findPreference<Preference>(DEVICE_UNLOCK_KEY)
            assertThat(preference?.summary.toString()).isEqualTo(entry.summary)
            assertIconResource(preference, R.drawable.ic_safety_recommendation)
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_OPEN_SAFETY_CENTER_APIS)
    fun deviceUnlockPref_withCriticalEntry_usesEntrySummaryAndWarnIcon() {
        val entry =
            createEntry(
                id = "TestEntry",
                title = "Entry with Severity Level Critical",
                sourceId = ANDROID_LOCK_SCREEN_SOURCE_ID,
                summary = "Entry Summary Critical",
                severity = SafetyCenterEntry.ENTRY_SEVERITY_LEVEL_CRITICAL_WARNING,
            )

        runTest(createScData(entries = listOf(entry))) { fragment ->
            val preference = fragment.findPreference<Preference>(DEVICE_UNLOCK_KEY)
            assertThat(preference?.summary.toString()).isEqualTo(entry.summary)
            assertIconResource(preference, R.drawable.ic_safety_warn)
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_OPEN_SAFETY_CENTER_APIS)
    fun deviceUnlockPref_withUnspecifiedEntryAndNoIconType_usesDefaultSummaryAndEmptyIcon() {
        val entry =
            createEntry(
                id = "TestEntry",
                title = "Entry with Severity Level Unspecified",
                sourceId = ANDROID_LOCK_SCREEN_SOURCE_ID,
                severity = SafetyCenterEntry.ENTRY_SEVERITY_LEVEL_UNSPECIFIED,
                iconType = SafetyCenterEntry.SEVERITY_UNSPECIFIED_ICON_TYPE_NO_ICON,
            )

        runTest(createScData(entries = listOf(entry))) { fragment ->
            val preference = fragment.findPreference<Preference>(DEVICE_UNLOCK_KEY)
            assertThat(preference?.summary.toString())
                .isEqualTo(expectedDefaultDeviceUnlockSummary())
            assertIconResource(preference, R.drawable.ic_safety_empty)
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_OPEN_SAFETY_CENTER_APIS)
    fun deviceUnlockPref_withUnknownEntryAndError_usesErrorSummaryAndNullIcon() {
        val entry =
            createEntry(
                id = "TestEntry",
                title = "Entry with Severity Level Unknown",
                sourceId = ANDROID_LOCK_SCREEN_SOURCE_ID,
                severity = SafetyCenterEntry.ENTRY_SEVERITY_LEVEL_UNKNOWN,
                hasError = true,
            )

        runTest(createScData(entries = listOf(entry))) { fragment ->
            val preference = fragment.findPreference<Preference>(DEVICE_UNLOCK_KEY)
            assertThat(preference?.summary.toString())
                .isEqualTo(mApplication.getString(R.string.safety_center_refresh_error))
            assertThat(preference?.icon).isNull()
        }
    }

    @Test
    @DisableFlags(Flags.FLAG_OPEN_SAFETY_CENTER_APIS)
    fun deviceUnlockPref_whenFlagDisabled_usesDefaultSummaryAndNullIcon() {
        val entry =
            createEntry(
                id = "TestEntry",
                title = "Entry with Severity Level OK",
                sourceId = ANDROID_LOCK_SCREEN_SOURCE_ID,
                severity = SafetyCenterEntry.ENTRY_SEVERITY_LEVEL_OK,
            )
        val issue =
            createIssue(
                id = "TestIssue",
                title = "Issue with Severity Level OK",
                summary = "Issue Summary",
                sourceIds = setOf(ANDROID_LOCK_SCREEN_SOURCE_ID),
                severity = SafetyCenterIssue.ISSUE_SEVERITY_LEVEL_OK,
            )

        runTest(createScData(entries = listOf(entry), activeIssues = listOf(issue))) { fragment ->
            val preference = fragment.findPreference<Preference>(DEVICE_UNLOCK_KEY)
            assertThat(preference?.summary.toString())
                .isEqualTo(expectedDefaultDeviceUnlockSummary())
            assertThat(preference?.icon).isNull()
        }
    }

    companion object {
        private const val DEVICE_UNLOCK_KEY = "device_unlock_subpage"
        private const val ANDROID_LOCK_SCREEN_SOURCE_ID = "AndroidLockScreen"
        private val DEFAULT_DEVICE_UNLOCK_SUMMARY_RES =
            R.string.device_unlock_subpage_default_summary
    }
}
