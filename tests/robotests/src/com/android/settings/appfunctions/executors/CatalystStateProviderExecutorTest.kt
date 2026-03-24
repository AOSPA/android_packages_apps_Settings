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

package com.android.settings.appfunctions.executors

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.android.settings.appfunctions.DeviceStateAppFunctionType
import com.android.settings.testutils.appfunctions.CatalystConfigBuilder.buildConfig
import com.android.settingslib.testutils.GraphTestUtils
import com.android.settingslib.testutils.GraphTestUtils.PreferenceScreenConfig
import com.android.settingslib.testutils.GraphTestUtils.createPersistentPreference
import com.android.settingslib.testutils.GraphTestUtils.createScreen
import com.android.settingslib.testutils.GraphTestUtils.createSimplePreference
import com.android.settingslib.testutils.GraphTestUtils.setRegistryFactories
import com.google.common.truth.Truth.assertThat
import java.util.Locale
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import androidx.fragment.app.Fragment
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.preconditions.Allowed
import com.android.settingslib.metadata.preferencesapi.types.AnyString
import com.android.settingslib.metadata.KeyParametersSchema

@RunWith(RobolectricTestRunner::class)
class CatalystStateProviderExecutorTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()!!
    private val englishContext = context.createConfigurationContext(
        Configuration(context.resources.configuration).also {
            it.setLocale(Locale.ENGLISH)
        }
    )

    @Test
    fun execute_onPersistentPreference_returnsCorrectPreferencePurpose() = runTest {
        setRegistryFactories(
            createScreen(
                PreferenceScreenConfig (
                    screenKey = "screen_key",
                    purpose = R.string.preference_screen_purpose,
                    preferences = listOf(
                        createPersistentPreference<Boolean>(
                            persistentPreferenceConfig = GraphTestUtils.PersistentPreferenceConfig(
                                GraphTestUtils.PreferenceConfig(
                                    key = "preference_key",
                                    purpose = R.string.preference_purpose,
                                ),
                            )
                        )
                    )
                )
            )
        )
        val executor = CatalystStateProviderExecutor(
            buildConfig("screen_key", listOf("preference_key")),
            context,
            englishContext
        )

        val result = executor.execute(DeviceStateAppFunctionType.GET_UNCATEGORIZED)

        // single screen
        assertThat(result.states).hasSize(1)
        // the preference
        assertThat(result.states[0].deviceStateItems).hasSize(1)
        assertThat(result.states[0].deviceStateItems[0].key).isEqualTo(
            "screen_key/preference_key"
        )
        assertThat(result.states[0].deviceStateItems[0].purpose).isEqualTo(
            context.getString(R.string.preference_purpose)
        )
    }

    @Test
    fun execute_onScreenWithTitle_returnsTitleAndPurposeAsDescription() = runTest {
        setRegistryFactories(
            createScreen(
                GraphTestUtils.PreferenceScreenConfig(
                    "screen_key",
                    purpose = R.string.preference_screen_purpose,
                    title = R.string.preference_screen_title,
                    preferences = listOf()
                )
            )
        )
        val executor = CatalystStateProviderExecutor(
            buildConfig("screen_key", listOf()),
            context,
            englishContext
        )

        val result = executor.execute(DeviceStateAppFunctionType.GET_UNCATEGORIZED)

        assertThat(result.states).hasSize(1)
        assertThat(result.states[0].description).isEqualTo(
            "${
                context.getString(R.string.preference_screen_title)
            }. ${
                context.getString(R.string.preference_screen_purpose)
            }"
        )
    }

    @Test
    fun execute_onScreenWithoutTitle_returnsPurposeAsDescription() = runTest {
        setRegistryFactories(
            createScreen(
                GraphTestUtils.PreferenceScreenConfig(
                    "screen_key",
                    purpose = R.string.preference_screen_purpose,
                    title = 0,
                    preferences = listOf(
                        createSimplePreference(
                            GraphTestUtils.PreferenceConfig(
                                key = "preference_key",
                                purpose = R.string.preference_purpose
                            ),
                        )
                    )
                )
            )
        )
        val executor = CatalystStateProviderExecutor(
            buildConfig(
                "screen_key",
                listOf("preference_key"),
            ),
            context,
            englishContext
        )

        val result = executor.execute(DeviceStateAppFunctionType.GET_UNCATEGORIZED)

        assertThat(result.states).hasSize(1)
        assertThat(result.states[0].description).isEqualTo(
            context.getString(R.string.preference_screen_purpose)
        )
    }

    @Test
    fun execute_onEntireScreenConfig_returnsAllPreferencesAsDeviceStateItem() = runTest {
        setRegistryFactories(
            createScreen(
                GraphTestUtils.PreferenceScreenConfig (
                    screenKey = "screen_key",
                    purpose = R.string.preference_screen_purpose,
                    preferences = listOf(
                        createPersistentPreference<Boolean>(
                            GraphTestUtils.PersistentPreferenceConfig(
                                GraphTestUtils.PreferenceConfig(
                                    key = "preference_key_1",
                                    purpose = R.string.preference_purpose
                                )
                            )
                        ),
                        createPersistentPreference<Boolean>(
                            GraphTestUtils.PersistentPreferenceConfig(
                                GraphTestUtils.PreferenceConfig(
                                    key = "preference_key_2",
                                    purpose = R.string.preference_purpose
                                )
                            )
                        )
                    )
                )
            )
        )
        val executor = CatalystStateProviderExecutor(
            buildConfig("screen_key", listOf()),
            context,
            englishContext
        )

        val result = executor.execute(DeviceStateAppFunctionType.GET_UNCATEGORIZED)

        // single screen
        assertThat(result.states).hasSize(1)
        // the 2 preferences
        assertThat(result.states[0].deviceStateItems).hasSize(2)
        assertThat(result.states[0].deviceStateItems[0].key).isEqualTo(
            "screen_key/preference_key_1"
        )
        assertThat(result.states[0].deviceStateItems[1].key).isEqualTo(
            "screen_key/preference_key_2"
        )
    }

    @Test
    fun execute_onScreenWithUiOnlyPreferences_returnsOnlyNonUiPreferencesAsDeviceStateItems() = runTest {
        setRegistryFactories(
            createScreen(
                GraphTestUtils.PreferenceScreenConfig (
                    screenKey = "screen_key",
                    purpose = R.string.preference_screen_purpose,
                    preferences = listOf(
                        createPersistentPreference<Boolean>(
                            GraphTestUtils.PersistentPreferenceConfig(
                                GraphTestUtils.PreferenceConfig(
                                    key = "ui_only_preference",
                                    purpose = R.string.preference_purpose,
                                    isUiOnly = true
                                )
                            )
                        ),
                        createPersistentPreference<Boolean>(
                            GraphTestUtils.PersistentPreferenceConfig(
                                GraphTestUtils.PreferenceConfig(
                                    key = "preference_key_2",
                                    purpose = R.string.preference_purpose
                                )
                            )
                        )
                    )
                )
            )
        )
        val executor = CatalystStateProviderExecutor(
            buildConfig("screen_key", listOf()),
            context,
            englishContext
        )

        val result = executor.execute(DeviceStateAppFunctionType.GET_UNCATEGORIZED)

        // single screen
        assertThat(result.states).hasSize(1)
        // the non-ui only preference
        assertThat(result.states[0].deviceStateItems).hasSize(1)
        assertThat(result.states[0].deviceStateItems[0].key).isEqualTo(
            "screen_key/preference_key_2"
        )
    }

    @Test
    fun execute_withUiOnlyPreferenceInConfig_DoesNotReturnItAsDeviceStateItem() = runTest {
        setRegistryFactories(
            createScreen(
                GraphTestUtils.PreferenceScreenConfig (
                    screenKey = "screen_key",
                    purpose = R.string.preference_screen_purpose,
                    preferences = listOf(
                        createPersistentPreference<Boolean>(
                            GraphTestUtils.PersistentPreferenceConfig(
                                GraphTestUtils.PreferenceConfig(
                                    key = "ui_only_preference",
                                    purpose = R.string.preference_purpose,
                                    isUiOnly = true
                                )
                            )
                        )
                    )
                )
            )
        )
        val executor = CatalystStateProviderExecutor(
            buildConfig("screen_key", listOf("ui_only_preference")),
            context,
            englishContext
        )

        val result = executor.execute(DeviceStateAppFunctionType.GET_UNCATEGORIZED)

        // single screen
        assertThat(result.states).hasSize(1)
        // no device state items
        assertThat(result.states[0].deviceStateItems).hasSize(0)
    }

    @Test
    fun execute_onScreenWithPreconditions_includesPreconditionsInDescription() = runTest {
        setRegistryFactories(ApiFirstTestScreen())
        val executor = CatalystStateProviderExecutor(
            buildConfig("api_first_screen", listOf()),
            context,
            englishContext
        )

        val result = executor.execute(DeviceStateAppFunctionType.GET_UNCATEGORIZED)

        assertThat(result.states).hasSize(1)
        assertThat(result.states[0].description).contains("Preconditions to accessing: Screen precondition.")
    }

    @Test
    fun execute_onApiFirstPreference_doesNotIncludeName() = runTest {
        setRegistryFactories(ApiFirstTestScreen())
        val executor = CatalystStateProviderExecutor(
            buildConfig("api_first_screen", listOf("writable_pref")),
            context,
            englishContext
        )

        val result = executor.execute(DeviceStateAppFunctionType.GET_UNCATEGORIZED)

        val item = result.states[0].deviceStateItems[0]
        assertThat(item.key).isEqualTo("api_first_screen/writable_pref")
        assertThat(item.name).isNull()
    }

    @Test
    fun execute_onScreenWithKeyParameters_includesKeyParametersInDescription() = runTest {
        setRegistryFactories(ScreenWithKeyParameters())
        val executor = CatalystStateProviderExecutor(
            buildConfig("screen_with_params", listOf()),
            context,
            englishContext
        )

        val result = executor.execute(DeviceStateAppFunctionType.GET_UNCATEGORIZED)

        assertThat(result.states).hasSize(1)
        assertThat(result.states[0].description).endsWith("[param=value]")
    }

    private class ScreenWithKeyParameters : PreferencesApiScreen(
        key = "screen_with_params",
        topLevelSettingsCategory = Category.SYSTEM,
        fragment = Fragment::class,
        purpose = R.string.preference_screen_purpose,
    ) {
        override val keyParameters = KeyParametersSchema {
            parameter("param", R.string.preference_purpose, type = AnyString)
        }.prepare("param" to "value")
    }

    private class ApiFirstTestScreen : PreferencesApiScreen(
        key = "api_first_screen",
        topLevelSettingsCategory = Category.SYSTEM,
        fragment = Fragment::class,
        purpose = R.string.preference_screen_purpose,
    ) {
        init {
            preconditions("Screen precondition") { Allowed }

            preference(
                key = "writable_pref",
                purpose = R.string.preference_purpose,
                type = AnyString,
            ) {
                get { execute { "true" } }
                set { execute {} }
            }
        }
    }
}
