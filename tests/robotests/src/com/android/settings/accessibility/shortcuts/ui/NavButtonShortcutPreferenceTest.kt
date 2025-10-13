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
import android.content.Context
import android.content.Intent
import android.text.SpannableStringBuilder
import android.text.style.ClickableSpan
import android.text.style.ImageSpan
import android.widget.TextView
import androidx.fragment.app.testing.EmptyFragmentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.android.internal.accessibility.AccessibilityShortcutController.COLOR_INVERSION_COMPONENT_NAME
import com.android.settings.R
import com.android.settings.accessibility.shortcuts.ShortcutOptionPreference as ShortcutOptionWidget
import com.android.settings.testutils.AccessibilityTestUtils
import com.android.settings.testutils.SettingsStoreRule
import com.android.settings.testutils.inflateViewHolder
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.preference.createAndBindWidget
import com.google.android.setupcompat.util.WizardManagerHelper.EXTRA_IS_SETUP_FLOW
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameters
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestParameterInjector
import org.robolectric.shadows.ShadowLooper
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestParameterInjector::class)
class NavButtonShortcutPreferenceTest {
    @get:Rule val settingsStoreRule = SettingsStoreRule()
    private val appContext: Application = ApplicationProvider.getApplicationContext()
    private var activityScenario: ActivityScenario<EmptyFragmentActivity>? = null
    private lateinit var preference: NavButtonShortcutPreference
    private val targets = setOf(COLOR_INVERSION_COMPONENT_NAME.flattenToString())

    @Before
    fun setUp() {
        AccessibilityTestUtils.setSoftwareShortcutMode(
            appContext,
            /* gestureNavEnabled= */ false,
            /* floatingButtonEnabled= */ false,
        )
        preference = NavButtonShortcutPreference(appContext, targets)
    }

    @After
    fun tearDown() {
        activityScenario?.close()
    }

    @Test
    fun key_returnsCorrectValue() {
        assertThat(preference.key).isEqualTo("shortcut_nav_button_pref")
    }

    @Test
    fun title_returnsCorrectValue() {
        assertThat(preference.title)
            .isEqualTo(R.string.accessibility_shortcut_edit_dialog_title_software)
    }

    @Test
    fun bind_setsIntroImage() {
        val widget = preference.createAndBindWidget<ShortcutOptionWidget>(appContext)

        val imageResId: Int = ReflectionHelpers.getField(widget, "mIntroImageResId")

        assertThat(imageResId).isEqualTo(R.drawable.accessibility_shortcut_type_navbar)
    }

    @Test
    fun getSummary_inSetupWizard_returnsSummaryWithoutLink() {
        val context = createContext(inSetupWizard = true)

        val summary = preference.getSummary(context)
        val clickableSpans =
            (summary as SpannableStringBuilder).getSpans(
                0,
                summary.length,
                ClickableSpan::class.java,
            )

        assertThat(clickableSpans).hasLength(0)
        assertThat(summary.toString())
            .isEqualTo(
                context.getString(R.string.accessibility_shortcut_edit_dialog_summary_software)
            )
    }

    @Test
    fun getSummary_notInSetupWizard_returnsSummaryWithLink() {
        val context = createContext(inSetupWizard = false)
        val mainSummary =
            appContext.getString(R.string.accessibility_shortcut_edit_dialog_summary_software)
        val moreActionsLinkText =
            appContext.getString(
                R.string.accessibility_shortcut_edit_dialog_summary_software_floating
            )
        val expectedSummary = "$mainSummary\n\n$moreActionsLinkText"
        val summary = preference.getSummary(context)
        val clickableSpans =
            (summary as SpannableStringBuilder).getSpans(
                0,
                summary.length,
                ClickableSpan::class.java,
            )

        assertThat(clickableSpans).hasLength(1)
        assertThat(summary.toString()).isEqualTo(expectedSummary)
    }

    @Test
    fun getSummary_notInSetupWizard_containsImageSpan() {
        val context = createContext(inSetupWizard = false)
        val summary = preference.getSummary(context) as SpannableStringBuilder
        val imageSpans = summary.getSpans(0, summary.length, ImageSpan::class.java)
        assertThat(imageSpans).hasLength(1)
    }

    @TestParameters(
        value =
            [
                "{gestureNavEnabled: false, floatingButtonEnabled: false, expectedValue: true}",
                "{gestureNavEnabled: false, floatingButtonEnabled: true, expectedValue: false}",
                "{gestureNavEnabled: true, floatingButtonEnabled: false, expectedValue: false}",
                "{gestureNavEnabled: true, floatingButtonEnabled: true, expectedValue: false}",
            ]
    )
    @Test
    fun isAvailable_returnsExpectedValue(
        gestureNavEnabled: Boolean,
        floatingButtonEnabled: Boolean,
        expectedValue: Boolean,
    ) {
        AccessibilityTestUtils.setSoftwareShortcutMode(
            appContext,
            gestureNavEnabled,
            floatingButtonEnabled,
        )

        assertThat(preference.isAvailable(appContext)).isEqualTo(expectedValue)
    }

    @Test
    fun createWidget_setsOnBindListener() {
        val widget = preference.createWidget(appContext)

        val onBindListener: ShortcutOptionWidget.OnBindListener =
            ReflectionHelpers.getField(widget, "mOnBindListener")

        assertThat(onBindListener).isNotNull()
    }

    @Test
    fun bindWidget_updatePreferenceMetadataTextLineHeightAndSummary() {
        val widget = preference.createAndBindWidget<ShortcutOptionWidget>(appContext)
        val preferenceViewHolder = widget.inflateViewHolder()
        val updatedLineHeight = 20
        val summaryTextView = preferenceViewHolder.findViewById(android.R.id.summary) as TextView
        summaryTextView.lineHeight = updatedLineHeight

        widget.onBindViewHolder(preferenceViewHolder)
        val textLineHeight: Int = ReflectionHelpers.getField(preference, "textLineHeight")

        assertThat(textLineHeight).isEqualTo(updatedLineHeight)
        assertThat(summaryTextView.text.toString())
            .isEqualTo(preference.getSummary(appContext).toString())
    }

    @Test
    fun lifecycle_onCreate_registersObserver() {
        val lifecycleContext = getPreferenceLifecycleContext()

        preference.onCreate(lifecycleContext)

        assertThat(SettingsSecureStore.get(appContext).hasAnyObserver()).isTrue()
    }

    @Test
    fun lifecycle_onDestroy_unregistersObserver() {
        val lifecycleContext = getPreferenceLifecycleContext()

        preference.run {
            onCreate(lifecycleContext)
            onDestroy(lifecycleContext)
        }

        assertThat(SettingsSecureStore.get(appContext).hasAnyObserver()).isFalse()
    }

    @Test
    fun observer_onChange_notifiesPreferenceChange() {
        AccessibilityTestUtils.setSoftwareShortcutMode(
            appContext,
            /* gestureNavEnabled= */ false,
            /* floatingButtonEnabled= */ false,
        )
        val lifecycleContext = getPreferenceLifecycleContext()

        preference.onCreate(lifecycleContext)
        AccessibilityTestUtils.setSoftwareShortcutMode(
            appContext,
            /* gestureNavEnabled= */ false,
            /* floatingButtonEnabled= */ true,
        )
        ShadowLooper.idleMainLooper()

        verify(lifecycleContext).notifyPreferenceChange(preference.key)
    }

    private fun getPreferenceLifecycleContext(): PreferenceLifecycleContext {
        return mock { on { applicationContext }.thenReturn(appContext) }
    }

    private fun createContext(inSetupWizard: Boolean = false): Context {
        var startedActivity: Context? = null
        val intent = Intent(appContext, EmptyFragmentActivity::class.java)
        intent.putExtra(EXTRA_IS_SETUP_FLOW, inSetupWizard)
        activityScenario = ActivityScenario.launch(intent)
        activityScenario!!.onActivity { activity -> startedActivity = activity }
        return startedActivity!!
    }
}
