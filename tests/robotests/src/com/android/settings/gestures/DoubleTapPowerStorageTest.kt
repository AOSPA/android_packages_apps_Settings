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

package com.android.settings.gestures

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.gestures.DoubleTapPowerStorage.Companion.CAMERA_LAUNCH_VALUE
import com.android.settings.gestures.DoubleTapPowerStorage.Companion.KEY
import com.android.settings.gestures.DoubleTapPowerStorage.Companion.WALLET_LAUNCH_VALUE
import com.android.settingslib.datastore.SettingsSecureStore
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DoubleTapPowerStorageTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val storage: DoubleTapPowerStorage = DoubleTapPowerStorage(context)

    @Test
    fun contains_withCameraKey_returnsTrue() {
        assertThat(storage.contains(DoubleTapPowerForCameraPreference.KEY)).isTrue()
    }

    @Test
    fun contains_withWalletKey_returnsTrue() {
        assertThat(storage.contains(DoubleTapPowerForWalletPreference.KEY)).isTrue()
    }

    @Test
    fun contains_withOtherKey_returnsFalse() {
        assertThat(storage.contains("other_key")).isFalse()
    }

    @Test
    fun getValue_cameraSet_returnsCorrectValues() {
        SettingsSecureStore.get(context).setInt(KEY, CAMERA_LAUNCH_VALUE)
        val cameraValue = storage.getBoolean(DoubleTapPowerForCameraPreference.KEY)
        val walletValue = storage.getBoolean(DoubleTapPowerForWalletPreference.KEY)

        assertThat(cameraValue).isTrue()
        assertThat(walletValue).isFalse()
    }

    @Test
    fun getValue_walletSet_returnsCorrectValues() {
        SettingsSecureStore.get(context).setInt(KEY, WALLET_LAUNCH_VALUE)
        val cameraValue = storage.getBoolean(DoubleTapPowerForCameraPreference.KEY)
        val walletValue = storage.getBoolean(DoubleTapPowerForWalletPreference.KEY)

        assertThat(cameraValue).isFalse()
        assertThat(walletValue).isTrue()
    }

    @Test
    fun setValue_setCamera_updatesSettings() {
        storage.setBoolean(DoubleTapPowerForCameraPreference.KEY, true)
        val storedValue = SettingsSecureStore.get(context).getInt(KEY)

        assertThat(storedValue).isEqualTo(CAMERA_LAUNCH_VALUE)
    }

    @Test
    fun setValue_setWallet_updatesSettings() {
        storage.setBoolean(DoubleTapPowerForWalletPreference.KEY, true)
        val storedValue = SettingsSecureStore.get(context).getInt(KEY)

        assertThat(storedValue).isEqualTo(WALLET_LAUNCH_VALUE)
    }

    @Test
    fun getValue_resetKey_shouldReturnCamera() {
        SettingsSecureStore.get(context).setInt(KEY, null)
        val cameraValue = storage.getBoolean(DoubleTapPowerForCameraPreference.KEY)

        assertThat(cameraValue).isTrue()
    }
}
