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
package com.android.settings.personalcontext

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.settings.R
import com.android.settingslib.spa.framework.compose.OverridableFlow
import com.android.settingslib.spa.widget.preference.SwitchPreference
import com.android.settingslib.spa.widget.preference.SwitchPreferenceModel
import kotlinx.coroutines.flow.flow

@Composable
fun PersonalContextAppPreference(app: ApplicationInfo) {
    val context = LocalContext.current
    val presenter = remember { PersonalContextPresenter(context, app) }
    val presentState by
        presenter.serviceAvailableState.collectAsStateWithLifecycle(initialValue = false)
    val checkedState by presenter.checkedFlow.collectAsStateWithLifecycle(initialValue = false)

    if (presentState) {
        SwitchPreference(
            remember {
                object : SwitchPreferenceModel {
                    override val title = context.getString(R.string.personal_context_title)
                    override val summary = {
                        context.getString(R.string.personal_context_switch_summary)
                    }
                    override val checked = { checkedState }
                    override val onCheckedChange = presenter::onCheckedChange
                }
            }
        )
    }
}

class PersonalContextPresenter(context: Context, app: ApplicationInfo) {
    val preferenceController = PersonalContextAppPreferenceController(context, app.packageName)

    val appEnabledFlow =
        OverridableFlow(flow { emit(preferenceController.isPersonalContextForAppEnabled()) })

    val checkedFlow = appEnabledFlow.flow

    val serviceAvailableState = flow {
        emit(
            preferenceController.isPersonalContextAvailable &&
                preferenceController.isPersonalContextServiceEnabled()
        )
    }

    fun onCheckedChange(enabled: Boolean) {
        // Set the enabled state on the controller.
        preferenceController.setPersonalContextEnabled(enabled)

        // Update check box UI
        appEnabledFlow.override(preferenceController.isPersonalContextForAppEnabled())
    }
}
