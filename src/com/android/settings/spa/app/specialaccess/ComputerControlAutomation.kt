/*
 * Copyright 2025 The Android Open Source Project
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

package com.android.settings.spa.app.specialaccess

import android.app.AppOpsManager
import android.companion.virtual.VirtualDeviceManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.UserHandle
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import com.android.settings.R
import com.android.settingslib.spa.framework.compose.rememberDrawablePainter
import com.android.settingslib.spa.framework.theme.SettingsDimension
import com.android.settingslib.spa.framework.theme.SettingsRadius
import com.android.settingslib.spa.framework.theme.SettingsSpace
import com.android.settingslib.spa.widget.preference.Preference
import com.android.settingslib.spa.widget.preference.PreferenceModel
import com.android.settingslib.spaprivileged.framework.common.appOpsManager
import com.android.settingslib.spaprivileged.model.app.AppOps
import com.android.settingslib.spaprivileged.model.app.AppRecord
import com.android.settingslib.spaprivileged.model.app.PackageManagers
import com.android.settingslib.spaprivileged.model.app.PackageManagers.hasRequestPermission
import com.android.settingslib.spaprivileged.model.app.getOpMode
import com.android.settingslib.spaprivileged.model.app.opModeFlow
import com.android.settingslib.spaprivileged.model.app.rememberAppRepository
import com.android.settingslib.spaprivileged.model.app.userId
import kotlinx.coroutines.flow.Flow

internal const val PACKAGE_NAME = "rt_packageName"
internal const val USER_ID = "rt_userId"

/** Represents an app record for computer control automation, holding its [ApplicationInfo]. */
data class ComputerControlAgentRecord(
    override val app: ApplicationInfo,
    val isDefaultAssistant: Boolean = false,
) : AppRecord

/**
 * The controller for managing the consent for automation.
 *
 * This controller is responsible for getting and setting the app op mode for the
 * [AppOpsManager.OP_COMPUTER_CONTROL] permission.
 *
 * This controller interacts with [VirtualDeviceManager] to get, remove, or clear the list of apps
 * that the agent is allowed to control.
 */
internal class ComputerControlConsentController(
    private val context: Context,
    private val app: ApplicationInfo,
) {

    private val vdm = context.getSystemService(VirtualDeviceManager::class.java)
    private val consentManager = vdm?.computerControlConsentManager
    private val appOpsManager = context.appOpsManager
    private val appOps =
        AppOps(
            op = AppOpsManager.OP_COMPUTER_CONTROL,
            modeForNotAllowed = AppOpsManager.MODE_IGNORED,
        )
    val appOpsModeFlow: Flow<Int> = appOpsManager.opModeFlow(appOps.op, app)
    val appOpsMode: Int
        get() = appOpsManager.getOpMode(appOps.op, app)

    fun setAppOpMode(mode: Int) {
        appOpsManager.setMode(appOps.op, app.uid, app.packageName, mode)
    }

    fun getAutomatablePackages(): Set<String> {
        return consentManager?.getAutomatableAppListForAgent(app.uid, app.packageName)?.toSet()
            ?: emptySet()
    }

    fun clearAutomatablePackages() {
        consentManager?.clearAutomatableAppListForAgent(app.uid, app.packageName)
    }

    fun removeAutomatablePackage(targetPackageName: String) {
        consentManager?.removeAppFromAutomatableAppListForAgent(
            app.uid,
            app.packageName,
            targetPackageName,
        )
    }

    /**
     * Converts a set of package names into a sorted list of [ApplicationInfo] objects for display.
     */
    fun getDisplayApps(packageNames: Set<String>): List<ApplicationInfo> {
        return packageNames
            .mapNotNull { pkgName ->
                PackageManagers.getPackageInfoAsUser(pkgName, app.userId)?.applicationInfo
            }
            .sortedBy { targetApp ->
                context.packageManager.getApplicationLabel(targetApp).toString()
            }
    }
}

fun <T> T.runIfComputerControlEnabled(block: T.() -> T): T {
    if (android.companion.virtualdevice.flags.Flags.computerControlAccess()) {
        return block()
    }
    return this
}

internal fun getAgentsForTarget(
    context: Context,
    targetApp: ApplicationInfo,
): List<ApplicationInfo> {
    val userContext = context.createContextAsUser(UserHandle.of(targetApp.userId), 0)
    val permission = android.Manifest.permission.ACCESS_COMPUTER_CONTROL
    val agents =
        userContext.packageManager.getInstalledApplications(0).filter {
            it.hasRequestPermission(permission)
        }
    return agents.filter { agent ->
        ComputerControlConsentController(userContext, agent)
            .getAutomatablePackages()
            .contains(targetApp.packageName)
    }
}

@Composable
internal fun AssistantLinkedRequirementFooter() {
    val context = LocalContext.current
    val requirementText = stringResource(R.string.computer_control_automation_footer_requirement)
    val clickableText =
        stringResource(R.string.computer_control_automation_default_digital_assistant_settings)
    val annotatedString = buildAnnotatedString {
        append(requirementText)
        append(" ")
        pushLink(
            LinkAnnotation.Clickable(
                tag = "URL",
                linkInteractionListener = {
                    val intent = Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)
                    context.startActivity(intent)
                },
            )
        )
        withStyle(
            style =
                SpanStyle(
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline,
                )
        ) {
            append(clickableText)
        }
        pop()
    }
    Text(
        text = annotatedString,
        style =
            MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            ),
    )
}

@Composable
internal fun AssistantRequirementCard(appLabel: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(SettingsRadius.large3))
                .background(MaterialTheme.colorScheme.surfaceBright)
                .padding(SettingsDimension.paddingLarge)
    ) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier =
                Modifier.padding(SettingsDimension.paddingTiny).size(SettingsDimension.itemIconSize),
        )

        Spacer(modifier = Modifier.height(SettingsSpace.extraSmall2))

        Text(
            text =
                stringResource(
                    R.string.computer_control_automation_banner_to_change_assistant_title,
                    appLabel,
                ),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(SettingsDimension.paddingTiny),
        )

        Spacer(modifier = Modifier.height(SettingsSpace.extraSmall4))

        Text(
            text =
                stringResource(
                    R.string.computer_control_automation_banner_to_change_assistant_text,
                    appLabel,
                ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(SettingsDimension.paddingTiny),
        )

        Spacer(modifier = Modifier.height(SettingsSpace.extraSmall6))

        Button(
            onClick = {
                val intent = Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(SettingsRadius.large3),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        ) {
            Text(
                text =
                    stringResource(
                        R.string.computer_control_automation_banner_to_change_assistant_button_text
                    )
            )
        }
    }
}

/** The icon of the app. */
@Composable
internal fun AppIcon(app: ApplicationInfo) {
    val appRepository = rememberAppRepository()
    Image(
        painter = rememberDrawablePainter(appRepository.produceIcon(app).value),
        contentDescription = appRepository.produceIconContentDescription(app).value,
        modifier = Modifier.size(SettingsDimension.appIconItemSize),
    )
}

/** Allowed automatable app list item with cross button to remove it */
@Composable
internal fun AllowedAutomatableAppListItem(
    app: ApplicationInfo,
    enabled: Boolean,
    shape: Shape,
    onRemove: () -> Unit,
) {
    val appRepository = rememberAppRepository()
    val label = appRepository.produceLabel(app).value
    Box(
        modifier =
            Modifier.fillMaxWidth().clip(shape).background(MaterialTheme.colorScheme.surfaceBright)
    ) {
        Preference(
            object : PreferenceModel {
                override val title = label
                override val summary = { "" }
                override val icon = @Composable { AppIcon(app = app) }
                override val onClick = null
                override val enabled: () -> Boolean
                    get() = { enabled }
            }
        )
        IconButton(
            onClick = onRemove,
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = SettingsSpace.small2),
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove",
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/** Custom shape drawable for app list */
@Composable
fun getAppItemShape(index: Int, total: Int): Shape {
    val large = SettingsRadius.large3
    val small = SettingsRadius.extraSmall2

    return when {
        total == 1 -> RoundedCornerShape(large)
        index == 0 ->
            RoundedCornerShape(
                topStart = large,
                topEnd = large,
                bottomStart = small,
                bottomEnd = small,
            )
        index == total - 1 ->
            RoundedCornerShape(
                topStart = small,
                topEnd = small,
                bottomStart = large,
                bottomEnd = large,
            )
        else -> RoundedCornerShape(small)
    }
}
