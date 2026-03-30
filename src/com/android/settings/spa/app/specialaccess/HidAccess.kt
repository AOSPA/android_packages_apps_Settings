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

package com.android.settings.spa.app.specialaccess

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import com.android.settings.R
import com.android.settingslib.spaprivileged.model.app.AppOps
import com.android.settingslib.spaprivileged.template.app.AppOpPermissionListModel
import com.android.settingslib.spaprivileged.template.app.TogglePermissionAppListProvider

object HidAccessAppListProvider : TogglePermissionAppListProvider {
    override val permissionType = "HidAccess"

    override fun createModel(context: Context) = HidAccessListModel(context)
}

class HidAccessListModel(context: Context) : AppOpPermissionListModel(context) {
    override val pageTitleResId = R.string.access_hid_title

    override val switchTitleResId = R.string.permit_access_hid

    override val footerResId = R.string.allow_access_hid_description

    override val appOps = AppOps(op = AppOpsManager.OP_ACCESS_HID, setModeByUid = true)

    override val permission = Manifest.permission.ACCESS_HID
}

fun <T> T.runIfHidAccessEnabled(block: T.() -> T): T {
    if (com.android.hardware.input.Flags.hidApi()) {
        return block()
    }
    return this
}
