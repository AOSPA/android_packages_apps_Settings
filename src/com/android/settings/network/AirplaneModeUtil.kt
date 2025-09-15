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
package com.android.settings.network

import android.companion.AssociationRequest
import android.companion.CompanionDeviceManager
import android.content.Context
import android.content.pm.PackageManager
import com.android.server.connectivity.Flags
import com.android.settings.R

/** Returns whether the airplane mode is eligible to be shown. */
fun Context.isAirplaneModeEligible() =
    resources.getBoolean(R.bool.config_show_toggle_airplane) &&
        !packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)

/** Returns whether there is a paired watch for airplane mode sync. */
fun Context.hasPairedWatchForAirplaneModeSync() =
    Flags.syncAirplaneModeWithWatches() &&
        getSystemService(CompanionDeviceManager::class.java).allAssociations.any {
            AssociationRequest.DEVICE_PROFILE_WATCH == it.deviceProfile
        }
