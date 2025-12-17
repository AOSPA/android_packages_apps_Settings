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

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.UserHandle
import android.safetycenter.SafetyCenterEntry
import android.safetycenter.SafetyCenterIssue
import android.safetycenter.SafetyCenterManager.EXTRA_SAFETY_SOURCE_ISSUE_ID
import android.safetycenter.config.SafetyCenterConfig
import android.safetycenter.config.SafetySource
import com.android.settings.core.instrumentation.SettingsStatsLog
import com.android.settingslib.safetycenter.SafetyCenterUiEntry
import com.android.settingslib.safetycenter.SafetySourcePreference
import java.math.BigInteger
import java.security.MessageDigest

/**
 * A helper class to log user interactions with the Safety Center.
 *
 * This class provides a structured way to record various user actions, such as viewing the Safety
 * Center, interacting with safety issues, and clicking on entries.
 *
 * The logger is initialized with a [SafetyCenterConfig] to determine which safety sources should be
 * excluded from logging.
 *
 * @property sessionId A unique identifier for the user's current session.
 * @property viewType The type of view the user is currently interacting with (e.g., FULL, SUBPAGE).
 * @property navigationSource The source from which the user navigated to the current screen (e.g.,
 *   NOTIFICATION, SETTINGS).
 * @property subpageId The key of the subpage the user is currently viewing, if any.
 */
class InteractionLogger
private constructor(private val context: Context, private val noLogSourceIds: Set<String?>) {
    var sessionId: Long = SafetyCenterSessionUtils.INVALID_SESSION_ID
        set(value) {
            if (field != value) {
                field = value
                // clearing viewed issues when user session changes
                clearViewedIssues()
            }
        }

    var viewType: ViewType = ViewType.UNKNOWN
    var navigationSource: NavigationSource = NavigationSource.UNKNOWN
    var subpageId: String? = null

    private val viewedIssueIds: MutableSet<String> = mutableSetOf()

    constructor(
        context: Context,
        safetyCenterConfig: SafetyCenterConfig?,
    ) : this(context, extractNoLogSourceIds(safetyCenterConfig))

    /** Records a generic user action. */
    fun record(action: Action) {
        writeAtom(action)
    }

    /**
     * Records that a safety issue has been viewed. To prevent duplicate logging, this method only
     * records the first time an issue is viewed within a session.
     */
    fun recordIssueViewed(issue: SafetyCenterIssue, isDismissed: Boolean) {
        if (viewedIssueIds.contains(issue.id)) {
            return
        }
        recordForIssue(Action.SAFETY_ISSUE_VIEWED, issue, isDismissed)
        viewedIssueIds.add(issue.id)
    }

    /** Clears the set of viewed issue IDs, allowing them to be logged again. */
    private fun clearViewedIssues() {
        viewedIssueIds.clear()
    }

    /** Records a user action related to a specific safety issue. */
    fun recordForIssue(action: Action, issue: SafetyCenterIssue, isDismissed: Boolean) {
        writeAtom(
            action = action,
            severityLevel = LogSeverityLevel.fromIssueSeverityLevel(issue.severityLevel),
            sourceId = issue.safetySourceIds.firstOrNull(),
            sourceProfileType = SafetySourceProfileType.fromUserHandle(context, issue.user),
            issueTypeId = issue.issueTypeId,
            issueState =
                if (isDismissed) {
                    SettingsStatsLog.SAFETY_CENTER_INTERACTION_REPORTED__ISSUE_STATE__DISMISSED
                } else {
                    SettingsStatsLog.SAFETY_CENTER_INTERACTION_REPORTED__ISSUE_STATE__ACTIVE
                },
        )
    }

    /** Records a user action related to a specific safety entry. */
    fun recordForEntry(action: Action, entry: SafetyCenterUiEntry) {
        writeAtom(
            action = action,
            severityLevel = LogSeverityLevel.fromEntrySeverityLevel(entry.severityLevel),
            sourceId = entry.safetySourceId,
            sourceProfileType = SafetySourceProfileType.fromUserHandle(context, entry.user),
        )
    }

    private fun writeAtom(
        action: Action,
        severityLevel: LogSeverityLevel = LogSeverityLevel.UNKNOWN,
        sourceId: String? = null,
        sourceProfileType: SafetySourceProfileType = SafetySourceProfileType.UNKNOWN,
        issueTypeId: String? = null,
        issueState: Int =
            SettingsStatsLog.SAFETY_CENTER_INTERACTION_REPORTED__ISSUE_STATE__ISSUE_STATE_UNKNOWN,
    ) {
        if (noLogSourceIds.contains(sourceId)) {
            return
        }

        SettingsStatsLog.write(
            SettingsStatsLog.SAFETY_CENTER_INTERACTION_REPORTED,
            sessionId,
            action.statsLogValue,
            viewType.statsLogValue,
            navigationSource.statsLogValue,
            severityLevel.statsLogValue,
            encodeStringId(sourceId),
            sourceProfileType.statsLogValue,
            encodeStringId(issueTypeId),
            SENSOR_UNKNOWN,
            encodeStringId(subpageId),
            issueState,
        )
    }

    companion object {
        const val SENSOR_UNKNOWN = 0

        /**
         * Encodes a string into an long ID. The ID is a MD5 hash of the string, truncated to 64
         * bits.
         */
        fun encodeStringId(id: String?): Long {
            if (id == null) return 0
            val digest = MessageDigest.getInstance("MD5")
            digest.update(id.toByteArray())
            // Truncate to the size of a long
            return BigInteger(digest.digest()).toLong()
        }

        private fun extractNoLogSourceIds(safetyCenterConfig: SafetyCenterConfig?): Set<String?> {
            if (safetyCenterConfig == null) return setOf()
            return safetyCenterConfig.safetySourcesGroups
                .asSequence()
                .flatMap { it.safetySources }
                .filterNot { it.isLoggable() }
                .map { it.id }
                .toSet()
        }

        private fun SafetySource.isLoggable(): Boolean =
            try {
                isLoggingAllowed
            } catch (ex: UnsupportedOperationException) {
                // isLoggingAllowed will throw if you call it on a static source
                // Default to logging all sources that don't support this config value.
                true
            }
    }
}

/** Represents a user action within the Safety Center. */
enum class Action(val statsLogValue: Int) {
    UNKNOWN(SettingsStatsLog.SAFETY_CENTER_INTERACTION_REPORTED__ACTION__ACTION_UNKNOWN),
    SAFETY_CENTER_VIEWED(
        SettingsStatsLog.SAFETY_CENTER_INTERACTION_REPORTED__ACTION__SAFETY_CENTER_VIEWED
    ),
    SAFETY_ISSUE_VIEWED(
        SettingsStatsLog.SAFETY_CENTER_INTERACTION_REPORTED__ACTION__SAFETY_ISSUE_VIEWED
    ),
    SCAN_INITIATED(SettingsStatsLog.SAFETY_CENTER_INTERACTION_REPORTED__ACTION__SCAN_INITIATED),
    ISSUE_PRIMARY_ACTION_CLICKED(
        SettingsStatsLog.SAFETY_CENTER_INTERACTION_REPORTED__ACTION__ISSUE_PRIMARY_ACTION_CLICKED
    ),
    ISSUE_SECONDARY_ACTION_CLICKED(
        SettingsStatsLog.SAFETY_CENTER_INTERACTION_REPORTED__ACTION__ISSUE_SECONDARY_ACTION_CLICKED
    ),
    ISSUE_DISMISS_CLICKED(
        SettingsStatsLog.SAFETY_CENTER_INTERACTION_REPORTED__ACTION__ISSUE_DISMISS_CLICKED
    ),
    ENTRY_CLICKED(SettingsStatsLog.SAFETY_CENTER_INTERACTION_REPORTED__ACTION__ENTRY_CLICKED),
}

/** Represents the type of view the user is interacting with. */
enum class ViewType(val statsLogValue: Int) {
    UNKNOWN(SettingsStatsLog.SAFETY_CENTER_INTERACTION_REPORTED__VIEW_TYPE__VIEW_TYPE_UNKNOWN),
    FULL(SettingsStatsLog.SAFETY_CENTER_INTERACTION_REPORTED__VIEW_TYPE__FULL),
    SUBPAGE(SettingsStatsLog.SAFETY_CENTER_INTERACTION_REPORTED__VIEW_TYPE__SUBPAGE),
    QUICK_SETTINGS(SettingsStatsLog.SAFETY_CENTER_INTERACTION_REPORTED__VIEW_TYPE__QUICK_SETTINGS),
}

/** Represents the source from which the user navigated to the current screen. */
enum class NavigationSource(val statsLogValue: Int) {
    UNKNOWN(SettingsStatsLog.SAFETY_CENTER_INTERACTION_REPORTED__NAVIGATION_SOURCE__SOURCE_UNKNOWN),
    NOTIFICATION(
        SettingsStatsLog.SAFETY_CENTER_INTERACTION_REPORTED__NAVIGATION_SOURCE__NOTIFICATION
    ),
    SETTINGS(SettingsStatsLog.SAFETY_CENTER_INTERACTION_REPORTED__NAVIGATION_SOURCE__SETTINGS),
    QUICK_SETTINGS_TILE(
        SettingsStatsLog.SAFETY_CENTER_INTERACTION_REPORTED__NAVIGATION_SOURCE__QUICK_SETTINGS_TILE
    ),
    SAFETY_CENTER(
        SettingsStatsLog.SAFETY_CENTER_INTERACTION_REPORTED__NAVIGATION_SOURCE__SAFETY_CENTER
    );

    /** Creates a Bundle with the navigation source. */
    fun createArgs(): Bundle {
        val args = Bundle()
        args.putInt(EXTRA_NAVIGATION_SOURCE, this.statsLogValue)
        return args
    }

    fun addToIntent(intent: Intent) {
        intent.putExtra(EXTRA_NAVIGATION_SOURCE, this.statsLogValue)
    }

    companion object {

        /** Intent extra representing the preference key of a search result */
        const val EXTRA_SETTINGS_FRAGMENT_ARGS_KEY: String = ":settings:fragment_args_key"
        /** Intent/Argument extra representing the navigation source. */
        const val EXTRA_NAVIGATION_SOURCE: String = "navigation_source"

        /**
         * Determines the navigation source by first checking the intent, then falling back to the
         * arguments bundle.
         */
        fun fromIntentOrArguments(intent: Intent?, args: Bundle?): NavigationSource {
            val source = fromIntent(intent)
            return if (source == UNKNOWN) fromArguments(args) else source
        }

        /** Determines the navigation source from the provided [Intent]. */
        private fun fromIntent(intent: Intent?): NavigationSource {
            if (intent == null) {
                return UNKNOWN
            }

            return when (intent.action) {
                Intent.ACTION_VIEW_SAFETY_CENTER_QS -> QUICK_SETTINGS_TILE
                else -> fromHomepageIntent(intent)
            }
        }

        private fun fromHomepageIntent(intent: Intent): NavigationSource {
            val sourceIssueId = intent.getStringExtra(EXTRA_SAFETY_SOURCE_ISSUE_ID)
            val searchKey = intent.getStringExtra(EXTRA_SETTINGS_FRAGMENT_ARGS_KEY)
            val intentNavigationSourceValue =
                intent.getIntExtra(EXTRA_NAVIGATION_SOURCE, UNKNOWN.statsLogValue)
            return if (sourceIssueId != null) {
                NOTIFICATION
            } else if (searchKey != null) {
                SETTINGS
            } else {
                fromStatsLogValue(intentNavigationSourceValue)
            }
        }

        /** Gets the navigation source from a Bundle. */
        private fun fromArguments(args: Bundle?): NavigationSource {
            if (args == null) {
                return UNKNOWN
            }
            val sourceValue = args.getInt(EXTRA_NAVIGATION_SOURCE, UNKNOWN.statsLogValue)
            return fromStatsLogValue(sourceValue)
        }

        private fun fromStatsLogValue(sourceValue: Int): NavigationSource {
            return entries.find { it.statsLogValue == sourceValue } ?: UNKNOWN
        }
    }
}

/** Represents the severity level of a safety issue or entry. */
enum class LogSeverityLevel(val statsLogValue: Int) {
    UNKNOWN(
        SettingsStatsLog
            .SAFETY_CENTER_INTERACTION_REPORTED__SEVERITY_LEVEL__SAFETY_SEVERITY_LEVEL_UNKNOWN
    ),
    UNSPECIFIED(
        SettingsStatsLog
            .SAFETY_CENTER_INTERACTION_REPORTED__SEVERITY_LEVEL__SAFETY_SEVERITY_UNSPECIFIED
    ),
    OK(SettingsStatsLog.SAFETY_CENTER_INTERACTION_REPORTED__SEVERITY_LEVEL__SAFETY_SEVERITY_OK),
    RECOMMENDATION(
        SettingsStatsLog
            .SAFETY_CENTER_INTERACTION_REPORTED__SEVERITY_LEVEL__SAFETY_SEVERITY_RECOMMENDATION
    ),
    CRITICAL_WARNING(
        SettingsStatsLog
            .SAFETY_CENTER_INTERACTION_REPORTED__SEVERITY_LEVEL__SAFETY_SEVERITY_CRITICAL_WARNING
    );

    companion object {
        /** Converts a [SafetyCenterIssue] severity level to a [LogSeverityLevel]. */
        fun fromIssueSeverityLevel(issueLevel: Int): LogSeverityLevel =
            when (issueLevel) {
                SafetyCenterIssue.ISSUE_SEVERITY_LEVEL_OK -> OK
                SafetyCenterIssue.ISSUE_SEVERITY_LEVEL_RECOMMENDATION -> RECOMMENDATION
                SafetyCenterIssue.ISSUE_SEVERITY_LEVEL_CRITICAL_WARNING -> CRITICAL_WARNING
                else -> UNKNOWN
            }

        /** Converts a [SafetyCenterEntry] severity level to a [LogSeverityLevel]. */
        fun fromEntrySeverityLevel(entryLevel: Int): LogSeverityLevel =
            when (entryLevel) {
                SafetyCenterEntry.ENTRY_SEVERITY_LEVEL_OK -> OK
                SafetyCenterEntry.ENTRY_SEVERITY_LEVEL_RECOMMENDATION -> RECOMMENDATION
                SafetyCenterEntry.ENTRY_SEVERITY_LEVEL_CRITICAL_WARNING -> CRITICAL_WARNING
                SafetyCenterEntry.ENTRY_SEVERITY_LEVEL_UNSPECIFIED -> UNSPECIFIED
                else -> UNKNOWN
            }
    }
}

/** Represents the user profile type associated with a safety source. */
enum class SafetySourceProfileType(val statsLogValue: Int) {
    UNKNOWN(
        SettingsStatsLog
            .SAFETY_CENTER_INTERACTION_REPORTED__SAFETY_SOURCE_PROFILE_TYPE__PROFILE_TYPE_UNKNOWN
    ),
    PERSONAL(
        SettingsStatsLog
            .SAFETY_CENTER_INTERACTION_REPORTED__SAFETY_SOURCE_PROFILE_TYPE__PROFILE_TYPE_PERSONAL
    ),
    MANAGED(
        SettingsStatsLog
            .SAFETY_CENTER_INTERACTION_REPORTED__SAFETY_SOURCE_PROFILE_TYPE__PROFILE_TYPE_MANAGED
    ),
    PRIVATE(
        SettingsStatsLog
            .SAFETY_CENTER_INTERACTION_REPORTED__SAFETY_SOURCE_PROFILE_TYPE__PROFILE_TYPE_PRIVATE
    );

    companion object {
        /** Determines the [SafetySourceProfileType] from the given [UserHandle]. */
        fun fromUserHandle(context: Context, userHandle: UserHandle?): SafetySourceProfileType {
            if (userHandle == null) return UNKNOWN
            return when (UserProfilesUtils.getUserProfile(context, userHandle)) {
                SafetySourcePreference.Profile.PERSONAL -> PERSONAL
                SafetySourcePreference.Profile.WORK -> MANAGED
                SafetySourcePreference.Profile.PRIVATE -> PRIVATE
                else -> UNKNOWN
            }
        }
    }
}
