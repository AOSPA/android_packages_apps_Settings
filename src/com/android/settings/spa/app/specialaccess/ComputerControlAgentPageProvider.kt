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

import android.Manifest
import android.app.AppOpsManager
import android.app.role.RoleManager
import android.companion.virtual.VirtualDeviceManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.os.UserHandle
import android.view.ViewTreeObserver
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
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
import com.android.settingslib.spa.framework.util.formatString
import com.android.settingslib.spa.widget.preference.ListPreferenceOption
import com.android.settingslib.spa.widget.preference.Preference
import com.android.settingslib.spa.widget.preference.PreferenceModel
import com.android.settingslib.spa.widget.preference.Radio2
import com.android.settingslib.spa.widget.preference.ZeroStatePreference
import com.android.settingslib.spa.widget.scaffold.RegularScaffold
import com.android.settingslib.spa.widget.scaffold.SettingsScaffold
import com.android.settingslib.spa.widget.ui.Category
import com.android.settingslib.spa.widget.ui.Footer
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
import com.android.settingslib.spaprivileged.template.app.AppListItemModel
import com.android.settingslib.spaprivileged.template.app.AppListState
import com.android.settingslib.spaprivileged.template.app.NoAppInfo
import com.android.settingslib.spaprivileged.template.common.UserProfilePager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

/**
 * A page provider for the special access page listing apps that can automate other apps.
 *
 * This page displays a list of applications that have requested the
 * [Manifest.permission.ACCESS_COMPUTER_CONTROL] permission and are allowed to run automation.
 */
object ComputerControlAutomationAppListProvider : SettingsPageProvider {

    private const val PAGE_NAME = "ComputerControlPermissionPage"

    private val PAGE_PARAMETER =
        listOf(
            navArgument(PACKAGE_NAME) { type = NavType.StringType },
            navArgument(USER_ID) { type = NavType.IntType },
        )

    override val name: String = PAGE_NAME

    /** Displays the list of apps that can automate other apps. */
    @Composable
    override fun Page(arguments: Bundle?) {
        val context = LocalContext.current
        val pageViewModel: ComputerControlAutomationViewModel = viewModel {
            ComputerControlAutomationViewModel(context)
        }
        val model = pageViewModel.model
        val perAppConsentEnabled = remember {
            android.companion.virtualdevice.flags.Flags.computerControlPerAppConsent()
        }
        val assistantRoleRequirement = remember {
            android.companion.virtualdevice.flags.Flags.computerControlRoleAssistantRequirement()
        }
        // Refresh list on focus gain (in case it changed externally, e.g. in split-screen)
        // TODO (b/485637560): Consider a listener style callback API for app list changes
        val view = LocalView.current
        DisposableEffect(view) {
            val listener =
                ViewTreeObserver.OnWindowFocusChangeListener { hasFocus ->
                    if (hasFocus) {
                        model.refresh()
                    }
                }
            view.viewTreeObserver.addOnWindowFocusChangeListener(listener)
            onDispose { view.viewTreeObserver.removeOnWindowFocusChangeListener(listener) }
        }
        SettingsScaffold(title = stringResource(model.pageTitleResId)) { paddingValues ->
            Box(Modifier.padding(top = paddingValues.calculateTopPadding()).fillMaxSize()) {
                UserProfilePager { userGroup ->
                    val userIds = remember(userGroup) { userGroup.userInfos.map { it.id } }
                    val hasItemsUsers by model.hasItemsUsers.collectAsState()
                    val hasItemsForThisUser =
                        remember(hasItemsUsers, userIds) { userIds.any { it in hasItemsUsers } }

                    val header =
                        @Composable {
                            if (perAppConsentEnabled && hasItemsForThisUser) {
                                Spacer(modifier = Modifier.height(SettingsSpace.extraSmall4))
                                Text(
                                    text =
                                        stringResource(
                                            R.string.computer_control_automation_agent_list_header
                                        ),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(modifier = Modifier.height(SettingsSpace.small4))
                            }
                        }

                    val footer =
                        @Composable {
                            if (assistantRoleRequirement && hasItemsForThisUser) {
                                Column(Modifier.padding(SettingsDimension.paddingSmall)) {
                                    Spacer(modifier = Modifier.height(SettingsSpace.small1))
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

                    AppListInput(
                            config =
                                AppListConfig(
                                    userIds = userIds,
                                    showInstantApps = false,
                                    matchAnyUserForAdmin = false,
                                ),
                            listModel = model,
                            state = AppListState(showSystem = { false }, searchQuery = { "" }),
                            header = header,
                            footer = footer,
                            bottomPadding = paddingValues.calculateBottomPadding(),
                        )
                        .AppList(
                            noAppInfo =
                                NoAppInfo(
                                    icon = Icons.Filled.Apps,
                                    title =
                                        R.string.computer_control_automation_agent_list_empty_title,
                                )
                        )
                }
            }
        }
    }

    /** A preference entry for the app info page for apps that can automate. */
    @Composable
    fun InfoPageEntryItem(app: ApplicationInfo) {
        val model = rememberContext(::ComputerControlAgentPageModel)
        if (!model.isValidAgent(app)) {
            return
        }
        // Refresh summary on focus gain (in case it changed externally, by removing/adding new
        // automatable apps through dialog)
        // TODO (b/485637560): Consider a listener style callback API for app list changes
        var reloadState by remember { mutableIntStateOf(0) }
        val view = LocalView.current
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
        val context = LocalContext.current
        val summary = remember(app, reloadState) { model.getSummary(context, app) }
        Preference(
            object : PreferenceModel {
                override val title = stringResource(model.pageTitleResId)
                override val summary: () -> CharSequence = { summary }

                override val onClick = navigator(app)
            }
        )
    }

    /** Gets the route prefix to this page. */
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

    fun buildAppListInjectEntry(): SettingsEntryBuilder {
        val appListPage =
            SettingsPage.create(name = PAGE_NAME, parameter = listOf(), arguments = bundleOf())
        return SettingsEntryBuilder.createInject(owner = appListPage).setUiLayoutFn {
            val listModel = rememberContext(::ComputerControlAgentPageModel)
            Preference(
                object : PreferenceModel {
                    override val title = stringResource(listModel.pageTitleResId)
                    override val onClick = navigator(route = PAGE_NAME)
                }
            )
        }
    }

    fun getAppListRoute(): String = PAGE_NAME
}

/** A page provider for the special access page of an app that can automate other apps. */
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
        val context = LocalContext.current
        val model = rememberContext(::ComputerControlAgentPageModel)
        val packageName = arguments?.getString(PACKAGE_NAME)!!
        val userId = arguments.getInt(USER_ID)
        val packageInfo =
            remember(packageName, userId) {
                PackageManagers.getPackageInfoAsUser(packageName, userId)
            } ?: return
        val appRepository = rememberAppRepository()
        val app = checkNotNull(packageInfo.applicationInfo)
        val appLabel = appRepository.produceLabel(app).value
        val roleManager = rememberContext { context.getSystemService(RoleManager::class.java)!! }
        val isDefaultAssistant =
            remember(app.packageName, app.userId) {
                roleManager
                    .getRoleHoldersAsUser(RoleManager.ROLE_ASSISTANT, UserHandle.of(app.userId))
                    .contains(app.packageName)
            }
        val perAppConsentEnabled = remember {
            android.companion.virtualdevice.flags.Flags.computerControlPerAppConsent()
        }
        val assistantRoleRequirement = remember {
            android.companion.virtualdevice.flags.Flags.computerControlRoleAssistantRequirement()
        }
        val blockAccessForNonAssistantApp = assistantRoleRequirement && !isDefaultAssistant

        // Custom composable layout for the app info page to support centered app description
        // without any app version information and custom header layout.
        val header =
            @Composable {
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
                        text = appLabel,
                        style = MaterialTheme.typography.titleLargeEmphasized,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(model.subHeadingResId, appLabel),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = SettingsSpace.extraSmall2),
                        textAlign = TextAlign.Center,
                    )
                }
                if (blockAccessForNonAssistantApp) {
                    AssistantRequirementCard(
                        appLabel,
                        modifier = Modifier.padding(SettingsDimension.itemPadding),
                    )
                    Spacer(modifier = Modifier.height(SettingsSpace.extraSmall4))
                }
            }

        val footer =
            @Composable {
                Footer {
                    if (perAppConsentEnabled) {
                        Text(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                            text =
                                AnnotatedString.fromHtml(
                                    stringResource(R.string.computer_control_automation_footer_info)
                                ),
                        )
                        Spacer(Modifier.height(SettingsSpace.small1))
                    }
                    Text(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        text = stringResource(model.footerResId, appLabel),
                    )
                    if (assistantRoleRequirement) {
                        Spacer(Modifier.height(SettingsSpace.small1))
                        AssistantLinkedRequirementFooter()
                    }
                }
            }

        RegularScaffold(title = stringResource(model.pageTitleResId)) {
            header()
            packageInfo.applicationInfo?.let {
                if (perAppConsentEnabled) {
                    Content(it, !blockAccessForNonAssistantApp)
                } else {
                    LegacyContent(it, model)
                }
            }
            footer()
        }
    }

    /** The content of the special access page with per app consent enabled. */
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Composable
    fun Content(app: ApplicationInfo, isTargetListEnabled: Boolean) {
        val context = LocalContext.current
        val appRepository = rememberAppRepository()
        val appLabel = appRepository.produceLabel(app).value
        val consentController = rememberContext { ComputerControlConsentController(it, app) }
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
            remember(automatablePackages) { consentController.getDisplayApps(automatablePackages) }
        if (displayApps.isNotEmpty()) {
            // Allowed app list header
            Text(
                text =
                    stringResource(
                        R.string.computer_control_automation_automatable_app_list_header
                    ),
                modifier = Modifier.padding(SettingsDimension.itemPadding),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLargeEmphasized,
            )
            // Allowed app list
            Category {
                Column(modifier = Modifier.fillMaxWidth()) {
                    displayApps.forEachIndexed { index, targetApp ->
                        val removeMessage =
                            stringResource(
                                R.string.computer_control_automation_automatable_app_removed
                            )
                        AllowedAutomatableAppListItem(
                            app = targetApp,
                            enabled = isTargetListEnabled,
                            shape = getAppItemShape(index, displayApps.size),
                        ) {
                            consentController.removeAutomatablePackage(targetApp.packageName)
                            automatablePackages = automatablePackages - targetApp.packageName
                            Toast.makeText(context, removeMessage, Toast.LENGTH_SHORT).show()
                        }

                        if (index < displayApps.size - 1) {
                            Spacer(modifier = Modifier.height(2.dp))
                        }
                    }
                }
            }
            // Remove all button
            val removeAllMessage =
                stringResource(R.string.computer_control_automation_automatable_all_apps_removed)
            Button(
                onClick = {
                    consentController.clearAutomatablePackages()
                    automatablePackages = emptySet()
                    Toast.makeText(context, removeAllMessage, Toast.LENGTH_SHORT).show()
                },
                modifier =
                    Modifier.heightIn(min = SettingsDimension.preferenceMinHeight)
                        .padding(SettingsDimension.itemPadding),
                shape = RoundedCornerShape(SettingsRadius.extraLarge1),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceBright,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
            ) {
                Text(text = stringResource(R.string.computer_control_automation_clear_all_apps))
            }
        } else {
            Spacer(modifier = Modifier.height(SettingsSpace.small1))
            ZeroStatePreference(
                Icons.Filled.Apps,
                stringResource(
                    R.string.computer_control_automation_agent_list_empty_title_per_app,
                    appLabel,
                ),
            )
        }
    }

    /** The content of the special access page without per app consent enabled. */
    @Composable
    private fun LegacyContent(app: ApplicationInfo, model: ComputerControlAgentPageModel) {
        val consentController = rememberContext { ComputerControlConsentController(it, app) }
        val selectedId by
            consentController.appOpsModeFlow.collectAsState(consentController.appOpsMode)
        val options =
            listOf(
                ListPreferenceOption(
                    id = AppOpsManager.MODE_ALLOWED,
                    text = stringResource(model.alwaysAllowTitleResId),
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
                Radio2(option, selectedId, enabled = true) { consentController.setAppOpMode(it) }
            }
        }
    }
}

/**
 * The model for the computer control special access agent pages.
 *
 * This model is also responsible for loading the list of apps that have requested the
 * [Manifest.permission.ACCESS_COMPUTER_CONTROL] permission and is allowed to automate other apps.
 */
class ComputerControlAgentPageModel(context: Context) : AppListModel<ComputerControlAgentRecord> {
    private val roleManager = context.getSystemService(RoleManager::class.java)!!
    private val vdm = context.getSystemService(VirtualDeviceManager::class.java)
    val permission = Manifest.permission.ACCESS_COMPUTER_CONTROL
    val hasItemsUsers = MutableStateFlow<Set<Int>>(emptySet())

    val alwaysAllowTitleResId = R.string.computer_control_automation_always_allow
    val askTitleResId = R.string.computer_control_automation_ask_every_time
    val deniedTitleResId = R.string.computer_control_automation_dont_allow
    val pageTitleResId = R.string.computer_control_automation_page_title
    val perAppConsentEnabled =
        android.companion.virtualdevice.flags.Flags.computerControlPerAppConsent()
    val assistantRoleRequirement =
        android.companion.virtualdevice.flags.Flags.computerControlRoleAssistantRequirement()
    val subHeadingResId =
        if (perAppConsentEnabled) {
            R.string.computer_control_automation_sub_heading_flag_per_app_consent
        } else {
            R.string.computer_control_automation_sub_heading
        }

    val footerResId =
        if (perAppConsentEnabled) {
            R.string.computer_control_automation_footer_summary_flag_per_app_consent
        } else {
            R.string.computer_control_automation_footer_summary
        }

    private val refreshFlow = MutableStateFlow(0)

    fun refresh() {
        refreshFlow.update { it + 1 }
    }

    override fun getComparator(option: Int): Comparator<AppEntry<ComputerControlAgentRecord>> =
        compareByDescending<AppEntry<ComputerControlAgentRecord>> { it.record.isDefaultAssistant }
            .then(super.getComparator(option))

    @Composable
    override fun AppListItemModel<ComputerControlAgentRecord>.AppItem() {
        val onClick = ComputerControlAutomationAppListProvider.navigator(app = record.app)
        Preference(
            remember(record) {
                object : PreferenceModel {
                    override val title = label
                    override val summary = this@AppItem.summary
                    override val icon = @Composable { AppIcon(app = record.app) }
                    override val onClick = onClick
                }
            }
        )
    }

    @Composable
    override fun getSummary(option: Int, record: ComputerControlAgentRecord): (() -> CharSequence) {
        val context = LocalContext.current
        val summary = getSummary(context, record.app)
        return { summary }
    }

    fun getSummary(context: Context, app: ApplicationInfo): String {
        if (perAppConsentEnabled) {
            val count =
                vdm?.computerControlConsentManager
                    ?.getAutomatableAppListForAgent(app.uid, app.packageName)
                    ?.size ?: 0
            if (count == 0) {
                return context.getString(R.string.computer_control_automation_no_apps_allowed)
            }
            return context.formatString(
                R.string.computer_control_automation_apps_allowed,
                "count" to count,
            )
        } else {
            val controller = ComputerControlConsentController(context, app)
            return context.getString(
                when (controller.appOpsMode) {
                    AppOpsManager.MODE_ALLOWED -> alwaysAllowTitleResId
                    AppOpsManager.MODE_IGNORED -> deniedTitleResId
                    else -> askTitleResId
                }
            )
        }
    }

    internal fun isValidAgent(app: ApplicationInfo): Boolean {
        if (!app.hasRequestPermission(permission)) {
            return false
        }
        if (!perAppConsentEnabled) {
            return true
        }
        return vdm?.isPackageApprovedToRunComputerControlAutomation(app.packageName, app.userId) ==
            true
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
                userId to
                    appList
                        .filter { app -> isValidAgent(app) }
                        .map {
                            ComputerControlAgentRecord(
                                app = it,
                                isDefaultAssistant =
                                    defaultAssistantPackage.contains(it.packageName),
                            )
                        }
            }
            .onEach { (userId, result) ->
                hasItemsUsers.update { if (result.isNotEmpty()) it + userId else it - userId }
            }
            .map { it.second }
}

class ComputerControlAutomationViewModel(context: Context) : ViewModel() {
    val model = ComputerControlAgentPageModel(context)
}
