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

package com.android.settings.accessibility.buttonshortcutsetting.ui

import android.content.Context
import android.provider.Settings
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.accessibility.AccessibilityUtil
import com.android.settings.accessibility.Flags
import com.android.settings.accessibility.buttonshortcutsetting.data.FloatingMenuTransparencyDataStore
import com.android.settings.accessibility.buttonshortcutsetting.data.FloatingMenuTransparencyDataStore.Companion.MAX_TRANSPARENCY_PROGRESS
import com.android.settings.accessibility.buttonshortcutsetting.data.FloatingMenuTransparencyDataStore.Companion.MIN_TRANSPARENCY_PROGRESS
import com.android.settings.accessibility.buttonshortcutsetting.data.FloatingMenuTransparencyDataStore.Companion.PRECISION
import com.android.settings.accessibility.buttonshortcutsetting.data.FloatingMenuTransparencyDataStore.Companion.SETTING_KEY
import com.android.settings.accessibility.buttonshortcutsetting.ui.FloatingMenuFadePreference.Companion.FADE_ENABLED_DEFAULT
import com.android.settingslib.datastore.HandlerExecutor
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.IntRangeValuePreference
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.widget.SliderPreference
import com.android.settingslib.widget.SliderPreferenceBinding
import java.text.NumberFormat

/**
 * A preference that allows the user to adjust the transparency of the floating accessibility menu.
 */
class FloatingMenuTransparencyPreference(context: Context) :
    IntRangeValuePreference, SliderPreferenceBinding, PreferenceLifecycleProvider {

    private var settingsKeyedObserver: KeyedObserver<String>? = null
    private val dataStore by lazy { FloatingMenuTransparencyDataStore(context) }
    private val settingsSecureStore by lazy { SettingsSecureStore.get(context) }
    private var percentFormat: NumberFormat? = null

    override val key: String
        get() = KEY

    override val purpose: Int
        get() = R.string.a11y_button_shortcut_transparency_purpose

    override val title: Int
        get() = R.string.accessibility_button_opacity_title

    override fun isEnabled(context: Context): Boolean {
        val fadeEnabled =
            settingsSecureStore.getBoolean(Settings.Secure.ACCESSIBILITY_FLOATING_MENU_FADE_ENABLED)
                ?: FADE_ENABLED_DEFAULT
        return AccessibilityUtil.isFloatingMenuEnabled(context) && fadeEnabled
    }

    override fun createWidget(context: Context): SliderPreference {
        return super.createWidget(context).apply {
            setHapticFeedbackMode(SliderPreference.HAPTIC_FEEDBACK_MODE_ON_ENDS)
            updatesContinuously = true
        }
    }

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        val sliderPreference = preference as SliderPreference
        sliderPreference.setSliderStateDescription(
            formatStateDescription(sliderPreference.context, sliderPreference.value)
        )
    }

    override fun getMinValue(context: Context): Int = MIN_TRANSPARENCY_PROGRESS

    override fun getMaxValue(context: Context): Int = MAX_TRANSPARENCY_PROGRESS

    override fun getUnitOfMeasurement() = "%"

    override fun storage(context: Context): KeyValueStore = dataStore

    override fun getWritePermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override val supportsWrite = true

    override fun onCreate(context: PreferenceLifecycleContext) {
        super.onCreate(context)
        settingsKeyedObserver = KeyedObserver { _, _ -> context.notifyPreferenceChange(key) }

        settingsKeyedObserver?.let { observer ->
            settingsSecureStore.addObserver(
                Settings.Secure.ACCESSIBILITY_BUTTON_MODE,
                observer,
                HandlerExecutor.main,
            )
            settingsSecureStore.addObserver(
                Settings.Secure.ACCESSIBILITY_FLOATING_MENU_FADE_ENABLED,
                observer,
                HandlerExecutor.main,
            )
        }
    }

    override fun onDestroy(context: PreferenceLifecycleContext) {
        super.onDestroy(context)
        settingsKeyedObserver?.let { observer ->
            settingsSecureStore.removeObserver(Settings.Secure.ACCESSIBILITY_BUTTON_MODE, observer)
            settingsSecureStore.removeObserver(
                Settings.Secure.ACCESSIBILITY_FLOATING_MENU_FADE_ENABLED,
                observer,
            )
        }
        settingsKeyedObserver = null
    }

    private fun formatStateDescription(context: Context, sliderValue: Int): CharSequence {
        if (!isEnabled(context) && Flags.floatingMenuTransparencySliderAnnouncesDisabled()) {
            return context.getString(com.android.settingslib.R.string.disabled)
        }

        val transparency = sliderValue / PRECISION
        if (percentFormat == null) {
            percentFormat =
                NumberFormat.getPercentInstance(context.resources.configuration.locales.get(0))
        }
        return percentFormat!!.format(transparency.toDouble())
    }

    companion object {
        const val KEY = SETTING_KEY
    }
}
