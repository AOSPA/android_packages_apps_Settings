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

package com.android.settings.accessibility

import android.app.settings.SettingsEnums
import android.content.ComponentName
import android.content.Context
import android.view.accessibility.AccessibilityManager
import androidx.fragment.app.testing.FragmentScenario
import androidx.lifecycle.Lifecycle
import androidx.preference.Preference
import androidx.test.core.app.ApplicationProvider
import com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType
import com.android.settings.R
import com.android.settings.testutils.AccessibilityTestUtils
import com.android.settings.testutils.AccessibilityTestUtils.assertShortcutsTutorialDialogShown
import com.android.settings.testutils.shadow.ShadowAccessibilityManager
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowDialog

/**
 * Tests for [ShortcutFragment]
 */
@RunWith(RobolectricTestRunner::class)
class ShortcutFragmentTest {
    private val context: Context = ApplicationProvider.getApplicationContext<Context?>()
    private lateinit var fragmentScenario: FragmentScenario<TestShortcutFragment>
    private lateinit var fragment: TestShortcutFragment

    private val a11yManager: ShadowAccessibilityManager =
        Shadow.extract(context.getSystemService(AccessibilityManager::class.java))

    @Before
    fun setUp() {
        context.setTheme(androidx.appcompat.R.style.Theme_AppCompat)
        launchFragment()
    }

    @Test
    fun verifyFragmentUI_containsShortcutToggle() {
        val pref: Preference? = fragment.findPreference<Preference?>(SHORTCUT_KEY)

        assertThat(pref).isNotNull()
        assertThat(pref!!.isVisible).isTrue()
        assertThat(pref.title).isEqualTo(SHORTCUT_LABEL)
    }

    @Test
    fun shortcutOff_clickShortcutToggle_turnOnShortcutAndShowShortcutTutorial() {
        a11yManager.enableShortcutsForTargets( /* enable= */ false, UserShortcutType.ALL,
            setOf(FAKE_COMPONENT.flattenToString()), context.userId
        )
        launchFragment()

        val pref: ShortcutPreference? = fragment.findPreference(SHORTCUT_KEY)
        assertThat(pref).isNotNull()
        assertThat(pref!!.isChecked).isFalse()
        val viewHolder =
            AccessibilityTestUtils.inflateShortcutPreferenceView(fragment.requireContext(), pref)

        val widget = viewHolder.findViewById(pref.switchResId)
        assertThat(widget).isNotNull()
        widget!!.performClick()

        assertThat(pref.isChecked).isTrue()
        assertShortcutsTutorialDialogShown(fragment)
    }

    @Test
    fun shortcutOn_clickShortcutToggle_turnOffShortcutAndNoTutorialShown() {
        a11yManager.enableShortcutsForTargets( /* enable= */ true,
            UserShortcutType.HARDWARE,
            setOf(FAKE_COMPONENT.flattenToString()),
            context.userId
        )
        launchFragment()

        val pref: ShortcutPreference? = fragment.findPreference(SHORTCUT_KEY)
        assertThat(pref).isNotNull()
        assertThat(pref!!.isChecked).isTrue()
        val viewHolder = AccessibilityTestUtils.inflateShortcutPreferenceView(
            fragment.requireContext(), pref
        )

        val widget = viewHolder.findViewById(pref.switchResId)
        assertThat(widget).isNotNull()
        widget!!.performClick()

        assertThat(pref.isChecked).isFalse()
        assertThat(a11yManager.getAccessibilityShortcutTargets(UserShortcutType.HARDWARE))
            .isEmpty()
        assertThat(ShadowDialog.getLatestDialog()).isNull()
    }

    @Test
    fun clickShortcutSettings_showEditShortcutsScreenWithoutChangingShortcutToggleState() {
        val pref: ShortcutPreference? = fragment.findPreference(SHORTCUT_KEY)
        assertThat(pref).isNotNull()
        val shortcutToggleState = pref!!.isChecked
        pref.performClick()

        AccessibilityTestUtils.assertEditShortcutsScreenShown(fragment)
        Truth.assertThat(pref.isChecked).isEqualTo(shortcutToggleState)
    }

    private fun launchFragment() {
        fragmentScenario = androidx.fragment.app.testing.launchFragment<TestShortcutFragment>(
            themeResId = androidx.appcompat.R.style.Theme_AppCompat,
            initialState = Lifecycle.State.RESUMED
        ).apply { onFragment { frag -> fragment = frag } }
    }
}

private const val SHORTCUT_KEY = "shortcut_preference"
private const val SHORTCUT_LABEL = "Fake shortcut"
private val FAKE_COMPONENT = ComponentName("FakePkg", "FakeClass")

/** [ShortcutFragment] used in test. */
class TestShortcutFragment : ShortcutFragment() {
    override fun getShortcutLabel(): CharSequence = SHORTCUT_LABEL

    override fun getFeatureComponentName() = FAKE_COMPONENT

    override fun getPreferenceScreenResId(): Int = R.xml.fake_shortcut_fragment_screen

    override fun getLogTag(): String? = null

    override fun getMetricsCategory(): Int = SettingsEnums.ACCESSIBILITY

}
