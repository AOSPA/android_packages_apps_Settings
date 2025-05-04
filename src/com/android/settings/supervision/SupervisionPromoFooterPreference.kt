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

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.preference.Preference
import com.android.settings.supervision.ipc.PreferenceData
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.preference.PreferenceBinding
import com.android.settingslib.widget.CardPreference
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** A bottom banner promoting other supervision features offered by the supervision app. */
class SupervisionPromoFooterPreference(
    private val preferenceDataProvider: PreferenceDataProvider,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : PreferenceMetadata, PreferenceBinding, PreferenceLifecycleProvider {

    /** Whether [preferenceData] holds an initialized value. */
    private var initialized = false

    private var preferenceData: PreferenceData? = null

    override val key: String
        get() = KEY

    override fun createWidget(context: Context) = CardPreference(context)

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)

        var intent: Intent? = null
        if (initialized) {
            val context = preference.context
            val targetIntent =
                Intent(preferenceData?.action).apply {
                    `package` = preferenceData?.targetPackage
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            intent = if (targetIntent.isValid(context)) targetIntent else null

            val leadingIconResId = preferenceData?.icon
            val leadingIcon =
                leadingIconResId?.let {
                    val resourcePackage = preferenceDataProvider.packageName
                    val icon = Icon.createWithResource(resourcePackage, it)
                    icon.loadDrawable(context)
                }

            preference.intent = intent
            preference.title = preferenceData?.title ?: preference.title
            preference.summary = preferenceData?.summary ?: preference.summary
            preference.icon = leadingIcon ?: preference.icon
            val trailingIcon: Int? = preferenceData?.trailingIcon
            if (trailingIcon != null) {
                (preference as CardPreference).setAdditionalAction(
                    trailingIcon,
                    // TODO(b/411279121): add content description once we have the finalized string.
                    contentDescription = "",
                ) {
                    @SuppressLint("RestrictedApi")
                    it.performClick()
                }
            }
        }

        // Icon, Title, Summary may be null but at least one of title or summary must be valid
        // and the action has to be valid for the preference to be visible.
        preference.isVisible =
            intent != null && (preferenceData?.title != null || preferenceData?.summary != null)
    }

    override fun onResume(context: PreferenceLifecycleContext) {
        context.lifecycleScope.launch {
            preferenceData =
                withContext(coroutineDispatcher) {
                    preferenceDataProvider.getPreferenceData(listOf(KEY))[KEY]
                }
            initialized = true

            context.notifyPreferenceChange(KEY)
        }
    }

    private fun Intent.isValid(context: Context) =
        action != null &&
            `package` != null &&
            context.packageManager.queryIntentActivitiesAsUser(this, 0, context.userId).isNotEmpty()

    companion object {
        const val KEY = "promo_footer"
    }
}
