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

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.biometrics.Flags
import android.provider.Settings
import android.proximity.IProximityResultCallback
import android.proximity.ProximityResultCode
import android.safetycenter.SafetyEvent
import android.safetycenter.SafetySourceData
import android.safetycenter.SafetySourceIssue
import android.security.authenticationpolicy.AuthenticationPolicyManager
import android.util.Log
import com.android.settings.R
import com.android.settings.biometrics.IdentityCheckPromoCardActivity
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import java.util.concurrent.Executors

/**
 * This class sets Safety Center Issue for [IdentityCheckSafetySource.SAFETY_SOURCE_ID]. It also
 * receives broadcast, [IdentityCheckSafetySource.ISSUE_CARD_DISMISSED_ACTION] when the issue card
 * is dismissed in Safety Center.
 */
class IdentityCheckSafetySource : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val intentAction = intent.action
        if (intentAction.equals(Intent.ACTION_BOOT_COMPLETED)) {
            val pendingResult = goAsync()
            val watchRangingFuture = context.getWatchRangingAvailabilityFuture()
            watchRangingFuture.addListener(
                {
                    try {
                        val watchRangingSupported = watchRangingFuture.get()
                        context.setWatchRangingSupported(watchRangingSupported)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error getting watch ranging support: ${e.message}", e)
                    } finally {
                        pendingResult?.finish()
                    }
                },
                Executors.newSingleThreadExecutor(),
            )
            context.updateIfIdentityCheckWasEnabledInV1()
        }
    }

    companion object {
        const val SAFETY_SOURCE_ID = "AndroidIdentityCheck"
        const val ACTION_ISSUE_CARD_SHOW_DETAILS =
            "com.android.settings.safetycenter.action.IDENTITY_CHECK_ISSUE_CARD_SHOW_DETAILS"
        const val ACTION_ISSUE_CARD_WATCH_SHOW_DETAILS =
            "com.android.settings.safetycenter.action.IDENTITY_CHECK_ISSUE_CARD_WATCH_SHOW_DETAILS"
        private const val TAG = "ICSafetySource"
        private const val ISSUE_CARD_VIEW_DETAILS = 1
        private const val IDENTITY_CHECK_PROMO_CARD_ISSUE_ID = "IdentityCheckPromoCardIssue"
        private const val IDENTITY_CHECK_PROMO_CARD_ISSUE_TYPE = "IdentityCheckAllSurfaces"

        /** Sets the safety source data with the Identity Check issue info. */
        fun setSafetySourceData(context: Context, safetyEvent: SafetyEvent) {
            if (!SafetyCenterManagerWrapper.get().isEnabled(context)) {
                return
            }
            if (!Flags.identityCheckAllSurfaces()) {
                sendNullData(context, safetyEvent)
                return
            }
            if (!hasPromoCardBeenShown(context) && isIdentityCheckEnabled(context)) {
                val safetySourceData =
                    SafetySourceData.Builder().addIssue(getIssue(context)).build()
                SafetyCenterManagerWrapper.get()
                    .setSafetySourceData(context, SAFETY_SOURCE_ID, safetySourceData, safetyEvent)
            } else {
                sendNullData(context, safetyEvent)
            }
        }

        private fun isIdentityCheckEnabled(context: Context): Boolean =
            Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.IDENTITY_CHECK_ENABLED_V1,
                0,
            ) == 1 &&
                Settings.Secure.getInt(
                    context.contentResolver,
                    Settings.Secure.MANDATORY_BIOMETRICS,
                    0, /* default */
                ) == 1

        private fun sendNullData(context: Context, safetyEvent: SafetyEvent) {
            SafetyCenterManagerWrapper.get()
                .setSafetySourceData(
                    context,
                    SAFETY_SOURCE_ID,
                    null /* safetySourceData */,
                    safetyEvent,
                )
        }

        private fun hasPromoCardBeenShown(context: Context): Boolean =
            Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.IDENTITY_CHECK_PROMO_CARD_SHOWN,
                0, /* def */
            ) == 1

        private fun getIssue(context: Context): SafetySourceIssue {
            var issueCardTitle = context.getString(R.string.identity_check_issue_card_title)
            var issueCardSummary = context.getString(R.string.identity_check_issue_card_summary)
            var intentAction = ACTION_ISSUE_CARD_SHOW_DETAILS
            if (shouldShowWatchRangingPromoCard(context)) {
                val watchIssueCardTitle =
                    context.getString(R.string.identity_check_watch_issue_card_title)
                val watchIssueCardSummary =
                    context.getString(R.string.identity_check_watch_issue_card_summary)
                issueCardTitle = watchIssueCardTitle
                issueCardSummary = watchIssueCardSummary
                intentAction = ACTION_ISSUE_CARD_WATCH_SHOW_DETAILS
            }
            return getIdentityCheckAllSurfacesIssue(
                context,
                issueCardTitle,
                issueCardSummary,
                intentAction,
            )
        }

        /**
         * Checks if watch ranging is supported and if the requires strings for the promo card is
         * not empty.
         */
        private fun shouldShowWatchRangingPromoCard(context: Context): Boolean {
            return isWatchRangingSupported(context) &&
                context.resources.getBoolean(R.bool.config_show_identity_check_watch_promo)
        }

        private fun isWatchRangingSupported(context: Context): Boolean =
            Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.WATCH_RANGING_SUPPORTED_BY_PRIMARY_DEVICE,
                0, /* def */
            ) == 1

        /** Returns the safety source issue for Identity Check. */
        private fun getIdentityCheckAllSurfacesIssue(
            context: Context,
            issueCardTitle: String,
            issueCardSummary: String,
            intentAction: String,
        ): SafetySourceIssue {
            val issueCardButtonText =
                context.getString(R.string.identity_check_issue_card_button_text)
            val action =
                SafetySourceIssue.Action.Builder(
                        ACTION_ISSUE_CARD_SHOW_DETAILS,
                        issueCardButtonText,
                        PendingIntent.getActivity(
                            context,
                            ISSUE_CARD_VIEW_DETAILS,
                            Intent(intentAction)
                                .setClass(context, IdentityCheckPromoCardActivity::class.java),
                            PendingIntent.FLAG_IMMUTABLE,
                        ),
                    )
                    .build()
            return SafetySourceIssue.Builder(
                    IDENTITY_CHECK_PROMO_CARD_ISSUE_ID,
                    issueCardTitle,
                    issueCardSummary,
                    SafetySourceData.SEVERITY_LEVEL_INFORMATION,
                    IDENTITY_CHECK_PROMO_CARD_ISSUE_TYPE,
                )
                .setIssueCategory(SafetySourceIssue.ISSUE_CATEGORY_GENERAL)
                .addAction(action)
                .setIssueActionability(SafetySourceIssue.ISSUE_ACTIONABILITY_TIP)
                .build()
        }
    }

    private fun Context.updateIfIdentityCheckWasEnabledInV1() {
        try {
            Settings.Secure.getInt(contentResolver, Settings.Secure.IDENTITY_CHECK_ENABLED_V1)
        } catch (exception: Settings.SettingNotFoundException) {
            val identityCheckToggleEnabled =
                Settings.Secure.getInt(
                    contentResolver,
                    Settings.Secure.MANDATORY_BIOMETRICS,
                    0, /* default */
                )
            Settings.Secure.putInt(
                contentResolver,
                Settings.Secure.IDENTITY_CHECK_ENABLED_V1,
                identityCheckToggleEnabled,
            )
        }
    }

    private fun Context.getWatchRangingAvailabilityFuture(): ListenableFuture<Boolean> {
        val future = SettableFuture.create<Boolean>()

        if (!hasPromoCardBeenShown(this) && Flags.identityCheckWatch()) {
            val authenticationPolicyManager =
                getSystemService(AuthenticationPolicyManager::class.java)

            if (authenticationPolicyManager == null) {
                Log.e(TAG, "Authentication policy manager is null. Setting future to false.")
                future.set(false)
            } else {
                authenticationPolicyManager.isWatchRangingAvailable(
                    object : IProximityResultCallback.Stub() {
                        override fun onError(errorCode: Int) {
                            val supported =
                                errorCode !=
                                    ProximityResultCode.PRIMARY_DEVICE_RANGING_NOT_SUPPORTED
                            future.set(supported)
                        }

                        override fun onSuccess(p0: Int) {
                            future.set(true)
                        }
                    }
                )
            }
        } else {
            future.set(false)
        }
        return future
    }

    private fun Context.setWatchRangingSupported(boolean: Boolean) =
        Settings.Global.putInt(
            contentResolver,
            Settings.Global.WATCH_RANGING_SUPPORTED_BY_PRIMARY_DEVICE,
            if (boolean) 1 else 0,
        )
}
