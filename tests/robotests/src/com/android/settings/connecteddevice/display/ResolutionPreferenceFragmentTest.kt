/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
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

import android.content.Context
import android.content.res.Resources
import android.view.Display.INVALID_DISPLAY
import android.view.View
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceScreen
import androidx.test.annotation.UiThreadTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.connecteddevice.display.ResolutionPreferenceFragment.DISPLAY_MODE_LIMIT_OVERRIDE_PROP
import com.android.settings.connecteddevice.display.ResolutionPreferenceFragment.EXTERNAL_DISPLAY_RESOLUTION_SETTINGS_RESOURCE
import com.android.settings.connecteddevice.display.ResolutionPreferenceFragment.MORE_OPTIONS_KEY
import com.android.settings.connecteddevice.display.ResolutionPreferenceFragment.TOP_OPTIONS_KEY
import com.android.settingslib.widget.SelectorWithWidgetPreference
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

/** Unit tests for [ResolutionPreferenceFragment]. */
@RunWith(AndroidJUnit4::class)
class ResolutionPreferenceFragmentTest : ExternalDisplayTestBase() {

    private lateinit var fragment: ResolutionPreferenceFragment
    @Mock private lateinit var mockedMetricsLogger: MetricsLogger

    private var preferenceIdFromResource: Int = 0
    private var displayId: Int = INVALID_DISPLAY

    @Test
    @UiThreadTest
    fun testCreateAndStart() {
        initFragment()
        mHandler.flush()
        assertThat(preferenceIdFromResource)
            .isEqualTo(EXTERNAL_DISPLAY_RESOLUTION_SETTINGS_RESOURCE)
        var pref = mPreferenceScreen.findPreference<Preference>(TOP_OPTIONS_KEY)
        assertThat(pref).isNull()
        pref = mPreferenceScreen.findPreference(MORE_OPTIONS_KEY)
        assertThat(pref).isNull()
    }

    @Test
    @UiThreadTest
    fun testCreateAndStartDefaultDisplayNotAllowed() {
        displayId = 0
        initFragment()
        mHandler.flush()
        var pref = mPreferenceScreen.findPreference<Preference>(TOP_OPTIONS_KEY)
        assertThat(pref).isNull()
        pref = mPreferenceScreen.findPreference(MORE_OPTIONS_KEY)
        assertThat(pref).isNull()
    }

    @Test
    @UiThreadTest
    fun testModePreferences_modeLimitFlagIsOn_noOverride() {
        doReturn(true).`when`(mMockedInjector).isModeLimitForExternalDisplayEnabled()
        doReturn(null).`when`(mMockedInjector).getSystemProperty(DISPLAY_MODE_LIMIT_OVERRIDE_PROP)
        val (topPref, morePref) = runTestModePreferences()
        assertThat(topPref.preferenceCount).isEqualTo(3)
        assertThat(morePref.preferenceCount).isEqualTo(1)
    }

    @Test
    @UiThreadTest
    fun testModePreferences_noModeLimitFlag_overrideIsTrue() {
        doReturn(false).`when`(mMockedInjector).isModeLimitForExternalDisplayEnabled()
        doReturn("true").`when`(mMockedInjector).getSystemProperty(DISPLAY_MODE_LIMIT_OVERRIDE_PROP)
        val (topPref, morePref) = runTestModePreferences()
        assertThat(topPref.preferenceCount).isEqualTo(3)
        assertThat(morePref.preferenceCount).isEqualTo(1)
    }

    @Test
    @UiThreadTest
    fun testModePreferences_noModeLimitFlag_noOverride() {
        doReturn(false).`when`(mMockedInjector).isModeLimitForExternalDisplayEnabled()
        doReturn(null).`when`(mMockedInjector).getSystemProperty(DISPLAY_MODE_LIMIT_OVERRIDE_PROP)
        val (topPref, morePref) = runTestModePreferences()
        assertThat(topPref.preferenceCount).isEqualTo(3)
        assertThat(morePref.preferenceCount).isEqualTo(2)
    }

    @Test
    @UiThreadTest
    fun testModePreferences_modeLimitFlagIsOn_butOverrideIsFalse() {
        doReturn(true).`when`(mMockedInjector).isModeLimitForExternalDisplayEnabled()
        doReturn("false")
            .`when`(mMockedInjector)
            .getSystemProperty(DISPLAY_MODE_LIMIT_OVERRIDE_PROP)
        val (topPref, morePref) = runTestModePreferences()
        assertThat(topPref.preferenceCount).isEqualTo(3)
        assertThat(morePref.preferenceCount).isEqualTo(2)
    }

    @Test
    @UiThreadTest
    fun testModeChange() {
        val display = mDisplays[0]
        displayId = display.id
        initFragment()
        mHandler.flush()
        val topPref = mPreferenceScreen.findPreference<PreferenceCategory>(TOP_OPTIONS_KEY)
        assertThat(topPref).isNotNull()
        val modePref = topPref!!.getPreference(1) as SelectorWithWidgetPreference
        modePref.onClick()
        val mode = display.supportedModes[1]
        verify(mMockedInjector).setUserPreferredDisplayMode(displayId, mode)
    }

    private fun runTestModePreferences(): Pair<PreferenceCategory, PreferenceCategory> {
        displayId = 1
        initFragment()
        mHandler.flush()
        val topPref = mPreferenceScreen.findPreference<PreferenceCategory>(TOP_OPTIONS_KEY)
        assertThat(topPref).isNotNull()
        val morePref = mPreferenceScreen.findPreference<PreferenceCategory>(MORE_OPTIONS_KEY)
        assertThat(morePref).isNotNull()
        return Pair(topPref!!, morePref!!)
    }

    private fun initFragment() {
        if (::fragment.isInitialized) {
            return
        }
        fragment = TestableResolutionPreferenceFragment()
        fragment.onCreateCallback(null)
        fragment.onActivityCreatedCallback(null)
        fragment.onStartCallback()
    }

    private inner class TestableResolutionPreferenceFragment :
        ResolutionPreferenceFragment(mMockedInjector) {
        private val mockRootView: View
        private val emptyView: TextView
        private val mockResources: Resources = mock(Resources::class.java)
        private val logger: MetricsLogger

        init {
            doReturn(61)
                .`when`(mockResources)
                .getInteger(com.android.internal.R.integer.config_externalDisplayPeakRefreshRate)
            doReturn(1920)
                .`when`(mockResources)
                .getInteger(com.android.internal.R.integer.config_externalDisplayPeakWidth)
            doReturn(1080)
                .`when`(mockResources)
                .getInteger(com.android.internal.R.integer.config_externalDisplayPeakHeight)
            doReturn(true)
                .`when`(mockResources)
                .getBoolean(com.android.internal.R.bool.config_refreshRateSynchronizationEnabled)
            mockRootView = mock(View::class.java)
            emptyView = TextView(mContext)
            doReturn(emptyView).`when`(mockRootView).findViewById<View>(android.R.id.empty)
            logger = mockedMetricsLogger
        }

        override fun getPreferenceScreen(): PreferenceScreen {
            return super@ResolutionPreferenceFragmentTest.mPreferenceScreen
        }

        override fun getView(): View {
            return mockRootView
        }

        override fun setEmptyView(view: View?) {
            assertThat(view).isEqualTo(emptyView)
        }

        override fun getEmptyView(): View {
            return emptyView
        }

        override fun addPreferencesFromResource(resource: Int) {
            preferenceIdFromResource = resource
        }

        override fun getDisplayIdArg(): Int {
            return displayId
        }

        override fun writePreferenceClickMetric(preference: Preference) {
            logger.writePreferenceClickMetric(preference)
        }

        override fun getResources(context: Context): Resources {
            return mockResources
        }
    }

    /** Interface allowing to mock and spy on log events. */
    interface MetricsLogger {
        /** On preference click metric */
        fun writePreferenceClickMetric(preference: Preference)
    }
}
