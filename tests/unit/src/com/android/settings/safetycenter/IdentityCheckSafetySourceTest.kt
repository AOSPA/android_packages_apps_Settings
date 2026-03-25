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

import android.app.job.JobScheduler
import android.content.Context
import android.content.Intent
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.Flags
import android.os.UserManager
import android.platform.test.annotations.RequiresFlagsDisabled
import android.platform.test.annotations.RequiresFlagsEnabled
import android.platform.test.flag.junit.CheckFlagsRule
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import android.provider.Settings
import android.safetycenter.SafetyEvent
import android.safetycenter.SafetyEvent.SAFETY_EVENT_TYPE_REFRESH_REQUESTED
import android.safetycenter.SafetyEvent.SAFETY_EVENT_TYPE_SOURCE_STATE_CHANGED
import android.safetycenter.SafetySourceData
import android.safetycenter.SafetySourceIssue
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.flags.Flags.FLAG_SCHEDULE_WATCH_RANGING_AVAILABILITY_WITH_JOB_SCHEDULER
import com.android.settings.safetycenter.IdentityCheckSafetySource.Companion.ACTION_ISSUE_CARD_SHOW_DETAILS
import com.android.settings.safetycenter.IdentityCheckSafetySource.Companion.ACTION_ISSUE_CARD_WATCH_SHOW_DETAILS
import com.android.settings.safetycenter.IdentityCheckSafetySource.Companion.ACTION_ISSUE_NOTIFICATION_CLICKED
import com.android.settings.safetycenter.IdentityCheckSafetySource.Companion.ACTION_WATCH_ISSUE_NOTIFICATION_CLICKED
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.TruthJUnit.assume
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
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class IdentityCheckSafetySourceTest {
    @get:Rule val mockitoRule: MockitoRule = MockitoJUnit.rule()

    @get:Rule val mCheckFlagsRule: CheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()

    @Mock lateinit var safetyCenterManagerWrapper: SafetyCenterManagerWrapper
    @Mock lateinit var biometricManager: BiometricManager
    @Mock lateinit var userManager: UserManager

    private val applicationContext: Context = ApplicationProvider.getApplicationContext()
    private val refreshSafetyEvent =
        SafetyEvent.Builder(SAFETY_EVENT_TYPE_REFRESH_REQUESTED)
            .setRefreshBroadcastId(IdentityCheckSafetySource.SAFETY_SOURCE_ID)
            .build()
    private val sourceChangeSafetyEvent =
        SafetyEvent.Builder(SAFETY_EVENT_TYPE_SOURCE_STATE_CHANGED).build()
    private val safetySourceDataArgumentCaptor = argumentCaptor<SafetySourceData>()

    private lateinit var identityCheckSafetySource: IdentityCheckSafetySource
    private lateinit var jobScheduler: JobScheduler

    @Before
    fun setUp() {
        SafetyCenterManagerWrapper.sInstance = safetyCenterManagerWrapper
        identityCheckSafetySource = IdentityCheckSafetySource()
        setIdentityCheckToggleStatus(true)
        jobScheduler = applicationContext.getSystemService(JobScheduler::class.java)
        whenever(biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG))
            .thenReturn(BiometricManager.BIOMETRIC_SUCCESS)
        whenever(userManager.isProfile(applicationContext.userId)).thenReturn(false)
    }

    @After
    fun tearDown() {
        SafetyCenterManagerWrapper.sInstance = null
    }

    @Test
    @RequiresFlagsEnabled(FLAG_SCHEDULE_WATCH_RANGING_AVAILABILITY_WITH_JOB_SCHEDULER)
    fun onReceive_whenBootComplete_schedulesWatchRangingJobService() {
        identityCheckSafetySource.onReceive(
            applicationContext,
            Intent(Intent.ACTION_BOOT_COMPLETED),
        )

        assertThat(jobScheduler.getPendingJob(IdentityCheckSafetySource.WATCH_RANGING_JOB_ID))
            .isNotNull()
    }

    @Test
    fun refreshSafetySources_whenSafetyCenterIsDisabled_doesNotSetData() {
        whenever(safetyCenterManagerWrapper.isEnabled(applicationContext)).thenReturn(false)

        setIdentityCheckPromoCardShown(false)

        IdentityCheckSafetySource.setSafetySourceData(
            applicationContext,
            refreshSafetyEvent,
            biometricManager,
            userManager,
        )

        verify(safetyCenterManagerWrapper, never()).setSafetySourceData(any(), any(), any(), any())
    }

    @Test
    fun refreshSafetySources_whenUserIsProfile_doesNotSetData() {
        val profileUserId = 10

        whenever(safetyCenterManagerWrapper.isEnabled(applicationContext)).thenReturn(true)
        whenever(userManager.isProfile(profileUserId)).thenReturn(true)

        setIdentityCheckPromoCardShown(false)

        IdentityCheckSafetySource.setSafetySourceData(
            applicationContext,
            refreshSafetyEvent,
            biometricManager,
            userManager,
            userId = profileUserId,
        )

        verify(safetyCenterManagerWrapper, never()).setSafetySourceData(any(), any(), any(), any())
    }

    @Test
    @RequiresFlagsDisabled(Flags.FLAG_IDENTITY_CHECK_WATCH)
    fun refreshSafetySources_whenPromoCardAlreadyShown_setsNullData() {
        whenever(safetyCenterManagerWrapper.isEnabled(applicationContext)).thenReturn(true)

        setIdentityCheckPromoCardShown(true)

        IdentityCheckSafetySource.setSafetySourceData(
            applicationContext,
            refreshSafetyEvent,
            biometricManager,
            userManager,
        )

        verify(safetyCenterManagerWrapper)
            .setSafetySourceData(
                eq(applicationContext),
                eq(IdentityCheckSafetySource.SAFETY_SOURCE_ID),
                eq(null),
                eq(refreshSafetyEvent),
            )
    }

    @Test
    fun refreshSafetySources_whenInvalidDevice_setsNullData() {
        whenever(safetyCenterManagerWrapper.isEnabled(applicationContext)).thenReturn(true)
        whenever(biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG))
            .thenReturn(BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE)
        setIdentityCheckPromoCardShown(false)

        IdentityCheckSafetySource.setSafetySourceData(
            applicationContext,
            refreshSafetyEvent,
            biometricManager,
            userManager,
        )

        verify(safetyCenterManagerWrapper)
            .setSafetySourceData(
                eq(applicationContext),
                eq(IdentityCheckSafetySource.SAFETY_SOURCE_ID),
                eq(null),
                eq(refreshSafetyEvent),
            )
    }

    @Test
    fun refreshSafetySources_whenDeviceIsATablet_setsNullData() {
        whenever(safetyCenterManagerWrapper.isEnabled(applicationContext)).thenReturn(true)
        setIdentityCheckPromoCardShown(false)

        IdentityCheckSafetySource.setSafetySourceData(
            applicationContext,
            refreshSafetyEvent,
            biometricManager,
            userManager,
            isTablet = true,
        )

        verify(safetyCenterManagerWrapper)
            .setSafetySourceData(
                eq(applicationContext),
                eq(IdentityCheckSafetySource.SAFETY_SOURCE_ID),
                eq(null),
                eq(refreshSafetyEvent),
            )
    }

    @Test
    fun refreshSafetySources_whenDeviceIsALowRamTablet_setsNullData() {
        whenever(safetyCenterManagerWrapper.isEnabled(applicationContext)).thenReturn(true)
        setIdentityCheckPromoCardShown(false)

        IdentityCheckSafetySource.setSafetySourceData(
            applicationContext,
            refreshSafetyEvent,
            biometricManager,
            userManager,
            isLowRamDevice = true,
        )

        verify(safetyCenterManagerWrapper)
            .setSafetySourceData(
                eq(applicationContext),
                eq(IdentityCheckSafetySource.SAFETY_SOURCE_ID),
                eq(null),
                eq(refreshSafetyEvent),
            )
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_IDENTITY_CHECK_WATCH)
    fun refreshSafetySources_whenWatchSupportedValueNotSet_setsNullData() {
        whenever(safetyCenterManagerWrapper.isEnabled(applicationContext)).thenReturn(true)

        setIdentityCheckPromoCardShown(false)
        resetWatchRangingSupportedValue()

        IdentityCheckSafetySource.setSafetySourceData(
            applicationContext,
            refreshSafetyEvent,
            biometricManager,
            userManager,
        )

        verify(safetyCenterManagerWrapper)
            .setSafetySourceData(
                eq(applicationContext),
                eq(IdentityCheckSafetySource.SAFETY_SOURCE_ID),
                eq(null),
                eq(refreshSafetyEvent),
            )
    }

    @Test
    fun refreshSafetySources_notificationNotClicked_setsSafetySourceDataWithNotification() {
        whenever(safetyCenterManagerWrapper.isEnabled(applicationContext)).thenReturn(true)

        setIdentityCheckNotificationBeenClicked(false)
        setIdentityCheckPromoCardShown(false)
        setWatchRangingSupportedValue(false)

        IdentityCheckSafetySource.setSafetySourceData(
            applicationContext,
            refreshSafetyEvent,
            biometricManager,
            userManager,
        )

        verify(safetyCenterManagerWrapper)
            .setSafetySourceData(
                eq(applicationContext),
                eq(IdentityCheckSafetySource.SAFETY_SOURCE_ID),
                safetySourceDataArgumentCaptor.capture(),
                eq(refreshSafetyEvent),
            )

        val safetySourceIssue: SafetySourceIssue =
            safetySourceDataArgumentCaptor.firstValue.issues[0]!!
        val actionPendingIntent = safetySourceIssue.actions[0].pendingIntent

        assertThat(safetySourceIssue.customNotification).isNotNull()
        assertThat(actionPendingIntent.intent.action).isEqualTo(ACTION_ISSUE_CARD_SHOW_DETAILS)
    }

    @Test
    fun refreshSafetySources_identityCheckNotEnabled_setsSafetySourceDataWithoutNotification() {
        whenever(safetyCenterManagerWrapper.isEnabled(applicationContext)).thenReturn(true)

        setIdentityCheckNotificationBeenClicked(false)
        setIdentityCheckPromoCardShown(false)
        setWatchRangingSupportedValue(false)
        setIdentityCheckToggleStatus(false)

        IdentityCheckSafetySource.setSafetySourceData(
            applicationContext,
            refreshSafetyEvent,
            biometricManager,
            userManager,
        )

        verify(safetyCenterManagerWrapper)
            .setSafetySourceData(
                eq(applicationContext),
                eq(IdentityCheckSafetySource.SAFETY_SOURCE_ID),
                safetySourceDataArgumentCaptor.capture(),
                eq(refreshSafetyEvent),
            )

        val safetySourceIssue: SafetySourceIssue =
            safetySourceDataArgumentCaptor.firstValue.issues[0]!!
        val actionPendingIntent = safetySourceIssue.actions[0].pendingIntent

        assertThat(safetySourceIssue.customNotification).isNull()
        assertThat(actionPendingIntent.intent.action).isEqualTo(ACTION_ISSUE_CARD_SHOW_DETAILS)
    }

    @Test
    fun refreshSafetySources_notificationClicked_setsSafetySourceDataWithoutNotification() {
        whenever(safetyCenterManagerWrapper.isEnabled(applicationContext)).thenReturn(true)

        setIdentityCheckNotificationBeenClicked(true)
        setIdentityCheckPromoCardShown(false)
        setWatchRangingSupportedValue(false)

        IdentityCheckSafetySource.setSafetySourceData(
            applicationContext,
            refreshSafetyEvent,
            biometricManager,
            userManager,
        )

        verify(safetyCenterManagerWrapper)
            .setSafetySourceData(
                eq(applicationContext),
                eq(IdentityCheckSafetySource.SAFETY_SOURCE_ID),
                safetySourceDataArgumentCaptor.capture(),
                eq(refreshSafetyEvent),
            )

        val safetySourceIssue: SafetySourceIssue =
            safetySourceDataArgumentCaptor.firstValue.issues[0]!!
        val actionPendingIntent = safetySourceIssue.actions[0].pendingIntent

        assertThat(safetySourceIssue.customNotification).isNull()
        assertThat(actionPendingIntent.intent.action).isEqualTo(ACTION_ISSUE_CARD_SHOW_DETAILS)
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_IDENTITY_CHECK_WATCH)
    fun refreshSafetySources_whenWatchPromoCardAlreadyShown_setsNullData() {
        assume()
            .that(
                applicationContext.resources.getBoolean(
                    R.bool.config_show_identity_check_watch_promo
                )
            )
            .isTrue()

        whenever(safetyCenterManagerWrapper.isEnabled(applicationContext)).thenReturn(true)

        setWatchRangingSupportedValue(true)
        setIdentityCheckWatchPromoCardShown(true)
        // This is always true if watch promo card is shown
        setIdentityCheckPromoCardShown(true)

        IdentityCheckSafetySource.setSafetySourceData(
            applicationContext,
            refreshSafetyEvent,
            biometricManager,
            userManager,
        )

        verify(safetyCenterManagerWrapper)
            .setSafetySourceData(
                eq(applicationContext),
                eq(IdentityCheckSafetySource.SAFETY_SOURCE_ID),
                eq(null),
                eq(refreshSafetyEvent),
            )
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_IDENTITY_CHECK_WATCH)
    fun refreshSafetySources_whenWatchPromoCardNotShownAndGeneralPromoCardShown_setsWatchData() {
        assume()
            .that(
                applicationContext.resources.getBoolean(
                    R.bool.config_show_identity_check_watch_promo
                )
            )
            .isTrue()

        whenever(safetyCenterManagerWrapper.isEnabled(applicationContext)).thenReturn(true)

        setWatchRangingSupportedValue(true)
        setIdentityCheckWatchPromoCardShown(false)
        setIdentityCheckPromoCardShown(true)

        IdentityCheckSafetySource.setSafetySourceData(
            applicationContext,
            refreshSafetyEvent,
            biometricManager,
            userManager,
        )

        verify(safetyCenterManagerWrapper)
            .setSafetySourceData(
                eq(applicationContext),
                eq(IdentityCheckSafetySource.SAFETY_SOURCE_ID),
                safetySourceDataArgumentCaptor.capture(),
                eq(refreshSafetyEvent),
            )

        val safetySourceIssue: SafetySourceIssue =
            safetySourceDataArgumentCaptor.firstValue.issues[0]!!

        assertThat(safetySourceIssue.actions[0].pendingIntent.intent.action)
            .isEqualTo(ACTION_ISSUE_CARD_WATCH_SHOW_DETAILS)
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_IDENTITY_CHECK_WATCH)
    fun refreshSafetySources_watchAvailable_notificationNotClicked_setsDataWithNotification() {
        whenever(safetyCenterManagerWrapper.isEnabled(applicationContext)).thenReturn(true)

        setWatchRangingSupportedValue(true)
        setIdentityCheckWatchPromoCardShown(false)
        setIdentityCheckWatchNotificationClicked(false)

        IdentityCheckSafetySource.setSafetySourceData(
            applicationContext,
            refreshSafetyEvent,
            biometricManager,
            userManager,
        )

        verify(safetyCenterManagerWrapper)
            .setSafetySourceData(
                eq(applicationContext),
                eq(IdentityCheckSafetySource.SAFETY_SOURCE_ID),
                safetySourceDataArgumentCaptor.capture(),
                eq(refreshSafetyEvent),
            )

        val safetySourceIssue: SafetySourceIssue =
            safetySourceDataArgumentCaptor.firstValue.issues[0]!!
        val actionPendingIntent = safetySourceIssue.actions[0].pendingIntent
        val showWatchPromo =
            applicationContext.resources.getBoolean(R.bool.config_show_identity_check_watch_promo)
        val expectedAction =
            if (showWatchPromo) ACTION_ISSUE_CARD_WATCH_SHOW_DETAILS
            else ACTION_ISSUE_CARD_SHOW_DETAILS
        val notificationExpectedAction =
            if (showWatchPromo) ACTION_WATCH_ISSUE_NOTIFICATION_CLICKED
            else ACTION_ISSUE_NOTIFICATION_CLICKED

        assertThat(actionPendingIntent.intent.action).isEqualTo(expectedAction)
        assertThat(safetySourceIssue.customNotification!!.actions[0].pendingIntent.intent.action)
            .isEqualTo(notificationExpectedAction)
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_IDENTITY_CHECK_WATCH)
    fun refreshSafetySources_watchAvailable_notificationClicked_setsDataWithoutNotification() {
        assume()
            .that(
                applicationContext.resources.getBoolean(
                    R.bool.config_show_identity_check_watch_promo
                )
            )
            .isTrue()

        whenever(safetyCenterManagerWrapper.isEnabled(applicationContext)).thenReturn(true)

        setWatchRangingSupportedValue(true)
        setIdentityCheckWatchPromoCardShown(false)
        setIdentityCheckWatchNotificationClicked(true)

        IdentityCheckSafetySource.setSafetySourceData(
            applicationContext,
            refreshSafetyEvent,
            biometricManager,
            userManager,
        )

        verify(safetyCenterManagerWrapper)
            .setSafetySourceData(
                eq(applicationContext),
                eq(IdentityCheckSafetySource.SAFETY_SOURCE_ID),
                safetySourceDataArgumentCaptor.capture(),
                eq(refreshSafetyEvent),
            )

        val safetySourceIssue: SafetySourceIssue =
            safetySourceDataArgumentCaptor.firstValue.issues[0]!!
        val actionPendingIntent = safetySourceIssue.actions[0].pendingIntent

        assertThat(safetySourceIssue.customNotification).isNull()
        assertThat(actionPendingIntent.intent.action)
            .isEqualTo(ACTION_ISSUE_CARD_WATCH_SHOW_DETAILS)
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_IDENTITY_CHECK_WATCH)
    @RequiresFlagsDisabled(FLAG_SCHEDULE_WATCH_RANGING_AVAILABILITY_WITH_JOB_SCHEDULER)
    fun watchContentObserver_onChange_setsSafetySourceData() {
        whenever(safetyCenterManagerWrapper.isEnabled(applicationContext)).thenReturn(true)

        setWatchRangingSupportedValue(true)
        setIdentityCheckPromoCardShown(false)

        val observer = IdentityCheckSafetySource.WatchContentObserver(applicationContext)
        val uri =
            Settings.Global.getUriFor(Settings.Global.WATCH_RANGING_SUPPORTED_BY_PRIMARY_DEVICE)

        observer.onChange(false, uri)

        verify(safetyCenterManagerWrapper, atLeastOnce())
            .setSafetySourceData(
                eq(applicationContext),
                eq(IdentityCheckSafetySource.SAFETY_SOURCE_ID),
                any(),
                eq(sourceChangeSafetyEvent),
            )
    }

    private fun setIdentityCheckWatchNotificationClicked(clicked: Boolean) {
        Settings.Secure.putInt(
            applicationContext.contentResolver,
            Settings.Secure.IDENTITY_CHECK_WATCH_NOTIFICATION_VIEW_DETAILS_CLICKED,
            if (clicked) 1 else 0,
        )
    }

    private fun setIdentityCheckWatchPromoCardShown(hasShown: Boolean) {
        Settings.Secure.putInt(
            applicationContext.contentResolver,
            Settings.Secure.IDENTITY_CHECK_WATCH_PROMO_CARD_SHOWN,
            if (hasShown) 1 else 0,
        )
    }

    private fun setIdentityCheckNotificationBeenClicked(clicked: Boolean) {
        Settings.Secure.putInt(
            applicationContext.contentResolver,
            Settings.Secure.IDENTITY_CHECK_NOTIFICATION_VIEW_DETAILS_CLICKED,
            if (clicked) 1 else 0,
        )
    }

    private fun setIdentityCheckPromoCardShown(hasShown: Boolean) {
        Settings.Secure.putInt(
            applicationContext.contentResolver,
            Settings.Secure.IDENTITY_CHECK_PROMO_CARD_SHOWN,
            if (hasShown) 1 else 0,
        )
    }

    private fun setWatchRangingSupportedValue(isWatchAvailable: Boolean) {
        Settings.Global.putInt(
            applicationContext.contentResolver,
            Settings.Global.WATCH_RANGING_SUPPORTED_BY_PRIMARY_DEVICE,
            if (isWatchAvailable) 1 else 0,
        )
    }

    private fun resetWatchRangingSupportedValue() {
        Settings.Global.putString(
            applicationContext.contentResolver,
            Settings.Global.WATCH_RANGING_SUPPORTED_BY_PRIMARY_DEVICE,
            null,
        )
    }

    private fun setIdentityCheckToggleStatus(enabled: Boolean) =
        Settings.Secure.putInt(
            applicationContext.contentResolver,
            Settings.Secure.MANDATORY_BIOMETRICS,
            if (enabled) 1 else 0,
        )
}
