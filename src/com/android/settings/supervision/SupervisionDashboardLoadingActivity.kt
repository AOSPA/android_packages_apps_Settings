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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.MenuItem
import androidx.fragment.app.FragmentActivity
import com.android.settings.R
import com.android.settings.core.CategoryMixin
import com.android.settings.overlay.FeatureFactory.Companion.featureFactory
import com.android.settingslib.collapsingtoolbar.R.drawable.settingslib_expressive_icon_back as EXPRESSIVE_BACK_ICON
import com.android.settingslib.drawer.CategoryKey.CATEGORY_SUPERVISION
import com.android.settingslib.widget.SettingsThemeHelper

class SupervisionDashboardLoadingActivity : FragmentActivity(), CategoryMixin.CategoryListener {

    private lateinit var categoryMixin: CategoryMixin
    private lateinit var supervisionPackage: String

    private val packageChangeReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                // Only react to supervision package updates.
                if (intent.data?.schemeSpecificPart == supervisionPackage) {
                    categoryMixin.updateCategories()

                    maybeLaunchDashboard()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get the supervision package name from the system config
        supervisionPackage =
            resources.getString(com.android.internal.R.string.config_systemSupervision)
        if (supervisionPackage.isEmpty()) {
            finish()
            return
        }

        setContentView(R.layout.supervision_dashboard_loading_screen)

        categoryMixin = CategoryMixin(this)
    }

    override fun onResume() {
        super.onResume()

        registerReceivers()
        tryEnableSupervisionPackage()

        setupActionBar()
        maybeLaunchDashboard()
    }

    override fun onStop() {
        super.onStop()

        unregisterReceivers()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setupActionBar() {
        actionBar?.apply {
            elevation = 0f
            setDisplayHomeAsUpEnabled(true)
            if (SettingsThemeHelper.isExpressiveTheme(this@SupervisionDashboardLoadingActivity)) {
                setHomeAsUpIndicator(EXPRESSIVE_BACK_ICON)
            }
        }
    }

    private fun registerReceivers() {
        val intentFilter =
            IntentFilter().apply {
                addAction(Intent.ACTION_PACKAGE_ADDED)
                addAction(Intent.ACTION_PACKAGE_CHANGED)
                addDataScheme("package")
            }
        registerReceiver(packageChangeReceiver, intentFilter, RECEIVER_NOT_EXPORTED)
        categoryMixin.addCategoryListener(this)
    }

    private fun unregisterReceivers() {
        unregisterReceiver(packageChangeReceiver)
        categoryMixin.removeCategoryListener(this)
    }

    override fun onCategoriesChanged(categories: Set<String>?) {
        // A null set means refreshed all categories.
        if (categories == null || categories.contains(CATEGORY_SUPERVISION)) {
            maybeLaunchDashboard()
        }
    }

    private fun maybeLaunchDashboard() {
        if (isFinishing) return
        if (isSettingsDashboardReady()) {
            // Supervision package with necessary component is ready, launch dashboard
            val dashboardActivity = Intent(this, SupervisionDashboardActivity::class.java)
            startActivity(dashboardActivity)
            finish()
        } else if (getSupervisionAppInstallActivityInfo() != null) {
            // Supervision package with necessary component is not ready, but a mitigation action
            // is available.
            startActivity(getSupervisionAppInstallIntent())
            finish()
        }
        // Otherwise, wait for another event to trigger this check again.
    }

    private fun isSettingsDashboardReady(): Boolean {
        val hasComponent = hasNecessarySupervisionComponent()
        val tilesExist =
            featureFactory.dashboardFeatureProvider.getTilesForCategory(CATEGORY_SUPERVISION) !=
                null
        return hasComponent && tilesExist
    }

    private fun tryEnableSupervisionPackage() {
        try {
            val packageInfo = packageManager.getPackageInfo(supervisionPackage, 0)
            if (packageInfo == null) return

            val applicationInfo = packageInfo.applicationInfo
            if (applicationInfo == null) return

            if (!applicationInfo.enabled) {
                packageManager.setApplicationEnabledSetting(
                    supervisionPackage,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    0,
                )
            }
        } catch (e: PackageManager.NameNotFoundException) {
            // Supervision package is not installed, do nothing
            return
        }
    }
}
