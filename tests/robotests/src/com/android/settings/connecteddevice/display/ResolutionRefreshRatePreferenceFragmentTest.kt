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

import android.app.Application
import android.os.Bundle
import android.os.Looper
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.preference.PreferenceCategory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.connecteddevice.display.ResolutionRefreshRatePreferenceFragment.Companion.DISPLAY_ID_ARG
import com.android.settings.connecteddevice.display.ResolutionRefreshRatePreferenceFragment.Companion.MORE_OPTIONS_KEY
import com.android.settings.connecteddevice.display.ResolutionRefreshRatePreferenceFragment.Companion.REFRESH_RATE_OPTIONS_KEY
import com.android.settings.connecteddevice.display.ResolutionRefreshRatePreferenceFragment.Companion.TOP_OPTIONS_KEY
import com.android.settings.testutils.InstantTaskExecutorRule
import com.android.settingslib.widget.SelectorWithWidgetPreference
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.whenever
import org.robolectric.Shadows.shadowOf

/** Unit tests for [ResolutionRefreshRatePreferenceFragment] */
@RunWith(AndroidJUnit4::class)
class ResolutionRefreshRatePreferenceFragmentTest : ExternalDisplayTestBase() {

    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var scenario: FragmentScenario<ResolutionRefreshRatePreferenceFragment>
    private lateinit var viewModel: ResolutionRefreshRatePreferenceViewModel
    private lateinit var application: Application

    private val externalDisplay by lazy { mDisplays.find { it.id == EXTERNAL_DISPLAY_ID }!! }

    @Before
    override fun setUp() {
        super.setUp()
        application = ApplicationProvider.getApplicationContext()

        viewModel =
            ResolutionRefreshRatePreferenceViewModel(
                application,
                EXTERNAL_DISPLAY_ID,
                mMockedInjector,
            )
    }

    private fun launchFragment() {
        val args = Bundle().apply { putInt(DISPLAY_ID_ARG, EXTERNAL_DISPLAY_ID) }
        scenario =
            launchFragmentInContainer(
                fragmentArgs = args,
                themeResId = R.style.Theme_Settings_Home,
            ) {
                ResolutionRefreshRatePreferenceFragment(viewModel)
            }
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun initialState_rendersCorrectPreferences() {
        launchFragment()

        scenario.onFragment { fragment ->
            val topCategory =
                fragment.preferenceScreen.findPreference<PreferenceCategory>(TOP_OPTIONS_KEY)!!
            val moreCategory =
                fragment.preferenceScreen.findPreference<PreferenceCategory>(MORE_OPTIONS_KEY)!!
            val refreshRateCategory =
                fragment.preferenceScreen.findPreference<PreferenceCategory>(
                    REFRESH_RATE_OPTIONS_KEY
                )!!

            assertThat(topCategory.preferenceCount).isEqualTo(3)
            assertThat(moreCategory.preferenceCount).isEqualTo(4)

            val initialResPref = topCategory.getPreference(1) as SelectorWithWidgetPreference
            assertThat(initialResPref.key).isEqualTo("1920x1080")
            assertThat(initialResPref.isChecked).isTrue()

            assertThat(refreshRateCategory.preferenceCount).isEqualTo(1)
            val initialRefreshPref =
                refreshRateCategory.getPreference(0) as SelectorWithWidgetPreference
            assertThat(initialRefreshPref.title.toString()).contains("60.00 Hz")
            assertThat(initialRefreshPref.isChecked).isTrue()
        }
    }

    @Test
    fun initialState_whenSelectedModeIsInMoreList_expandsMoreOptionsByDefault() {
        // Choose resolution that is not in the top 3 resolutions
        val width = 320
        val height = 240
        val mode =
            externalDisplay.supportedModes.find {
                it.physicalWidth == width && it.physicalHeight == height
            }!!
        val displayWithDifferentInitialMode =
            DisplayDevice(
                id = externalDisplay.id,
                uniqueId = externalDisplay.uniqueId,
                name = externalDisplay.name,
                mode = mode,
                supportedModes = externalDisplay.supportedModes,
                isEnabled = externalDisplay.isEnabled,
                isConnectedDisplay = externalDisplay.isConnectedDisplay,
                rotation = externalDisplay.rotation,
            )
        whenever(mMockedInjector.getDisplay(EXTERNAL_DISPLAY_ID))
            .thenReturn(displayWithDifferentInitialMode)
        mListener.update(EXTERNAL_DISPLAY_ID)

        launchFragment()

        // Verify it's initially expanded
        assertThat(viewModel.uiState.value?.areMoreOptionsExpanded).isTrue()
        scenario.onFragment { fragment ->
            val moreCategory =
                fragment.preferenceScreen.findPreference<PreferenceCategory>(MORE_OPTIONS_KEY)!!
            assertThat(moreCategory.initialExpandedChildrenCount).isEqualTo(Integer.MAX_VALUE)
        }
    }

    @Test
    fun displayDisconnected_finishesActivity() {
        launchFragment()
        whenever(mMockedInjector.getDisplay(EXTERNAL_DISPLAY_ID)).thenReturn(null)
        mListener.update(EXTERNAL_DISPLAY_ID)
        shadowOf(Looper.getMainLooper()).idle()

        scenario.onFragment { fragment ->
            assertThat(fragment.requireActivity().isFinishing).isTrue()
        }
    }

    @Test
    fun onResolutionClick_updatesStateAndRendersNewSelection() {
        launchFragment()

        // Selecting resolution with multiple refresh rates
        scenario.onFragment { fragment ->
            val moreCategory =
                fragment.preferenceScreen.findPreference<PreferenceCategory>(MORE_OPTIONS_KEY)!!
            val prefToClick = moreCategory.findPreference<SelectorWithWidgetPreference>("640x480")!!
            prefToClick.performClick()
        }
        shadowOf(Looper.getMainLooper()).idle()

        // Highest refresh rate for 640x480 should be selected (60Hz)
        val highestRefreshRateMode = externalDisplay.supportedModes.find { it.modeId == 3 }
        assertThat(viewModel.uiState.value?.pendingMode).isEqualTo(highestRefreshRateMode)

        scenario.onFragment { fragment ->
            val refreshRateCategory =
                fragment.preferenceScreen.findPreference<PreferenceCategory>(
                    REFRESH_RATE_OPTIONS_KEY
                )!!
            assertThat(refreshRateCategory.preferenceCount).isEqualTo(2)
            val selectedRefreshPref =
                refreshRateCategory.getPreference(0) as SelectorWithWidgetPreference
            assertThat(selectedRefreshPref.title.toString()).contains("60.00 Hz")
            assertThat(selectedRefreshPref.isChecked).isTrue()
        }
    }

    @Test
    fun onRefreshRateClick_updatesStateAndRendersNewSelection() {
        launchFragment()
        // Selecting resolution with multiple refresh rates
        scenario.onFragment { f ->
            f.preferenceScreen
                .findPreference<SelectorWithWidgetPreference>("640x480")!!
                .performClick()
        }
        shadowOf(Looper.getMainLooper()).idle()

        // Select other refresh rate (640x480, 50Hz)
        val refreshRateModeId = 4
        scenario.onFragment { fragment ->
            val refreshRateCategory =
                fragment.preferenceScreen.findPreference<PreferenceCategory>(
                    REFRESH_RATE_OPTIONS_KEY
                )!!
            val prefToClick =
                refreshRateCategory.findPreference<SelectorWithWidgetPreference>(
                    refreshRateModeId.toString()
                )!!
            prefToClick.performClick()
        }
        shadowOf(Looper.getMainLooper()).idle()

        val modeAt50Hz = externalDisplay.supportedModes.find { it.modeId == refreshRateModeId }
        assertThat(viewModel.uiState.value?.pendingMode).isEqualTo(modeAt50Hz)
    }
}
