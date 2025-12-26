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

import android.os.Bundle
import com.android.settings.R
import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity

class SatelliteLandingPageActivity : CollapsingToolbarBaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set the title on the collapsing toolbar
        setTitle(R.string.satellite_connectivity_title)

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.content_frame, SatelliteLandingPageFragment())
            .commit()
    }

    override fun onNavigateUp(): Boolean {
        finish() // Closes the activity and returns to the previous screen.
        return true
    }
}
