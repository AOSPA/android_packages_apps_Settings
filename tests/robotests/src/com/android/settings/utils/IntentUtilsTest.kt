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

package com.android.settings.utils

import android.content.Intent
import android.os.Bundle
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class IntentUtilsTest {
    @Test
    fun flattenBundles_replacesBundlesWithRawStrings() {
        val bundle = Bundle().apply {
            putString("pkg", "com.android.settings")
            putInt("user_id", 2737)
            putBoolean("is_new", false)
        }
        val intent = Intent().apply {
            putExtra("screen_args", bundle)
            putExtra("other_extra", "non_bundle_value")
        }

        intent.flattenBundles()

        assertThat(intent.hasExtra("screen_args")).isFalse()
        assertThat(intent.hasExtra("screen_args_raw")).isTrue()

        val rawValue = intent.getStringExtra("screen_args_raw")!!
        assertThat(rawValue).contains("pkg=com.android.settings")
        assertThat(rawValue).contains("user_id=2737")
        assertThat(rawValue).contains("is_new=false")

        assertThat(intent.getStringExtra("other_extra")).isEqualTo("non_bundle_value")
    }

    @Test
    fun unflattenBundles_restoresBundlesFromRawStrings() {
        val intent = Intent().apply {
            putExtra("screen_args_raw", "[pkg=com.android.settings,user_id=2737,is_new=false]")
            putExtra("existing_extra", "preserved")
        }

        intent.unflattenBundles()

        assertThat(intent.hasExtra("screen_args_raw")).isFalse()
        assertThat(intent.hasExtra("screen_args")).isTrue()

        val restoredBundle = intent.getBundleExtra("screen_args")!!
        assertThat(restoredBundle.getString("pkg")).isEqualTo("com.android.settings")
        assertThat(restoredBundle.getString("user_id")).isEqualTo("2737")
        assertThat(restoredBundle.getString("is_new")).isEqualTo("false")

        assertThat(intent.getStringExtra("existing_extra")).isEqualTo("preserved")
    }

    @Test
    fun flattenAndUnflatten_isConsistent() {
        val originalBundle = Bundle().apply {
            putString("key_a", "value_a")
            putString("key_b", "value_b")
        }
        val intent = Intent().apply {
            putExtra("data", originalBundle)
        }

        intent.flattenBundles()
        intent.unflattenBundles()

        val finalBundle = intent.getBundleExtra("data")!!
        assertThat(finalBundle.getString("key_a")).isEqualTo("value_a")
        assertThat(finalBundle.getString("key_b")).isEqualTo("value_b")
        assertThat(intent.hasExtra("data_raw")).isFalse()
    }

    @Test
    fun flattenBundles_handlesEmptyIntent() {
        val intent = Intent()
        intent.flattenBundles()
        assertThat(intent.extras).isNull()
    }

    @Test
    fun unflattenBundles_handlesNoRawExtras() {
        val intent = Intent().apply {
            putExtra("normal_key", "normal_value")
        }
        intent.unflattenBundles()
        assertThat(intent.getStringExtra("normal_key")).isEqualTo("normal_value")
        assertThat(intent.extras?.keySet()).hasSize(1)
    }
}