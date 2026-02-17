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

package com.android.settings.applications.specialaccess.pictureinpicture

import android.app.settings.SettingsEnums
import android.content.Context
import android.content.Intent
import android.provider.Settings.ACTION_PICTURE_IN_PICTURE_SETTINGS
import com.android.settings.R
import com.android.settings.applications.specialaccess.SpecialAccessAppListScreen
import com.android.settings.contract.TAG_DEVICE_STATE_SCREEN
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferencesapi.PreferencesApiScreen.Companion.APP_FUNCTION_UNCATEGORIZED

@ProvidePreferenceScreen(PictureInPictureAppListScreen.KEY)
open class PictureInPictureAppListScreen : SpecialAccessAppListScreen() {
    override fun tags(context: Context) = arrayOf(APP_FUNCTION_UNCATEGORIZED, TAG_DEVICE_STATE_SCREEN)

    override val key: String
        get() = KEY

    //TODO(b/462618020) Catalyst-purpose: replace default purpose with 2 line description
    override val purpose: Int
        get() = R.string.special_access_picture_in_picture_app_list_purpose

    override val title: Int
        get() = R.string.picture_in_picture_title

    override fun isFlagEnabled(context: Context) = context.isPictureInPictureEnabled()

    override fun getMetricsCategory() = SettingsEnums.SETTINGS_MANAGE_PICTURE_IN_PICTURE



    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        if (metadata == null) Intent(ACTION_PICTURE_IN_PICTURE_SETTINGS) else null

    override val appDetailScreenKey
        get() = PictureInPictureAppDetailScreen.KEY

    @Deprecated(
        message =
            "This method will be removed once the catalyst framework stops passing the arguments as a bundle. Use keyParameters instead."
    )
    override fun appDetailParameters(context: Context, hierarchyType: Boolean) =
        PictureInPictureAppDetailScreen.parameters(context, hierarchyType)

    override fun appDetailKeyParameters(context: Context, hierarchyType: Boolean) =
        PictureInPictureAppDetailScreen.keyParameters(context, hierarchyType)

    companion object {
        const val KEY = "special_access_picture_in_picture_app_list"
    }
}
