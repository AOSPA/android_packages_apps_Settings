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

import android.util.Log
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settings.overlay.FeatureFactory.Companion.featureFactory
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.types.GeneratedParameterType
import com.android.settingslib.metadata.preferencesapi.types.GeneratedValue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

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

        if (printRepository == null) {
            Log.e(TAG, "printRepository is null, no screen parameters can be provided.")
        } else {
            parameters {
                parameter(
                    name = EXTRA_SERVICE_COMPONENT_NAME,
                    purpose = R.string.print_service_parameter_purpose,
                    type =
                        GeneratedParameterType(R.string.print_service_parameter_description) {
                            runBlocking {
                                printRepository.printServiceDisplayInfosFlow().first().map {
                                    GeneratedValue(it.componentName, it.title)
                                }
                            }
                        },
                )

                prepareScreenExtras { keyParameters, extras ->
                    val componentName =
                        keyParameters[EXTRA_SERVICE_COMPONENT_NAME] ?: return@prepareScreenExtras

                    runBlocking {
                            printRepository.printServiceDisplayInfosFlow().first().find {
                                it.componentName == componentName
                            }
                        }
                        ?.let { info ->
                            extras.putString(EXTRA_SERVICE_COMPONENT_NAME, componentName)
                            extras.putString(EXTRA_TITLE, info.title)
                            extras.putBoolean(EXTRA_CHECKED, info.isEnabled)
                        } ?: Log.e(TAG, "Print service info not found for: $componentName")
                }
            }
        }
    }

    companion object {
        private const val TAG = "PrintServiceApiScreen"
        const val KEY = "print_service_settings"
        const val EXTRA_SERVICE_COMPONENT_NAME = "EXTRA_SERVICE_COMPONENT_NAME"
        const val EXTRA_TITLE = "EXTRA_TITLE"
        const val EXTRA_CHECKED = "EXTRA_CHECKED"
    }
}
// LINT.ThenChange(PrintServiceSettingsFragment.java)
