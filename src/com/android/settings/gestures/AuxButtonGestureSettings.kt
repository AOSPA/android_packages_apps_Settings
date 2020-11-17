/*
 * Copyright 2020, The Paranoid Android Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.gestures

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.UserHandle.USER_CURRENT
import android.provider.Settings

import com.android.settings.R
import com.android.settings.search.BaseSearchIndexProvider
import com.android.settings.search.actionbar.SearchMenuController
import com.android.settings.support.actionbar.HelpMenuController
import com.android.settings.support.actionbar.HelpResourceProvider
import com.android.settings.utils.CandidateInfoExtra
import com.android.settings.widget.RadioButtonPickerFragment
import com.android.settings.widget.RadioButtonPreferenceWithExtraWidget
import com.android.settings.widget.RadioButtonPreferenceWithExtraWidget.EXTRA_WIDGET_VISIBILITY_GONE
import com.android.settings.widget.RadioButtonPreferenceWithExtraWidget.EXTRA_WIDGET_VISIBILITY_SETTING
import com.android.settingslib.search.SearchIndexable
import com.android.settingslib.widget.CandidateInfo
import com.android.settingslib.widget.RadioButtonPreference

import co.aospa.framework.device.actions.AuxiliaryKeyHandlerActions

// @SearchIndexable
class AuxButtonGestureSettings : RadioButtonPickerFragment(), HelpResourceProvider {

    override fun onAttach(context: Context) {
        super.onAttach(context)
        SearchMenuController.init(this)
        HelpMenuController.init(this)
    }

    override fun getMetricsCategory(): Int = -1
    override protected fun getPreferenceScreenResId(): Int = R.xml.aux_button_settings

    override fun updateCandidates() {
        val defaultKey = getDefaultKey()
        val systemDefaultKey = getSystemDefaultKey() ?: KEY_AUX_NONE
        val screen = getPreferenceScreen()
        screen.removeAll()

        for (info in getCandidates()) {
            val pref = RadioButtonPreferenceWithExtraWidget(prefContext)
            bindPreference(pref, info.key, info, defaultKey)
            bindPreferenceExtra(pref, info.key, info, defaultKey, systemDefaultKey);
            screen.addPreference(pref)
        }
        mayCheckOnlyRadioButton()
    }

    override fun bindPreferenceExtra(
        pref: RadioButtonPreference,
        key: String,
        info: CandidateInfo,
        defaultKey: String,
        systemDefaultKey: String
    ) {
        (pref as RadioButtonPreferenceWithExtraWidget).setSummary((info as CandidateInfoExtra).loadSummary())

        when (info.key) {
            KEY_AUX_ASSIST -> {
                pref.setExtraWidgetVisibility(EXTRA_WIDGET_VISIBILITY_SETTING)
                pref.setExtraWidgetOnClickListener {
                    startActivity(Intent(Settings.ACTION_VOICE_INPUT_SETTINGS))
                }
            }
            else -> pref.setExtraWidgetVisibility(EXTRA_WIDGET_VISIBILITY_GONE)
        }
    }

    override protected fun getCandidates(): MutableList<out CandidateInfo> {
        val context = requireContext()
        val candidates = mutableListOf<CandidateInfo>()

        
        candidates += CandidateInfoExtra(
            context.getText(R.string.aux_button_ringer_title),
            context.getText(R.string.aux_button_ringer_summary),
            KEY_AUX_RINGER,
            true
        )
        candidates += CandidateInfoExtra(
            context.getText(R.string.aux_button_dnd_title),
            context.getText(R.string.aux_button_dnd_summary),
            KEY_AUX_DND,
            true
        )
        candidates += CandidateInfoExtra(
            context.getText(R.string.aux_button_assist_title),
            context.getText(R.string.aux_button_assist_summary),
            KEY_AUX_ASSIST,
            true
        )
        if (hasFlash()) candidates += CandidateInfoExtra(
            context.getText(R.string.aux_button_torch_title),
            context.getText(R.string.aux_button_torch_summary),
            KEY_AUX_TORCH,
            true
        )
        candidates += CandidateInfoExtra(
            context.getText(R.string.aux_button_camera_title),
            context.getText(R.string.aux_button_camera_summary),
            KEY_AUX_CAMERA,
            true
        )
        candidates += CandidateInfoExtra(
            context.getText(R.string.aux_button_none_title),
            null,
            KEY_AUX_NONE,
            true
        )

        return candidates
    }

    override protected fun getDefaultKey(): String {
        val context = requireContext()

        val setting = try {
            Settings.System.getIntForUser(
                context.contentResolver,
                Settings.System.AUX_BUTTON,
                USER_CURRENT
            )
        } catch (e: Exception) {
            -1
        }

        return when (setting) {
            AuxiliaryKeyHandlerActions.ACTION_ASSIST -> KEY_AUX_ASSIST
            AuxiliaryKeyHandlerActions.ACTION_CAMERA -> KEY_AUX_CAMERA
            AuxiliaryKeyHandlerActions.ACTION_TORCH -> KEY_AUX_TORCH
            AuxiliaryKeyHandlerActions.ACTION_DND -> KEY_AUX_DND
            AuxiliaryKeyHandlerActions.ACTION_RINGER -> KEY_AUX_RINGER
            else -> KEY_AUX_NONE
        }
    }

    override protected fun setDefaultKey(key: String): Boolean {
        val context = requireContext()

        val setting = when (key) {
            KEY_AUX_ASSIST -> AuxiliaryKeyHandlerActions.ACTION_ASSIST
            KEY_AUX_CAMERA -> AuxiliaryKeyHandlerActions.ACTION_CAMERA
            KEY_AUX_TORCH -> AuxiliaryKeyHandlerActions.ACTION_TORCH
            KEY_AUX_DND -> AuxiliaryKeyHandlerActions.ACTION_DND
            KEY_AUX_RINGER -> AuxiliaryKeyHandlerActions.ACTION_RINGER
            else -> -1
        }
        
        Settings.System.putIntForUser(
            context.contentResolver,
            Settings.System.AUX_BUTTON,
            setting,
            USER_CURRENT
        )

        return true
    }

    fun hasFlash(): Boolean = requireContext().packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)

    companion object {
        const val KEY_AUX_NONE = "aux_none"
        const val KEY_AUX_ASSIST = "aux_assist"
        const val KEY_AUX_CAMERA = "aux_camera"
        const val KEY_AUX_TORCH = "aux_torch"
        const val KEY_AUX_DND = "aux_dnd"
        const val KEY_AUX_RINGER = "aux_ringer"

        // const val PREF_KEY_SUGGESTION_COMPLETE: String = "pref_double_tap_power_suggestion_complete"
        const val TAG: String = "AuxButton"

        // @JvmStatic const val SEARCH_INDEX_DATA_PROVIDER = BaseSearchIndexProvider(R.xml.aux_button_settings)
    }
}
