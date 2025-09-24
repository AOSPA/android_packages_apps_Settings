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

package com.android.settings.safetycenter

import android.app.PendingIntent
import android.content.Intent
import android.os.UserHandle
import android.permission.flags.Flags
import android.safetycenter.SafetyCenterData
import android.safetycenter.SafetyCenterEntry
import android.safetycenter.SafetyCenterEntryOrGroup
import android.safetycenter.SafetyCenterIssue
import android.safetycenter.SafetyCenterStatus
import androidx.test.core.app.ApplicationProvider

/** Common utilities and constants for Safety Center tests. */
object SafetyCenterTestUtils {

    val USER_PERSONAL: UserHandle = UserHandle.of(0)
    val USER_WORK_PROFILE: UserHandle = UserHandle.of(10)
    const val TEST_ACTION = "TEST_ACTION"
    val testIntent = Intent("TEST_ACTION")
    val TEST_PENDING_INTENT: PendingIntent =
        PendingIntent.getActivity(
            ApplicationProvider.getApplicationContext(),
            0,
            testIntent,
            PendingIntent.FLAG_IMMUTABLE,
        )
    val DEFAULT_STATUS: SafetyCenterStatus =
        SafetyCenterStatus.Builder("Test Title", "Test Summary")
            .setSeverityLevel(SafetyCenterStatus.OVERALL_SEVERITY_LEVEL_OK)
            .build()
    val EMPTY_SC_DATA: SafetyCenterData = SafetyCenterData.Builder(DEFAULT_STATUS).build()

    fun createEntry(
        id: String,
        title: String,
        userHandle: UserHandle = USER_PERSONAL,
        sourceId: String,
        summary: String? = "Summary $id",
        pendingIntent: PendingIntent? = TEST_PENDING_INTENT,
        severity: Int = SafetyCenterEntry.ENTRY_SEVERITY_LEVEL_OK,
        hasError: Boolean = false,
        iconType: Int? = null,
    ): SafetyCenterEntry {
        val builder =
            if (Flags.openSafetyCenterApis()) {
                SafetyCenterEntry.Builder(id, title, userHandle, sourceId).setHasError(hasError)
            } else {
                SafetyCenterEntry.Builder(id, title)
            }

        builder.apply {
            setSummary(summary)
            setPendingIntent(pendingIntent)
            setSeverityLevel(severity)
            iconType?.let { setSeverityUnspecifiedIconType(it) }
        }
        return builder.build()
    }

    fun createIssue(
        id: String,
        title: String = "Issue Title $id",
        summary: String = "Summary $id",
        userHandle: UserHandle = USER_PERSONAL,
        sourceIds: Set<String>,
        severity: Int = SafetyCenterIssue.ISSUE_SEVERITY_LEVEL_RECOMMENDATION,
    ): SafetyCenterIssue {
        val builder =
            if (Flags.openSafetyCenterApis()) {
                SafetyCenterIssue.Builder(id, title, summary, userHandle, sourceIds, "type_$id")
            } else {
                SafetyCenterIssue.Builder(id, title, summary)
            }
        return builder.setSeverityLevel(severity).build()
    }

    fun createScData(
        entries: List<SafetyCenterEntry> = emptyList(),
        activeIssues: List<SafetyCenterIssue> = emptyList(),
        status: SafetyCenterStatus = DEFAULT_STATUS,
    ): SafetyCenterData {
        val builder = SafetyCenterData.Builder(status)
        entries.forEach { builder.addEntryOrGroup(SafetyCenterEntryOrGroup(it)) }
        activeIssues.forEach { builder.addIssue(it) }
        return builder.build()
    }
}
