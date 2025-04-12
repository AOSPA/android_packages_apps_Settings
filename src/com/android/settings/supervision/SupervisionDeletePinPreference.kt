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
package com.android.settings.supervision

import com.android.settings.R
import com.android.settingslib.metadata.PreferenceMetadata

/**
 * Setting on PIN Management screen (Settings > Supervision > Manage Pin) that invokes the flow to
 * delete the device PIN.
 */
class SupervisionDeletePinPreference : PreferenceMetadata {
    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.supervision_delete_pin_preference_title

    override val summary: Int
        get() = R.string.supervision_delete_pin_preference_summary

    // TODO(b/406082832): Implements the delete PIN flow in settings.

    companion object {
        const val KEY = "supervision_delete_pin"
    }
}
