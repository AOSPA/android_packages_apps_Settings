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

@file:Suppress("UNCHECKED_CAST")

package com.android.settings.accessibility.captionpreferences.data

import android.content.Context
import android.provider.Settings
import android.view.accessibility.CaptioningManager
import androidx.core.content.getSystemService
import com.android.settings.accessibility.CaptionHelper
import com.android.settingslib.datastore.AbstractKeyedDataObservable
import com.android.settingslib.datastore.DataChangeReason
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.SettingsSecureStore

/** Base data store for caption preferences. */
sealed class CaptionDataStore(protected val context: Context, protected val settingKey: String) :
    AbstractKeyedDataObservable<String>(), KeyValueStore {
    protected val settingsSecureStore = SettingsSecureStore.get(context)
    protected val captionHelper by lazy { CaptionHelper(context) }

    override fun contains(key: String) = settingsSecureStore.contains(settingKey)

    // Whenever a style of a caption is changed, we'll turn on captioning. See b/221051127
    protected fun enableCaptioning() {
        captionHelper.isEnabled = true
    }
}

/** Data store for caption font size preference. */
class CaptionFontSizeDataStore(context: Context) :
    CaptionDataStore(context, Settings.Secure.ACCESSIBILITY_CAPTIONING_FONT_SCALE) {
    private val captionManager = context.getSystemService<CaptioningManager>()
    private val captioningChangeListener by lazy {
        object : CaptioningManager.CaptioningChangeListener() {
            override fun onFontScaleChanged(fontScale: Float) {
                notifyChange(DataChangeReason.UPDATE)
            }
        }
    }

    override fun <T : Any> getDefaultValue(key: String, valueType: Class<T>): T? =
        when (valueType) {
            String::class.javaObjectType -> DEFAULT_CAPTION_SIZE.toString() as? T
            Float::class.javaObjectType -> DEFAULT_CAPTION_SIZE as? T
            else -> super.getDefaultValue(key, valueType)
        }

    override fun <T : Any> getValue(key: String, valueType: Class<T>): T? {
        val currentFontScale = captionManager?.fontScale ?: DEFAULT_CAPTION_SIZE
        return when (valueType) {
            String::class.javaObjectType -> currentFontScale.toString() as? T
            Float::class.javaObjectType -> currentFontScale as? T
            else -> null
        }
    }

    override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {
        when (valueType) {
            String::class.javaObjectType ->
                setFloat(settingKey, (value as? String)?.toFloatOrNull())
            Float::class.javaObjectType -> {
                settingsSecureStore.setFloat(settingKey, value as Float?)
                enableCaptioning()
            }
        }
    }

    override fun onFirstObserverAdded() {
        captionManager?.addCaptioningChangeListener(captioningChangeListener)
    }

    override fun onLastObserverRemoved() {
        captionManager?.removeCaptioningChangeListener(captioningChangeListener)
    }

    companion object {
        const val DEFAULT_CAPTION_SIZE = 1f
    }
}
