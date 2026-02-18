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

package com.android.settings.preference

import android.app.Application
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.PreferenceScreenCollector
import com.android.settingslib.metadata.PreferenceGroup
import com.android.settingslib.metadata.PreferenceHierarchyNode
import com.android.settingslib.metadata.PreferenceScreenMetadataParameterizedFactory
import com.android.settingslib.metadata.PreferenceScreenRegistry
import com.android.settingslib.metadata.isUiOnlyPreference
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PreferenceKeysUniquenessComponentTest {

    @Before
    fun setUp() {

        PreferenceScreenRegistry.preferenceScreenMetadataFactories = PreferenceScreenCollector.get()
    }

    @Test
    fun verifyAllPreferenceKeysAreUnique() = runBlocking {
        val application = ApplicationProvider.getApplicationContext<Application>()
        Settings.Global.putInt(
            application.contentResolver,
            "com.android.settings.SKIP_CATALYST_FLAG_CHECKS",
            1,
        )

        val duplicateKeys = getDuplicatePreferenceKeys(application, this)

        val failureMessage =
            duplicateKeys.entries.joinToString(
                prefix = "The following preference keys appear in multiple screens:\n",
                separator = "\n",
            ) { (prefKey, screens) ->
                " - Preference Key: \"$prefKey\" is found in screens: $screens"
            }

        assertWithMessage(failureMessage).that(duplicateKeys).isEmpty()
    }

    private suspend fun getDuplicatePreferenceKeys(
        application: Application,
        coroutineScope: CoroutineScope,
    ): Map<String, MutableList<String>> {
        val seenKeys = mutableMapOf<String, MutableList<String>>()
        val factories = PreferenceScreenRegistry.preferenceScreenMetadataFactories

        assertWithMessage("Expected at least 100 factories, but found ${factories.size}")
            .that(factories.size)
            .isGreaterThan(100)

        factories.forEachAsync { screenKey, factory ->
            if (factory is PreferenceScreenMetadataParameterizedFactory) {
                factory.keyParameters(application).take(1).collect { params ->
                    val screenMetadata = factory.createWithKeyParameters(application, params)
                    val hierarchy =
                        screenMetadata.getPreferenceHierarchy(application, coroutineScope)
                    hierarchy.forEachRecursivelyAsync { node ->
                        if (shouldNodeBeChecked(node, application)) {
                            val prefKey = node.metadata.key
                            if (prefKey.isNotEmpty()) {
                                seenKeys.getOrPut(prefKey) { mutableListOf() }.add(screenKey)
                            }
                        }
                    }
                }
            } else {
                val screenMetadata = factory.create(application)
                val hierarchy = screenMetadata.getPreferenceHierarchy(application, coroutineScope)
                hierarchy.forEachRecursivelyAsync { node ->
                    if (shouldNodeBeChecked(node, application)) {
                        val prefKey = node.metadata.key
                        if (prefKey.isNotEmpty()) {
                            seenKeys.getOrPut(prefKey) { mutableListOf() }.add(screenMetadata.key)
                        }
                    }
                }
            }
        }
        return seenKeys.filter { it.value.size > 1 }
    }

    private fun shouldNodeBeChecked(
        node: PreferenceHierarchyNode,
        application: Application,
    ): Boolean =
        node.metadata !is PreferenceGroup &&
            !node.metadata.isUiOnlyPreference(application) &&
            node.metadata.key != "no_app"
}
