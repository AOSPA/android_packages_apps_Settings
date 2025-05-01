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
import android.content.Intent
import androidx.preference.Preference
import com.android.settings.applications.AppStoreUtil.getAppStoreLink
import com.android.settings.applications.AppStoreUtil.getInstallerPackageName
import com.android.settings.core.BasePreferenceController
import com.android.settings.supervision.ipc.SupervisionMessengerClient.Companion.SUPERVISION_MESSENGER_SERVICE_BIND_ACTION

/** Controller for the top level Supervision settings Preference item. */
class TopLevelSupervisionPreferenceController(
    private val context: Context,
    private val key: String,
) : BasePreferenceController(context, key) {
    private val supervisionPackage =
        SupervisionHelper.getInstance(context).getSupervisionPackageName()

    private var missingAppStoreLink = false

    private var redirectIntent: Intent? = null

    override fun handlePreferenceTreeClick(preference: Preference?): Boolean {
        if (preference?.key.equals(key) && redirectIntent != null) {
            context.startActivity(redirectIntent)
            return true
        }
        return super.handlePreferenceTreeClick(preference)
    }

    override fun updateState(preference: Preference?) {
        super.updateState(preference)
        if (!hasNecessarySupervisionComponent() && missingAppStoreLink) {
            preference?.isEnabled = false
        }
    }

    override fun getAvailabilityStatus(): Int {
        if (!Flags.enableSupervisionSettingsScreen() || supervisionPackage == null)
            return UNSUPPORTED_ON_DEVICE

        // Try to navigate to app store if supervision app with necessary component is not installed
        if (!hasNecessarySupervisionComponent()) {
            val installerPackageName = getInstallerPackageName(context, supervisionPackage)
            val appStoreLinkIntent =
                installerPackageName?.let {
                    getAppStoreLink(context, installerPackageName, supervisionPackage)
                }
            if (appStoreLinkIntent == null) {
                missingAppStoreLink = true
                return AVAILABLE
            }
            missingAppStoreLink = false
            redirectIntent = appStoreLinkIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        if (hasRedirect()) {
            redirectIntent = Intent(SETTINGS_REDIRECT_ACTION).setPackage(supervisionPackage)
        }

        return AVAILABLE
    }

    private fun hasNecessarySupervisionComponent(): Boolean {
        val intent =
            Intent(SUPERVISION_MESSENGER_SERVICE_BIND_ACTION).setPackage(supervisionPackage)

        return supervisionPackage != null &&
            context.packageManager.queryIntentServices(intent, 0).isNotEmpty()
    }

    private fun hasRedirect(): Boolean {
        val intent = Intent(SETTINGS_REDIRECT_ACTION).setPackage(supervisionPackage)
        return supervisionPackage != null &&
            context.packageManager
                .queryIntentActivitiesAsUser(intent, 0, context.userId)
                .isNotEmpty()
    }

    companion object {
        // Supervision app should declare an intent-filter with this action to redirect the settings
        // navigation target.
        const val SETTINGS_REDIRECT_ACTION = "android.app.supervision.action.VIEW_SETTINGS"
    }
}
