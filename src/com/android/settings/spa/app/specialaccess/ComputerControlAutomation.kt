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

import android.Manifest
import android.app.AppOpsManager
import android.companion.virtual.VirtualDeviceManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.view.ViewTreeObserver
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.android.settings.R
import com.android.settings.spa.app.specialaccess.ComputerControlAppInfoPageProvider.COMPUTER_CONTROL_APP_INFO_PAGE
import com.android.settingslib.spa.framework.common.SettingsEntry
import com.android.settingslib.spa.framework.common.SettingsEntryBuilder
import com.android.settingslib.spa.framework.common.SettingsPage
import com.android.settingslib.spa.framework.common.SettingsPageProvider
import com.android.settingslib.spa.framework.compose.navigator
import com.android.settingslib.spa.framework.compose.rememberContext
import com.android.settingslib.spa.framework.compose.rememberDrawablePainter
import com.android.settingslib.spa.framework.theme.SettingsDimension
import com.android.settingslib.spa.framework.theme.SettingsRadius
import com.android.settingslib.spa.framework.theme.SettingsSpace
import com.android.settingslib.spa.framework.util.filterItem
import com.android.settingslib.spa.framework.util.mapItem
import com.android.settingslib.spa.widget.preference.ListPreferenceOption
import com.android.settingslib.spa.widget.preference.MainSwitchPreference
import com.android.settingslib.spa.widget.preference.Preference
import com.android.settingslib.spa.widget.preference.PreferenceModel
import com.android.settingslib.spa.widget.preference.Radio2
import com.android.settingslib.spa.widget.preference.SwitchPreferenceModel
import com.android.settingslib.spa.widget.scaffold.RegularScaffold
import com.android.settingslib.spa.widget.ui.Category
import com.android.settingslib.spa.widget.ui.Footer
import com.android.settingslib.spaprivileged.framework.common.appOpsManager
import com.android.settingslib.spaprivileged.model.app.AppListModel
import com.android.settingslib.spaprivileged.model.app.AppOps
import com.android.settingslib.spaprivileged.model.app.AppRecord
import com.android.settingslib.spaprivileged.model.app.PackageManagers
import com.android.settingslib.spaprivileged.model.app.PackageManagers.hasRequestPermission
import com.android.settingslib.spaprivileged.model.app.getOpMode
import com.android.settingslib.spaprivileged.model.app.opModeFlow
import com.android.settingslib.spaprivileged.model.app.rememberAppRepository
import com.android.settingslib.spaprivileged.model.app.toRoute
import com.android.settingslib.spaprivileged.model.app.userId
import com.android.settingslib.spaprivileged.template.app.AppList
import com.android.settingslib.spaprivileged.template.app.AppListInput
import com.android.settingslib.spaprivileged.template.app.AppListItem
import com.android.settingslib.spaprivileged.template.app.AppListItemModel
import com.android.settingslib.spaprivileged.template.app.AppListPage
import kotlinx.coroutines.flow.Flow

// Although this class is very similar to TogglePermissionAppListProvider, the AppInfo
// page actually provides a 3 states radio button group which is only specific to this AppOp
// control.

/** Represents an app record for computer control automation, holding its [ApplicationInfo]. */
data class ComputerControlAppRecord(override val app: ApplicationInfo) : AppRecord

private const val PACKAGE_NAME = "rt_packageName"
private const val USER_ID = "rt_userId"

/**
 * A page provider for the special access list of apps that can control the computer.
 *
 * This page displays a list of applications that have requested the
 * [Manifest.permission.ACCESS_COMPUTER_CONTROL] permission.
 */
object ComputerControlAutomationAppListProvider : SettingsPageProvider {

    private const val PAGE_NAME = "ComputerControlPermissionPage"

    private val PAGE_PARAMETER =
        listOf(
            navArgument(PACKAGE_NAME) { type = NavType.StringType },
            navArgument(USER_ID) { type = NavType.IntType },
        )

    override val name: String = PAGE_NAME

    @Composable
    override fun Page(arguments: Bundle?) {
        ComputerControlAutomationModeAppList()
    }

    /** Displays the list of apps that can control the computer. */
    @Composable
    fun ComputerControlAutomationModeAppList(
        appList: @Composable AppListInput<ComputerControlAppRecord>.() -> Unit = { AppList() }
    ) {
        val model = remember(::ComputerControlAutomationAppListModel)
        AppListPage(
            title = stringResource(model.pageTitleResId),
            listModel = model,
            appList = appList,
        )
    }

    /** A preference entry for the app info page. */
    @Composable
    fun InfoPageEntryItem(app: ApplicationInfo) {
        val model = remember(::ComputerControlAutomationAppListModel)
        if (!app.hasRequestPermission(model.permission)) {
            return
        }
        val context = LocalContext.current
        Preference(
            object : PreferenceModel {
                override val title = stringResource(model.pageTitleResId)
                override val summary: () -> CharSequence
                    get() = { model.getSummary(context, ComputerControlAppRecord(app)) }

                override val onClick = navigator(app)
            }
        )
    }

    /**
     * Gets the route prefix to this page.
     *
     * Expose route prefix to enable enter from non-SPA pages.
     */
    fun getAppInfoRoutePrefix() = PAGE_NAME

    @Composable
    fun navigator(app: ApplicationInfo) =
        navigator(route = "$COMPUTER_CONTROL_APP_INFO_PAGE/${app.toRoute()}")

    fun buildPageData(): SettingsPage {
        return SettingsPage.create(
            name = COMPUTER_CONTROL_APP_INFO_PAGE,
            parameter = PAGE_PARAMETER,
        )
    }

    override fun buildEntry(arguments: Bundle?): List<SettingsEntry> {
        val appListPage = SettingsPage.create(name, parameter = parameter, arguments = arguments)
        val appInfoPage = buildPageData()
        return listOf(
            SettingsEntryBuilder.createLinkFrom("AppList", appListPage)
                .setLink(toPage = appInfoPage)
                .build()
        )
    }

    fun buildInjectEntry(
        listModelSupplier: (Context) -> ComputerControlAutomationAppListModel
    ): SettingsEntryBuilder {
        val appListPage =
            SettingsPage.create(name = PAGE_NAME, parameter = listOf(), arguments = bundleOf())
        return SettingsEntryBuilder.createInject(owner = appListPage).setUiLayoutFn {
            val listModel = rememberContext(listModelSupplier)
            Preference(
                object : PreferenceModel {
                    override val title = stringResource(listModel.pageTitleResId)
                    override val onClick = navigator(route = PAGE_NAME)
                }
            )
        }
    }

    fun buildAppListInjectEntry(): SettingsEntryBuilder {
        return buildInjectEntry { ComputerControlAutomationAppListModel() }
    }

    fun getAppListRoute(): String = PAGE_NAME
}

/**
 * A page provider for the app info page of an app that can control the computer.
 *
 * This page displays the permission switch for an app that has requested the
 * [Manifest.permission.ACCESS_COMPUTER_CONTROL] permission.
 */
object ComputerControlAppInfoPageProvider : SettingsPageProvider {

    const val COMPUTER_CONTROL_APP_INFO_PAGE = "ComputerControlAppInfoPage"

    private val PAGE_PARAMETER =
        listOf(
            navArgument(PACKAGE_NAME) { type = NavType.StringType },
            navArgument(USER_ID) { type = NavType.IntType },
        )

    override val name = COMPUTER_CONTROL_APP_INFO_PAGE

    override val parameter = PAGE_PARAMETER

    override fun buildEntry(arguments: Bundle?): List<SettingsEntry> {
        val owner = SettingsPage.create(name, parameter = parameter, arguments = arguments)
        return listOf(SettingsEntryBuilder.create("AllowControl", owner).build())
    }

    @Composable
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    override fun Page(arguments: Bundle?) {
        val model = remember(::ComputerControlAutomationAppListModel)
        val packageName = arguments?.getString(PACKAGE_NAME)!!
        val userId = arguments.getInt(USER_ID)
        val packageInfo =
            remember(packageName, userId) {
                PackageManagers.getPackageInfoAsUser(packageName, userId)
            } ?: return
        val appRepository = rememberAppRepository()
        val app = checkNotNull(packageInfo.applicationInfo)
        val title = appRepository.produceLabel(app).value
        // Custom composable layout for the app info page to support centered app description
        // without any app version information.
        RegularScaffold(title = stringResource(model.pageTitleResId)) {
            Column(
                modifier =
                    Modifier.fillMaxWidth()
                        .padding(
                            horizontal = SettingsSpace.small4,
                            vertical = SettingsSpace.small1,
                        ),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painter = rememberDrawablePainter(appRepository.produceIcon(app).value),
                    contentDescription = appRepository.produceIconContentDescription(app).value,
                    modifier = Modifier.size(SettingsDimension.itemIconContainerSize),
                )
                Spacer(Modifier.height(SettingsSpace.small1))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLargeEmphasized,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(model.subHeadingResId, title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier =
                        Modifier.padding(
                            vertical = SettingsSpace.extraSmall2,
                            horizontal = SettingsSpace.small4,
                        ),
                    textAlign = TextAlign.Center,
                )
            }
            packageInfo.applicationInfo?.let { Content(it, model) }
            Footer {
                Text(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    text = stringResource(model.footerResId, title),
                )
            }
        }
    }

    /** The content of the app info page. */
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Composable
    fun Content(app: ApplicationInfo, model: ComputerControlAutomationAppListModel) {
        val appRepository = rememberAppRepository()
        val appLabel = appRepository.produceLabel(app).value
        val consentController = rememberContext { ComputerControlConsentController(it, app) }
        val selectedId by
            consentController.appOpsModeFlow.collectAsState(consentController.appOpsMode)

        if (android.companion.virtualdevice.flags.Flags.computerControlPerAppConsent()) {
            val isChecked = selectedId == AppOpsManager.MODE_ALLOWED
            var automatablePackages by
                remember(app.uid, app.packageName) {
                    mutableStateOf(consentController.getAutomatablePackages())
                }
            // Refresh list on focus gain (in case it changed externally, e.g. in split-screen)
            // TODO (b/485637560): Consider a listener style callback API for app list changes
            val view = LocalView.current
            DisposableEffect(view) {
                val listener =
                    ViewTreeObserver.OnWindowFocusChangeListener { hasFocus ->
                        if (hasFocus) {
                            automatablePackages = consentController.getAutomatablePackages()
                        }
                    }
                view.viewTreeObserver.addOnWindowFocusChangeListener(listener)
                onDispose { view.viewTreeObserver.removeOnWindowFocusChangeListener(listener) }
            }
            val displayApps =
                remember(automatablePackages) {
                    consentController.getDisplayApps(automatablePackages)
                }

            MainSwitchPreference(
                object : SwitchPreferenceModel {
                    override val title =
                        stringResource(R.string.computer_control_automation_toggle_title)
                    override val checked = { isChecked }
                    override val onCheckedChange = { newChecked: Boolean ->
                        if (!newChecked) {
                            consentController.clearAutomatablePackages()
                            automatablePackages = emptySet()
                        }
                        consentController.setAppOpMode(
                            if (newChecked) AppOpsManager.MODE_ALLOWED
                            else AppOpsManager.MODE_DEFAULT
                        )
                    }
                }
            )

            Text(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                text =
                    stringResource(
                        R.string.computer_control_automation_automatable_app_list_header_summary,
                        appLabel,
                    ),
                modifier = Modifier.padding(SettingsDimension.itemPadding),
            )

            if (isChecked && displayApps.isNotEmpty()) {
                Category() {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        displayApps.forEachIndexed { index, targetApp ->
                            AllowedAppItem(
                                app = targetApp,
                                shape = getAppItemShape(index, displayApps.size),
                            ) {
                                consentController.removeAutomatablePackage(targetApp.packageName)
                                automatablePackages = automatablePackages - targetApp.packageName
                            }

                            if (index < displayApps.size - 1) {
                                Spacer(modifier = Modifier.height(2.dp))
                            }
                        }
                    }
                }
            } else {
                Text(
                    text =
                        stringResource(
                            R.string.computer_control_automation_automatable_app_list_header_no_apps
                        ),
                    modifier = Modifier.padding(SettingsDimension.itemPadding),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLargeEmphasized,
                )
            }
        } else {
            val options =
                listOf(
                    ListPreferenceOption(
                        id = AppOpsManager.MODE_ALLOWED,
                        text = stringResource(model.alwaysAllowedTitleResId),
                    ),
                    ListPreferenceOption(
                        id = AppOpsManager.MODE_DEFAULT,
                        text = stringResource(model.askTitleResId),
                    ),
                    ListPreferenceOption(
                        id = AppOpsManager.MODE_IGNORED,
                        text = stringResource(model.deniedTitleResId),
                    ),
                )
            Category(modifier = Modifier.selectableGroup()) {
                for (option in options) {
                    Radio2(option, selectedId, enabled = true) {
                        consentController.setAppOpMode(it)
                    }
                }
            }
        }
    }

    @Composable
    fun AllowedAppItem(app: ApplicationInfo, shape: Shape, onRemove: () -> Unit) {
        val appRepository = rememberAppRepository()
        val label = appRepository.produceLabel(app).value
        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
        ) {
            Preference(
                object : PreferenceModel {
                    override val title = label
                    override val summary = { "" }
                    override val icon = @Composable { AppIcon(app = app) }
                    override val onClick = null
                }
            )
            IconButton(
                onClick = onRemove,
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = SettingsSpace.small2),
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    @Composable
    private fun getAppItemShape(index: Int, total: Int): Shape {
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

    /** The icon of the app. */
    @Composable
    fun AppIcon(app: ApplicationInfo) {
        val appRepository = rememberAppRepository()
        Image(
            painter = rememberDrawablePainter(appRepository.produceIcon(app).value),
            contentDescription = appRepository.produceIconContentDescription(app).value,
            modifier = Modifier.size(SettingsDimension.appIconItemSize),
        )
    }
}

/**
 * The app list model for the computer control automation page.
 *
 * This model is responsible for loading the list of apps that have requested the
 * [Manifest.permission.ACCESS_COMPUTER_CONTROL] permission.
 */
class ComputerControlAutomationAppListModel : AppListModel<ComputerControlAppRecord> {
    val permission = Manifest.permission.ACCESS_COMPUTER_CONTROL
    val alwaysAllowedTitleResId = R.string.computer_control_automation_always_allowed
    val askTitleResId = R.string.computer_control_automation_ask_every_time
    val deniedTitleResId = R.string.computer_control_automation_dont_allow
    val pageTitleResId = R.string.computer_control_automation_page_title
    val subHeadingResId =
        if (android.companion.virtualdevice.flags.Flags.computerControlPerAppConsent()) {
            R.string.computer_control_automation_sub_heading_flag_per_app_consent
        } else {
            R.string.computer_control_automation_sub_heading
        }
    val footerResId = R.string.computer_control_automation_footer_summary

    @Composable
    override fun AppListItemModel<ComputerControlAppRecord>.AppItem() {
        AppListItem(onClick = ComputerControlAutomationAppListProvider.navigator(app = record.app))
    }

    @Composable
    override fun getSummary(option: Int, record: ComputerControlAppRecord): (() -> CharSequence) {
        val context = LocalContext.current
        return { getSummary(context, record) }
    }

    fun getSummary(context: Context, record: ComputerControlAppRecord): String {
        val controller = ComputerControlConsentController(context, record.app)
        if (android.companion.virtualdevice.flags.Flags.computerControlPerAppConsent()) {
            return context.getString(
                when (controller.appOpsMode) {
                    AppOpsManager.MODE_ALLOWED -> R.string.computer_control_automation_allowed
                    else -> R.string.computer_control_automation_not_allowed
                }
            )
        } else {
            return context.getString(
                when (controller.appOpsMode) {
                    AppOpsManager.MODE_ALLOWED -> alwaysAllowedTitleResId
                    AppOpsManager.MODE_IGNORED -> deniedTitleResId
                    else -> askTitleResId
                }
            )
        }
    }

    override fun transform(
        userIdFlow: Flow<Int>,
        appListFlow: Flow<List<ApplicationInfo>>,
    ): Flow<List<ComputerControlAppRecord>> =
        appListFlow
            .filterItem { it.hasRequestPermission(permission) }
            .mapItem(::ComputerControlAppRecord)
}

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
