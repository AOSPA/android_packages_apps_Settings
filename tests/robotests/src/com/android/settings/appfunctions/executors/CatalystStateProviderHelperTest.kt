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

package com.android.settings.appfunctions.executors

import android.content.Context
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import com.android.settings.appfunctions.CatalystConfig
import com.android.settings.appfunctions.DeviceStateAppFunctionType
import com.android.settings.appfunctions.PerScreenCatalystConfig
import com.android.settings.applications.InstalledPackageName
import com.android.settingslib.metadata.CatalystFlagProvider
import com.android.settingslib.metadata.CatalystFlagProviderFactory
import com.android.settingslib.metadata.FixedArrayMap
import com.android.settingslib.metadata.KEY_PACKAGE_NAME
import com.android.settingslib.metadata.KeyParametersSchema
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceScreenMetadata
import com.android.settingslib.metadata.PreferenceScreenMetadataParameterizedFactory
import com.android.settingslib.metadata.PreferenceScreenRegistry
import com.android.settingslib.metadata.ValidatedKeyParameters
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.testutils.GraphTestUtils
import com.android.settingslib.testutils.GraphTestUtils.PreferenceConfig
import com.android.settingslib.testutils.GraphTestUtils.createScreen
import com.android.settingslib.testutils.GraphTestUtils.createSimplePreference
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CatalystStateProviderHelperTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val mockCatalystFlagProvider = mock<CatalystFlagProvider>()

    private lateinit var originalProvider: CatalystFlagProvider

    @Before
    fun setUp() {
        originalProvider = CatalystFlagProviderFactory.getInstance()
        CatalystFlagProviderFactory.setProvider(mockCatalystFlagProvider)
        whenever(mockCatalystFlagProvider.catalystUseKeyParameters()).thenReturn(true)
    }

    @After
    fun tearDown() {
        CatalystFlagProviderFactory.setProvider(originalProvider)
    }

    @Test
    fun getEnabledPreferencesHierarchy_screenDisabled_returnsEmpty() = runTest {
        val screenKey = "disabled_screen"
        val config = CatalystConfig(
            screenConfigs = listOf(
                PerScreenCatalystConfig(
                    enabled = false,
                    screenKey = screenKey,
                    appFunctionTypes = setOf(DeviceStateAppFunctionType.GET_UNCATEGORIZED)
                )
            )
        )

        val result = getEnabledPreferencesHierarchy(
            config = config,
            context = context,
            screenKey = screenKey
        )

        assertThat(result).isEmpty()
    }

    @Test
    fun getEnabledPreferencesHierarchy_appFunctionTypeMismatch_returnsEmpty() = runTest {
        val screenKey = "mismatched_screen"
        val config = CatalystConfig(
            screenConfigs = listOf(
                PerScreenCatalystConfig(
                    enabled = true,
                    screenKey = screenKey,
                    appFunctionTypes = setOf(DeviceStateAppFunctionType.GET_UNCATEGORIZED)
                )
            )
        )

        val result = getEnabledPreferencesHierarchy(
            config = config,
            context = context,
            appFunctionType = DeviceStateAppFunctionType.GET_STORAGE,
            screenKey = screenKey
        )

        assertThat(result).isEmpty()
    }

    @Test
    fun getEnabledPreferencesHierarchy_simpleScreen_returnsPreferences() = runTest {
        val screenKey = "simple_screen"
        val prefKey = "pref1"
        val config = CatalystConfig(
            screenConfigs = listOf(
                PerScreenCatalystConfig(
                    enabled = true,
                    screenKey = screenKey,
                    appFunctionTypes = setOf(DeviceStateAppFunctionType.GET_UNCATEGORIZED)
                )
            )
        )

        val prefMetadata: PreferenceMetadata = createSimplePreference(
            PreferenceConfig(key = prefKey, purpose = 1)
        )
        val screenMetadata = createScreen(
            GraphTestUtils.PreferenceScreenConfig(
                screenKey = screenKey,
                purpose = 1,
                preferences = listOf(prefMetadata)
            )
        )

        GraphTestUtils.setRegistryFactories(screenMetadata)

        val result = getEnabledPreferencesHierarchy(
            config = config,
            context = context,
            screenKey = screenKey
        )

        assertThat(result).hasSize(1)
        val nodes = result.values.first()
        assertThat(nodes.map { it.metadata.key }).contains(prefKey)
    }

    @Test
    fun getEnabledPreferencesHierarchy_nonExposablePreference_skipped() = runTest {
        val screenKey = "screen_with_disabled"
        val prefKey = "pref_non_exposable"
        val config = CatalystConfig(
            screenConfigs = listOf(
                PerScreenCatalystConfig(
                    enabled = true,
                    screenKey = screenKey,
                    appFunctionTypes = setOf(DeviceStateAppFunctionType.GET_UNCATEGORIZED)
                )
            )
        )

        val nonExposablePref = mock<PreferenceMetadata> {
            on { key } doReturn prefKey
            on { bindingKey } doReturn prefKey
        }

        val screenMetadata = createScreen(
            GraphTestUtils.PreferenceScreenConfig(
                screenKey = screenKey,
                purpose = 1,
                preferences = listOf(nonExposablePref)
            )
        )

        GraphTestUtils.setRegistryFactories(screenMetadata)

        val result = getEnabledPreferencesHierarchy(
            config = config,
            context = context,
            screenKey = screenKey
        )

        assertThat(result).hasSize(1)
        val nodes = result.values.first()
        assertThat(nodes.map { it.metadata.key }).doesNotContain(prefKey)
    }

    @Test
    fun getEnabledPreferencesHierarchy_parameterizedScreen_returnsMultipleHierarchies() = runTest {
        val screenKey = "parameterized_screen"
        val prefKey = "pref"

        val config = CatalystConfig(
            screenConfigs = listOf(
                PerScreenCatalystConfig(
                    enabled = true,
                    screenKey = screenKey,
                    appFunctionTypes = setOf(DeviceStateAppFunctionType.GET_UNCATEGORIZED)
                )
            )
        )

        val testSchema = KeyParametersSchema {
            parameter(
                KEY_PACKAGE_NAME,
                "The package name",
                required = true,
                type = InstalledPackageName
            )
        }
        val params1 = testSchema.prepare(mapOf(KEY_PACKAGE_NAME to "com.android.settings"))
        val params2 = testSchema.prepare(mapOf(KEY_PACKAGE_NAME to "com.google.android.gm"))

        val factory = object : PreferenceScreenMetadataParameterizedFactory {
            override val parametersSchema: KeyParametersSchema = testSchema
            override fun create(context: Context, args: Bundle) = create(context)
            override fun parameters(context: Context) = flowOf<Bundle>()

            override fun createWithKeyParameters(
                context: Context,
                keyParameters: ValidatedKeyParameters
            ): PreferenceScreenMetadata {
                val pref = createSimplePreference(PreferenceConfig(key = prefKey, purpose = 1))
                return object : PreferenceScreenMetadata {
                    override fun fragmentClass() = null
                    override fun getPreferenceHierarchy(
                        context: Context,
                        coroutineScope: CoroutineScope
                    ) =
                        preferenceHierarchy(context) { +pref }

                    override val key = screenKey
                    override val purpose = 1
                }
            }

            override fun keyParameters(context: Context) = flowOf(params1, params2)
            override fun create(context: Context) = createWithKeyParameters(context, mock())
        }

        PreferenceScreenRegistry.preferenceScreenMetadataFactories = FixedArrayMap(1) {
            it.put(screenKey, factory)
        }

        val result = getEnabledPreferencesHierarchy(
            config = config,
            context = context,
            screenKey = screenKey
        )

        assertThat(result).hasSize(2)
    }

    @Test
    fun getEnabledPreferencesHierarchy_legacyParams_returnsMultipleHierarchies() = runTest {
        whenever(mockCatalystFlagProvider.catalystUseKeyParameters()).thenReturn(false)
        val screenKey = "legacy_parameterized_screen"

        val config = CatalystConfig(
            screenConfigs = listOf(
                PerScreenCatalystConfig(
                    enabled = true,
                    screenKey = screenKey,
                    appFunctionTypes = setOf(DeviceStateAppFunctionType.GET_UNCATEGORIZED)
                )
            )
        )

        val bundle1 = Bundle().apply { putString("key", "val1") }
        val bundle2 = Bundle().apply { putString("key", "val2") }

        val factory = object : PreferenceScreenMetadataParameterizedFactory {
            override val parametersSchema: KeyParametersSchema = KeyParametersSchema { }
            override fun create(context: Context, args: Bundle): PreferenceScreenMetadata {
                return object : PreferenceScreenMetadata {
                    override fun fragmentClass() = null
                    override fun getPreferenceHierarchy(context: Context, scope: CoroutineScope) =
                        preferenceHierarchy(context) { }

                    override val key = screenKey
                    override val purpose = 1
                }
            }

            override fun parameters(context: Context) = flowOf(bundle1, bundle2)
            override fun createWithKeyParameters(c: Context, p: ValidatedKeyParameters) = create(c)
            override fun keyParameters(context: Context) = flowOf<ValidatedKeyParameters>()
            override fun create(context: Context) = create(context, Bundle.EMPTY)
        }

        PreferenceScreenRegistry.preferenceScreenMetadataFactories = FixedArrayMap(1) {
            it.put(screenKey, factory)
        }

        val result = getEnabledPreferencesHierarchy(
            config = config,
            context = context,
            screenKey = screenKey
        )

        assertThat(result).hasSize(2)
    }

    @Test
    fun getEnabledPreferencesHierarchy_parameterizedScreen_removeDuplicates() = runTest {
        val screenKey = "parameterized_screen_dedup"

        val config = CatalystConfig(
            screenConfigs = listOf(
                PerScreenCatalystConfig(
                    enabled = true,
                    screenKey = screenKey,
                    appFunctionTypes = setOf(DeviceStateAppFunctionType.GET_UNCATEGORIZED)
                )
            )
        )

        val testSchema = KeyParametersSchema {
            parameter(KEY_PACKAGE_NAME, "Package", required = true, type = InstalledPackageName)
        }
        val params1 = testSchema.prepare(mapOf(KEY_PACKAGE_NAME to "com.android.settings"))
        val params2 = testSchema.prepare(mapOf(KEY_PACKAGE_NAME to "com.google.android.gm"))

        val factory = object : PreferenceScreenMetadataParameterizedFactory {
            override val parametersSchema: KeyParametersSchema = testSchema
            override fun create(context: Context, args: Bundle) = create(context)
            override fun parameters(context: Context) = flowOf<Bundle>()

            override fun createWithKeyParameters(
                context: Context,
                p: ValidatedKeyParameters
            ): PreferenceScreenMetadata {
                return object : PreferenceScreenMetadata {
                    override fun fragmentClass() = null
                    override fun getPreferenceHierarchy(c: Context, s: CoroutineScope) =
                        preferenceHierarchy(c) { }

                    override val key = screenKey
                    override val purpose = 1
                }
            }

            override fun keyParameters(context: Context) = flowOf(params1, params2)
            override fun create(context: Context) = createWithKeyParameters(context, mock())
        }

        PreferenceScreenRegistry.preferenceScreenMetadataFactories = FixedArrayMap(1) {
            it.put(screenKey, factory)
        }

        val result = getEnabledPreferencesHierarchy(
            config = config,
            context = context,
            screenKey = screenKey,
            removeDuplicates = true
        )

        assertThat(result).hasSize(1)
    }
}
