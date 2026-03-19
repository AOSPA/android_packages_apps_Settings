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

package com.android.settings

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_FORWARD_RESULT
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Process
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.android.settings.SettingsActivity.EXTRA_FRAGMENT_ARG_KEY
import com.android.settings.SettingsLaunchpadActivity.Companion.EXTRA_HIGHLIGHT_KEY
import com.android.settings.activityembedding.ActivityEmbeddingUtils
import com.android.settings.activityembedding.EmbeddedDeepLinkUtils.getTrampolineIntent
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.core.SubSettingLauncher
import com.android.settings.spa.SpaActivity.Companion.getSpaActivityIntent
import com.android.settings.spa.SpaActivity.Companion.startSpaActivity
import com.android.settingslib.core.instrumentation.Instrumentable.METRICS_CATEGORY_UNKNOWN
import com.android.settingslib.metadata.CatalystFlagProviderFactory
import com.android.settingslib.metadata.EXTRA_BINDING_SCREEN_ARGS
import com.android.settingslib.metadata.EXTRA_BINDING_SCREEN_KEY
import com.android.settingslib.metadata.KeyParameters
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceScreenCoordinate
import com.android.settingslib.metadata.PreferenceScreenMetadata
import com.android.settingslib.metadata.PreferenceScreenMetadata.Companion.EXTRA_LAUNCH_SCREEN
import com.android.settingslib.metadata.PreferenceScreenMetadata.Companion.EXTRA_SCREEN_ARGS
import com.android.settingslib.metadata.PreferenceScreenMetadata.Companion.EXTRA_SCREEN_KEY
import com.android.settingslib.metadata.PreferenceScreenRegistry
import com.android.settingslib.metadata.PreferenceSearchIndexablesProvider
import com.android.settingslib.metadata.ValidatedKeyParameters
import com.android.settingslib.metadata.isExposable
import com.android.settingslib.metadata.preferencesapi.ApiOperationContext
import com.android.settingslib.metadata.preferencesapi.FlagContext
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.preconditions.Allowed
import com.android.settingslib.metadata.preferencesapi.preconditions.Disallowed
import com.android.settingslib.metadata.toMap
import kotlinx.coroutines.runBlocking

/**
 * A trampoline Activity that launches a settings screen based on a generic screen key.
 *
 * This activity serves as a secure, generic entry point into the Settings app. Its primary purpose
 * is to decouple external callers from the internal implementation details of Settings, such as
 * Fragment or Activity class names.
 *
 * ### How it Works
 * 1. It receives an Intent with a mandatory [EXTRA_SCREEN_KEY].
 * 2. It uses the [PreferenceScreenRegistry] to resolve this key into screen metadata.
 * 3. It immediately calls `finish()` on itself.
 *
 * ### Usage
 * To launch a screen, build an Intent targeting this activity and include the following extras:
 * - **[EXTRA_SCREEN_KEY] (String, Required):** The unique identifier for the target screen.
 * - **[EXTRA_SCREEN_ARGS] (Bundle, Optional):** Arguments for parameterized screens (e.g., a
 *   package name for an app-specific screen).
 * - **[EXTRA_HIGHLIGHT_KEY] (String, Optional):** The key of a preference to scroll to and
 *   highlight within the target screen.
 */
open class SettingsLaunchpadActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isFinishing) return

        try {
            if (!checkCallerPermission()) {
                val callingPackage = getLaunchedFromPackage() ?: "unknown"
                Log.w(TAG, "Permission check failed for caller: $callingPackage")
                return
            }

            processIntentAndLaunch()
        } finally {
            finish()
        }
    }

    private fun processIntentAndLaunch() {
        val screenKey =
            intent.getStringExtra(EXTRA_SCREEN_KEY)
                ?: intent.getStringExtra(EXTRA_FRAGMENT_ARG_KEY)?.let {
                    // update the high light key from the search result intent
                    val searchHighlightKey = PreferenceSearchIndexablesProvider.getHighlightKey(it)
                    intent.putExtra(EXTRA_HIGHLIGHT_KEY, searchHighlightKey)
                    PreferenceSearchIndexablesProvider.getScreenKey(it)
                }

        if (screenKey.isNullOrEmpty()) {
            Log.e(TAG, "Required extra '$EXTRA_SCREEN_KEY' is missing or empty.")
            return
        }

        val highlightKey = intent.getStringExtra(EXTRA_HIGHLIGHT_KEY)
        var screenArgsBundle = intent.getBundleExtra(EXTRA_SCREEN_ARGS)

        if (screenArgsBundle == null) {
            val itemization = intent.getStringExtra(PreferenceScreenMetadata.EXTRA_ITEMIZATION)
            if (!itemization.isNullOrEmpty()) {
                val schema = PreferenceScreenRegistry.getScreenParametersSchema(screenKey)
                val firstParamName = schema?.getParameters()?.keys?.firstOrNull()

                if (firstParamName != null) {
                    // Map the itemization value to the screen's first formal parameter
                    screenArgsBundle = Bundle().apply { putString(firstParamName, itemization) }
                    Log.d(TAG, "Mapped itemization to '$firstParamName' for screen: $screenKey")
                }
            }
        }

        val screenCoordinate = createScreenCoordinate(screenKey, screenArgsBundle)

        // Using PreferenceScreenRegistry.create() to get screen metadata
        val screenMetadata =
            PreferenceScreenRegistry.create(this, screenCoordinate)
                ?: run {
                    Log.e(TAG, "Cannot find screen metadata for key: $screenKey")
                    return
                }

        if (shouldBlockScreenLaunch(this, screenMetadata)) {
            return
        }

        val menuKey = resolveMenuKey(screenMetadata)

        if (screenMetadata is PreferencesApiScreen) {
            val checkScreenFlag = screenMetadata.flag?.check(FlagContext(this)) ?: true
            if (!checkScreenFlag) { // Do not launch the screen if flag is disabled.
                Log.w(TAG, "Screen flag is disabled for key '$screenKey'. Aborting launch.")
                return
            }

            // Check screen-specific permissions unless the caller holds trusted system-level
            // permissions.
            val launchedUid = getLaunchedFromUid()
            val isTrustedCaller =
                TRUSTED_SCREEN_BYPASS_PERMISSIONS.any { permission ->
                    validatePermission(permission, launchedUid) == PackageManager.PERMISSION_GRANTED
                }

            if (!isTrustedCaller) {
                val screenPermissions = screenMetadata.screenPermissions
                if (screenPermissions != null && !screenPermissions.check(this, -1, launchedUid)) {
                    Log.w(TAG, "Caller does not have required permissions for screen: $screenKey")
                    return
                }
            }

            val opContext =
                ApiOperationContext(
                    context = this@SettingsLaunchpadActivity.applicationContext,
                    parameters = screenMetadata.keyParameters ?: ValidatedKeyParameters.EMPTY,
                )

            try {
                // Precondition checks are suspend functions. Since this is a trampoline
                // activity that should execute quickly, we use runBlocking. This assumes
                // the precondition checks are fast and won't cause ANRs.
                val screenPreconditionsCheck =
                    runBlocking { screenMetadata.screenPreconditions?.check(opContext) } ?: Allowed
                if (
                    screenPreconditionsCheck != Allowed
                ) { // Do not launch the screen if preconditions are not met.
                    val reason =
                        (screenPreconditionsCheck as Disallowed).getReason(opContext.context)
                    Log.w(
                        TAG,
                        "Screen preconditions not met for key '$screenKey' with reason: $reason. Aborting launch.",
                    )
                    return
                }
            } catch (ex: Exception) {
                // this can happen when a client wants to open a package that doesn't exist on device
                Log.e(TAG, "Screen precondition check threw an exception for key: $screenKey", ex)
                return
            }

            val spaRoute = screenMetadata.getSpaRoute()
            if (!spaRoute.isNullOrEmpty()) {
                startScreen(
                    menuKey,
                    { getSpaActivityIntent(spaRoute) },
                    { startSpaActivity(spaRoute) },
                )
                return
            }
        }

        // Does this screenMetadata define a custom launch intent?
        val metadataIntent = screenMetadata.getLaunchIntent(this, null)
        if (metadataIntent != null &&
            metadataIntent.action != PreferenceScreenMetadata.LAUNCH_SETTINGS_PAGES_ACTION
        ) {
            try {
                startActivity(metadataIntent.addFlags(FLAG_ACTIVITY_NEW_TASK))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to launch custom intent for $screenKey", e)
            }
            return
        }

        val fragmentClass =
            try {
                screenMetadata.fragmentClass()
            } catch (e: IllegalStateException) {
                Log.e(TAG, "Invalid screen implementation for key: $screenKey", e)
                null
            }

        if (fragmentClass == null) {
            Log.e(TAG, "Fragment class name is null for key: $screenKey")
            return
        }

        // get the launch screen extra from the screen metadata intent if present
        val launchScreenExtra = metadataIntent?.getBundleExtra(EXTRA_LAUNCH_SCREEN)

        launchFragment(
            fragmentClass.name,
            screenKey,
            screenArgsBundle,
            highlightKey,
            launchScreenExtra,
            menuKey
        )
    }

    private fun launchFragment(
        fragmentClass: String,
        key: String,
        args: Bundle?,
        highlightKey: String?,
        launchScreenExtra: Bundle?,
        menuKey: String?,
    ) {
        val launchArgs =
            Bundle().apply {
                putString(EXTRA_BINDING_SCREEN_KEY, key)
                putBundle(EXTRA_BINDING_SCREEN_ARGS, args)
                putString(EXTRA_FRAGMENT_ARG_KEY, highlightKey)

                // add all values from the launchScreenExtra to this bundle
                launchScreenExtra?.let { putAll(it) }
            }

        val launcher =
            SubSettingLauncher(this)
                .setDestination(fragmentClass)
                .addFlags(FLAG_ACTIVITY_FORWARD_RESULT)
                .setArguments(launchArgs)
                .setSourceMetricsCategory(
                    METRICS_CATEGORY_UNKNOWN
                ) // TODO(b/465855195): set a meaningful metrics category

        startScreen(
            menuKey,
            { launcher.toIntent() },
            { launcher.addFlags(FLAG_ACTIVITY_NEW_TASK).launch() },
        )
    }

    private fun startScreen(
        menuKey: String?,
        intent: () -> Intent,
        launch: () -> Unit,
    ) {
        if (shouldLaunchDeepLinkTrampoline()) {
            val deepLinkIntent =
                getTrampolineIntent(intent(), menuKey).addFlags(FLAG_ACTIVITY_NEW_TASK)
            startActivity(deepLinkIntent)
        } else {
            launch()
        }
    }

    private fun resolveMenuKey(metadata: PreferenceScreenMetadata): String? =
        when (metadata) {
            is PreferencesApiScreen -> metadata.topLevelSettingsCategory.value
            is PreferenceScreenMixin ->
                metadata.highlightMenuKey.takeIf { it != 0 }?.let { getString(it) }

            else -> null
        }

    private fun shouldBlockScreenLaunch(
        context: Context,
        metadata: PreferenceScreenMetadata
    ): Boolean {
        // If the screen is not exposable => block.
        if (!metadata.isExposable(context)) {
            Log.w(TAG, "Screen ${metadata.key} is not exposable. Blocking launch.")
            return true
        }

        // If the screen is not available => block.
        if (metadata is PreferenceAvailabilityProvider && !metadata.isAvailable(context)) {
            Log.w(TAG, "Screen ${metadata.key} is not available. Blocking launch.")
            return true
        }

        return false
    }

    private fun shouldLaunchDeepLinkTrampoline(): Boolean {
        return ActivityEmbeddingUtils.isEmbeddingActivityEnabled(this) &&
                !ActivityEmbeddingUtils.isAlreadyEmbedded(this)
    }

    private fun createScreenCoordinate(
        screenKey: String,
        screenArgsBundle: Bundle?,
    ): PreferenceScreenCoordinate {
        val screenCoordinate =
            if (
                CatalystFlagProviderFactory.catalystUseKeyParameters() &&
                PreferenceScreenRegistry.isParameterized(this, screenKey)
            ) {
                PreferenceScreenCoordinate(
                    screenKey,
                    screenArgsBundle?.let { KeyParameters(it.toMap()) },
                )
            } else {
                PreferenceScreenCoordinate(screenKey, screenArgsBundle)
            }
        return screenCoordinate
    }

    /** A function to check caller's permission. */
    private fun checkCallerPermission(): Boolean {
        val launchedFromPackage = getLaunchedFromPackage()
        val launchedFromUid = getLaunchedFromUid()

        if (launchedFromPackage == null) {
            Log.w(
                TAG,
                "launchedFromPackage is null. " +
                        "Callers must use ActivityOptions.setShareIdentityEnabled(true).",
            )
            return false
        }

        // If the caller is the app itself or a PCC isolated process of the app, allow it.
        if (PccAwareUidComparator.isSameApp(this, Process.myUid(), launchedFromUid)) {
            return true
        }

        return USE_TRAMPOLINE_PERMISSIONS.any { permission ->
            validatePermission(permission, launchedFromUid) == PackageManager.PERMISSION_GRANTED
        }
    }

    /** A helper method to validate permissions. This is made open for testing purposes. */
    @VisibleForTesting
    open fun validatePermission(permission: String, uid: Int): Int {
        return checkPermission(permission, -1, uid)
    }

    /** Returns the package name of the application that launched this activity. */
    @VisibleForTesting
    override fun getLaunchedFromPackage(): String? = super.getLaunchedFromPackage()

    /** Returns the UID of the application that launched this activity. */
    @VisibleForTesting
    override fun getLaunchedFromUid(): Int = super.getLaunchedFromUid()

    companion object {
        const val EXTRA_HIGHLIGHT_KEY = "highlight_key"

        private const val TAG = "SettingsLaunchpad"

        /** Permissions that allow a caller to enter this trampoline activity. */
        private val USE_TRAMPOLINE_PERMISSIONS =
            listOf(
                Manifest.permission.READ_SYSTEM_PREFERENCES,
                Manifest.permission.EXECUTE_APP_FUNCTIONS,
            )

        /** Permissions that allow a caller to bypass screen-specific permission checks. */
        private val TRUSTED_SCREEN_BYPASS_PERMISSIONS =
            listOf(Manifest.permission.EXECUTE_APP_FUNCTIONS)
    }
}
