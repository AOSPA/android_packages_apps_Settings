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

package com.android.settings.accessibility.shortcuts.ui

import android.app.Application
import android.app.settings.SettingsEnums
import android.content.Context
import android.content.Intent
import android.text.SpannableStringBuilder
import android.text.style.ClickableSpan
import androidx.fragment.app.testing.EmptyFragmentActivity
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType
import com.android.settings.R
import com.android.settings.SettingsActivity
import com.android.settings.SubSettings
import com.android.settings.accessibility.AccessibilityButtonFragment
import com.android.settings.accessibility.shortcuts.ShortcutOptionPreference as ShortcutOptionWidget
import com.android.settings.accessibility.shortcuts.data.ShortcutOptionDataStore
import com.android.settings.accessibility.shortcuts.ui.ShortcutOptionPreference.Companion.getCustomizeAccessibilityButtonLink
import com.android.settings.testutils.inflateViewHolder
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.preference.createAndBindWidget
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class ShortcutOptionPreferenceTest {

    @get:Rule
    val rule: ActivityScenarioRule<EmptyFragmentActivity> =
        ActivityScenarioRule(EmptyFragmentActivity::class.java)
    private lateinit var context: Context
    private lateinit var preference: ShortcutOptionPreference

    @Before
    fun setUp() {
        rule.scenario.onActivity { activity -> context = activity }
        preference =
            object :
                ShortcutOptionPreference(
                    context = context,
                    shortcutType = UserShortcutType.SOFTWARE,
                    targets = setOf("com.test.Target1", "com.test.Target2"),
                ) {
                override val key: String
                    get() = "test_shortcut_option_pref"
            }
    }

    @Test
    fun indexable_isFalse() {
        assertThat(preference.indexable).isFalse()
    }

    @Test
    fun createWidget_returnsInstanceOfShortcutOptionWidget() {
        val widget = preference.createWidget(context)

        assertThat(widget).isInstanceOf(ShortcutOptionWidget::class.java)
    }

    @Test
    fun storage_returnsShortcutOptionDataStore() {
        val storage = preference.storage(context)

        assertThat(storage).isInstanceOf(ShortcutOptionDataStore::class.java)
    }

    @Test
    fun getReadPermissions_returnsSecureStorePermissions() {
        val expectedPermissions = SettingsSecureStore.getReadPermissions()
        assertThat(preference.getReadPermissions(context)).isEqualTo(expectedPermissions)
    }

    @Test
    fun getWritePermissions_returnsSecureStorePermissions() {
        val expectedPermissions = SettingsSecureStore.getWritePermissions()
        assertThat(preference.getWritePermissions(context)).isEqualTo(expectedPermissions)
    }

    @Test
    fun getReadPermit_alwaysReturnsAllow() {
        val permit = preference.getReadPermit(context, callingPid = 0, callingUid = 0)

        assertThat(permit).isEqualTo(ReadWritePermit.ALLOW)
    }

    @Test
    fun getWritePermit_withTrueValue_returnsDisallow() {
        val permit =
            preference.getWritePermit(
                context = context,
                value = true,
                callingPid = 0,
                callingUid = 0,
            )

        assertThat(permit).isEqualTo(ReadWritePermit.DISALLOW)
    }

    @Test
    fun getWritePermit_withFalseValue_returnsAllow() {
        val permit =
            preference.getWritePermit(
                context = context,
                value = false,
                callingPid = 0,
                callingUid = 0,
            )

        assertThat(permit).isEqualTo(ReadWritePermit.ALLOW)
    }

    @Test
    fun getWritePermit_withNullValue_returnsAllow() {
        val permit =
            preference.getWritePermit(
                context = context,
                value = null,
                callingPid = 0,
                callingUid = 0,
            )

        assertThat(permit).isEqualTo(ReadWritePermit.ALLOW)
    }

    @Test
    fun getCustomizeAccessibilityButtonLink_onClick_launchesAccessibilityButtonFragment() {
        val preferenceViewHolder =
            preference.createAndBindWidget<ShortcutOptionWidget>(context).inflateViewHolder()
        val summaryView = requireNotNull(preferenceViewHolder.findViewById(android.R.id.summary))
        val spannableSummary =
            getCustomizeAccessibilityButtonLink(context) as SpannableStringBuilder

        val clickableSpans =
            spannableSummary.getSpans(0, spannableSummary.length, ClickableSpan::class.java)
        assertThat(clickableSpans).hasLength(1)
        val targetSpan = clickableSpans.first()

        targetSpan.onClick(summaryView)

        // Verify the correct Intent was launched.
        val shadowApplication = shadowOf(context.applicationContext as Application)
        val launchedIntent: Intent = shadowApplication.peekNextStartedActivity()

        assertThat(launchedIntent.component!!.className).isEqualTo(SubSettings::class.java.name)
        assertThat(launchedIntent.getStringExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT))
            .isEqualTo(AccessibilityButtonFragment::class.java.name)
        assertThat(
                launchedIntent.getIntExtra(MetricsFeatureProvider.EXTRA_SOURCE_METRICS_CATEGORY, -1)
            )
            .isEqualTo(SettingsEnums.DIALOG_ACCESSIBILITY_SERVICE_EDIT_SHORTCUT)

        val expectedSummary =
            context.getString(R.string.accessibility_shortcut_edit_dialog_summary_software_floating)
        assertThat(spannableSummary.toString()).isEqualTo(expectedSummary)
    }
}
