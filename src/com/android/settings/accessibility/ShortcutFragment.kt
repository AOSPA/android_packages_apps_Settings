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

package com.android.settings.accessibility

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.preference.Preference
import androidx.recyclerview.widget.RecyclerView
import com.android.settings.accessibility.shortcuts.EditShortcutsPreferenceFragment
import com.google.android.setupcompat.util.WizardManagerHelper

/**
 * Base class for Fragment that holds a [ShortcutPreference]. By default, the fragment
 * is not restricted. If the fragment should be restricted, pass the restriction key when calling
 * super's constructor. See [com.android.settings.dashboard.RestrictedDashboardFragment]
 */
abstract class ShortcutFragment(restrictionKey: String? = null) :
    BaseRestrictedSupportFragment(restrictionKey) {

    abstract fun getFeatureName(): CharSequence
    abstract fun getFeatureComponentName(): ComponentName
    open fun getShortcutPreferenceController(): ToggleShortcutPreferenceController {
        return use<ToggleShortcutPreferenceController>(ToggleShortcutPreferenceController::class.java)
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        if (preference is ShortcutPreference) {
            val isChecked = preference.isChecked
            val prefController = getShortcutPreferenceController()
            if (isChecked) {
                AccessibilityShortcutsTutorial.DialogFragment.showDialog(
                    getChildFragmentManager(),
                    prefController.getUserPreferredShortcutTypes(getFeatureComponentName()),
                    getFeatureName(),
                    WizardManagerHelper.isAnySetupWizard(getIntent())
                )
            }
            return
        }

        super.onDisplayPreferenceDialog(preference)
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        if (preference is ShortcutPreference) {
            EditShortcutsPreferenceFragment.showEditShortcutScreen(
                requireContext(), getMetricsCategory(), preference.title,
                getFeatureComponentName(), getIntent()
            )
            return true
        }
        return super.onPreferenceTreeClick(preference)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        getShortcutPreferenceController().initialize(getFeatureComponentName())
    }

    override fun onCreateRecyclerView(
        inflater: LayoutInflater,
        parent: ViewGroup,
        savedInstanceState: Bundle?
    ): RecyclerView {
        val recyclerView =
            super.onCreateRecyclerView(inflater, parent, savedInstanceState)
        return AccessibilityFragmentUtils.addCollectionInfoToAccessibilityDelegate(recyclerView)
    }
}
