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
import android.content.Intent
import android.os.Bundle
import android.permission.flags.Flags
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.provider.Settings
import android.safetycenter.SafetyCenterData
import android.safetycenter.SafetyCenterEntry
import android.safetycenter.SafetyCenterIssue
import android.safetycenter.SafetyCenterManager
import android.safetycenter.SafetyCenterStatus
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.preference.Preference
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.swipeUp
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.isNotEnabled
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.Settings.EXTRA_SHOW_FRAGMENT
import com.android.settings.Settings.SafetyCenterActivity
import com.android.settings.SubSettings
import com.android.settings.core.instrumentation.SettingsStatsLog
import com.android.settings.dashboard.DashboardFeatureProvider
import com.android.settings.overlay.FeatureFactory
import com.android.settings.safetycenter.SafetyCenterTestUtils.EMPTY_SC_DATA
import com.android.settings.safetycenter.SafetyCenterTestUtils.TEST_ACTION
import com.android.settings.safetycenter.SafetyCenterTestUtils.TEST_SESSION_ID
import com.android.settings.safetycenter.SafetyCenterTestUtils.USER_PERSONAL
import com.android.settings.safetycenter.SafetyCenterTestUtils.USER_WORK_PROFILE
import com.android.settings.safetycenter.SafetyCenterTestUtils.createEntry
import com.android.settings.safetycenter.SafetyCenterTestUtils.createFocusedIntent
import com.android.settings.safetycenter.SafetyCenterTestUtils.createIssue
import com.android.settings.safetycenter.SafetyCenterTestUtils.createIssueAction
import com.android.settings.safetycenter.SafetyCenterTestUtils.createScData
import com.android.settings.safetycenter.ui.Action
import com.android.settings.safetycenter.ui.InteractionLogger
import com.android.settings.safetycenter.ui.LogSeverityLevel
import com.android.settings.safetycenter.ui.NavigationSource
import com.android.settings.safetycenter.ui.PrivacyControlsFragment
import com.android.settings.safetycenter.ui.SafetyCenterFragment
import com.android.settings.safetycenter.ui.SafetyCenterSessionUtils.EXTRA_SESSION_ID
import com.android.settings.safetycenter.ui.SafetyCenterSubpageRegistry
import com.android.settings.safetycenter.ui.SafetySourceProfileType
import com.android.settings.safetycenter.ui.ViewType
import com.android.settingslib.drawer.CategoryKey
import com.android.settingslib.drawer.DashboardCategory
import com.android.settingslib.drawer.Tile
import com.android.settingslib.safetycenter.SafetySourcePreference
import com.android.settingslib.widget.BannerMessagePreference
import com.android.settingslib.widget.BannerMessagePreferenceGroup
import com.android.settingslib.widget.StatusBannerPreference
import com.android.settingslib.widget.StatusBannerPreference.BannerStatus
import com.android.settingslib.widget.preference.banner.R as BannerR
import com.android.settingslib.widget.preference.button.R as ButtonR
import com.android.settingslib.widget.preference.statusbanner.R as StatusBannerR
import com.google.common.truth.Truth.assertThat
import org.hamcrest.Matchers.allOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowDrawable
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.ShadowSafetyCenterManager

// Suppressing MissingPermission lint: The Settings app holds the MANAGE_SAFETY_CENTER permission,
// which is required by the SafetyCenterManager APIs.
@SuppressLint("MissingPermission")
@RunWith(AndroidJUnit4::class)
@Config(shadows = [SafetyCenterTestUtils.ShadowSettingsStatsLog::class])
class SafetyCenterFragmentTest {
    @get:Rule val setFlagsRule = SetFlagsRule()

    @Mock private lateinit var mockFeatureFactory: FeatureFactory
    @Mock private lateinit var mockDashboardFeatureProvider: DashboardFeatureProvider
    @Mock private lateinit var mockTile: Tile

    private lateinit var mApplication: Application
    private lateinit var shadowSafetyCenterManager: ShadowSafetyCenterManager

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        mApplication = ApplicationProvider.getApplicationContext()
        val safetyCenterManager = mApplication.getSystemService(SafetyCenterManager::class.java)!!
        shadowSafetyCenterManager = Shadow.extract(safetyCenterManager)
        shadowSafetyCenterManager.setSafetyCenterEnabled(true)
        SafetyCenterTestUtils.ShadowSettingsStatsLog.reset()

        FeatureFactory.setFactory(mApplication, mockFeatureFactory)
        `when`(mockFeatureFactory.dashboardFeatureProvider).thenReturn(mockDashboardFeatureProvider)
    }

    @Test
    fun newInstance_withQuickSettingsTrue_setsArgument() {
        val fragment = SafetyCenterFragment.newInstance(isQuickSettings = true)

        assertThat(fragment.arguments?.getBoolean(ARG_IS_QUICK_SETTINGS)).isTrue()
    }

    @Test
    fun newInstance_withQuickSettingsFalse_setsArgument() {
        val fragment = SafetyCenterFragment.newInstance(isQuickSettings = false)

        assertThat(fragment.arguments?.getBoolean(ARG_IS_QUICK_SETTINGS, false)).isFalse()
    }

    private fun runTest(
        data: SafetyCenterData,
        isQuickSettings: Boolean = false,
        navigationSource: NavigationSource = NavigationSource.SETTINGS,
        testBlock: (SafetyCenterFragment) -> Unit,
    ) {
        shadowSafetyCenterManager.setSafetyCenterData(data)
        val fragmentArgs =
            Bundle().apply {
                putLong(EXTRA_SESSION_ID, TEST_SESSION_ID)
                putAll(navigationSource.createArgs())
                putBoolean(ARG_IS_QUICK_SETTINGS, isQuickSettings)
            }
        val scenario =
            launchFragmentInContainer<SafetyCenterFragment>(
                fragmentArgs = fragmentArgs,
                themeResId = R.style.Theme_SubSettings,
            )
        scenario.onFragment { fragment ->
            ShadowLooper.idleMainLooper()
            testBlock(fragment)
        }
        scenario.close()
    }

    private fun runTestWithIntent(
        intent: Intent,
        data: SafetyCenterData,
        testBlock: (SafetyCenterFragment) -> Unit,
    ) {
        shadowSafetyCenterManager.setSafetyCenterData(data)
        ActivityScenario.launch<SafetyCenterActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                ShadowLooper.idleMainLooper()

                val fragment = activity.supportFragmentManager.findFragmentById(R.id.main_content)
                assertThat(fragment).isNotNull()
                assertThat(fragment).isInstanceOf(SafetyCenterFragment::class.java)
                testBlock(fragment as SafetyCenterFragment)
            }
        }
    }

    private fun assertIconResource(preference: Preference?, expectedResId: Int) {
        assertThat(preference?.icon).isNotNull()
        val shadowDrawable: ShadowDrawable = Shadow.extract(preference?.icon)
        assertThat(shadowDrawable.createdFromResId).isEqualTo(expectedResId)
    }

    private fun expectedDefaultDeviceUnlockSummary(): String {
        return mApplication.getString(DEFAULT_DEVICE_UNLOCK_SUMMARY_RES)
    }

    private fun expectedDefaultPrivacyControlsSummary(): String {
        return mApplication.getString(DEFAULT_PRIVACY_CONTROLS_SUMMARY_RES)
    }

    @Test
    @EnableFlags(Flags.FLAG_OPEN_SAFETY_CENTER_APIS)
    fun fragment_onLaunch_showsAllPreferences() {
        val entry =
            createEntry(
                id = "TestEntry",
                title = "Entry with Severity Level OK",
                sourceId = ANDROID_LOCK_SCREEN_SOURCE_ID,
                severity = SafetyCenterEntry.ENTRY_SEVERITY_LEVEL_OK,
            )

        runTest(createScData(entries = listOf(entry))) { _ ->
            onView(withId(StatusBannerR.id.banner_container)).check(matches(isDisplayed()))

            onView(withText(mApplication.getString(R.string.security_header)))
                .check(matches(isDisplayed()))

            onView(withText(mApplication.getString(R.string.device_unlock_subpage_title)))
                .check(matches(isDisplayed()))

            onView(withText(mApplication.getString(R.string.privacy_dashboard_title)))
                .check(matches(isDisplayed()))

            onView(isRoot()).perform(swipeUp())
            onView(withText(mApplication.getString(R.string.privacy_sources_title)))
                .check(matches(isDisplayed()))

            onView(withText(mApplication.getString(R.string.permissions_usage_title)))
                .perform(scrollTo())
                .check(matches(isDisplayed()))

            onView(withText(mApplication.getString(R.string.more_security_privacy_category_title)))
                .check(matches(isDisplayed()))

            onView(withText(mApplication.getString(R.string.more_security_privacy_settings)))
                .check(matches(isDisplayed()))
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_OPEN_SAFETY_CENTER_APIS)
    fun fragment_onLaunchQuickSettings_showsIssuesAndStatusButNotSubpages() {
        val activeIssue =
            createIssue(id = "activeIssue", title = "Active Issue Title", sourceIds = setOf("any"))
        val entry =
            createEntry(
                id = "TestEntry",
                title = "Entry with Severity Level OK",
                sourceId = ANDROID_LOCK_SCREEN_SOURCE_ID,
                severity = SafetyCenterEntry.ENTRY_SEVERITY_LEVEL_OK,
            )

        runTest(
            createScData(entries = listOf(entry), activeIssues = listOf(activeIssue)),
            isQuickSettings = true,
            navigationSource = NavigationSource.QUICK_SETTINGS_TILE,
        ) { _ ->
            // Status banner and issues should be present in quick settings.
            onView(withId(StatusBannerR.id.banner_container)).check(matches(isDisplayed()))
            onView(withText(activeIssue.title.toString())).check(matches(isDisplayed()))

            // Subpage preferences should NOT be displayed in quick settings.
            onView(withText(mApplication.getString(R.string.device_unlock_subpage_title)))
                .check(doesNotExist())
            onView(withText(mApplication.getString(R.string.privacy_dashboard_title)))
                .check(doesNotExist())
            onView(withText(mApplication.getString(R.string.more_security_privacy_category_title)))
                .check(doesNotExist())
        }
    }

    // Tests for Device Unlock preference summary and icon in Safety Center main page

    @Test
    @EnableFlags(Flags.FLAG_OPEN_SAFETY_CENTER_APIS)
    fun deviceUnlockPref_whenNoDataAndNoInjectedTiles_subpageHidden() {
        `when`(mockDashboardFeatureProvider.getTilesForCategory(any())).thenReturn(null)

        runTest(EMPTY_SC_DATA) { fragment ->
            val preference = fragment.findPreference<Preference>(DEVICE_UNLOCK_KEY)
            assertThat(preference?.isVisible).isFalse()
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_OPEN_SAFETY_CENTER_APIS)
    fun deviceUnlockPref_whenNoDataButHasInjectedTiles_subpageVisible() {
        val categoryWithTiles = DashboardCategory(CategoryKey.CATEGORY_SC_DEVICE_UNLOCK)
        categoryWithTiles.addTile(mockTile)
        `when`(
                mockDashboardFeatureProvider.getTilesForCategory(
                    CategoryKey.CATEGORY_SC_DEVICE_UNLOCK
                )
            )
            .thenReturn(categoryWithTiles)

        runTest(EMPTY_SC_DATA) { fragment ->
            val preference = fragment.findPreference<Preference>(DEVICE_UNLOCK_KEY)
            assertThat(preference?.isVisible).isTrue()
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_OPEN_SAFETY_CENTER_APIS)
    fun deviceUnlockPref_withOkEntriesAndNoIssues_usesSourceListSummaryAndInfoIcon() {
        val entry1 =
            createEntry(
                id = "TestEntry1",
                title = "Entry1",
                sourceId = ANDROID_LOCK_SCREEN_SOURCE_ID,
                severity = SafetyCenterEntry.ENTRY_SEVERITY_LEVEL_OK,
            )
        val entry2 =
            createEntry(
                id = "TestEntry2",
                title = "Entry2",
                sourceId = ANDROID_FACE_UNLOCK_SOURCE_ID,
                severity = SafetyCenterEntry.ENTRY_SEVERITY_LEVEL_OK,
            )
        val entry3 =
            createEntry(
                id = "TestEntry3",
                title = "Entry3",
                sourceId = ANDROID_FACE_UNLOCK_SOURCE_ID,
                severity = SafetyCenterEntry.ENTRY_SEVERITY_LEVEL_OK,
                userHandle = USER_WORK_PROFILE,
            )

        runTest(createScData(entries = listOf(entry1, entry2, entry3))) { fragment ->
            val preference = fragment.findPreference<Preference>(DEVICE_UNLOCK_KEY)
            assertThat(preference?.isVisible).isTrue()
            assertThat(preference?.summary.toString()).isEqualTo("Entry1, Entry2")
            assertIconResource(preference, R.drawable.ic_safety_info)
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_OPEN_SAFETY_CENTER_APIS)
    fun deviceUnlockPref_withOkWorkEntriesAndNoIssues_usesSourceListSummaryAndInfoIcon() {
        val entry1 =
            createEntry(
                id = "TestEntry1",
                title = "Entry1",
                sourceId = ANDROID_FINGERPRINT_UNLOCK_SOURCE_ID,
                severity = SafetyCenterEntry.ENTRY_SEVERITY_LEVEL_OK,
                userHandle = USER_WORK_PROFILE,
            )
        val entry2 =
            createEntry(
                id = "TestEntry2",
                title = "Entry2",
                sourceId = ANDROID_FACE_UNLOCK_SOURCE_ID,
                severity = SafetyCenterEntry.ENTRY_SEVERITY_LEVEL_OK,
                userHandle = USER_WORK_PROFILE,
            )

        runTest(createScData(entries = listOf(entry1, entry2))) { fragment ->
            val preference = fragment.findPreference<Preference>(DEVICE_UNLOCK_KEY)
            assertThat(preference?.isVisible).isTrue()
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
            assertThat(preference?.isVisible).isTrue()
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
            assertThat(preference?.isVisible).isTrue()
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
            assertThat(preference?.isVisible).isTrue()
            assertThat(preference?.summary.toString()).isEqualTo(entry.summary)
            assertIconResource(preference, R.drawable.ic_safety_warn)
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_OPEN_SAFETY_CENTER_APIS)
    fun deviceUnlockPref_withUnspecifiedEntryAndNoIconType_usesSourceListSummaryAndEmptyIcon() {
        val entry =
            createEntry(
                id = "TestEntry",
                title = "Entry",
                sourceId = ANDROID_LOCK_SCREEN_SOURCE_ID,
                severity = SafetyCenterEntry.ENTRY_SEVERITY_LEVEL_UNSPECIFIED,
                iconType = SafetyCenterEntry.SEVERITY_UNSPECIFIED_ICON_TYPE_NO_ICON,
            )

        runTest(createScData(entries = listOf(entry))) { fragment ->
            val preference = fragment.findPreference<Preference>(DEVICE_UNLOCK_KEY)
            assertThat(preference?.isVisible).isTrue()
            assertThat(preference?.summary.toString()).isEqualTo("Entry")
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
            assertThat(preference?.isVisible).isTrue()
            assertThat(preference?.summary.toString())
                .isEqualTo(mApplication.getString(R.string.safety_center_refresh_error))
            assertIconResource(preference, R.drawable.ic_safety_null_state)
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_OPEN_SAFETY_CENTER_APIS)
    fun deviceUnlockPref_withUnknownAndUnspecifiedEntries_usesErrorSummaryAndNullIcon() {
        val errorEntry =
            createEntry(
                id = "ErrorEntry",
                title = "Error Entry",
                sourceId = ANDROID_LOCK_SCREEN_SOURCE_ID,
                severity = SafetyCenterEntry.ENTRY_SEVERITY_LEVEL_UNKNOWN,
                hasError = true,
            )
        val unspecifiedEntry =
            createEntry(
                id = "UnspecifiedEntry",
                title = "Unspecified Entry",
                sourceId = ANDROID_FACE_UNLOCK_SOURCE_ID,
                severity = SafetyCenterEntry.ENTRY_SEVERITY_LEVEL_UNSPECIFIED,
            )

        // Verifies that UNKNOWN correctly overrides UNSPECIFIED, catching the error state
        runTest(createScData(entries = listOf(errorEntry, unspecifiedEntry))) { fragment ->
            val preference = fragment.findPreference<Preference>(DEVICE_UNLOCK_KEY)
            assertThat(preference?.isVisible).isTrue()
            assertThat(preference?.summary.toString())
                .isEqualTo(mApplication.getString(R.string.safety_center_refresh_error))
            assertIconResource(preference, R.drawable.ic_safety_null_state)
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_OPEN_SAFETY_CENTER_APIS)
    fun deviceUnlockPref_withUnknownAndOkEntries_usesErrorSummaryAndNullIcon() {
        val errorEntry =
            createEntry(
                id = "ErrorEntry",
                title = "Error Entry",
                sourceId = ANDROID_LOCK_SCREEN_SOURCE_ID,
                severity = SafetyCenterEntry.ENTRY_SEVERITY_LEVEL_UNKNOWN,
                hasError = true,
            )
        val okEntry =
            createEntry(
                id = "OkEntry",
                title = "OK Entry",
                sourceId = ANDROID_FACE_UNLOCK_SOURCE_ID,
                severity = SafetyCenterEntry.ENTRY_SEVERITY_LEVEL_OK,
            )

        // Verifies that UNKNOWN correctly overrides OK
        runTest(createScData(entries = listOf(errorEntry, okEntry))) { fragment ->
            val preference = fragment.findPreference<Preference>(DEVICE_UNLOCK_KEY)
            assertThat(preference?.isVisible).isTrue()
            assertThat(preference?.summary.toString())
                .isEqualTo(mApplication.getString(R.string.safety_center_refresh_error))
            assertIconResource(preference, R.drawable.ic_safety_null_state)
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_OPEN_SAFETY_CENTER_APIS)
    fun deviceUnlockPref_withUnknownAndCriticalEntries_usesCriticalSummaryAndWarnIcon() {
        val errorEntry =
            createEntry(
                id = "ErrorEntry",
                title = "Error Entry",
                sourceId = ANDROID_LOCK_SCREEN_SOURCE_ID,
                severity = SafetyCenterEntry.ENTRY_SEVERITY_LEVEL_UNKNOWN,
                hasError = true,
            )
        val criticalEntry =
            createEntry(
                id = "CriticalEntry",
                title = "Critical Entry",
                sourceId = ANDROID_FACE_UNLOCK_SOURCE_ID,
                summary = "Critical Summary",
                severity = SafetyCenterEntry.ENTRY_SEVERITY_LEVEL_CRITICAL_WARNING,
            )

        // Verifies that CRITICAL correctly overrides UNKNOWN
        runTest(createScData(entries = listOf(errorEntry, criticalEntry))) { fragment ->
            val preference = fragment.findPreference<Preference>(DEVICE_UNLOCK_KEY)
            assertThat(preference?.isVisible).isTrue()
            assertThat(preference?.summary.toString()).isEqualTo("Critical Summary")
            assertIconResource(preference, R.drawable.ic_safety_warn)
        }
    }

    @Test
    @DisableFlags(Flags.FLAG_OPEN_SAFETY_CENTER_APIS)
    fun deviceUnlockPref_whenFlagDisabled_subpageHidden() {
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
            assertThat(preference?.isVisible).isFalse()
        }
    }

    // Tests for Privacy controls preference summary and icon in Safety Center main page

    @Test
    @EnableFlags(Flags.FLAG_OPEN_SAFETY_CENTER_APIS)
    fun privacyControlsPref_whenNoData_usesDefaultSummaryAndNullIcon() {
        runTest(EMPTY_SC_DATA) { fragment ->
            val preference = fragment.findPreference<Preference>(PRIVACY_CONTROLS_SUBPAGE_KEY)
            assertThat(preference?.isVisible).isTrue()
            assertThat(preference?.summary.toString())
                .isEqualTo(expectedDefaultPrivacyControlsSummary())
            assertThat(preference?.icon).isNull()
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_OPEN_SAFETY_CENTER_APIS)
    fun privacyControlsPref_whenDataWithNoIssues_usesDefaultSummaryAndNullIcon() {
        val entry =
            createEntry(
                id = "TestEntry",
                title = "Entry with Severity Level OK",
                sourceId = ANDROID_HEALTH_CONNECT_SOURCE_ID,
                severity = SafetyCenterEntry.ENTRY_SEVERITY_LEVEL_OK,
            )

        runTest(createScData(entries = listOf(entry))) { fragment ->
            val preference = fragment.findPreference<Preference>(PRIVACY_CONTROLS_SUBPAGE_KEY)
            assertThat(preference?.isVisible).isTrue()
            assertThat(preference?.summary.toString())
                .isEqualTo(expectedDefaultPrivacyControlsSummary())
            assertThat(preference?.icon).isNull()
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_OPEN_SAFETY_CENTER_APIS)
    fun privacyControlsPref_whenEntryWithIssue_usesEntrySummaryAndNullIcon() {
        val entry =
            createEntry(
                id = "TestEntry",
                title = "Entry with Severity Level OK",
                summary = "health summary",
                sourceId = ANDROID_HEALTH_CONNECT_SOURCE_ID,
                severity = SafetyCenterEntry.ENTRY_SEVERITY_LEVEL_RECOMMENDATION,
            )
        val issue =
            createIssue(
                id = "TestIssue",
                title = "Issue with Severity Level OK",
                summary = "Issue Summary",
                sourceIds = setOf(ANDROID_HEALTH_CONNECT_SOURCE_ID),
                severity = SafetyCenterIssue.ISSUE_SEVERITY_LEVEL_RECOMMENDATION,
            )

        runTest(createScData(entries = listOf(entry), activeIssues = listOf(issue))) { fragment ->
            val preference = fragment.findPreference<Preference>(PRIVACY_CONTROLS_SUBPAGE_KEY)
            assertThat(preference?.isVisible).isTrue()
            assertThat(preference?.summary.toString()).isEqualTo("health summary")
            assertThat(preference?.icon).isNull()
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_OPEN_SAFETY_CENTER_APIS)
    fun privacyControlsPref_whenIssueOnlySourceIssue_usesIssueTitleAndNullIcon() {
        val issue =
            createIssue(
                id = "TestIssue",
                title = "Issue with Severity Level OK",
                summary = "Issue Summary",
                sourceIds = setOf(ANDROID_A11Y_SOURCES_ID),
                severity = SafetyCenterIssue.ISSUE_SEVERITY_LEVEL_OK,
            )

        runTest(createScData(activeIssues = listOf(issue))) { fragment ->
            val preference = fragment.findPreference<Preference>(PRIVACY_CONTROLS_SUBPAGE_KEY)
            assertThat(preference?.isVisible).isTrue()
            assertThat(preference?.summary.toString()).isEqualTo("Issue with Severity Level OK")
            assertThat(preference?.icon).isNull()
        }
    }

    // --- Tests for Issues Banner Group ---
    @Test
    fun issuesBannerGroup_whenNoIssues_isHidden() {
        runTest(EMPTY_SC_DATA) { fragment ->
            val bannerGroup =
                fragment.findPreference<BannerMessagePreferenceGroup>(SAFETY_ISSUES_BANNER_KEY)
            assertThat(bannerGroup?.isVisible).isFalse()
        }
    }

    @Test
    fun issuesBannerGroup_withOnlyDismissedIssues_isHidden() {
        val dismissedIssue = createIssue(id = "dismissedIssue", sourceIds = setOf("any"))
        runTest(createScData(dismissedIssues = listOf(dismissedIssue))) { fragment ->
            val bannerGroup =
                fragment.findPreference<BannerMessagePreferenceGroup>(SAFETY_ISSUES_BANNER_KEY)
            assertThat(bannerGroup?.isVisible).isFalse()
        }
    }

    @Test
    fun issuesBannerGroup_withOneActiveIssue_isVisibleAndShowsBannerDetails() {
        val action1 = createIssueAction(id = "action1", label = "Primary Button")
        val action2 = createIssueAction(id = "action2", label = "Secondary Button")
        val activeIssue =
            createIssue(
                id = "activeIssue",
                title = "Active Issue Title",
                summary = "Active Issue Summary",
                actions = listOf(action1, action2),
                sourceIds = setOf("any"),
            )
        runTest(createScData(activeIssues = listOf(activeIssue))) { fragment ->
            val bannerGroup =
                fragment.findPreference<BannerMessagePreferenceGroup>(SAFETY_ISSUES_BANNER_KEY)
            assertThat(bannerGroup?.isVisible).isTrue()
            assertThat(bannerGroup?.preferenceCount).isEqualTo(1)

            val banner =
                bannerGroup?.findPreference<BannerMessagePreference>("active_${activeIssue.id}")
            assertThat(banner).isNotNull()
            assertThat(banner?.title.toString()).isEqualTo(activeIssue.title)
            assertThat(banner?.summary.toString()).isEqualTo(activeIssue.summary)
            onView(withText(activeIssue.title.toString())).check(matches(isDisplayed()))
            onView(withText(activeIssue.summary.toString())).check(matches(isDisplayed()))
            onView(withId(BannerR.id.banner_dismiss_btn)).check(matches(isDisplayed()))
            onView(withId(BannerR.id.banner_positive_btn))
                .check(matches(allOf(isDisplayed(), withText(action1.label.toString()))))
            onView(withId(BannerR.id.banner_negative_btn))
                .check(matches(allOf(isDisplayed(), withText(action2.label.toString()))))
        }
    }

    @Test
    fun issuesBannerGroup_clickDismissNoConfirmation_removesIssue() {
        val activeIssue =
            createIssue(
                id = "dismissIssueWithoutConfirm",
                title = "Dismiss Issue Without Confirm",
                sourceIds = setOf("any"),
                isDismissible = true,
                shouldConfirmDismissal = false,
            )
        runTest(createScData(activeIssues = listOf(activeIssue))) { fragment ->
            onView(allOf(withId(BannerR.id.banner_dismiss_btn), isDisplayed())).perform(click())
            ShadowLooper.idleMainLooper()

            val bannerGroup =
                fragment.findPreference<BannerMessagePreferenceGroup>(SAFETY_ISSUES_BANNER_KEY)
            assertThat(bannerGroup?.isVisible).isFalse()
        }
    }

    @Test
    fun issuesBannerGroup_clickDismissWithConfirmation_removesIssue() {
        val activeIssue =
            createIssue(
                id = "dismissIssueWithConfirmation",
                title = "Dismiss Issue With Confirmation",
                sourceIds = setOf("any"),
                isDismissible = true,
                shouldConfirmDismissal = true,
            )
        runTest(createScData(activeIssues = listOf(activeIssue))) { fragment ->
            onView(allOf(withId(BannerR.id.banner_dismiss_btn), isDisplayed())).perform(click())
            ShadowLooper.idleMainLooper()

            onView(withText(R.string.safety_center_issue_card_dismiss_confirmation_title))
                .inRoot(isDialog())
                .check(matches(isDisplayed()))

            onView(withText(R.string.dismiss)).inRoot(isDialog()).perform(click())
            ShadowLooper.idleMainLooper()

            val bannerGroup =
                fragment.findPreference<BannerMessagePreferenceGroup>(SAFETY_ISSUES_BANNER_KEY)
            assertThat(bannerGroup?.isVisible).isFalse()
        }
    }

    @Test
    fun issuesBannerGroup_clickPrimaryActionNoConfirmation_resolvesIssue() {
        val action =
            createIssueAction(
                id = "primaryActionWithoutConfirmation",
                label = "Resolve",
                hasConfirmation = false,
                willResolve = true,
            )
        val activeIssue =
            createIssue(
                id = "primaryActionWithoutConfirmationIssue",
                title = "Primary Action Without Confirmation Issue",
                actions = listOf(action),
                sourceIds = setOf("any"),
            )

        runTest(createScData(activeIssues = listOf(activeIssue))) { fragment ->
            onView(allOf(withId(BannerR.id.banner_positive_btn), withText(action.label.toString())))
                .perform(click())
            ShadowLooper.idleMainLooper()

            // Manually update the data to simulate the issue being resolved
            shadowSafetyCenterManager.setSafetyCenterData(EMPTY_SC_DATA)
            ShadowLooper.idleMainLooper()

            val bannerGroup =
                fragment.findPreference<BannerMessagePreferenceGroup>(SAFETY_ISSUES_BANNER_KEY)
            assertThat(bannerGroup?.isVisible).isFalse()
        }
    }

    @Test
    fun issuesBannerGroup_clickPrimaryActionWithConfirmation_resolvesIssue() {
        val action =
            createIssueAction(
                id = "primaryActionWithConfirmation",
                label = "Resolve",
                hasConfirmation = true,
                willResolve = true,
            )
        val activeIssue =
            createIssue(
                id = "primaryActionWithConfirmationIssue",
                title = "Primary Action With Confirmation Issue",
                actions = listOf(action),
                sourceIds = setOf("any"),
            )

        runTest(createScData(activeIssues = listOf(activeIssue))) { fragment ->
            onView(allOf(withId(BannerR.id.banner_positive_btn), withText(action.label.toString())))
                .perform(click())
            ShadowLooper.idleMainLooper()

            val expectedDetails = action.confirmationDialogDetails!!
            onView(withText(expectedDetails.title.toString()))
                .inRoot(isDialog())
                .check(matches(isDisplayed()))

            onView(withText(expectedDetails.acceptButtonText.toString()))
                .inRoot(isDialog())
                .perform(click())
            ShadowLooper.idleMainLooper()

            // Manually update the data to simulate the issue being resolved
            shadowSafetyCenterManager.setSafetyCenterData(EMPTY_SC_DATA)
            ShadowLooper.idleMainLooper()

            val bannerGroup =
                fragment.findPreference<BannerMessagePreferenceGroup>(SAFETY_ISSUES_BANNER_KEY)
            assertThat(bannerGroup?.isVisible).isFalse()
        }
    }

    @Test
    fun issuesBannerGroup_withMultipleIssues_isCollapsedAndCanExpand() {
        val issue1 = createIssue(id = "issue1", title = "Issue 1", sourceIds = setOf("any"))
        val issue2 = createIssue(id = "issue2", title = "Issue 2", sourceIds = setOf("any"))
        val issue3 = createIssue(id = "issue3", title = "Issue 3", sourceIds = setOf("any"))
        runTest(createScData(activeIssues = listOf(issue1, issue2, issue3))) { fragment ->
            val bannerGroup =
                fragment.findPreference<BannerMessagePreferenceGroup>(SAFETY_ISSUES_BANNER_KEY)
            assertThat(bannerGroup?.isVisible).isTrue()
            // 3 active issues, 1 expand preference, 1 collapse preference
            assertThat(bannerGroup?.preferenceCount).isEqualTo(5)

            val banner1 =
                bannerGroup?.findPreference<BannerMessagePreference>("active_${issue1.id}")
            val banner2 =
                bannerGroup?.findPreference<BannerMessagePreference>("active_${issue2.id}")
            val banner3 =
                bannerGroup?.findPreference<BannerMessagePreference>("active_${issue3.id}")

            // Initially collapsed
            assertThat(banner1?.isVisible).isTrue()
            assertThat(banner2?.isVisible).isFalse()
            assertThat(banner3?.isVisible).isFalse()

            val expandButtonText =
                mApplication.getString(R.string.safety_center_issues_banner_group_expandable_title)
            onView(allOf(withId(ButtonR.id.settingslib_number_title), withText(expandButtonText)))
                .check(matches(isDisplayed()))
            onView(allOf(withId(ButtonR.id.settingslib_number_count), withText("2")))
                .check(matches(isDisplayed()))

            // Click expand
            onView(withId(ButtonR.id.settingslib_number_button))
                .perform(scrollTo())
                .perform(click())
            ShadowLooper.idleMainLooper()

            assertThat(banner1?.isVisible).isTrue()
            assertThat(banner2?.isVisible).isTrue()
            assertThat(banner3?.isVisible).isTrue()
        }
    }

    @Test
    fun focusedIssue_noActionIntent_noReordering() {
        val issue1 =
            createIssue(
                id = "issue1",
                title = "Issue 1",
                sourceIds = setOf("any"),
                severity = SafetyCenterIssue.ISSUE_SEVERITY_LEVEL_CRITICAL_WARNING,
            )
        val issue2 =
            createIssue(
                id = "issue2",
                title = "Issue 2",
                sourceIds = setOf("any"),
                severity = SafetyCenterIssue.ISSUE_SEVERITY_LEVEL_RECOMMENDATION,
            )
        val intent = Intent("android.intent.action.MAIN")
        intent.setClass(mApplication, SafetyCenterActivity::class.java)

        runTestWithIntent(intent, createScData(activeIssues = listOf(issue1, issue2))) { fragment ->
            val bannerGroup =
                fragment.findPreference<BannerMessagePreferenceGroup>(SAFETY_ISSUES_BANNER_KEY)
            assertThat(bannerGroup?.visiblePreferencesWhenCollapsedCount).isEqualTo(1)
            assertThat(bannerGroup?.getPreference(0)?.key).isEqualTo("active_${issue1.id}")
            assertThat(bannerGroup?.getPreference(1)?.key).isEqualTo("active_${issue2.id}")
            assertThat(bannerGroup?.findPreference<Preference>("active_${issue1.id}")?.isVisible)
                .isTrue()
            assertThat(bannerGroup?.findPreference<Preference>("active_${issue2.id}")?.isVisible)
                .isFalse()
        }
    }

    @Test
    fun focusedIssue_missingExtras_noReordering() {
        val issue1 =
            createIssue(
                id = "issue1",
                title = "Issue 1",
                sourceIds = setOf("any"),
                severity = SafetyCenterIssue.ISSUE_SEVERITY_LEVEL_CRITICAL_WARNING,
            )
        val issue2 =
            createIssue(
                id = "issue2",
                title = "Issue 2",
                sourceIds = setOf("any"),
                severity = SafetyCenterIssue.ISSUE_SEVERITY_LEVEL_RECOMMENDATION,
            )
        val intent =
            Intent(Intent.ACTION_SAFETY_CENTER).apply {
                putExtra(SafetyCenterManager.EXTRA_SAFETY_SOURCE_ID, "any")
            }

        runTestWithIntent(intent, createScData(activeIssues = listOf(issue1, issue2))) { fragment ->
            val bannerGroup =
                fragment.findPreference<BannerMessagePreferenceGroup>(SAFETY_ISSUES_BANNER_KEY)
            assertThat(bannerGroup?.visiblePreferencesWhenCollapsedCount).isEqualTo(1)
            assertThat(bannerGroup?.getPreference(0)?.key).isEqualTo("active_${issue1.id}")
            assertThat(bannerGroup?.getPreference(1)?.key).isEqualTo("active_${issue2.id}")
            assertThat(bannerGroup?.findPreference<Preference>("active_${issue1.id}")?.isVisible)
                .isTrue()
            assertThat(bannerGroup?.findPreference<Preference>("active_${issue2.id}")?.isVisible)
                .isFalse()
        }
    }

    @Test
    fun focusedIssue_issueNotFound_noReordering() {
        val issue1 =
            createIssue(
                id = "issue1",
                title = "Issue 1",
                sourceIds = setOf("any"),
                severity = SafetyCenterIssue.ISSUE_SEVERITY_LEVEL_CRITICAL_WARNING,
            )
        val issue2 =
            createIssue(
                id = "issue2",
                title = "Issue 2",
                sourceIds = setOf("any"),
                severity = SafetyCenterIssue.ISSUE_SEVERITY_LEVEL_RECOMMENDATION,
            )
        val intent = createFocusedIntent(sourceIssueId = "nonExistentIssue", sourceId = "any")

        runTestWithIntent(intent, createScData(activeIssues = listOf(issue1, issue2))) { fragment ->
            val bannerGroup =
                fragment.findPreference<BannerMessagePreferenceGroup>(SAFETY_ISSUES_BANNER_KEY)
            assertThat(bannerGroup?.visiblePreferencesWhenCollapsedCount).isEqualTo(1)
            assertThat(bannerGroup?.getPreference(0)?.key).isEqualTo("active_${issue1.id}")
            assertThat(bannerGroup?.getPreference(1)?.key).isEqualTo("active_${issue2.id}")
            assertThat(bannerGroup?.findPreference<Preference>("active_${issue1.id}")?.isVisible)
                .isTrue()
            assertThat(bannerGroup?.findPreference<Preference>("active_${issue2.id}")?.isVisible)
                .isFalse()
        }
    }

    @Test
    fun focusedIssue_existsAndIsHighestSeverity_isFirstAndOneVisible() {
        val issue1 =
            createIssue(
                id = "focusedIssueId",
                title = "Focused Issue",
                safetySourceIssueId = "focusedSourceIssueId",
                sourceIds = setOf("testSource"),
                severity = SafetyCenterIssue.ISSUE_SEVERITY_LEVEL_CRITICAL_WARNING,
            )
        val issue2 =
            createIssue(
                id = "issue2",
                title = "Other Issue",
                sourceIds = setOf("any"),
                severity = SafetyCenterIssue.ISSUE_SEVERITY_LEVEL_RECOMMENDATION,
            )
        val intent =
            createFocusedIntent(sourceIssueId = "focusedSourceIssueId", sourceId = "testSource")

        runTestWithIntent(intent, createScData(activeIssues = listOf(issue1, issue2))) { fragment ->
            val bannerGroup =
                fragment.findPreference<BannerMessagePreferenceGroup>(SAFETY_ISSUES_BANNER_KEY)
            assertThat(bannerGroup?.visiblePreferencesWhenCollapsedCount).isEqualTo(1)
            assertThat(bannerGroup?.getPreference(0)?.key).isEqualTo("active_${issue1.id}")
            assertThat(bannerGroup?.getPreference(1)?.key).isEqualTo("active_${issue2.id}")
            assertThat(bannerGroup?.findPreference<Preference>("active_${issue1.id}")?.isVisible)
                .isTrue()
            assertThat(bannerGroup?.findPreference<Preference>("active_${issue2.id}")?.isVisible)
                .isFalse()
        }
    }

    @Test
    fun focusedIssue_existsNotHighestSeverity_isSecondAndTwoVisible() {
        val issue1 =
            createIssue(
                id = "issue1",
                title = "Critical Issue",
                sourceIds = setOf("any"),
                severity = SafetyCenterIssue.ISSUE_SEVERITY_LEVEL_CRITICAL_WARNING,
            )
        val issue2 =
            createIssue(
                id = "focusedIssue",
                title = "Focused Issue",
                safetySourceIssueId = "focusedSourceIssueId",
                sourceIds = setOf("testSource"),
                severity = SafetyCenterIssue.ISSUE_SEVERITY_LEVEL_RECOMMENDATION,
            )
        val issue3 =
            createIssue(
                id = "issue3",
                title = "OK Issue",
                sourceIds = setOf("any"),
                severity = SafetyCenterIssue.ISSUE_SEVERITY_LEVEL_OK,
            )
        val intent =
            createFocusedIntent(sourceIssueId = "focusedSourceIssueId", sourceId = "testSource")

        runTestWithIntent(intent, createScData(activeIssues = listOf(issue1, issue2, issue3))) {
            fragment ->
            val bannerGroup =
                fragment.findPreference<BannerMessagePreferenceGroup>(SAFETY_ISSUES_BANNER_KEY)
            assertThat(bannerGroup?.visiblePreferencesWhenCollapsedCount).isEqualTo(2)
            assertThat(bannerGroup?.getPreference(0)?.key).isEqualTo("active_${issue1.id}")
            assertThat(bannerGroup?.getPreference(1)?.key).isEqualTo("active_${issue2.id}")
            assertThat(bannerGroup?.getPreference(2)?.key).isEqualTo("active_${issue3.id}")
            // First two are visible when collapsed
            assertThat(bannerGroup?.findPreference<Preference>("active_${issue1.id}")?.isVisible)
                .isTrue()
            assertThat(bannerGroup?.findPreference<Preference>("active_${issue2.id}")?.isVisible)
                .isTrue()
            assertThat(bannerGroup?.findPreference<Preference>("active_${issue3.id}")?.isVisible)
                .isFalse()
        }
    }

    @Test
    fun focusedIssue_onlyIssue_isFirstAndOneVisible() {
        val issue1 =
            createIssue(
                id = "focusedIssue",
                title = "Focused Issue",
                safetySourceIssueId = "focusedSourceIssueId",
                sourceIds = setOf("testSource"),
                severity = SafetyCenterIssue.ISSUE_SEVERITY_LEVEL_RECOMMENDATION,
            )
        val intent =
            createFocusedIntent(sourceIssueId = "focusedSourceIssueId", sourceId = "testSource")

        runTestWithIntent(intent, createScData(activeIssues = listOf(issue1))) { fragment ->
            val bannerGroup =
                fragment.findPreference<BannerMessagePreferenceGroup>(SAFETY_ISSUES_BANNER_KEY)
            assertThat(bannerGroup?.visiblePreferencesWhenCollapsedCount).isEqualTo(1)
            assertThat(bannerGroup?.preferenceCount).isEqualTo(1)
            assertThat(bannerGroup?.getPreference(0)?.key).isEqualTo("active_${issue1.id}")
            assertThat(bannerGroup?.findPreference<Preference>("active_${issue1.id}")?.isVisible)
                .isTrue()
        }
    }

    @Test
    fun focusedIssue_withSameHighestSeverity_reordersFocusedToFirst() {
        val issue1 =
            createIssue(
                id = "issue1",
                title = "Critical Issue 1",
                safetySourceIssueId = "critical1",
                sourceIds = setOf("sourceA"),
                severity = SafetyCenterIssue.ISSUE_SEVERITY_LEVEL_CRITICAL_WARNING,
            )
        val issue2 =
            createIssue(
                id = "issue2",
                title = "Critical Issue 2 (Focused)",
                safetySourceIssueId = "critical2",
                sourceIds = setOf("sourceB"),
                severity = SafetyCenterIssue.ISSUE_SEVERITY_LEVEL_CRITICAL_WARNING,
            )
        val intent = createFocusedIntent(sourceIssueId = "critical2", sourceId = "sourceB")

        runTestWithIntent(intent, createScData(activeIssues = listOf(issue1, issue2))) { fragment ->
            val bannerGroup =
                fragment.findPreference<BannerMessagePreferenceGroup>(SAFETY_ISSUES_BANNER_KEY)
            assertThat(bannerGroup?.visiblePreferencesWhenCollapsedCount).isEqualTo(1)
            // issue2 should be moved to the top
            assertThat(bannerGroup?.getPreference(0)?.key).isEqualTo("active_${issue2.id}")
            assertThat(bannerGroup?.getPreference(1)?.key).isEqualTo("active_${issue1.id}")
            assertThat(bannerGroup?.findPreference<Preference>("active_${issue2.id}")?.isVisible)
                .isTrue()
            assertThat(bannerGroup?.findPreference<Preference>("active_${issue1.id}")?.isVisible)
                .isFalse()
        }
    }

    @Test
    fun focusedIssue_whenDiffersByUserHandle_focusesMatchingUser() {
        val issueId = "sharedIssueId"
        val sourceId = "sharedSourceId"
        val issue1 =
            createIssue(
                id = "issuePersonal",
                title = "Issue Personal",
                safetySourceIssueId = issueId,
                sourceIds = setOf(sourceId),
                userHandle = USER_PERSONAL,
                severity = SafetyCenterIssue.ISSUE_SEVERITY_LEVEL_CRITICAL_WARNING,
            )
        val issue2 =
            createIssue(
                id = "issueWork",
                title = "Issue Work",
                safetySourceIssueId = issueId,
                sourceIds = setOf(sourceId),
                userHandle = USER_WORK_PROFILE,
                severity = SafetyCenterIssue.ISSUE_SEVERITY_LEVEL_CRITICAL_WARNING,
            )
        val intent =
            createFocusedIntent(
                sourceIssueId = issueId,
                sourceId = sourceId,
                userHandle = USER_WORK_PROFILE,
            )

        runTestWithIntent(intent, createScData(activeIssues = listOf(issue1, issue2))) { fragment ->
            val bannerGroup =
                fragment.findPreference<BannerMessagePreferenceGroup>(SAFETY_ISSUES_BANNER_KEY)
            assertThat(bannerGroup?.visiblePreferencesWhenCollapsedCount).isEqualTo(1)
            // issue2 (Work) should be focused and on top
            assertThat(bannerGroup?.getPreference(0)?.key).isEqualTo("active_${issue2.id}")
            assertThat(bannerGroup?.getPreference(1)?.key).isEqualTo("active_${issue1.id}")
            assertThat(bannerGroup?.findPreference<Preference>("active_${issue2.id}")?.isVisible)
                .isTrue()
            assertThat(bannerGroup?.findPreference<Preference>("active_${issue1.id}")?.isVisible)
                .isFalse()
        }
    }

    @Test
    fun focusedIssue_whenDiffersBySourceId_focusesMatchingSourceId() {
        val sourceIssueId = "sharedSourceIssueId"
        val issue1 =
            createIssue(
                id = "issueSourceA",
                title = "Issue Source A",
                safetySourceIssueId = sourceIssueId,
                sourceIds = setOf("sourceA"),
                severity = SafetyCenterIssue.ISSUE_SEVERITY_LEVEL_CRITICAL_WARNING,
            )
        val issue2 =
            createIssue(
                id = "issueSourceB",
                title = "Issue Source B",
                safetySourceIssueId = sourceIssueId,
                sourceIds = setOf("sourceB"),
                severity = SafetyCenterIssue.ISSUE_SEVERITY_LEVEL_CRITICAL_WARNING,
            )
        val intent = createFocusedIntent(sourceIssueId = sourceIssueId, sourceId = "sourceB")

        runTestWithIntent(intent, createScData(activeIssues = listOf(issue1, issue2))) { fragment ->
            val bannerGroup =
                fragment.findPreference<BannerMessagePreferenceGroup>(SAFETY_ISSUES_BANNER_KEY)
            assertThat(bannerGroup?.visiblePreferencesWhenCollapsedCount).isEqualTo(1)
            // issue2 (Source B) should be focused and on top
            assertThat(bannerGroup?.getPreference(0)?.key).isEqualTo("active_${issue2.id}")
            assertThat(bannerGroup?.getPreference(1)?.key).isEqualTo("active_${issue1.id}")
            assertThat(bannerGroup?.findPreference<Preference>("active_${issue2.id}")?.isVisible)
                .isTrue()
            assertThat(bannerGroup?.findPreference<Preference>("active_${issue1.id}")?.isVisible)
                .isFalse()
        }
    }

    // Tests for safety source directly displayed on the main page

    @Test
    fun workPolicyInfo_whenEntryExists_isVisibleAndClickable() {
        val entry =
            createEntry(
                id = "workPolicyInfoEntry",
                title = "Your work policy info",
                userHandle = USER_PERSONAL,
                sourceId = ANDROID_WORK_POLICY_INFO_SOURCE_ID,
                summary = "Settings managed by your IT admin",
            )

        runTest(createScData(listOf(entry))) { fragment ->
            val preference =
                fragment.findPreference<SafetySourcePreference>(
                    ANDROID_WORK_POLICY_INFO_PREFERENCE_KEY
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
    @EnableFlags(Flags.FLAG_OPEN_SAFETY_CENTER_APIS)
    fun statusBanner_whenSeverityOk_showsOkStateAndRescanButton() {
        val status =
            SafetyCenterStatus.Builder("Title OK", "Summary OK")
                .setSeverityLevel(SafetyCenterStatus.OVERALL_SEVERITY_LEVEL_OK)
                .build()

        runTest(createScData(status = status)) { fragment ->
            val preference = fragment.findPreference<StatusBannerPreference>(STATUS_BANNER_KEY)
            assertThat(preference?.isVisible).isTrue()
            assertThat(preference?.title.toString()).isEqualTo("Title OK")
            assertThat(preference?.summary.toString()).isEqualTo("Summary OK")
            assertThat(preference?.iconLevel).isEqualTo(BannerStatus.LOW)

            onView(withText(R.string.safety_center_rescan_button)).check(matches(isDisplayed()))
            onView(withText(R.string.safety_center_rescan_button)).check(matches(isEnabled()))
            onView(withText(R.string.safety_center_review_settings)).check(doesNotExist())
        }
    }

    @Test
    fun statusBanner_whenSeverityOkAndRefreshing_showsOkStateAndDisabledButton() {
        val status =
            SafetyCenterStatus.Builder("Title", "Summary Refreshing")
                .setSeverityLevel(SafetyCenterStatus.OVERALL_SEVERITY_LEVEL_OK)
                .setRefreshStatus(SafetyCenterStatus.REFRESH_STATUS_FULL_RESCAN_IN_PROGRESS)
                .build()

        runTest(createScData(status = status)) { fragment ->
            val preference = fragment.findPreference<StatusBannerPreference>(STATUS_BANNER_KEY)
            assertThat(preference?.isVisible).isTrue()
            assertThat(preference?.title.toString()).isEqualTo("Title")
            assertThat(preference?.summary.toString()).isEqualTo("Summary Refreshing")
            assertThat(preference?.iconLevel).isEqualTo(BannerStatus.LOW)

            onView(withText(R.string.safety_center_rescan_button)).check(matches(isDisplayed()))
            onView(withText(R.string.safety_center_rescan_button)).check(matches(isNotEnabled()))
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_OPEN_SAFETY_CENTER_APIS)
    fun statusBanner_whenSeverityOkAndHasActiveIssues_showsOkStateAndNoButton() {
        val status =
            SafetyCenterStatus.Builder("Title OK", "Summary OK")
                .setSeverityLevel(SafetyCenterStatus.OVERALL_SEVERITY_LEVEL_OK)
                .build()
        val activeIssue = createIssue(id = "activeIssue", sourceIds = setOf("any"))

        runTest(createScData(status = status, activeIssues = listOf(activeIssue))) { fragment ->
            val preference = fragment.findPreference<StatusBannerPreference>(STATUS_BANNER_KEY)
            assertThat(preference?.isVisible).isTrue()
            assertThat(preference?.title.toString()).isEqualTo("Title OK")
            assertThat(preference?.summary.toString()).isEqualTo("Summary OK")
            assertThat(preference?.iconLevel).isEqualTo(BannerStatus.LOW)

            onView(withText(R.string.safety_center_rescan_button)).check(doesNotExist())
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_OPEN_SAFETY_CENTER_APIS)
    fun statusBanner_whenSeverityUnknown_showsOkStateAndRescanButton() {
        val status =
            SafetyCenterStatus.Builder("Title OK", "Summary OK")
                .setSeverityLevel(SafetyCenterStatus.OVERALL_SEVERITY_LEVEL_UNKNOWN)
                .build()

        runTest(createScData(status = status)) { fragment ->
            val preference = fragment.findPreference<StatusBannerPreference>(STATUS_BANNER_KEY)
            assertThat(preference?.isVisible).isTrue()
            assertThat(preference?.title.toString()).isEqualTo("Title OK")
            assertThat(preference?.summary.toString()).isEqualTo("Summary OK")
            assertThat(preference?.iconLevel).isEqualTo(BannerStatus.LOW)

            onView(withText(R.string.safety_center_rescan_button)).check(matches(isDisplayed()))
            onView(withText(R.string.safety_center_rescan_button)).check(matches(isEnabled()))
            onView(withText(R.string.safety_center_review_settings)).check(doesNotExist())
        }
    }

    @Test
    fun statusBanner_whenSeverityUnknownAndRefreshing_showsOkStateAndDisabledButton() {
        val status =
            SafetyCenterStatus.Builder("Title", "Summary Refreshing")
                .setSeverityLevel(SafetyCenterStatus.OVERALL_SEVERITY_LEVEL_UNKNOWN)
                .setRefreshStatus(SafetyCenterStatus.REFRESH_STATUS_FULL_RESCAN_IN_PROGRESS)
                .build()

        runTest(createScData(status = status)) { fragment ->
            val preference = fragment.findPreference<StatusBannerPreference>(STATUS_BANNER_KEY)
            assertThat(preference?.isVisible).isTrue()
            assertThat(preference?.title.toString()).isEqualTo("Title")
            assertThat(preference?.summary.toString()).isEqualTo("Summary Refreshing")
            assertThat(preference?.iconLevel).isEqualTo(BannerStatus.LOW)

            onView(withText(R.string.safety_center_rescan_button)).check(matches(isDisplayed()))
            onView(withText(R.string.safety_center_rescan_button)).check(matches(isNotEnabled()))
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_OPEN_SAFETY_CENTER_APIS)
    fun statusBanner_whenSeverityUnknownAndHasActiveIssues_showsOkStateAndNoButton() {
        val status =
            SafetyCenterStatus.Builder("Title Unknown", "Summary Unknown")
                .setSeverityLevel(SafetyCenterStatus.OVERALL_SEVERITY_LEVEL_UNKNOWN)
                .build()
        val activeIssue = createIssue(id = "activeIssue", sourceIds = setOf("any"))

        runTest(createScData(status = status, activeIssues = listOf(activeIssue))) { fragment ->
            val preference = fragment.findPreference<StatusBannerPreference>(STATUS_BANNER_KEY)
            assertThat(preference?.isVisible).isTrue()
            assertThat(preference?.title.toString()).isEqualTo("Title Unknown")
            assertThat(preference?.summary.toString()).isEqualTo("Summary Unknown")
            assertThat(preference?.iconLevel).isEqualTo(BannerStatus.LOW)

            onView(withText(R.string.safety_center_rescan_button)).check(doesNotExist())
        }
    }

    @Test
    fun statusBanner_whenSeverityRecommendation_showsRecommendationStateAndNoButton() {
        val status =
            SafetyCenterStatus.Builder("Title Rec", "Summary Rec")
                .setSeverityLevel(SafetyCenterStatus.OVERALL_SEVERITY_LEVEL_RECOMMENDATION)
                .build()

        runTest(createScData(status = status)) { fragment ->
            val preference = fragment.findPreference<StatusBannerPreference>(STATUS_BANNER_KEY)
            assertThat(preference?.isVisible).isTrue()
            assertThat(preference?.title.toString()).isEqualTo("Title Rec")
            assertThat(preference?.summary.toString()).isEqualTo("Summary Rec")
            assertThat(preference?.iconLevel).isEqualTo(BannerStatus.MEDIUM)

            onView(withText(R.string.safety_center_rescan_button)).check(doesNotExist())
        }
    }

    @Test
    fun statusBanner_whenSeverityRecommendationAndRefreshing_showsRecommendationStateAndNoButton() {
        val status =
            SafetyCenterStatus.Builder("Title", "Summary Refreshing")
                .setSeverityLevel(SafetyCenterStatus.OVERALL_SEVERITY_LEVEL_RECOMMENDATION)
                .setRefreshStatus(SafetyCenterStatus.REFRESH_STATUS_DATA_FETCH_IN_PROGRESS)
                .build()

        runTest(createScData(status = status)) { fragment ->
            val preference = fragment.findPreference<StatusBannerPreference>(STATUS_BANNER_KEY)
            assertThat(preference?.isVisible).isTrue()
            assertThat(preference?.title.toString()).isEqualTo("Title")
            assertThat(preference?.summary.toString()).isEqualTo("Summary Refreshing")
            assertThat(preference?.iconLevel).isEqualTo(BannerStatus.MEDIUM)

            onView(withText(R.string.safety_center_rescan_button)).check(doesNotExist())
        }
    }

    @Test
    fun statusBanner_whenSeverityCriticalWarning_showsCriticalStateAndNoButton() {
        val status =
            SafetyCenterStatus.Builder("Title Warn", "Summary Warn")
                .setSeverityLevel(SafetyCenterStatus.OVERALL_SEVERITY_LEVEL_CRITICAL_WARNING)
                .build()

        runTest(createScData(status = status)) { fragment ->
            val preference = fragment.findPreference<StatusBannerPreference>(STATUS_BANNER_KEY)
            assertThat(preference?.isVisible).isTrue()
            assertThat(preference?.title.toString()).isEqualTo("Title Warn")
            assertThat(preference?.summary.toString()).isEqualTo("Summary Warn")
            assertThat(preference?.iconLevel).isEqualTo(BannerStatus.HIGH)

            onView(withText(R.string.safety_center_rescan_button)).check(doesNotExist())
        }
    }

    @Test
    fun statusBanner_whenSeverityCriticalWarningAndRefreshing_showsCriticalStateAndNoButton() {
        val status =
            SafetyCenterStatus.Builder("Title", "Summary Refreshing")
                .setSeverityLevel(SafetyCenterStatus.OVERALL_SEVERITY_LEVEL_CRITICAL_WARNING)
                .setRefreshStatus(SafetyCenterStatus.REFRESH_STATUS_FULL_RESCAN_IN_PROGRESS)
                .build()

        runTest(createScData(status = status)) { fragment ->
            val preference = fragment.findPreference<StatusBannerPreference>(STATUS_BANNER_KEY)
            assertThat(preference?.isVisible).isTrue()
            assertThat(preference?.title.toString()).isEqualTo("Title")
            assertThat(preference?.summary.toString()).isEqualTo("Summary Refreshing")
            assertThat(preference?.iconLevel).isEqualTo(BannerStatus.HIGH)

            onView(withText(R.string.safety_center_rescan_button)).check(doesNotExist())
        }
    }

    @Test
    fun statusBanner_clickRescanButton_disablesButton() {
        val initialStatus =
            SafetyCenterStatus.Builder("Title OK", "Summary OK")
                .setSeverityLevel(SafetyCenterStatus.OVERALL_SEVERITY_LEVEL_OK)
                .build()

        runTest(createScData(status = initialStatus)) { _ ->
            onView(withText(R.string.safety_center_rescan_button)).perform(click())
            ShadowLooper.idleMainLooper()

            onView(withText(R.string.safety_center_rescan_button)).check(matches(isDisplayed()))
            onView(withText(R.string.safety_center_rescan_button)).check(matches(isNotEnabled()))

            val refreshingStatus =
                SafetyCenterStatus.Builder("Title", "Summary Refreshing")
                    .setSeverityLevel(SafetyCenterStatus.OVERALL_SEVERITY_LEVEL_OK)
                    .setRefreshStatus(SafetyCenterStatus.REFRESH_STATUS_FULL_RESCAN_IN_PROGRESS)
                    .build()
            shadowSafetyCenterManager.setSafetyCenterData(createScData(status = refreshingStatus))
            ShadowLooper.idleMainLooper()

            onView(withText(R.string.safety_center_rescan_button)).check(matches(isDisplayed()))
            onView(withText(R.string.safety_center_rescan_button)).check(matches(isNotEnabled()))
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_OPEN_SAFETY_CENTER_APIS)
    fun launchWithSafetyCenterAction_withGroupId_startsSubSettingsWithFragment() {
        shadowSafetyCenterManager.setSafetyCenterData(EMPTY_SC_DATA)
        val testGroupId =
            mApplication.getString(R.string.config_safety_center_lock_screen_subpage_id)
        val startIntent =
            Intent(Intent.ACTION_SAFETY_CENTER).apply {
                putExtra(SafetyCenterManager.EXTRA_SAFETY_SOURCES_GROUP_ID, testGroupId)
                setClass(
                    ApplicationProvider.getApplicationContext(),
                    SafetyCenterActivity::class.java,
                )
            }

        ActivityScenario.launch<SafetyCenterActivity>(startIntent).use {
            val nextIntent = shadowOf(mApplication).nextStartedActivity

            assertThat(nextIntent).isNotNull()
            assertThat(nextIntent.component?.className).isEqualTo(SubSettings::class.java.name)
            val extras = nextIntent.extras
            assertThat(extras).isNotNull()
            val expectedFragmentClass =
                SafetyCenterSubpageRegistry.getSubpageFragmentClassNameFor(
                    mApplication,
                    testGroupId,
                )
            assertThat(extras?.getString(EXTRA_SHOW_FRAGMENT)).isEqualTo(expectedFragmentClass)
            return
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_OPEN_SAFETY_CENTER_APIS)
    fun launchWithPrivacyControlsAction_startsSubSettingsWithFragment() {
        shadowSafetyCenterManager.setSafetyCenterData(EMPTY_SC_DATA)
        val startIntent =
            Intent(Settings.ACTION_PRIVACY_CONTROLS).apply {
                setClass(
                    ApplicationProvider.getApplicationContext(),
                    SafetyCenterActivity::class.java,
                )
            }

        ActivityScenario.launch<SafetyCenterActivity>(startIntent).use {
            val nextIntent = shadowOf(mApplication).nextStartedActivity

            assertThat(nextIntent).isNotNull()
            assertThat(nextIntent.component?.className).isEqualTo(SubSettings::class.java.name)
            val extras = nextIntent.extras
            assertThat(extras).isNotNull()
            assertThat(extras?.getString(EXTRA_SHOW_FRAGMENT))
                .isEqualTo(PrivacyControlsFragment::class.qualifiedName)
            return
        }
    }

    // --- Tests for InteractionLogger ---

    @Test
    fun interactionLogger_onPageCreation_logsSafetyCenterViewed() {
        runTest(EMPTY_SC_DATA) {
            val events = SafetyCenterTestUtils.ShadowSettingsStatsLog.getWrittenEvents()
            assertThat(events).hasSize(1)
            val event = events[0]

            assertThat(event.atomId).isEqualTo(SettingsStatsLog.SAFETY_CENTER_INTERACTION_REPORTED)
            assertThat(event.sessionId).isEqualTo(TEST_SESSION_ID)
            assertThat(event.action).isEqualTo(Action.SAFETY_CENTER_VIEWED.statsLogValue)
            assertThat(event.viewType).isEqualTo(ViewType.FULL.statsLogValue)
            assertThat(event.navigationSource).isEqualTo(NavigationSource.SETTINGS.statsLogValue)
            assertThat(event.severityLevel).isEqualTo(LogSeverityLevel.UNKNOWN.statsLogValue)
            assertThat(event.sourceId).isEqualTo(0)
            assertThat(event.sourceProfileType)
                .isEqualTo(SafetySourceProfileType.UNKNOWN.statsLogValue)
            assertThat(event.issueTypeId).isEqualTo(0)
            assertThat(event.subpageId).isEqualTo(0)
            assertThat(event.issueState)
                .isEqualTo(
                    SettingsStatsLog
                        .SAFETY_CENTER_INTERACTION_REPORTED__ISSUE_STATE__ISSUE_STATE_UNKNOWN
                )
        }
    }

    @Test
    fun interactionLogger_onLaunchWithQuickSettings_logsViewTypeAndNavigationSource() {
        runTest(
            EMPTY_SC_DATA,
            isQuickSettings = true,
            navigationSource = NavigationSource.QUICK_SETTINGS_TILE,
        ) {
            val events = SafetyCenterTestUtils.ShadowSettingsStatsLog.getWrittenEvents()
            assertThat(events).hasSize(1)
            val event = events[0]

            assertThat(event.atomId).isEqualTo(SettingsStatsLog.SAFETY_CENTER_INTERACTION_REPORTED)
            assertThat(event.sessionId).isEqualTo(TEST_SESSION_ID)
            assertThat(event.action).isEqualTo(Action.SAFETY_CENTER_VIEWED.statsLogValue)
            assertThat(event.viewType).isEqualTo(ViewType.QUICK_SETTINGS.statsLogValue)
            assertThat(event.navigationSource)
                .isEqualTo(NavigationSource.QUICK_SETTINGS_TILE.statsLogValue)
        }
    }

    @Test
    fun interactionLogger_onLaunchWithIntent_usesSessionIdFromIntent() {
        val intent =
            Intent(mApplication, SafetyCenterActivity::class.java).apply {
                putExtra(EXTRA_SESSION_ID, TEST_SESSION_ID)
            }

        runTestWithIntent(intent, EMPTY_SC_DATA) {
            val events = SafetyCenterTestUtils.ShadowSettingsStatsLog.getWrittenEvents()
            assertThat(events).hasSize(1)
            val event = events[0]

            assertThat(event.atomId).isEqualTo(SettingsStatsLog.SAFETY_CENTER_INTERACTION_REPORTED)
            assertThat(event.sessionId).isEqualTo(TEST_SESSION_ID)
            assertThat(event.action).isEqualTo(Action.SAFETY_CENTER_VIEWED.statsLogValue)
            assertThat(event.viewType).isEqualTo(ViewType.FULL.statsLogValue)
            assertThat(event.navigationSource).isEqualTo(NavigationSource.UNKNOWN.statsLogValue)
            assertThat(event.severityLevel).isEqualTo(LogSeverityLevel.UNKNOWN.statsLogValue)
            assertThat(event.sourceId).isEqualTo(0)
            assertThat(event.sourceProfileType)
                .isEqualTo(SafetySourceProfileType.UNKNOWN.statsLogValue)
            assertThat(event.issueTypeId).isEqualTo(0)
            assertThat(event.subpageId).isEqualTo(0)
            assertThat(event.issueState)
                .isEqualTo(
                    SettingsStatsLog
                        .SAFETY_CENTER_INTERACTION_REPORTED__ISSUE_STATE__ISSUE_STATE_UNKNOWN
                )
        }
    }

    @Test
    fun interactionLogger_onLaunchWithNotificationIntent_logsNavigationSourceNotification() {
        val intent =
            createFocusedIntent(sourceIssueId = "issueId", sourceId = "sourceId").apply {
                putExtra(EXTRA_SESSION_ID, TEST_SESSION_ID)
            }
        val issue =
            createIssue(
                id = "issue1",
                safetySourceIssueId = "issueId",
                sourceIds = setOf("sourceId"),
            )

        runTestWithIntent(intent, createScData(activeIssues = listOf(issue))) {
            val events = SafetyCenterTestUtils.ShadowSettingsStatsLog.getWrittenEvents()
            assertThat(events).hasSize(2) // SAFETY_CENTER_VIEWED and SAFETY_ISSUE_VIEWED
            val event = events.find { it.action == Action.SAFETY_CENTER_VIEWED.statsLogValue }!!

            assertThat(event.navigationSource)
                .isEqualTo(NavigationSource.NOTIFICATION.statsLogValue)
        }
    }

    @Test
    fun interactionLogger_onLaunchWithSearchIntent_logsNavigationSourceSettings() {
        val intent =
            Intent(mApplication, SafetyCenterActivity::class.java).apply {
                putExtra(EXTRA_SESSION_ID, TEST_SESSION_ID)
                putExtra(
                    NavigationSource.EXTRA_SETTINGS_FRAGMENT_ARGS_KEY,
                    "some_search_result_key",
                )
            }

        runTestWithIntent(intent, EMPTY_SC_DATA) {
            val events = SafetyCenterTestUtils.ShadowSettingsStatsLog.getWrittenEvents()
            assertThat(events).hasSize(1)
            val event = events[0]

            assertThat(event.navigationSource).isEqualTo(NavigationSource.SETTINGS.statsLogValue)
        }
    }

    @Test
    fun interactionLogger_onLaunchWithQuickSettingsIntent_logsNavigationSourceQuickSettingsTile() {
        val intent =
            Intent(mApplication, SafetyCenterActivity::class.java)
                .setAction(Intent.ACTION_SAFETY_CENTER)
        NavigationSource.QUICK_SETTINGS_TILE.addToIntent(intent)

        runTestWithIntent(intent, EMPTY_SC_DATA) {
            val events = SafetyCenterTestUtils.ShadowSettingsStatsLog.getWrittenEvents()
            assertThat(events).hasSize(1)
            val event = events[0]

            assertThat(event.navigationSource)
                .isEqualTo(NavigationSource.QUICK_SETTINGS_TILE.statsLogValue)
        }
    }

    @Test
    fun sessionId_onConfigurationChange_isPreserved() {
        val intent =
            Intent(mApplication, SafetyCenterActivity::class.java).apply {
                putExtra(EXTRA_SESSION_ID, TEST_SESSION_ID)
            }
        shadowSafetyCenterManager.setSafetyCenterData(EMPTY_SC_DATA)

        ActivityScenario.launch<SafetyCenterActivity>(intent).use { scenario ->
            // Initial launch
            scenario.onActivity {
                val events = SafetyCenterTestUtils.ShadowSettingsStatsLog.getWrittenEvents()
                assertThat(events).hasSize(1)
                assertThat(events[0].atomId)
                    .isEqualTo(SettingsStatsLog.SAFETY_CENTER_INTERACTION_REPORTED)
                assertThat(events[0].sessionId).isEqualTo(TEST_SESSION_ID)
                assertThat(events[0].action).isEqualTo(Action.SAFETY_CENTER_VIEWED.statsLogValue)
            }

            // Trigger configuration change (e.g., rotation)
            scenario.recreate()
            ShadowLooper.idleMainLooper()

            // After recreation
            scenario.onActivity {
                val events = SafetyCenterTestUtils.ShadowSettingsStatsLog.getWrittenEvents()
                // Should have a new view event, but session ID should be the same
                assertThat(events).hasSize(2)
                assertThat(events[0].sessionId).isEqualTo(TEST_SESSION_ID)
                assertThat(events[1].sessionId).isEqualTo(TEST_SESSION_ID)
                assertThat(events[1].action).isEqualTo(Action.SAFETY_CENTER_VIEWED.statsLogValue)
            }
        }
    }

    @Test
    fun interactionLogger_onIssueViewed_logsIssueViewed() {
        val activeIssue = createIssue(id = "activeIssue", sourceIds = setOf("any"))
        runTest(createScData(activeIssues = listOf(activeIssue))) {
            ShadowLooper.idleMainLooper()
            val events = SafetyCenterTestUtils.ShadowSettingsStatsLog.getWrittenEvents()
            assertThat(events).hasSize(2)
            val issueViewedEvent =
                events.find { it.action == Action.SAFETY_ISSUE_VIEWED.statsLogValue }!!

            assertThat(issueViewedEvent.atomId)
                .isEqualTo(SettingsStatsLog.SAFETY_CENTER_INTERACTION_REPORTED)
            assertThat(issueViewedEvent.sessionId).isEqualTo(TEST_SESSION_ID)
            assertThat(issueViewedEvent.action).isEqualTo(Action.SAFETY_ISSUE_VIEWED.statsLogValue)
            assertThat(issueViewedEvent.viewType).isEqualTo(ViewType.FULL.statsLogValue)
            assertThat(issueViewedEvent.navigationSource)
                .isEqualTo(NavigationSource.SETTINGS.statsLogValue)
            assertThat(issueViewedEvent.sourceId)
                .isEqualTo(InteractionLogger.encodeStringId(activeIssue.safetySourceIds.first()))
            assertThat(issueViewedEvent.issueTypeId)
                .isEqualTo(InteractionLogger.encodeStringId(activeIssue.issueTypeId))
            assertThat(issueViewedEvent.severityLevel)
                .isEqualTo(LogSeverityLevel.RECOMMENDATION.statsLogValue)
            assertThat(issueViewedEvent.sourceProfileType)
                .isEqualTo(SafetySourceProfileType.PERSONAL.statsLogValue)
            assertThat(issueViewedEvent.subpageId).isEqualTo(0)
            assertThat(issueViewedEvent.issueState)
                .isEqualTo(SettingsStatsLog.SAFETY_CENTER_INTERACTION_REPORTED__ISSUE_STATE__ACTIVE)
        }
    }

    @Test
    fun interactionLogger_onRescanClick_logsScanInitiated() {
        val status =
            SafetyCenterStatus.Builder("Title OK", "Summary OK")
                .setSeverityLevel(SafetyCenterStatus.OVERALL_SEVERITY_LEVEL_OK)
                .build()

        runTest(createScData(status = status)) {
            onView(withText(R.string.safety_center_rescan_button)).perform(click())
            ShadowLooper.idleMainLooper()

            val events = SafetyCenterTestUtils.ShadowSettingsStatsLog.getWrittenEvents()
            // Events: SAFETY_CENTER_VIEWED, SCAN_INITIATED
            assertThat(events).hasSize(2)
            val event = events[1]

            assertThat(event.atomId).isEqualTo(SettingsStatsLog.SAFETY_CENTER_INTERACTION_REPORTED)
            assertThat(event.sessionId).isEqualTo(TEST_SESSION_ID)
            assertThat(event.action).isEqualTo(Action.SCAN_INITIATED.statsLogValue)
            assertThat(event.viewType).isEqualTo(ViewType.FULL.statsLogValue)
            assertThat(event.navigationSource).isEqualTo(NavigationSource.SETTINGS.statsLogValue)
            assertThat(event.severityLevel).isEqualTo(LogSeverityLevel.UNKNOWN.statsLogValue)
            assertThat(event.sourceId).isEqualTo(0)
            assertThat(event.sourceProfileType)
                .isEqualTo(SafetySourceProfileType.UNKNOWN.statsLogValue)
            assertThat(event.issueTypeId).isEqualTo(0)
            assertThat(event.subpageId).isEqualTo(0)
            assertThat(event.issueState)
                .isEqualTo(
                    SettingsStatsLog
                        .SAFETY_CENTER_INTERACTION_REPORTED__ISSUE_STATE__ISSUE_STATE_UNKNOWN
                )
        }
    }

    companion object {
        private const val ARG_IS_QUICK_SETTINGS = "is_quick_settings"
        private const val DEVICE_UNLOCK_KEY = "device_unlock_subpage"
        private const val STATUS_BANNER_KEY = "safety_center_status_banner"
        private const val PRIVACY_CONTROLS_SUBPAGE_KEY = "privacy_controls_page"
        private const val ANDROID_LOCK_SCREEN_SOURCE_ID = "AndroidLockScreen"
        private const val ANDROID_FACE_UNLOCK_SOURCE_ID = "AndroidFaceUnlock"
        private const val ANDROID_FINGERPRINT_UNLOCK_SOURCE_ID = "AndroidFingerprintUnlock"
        private const val ANDROID_HEALTH_CONNECT_SOURCE_ID = "AndroidHealthConnect"
        private const val ANDROID_WORK_POLICY_INFO_SOURCE_ID = "AndroidWorkPolicyInfo"
        private const val ANDROID_A11Y_SOURCES_ID = "AndroidAccessibility"
        private const val SAFETY_ISSUES_BANNER_KEY = "issues_banner_group"
        private const val ANDROID_WORK_POLICY_INFO_PREFERENCE_KEY = "work_policy_info"
        private val DEFAULT_DEVICE_UNLOCK_SUMMARY_RES = R.string.safety_center_device_unlock_summary
        private val DEFAULT_PRIVACY_CONTROLS_SUMMARY_RES = R.string.privacy_sources_summary
    }
}
