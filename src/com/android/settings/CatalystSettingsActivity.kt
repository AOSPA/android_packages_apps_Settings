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

package com.android.settings

import android.content.Intent
import android.os.Bundle
import com.android.settingslib.metadata.EXTRA_BINDING_SCREEN_KEY
import com.android.settingslib.preference.PreferenceFragment

/**
 * Activity to load catalyst preference screen.
 *
 * @param bindingScreenKey preference screen key
 * @param fragmentClass fragment class to load the preference screen
 */
open class CatalystSettingsActivity
@JvmOverloads
constructor(
    private val bindingScreenKey: String,
    private val fragmentClass: Class<out PreferenceFragment> = CatalystFragment::class.java,
) : SettingsActivity() {

    override fun isValidFragment(fragmentName: String?) = fragmentName == fragmentClass.name

    override fun getInitialFragmentName(intent: Intent?): String = fragmentClass.name

    override fun getInitialFragmentArguments(intent: Intent?): Bundle? =
        Bundle().apply { putString(EXTRA_BINDING_SCREEN_KEY, bindingScreenKey) }
}

/**
 * Fragment to load catalyst preference screen.
 *
 * `PreferenceFragment` class is not used as it does not support highlighting specific preference.
 */
class CatalystFragment : SettingsPreferenceFragment() {

    override fun getMetricsCategory() = 0

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceScreen = createPreferenceScreen()
    }
}
