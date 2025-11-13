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
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.IntState
import androidx.compose.runtime.asIntState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
import com.android.settingslib.spa.framework.util.filterItem
import com.android.settingslib.spa.framework.util.mapItem
import com.android.settingslib.spa.widget.preference.ListPreferenceModel
import com.android.settingslib.spa.widget.preference.ListPreferenceOption
import com.android.settingslib.spa.widget.preference.Preference
import com.android.settingslib.spa.widget.preference.PreferenceModel
import com.android.settingslib.spa.widget.preference.RadioPreferences
import com.android.settingslib.spa.widget.ui.AnnotatedText
import com.android.settingslib.spaprivileged.framework.common.appOpsManager
import com.android.settingslib.spaprivileged.model.app.AppListModel
import com.android.settingslib.spaprivileged.model.app.AppOps
import com.android.settingslib.spaprivileged.model.app.AppRecord
import com.android.settingslib.spaprivileged.model.app.PackageManagers
import com.android.settingslib.spaprivileged.model.app.PackageManagers.hasRequestPermission
import com.android.settingslib.spaprivileged.model.app.getOpMode
import com.android.settingslib.spaprivileged.model.app.opModeFlow
import com.android.settingslib.spaprivileged.model.app.toRoute
import com.android.settingslib.spaprivileged.template.app.AppInfoPage
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
    override fun Page(arguments: Bundle?) {
        val model = remember(::ComputerControlAutomationAppListModel)
        val packageName = arguments?.getString(PACKAGE_NAME)!!
        val userId = arguments.getInt(USER_ID)
        AppInfoPage(
            title = stringResource(model.pageTitleResId),
            packageName = packageName,
            userId = userId,
            packageManagers = PackageManagers,
            footerContent = { AnnotatedText(model.footerResId) },
        ) {
            applicationInfo?.let { Content(it, model) }
        }
    }

    /** The content of the app info page. */
    @Composable
    fun Content(app: ApplicationInfo, model: ComputerControlAutomationAppListModel) {
        val controller = rememberContext { ComputerAutomationController(it, app, model.appOps) }
        RadioPreferences(
            model =
                object : ListPreferenceModel {
                    override val title = stringResource(model.switchTitleResId)
                    override val options =
                        listOf(
                            ListPreferenceOption(
                                id = AppOpsManager.MODE_ALLOWED,
                                text = stringResource(model.allowedTitleResId),
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
                    override val selectedId: IntState =
                        controller.modeFlow.collectAsState(controller.mode).asIntState()

                    override val onIdSelected: (id: Int) -> Unit = { controller.setMode(it) }
                }
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
    val allowedTitleResId = R.string.computer_control_automation_always_allowed
    val askTitleResId = R.string.computer_control_automation_ask_every_time
    val deniedTitleResId = R.string.computer_control_automation_dont_allow
    val pageTitleResId = R.string.computer_control_automation_page_title
    val switchTitleResId = R.string.computer_control_automation_switch_title
    val footerResId = R.string.computer_control_automation_footer_description
    val appOps =
        AppOps(
            op = AppOpsManager.OP_COMPUTER_CONTROL,
            modeForNotAllowed = AppOpsManager.MODE_IGNORED,
        )

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
        val controller = ComputerAutomationController(context, record.app, appOps)
        return context.getString(
            when (controller.mode) {
                AppOpsManager.MODE_ALLOWED -> allowedTitleResId
                AppOpsManager.MODE_IGNORED -> deniedTitleResId
                else -> askTitleResId
            }
        )
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
 * The controller for the computer control automation page.
 *
 * This controller is responsible for getting and setting the app op mode for the
 * [AppOpsManager.OP_COMPUTER_CONTROL] permission.
 */
internal class ComputerAutomationController(
    context: Context,
    private val app: ApplicationInfo,
    private val appOps: AppOps,
) {
    private val appOpsManager = context.appOpsManager

    val modeFlow: Flow<Int> = appOpsManager.opModeFlow(appOps.op, app)

    fun setMode(mode: Int) {
        appOpsManager.setMode(appOps.op, app.uid, app.packageName, mode)
    }

    val mode: Int
        get() = appOpsManager.getOpMode(appOps.op, app)
}

fun <T> T.runIfComputerControlEnabled(block: T.() -> T): T {
    if (android.companion.virtualdevice.flags.Flags.computerControlAccess()) {
        return block()
    }
    return this
}
