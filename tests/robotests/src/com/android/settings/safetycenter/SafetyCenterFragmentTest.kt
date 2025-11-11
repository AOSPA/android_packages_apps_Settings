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
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.swipeUp
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.Settings
import com.android.settings.safetycenter.SafetyCenterTestUtils.EMPTY_SC_DATA
import com.android.settings.safetycenter.SafetyCenterTestUtils.TEST_ACTION
import com.android.settings.safetycenter.SafetyCenterTestUtils.USER_PERSONAL
import com.android.settings.safetycenter.SafetyCenterTestUtils.USER_WORK_PROFILE
import com.android.settings.safetycenter.SafetyCenterTestUtils.createEntry
import com.android.settings.safetycenter.SafetyCenterTestUtils.createFocusedIntent
import com.android.settings.safetycenter.SafetyCenterTestUtils.createIssue
import com.android.settings.safetycenter.SafetyCenterTestUtils.createIssueAction
import com.android.settings.safetycenter.SafetyCenterTestUtils.createScData
import com.android.settings.safetycenter.ui.SafetyCenterFragment
import com.android.settingslib.safetycenter.SafetySourcePreference
import com.android.settingslib.widget.BannerMessagePreference
import com.android.settingslib.widget.BannerMessagePreferenceGroup
import com.android.settingslib.widget.preference.banner.R as BannerR
import com.android.settingslib.widget.preference.button.R as ButtonR
import com.android.settingslib.widget.preference.statusbanner.R as StatusBannerR
import com.google.common.truth.Truth.assertThat
import org.hamcrest.Matchers.allOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
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
        shadowSafetyCenterManager.setSafetyCenterData(data)
        val scenario =
            launchFragmentInContainer<SafetyCenterFragment>(themeResId = R.style.Theme_SubSettings)
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
        ActivityScenario.launch<Settings.SafetyCenterActivity>(intent).use { scenario ->
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

    // Tests for Device Unlock preference summary and icon in Safety Center main page

    @Test
    @EnableFlags(Flags.FLAG_OPEN_SAFETY_CENTER_APIS)
    fun deviceUnlockPref_whenNoData_subpageHidden() {
        runTest(EMPTY_SC_DATA) { fragment ->
            val preference = fragment.findPreference<Preference>(DEVICE_UNLOCK_KEY)
            assertThat(preference?.isVisible).isFalse()
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
            assertThat(preference?.isVisible).isTrue()
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
            assertThat(preference?.isVisible).isTrue()
            assertThat(preference?.summary.toString())
                .isEqualTo(mApplication.getString(R.string.safety_center_refresh_error))
            assertThat(preference?.icon).isNull()
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
        intent.setClass(mApplication, Settings.SafetyCenterActivity::class.java)

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

    companion object {
        private const val DEVICE_UNLOCK_KEY = "device_unlock_subpage"
        private const val PRIVACY_CONTROLS_SUBPAGE_KEY = "privacy_controls_page"
        private const val ANDROID_LOCK_SCREEN_SOURCE_ID = "AndroidLockScreen"
        private const val ANDROID_HEALTH_CONNECT_SOURCE_ID = "AndroidHealthConnect"
        private const val ANDROID_WORK_POLICY_INFO_SOURCE_ID = "AndroidWorkPolicyInfo"
        private const val ANDROID_A11Y_SOURCES_ID = "AndroidAccessibility"
        private const val SAFETY_ISSUES_BANNER_KEY = "issues_banner_group"
        private const val ANDROID_WORK_POLICY_INFO_PREFERENCE_KEY = "work_policy_info"
        private val DEFAULT_DEVICE_UNLOCK_SUMMARY_RES = R.string.safety_center_device_unlock_summary
        private val DEFAULT_PRIVACY_CONTROLS_SUMMARY_RES = R.string.privacy_sources_summary
    }
}
