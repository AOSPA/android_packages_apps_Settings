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
import android.safetycenter.SafetyCenterManager
import android.safetycenter.SafetyEvent
import android.safetycenter.SafetySourceData
import android.safetycenter.SafetySourceIssue
import android.util.Log
import com.android.internal.annotations.VisibleForTesting
import com.android.settings.R
import com.android.settings.biometrics.IdentityCheckPromoCardActivity

/**
 * This class sets Safety Center Issue for [IdentityCheckSafetySource.SAFETY_SOURCE_ID].
 * It also receives broadcast, [IdentityCheckSafetySource.ISSUE_CARD_DISMISSED_ACTION]
 * when the issue card is dismissed in Safety Center.
 */
class IdentityCheckSafetySource : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val intentAction = intent.action
        if (intentAction.equals(ISSUE_CARD_DISMISSED_ACTION)) {
            Log.d(TAG, "Identity Check issue dismissed in Safety Center.");
            Settings.Global.putInt(context.contentResolver,
                Settings.Global.IDENTITY_CHECK_PROMO_CARD_SHOWN, 1);
        }
    }

    companion object {
        const val SAFETY_SOURCE_ID = "AndroidIdentityCheck"
        @VisibleForTesting
        const val ISSUE_CARD_DISMISSED_ACTION =
            "com.android.settings.safetycenter.action.IDENTITY_CHECK_ISSUE_CARD_DISMISSED"

        private const val TAG = "ICSafetySource"
        private const val ISSUE_CARD_VIEW_DETAILS = 0
        private const val ISSUE_CARD_DISMISSED = 1
        private const val IDENTITY_CHECK_PROMO_CARD_ISSUE_ID = "IdentityCheckPromoCardIssue"
        private const val IDENTITY_CHECK_PROMO_CARD_ISSUE_TYPE = "IdentityCheckAllSurfaces"
        private const val ISSUE_CARD_SHOW_DETAILS=
            "com.android.settings.safetycenter.action.IDENTITY_CHECK_ISSUE_CARD_SHOW_DETAILS"

        /**
         * Sets the safety source data with the Identity Check issue info.
         */
        fun setSafetySourceData(context: Context, safetyEvent: SafetyEvent) {
            if (!SafetyCenterManagerWrapper.get().isEnabled(context)) {
                return
            }
            if (!Flags.identityCheckAllSurfaces()) {
                sendNullData(context, safetyEvent)
                return
            }

            val hasPromoCardBeenShown = Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.IDENTITY_CHECK_PROMO_CARD_SHOWN,
                0 /* def */) == 1
            val isIdentityCheckEnabled = Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.MANDATORY_BIOMETRICS,
                0 /* def */) == 1

            if (!hasPromoCardBeenShown && isIdentityCheckEnabled) {
                val safetySourceData = SafetySourceData.Builder()
                    .addIssue(getIdentityCheckAllSurfacesIssue(context))
                    .build()
                SafetyCenterManagerWrapper.get().setSafetySourceData(
                    context, SAFETY_SOURCE_ID, safetySourceData, safetyEvent
                )
            } else {
                sendNullData(context, safetyEvent)
            }
        }

        private fun sendNullData(context: Context, safetyEvent: SafetyEvent) {
            SafetyCenterManagerWrapper.get().setSafetySourceData(
                context, SAFETY_SOURCE_ID, null /* safetySourceData */, safetyEvent
            )
        }

        /**
         * Returns the safety source issue for Identity Check.
         */
        private fun getIdentityCheckAllSurfacesIssue(context: Context): SafetySourceIssue {
            val issueCardTitle = context.getString(R.string.identity_check_issue_card_title)
            val issueCardSummary = context.getString(R.string.identity_check_issue_card_summary)
            val issueCardButtonText = context.getString(
                R.string.identity_check_issue_card_button_text)
            val action = SafetySourceIssue.Action.Builder(
                ISSUE_CARD_SHOW_DETAILS,
                issueCardButtonText,
                PendingIntent.getActivity(
                    context,
                    ISSUE_CARD_VIEW_DETAILS,
                    Intent().setClass(context, IdentityCheckPromoCardActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE))
                .build()
            val onDismissPendingIntent = PendingIntent.getBroadcast(
                context,
                ISSUE_CARD_DISMISSED,
                Intent(ISSUE_CARD_DISMISSED_ACTION).setClass(
                    context, IdentityCheckSafetySource::class.java
                ),
                PendingIntent.FLAG_IMMUTABLE)

            return SafetySourceIssue.Builder(
                IDENTITY_CHECK_PROMO_CARD_ISSUE_ID,
                issueCardTitle,
                issueCardSummary,
                SafetySourceData.SEVERITY_LEVEL_INFORMATION,
                IDENTITY_CHECK_PROMO_CARD_ISSUE_TYPE)
                .setIssueCategory(SafetySourceIssue.ISSUE_CATEGORY_GENERAL)
                .addAction(action)
                .setIssueActionability(SafetySourceIssue.ISSUE_ACTIONABILITY_MANUAL)
                .setOnDismissPendingIntent(onDismissPendingIntent)
                .build()
        }
    }
}