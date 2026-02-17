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

package com.android.settings.core

import android.content.Context
import android.content.Intent
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.android.settings.CatalystFragment
import com.android.settings.utils.makeLaunchpadIntent
import com.android.settingslib.core.instrumentation.Instrumentable
import com.android.settingslib.metadata.CatalystFlagProviderFactory
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.preference.PreferenceScreenCreator

/** Mixin for settings preference screen. */
interface PreferenceScreenMixin : PreferenceScreenCreator, Instrumentable {

    override val indexable
        get() = false

    override fun fragmentClass(): Class<out Fragment>? = CatalystFragment::class.java

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?): Intent? {
        return if (CatalystFlagProviderFactory.catalystUseKeyParameters() && keyParameters != null) {
            makeLaunchpadIntent(context, key, keyParameters!!, metadata?.key)
        } else if (arguments != null) {
            makeLaunchpadIntent(context, key, arguments!!, metadata?.key)
        } else {
            makeLaunchpadIntent(context, key, metadata?.key)
        }
    }

    /**
     * A string resource indicates the menu item that should be highlighted on settings home menu.
     */
    @get:StringRes val highlightMenuKey: Int
}
