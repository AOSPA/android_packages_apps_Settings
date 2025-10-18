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
import android.content.pm.UserInfo
import android.os.UserManager
import android.permission.flags.Flags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.safetycenter.SafetyCenterData
import android.safetycenter.SafetyCenterIssue
import android.safetycenter.SafetyCenterManager
import androidx.fragment.app.testing.launchFragmentInContainer
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
import com.android.settings.safetycenter.SafetyCenterTestUtils.EMPTY_SC_DATA
import com.android.settings.safetycenter.SafetyCenterTestUtils.TEST_ACTION
import com.android.settings.safetycenter.SafetyCenterTestUtils.USER_PERSONAL
import com.android.settings.safetycenter.SafetyCenterTestUtils.USER_WORK_PROFILE
import com.android.settings.safetycenter.SafetyCenterTestUtils.createEntry
import com.android.settings.safetycenter.SafetyCenterTestUtils.createIssue
import com.android.settings.safetycenter.SafetyCenterTestUtils.createIssueAction
import com.android.settings.safetycenter.SafetyCenterTestUtils.createScData
import com.android.settings.safetycenter.ui.DeviceUnlockSubPageFragment
import com.android.settingslib.safetycenter.SafetySourcePreference
import com.android.settingslib.widget.BannerMessagePreference
import com.android.settingslib.widget.BannerMessagePreferenceGroup
import com.android.settingslib.widget.IllustrationPreference
import com.android.settingslib.widget.preference.button.R as ButtonR
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowDialog
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.ShadowSafetyCenterManager
import org.robolectric.shadows.ShadowUserManager

// Suppressing MissingPermission lint: The Settings app holds the MANAGE_SAFETY_CENTER permission,
// which is required by the SafetyCenterManager APIs.
@SuppressLint("MissingPermission")
@RunWith(AndroidJUnit4::class)
@EnableFlags(Flags.FLAG_OPEN_SAFETY_CENTER_APIS)
class DeviceUnlockSubPageFragmentTest {
    @get:Rule val setFlagsRule = SetFlagsRule()
    private lateinit var application: Application
    private lateinit var shadowSafetyCenterManager: ShadowSafetyCenterManager

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        val userManager = application.getSystemService(UserManager::class.java)!!
        val shadowUserManager = Shadow.extract(userManager) as ShadowUserManager
        val safetyCenterManager = application.getSystemService(SafetyCenterManager::class.java)!!
        shadowSafetyCenterManager = Shadow.extract(safetyCenterManager)

        shadowUserManager.addUser(USER_PERSONAL.identifier, "Personal", 0)
        shadowUserManager.addProfile(
            USER_PERSONAL.identifier,
            USER_WORK_PROFILE.identifier,
            "Work Profile",
            UserInfo.FLAG_MANAGED_PROFILE,
        )
        shadowSafetyCenterManager.setSafetyCenterEnabled(true)
    }

    private fun runTest(data: SafetyCenterData, testBlock: (DeviceUnlockSubPageFragment) -> Unit) {
        shadowSafetyCenterManager.setSafetyCenterData(data)
        val scenario =
            launchFragmentInContainer<DeviceUnlockSubPageFragment>(
                themeResId = R.style.Theme_SubSettings
            )
        scenario.onFragment { fragment ->
            ShadowLooper.idleMainLooper()
            testBlock(fragment)
        }
        scenario.close()
    }

    @Test
    fun illustrationPreference_always_isVisible() {
        runTest(EMPTY_SC_DATA) { fragment ->
            val illustration =
                fragment.findPreference<IllustrationPreference>(DEVICE_UNLOCK_ILLUSTRATION_KEY)
            assertThat(illustration?.isVisible).isTrue()
        }
    }

    @Test
    fun lockScreenPref_whenEntryExists_isVisibleAndClickable() {
        val entry =
            createEntry(
                id = "lockScreenEntry",
                title = "Screen Lock",
                userHandle = USER_PERSONAL,
                sourceId = ANDROID_LOCK_SCREEN_SOURCE_ID,
                summary = "set screen lock",
            )

        runTest(createScData(listOf(entry))) { fragment ->
            val preference =
                fragment.findPreference<SafetySourcePreference>(ANDROID_LOCK_SCREEN_PREFERENCE_KEY)
            assertThat(preference?.isVisible).isTrue()
            assertThat(preference?.title.toString()).isEqualTo(entry.title)
            assertThat(preference?.summary.toString()).isEqualTo(entry.summary)

            preference?.performClick()
            ShadowLooper.idleMainLooper()
            val startedIntent = shadowOf(application).nextStartedActivity
            assertThat(startedIntent).isNotNull()
            assertThat(startedIntent.action).isEqualTo(TEST_ACTION)
        }
    }

    @Test
    fun lockScreenPref_whenNoEntry_isHidden() {
        runTest(EMPTY_SC_DATA) { fragment ->
            val preference =
                fragment.findPreference<SafetySourcePreference>(ANDROID_LOCK_SCREEN_PREFERENCE_KEY)
            assertThat(preference?.isVisible).isFalse()
        }
    }

    @Test
    fun lockScreenPref_whenDataChanges_uiIsUpdated() {
        runTest(EMPTY_SC_DATA) { fragment ->
            val preference =
                fragment.findPreference<SafetySourcePreference>(ANDROID_LOCK_SCREEN_PREFERENCE_KEY)
            // Initial state: No lock screen entry
            assertThat(preference?.isVisible).isFalse()

            // Data change: Add lock screen entry
            val newEntry =
                createEntry(
                    id = "lockScreenEntry",
                    title = "Screen Lock",
                    userHandle = USER_PERSONAL,
                    sourceId = ANDROID_LOCK_SCREEN_SOURCE_ID,
                    summary = "set screen lock",
                )
            shadowSafetyCenterManager.setSafetyCenterData(createScData(listOf(newEntry)))
            ShadowLooper.idleMainLooper()

            // UI should update to reflect the new state
            assertThat(preference?.isVisible).isTrue()
            assertThat(preference?.title.toString()).isEqualTo(newEntry.title)
            assertThat(preference?.summary.toString()).isEqualTo(newEntry.summary)
        }
    }

    @Test
    fun fingerprintPersonalPref_whenEntryExists_isVisibleAndClickable() {
        val entry =
            createEntry(
                id = "fingerprint_personal",
                title = "Fingerprint",
                userHandle = USER_PERSONAL,
                sourceId = ANDROID_FINGERPRINT_UNLOCK_SOURCE_ID,
                summary = "2 fingerprints added",
            )

        runTest(createScData(listOf(entry))) { fragment ->
            val preference =
                fragment.findPreference<SafetySourcePreference>(
                    FINGERPRINT_UNLOCK_PERSONAL_PREFERENCE_KEY
                )
            assertThat(preference?.isVisible).isTrue()
            assertThat(preference?.title.toString()).isEqualTo(entry.title)
            assertThat(preference?.summary.toString()).isEqualTo(entry.summary)

            preference?.performClick()
            ShadowLooper.idleMainLooper()
            val startedIntent = shadowOf(application).nextStartedActivity
            assertThat(startedIntent).isNotNull()
            assertThat(startedIntent.action).isEqualTo(TEST_ACTION)
        }
    }

    @Test
    fun fingerprintPersonalPref_whenNoEntry_isHidden() {
        runTest(EMPTY_SC_DATA) { fragment ->
            val preference =
                fragment.findPreference<SafetySourcePreference>(
                    FINGERPRINT_UNLOCK_PERSONAL_PREFERENCE_KEY
                )
            assertThat(preference?.isVisible).isFalse()
        }
    }

    @Test
    fun fingerprintWorkPref_whenWorkProfileAndEntryExists_isVisibleAndClickable() {
        val entry =
            createEntry(
                id = "fingerprint_work",
                title = "Fingerprint For work",
                userHandle = USER_WORK_PROFILE,
                sourceId = ANDROID_FINGERPRINT_UNLOCK_SOURCE_ID,
                summary = "Work fingerprint",
            )

        runTest(createScData(listOf(entry))) { fragment ->
            val preference =
                fragment.findPreference<SafetySourcePreference>(
                    FINGERPRINT_UNLOCK_WORK_PREFERENCE_KEY
                )
            assertThat(preference?.isVisible).isTrue()
            assertThat(preference?.title.toString()).isEqualTo(entry.title)
            assertThat(preference?.summary.toString()).isEqualTo(entry.summary)

            preference?.performClick()
            ShadowLooper.idleMainLooper()
            val startedIntent = shadowOf(application).nextStartedActivity
            assertThat(startedIntent).isNotNull()
            assertThat(startedIntent.action).isEqualTo(TEST_ACTION)
        }
    }

    @Test
    fun fingerprintWorkPref_whenNoEntry_isHidden() {
        runTest(EMPTY_SC_DATA) { fragment ->
            val preference =
                fragment.findPreference<SafetySourcePreference>(
                    FINGERPRINT_UNLOCK_WORK_PREFERENCE_KEY
                )
            assertThat(preference?.isVisible).isFalse()
        }
    }

    @Test
    fun fingerprintWorkPref_whenNoWorkProfileEntry_isHidden() {
        val entry =
            createEntry(
                id = "fingerprint_personal",
                title = "Fingerprint",
                userHandle = USER_PERSONAL,
                sourceId = ANDROID_FINGERPRINT_UNLOCK_SOURCE_ID,
                summary = "2 fingerprints added",
            )

        runTest(createScData(listOf(entry))) { fragment ->
            val preference =
                fragment.findPreference<SafetySourcePreference>(
                    FINGERPRINT_UNLOCK_WORK_PREFERENCE_KEY
                )
            assertThat(preference?.isVisible).isFalse()
        }
    }

    @Test
    fun faceUnlockPersonalPref_whenEntryExists_isVisibleAndClickable() {
        val entry =
            createEntry(
                id = "face_personal",
                title = "Face Unlock",
                userHandle = USER_PERSONAL,
                sourceId = ANDROID_FACE_UNLOCK_SOURCE_ID,
                summary = "face added",
            )
        runTest(createScData(listOf(entry))) { fragment ->
            val preference =
                fragment.findPreference<SafetySourcePreference>(FACE_UNLOCK_PERSONAL_PREFERENCE_KEY)
            assertThat(preference?.isVisible).isTrue()
            assertThat(preference?.title.toString()).isEqualTo(entry.title)
            assertThat(preference?.summary.toString()).isEqualTo(entry.summary)

            preference?.performClick()
            ShadowLooper.idleMainLooper()
            val startedIntent = shadowOf(application).nextStartedActivity
            assertThat(startedIntent).isNotNull()
            assertThat(startedIntent.action).isEqualTo(TEST_ACTION)
        }
    }

    @Test
    fun faceUnlockPersonalPref_whenNoEntry_isHidden() {
        runTest(EMPTY_SC_DATA) { fragment ->
            val preference =
                fragment.findPreference<SafetySourcePreference>(FACE_UNLOCK_PERSONAL_PREFERENCE_KEY)
            assertThat(preference?.isVisible).isFalse()
        }
    }

    @Test
    fun faceUnlockWorkPref_whenWorkProfileAndEntryExists_isVisibleAndClickable() {
        val entry =
            createEntry(
                id = "face_work",
                title = "Face Unlock for Work",
                userHandle = USER_WORK_PROFILE,
                sourceId = ANDROID_FACE_UNLOCK_SOURCE_ID,
                summary = "face added",
            )

        runTest(createScData(listOf(entry))) { fragment ->
            val preference =
                fragment.findPreference<SafetySourcePreference>(FACE_UNLOCK_WORK_PREFERENCE_KEY)
            assertThat(preference?.isVisible).isTrue()
            assertThat(preference?.title.toString()).isEqualTo(entry.title)
            assertThat(preference?.summary.toString()).isEqualTo(entry.summary)

            preference?.performClick()
            ShadowLooper.idleMainLooper()
            val startedIntent = shadowOf(application).nextStartedActivity
            assertThat(startedIntent).isNotNull()
            assertThat(startedIntent.action).isEqualTo(TEST_ACTION)
        }
    }

    @Test
    fun faceUnlockWorkPref_whenNoEntry_isHidden() {
        runTest(EMPTY_SC_DATA) { fragment ->
            val preference =
                fragment.findPreference<SafetySourcePreference>(FACE_UNLOCK_WORK_PREFERENCE_KEY)
            assertThat(preference?.isVisible).isFalse()
        }
    }

    @Test
    fun faceUnlockWorkPref_whenNoWorkProfileEntry_isHidden() {
        val entry =
            createEntry(
                id = "face_personal",
                title = "Face Unlock",
                userHandle = USER_PERSONAL,
                sourceId = ANDROID_FACE_UNLOCK_SOURCE_ID,
                summary = "face added",
            )

        runTest(createScData(listOf(entry))) { fragment ->
            val preference =
                fragment.findPreference<SafetySourcePreference>(FACE_UNLOCK_WORK_PREFERENCE_KEY)
            assertThat(preference?.isVisible).isFalse()
        }
    }

    @Test
    fun wearUnlockPersonalPref_whenEntryExists_isVisibleAndClickable() {
        val entry =
            createEntry(
                id = "wear_personal",
                title = "Wear Unlock",
                userHandle = USER_PERSONAL,
                sourceId = ANDROID_WEAR_UNLOCK_SOURCE_ID,
                summary = "android wear summary",
            )

        runTest(createScData(listOf(entry))) { fragment ->
            val preference =
                fragment.findPreference<SafetySourcePreference>(WEAR_UNLOCK_PERSONAL_PREFERENCE_KEY)
            assertThat(preference?.isVisible).isTrue()
            assertThat(preference?.title.toString()).isEqualTo(entry.title)
            assertThat(preference?.summary.toString()).isEqualTo(entry.summary)

            preference?.performClick()
            ShadowLooper.idleMainLooper()
            val startedIntent = shadowOf(application).nextStartedActivity
            assertThat(startedIntent).isNotNull()
            assertThat(startedIntent.action).isEqualTo(TEST_ACTION)
        }
    }

    @Test
    fun wearUnlockPersonalPref_whenNoEntry_isHidden() {
        runTest(EMPTY_SC_DATA) { fragment ->
            val preference =
                fragment.findPreference<SafetySourcePreference>(WEAR_UNLOCK_PERSONAL_PREFERENCE_KEY)
            assertThat(preference?.isVisible).isFalse()
        }
    }

    @Test
    fun wearUnlockWorkPref_whenWorkProfileAndEntryExists_isVisibleAndClickable() {
        val entry =
            createEntry(
                id = "wear_work",
                title = "Wear Unlock for Work",
                userHandle = USER_WORK_PROFILE,
                sourceId = ANDROID_WEAR_UNLOCK_SOURCE_ID,
                summary = "android wear summary",
            )

        runTest(createScData(listOf(entry))) { fragment ->
            val preference =
                fragment.findPreference<SafetySourcePreference>(WEAR_UNLOCK_WORK_PREFERENCE_KEY)
            assertThat(preference?.isVisible).isTrue()
            assertThat(preference?.title.toString()).isEqualTo(entry.title)
            assertThat(preference?.summary.toString()).isEqualTo(entry.summary)

            preference?.performClick()
            ShadowLooper.idleMainLooper()
            val startedIntent = shadowOf(application).nextStartedActivity
            assertThat(startedIntent).isNotNull()
            assertThat(startedIntent.action).isEqualTo(TEST_ACTION)
        }
    }

    @Test
    fun wearUnlockWorkPref_whenNoEntry_isHidden() {
        runTest(EMPTY_SC_DATA) { fragment ->
            val preference =
                fragment.findPreference<SafetySourcePreference>(WEAR_UNLOCK_WORK_PREFERENCE_KEY)
            assertThat(preference?.isVisible).isFalse()
        }
    }

    @Test
    fun wearUnlockWorkPref_whenNoWorkProfileEntry_isHidden() {
        val entry =
            createEntry(
                id = "wear_personal",
                title = "Wear Unlock",
                userHandle = USER_PERSONAL,
                sourceId = ANDROID_WEAR_UNLOCK_SOURCE_ID,
                summary = "android wear summary",
            )

        runTest(createScData(listOf(entry))) { fragment ->
            val preference =
                fragment.findPreference<SafetySourcePreference>(WEAR_UNLOCK_WORK_PREFERENCE_KEY)
            assertThat(preference?.isVisible).isFalse()
        }
    }

    @Test
    fun onPreferenceClick_nullIntent_doesNothing() {
        val entry =
            createEntry(
                id = "lockScreenEntry",
                title = "Screen Lock",
                userHandle = USER_PERSONAL,
                sourceId = ANDROID_LOCK_SCREEN_SOURCE_ID,
                summary = "set screen lock",
                pendingIntent = null,
            )
        runTest(createScData(listOf(entry))) { fragment ->
            val preference =
                fragment.findPreference<SafetySourcePreference>(ANDROID_LOCK_SCREEN_PREFERENCE_KEY)

            preference?.performClick()
            ShadowLooper.idleMainLooper()

            val startedIntent = shadowOf(application).nextStartedActivity
            assertThat(startedIntent).isNull()
        }
    }

    // --- Tests for Issues Banner Group (Warning Cards) ---

    @Test
    fun issuesBannerGroup_whenNoIssues_isHiddenAndIllustrationVisible() {
        runTest(EMPTY_SC_DATA) { fragment ->
            val bannerGroup =
                fragment.findPreference<BannerMessagePreferenceGroup>(DEVICE_UNLOCK_ISSUES_KEY)
            val illustration =
                fragment.findPreference<IllustrationPreference>(DEVICE_UNLOCK_ILLUSTRATION_KEY)

            assertThat(bannerGroup?.isVisible).isFalse()
            assertThat(illustration?.isVisible).isTrue()
        }
    }

    @Test
    fun issuesBannerGroup_withActiveIssue_isVisibleAndShowsBanner() {
        val activeIssue =
            createIssue(
                id = "activeIssue",
                title = "Active Issue Title",
                sourceIds = setOf(ANDROID_LOCK_SCREEN_SOURCE_ID),
                severity = SafetyCenterIssue.ISSUE_SEVERITY_LEVEL_CRITICAL_WARNING,
            )
        runTest(createScData(activeIssues = listOf(activeIssue))) { fragment ->
            val bannerGroup =
                fragment.findPreference<BannerMessagePreferenceGroup>(DEVICE_UNLOCK_ISSUES_KEY)
            val illustration =
                fragment.findPreference<IllustrationPreference>(DEVICE_UNLOCK_ILLUSTRATION_KEY)
            val banner = bannerGroup?.findPreference<BannerMessagePreference>(activeIssue.id)

            assertThat(bannerGroup?.isVisible).isTrue()
            assertThat(illustration?.isVisible).isFalse()
            assertThat(bannerGroup?.preferenceCount).isEqualTo(1)
            assertThat(banner).isNotNull()
            assertThat(banner?.title.toString()).isEqualTo(activeIssue.title)
            assertThat(banner?.summary.toString()).isEqualTo(activeIssue.summary)
        }
    }

    @Test
    fun issuesBannerGroup_withDismissedIssue_isVisibleAndShowsBannerInSubsection() {
        val dismissedIssue =
            createIssue(
                id = "dismissedIssue",
                title = "Dismissed Issue Title",
                sourceIds = setOf(ANDROID_FACE_UNLOCK_SOURCE_ID),
                severity = SafetyCenterIssue.ISSUE_SEVERITY_LEVEL_RECOMMENDATION,
            )
        runTest(createScData(dismissedIssues = listOf(dismissedIssue))) { fragment ->
            val bannerGroup =
                fragment.findPreference<BannerMessagePreferenceGroup>(DEVICE_UNLOCK_ISSUES_KEY)
            val illustration =
                fragment.findPreference<IllustrationPreference>(DEVICE_UNLOCK_ILLUSTRATION_KEY)
            val banner = bannerGroup?.findPreference<BannerMessagePreference>(dismissedIssue.id)

            assertThat(bannerGroup?.isVisible).isTrue()
            assertThat(illustration?.isVisible).isTrue()
            // 1 for expand preference, 1 for collapse preference, 1 for the issue banner
            assertThat(bannerGroup?.preferenceCount).isEqualTo(3)
            assertThat(banner).isNotNull()
            assertThat(banner?.title.toString()).isEqualTo(dismissedIssue.title)
            assertThat(banner?.summary.toString()).isEqualTo(dismissedIssue.summary)
        }
    }

    @Test
    fun issuesBannerGroup_withActiveAndDismissedIssues_showsBothAndHidesIllustration() {
        val activeIssue =
            createIssue(
                "activeIssue",
                "Active Issue Title",
                sourceIds = setOf(ANDROID_LOCK_SCREEN_SOURCE_ID),
            )
        val dismissedIssue =
            createIssue(
                "dismissedIssue",
                "Dismissed Issue Title",
                sourceIds = setOf(ANDROID_FINGERPRINT_UNLOCK_SOURCE_ID),
            )
        runTest(
            createScData(
                activeIssues = listOf(activeIssue),
                dismissedIssues = listOf(dismissedIssue),
            )
        ) { fragment ->
            val bannerGroup =
                fragment.findPreference<BannerMessagePreferenceGroup>(DEVICE_UNLOCK_ISSUES_KEY)
            val illustration =
                fragment.findPreference<IllustrationPreference>(DEVICE_UNLOCK_ILLUSTRATION_KEY)
            val activeBanner = bannerGroup?.findPreference<BannerMessagePreference>(activeIssue.id)
            val dismissedBanner =
                bannerGroup?.findPreference<BannerMessagePreference>(dismissedIssue.id)

            assertThat(bannerGroup?.isVisible).isTrue()
            assertThat(illustration?.isVisible).isFalse()
            // 1 for active issue banner, 1 for expand preference, 1 for collapse preference, 1 for
            // dismissed issue banner
            assertThat(bannerGroup?.preferenceCount).isEqualTo(4)
            assertThat(activeBanner).isNotNull()
            assertThat(dismissedBanner).isNotNull()
        }
    }

    @Test
    fun issuesBannerGroup_filtersIssuesBySourceId() {
        val relevantIssue =
            createIssue(
                "relevantIssue",
                "Relevant Issue Title",
                sourceIds = setOf(ANDROID_LOCK_SCREEN_SOURCE_ID),
            )
        val irrelevantIssue =
            createIssue(
                "irrelevantIssue",
                "Irrelevant Issue Title",
                sourceIds = setOf("OtherSource"),
            )
        runTest(createScData(activeIssues = listOf(relevantIssue, irrelevantIssue))) { fragment ->
            val bannerGroup =
                fragment.findPreference<BannerMessagePreferenceGroup>(DEVICE_UNLOCK_ISSUES_KEY)
            val relevantBanner =
                bannerGroup?.findPreference<BannerMessagePreference>(relevantIssue.id)
            val irrelevantBanner =
                bannerGroup?.findPreference<BannerMessagePreference>(irrelevantIssue.id)

            assertThat(bannerGroup?.isVisible).isTrue()
            assertThat(bannerGroup?.preferenceCount).isEqualTo(1)
            assertThat(relevantBanner).isNotNull()
            assertThat(irrelevantBanner).isNull()
        }
    }

    @Test
    fun issuesBannerGroup_clickDismissWithConfirmation_showsDialogAndMovesIssueToDismissed() {
        val activeIssue =
            createIssue(
                id = "dismissIssueWithConfirmation",
                title = "Dismiss Issue With Confirmation",
                sourceIds = setOf(ANDROID_LOCK_SCREEN_SOURCE_ID),
                severity = SafetyCenterIssue.ISSUE_SEVERITY_LEVEL_CRITICAL_WARNING,
                isDismissible = true,
                shouldConfirmDismissal = true,
            )
        runTest(createScData(activeIssues = listOf(activeIssue))) { fragment ->
            onView(withId(R.id.banner_dismiss_btn)).perform(click())
            ShadowLooper.idleMainLooper()

            onView(withText(R.string.safety_center_issue_card_dismiss_confirmation_title))
                .inRoot(isDialog())
                .check(matches(isDisplayed()))
            onView(withText(R.string.safety_center_issue_card_dismiss_confirmation_message))
                .inRoot(isDialog())
                .check(matches(isDisplayed()))
            onView(withText(R.string.dismiss)).inRoot(isDialog()).check(matches(isDisplayed()))
            onView(withText(R.string.cancel)).inRoot(isDialog()).check(matches(isDisplayed()))

            onView(withText(R.string.dismiss)).inRoot(isDialog()).perform(click())
            ShadowLooper.idleMainLooper()

            val bannerGroup =
                fragment.findPreference<BannerMessagePreferenceGroup>(DEVICE_UNLOCK_ISSUES_KEY)
            assertThat(bannerGroup?.findPreference<BannerMessagePreference>(activeIssue.id))
                .isNotNull()

            onView(withId(ButtonR.id.settingslib_number_button)).perform(click())
            ShadowLooper.idleMainLooper()
            onView(withText(activeIssue.title.toString())).check(matches(isDisplayed()))
            val illustration =
                fragment.findPreference<IllustrationPreference>(DEVICE_UNLOCK_ILLUSTRATION_KEY)
            assertThat(illustration?.isVisible).isTrue()
        }
    }

    @Test
    fun issuesBannerGroup_clickDismissNoConfirmation_movesIssueToDismissed() {
        val activeIssue =
            createIssue(
                id = "dismissIssueWithoutConfirm",
                title = "Dismiss issue without confirmation",
                sourceIds = setOf(ANDROID_LOCK_SCREEN_SOURCE_ID),
                isDismissible = true,
                shouldConfirmDismissal = false,
            )
        runTest(createScData(activeIssues = listOf(activeIssue))) { fragment ->
            onView(withId(R.id.banner_dismiss_btn)).perform(click())
            ShadowLooper.idleMainLooper()

            val dialog = ShadowDialog.getLatestDialog()
            assertThat(dialog).isNull()

            val bannerGroup =
                fragment.findPreference<BannerMessagePreferenceGroup>(DEVICE_UNLOCK_ISSUES_KEY)
            assertThat(bannerGroup?.findPreference<BannerMessagePreference>(activeIssue.id))
                .isNotNull()

            onView(withId(ButtonR.id.settingslib_number_button)).perform(click())
            ShadowLooper.idleMainLooper()
            onView(withText(activeIssue.title.toString())).check(matches(isDisplayed()))
            val illustration =
                fragment.findPreference<IllustrationPreference>(DEVICE_UNLOCK_ILLUSTRATION_KEY)
            assertThat(illustration?.isVisible).isTrue()
        }
    }

    @Test
    fun issuesBannerGroup_clickPrimaryActionNoConfirmation_resolvesIssue() {
        val action =
            createIssueAction(
                id = "primaryActionWithoutConfirmation",
                label = "Primary Action Without Confirmation",
                hasConfirmation = false,
                willResolve = true,
            )
        val activeIssue =
            createIssue(
                id = "primaryActionWithoutConfirmationIssue",
                actions = listOf(action),
                sourceIds = setOf(ANDROID_LOCK_SCREEN_SOURCE_ID),
            )

        runTest(createScData(activeIssues = listOf(activeIssue))) { fragment ->
            onView(withId(R.id.banner_positive_btn)).perform(click())
            ShadowLooper.idleMainLooper()

            val dialog = ShadowDialog.getLatestDialog()
            assertThat(dialog).isNull()

            // Manually update the data to simulate the issue being resolved
            shadowSafetyCenterManager.setSafetyCenterData(EMPTY_SC_DATA)
            ShadowLooper.idleMainLooper()

            val bannerGroup =
                fragment.findPreference<BannerMessagePreferenceGroup>(DEVICE_UNLOCK_ISSUES_KEY)
            assertThat(bannerGroup?.findPreference<BannerMessagePreference>(activeIssue.id))
                .isNull()
            assertThat(bannerGroup?.isVisible).isFalse()
            val illustration =
                fragment.findPreference<IllustrationPreference>(DEVICE_UNLOCK_ILLUSTRATION_KEY)
            assertThat(illustration?.isVisible).isTrue()
        }
    }

    @Test
    fun issuesBannerGroup_clickSecondaryActionNoConfirmation_doesNotShowDialog() {
        val action1 = createIssueAction(id = "primaryAction", label = "Primary Action")
        val action2 = createIssueAction(id = "secondaryAction", label = "Secondary Action")
        val activeIssue =
            createIssue(
                id = "issueWithSecondaryAction",
                actions = listOf(action1, action2),
                sourceIds = setOf(ANDROID_LOCK_SCREEN_SOURCE_ID),
            )

        runTest(createScData(activeIssues = listOf(activeIssue))) { fragment ->
            onView(withId(R.id.banner_negative_btn)).perform(click())
            ShadowLooper.idleMainLooper()

            val dialog = ShadowDialog.getLatestDialog()
            assertThat(dialog).isNull()
        }
    }

    @Test
    fun issuesBannerGroup_clickPrimaryActionWithConfirmation_showsDialogAndResolvesIssue() {
        val action =
            createIssueAction(
                id = "primaryActionWithConfirmation",
                label = "Primary Action With Confirmation",
                hasConfirmation = true,
                willResolve = true,
            )
        val activeIssue =
            createIssue(
                id = "primaryActionWithConfirmationIssue",
                actions = listOf(action),
                sourceIds = setOf(ANDROID_LOCK_SCREEN_SOURCE_ID),
            )

        runTest(createScData(activeIssues = listOf(activeIssue))) { fragment ->
            onView(withId(R.id.banner_positive_btn)).perform(click())
            ShadowLooper.idleMainLooper()

            val expectedDetails = action.confirmationDialogDetails!!
            onView(withText(expectedDetails.title.toString()))
                .inRoot(isDialog())
                .check(matches(isDisplayed()))
            onView(withText(expectedDetails.text.toString()))
                .inRoot(isDialog())
                .check(matches(isDisplayed()))
            onView(withText(expectedDetails.acceptButtonText.toString()))
                .inRoot(isDialog())
                .check(matches(isDisplayed()))
            onView(withText(expectedDetails.denyButtonText.toString()))
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
                fragment.findPreference<BannerMessagePreferenceGroup>(DEVICE_UNLOCK_ISSUES_KEY)
            assertThat(bannerGroup?.findPreference<BannerMessagePreference>(activeIssue.id))
                .isNull()
            assertThat(bannerGroup?.isVisible).isFalse()
            val illustration =
                fragment.findPreference<IllustrationPreference>(DEVICE_UNLOCK_ILLUSTRATION_KEY)
            assertThat(illustration?.isVisible).isTrue()
        }
    }

    @Test
    fun issuesBannerGroup_withMultipleIssues_isCollapsedAndCanExpand() {
        val issue1 =
            createIssue("issue1", "Issue 1", sourceIds = setOf(ANDROID_LOCK_SCREEN_SOURCE_ID))
        val issue2 =
            createIssue("issue2", "Issue 2", sourceIds = setOf(ANDROID_LOCK_SCREEN_SOURCE_ID))
        val issue3 =
            createIssue("issue3", "Issue 3", sourceIds = setOf(ANDROID_LOCK_SCREEN_SOURCE_ID))
        runTest(createScData(activeIssues = listOf(issue1, issue2, issue3))) { fragment ->
            val bannerGroup =
                fragment.findPreference<BannerMessagePreferenceGroup>(DEVICE_UNLOCK_ISSUES_KEY)
            assertThat(bannerGroup?.isVisible).isTrue()
            val banner1 = bannerGroup?.findPreference<BannerMessagePreference>("issue1")
            val banner2 = bannerGroup?.findPreference<BannerMessagePreference>("issue2")
            val banner3 = bannerGroup?.findPreference<BannerMessagePreference>("issue3")

            // Initially collapsed
            assertThat(banner1?.isVisible).isTrue()
            assertThat(banner2?.isVisible).isFalse()
            assertThat(banner3?.isVisible).isFalse()
            val expandButtonText =
                application.getString(R.string.safety_center_issues_banner_group_expandable_title)
            onView(withText(expandButtonText)).check(matches(isDisplayed()))

            // Click expand
            onView(withId(ButtonR.id.settingslib_number_button)).perform(click())
            ShadowLooper.idleMainLooper()

            assertThat(banner1?.isVisible).isTrue()
            assertThat(banner2?.isVisible).isTrue()
            assertThat(banner3?.isVisible).isTrue()

            onView(isRoot()).perform(swipeUp())
            val collapseButtonText =
                application.getString(R.string.safety_center_issues_banner_group_collapsible_title)
            onView(withId(R.id.settingslib_section_button)).perform(scrollTo())
            onView(withText(collapseButtonText)).check(matches(isDisplayed()))

            // Click collapse
            onView(withId(R.id.settingslib_section_button)).perform(click())
            ShadowLooper.idleMainLooper()

            assertThat(banner1?.isVisible).isTrue()
            assertThat(banner2?.isVisible).isFalse()
            assertThat(banner3?.isVisible).isFalse()
        }
    }

    companion object {
        private const val DEVICE_UNLOCK_ILLUSTRATION_KEY = "device_unlock_illustration"
        private const val DEVICE_UNLOCK_ISSUES_KEY = "device_unlock_issues_banner_group"
        private const val ANDROID_LOCK_SCREEN_PREFERENCE_KEY = "android_lock_screen"
        private const val FINGERPRINT_UNLOCK_PERSONAL_PREFERENCE_KEY =
            "android_fingerprint_unlock_personal"
        private const val FINGERPRINT_UNLOCK_WORK_PREFERENCE_KEY = "android_fingerprint_unlock_work"
        private const val FACE_UNLOCK_PERSONAL_PREFERENCE_KEY = "android_face_unlock_personal"
        private const val FACE_UNLOCK_WORK_PREFERENCE_KEY = "android_face_unlock_work"
        private const val WEAR_UNLOCK_PERSONAL_PREFERENCE_KEY = "android_wear_unlock_personal"
        private const val WEAR_UNLOCK_WORK_PREFERENCE_KEY = "android_wear_unlock_work"

        private const val ANDROID_LOCK_SCREEN_SOURCE_ID = "AndroidLockScreen"
        private const val ANDROID_FINGERPRINT_UNLOCK_SOURCE_ID = "AndroidFingerprintUnlock"
        private const val ANDROID_FACE_UNLOCK_SOURCE_ID = "AndroidFaceUnlock"
        private const val ANDROID_WEAR_UNLOCK_SOURCE_ID = "AndroidWearUnlock"
    }
}
