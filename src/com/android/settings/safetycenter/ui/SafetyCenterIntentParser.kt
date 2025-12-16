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

package com.android.settings.safetycenter.ui

import android.content.Intent
import android.os.UserHandle
import android.safetycenter.SafetyCenterIssue
import android.safetycenter.SafetyCenterManager.EXTRA_SAFETY_SOURCE_ID
import android.safetycenter.SafetyCenterManager.EXTRA_SAFETY_SOURCE_ISSUE_ID
import android.safetycenter.SafetyCenterManager.EXTRA_SAFETY_SOURCE_USER_HANDLE
import android.util.Log

/** Helper object to parse intent extras related to a focused Safety Center issue. */
object SafetyCenterIntentParser {

    private const val TAG = "SCIntentParser"

    /**
     * Parses the intent to extract the focused issue key.
     *
     * @param intent The intent to parse.
     * @return A [FocusedIssueKey] if all required extras are present, null otherwise.
     */
    fun getFocusedIssueKeyFromIntent(intent: Intent): FocusedIssueKey? {
        if (Intent.ACTION_SAFETY_CENTER != intent.action) {
            Log.d(TAG, "Intent action is not SAFETY_CENTER: ${intent.action}, no focused issue.")
            return null
        }

        val sourceIssueId = intent.getStringExtra(EXTRA_SAFETY_SOURCE_ISSUE_ID)
        val sourceId = intent.getStringExtra(EXTRA_SAFETY_SOURCE_ID)
        val userHandle: UserHandle =
            intent.getParcelableExtra(EXTRA_SAFETY_SOURCE_USER_HANDLE)
                ?: UserHandle.of(UserHandle.myUserId())

        return if (sourceIssueId != null && sourceId != null) {
            val focusedIssueKey = FocusedIssueKey(sourceIssueId, sourceId, userHandle)
            Log.d(TAG, "Parsed FocusedIssueKey: $focusedIssueKey")
            focusedIssueKey
        } else {
            Log.d(
                TAG,
                "Missing required extras for FocusedIssueKey - Issue ID: $sourceIssueId, Source ID: $sourceId",
            )
            null
        }
    }
}

/**
 * Data class to hold the components to identify a specific safety issue. These components are
 * typically extracted from an intent.
 *
 * @property sourceIssueId The ID of the issue as provided by the safety source.
 * @property sourceId The ID of the safety source that reported the issue.
 * @property userHandle The Android [UserHandle] the issue belongs to.
 */
data class FocusedIssueKey(
    val sourceIssueId: String,
    val sourceId: String,
    val userHandle: UserHandle,
) {
    /**
     * Checks if this [FocusedIssueKey] matches the given [SafetyCenterIssue].
     *
     * A match is determined by comparing the `sourceIssueId`, `sourceId`, and `userHandle`.
     *
     * @param issue The [SafetyCenterIssue] to compare against this key.
     * @return `true` if the issue's properties match this key, `false` otherwise.
     */
    fun matchesSafetyCenterIssue(issue: SafetyCenterIssue): Boolean {
        return issue.safetySourceIssueId == this.sourceIssueId &&
            issue.safetySourceIds.contains(this.sourceId) &&
            issue.user == this.userHandle
    }
}
