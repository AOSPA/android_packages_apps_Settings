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

import android.app.supervision.flags.Flags
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.os.Looper
import android.platform.test.annotations.EnableFlags
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentActivity
import com.android.settings.R
import com.android.settings.testutils.shadow.SettingsShadowResources
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog

@RunWith(RobolectricTestRunner::class)
@Config(shadows = [SettingsShadowResources::class])
class EnableSupervisionDialogFragmentTest {

    private val supervisionAppName = "Test Supervisor App"
    private val supervisionPackageName = "com.test.app"

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_UI_UPDATES)
    fun onCreateDialog_flagEnabled_showsUpdatedTitleAndWarning() {
        val controller: ActivityController<FragmentActivity> =
            Robolectric.buildActivity(FragmentActivity::class.java)
        val activity = controller.get()
        val shadowActivity = shadowOf(activity)
        shadowActivity.setCallingPackage(supervisionPackageName)

        val shadowPackageManager = shadowOf(activity.packageManager)
        val appInfo =
            ApplicationInfo().apply {
                packageName = supervisionPackageName
                name = "Test App"
                nonLocalizedLabel = supervisionAppName
            }
        val packageInfo =
            PackageInfo().apply {
                packageName = supervisionPackageName
                applicationInfo = appInfo
            }
        shadowPackageManager.installPackage(packageInfo)
        controller.setup()

        val fragment = EnableSupervisionDialogFragment.newInstance(null, null)
        fragment.show(activity.supportFragmentManager, "tag")
        shadowOf(Looper.getMainLooper()).idle()

        val dialog = ShadowDialog.getLatestDialog() as AlertDialog
        val title = dialog.findViewById<TextView>(androidx.appcompat.R.id.alertTitle)?.text
        val warning = dialog.findViewById<TextView>(R.id.supervision_activation_warning)?.text

        val expectedTitle =
            activity.getString(R.string.supervision_confirmation_title, supervisionAppName)
        assertThat(title).isEqualTo(expectedTitle)

        val expectedWarning =
            activity.getString(R.string.supervision_role_holder_warning, supervisionAppName)
        assertThat(warning).isEqualTo(expectedWarning)
    }
}
