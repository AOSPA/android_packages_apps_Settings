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

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import androidx.preference.PreferenceViewHolder
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.supervision.ipc.SupportedApp
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowPackageManager

@RunWith(AndroidJUnit4::class)
@LooperMode(LooperMode.Mode.INSTRUMENTATION_TEST)
class SupervisionWebContentFiltersSupportedAppsEntryPointPreferenceTest {
    private lateinit var shadowPackageManager: ShadowPackageManager
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val preference =
        SupervisionWebContentFiltersSupportedAppsEntryPointPreference(
            context,
            listOf(
                SupportedApp(
                    title = "Supported app",
                    summary = "App summary",
                    packageName = "com.android.chrome",
                )
            ),
        )
    private val iconDrawable: ColorDrawable = ColorDrawable(Color.RED)

    @Before
    fun setUp() {
        shadowPackageManager = shadowOf(context.packageManager)
        val appPackageName = "com.android.chrome"
        shadowPackageManager.installPackage(
            PackageInfo().apply {
                packageName = appPackageName
                this.applicationInfo = ApplicationInfo().apply { packageName = appPackageName }
            }
        )
        shadowPackageManager.setApplicationIcon(appPackageName, iconDrawable)
    }

    @Test
    fun getWidgetLayourResource() {
        assertThat(preference.widgetLayoutResource)
            .isEqualTo(R.layout.supervision_supported_apps_entry_point_preference)
    }

    @Test
    fun filterEnabled_supportedAppIconExistsWithNoColorFilter() {
        val holder =
            PreferenceViewHolder.createInstanceForTests(
                LayoutInflater.from(context)
                    .inflate(
                        R.layout.supervision_supported_apps_entry_point_preference,
                        /* root= */ null,
                    )
            )
        preference.onBindViewHolder(holder)
        val icon =
            holder.itemView.findViewById<ImageView?>(
                R.id.supervision_supported_apps_entry_point_icon_1
            )
        assertThat(icon).isNotNull()
        assertThat(icon?.visibility).isEqualTo(View.VISIBLE)
        assertThat(icon?.drawable).isEqualTo(iconDrawable)
        assertThat(icon?.colorFilter).isNull()
    }

    @Test
    fun filterDisabled_supportedAppIconExistsWithColorFilter() {
        preference.isEnabled = false
        val holder =
            PreferenceViewHolder.createInstanceForTests(
                LayoutInflater.from(context)
                    .inflate(
                        R.layout.supervision_supported_apps_entry_point_preference,
                        /* root= */ null,
                    )
            )
        preference.onBindViewHolder(holder)
        val icon =
            holder.itemView.findViewById<ImageView?>(
                R.id.supervision_supported_apps_entry_point_icon_1
            )
        assertThat(icon).isNotNull()
        assertThat(icon?.visibility).isEqualTo(View.VISIBLE)
        assertThat(icon?.drawable).isEqualTo(iconDrawable)
        assertThat(icon?.colorFilter).isNotNull()
    }
}
