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

package com.android.settings.network.telephony.satellite.quicksettings

import androidx.fragment.app.testing.launchFragmentInContainer
import com.android.settings.R
import com.android.settingslib.widget.IllustrationPreference
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SatelliteLandingPageFragmentTest {
    @Test
    fun onCreate_loadsPreferencesAndSetsIllustration() {
        val scenario =
            launchFragmentInContainer<SatelliteLandingPageFragment>(
                themeResId = R.style.Theme_Settings
            )
        scenario.onFragment { fragment ->
            val illustrationPreference =
                fragment.findPreference<IllustrationPreference>("illustration")

            // Verify that the preference screen is loaded and the illustration preference exists.
            assertThat(illustrationPreference).isNotNull()
            // Verify that the illustration drawable is set.
            assertThat(illustrationPreference?.getImageDrawable()).isNotNull()
        }
    }
}
