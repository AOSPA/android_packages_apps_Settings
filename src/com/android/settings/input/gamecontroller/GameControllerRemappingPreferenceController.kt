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

package com.android.settings.input.gamecontroller

import android.content.Context
import android.text.TextUtils
import android.util.Log
import android.view.InputDevice
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.android.settings.R
import com.android.settings.core.BasePreferenceController
import com.android.settings.input.gamecontroller.GameControllerUtils.buttonToPreferenceKeyMap
import com.android.settings.input.gamecontroller.GameControllerUtils.preferenceKeyToAxesMap
import com.android.settings.input.gamecontroller.GameControllerUtils.preferenceKeyToButtonMap
import com.android.settings.input.gamecontroller.GameControllerUtils.preferenceKeyToNameMap
import com.android.settingslib.Utils.getColorAttrDefaultColor

/** Preference controller for game controller key and stick remapping */
class GameControllerRemappingPreferenceController(
    private val context: Context,
    private val viewModel: GameControllerViewModel,
) : BasePreferenceController(context, "game_controller_remapping") {

    private var preferenceScreen: PreferenceScreen? = null

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        preferenceScreen = screen
        val hasGamepadSource =
            (viewModel.controllerDevice.sources and InputDevice.SOURCE_GAMEPAD) ==
                InputDevice.SOURCE_GAMEPAD
        val hasJoystickSource =
            (viewModel.controllerDevice.sources and InputDevice.SOURCE_JOYSTICK) ==
                InputDevice.SOURCE_JOYSTICK
        val color = getColorAttrDefaultColor(context, android.R.attr.colorControlNormal)
        // Update visibility for all button and axis preferences
        preferenceKeyToButtonMap.forEach { (preferenceKey, keyCode) ->
            val preference = screen.findPreference<Preference>(preferenceKey)
            preference?.isVisible = hasGamepadSource
            val glyph = viewModel.glyphMap?.getDrawableForKeycode(context, keyCode)
            val displayName = viewModel.glyphMap?.getDisplayNameForKeycode(keyCode)
            if (glyph != null) {
                glyph.setTint(color)
                preference?.icon = glyph
            }
            if (!TextUtils.isEmpty(displayName)) {
                preference?.title = displayName
            }
        }
        preferenceKeyToAxesMap.keys.forEach {
            screen.findPreference<Preference>(it)?.isVisible = hasJoystickSource
        }
    }

    override fun updateState(preference: Preference) {
        super.updateState(preference)
        val screen = preferenceScreen ?: return
        // Loop through all the key remapping preferences we manage and update their summaries
        preferenceKeyToButtonMap.keys.forEach { fromPreferenceKey ->
            screen.findPreference<GameControllerRemappingPreference>(fromPreferenceKey)?.let { pref ->
                val toPreferenceKey = findMappedPreference(fromPreferenceKey)
                if (toPreferenceKey != null) {
                    val displayName =
                        viewModel.glyphMap?.getDisplayNameForKeycode(
                            preferenceKeyToButtonMap[toPreferenceKey]!!
                        )
                    pref.summary =
                        displayName ?: context.getString(preferenceKeyToNameMap[toPreferenceKey]!!)
                    pref.updateSummaryContentDescription()
                } else {
                    Log.w(GameControllerUtils.TAG, "Unmapped preference key $fromPreferenceKey")
                }
            }
        }

        preferenceKeyToAxesMap.keys.forEach { fromPreferenceKey ->
            screen.findPreference<GameControllerRemappingPreference>(fromPreferenceKey)?.let { pref ->
                val toPreferenceKey = findMappedPreference(fromPreferenceKey)
                if (toPreferenceKey != null) {
                    pref.setSummary(context.getString(preferenceKeyToNameMap[toPreferenceKey]!!))
                    pref.updateSummaryContentDescription()
                } else {
                    Log.w(GameControllerUtils.TAG, "Unmapped preference key $fromPreferenceKey")
                }
            }
        }
    }

    private fun findMappedPreference(fromPreferenceKey: String): String? {
        if (preferenceKeyToButtonMap.containsKey(fromPreferenceKey)) {
            val buttonRemappingMap = viewModel.buttonRemapping.value ?: return null
            val fromButton = preferenceKeyToButtonMap[fromPreferenceKey]!!
            val toButton = buttonRemappingMap[fromButton] ?: fromButton
            return buttonToPreferenceKeyMap[toButton]
        } else if (preferenceKeyToAxesMap.containsKey(fromPreferenceKey)) {
            val axisRemappingMap = viewModel.axisRemapping.value ?: return null
            val fromAxes = preferenceKeyToAxesMap[fromPreferenceKey]!!
            // Determine what the current set of axes are mapped to.
            val toAxes =
                fromAxes
                    .map { fromAxis -> axisRemappingMap.getOrDefault(fromAxis, fromAxis) }
                    .toTypedArray()
            // Find the preference key that corresponds to the mapped axes.
            val toPreferenceKey =
                preferenceKeyToAxesMap.entries.firstOrNull { it.value.contentEquals(toAxes) }?.key
            return toPreferenceKey
        }
        return null
    }

    override fun handlePreferenceTreeClick(preference: Preference): Boolean {
        val toPreferenceKey = findMappedPreference(preference.key)
        if (toPreferenceKey == null) {
            Log.w(
                GameControllerUtils.TAG,
                "Unmapped preference key ${preference.key} was clicked, ignoring!",
            )
            return false
        }
        viewModel.requestRemappingDialog(preference.key, toPreferenceKey)
        return true
    }

    override fun getAvailabilityStatus(): Int = AVAILABLE
}
