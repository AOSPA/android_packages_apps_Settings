/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.connecteddevice.display

import android.app.Application
import android.content.Context
import android.provider.Settings
import android.view.Display.DEFAULT_DISPLAY
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceScreen
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.connecteddevice.display.SelectedDisplayPreferenceFragment.PrefInfo
import com.android.settings.testutils.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.doReturn

/** Unit tests for [SelectedDisplayPreferenceFragment]. */
@RunWith(AndroidJUnit4::class)
class SelectedDisplayPreferenceFragmentTest : ExternalDisplayTestBase() {
    // Rule to execute LiveData operations synchronously
    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var application: Application
    private lateinit var viewModel: DisplayPreferenceViewModel
    private lateinit var fragment: TestableSelectedDisplayPreferenceFragment

    @Before
    override fun setUp() {
        super.setUp()
        application = ApplicationProvider.getApplicationContext() as Application

        viewModel = DisplayPreferenceViewModel(application, mMockedInjector)
        setMirroringMode(false)

        fragment = initFragment()
    }

    @Test
    fun testNoDisplays_noPreferenceShown() {
        doReturn(emptyList<DisplayDevice>()).`when`(mMockedInjector).getDisplays()

        viewModel.updateEnabledDisplays()

        val category = mPreferenceScreen.getPreference(0) as PreferenceCategory
        assertThat(category.title).isEqualTo("")

        // Verify all other preferences are hidden
        for (i in 0 until category.preferenceCount) {
            val pref = category.getPreference(i)
            assertFalse(pref.isVisible)
        }
    }

    @Test
    fun testDefaultDisplaySelected_showsBuiltInPreferences() {
        includeBuiltinDisplay()
        viewModel.updateEnabledDisplays()
        viewModel.updateSelectedDisplay(DEFAULT_DISPLAY)

        val category = mPreferenceScreen.getPreference(0) as PreferenceCategory
        assertVisible(category, PrefInfo.DISPLAY_MIRRORING.key, true)
        assertVisible(category, PrefInfo.DISPLAY_SIZE_AND_TEXT.key, true)
        assertVisible(category, PrefInfo.DISPLAY_SIZE.key, false)
        assertVisible(category, PrefInfo.DISPLAY_RESOLUTION.key, false)
        assertVisible(category, PrefInfo.DISPLAY_ROTATION.key, false)
    }

    @Test
    fun testDefaultDisplaySelected_launchingDefaultDisplaySettings() {
        includeBuiltinDisplay()
        viewModel.updateEnabledDisplays()
        viewModel.updateSelectedDisplay(DEFAULT_DISPLAY)

        val category = mPreferenceScreen.getPreference(0) as PreferenceCategory
        val sizeAndTextPref =
            category.findPreference<Preference>(PrefInfo.DISPLAY_SIZE_AND_TEXT.key)
        sizeAndTextPref!!.onPreferenceClickListener!!.onPreferenceClick(sizeAndTextPref)

        assertTrue(fragment.isBuiltinDisplaySettingsLaunched)
    }

    @Test
    fun testExternalDisplaySelected_showsExternalPreferences() {
        val display = mDisplays.first { it.id == EXTERNAL_DISPLAY_ID }

        viewModel.updateSelectedDisplay(display.id)

        val category = mPreferenceScreen.getPreference(0) as PreferenceCategory
        assertVisible(category, PrefInfo.DISPLAY_MIRRORING.key, false)
        assertVisible(category, PrefInfo.DISPLAY_SIZE_AND_TEXT.key, false)
        assertVisible(category, PrefInfo.DISPLAY_SIZE.key, true)
        assertVisible(category, PrefInfo.DISPLAY_RESOLUTION.key, true)
        assertVisible(category, PrefInfo.DISPLAY_ROTATION.key, true)
    }

    @Test
    fun testExternalDisplaySelected_isMirroring_hideDisplaySizePreference() {
        val display = mDisplays.first { it.id == EXTERNAL_DISPLAY_ID }

        setMirroringMode(true)
        viewModel.updateSelectedDisplay(display.id)

        val category = mPreferenceScreen.getPreference(0) as PreferenceCategory
        assertVisible(category, PrefInfo.DISPLAY_MIRRORING.key, false)
        assertVisible(category, PrefInfo.DISPLAY_SIZE_AND_TEXT.key, false)
        assertVisible(category, PrefInfo.DISPLAY_SIZE.key, false)
        assertVisible(category, PrefInfo.DISPLAY_RESOLUTION.key, true)
        assertVisible(category, PrefInfo.DISPLAY_ROTATION.key, true)
    }

    @Test
    fun testExternalDisplaySelected_launchingResolutionSelector() {
        val display = mDisplays.first { it.id == EXTERNAL_DISPLAY_ID }

        viewModel.updateSelectedDisplay(display.id)
        val category = mPreferenceScreen.getPreference(0) as PreferenceCategory
        val resolutionPref = category.findPreference<Preference>(PrefInfo.DISPLAY_RESOLUTION.key)!!
        resolutionPref.onPreferenceClickListener!!.onPreferenceClick(resolutionPref)

        assertThat(resolutionPref.summary.toString())
            .isEqualTo("${display.mode?.physicalWidth} x ${display.mode?.physicalHeight}")
        assertThat(fragment.writtenMetricsPreference).isEqualTo(resolutionPref)
        assertThat(fragment.resolutionSelectorLaunchDisplayId).isEqualTo(EXTERNAL_DISPLAY_ID)
    }

    @Test
    fun testExternalDisplaySelected_updatesRotationPreference() {
        val display = mDisplays.first { it.id == EXTERNAL_DISPLAY_ID }
        doReturn(true).`when`(mMockedInjector).freezeDisplayRotation(display.id, 1)

        viewModel.updateSelectedDisplay(display.id)
        val category = mPreferenceScreen.getPreference(0) as PreferenceCategory
        val rotationPref = category.findPreference<ListPreference>(PrefInfo.DISPLAY_ROTATION.key)!!
        val initialValue = rotationPref.value
        rotationPref.onPreferenceChangeListener!!.onPreferenceChange(rotationPref, "1")

        assertThat(initialValue).isEqualTo("0")
        assertThat(fragment.writtenMetricsPreference).isEqualTo(rotationPref)
        verify(mMockedInjector).freezeDisplayRotation(display.id, 1)
        assertThat(rotationPref.value).isEqualTo("1")
    }

    private fun setMirroringMode(enable: Boolean) {
        Settings.Secure.putInt(
            application.contentResolver,
            Settings.Secure.MIRROR_BUILT_IN_DISPLAY,
            if (enable) 1 else 0,
        )
        viewModel.mirrorModeObserver.onChange(/* selfChange= */ false)
    }

    private fun assertVisible(category: PreferenceCategory, key: String, isVisible: Boolean) {
        assertThat(category.findPreference<Preference>(key)!!.isVisible).isEqualTo(isVisible)
    }

    private fun initFragment(): TestableSelectedDisplayPreferenceFragment {
        val fragment =
            TestableSelectedDisplayPreferenceFragment(mContext, mPreferenceScreen, viewModel)
        fragment.onCreateCallback(null)
        fragment.onActivityCreatedCallback(null)
        fragment.onStartCallback()
        return fragment
    }

    class TestableSelectedDisplayPreferenceFragment(
        private val context: Context,
        private val preferenceScreen: PreferenceScreen,
        viewModel: DisplayPreferenceViewModel,
    ) : SelectedDisplayPreferenceFragment(viewModel) {
        var resolutionSelectorLaunchDisplayId = -123
        var isBuiltinDisplaySettingsLaunched = false
        var writtenMetricsPreference: Preference? = null
        private val mockViewLifecycleOwner = mock(LifecycleOwner::class.java)

        val lifecycleRegistry = LifecycleRegistry(this)

        init {
            doReturn(lifecycleRegistry).`when`(mockViewLifecycleOwner).lifecycle
            // Required to allow observer to start observing data
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }

        override fun getContext(): Context {
            return context
        }

        override fun getViewLifecycleOwner(): LifecycleOwner {
            return mockViewLifecycleOwner
        }

        override fun addPreferencesFromResource(preferencesResId: Int) {
            // No-op
        }

        override fun getPreferenceScreen(): PreferenceScreen {
            return preferenceScreen
        }

        override fun writePreferenceClickMetric(preference: Preference?) {
            writtenMetricsPreference = preference
        }

        override fun launchResolutionSelector(displayId: Int) {
            resolutionSelectorLaunchDisplayId = displayId
        }

        override fun launchBuiltinDisplaySettings() {
            isBuiltinDisplaySettingsLaunched = true
        }
    }
}
