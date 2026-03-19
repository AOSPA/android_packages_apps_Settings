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
package com.android.settings.spa.accessibility

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.UserHandle
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.android.settings.R
import com.android.settingslib.spa.framework.common.SettingsPageProvider
import com.android.settingslib.spa.framework.compose.rememberContext
import com.android.settingslib.spa.framework.theme.SettingsDimension
import com.android.settingslib.spa.lifecycle.collectAsCallbackWithLifecycle
import com.android.settingslib.spa.widget.ui.SettingsIntro
import com.android.settingslib.spaprivileged.model.app.AppEntry
import com.android.settingslib.spaprivileged.model.app.AppListModel
import com.android.settingslib.spaprivileged.model.app.AppRecord
import com.android.settingslib.spaprivileged.template.app.AppListItemModel
import com.android.settingslib.spaprivileged.template.app.AppListPage
import com.android.settingslib.spaprivileged.template.app.AppListSwitchItem
import java.util.concurrent.TimeUnit
import kotlin.text.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext

object ForceDarkAppExceptionsPageProvider : SettingsPageProvider {
    override val name = "ForceDarkAppExceptions"

    @Composable
    override fun Page(arguments: Bundle?) {
        AppListPage(
            title = stringResource(R.string.accessibility_expanded_dark_theme_exceptions_title),
            listModel = rememberContext(::ForceDarkAppExceptionsListModel),
            header = {
                Box(Modifier.padding(SettingsDimension.itemPadding)) {
                    SettingsIntro(
                        stringResource(
                            R.string.accessibility_expanded_dark_theme_exceptions_header_description
                        )
                    )
                }
            },
            noMoreOptions = true,
        )
    }
}

data class ForceDarkAppExceptionRecord(
    override val app: ApplicationInfo,
    val controller: ForceDarkAppExceptionsController,
    var lastTimeUsed: Long = LAST_TIME_USED_DEFAULT,
) : AppRecord {
    companion object {
        const val LAST_TIME_USED_DEFAULT = 0L
    }
}

class ForceDarkAppExceptionsListModel(private val context: Context) :
    AppListModel<ForceDarkAppExceptionRecord> {

    @VisibleForTesting
    var repositoryFactory: (Context, Int) -> ForceDarkAppExceptionsRepository = { context, userId ->
        ForceDarkAppExceptionsRepository(context, userId)
    }

    /** Returns packages that should not be included in the recently accessed category. */
    private fun getPackagesToRemove(): Set<String> {
        return buildSet {
            // 1. Exclude the Settings app itself
            add(context.packageName)

            // 2. Exclude the Default Launcher
            getDefaultLauncherPackageName(context)?.let { add(it) }

            // 3. Optional: Add specific system packages or hardcoded app list here
            // add("com.android.systemui")
        }
    }

    private fun getDefaultLauncherPackageName(context: Context): String? {
        val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_HOME) }

        // Resolve the activity that handles the HOME intent
        val resolveInfo =
            context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)

        // If multiple launchers exist and none is set as default,
        // the system might return "android" (the resolver activity).
        return resolveInfo?.activityInfo?.packageName
    }

    // Helper to get UsageStatsManager for a specific userId
    private fun getUserUsageStatsManager(userId: Int): UsageStatsManager {
        val userContext = context.createContextAsUser(UserHandle.of(userId), 0)
        return userContext.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    }

    // Helper to get or create the UsageStatsMap for a specific userId
    private suspend fun getUsageStatsMapForUser(userId: Int): Map<String, UsageStats> =
        withContext(Dispatchers.IO) {
            val usageStatsManager = getUserUsageStatsManager(userId)
            val now = System.currentTimeMillis()
            val startTime = now - TimeUnit.DAYS.toMillis(5)

            // This query is inherently expensive and should run in the background
            usageStatsManager.queryAndAggregateUsageStats(startTime, now).toMutableMap().apply {
                getPackagesToRemove().forEach { pkgName -> remove(pkgName) }
            }
        }

    override fun transform(userIdFlow: Flow<Int>, appListFlow: Flow<List<ApplicationInfo>>) =
        userIdFlow
            .mapLatest { userId ->
                repositoryFactory(context, userId) to getUsageStatsMapForUser(userId)
            }
            .combine(appListFlow) { (repository, usageStatsMap), appList ->
                appList.map { app ->
                    ForceDarkAppExceptionRecord(
                        app = app,
                        controller = ForceDarkAppExceptionsController(app, repository),
                        lastTimeUsed =
                            usageStatsMap[app.packageName]?.lastTimeUsed
                                ?: ForceDarkAppExceptionRecord.LAST_TIME_USED_DEFAULT,
                    )
                }
            }

    override fun getComparator(option: Int): Comparator<AppEntry<ForceDarkAppExceptionRecord>> =
        compareByDescending<AppEntry<ForceDarkAppExceptionRecord>> {
                // Ordering by the Force dark override status
                !it.record.controller.isForceDarkAllowed.value
            }
            .thenByDescending { it.record.lastTimeUsed }
            .then(super.getComparator(option))

    @Composable
    override fun AppListItemModel<ForceDarkAppExceptionRecord>.AppItem() {
        AppListSwitchItem(
            checked = record.controller.isForceDarkAllowed.collectAsCallbackWithLifecycle(),
            changeable = {
                // TODO(b/448469020): If the app exists in our blocklist, the switch item for the
                // app should not be changeable.
                true
            },
            // When the switch is on, enable EDT for the app, otherwise, exclude the app from EDT.
            onCheckedChange = record.controller::setIsForceDarkAllowed,
        )
    }
}
