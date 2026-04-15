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

package com.android.settings.deviceinfo.storage

import android.content.Context
import android.content.Intent
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.metadata.PersistentPreference
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.PreferenceTitleProvider
import com.android.settingslib.metadata.SensitivityLevel

class StoragePreference(
    override val key: String,
    override val purpose: Int,
    override val title: Int,
    val provideIntent: (Context) -> Intent?,
    val provideSummary: (Context) -> CharSequence?,
    val provideTitle: (Context) -> CharSequence? = { it.getString(title) },
    private val tags: List<String> = emptyList(),
) : PersistentPreference<String>,
    PreferenceMetadata,
    PreferenceTitleProvider,
    PreferenceSummaryProvider {
    override val supportsWrite = false
    override val valueType = String::class.javaObjectType
    override fun storage(context: Context): KeyValueStore = createSummaryStorage(context, key)
    override fun getSummary(context: Context) = provideSummary(context)
    override fun getTitle(context: Context) = provideTitle(context)
    override fun intent(context: Context) = provideIntent(context)
    override val sensitivityLevel
        get() = SensitivityLevel.DEEP_LINK_ONLY
    override fun tags(context: Context) = tags.toTypedArray()
}