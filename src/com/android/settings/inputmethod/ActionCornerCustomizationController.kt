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

package com.android.settings.inputmethod

import android.app.ActivityManager
import android.content.Context
import android.provider.Settings
import android.provider.Settings.Secure.ACTION_CORNER_ACTION_NONE
import android.provider.Settings.Secure.ACTION_CORNER_BOTTOM_LEFT_ACTION
import android.provider.Settings.Secure.ACTION_CORNER_BOTTOM_RIGHT_ACTION
import android.provider.Settings.Secure.ACTION_CORNER_TOP_LEFT_ACTION
import android.provider.Settings.Secure.ACTION_CORNER_TOP_RIGHT_ACTION
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.android.settings.R
import com.android.settings.core.BasePreferenceController
import com.android.settings.flags.Flags.actionCornerCustomization
import com.android.settings.inputmethod.InputPeripheralsSettingsUtils.isMouse
import com.android.settings.inputmethod.InputPeripheralsSettingsUtils.isTouchpad

/**
 * The controller that handles the customization of each action corner.
 */
class ActionCornerCustomizationController(context: Context, preferenceKey: String) :
    BasePreferenceController(context, preferenceKey),
    Preference.OnPreferenceChangeListener {
    private lateinit var listPreference: ListPreference

    private val corner: Corner = prefKeyToCorner[preferenceKey]!!

    private val actions = context.resources.getStringArray(R.array.action_corner_action_values).asList()

    override fun getAvailabilityStatus(): Int {
        return if (actionCornerCustomization() && (isTouchpad() || isMouse())) AVAILABLE
        else CONDITIONALLY_UNAVAILABLE
    }

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        listPreference = screen.findPreference(preferenceKey)!!
        val cornerName = mContext.getString(corner.nameId)
        listPreference.dialogTitle =
            mContext.getString(R.string.action_corner_action_dialog_title, cornerName)
    }

    override fun updateState(preference: Preference?) {
        updateListPreference()
    }

    private fun updateListPreference() {
        listPreference.value = getCurrentAction()
        listPreference.summary = summary
    }

    private fun getCurrentAction(): String {
        val current = Settings.System.getIntForUser(mContext.contentResolver,
            corner.target,
            ACTION_CORNER_ACTION_NONE, ActivityManager.getCurrentUser())
        return actions[current]
    }

    override fun getSummary(): CharSequence? = listPreference.entry

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        val action = actions.indexOf(newValue.toString())
        Settings.System.putIntForUser(
            mContext.contentResolver, corner.target, action, ActivityManager.getCurrentUser())
        updateListPreference()
        return true
    }

    companion object {
        val prefKeyToCorner = mapOf(
            "action_corner_bottom_left" to Corner.BOTTOM_LEFT,
            "action_corner_bottom_right" to Corner.BOTTOM_RIGHT,
            "action_corner_top_left" to Corner.TOP_LEFT,
            "action_corner_top_right" to Corner.TOP_RIGHT,)
    }

    enum class Corner(val nameId: Int, val target: String) {
        BOTTOM_LEFT(
            nameId = R.string.action_corner_bottom_left_name,
            target = ACTION_CORNER_BOTTOM_LEFT_ACTION
        ),
        BOTTOM_RIGHT(
            nameId = R.string.action_corner_bottom_right_name,
            target = ACTION_CORNER_BOTTOM_RIGHT_ACTION
        ),
        TOP_LEFT(
            nameId = R.string.action_corner_top_left_name,
            target = ACTION_CORNER_TOP_LEFT_ACTION
        ),
        TOP_RIGHT(
            nameId = R.string.action_corner_top_right_name,
            target = ACTION_CORNER_TOP_RIGHT_ACTION
        ),
    }
}
