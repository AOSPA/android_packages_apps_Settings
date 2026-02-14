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

import android.content.Context
import android.view.View
import android.view.accessibility.CaptioningManager
import android.view.accessibility.CaptioningManager.CaptionStyle
import androidx.core.content.getSystemService
import androidx.core.view.doOnLayout
import androidx.preference.Preference
import com.android.internal.widget.SubtitleView
import com.android.settings.R
import com.android.settings.accessibility.CaptionHelper
import com.android.settingslib.accessibility.AccessibilityUtils
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.UI_ONLY_PREFERENCE
import com.android.settingslib.preference.PreferenceBinding
import com.android.settingslib.widget.LayoutPreference
import java.util.Locale

/** Preference that provides a preview of the caption appearance settings. */
class CaptionAppearancePreviewPreference(context: Context) :
    PreferenceMetadata, PreferenceBinding, PreferenceLifecycleProvider {
    override val key: String
        get() = KEY

    override val indexable: Boolean
        get() = false

    override val purpose: Int
        get() = R.string.caption_preferences_appearance_preview_purpose

    private val captionHelper by lazy { CaptionHelper(context) }
    private val captioningChangeListener by lazy {
        object : CaptioningManager.CaptioningChangeListener() {
            override fun onUserStyleChanged(userStyle: CaptionStyle) = refreshPreview()

            override fun onFontScaleChanged(fontScale: Float) = refreshPreview()

            override fun onLocaleChanged(locale: Locale?) = refreshPreview()
        }
    }

    private var layoutPreference: LayoutPreference? = null

    override fun tags(context: Context) = arrayOf(UI_ONLY_PREFERENCE)

    override fun createWidget(context: Context): Preference {
        return LayoutPreference(context, R.layout.captioning_preview).apply {
            isSelectable = false
            val previewViewport: View? = findViewById(R.id.preview_viewport)
            previewViewport?.doOnLayout { refreshPreviewText(this) }
        }
    }

    private fun refreshPreview() {
        layoutPreference?.let { refreshPreviewText(it) }
    }

    private fun refreshPreviewText(widget: LayoutPreference) {
        val previewText: SubtitleView = widget.findViewById(R.id.preview_text) ?: return
        val previewViewport: View? = widget.findViewById(R.id.preview_viewport)
        val previewWindow: View = widget.findViewById(R.id.preview_window) ?: return

        // Apply Caption Properties
        val styleId: Int = captionHelper.rawUserStyle
        captionHelper.applyCaptionProperties(previewText, previewViewport, styleId)

        // Set Localized Text
        val locale: Locale? = captionHelper.locale
        val textResId = R.string.captioning_preview_text
        val text =
            if (locale != null) {
                AccessibilityUtils.getTextForLocale(widget.context, locale, textResId)
            } else {
                widget.context.getString(textResId)
            }
        previewText.setText(text)

        // Set Window Background
        val style: CaptionStyle = captionHelper.userStyle
        val windowColor =
            if (style.hasWindowColor()) {
                style.windowColor
            } else {
                CaptionStyle.DEFAULT.windowColor
            }
        previewWindow.setBackgroundColor(windowColor)
    }

    override fun onCreate(context: PreferenceLifecycleContext) {
        super.onCreate(context)
        layoutPreference = context.findPreference(bindingKey)

        context
            .getSystemService<CaptioningManager>()
            ?.addCaptioningChangeListener(captioningChangeListener)
    }

    override fun onDestroy(context: PreferenceLifecycleContext) {
        super.onDestroy(context)
        context
            .getSystemService<CaptioningManager>()
            ?.removeCaptioningChangeListener(captioningChangeListener)
        layoutPreference = null
    }

    companion object {
        const val KEY = "captioning_preview"
    }
}
