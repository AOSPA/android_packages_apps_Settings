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

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.android.settings.R
import com.android.settingslib.spa.framework.common.SettingsPageProvider
import com.android.settingslib.spa.framework.compose.rememberContext
import com.android.settingslib.spa.framework.util.mapItem
import com.android.settingslib.spa.lifecycle.collectAsCallbackWithLifecycle
import com.android.settingslib.spaprivileged.model.app.AppListModel
import com.android.settingslib.spaprivileged.model.app.AppRecord
import com.android.settingslib.spaprivileged.template.app.AppListItemModel
import com.android.settingslib.spaprivileged.template.app.AppListPage
import com.android.settingslib.spaprivileged.template.app.AppListSwitchItem
import kotlinx.coroutines.flow.Flow

object ForceDarkAppExceptionsPageProvider : SettingsPageProvider {
    override val name = "ForceDarkAppExceptions"

    @Composable
    override fun Page(arguments: Bundle?) {
        AppListPage(
            title = stringResource(R.string.accessibility_expanded_dark_theme_exceptions_title),
            listModel = rememberContext(::ForceDarkAppExceptionsListModel),
        )
    }
}

data class ForceDarkAppExceptionRecord(
    override val app: ApplicationInfo,
    val controller: ForceDarkAppExceptionsController,
) : AppRecord

class ForceDarkAppExceptionsListModel(
    private val context: Context,
    private val repository: ForceDarkAppExceptionsRepository =
        ForceDarkAppExceptionsRepository(context = context),
) : AppListModel<ForceDarkAppExceptionRecord> {

    override fun transform(userIdFlow: Flow<Int>, appListFlow: Flow<List<ApplicationInfo>>) =
        appListFlow.mapItem { app ->
            ForceDarkAppExceptionRecord(
                app = app,
                controller = ForceDarkAppExceptionsController(app, repository),
            )
        }

    @Composable
    override fun AppListItemModel<ForceDarkAppExceptionRecord>.AppItem() {
        AppListSwitchItem(
            checked = record.controller.isException.collectAsCallbackWithLifecycle(),
            changeable = {
                // TODO(b/448469020): If the app exists in our blocklist, the switch item for the
                // app should not be changeable.
                true
            },
            onCheckedChange = record.controller::setException,
        )
    }
}
