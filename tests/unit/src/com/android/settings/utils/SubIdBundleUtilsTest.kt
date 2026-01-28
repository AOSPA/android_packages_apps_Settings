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

package com.android.settings.utils

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
class SubIdBundleUtilsTest {

    @get:Rule val platformFlags = SetFlagsRule()

    private val testKey = "test_sub_id"
    private val testSubId = 2737
    private val defaultSubId = -1

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
    fun putSubId_whenFlagIsTrue_putsString() {
        val bundle = Bundle()
        bundle.putSubId(testKey, testSubId)

        assertThat(bundle.getString(testKey)).isEqualTo(testSubId.toString())
        assertThat(bundle.containsKey(testKey)).isTrue()
        assertThat(bundle.getInt(testKey, defaultSubId)).isEqualTo(defaultSubId)
    }

    @Test
    @DisableFlags(Flags.FLAG_CATALYST_USE_STRING_BUNDLE)
    fun putSubId_whenFlagsAreFalse_putsInt() {
        setCatalystUseKeyParameters(false)

        val bundle = Bundle()
        bundle.putSubId(testKey, testSubId)

        assertThat(bundle.getInt(testKey)).isEqualTo(testSubId)
        assertThat(bundle.containsKey(testKey)).isTrue()
        assertThat(bundle.getString(testKey)).isNull()
    }

    @Test
    @DisableFlags(Flags.FLAG_CATALYST_USE_STRING_BUNDLE)
    fun putSubId_whenFlagIsFalse_butCatalystKeyParametersFlagIsTrue_putsString() {
        setCatalystUseKeyParameters(true)

        val bundle = Bundle()
        bundle.putSubId(testKey, testSubId)

        assertThat(bundle.getString(testKey)).isEqualTo(testSubId.toString())
        assertThat(bundle.containsKey(testKey)).isTrue()
        assertThat(bundle.getInt(testKey, defaultSubId)).isEqualTo(defaultSubId)
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_USE_STRING_BUNDLE)
    fun getSubId_whenFlagIsTrue_readsStringCorrectly() {
        val bundle = Bundle().apply { putString(testKey, testSubId.toString()) }
        val result = bundle.getSubId(testKey, defaultSubId)

        assertThat(result).isEqualTo(testSubId)
    }

    @Test
    @DisableFlags(Flags.FLAG_CATALYST_USE_STRING_BUNDLE)
    fun getSubId_whenFlagsAreFalse_readsIntCorrectly() {
        setCatalystUseKeyParameters(false)

        val bundle = Bundle().apply { putInt(testKey, testSubId) }
        val result = bundle.getSubId(testKey, defaultSubId)

        assertThat(result).isEqualTo(testSubId)
    }

    @Test
    @DisableFlags(Flags.FLAG_CATALYST_USE_STRING_BUNDLE)
    fun getSubId_whenFlagIsFalse_butCatalystKeyParametersFlagIsTrue_readsStringCorrectly() {
        setCatalystUseKeyParameters(true)

        val bundle = Bundle().apply { putString(testKey, testSubId.toString()) }
        val result = bundle.getSubId(testKey, defaultSubId)

        assertThat(result).isEqualTo(testSubId)
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_USE_STRING_BUNDLE)
    fun getSubId_whenKeyIsMissing_returnsDefaultValue() {
        val bundle = Bundle()
        val result = bundle.getSubId(testKey, defaultSubId)

        assertThat(result).isEqualTo(defaultSubId)
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_USE_STRING_BUNDLE)
    fun getSubId_whenFlagIsTrue_andStringIsInvalid_returnsDefaultValue() {
        val bundle = Bundle().apply { putString(testKey, "not-an-integer") }
        val result = bundle.getSubId(testKey, defaultSubId)

        assertThat(result).isEqualTo(defaultSubId)
    }

    private fun setCatalystUseKeyParameters(value: Boolean) {
        CatalystFlagProviderFactory.setProvider(object : CatalystFlagProvider {
            override fun catalystUseKeyParameters() = value
        })
    }
}
