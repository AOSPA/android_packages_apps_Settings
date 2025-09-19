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
package com.android.settings.supervision

import android.app.supervision.flags.Flags
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.supervision.ipc.SupervisionMessengerClient
import com.android.settings.supervision.ipc.SupportedApp
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceTitleProvider
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.preference.PreferenceBinding
import com.android.settingslib.supervision.SupervisionLog.TAG
import com.android.settingslib.utils.StringUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Abstract base class for screens displaying supported apps for web content filters. */
abstract class SupervisionWebContentFilterSupportedAppsScreen :
    PreferenceScreenMixin, PreferenceLifecycleProvider, PreferenceTitleProvider, PreferenceBinding {
    protected var supportedApps: List<SupportedApp> = emptyList()
    private var supervisionClient: SupervisionMessengerClient? = null

    override fun getTitle(context: Context): CharSequence? =
        StringUtil.getIcuPluralsString(
            context,
            supportedApps.size,
            R.string.supervision_web_content_filters_switch_summary,
        )

    /** The key used to fetch the list of supported apps from [SupervisionMessengerClient]. */
    abstract val supportedAppsKey: String

    override fun isFlagEnabled(context: Context) = Flags.enableSupervisionSettingsUiUpdates()

    override val indexable
        get() = true

    override val highlightMenuKey: Int
        get() = R.string.menu_key_supervision

    override fun onCreate(context: PreferenceLifecycleContext) {
        supervisionClient =
            supervisionClient ?: SupervisionMessengerClient(context).also { supervisionClient = it }

        context.lifecycleScope.launch {
            val rawSupportedApps =
                withContext(Dispatchers.IO) {
                        supervisionClient?.getSupportedApps(listOf(supportedAppsKey))
                    }
                    ?.get(supportedAppsKey) ?: emptyList()

            supportedApps =
                rawSupportedApps
                    .filter { it.packageName != null }
                    .filter { supportedApp ->
                        val packageName = supportedApp.packageName!!
                        try {
                            context.packageManager.getApplicationInfo(packageName, 0)
                            true
                        } catch (e: PackageManager.NameNotFoundException) {
                            Log.d(
                                TAG,
                                "Package not found for supported app, skipping: $packageName",
                                e,
                            )
                            false
                        } catch (e: Exception) {
                            Log.d(
                                TAG,
                                "Exception thrown for supported app, skipping: $packageName",
                                e,
                            )
                            false
                        }
                    }
            context.notifyPreferenceChange(key)
        }
    }

    override fun onDestroy(context: PreferenceLifecycleContext) {
        supervisionClient?.close()
    }

    override fun createWidget(context: Context): Preference =
        SupervisionWebContentFiltersSupportedAppsEntryPointPreference(context, supportedApps)

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        if (preference is SupervisionWebContentFiltersSupportedAppsEntryPointPreference) {
            preference.updateSupportedApps(supportedApps)
        }
    }

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {}
}
