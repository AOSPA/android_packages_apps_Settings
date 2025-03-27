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

package com.android.settings.wifi.utils

import android.util.Log
import androidx.appcompat.app.AlertDialog

class WifiDialogHelper(
    alertDialog: AlertDialog,
    private val ssidInputGroup: TextInputGroup? = null,
) : AlertDialogHelper(alertDialog) {

    override fun canDismiss(): Boolean {
        val isValid = ssidInputGroup?.validate() ?: true
        if (!isValid) Log.w(TAG, "SSID is invalid!")
        return isValid
    }

    companion object {
        const val TAG = "WifiDialogHelper"
    }
}
