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
import android.graphics.Color
import android.provider.Settings
import android.view.accessibility.CaptioningManager.CaptionStyle
import com.android.settingslib.datastore.HandlerExecutor
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.datastore.SettingsSecureStore

/** Caption data store that is backed by [SettingsSecureStore] and observes [settingKey]. */
sealed class SecureSettingsCaptionDataStore(context: Context, settingKey: String) :
    CaptionDataStore(context, settingKey) {
    private val observer by lazy { KeyedObserver<String> { _, reason -> notifyChange(reason) } }

    override fun onFirstObserverAdded() {
        settingsSecureStore.addObserver(settingKey, observer, HandlerExecutor.main)
    }

    override fun onLastObserverRemoved() {
        settingsSecureStore.removeObserver(settingKey, observer)
    }
}

/** Data store for caption style preference. */
class CaptionStyleDataStore(appContext: Context) :
    SecureSettingsCaptionDataStore(appContext, Settings.Secure.ACCESSIBILITY_CAPTIONING_PRESET) {

    override fun <T : Any> getDefaultValue(key: String, valueType: Class<T>): T? =
        DEFAULT_STYLE as? T

    override fun <T : Any> getValue(key: String, valueType: Class<T>): T? {
        if (valueType != Int::class.javaObjectType) return null
        return captionHelper.rawUserStyle as T
    }

    override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {
        if (valueType != Int::class.javaObjectType) return
        captionHelper.rawUserStyle = value as Int
        enableCaptioning()
    }

    companion object {
        // This should match CaptioningManager#DEFAULT_PRESET
        const val DEFAULT_STYLE = 0
    }
}

/** Data store for caption font family preference. */
class CaptionFontFamilyDataStore(appContext: Context) :
    SecureSettingsCaptionDataStore(appContext, Settings.Secure.ACCESSIBILITY_CAPTIONING_TYPEFACE) {
    private val contentResolver by lazy { appContext.contentResolver }

    override fun <T : Any> getDefaultValue(key: String, valueType: Class<T>): T? =
        DEFAULT_FONT_FAMILY as? T

    override fun <T : Any> getValue(key: String, valueType: Class<T>): T? {
        if (valueType != String::class.javaObjectType) return null
        val attrs = CaptionStyle.getCustomStyle(contentResolver)
        return (attrs.mRawTypeface ?: DEFAULT_FONT_FAMILY) as? T
    }

    override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {
        if (valueType != String::class.javaObjectType) return
        settingsSecureStore.setString(settingKey, value as String)
        enableCaptioning()
    }

    companion object {
        const val DEFAULT_FONT_FAMILY = ""
    }
}

class CaptionEdgeTypeDataStore(context: Context) :
    SecureSettingsCaptionDataStore(context, Settings.Secure.ACCESSIBILITY_CAPTIONING_EDGE_TYPE) {

    override fun <T : Any> getDefaultValue(key: String, valueType: Class<T>): T? {
        return DEFAULT_EDGE_TYPE as? T
    }

    override fun <T : Any> getValue(key: String, valueType: Class<T>): T? {
        if (valueType != Int::class.javaObjectType) return null
        return captionHelper.edgeType as? T
    }

    override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {
        if (valueType != Int::class.javaObjectType) return
        captionHelper.edgeType = value as? Int ?: DEFAULT_EDGE_TYPE
        enableCaptioning()
    }

    companion object {
        const val DEFAULT_EDGE_TYPE = CaptionStyle.EDGE_TYPE_NONE
    }
}

class CaptionEdgeColorDataStore(context: Context) :
    SecureSettingsCaptionDataStore(context, Settings.Secure.ACCESSIBILITY_CAPTIONING_EDGE_COLOR) {
    override fun <T : Any> getDefaultValue(key: String, valueType: Class<T>): T? {
        return DEFAULT_EDGE_COLOR as? T
    }

    override fun <T : Any> getValue(key: String, valueType: Class<T>): T? {
        if (valueType != Int::class.javaObjectType) return null
        return captionHelper.edgeColor as? T
    }

    override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {
        if (valueType != Int::class.javaObjectType) return
        captionHelper.edgeColor = value as? Int ?: DEFAULT_EDGE_COLOR
        enableCaptioning()
    }

    companion object {
        const val DEFAULT_EDGE_COLOR = Color.BLACK
    }
}
