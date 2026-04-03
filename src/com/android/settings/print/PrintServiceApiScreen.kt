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

package com.android.settings.print

import android.Manifest
import android.util.Log
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settings.overlay.FeatureFactory.Companion.featureFactory
import com.android.settings.print.PrintRepository.PrintServiceDisplayInfo
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.types.AnyBoolean
import com.android.settingslib.metadata.preferencesapi.types.GeneratedParameterType
import com.android.settingslib.metadata.preferencesapi.types.GeneratedValue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import com.android.settingslib.metadata.preferencesapi.unsafe

// LINT.IfChange
/**
 * The [PreferencesApiScreen] for the Print Service Settings screen.
 *
 * This screen allows users to manage settings for a specific print service.
 */
@ProvidePreferenceScreen(PrintServiceApiScreen.KEY, parameterized = true)
class PrintServiceApiScreen :
    PreferencesApiScreen(
        key = KEY,
        topLevelSettingsCategory = Category.CONNECTED_DEVICES,
        fragment = PrintServiceSettingsFragment::class,
        purpose = R.string.print_service_settings_purpose,
    ) {
    private val printRepository = featureFactory.wifiFeatureProvider.printRepository

    init {
        flag { Flags.catalystMigration26q2() }

        parameters {
            parameter(
                name = EXTRA_SERVICE_COMPONENT_NAME,
                purpose = R.string.print_service_parameter_purpose,
                type =
                    GeneratedParameterType(
                        R.string.print_service_parameter_description,
                        key = "PrintServiceComponent"
                    ) {
                        fetchDisplayInfos().map { GeneratedValue(it.componentName.unsafe(), it.title.unsafe()) }
                    },
            )

            prepareScreenExtras { keyParameters, extras ->
                val componentName =
                    keyParameters[EXTRA_SERVICE_COMPONENT_NAME] ?: return@prepareScreenExtras
                findDisplayInfo(componentName)?.let { info ->
                    extras.putString(EXTRA_SERVICE_COMPONENT_NAME, componentName)
                    extras.putString(EXTRA_TITLE, info.title)
                    extras.putBoolean(EXTRA_CHECKED, info.isEnabled)
                } ?: Log.e(TAG, "Service info not found: $componentName")
            }
        }

        preference(PRINT_SWITCH_KEY, PRINT_SWITCH_PURPOSE, AnyBoolean) {
            sensitivityLevel(SensitivityLevel.NO_SENSITIVITY)

            get {
                permissions(Manifest.permission.READ_PRINT_SERVICES)
                execute {
                    keyParameters?.get(EXTRA_SERVICE_COMPONENT_NAME)?.let {
                        printRepository.isEnabled(it)
                    } ?: false
                }
            }
            set {
                // No permission required to set print service state.
                execute { value ->
                    keyParameters?.get(EXTRA_SERVICE_COMPONENT_NAME)?.let {
                        printRepository.setEnabled(it, value)
                    }
                }
            }
        }
    }

    private fun fetchDisplayInfos(): List<PrintServiceDisplayInfo> = runBlocking {
        printRepository?.printServiceDisplayInfosFlow()?.first()
            ?: emptyList<PrintServiceDisplayInfo>().also {
                Log.e(TAG, "printRepository is null or empty")
            }
    }

    private fun findDisplayInfo(componentName: String?): PrintServiceDisplayInfo? {
        if (componentName == null) return null
        return fetchDisplayInfos().find { it.componentName == componentName }
    }

    companion object {
        private const val TAG = "PrintServiceApiScreen"
        const val KEY = "print_service_settings"
        const val EXTRA_SERVICE_COMPONENT_NAME = "EXTRA_SERVICE_COMPONENT_NAME"
        const val EXTRA_TITLE = "EXTRA_TITLE"
        const val EXTRA_CHECKED = "EXTRA_CHECKED"
        const val PRINT_SWITCH_KEY = "print_service_switch"
        private val PRINT_SWITCH_PURPOSE = R.string.print_switch_purpose
    }
}
// LINT.ThenChange(PrintServiceSettingsFragment.java)
