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

package com.android.settings.backup

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.UserHandle
import android.os.UserManager
import androidx.preference.Preference
import com.android.settings.Settings.AccountsAndBackupDashboardActivity
import com.android.settings.SettingsLaunchpadActivity
import com.android.settings.core.BasePreferenceController
import com.android.settings.dashboard.profileselector.ProfileSelectDialog
import com.android.settings.dashboard.profileselector.UserAdapter
import com.android.settings.flags.Flags
import java.lang.ref.WeakReference

class TopLevelAccountsAndBackupEntryPreferenceController(
    context: Context,
    key: String,
) : BasePreferenceController(context, key) {

    private var profileSelectDialog: WeakReference<Dialog> = WeakReference(null)

    override fun getAvailabilityStatus(): Int {
        return if (Flags.enableAccountsAndBackupScreen()) AVAILABLE else CONDITIONALLY_UNAVAILABLE
    }

    override fun handlePreferenceTreeClick(preference: Preference): Boolean {
        if (preference.key == preferenceKey) {
            // Manually create the Intent to launch the correct Catalyst activity.
            val intent =
                Intent(mContext, AccountsAndBackupDashboardActivity::class.java).apply {
                    putExtra(
                        SettingsLaunchpadActivity.EXTRA_SCREEN_KEY,
                        AccountsAndBackupScreen.KEY,
                    )
                }

            val userHandles = UserManager.get(mContext).enabledProfiles
            if (userHandles.size <= 1) {
                mContext.startActivityAsUser(intent, userHandles.get(UserHandle.myUserId())!!)
                return true
            }

            val clickListener =
                UserAdapter.OnClickListener { position: Int ->
                    val selectedUser = userHandles.get(position)
                    mContext.startActivityAsUser(intent, selectedUser)
                    profileSelectDialog.get()?.dismiss()
                }

            val dialog = ProfileSelectDialog.createDialog(mContext, userHandles, clickListener)
            profileSelectDialog = WeakReference(dialog)
            dialog.show()

            return true
        }

        return super.handlePreferenceTreeClick(preference)
    }
}
