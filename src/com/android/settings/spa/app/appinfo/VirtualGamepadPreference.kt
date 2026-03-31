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

package com.android.settings.spa.app.appinfo

import android.app.AppGlobals
import android.app.compat.CompatChanges
import android.content.Context
import android.content.pm.ActivityInfo.OVERRIDE_ENABLE_VIRTUAL_GAMEPAD
import android.content.pm.ApplicationInfo
import android.content.pm.IPackageManager
import android.content.pm.PackageManager
import android.os.UserHandle
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.settings.R
import com.android.settingslib.spa.widget.preference.SwitchPreference
import com.android.settingslib.spa.widget.preference.SwitchPreferenceModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Composable
fun VirtualGamepadPreference(app: ApplicationInfo) {
    val context = LocalContext.current
    val presenter = remember {
        VirtualGamepadPresenter(context, AppGlobals.getPackageManager(), app)
    }
    if (!presenter.isAvailable) return

    val textEnabled = stringResource(com.android.settingslib.R.string.enabled)
    val textDisabled = stringResource(com.android.settingslib.R.string.disabled)
    val title = stringResource(R.string.virtual_gamepad_title)
    val isCheckedState = presenter.isChecked.collectAsStateWithLifecycle(initialValue = false)

    SwitchPreference(
        remember {
            object : SwitchPreferenceModel {
                override val title = title
                override val summary = { if (isCheckedState.value) textEnabled else textDisabled }
                override val checked = { isCheckedState.value }
                override val onCheckedChange = presenter::onCheckedChange
            }
        }
    )
}

class VirtualGamepadPresenter(
    private val context: Context,
    private val pm: IPackageManager,
    private val app: ApplicationInfo,
) {
    private var toastShown = false
    val isSystemOverrideEnabled =
        CompatChanges.isChangeEnabled(
            OVERRIDE_ENABLE_VIRTUAL_GAMEPAD,
            app.packageName,
            UserHandle.CURRENT,
        )

    val isAvailable = isVirtualGamepadOptionAvailable()

    private val _isChecked =
        MutableStateFlow(
            isSystemOverrideEnabled &&
                getUserOption() != PackageManager.VIRTUAL_GAMEPAD_USER_OPTION_OPT_OUT
        )

    val isChecked: StateFlow<Boolean> = _isChecked

    fun onCheckedChange(newChecked: Boolean) {
        _isChecked.value = newChecked
        setEnabled(newChecked)
        showToast()
    }

    private fun showToast() {
        if (!toastShown) {
            Toast.makeText(context, R.string.virtual_gamepad_restart_game_toast, Toast.LENGTH_SHORT)
                .show()
            toastShown = true
        }
    }

    @PackageManager.VirtualGamepadUserOption
    private fun getUserOption(): Int {
        return pm.getVirtualGamepadUserOption(app.packageName, context.userId)
    }

    private fun setUserOption(@PackageManager.VirtualGamepadUserOption option: Int) {
        pm.setVirtualGamepadUserOption(app.packageName, context.userId, option)
    }

    private fun setEnabled(enabled: Boolean) {
        try {
            if (enabled) {
                setUserOption(PackageManager.VIRTUAL_GAMEPAD_USER_OPTION_UNSET)
            } else {
                setUserOption(PackageManager.VIRTUAL_GAMEPAD_USER_OPTION_OPT_OUT)
            }
        } catch (e: RuntimeException) {
            Log.e(TAG, "Failed to set virtual gamepad preference.", e)
        }
    }

    private fun isVirtualGamepadOptionAvailable(): Boolean {
        // Check if virtual gamepad is supported on the device.
        if (
            context
                .getResources()
                .getString(com.android.internal.R.string.config_virtual_gamepad_package_name)
                .isEmpty()
        ) {
            return false
        }
        if (
            context
                .getResources()
                .getString(com.android.internal.R.string.config_virtual_gamepad_activity_name)
                .isEmpty()
        ) {
            return false
        }

        // The opt-out option is only available if the game app is allowlisted
        return isSystemOverrideEnabled
    }

    companion object {
        private const val TAG = "VirtualGamepadPresenter"
    }
}
