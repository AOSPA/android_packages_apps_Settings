/*
 * Copyright (C) 2026 The Android Open Source Project
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

package com.android.settings.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.android.settings.R
import com.android.settingslib.spa.widget.dialog.AlertDialogButton
import com.android.settingslib.spa.widget.dialog.AlertDialogPresenter
import com.android.settingslib.spa.widget.dialog.rememberAlertDialogPresenter

/** Utilities for HSU (Headless System User) apps. */
object HsuUtils {
    /**
     * Checks if the given app is a Headless System User (HSU) app.
     *
     * HSU apps run as the system user (User 0) on devices where the system user is headless. These
     * apps are distinct from the current user's apps.
     */
    @JvmStatic
    fun isHsuApp(context: Context, app: ApplicationInfo): Boolean {
        if (app.uid == Process.INVALID_UID) return false
        return UserManager.isHeadlessSystemUserMode() &&
            UserHandle.getUserId(app.uid) == UserHandle.USER_SYSTEM
    }

    /** Checks if the current user is an admin. */
    @JvmStatic
    fun isAdmin(context: Context): Boolean {
        val userManager = context.getSystemService(UserManager::class.java)!!
        return userManager.isAdminUser
    }

    /**
     * Checks if the current user can control the given HSU app. Returns true if it's NOT an HSU
     * app, or if it IS an HSU app and the user is an Admin.
     *
     * Non-admin users are restricted from modifying HSU apps to prevent system-wide impact.
     */
    @JvmStatic
    fun canControlHsuApp(context: Context, app: ApplicationInfo): Boolean {
        if (!isHsuApp(context, app)) return true
        return isAdmin(context)
    }

    /**
     * Creates a presenter for the warning dialog shown when an admin attempts to modify an HSU app.
     *
     * This dialog warns the admin that changes will affect all users on the device.
     */
    @Composable
    fun rememberHsuAppWarningDialogPresenter(onConfirm: () -> Unit): AlertDialogPresenter {
        return rememberAlertDialogPresenter(
            confirmButton =
                AlertDialogButton(text = stringResource(R.string.okay), onClick = onConfirm),
            dismissButton = AlertDialogButton(stringResource(R.string.cancel)),
            title = stringResource(R.string.hsu_app_warning_dialog_title),
            text = { Text(stringResource(R.string.hsu_app_warning_dialog_message)) },
        )
    }
}
