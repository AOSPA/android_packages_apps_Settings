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

package com.android.settings.safetycenter.ui.model

import android.Manifest
import android.app.Application
import android.permission.flags.Flags
import android.safetycenter.SafetyCenterData
import android.safetycenter.SafetyCenterErrorDetails
import android.safetycenter.SafetyCenterIssue
import android.safetycenter.SafetyCenterManager
import android.safetycenter.SafetyCenterStatus
import android.util.Log
import androidx.annotation.MainThread
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat.getMainExecutor
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.map
import androidx.lifecycle.viewmodel.CreationExtras
import com.android.settings.R
import com.android.settings.safetycenter.ui.InteractionLogger
import com.android.settings.safetycenter.ui.SafetyCenterSubpageRegistry
import com.android.settingslib.safetycenter.SafetyCenterDataTransformer
import com.android.settingslib.safetycenter.SafetyCenterUiData
import kotlin.reflect.KClass

/**
 * A [SafetyCenterViewModel] that provides real-time data from the backing [SafetyCenterManager]
 * service.
 *
 * @param app The application context, used to retrieve system services.
 */
class LiveSafetyCenterViewModel(app: Application) : SafetyCenterViewModel(app) {

    private val safetyCenterManager = app.getSystemService(SafetyCenterManager::class.java)!!
    private val _safetyCenterLiveData = SafetyCenterLiveData()
    private val _errorLiveData = MutableLiveData<SafetyCenterErrorDetails>()
    private var changingConfigurations = false

    override val safetyCenterUiLiveData: LiveData<SafetyCenterUiData> by this::_safetyCenterLiveData

    override val statusUiLiveData: LiveData<StatusUiData>
        get() = safetyCenterUiLiveData.map { StatusUiData(it) }

    override val errorLiveData: LiveData<SafetyCenterErrorDetails> by this::_errorLiveData

    override val interactionLogger: InteractionLogger by lazy {
        // Fetching the config to build this set of source IDs requires IPC, so we do this
        // initialization lazily.
        InteractionLogger(app, safetyCenterManager.safetyCenterConfig)
    }

    private val sourceIdToSubpageTitleResIdMap: Map<String, Int> by lazy {
        SafetyCenterSubpageRegistry.subpageConfigs.entries
            .flatMap { (subpageKey, config) ->
                val sourceIds =
                    SafetyCenterSubpageRegistry.getAllSafetySourceIds(getApplication(), subpageKey)
                sourceIds.map { sourceId -> sourceId to config.titleResId }
            }
            .toMap()
    }

    /** Creates a map of Issue ID to the String Resource ID for the header title. */
    fun getIssueIdToHeaderResIdMap(data: SafetyCenterUiData): Map<String, Int> {
        val allIssues = (data.getActiveIssues() + data.getDismissedIssues()).distinctBy { it.id }

        return allIssues.associate { issue ->
            val subpageTitleResId =
                issue.safetySourceIds.firstNotNullOfOrNull { sourceIdToSubpageTitleResIdMap[it] }
            issue.id to (subpageTitleResId ?: R.string.safety_center_title)
        }
    }

    /**
     * A [MutableLiveData] that listens for changes from the [SafetyCenterManager].
     *
     * It automatically registers and unregisters the listener when it becomes active or inactive.
     * It also manages a queue to handle issue resolution animations.
     */
    private inner class SafetyCenterLiveData :
        MutableLiveData<SafetyCenterUiData>(),
        SafetyCenterManager.OnSafetyCenterDataChangedListener {
        // Managing the data queue isn't designed to support multithreading. Any methods that
        // manipulate it, or the inFlight or resolved issues lists should only be called on the
        // main thread, and are marked accordingly.
        private val safetyCenterDataQueue = ArrayDeque<SafetyCenterData>()
        private var issuesPendingResolution = mapOf<IssueId, ActionId>()
        private val currentResolvedIssues = mutableMapOf<IssueId, ActionId>()

        init {
            // Initialize the LiveData with the current data so that it is available immediately.
            // This allows controllers to access the data in displayPreference() before the
            // LiveData is active.
            onSafetyCenterDataChanged(safetyCenterManager.safetyCenterData)
        }

        @RequiresPermission(Manifest.permission.MANAGE_SAFETY_CENTER)
        override fun onActive() {
            safetyCenterManager.addOnSafetyCenterDataChangedListener(
                getMainExecutor(app.applicationContext),
                this,
            )
            super.onActive()
        }

        @RequiresPermission(Manifest.permission.MANAGE_SAFETY_CENTER)
        override fun onInactive() {
            safetyCenterManager.removeOnSafetyCenterDataChangedListener(this)

            if (!changingConfigurations) {
                // Remove all the tracked state and start from scratch when active again.
                issuesPendingResolution = mapOf()
                currentResolvedIssues.clear()
                safetyCenterDataQueue.clear()
            }
            super.onInactive()
        }

        @MainThread
        override fun onSafetyCenterDataChanged(data: SafetyCenterData) {
            safetyCenterDataQueue.addLast(data)
            maybeProcessDataToNextResolvedIssues()
        }

        override fun onError(errorDetails: SafetyCenterErrorDetails) {
            _errorLiveData.value = errorDetails
        }

        @MainThread
        private fun maybeProcessDataToNextResolvedIssues() {
            // Only process data updates while we aren't waiting for issue resolution animations
            // to complete.
            if (currentResolvedIssues.isNotEmpty()) {
                Log.d(
                    TAG,
                    "Received SafetyCenterData while issue resolution animations" +
                        " occurring. Will update UI with new data soon.",
                )
                return
            }

            while (safetyCenterDataQueue.isNotEmpty() && currentResolvedIssues.isEmpty()) {
                val nextData = safetyCenterDataQueue.first()

                // Calculate newly resolved issues by diffing the tracked in-flight issues and the
                // current update. Resolved issues are formerly in-flight issues that no longer
                // appear in a subsequent SafetyCenterData update.
                val nextResolvedIssues: Map<IssueId, ActionId> =
                    determineResolvedIssues(nextData.buildIssueIdSet())

                // Save the set of in-flight issues to diff against the next data update, removing
                // the now-resolved, formerly in-flight issues. If these are not tracked separately
                // the queue will not progress once the issue resolution animations complete.
                issuesPendingResolution = nextData.getInFlightIssues()

                if (nextResolvedIssues.isNotEmpty()) {
                    currentResolvedIssues.putAll(nextResolvedIssues)
                    sendResolvedIssuesAndCurrentData()
                } else if (shouldEndScan(nextData) || shouldSendLastDataInQueue()) {
                    sendNextData()
                } else {
                    skipNextData()
                }
            }
        }

        private fun determineResolvedIssues(nextIssueIds: Set<IssueId>): Map<IssueId, ActionId> {
            // Any previously in-flight issue that does not appear in the incoming SafetyCenterData
            // is considered resolved.
            return issuesPendingResolution.filterNot { issue -> nextIssueIds.contains(issue.key) }
        }

        private fun shouldEndScan(nextData: SafetyCenterData): Boolean =
            isCurrentlyScanning() && !nextData.isScanning()

        private fun shouldSendLastDataInQueue(): Boolean =
            !isCurrentlyScanning() && safetyCenterDataQueue.size == 1

        private fun isCurrentlyScanning(): Boolean =
            value?.status?.refreshStatus ==
                SafetyCenterStatus.REFRESH_STATUS_FULL_RESCAN_IN_PROGRESS

        private fun sendNextData() {
            value = SafetyCenterDataTransformer.transform(safetyCenterDataQueue.removeFirst())
        }

        private fun skipNextData() = safetyCenterDataQueue.removeFirst()

        private fun sendResolvedIssuesAndCurrentData() {
            val currentUiData = value
            if (currentUiData == null || currentResolvedIssues.isEmpty()) {
                // There can only be resolved issues after receiving data with in-flight issues,
                // so we should always have already sent data here.
                throw IllegalArgumentException("No current data or no resolved issues")
            }
            // The current SafetyCenterUiData still contains the resolved SafetyCenterIssue objects.
            // Send it with the resolved IDs so the UI can generate the correct preferences and
            // trigger the right animations for issue resolution.
            value = currentUiData.copyWithResolvedIssues(currentResolvedIssues)
        }

        @MainThread
        fun markIssueResolvedUiCompleted(issueId: IssueId) {
            currentResolvedIssues.remove(issueId)
            maybeProcessDataToNextResolvedIssues()
        }
    }

    override fun changingConfigurations() {
        changingConfigurations = true
    }

    private fun executeIfNotChangingConfigurations(block: () -> Unit) {
        if (changingConfigurations) {
            changingConfigurations = false
            return
        }
        block()
    }

    @RequiresPermission(Manifest.permission.MANAGE_SAFETY_CENTER)
    override fun rescan() {
        safetyCenterManager.refreshSafetySources(
            SafetyCenterManager.REFRESH_REASON_RESCAN_BUTTON_CLICK
        )
    }

    @RequiresPermission(Manifest.permission.MANAGE_SAFETY_CENTER)
    override fun pageOpen(safetySourceIds: List<String>) {
        executeIfNotChangingConfigurations {
            if (safetySourceIds.isEmpty()) {
                safetyCenterManager.refreshSafetySources(
                    SafetyCenterManager.REFRESH_REASON_PAGE_OPEN
                )
            } else {
                safetyCenterManager.refreshSafetySources(
                    SafetyCenterManager.REFRESH_REASON_PAGE_OPEN,
                    safetySourceIds,
                )
            }
        }
    }

    override fun clearError() {
        _errorLiveData.value = null
    }

    @RequiresPermission(Manifest.permission.MANAGE_SAFETY_CENTER)
    override fun dismissIssue(issue: SafetyCenterIssue) {
        safetyCenterManager.dismissSafetyCenterIssue(issue.id)
    }

    @RequiresPermission(Manifest.permission.MANAGE_SAFETY_CENTER)
    override fun executeIssueAction(
        issue: SafetyCenterIssue,
        action: SafetyCenterIssue.Action,
        launchTaskId: Int?,
    ) {
        if (Flags.openSafetyCenterApis() && launchTaskId != null) {
            safetyCenterManager.executeSafetyCenterIssueAction(issue.id, action.id, launchTaskId)
        } else {
            safetyCenterManager.executeSafetyCenterIssueAction(issue.id, action.id)
        }
    }

    override fun markIssueResolvedUiCompleted(issueId: IssueId) {
        _safetyCenterLiveData.markIssueResolvedUiCompleted(issueId)
    }

    companion object {
        private const val TAG = "LiveSCViewModel"
    }
}

/** Returns inflight issues pending resolution */
private fun SafetyCenterData.getInFlightIssues(): Map<IssueId, ActionId> =
    allResolvableIssues
        .map { issue ->
            issue.actions
                // UX requirements require skipping resolution UI for issues that do not have a
                // valid successMessage
                .filter { it.isInFlight && !it.successMessage.isNullOrEmpty() }
                .map { issue.id to it.id }
        }
        .flatten()
        .toMap()

private fun SafetyCenterData.isScanning() =
    status.refreshStatus == SafetyCenterStatus.REFRESH_STATUS_FULL_RESCAN_IN_PROGRESS

private fun SafetyCenterData.buildIssueIdSet(): Set<IssueId> =
    allResolvableIssues.map { it.id }.toSet()

private val SafetyCenterData.allResolvableIssues: Sequence<SafetyCenterIssue>
    get() = issues.asSequence() + dismissedIssues.asSequence()

/**
 * A [ViewModelProvider.Factory] for creating instances of [LiveSafetyCenterViewModel].
 *
 * @param app The application context to be provided to the ViewModel.
 */
class LiveSafetyCenterViewModelFactory(private val app: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
        @Suppress("UNCHECKED_CAST")
        return LiveSafetyCenterViewModel(app) as T
    }
}
