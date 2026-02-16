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

import android.app.Activity
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_FORWARD_RESULT
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Bundle
import android.util.Log
import com.android.settings.SettingsActivity.EXTRA_FRAGMENT_ARG_KEY
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
import com.android.settingslib.metadata.PreferenceScreenCoordinate
import com.android.settingslib.metadata.PreferenceScreenMetadata
import com.android.settingslib.metadata.PreferenceScreenMetadata.Companion.EXTRA_LAUNCH_SCREEN
import com.android.settingslib.metadata.PreferenceScreenRegistry
import com.android.settingslib.metadata.PreferenceSearchIndexablesProvider
import com.android.settingslib.metadata.ValidatedKeyParameters
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
class SettingsLaunchpadActivity : Activity() {

    companion object {
        const val EXTRA_SCREEN_KEY = "screen_key"
        const val EXTRA_SCREEN_ARGS = "screen_args"
        const val EXTRA_HIGHLIGHT_KEY = "highlight_key"

        private const val TAG = "SettingsLaunchpad"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (isFinishing) return

        if (!checkCallerPermission()) {
            val callingPackage = callingPackage ?: "unknown"
            Log.w(TAG, "Permission check failed for caller: $callingPackage")
            finish()
            return
        }

        processIntentAndLaunch()
        finish()
    }

    private fun processIntentAndLaunch() {
        val screenKey =
            intent.getStringExtra(EXTRA_SCREEN_KEY)
                ?: intent.getStringExtra(EXTRA_FRAGMENT_ARG_KEY)?.let {
                    // update the high light key from the search result intent
                    intent.putExtra(
                        EXTRA_HIGHLIGHT_KEY,
                        PreferenceSearchIndexablesProvider.getHighlightKey(it),
                    )
                    PreferenceSearchIndexablesProvider.getScreenKey(it)
                }

        if (screenKey.isNullOrEmpty()) {
            Log.e(TAG, "Required extra '$EXTRA_SCREEN_KEY' is missing or empty.")
            return
        }

        val screenArgsBundle = intent.getBundleExtra(EXTRA_SCREEN_ARGS)
        val screenCoordinate = createScreenCoordinate(screenKey, screenArgsBundle)

        // Using PreferenceScreenRegistry.create() to get screen metadata
        val screenMetadata =
            PreferenceScreenRegistry.create(this, screenCoordinate)
                ?: run {
                    Log.e(TAG, "Cannot find screen metadata for key: $screenKey")
                    return
                }

        if (screenMetadata is PreferencesApiScreen) {
            val checkScreenFlag = screenMetadata.flag?.check(FlagContext(this)) ?: true
            if (!checkScreenFlag) { // Do not launch the screen if flag is disabled.
                Log.w(TAG, "Screen flag is disabled for key '$screenKey'. Aborting launch.")
                return
            }

            val opContext =
                ApiOperationContext(
                    context = this@SettingsLaunchpadActivity.applicationContext,
                    parameters = screenMetadata.keyParameters ?: ValidatedKeyParameters.EMPTY,
                )

            // Precondition checks are suspend functions. Since this is a trampoline
            // activity that should execute quickly, we use runBlocking. This assumes
            // the precondition checks are fast and won't cause ANRs.
            val screenPreconditionsCheck =
                runBlocking { screenMetadata.screenPreconditions?.check(opContext) } ?: Allowed
            if (
                screenPreconditionsCheck != Allowed
            ) { // Do not launch the screen if preconditions are not met.
                val reason = (screenPreconditionsCheck as Disallowed).getReason(opContext.context)
                Log.w(
                    TAG,
                    "Screen preconditions not met for key '$screenKey' with reason: $reason. Aborting launch.",
                )
                return
            }

            val spaRoute = screenMetadata.getSpaRoute()
            if (!spaRoute.isNullOrEmpty()) {
                startScreen(
                    screenMetadata,
                    { getSpaActivityIntent(spaRoute) },
                    { startSpaActivity(spaRoute) },
                )
                return
            }
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

        launchFragment(fragmentClass.name, screenKey, screenArgsBundle, screenMetadata)
    }

    private fun launchFragment(
        fragmentClass: String,
        key: String,
        args: Bundle?,
        metadata: PreferenceScreenMetadata,
    ) {
        val launchArgs =
            Bundle().apply {
                putString(EXTRA_BINDING_SCREEN_KEY, key)
                putBundle(EXTRA_BINDING_SCREEN_ARGS, args)
                putString(
                    SettingsActivity.EXTRA_FRAGMENT_ARG_KEY,
                    intent.getStringExtra(EXTRA_HIGHLIGHT_KEY),
                )

                // add all values from the launchScreenExtra to this bundle
                val launchScreenExtra = intent.getBundleExtra(EXTRA_LAUNCH_SCREEN)
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
            metadata,
            { launcher.toIntent() },
            { launcher.addFlags(FLAG_ACTIVITY_NEW_TASK).launch() },
        )
    }

    private fun startScreen(
        metadata: PreferenceScreenMetadata,
        intent: () -> Intent,
        launch: () -> Unit,
    ) {
        if (shouldLaunchDeepLinkTrampoline()) {
            val menuKey = resolveMenuKey(metadata)
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

    /** A dummy function to check caller's permission. */
    private fun checkCallerPermission(): Boolean {
        // TODO: Implement real permission check.

        // Get the UID of the calling process.
        // If the caller is the system or the app itself, allow it.

        // check permission
        // check signature
        return true
    }
}
