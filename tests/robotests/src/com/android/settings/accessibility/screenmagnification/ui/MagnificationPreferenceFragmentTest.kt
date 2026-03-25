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

package com.android.settings.accessibility.screenmagnification.ui

import android.app.settings.SettingsEnums
import android.content.ComponentName
import android.content.Intent
import android.provider.Settings
import android.view.InputDevice
import android.view.View
import androidx.fragment.app.FragmentFactory
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.FragmentScenario.FragmentAction
import androidx.lifecycle.Lifecycle
import androidx.preference.Preference
import com.android.internal.accessibility.AccessibilityShortcutController
import com.android.settings.R
import com.android.settings.accessibility.BaseShortcutInteractionsTestCases
import com.android.settings.accessibility.MagnificationCapabilities
import com.android.settings.accessibility.MagnificationCapabilities.MagnificationMode
import com.android.settings.accessibility.ShortcutPreference
import com.android.settings.accessibility.screenmagnification.dialogs.CursorFollowingModeChooser
import com.android.settings.accessibility.screenmagnification.dialogs.MagnificationModeChooser
import com.android.settings.testutils.AccessibilityTestUtils.assertDialogShown
import com.android.settings.testutils.AccessibilityTestUtils.launchFragmentInSetupWizardFlow
import com.android.settings.testutils.inflateViewHolder
import com.android.settings.testutils.shadow.SettingsShadowResources
import com.android.settings.testutils.shadow.ShadowInputDevice
import com.android.settingslib.widget.preference.footer.R as FooterPrefR
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@Config(shadows = [ShadowInputDevice::class, SettingsShadowResources::class])
@RunWith(RobolectricTestRunner::class)
class MagnificationPreferenceFragmentTest :
    BaseShortcutInteractionsTestCases<MagnificationPreferenceFragment>() {
    private var fragScenario: FragmentScenario<MagnificationPreferenceFragment>? = null
    private var fragment: MagnificationPreferenceFragment? = null

    @After
    fun cleanUp() {
        fragScenario?.close()
        ShadowInputDevice.reset()
    }

    @Test
    fun clickModePreference_showModeChooserDialog() {
        val fragment = launchFragment()
        val modePref: Preference? = fragment.findPreference(MAGNIFICATION_MODE_PREF_KEY)
        requireNotNull(modePref)

        modePref.inflateViewHolder().itemView.performClick()
        ShadowLooper.idleMainLooper()

        assertDialogShown(fragment, MagnificationModeChooser::class.java)
    }

    @Test
    fun clickCursorFollowingModePreference_showDedicatedDialog() {
        // Setup cursor following mode enabled
        val device =
            ShadowInputDevice.makeInputDevicebyIdWithSources(/* id= */ 1, InputDevice.SOURCE_MOUSE)
        ShadowInputDevice.addDevice(device.id, device)
        MagnificationCapabilities.setCapabilities(context, MagnificationMode.FULLSCREEN)

        val fragment = launchFragment()
        val modePref: Preference? = fragment.findPreference(CURSOR_FOLLOWING_MODE_PREF_KEY)
        requireNotNull(modePref)

        modePref.inflateViewHolder().itemView.performClick()
        ShadowLooper.idleMainLooper()

        assertDialogShown(fragment, CursorFollowingModeChooser::class.java)
    }

    @Test
    fun getMetricsCategory_returnsCorrectCategory() {
        val fragment = launchFragment()

        assertThat(fragment.metricsCategory)
            .isEqualTo(SettingsEnums.ACCESSIBILITY_TOGGLE_SCREEN_MAGNIFICATION)
    }

    @Test
    fun getHelpResource_returnsCorrectHelpResource() {
        val fragment = launchFragment()

        assertThat(fragment.helpResource).isEqualTo(R.string.help_url_magnification)
    }

    @Test
    fun launchFragment_helpUrlResolvable_learnMoreTextIsVisible() {
        setupHelpUri(isResolvable = true)

        val fragment = launchFragment()

        assertThat(isLearnMoreTextVisible(fragment)).isTrue()
    }

    @Test
    fun launchFragment_helpUrlNotResolvable_learnMoreTextIsNotVisible() {
        setupHelpUri(isResolvable = false)

        val fragment = launchFragment()

        assertThat(isLearnMoreTextVisible(fragment)).isFalse()
    }

    @Test
    fun launchFragmentInSetupWizard_helpUrlResolvable_learnMoreTextIsNotVisible() {
        setupHelpUri(isResolvable = true)

        launchFragmentInSetupWizardFlow(MagnificationPreferenceFragment::class.java, null).use {
            it.moveToState(Lifecycle.State.RESUMED).onFragment {
                frag: MagnificationPreferenceFragment? ->
                fragment = frag
            }
            assertThat(fragment).isNotNull()

            assertThat(isLearnMoreTextVisible(fragment!!)).isFalse()
        }
    }

    override fun getShortcutToggle(): ShortcutPreference? {
        return fragment?.findPreference(SHORTCUT_PREF_KEY)
    }

    override fun launchFragment(): MagnificationPreferenceFragment {
        fragScenario =
            FragmentScenario.launch(
                    MagnificationPreferenceFragment::class.java,
                    /* fragmentArgs= */ null,
                    androidx.appcompat.R.style.Theme_AppCompat,
                    null as FragmentFactory?,
                )
                .moveToState(Lifecycle.State.RESUMED)
        fragScenario!!.onFragment(
            FragmentAction { frag: MagnificationPreferenceFragment? -> fragment = frag }
        )
        return fragment!!
    }

    override fun getFeatureComponent(): ComponentName {
        return AccessibilityShortcutController.MAGNIFICATION_COMPONENT_NAME
    }

    override fun getFeatureComponentString(): String {
        return AccessibilityShortcutController.MAGNIFICATION_CONTROLLER_NAME
    }

    private fun isLearnMoreTextVisible(fragment: MagnificationPreferenceFragment): Boolean {
        val footerPreference =
            fragment.findPreference<Preference>(MagnificationFooterPreference.KEY)
        val view =
            footerPreference
                ?.inflateViewHolder()
                ?.findViewById(FooterPrefR.id.settingslib_learn_more)

        return view?.visibility == View.VISIBLE
    }

    private fun setupHelpUri(isResolvable: Boolean) {
        // If the device is not provisioned, the HelperUtils#getHelpIntent would return null
        Settings.Global.putInt(context.contentResolver, Settings.Global.DEVICE_PROVISIONED, 1)
        if (isResolvable) {
            // Use the Uri that is resolvable by the Settings app to reduce extra test setup
            SettingsShadowResources.overrideResource(
                R.string.help_url_magnification,
                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).toUri(Intent.URI_INTENT_SCHEME),
            )
        } else {
            SettingsShadowResources.overrideResource(R.string.help_url_magnification, "")
        }
    }

    companion object {
        private const val SHORTCUT_PREF_KEY = "magnification_shortcut_preference"
        private const val MAGNIFICATION_MODE_PREF_KEY = "accessibility_magnification_capability"
        private const val CURSOR_FOLLOWING_MODE_PREF_KEY =
            "accessibility_magnification_cursor_following_mode"
    }
}
