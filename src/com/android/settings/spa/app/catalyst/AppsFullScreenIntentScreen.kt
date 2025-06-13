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

package com.android.settings.spa.app.catalyst

import android.app.settings.SettingsEnums
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.android.settings.R
import com.android.settings.contract.TAG_DEVICE_STATE_SCREEN
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.flags.Flags
import com.android.settings.spa.SpaBridgeActivity
import com.android.settings.spa.app.catalyst.AppInfoFullScreenIntentScreen.Companion.hasFullScreenPermission
import com.android.settingslib.metadata.PreferenceHierarchy
import com.android.settingslib.metadata.PreferenceHierarchyGenerator
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.asyncPreferenceHierarchy
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.spaprivileged.model.app.AppListRepositoryImpl
import kotlinx.coroutines.CoroutineScope

@ProvidePreferenceScreen(AppsFullScreenIntentAppListScreen.KEY)
open class AppsFullScreenIntentAppListScreen :
    PreferenceScreenMixin, PreferenceHierarchyGenerator<Boolean> {

    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.full_screen_intent_title

    override val highlightMenuKey: Int
        get() = R.string.menu_key_apps

    override fun getMetricsCategory() = SettingsEnums.PAGE_UNKNOWN // TODO: correct page id

    override fun tags(context: Context) = arrayOf(TAG_DEVICE_STATE_SCREEN)

    override fun isFlagEnabled(context: Context) = Flags.deviceState()

    override fun hasCompleteHierarchy() = false

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?): Intent =
        Intent(context, SpaBridgeActivity::class.java)

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {}

    override val defaultType: Boolean
        get() = true // include system apps

    override suspend fun generatePreferenceHierarchy(
        context: Context,
        type: Boolean, // whether to include system apps
    ): PreferenceHierarchy =
        asyncPreferenceHierarchy(context, this) {
            AppListRepositoryImpl(context).loadAndFilterApps(context.userId, type).forEach {
                if (it.hasFullScreenPermission(context)) {
                    val arguments = Bundle(1).apply { putString("app", it.packageName) }
                    +(AppInfoFullScreenIntentScreen.KEY args arguments)
                }
            }
        }

    companion object {
        const val KEY = "device_state_apps_full_screen_intent"
    }
}
