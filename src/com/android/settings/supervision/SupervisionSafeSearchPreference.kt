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

import android.app.Activity
import android.app.settings.SettingsEnums
import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.annotation.VisibleForTesting
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.metrics.PreferenceActionMetricsProvider
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.BooleanValuePreference
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.preference.BooleanValuePreferenceBinding
import com.android.settingslib.supervision.SupervisionIntentProvider
import com.android.settingslib.widget.SelectorWithWidgetPreference

/** Base class of web content filters Search filter preferences. */
sealed class SupervisionSafeSearchPreference(
    private val dataStore: SupervisionSafeSearchDataStore
) :
    BooleanValuePreference,
    BooleanValuePreferenceBinding,
    PreferenceActionMetricsProvider,
    SelectorWithWidgetPreference.OnClickListener,
    PreferenceLifecycleProvider {

    private lateinit var lifeCycleContext: PreferenceLifecycleContext

    private lateinit var supervisionCredentialLauncher: ActivityResultLauncher<Intent>

    override fun storage(context: Context) = dataStore

    override fun getReadPermissions(context: Context) = SettingsSecureStore.getReadPermissions()

    override fun getWritePermissions(context: Context) = SettingsSecureStore.getWritePermissions()

    override fun getReadPermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override fun getWritePermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.DISALLOW

    override fun createWidget(context: Context) = SelectorWithWidgetPreference(context)

    override fun onCreate(context: PreferenceLifecycleContext) {
        lifeCycleContext = context
        supervisionCredentialLauncher =
            context.registerForActivityResult(StartActivityForResult(), ::onConfirmCredentials)
    }

    override fun onRadioButtonClicked(emiter: SelectorWithWidgetPreference) {
        val intent =
            SupervisionIntentProvider.getConfirmSupervisionCredentialsIntent(lifeCycleContext)
        if (intent != null) {
            supervisionCredentialLauncher.launch(intent)
        }
    }

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        (preference as SelectorWithWidgetPreference).setOnClickListener(this)
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun onConfirmCredentials(result: ActivityResult) {
        if (result.resultCode == Activity.RESULT_OK) {
            // Update checked state with dataStore also works but it will bypass metrics logging
            lifeCycleContext.requirePreference<SelectorWithWidgetPreference>(key).isChecked = true
        }
    }
}

/** The SafeSearch filter on preference. */
class SupervisionSearchFilterOnPreference(dataStore: SupervisionSafeSearchDataStore) :
    SupervisionSafeSearchPreference(dataStore) {

    override val key
        get() = KEY

    override val title
        get() = R.string.supervision_web_content_filters_search_filter_on_title

    override val summary
        get() = R.string.supervision_web_content_filters_search_filter_on_summary

    override val preferenceActionMetrics: Int
        get() = SettingsEnums.ACTION_SUPERVISION_SEARCH_FILTER_ON

    companion object {
        const val KEY = "web_content_filters_search_filter_on"
    }
}

/** The SafeSearch filter off preference. */
class SupervisionSearchFilterOffPreference(dataStore: SupervisionSafeSearchDataStore) :
    SupervisionSafeSearchPreference(dataStore) {

    override val key
        get() = KEY

    override val title
        get() = R.string.supervision_web_content_filters_search_filter_off_title

    override val preferenceActionMetrics: Int
        get() = SettingsEnums.ACTION_SUPERVISION_SEARCH_FILTER_OFF

    companion object {
        const val KEY = "web_content_filters_search_filter_off"
    }
}
