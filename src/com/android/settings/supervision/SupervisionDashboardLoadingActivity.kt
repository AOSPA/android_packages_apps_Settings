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

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.MenuItem
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.android.settings.R
import com.android.settings.core.CategoryMixin
import com.android.settings.overlay.FeatureFactory.Companion.featureFactory
import com.android.settingslib.collapsingtoolbar.R.drawable.settingslib_expressive_icon_back as EXPRESSIVE_BACK_ICON
import com.android.settingslib.drawer.CategoryKey.CATEGORY_SUPERVISION
import com.android.settingslib.widget.SettingsThemeHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SupervisionDashboardLoadingActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get the supervision package name from the system config
        val supervisionPackage =
            resources.getString(com.android.internal.R.string.config_systemSupervision)
        if (supervisionPackage == null) {
            finish()
            return
        }

        setContentView(R.layout.supervision_dashboard_loading_screen)

        // Enable supervision app
        lifecycleScope.launch(Dispatchers.Default) {
            packageManager.setApplicationEnabledSetting(
                supervisionPackage,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                0,
            )

            // Update category after enabling supervision app
            CategoryMixin(this@SupervisionDashboardLoadingActivity).updateCategories()
            val dashboardFeatureProvider = featureFactory.dashboardFeatureProvider

            while (
                !hasNecessarySupervisionComponent(supervisionPackage) ||
                    dashboardFeatureProvider.getTilesForCategory(CATEGORY_SUPERVISION) == null
            ) {
                delay(100) // Check every 100 ms (adjust as needed)
            }

            lifecycleScope.launch(Dispatchers.Main) {
                val dashboardActivity =
                    Intent(
                        this@SupervisionDashboardLoadingActivity,
                        SupervisionDashboardActivity::class.java,
                    )
                startActivity(dashboardActivity)
                finish()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        val actionBar = getActionBar()
        if (actionBar != null) {
            actionBar.elevation = 0f
            actionBar.setDisplayHomeAsUpEnabled(true)
            if (SettingsThemeHelper.isExpressiveTheme(this)) {
                actionBar.setHomeAsUpIndicator(EXPRESSIVE_BACK_ICON)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
