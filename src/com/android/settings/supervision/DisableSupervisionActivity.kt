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

import android.app.supervision.SupervisionManager
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.FragmentActivity
import com.android.settingslib.supervision.SupervisionLog.TAG

/**
 * Activity for disabling device supervision.
 *
 * This activity is only available to the system supervision role holder. It disables device
 * supervision and finishes the activity with `Activity.RESULT_OK`.
 */
class DisableSupervisionActivity : FragmentActivity() {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.e(TAG, "onCreate for DisableSupervisionActivity")

        if (!isCallerSystemSupervisionRoleHolder()) {
            Log.w(TAG, "Caller is not the system supervision role holder. Finishing activity.")
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        val supervisionManager = getSystemService(SupervisionManager::class.java)
        if (supervisionManager == null) {
            Log.e(TAG, "SupervisionManager is null. Finishing activity.")
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        supervisionManager.setSupervisionEnabled(false)
        setResult(RESULT_OK)
        finish()
    }

    private fun isCallerSystemSupervisionRoleHolder(): Boolean {
        return callingPackage == systemSupervisionPackageName
    }
}
