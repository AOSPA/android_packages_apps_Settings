/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.spa.app.appinfo

import android.content.pm.ApplicationInfo
import android.os.UserHandle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.android.settings.R
import com.android.settings.Utils
import com.android.settings.overlay.FeatureFactory
import com.android.settingslib.spa.widget.button.ActionButton
import com.android.settingslib.spa.widget.dialog.AlertDialogButton
import com.android.settingslib.spa.widget.dialog.AlertDialogPresenter
import com.android.settingslib.spa.widget.dialog.rememberAlertDialogPresenter
import com.android.settingslib.spaprivileged.framework.common.devicePolicyManager
import com.android.settingslib.spaprivileged.framework.common.userManager
import com.android.settingslib.spaprivileged.model.app.isDisabledUntilUsed
import com.android.settingslib.Utils as SettingsLibUtils

class AppHideButton(
    private val packageInfoPresenter: PackageInfoPresenter,
) {
    private val context = packageInfoPresenter.context
    private val appButtonRepository = AppButtonRepository(context)
    private val resources = context.resources
    private val packageManager = context.packageManager
    private val userManager = context.userManager
    private val devicePolicyManager = context.devicePolicyManager
    private val applicationFeatureProvider =
        FeatureFactory.getFactory(context).getApplicationFeatureProvider(context)

    @Composable
    fun getActionButton(app: ApplicationInfo): ActionButton? {
        return when {
            packageManager.getApplicationHiddenSettingAsUser(
                app.packageName,
                UserHandle.of(packageInfoPresenter.userId)
            ) -> unhideButton(app)

            app.enabled && !app.isDisabledUntilUsed -> {
                hideButton(app)
            }

            else -> return null
        }
    }

    /**
     * Gets whether a package can be hidden.
     */
    private fun ApplicationInfo.canBeHidden(): Boolean = when {
        // Try to prevent the user from bricking their phone by not allowing disabling of apps
        // signed with the system certificate.
        isSignedWithPlatformKey -> false

        // system/vendor resource overlays can never be disabled.
        isResourceOverlay -> false

        packageName in applicationFeatureProvider.keepEnabledPackages -> false

        // Home launcher apps need special handling. In system ones we don't risk downgrading
        // because that can interfere with home-key resolution.
        packageName in appButtonRepository.getHomePackageInfo().homePackages -> false

        SettingsLibUtils.isEssentialPackage(resources, packageManager, packageName) -> false

        // We don't allow disabling DO/PO on *any* users if it's a system app, because
        // "disabling" is actually "downgrade to the system version + disable", and "downgrade"
        // will clear data on all users.
        Utils.isProfileOrDeviceOwner(userManager, devicePolicyManager, packageName) -> false

        appButtonRepository.isDisallowControl(this) -> false

        else -> true
    }

    @Composable
    private fun hideButton(app: ApplicationInfo): ActionButton {
        val dialogPresenter = confirmDialogPresenter(app)
        return ActionButton(
            text = context.getString(R.string.hide),
            imageVector = Icons.Outlined.VisibilityOff,
            enabled = app.canBeHidden(),
        ) {
            // Currently we apply the same device policy for both the uninstallation and disable
            // button.
            if (!appButtonRepository.isUninstallBlockedByAdmin(app)) {
                dialogPresenter.open()
            }
        }
    }

    @Composable
    private fun unhideButton(app: ApplicationInfo): ActionButton {
        val dialogPresenter = confirmDialogPresenter(app)
        return ActionButton(
            text = context.getString(R.string.unhide),
            imageVector = Icons.Outlined.Visibility,
        ) {
            dialogPresenter.open()
        }
    }


    @Composable
    private fun confirmDialogPresenter(app: ApplicationInfo): AlertDialogPresenter {
        val hidden = packageManager.getApplicationHiddenSettingAsUser(
            app.packageName,
            UserHandle.of(packageInfoPresenter.userId)
        )
        return rememberAlertDialogPresenter(
            confirmButton = AlertDialogButton(
                text = if (hidden) stringResource(R.string.unhide_app)
                else stringResource(R.string.hide_app),
                onClick = if (hidden) packageInfoPresenter::unhide else packageInfoPresenter::hide,
            ),
            dismissButton = AlertDialogButton(stringResource(R.string.cancel)),
            text = {
                if (hidden) Text(stringResource(R.string.unhide_app_desc))
                else Text(stringResource(R.string.hide_app_desc))
            },
        )
    }
}

