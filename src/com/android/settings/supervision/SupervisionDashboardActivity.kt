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

import android.content.Intent
import android.os.Bundle
import com.android.settings.CatalystSettingsActivity

/**
 * Activity to display the Supervision settings landing page (Settings > Supervision).
 *
 * See [SupervisionDashboardScreen] for details on the page contents.
 */
class SupervisionDashboardActivity :
    CatalystSettingsActivity(
        SupervisionDashboardScreen.KEY,
        SupervisionDashboardFragment::class.java,
    ) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // If the supervision package doesn't have the necessary components, the dashboard can't be
        // directly loaded.
        if (!hasNecessarySupervisionComponent(matchAll = true)) {
            // Check if the app provides any mitigating actions and trigger them if so.
            if (systemSupervisionPackageName != null) {
                val installIntent =
                    Intent(INSTALL_SUPERVISION_APP_ACTION).setPackage(systemSupervisionPackageName)
                if (
                    packageManager
                        .queryIntentActivitiesAsUser(installIntent, 0, userId)
                        .isNotEmpty()
                ) {
                    startActivity(installIntent)
                }
            }
            finish()
            return
        }

        // If the supervision package has the necessary component but not in the enabled state,
        // launch a loading screen while trying to enable it.
        if (!hasNecessarySupervisionComponent()) {
            val loadingActivity = Intent(this, SupervisionDashboardLoadingActivity::class.java)
            startActivity(loadingActivity)
            finish()
            return
        }

        if (shouldRedirectToFullSupervision()) {
            val intent =
                Intent(FULL_SUPERVISION_REDIRECT_ACTION).setPackage(systemSupervisionPackageName)
            startActivity(intent)
            finish()
        }
    }

    private fun shouldRedirectToFullSupervision(): Boolean {
        // The user is deemed to be fully supervised if the supervision role holder is not empty
        if (supervisionRoleHolders.isEmpty() || systemSupervisionPackageName == null) {
            return false
        }

        val intent =
            Intent(FULL_SUPERVISION_REDIRECT_ACTION).setPackage(systemSupervisionPackageName)
        return packageManager.queryIntentActivitiesAsUser(intent, 0, userId).isNotEmpty()
    }

    companion object {
        const val FULL_SUPERVISION_REDIRECT_ACTION = "android.app.supervision.action.VIEW_SETTINGS"
        const val INSTALL_SUPERVISION_APP_ACTION =
            "android.app.supervision.action.INSTALL_SUPERVISION_APP"
    }
}
