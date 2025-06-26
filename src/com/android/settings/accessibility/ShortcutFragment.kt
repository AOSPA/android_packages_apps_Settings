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
 * Base class for Fragment that holds a [ShortcutPreference]. If your fragment is restricted and
 * also wants to holds a [ShortcutPreference], use [RestrictedShortcutFragment] instead.
 */
abstract class ShortcutFragment : BaseSupportFragment() {

    abstract fun getFeatureName(): CharSequence

    abstract fun getFeatureComponentName(): ComponentName

    open fun getShortcutPreferenceController(): ToggleShortcutPreferenceController? {
        return use<ToggleShortcutPreferenceController>(
            ToggleShortcutPreferenceController::class.java
        )
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        if (preference is ShortcutPreference) {
            val isChecked = preference.isChecked
            val prefController = getShortcutPreferenceController() ?: return
            if (isChecked) {
                showShortcutsTutorial(
                    prefController.getUserPreferredShortcutTypes(getFeatureComponentName())
                )
            }
            return
        }

        super.onDisplayPreferenceDialog(preference)
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        if (preference is ShortcutPreference) {
            showEditShortcutsScreen(preference.title ?: "")
            // log here since calling super.onPreferenceTreeClick will be skipped
            writePreferenceClickMetric(preference)
            return true
        }
        return super.onPreferenceTreeClick(preference)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        getShortcutPreferenceController()?.initialize(getFeatureComponentName())
    }

    override fun onCreateRecyclerView(
        inflater: LayoutInflater,
        parent: ViewGroup,
        savedInstanceState: Bundle?,
    ): RecyclerView {
        val recyclerView = super.onCreateRecyclerView(inflater, parent, savedInstanceState)
        return AccessibilityFragmentUtils.addCollectionInfoToAccessibilityDelegate(recyclerView)
    }

    protected fun showShortcutsTutorial(shortcutTypes: Int) {
        AccessibilityShortcutsTutorial.DialogFragment.showDialog(
            getChildFragmentManager(),
            shortcutTypes,
            getFeatureName(),
            WizardManagerHelper.isAnySetupWizard(getIntent()),
        )
    }

    protected fun showEditShortcutsScreen(screenTitle: CharSequence) {
        EditShortcutsPreferenceFragment.showEditShortcutScreen(
            requireContext(),
            getMetricsCategory(),
            screenTitle,
            getFeatureComponentName(),
            getIntent(),
        )
    }
}
