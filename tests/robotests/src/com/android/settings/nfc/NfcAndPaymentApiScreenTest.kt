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

package com.android.settings.nfc

import android.Manifest.permission.WRITE_SECURE_SETTINGS
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.flags.Flags.FLAG_CATALYST_MIGRATION_26Q2
import com.android.settings.testutils2.ApiTester
import com.android.settings.testutils2.HardwareUnsupportedException
import com.android.settings.testutils2.MissingPermissionException
import com.android.settingslib.metadata.preferencesapi.types.AnyBoolean
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows

@RunWith(AndroidJUnit4::class)
class NfcAndPaymentApiScreenTest {
    private val tester = ApiTester(NfcAndPaymentApiScreen())
    private val context: Context = ApplicationProvider.getApplicationContext()

    @get:Rule val setFlagsRule = SetFlagsRule()

    private fun grantPermission(permission: String) {
        val application = ApplicationProvider.getApplicationContext<Application>()
        Shadows.shadowOf(application).grantPermissions(permission)
    }

    private fun denyPermission(permission: String) {
        val application = ApplicationProvider.getApplicationContext<Application>()
        Shadows.shadowOf(application).denyPermissions(permission)
    }

    @Before
    fun setUp() {
        denyPermission(WRITE_SECURE_SETTINGS)

        val shadowPackageManager = Shadows.shadowOf(context.packageManager)
        shadowPackageManager.setSystemFeature(PackageManager.FEATURE_NFC_ANY, true)
        shadowPackageManager.setSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION, true)

        val nfcAdapter = NfcAdapter.getDefaultAdapter(context)
        if (nfcAdapter != null) {
            val shadowNfc = Shadows.shadowOf(nfcAdapter)
            shadowNfc.setEnabled(false)
            shadowNfc.setSecureNfcSupported(true)
            nfcAdapter.enableSecureNfc(false)
        }
    }

    @Test
    @EnableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun getScreen_isNotNull() {
        assertThat(tester.getScreen()).isNotNull()
    }

    @Test
    @DisableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun getScreen_flagDisabled_isNull() {
        assertThat(tester.getScreen()).isNull()
    }

    // --- Tests for USE_NFC ---

    @Test
    @EnableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun useNfcPreference_hardwareUnsupported_throwsException() {
        val shadowPackageManager = Shadows.shadowOf(context.packageManager)
        shadowPackageManager.setSystemFeature(PackageManager.FEATURE_NFC_ANY, false)

        val e =
            assertFailsWith<HardwareUnsupportedException> {
                tester.get<AnyBoolean>(NfcAndPaymentApiScreen.PREFERENCE_KEY_USE_NFC)
            }
        assertThat(e.reason).contains(context.getString(R.string.use_nfc_unavailable))
    }

    @Test
    @EnableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun useNfcPreference_get_returnsAdapterState() {
        val nfcAdapter = NfcAdapter.getDefaultAdapter(context)
        val shadowNfc = Shadows.shadowOf(nfcAdapter)

        shadowNfc.setEnabled(true)
        assertThat(tester.get<AnyBoolean>(NfcAndPaymentApiScreen.PREFERENCE_KEY_USE_NFC))
            .isEqualTo(true)

        shadowNfc.setEnabled(false)
        assertThat(tester.get<AnyBoolean>(NfcAndPaymentApiScreen.PREFERENCE_KEY_USE_NFC))
            .isEqualTo(false)
    }

    @Test
    @EnableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun useNfcPreference_set_noPermissions_throwsException() {
        assertFailsWith<MissingPermissionException> {
            tester.set(NfcAndPaymentApiScreen.PREFERENCE_KEY_USE_NFC, true)
        }
    }

    @Test
    @EnableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun useNfcPreference_set_withPermissions_updatesAdapter() {
        grantPermission(WRITE_SECURE_SETTINGS)
        val nfcAdapter = NfcAdapter.getDefaultAdapter(context)

        tester.set(NfcAndPaymentApiScreen.PREFERENCE_KEY_USE_NFC, true)
        assertThat(nfcAdapter.isEnabled).isEqualTo(true)
        assertThat(tester.get<AnyBoolean>(NfcAndPaymentApiScreen.PREFERENCE_KEY_USE_NFC))
            .isEqualTo(true)

        tester.set(NfcAndPaymentApiScreen.PREFERENCE_KEY_USE_NFC, false)
        assertThat(nfcAdapter.isEnabled).isEqualTo(false)
        assertThat(tester.get<AnyBoolean>(NfcAndPaymentApiScreen.PREFERENCE_KEY_USE_NFC))
            .isEqualTo(false)
    }

    // --- Tests for REQUIRE_DEVICE_UNLOCK_FOR_NFC ---

    @Test
    @EnableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun secureNfcPreference_notSupported_throwsHardwareUnsupportedException() {
        val nfcAdapter = NfcAdapter.getDefaultAdapter(context)
        val shadowNfc = Shadows.shadowOf(nfcAdapter)
        shadowNfc.setSecureNfcSupported(false)

        val e =
            assertFailsWith<HardwareUnsupportedException> {
                tester.get<AnyBoolean>(NfcAndPaymentApiScreen.PREFERENCE_KEY_SECURE_NFC)
            }
        assertThat(e.reason)
            .contains(context.getString(R.string.require_device_unlock_for_nfc_unavailable))
    }

    @Test
    @EnableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun secureNfcPreference_get_returnsAdapterState() {
        val nfcAdapter = NfcAdapter.getDefaultAdapter(context)

        nfcAdapter.enableSecureNfc(true)
        assertThat(tester.get<AnyBoolean>(NfcAndPaymentApiScreen.PREFERENCE_KEY_SECURE_NFC))
            .isEqualTo(true)

        nfcAdapter.enableSecureNfc(false)
        assertThat(tester.get<AnyBoolean>(NfcAndPaymentApiScreen.PREFERENCE_KEY_SECURE_NFC))
            .isEqualTo(false)
    }

    @Test
    @EnableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun secureNfcPreference_set_noPermissions_throwsException() {
        assertFailsWith<MissingPermissionException> {
            tester.set(NfcAndPaymentApiScreen.PREFERENCE_KEY_SECURE_NFC, true)
        }
    }

    @Test
    @EnableFlags(FLAG_CATALYST_MIGRATION_26Q2)
    fun secureNfcPreference_set_withPermissions_updatesAdapter() {
        grantPermission(WRITE_SECURE_SETTINGS)
        val nfcAdapter = NfcAdapter.getDefaultAdapter(context)

        tester.set(NfcAndPaymentApiScreen.PREFERENCE_KEY_SECURE_NFC, true)
        assertThat(nfcAdapter.isSecureNfcEnabled).isEqualTo(true)
        assertThat(tester.get<AnyBoolean>(NfcAndPaymentApiScreen.PREFERENCE_KEY_SECURE_NFC))
            .isEqualTo(true)

        tester.set(NfcAndPaymentApiScreen.PREFERENCE_KEY_SECURE_NFC, false)
        assertThat(nfcAdapter.isSecureNfcEnabled).isEqualTo(false)
        assertThat(tester.get<AnyBoolean>(NfcAndPaymentApiScreen.PREFERENCE_KEY_SECURE_NFC))
            .isEqualTo(false)
    }
}
