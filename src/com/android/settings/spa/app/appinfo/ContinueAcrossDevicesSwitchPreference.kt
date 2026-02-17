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

import android.app.AppOpsManager.MODE_ALLOWED
import android.app.AppOpsManager.MODE_DEFAULT
import android.app.AppOpsManager.MODE_IGNORED
import android.app.AppOpsManager.OP_CONTINUE_ACROSS_DEVICES
import android.companion.datatransfer.continuity.TaskContinuityManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.settings.R
import com.android.settingslib.spa.framework.compose.OverridableFlow
import com.android.settingslib.spa.widget.preference.SwitchPreference
import com.android.settingslib.spa.widget.preference.SwitchPreferenceModel
import com.android.settingslib.spaprivileged.framework.common.appOpsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow

@Composable
fun ContinueAcrossDevicesSwitchPreference(
    app: ApplicationInfo,
    isContinueAcrossDevicesSwitchEnabledStateFlow: MutableStateFlow<Boolean>,
) {
    val context = LocalContext.current
    val presenter = remember(app) { ContinueAcrossDevicesSwitchPresenter(context, app) }
    if (!presenter.isAvailable()) return

    val isEligibleState by
        presenter.isEligibleFlow.collectAsStateWithLifecycle(initialValue = false)
    val isCheckedState = presenter.isCheckedFlow.collectAsStateWithLifecycle(initialValue = null)
    SwitchPreference(
        remember {
            object : SwitchPreferenceModel {
                override val title = context.getString(R.string.continue_across_devices_switch)
                override val summary = {
                    context.getString(R.string.continue_across_devices_switch_summary)
                }
                override val changeable = { isEligibleState }
                override val checked = {
                    val result = if (changeable()) isCheckedState.value else false
                    result.also { isChecked ->
                        isChecked?.let { isContinueAcrossDevicesSwitchEnabledStateFlow.value = it }
                    }
                }
                override val onCheckedChange = presenter::onCheckedChange
            }
        }
    )
}

private class ContinueAcrossDevicesSwitchPresenter(
    context: Context,
    private val app: ApplicationInfo,
) {
    private val appOpsManager = context.appOpsManager
    private val executor = Dispatchers.IO.asExecutor()
    private val taskContinuityManager =
        context.getSystemService(TaskContinuityManager::class.java) as TaskContinuityManager

    fun isAvailable() = android.companion.Flags.taskContinuity()

    val isEligibleFlow = callbackFlow {
        val listener =
            object : TaskContinuityManager.HandoffFeatureStateListener {
                override fun onHandoffFeatureStateChanged(
                    availabilityStatus: Int,
                    enabled: Boolean,
                ) {
                    trySend(
                        availabilityStatus ==
                            TaskContinuityManager.HANDOFF_AVAILABILITY_STATUS_AVAILABLE
                    )
                }
            }
        taskContinuityManager.registerHandoffFeatureStateListener(executor, listener)
        awaitClose { taskContinuityManager.unregisterHandoffFeatureStateListener(listener) }
    }

    private val isChecked =
        OverridableFlow(
            flow {
                val mode =
                    appOpsManager.checkOpNoThrow(
                        OP_CONTINUE_ACROSS_DEVICES,
                        app.uid,
                        app.packageName,
                    )
                emit(mode == MODE_ALLOWED || mode == MODE_DEFAULT)
            }
        )

    val isCheckedFlow = isChecked.flow

    fun onCheckedChange(newChecked: Boolean) {
        try {
            appOpsManager.setUidMode(
                OP_CONTINUE_ACROSS_DEVICES,
                app.uid,
                if (newChecked) MODE_ALLOWED else MODE_IGNORED,
            )
            isChecked.override(newChecked)
        } catch (e: RuntimeException) {
            Log.e(
                "ContinueAcrossDevicesSwitchPresenter",
                "Failed to set continue across devices mode",
                e,
            )
        }
    }
}
