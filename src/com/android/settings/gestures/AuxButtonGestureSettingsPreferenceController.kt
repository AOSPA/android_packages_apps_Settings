/*
 * Copyright 2020, The Paranoid Android Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.gestures

import android.content.Context
import android.os.UserHandle.USER_CURRENT
import android.provider.Settings

import com.android.settings.R
import com.android.settings.core.BasePreferenceController

import co.aospa.framework.device.actions.AuxiliaryKeyHandlerActions

class AuxButtonGestureSettingsPreferenceController(
    private val context: Context,
    private val key: String
) : BasePreferenceController(context, key) {

    val resources = context.resources

    override fun getAvailabilityStatus(): Int =
        if (resources.getBoolean(R.bool.config_hasAuxiliaryButton)) AVAILABLE
        else UNSUPPORTED_ON_DEVICE

    override fun getSummary(): CharSequence {
        val setting = try {
            Settings.System.getIntForUser(
                context.contentResolver,
                Settings.System.AUX_BUTTON,
                USER_CURRENT
            )
        } catch (e: Exception) {
            -1
        }

        return when (setting) {
            AuxiliaryKeyHandlerActions.ACTION_ASSIST -> context.getText(R.string.aux_button_assist_title)
            AuxiliaryKeyHandlerActions.ACTION_CAMERA -> context.getText(R.string.aux_button_camera_title)
            AuxiliaryKeyHandlerActions.ACTION_TORCH -> context.getText(R.string.aux_button_torch_title)
            AuxiliaryKeyHandlerActions.ACTION_DND -> context.getText(R.string.aux_button_dnd_title)
            AuxiliaryKeyHandlerActions.ACTION_RINGER -> context.getText(R.string.aux_button_ringer_title)
            else -> context.getText(R.string.aux_button_none_title)
        }
    }
}
