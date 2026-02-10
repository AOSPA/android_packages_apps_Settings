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

package com.android.settings.accessibility.a11yservice.ui

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.view.View
import android.view.accessibility.AccessibilityManager
import androidx.fragment.app.FragmentFactory
import androidx.fragment.app.testing.FragmentScenario
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import com.android.internal.accessibility.common.ShortcutConstants.USER_SHORTCUT_TYPES
import com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.HARDWARE
import com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.QUICK_SETTINGS
import com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.SOFTWARE
import com.android.settings.R
import com.android.settings.SettingsActivity
import com.android.settings.accessibility.ShortcutPreference
import com.android.settings.accessibility.data.AccessibilityRepositoryProvider
import com.android.settings.accessibility.shared.dialogs.AccessibilityServiceWarningDialogFragment
import com.android.settings.accessibility.shortcuts.EditShortcutsPreferenceFragment
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.testutils.AccessibilityTestUtils
import com.android.settings.testutils.SettingsStoreRule
import com.android.settings.testutils.shadow.ShadowAccessibilityManager
import com.android.settingslib.metadata.FixedArrayMap
import com.android.settingslib.metadata.PreferenceScreenRegistry
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.preference.PreferenceFragment
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.spy
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowDialog
import org.robolectric.shadows.ShadowLooper

/** Tests for [A11yServiceShortcutPreference]. */
@RunWith(RobolectricTestRunner::class)
class A11yServiceShortcutPreferenceTest {
    @get:Rule val settingsStoreRule = SettingsStoreRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val a11yManager: ShadowAccessibilityManager =
        Shadow.extract(context.getSystemService(AccessibilityManager::class.java))
    private val fakeComponentName = ComponentName("FakePackage", "StandardA11yService")
    private val serviceInfo =
        spy(
            AccessibilityTestUtils.createAccessibilityServiceInfo(
                context,
                fakeComponentName,
                /* isAlwaysOnService= */ false,
            )
        )
    private val serviceWarningAllowButtonId =
        com.android.internal.R.id.accessibility_permission_enable_allow_button
    private val serviceWarningDenyButtonId =
        com.android.internal.R.id.accessibility_permission_enable_deny_button

    private var fragScenario: FragmentScenario<TestA11yServiceShortcutFragment>? = null
    private var fragment: TestA11yServiceShortcutFragment? = null

    @Before
    fun setUp() {
        setTargetSdk(Build.VERSION_CODES.R)
        setServiceWarningRequired(false)
        a11yManager.setInstalledAccessibilityServiceList(listOf(serviceInfo))
    }

    @After
    fun cleanUp() {
        fragment = null
        fragScenario?.close()
        AccessibilityRepositoryProvider.resetInstanceForTesting()
    }

    @Test
    fun getTitle_returnsCorrectString() {
        val preference = A11yServiceShortcutPreference(context, serviceInfo, 0)

        assertThat(preference.getTitle(context).toString())
            .isEqualTo(
                context.getString(R.string.accessibility_shortcut_title, "StandardA11yService")
            )
    }

    @Test
    fun getFeatureName_returnsCorrectString() {
        val preference = A11yServiceShortcutPreference(context, serviceInfo, 0)

        assertThat(preference.getFeatureName(context).toString()).isEqualTo("StandardA11yService")
    }

    @Test
    fun bindWidget_targetSdkIsAtLeastR_widgetIsEditable() {
        setTargetSdk(Build.VERSION_CODES.R)

        launchFragment()

        assertThat(getShortcutWidget().isSettingsEditable).isTrue()
    }

    @Test
    fun bindWidget_targetSdkLessThanR_widgetIsNotEditable() {
        setTargetSdk(Build.VERSION_CODES.Q)

        launchFragment()

        assertThat(getShortcutWidget().isSettingsEditable).isFalse()
    }

    @Test
    fun clickSettings_serviceWarningRequiredAndShortcutIsOff_showsServiceWarningDialog() {
        setServiceWarningRequired(true)
        setShortcutEnabled(false)
        launchFragment()

        getShortcutWidget().performClick()
        ShadowLooper.idleMainLooper()

        assertServiceWarningDialogShown()
    }

    @Test
    fun clickSettings_serviceWarningRequired_clickAllow_showsEditShortcutsScreen() {
        setServiceWarningRequired(true)
        setShortcutEnabled(false)
        launchFragment()
        getShortcutWidget().performClick()
        ShadowLooper.idleMainLooper()

        clickDialogButton(serviceWarningAllowButtonId)

        assertThat(isEditShortcutsScreenShown()).isTrue()
    }

    @Test
    fun clickSettings_serviceWarningRequired_clickDeny_shortcutStaysOff() {
        setServiceWarningRequired(true)
        setShortcutEnabled(false)
        launchFragment()
        getShortcutWidget().performClick()
        ShadowLooper.idleMainLooper()

        clickDialogButton(serviceWarningDenyButtonId)

        assertThat(getShortcutWidget().isChecked).isFalse()
        assertThat(hasAnyShortcutTargets()).isFalse()
    }

    @Test
    fun clickSettings_serviceWarningNotRequiredAndShortcutIsOff_showsEditShortcutsScreen() {
        setServiceWarningRequired(false)
        setShortcutEnabled(false)
        launchFragment()

        getShortcutWidget().performClick()
        ShadowLooper.idleMainLooper()

        assertThat(isEditShortcutsScreenShown()).isTrue()
    }

    @Test
    fun clickSettings_shortcutIsOn_showsEditShortcutsScreen() {
        setServiceWarningRequired(false)
        setShortcutEnabled(true)
        launchFragment()

        getShortcutWidget().performClick()
        ShadowLooper.idleMainLooper()

        assertThat(isEditShortcutsScreenShown()).isTrue()
    }

    @Test
    fun clickToggle_serviceWarningRequiredAndShortcutIsOff_showsServiceWarningDialog() {
        setServiceWarningRequired(true)
        setShortcutEnabled(false)
        launchFragment()

        getShortcutWidgetToggle().performClick()
        ShadowLooper.idleMainLooper()

        assertServiceWarningDialogShown()
    }

    @Test
    fun clickToggle_serviceWarningRequired_clickAllow_turnsOnShortcut() {
        setServiceWarningRequired(true)
        setShortcutEnabled(false)
        launchFragment()
        getShortcutWidgetToggle().performClick()
        ShadowLooper.idleMainLooper()

        clickDialogButton(serviceWarningAllowButtonId)

        assertThat(hasAnyShortcutTargets()).isTrue()
    }

    @Test
    fun clickToggle_serviceWarningRequired_clickDeny_shortcutStaysOff() {
        setServiceWarningRequired(true)
        setShortcutEnabled(false)
        launchFragment()
        getShortcutWidgetToggle().performClick()
        ShadowLooper.idleMainLooper()

        clickDialogButton(serviceWarningDenyButtonId)

        assertThat(hasAnyShortcutTargets()).isFalse()
    }

    @Test
    fun clickToggle_shortcutIsOn_turnsOffShortcut() {
        setServiceWarningRequired(true)
        setShortcutEnabled(true)
        launchFragment()

        getShortcutWidgetToggle().performClick()
        ShadowLooper.idleMainLooper()

        assertThat(hasAnyShortcutTargets()).isFalse()
    }

    @Test
    fun clickToggle_serviceWarningNotRequiredAndShortcutIsOff_turnsOnShortcut() {
        setServiceWarningRequired(false)
        setShortcutEnabled(false)
        launchFragment()

        getShortcutWidgetToggle().performClick()
        ShadowLooper.idleMainLooper()

        assertThat(hasAnyShortcutTargets()).isTrue()
    }

    @Test
    fun clickToggle_inSetupWizard_turnsOnSoftwareShortcut() {
        setServiceWarningRequired(false)
        setTargetSdk(Build.VERSION_CODES.R)
        setIsAccessibilityTool(true)
        setHasTile(true)

        launchFragment(inSetupWizard = true)
        getShortcutWidgetToggle().performClick()
        ShadowLooper.idleMainLooper()

        assertHasShortcutTarget(SOFTWARE)
    }

    @Test
    fun clickToggle_notInSetupWizard_turnsOnQuickSettingsShortcut() {
        setServiceWarningRequired(false)
        setTargetSdk(Build.VERSION_CODES.R)
        setIsAccessibilityTool(true)
        setHasTile(true)

        launchFragment(inSetupWizard = false)
        getShortcutWidgetToggle().performClick()
        ShadowLooper.idleMainLooper()

        assertHasShortcutTarget(QUICK_SETTINGS)
    }

    @Test
    fun clickToggle_targetSdkLessThanR_turnsOnHardwareShortcut() {
        setServiceWarningRequired(false)
        setTargetSdk(Build.VERSION_CODES.Q)

        launchFragment()
        getShortcutWidgetToggle().performClick()
        ShadowLooper.idleMainLooper()

        assertHasShortcutTarget(HARDWARE)
    }

    private fun setTargetSdk(targetSdk: Int) {
        serviceInfo.resolveInfo.serviceInfo.applicationInfo.targetSdkVersion = targetSdk
    }

    private fun setIsAccessibilityTool(isTool: Boolean) {
        whenever(serviceInfo.isAccessibilityTool) doReturn isTool
    }

    private fun setHasTile(hasTile: Boolean) {
        whenever(serviceInfo.tileServiceName) doReturn (if (hasTile) "SomeTileService" else null)
    }

    private fun setServiceWarningRequired(required: Boolean) {
        if (required) {
            a11yManager.removeAccessibilityServiceWarningExempted(serviceInfo.componentName)
        } else {
            a11yManager.setAccessibilityServiceWarningExempted(serviceInfo.componentName)
        }
    }

    private fun setShortcutEnabled(enabled: Boolean) {
        val targets =
            if (enabled) listOf(serviceInfo.componentName.flattenToString()) else emptyList()
        a11yManager.setAccessibilityShortcutTargets(SOFTWARE, targets)
    }

    private fun launchFragment(inSetupWizard: Boolean = false) {
        setupTestData(serviceInfo)

        fragScenario =
            if (inSetupWizard) {
                    AccessibilityTestUtils.launchFragmentInSetupWizardFlow(
                        TestA11yServiceShortcutFragment::class.java,
                        null,
                    )
                } else {
                    FragmentScenario.launch(
                        TestA11yServiceShortcutFragment::class.java,
                        null,
                        androidx.appcompat.R.style.Theme_AppCompat,
                        null as FragmentFactory?,
                    )
                }
                .moveToState(Lifecycle.State.RESUMED)

        fragScenario?.onFragment { fragment = it }
    }

    private fun getShortcutWidget(): ShortcutPreference =
        checkNotNull(fragment?.findPreference(A11yServiceShortcutPreference.KEY))

    private fun getShortcutWidgetToggle(): View {
        val shortcutPrefWidget = getShortcutWidget()
        val viewHolder =
            AccessibilityTestUtils.inflateShortcutPreferenceView(
                checkNotNull(fragment).requireContext(),
                shortcutPrefWidget,
            )
        return checkNotNull(viewHolder.findViewById(shortcutPrefWidget.switchResId))
    }

    private fun setupTestData(serviceInfo: AccessibilityServiceInfo) {
        PreferenceScreenRegistry.preferenceScreenMetadataFactories =
            FixedArrayMap(1) {
                it.put(FakePreferenceScreen.KEY) { FakePreferenceScreen(serviceInfo) }
            }
    }

    private fun clickDialogButton(buttonId: Int) {
        val dialog = shadowOf(ShadowDialog.getLatestDialog())
        dialog.clickOn(buttonId)
        ShadowLooper.idleMainLooper()
    }

    private fun assertServiceWarningDialogShown() {
        ShadowLooper.idleMainLooper()
        val fragments = checkNotNull(fragment).childFragmentManager.fragments
        assertThat(fragments).hasSize(1)
        assertThat(fragments[0]).isInstanceOf(AccessibilityServiceWarningDialogFragment::class.java)
    }

    private fun isEditShortcutsScreenShown(): Boolean {
        ShadowLooper.idleMainLooper()
        val intent =
            shadowOf(checkNotNull(fragment).context as ContextWrapper).peekNextStartedActivity()
        return intent?.getExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT) ==
            EditShortcutsPreferenceFragment::class.java.name
    }

    private fun hasAnyShortcutTargets(): Boolean =
        USER_SHORTCUT_TYPES.any { a11yManager.getAccessibilityShortcutTargets(it).isNotEmpty() }

    private fun assertHasShortcutTarget(shortcutType: Int) {
        assertThat(a11yManager.getAccessibilityShortcutTargets(shortcutType))
            .contains(serviceInfo.componentName.flattenToString())
    }
}

class TestA11yServiceShortcutFragment : PreferenceFragment() {
    override fun getPreferenceScreenBindingKey(context: Context): String = FakePreferenceScreen.KEY
}

private class FakePreferenceScreen(private val serviceInfo: AccessibilityServiceInfo) :
    PreferenceScreenMixin {
    override val highlightMenuKey = 0

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {
            +A11yServiceShortcutPreference(context, serviceInfo, getMetricsCategory())
        }

    override val key: String = KEY

    override val purpose: Int = 0

    override fun getMetricsCategory(): Int = 0

    companion object {
        const val KEY = "fake_screen"
    }
}
