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

import android.Manifest
import android.app.Application
import android.content.Context
import android.os.Build
import android.os.UserHandle
import android.permission.flags.Flags
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.safetycenter.SafetyCenterData
import android.safetycenter.SafetyCenterEntry
import android.safetycenter.SafetyCenterEntryOrGroup
import android.safetycenter.SafetyCenterIssue
import android.safetycenter.SafetyCenterManager
import android.safetycenter.SafetyCenterStatus
import androidx.annotation.RequiresPermission
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
import com.android.settings.safetycenter.ui.SafetyCenterFragment
import com.android.settingslib.widget.preference.statusbanner.R as SettingsLibR
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowContextImpl
import org.robolectric.shadows.ShadowDrawable
import org.robolectric.shadows.ShadowLooper

@RunWith(AndroidJUnit4::class)
@Config(minSdk = Build.VERSION_CODES.BAKLAVA)
class SafetyCenterFragmentTest {

    @get:Rule val setFlagsRule = SetFlagsRule()

    private lateinit var mApplication: Application
    private val mockSafetyCenterManager = mock<SafetyCenterManager>()

    @RequiresPermission(Manifest.permission.MANAGE_SAFETY_CENTER)
    @Before
    fun setUp() {
        mApplication = ApplicationProvider.getApplicationContext()

        val shadowContextImpl = Shadow.extract<ShadowContextImpl>(mApplication.baseContext)
        shadowContextImpl.setSystemService(Context.SAFETY_CENTER_SERVICE, mockSafetyCenterManager)

        // Default empty data
        whenever(mockSafetyCenterManager.safetyCenterData) doReturn EMPTY_SC_DATA
    }

    @RequiresPermission(Manifest.permission.MANAGE_SAFETY_CENTER)
    private fun runTest(data: SafetyCenterData, testBlock: (SafetyCenterFragment) -> Unit) {
        whenever(mockSafetyCenterManager.safetyCenterData) doReturn data
        val scenario =
            launchFragmentInContainer<SafetyCenterFragment>(themeResId = R.style.Theme_SubSettings)
        scenario.onFragment { fragment ->
            ShadowLooper.idleMainLooper()
            testBlock(fragment)
        }
        scenario.close()
    }

    private fun createEntry(
        id: String,
        sourceId: String = ENTRY_SOURCE_ID,
        severity: Int = SafetyCenterEntry.ENTRY_SEVERITY_LEVEL_OK,
        summary: String? = "Entry Summary $id",
        hasError: Boolean = false,
        iconType: Int? = null,
    ): SafetyCenterEntry {
        val title = "Title $id"
        val builder =
            if (Flags.openSafetyCenterApis()) {
                SafetyCenterEntry.Builder(id, title, USER0, sourceId).setHasError(hasError)
            } else {
                SafetyCenterEntry.Builder(id, title)
            }
        builder.setSeverityLevel(severity).setSummary(summary)
        iconType?.let { builder.setSeverityUnspecifiedIconType(it) }
        return builder.build()
    }

    private fun createIssue(
        id: String,
        sourceIds: Set<String> = setOf(ENTRY_SOURCE_ID),
        severity: Int = SafetyCenterIssue.ISSUE_SEVERITY_LEVEL_RECOMMENDATION,
        title: String = "Issue Title $id",
    ): SafetyCenterIssue {
        val summary = "Summary $id"
        val builder =
            if (Flags.openSafetyCenterApis()) {
                SafetyCenterIssue.Builder(id, title, summary, USER0, sourceIds, "type_$id")
            } else {
                SafetyCenterIssue.Builder(id, title, summary)
            }
        builder.setSeverityLevel(severity)
        return builder.build()
    }

    private fun createScData(
        entries: List<SafetyCenterEntry> = emptyList(),
        activeIssues: List<SafetyCenterIssue> = emptyList(),
    ): SafetyCenterData {
        val builder = SafetyCenterData.Builder(DEFAULT_STATUS)
        entries.forEach { builder.addEntryOrGroup(SafetyCenterEntryOrGroup(it)) }
        activeIssues.forEach { builder.addIssue(it) }
        return builder.build()
    }

    private fun assertIconResource(preference: Preference?, expectedResId: Int) {
        assertThat(preference?.icon).isNotNull()
        val shadowDrawable: ShadowDrawable = Shadow.extract(preference?.icon)
        assertThat(shadowDrawable.createdFromResId).isEqualTo(expectedResId)
    }

    private fun expectedDefaultUnlockSummary(): String {
        return mApplication.getString(DEFAULT_UNLOCK_SUMMARY_RES)
    }

    @RequiresPermission(Manifest.permission.MANAGE_SAFETY_CENTER)
    @Test
    fun shouldShowAllPreferences() {
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

            onView(withText(mApplication.getString(R.string.permissions_usage_title)))
                .perform(scrollTo())
                .check(matches(isDisplayed()))

            onView(isRoot()).perform(swipeUp())
            onView(withText(mApplication.getString(R.string.more_security_privacy_category_title)))
                .check(matches(isDisplayed()))

            onView(withText(mApplication.getString(R.string.more_security_privacy_settings)))
                .check(matches(isDisplayed()))
        }
    }

    // Tests for Device Unlock SubpagePreferenceController
    @RequiresPermission(Manifest.permission.MANAGE_SAFETY_CENTER)
    @Test
    @EnableFlags(Flags.FLAG_OPEN_SAFETY_CENTER_APIS)
    fun deviceUnlockWhenNoDataUsesDefaultSummary() {
        runTest(EMPTY_SC_DATA) { fragment ->
            val preference = fragment.findPreference<Preference>(DEVICE_UNLOCK_KEY)
            assertThat(preference?.summary.toString()).isEqualTo(expectedDefaultUnlockSummary())
            assertThat(preference?.icon).isNull()
        }
    }

    @RequiresPermission(Manifest.permission.MANAGE_SAFETY_CENTER)
    @Test
    @EnableFlags(Flags.FLAG_OPEN_SAFETY_CENTER_APIS)
    fun deviceUnlockWithOkEntryAndNoIssuesUsesDefaultSummaryAndInfoIcon() {
        val entry = createEntry("ok", severity = SafetyCenterEntry.ENTRY_SEVERITY_LEVEL_OK)
        runTest(createScData(entries = listOf(entry))) { fragment ->
            val preference = fragment.findPreference<Preference>(DEVICE_UNLOCK_KEY)
            assertThat(preference?.summary.toString()).isEqualTo(expectedDefaultUnlockSummary())
            assertIconResource(preference, R.drawable.ic_safety_info)
        }
    }

    @RequiresPermission(Manifest.permission.MANAGE_SAFETY_CENTER)
    @Test
    @EnableFlags(Flags.FLAG_OPEN_SAFETY_CENTER_APIS)
    fun deviceUnlockWithOkEntryAndOkIssueUsesEntrySummaryAndInfoIcon() {
        val entry = createEntry("ok_issue", severity = SafetyCenterEntry.ENTRY_SEVERITY_LEVEL_OK)
        val issue = createIssue("issue", severity = SafetyCenterIssue.ISSUE_SEVERITY_LEVEL_OK)
        runTest(createScData(entries = listOf(entry), activeIssues = listOf(issue))) { fragment ->
            val preference = fragment.findPreference<Preference>(DEVICE_UNLOCK_KEY)
            assertThat(preference?.summary.toString()).isEqualTo(entry.summary)
            assertIconResource(preference, R.drawable.ic_safety_info)
        }
    }

    @RequiresPermission(Manifest.permission.MANAGE_SAFETY_CENTER)
    @Test
    @EnableFlags(Flags.FLAG_OPEN_SAFETY_CENTER_APIS)
    fun deviceUnlockWithRecommendationEntryUsesEntrySummaryAndRecoIcon() {
        val entry =
            createEntry("reco", severity = SafetyCenterEntry.ENTRY_SEVERITY_LEVEL_RECOMMENDATION)
        runTest(createScData(entries = listOf(entry))) { fragment ->
            val preference = fragment.findPreference<Preference>(DEVICE_UNLOCK_KEY)
            assertThat(preference?.summary.toString()).isEqualTo(entry.summary)
            assertIconResource(preference, R.drawable.ic_safety_recommendation)
        }
    }

    @RequiresPermission(Manifest.permission.MANAGE_SAFETY_CENTER)
    @Test
    @EnableFlags(Flags.FLAG_OPEN_SAFETY_CENTER_APIS)
    fun deviceUnlockWithCriticalEntryUsesEntrySummaryAndWarnIcon() {
        val entry =
            createEntry("crit", severity = SafetyCenterEntry.ENTRY_SEVERITY_LEVEL_CRITICAL_WARNING)
        runTest(createScData(entries = listOf(entry))) { fragment ->
            val preference = fragment.findPreference<Preference>(DEVICE_UNLOCK_KEY)
            assertThat(preference?.summary.toString()).isEqualTo(entry.summary)
            assertIconResource(preference, R.drawable.ic_safety_warn)
        }
    }

    @RequiresPermission(Manifest.permission.MANAGE_SAFETY_CENTER)
    @Test
    @EnableFlags(Flags.FLAG_OPEN_SAFETY_CENTER_APIS)
    fun deviceUnlockWithUnspecifiedEntryAndNoIconTypeUsesDefaultSummaryAndEmptyIcon() {
        val entry =
            createEntry(
                "unspecified",
                severity = SafetyCenterEntry.ENTRY_SEVERITY_LEVEL_UNSPECIFIED,
                iconType = SafetyCenterEntry.SEVERITY_UNSPECIFIED_ICON_TYPE_NO_ICON,
            )
        runTest(createScData(entries = listOf(entry))) { fragment ->
            val preference = fragment.findPreference<Preference>(DEVICE_UNLOCK_KEY)
            assertThat(preference?.summary.toString()).isEqualTo(expectedDefaultUnlockSummary())
            assertIconResource(preference, R.drawable.ic_safety_empty)
        }
    }

    @RequiresPermission(Manifest.permission.MANAGE_SAFETY_CENTER)
    @Test
    @EnableFlags(Flags.FLAG_OPEN_SAFETY_CENTER_APIS)
    fun deviceUnlockWithUnknownEntryAndErrorUsesErrorSummary() {
        val entry =
            createEntry(
                "err",
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

    @RequiresPermission(Manifest.permission.MANAGE_SAFETY_CENTER)
    @Test
    @DisableFlags(Flags.FLAG_OPEN_SAFETY_CENTER_APIS)
    fun deviceUnlockWhenFlagDisabledUsesDefaultSummaryAndNullIcon() {
        val entry =
            createEntry("ok_issue_flag_off", severity = SafetyCenterEntry.ENTRY_SEVERITY_LEVEL_OK)
        val issue = createIssue("issue", severity = SafetyCenterIssue.ISSUE_SEVERITY_LEVEL_OK)
        runTest(createScData(entries = listOf(entry), activeIssues = listOf(issue))) { fragment ->
            val preference = fragment.findPreference<Preference>(DEVICE_UNLOCK_KEY)
            assertThat(preference?.summary.toString()).isEqualTo(expectedDefaultUnlockSummary())
            assertThat(preference?.icon).isNull()
        }
    }

    companion object {
        private const val DEVICE_UNLOCK_KEY = "device_unlock_subpage"
        private const val ENTRY_SOURCE_ID = "AndroidLockScreen"
        private val DEFAULT_UNLOCK_SUMMARY_RES = R.string.device_unlock_subpage_default_summary

        private val USER0 = UserHandle.of(0)

        private val DEFAULT_STATUS =
            SafetyCenterStatus.Builder("Test Title", "Test Summary")
                .setSeverityLevel(SafetyCenterStatus.OVERALL_SEVERITY_LEVEL_OK)
                .build()
        private val EMPTY_SC_DATA = SafetyCenterData.Builder(DEFAULT_STATUS).build()
    }
}
