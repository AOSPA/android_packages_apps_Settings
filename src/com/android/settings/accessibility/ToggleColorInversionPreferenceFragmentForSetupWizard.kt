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

import android.app.Activity
import android.app.settings.SettingsEnums
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.preference.PreferenceScreen
import androidx.recyclerview.widget.RecyclerView
import com.android.settings.R
import com.android.settings.accessibility.colorinversion.ui.IntroPreference
import com.android.settings.accessibility.shared.utils.TogglePreferenceAdapterInSuw
import com.android.settingslib.widget.MainSwitchPreference
import com.android.settingslib.widget.SettingsThemeHelper
import com.android.settingslib.widget.TopIntroPreference
import com.google.android.setupcompat.template.FooterBarMixin
import com.google.android.setupcompat.util.DelightHelper.shouldApplyAnimatedIcon
import com.google.android.setupdesign.GlifPreferenceLayout
import com.google.android.setupdesign.template.IconMixin

/** Fragment for toggling color inversion in Setup Wizard. */
class ToggleColorInversionPreferenceFragmentForSetupWizard :
    ToggleColorInversionPreferenceFragment() {
    private var toggleSwitchWasInitiallyChecked = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (view is GlifPreferenceLayout) {
            val layout = view
            val title =
                requireContext()
                    .getString(R.string.accessibility_display_inversion_preference_title)
            val description =
                requireContext()
                    .getString(R.string.accessibility_display_inversion_preference_intro_text)
            val icon = requireContext().getDrawable(R.drawable.ic_accessibility_visibility)
            AccessibilitySetupWizardUtils.updateGlifPreferenceLayout(
                requireContext(),
                layout,
                title,
                description,
                icon,
            )
            if (shouldApplyAnimatedIcon(getContext())) {
                val iconMixin = layout.getMixin(IconMixin::class.java)
                iconMixin.setAnimatedIcon(R.raw.icon_visibility)
                iconMixin.setAnimatedIconDelayed(false)
            }
            val mixin = layout.getMixin(FooterBarMixin::class.java)
            AccessibilitySetupWizardUtils.setPrimaryButton(requireContext(), mixin, R.string.done) {
                setResult(Activity.RESULT_OK)
                finish()
            }
        }
    }

    override fun onCreateAdapter(preferenceScreen: PreferenceScreen): RecyclerView.Adapter<*> {
        return if (SettingsThemeHelper.isExpressiveTheme(requireContext())) {
            TogglePreferenceAdapterInSuw(preferenceScreen)
        } else super.onCreateAdapter(preferenceScreen)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        // Hide the color inversion preference settings in the SuW's vision settings.
        val topIntroPreference: TopIntroPreference? = findPreference(IntroPreference.KEY)
        topIntroPreference?.isVisible = false

        val preference: MainSwitchPreference? = findPreference(MAIN_SWITCH_PREF_KEY)
        if (preference != null) {
            toggleSwitchWasInitiallyChecked = preference.isChecked
        }
    }

    override fun onCreateRecyclerView(
        inflater: LayoutInflater,
        parent: ViewGroup,
        savedInstanceState: Bundle?,
    ): RecyclerView {
        if (parent is GlifPreferenceLayout) {
            val layout = parent
            return AccessibilityFragmentUtils.addCollectionInfoToAccessibilityDelegate(
                layout.onCreateRecyclerView(inflater, parent, savedInstanceState)
            )
        }
        return super.onCreateRecyclerView(inflater, parent, savedInstanceState)
    }

    override fun getMetricsCategory(): Int =
        SettingsEnums.SUW_ACCESSIBILITY_TOGGLE_SCREEN_COLOR_INVERSION

    override fun onStop() {
        val preference: MainSwitchPreference? = findPreference(MAIN_SWITCH_PREF_KEY)
        if (preference!!.isChecked != toggleSwitchWasInitiallyChecked) {
            mMetricsFeatureProvider.action(
                context,
                SettingsEnums.SUW_ACCESSIBILITY_TOGGLE_SCREEN_COLOR_INVERSION,
                preference.isChecked,
            )
        }
        super.onStop()
    }

    override fun getHelpResource(): Int = 0 // Hides help center in action bar and footer bar in SuW

    companion object {
        internal const val MAIN_SWITCH_PREF_KEY = "accessibility_display_inversion_enabled"
    }
}
