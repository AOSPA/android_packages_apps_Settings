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
import android.content.Intent
import androidx.fragment.app.Fragment
import androidx.test.core.app.ApplicationProvider
import com.android.settings.appfunctions.CatalystConfig
import com.android.settings.appfunctions.DeviceStateItemConfig
import com.android.settings.appfunctions.PerScreenCatalystConfig
import com.android.settingslib.metadata.PersistentPreference
import com.android.settingslib.metadata.PreferenceHierarchy
import com.android.settingslib.metadata.PreferenceHierarchyNode
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceScreenMetadata
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.preferenceHierarchy
import com.google.android.appfunctions.schema.common.v1.devicestate.PerScreenMetadata
import com.google.common.truth.Truth.assertThat
import java.lang.reflect.Method
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CatalystStateMetadataProviderExecutorTest {

    private lateinit var executor: CatalystStateMetadataProviderExecutor

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        executor = CatalystStateMetadataProviderExecutor(mockConfig, context, context)
    }

    @Test
    fun buildPerScreenDeviceStatesMetadata_writableWhenAllowAndPersistent() = runTest {
        val metadata =
            TestPreferenceMetadata(
                bindingKey = "test_key_writable",
                isPersistent = true,
                writePermit = ReadWritePermit.ALLOW,
            )
        val preferencesHierarchy = listOf(createPreferenceHierarchyNode(metadata))

        val result =
            callBuildPerScreenDeviceStatesMetadata(testScreenMetadata, preferencesHierarchy)

        assertThat(result.deviceStateItemsMetadata).hasSize(1)
        assertThat(result.deviceStateItemsMetadata[0].writable).isTrue()
    }

    @Test
    fun buildPerScreenDeviceStatesMetadata_notWritableWhenNotPersistent() = runTest {
        val metadata =
            TestPreferenceMetadata(
                bindingKey = "test_key_writable",
                isPersistent = false,
                writePermit = ReadWritePermit.ALLOW,
            )
        val preferencesHierarchy = listOf(createPreferenceHierarchyNode(metadata))

        val result =
            callBuildPerScreenDeviceStatesMetadata(testScreenMetadata, preferencesHierarchy)

        assertThat(result.deviceStateItemsMetadata).hasSize(1)
        assertThat(result.deviceStateItemsMetadata[0].writable).isFalse()
    }

    @Test
    fun buildPerScreenDeviceStatesMetadata_notWritableWhenNotAllow() = runTest {
        val metadata =
            TestPreferenceMetadata(
                bindingKey = "test_key_not_writable",
                isPersistent = true,
                writePermit = ReadWritePermit.DISALLOW,
            )
        val preferencesHierarchy = listOf(createPreferenceHierarchyNode(metadata))

        val result =
            callBuildPerScreenDeviceStatesMetadata(testScreenMetadata, preferencesHierarchy)

        assertThat(result.deviceStateItemsMetadata).hasSize(1)
        assertThat(result.deviceStateItemsMetadata[0].writable).isFalse()
    }

    private fun CoroutineScope.callBuildPerScreenDeviceStatesMetadata(
        screenMetadata: PreferenceScreenMetadata,
        preferencesHierarchy: List<PreferenceHierarchyNode>,
    ): PerScreenMetadata {
        val method: Method =
            CatalystStateMetadataProviderExecutor::class
                .java
                .getDeclaredMethod(
                    "buildPerScreenDeviceStatesMetadata",
                    CoroutineScope::class.java,
                    PreferenceScreenMetadata::class.java,
                    java.util.List::class.java,
                    Boolean::class.javaPrimitiveType,
                    Class.forName("kotlin.coroutines.Continuation"),
                )
        method.isAccessible = true
        return method.invoke(executor, this, screenMetadata, preferencesHierarchy, false, null)
            as PerScreenMetadata
    }

    private fun createPreferenceHierarchyNode(
        metadata: PreferenceMetadata
    ): PreferenceHierarchyNode {
        val constructor =
            PreferenceHierarchyNode::class
                .java
                .getDeclaredConstructor(PreferenceMetadata::class.java)
        constructor.isAccessible = true
        return constructor.newInstance(metadata)
    }

    private val mockConfig =
        CatalystConfig(
            deviceStateItems =
                listOf(
                    DeviceStateItemConfig(
                        settingKey = "test_key_writable",
                        settingScreenKey = "test_screen",
                    ),
                    DeviceStateItemConfig(
                        settingKey = "test_key_not_writable",
                        settingScreenKey = "test_screen",
                    ),
                ),
            screenConfigs =
                listOf(PerScreenCatalystConfig(enabled = true, screenKey = "test_screen")),
        )

    private class TestPreferenceMetadata(
        override val bindingKey: String,
        private val isPersistent: Boolean,
        val writePermit: Int?,
    ) : PersistentPreference<Any> {
        override val key: String
            get() = bindingKey

        override val title: Int = android.R.string.ok
        override val purpose: Int = 0

        override fun isPersistent(context: Context): Boolean = isPersistent

        override val valueType: Class<Any> = Any::class.java
        override val sensitivityLevel: Int = SensitivityLevel.NO_SENSITIVITY

        override fun getWritePermit(context: Context, callingPid: Int, callingUid: Int): Int? =
            writePermit
    }

    private val testScreenMetadata =
        object : PreferenceScreenMetadata {
            override val key: String = "test_screen"
            override val title: Int = android.R.string.ok
            override val purpose: Int = 0

            override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?): Intent? =
                null

            override fun fragmentClass(): Class<out Fragment>? = null

            override fun getPreferenceHierarchy(
                context: Context,
                coroutineScope: CoroutineScope,
            ): PreferenceHierarchy = preferenceHierarchy(context) {}
        }
}
