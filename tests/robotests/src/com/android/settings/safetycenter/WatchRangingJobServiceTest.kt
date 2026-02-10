/*
 * Copyright (C) 2026 The Android Open Source Project
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

import android.app.job.JobParameters
import android.content.Context
import android.hardware.biometrics.Flags
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.provider.Settings
import android.proximity.IProximityResultCallback
import android.proximity.ProximityResultCode
import android.security.authenticationpolicy.AuthenticationPolicyManager
import android.security.authenticationpolicy.IAuthenticationPolicyService
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settingslib.utils.ThreadUtils
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.robolectric.Robolectric
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowApplication

@RunWith(AndroidJUnit4::class)
class WatchRangingJobServiceTest {
    @get:Rule val mockitoRule: MockitoRule = MockitoJUnit.rule()
    @get:Rule val setFlagsRule = SetFlagsRule()

    @Mock lateinit var params: JobParameters
    @Mock lateinit var authenticationPolicyService: IAuthenticationPolicyService

    private val proximityResultCallback =
        ArgumentCaptor.forClass(IProximityResultCallback::class.java)
    private lateinit var applicationContext: Context
    private lateinit var authenticationPolicyManager: AuthenticationPolicyManager
    private lateinit var watchRangingJobService: WatchRangingJobService

    @Before
    fun setUp() {
        applicationContext = ApplicationProvider.getApplicationContext()
        authenticationPolicyManager =
            AuthenticationPolicyManager(applicationContext, authenticationPolicyService)
        Shadow.extract<ShadowApplication>(applicationContext)
            .setSystemService(Context.AUTHENTICATION_POLICY_SERVICE, authenticationPolicyManager)
        watchRangingJobService =
            Robolectric.buildService(WatchRangingJobService::class.java).create().get()
    }

    @Test
    @EnableFlags(Flags.FLAG_IDENTITY_CHECK_WATCH)
    fun startJobService_watchRangingIsAvailable_setsGlobalValue() {
        val executor = ThreadUtils.getBackgroundExecutor()
        setWatchRangingSupportedByPrimaryDevice(false)

        watchRangingJobService.finishJobWhenWatchRangingStatusKnown(executor, params)
        executor.awaitTermination(5000, TimeUnit.MILLISECONDS)

        verify(authenticationPolicyService)
            .isWatchRangingAvailable(proximityResultCallback.capture())

        proximityResultCallback.value.onError(ProximityResultCode.NO_ASSOCIATED_DEVICE)
        executor.awaitTermination(5000, TimeUnit.MILLISECONDS)

        assertThat(getWatchRangingSupportedByPrimaryDevice()).isEqualTo(1)
    }

    @Test
    @EnableFlags(Flags.FLAG_IDENTITY_CHECK_WATCH)
    fun startJobService_npWatchRangingIsAvailable_setsGlobalValue() {
        val executor = Executors.newSingleThreadExecutor()
        setWatchRangingSupportedByPrimaryDevice(true)

        watchRangingJobService.finishJobWhenWatchRangingStatusKnown(executor, params)
        executor.awaitTermination(5000, TimeUnit.MILLISECONDS)

        verify(authenticationPolicyService)
            .isWatchRangingAvailable(proximityResultCallback.capture())

        proximityResultCallback.value.onError(
            ProximityResultCode.PRIMARY_DEVICE_RANGING_NOT_SUPPORTED
        )
        executor.awaitTermination(5000, TimeUnit.MILLISECONDS)

        assertThat(getWatchRangingSupportedByPrimaryDevice()).isEqualTo(0)
    }

    @Test
    @DisableFlags(Flags.FLAG_IDENTITY_CHECK_WATCH)
    fun startJobService_watchRangingIsAvailableAndFlagDisabled_doesNotSetGlobalValue() {
        val executor = Executors.newSingleThreadExecutor()
        setWatchRangingSupportedByPrimaryDevice(false)

        watchRangingJobService.finishJobWhenWatchRangingStatusKnown(executor, params)
        executor.awaitTermination(5000, TimeUnit.MILLISECONDS)

        verifyNoInteractions(authenticationPolicyService)
        assertThat(getWatchRangingSupportedByPrimaryDevice()).isEqualTo(0)
    }

    fun setWatchRangingSupportedByPrimaryDevice(status: Boolean) =
        Settings.Global.putInt(
            applicationContext.contentResolver,
            Settings.Global.WATCH_RANGING_SUPPORTED_BY_PRIMARY_DEVICE,
            if (status) 1 else 0,
        )

    fun getWatchRangingSupportedByPrimaryDevice(): Int =
        Settings.Global.getInt(
            applicationContext.contentResolver,
            Settings.Global.WATCH_RANGING_SUPPORTED_BY_PRIMARY_DEVICE,
            0,
        )
}
