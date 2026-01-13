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

package com.android.settings.network.telephony.satellite.quicksettings

import android.content.pm.ApplicationInfo
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import com.android.settingslib.spa.framework.compose.rememberDrawablePainter
import com.android.settingslib.spa.framework.theme.SettingsDimension
import com.android.settingslib.spa.widget.preference.Preference
import com.android.settingslib.spa.widget.preference.PreferenceModel
import com.android.settingslib.spaprivileged.model.app.AppRecord
import com.android.settingslib.spaprivileged.model.app.rememberAppRepository
import com.android.settingslib.spaprivileged.template.app.AppListItemModel

/**
 * Extension of [AppListItemModel] that supports an enabled state to prevent broken accessibility
 * experiences and improper clicks.
 */
@Composable
fun <T : AppRecord> AppListItemModel<T>.SatelliteAppListItem(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier.testTag("SatelliteAppListItem").semantics(mergeDescendants = true) {
                if (!enabled) disabled()
            }
    ) {
        Preference(
            remember(this@SatelliteAppListItem, enabled) {
                object : PreferenceModel {
                    override val title = label
                    override val summary = this@SatelliteAppListItem.summary
                    override val icon =
                        @Composable {
                            AppIcon(app = record.app, size = SettingsDimension.appIconItemSize)
                        }
                    override val onClick = if (enabled) onClick else null
                    override val enabled = { enabled }
                }
            }
        )
    }
}

@Composable
private fun AppIcon(app: ApplicationInfo, size: Dp) {
    val appRepository = rememberAppRepository()
    Image(
        painter = rememberDrawablePainter(appRepository.produceIcon(app).value),
        contentDescription = appRepository.produceIconContentDescription(app).value,
        modifier = Modifier.size(size),
    )
}
