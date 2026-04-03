/*
 * Copyright (C) 2026 The Android Open Source Project
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

package com.android.settings.accessibility.captionpreferences.ui

import android.app.settings.SettingsEnums
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.CaptioningManager
import androidx.core.content.getSystemService
import androidx.fragment.app.Fragment
import com.android.settings.R
import com.android.settings.accessibility.AccessibilityUtil
import com.android.settings.accessibility.CaptioningAppearanceFragment
import com.android.settings.accessibility.CaptioningMoreOptionsFragment
import com.android.settings.accessibility.CaptioningPropertiesFragment
import com.android.settings.accessibility.Flags
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.utils.highlightPreference
import com.android.settingslib.datastore.HandlerExecutor
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.preferencesapi.preconditions.PreconditionStability
import com.android.settingslib.metadata.PreferenceCategory
import com.android.settingslib.metadata.PreferenceIndexableProvider
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import kotlinx.coroutines.CoroutineScope

/** Main settings screen for caption preferences. */
// LINT.IfChange(caption_properties_screen)
@ProvidePreferenceScreen(CaptioningPropertiesScreen.KEY)
open class CaptioningPropertiesScreen :
    PreferenceScreenMixin, PreferenceLifecycleProvider, PreferenceSummaryProvider {
    override val highlightMenuKey: Int
        get() = R.string.menu_key_accessibility

    override val key: String
        get() = KEY

    override val purpose: Int
        get() = R.string.caption_preferences_screen_purpose

    override val title: Int
        get() = R.string.accessibility_captioning_title

    override val icon: Int
        get() = R.drawable.ic_captioning

    override val indexable: Boolean
        get() = true

    private var captionEnabledObserver: KeyedObserver<String>? = null

    override fun getMetricsCategory(): Int = SettingsEnums.ACCESSIBILITY_CAPTION_PROPERTIES

    override fun fragmentClass(): Class<out Fragment>? = CaptioningPropertiesFragment::class.java

    override fun isFlagEnabled(context: Context): Boolean = Flags.catalystCaptionPreferencesScreen()

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        Intent(Settings.ACTION_CAPTIONING_SETTINGS).apply { highlightPreference(metadata?.key) }

    override fun getSummary(context: Context): CharSequence? =
        AccessibilityUtil.getSummary(
            context,
            MAIN_SETTING_KEY,
            R.string.show_captions_enabled,
            R.string.show_captions_disabled,
        )

    override fun onCreate(context: PreferenceLifecycleContext) {
        super.onCreate(context)
        if (isEntryPoint(context)) {
            val observer =
                KeyedObserver<String> { _, _ -> context.notifyPreferenceChange(bindingKey) }
            SettingsSecureStore.get(context)
                .addObserver(MAIN_SETTING_KEY, observer, HandlerExecutor.main)
            captionEnabledObserver = observer
        }
    }

    override fun onDestroy(context: PreferenceLifecycleContext) {
        super.onDestroy(context)
        captionEnabledObserver?.let {
            SettingsSecureStore.get(context).removeObserver(MAIN_SETTING_KEY, it)
            captionEnabledObserver = null
        }
    }

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {
            +CaptioningPropertiesTopIntro()
            +CaptioningIllustrationPreference()
            +CaptioningMainSwitchPreference()
            +CaptioningAppearanceScreen.KEY
            +CaptioningMoreOptionsScreen.KEY
            +CaptioningFooterPreference("captioning_settings_footer")
        }

    companion object {
        const val KEY = "captioning_preference_screen"
        private const val MAIN_SETTING_KEY = Settings.Secure.ACCESSIBILITY_CAPTIONING_ENABLED
    }
}
// LINT.ThenChange()

/** Screens where the user can customize the size and style of the caption. */
// LINT.IfChange(caption_appearance_screen)
@ProvidePreferenceScreen(CaptioningAppearanceScreen.KEY)
open class CaptioningAppearanceScreen(context: Context) :
    PreferenceScreenMixin, PreferenceSummaryProvider, PreferenceLifecycleProvider {
    override val highlightMenuKey: Int
        get() = R.string.menu_key_accessibility

    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.captioning_appearance_title

    override val indexable: Boolean
        get() = true

    override val purpose: Int
        get() = R.string.caption_preferences_appearance_purpose

    private val captionFontSizeMetadata by lazy { CaptionFontSizePreference(context) }
    private val captionStyleSizeMetadata by lazy { CaptionStylePreference(context) }

    private var appearanceChangeObserver: KeyedObserver<String?>? = null

    override fun isFlagEnabled(context: Context): Boolean = Flags.catalystCaptionPreferencesScreen()

    override fun fragmentClass(): Class<out Fragment>? = CaptioningAppearanceFragment::class.java

    override fun getMetricsCategory(): Int = SettingsEnums.ACCESSIBILITY_CAPTION_APPEARANCE

    override fun getSummary(context: Context): CharSequence? {
        return context.getString(
            com.android.settingslib.R.string.preference_summary_default_combination,
            captionFontSizeMetadata.getSummary(context),
            captionStyleSizeMetadata.getSummary(context),
        )
    }

    override fun onCreate(context: PreferenceLifecycleContext) {
        super.onCreate(context)
        if (isEntryPoint(context)) {
            val observer =
                KeyedObserver<String?> { _, _ -> context.notifyPreferenceChange(bindingKey) }
            captionFontSizeMetadata.storage(context).addObserver(observer, HandlerExecutor.main)
            captionStyleSizeMetadata.storage(context).addObserver(observer, HandlerExecutor.main)
            appearanceChangeObserver = observer
        }
    }

    override fun onDestroy(context: PreferenceLifecycleContext) {
        super.onDestroy(context)
        appearanceChangeObserver?.let {
            captionFontSizeMetadata.storage(context).removeObserver(it)
            captionStyleSizeMetadata.storage(context).removeObserver(it)
            appearanceChangeObserver = null
        }
    }

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {
            +CaptionAppearancePreviewPreference(context)
            +CaptionFontSizePreference(context)
            +CaptionStylePreference(context)
            +CustomCaptionOptionsPreference() += {
                +CaptionFontFamilyPreference(context)
                +CaptionTextColorPreference(context)
                +CaptionTextOpacityPreference(context)
                +CaptionEdgeTypePreference(context)
                +CaptionEdgeColorPreference(context)
                +CaptionBackgroundColorPreference(context)
                +CaptionBackgroundOpacityPreference(context)
                +CaptionWindowColorPreference(context)
                +CaptionWindowOpacityPreference(context)
            }
            +CaptioningFooterPreference("captioning_appearance_footer")
        }

    companion object {
        const val KEY = "captioning_appearance"
    }
}
// LINT.ThenChange()

/** Displays "More options" in captioning settings. */
// LINT.IfChange(more_options_screen)
@ProvidePreferenceScreen(CaptioningMoreOptionsScreen.KEY)
open class CaptioningMoreOptionsScreen : PreferenceScreenMixin, PreferenceIndexableProvider {
    override val highlightMenuKey: Int
        get() = R.string.menu_key_accessibility

    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.captioning_more_options_title

    override val purpose: Int
        get() = R.string.caption_preferences_more_options_screen_purpose

    override fun isFlagEnabled(context: Context): Boolean = Flags.catalystCaptionPreferencesScreen()

    override fun fragmentClass(): Class<out Fragment>? = CaptioningMoreOptionsFragment::class.java

    override fun getMetricsCategory(): Int = SettingsEnums.ACCESSIBILITY_CAPTION_MORE_OPTIONS

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {
            +CaptionLocalePreference(context)
            +CaptioningFooterPreference("captioning_more_options_footer")
        }

    override fun isIndexable(context: Context) =
        SettingsSecureStore.get(context)
            .getBoolean(Settings.Secure.ACCESSIBILITY_CAPTIONING_ENABLED) ?: false

    companion object {
        const val KEY = "captioning_more_options"
    }
}
// LINT.ThenChange()

// LINT.IfChange(custom_options_category)
/** Category container for custom captioning options. */
class CustomCaptionOptionsPreference :
    PreferenceCategory(
        key = KEY,
        title = R.string.captioning_custom_options_title,
        purpose = R.string.caption_preferences_appearance_custom_options_purpose,
    ),
    PreferenceLifecycleProvider,
    PreferenceAvailabilityProvider {

    private var captionStyleChangeListener: CaptioningManager.CaptioningChangeListener? = null

    override fun onCreate(context: PreferenceLifecycleContext) {
        super.onCreate(context)
        val listener =
            object : CaptioningManager.CaptioningChangeListener() {
                override fun onUserStyleChanged(userStyle: CaptioningManager.CaptionStyle) {
                    context.notifyPreferenceChange(key)
                }
            }
        context.getSystemService<CaptioningManager>()?.addCaptioningChangeListener(listener)
        captionStyleChangeListener = listener
    }

    override fun onDestroy(context: PreferenceLifecycleContext) {
        super.onDestroy(context)
        captionStyleChangeListener?.let {
            context.getSystemService<CaptioningManager>()?.removeCaptioningChangeListener(it)
            captionStyleChangeListener = null
        }
    }

    override val availabilityDescription =
        "The device must have a custom caption style selected."

    override fun getAvailabilityStability() = PreconditionStability.UNSTABLE

    override fun isAvailable(context: Context): Boolean =
        context.getSystemService<CaptioningManager>()?.rawUserStyle ==
            CaptioningManager.CaptionStyle.PRESET_CUSTOM

    companion object {
        const val KEY = "captioning_custom_options_category"
    }
}
// LINT.ThenChange()
