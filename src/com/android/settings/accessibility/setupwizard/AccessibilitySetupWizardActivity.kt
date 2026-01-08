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

package com.android.settings.accessibility.setupwizard

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.android.settings.R
import com.google.android.setupdesign.util.ThemeHelper

/**
 * Host Activity for Accessibility Setup Wizard flow. It manages the theme and the fragment
 * container.
 */
class AccessibilitySetupWizardActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        applySuwTheme()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.accessibility_suw_activity)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.fragment_container, AccessibilitySetupWizardFragment())
                .commit()
        }
    }

    private fun applySuwTheme() {
        val context = applicationContext
        if (ThemeHelper.shouldApplyGlifExpressiveStyle(context)) {
            if (!ThemeHelper.trySetSuwTheme(this)) {
                setTheme(ThemeHelper.getSuwDefaultTheme(context))
                ThemeHelper.trySetDynamicColor(this)
            }
        } else {
            ThemeHelper.applyTheme(this)
            ThemeHelper.trySetDynamicColor(this)
        }
    }
}
