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

package com.android.settings.sound

import android.app.settings.SettingsEnums
import android.content.Context
import com.android.settings.R
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.flags.Flags
import com.android.settings.sound.MediaControlsSwitchPreference.Companion.mediaControlsDataStore
import com.android.settingslib.datastore.AbstractKeyedDataObservable
import com.android.settingslib.datastore.HandlerExecutor
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.metadata.PreferenceChangeReason
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy

// LINT.IfChange
@ProvidePreferenceScreen(MediaControlsScreen.KEY)
open class MediaControlsScreen(context: Context) :
    AbstractKeyedDataObservable<String>(), PreferenceScreenMixin, PreferenceSummaryProvider {

    private val observer =
        KeyedObserver<String> { _, _ -> notifyChange(KEY, PreferenceChangeReason.STATE) }

    private val mediaControlsStore = context.mediaControlsDataStore

    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.media_controls_title

    override val keywords: Int
        get() = R.string.keywords_media_controls

    override val highlightMenuKey: Int
        get() = R.string.keywords_sounds

    override fun getMetricsCategory() = SettingsEnums.MEDIA_CONTROLS_SETTINGS

    override fun onFirstObserverAdded() {
        mediaControlsStore.addObserver(
            MediaControlsSwitchPreference.KEY,
            observer,
            HandlerExecutor.main,
        )
    }

    override fun onLastObserverRemoved() {
        mediaControlsStore.removeObserver(MediaControlsSwitchPreference.KEY, observer)
    }

    override fun isFlagEnabled(context: Context) = Flags.catalystMediaControls()

    override fun fragmentClass() = MediaControlsSettings::class.java

    override fun getPreferenceHierarchy(context: Context) =
        preferenceHierarchy(context, this) {
            +MediaControlsSwitchPreference(mediaControlsStore)
            +MediaControlsLockscreenSwitchPreference()
            if (Flags.deviceSuggestionsPreference()) {
                +SuggestionsPreference()
            }
        }

    override fun getSummary(context: Context): CharSequence? =
        if (mediaControlsStore.getBoolean(MediaControlsSwitchPreference.KEY) == false) {
            context.getString(R.string.media_controls_hide_player)
        } else {
            context.getString(R.string.media_controls_show_player)
        }

    companion object {
        const val KEY = "media_controls"
    }
}
// LINT.ThenChange(MediaControlsSettings.java)
