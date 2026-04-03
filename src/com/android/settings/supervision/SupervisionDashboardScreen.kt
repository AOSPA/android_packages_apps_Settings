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

import android.app.role.OnRoleHoldersChangedListener
import android.app.role.RoleManager
import android.app.role.RoleManager.ROLE_SUPERVISION
import android.app.settings.SettingsEnums
import android.app.supervision.SupervisionManager
import android.app.supervision.flags.Flags
import android.content.Context
import android.content.Intent
import android.os.UserHandle
import android.provider.Settings
import android.util.Log
import androidx.annotation.NonNull
import androidx.fragment.app.Fragment
import androidx.preference.Preference
import androidx.preference.PreferenceGroup
import com.android.settings.R
import com.android.settings.Utils
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.supervision.appstorefilters.SupervisionAppStoreFiltersScreen
import com.android.settings.supervision.credentialmanagement.SupervisionPinManagementScreen
import com.android.settings.supervision.ipc.SupervisionMessengerClient
import com.android.settings.supervision.shared.supervisionRoleHolders
import com.android.settings.supervision.shared.widget.AutoHidingPreferenceCategory
import com.android.settings.supervision.shared.widget.NonIndexablePreferenceCategory
import com.android.settings.supervision.webcontentfilters.SupervisionWebContentFiltersScreen
import com.android.settings.utils.makeLaunchIntent
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.supervision.SupervisionLog
import com.android.settingslib.widget.UntitledPreferenceCategoryMetadata
import kotlinx.coroutines.CoroutineScope
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen.Companion.APP_FUNCTION_NONE

/**
 * Supervision settings landing page (Settings > Supervision).
 *
 * This screen typically includes three parts:
 * 1. Primary switch to toggle supervision on and off.
 * 2. List of supervision features. Individual features like website filters or bedtime schedules
 *    will be listed in a group and link out to their own respective settings pages. Features
 *    implemented by supervision client apps can also be dynamically injected into this group.
 * 3. Entry point to supervision PIN management settings page.
 */
@ProvidePreferenceScreen(SupervisionDashboardScreen.KEY)
open class SupervisionDashboardScreen :
    PreferenceAvailabilityProvider,
    PreferenceScreenMixin,
    PreferenceLifecycleProvider,
    OnRoleHoldersChangedListener {
    override fun tags(context: Context) = arrayOf(APP_FUNCTION_NONE)

    private var supervisionClient: SupervisionMessengerClient? = null
    private var supervisionManager: SupervisionManager? = null
    private var lifeCycleContext: PreferenceLifecycleContext? = null

    private lateinit var roleManager: RoleManager
    private var isSupervisionAppListBuilt: Boolean = false

    private val supervisionListener =
        object : SupervisionManager.SupervisionListener() {
            override fun onSupervisionEnabled(userId: Int) {
                refreshPreferences()
            }

            override fun onSupervisionDisabled(userId: Int) {
                refreshPreferences()
            }

            private fun refreshPreferences() {
                lifeCycleContext?.notifyPreferenceChange(KEY)
                lifeCycleContext?.notifyPreferenceChange(SupervisionPinManagementScreen.KEY)
                if (Flags.enableSupervisionSettingsUiUpdates()) {
                    lifeCycleContext?.notifyPreferenceChange(SupervisionSetUpPinPreference.KEY)
                } else {
                    lifeCycleContext?.notifyPreferenceChange(SupervisionMainSwitchPreference.KEY)
                }
            }
        }

    override fun onRoleHoldersChanged(@NonNull roleName: String, @NonNull user: UserHandle) {
        if (roleName == ROLE_SUPERVISION) {
            lifeCycleContext?.let { context ->
                // Force a rebuild and reset the flag so onResume doesn't rebuild it again
                // if the change happened while on a different screen.
                updateSupervisionAppPreferences(context)
            }
        }
    }

    override fun onCreate(context: PreferenceLifecycleContext) {
        if (isContainer(context)) {
            this.lifeCycleContext = context
            supervisionManager = context.getSystemService(SupervisionManager::class.java)
            roleManager = context.getSystemService(RoleManager::class.java)!!
            if (!Flags.enableSupervisionSettingsUiUpdates()) {
                supervisionManager?.registerSupervisionListener(supervisionListener)
            }
        }
    }

    override fun onStart(context: PreferenceLifecycleContext) {
        if (Flags.enableSupervisionSettingsUiUpdates() && isContainer(context)) {
            supervisionManager?.registerSupervisionListener(supervisionListener)
        }
    }

    override fun onResume(context: PreferenceLifecycleContext) {
        if (Flags.enableSupervisionSettingsUiUpdates() && isContainer(context)) {
            // TODO(b/480262048): Temporary fix to refresh the PIN management preference. Remove
            // this line once b/480262048 is fixed.
            lifeCycleContext?.notifyPreferenceChange(SupervisionPinManagementScreen.KEY)

            roleManager.addOnRoleHoldersChangedListenerAsUser(
                context.mainExecutor,
                this,
                UserHandle.ALL,
            )
            // Build the supervision app list for the first time
            if (!isSupervisionAppListBuilt) {
                updateSupervisionAppPreferences(context)
            }
        }
    }

    override fun onStop(context: PreferenceLifecycleContext) {
        if (Flags.enableSupervisionSettingsUiUpdates() && isContainer(context)) {
            supervisionManager?.unregisterSupervisionListener(supervisionListener)
        }
    }

    override fun onPause(context: PreferenceLifecycleContext) {
        if (Flags.enableSupervisionSettingsUiUpdates() && isContainer(context)) {
            roleManager.removeOnRoleHoldersChangedListenerAsUser(this, UserHandle.ALL)
            this.isSupervisionAppListBuilt = false
        }
    }

    override fun isFlagEnabled(context: Context) = Flags.enableSupervisionSettingsScreen()

    override val key: String
        get() = KEY

    // TODO(b/462618020) Catalyst-purpose: replace default purpose with 2 line description
    override val purpose: Int
        get() = R.string.top_level_supervision_purpose

    override val title: Int
        get() = R.string.supervision_settings_title

    override val summary: Int
        get() = R.string.supervision_settings_summary

    override val icon: Int
        get() = R.drawable.ic_account_child_invert

    override val availabilityDescription = "The device must not be in demo mode, or the device must support supervision during demo mode."

    override fun getAvailabilityStability() = PreconditionStability.UNSTABLE

    override fun isAvailable(context: Context) = !Utils.shouldHideSupervisionInDemoMode(context)

    override val indexable
        get() = true

    override val keywords: Int
        get() = R.string.keywords_supervision_settings

    override fun fragmentClass(): Class<out Fragment>? = SupervisionDashboardFragment::class.java

    override fun getMetricsCategory() = SettingsEnums.SUPERVISION_DASHBOARD

    override val highlightMenuKey: Int
        get() = R.string.menu_key_supervision

    override fun onDestroy(context: PreferenceLifecycleContext) {
        if (isContainer(context)) {
            supervisionClient?.close()
            if (!Flags.enableSupervisionSettingsUiUpdates()) {
                supervisionManager?.unregisterSupervisionListener(supervisionListener)
            }
            this.lifeCycleContext = null
            this.supervisionManager = null
        }
    }

    override fun hasCompleteHierarchy() = true

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {
            val supervisionClient = getSupervisionClient(context)
            if (Flags.enableSupervisionSettingsUiUpdates()) {
                +SupervisionRecoveryBannerPreference() order -250
                +NonIndexablePreferenceCategory(
                    SUPERVISION_DYNAMIC_GROUP_1,
                    R.string.supervision_features_group_1_purpose,
                    R.string.device_supervision_features_title,
                ) order -100
                +UntitledPreferenceCategoryMetadata(
                    SUPERVISION_DYNAMIC_GROUP_2,
                    R.string.supervision_dynamic_group_2,
                ) order 10 +=
                    {
                        +SupervisionAppStoreFiltersScreen.KEY order -100
                        +SupervisionWebContentFiltersScreen.KEY order -50
                    }
            } else {
                +SupervisionMainSwitchPreference(context, supervisionClient) order -200
                +UntitledPreferenceCategoryMetadata(
                    key = SUPERVISION_DYNAMIC_GROUP_1,
                    purpose = R.string.supervision_features_group_1_purpose,
                ) order -100 +=
                    {
                        +SupervisionWebContentFiltersScreen.KEY order 100
                    }
            }
            +UntitledPreferenceCategoryMetadata(
                key = "pin_management_group",
                purpose = R.string.pin_management_group_purpose,
            ) order 100 +=
                {
                    if (Flags.enableSupervisionSettingsUiUpdates()) {
                        +SupervisionSetUpPinPreference() order 5
                    }
                    +SupervisionPinManagementScreen.KEY order 10
                }
            if (Flags.enableSupervisionSettingsUiUpdates()) {
                +NonIndexablePreferenceCategory(
                    ACTIVE_SUPERVISION_APPS_GROUP,
                    R.string.active_supervision_apps_group_purpose,
                    R.string.supervision_apps_managing_this_device_title,
                ) order 200
                +AutoHidingPreferenceCategory(
                    AVAILABLE_SUPERVISION_APPS_GROUP,
                    R.string.available_supervision_apps_group_purpose,
                    R.string.supervision_available_apps_title,
                ) order 300 +=
                    {
                        +SupervisionPromoFooterPreference(supervisionClient) order 30
                    }
                +SupervisionAocFooterPreference(supervisionClient) order 400
            } else {
                +UntitledPreferenceCategoryMetadata(
                    key = "footer_group",
                    purpose = R.string.footer_group_purpose,
                ) order 300 +=
                    {
                        +SupervisionPromoFooterPreference(supervisionClient) order 30
                        +SupervisionAocFooterPreference(supervisionClient) order 40
                    }
            }
        }

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        makeLaunchIntent(context, SupervisionDashboardActivity::class.java, metadata?.key)

    private fun getSupervisionClient(context: Context) =
        supervisionClient ?: SupervisionMessengerClient(context).also { supervisionClient = it }

    private fun updateSupervisionAppPreferences(context: PreferenceLifecycleContext) {
        var supervisionAppCount = 0
        val supervisionAppsGroup =
            context.findPreference<PreferenceGroup>(ACTIVE_SUPERVISION_APPS_GROUP)?.apply {
                removeAll()
                for (supervisionApp in context.supervisionRoleHolders) {
                    try {
                        addPreference(createSupervisionAppPreference(context, supervisionApp))
                        // Increment the count on successfully adding the preference
                        supervisionAppCount++
                    } catch (e: Exception) {
                        Log.e(
                            SupervisionLog.TAG,
                            "Error displaying supervision app preference for: $supervisionApp",
                            e,
                        )
                    }
                }
            }
        // Set the visibility of the entire group based on whether any apps were found.
        supervisionAppsGroup?.isVisible = supervisionAppCount > 0
        isSupervisionAppListBuilt = true
    }

    /** Creates a Preference item for a specific supervision app package. */
    private fun createSupervisionAppPreference(context: Context, packageName: String): Preference {
        val packageManager = context.packageManager
        val targetIntent =
            Intent(Settings.MANAGE_SUPERVISION_APP_SETTINGS).apply {
                addCategory(Intent.CATEGORY_DEFAULT)
                setPackage(packageName)
            }
        val resolveInfoList = packageManager.queryIntentActivities(targetIntent, 0)
        if (resolveInfoList.isEmpty()) {
            throw IllegalStateException(
                "No activity found for details action in package: $packageName"
            )
        }

        val activityInfo = resolveInfoList.first().activityInfo
        return Preference(context, /* attrs= */ null).apply {
            setIcon(activityInfo.loadIcon(context.packageManager))
            setTitle(activityInfo.loadLabel(context.packageManager))
            setWidgetLayoutResource(R.layout.preference_external_action_icon)
            intent = targetIntent.setClassName(packageName, activityInfo.name)
        }
    }

    companion object {
        const val KEY = "top_level_supervision"
        internal const val SUPERVISION_DYNAMIC_GROUP_1 = "supervision_features_group_1"
        internal const val SUPERVISION_DYNAMIC_GROUP_2 = "supervision_features_group_2"
        internal val FEATURE_GROUP_KEYS =
            listOf(SUPERVISION_DYNAMIC_GROUP_1, SUPERVISION_DYNAMIC_GROUP_2)
        internal const val ACTIVE_SUPERVISION_APPS_GROUP = "active_supervision_apps_group"

        internal const val AVAILABLE_SUPERVISION_APPS_GROUP = "available_supervision_apps_group"
    }
}
