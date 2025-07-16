/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.safetycenter

import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_BOOT_COMPLETED
import android.hardware.biometrics.Flags
import android.platform.test.annotations.RequiresFlagsDisabled
import android.platform.test.annotations.RequiresFlagsEnabled
import android.platform.test.flag.junit.CheckFlagsRule
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import android.provider.Settings
import android.safetycenter.SafetyEvent
import android.safetycenter.SafetyEvent.SAFETY_EVENT_TYPE_REFRESH_REQUESTED
import android.safetycenter.SafetySourceData
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class IdentityCheckSafetySourceTest {
    @get:Rule val mockitoRule: MockitoRule = MockitoJUnit.rule()

    @get:Rule val mCheckFlagsRule: CheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()

    @Mock lateinit var safetyCenterManagerWrapper: SafetyCenterManagerWrapper

    private val applicationContext: Context = ApplicationProvider.getApplicationContext()
    private val safetyEvent =
        SafetyEvent.Builder(SAFETY_EVENT_TYPE_REFRESH_REQUESTED)
            .setRefreshBroadcastId(IdentityCheckSafetySource.SAFETY_SOURCE_ID)
            .build()
    private val safetySourceDataArgumentCaptor = argumentCaptor<SafetySourceData>()

    private lateinit var identityCheckSafetySource: IdentityCheckSafetySource

    @Before
    fun setUp() {
        SafetyCenterManagerWrapper.sInstance = safetyCenterManagerWrapper
        identityCheckSafetySource = IdentityCheckSafetySource()
        identityCheckSafetySource.onReceive(applicationContext, Intent(ACTION_BOOT_COMPLETED))
    }

    @After
    fun tearDown() {
        SafetyCenterManagerWrapper.sInstance = null
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_IDENTITY_CHECK_ALL_SURFACES)
    fun refreshSafetySources_whenSafetyCenterIsDisabled_doesNotSetData() {
        whenever(safetyCenterManagerWrapper.isEnabled(applicationContext)).thenReturn(false)

        setIdentityCheckPromoCardShown(false)
        setIdentityCheckEnabled()

        IdentityCheckSafetySource.setSafetySourceData(applicationContext, safetyEvent)

        verify(safetyCenterManagerWrapper, never()).setSafetySourceData(any(), any(), any(), any())
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_IDENTITY_CHECK_ALL_SURFACES)
    fun refreshSafetySources_whenIdentityCheckDisabledInV1_setsNullData() {
        whenever(safetyCenterManagerWrapper.isEnabled(applicationContext)).thenReturn(true)

        setIdentityCheckPromoCardShown(false)
        setIdentityCheckEnabled(enabledV1Status = false, enabledCurrentStatus = true)

        IdentityCheckSafetySource.setSafetySourceData(applicationContext, safetyEvent)

        verify(safetyCenterManagerWrapper)
            .setSafetySourceData(
                eq(applicationContext),
                eq(IdentityCheckSafetySource.SAFETY_SOURCE_ID),
                eq(null),
                eq(safetyEvent),
            )
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_IDENTITY_CHECK_ALL_SURFACES)
    fun refreshSafetySources_whenIdentityCheckDisabledCurrently_setsNullData() {
        whenever(safetyCenterManagerWrapper.isEnabled(applicationContext)).thenReturn(true)

        setIdentityCheckPromoCardShown(false)
        setIdentityCheckEnabled(enabledV1Status = true, enabledCurrentStatus = false)

        IdentityCheckSafetySource.setSafetySourceData(applicationContext, safetyEvent)

        verify(safetyCenterManagerWrapper)
            .setSafetySourceData(
                eq(applicationContext),
                eq(IdentityCheckSafetySource.SAFETY_SOURCE_ID),
                eq(null),
                eq(safetyEvent),
            )
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_IDENTITY_CHECK_ALL_SURFACES)
    fun refreshSafetySources_whenPromoCardAlreadyShown_setsNullData() {
        whenever(safetyCenterManagerWrapper.isEnabled(applicationContext)).thenReturn(true)

        setIdentityCheckPromoCardShown(true)
        setIdentityCheckEnabled()

        IdentityCheckSafetySource.setSafetySourceData(applicationContext, safetyEvent)

        verify(safetyCenterManagerWrapper)
            .setSafetySourceData(
                eq(applicationContext),
                eq(IdentityCheckSafetySource.SAFETY_SOURCE_ID),
                eq(null),
                eq(safetyEvent),
            )
    }

    @Test
    @RequiresFlagsDisabled(Flags.FLAG_IDENTITY_CHECK_ALL_SURFACES)
    fun refreshSafetySources_whenFlagDisabled_setsNullData() {
        whenever(safetyCenterManagerWrapper.isEnabled(applicationContext)).thenReturn(true)

        setIdentityCheckPromoCardShown(false)
        setIdentityCheckEnabled()

        IdentityCheckSafetySource.setSafetySourceData(applicationContext, safetyEvent)

        verify(safetyCenterManagerWrapper)
            .setSafetySourceData(
                eq(applicationContext),
                eq(IdentityCheckSafetySource.SAFETY_SOURCE_ID),
                eq(null),
                eq(safetyEvent),
            )
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_IDENTITY_CHECK_ALL_SURFACES)
    fun refreshSafetySources_setsSafetySourceData() {
        whenever(safetyCenterManagerWrapper.isEnabled(applicationContext)).thenReturn(true)

        setIdentityCheckPromoCardShown(false)
        setIdentityCheckEnabled()

        IdentityCheckSafetySource.setSafetySourceData(applicationContext, safetyEvent)

        verify(safetyCenterManagerWrapper)
            .setSafetySourceData(
                eq(applicationContext),
                eq(IdentityCheckSafetySource.SAFETY_SOURCE_ID),
                safetySourceDataArgumentCaptor.capture(),
                eq(safetyEvent),
            )

        assertThat(safetySourceDataArgumentCaptor.firstValue).isNotNull()
    }

    private fun setIdentityCheckPromoCardShown(hasShown: Boolean) {
        Settings.Secure.putInt(
            applicationContext.contentResolver,
            Settings.Secure.IDENTITY_CHECK_PROMO_CARD_SHOWN,
            if (hasShown) 1 else 0,
        )
    }

    private fun setIdentityCheckEnabled(
        enabledV1Status: Boolean = true,
        enabledCurrentStatus: Boolean = true,
    ) {
        Settings.Secure.putInt(
            applicationContext.contentResolver,
            Settings.Secure.IDENTITY_CHECK_ENABLED_V1,
            if (enabledV1Status) 1 else 0,
        )

        Settings.Secure.putInt(
            applicationContext.contentResolver,
            Settings.Secure.MANDATORY_BIOMETRICS,
            if (enabledCurrentStatus) 1 else 0,
        )
    }
}
