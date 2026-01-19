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

package com.android.settings.accessibility.extensions

import android.content.ComponentName
import android.os.Bundle
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.flags.Flags
import com.android.settingslib.metadata.CatalystFlagProvider
import com.android.settingslib.metadata.CatalystFlagProviderFactory
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ComponentNameBundleUtilsTest {

    @get:Rule val platformFlags = SetFlagsRule()

    private val testKey = "test_component_name"
    private val testPackage = "com.example.package"
    private val testClass = "com.example.package.TestClass"
    private val testComponentName = ComponentName(testPackage, testClass)

    private lateinit var originalProvider: CatalystFlagProvider

    @Before
    fun setUp() {
        originalProvider = CatalystFlagProviderFactory.getInstance()
    }

    @After
    fun tearDown() {
        CatalystFlagProviderFactory.setProvider(originalProvider)
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_USE_STRING_BUNDLE)
    fun putComponentName_whenFlagIsEnabled_putsString() {
        val bundle = Bundle()
        bundle.putComponentName(testKey, testComponentName)

        assertThat(bundle.getString(testKey)).isEqualTo(testComponentName.flattenToString())
        assertThat(bundle.containsKey(testKey)).isTrue()
        assertThat(bundle.getParcelable(testKey, ComponentName::class.java)).isNull()
    }

    @Test
    @DisableFlags(Flags.FLAG_CATALYST_USE_STRING_BUNDLE)
    fun putComponentName_whenFlagsAreDisabled_putsParcelable() {
        setCatalystUseKeyParameters(false)

        val bundle = Bundle()
        bundle.putComponentName(testKey, testComponentName)

        assertThat(bundle.getParcelable(testKey, ComponentName::class.java))
            .isEqualTo(testComponentName)
        assertThat(bundle.containsKey(testKey)).isTrue()
        assertThat(bundle.getString(testKey)).isNull()
    }

    @Test
    @DisableFlags(Flags.FLAG_CATALYST_USE_STRING_BUNDLE)
    fun putComponentName_whenFlagIsDisabled_butCatalystKeyParametersFlagIsEnabled_putsString() {
        setCatalystUseKeyParameters(true)

        val bundle = Bundle()
        bundle.putComponentName(testKey, testComponentName)

        assertThat(bundle.getString(testKey)).isEqualTo(testComponentName.flattenToString())
        assertThat(bundle.containsKey(testKey)).isTrue()
        assertThat(bundle.getParcelable(testKey, ComponentName::class.java)).isNull()
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_USE_STRING_BUNDLE)
    fun getComponentName_whenFlagIsEnabled_readsStringCorrectly() {
        val bundle = Bundle().apply { putString(testKey, testComponentName.flattenToString()) }
        val result = bundle.getComponentName(testKey)

        assertThat(result).isEqualTo(testComponentName)
    }

    @Test
    @DisableFlags(Flags.FLAG_CATALYST_USE_STRING_BUNDLE)
    fun getComponentName_whenFlagsAreDisabled_readsParcelableCorrectly() {
        setCatalystUseKeyParameters(false)

        val bundle = Bundle().apply { putParcelable(testKey, testComponentName) }
        val result = bundle.getComponentName(testKey)

        assertThat(result).isEqualTo(testComponentName)
    }

    @Test
    @DisableFlags(Flags.FLAG_CATALYST_USE_STRING_BUNDLE)
    fun getComponentName_whenFlagIsDisabled_butCatalystKeyParametersFlagIsEnabled_readsStringCorrectly() {
        setCatalystUseKeyParameters(true)

        val bundle = Bundle().apply { putString(testKey, testComponentName.flattenToString()) }
        val result = bundle.getComponentName(testKey)

        assertThat(result).isEqualTo(testComponentName)
    }

    @Test
    fun getComponentName_whenKeyIsMissing_returnsNull() {
        val bundle = Bundle()
        val result = bundle.getComponentName(testKey)

        assertThat(result).isNull()
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_USE_STRING_BUNDLE)
    fun getComponentName_whenFlagIsEnabled_andStringIsMalformed_returnsNull() {
        val bundle = Bundle().apply { putString(testKey, "not-a-component-name") }
        val result = bundle.getComponentName(testKey)

        assertThat(result).isNull()
    }

    private fun setCatalystUseKeyParameters(value: Boolean) {
        CatalystFlagProviderFactory.setProvider(object : CatalystFlagProvider {
            override fun catalystUseKeyParameters() = value
        })
    }
}
