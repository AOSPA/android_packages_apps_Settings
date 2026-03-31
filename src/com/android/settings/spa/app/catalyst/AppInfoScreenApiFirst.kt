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

package com.android.settings.spa.app.catalyst

import android.app.AppOpsManager
import android.apphibernation.AppHibernationManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import android.permission.PermissionControllerManager
import android.provider.DeviceConfig
import com.android.settings.R
import com.android.settings.applications.InstalledPackageName
import com.android.settings.applications.appinfo.AppInfoDashboardFragment
import com.android.settings.applications.getApplicationInfo
import com.android.settings.flags.Flags
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen
import com.android.settingslib.metadata.preferencesapi.category.Category
import com.android.settingslib.metadata.preferencesapi.multiusers.ManagementScope
import com.android.settingslib.metadata.preferencesapi.multiusers.PreferenceTarget
import com.android.settingslib.metadata.preferencesapi.preconditions.Allowed
import com.android.settingslib.metadata.preferencesapi.preconditions.Custom
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import com.android.settingslib.metadata.preferencesapi.types.AnyBoolean
import com.android.settingslib.spaprivileged.framework.common.asUser
import com.android.settingslib.spaprivileged.framework.common.permissionControllerManager
import com.android.settingslib.spaprivileged.model.app.userHandle
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.suspendCancellableCoroutine

// Lint.IfChange
@ProvidePreferenceScreen(AppInfoScreenApiFirst.KEY, parameterized = true)
class AppInfoScreenApiFirst :
    PreferencesApiScreen(
        key = KEY,
        topLevelSettingsCategory = Category.APPS,
        fragment = AppInfoDashboardFragment::class,
        purpose = R.string.installed_app_detail_settings_screen_purpose,
        alreadyPartiallyMigrated = AppInfoScreen::class,
        canManage = ManagementScope.PROFILE_GROUP,
    ) {

    // Caching within a single instance of a single screen is not optimal but
    // it is simple. We will in future offer cross-preference caching.
    private var cachedPackageName: String? = null
    private var cachedUserId: Int? = null
    private var cachedAppInfo: ApplicationInfo? = null
    private var cachedEligibility: Boolean? = null

    private fun getCachedAppInfo(context: Context, packageName: String, userId: Int): ApplicationInfo? {
        if (cachedPackageName == packageName && cachedUserId == userId && cachedAppInfo != null) {
            return cachedAppInfo
        }
        val appInfo =
            try {
                context.packageManager.getApplicationInfoAsUser(packageName, 0, userId)
            } catch (e: Exception) {
                null
            }
        cachedPackageName = packageName
        cachedUserId = userId
        cachedAppInfo = appInfo
        cachedEligibility = null
        return appInfo
    }

    private suspend fun isHibernationEligibleCached(
        context: Context,
        app: ApplicationInfo,
    ): Boolean {
        cachedEligibility?.let { return it }
        val isEligible = isHibernationEligibleSuspend(context, app)
        cachedEligibility = isEligible
        return isEligible
    }

    init {
        flag { Flags.catalystMigration26q2() }

        tags(APP_FUNCTION_NONE)

        parameters {
            parameter(
                name = PARAM_PACKAGE,
                purpose = R.string.installed_app_detail_manage_app_unused_parameter_purpose,
                required = true,
                type = InstalledPackageName,
            )

            prepareScreenExtras { keyParameters, extras ->
                extras.putString(
                    AppInfoDashboardFragment.ARG_PACKAGE_NAME,
                    keyParameters[PARAM_PACKAGE],
                )
            }
        }

        preference(
            key = "unused_apps_switch",
            purpose = R.string.installed_app_detail_unused_apps_switch_purpose,
            type = AnyBoolean,
            appliesTo = PreferenceTarget.USER(canManage = ManagementScope.PROFILE_GROUP),
        ) {
            sensitivityLevel(SensitivityLevel.REQUIRES_CONFIRMATION)

            preconditions("App hibernation must be available on the device and the app must not be archived.") {
                val isFeatureEnabled =
                    DeviceConfig.getBoolean(
                        DeviceConfig.NAMESPACE_APP_HIBERNATION,
                        "app_hibernation_enabled",
                        true,
                    )

                if (!isFeatureEnabled)
                    return@preconditions Custom(
                        "App hibernation is not available on this device",
                        stability = PreconditionStability.STABLE_UNTIL_APK_UPDATE,
                    )

                val appInfo =
                    getCachedAppInfo(context, parameters.getRequired(PARAM_PACKAGE), context.userId)
                        ?: return@preconditions Custom(
                            R.string.installed_app_detail_manage_app_unused_precondition_null_app,
                            stability = PreconditionStability.STABLE_UNTIL_APK_UPDATE,
                        )

                if (appInfo.isArchived)
                    return@preconditions Custom(
                        "The app is archived",
                        stability = PreconditionStability.UNSTABLE,
                    )

                Allowed
            }

            get {
                execute {
                    val appInfo =
                        getCachedAppInfo(context, parameters.getRequired(PARAM_PACKAGE), userId)
                            ?: return@execute false

                    val isEligible = isHibernationEligibleCached(context, appInfo)
                    if (isEligible) {
                        isHibernationEnabled(context, appInfo)
                    } else {
                        false
                    }
                }
            }

            set {
                preconditions("The app must not be a critical system app.") {
                    val appInfo =
                        getCachedAppInfo(context, parameters.getRequired(PARAM_PACKAGE), userId)
                            ?: return@preconditions Custom(
                                R.string
                                    .installed_app_detail_manage_app_unused_precondition_null_app,
                                stability = PreconditionStability.UNSTABLE,
                            )

                    val isEligible =
                        kotlinx.coroutines.runBlocking {
                            isHibernationEligibleCached(context, appInfo)
                        }

                    if (!isEligible) {
                        Custom(
                            R.string.installed_app_detail_manage_app_unused_precondition_exempt_app,
                            stability = PreconditionStability.UNSTABLE,
                        )
                    } else {
                        Allowed
                    }
                }

                execute { isChecked ->
                    val appInfo =
                        getCachedAppInfo(context, parameters.getRequired(PARAM_PACKAGE), userId)
                            ?: return@execute
                    setHibernationEnabled(context, appInfo, isChecked)
                }
            }
        }
    }

    private suspend fun isHibernationEligibleSuspend(
        context: Context,
        app: ApplicationInfo,
    ): Boolean {
        return suspendCancellableCoroutine { continuation ->
            val permissionController = context.asUser(app.userHandle).permissionControllerManager
            val executor = Dispatchers.IO.asExecutor()

            permissionController.getHibernationEligibility(app.packageName, executor) { eligibility
                ->
                val isEligible =
                    eligibility !=
                        PermissionControllerManager.HIBERNATION_ELIGIBILITY_EXEMPT_BY_SYSTEM &&
                        eligibility != PermissionControllerManager.HIBERNATION_ELIGIBILITY_UNKNOWN

                if (continuation.isActive) {
                    continuation.resume(isEligible)
                }
            }
        }
    }

    private fun isHibernationEnabled(context: Context, app: ApplicationInfo): Boolean {
        val appOpsManager = context.getSystemService(AppOpsManager::class.java) ?: return false

        val mode =
            appOpsManager.checkOpNoThrow(
                AppOpsManager.OPSTR_AUTO_REVOKE_PERMISSIONS_IF_UNUSED,
                app.uid,
                app.packageName,
            )

        return when (mode) {
            AppOpsManager.MODE_ALLOWED -> true
            AppOpsManager.MODE_IGNORED -> false
            AppOpsManager.MODE_DEFAULT -> {
                val isTargetPreS = app.targetSdkVersion <= Build.VERSION_CODES.Q
                !isTargetPreS
            }
            else -> false
        }
    }

    private fun setHibernationEnabled(context: Context, app: ApplicationInfo, isEnabled: Boolean) {
        val appOpsManager = context.getSystemService(AppOpsManager::class.java) ?: return
        val newMode = if (isEnabled) AppOpsManager.MODE_ALLOWED else AppOpsManager.MODE_IGNORED

        appOpsManager.setUidMode(
            AppOpsManager.OPSTR_AUTO_REVOKE_PERMISSIONS_IF_UNUSED,
            app.uid,
            newMode,
        )

        if (!isEnabled) {
            val ahm = context.getSystemService(AppHibernationManager::class.java)
            ahm?.setHibernatingForUser(app.packageName, false)
            ahm?.setHibernatingGlobally(app.packageName, false)
        }
    }

    companion object {
        const val KEY = "api_installed_app_detail_settings_screen"
        const val PARAM_PACKAGE = "package"
    }
}
// Lint.ThenChange(AppInfoDashboardFragment.java, HibernationSwitchPreferenceController.java)
