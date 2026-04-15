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
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.android.settings.appfunctions.DeviceStateAppFunctionType
import com.android.settings.testutils.appfunctions.CatalystConfigBuilder.buildConfig
import com.android.settingslib.metadata.PersistentPreference
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.testutils.GraphTestUtils
import com.android.settingslib.testutils.GraphTestUtils.PreferenceScreenConfig
import com.android.settingslib.testutils.GraphTestUtils.createPersistentPreference
import com.android.settingslib.testutils.GraphTestUtils.createScreen
import com.android.settingslib.testutils.GraphTestUtils.createSimplePreference
import com.android.settingslib.testutils.GraphTestUtils.setRegistryFactories
import android.os.Build
import android.provider.Settings
import com.google.common.truth.Truth.assertThat
import java.util.Locale
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowBuild
import org.robolectric.shadows.ShadowSettings
import androidx.fragment.app.Fragment
import com.android.settingslib.metadata.PreferenceScreenMetadata
import com.android.settingslib.metadata.PreferenceScreenMetadataFactory
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.graph.proto.KeyParametersProto
import com.android.settingslib.graph.proto.PossibleValueProto
import com.android.settingslib.graph.proto.PreferenceValueDescriptorProto
import com.android.settingslib.graph.proto.PreferenceValueProto
import com.android.settingslib.graph.proto.RangeValueProto
import com.android.settingslib.metadata.preferencesapi.preconditions.Allowed
import com.android.settingslib.metadata.preferencesapi.types.AnyString
import com.android.settingslib.metadata.preferencesapi.types.ApiType
import com.android.settingslib.metadata.preferencesapi.types.DirectFiniteOptionsType
import com.google.android.appfunctions.schema.common.v1.devicestate.ItemizationDetail
import com.android.settingslib.metadata.preferencesapi.SafetyAnnotated
import com.android.settingslib.metadata.preferencesapi.safe
import com.android.settingslib.metadata.preferencesapi.types.EType

@RunWith(RobolectricTestRunner::class)
class CatalystStateMetadataProviderExecutorTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()!!
    private val englishContext = context.createConfigurationContext(
        Configuration(context.resources.configuration).also {
            it.setLocale(Locale.ENGLISH)
        }
    )

    @Before
    fun setUp() {
        ShadowBuild.setType(Build.TYPE)
        ShadowSettings.reset()
    }


    @Test
    fun execute_onWritablePreference_notApiFirst_returnsWritableDeviceStateItem() = runTest {
        val metadata = TestPreferenceMetadata(
                bindingKey = "test_key_writable",
                isPersistent = true,
                writePermit = ReadWritePermit.ALLOW,
            )
        setRegistryFactories(
            createScreen(
                GraphTestUtils.PreferenceScreenConfig (
                    "screen_key",
                    purpose = R.string.preference_screen_purpose,
                    preferences = listOf(metadata)
                )
            )
        )
        val executor = CatalystStateMetadataProviderExecutor(
            buildConfig("screen_key", listOf("test_key_writable")),
            context,
            englishContext
        )

        val result = executor.execute(DeviceStateAppFunctionType.GET_METADATA)
        assertThat(result.metadata[0].deviceStateItemsMetadata).hasSize(1)
        assertThat(result.metadata[0].deviceStateItemsMetadata[0].key).isEqualTo(
            "screen_key/test_key_writable"
        )
        assertThat(result.metadata[0].deviceStateItemsMetadata[0].writable).isTrue()
    }

    @Test
    fun execute_onWritablePreference_notApiFirst_notWritable_returnsNotWritableDeviceStateItem() = runTest {
        val metadata = TestPreferenceMetadata(
                bindingKey = "test_key_writable",
                isPersistent = true,
                writePermit = null,
            )
        setRegistryFactories(
            createScreen(
                GraphTestUtils.PreferenceScreenConfig (
                    "screen_key",
                    purpose = R.string.preference_screen_purpose,
                    preferences = listOf(metadata)
                )
            )
        )
        val executor = CatalystStateMetadataProviderExecutor(
            buildConfig("screen_key", listOf("test_key_writable")),
            context,
            englishContext
        )

        val result = executor.execute(DeviceStateAppFunctionType.GET_METADATA)
        assertThat(result.metadata[0].deviceStateItemsMetadata).hasSize(1)
        assertThat(result.metadata[0].deviceStateItemsMetadata[0].key).isEqualTo(
            "screen_key/test_key_writable"
        )
        assertThat(result.metadata[0].deviceStateItemsMetadata[0].writable).isFalse()
    }

    @Test
    fun execute_onSimplePreference_returnsCorrectPreferencePurpose() = runTest {
        val metadata =
            TestPreferenceMetadata(
                bindingKey = "test_key_writable",
                isPersistent = true,
                writePermit = ReadWritePermit.ALLOW,
                purpose = R.string.preference_purpose,
            )
        setRegistryFactories(
            createScreen(
                GraphTestUtils.PreferenceScreenConfig (
                    screenKey = "screen_key",
                    purpose = R.string.preference_screen_purpose,
                    preferences = listOf(metadata)
                )
            )
        )
        val executor = CatalystStateMetadataProviderExecutor(
            buildConfig("screen_key", listOf("test_key_not_writable")),
            context,
            englishContext
        )

        val result = executor.execute(DeviceStateAppFunctionType.GET_METADATA)

        // single screen
        assertThat(result.metadata).hasSize(1)
        // the preference
        assertThat(result.metadata[0].deviceStateItemsMetadata).hasSize(1)
        assertThat(result.metadata[0].deviceStateItemsMetadata[0].key).isEqualTo(
            "screen_key/test_key_writable"
        )
        assertThat(result.metadata[0].deviceStateItemsMetadata[0].purpose).isEqualTo(
            context.getString(R.string.preference_purpose)
        )
    }

    @Test
    fun execute_onScreenWithTitle_returnsTitleAndPurposeAsDescription() = runTest {
        val metadata =
            TestPreferenceMetadata(
                bindingKey = "test_key_writable",
                purpose = R.string.preference_purpose,
                isPersistent = true,
                writePermit = ReadWritePermit.ALLOW,
            )
        setRegistryFactories(
            createScreen(
                GraphTestUtils.PreferenceScreenConfig(
                    "screen_key",
                    purpose = R.string.preference_screen_purpose,
                    title = R.string.preference_screen_title,
                    preferences = listOf(metadata)
                )
            )
        )
        val executor = CatalystStateMetadataProviderExecutor(
            buildConfig("screen_key", listOf("test_key_writable")),
            context,
            englishContext
        )

        val result = executor.execute(DeviceStateAppFunctionType.GET_METADATA)

        assertThat(result.metadata).hasSize(1)
        assertThat(result.metadata[0].description).isEqualTo(
            "${
                context.getString(R.string.preference_screen_title)
            }. ${
                context.getString(R.string.preference_screen_purpose)
            }"
        )
    }

    @Test
    fun execute_onScreenWithoutTitle_returnsPurpose() = runTest {
        setRegistryFactories(
            createScreen(
                GraphTestUtils.PreferenceScreenConfig (
                    "screen_key",
                    purpose = R.string.preference_screen_purpose,
                    title = 0,
                    preferences = listOf(
                        createSimplePreference(
                            GraphTestUtils.PreferenceConfig(
                                key = "preference_key",
                                purpose = R.string.preference_purpose,
                            )
                        )
                    )
                )
            )
        )
        val executor = CatalystStateMetadataProviderExecutor(
            buildConfig(
                "screen_key",
                listOf("preference_key"),
            ),
            context,
            englishContext
        )

        val result = executor.execute(DeviceStateAppFunctionType.GET_METADATA)

        assertThat(result.metadata).hasSize(1)
        assertThat(result.metadata[0].description).isEqualTo (
                context.getString(R.string.preference_screen_purpose)
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
        val executor = CatalystStateMetadataProviderExecutor(
            buildConfig(
                "screen_key",
                listOf("preference_key"),
            ),
            context,
            englishContext
        )

        val result = executor.execute(DeviceStateAppFunctionType.GET_METADATA)

        assertThat(result.metadata).hasSize(1)
        assertThat(result.metadata[0].description).isEqualTo(
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
        val executor = CatalystStateMetadataProviderExecutor(
            buildConfig("screen_key", listOf()),
            context,
            englishContext
        )

        val result = executor.execute(DeviceStateAppFunctionType.GET_METADATA)

        // single screen
        assertThat(result.metadata).hasSize(1)
        // the 2 preferences
        assertThat(result.metadata[0].deviceStateItemsMetadata).hasSize(2)
        assertThat(result.metadata[0].deviceStateItemsMetadata[0].key).isEqualTo(
            "screen_key/preference_key_1"
        )
        assertThat(result.metadata[0].deviceStateItemsMetadata[1].key).isEqualTo(
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
        val executor = CatalystStateMetadataProviderExecutor(
            buildConfig("screen_key", listOf()),
            context,
            englishContext
        )

        val result = executor.execute(DeviceStateAppFunctionType.GET_METADATA)

        // single screen
        assertThat(result.metadata).hasSize(1)
        // the non-ui only preference
        assertThat(result.metadata[0].deviceStateItemsMetadata).hasSize(1)
        assertThat(result.metadata[0].deviceStateItemsMetadata[0].key).isEqualTo(
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
        val executor = CatalystStateMetadataProviderExecutor(
            buildConfig("screen_key", listOf("ui_only_preference")),
            context,
            englishContext
        )

        val result = executor.execute(DeviceStateAppFunctionType.GET_METADATA)

        // single screen
        assertThat(result.metadata).hasSize(1)
        // no preferences
        assertThat(result.metadata[0].deviceStateItemsMetadata).hasSize(0)
    }

    class TestPreferenceMetadata(
        override val bindingKey: String,
        override val purpose: Int = R.string.preference_purpose,
        private val isPersistent: Boolean,
        val writePermit: Int?,
    ) : PersistentPreference<Boolean> {
        override val key: String
            get() = bindingKey

        override val title: Int = android.R.string.ok

        override fun isPersistent(context: Context): Boolean = isPersistent

        override val valueType: Class<Boolean> = Boolean::class.java
        override val sensitivityLevel: Int = SensitivityLevel.NO_SENSITIVITY

        override fun getWritePermit(context: Context, callingPid: Int, callingUid: Int): Int? =
            writePermit

        override val supportsWrite = writePermit != null

        override fun storage(context: Context) = GraphTestUtils.createStorage(null, bindingKey)
    }

    private class ApiFirstTestScreen : PreferencesApiScreen(
        key = "api_first_screen",
        topLevelSettingsCategory = Category.SYSTEM,
        fragment = Fragment::class,
        purpose = R.string.preference_screen_purpose,
    ) {
        init {
            preference(
                key = "writable_pref",
                purpose = R.string.preference_purpose,
                type = AnyString,
            ) {
                sensitivityLevel(SensitivityLevel.NO_SENSITIVITY)
                get { execute { "true" } }
                set { execute {} }
            }

            preference(
                key = "non_writable_pref",
                purpose = R.string.preference_purpose,
                type = AnyString,
            ) {
                sensitivityLevel(SensitivityLevel.NO_SENSITIVITY)
                get { execute { "true" } }
            }
        }
    }

    @Test
    fun execute_onDebuggableBuildWithSetting_includesScreenKeyInDescription() = runTest {
        ShadowBuild.setType("userdebug")
        Settings.Global.putInt(
            context.contentResolver,
            "com.android.settings.APP_FUNCTION_INCLUDE_SCREEN_KEY_IN_DESCRIPTION",
            1,
        )
        setRegistryFactories(
            createScreen(
                GraphTestUtils.PreferenceScreenConfig(
                    "screen_key",
                    purpose = R.string.preference_screen_purpose,
                    title = R.string.preference_screen_title,
                    preferences = emptyList(),
                )
            )
        )
        val executor =
            CatalystStateMetadataProviderExecutor(
                buildConfig("screen_key", emptyList()),
                context,
                englishContext
            )

        val result = executor.execute(DeviceStateAppFunctionType.GET_METADATA)

        assertThat(result.metadata[0].description).contains("[key=screen_key]")
    }

    @Test
    fun execute_onDebuggableBuildWithoutSetting_doesNotIncludeScreenKeyInDescription() = runTest {
        ShadowBuild.setType("userdebug")
        Settings.Global.putInt(
            context.contentResolver,
            "com.android.settings.APP_FUNCTION_INCLUDE_SCREEN_KEY_IN_DESCRIPTION",
            0,
        )
        setRegistryFactories(
            createScreen(
                GraphTestUtils.PreferenceScreenConfig(
                    "screen_key",
                    purpose = R.string.preference_screen_purpose,
                    title = R.string.preference_screen_title,
                    preferences = emptyList(),
                )
            )
        )
        val executor =
            CatalystStateMetadataProviderExecutor(
                buildConfig("screen_key", emptyList()),
                context,
                englishContext
            )

        val result = executor.execute(DeviceStateAppFunctionType.GET_METADATA)

        assertThat(result.metadata[0].description).doesNotContain("[key=screen_key]")
    }

    @Test
    fun execute_onNonDebuggableBuildWithSetting_doesNotIncludeScreenKeyInDescription() = runTest {
        ShadowBuild.setType("user")
        Settings.Global.putInt(
            context.contentResolver,
            "com.android.settings.APP_FUNCTION_INCLUDE_SCREEN_KEY_IN_DESCRIPTION",
            1,
        )
        setRegistryFactories(
            createScreen(
                GraphTestUtils.PreferenceScreenConfig(
                    "screen_key",
                    purpose = R.string.preference_screen_purpose,
                    title = R.string.preference_screen_title,
                    preferences = emptyList(),
                )
            )
        )
        val executor =
            CatalystStateMetadataProviderExecutor(
                buildConfig("screen_key", emptyList()),
                context,
                englishContext
            )

        val result = executor.execute(DeviceStateAppFunctionType.GET_METADATA)

        assertThat(result.metadata[0].description).doesNotContain("[key=screen_key]")
    }

    @Test
    fun execute_onApiFirstPreferenceWithSetter_isWritable() = runTest {
        setRegistryFactories(ApiFirstTestScreen())
        val executor = CatalystStateMetadataProviderExecutor(
            buildConfig("api_first_screen", listOf("writable_pref")),
            context,
            englishContext
        )

        val result = executor.execute(DeviceStateAppFunctionType.GET_METADATA)

        val prefMetadata = result.metadata[0].deviceStateItemsMetadata[0]
        assertThat(prefMetadata.key).isEqualTo("api_first_screen/writable_pref")
        assertThat(prefMetadata.writable).isTrue()
    }

    @Test
    fun execute_onApiFirstPreferenceWithoutSetter_isNotWritable() = runTest {
        setRegistryFactories(ApiFirstTestScreen())
        val executor = CatalystStateMetadataProviderExecutor(
            buildConfig("api_first_screen", listOf("non_writable_pref")),
            context,
            englishContext
        )

        val result = executor.execute(DeviceStateAppFunctionType.GET_METADATA)

        val prefMetadata = result.metadata[0].deviceStateItemsMetadata[1]
        assertThat(prefMetadata.key).isEqualTo("api_first_screen/non_writable_pref")
        assertThat(prefMetadata.writable).isFalse()
    }

    private class PreconditionsTestScreen : PreferencesApiScreen(
        key = "preconditions_screen",
        topLevelSettingsCategory = Category.SYSTEM,
        fragment = Fragment::class,
        purpose = R.string.preference_screen_purpose,
    ) {
        init {
            preconditions("Screen precondition") { Allowed }

            preference(
                key = "pref_with_preconditions",
                purpose = R.string.preference_purpose,
                type = AnyString,
            ) {
                sensitivityLevel(SensitivityLevel.NO_SENSITIVITY)

                preconditions("Preference precondition") { Allowed }
                get {
                    preconditions("Get precondition") { Allowed }
                    execute { "value" }
                }
                set {
                    preconditions("Set precondition") { Allowed }
                    execute { }
                }
            }
        }
    }

    @Test
    fun execute_onPreferenceWithPreconditions_includesPreconditionsInHintText() = runTest {
        setRegistryFactories(PreconditionsTestScreen())
        val executor = CatalystStateMetadataProviderExecutor(
            buildConfig("preconditions_screen", listOf("pref_with_preconditions")),
            context,
            englishContext
        )

        val result = executor.execute(DeviceStateAppFunctionType.GET_METADATA)

        val prefMetadata = result.metadata[0].deviceStateItemsMetadata[0]
        assertThat(prefMetadata.key).isEqualTo("preconditions_screen/pref_with_preconditions")
        assertThat(prefMetadata.hintText).contains(
            "Preconditions to accessing: Screen precondition, Preference precondition.\n" +
                "Preconditions to getting: Get precondition.\n" +
                "Preconditions to setting: Set precondition."
        )
        assertThat(prefMetadata.hintText).contains("Preconditions to setting: Set precondition.")
    }

    private class SetWarningTestScreen : PreferencesApiScreen(
        key = "set_warning_screen",
        topLevelSettingsCategory = Category.SYSTEM,
        fragment = Fragment::class,
        purpose = R.string.preference_screen_purpose,
    ) {
        init {
            preference(
                key = "pref_with_set_warning_without_preconditions",
                purpose = R.string.preference_purpose,
                type = AnyString,
            ) {
                sensitivityLevel(SensitivityLevel.NO_SENSITIVITY)
                get {
                    execute { "value" }
                }
                set {
                    warning {
                        warn("Set warning 1")
                    }
                    execute { }
                }
            }

            preference(
                key = "pref_with_set_warning_with_preconditions",
                purpose = R.string.preference_purpose,
                type = AnyString,
            ) {
                sensitivityLevel(SensitivityLevel.NO_SENSITIVITY)
                get {
                    execute { "value" }
                }
                set {
                    warning {
                        preconditions("Set preconditions") {
                            Allowed
                        }
                        warn("Set warning 2")
                    }
                    execute { }
                }
            }

            preference(
                key = "pref_with_set_warning_with_value_preconditions",
                purpose = R.string.preference_purpose,
                type = AnyString,
            ) {
                sensitivityLevel(SensitivityLevel.NO_SENSITIVITY)
                get {
                    execute { "value" }
                }
                set {
                    warning {
                        valuePreconditions("Set value preconditions") { _ ->
                            Allowed
                        }
                        warn("Set warning 3")
                    }
                    execute { }
                }
            }
        }
    }

    @Test
    fun execute_onPreferenceWithSetWarning_includesPreconditionsAndValuePreconditionsAndWarningMessageInHintText() = runTest {
        setRegistryFactories(SetWarningTestScreen())
        val executor = CatalystStateMetadataProviderExecutor(
            buildConfig(
                "set_warning_screen",
                listOf(
                    "pref_with_set_warning_without_preconditions",
                    "pref_with_set_warning_with_preconditions",
                    "pref_with_set_warning_with_value_preconditions"
                )
            ),
            context,
            englishContext
        )

        val result = executor.execute(DeviceStateAppFunctionType.GET_METADATA)

        val prefSetWarningWithoutPreconditionsMetadata = result.metadata[0].deviceStateItemsMetadata[0]
        assertThat(prefSetWarningWithoutPreconditionsMetadata.key).isEqualTo("set_warning_screen/pref_with_set_warning_without_preconditions")
        assertThat(prefSetWarningWithoutPreconditionsMetadata.hintText).contains("[Must show to user]: Set warning 1.")

        val prefSetWarningWithPreconditionsMetadata = result.metadata[0].deviceStateItemsMetadata[1]
        assertThat(prefSetWarningWithPreconditionsMetadata.key).isEqualTo("set_warning_screen/pref_with_set_warning_with_preconditions")
        assertThat(prefSetWarningWithPreconditionsMetadata.hintText).contains("[Must show to user]: Set warning 2 (if preconditions are met: Set preconditions).")

        val prefSetWarningWithValuePreconditionsMetadata = result.metadata[0].deviceStateItemsMetadata[2]
        assertThat(prefSetWarningWithValuePreconditionsMetadata.key).isEqualTo("set_warning_screen/pref_with_set_warning_with_value_preconditions")
        assertThat(prefSetWarningWithValuePreconditionsMetadata.hintText).contains("[Must show to user]: Set warning 3 (if preconditions are met: Set value preconditions).")
    }

    @Test
    fun execute_onApiFirstPreference_doesNotIncludeName() = runTest {
        setRegistryFactories(ApiFirstTestScreen())
        val executor = CatalystStateMetadataProviderExecutor(
            buildConfig("api_first_screen", listOf("writable_pref")),
            context,
            englishContext
        )

        val result = executor.execute(DeviceStateAppFunctionType.GET_METADATA)

        val prefMetadata = result.metadata[0].deviceStateItemsMetadata[0]
        assertThat(prefMetadata.key).isEqualTo("api_first_screen/writable_pref")
        assertThat(prefMetadata.name).isNull()
    }

    @Test
    fun execute_onDoNotExposeScreen_doesNotIncludeAnyOfItsData() = runTest {
        setRegistryFactories(
            createScreen(
                PreferenceScreenConfig(
                    screenKey = "do_not_expose_screen_key",
                    purpose = R.string.preference_screen_purpose,
                    preferences = listOf(
                        createPersistentPreference<Boolean>(
                            persistentPreferenceConfig = GraphTestUtils.PersistentPreferenceConfig(
                                preferenceConfig = GraphTestUtils.PreferenceConfig(
                                    key = "no_sensitivity_preference",
                                    purpose = R.string.preference_purpose,
                                    sensitivityLevel = SensitivityLevel.NO_SENSITIVITY
                                ),
                            )
                        )
                    ),
                    sensitivityLevel = SensitivityLevel.DO_NOT_EXPOSE
                )
            )
        )
        val executor = CatalystStateMetadataProviderExecutor(
            buildConfig("do_not_expose_screen_key", listOf()),
            context,
            englishContext
        )

        val deviceStateResult = executor.execute(DeviceStateAppFunctionType.GET_METADATA)
        assertThat(deviceStateResult.metadata).hasSize(0)
    }

    @Test
    fun execute_onDoNotExposePreference_doesNotIncludeIt() = runTest {
        setRegistryFactories(
            createScreen(
                PreferenceScreenConfig(
                    screenKey = "no_sensitivity_screen_key",
                    purpose = R.string.preference_screen_purpose,
                    preferences = listOf(
                        createPersistentPreference<Boolean>(
                            persistentPreferenceConfig = GraphTestUtils.PersistentPreferenceConfig(
                                preferenceConfig = GraphTestUtils.PreferenceConfig(
                                    key = "do_not_expose_preference",
                                    purpose = R.string.preference_purpose,
                                    sensitivityLevel = SensitivityLevel.DO_NOT_EXPOSE
                                ),
                            )
                        )
                    ),
                    sensitivityLevel = SensitivityLevel.NO_SENSITIVITY
                )
            )
        )
        val executor = CatalystStateMetadataProviderExecutor(
            buildConfig("no_sensitivity_screen_key", listOf()),
            context,
            englishContext
        )

        val deviceStateResult = executor.execute(DeviceStateAppFunctionType.GET_METADATA)
        assertThat(deviceStateResult.metadata).hasSize(1)
        assertThat(deviceStateResult.metadata[0].deviceStateItemsMetadata).hasSize(0)
    }

    @Test
    fun execute_onNoSensitivityScreenAndPreference_includesThem() = runTest {
        setRegistryFactories(
            createScreen(
                PreferenceScreenConfig(
                    screenKey = "no_sensitivity_screen_key",
                    purpose = R.string.preference_screen_purpose,
                    preferences = listOf(
                        createPersistentPreference<Boolean>(
                            persistentPreferenceConfig = GraphTestUtils.PersistentPreferenceConfig(
                                preferenceConfig = GraphTestUtils.PreferenceConfig(
                                    key = "no_sensitivity_preference",
                                    purpose = R.string.preference_purpose,
                                    sensitivityLevel = SensitivityLevel.NO_SENSITIVITY
                                ),
                            )
                        )
                    ),
                    sensitivityLevel = SensitivityLevel.NO_SENSITIVITY
                )
            )
        )
        val executor = CatalystStateMetadataProviderExecutor(
            buildConfig("no_sensitivity_screen_key", listOf()),
            context,
            englishContext
        )

        val deviceStateResult = executor.execute(DeviceStateAppFunctionType.GET_METADATA)

        assertThat(deviceStateResult.metadata).hasSize(1)
        assertThat(deviceStateResult.metadata[0].deviceStateItemsMetadata).hasSize(1)
        assertThat(deviceStateResult.metadata[0].deviceStateItemsMetadata[0].key).isEqualTo(
            "no_sensitivity_screen_key/no_sensitivity_preference"
        )
    }

    @Test
    fun execute_onNoSensitivityNestedScreens_includesOnlyOuterScreen() = runTest {
        val innerSensitiveScreen = createScreen(
            PreferenceScreenConfig(
                screenKey = "inner_no_sensitive_screen_key",
                purpose = R.string.preference_screen_purpose,
                preferences = listOf(
                    createPersistentPreference<Boolean>(
                        persistentPreferenceConfig = GraphTestUtils.PersistentPreferenceConfig(
                            preferenceConfig = GraphTestUtils.PreferenceConfig(
                                key = "inner_no_sensitivity_preference",
                                purpose = R.string.preference_purpose,
                                sensitivityLevel = SensitivityLevel.NO_SENSITIVITY
                            ),
                        )
                    )
                ),
                summary = R.string.preference_screen_summary,
                sensitivityLevel = SensitivityLevel.NO_SENSITIVITY
            )
        )
        val outerNoSensitivityScreen = createScreen(
            PreferenceScreenConfig(
                screenKey = "outer_no_sensitivity_screen_key",
                purpose = R.string.preference_screen_purpose,
                preferences = listOf(
                    createPersistentPreference<Boolean>(
                        persistentPreferenceConfig = GraphTestUtils.PersistentPreferenceConfig(
                            preferenceConfig = GraphTestUtils.PreferenceConfig(
                                key = "outer_no_sensitivity_preference",
                                purpose = R.string.preference_purpose,
                                sensitivityLevel = SensitivityLevel.NO_SENSITIVITY
                            ),
                        )
                    ),
                    innerSensitiveScreen
                ),
                sensitivityLevel = SensitivityLevel.NO_SENSITIVITY
            )
        )
        setRegistryFactories(innerSensitiveScreen, outerNoSensitivityScreen)
        val executor = CatalystStateMetadataProviderExecutor(
            buildConfig("outer_no_sensitivity_screen_key", listOf()),
            context,
            englishContext
        )

        val deviceStateResult = executor.execute(DeviceStateAppFunctionType.GET_METADATA)

        assertThat(deviceStateResult.metadata).hasSize(1)
        assertThat(deviceStateResult.metadata[0].deviceStateItemsMetadata).hasSize(1)
        assertThat(deviceStateResult.metadata[0].deviceStateItemsMetadata[0].key).isEqualTo("outer_no_sensitivity_screen_key/outer_no_sensitivity_preference")
    }

    @Test
    fun invoke_onExposableScreenWithInnerNonExposableScreen_onlyIncludesOuterScreen() = runTest {
        val innerSensitiveScreen = createScreen(
            PreferenceScreenConfig(
                screenKey = "inner_sensitive_screen_key",
                purpose = R.string.preference_screen_purpose,
                preferences = listOf(
                    createPersistentPreference<Boolean>(
                        persistentPreferenceConfig = GraphTestUtils.PersistentPreferenceConfig(
                            preferenceConfig = GraphTestUtils.PreferenceConfig(
                                key = "inner_no_sensitivity_preference",
                                purpose = R.string.preference_purpose,
                                sensitivityLevel = SensitivityLevel.NO_SENSITIVITY
                            ),
                        )
                    )
                ),
                sensitivityLevel = SensitivityLevel.DO_NOT_EXPOSE,
                summary = R.string.preference_screen_summary,
            )
        )
        val outerNoSensitivityScreen = createScreen(
            PreferenceScreenConfig(
                screenKey = "outer_no_sensitivity_screen_key",
                purpose = R.string.preference_screen_purpose,
                preferences = listOf(
                    createPersistentPreference<Boolean>(
                        persistentPreferenceConfig = GraphTestUtils.PersistentPreferenceConfig(
                            preferenceConfig = GraphTestUtils.PreferenceConfig(
                                key = "outer_no_sensitivity_preference",
                                purpose = R.string.preference_purpose,
                                sensitivityLevel = SensitivityLevel.NO_SENSITIVITY
                            ),
                        )
                    ),
                    innerSensitiveScreen
                ),
                sensitivityLevel = SensitivityLevel.NO_SENSITIVITY,
                summary = R.string.preference_screen_summary,
            )
        )
        setRegistryFactories(innerSensitiveScreen, outerNoSensitivityScreen)

        val executor = CatalystStateMetadataProviderExecutor(
            buildConfig("outer_no_sensitivity_screen_key", listOf()),
            context,
            englishContext
        )

        val deviceStateResult = executor.execute(DeviceStateAppFunctionType.GET_METADATA)

        assertThat(deviceStateResult.metadata).hasSize(1)
        assertThat(deviceStateResult.metadata[0].deviceStateItemsMetadata).hasSize(1)
        assertThat(deviceStateResult.metadata[0].deviceStateItemsMetadata[0].key).isEqualTo("outer_no_sensitivity_screen_key/outer_no_sensitivity_preference")
    }

    @Test
    fun execute_onNoSensitivityScreenWithCategoriesAndVariousSensitivities_hasOnlyNonSensitivityPreferences() = runTest {
        val noSensPref = createPersistentPreference<Boolean>(
            GraphTestUtils.PersistentPreferenceConfig(
                GraphTestUtils.PreferenceConfig(
                    key = "no_sens_pref",
                    purpose = R.string.preference_purpose,
                    sensitivityLevel = SensitivityLevel.NO_SENSITIVITY
                ),
                readPermission = null,
                writePermission = null
            )
        )
        val sensPref = createPersistentPreference<Boolean>(
            GraphTestUtils.PersistentPreferenceConfig(
                GraphTestUtils.PreferenceConfig(
                    key = "sens_pref",
                    purpose = R.string.preference_purpose,
                    sensitivityLevel = SensitivityLevel.DO_NOT_EXPOSE
                ),
                readPermission = null,
                writePermission = null
            )
        )
        val noSensPrefInOuter = createPersistentPreference<Boolean>(
            GraphTestUtils.PersistentPreferenceConfig(
                GraphTestUtils.PreferenceConfig(
                    key = "no_sens_outer",
                    purpose = R.string.preference_purpose,
                    sensitivityLevel = SensitivityLevel.NO_SENSITIVITY,
                ),
                readPermission = null,
                writePermission = null
            )
        )
        val sensPrefInInner = createPersistentPreference<Boolean>(
            GraphTestUtils.PersistentPreferenceConfig(
                    GraphTestUtils.PreferenceConfig(
                    key = "sens_inner",
                    purpose = R.string.preference_purpose,
                    sensitivityLevel = SensitivityLevel.DO_NOT_EXPOSE
                ),
                readPermission = null,
                writePermission = null
            )
        )

        val innerCategory = GraphTestUtils.PreferenceCategoryConfig(
            key = "inner_category",
            preferences = listOf(sensPrefInInner)
        )
        val outerCategory = GraphTestUtils.PreferenceCategoryConfig(
            key = "outer_category",
            preferences = listOf(noSensPrefInOuter),
            innerCategories = listOf(innerCategory)
        )

        setRegistryFactories(
            createScreen(
                PreferenceScreenConfig(
                    screenKey = "test_screen",
                    purpose = R.string.preference_screen_purpose,
                    preferences = listOf(noSensPref, sensPref),
                    preferencesInCategories = listOf(outerCategory),
                    sensitivityLevel = SensitivityLevel.NO_SENSITIVITY
                )
            )
        )
        val executor = CatalystStateMetadataProviderExecutor(
            buildConfig("test_screen", listOf()),
            context,
            englishContext
        )

        val result = executor.execute(DeviceStateAppFunctionType.GET_METADATA)

        assertThat(result.metadata).hasSize(1)
        val screenMetadata = result.metadata[0]
        assertThat(screenMetadata.deviceStateItemsMetadata).hasSize(2)
        assertThat(screenMetadata.deviceStateItemsMetadata[0].key).isEqualTo("test_screen/no_sens_pref")
        assertThat(screenMetadata.deviceStateItemsMetadata[1].key).isEqualTo("test_screen/no_sens_outer")
    }

    @Test
    fun invoke_onScreenWithFlagDisabled_notIncluded() = runTest {
        val innerSensitiveScreen = createScreen(
            PreferenceScreenConfig(
                screenKey = "flag_disabled_screen_key",
                purpose = R.string.preference_screen_purpose,
                preferences = listOf(
                    createPersistentPreference<Boolean>(
                        persistentPreferenceConfig = GraphTestUtils.PersistentPreferenceConfig(
                            preferenceConfig = GraphTestUtils.PreferenceConfig(
                                key = "inner_preference",
                                purpose = R.string.preference_purpose,
                                sensitivityLevel = SensitivityLevel.NO_SENSITIVITY
                            ),
                        )
                    )
                ),
                sensitivityLevel = SensitivityLevel.DO_NOT_EXPOSE,
                summary = R.string.preference_screen_summary,
                isFlagEnabled = false,
            )
        )

        setRegistryFactories(innerSensitiveScreen)

        val executor = CatalystStateMetadataProviderExecutor(
            buildConfig("flag_disabled_screen_key", listOf()),
            context,
            englishContext
        )

        val deviceStateResult = executor.execute(DeviceStateAppFunctionType.GET_METADATA)

        assertThat(deviceStateResult.metadata).hasSize(0)
    }

    @Test
    fun toDeviceStateString_booleanType_returnsBOOL() {
        val proto = PreferenceValueDescriptorProto.newBuilder()
            .setBooleanType(true)
            .build()

        with(CatalystStateMetadataProviderExecutor) {
            assertThat(proto.toDeviceStateString()).isEqualTo("BOOL")
        }
    }

    @Test
    fun toDeviceStateString_floatType_returnsFLOAT() {
        val proto = PreferenceValueDescriptorProto.newBuilder()
            .setFloatType(true)
            .build()

        with(CatalystStateMetadataProviderExecutor) {
            assertThat(proto.toDeviceStateString()).isEqualTo("FLOAT")
        }
    }

    @Test
    fun toDeviceStateString_longType_returnsLONG() {
        val proto = PreferenceValueDescriptorProto.newBuilder()
            .setLongType(true)
            .build()

        with(CatalystStateMetadataProviderExecutor) {
            assertThat(proto.toDeviceStateString()).isEqualTo("LONG")
        }
    }

    @Test
    fun toDeviceStateString_stringType_returnsSTRING() {
        val proto = PreferenceValueDescriptorProto.newBuilder()
            .setStringType(true)
            .build()

        with(CatalystStateMetadataProviderExecutor) {
            assertThat(proto.toDeviceStateString()).isEqualTo("STRING")
        }
    }

    @Test
    fun toDeviceStateString_rangeType_withMinMaxStep_returnsINTEGERWithValues() {
        val range = RangeValueProto.newBuilder()
            .setMin(0)
            .setMax(10)
            .setStep(2)
            .build()
        val proto = PreferenceValueDescriptorProto.newBuilder()
            .setRangeValue(range)
            .build()

        with(CatalystStateMetadataProviderExecutor) {
            assertThat(proto.toDeviceStateString()).isEqualTo("INTEGER(min=0, max=10, step=2)")
        }
    }

    @Test
    fun toDeviceStateString_rangeType_withMinOnly_returnsINTEGERWithMin() {
        val range = RangeValueProto.newBuilder()
            .setMin(5)
            .build()
        val proto = PreferenceValueDescriptorProto.newBuilder()
            .setRangeValue(range)
            .build()

        with(CatalystStateMetadataProviderExecutor) {
            assertThat(proto.toDeviceStateString()).isEqualTo("INTEGER(min=5)")
        }
    }

    @Test
    fun toDeviceStateString_rangeType_empty_returnsINTEGER() {
        val range = RangeValueProto.newBuilder().build()
        val proto = PreferenceValueDescriptorProto.newBuilder()
            .setRangeValue(range)
            .build()

        with(CatalystStateMetadataProviderExecutor) {
            assertThat(proto.toDeviceStateString()).isEqualTo("INTEGER")
        }
    }

    @Test
    fun toDeviceStateString_possibleValues_returnsValuesAndDescriptions() {
        val value1 = PossibleValueProto.newBuilder()
            .setValue(PreferenceValueProto.newBuilder().setBooleanValue(true))
            .setDescription("On")
            .build()
        val value2 = PossibleValueProto.newBuilder()
            .setValue(PreferenceValueProto.newBuilder().setBooleanValue(false))
            .setDescription("Off")
            .build()

        val proto = PreferenceValueDescriptorProto.newBuilder()
            .addPossibleValues(value1)
            .addPossibleValues(value2)
            .build()

        with(CatalystStateMetadataProviderExecutor) {
            assertThat(proto.toDeviceStateString()).isEqualTo("true (On), false (Off)")
        }
    }

    @Test
    fun toDeviceStateString_withParameters_returnsStringWithParameters() {
        val parameters = KeyParametersProto.newBuilder()
            .putValues("param1", "value1")
            .putValues("param2", "value2")
            .build()
        val proto = PreferenceValueDescriptorProto.newBuilder()
            .setStringType(true)
            .setParameters(parameters)
            .build()

        with(CatalystStateMetadataProviderExecutor) {
            assertThat(proto.toDeviceStateString()).isEqualTo("STRING [param1=value1,param2=value2]")
        }
    }

    @Test
    fun execute_includesItemizationTypes() = runTest {
        setRegistryFactories(ScreenWithItemizationType())
        val executor = CatalystStateMetadataProviderExecutor(
            buildConfig("screen_with_itemization", listOf("pref_with_itemization")),
            context,
            englishContext
        )

        val result = executor.execute(DeviceStateAppFunctionType.GET_METADATA)

        assertThat(result.itemizationTypes).hasSize(1)
        val itemizationType = result.itemizationTypes.first()
        assertThat(itemizationType.key).isEqualTo("test_enum")
        assertThat(itemizationType.hintText).isEqualTo("Test Enum Description")
        assertThat(itemizationType.values).containsExactly(
            ItemizationDetail(key = "OPTION_1", value = "Option 1"),
            ItemizationDetail(key = "OPTION_2", value = "Option 2")
        )
    }

    private class ScreenWithItemizationType : PreferencesApiScreen(
        key = "screen_with_itemization",
        topLevelSettingsCategory = Category.SYSTEM,
        fragment = Fragment::class,
        purpose = R.string.preference_screen_purpose,
    ) {
        init {
            preference(
                key = "pref_with_itemization",
                purpose = R.string.preference_purpose,
                type = TestEnumType,
            ) {
                sensitivityLevel(SensitivityLevel.NO_SENSITIVITY)
                get { execute { "OPTION_1" } }
            }
        }
    }

    private object TestEnumType : DirectFiniteOptionsType<String> {
        override val externalType = EType.String
        override fun getKey(): String = "test_enum"
        override fun getDescription(context: Context): String = "Test Enum Description"
        override suspend fun getOptions(context: Context): List<Pair<SafetyAnnotated<String>, SafetyAnnotated<String>>> = listOf(
            "OPTION_1".safe() to "Option 1".safe(),
            "OPTION_2".safe() to "Option 2".safe()
        )
    }
}
