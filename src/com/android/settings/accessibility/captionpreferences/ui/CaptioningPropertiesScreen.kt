/*
 * Copyright (C) 2026 The Android Open Source Project
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

package com.android.settings.accessibility.captionpreferences.ui

import android.app.settings.SettingsEnums
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.provider.Settings.ACTION_CAPTIONING_SETTINGS
import androidx.fragment.app.Fragment
import com.android.settings.R
import com.android.settings.accessibility.AccessibilityUtil
import com.android.settings.accessibility.CaptioningPropertiesFragment
import com.android.settings.accessibility.Flags
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.utils.highlightPreference
import com.android.settingslib.datastore.HandlerExecutor
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import kotlinx.coroutines.CoroutineScope

/**
 * Screens that allow users to customize the visual appearance—such as text size, font style, color
 * and language—of captions for apps that support system-wide accessibility settings.
 */
@ProvidePreferenceScreen(CaptioningPropertiesScreen.KEY)
open class CaptioningPropertiesScreen :
    PreferenceScreenMixin, PreferenceLifecycleProvider, PreferenceSummaryProvider {
    override val highlightMenuKey: Int
        get() = R.string.menu_key_accessibility

    override val key: String
        get() = KEY

    override val purpose: Int
        get() = R.string.caption_preferences_screen_purpose

    override val title: Int
        get() = R.string.accessibility_captioning_title

    override val icon: Int
        get() = R.drawable.ic_captioning

    override val indexable: Boolean
        get() = true

    private var captionEnabledObserver: KeyedObserver<String>? = null

    override fun getMetricsCategory(): Int = SettingsEnums.ACCESSIBILITY_CAPTION_PROPERTIES

    override fun fragmentClass(): Class<out Fragment>? = CaptioningPropertiesFragment::class.java

    override fun isFlagEnabled(context: Context): Boolean = Flags.catalystCaptionPreferencesScreen()

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        Intent(ACTION_CAPTIONING_SETTINGS).apply { highlightPreference(metadata?.key) }

    override fun getSummary(context: Context): CharSequence? {
        return AccessibilityUtil.getSummary(
            context,
            MAIN_SETTING_KEY,
            R.string.show_captions_enabled,
            R.string.show_captions_disabled,
        )
    }

    override fun onCreate(context: PreferenceLifecycleContext) {
        super.onCreate(context)
        if (isEntryPoint(context)) {
            val observer =
                KeyedObserver<String> { _, _ -> context.notifyPreferenceChange(bindingKey) }
            SettingsSecureStore.get(context)
                .addObserver(MAIN_SETTING_KEY, observer, HandlerExecutor.main)
            captionEnabledObserver = observer
        }
    }

    override fun onDestroy(context: PreferenceLifecycleContext) {
        super.onDestroy(context)
        captionEnabledObserver?.let {
            SettingsSecureStore.get(context).removeObserver(MAIN_SETTING_KEY, it)
            captionEnabledObserver = null
        }
    }

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {}

    companion object {
        const val KEY = "captioning_preference_screen"
        private const val MAIN_SETTING_KEY = Settings.Secure.ACCESSIBILITY_CAPTIONING_ENABLED
    }
}
