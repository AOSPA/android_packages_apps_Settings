/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.settings.supervision

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowPackageManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.BAKLAVA])
class SupervisionPinRecoveryActivityTest {
    // TODO(b/399484695): Add test cases to verify different scenarios.

    private lateinit var activity: SupervisionPinRecoveryActivity
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var shadowPackageManager: ShadowPackageManager

    @Before
    fun setUp() {
        shadowPackageManager = shadowOf(context.packageManager)
        val intentFilter = IntentFilter(CONFIRM_PIN_ACTIVITY_ACTION)
        val componentName =
            ComponentName(
                "com.android.settings",
                ConfirmSupervisionCredentialsActivity::class.java.name,
            )
        shadowPackageManager.addActivityIfNotPresent(componentName)
        shadowPackageManager.addIntentFilterForActivity(componentName, intentFilter)
    }

    @Test
    fun onCreate_nullAction_activityCanceled() {
        val intent = Intent()
        activity =
            Robolectric.buildActivity(SupervisionPinRecoveryActivity::class.java, intent)
                .create()
                .get()
        assert(activity.isFinishing)
    }

    @Test
    fun onCreate_setupVerifiedAction_startsConfirmPinActivity() {
        val intent =
            Intent().apply { action = SupervisionPinRecoveryActivity.ACTION_SETUP_VERIFIED }
        activity =
            Robolectric.buildActivity(SupervisionPinRecoveryActivity::class.java, intent)
                .create()
                .get()

        val startedIntent = shadowOf(activity).nextStartedActivity
        assert(startedIntent.action == CONFIRM_PIN_ACTIVITY_ACTION)
    }

    @Test
    fun onCreate_updateAction_startsConfirmPinActivity() {
        val intent = Intent().apply { action = SupervisionPinRecoveryActivity.ACTION_UPDATE }

        activity =
            Robolectric.buildActivity(SupervisionPinRecoveryActivity::class.java, intent)
                .create()
                .get()

        val startedIntent = shadowOf(activity).nextStartedActivity
        assert(startedIntent.action == CONFIRM_PIN_ACTIVITY_ACTION)
    }

    @Test
    fun onCreate_postSetupVerifyAction_startsConfirmPinActivity() {
        val intent =
            Intent().apply { action = SupervisionPinRecoveryActivity.ACTION_POST_SETUP_VERIFY }
        activity =
            Robolectric.buildActivity(SupervisionPinRecoveryActivity::class.java, intent)
                .create()
                .get()

        val startedIntent = shadowOf(activity).nextStartedActivity
        assert(startedIntent.action == CONFIRM_PIN_ACTIVITY_ACTION)
    }

    companion object {
        const val CONFIRM_PIN_ACTIVITY_ACTION =
            "android.app.supervision.action.CONFIRM_SUPERVISION_CREDENTIALS"
    }
}
