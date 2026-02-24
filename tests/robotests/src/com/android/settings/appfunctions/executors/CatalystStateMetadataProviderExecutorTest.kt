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
import com.android.settingslib.metadata.preferencesapi.preconditions.Allowed
import com.android.settingslib.metadata.preferencesapi.types.AnyString

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
    fun execute_onWritablePreference_notApiFirst_returnsNonWritableDeviceStateItem() = runTest {
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
        assertThat(result.metadata[0].deviceStateItemsMetadata).hasSize(2)
        assertThat(result.metadata[0].deviceStateItemsMetadata[1].key).isEqualTo(
            "screen_key/test_key_writable"
        )
        assertThat(result.metadata[0].deviceStateItemsMetadata[1].writable).isFalse()
    }


    @Test
    fun execute_onNonPersistentPreference_returnsUnwritableDeviceStateItem() = runTest {
        val metadata =
            TestPreferenceMetadata(
                bindingKey = "test_key_writable",
                isPersistent = false,
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

        // single screen
        assertThat(result.metadata).hasSize(1)
        // the screen itself and the preference
        assertThat(result.metadata[0].deviceStateItemsMetadata).hasSize(2)
        assertThat(result.metadata[0].deviceStateItemsMetadata[1].key).isEqualTo(
            "screen_key/test_key_writable"
        )
        assertThat(result.metadata[0].deviceStateItemsMetadata[1].writable).isFalse()
    }

    @Test
    fun execute_onDisallowedPermitPreference_returnsUnwritableDeviceStateItem() = runTest {
        val metadata =
            TestPreferenceMetadata(
                bindingKey = "test_key_not_writable",
                isPersistent = true,
                writePermit = ReadWritePermit.DISALLOW,
            )
        setRegistryFactories(
            createScreen(
                GraphTestUtils.PreferenceScreenConfig(
                    "screen_key",
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
        // the screen itself and the preference
        assertThat(result.metadata[0].deviceStateItemsMetadata).hasSize(2)
        assertThat(result.metadata[0].deviceStateItemsMetadata[1].key).isEqualTo(
            "screen_key/test_key_not_writable"
        )
        assertThat(result.metadata[0].deviceStateItemsMetadata[1].writable).isFalse()
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
        // the screen itself and the preference
        assertThat(result.metadata[0].deviceStateItemsMetadata).hasSize(2)
        assertThat(result.metadata[0].deviceStateItemsMetadata[1].key).isEqualTo(
            "screen_key/test_key_writable"
        )
        assertThat(result.metadata[0].deviceStateItemsMetadata[1].purpose).isEqualTo(
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

    //TODO (b/481263255) Additional description is ignored in metadata
    @Test
    fun execute_onScreenWithTitleAndAdditionalDescription_returnsTitleAndPurposeAsDescription() = runTest {
        val metadata =
            TestPreferenceMetadata(
                bindingKey = "test_key_writable",
                isPersistent = true,
                writePermit = ReadWritePermit.ALLOW,
            )
        setRegistryFactories(
            createScreen(
                GraphTestUtils.PreferenceScreenConfig(
                    screenKey = "screen_key",
                    title = R.string.preference_screen_title,
                    purpose = R.string.preference_screen_purpose,
                    preferences = listOf(metadata)
                )
            )
        )
        val executor = CatalystStateMetadataProviderExecutor(
            buildConfig(
                "screen_key",
                listOf("test_key_writable"),
                "Additional screen description"
            ),
            context,
            englishContext
        )

        val result = executor.execute(DeviceStateAppFunctionType.GET_METADATA)

        assertThat(result.metadata).hasSize(1)
        assertThat(result.metadata[0].description).isEqualTo (
            "${
                context.getString(R.string.preference_screen_title)
            }. ${
                context.getString(R.string.preference_screen_purpose)
            }"
        )
    }

    //TODO (b/481263255) Additional description is ignored in metadata
    @Test
    fun execute_onScreenWithoutTitleAndAdditionalDescription_returnsPurpose() = runTest {
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
                "Additional screen description"
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
    fun execute_onScreenWithoutTitleAndWithoutAdditionalDescription_returnsPurposeAsDescription() = runTest {
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
        // the screen itself and the 2 preferences
        assertThat(result.metadata[0].deviceStateItemsMetadata).hasSize(3)
        assertThat(result.metadata[0].deviceStateItemsMetadata[0].key).isEqualTo(
            "screen_key/screen_key"
        )
        assertThat(result.metadata[0].deviceStateItemsMetadata[1].key).isEqualTo(
            "screen_key/preference_key_1"
        )
        assertThat(result.metadata[0].deviceStateItemsMetadata[2].key).isEqualTo(
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
        // the screen itself and the non-ui only preference
        assertThat(result.metadata[0].deviceStateItemsMetadata).hasSize(2)
        assertThat(result.metadata[0].deviceStateItemsMetadata[0].key).isEqualTo(
            "screen_key/screen_key"
        )
        assertThat(result.metadata[0].deviceStateItemsMetadata[1].key).isEqualTo(
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
        // the screen itself only
        assertThat(result.metadata[0].deviceStateItemsMetadata).hasSize(1)
        assertThat(result.metadata[0].deviceStateItemsMetadata[0].key).isEqualTo(
            "screen_key/screen_key"
        )
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
                get { execute { "true" } }
                set { execute {} }
            }

            preference(
                key = "non_writable_pref",
                purpose = R.string.preference_purpose,
                type = AnyString,
            ) {
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

        val prefMetadata = result.metadata[0].deviceStateItemsMetadata[1]
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

        val prefMetadata = result.metadata[0].deviceStateItemsMetadata[2]
        assertThat(prefMetadata.key).isEqualTo("api_first_screen/non_writable_pref")
        assertThat(prefMetadata.writable).isFalse()
    }

    @Test
    fun execute_onApiFirstScreen_isNotWritable() = runTest {
        setRegistryFactories(ApiFirstTestScreen())
        val executor = CatalystStateMetadataProviderExecutor(
            buildConfig("api_first_screen", emptyList()), // empty list means all prefs on screen
            context,
            englishContext
        )

        val result = executor.execute(DeviceStateAppFunctionType.GET_METADATA)

        val screenMetadata = result.metadata[0].deviceStateItemsMetadata[0]
        assertThat(screenMetadata.key).isEqualTo("api_first_screen/api_first_screen")
        assertThat(screenMetadata.writable).isFalse()
        assertThat(result.metadata[0].deviceStateItemsMetadata[1].key).isEqualTo(
            "api_first_screen/writable_pref"
        )
        assertThat(result.metadata[0].deviceStateItemsMetadata[2].key).isEqualTo(
            "api_first_screen/non_writable_pref"
        )
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

        val prefMetadata = result.metadata[0].deviceStateItemsMetadata[1]
        assertThat(prefMetadata.key).isEqualTo("preconditions_screen/pref_with_preconditions")
        assertThat(prefMetadata.hintText).contains(
            "Preconditions to accessing: Screen precondition, Preference precondition.\n" +
                "Preconditions to reading: Get precondition.\n" +
                "Preconditions to writing: Set precondition."
        )
        assertThat(prefMetadata.hintText).contains("Preconditions to writing: Set precondition.")
    }

    @Test
    fun execute_onApiFirstScreen_doesNotIncludeName() = runTest {
        setRegistryFactories(ApiFirstTestScreen())
        val executor = CatalystStateMetadataProviderExecutor(
            buildConfig("api_first_screen", emptyList()),
            context,
            englishContext
        )

        val result = executor.execute(DeviceStateAppFunctionType.GET_METADATA)

        val screenMetadata = result.metadata[0].deviceStateItemsMetadata[0]
        assertThat(screenMetadata.key).isEqualTo("api_first_screen/api_first_screen")
        assertThat(screenMetadata.name).isNull()
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

        val prefMetadata = result.metadata[0].deviceStateItemsMetadata[1]
        assertThat(prefMetadata.key).isEqualTo("api_first_screen/writable_pref")
        assertThat(prefMetadata.name).isNull()
    }
}
