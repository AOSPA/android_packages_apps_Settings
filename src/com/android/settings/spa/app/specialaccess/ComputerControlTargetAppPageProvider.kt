/*
 * Copyright 2026 The Android Open Source Project
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

import android.app.role.RoleManager
import android.companion.virtual.VirtualDeviceManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.os.UserHandle
import android.view.ViewTreeObserver
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.android.settings.R
import com.android.settingslib.spa.framework.common.SettingsEntryBuilder
import com.android.settingslib.spa.framework.common.SettingsPage
import com.android.settingslib.spa.framework.common.SettingsPageProvider
import com.android.settingslib.spa.framework.compose.navigator
import com.android.settingslib.spa.framework.compose.rememberDrawablePainter
import com.android.settingslib.spa.framework.theme.SettingsDimension
import com.android.settingslib.spa.framework.theme.SettingsSpace
import com.android.settingslib.spa.framework.util.formatString
import com.android.settingslib.spa.widget.preference.Preference
import com.android.settingslib.spa.widget.preference.PreferenceModel
import com.android.settingslib.spa.widget.scaffold.SettingsScaffold
import com.android.settingslib.spaprivileged.model.app.AppEntry
import com.android.settingslib.spaprivileged.model.app.AppListModel
import com.android.settingslib.spaprivileged.model.app.PackageManagers
import com.android.settingslib.spaprivileged.model.app.PackageManagers.hasRequestPermission
import com.android.settingslib.spaprivileged.model.app.rememberAppRepository
import com.android.settingslib.spaprivileged.model.app.toRoute
import com.android.settingslib.spaprivileged.model.app.userId
import com.android.settingslib.spaprivileged.template.app.AppList
import com.android.settingslib.spaprivileged.template.app.AppListConfig
import com.android.settingslib.spaprivileged.template.app.AppListInput
import com.android.settingslib.spaprivileged.template.app.AppListItem
import com.android.settingslib.spaprivileged.template.app.AppListItemModel
import com.android.settingslib.spaprivileged.template.app.AppListState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

/**
 * A page provider for the target app pages for screen automation.
 *
 * This page displays a list of agent apps that can automate the target app.
 */
object ComputerControlTargetAppPageProvider : SettingsPageProvider {
    override val name = "ComputerControlTargetApp"

    override val parameter =
        listOf(
            navArgument(PACKAGE_NAME) { type = NavType.StringType },
            navArgument(USER_ID) { type = NavType.IntType },
        )

    @Composable
    override fun Page(arguments: Bundle?) {
        val packageName = arguments?.getString(PACKAGE_NAME)!!
        val userId = arguments.getInt(USER_ID)
        val packageInfo =
            remember(packageName, userId) {
                PackageManagers.getPackageInfoAsUser(packageName, userId)
            } ?: return
        val targetApp = packageInfo.applicationInfo!!
        val appRepository = rememberAppRepository()
        val app = checkNotNull(packageInfo.applicationInfo)
        val appLabel = appRepository.produceLabel(app).value

        val context = LocalContext.current
        val pageViewModel: ComputerControlTargetAppViewModel = viewModel {
            ComputerControlTargetAppViewModel(context, targetApp)
        }
        val listModel = pageViewModel.model

        // Refresh list on focus gain (in case it changed externally, e.g. in split-screen)
        // TODO (b/485637560): Consider a listener style callback API for app list changes
        val view = LocalView.current
        DisposableEffect(view) {
            val listener =
                ViewTreeObserver.OnWindowFocusChangeListener { hasFocus ->
                    if (hasFocus) {
                        listModel.refresh()
                    }
                }
            view.viewTreeObserver.addOnWindowFocusChangeListener(listener)
            onDispose { view.viewTreeObserver.removeOnWindowFocusChangeListener(listener) }
        }
        val assistantRoleRequirement = remember {
            android.companion.virtualdevice.flags.Flags.computerControlPerAppConsent() &&
                android.companion.virtualdevice.flags.Flags
                    .computerControlRoleAssistantRequirement()
        }
        val hasItems by listModel.hasItemsFlow.collectAsState()

        val header =
            @Composable {
                if (hasItems) {
                    Column(
                        modifier =
                            Modifier.fillMaxWidth()
                                .padding(
                                    horizontal = SettingsSpace.extraSmall4,
                                    vertical = SettingsSpace.small1,
                                ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Image(
                            painter = rememberDrawablePainter(appRepository.produceIcon(app).value),
                            contentDescription =
                                appRepository.produceIconContentDescription(app).value,
                            modifier = Modifier.size(SettingsDimension.itemIconContainerSize),
                        )
                        Spacer(Modifier.height(SettingsSpace.small1))
                        Text(
                            text = appLabel,
                            style = MaterialTheme.typography.titleLargeEmphasized,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text =
                                stringResource(
                                    R.string.computer_control_automation_target_list_header,
                                    appLabel,
                                ),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = SettingsSpace.extraSmall2),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

        val footer =
            @Composable {
                if (hasItems && assistantRoleRequirement) {
                    Column(Modifier.padding(SettingsDimension.paddingSmall)) {
                        Spacer(modifier = Modifier.height(SettingsSpace.extraSmall4))
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            modifier = Modifier.size(SettingsDimension.itemIconSize),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(SettingsSpace.extraSmall4))
                        AssistantLinkedRequirementFooter()
                    }
                }
            }

        SettingsScaffold(title = stringResource(R.string.computer_control_automation_page_title)) {
            paddingValues ->
            Box(Modifier.padding(top = paddingValues.calculateTopPadding()).fillMaxSize()) {
                AppListInput(
                        config =
                            AppListConfig(
                                userIds = listOf(userId),
                                showInstantApps = false,
                                matchAnyUserForAdmin = false,
                            ),
                        listModel = listModel,
                        state = AppListState(showSystem = { false }, searchQuery = { "" }),
                        header = header,
                        footer = footer,
                        bottomPadding = paddingValues.calculateBottomPadding(),
                    )
                    .AppList()
            }
        }
    }

    /** A preference entry for the target app's app info page. */
    @Composable
    fun InfoPageEntryItem(app: ApplicationInfo) {
        val context = LocalContext.current
        var reloadState by remember { mutableIntStateOf(0) }
        val view = LocalView.current
        // Refresh list on focus gain (in case it changed externally, e.g. in split-screen)
        // TODO (b/485637560): Consider a listener style callback API for app list changes
        DisposableEffect(view) {
            val listener =
                ViewTreeObserver.OnWindowFocusChangeListener { hasFocus ->
                    if (hasFocus) {
                        reloadState++
                    }
                }
            view.viewTreeObserver.addOnWindowFocusChangeListener(listener)
            onDispose { view.viewTreeObserver.removeOnWindowFocusChangeListener(listener) }
        }

        val vdm = remember { context.getSystemService(VirtualDeviceManager::class.java) }
        val agents = remember(app, reloadState) { getAgentsForTarget(context, app) }
        val isAlwaysAllowedByAnyAgent = agents.isNotEmpty()
        val isTargetAllowed =
            isAlwaysAllowedByAnyAgent ||
                remember(app) {
                    vdm?.isPackageTargetableForComputerControlAutomation(
                        app.packageName,
                        app.userId,
                    ) == true
                }
        if (!isTargetAllowed) {
            return
        }
        val onNavigate = if (isAlwaysAllowedByAnyAgent) navigator(app) else null
        val summary =
            if (isAlwaysAllowedByAnyAgent) {
                context.formatString(
                    R.string.computer_control_automation_agents_allowed,
                    "count" to agents.size,
                )
            } else {
                stringResource(R.string.computer_control_automation_ask_every_time)
            }

        Preference(
            object : PreferenceModel {
                override val title = stringResource(R.string.computer_control_automation_page_title)
                override val summary = { summary }
                override val enabled = { isAlwaysAllowedByAnyAgent }
                override val onClick = onNavigate
            }
        )
    }

    @Composable
    private fun navigator(app: ApplicationInfo) = navigator(route = "$name/${app.toRoute()}")

    override fun buildEntry(arguments: Bundle?) =
        listOf(
            SettingsEntryBuilder.create(
                    "ComputerControlTargetApp",
                    SettingsPage.create(name = name, parameter = parameter, arguments = arguments),
                )
                .build()
        )
}

/**
 * The app list model for the computer control target app page.
 *
 * This model is responsible for loading the list of agent apps that can automate the target app.
 */
class ComputerControlTargetAppListModel(
    private val context: Context,
    private val targetApp: ApplicationInfo,
) : AppListModel<ComputerControlAgentRecord> {
    val permission = android.Manifest.permission.ACCESS_COMPUTER_CONTROL
    private val roleManager = context.getSystemService(RoleManager::class.java)!!
    val assistantRoleRequirement =
        android.companion.virtualdevice.flags.Flags.computerControlRoleAssistantRequirement()
    val hasItemsFlow = MutableStateFlow(false)
    private val refreshFlow = MutableStateFlow(0)

    fun refresh() {
        refreshFlow.update { it + 1 }
    }

    override fun transform(
        userIdFlow: Flow<Int>,
        appListFlow: Flow<List<ApplicationInfo>>,
    ): Flow<List<ComputerControlAgentRecord>> =
        combine(userIdFlow, appListFlow, refreshFlow) { userId, appList, _ ->
                val defaultAssistantPackage =
                    if (assistantRoleRequirement) {
                        roleManager.getRoleHoldersAsUser(
                            RoleManager.ROLE_ASSISTANT,
                            UserHandle.of(userId),
                        )
                    } else {
                        emptyList()
                    }
                appList
                    .filter { agent ->
                        agent.hasRequestPermission(permission) &&
                            ComputerControlConsentController(context, agent)
                                .getAutomatablePackages()
                                .contains(targetApp.packageName)
                    }
                    .map {
                        ComputerControlAgentRecord(
                            app = it,
                            isDefaultAssistant = defaultAssistantPackage.contains(it.packageName),
                        )
                    }
            }
            .onEach { list -> hasItemsFlow.value = list.isNotEmpty() }

    override fun getComparator(option: Int): Comparator<AppEntry<ComputerControlAgentRecord>> =
        compareByDescending<AppEntry<ComputerControlAgentRecord>> { it.record.isDefaultAssistant }
            .then(super.getComparator(option))

    @Composable
    override fun getSummary(option: Int, record: ComputerControlAgentRecord): (() -> CharSequence) {
        val summary =
            if (record.isDefaultAssistant) {
                stringResource(R.string.computer_control_automation_default_digital_assistant)
            } else {
                ""
            }
        return { summary }
    }

    @Composable
    override fun AppListItemModel<ComputerControlAgentRecord>.AppItem() {
        AppListItem(onClick = ComputerControlAutomationAppListProvider.navigator(app = record.app))
    }
}

class ComputerControlTargetAppViewModel(context: Context, targetApp: ApplicationInfo) :
    ViewModel() {
    val model = ComputerControlTargetAppListModel(context, targetApp)
}
